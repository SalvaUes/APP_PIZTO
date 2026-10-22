package sv.edu.ues.vl23003.saldo.data.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
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

    public void register(String email, String fullName, String password, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                if (!NetworkUtil.isNetworkAvailable(context)) {
                    callback.onError(new Exception("Se requiere internet para el registro inicial"));
                    return;
                }
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(authResult -> {
                            String uid = authResult.getUser().getUid();
                            executor.execute(() -> {
                                try {
                                    UserEntity entity = buildUserEntity(uid, email, fullName, password);
                                    userDao.insert(entity);
                                    saveUserToFirestore(uid, email, fullName);
                                    sessionManager.saveSession(uid, email, fullName);
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
                                String uid = authResult.getUser().getUid();
                                executor.execute(() -> {
                                    try {
                                        UserEntity existing = userDao.findByEmail(email);
                                        if (existing != null) {
                                            existing.setUid(uid);
                                            existing.setSyncStatus(SyncStatus.SYNCED.name());
                                            userDao.update(existing);
                                            sessionManager.saveSession(uid, email, existing.getFullName());
                                        } else {
                                            sessionManager.saveSession(uid, email, email);
                                        }
                                        callback.onSuccess(null);
                                    } catch (Exception e) {
                                        callback.onError(e);
                                    }
                                });
                            })
                            .addOnFailureListener(callback::onError);
                } else {
                    UserEntity local = userDao.findByEmail(email);
                    if (local != null && PasswordUtil.sha256(password).equals(local.getPasswordSha256())) {
                        String uid = local.getUid() != null ? local.getUid() : "offline";
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

    public void updateProfile(String newFullName, String currentUid, Callback<Void> callback) {
        executor.execute(() -> {
            try {
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

                if (NetworkUtil.isNetworkAvailable(context)) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fullName", newFullName);
                    updates.put("updatedAt", System.currentTimeMillis());
                    firestoreClient.usersRef(currentUid)
                            .document(PROFILE_DOC_ID)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> executor.execute(() -> {
                                local.setSyncStatus(SyncStatus.SYNCED.name());
                                userDao.update(local);
                                callback.onSuccess(null);
                            }))
                            .addOnFailureListener(callback::onError);
                } else {
                    callback.onSuccess(null);
                }
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

    private UserEntity buildUserEntity(String uid, String email, String fullName, String password) {
        UserEntity entity = new UserEntity();
        entity.setUid(uid);
        entity.setEmail(email);
        entity.setFullName(fullName);
        entity.setPasswordSha256(PasswordUtil.sha256(password));
        entity.setCreatedAt(System.currentTimeMillis());
        entity.setUpdatedAt(entity.getCreatedAt());
        entity.setSyncStatus(SyncStatus.SYNCED.name());
        return entity;
    }

    private void saveUserToFirestore(String uid, String email, String fullName) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("fullName", fullName);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("updatedAt", System.currentTimeMillis());
        firestoreClient.usersRef(uid).document(PROFILE_DOC_ID).set(userData);
    }
}