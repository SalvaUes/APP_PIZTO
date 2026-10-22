package sv.edu.ues.vl23003.saldo.ui.splash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.ui.auth.LoginActivity;
import sv.edu.ues.vl23003.saldo.ui.home.HomeActivity;
import sv.edu.ues.vl23003.saldo.util.SessionManager;

public class SplashActivity extends Activity {

 private final Handler handler = new Handler(Looper.getMainLooper());
 private Runnable next;

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_splash);

  final SessionManager session = new SessionManager(this);

  next = () -> {
   Intent intent;
   if (session.isLoggedIn()) {
    intent = new Intent(this, HomeActivity.class);
   } else {
    intent = new Intent(this, LoginActivity.class);
   }
   startActivity(intent);
   finish();
  };

  handler.postDelayed(next, 1600);
 }

 @Override
 protected void onDestroy() {
  if (next != null) {
   handler.removeCallbacks(next);
  }
  super.onDestroy();
 }
}