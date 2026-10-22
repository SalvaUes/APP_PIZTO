package sv.edu.ues.vl23003.saldo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
import sv.edu.ues.vl23003.saldo.databinding.ActivityLoginBinding;
import sv.edu.ues.vl23003.saldo.ui.home.HomeActivity;
import sv.edu.ues.vl23003.saldo.util.Callback;
import sv.edu.ues.vl23003.saldo.util.SessionManager;
import sv.edu.ues.vl23003.saldo.util.Validators;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private SessionManager session;
    private UserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        repository = new UserRepository(this);

        if (session.hasSavedCredentials()) {
            binding.email.setText(session.getSavedEmail());
            binding.password.setText(session.getSavedPassword());
            binding.remember.setChecked(true);
        }

        binding.btnLogin.setOnClickListener(v -> performLogin());
        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private boolean validateInputs() {
        boolean valid = true;
        String email = getText(binding.email);
        String password = getText(binding.password);

        if (!Validators.isValidEmail(email)) {
            binding.email.setError("Ingresa un correo valido");
            valid = false;
        }

        if (!Validators.isValidPassword(password)) {
            binding.password.setError("Usa al menos 8 caracteres con letras y numeros sin simbolos");
            valid = false;
        }

        return valid;
    }

    private void performLogin() {
        if (!validateInputs()) {
            return;
        }

        final String email = getText(binding.email).trim();
        final String password = getText(binding.password);

        binding.btnLogin.setEnabled(false);

        repository.login(email, password, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                LoginActivity.this.runOnUiThread(() -> {
                    if (binding.remember.isChecked()) {
                        session.saveCredentials(email, password);
                    } else {
                        session.clearCredentials();
                    }
                    binding.btnLogin.setEnabled(true);
                    navigateToHome();
                });
            }

            @Override
            public void onError(Exception error) {
                LoginActivity.this.runOnUiThread(() -> {
                    binding.btnLogin.setEnabled(true);
                    String message = error.getMessage() != null ? error.getMessage() : "No se pudo iniciar sesion";
                    binding.password.setError(message);
                });
            }
        });
    }

    private String getText(android.widget.EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}