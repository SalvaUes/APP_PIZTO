package sv.edu.ues.vl23003.saldo.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity findByEmail(String email);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getById(long id);

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    UserEntity findByUid(String uid);

    @Query("SELECT * FROM users WHERE syncStatus IS NOT NULL AND syncStatus != 'SYNCED'")
    List<UserEntity> getPendingUsers();

    @Query("SELECT COUNT(*) FROM users WHERE syncStatus IS NOT NULL AND syncStatus != 'SYNCED'")
    int countPendingUsers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT COUNT(*) FROM users")
    int count();
}