package sv.edu.ues.vl23003.saldo.util;
public interface Callback<T>{ void onSuccess(T value);
    void onError(Exception error);
    default void onFailure(Exception error){ onError(error); } }
