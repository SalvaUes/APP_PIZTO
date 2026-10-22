package sv.edu.ues.vl23003.saldo.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import sv.edu.ues.vl23003.saldo.util.Callback;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncManager manager = new SyncManager(getApplicationContext());
        final boolean[] finished = {false};
        final boolean[] success = {true};

        manager.pushPendingMovements(new Callback<Integer>() {
            @Override
            public void onSuccess(Integer value) {
                finished[0] = true;
                success[0] = true;
            }

            @Override
            public void onError(Exception error) {
                finished[0] = true;
                success[0] = false;
            }
        });

        long timeout = 30000;
        long elapsed = 0;
        while (!finished[0] && elapsed < timeout) {
            try {
                Thread.sleep(100);
                elapsed += 100;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Result.retry();
            }
        }

        if (!finished[0]) {
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.retry();
    }
}