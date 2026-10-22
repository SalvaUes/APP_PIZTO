package sv.edu.ues.vl23003.saldo.ui.profile;
import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import sv.edu.ues.vl23003.saldo.databinding.ActivityProfileBinding;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;
import sv.edu.ues.vl23003.saldo.ui.auth.LoginActivity;
import sv.edu.ues.vl23003.saldo.util.*;
public class ProfileActivity extends AppCompatActivity {
 private ActivityProfileBinding b;
 protected void onCreate(Bundle state){super.onCreate(state);b=ActivityProfileBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
 b.btnSync.setOnClickListener(v->load(true));
 b.btnLogout.setOnClickListener(v->{new SessionManager(this).clearSession();FirebaseAuth.getInstance().signOut();startActivity(new Intent(this,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));finish();});load(true);}
 private void load(boolean retry){b.btnSync.setEnabled(false);b.syncStatus.setText("Comprobando sincronización…");new UserRepository(this).profile(new SessionManager(this).getSessionEmail(),retry,new Callback<UserEntity>(){
 public void onSuccess(UserEntity u){b.btnSync.setEnabled(true);b.profileData.setText(u.fullName+"\n"+u.email);b.syncStatus.setText("SYNCED".equals(u.syncStatus)?"Sincronizado con Firestore":"Actualización pendiente. Verifica conexión y vuelve a intentar.");}
 public void onError(Exception e){b.btnSync.setEnabled(true);b.syncStatus.setText("No se pudo consultar el perfil");Toast.makeText(ProfileActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();}
 });}
}