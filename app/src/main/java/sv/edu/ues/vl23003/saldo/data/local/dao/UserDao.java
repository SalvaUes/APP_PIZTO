package sv.edu.ues.vl23003.saldo.data.local.dao;

import androidx.room.*;
import sv.edu.ues.vl23003.saldo.data.local.entity.UserEntity;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1") UserEntity findByEmail(String email);
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") UserEntity getById(long id);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insertOnConflictReplace(UserEntity user);
    @Update void update(UserEntity user);
    @Query("SELECT COUNT(*) FROM users") int count();
}
