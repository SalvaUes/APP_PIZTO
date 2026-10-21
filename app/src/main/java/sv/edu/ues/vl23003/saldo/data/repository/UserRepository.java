package sv.edu.ues.vl23003.saldo.data.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;
import sv.edu.ues.vl23003.saldo.data.remote.FirestoreClient;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
import sv.edu.ues.vl23003.saldo.util.PasswordUtil;

public class UserRepository {
    private final AppDatabase database;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    public UserRepository(Context context) { database = AppDatabase.getInstance(context); }
    public void register(String email, String fullName, String password, Callback<String> callback) {
        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(result -> {
            String uid = result.getUser() == null ? "" : result.getUser().getUid();
            HashMap<String, Object> data = new HashMap<>();
            data.put("email", email); data.put("fullName", fullName); data.put("createdAt", System.currentTimeMillis());
            FirestoreClient.getInstance().database().collection(FirestoreClient.COLLECTION_USERS).document(uid).set(data)
                    .addOnSuccessListener(v -> { saveLocal(uid, email, fullName, password); callback.onSuccess(uid); })
                    .addOnFailureListener(callback::onFailure);
        }).addOnFailureListener(callback::onFailure);
    }
    public void login(String email, String password, boolean isNetworkAvailable, Callback<String> callback) {
        if (!isNetworkAvailable) {
            UserEntity user = database.abstractUserDao().findByEmail(email);
            if (user != null && PasswordUtil.sha256(password).equals(user.passwordSha256)) callback.onSuccess(user.uid);
            else callback.onFailure(new IllegalStateException("Credenciales locales inválidas."));
            return;
        }
        auth.signInWithEmailAndPassword(email, password).addOnSuccessListener(result -> {
            String uid = result.getUser() == null ? "" : result.getUser().getUid();
            UserEntity user = database.abstractUserDao().findByEmail(email);
            if (user == null) saveLocal(uid, email, "", password);
            callback.onSuccess(uid);
        }).addOnFailureListener(callback::onFailure);
    }
    private void saveLocal(String uid, String email, String fullName, String password) {
        UserEntity user = database.abstractUserDao().findByEmail(email);
        if (user == null) user = new UserEntity();
        user.uid = uid; user.email = email; user.fullName = fullName;
        user.passwordSha256 = PasswordUtil.sha256(password);
        if (user.createdAt == 0) user.createdAt = System.currentTimeMillis();
        user.updatedAt = System.currentTimeMillis(); user.syncStatus = "SYNCED";
        database.abstractUserDao().insertOnConflictReplace(user);
    }
}
