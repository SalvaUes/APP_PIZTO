package sv.edu.ues.vl23003.saldo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityLoginBinding;
import sv.edu.ues.vl23003.saldo.ui.home.HomeActivity;
import sv.edu.ues.vl23003.saldo.util.NetworkUtil;
import sv.edu.ues.vl23003.saldo.util.SessionManager;
import sv.edu.ues.vl23003.saldo.util.Validators;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private SessionManager session;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); binding = ActivityLoginBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        session = new SessionManager(this);
        if (session.hasSavedCredentials()) { binding.email.setText(session.getSavedEmail()); binding.password.setText(session.getSavedPassword()); binding.remember.setChecked(true); binding.remember.setChecked(true); }
        binding.btnLogin.setOnClickListener(v -> performLogin());
        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
    private boolean validateInputs() {
 boolean valid=true;
 if(!Validators.isValidEmail(binding.email.getText().toString())) { binding.email.setError("Ingresa un correo válido"); valid=false; }
 if(!Validators.isValidPassword(binding.password.getText().toString())) { binding.password.setError("Usa al menos 8 caracteres, con letras y números, sin símbolos"); valid=false; }
 return valid;
 }
    private void performLogin() {
        if (!validateInputs()) return;
        String email = binding.email.getText().toString().trim(); String password = binding.password.getText().toString();
        binding.btnLogin.setEnabled(false);
        new UserRepository(this).login(email, password, NetworkUtil.isNetworkAvailable(this), new sv.edu.ues.vl23003.saldo.util.Callback<String>() {
            public void onSuccess(String uid) { session.saveSession(uid, email, ""); if(binding.remember.isChecked()) session.saveCredentials(email,password); else session.clearCredentials(); binding.btnLogin.setEnabled(true); navigateToHome(); }
            public void onError(Exception error) { binding.btnLogin.setEnabled(true); binding.password.setError(error.getMessage()); }
        });
    }
    private void navigateToHome() { startActivity(new Intent(this, HomeActivity.class)); finish(); }
}
