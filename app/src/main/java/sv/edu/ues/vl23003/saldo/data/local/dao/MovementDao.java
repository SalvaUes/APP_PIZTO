package sv.edu.ues.vl23003.saldo.data.local.dao;

import androidx.room.*;
import java.util.List;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;

@Dao
public interface MovementDao {
    @Query("SELECT * FROM movements ORDER BY dueDate DESC") List<MovementEntity> getAllByDateDesc();
    @Query("SELECT * FROM movements WHERE syncStatus != 'SYNCED'") List<MovementEntity> getPendingSync();
    @Query("SELECT * FROM movements WHERE id = :id LIMIT 1") MovementEntity getById(long id);
    @Insert long insert(MovementEntity movement);
    @Update void update(MovementEntity movement);
    @Delete void delete(MovementEntity movement);
    @Query("UPDATE movements SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :id")
    void updateSyncStatus(long id, String syncStatus, long updatedAt);
    @Query("SELECT COUNT(*) FROM movements WHERE syncStatus = 'PENDING_SYNC'") int countPending();
}
