package sv.edu.ues.vl23003.saldo.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private final SharedPreferences preferences;
    public SessionManager(Context context) { preferences = context.getSharedPreferences(AppConstants.PREFS, Context.MODE_PRIVATE); }
    public void saveCredentials(String email, String password) { preferences.edit().putString("saved_email", email).putString("saved_password", password).apply(); }
    public String getSavedEmail() { return preferences.getString("saved_email", ""); }
    public String getSavedPassword() { return preferences.getString("saved_password", ""); }
    public boolean hasSavedCredentials() { return !getSavedEmail().isEmpty(); }
    public void clearCredentials() { preferences.edit().remove("saved_email").remove("saved_password").apply(); }
    public void saveSession(String uid, String email, String fullName) { preferences.edit().putString("uid", uid).putString("email", email).putString("full_name", fullName).apply(); }
    public boolean isLoggedIn() { return !preferences.getString("uid", "").isEmpty(); }
    public String getSessionEmail() { return preferences.getString("email", ""); }
    public String getSessionFullName() { return preferences.getString("full_name", ""); }
    public void clearSession() { preferences.edit().remove("uid").remove("email").remove("full_name").apply(); }
}
