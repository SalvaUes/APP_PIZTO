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
        if (session.hasSavedCredentials()) { binding.email.setText(session.getSavedEmail()); binding.password.setText(session.getSavedPassword()); }
        binding.btnLogin.setOnClickListener(v -> performLogin());
        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
    private boolean validateInputs() { return Validators.isValidEmail(binding.email.getText().toString()) && Validators.isValidPassword(binding.password.getText().toString()); }
    private void performLogin() {
        if (!validateInputs()) { binding.email.setError("Verifica correo y contraseña"); return; }
        String email = binding.email.getText().toString().trim(); String password = binding.password.getText().toString();
        new UserRepository(this).login(email, password, NetworkUtil.isNetworkAvailable(this), new sv.edu.ues.vl23003.saldo.util.Callback<String>() {
            public void onSuccess(String uid) { session.saveSession(uid, email, ""); navigateToHome(); }
            public void onError(Exception error) { binding.password.setError(error.getMessage()); }
        });
    }
    private void navigateToHome() { startActivity(new Intent(this, HomeActivity.class)); finish(); }
}
