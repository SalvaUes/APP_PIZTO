package sv.edu.ues.vl23003.saldo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.data.repository.MovementRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityHomeBinding;
import sv.edu.ues.vl23003.saldo.ui.map.MapActivity;
import sv.edu.ues.vl23003.saldo.ui.movement.MovementFormActivity;
import sv.edu.ues.vl23003.saldo.ui.movement.MovementListActivity;
import sv.edu.ues.vl23003.saldo.ui.profile.ProfileActivity;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.SessionManager;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private SessionManager sessionManager;
    private MovementRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        repository = new MovementRepository(this);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PIZTO");
        }

        loadUserData();
        loadSummary();
        setupListeners();
    }

    private void loadUserData() {
        String fullName = sessionManager.getSessionFullName();
        String firstName = extractFirstName(fullName);
        binding.tvGreeting.setText("Hola, " + firstName);
        binding.tvLastUpdate.setText("Actualizado " + currentTime());
    }

    private void loadSummary() {
        repository.getFinancialSummary(new Callback<MovementRepository.FinancialSummary>() {
            @Override
            public void onSuccess(MovementRepository.FinancialSummary summary) {
                if (isFinishing()) {
                    return;
                }

                binding.tvBalance.setText(formatCurrency(summary.balance));
                binding.tvIncome.setText(formatCurrency(summary.income));
                binding.tvExpense.setText(formatCurrency(summary.expense));
                binding.tvLastUpdate.setText("Actualizado " + currentTime());
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing()) {
                    return;
                }

                String message = error.getMessage() != null ? error.getMessage() : "No se pudo calcular el resumen";
                Snackbar.make(binding.getRoot(), "Error: " + message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupListeners() {
        binding.fabNewMovement.setOnClickListener(v -> {
            startActivity(new Intent(this, MovementFormActivity.class));
        });

        binding.btnViewMovements.setOnClickListener(v -> {
            startActivity(new Intent(this, MovementListActivity.class));
        });

        binding.btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        binding.btnMap.setOnClickListener(v -> {
            startActivity(new Intent(this, MapActivity.class));
        });
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Usuario";
        }

        String trimmed = fullName.trim();
        String[] parts = trimmed.split("\\s+");

        if (parts.length == 0 || parts[0].isEmpty()) {
            return "Usuario";
        }

        return parts[0];
    }

    private String formatCurrency(double value) {
        if (value < 0) {
            return "-$" + String.format(Locale.getDefault(), "%,.2f", Math.abs(value));
        }

        return "$" + String.format(Locale.getDefault(), "%,.2f", value);
    }

    private String currentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.menu_map) {
            startActivity(new Intent(this, MapActivity.class));
            return true;
        } else if (id == R.id.menu_movements) {
            startActivity(new Intent(this, MovementListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadSummary();
    }
}