package com.example.android_project_eijv_25;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_MANUAL_LOCATION     = 1002;

    private DrawerLayout drawerLayout;
    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private double filterDistanceKm = -1;
    private LatLng userLocation = null;
    private Marker userMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initToolbar();
        initDrawer();
        initDistanceFilter();
        initMap();
        initFab();
        requestLocationPermission();
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────

    private void initToolbar() {
        findViewById(R.id.btnMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));
    }

    // ── Drawer ────────────────────────────────────────────────────────────────

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
                case 0: filterDistanceKm = -1;  break;
                case 1: filterDistanceKm = 5;   break;
                case 2: filterDistanceKm = 10;  break;
                case 3: filterDistanceKm = 15;  break;
                case 4: filterDistanceKm = 30;  break;
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

        // France par défaut en attendant GPS
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(46.603354, 1.888334), 6));

        loadEventsOnMap();

        mMap.setOnMarkerClickListener(marker -> {
            if ("user_position".equals(marker.getTag())) {
                // Clic marqueur bleu → dialog adresse + options
                showUserMarkerOptions();
                return true;
            }
            // Clic événement rouge → détails
            String eventId = (String) marker.getTag();
            if (eventId != null) {
                Intent intent = new Intent(this, DetailEvenementActivity.class);
                intent.putExtra("eventId", eventId);
                startActivity(intent);
            }
            return false;
        });
    }

    // ── Marqueur bleu utilisateur ─────────────────────────────────────────────

    private void placeUserMarker() {
        if (mMap == null || userLocation == null) return;
        if (userMarker != null) userMarker.remove();

        // Pas de titre → dialog s'ouvre directement au clic
        userMarker = mMap.addMarker(new MarkerOptions()
                .position(userLocation)
                .icon(BitmapDescriptorFactory.defaultMarker(
                        BitmapDescriptorFactory.HUE_AZURE)));

        if (userMarker != null) {
            userMarker.setTag("user_position");
        }
    }

    // ── Dialog options marqueur bleu ──────────────────────────────────────────

    private void showUserMarkerOptions() {
        String adresse = userLocation != null
                ? geocodeAddress(userLocation.latitude, userLocation.longitude)
                : "Position inconnue";

        new AlertDialog.Builder(this)
                .setTitle("📍 Ma position")
                .setMessage(adresse)
                .setPositiveButton("🗺️ Choisir manuellement", (dialog, which) -> {
                    Intent intent = new Intent(this, SelectLocationActivity.class);
                    startActivityForResult(intent, REQUEST_MANUAL_LOCATION);
                })
                .setNeutralButton("📡 Relocaliser GPS", (dialog, which) ->
                        getUserLocation())
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ── Geocoding : coordonnées → adresse lisible ─────────────────────────────

    private String geocodeAddress(double lat, double lng) {
        if (!Geocoder.isPresent())
            return String.format(Locale.US, "%.5f, %.5f", lat, lng);
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException ignored) {}
        return String.format(Locale.US, "%.5f, %.5f", lat, lng);
    }

    // ── Chargement événements ─────────────────────────────────────────────────

    private void loadEventsOnMap() {
        mMap.clear();
        if (userLocation != null) placeUserMarker();

        db.collection("Evenements")
                .get()
                .addOnSuccessListener(snapshots -> {
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String titre = doc.getString("titre");
                        if (lat == null || lng == null) continue;

                        LatLng pos = new LatLng(lat, lng);

                        if (filterDistanceKm > 0 && userLocation != null) {
                            double dist = distanceKm(
                                    userLocation.latitude, userLocation.longitude, lat, lng);
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
                        Toast.makeText(this, getString(R.string.error_load_events),
                                Toast.LENGTH_SHORT).show());
    }

    // ── FAB ───────────────────────────────────────────────────────────────────

    private void initFab() {
        findViewById(R.id.fabAddEvent).setOnClickListener(v -> {
            Intent intent = new Intent(this, ListEvenementActivity.class);
            intent.putExtra("openAddDialog", true);
            startActivity(intent);
        });
    }

    // ── GPS étape 1 : demander permission ────────────────────────────────────

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission GPS")
                    .setMessage("La localisation permet de filtrer les événements près de vous.")
                    .setPositiveButton("Oui, activer", (dialog, which) ->
                            ActivityCompat.requestPermissions(this,
                                    new String[]{
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                    },
                                    REQUEST_LOCATION_PERMISSION))
                    .setNegativeButton("Non, choisir manuellement", (dialog, which) -> {
                        Intent intent = new Intent(this, SelectLocationActivity.class);
                        startActivityForResult(intent, REQUEST_MANUAL_LOCATION);
                    })
                    .show();
            return;
        }

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_LOCATION_PERMISSION);
    }

    // ── GPS étape 2 : résultat popup ─────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION_PERMISSION) return;

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            Toast.makeText(this,
                    "GPS refusé. Choisissez votre position manuellement.",
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SelectLocationActivity.class);
            startActivityForResult(intent, REQUEST_MANUAL_LOCATION);
        }
    }

    // ── GPS étape 3 : getLastLocation (cache) puis requestLocationUpdates ─────

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // Essayer le cache d'abord (instantané)
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(lastLocation -> {
                    if (lastLocation != null) {
                        userLocation = new LatLng(
                                lastLocation.getLatitude(),
                                lastLocation.getLongitude());
                        updateMapWithUserLocation();
                    }
                    // Dans tous les cas → demander position fraîche
                    startLocationUpdates();
                })
                .addOnFailureListener(e -> startLocationUpdates());
    }

    // ── GPS : forcer position fraîche via LocationUpdates ────────────────────

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(1) // une seule position suffit
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                android.location.Location location = result.getLastLocation();
                if (location != null) {
                    userLocation = new LatLng(
                            location.getLatitude(), location.getLongitude());
                    updateMapWithUserLocation();
                    // Arrêter après avoir eu la position
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, getMainLooper());
    }

    // ── GPS : mettre à jour la carte avec la position ─────────────────────────

    private void updateMapWithUserLocation() {
        if (mMap == null || userLocation == null) return;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        loadEventsOnMap();
    }

    // ── Résultat sélection manuelle ───────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANUAL_LOCATION
                && resultCode == RESULT_OK
                && data != null) {
            double lat = data.getDoubleExtra("latitude", 49.894067);
            double lng = data.getDoubleExtra("longitude", 2.295753);
            userLocation = new LatLng(lat, lng);
            if (mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15));
                loadEventsOnMap();
            }
        }
    }

    // ── Haversine : calcul distance ───────────────────────────────────────────

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

    // ── Nettoyage : éviter fuite mémoire ─────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}