package com.example.android_project_eijv_25;

import com.google.firebase.Timestamp;

/**
 * Modèle de données pour un événement Firestore.
 * Champs correspondant exactement à la collection "Evenements".
 */
public class Evenement {

    private String id;           // ID du document Firestore (non stocké en champ)
    private String titre;
    private String description;
    private double latitude;
    private double longitude;
    private String adresse;
    private String date_debut;
    private String date_fin;
    private String image_url;
    private String user_id;
    private Timestamp date_creation;

    // Constructeur vide requis par Firestore
    public Evenement() {}

    public Evenement(String titre, String description, double latitude, double longitude,
                     String adresse, String date_debut, String date_fin,
                     String image_url, String user_id, Timestamp date_creation) {
        this.titre = titre;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.adresse = adresse;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
        this.image_url = image_url;
        this.user_id = user_id;
        this.date_creation = date_creation;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAdresse() { return adresse; }
    public String getDate_debut() { return date_debut; }
    public String getDate_fin() { return date_fin; }
    public String getImage_url() { return image_url; }
    public String getUser_id() { return user_id; }
    public Timestamp getDate_creation() { return date_creation; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(String id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public void setDate_debut(String date_debut) { this.date_debut = date_debut; }
    public void setDate_fin(String date_fin) { this.date_fin = date_fin; }
    public void setImage_url(String image_url) { this.image_url = image_url; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setDate_creation(Timestamp date_creation) { this.date_creation = date_creation; }
}