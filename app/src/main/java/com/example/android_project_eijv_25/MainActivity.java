package com.example.android_project_eijv_25;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private DrawerLayout drawerLayout;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double filterDistanceKm = -1; // -1 = pas de filtre
    private LatLng userLocation = new LatLng(49.894067, 2.295753); // Amiens par défaut

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initToolbar();
        initDrawer();
        initDistanceFilter();
        initMap();
        initFab();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void initToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));
    }

    // ── Drawer (menu hamburger) ───────────────────────────────────────────────

    private void initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);

        findViewById(R.id.btnCloseDrawer).setOnClickListener(v ->
                drawerLayout.closeDrawer(GravityCompat.START));

        findViewById(R.id.menuAccueil).setOnClickListener(v ->
                drawerLayout.closeDrawer(GravityCompat.START));

        findViewById(R.id.menuMesEvenements).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, ListEvenementActivity.class));
        });

        findViewById(R.id.menuAPropos).setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, AProposActivity.class));
        });

        findViewById(R.id.menuDeconnecter).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ── Filtre par distance ───────────────────────────────────────────────────

    private void initDistanceFilter() {
        String[] options = getResources().getStringArray(R.array.filter_distance_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, options);
        AutoCompleteTextView spinner = findViewById(R.id.spinnerDistance);
        spinner.setAdapter(adapter);
        spinner.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0: filterDistanceKm = -1;  break; // Tous
                case 1: filterDistanceKm = 5;   break; // 1-5 km
                case 2: filterDistanceKm = 10;  break; // 5-10 km
                case 3: filterDistanceKm = 15;  break; // 10-15 km
                case 4: filterDistanceKm = 30;  break; // 15-30 km
            }
            if (mMap != null) loadEventsOnMap();
        });
    }

    // ── Carte ─────────────────────────────────────────────────────────────────

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) throw new AssertionError();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));
        loadEventsOnMap();

        // Clic sur un marqueur → afficher détails
        mMap.setOnMarkerClickListener(marker -> {
            String eventId = (String) marker.getTag();
            if (eventId != null) {
                Intent intent = new Intent(this, DetailEvenementActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            }
            return false;
        });
    }

    private void loadEventsOnMap() {
        mMap.clear();
        db.collection("Evenements")
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String titre = doc.getString("titre");
                        if (lat == null || lng == null) continue;

                        LatLng pos = new LatLng(lat, lng);

                        // Filtrage par distance si actif
                        if (filterDistanceKm > 0) {
                            double dist = distanceKm(userLocation.latitude, userLocation.longitude, lat, lng);
                            if (dist > filterDistanceKm) continue;
                        }

                        MarkerOptions opts = new MarkerOptions()
                                .position(pos)
                                .title(titre != null ? titre : "")
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_RED));

                        var marker = mMap.addMarker(opts);
                        if (marker != null) marker.setTag(doc.getId());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_load_events), Toast.LENGTH_SHORT).show());
    }

    // ── FAB ajouter événement ─────────────────────────────────────────────────

    private void initFab() {
        findViewById(R.id.fabAddEvent).setOnClickListener(v -> {
            Intent intent = new Intent(this, ListEvenementActivity.class);
            intent.putExtra("openAddDialog", true);
            startActivity(intent);
        });
    }

    // ── Utilitaire : calcul distance (Haversine) ──────────────────────────────

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) loadEventsOnMap();
    }
}