package sv.edu.ues.vl23003.saldo.sync;

import android.content.Context;
import com.google.firebase.firestore.DocumentReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.dao.MovementDao;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.model.SyncStatus;
import sv.edu.ues.vl23003.saldo.data.remote.FirestoreClient;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
import sv.edu.ues.vl23003.saldo.util.SessionManager;

public class SyncManager {

    private static final String SYNC_WORK_NAME = "pizto_sync_work";

    private final Context context;
    private final MovementDao movementDao;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final FirestoreClient firestoreClient;

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.movementDao = AppDatabase.getInstance(context).abstractMovementDao();
        this.sessionManager = new SessionManager(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.firestoreClient = FirestoreClient.getInstance();
    }

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15,
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }

    public void pushPendingMovements(Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    callback.onError(new Exception("Sin conexion a internet"));
                    return;
                }
                String uid = sessionManager.getSessionUid();
                if (uid == null || uid.isEmpty()) {
                    callback.onError(new Exception("Sesion no activa"));
                    return;
                }

                List<MovementEntity> pending = movementDao.getPendingSync();
                if (pending == null || pending.isEmpty()) {
                    callback.onSuccess(0);
                    return;
                }

                final int total = pending.size();
                final int[] syncedCount = {0};
                final int[] processed = {0};

                for (MovementEntity movement : pending) {
                    Map<String, Object> dataMap = entityToMap(movement);

                    if (movement.getRemoteId() == null || movement.getRemoteId().isEmpty()) {
                        firestoreClient.movementsRef(uid)
                                .add(dataMap)
                                .addOnSuccessListener((DocumentReference ref) -> executor.execute(() -> {
                                    movement.setRemoteId(ref.getId());
                                    movement.setSyncStatus(SyncStatus.SYNCED.name());
                                    movementDao.update(movement);
                                    syncedCount[0]++;
                                    processed[0]++;
                                    if (processed[0] == total) {
                                        callback.onSuccess(syncedCount[0]);
                                    }
                                }))
                                .addOnFailureListener(e -> executor.execute(() -> {
                                    movement.setSyncStatus(SyncStatus.ERROR.name());
                                    movementDao.update(movement);
                                    processed[0]++;
                                    if (processed[0] == total) {
                                        callback.onSuccess(syncedCount[0]);
                                    }
                                }));
                    } else {
                        firestoreClient.movementsRef(uid)
                                .document(movement.getRemoteId())
                                .set(dataMap)
                                .addOnSuccessListener(aVoid -> executor.execute(() -> {
                                    movement.setSyncStatus(SyncStatus.SYNCED.name());
                                    movementDao.update(movement);
                                    syncedCount[0]++;
                                    processed[0]++;
                                    if (processed[0] == total) {
                                        callback.onSuccess(syncedCount[0]);
                                    }
                                }))
                                .addOnFailureListener(e -> executor.execute(() -> {
                                    movement.setSyncStatus(SyncStatus.ERROR.name());
                                    movementDao.update(movement);
                                    processed[0]++;
                                    if (processed[0] == total) {
                                        callback.onSuccess(syncedCount[0]);
                                    }
                                }));
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
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
}