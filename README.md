#  GeoEvent — Application de partage d'événements géolocalisés

> **Projet Android **  
> Année universitaire : 2025-2026

---

##  Présentation

**GeoEvent** est une application Android communautaire permettant aux utilisateurs de partager et découvrir des événements géolocalisés autour d'eux.

### Fonctionnalités principales
- Visualiser des événements sur une carte interactive Google Maps
- Ajouter un événement avec titre, description, localisation GPS et photo
- Consulter les détails complets d'un événement
- Filtrer les événements par distance par rapport à sa position réelle
- S'authentifier de façon sécurisée (inscription / connexion)
- Gérer sa position GPS ou la choisir manuellement sur la carte

---

## Équipe

| Membre | Rôle | Contributions |
|--------|------|---------------|
| **Layla Kaddani** | Développeuse & Sécurité | GPS réel + permissions runtime, Sélection manuelle de position, Firestore Security Rules, Sécurité Manifest, Renforcement .gitignore, Architecture initiale |
| **Mohamed nasrellah abdi** | Développeur | Carte Google Maps, Marqueurs, Intégration Maps API,  Cloudinary, Architecture initiale |
| **Harerimana Lambert** | Développeur | Authentification, Interface Auth, Créer et modifier des évènements, Architecture initiale |

---

## Architecture

### Organisation des packages

```
com.example.android_project_eijv_25/
│
├──   Activities
│   ├── AuthActivity.java              # Écran connexion / inscription (Firebase Auth)
│   ├── MainActivity.java              # Carte principale, GPS, filtre distance
│   ├── ListEvenementActivity.java     # Liste des événements de l'utilisateur
│   ├── DetailEvenementActivity.java   # Détails, modification, suppression
│   ├── SelectLocationActivity.java    # Sélection manuelle d'une position sur carte
│   └── AProposActivity.java           # Page À propos
│
├── 🧩 Base
│   └── BaseDrawerActivity.java        # Classe abstraite : menu tiroir partagé
│
├── 📦 Modèles
│   └── Evenement.java                 # Modèle de données POJO (Firestore)
│
├── 🔧 Adapters
│   └── EvenementAdapter.java          # RecyclerView adapter liste événements
│
└── ☁️ Services
    └── CloudinaryUploader.java        # Upload images Cloudinary (thread séparé)
```

### Schéma d'architecture

```
┌─────────────────────────────────────────────────────┐
│                  Application Android                  │
│                                                       │
│  AuthActivity ──────────────────► MainActivity        │
│       │                               │               │
│  Inscription/                    Google Maps SDK      │
│  Connexion                            │               │
│       │                     ┌─────────┴──────────┐   │
│  Firebase Auth          ListEvenement    Detail    │   │
│                         Activity        Evenement  │   │
│                              │          Activity   │   │
│                         SelectLocation             │   │
│                         Activity                   │   │
└─────────────────────────────────────────────────────┘
          │                         │
   Firebase Firestore          Cloudinary
   (base de données +          (stockage
    authentification)           images)
```

### Séparation des responsabilités

| Classe | Responsabilité unique |
|--------|-----------------------|
| `Evenement.java` | Modèle de données pur, aucune logique UI |
| `BaseDrawerActivity.java` | Menu tiroir partagé, évite la duplication |
| `EvenementAdapter.java` | Affichage liste + recherche, indépendant de la source |
| `CloudinaryUploader.java` | Upload image dans thread séparé, callback main thread |
| `SelectLocationActivity.java` | Sélection position réutilisée (ajout + position utilisateur) |

---

##  Choix techniques

| Technologie | Version | Justification |
|-------------|---------|---------------|
| **Java** 
| **Google Maps SDK** | 18+ | Fiable, natif Android, bonne documentation |
| **Firebase Firestore** | BoM 34.12.0 | NoSQL temps réel, Auth intégré, gratuit |
| **Firebase Auth** | — | Gestion sécurisée des tokens, pas de mot de passe stocké |
| **Cloudinary** | API REST | Upload unsigned, pas de clé secrète exposée |
| **Glide** | 5.0.7 | Cache automatique images, performances optimisées |
| **FusedLocationProviderClient** | play-services-location:21.0.1 | API Google recommandée, combine GPS + WiFi + réseau |
| **DrawerLayout** | — | Navigation cohérente sur tous les écrans |
| **Material Design** | — | Interface moderne et cohérente |

---

##  Sécurité — Defense in Depth

Ce projet implémente plusieurs couches de sécurité indépendantes (**Defense in Depth**), principe fondamental en cybersécurité.

### 1. 📋 Permissions runtime GPS
La permission de localisation est demandée dynamiquement à l'utilisateur conformément aux bonnes pratiques Android 6+ (API 23+). Trois scénarios sont gérés :
- **Accordée (précise)** → vraie position GPS via `getCurrentLocation()`
- **Accordée (approximative)** → position réseau/WiFi
- **Refusée** → sélection manuelle sur la carte via `SelectLocationActivity`

### 2.  Activities non exportées
Toutes les activités sauf `AuthActivity` sont déclarées `android:exported="false"` dans le `AndroidManifest.xml`.

**Sans cette mesure**, une application malveillante installée sur le même téléphone pourrait lancer directement `MainActivity` via un Intent explicite, **en contournant totalement l'authentification Firebase**.

