package com.example.android_project_eijv_25;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DetailEvenementActivity extends BaseDrawerActivity {

    private static final int REQUEST_LOCATION_EDIT = 200;

    // ── Vues détail ───────────────────────────────────────────────────────────
    private android.widget.ImageView ivDetailImage;
    private TextView tvTitre, tvDescription, tvAdresse, tvLatLng,
            tvDateDebut, tvDateFin, tvUserId, tvCreatedAt;
    private View btnBar;

    // ── Données courantes ─────────────────────────────────────────────────────
    private Evenement currentEvent;
    private String eventId;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // ── Cloudinary ────────────────────────────────────────────────────────────
    private CloudinaryUploader cloudinaryUploader;

    // ── Dialog modification ───────────────────────────────────────────────────
    private AlertDialog editDialog;
    private TextInputEditText etEditTitre, etEditDescription, etEditAdresse,
            etEditDateDebut, etEditDateFin;
    private TextView tvEditImageName, tvEditError;
    private Uri editImageUri;
    private double editLat, editLng;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    editImageUri = uri;
                    if (tvEditImageName != null) tvEditImageName.setText(uri.getLastPathSegment());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_evenement);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        cloudinaryUploader = new CloudinaryUploader(this);

        setupDrawer(R.id.drawer_layout);
        initViews();

        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) loadEventDetails();
    }

    // ── Initialisation des vues ───────────────────────────────────────────────

    private void initViews() {
        ivDetailImage = findViewById(R.id.ivDetailImage);
        tvTitre = findViewById(R.id.tvDetailTitre);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvAdresse = findViewById(R.id.tvDetailAdresse);
        tvLatLng = findViewById(R.id.tvDetailLatLng);
        tvDateDebut = findViewById(R.id.tvDetailDateDebut);
        tvDateFin = findViewById(R.id.tvDetailDateFin);
        tvUserId = findViewById(R.id.tvDetailUserId);
        tvCreatedAt = findViewById(R.id.tvDetailCreatedAt);
        btnBar = findViewById(R.id.btnBar);

        findViewById(R.id.btnEffacer).setOnClickListener(v -> showDeleteConfirm());
        findViewById(R.id.btnModifier).setOnClickListener(v -> showEditDialog());
    }

    // ── Chargement depuis Firestore ───────────────────────────────────────────

    private void loadEventDetails() {
        db.collection("Evenements").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, getString(R.string.error_event_not_found),
                                Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    currentEvent = doc.toObject(Evenement.class);
                    if (currentEvent == null) return;
                    currentEvent.setId(doc.getId());
                    displayEvent();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show());
    }

    private void displayEvent() {
        tvTitre.setText(currentEvent.getTitre());
        tvDescription.setText(currentEvent.getDescription());
        tvAdresse.setText(currentEvent.getAdresse());
        tvLatLng.setText(String.format(Locale.US,
                "Lat: %.5f  |  Lng: %.5f",
                currentEvent.getLatitude(), currentEvent.getLongitude()));
        tvDateDebut.setText(getString(R.string.label_date_debut) + " : "
                + currentEvent.getDate_debut());
        tvDateFin.setText(getString(R.string.label_date_fin) + " : "
                + currentEvent.getDate_fin());
        tvUserId.setText(getString(R.string.label_created_by) + " : "
                + currentEvent.getUser_id());

        if (currentEvent.getDate_creation() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);
            tvCreatedAt.setText(getString(R.string.label_created_at) + " : "
                    + sdf.format(currentEvent.getDate_creation().toDate()));
        }

        if (currentEvent.getImage_url() != null && !currentEvent.getImage_url().isEmpty()) {
            Glide.with(this)
                    .load(currentEvent.getImage_url())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(ivDetailImage);
        }

        // Boutons visibles seulement pour l'auteur
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (uid.equals(currentEvent.getUser_id())) {
            btnBar.setVisibility(View.VISIBLE);
        }
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_delete_event))
                .setPositiveButton(getString(R.string.btn_oui), (dialog, which) -> deleteEvent())
                .setNegativeButton(getString(R.string.btn_non), null)
                .show();
    }

    private void deleteEvent() {
        db.collection("Evenements").document(eventId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, getString(R.string.msg_event_deleted),
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Modification ──────────────────────────────────────────────────────────

    private void showEditDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);

        bindEditViews(dialogView);
        prefillEditDialog();

        dialogView.findViewById(R.id.btnPickLocation).setOnClickListener(v -> {
            Intent intent = new Intent(this, SelectLocationActivity.class);
            startActivityForResult(intent, REQUEST_LOCATION_EDIT);
        });

        dialogView.findViewById(R.id.btnPickImage).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        dialogView.findViewById(R.id.etDateDebut).setOnClickListener(v ->
                showDatePicker(etEditDateDebut));
        dialogView.findViewById(R.id.etDateFin).setOnClickListener(v ->
                showDatePicker(etEditDateFin));
        ((TextInputLayout) dialogView.findViewById(R.id.tilDateDebut))
                .setEndIconOnClickListener(v -> showDatePicker(etEditDateDebut));
        ((TextInputLayout) dialogView.findViewById(R.id.tilDateFin))
                .setEndIconOnClickListener(v -> showDatePicker(etEditDateFin));

        dialogView.findViewById(R.id.btnAnnuler).setOnClickListener(v -> {
            if (editDialog != null) editDialog.dismiss();
        });

        MaterialButton btnSave = dialogView.findViewById(R.id.btnSauvegarder);
        btnSave.setText(getString(R.string.btn_sauvegarder));
        btnSave.setOnClickListener(v -> validateAndUpdateEvent(dialogView));

        editDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (editDialog.getWindow() != null) {
            editDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        editDialog.show();
    }

    private void bindEditViews(View v) {
        etEditTitre = v.findViewById(R.id.etTitre);
        etEditDescription = v.findViewById(R.id.etDescription);
        etEditAdresse = v.findViewById(R.id.etAdresse);
        etEditDateDebut = v.findViewById(R.id.etDateDebut);
        etEditDateFin = v.findViewById(R.id.etDateFin);
        tvEditImageName = v.findViewById(R.id.tvImageName);
        tvEditError = v.findViewById(R.id.tvDialogError);
    }

    private void prefillEditDialog() {
        if (currentEvent == null) return;
        etEditTitre.setText(currentEvent.getTitre());
        etEditDescription.setText(currentEvent.getDescription());
        etEditAdresse.setText(currentEvent.getAdresse());
        etEditDateDebut.setText(currentEvent.getDate_debut());
        etEditDateFin.setText(currentEvent.getDate_fin());
        editLat = currentEvent.getLatitude();
        editLng = currentEvent.getLongitude();
        editImageUri = null;
        if (currentEvent.getImage_url() != null && !currentEvent.getImage_url().isEmpty()) {
            tvEditImageName.setText(getString(R.string.image_already_set));
        }
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%02d/%02d/%04d", day, month + 1, year);
            target.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void validateAndUpdateEvent(View dialogView) {
        String titre = getText(etEditTitre);
        String desc = getText(etEditDescription);
        String adresse = getText(etEditAdresse);
        String dateDebut = getText(etEditDateDebut);
        String dateFin = getText(etEditDateFin);

        if (titre.isEmpty()) {
            showEditError(getString(R.string.error_field_titre));
            return;
        }

        dialogView.findViewById(R.id.btnSauvegarder).setEnabled(false);

        if (editImageUri != null) {
            cloudinaryUploader.upload(editImageUri, new CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    updateFirestore(titre, desc, adresse, dateDebut, dateFin,
                            imageUrl, dialogView);
                }
                @Override
                public void onFailure(String errorMessage) {
                    dialogView.findViewById(R.id.btnSauvegarder).setEnabled(true);
                    showEditError(getString(R.string.error_upload_image));
                }
            });
        } else {
            String existingUrl = currentEvent.getImage_url() != null
                    ? currentEvent.getImage_url() : "";
            updateFirestore(titre, desc, adresse, dateDebut, dateFin, existingUrl, dialogView);
        }
    }

    private void updateFirestore(String titre, String desc, String adresse,
                                 String dateDebut, String dateFin,
                                 String imageUrl, View dialogView) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("titre", titre);
        updates.put("description", desc);
        updates.put("adresse", adresse);
        updates.put("latitude", editLat);
        updates.put("longitude", editLng);
        updates.put("date_debut", dateDebut);
        updates.put("date_fin", dateFin);
        updates.put("image_url", imageUrl);

        db.collection("Evenements").document(eventId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    if (editDialog != null) editDialog.dismiss();
                    Toast.makeText(this, getString(R.string.msg_event_updated),
                            Toast.LENGTH_SHORT).show();
                    loadEventDetails();
                })
                .addOnFailureListener(e -> {
                    dialogView.findViewById(R.id.btnSauvegarder).setEnabled(true);
                    showEditError(getString(R.string.error_generic));
                });
    }

    private void showEditError(String msg) {
        if (tvEditError != null) {
            tvEditError.setText(msg);
            tvEditError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION_EDIT && resultCode == Activity.RESULT_OK
                && data != null) {
            editLat = data.getDoubleExtra("latitude", editLat);
            editLng = data.getDoubleExtra("longitude", editLng);
            String adresse = data.getStringExtra("adresse");
            if (etEditAdresse != null) {
                etEditAdresse.setText(adresse != null ? adresse : "");
            }
        }
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }
}