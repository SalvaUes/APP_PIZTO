package sv.edu.ues.vl23003.saldo.ui.splash;
import android.app.Activity;import android.os.*;import android.content.Intent;
import sv.edu.ues.vl23003.saldo.R;import sv.edu.ues.vl23003.saldo.ui.auth.LoginActivity;
public class SplashActivity extends Activity {
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Runnable next=()->{startActivity(new Intent(this,LoginActivity.class));finish();};
 protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_splash);handler.postDelayed(next,1600);}
 protected void onDestroy(){handler.removeCallbacks(next);super.onDestroy();}
}