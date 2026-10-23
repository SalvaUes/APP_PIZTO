package sv.edu.ues.vl23003.saldo.data.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.dao.UserDao;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;
import sv.edu.ues.vl23003.saldo.data.model.SyncStatus;
import sv.edu.ues.vl23003.saldo.data.remote.FirestoreClient;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
import sv.edu.ues.vl23003.saldo.util.PasswordUtil;
import sv.edu.ues.vl23003.saldo.util.SessionManager;

public class UserRepository {

    private static final String PROFILE_DOC_ID = "data";

    private final UserDao userDao;
    private final FirebaseAuth auth;
    private final FirestoreClient firestoreClient;
    private final SessionManager sessionManager;
    private final Context context;
    private final ExecutorService executor;

    public UserRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.userDao = db.abstractUserDao();
        this.auth = FirebaseAuth.getInstance();
        this.firestoreClient = FirestoreClient.getInstance();
        this.sessionManager = new SessionManager(context);
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void register(String email, String fullName, String password, final Callback<Void> callback) {
        executor.execute(() -> {
            try {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    callback.onError(new Exception("Se requiere internet para el registro inicial"));
                    return;
                }

                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            if (authResult.getUser() == null) {
                                callback.onError(new Exception("No se pudo obtener el identificador del usuario"));
                                return;
                            }

                            String uid = authResult.getUser().getUid();

                            executor.execute(() -> {
                                try {
                                    UserEntity entity = buildUserEntity(uid, email, fullName, password);
                                    userDao.insert(entity);
                                    pushUserToFirestore(uid, email, fullName, new Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            executor.execute(() -> {
                                                try {
                                                    UserEntity saved = userDao.findByUid(uid);
                                                    if (saved != null) {
                                                        saved.setSyncStatus(SyncStatus.SYNCED.name());
                                                        saved.setUpdatedAt(System.currentTimeMillis());
                                                        userDao.update(saved);
                                                    }
                                                } catch (Exception e) {
                                                    // The local record remains available for the next retry.
                                                }
                                            });
                                        }

                                        @Override
                                        public void onError(Exception error) {
                                            // Registration is already complete; WorkManager will retry the pending record.
                                        }
                                    });
                                    callback.onSuccess(null);
                                } catch (Exception e) {
                                    callback.onError(e);
                                }
                            });
                        })
                        .addOnFailureListener(callback::onError);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void login(String email, String password, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                if (NetworkUtil.isNetworkAvailable(context)) {
                    auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                if (authResult.getUser() == null) {
                                    callback.onError(new Exception("No se pudo obtener el identificador del usuario"));
                                    return;
                                }

                                String uid = authResult.getUser().getUid();

                                executor.execute(() -> {
                                    try {
                                        UserEntity existing = userDao.findByEmail(email);
                                        if (existing != null) {
                                            existing.setUid(uid);
                                            existing.setPasswordSha256(PasswordUtil.sha256(password));
                                            if (existing.getSyncStatus() == null || existing.getSyncStatus().isEmpty()) {
                                                existing.setSyncStatus(SyncStatus.SYNCED.name());
                                            }
                                            userDao.update(existing);
                                            sessionManager.saveSession(uid, email, existing.getFullName());
                                        } else {
                                            UserEntity local = buildUserEntity(uid, email, email, password);
                                            local.setSyncStatus(SyncStatus.SYNCED.name());
                                            userDao.insert(local);
                                            sessionManager.saveSession(uid, email, email);
                                        }
                                        loadRemoteName(uid, email, callback);
                                    } catch (Exception e) {
                                        callback.onError(e);
                                    }
                                });
                            })
                            .addOnFailureListener(callback::onError);
                } else {
                    UserEntity local = userDao.findByEmail(email);
                    if (local != null && PasswordUtil.sha256(password).equals(local.getPasswordSha256())) {
                        String uid = local.getUid() != null && !local.getUid().isEmpty() ? local.getUid() : "offline";
                        sessionManager.saveSession(uid, email, local.getFullName());
                        callback.onSuccess(null);
                    } else {
                        callback.onError(new Exception("Credenciales invalidas o sin internet"));
                    }
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updateProfile(String newFullName, String currentUid, Callback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (currentUid == null || currentUid.isEmpty()) {
                    callback.onError(new Exception("Sesion expirada"));
                    return;
                }

                UserEntity local = userDao.findByUid(currentUid);
                if (local == null) {
                    callback.onError(new Exception("Usuario local no encontrado"));
                    return;
                }

                local.setFullName(newFullName);
                local.setUpdatedAt(System.currentTimeMillis());
                local.setSyncStatus(SyncStatus.PENDING_SYNC.name());
                userDao.update(local);
                sessionManager.saveSession(currentUid, local.getEmail(), newFullName);

                if (!NetworkUtil.isNetworkAvailable(context)) {
                    callback.onSuccess(false);
                    return;
                }

                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", newFullName);
                updates.put("updatedAt", System.currentTimeMillis());

                firestoreClient.usersRef(currentUid)
                        .document(PROFILE_DOC_ID)
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            try {
                                UserEntity fresh = userDao.findByUid(currentUid);
                                if (fresh != null) {
                                    fresh.setSyncStatus(SyncStatus.SYNCED.name());
                                    fresh.setUpdatedAt(System.currentTimeMillis());
                                    userDao.update(fresh);
                                }
                                sessionManager.saveLastSync(System.currentTimeMillis());
                                callback.onSuccess(true);
                            } catch (Exception e) {
                                callback.onSuccess(false);
                            }
                        }))
                        .addOnFailureListener(error -> callback.onSuccess(false));
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updatePassword(String newPassword, String currentUid, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                if (auth.getCurrentUser() == null) {
                    callback.onError(new Exception("Sesion expirada"));
                    return;
                }

                if (!NetworkUtil.isNetworkAvailable(context)) {
                    callback.onError(new Exception("Se requiere internet para cambiar la contrasena"));
                    return;
                }

                auth.getCurrentUser().updatePassword(newPassword)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            try {
                                UserEntity local = userDao.findByUid(currentUid);
                                if (local != null) {
                                    local.setPasswordSha256(PasswordUtil.sha256(newPassword));
                                    local.setUpdatedAt(System.currentTimeMillis());
                                    userDao.update(local);
                                }
                                sessionManager.saveLastSync(System.currentTimeMillis());
                                callback.onSuccess(null);
                            } catch (Exception e) {
                                callback.onError(e);
                            }
                        }))
                        .addOnFailureListener(callback::onError);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public int getPendingProfileCount() {
        return userDao.countPendingUsers();
    }

    private void loadRemoteName(String uid, String email, Callback<Void> callback) {
        firestoreClient.usersRef(uid).document(PROFILE_DOC_ID).get()
                .addOnSuccessListener(snapshot -> executor.execute(() -> {
                    try {
                        UserEntity local = userDao.findByUid(uid);
                        String fullName = snapshot.getString("fullName");
                        if (local != null && fullName != null && !fullName.trim().isEmpty()) {
                            local.setFullName(fullName.trim());
                            local.setSyncStatus(SyncStatus.SYNCED.name());
                            userDao.update(local);
                        }
                        sessionManager.saveSession(uid, email,
                                local != null ? local.getFullName() : email);
                        callback.onSuccess(null);
                    } catch (Exception error) {
                        callback.onError(error);
                    }
                }))
                .addOnFailureListener(error -> callback.onSuccess(null));
    }

    private UserEntity buildUserEntity(String uid, String email, String fullName, String password) {
        UserEntity entity = new UserEntity();
        entity.setUid(uid);
        entity.setEmail(email);
        entity.setFullName(fullName);
        entity.setPasswordSha256(PasswordUtil.sha256(password));
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setUpdatedAt(entity.getCreatedAt());
        entity.setSyncStatus(SyncStatus.PENDING_SYNC.name());
        return entity;
    }

    private void pushUserToFirestore(String uid, String email, String fullName, Callback<Void> callback) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("fullName", fullName);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("updatedAt", System.currentTimeMillis());

        firestoreClient.usersRef(uid)
                .document(PROFILE_DOC_ID)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onError);
    }
}