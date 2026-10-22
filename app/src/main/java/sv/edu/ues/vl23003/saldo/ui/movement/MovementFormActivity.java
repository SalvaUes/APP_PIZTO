package sv.edu.ues.vl23003.saldo.ui.movement;

import android.app.DatePickerDialog;
import android.os.Bundle;
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
import sv.edu.ues.vl23003.saldo.util.Callback;

public class MovementFormActivity extends AppCompatActivity {

    private ActivityMovementFormBinding binding;
    private MovementRepository repository;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedType = MovementType.INCOME.name();

    private final String[] categories = {"Alimentación", "Transporte", "Servicios", "Ventas", "Hogar", "Otros"};
    private final String[] paymentMethods = {"Efectivo", "Tarjeta", "Transferencia", "Otro"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovementFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new MovementRepository(this);
        setupToolbar();
        setupTypeSelector();
        setupSpinners();
        setupDatePicker();
        setupSaveButton();
        updateDateDisplay();
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
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categories);
        binding.actCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, paymentMethods);
        binding.actPayment.setAdapter(paymentAdapter);
    }

    private void setupDatePicker() {
        binding.etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                updateDateDisplay();
            }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.etDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> saveMovement());
    }

    private void saveMovement() {
        String amountStr = binding.etAmount.getText().toString().trim();
        String concept = binding.etConcept.getText().toString().trim();
        String category = binding.actCategory.getText().toString().trim();
        String contact = binding.etContact.getText().toString().trim();
        String payment = binding.actPayment.getText().toString().trim();
        String note = binding.etNote.getText().toString().trim();
        boolean isPaid = binding.switchPaid.isChecked();

        if (amountStr.isEmpty()) {
            binding.etAmount.setError("Ingrese un monto");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
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
                Toast.makeText(MovementFormActivity.this, "Movimiento guardado", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MovementFormActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}