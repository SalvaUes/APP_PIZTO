package sv.edu.ues.vl23003.saldo.ui.movement;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.model.MovementStatus;
import sv.edu.ues.vl23003.saldo.data.model.MovementType;
import sv.edu.ues.vl23003.saldo.data.repository.MovementRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityMovementFormBinding;
import sv.edu.ues.vl23003.saldo.ui.home.HomeActivity;
import sv.edu.ues.vl23003.saldo.util.AppConstants;
import sv.edu.ues.vl23003.saldo.util.Callback;

public class MovementFormActivity extends AppCompatActivity {

    private ActivityMovementFormBinding binding;
    private MovementRepository repository;

    private Calendar selectedDate = Calendar.getInstance();
    private String selectedType = MovementType.INCOME.name();

    private long editingId = -1;
    private MovementEntity editingMovement;
    private boolean saving = false;

    private final Handler navigationHandler = new Handler(Looper.getMainLooper());

    private final String[] categories = {
            "Alimentación",
            "Transporte",
            "Servicios",
            "Ventas",
            "Hogar",
            "Otros"
    };

    private final String[] paymentMethods = {
            "Efectivo",
            "Tarjeta",
            "Transferencia",
            "Otro"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovementFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new MovementRepository(this);
        editingId = getIntent().getLongExtra(AppConstants.EXTRA_MOVEMENT_ID, -1);

        setupToolbar();
        setupTypeSelector();
        setupSpinners();
        setupDatePicker();
        setupSaveButton();
        updateDateDisplay();

        if (editingId != -1) {
            binding.toolbar.setTitle("Editar movimiento");
            loadEditingMovement();
        } else {
            binding.toolbar.setTitle("Nuevo movimiento");
        }
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTypeSelector() {
        binding.chipGroupType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_income) {
                selectedType = MovementType.INCOME.name();
            } else if (checkedId == R.id.chip_expense) {
                selectedType = MovementType.EXPENSE.name();
            }
        });
    }

    private void setupSpinners() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                categories
        );
        binding.actCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                paymentMethods
        );
        binding.actPayment.setAdapter(paymentAdapter);
    }

    private void setupDatePicker() {
        binding.etDate.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.etDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> saveMovement());
    }

    private void loadEditingMovement() {
        repository.getMovementById(editingId, new Callback<MovementEntity>() {
            @Override
            public void onSuccess(MovementEntity data) {
                if (isFinishing()) {
                    return;
                }

                editingMovement = data;
                populateForm(data);
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing()) {
                    return;
                }

                String message = error.getMessage() != null ? error.getMessage() : "No se pudo cargar el movimiento";
                Toast.makeText(MovementFormActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void populateForm(MovementEntity movement) {
        selectedType = movement.getType() != null ? movement.getType() : MovementType.INCOME.name();

        if (MovementType.EXPENSE.name().equals(selectedType)) {
            binding.chipExpense.setChecked(true);
        } else {
            binding.chipIncome.setChecked(true);
        }

        binding.etAmount.setText(String.format(Locale.getDefault(), "%.2f", movement.getAmount()));
        binding.etConcept.setText(movement.getConcept() != null ? movement.getConcept() : "");

        String category = movement.getCategory() != null ? movement.getCategory() : "";
        binding.actCategory.setText(category, false);

        binding.etContact.setText(movement.getContactName() != null ? movement.getContactName() : "");

        String payment = movement.getPaymentMethod() != null ? movement.getPaymentMethod() : "";
        binding.actPayment.setText(payment, false);

        binding.etNote.setText(movement.getNote() != null ? movement.getNote() : "");

        if (movement.getDueDate() > 0) {
            selectedDate = Calendar.getInstance();
            selectedDate.setTimeInMillis(movement.getDueDate());
        } else {
            selectedDate = Calendar.getInstance();
        }

        updateDateDisplay();

        binding.switchPaid.setChecked(MovementStatus.PAID.name().equals(movement.getStatus()));
    }

    private void saveMovement() {
        if (saving) {
            return;
        }

        String amountRaw = binding.etAmount.getText().toString().trim();
        String concept = binding.etConcept.getText().toString().trim();
        String category = binding.actCategory.getText().toString().trim();
        String contact = binding.etContact.getText().toString().trim();
        String payment = binding.actPayment.getText().toString().trim();
        String note = binding.etNote.getText().toString().trim();
        boolean isPaid = binding.switchPaid.isChecked();

        if (amountRaw.isEmpty()) {
            binding.etAmount.setError("Ingrese un monto");
            return;
        }

        String amountNormalized = amountRaw.replace(",", ".");
        double amount;

        try {
            amount = Double.parseDouble(amountNormalized);
            if (amount <= 0) {
                binding.etAmount.setError("El monto debe ser mayor a 0");
                return;
            }
        } catch (NumberFormatException e) {
            binding.etAmount.setError("Monto inválido");
            return;
        }

        if (concept.isEmpty()) {
            binding.etConcept.setError("Ingrese un concepto");
            return;
        }

        if (editingId != -1 && editingMovement == null) {
            Toast.makeText(this, "No se pudo cargar el movimiento original", Toast.LENGTH_LONG).show();
            return;
        }

        saving = true;
        binding.btnSave.setEnabled(false);
        binding.btnSave.setText("Guardando");

        if (editingId != -1) {
            MovementEntity movement = editingMovement;

            movement.setType(selectedType);
            movement.setAmount(amount);
            movement.setConcept(concept);
            movement.setCategory(category.isEmpty() ? "Otros" : category);
            movement.setContactName(contact);
            movement.setPaymentMethod(payment.isEmpty() ? "Efectivo" : payment);
            movement.setNote(note);
            movement.setDueDate(selectedDate.getTimeInMillis());
            movement.setStatus(isPaid ? MovementStatus.PAID.name() : MovementStatus.PENDING.name());

            repository.updateMovement(movement, new Callback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    if (isFinishing()) {
                        return;
                    }

                    Toast.makeText(MovementFormActivity.this, "Movimiento actualizado correctamente", Toast.LENGTH_SHORT).show();
                    goToHomeAfterDelay();
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing()) {
                        return;
                    }

                    saving = false;
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText("Guardar");

                    String message = error.getMessage() != null ? error.getMessage() : "No se pudo actualizar";
                    Toast.makeText(MovementFormActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            MovementEntity movement = new MovementEntity();

            movement.setType(selectedType);
            movement.setAmount(amount);
            movement.setConcept(concept);
            movement.setCategory(category.isEmpty() ? "Otros" : category);
            movement.setContactName(contact);
            movement.setPaymentMethod(payment.isEmpty() ? "Efectivo" : payment);
            movement.setNote(note);
            movement.setDueDate(selectedDate.getTimeInMillis());
            movement.setStatus(isPaid ? MovementStatus.PAID.name() : MovementStatus.PENDING.name());

            repository.createMovement(movement, new Callback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    if (isFinishing()) {
                        return;
                    }

                    Toast.makeText(MovementFormActivity.this, "Movimiento guardado correctamente", Toast.LENGTH_SHORT).show();
                    goToHomeAfterDelay();
                }

                @Override
                public void onError(Exception error) {
                    if (isFinishing()) {
                        return;
                    }

                    saving = false;
                    binding.btnSave.setEnabled(true);
                    binding.btnSave.setText("Guardar");

                    String message = error.getMessage() != null ? error.getMessage() : "No se pudo guardar";
                    Toast.makeText(MovementFormActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void goToHomeAfterDelay() {
        navigationHandler.postDelayed(() -> {
            if (!isFinishing()) {
                goToHome();
            }
        }, 700);
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        navigationHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}