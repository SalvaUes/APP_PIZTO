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
import sv.edu.ues.vl23003.saldo.databinding.ActivityMapBinding;
import sv.edu.ues.vl23003.saldo.util.AppConstants;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Mapa del negocio");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_container);

        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }

        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng businessLocation = new LatLng(
                AppConstants.BUSINESS_LAT,
                AppConstants.BUSINESS_LNG
        );

        googleMap.addMarker(new MarkerOptions()
                .position(businessLocation)
                .title("PIZTO")
                .snippet("Punto de referencia del negocio"));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(businessLocation, 16f));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }
}