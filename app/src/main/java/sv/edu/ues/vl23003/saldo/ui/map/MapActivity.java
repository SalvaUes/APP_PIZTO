package sv.edu.ues.vl23003.saldo.ui.map;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import sv.edu.ues.vl23003.saldo.R;
import sv.edu.ues.vl23003.saldo.util.AppConstants;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // carga el mapa desde el fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        // punto del negocio
        LatLng businessLocation = new LatLng(
                AppConstants.BUSINESS_LAT,
                AppConstants.BUSINESS_LNG
        );

        // marcador principal
        googleMap.addMarker(new MarkerOptions()
                .position(businessLocation)
                .title("PIZTO")
                .snippet("Punto de referencia del negocio"));

        // centra la camara en el negocio
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(businessLocation, 15f));
    }
}