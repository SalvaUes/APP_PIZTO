package sv.edu.ues.vl23003.saldo.ui.movement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.model.MovementStatus;
import sv.edu.ues.vl23003.saldo.data.model.MovementType;
import sv.edu.ues.vl23003.saldo.data.model.SyncStatus;
import sv.edu.ues.vl23003.saldo.data.repository.MovementRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityMovementDetailBinding;
import sv.edu.ues.vl23003.saldo.util.AppConstants;
import sv.edu.ues.vl23003.saldo.util.Callback;

public class MovementDetailActivity extends AppCompatActivity {

    private ActivityMovementDetailBinding binding;
    private MovementRepository repository;
    private MovementEntity movement;
    private long movementId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovementDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new MovementRepository(this);
        movementId = getIntent().getLongExtra(AppConstants.EXTRA_MOVEMENT_ID, -1);

        if (movementId == -1) {
            Toast.makeText(this, "Movimiento no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        loadMovement();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadMovement() {
        repository.getMovementById(movementId, new Callback<MovementEntity>() {
            @Override
            public void onSuccess(MovementEntity data) {
                movement = data;
                displayMovement();
                setupButtons();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MovementDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayMovement() {
        String typeText = MovementType.INCOME.name().equals(movement.getType()) ? "Ingreso" : "Gasto";
        binding.tvType.setText(typeText);

        String amountText = "$" + String.format(Locale.getDefault(), "%.2f", movement.getAmount());
        if (MovementType.INCOME.name().equals(movement.getType())) {
            binding.tvAmount.setText("+" + amountText);
            binding.tvAmount.setTextColor(getColor(R.color.pizto_income));
        } else {
            binding.tvAmount.setText("-" + amountText);
            binding.tvAmount.setTextColor(getColor(R.color.pizto_expense));
        }

        String syncText = "Sincronización: ";
        if (SyncStatus.SYNCED.name().equals(movement.getSyncStatus())) {
            syncText += "Sincronizado";
        } else if (SyncStatus.ERROR.name().equals(movement.getSyncStatus())) {
            syncText += "Error (reintentar desde Perfil)";
        } else {
            syncText += "Pendiente";
        }
        binding.tvSyncStatus.setText(syncText);

        binding.tvConcept.setText(movement.getConcept() != null ? movement.getConcept() : "Sin concepto");
        binding.tvCategory.setText(movement.getCategory() != null ? movement.getCategory() : "Sin categoría");
        binding.tvContact.setText(movement.getContactName() != null && !movement.getContactName().isEmpty() ? movement.getContactName() : "No especificado");
        binding.tvPayment.setText(movement.getPaymentMethod() != null && !movement.getPaymentMethod().isEmpty() ? movement.getPaymentMethod() : "No especificado");

        if (movement.getDueDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            binding.tvDate.setText(sdf.format(new Date(movement.getDueDate())));
        } else {
            binding.tvDate.setText("Sin fecha");
        }

        String statusText = MovementStatus.PAID.name().equals(movement.getStatus()) ? "Pagado" : "Pendiente";
        binding.tvStatus.setText(statusText);

        binding.tvNote.setText(movement.getNote() != null && !movement.getNote().isEmpty() ? movement.getNote() : "Sin nota");

        if (MovementStatus.PAID.name().equals(movement.getStatus())) {
            binding.btnMarkPaid.setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        binding.btnMarkPaid.setOnClickListener(v -> markAsPaid());
        binding.btnEdit.setOnClickListener(v -> editMovement());
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void markAsPaid() {
        movement.setStatus(MovementStatus.PAID.name());
        repository.updateMovement(movement, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(MovementDetailActivity.this, "Marcado como pagado", Toast.LENGTH_SHORT).show();
                displayMovement();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MovementDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void editMovement() {
        Intent intent = new Intent(this, MovementFormActivity.class);
        intent.putExtra(AppConstants.EXTRA_MOVEMENT_ID, movement.getId());
        startActivity(intent);
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar movimiento")
                .setMessage("¿Estás seguro de que deseas eliminar este movimiento?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteMovement())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteMovement() {
        repository.deleteMovement(movement.getId(), new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(MovementDetailActivity.this, "Movimiento eliminado", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MovementDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}