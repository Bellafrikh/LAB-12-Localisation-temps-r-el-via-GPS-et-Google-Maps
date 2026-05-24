package com.example.localisation_lab12;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private RequestQueue requestQueue;
    private final String showUrl = "http://192.168.43.228/localisation/showPositions.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialisation de la file d'attente Volley
        requestQueue = Volley.newRequestQueue(getApplicationContext());

        // Récupération asynchrone du fragment de la carte
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setUpMap();
    }

    private void setUpMap() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                showUrl,
                null,
                response -> {
                    try {
                        // Extraction du tableau JSON "positions" renvoyé par le script PHP
                        JSONArray positions = response.getJSONArray("positions");

                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject position = positions.getJSONObject(i);
                            double lat = position.getDouble("latitude");
                            double lon = position.getDouble("longitude");

                            // Ajout du repère visuel sur la carte
                            LatLng location = new LatLng(lat, lon);
                            mMap.addMarker(new MarkerOptions()
                                    .position(location)
                                    .title("Position enregistrée n°" + (i + 1)));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MapsActivity.this, "Erreur de décodage des données du serveur.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Logs précieux pour identifier une déconnexion réseau ou un crash de WampServer/XAMPP
                    Log.e("Volley_Error", "Détails : " + error.toString());
                    Toast.makeText(MapsActivity.this, "Impossible de contacter le serveur distant.", Toast.LENGTH_LONG).show();
                }
        );

        // Envoi effectif de la requête
        requestQueue.add(jsonObjectRequest);
    }
}