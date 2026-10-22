package sv.edu.ues.vl23003.saldo.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sv.edu.ues.vl23003.saldo.data.repository.MovementRepository;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityProfileBinding;
import sv.edu.ues.vl23003.saldo.sync.SyncManager;
import sv.edu.ues.vl23003.saldo.ui.auth.LoginActivity;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.SessionManager;
import sv.edu.ues.vl23003.saldo.util.Validators;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private MovementRepository movementRepository;
    private SyncManager syncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);
        movementRepository = new MovementRepository(this);
        syncManager = new SyncManager(this);

        setupToolbar();
        loadUserData();
        loadPendingCount();
        setupButtons();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        String email = sessionManager.getSessionEmail();
        String fullName = sessionManager.getSessionFullName();
        binding.tvEmail.setText(email != null ? email : "");
        binding.etFullName.setText(fullName != null ? fullName : "");
    }

    private void loadPendingCount() {
        executor.execute(() -> {
            int count = movementRepository.getPendingCount();
            runOnUiThread(() -> binding.tvPendingCount.setText(
                    String.format(Locale.getDefault(), "Movimientos pendientes: %d", count)));
        });
    }

    private void setupButtons() {
        binding.btnUpdateName.setOnClickListener(v -> updateName());
        binding.btnUpdatePassword.setOnClickListener(v -> updatePassword());
        binding.btnSyncNow.setOnClickListener(v -> syncNow());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
    }

    private void updateName() {
        String newFullName = binding.etFullName.getText().toString().trim();
        if (!Validators.isValidFullName(newFullName)) {
            binding.etFullName.setError("El nombre debe tener minimo 7 caracteres");
            return;
        }
        String uid = sessionManager.getSessionUid();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Sesion expirada", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnUpdateName.setEnabled(false);
        userRepository.updateProfile(newFullName, uid, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    binding.btnUpdateName.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Nombre actualizado", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    binding.btnUpdateName.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updatePassword() {
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!Validators.isValidPassword(newPassword)) {
            binding.etNewPassword.setError("Minimo 8 caracteres alfanumericos");
            return;
        }
        if (!Validators.passwordsMatch(newPassword, confirmPassword)) {
            binding.etConfirmPassword.setError("Las contrasenas no coinciden");
            return;
        }

        String uid = sessionManager.getSessionUid();
        if (uid == null || uid.isEmpty()) {
            Toast.makeText(this, "Sesion expirada", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnUpdatePassword.setEnabled(false);
        userRepository.updatePassword(newPassword, uid, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                runOnUiThread(() -> {
                    binding.btnUpdatePassword.setEnabled(true);
                    binding.etNewPassword.setText("");
                    binding.etConfirmPassword.setText("");
                    Toast.makeText(ProfileActivity.this, "Contrasena actualizada", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    binding.btnUpdatePassword.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void syncNow() {
        binding.btnSyncNow.setEnabled(false);
        binding.btnSyncNow.setText("Sincronizando...");
        syncManager.pushPendingMovements(new Callback<Integer>() {
            @Override
            public void onSuccess(Integer synced) {
                runOnUiThread(() -> {
                    binding.btnSyncNow.setEnabled(true);
                    binding.btnSyncNow.setText("Reintentar sincronizacion");
                    int count = synced != null ? synced : 0;
                    Toast.makeText(ProfileActivity.this,
                            String.format(Locale.getDefault(), "Sincronizados: %d", count),
                            Toast.LENGTH_SHORT).show();
                    String now = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
                    binding.tvLastSync.setText(String.format(Locale.getDefault(), "Ultima sincronizacion: %s", now));
                    loadPendingCount();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    binding.btnSyncNow.setEnabled(true);
                    binding.btnSyncNow.setText("Reintentar sincronizacion");
                    Toast.makeText(ProfileActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("Estas seguro de que deseas cerrar sesion?")
                .setPositiveButton("Cerrar", (dialog, which) -> logout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}