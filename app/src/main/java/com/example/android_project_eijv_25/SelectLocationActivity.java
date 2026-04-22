package com.example.android_project_eijv_25;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SelectLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker selectedMarker;
    private double selectedLat = 0;
    private double selectedLng = 0;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_location);
        Objects.requireNonNull(getSupportActionBar()).hide();

        btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setEnabled(false);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> confirmLocation());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapSelect);
        if (mapFragment == null) throw new AssertionError();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Centrer sur Amiens par défaut
        LatLng defaultPos = new LatLng(49.894067, 2.295753);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPos, 12));

        // Long press pour placer le marqueur
        mMap.setOnMapLongClickListener(latLng -> {
            if (selectedMarker != null) selectedMarker.remove();

            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;

            selectedMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.marker_selected_location)));

            btnConfirm.setEnabled(true);
        });
    }

    private void confirmLocation() {
        String adresse = geocodeAddress(selectedLat, selectedLng);
        Intent result = new Intent();
        result.putExtra("latitude", selectedLat);
        result.putExtra("longitude", selectedLng);
        result.putExtra("adresse", adresse);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    private String geocodeAddress(double lat, double lng) {
        if (!Geocoder.isPresent()) return "";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) sb.append(", ");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_geocode), Toast.LENGTH_SHORT).show();
        }
        return String.format(Locale.US, "%.5f, %.5f", lat, lng);
    }
}