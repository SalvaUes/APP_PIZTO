package sv.edu.ues.vl23003.saldo.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.dao.MovementDao;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.model.MovementType;
import sv.edu.ues.vl23003.saldo.data.model.SyncStatus;
import sv.edu.ues.vl23003.saldo.data.remote.FirestoreClient;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
import sv.edu.ues.vl23003.saldo.util.SessionManager;

public class MovementRepository {

    private final MovementDao movementDao;
    private final FirestoreClient firestoreClient;
    private final SessionManager sessionManager;
    private final Context context;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public MovementRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.movementDao = database.abstractMovementDao();
        this.firestoreClient = FirestoreClient.getInstance();
        this.sessionManager = new SessionManager(context);
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public long create(MovementEntity movement) {
        movement.setSyncStatus(SyncStatus.PENDING_SYNC.name());
        movement.setCreatedAt(System.currentTimeMillis());
        movement.setUpdatedAt(movement.getCreatedAt());
        return movementDao.insert(movement);
    }

    public long saveLocal(MovementEntity movement) {
        return create(movement);
    }

    public void update(MovementEntity movement) {
        movement.setSyncStatus(SyncStatus.PENDING_SYNC.name());
        movement.setUpdatedAt(System.currentTimeMillis());
        movementDao.update(movement);
    }

    public void delete(MovementEntity movement) {
        movementDao.delete(movement);
    }

    public List<MovementEntity> getAll() {
        return movementDao.getAllByDateDesc();
    }

    public MovementEntity getById(long id) {
        return movementDao.getById(id);
    }

    public int getPendingCount() {
        return movementDao.countPending();
    }

    public void createMovement(MovementEntity movement, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                movement.setSyncStatus(SyncStatus.PENDING_SYNC.name());
                movement.setCreatedAt(System.currentTimeMillis());
                movement.setUpdatedAt(movement.getCreatedAt());

                long id = movementDao.insert(movement);
                movement.setId(id);

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
                return;
            }