### 3. 🔐Firestore Security Rules (protection côté serveur)
Les règles Firestore protègent la base de données indépendamment du code Android. Même si quelqu'un utilise Postman ou curl directement, les règles s'appliquent :

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /Evenements/{eventId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
                    && request.resource.data.user_id == request.auth.uid;
      allow update, delete: if request.auth != null
                             && resource.data.user_id == request.auth.uid;
    }
  }
}
```

La validation côté Android (cacher les boutons) et les Security Rules sont **complémentaires** : l'une protège l'UX, l'autre protège réellement la donnée côté serveur.

### 4. Privacy by Design — Position jamais stockée
La position de l'utilisateur est utilisée **uniquement côté client** pour le filtrage par distance. Elle n'est jamais envoyée ni stockée dans Firebase. Seule la position des événements est persistée, conformément au principe **Privacy by Design**.

### 5. Clés API protégées
- `google-services.json` exclu du dépôt Git via `.gitignore`
- Clés Cloudinary dans `strings.xml` (hors dépôt)
- Upload Cloudinary en mode **unsigned** → aucune clé secrète dans le code
- Keystores (`.jks`, `.keystore`) exclus du dépôt

### 6. Validation des données
Chaque formulaire valide les champs côté client avant tout envoi :
- Format email via `android.util.Patterns.EMAIL_ADDRESS`
- Longueur mot de passe (minimum 6 caractères)
- Champs obligatoires (titre, localisation, date)
- Erreurs Firebase typées (`FirebaseAuthInvalidCredentialsException`, etc.)

### 7.  Session non persistante (choix délibéré)
La reconnexion automatique est désactivée. L'utilisateur doit se reconnecter à chaque session. Ce choix protège les données en cas de **vol ou perte du téléphone** — compromis délibéré entre sécurité et utilisabilité (**Security vs Usability**).

##  Fonctionnalités détaillées

### Option 2 — Niveau Intermédiaire (20 points)

| Fonctionnalité | Détail |
|----------------|--------|
| - Position utilisateur sur carte | Marqueur bleu, position GPS réelle |
| - Ajout événement | Titre, description, GPS, date début/fin, image |
| - Sauvegarde Firestore | Collection `Evenements` avec `user_id` |
| - Affichage marqueurs | Marqueurs rouges, clic → détails |
| - Détails événement | Image, titre, description, adresse, dates, auteur |
| -Authentification | Inscription + connexion Firebase Auth |
| - Upload image | Cloudinary via multipart HTTP, thread séparé |
| - Filtrage par distance | Spinner 5/10/15/30 km, formule Haversine |
| - Permissions GPS runtime | Précis / approximatif / refus géré |
| - Sélection manuelle | Appui long sur carte si GPS refusé |
| - Modification événement | Auteur uniquement |
| - Suppression événement | Auteur uniquement avec confirmation |
| - Menu navigation | DrawerLayout hamburger |
| - Recherche événements | Recherche temps réel par titre |

---

##  Procédure d'installation

### Prérequis
- Android Studio Hedgehog ou supérieur
- JDK 11
- Android SDK API 29+
- Compte Firebase (gratuit)
- Compte Cloudinary (gratuit)
- Clé API Google Maps

### Étapes

**1. Cloner le dépôt**
```bash
git clone https://github.com/LaylaKaddani/android-project_EIJV-25.git
cd android-project_EIJV-25
```

**2. Configurer Firebase**
- Créer un projet sur [Firebase Console](https://console.firebase.google.com)
- Activer **Firestore Database** et **Authentication** (Email/Password)
- Télécharger `google-services.json` et le placer dans `app/`

**3. Appliquer les Firestore Security Rules**

Dans Firebase Console → Firestore → onglet Rules :
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /Evenements/{eventId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
                    && request.resource.data.user_id == request.auth.uid;
      allow update, delete: if request.auth != null
                             && resource.data.user_id == request.auth.uid;
    }
  }
}
```
Cliquer sur **Publish**.

**4. Configurer Google Maps**
- Obtenir une clé API sur [Google Cloud Console](https://console.cloud.google.com)
- Activer **Maps SDK for Android**
- Ajouter dans `local.properties` :
```
MAPS_API_KEY=ta_clé_ici
```

**5. Configurer Cloudinary**
- Créer un compte sur [Cloudinary](https://cloudinary.com)
- Créer un **Upload Preset** en mode `unsigned`
- Ajouter dans `res/values/strings.xml` :
```xml
<string name="cloudinary_cloud_name">ton_cloud_name</string>
<string name="cloudinary_upload_preset">ton_upload_preset</string>
```

**6. Lancer l'application**
```
Android Studio → Sync Project with Gradle Files → Run
```
> Tester sur un **appareil physique** (API 29+) pour le GPS réel.

---

##  Dépendances principales

```gradle
// Google Maps
implementation(libs.play.services.maps)

// GPS / Localisation
implementation("com.google.android.gms:play-services-location:21.0.1")

// Firebase
implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
implementation(libs.firebase.auth)
implementation(libs.firebase.firestore)
implementation(libs.firebase.storage)
implementation(libs.firebase.analytics)

// Glide (chargement images)
implementation("com.github.bumptech.glide:glide:5.0.7")

// Material Design
implementation(libs.material)
```

---

## Licence

Projet académique — EIJV 2025-2026  
Tous droits réservés
