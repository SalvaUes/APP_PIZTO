package sv.edu.ues.vl23003.saldo.data.repository;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Tasks;
import java.util.HashMap;
import java.util.concurrent.*;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;
import sv.edu.ues.vl23003.saldo.data.remote.FirestoreClient;
import sv.edu.ues.vl23003.saldo.util.*;
public class UserRepository {
 private static final ExecutorService IO=Executors.newSingleThreadExecutor();
 private final AppDatabase db;
 private final FirebaseAuth auth=FirebaseAuth.getInstance();
 private final Handler main=new Handler(Looper.getMainLooper());
 public UserRepository(Context context){db=AppDatabase.getInstance(context);}
 private <T> void success(Callback<T> c,T value){main.post(()->c.onSuccess(value));}
 private void error(Callback<?> c,Exception e){main.post(()->c.onError(e));}
 public void register(String email,String name,String password,Callback<String> c){
 auth.createUserWithEmailAndPassword(email.trim(),password).addOnSuccessListener(result->{
 String uid=result.getUser().getUid();
 IO.execute(()->{try{
 UserEntity u=saveLocal(uid,email.trim(),name,password,"PENDING_SYNC");
 success(c,uid); syncUser(u);
 }catch(Exception e){error(c,e);}});
 }).addOnFailureListener(e->error(c,e));
 }
 public void login(String email,String password,boolean online,Callback<String> c){
 if(!online){IO.execute(()->{try{
 UserEntity u=db.abstractUserDao().findByEmail(email);
 if(u==null || !PasswordUtil.sha256(password).equals(u.passwordSha256))throw new IllegalStateException("Credenciales locales inválidas. Inicia sesión con conexión al menos una vez.");
 success(c,u.uid);
 }catch(Exception e){error(c,e);}});return;}
 auth.signInWithEmailAndPassword(email,password).addOnSuccessListener(result->IO.execute(()->{try{
 String uid=result.getUser().getUid(); UserEntity u=db.abstractUserDao().findByEmail(email);
 if(u==null){
 String name="";
 try {String remoteName=Tasks.await(FirestoreClient.getInstance().database().collection("users").document(uid).get(),10,TimeUnit.SECONDS).getString("fullName");if(remoteName!=null)name=remoteName;}catch(Exception ignored){}
 u=saveLocal(uid,email,name,password,name.isEmpty()?"PENDING_SYNC":"SYNCED");
 } else {u.uid=uid;u.passwordSha256=PasswordUtil.sha256(password);db.abstractUserDao().update(u);}
 success(c,uid);
 }catch(Exception e){error(c,e);}})).addOnFailureListener(e->error(c,e));
 }
 private UserEntity saveLocal(String uid,String email,String name,String password,String status){
 UserEntity u=db.abstractUserDao().findByEmail(email);if(u==null)u=new UserEntity();
 u.uid=uid;u.email=email;u.fullName=name;u.passwordSha256=PasswordUtil.sha256(password);
 if(u.createdAt==0)u.createdAt=System.currentTimeMillis();u.updatedAt=System.currentTimeMillis();u.syncStatus=status;
 u.id=db.abstractUserDao().insertOnConflictReplace(u);return u;
 }
 private boolean syncUser(UserEntity u){
 if(auth.getCurrentUser()==null || !auth.getCurrentUser().getUid().equals(u.uid))return false;
 try{
 HashMap<String,Object> data=new HashMap<>();data.put("email",u.email);data.put("fullName",u.fullName);data.put("createdAt",u.createdAt);data.put("updatedAt",u.updatedAt);
 Tasks.await(FirestoreClient.getInstance().database().collection("users").document(u.uid).set(data,com.google.firebase.firestore.SetOptions.merge()),15,TimeUnit.SECONDS);
 u.syncStatus="SYNCED";db.abstractUserDao().update(u);return true;
 }catch(Exception e){u.syncStatus="PENDING_SYNC";db.abstractUserDao().update(u);return false;}
 }
 public void profile(String email,boolean retry,Callback<UserEntity> c){IO.execute(()->{try{
 UserEntity u=db.abstractUserDao().findByEmail(email);if(u==null)throw new IllegalStateException("No existe perfil local");
 if(retry && !"SYNCED".equals(u.syncStatus))syncUser(u);success(c,u);
 }catch(Exception e){error(c,e);}});}
 public boolean syncCurrentBlocking(){if(auth.getCurrentUser()==null)return true;UserEntity u=db.abstractUserDao().findByEmail(auth.getCurrentUser().getEmail());return u==null||"SYNCED".equals(u.syncStatus)||syncUser(u);}
}