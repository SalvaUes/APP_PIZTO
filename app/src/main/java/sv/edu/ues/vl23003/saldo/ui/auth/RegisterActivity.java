package sv.edu.ues.vl23003.saldo.ui.auth;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.ues.vl23003.saldo.databinding.ActivityRegisterBinding;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
import sv.edu.ues.vl23003.saldo.util.*;
public class RegisterActivity extends AppCompatActivity {
 private ActivityRegisterBinding b;
 @Override protected void onCreate(Bundle state){super.onCreate(state);b=ActivityRegisterBinding.inflate(getLayoutInflater());setContentView(b.getRoot());b.btnLogin.setOnClickListener(v->finish());b.btnRegister.setOnClickListener(v->register());}
 private void register(){
 String name=b.fullName.getText().toString().trim(),email=b.email.getText().toString().trim(),password=b.password.getText().toString(); boolean valid=true;
 if(!Validators.isValidFullName(name)){b.fullName.setError("Ingresa al menos 7 caracteres");valid=false;}
 if(!Validators.isValidEmail(email)){b.email.setError("Ingresa un correo válido");valid=false;}
 if(!Validators.isValidPassword(password)){b.password.setError("Mínimo 8 caracteres: letras y números, sin símbolos");valid=false;}
 if(!Validators.passwordsMatch(password,b.confirmPassword.getText().toString())){b.confirmPassword.setError("Las contraseñas no coinciden");valid=false;}
 if(!valid)return;
 b.btnRegister.setEnabled(false);
 new UserRepository(this).register(email,name,password,new Callback<String>(){
 public void onSuccess(String uid){Toast.makeText(RegisterActivity.this,"Cuenta creada. Inicia sesión; revisa la sincronización en Perfil.",Toast.LENGTH_LONG).show();finish();}
 public void onError(Exception e){b.btnRegister.setEnabled(true);Toast.makeText(RegisterActivity.this,"No se pudo registrar: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 });
 }
}