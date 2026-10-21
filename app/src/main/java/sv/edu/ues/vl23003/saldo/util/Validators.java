package sv.edu.ues.vl23003.saldo.util;

import android.util.Patterns;
import java.util.regex.Pattern;

public final class Validators {
    private Validators() {}
    public static boolean isValidEmail(String value) { return value != null && Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches(); }
    public static boolean isValidPassword(String value) { return value != null && Pattern.matches("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z0-9]{8,}$", value); }
    public static boolean isValidFullName(String value) { return value != null && value.trim().length() >= 7; }
    public static boolean passwordsMatch(String a, String b) { return a != null && a.equals(b); }
}
