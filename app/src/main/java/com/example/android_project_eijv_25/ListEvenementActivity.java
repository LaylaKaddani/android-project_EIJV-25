package com.example.android_project_eijv_25;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListEvenementActivity extends BaseDrawerActivity {

    private static final int REQUEST_LOCATION = 100;

    // ── Vues ──────────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private EvenementAdapter adapter;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    // ── Cloudinary ────────────────────────────────────────────────────────────
    private CloudinaryUploader cloudinaryUploader;

    // ── Dialog ajout ──────────────────────────────────────────────────────────
    private AlertDialog addDialog;
    private TextInputEditText etTitre, etDescription, etAdresse, etDateDebut, etDateFin;
    private TextView tvImageName, tvDialogError;
    private Uri selectedImageUri;
    private double selectedLat = 0, selectedLng = 0;

    // ── Launcher image ────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    tvImageName.setText(uri.getLastPathSegment());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_evenement);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        cloudinaryUploader = new CloudinaryUploader(this);
        currentUserId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        setupDrawer(R.id.drawer_layout);
        initRecycler();
        initSearch();
        loadUserEvents();

        findViewById(R.id.btnAddEvent).setOnClickListener(v -> showAddDialog());

        if (getIntent().getBooleanExtra("openAddDialog", false)) {
            showAddDialog();
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private void initRecycler() {
        recyclerView = findViewById(R.id.recyclerEvenements);
        tvEmpty = findViewById(R.id.tvEmpty);
        adapter = new EvenementAdapter(this, ev -> {
            Intent intent = new Intent(this, DetailEvenementActivity.class);
            intent.putExtra("eventId", ev.getId());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void initSearch() {
        TextInputEditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── Chargement des événements de l'utilisateur ───────────────────────────

    private void loadUserEvents() {
        db.collection("Evenements")
                .whereEqualTo("user_id", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<Evenement> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Evenement ev = doc.toObject(Evenement.class);
                        ev.setId(doc.getId());
                        list.add(ev);
                    }
                    adapter.setEvents(list);
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_load_events),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Dialog ajout événement ────────────────────────────────────────────────

    private void showAddDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_event, null);

        bindAddDialogViews(dialogView);
        resetAddDialog();

        dialogView.findViewById(R.id.btnPickLocation).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectLocationActivity.class);
            startActivityForResult(intent, REQUEST_LOCATION);
        });

        dialogView.findViewById(R.id.btnPickImage).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        dialogView.findViewById(R.id.etDateDebut).setOnClickListener(v ->
                showDatePicker(etDateDebut));
        dialogView.findViewById(R.id.etDateFin).setOnClickListener(v ->
                showDatePicker(etDateFin));
        ((TextInputLayout) dialogView.findViewById(R.id.tilDateDebut))
                .setEndIconOnClickListener(v -> showDatePicker(etDateDebut));
        ((TextInputLayout) dialogView.findViewById(R.id.tilDateFin))
                .setEndIconOnClickListener(v -> showDatePicker(etDateFin));

        dialogView.findViewById(R.id.btnAnnuler).setOnClickListener(v -> {
            if (addDialog != null) addDialog.dismiss();
        });

        dialogView.findViewById(R.id.btnSauvegarder).setOnClickListener(v ->
                validateAndSaveEvent(dialogView));

        addDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (addDialog.getWindow() != null) {
            addDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        addDialog.show();
    }

    private void bindAddDialogViews(View v) {
        etTitre = v.findViewById(R.id.etTitre);
        etDescription = v.findViewById(R.id.etDescription);
        etAdresse = v.findViewById(R.id.etAdresse);
        etDateDebut = v.findViewById(R.id.etDateDebut);
        etDateFin = v.findViewById(R.id.etDateFin);
        tvImageName = v.findViewById(R.id.tvImageName);
        tvDialogError = v.findViewById(R.id.tvDialogError);
    }

    private void resetAddDialog() {
        selectedImageUri = null;
        selectedLat = 0;
        selectedLng = 0;
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%02d/%02d/%04d", day, month + 1, year);
            target.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void validateAndSaveEvent(View dialogView) {
        String titre = getText(etTitre);
        String desc = getText(etDescription);
        String adresse = getText(etAdresse);
        String dateDebut = getText(etDateDebut);
        String dateFin = getText(etDateFin);

        if (titre.isEmpty()) {
            showDialogError(getString(R.string.error_field_titre));
            return;
        }
        if (selectedLat == 0 && selectedLng == 0) {
            showDialogError(getString(R.string.error_field_location));
            return;
        }
        if (dateDebut.isEmpty()) {
            showDialogError(getString(R.string.error_field_date));
            return;
        }

        setDialogLoading(dialogView, true);

        if (selectedImageUri != null) {
            cloudinaryUploader.upload(selectedImageUri, new CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    saveEventToFirestore(titre, desc, adresse, dateDebut, dateFin,
                            imageUrl, dialogView);
                }
                @Override
                public void onFailure(String errorMessage) {
                    setDialogLoading(dialogView, false);
                    showDialogError(getString(R.string.error_upload_image));
                }
            });
        } else {
            saveEventToFirestore(titre, desc, adresse, dateDebut, dateFin, "", dialogView);
        }
    }

    private void saveEventToFirestore(String titre, String desc, String adresse,
                                      String dateDebut, String dateFin,
                                      String imageUrl, View dialogView) {
        Map<String, Object> data = new HashMap<>();
        data.put("titre", titre);
        data.put("description", desc);
        data.put("latitude", selectedLat);
        data.put("longitude", selectedLng);
        data.put("adresse", adresse);
        data.put("date_debut", dateDebut);
        data.put("date_fin", dateFin);
        data.put("image_url", imageUrl);
        data.put("user_id", currentUserId);
        data.put("date_creation", Timestamp.now());

        db.collection("Evenements")
                .add(data)
                .addOnSuccessListener(ref -> {
                    if (addDialog != null) addDialog.dismiss();
                    Toast.makeText(this, getString(R.string.msg_event_added),
                            Toast.LENGTH_SHORT).show();
                    loadUserEvents();
                })
                .addOnFailureListener(e -> {
                    setDialogLoading(dialogView, false);
                    showDialogError(getString(R.string.error_generic));
                });
    }

    private void showDialogError(String msg) {
        if (tvDialogError != null) {
            tvDialogError.setText(msg);
            tvDialogError.setVisibility(View.VISIBLE);
        }
    }

    private void setDialogLoading(View dialogView, boolean loading) {
        View btn = dialogView.findViewById(R.id.btnSauvegarder);
        if (btn != null) btn.setEnabled(!loading);
    }

    // ── Résultat de SelectLocationActivity ───────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION && resultCode == Activity.RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("latitude", 0);
            selectedLng = data.getDoubleExtra("longitude", 0);
            String adresse = data.getStringExtra("adresse");
            if (etAdresse != null) {
                etAdresse.setText(adresse != null ? adresse : "");
            }
        }
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}