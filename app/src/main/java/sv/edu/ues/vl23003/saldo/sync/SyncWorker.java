package sv.edu.ues.vl23003.saldo.sync;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import sv.edu.ues.vl23003.saldo.data.repository.UserRepository;
public class SyncWorker extends Worker {
 public SyncWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
 @NonNull public Result doWork(){try{return new UserRepository(getApplicationContext()).syncCurrentBlocking()?Result.success():Result.retry();}catch(Exception e){return Result.retry();}}
}