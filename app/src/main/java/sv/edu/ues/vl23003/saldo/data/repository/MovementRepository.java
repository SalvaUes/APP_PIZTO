package sv.edu.ues.vl23003.saldo.data.repository;

import android.content.Context;
import java.util.List;
import sv.edu.ues.vl23003.saldo.data.local.AppDatabase;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;

public class MovementRepository {
    private final AppDatabase database;
    public MovementRepository(Context context) { database = AppDatabase.getInstance(context); }
    public long create(MovementEntity movement) { movement.syncStatus = "PENDING_SYNC"; movement.createdAt = System.currentTimeMillis(); movement.updatedAt = movement.createdAt; return database.abstractMovementDao().insert(movement); }
    public long saveLocal(MovementEntity movement) { return create(movement); }
    public void update(MovementEntity movement) { movement.syncStatus = "PENDING_SYNC"; movement.updatedAt = System.currentTimeMillis(); database.abstractMovementDao().update(movement); }
    public void delete(MovementEntity movement) { database.abstractMovementDao().delete(movement); }
    public List<MovementEntity> getAll() { return database.abstractMovementDao().getAllByDateDesc(); }
    public MovementEntity getById(long id) { return database.abstractMovementDao().getById(id); }
    public int getPendingCount() { return database.abstractMovementDao().countPending(); }
}