            // El movimiento ya quedó guardado en la base de datos local.
            mainHandler.post(() -> callback.onSuccess(null));
        });
    }

    public void updateMovement(MovementEntity movement, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                movement.setSyncStatus(SyncStatus.PENDING_SYNC.name());
                movement.setUpdatedAt(System.currentTimeMillis());

                movementDao.update(movement);

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
                return;
            }

            // La actualización local ya terminó.
            mainHandler.post(() -> callback.onSuccess(null));
        });
    }

    public void deleteMovement(long id, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                MovementEntity movement = movementDao.getById(id);
                if (movement == null) {
                    mainHandler.post(() -> callback.onError(new Exception("Movimiento no encontrado")));
                    return;
                }

                movementDao.delete(movement);

                if (canSync() && movement.getRemoteId() != null && !movement.getRemoteId().isEmpty()) {
                    pushDelete(movement, callback);
                } else {
                    mainHandler.post(() -> callback.onSuccess(null));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getAllMovements(Callback<List<MovementEntity>> callback) {
        executor.execute(() -> {
            try {
                List<MovementEntity> list = movementDao.getAllByDateDesc();
                mainHandler.post(() -> callback.onSuccess(list));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getMovementById(long id, Callback<MovementEntity> callback) {
        executor.execute(() -> {
            try {
                MovementEntity movement = movementDao.getById(id);
                if (movement == null) {
                    mainHandler.post(() -> callback.onError(new Exception("Movimiento no encontrado")));
                } else {
                    mainHandler.post(() -> callback.onSuccess(movement));
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getFinancialSummary(Callback<FinancialSummary> callback) {
        executor.execute(() -> {
            try {
                List<MovementEntity> list = movementDao.getAllByDateDesc();
                if (list == null) {
                    list = new ArrayList<>();
                }

                double income = 0.0;
                double expense = 0.0;

                for (MovementEntity movement : list) {
                    if (movement == null) {
                        continue;
                    }

                    double amount = movement.getAmount();

                    if (MovementType.INCOME.name().equals(movement.getType())) {
                        income += amount;
                    } else if (MovementType.EXPENSE.name().equals(movement.getType())) {
                        expense += amount;
                    }
                }

                double balance = income - expense;
                FinancialSummary summary = new FinancialSummary(income, expense, balance);
                mainHandler.post(() -> callback.onSuccess(summary));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private boolean canSync() {
        String uid = currentUid();
        return NetworkUtil.isNetworkAvailable(context)
                && uid != null
                && !uid.isEmpty()
                && !uid.startsWith("offline");
    }

    private String currentUid() {
        String uid = sessionManager.getSessionUid();
        return uid == null ? "" : uid;
    }

    private void pushCreate(MovementEntity movement, Callback<Void> callback) {
        firestoreClient.movementsRef(currentUid())
                .add(entityToMap(movement))
                .addOnSuccessListener(ref -> executor.execute(() -> {
                    try {
                        movement.setRemoteId(ref.getId());
                        movement.setSyncStatus(SyncStatus.SYNCED.name());
                        movement.setUpdatedAt(System.currentTimeMillis());
                        movementDao.update(movement);
                        mainHandler.post(() -> callback.onSuccess(null));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError(e));
                    }
                }))
                .addOnFailureListener(e -> mainHandler.post(() -> callback.onSuccess(null)));
    }

    private void pushUpsert(MovementEntity movement, Callback<Void> callback) {
        String uid = currentUid();
        Map<String, Object> data = entityToMap(movement);

        if (movement.getRemoteId() == null || movement.getRemoteId().isEmpty()) {
            firestoreClient.movementsRef(uid)
                    .add(data)
                    .addOnSuccessListener(ref -> executor.execute(() -> {
                        try {
                            movement.setRemoteId(ref.getId());
                            movement.setSyncStatus(SyncStatus.SYNCED.name());
                            movement.setUpdatedAt(System.currentTimeMillis());
                            movementDao.update(movement);
                            mainHandler.post(() -> callback.onSuccess(null));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }))
                    .addOnFailureListener(e -> mainHandler.post(() -> callback.onSuccess(null)));
        } else {
            firestoreClient.movementsRef(uid)
                    .document(movement.getRemoteId())
                    .set(data)
                    .addOnSuccessListener(aVoid -> executor.execute(() -> {
                        try {
                            movement.setSyncStatus(SyncStatus.SYNCED.name());
                            movement.setUpdatedAt(System.currentTimeMillis());
                            movementDao.update(movement);
                            mainHandler.post(() -> callback.onSuccess(null));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    }))
                    .addOnFailureListener(e -> mainHandler.post(() -> callback.onSuccess(null)));
        }
    }

    private void pushDelete(MovementEntity movement, Callback<Void> callback) {
        firestoreClient.movementsRef(currentUid())
                .document(movement.getRemoteId())
                .delete()
                .addOnSuccessListener(aVoid -> mainHandler.post(() -> callback.onSuccess(null)))
                .addOnFailureListener(e -> mainHandler.post(() -> callback.onSuccess(null)));
    }

    private Map<String, Object> entityToMap(MovementEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("type", entity.getType());
        map.put("amount", entity.getAmount());
        map.put("concept", entity.getConcept());
        map.put("category", entity.getCategory());
        map.put("contactName", entity.getContactName());
        map.put("paymentMethod", entity.getPaymentMethod());
        map.put("note", entity.getNote());
        map.put("photoPath", entity.getPhotoPath());
        map.put("dueDate", entity.getDueDate());
        map.put("status", entity.getStatus());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }

    public static final class FinancialSummary {
        public final double income;
        public final double expense;
        public final double balance;

        public FinancialSummary(double income, double expense, double balance) {
            this.income = income;
            this.expense = expense;
            this.balance = balance;
        }
    }
}