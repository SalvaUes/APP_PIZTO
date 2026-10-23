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
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
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
        loadLastSync();
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
            try {
                int movementPending = movementRepository.getPendingCount();
                int profilePending = userRepository.getPendingProfileCount();
                int total = movementPending + profilePending;

                runOnUiThread(() -> {
                    if (total == 0) {
                        binding.tvPendingCount.setText("No hay cambios pendientes");
                    } else {
                        binding.tvPendingCount.setText(String.format(Locale.getDefault(), "Cambios pendientes: %d", total));
                    }

                });
            } catch (Exception e) {
                runOnUiThread(() -> binding.tvPendingCount.setText("No se pudo calcular la sincronización"));
            }
        });
    }

    private void loadLastSync() {
        long timestamp = sessionManager.getLastSync();
        if (timestamp == 0L) {
            binding.tvLastSync.setText("Última sincronización con Firebase: nunca");
            return;
        }

        String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(timestamp));
        binding.tvLastSync.setText(String.format(Locale.getDefault(),
                "Última sincronización con Firebase: %s", formatted));
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

        userRepository.updateProfile(newFullName, uid, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean synced) {
                runOnUiThread(() -> {
                    binding.btnUpdateName.setEnabled(true);
                    if (Boolean.TRUE.equals(synced)) {
                        showUpdateConfirmation("Nombre actualizado correctamente.");
                    } else {
                        showUpdateConfirmation("Nombre guardado localmente. Quedará pendiente de sincronización.");
                    }
                    loadPendingCount();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    binding.btnUpdateName.setEnabled(true);
                    String message = error.getMessage() != null ? error.getMessage() : "No se pudo actualizar el nombre";
                    Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
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

        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, "Se requiere internet para cambiar la contrasena", Toast.LENGTH_LONG).show();
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
                    showUpdateConfirmation("Contraseña actualizada correctamente.");
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    binding.btnUpdatePassword.setEnabled(true);
                    String message = error.getMessage() != null ? error.getMessage() : "No se pudo actualizar la contrasena";
                    Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void syncNow() {
        binding.btnSyncNow.setEnabled(false);
        binding.btnSyncNow.setText("Sincronizando");

        syncManager.syncAllPendingChanges(new Callback<Integer>() {
            @Override
            public void onSuccess(Integer synced) {
                runOnUiThread(() -> {
                    binding.btnSyncNow.setEnabled(true);
                    binding.btnSyncNow.setText("Reintentar sincronizacion");

                    int count = synced != null ? synced : 0;
                    if (count == 0) {
                        Toast.makeText(ProfileActivity.this, "No habia cambios pendientes", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this,
                                String.format(Locale.getDefault(), "Cambios sincronizados: %d", count),
                                Toast.LENGTH_SHORT).show();
                    }

                    loadLastSync();
                    loadPendingCount();
                });
            }

            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    binding.btnSyncNow.setEnabled(true);
                    binding.btnSyncNow.setText("Reintentar sincronizacion");
                    String message = error.getMessage() != null ? error.getMessage() : "No se pudo sincronizar";
                    Toast.makeText(ProfileActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
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

    private void showUpdateConfirmation(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Actualización guardada")
                .setMessage(message)
                .setPositiveButton("Ir al inicio", (dialog, which) -> navigateToHome())
                .setCancelable(false)
                .show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, sv.edu.ues.vl23003.saldo.ui.home.HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
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
        loadLastSync();
        loadPendingCount();
        if (NetworkUtil.isNetworkAvailable(this)) {
            syncManager.syncAllPendingChanges(new Callback<Integer>() {
                @Override
                public void onSuccess(Integer value) {
                    runOnUiThread(() -> {
                        loadLastSync();
                        loadPendingCount();
                    });
                }

                @Override
                public void onError(Exception error) {
                    // Pending records remain untouched and can be retried manually.
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}