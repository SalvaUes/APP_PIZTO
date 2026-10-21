package sv.edu.ues.vl23003.saldo.data.remote;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public final class FirestoreClient {
    public static final String COLLECTION_USERS = "users";
    public static final String SUBCOLLECTION_MOVEMENTS = "movements";
    private static final FirestoreClient INSTANCE = new FirestoreClient();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private FirestoreClient() {}
    public static FirestoreClient getInstance() { return INSTANCE; }
    public CollectionReference usersRef(String uid) { return firestore.collection(COLLECTION_USERS).document(uid).collection("profile"); }
    public CollectionReference movementsRef(String uid) { return firestore.collection(COLLECTION_USERS).document(uid).collection(SUBCOLLECTION_MOVEMENTS); }
    public FirebaseFirestore database() { return firestore; }
}
