package sv.edu.ues.vl23003.saldo.sync;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.dao.MovementDao;
import sv.edu.ues.vl23003.saldo.data.local.dao.UserDao;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;
import sv.edu.ues.vl23003.saldo.data.model.SyncStatus;
import sv.edu.ues.vl23003.saldo.data.remote.FirestoreClient;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
import sv.edu.ues.vl23003.saldo.util.SessionManager;

public class SyncManager {

    private static final String SYNC_WORK_NAME = "pizto_sync_work";
    private static final String PROFILE_DOC_ID = "data";

    private final Context context;
    private final MovementDao movementDao;
    private final UserDao userDao;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final FirestoreClient firestoreClient;

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = AppDatabase.getInstance(context);
        this.movementDao = database.abstractMovementDao();
        this.userDao = database.abstractUserDao();
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
        syncAllPendingChanges(callback);
    }

    public void syncAllPendingChanges(Callback<Integer> callback) {
        executor.execute(() -> {
            try {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    callback.onError(new Exception("Sin conexion a internet"));
                    return;
                }

                List<MovementEntity> pendingMovements = movementDao.getPendingSync();
                List<UserEntity> pendingUsers = userDao.getPendingUsers();

                int movementsSize = pendingMovements == null ? 0 : pendingMovements.size();
                int usersSize = pendingUsers == null ? 0 : pendingUsers.size();
                int total = movementsSize + usersSize;

                if (total == 0) {
                    callback.onSuccess(0);
                    return;
                }

                final AtomicInteger processed = new AtomicInteger(0);
                final AtomicInteger synced = new AtomicInteger(0);
                final String sessionUid = sessionManager.getSessionUid();

                if (pendingMovements != null) {
                    for (MovementEntity movement : pendingMovements) {
                        if (isValidUid(sessionUid)) {
                            pushMovement(movement, sessionUid, processed, synced, callback, total);
                        } else {
                            finishItem(processed, synced, callback, total, false);
                        }
                    }
                }

                if (pendingUsers != null) {
                    for (UserEntity user : pendingUsers) {
                        String uid = user.getUid();
                        if (isValidUid(uid)) {
                            pushUser(user, uid, processed, synced, callback, total);
                        } else {
                            finishItem(processed, synced, callback, total, false);
                        }
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    private boolean isValidUid(String uid) {
        return uid != null && !uid.isEmpty() && !uid.startsWith("offline");
    }

    private void finishItem(AtomicInteger processed,
                            AtomicInteger synced,
                            Callback<Integer> callback,
                            int total,
                            boolean success) {
        if (success) {
            synced.incrementAndGet();
        }

        int done = processed.incrementAndGet();
        if (done == total) {
            callback.onSuccess(synced.get());
        }
    }

    private void pushMovement(MovementEntity movement,
                              String uid,
                              AtomicInteger processed,
                              AtomicInteger synced,
                              Callback<Integer> callback,
                              int total) {
        Map<String, Object> data = entityToMap(movement);

        if (movement.getRemoteId() == null || movement.getRemoteId().isEmpty()) {
            firestoreClient.movementsRef(uid)
                    .add(data)
                    .addOnSuccessListener((DocumentReference ref) -> executor.execute(() -> {
                        try {
                            movement.setRemoteId(ref.getId());
                            movement.setSyncStatus(SyncStatus.SYNCED.name());
                            movement.setUpdatedAt(System.currentTimeMillis());
                            movementDao.update(movement);
                            finishItem(processed, synced, callback, total, true);
                        } catch (Exception e) {
                            finishItem(processed, synced, callback, total, false);
                        }
                    }))
                    .addOnFailureListener(e -> finishItem(processed, synced, callback, total, false));
        } else {
            firestoreClient.movementsRef(uid)
                    .document(movement.getRemoteId())
                    .set(data)
                    .addOnSuccessListener(aVoid -> executor.execute(() -> {
                        try {
                            movement.setSyncStatus(SyncStatus.SYNCED.name());
                            movement.setUpdatedAt(System.currentTimeMillis());
                            movementDao.update(movement);
                            finishItem(processed, synced, callback, total, true);
                        } catch (Exception e) {
                            finishItem(processed, synced, callback, total, false);
                        }
                    }))
                    .addOnFailureListener(e -> finishItem(processed, synced, callback, total, false));
        }
    }

    private void pushUser(UserEntity user,
                          String uid,
                          AtomicInteger processed,
                          AtomicInteger synced,
                          Callback<Integer> callback,
                          int total) {
        Map<String, Object> data = userToMap(user);

        firestoreClient.usersRef(uid)
                .document(PROFILE_DOC_ID)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> executor.execute(() -> {
                    try {
                        user.setSyncStatus(SyncStatus.SYNCED.name());
                        user.setUpdatedAt(System.currentTimeMillis());
                        userDao.update(user);
                        finishItem(processed, synced, callback, total, true);
                    } catch (Exception e) {
                        finishItem(processed, synced, callback, total, false);
                    }
                }))
                .addOnFailureListener(e -> finishItem(processed, synced, callback, total, false));
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

    private Map<String, Object> userToMap(UserEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("email", entity.getEmail());
        map.put("fullName", entity.getFullName());
        map.put("createdAt", entity.getCreatedAt());
        map.put("updatedAt", entity.getUpdatedAt());
        return map;
    }
}