package sv.edu.ues.vl23003.saldo.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "movements")
public class MovementEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String remoteId;
    public String type;
    public double amount;
    public String concept;
    public String category;
    public String contactName;
    public String paymentMethod;
    public String note;
    public String photoPath;
    public long dueDate;
    public String status;
    public String syncStatus;
    public long createdAt;
    public long updatedAt;
}
