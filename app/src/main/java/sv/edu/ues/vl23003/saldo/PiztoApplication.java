package sv.edu.ues.vl23003.saldo;
import android.app.Application; import sv.edu.ues.vl23003.saldo.sync.SyncManager;
public class PiztoApplication extends Application { @Override public void onCreate(){ super.onCreate(); SyncManager.schedule(this); } }
