package com.example.localisation_lab12;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private TextView textLatitude;
    private TextView textLongitude;

    // Logic Components
    private RequestQueue requestQueue;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUI();
        setupServices();
        checkPermissionsAndStartUpdates();
    }

    private void initializeUI() {
        textLatitude = findViewById(R.id.text_latitude);
        textLongitude = findViewById(R.id.text_longitude);
        Button buttonShowMap = findViewById(R.id.button_show_map);

        // Action de clic pour basculer vers MapsActivity
        buttonShowMap.setOnClickListener(v -> {
            Toast.makeText(this, "Ouverture de la carte...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(intent);
        });
    }

    private void setupServices() {
        requestQueue = Volley.newRequestQueue(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void checkPermissionsAndStartUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (locationManager != null) {
            // Mise à jour toutes les 60 secondes ou tous les 150 mètres
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    60000,
                    150,
                    locationListener
            );
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateUI(location);
            sendLocationToServer(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String statusName = "Inconnu";
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE: statusName = "Hors service"; break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE: statusName = "Temporairement indisponible"; break;
                case LocationProvider.AVAILABLE: statusName = "Disponible"; break;
            }
            Log.d(TAG, "Le fournisseur " + provider + " a changé de statut : " + statusName);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Toast.makeText(MainActivity.this, "Fournisseur activé : " + provider, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Toast.makeText(MainActivity.this, "Fournisseur désactivé : " + provider, Toast.LENGTH_SHORT).show();
        }
    };

    private void updateUI(Location location) {
        textLatitude.setText(String.format(Locale.getDefault(), "Latitude : %f", location.getLatitude()));
        textLongitude.setText(String.format(Locale.getDefault(), "Longitude : %f", location.getLongitude()));

        String detailMsg = String.format(Locale.FRANCE, "Nouvelle position :\nLat: %f, Lon: %f\nAlt: %.1fm, Précision: %.1fm",
                location.getLatitude(), location.getLongitude(),
                location.getAltitude(), location.getAccuracy());

        showToast(detailMsg);
    }

    private void sendLocationToServer(final Location location) {
        final String apiUrl = "http://192.168.43.228/localisation/createPosition.php";

        StringRequest request = new StringRequest(
                Request.Method.POST,
                apiUrl,
                response -> Log.d(TAG, "Réponse serveur: " + response),
                error -> Log.e(TAG, "Erreur réseau: " + error.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                params.put("latitude", String.valueOf(location.getLatitude()));
                params.put("longitude", String.valueOf(location.getLongitude()));
                params.put("date", dateFormat.format(new Date()));
                params.put("imei", getDeviceIdentifier());

                return params;
            }
        };

        requestQueue.add(request);
    }

    @SuppressLint("HardwareIds")
    private String getDeviceIdentifier() {
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return (id != null) ? id : "UNKNOWN_DEVICE";
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                showToast("Permission de localisation refusée.");
            }
        }
    }
}