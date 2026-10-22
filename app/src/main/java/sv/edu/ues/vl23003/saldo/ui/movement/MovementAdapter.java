package sv.edu.ues.vl23003.saldo.ui.movement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.model.MovementType;

public class MovementAdapter extends RecyclerView.Adapter<MovementAdapter.MovementViewHolder> {

    private List<MovementEntity> movements = new ArrayList<>();
    private OnMovementClickListener listener;

    public interface OnMovementClickListener {
        void onMovementClick(MovementEntity movement);
    }

    public void setOnMovementClickListener(OnMovementClickListener listener) {
        this.listener = listener;
    }

    public void setMovements(List<MovementEntity> movements) {
        this.movements = movements;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movement, parent, false);
        return new MovementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovementViewHolder holder, int position) {
        MovementEntity movement = movements.get(position);
        holder.bind(movement, listener);
    }

    @Override
    public int getItemCount() {
        return movements.size();
    }

    static class MovementViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvConcept;
        private final TextView tvCategory;
        private final TextView tvDate;
        private final TextView tvAmount;

        public MovementViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConcept = itemView.findViewById(R.id.tv_concept);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }

        public void bind(MovementEntity movement, OnMovementClickListener listener) {
            tvConcept.setText(movement.getConcept() != null ? movement.getConcept() : "Sin concepto");
            tvCategory.setText(movement.getCategory() != null ? movement.getCategory() : "Sin categoría");

            if (movement.getDueDate() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(movement.getDueDate())));
            } else {
                tvDate.setText("Sin fecha");
            }

            String amountText = "$" + String.format(Locale.getDefault(), "%.2f", movement.getAmount());
            if (MovementType.INCOME.name().equals(movement.getType())) {
                tvAmount.setText("+" + amountText);
            } else {
                tvAmount.setText("-" + amountText);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMovementClick(movement);
                }
            });
        }
    }
}