package sv.edu.ues.vl23003.saldo.ui.movement;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.List;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.model.MovementStatus;
import sv.edu.ues.vl23003.saldo.data.model.MovementType;
import sv.edu.ues.vl23003.saldo.data.repository.MovementRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityMovementListBinding;
import sv.edu.ues.vl23003.saldo.util.AppConstants;
import sv.edu.ues.vl23003.saldo.util.Callback;

public class MovementListActivity extends AppCompatActivity {

    private ActivityMovementListBinding binding;
    private MovementAdapter adapter;
    private MovementRepository repository;
    private List<MovementEntity> allMovements = new ArrayList<>();
    private String currentFilter = "ALL";
    private String currentSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovementListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new MovementRepository(this);
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        setupSearch();
        setupFab();
        loadMovements();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new MovementAdapter();
        adapter.setOnMovementClickListener(movement -> {
            Intent intent = new Intent(this, MovementDetailActivity.class);
            intent.putExtra(AppConstants.EXTRA_MOVEMENT_ID, movement.getId());
            startActivity(intent);
        });
        binding.rvMovements.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMovements.setAdapter(adapter);
    }

    private void setupFilters() {
        binding.chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_all) {
                currentFilter = "ALL";
            } else if (checkedId == R.id.chip_income) {
                currentFilter = "INCOME";
            } else if (checkedId == R.id.chip_expense) {
                currentFilter = "EXPENSE";
            } else if (checkedId == R.id.chip_pending) {
                currentFilter = "PENDING";
            }
            applyFilters();
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, MovementFormActivity.class);
            startActivity(intent);
        });
    }

    private void loadMovements() {
        repository.getAllMovements(new Callback<List<MovementEntity>>() {
            @Override
            public void onSuccess(List<MovementEntity> data) {
                allMovements = data != null ? data : new ArrayList<>();
                applyFilters();
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(MovementListActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        List<MovementEntity> filtered = new ArrayList<>();

        for (MovementEntity m : allMovements) {
            boolean matchesFilter = true;
            boolean matchesSearch = true;

            if (!currentFilter.equals("ALL")) {
                if (currentFilter.equals("INCOME")) {
                    matchesFilter = MovementType.INCOME.name().equals(m.getType());
                } else if (currentFilter.equals("EXPENSE")) {
                    matchesFilter = MovementType.EXPENSE.name().equals(m.getType());
                } else if (currentFilter.equals("PENDING")) {
                    matchesFilter = MovementStatus.PENDING.name().equals(m.getStatus());
                }
            }

            if (!currentSearch.isEmpty()) {
                String concept = m.getConcept() != null ? m.getConcept().toLowerCase() : "";
                String category = m.getCategory() != null ? m.getCategory().toLowerCase() : "";
                matchesSearch = concept.contains(currentSearch) || category.contains(currentSearch);
            }

            if (matchesFilter && matchesSearch) {
                filtered.add(m);
            }
        }

        adapter.setMovements(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvMovements.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMovements();
    }
}