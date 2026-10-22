package sv.edu.ues.vl23003.saldo.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import sv.edu.ues.vl23003.saldo.data.local.entity.MovementEntity;

@Dao
public interface MovementDao {

    @Query("SELECT * FROM movements ORDER BY dueDate DESC, createdAt DESC")
    List<MovementEntity> getAllByDateDesc();

    @Query("SELECT * FROM movements WHERE id = :id LIMIT 1")
    MovementEntity getById(long id);

    @Query("SELECT * FROM movements WHERE syncStatus IS NOT NULL AND syncStatus != 'SYNCED' ORDER BY dueDate DESC, createdAt DESC")
    List<MovementEntity> getPendingSync();

    @Query("SELECT COUNT(*) FROM movements WHERE syncStatus IS NOT NULL AND syncStatus != 'SYNCED'")
    int countPending();

    @Insert
    long insert(MovementEntity movement);

    @Update
    void update(MovementEntity movement);

    @Delete
    void delete(MovementEntity movement);
}