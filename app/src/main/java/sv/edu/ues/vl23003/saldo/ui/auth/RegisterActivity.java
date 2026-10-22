package sv.edu.ues.vl23003.saldo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityRegisterBinding;
import sv.edu.ues.vl23003.saldo.ui.home.HomeActivity;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.Validators;

public class RegisterActivity extends AppCompatActivity {

 private ActivityRegisterBinding binding;
 private UserRepository repository;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  binding = ActivityRegisterBinding.inflate(getLayoutInflater());
  setContentView(binding.getRoot());

  repository = new UserRepository(this);

  binding.btnLogin.setOnClickListener(v -> finish());
  binding.btnRegister.setOnClickListener(v -> register());
 }

 private void register() {
  final String name = getText(binding.fullName).trim();
  final String email = getText(binding.email).trim();
  final String password = getText(binding.password);
  final String confirmPassword = getText(binding.confirmPassword);

  boolean valid = true;

  if (!Validators.isValidFullName(name)) {
   binding.fullName.setError("Ingresa al menos 7 caracteres");
   valid = false;
  }

  if (!Validators.isValidEmail(email)) {
   binding.email.setError("Ingresa un correo valido");
   valid = false;
  }

  if (!Validators.isValidPassword(password)) {
   binding.password.setError("Minimo 8 caracteres con letras y numeros sin simbolos");
   valid = false;
  }

  if (!Validators.passwordsMatch(password, confirmPassword)) {
   binding.confirmPassword.setError("Las contrasenas no coinciden");
   valid = false;
  }

  if (!valid) {
   return;
  }

  binding.btnRegister.setEnabled(false);

  repository.register(email, name, password, new Callback<Void>() {
   @Override
   public void onSuccess(Void value) {
    RegisterActivity.this.runOnUiThread(() -> {
     Toast.makeText(RegisterActivity.this, "Cuenta creada", Toast.LENGTH_SHORT).show();
     navigateToHome();
    });
   }

   @Override
   public void onError(Exception error) {
    RegisterActivity.this.runOnUiThread(() -> {
     binding.btnRegister.setEnabled(true);
     String message = error.getMessage() != null ? error.getMessage() : "No se pudo registrar";
     Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
    });
   }
  });
 }

 private String getText(android.widget.EditText editText) {
  return editText.getText() != null ? editText.getText().toString() : "";
 }

 private void navigateToHome() {
  Intent intent = new Intent(this, HomeActivity.class);
  intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
  startActivity(intent);
  finish();
 }
}