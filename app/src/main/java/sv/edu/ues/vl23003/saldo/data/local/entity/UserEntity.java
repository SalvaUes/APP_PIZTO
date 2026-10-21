package sv.edu.ues.vl23003.saldo.data.local.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = {"email"}, unique = true)})
public class UserEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String uid;
    public String email;
    public String fullName;
    public String passwordSha256;
    public long createdAt;
    public long updatedAt;
    public String syncStatus;
}
