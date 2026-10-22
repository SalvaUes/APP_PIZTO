package sv.edu.ues.vl23003.saldo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.databinding.ActivityHomeBinding;
import sv.edu.ues.vl23003.saldo.ui.map.MapActivity;
import sv.edu.ues.vl23003.saldo.ui.movement.MovementFormActivity;
import sv.edu.ues.vl23003.saldo.ui.movement.MovementListActivity;
import sv.edu.ues.vl23003.saldo.ui.profile.ProfileActivity;
import sv.edu.ues.vl23003.saldo.util.SessionManager;




public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // aqui se inicializa el binding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // aqui se inicializa el sessionManager
        sessionManager = new SessionManager(this);

        //  Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PIZTO");
        }

        // Cargar datos del usuario
        loadUserData();


        setupListeners();
    }



    private void loadUserData() {
        String fullName = sessionManager.getSessionFullName();
        if (fullName != null && !fullName.isEmpty()) {
            binding.tvGreeting.setText("Hola, " + fullName);
        } else {
            binding.tvGreeting.setText("Hola, Usuario");
        }

        // Cargar saldo, ingresos y gastos desde MovementRepository

        binding.tvBalance.setText("$0.00");
        binding.tvIncome.setText("$0.00");
        binding.tvExpense.setText("$0.00");
        binding.tvLastUpdate.setText("Última actualización: ahora");
    }

    private void setupListeners() {
        // Boton flotante para el nuevo movimiento
        binding.fabNewMovement.setOnClickListener(v -> {
            Intent intent = new Intent(this, MovementFormActivity.class);
            startActivity(intent);
        });

        // Boton para ver todos los movimientos
        binding.btnViewMovements.setOnClickListener(v -> {
            Intent intent = new Intent(this, MovementListActivity.class);
            startActivity(intent);
        });
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
}