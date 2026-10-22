package sv.edu.ues.vl23003.saldo.data.repository;

import android.content.Context;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;
import sv.edu.ues.vl23003.saldo.util.Callback;

public class MovementRepository {
    private final AppDatabase database;
    private final ExecutorService executor;

    public MovementRepository(Context context) {
        database = AppDatabase.getInstance(context);
        executor = Executors.newSingleThreadExecutor();
    }

    public long create(MovementEntity movement) {
        movement.syncStatus = "PENDING_SYNC";
        movement.createdAt = System.currentTimeMillis();
        movement.updatedAt = movement.createdAt;
        return database.abstractMovementDao().insert(movement);
    }

    public long saveLocal(MovementEntity movement) {
        return create(movement);
    }

    public void update(MovementEntity movement) {
        movement.syncStatus = "PENDING_SYNC";
        movement.updatedAt = System.currentTimeMillis();
        database.abstractMovementDao().update(movement);
    }

    public void delete(MovementEntity movement) {
        database.abstractMovementDao().delete(movement);
    }

    public List<MovementEntity> getAll() {
        return database.abstractMovementDao().getAllByDateDesc();
    }

    public MovementEntity getById(long id) {
        return database.abstractMovementDao().getById(id);
    }

    public int getPendingCount() {
        return database.abstractMovementDao().countPending();
    }

    public void createMovement(MovementEntity movement, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                create(movement);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void updateMovement(MovementEntity movement, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                update(movement);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void deleteMovement(long id, Callback<Void> callback) {
        executor.execute(() -> {
            try {
                MovementEntity movement = getById(id);
                if (movement != null) {
                    delete(movement);
                    callback.onSuccess(null);
                } else {
                    callback.onError(new Exception("Movimiento no encontrado"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getAllMovements(Callback<List<MovementEntity>> callback) {
        executor.execute(() -> {
            try {
                List<MovementEntity> list = getAll();
                callback.onSuccess(list);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void getMovementById(long id, Callback<MovementEntity> callback) {
        executor.execute(() -> {
            try {
                MovementEntity movement = getById(id);
                if (movement != null) {
                    callback.onSuccess(movement);
                } else {
                    callback.onError(new Exception("Movimiento no encontrado"));
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}