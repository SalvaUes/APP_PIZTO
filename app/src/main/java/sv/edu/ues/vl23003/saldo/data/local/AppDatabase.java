package sv.edu.ues.vl23003.saldo.data.local;
import android.content.Context; import androidx.room.*; import sv.edu.ues.vl23003.saldo.data.local.dao.*; import sv.edu.ues.vl23003.saldo.data.local.entity.*;
@Database(entities={UserEntity.class,MovementEntity.class},version=1,exportSchema=false)
public abstract class AppDatabase extends RoomDatabase { public abstract UserDao abstractUserDao(); public abstract MovementDao abstractMovementDao(); private static volatile AppDatabase instance; public static AppDatabase getInstance(Context context){ if(instance==null) synchronized(AppDatabase.class){if(instance==null) instance=Room.databaseBuilder(context.getApplicationContext(),AppDatabase.class,"pizto.db").fallbackToDestructiveMigration().build();} return instance; } }
