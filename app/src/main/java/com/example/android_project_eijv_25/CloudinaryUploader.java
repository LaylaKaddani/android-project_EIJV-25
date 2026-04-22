package com.example.android_project_eijv_25;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gère l'upload d'images vers Cloudinary en mode unsigned (sans clé secrète).
 * Utilise un thread séparé pour ne pas bloquer l'UI.
 */
public class CloudinaryUploader {

    private static final String CLOUD_NAME   = "dikikeoex";
    private static final String UPLOAD_PRESET = "geoevent_upload";
    private static final String UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    private static final String BOUNDARY = "----GeoEventBoundary" + System.currentTimeMillis();
    private static final String LINE_FEED = "\r\n";

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CloudinaryUploader(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Lance l'upload en arrière-plan.
     * Le callback est appelé sur le thread principal.
     */
    public void upload(Uri imageUri, UploadCallback callback) {
        executor.execute(() -> {
            try {
                byte[] imageBytes = readBytes(imageUri);
                String responseUrl = performUpload(imageBytes);
                runOnMain(() -> callback.onSuccess(responseUrl));
            } catch (Exception e) {
                runOnMain(() -> callback.onFailure(e.getMessage() != null
                        ? e.getMessage() : "Erreur inconnue"));
            }
        });
    }

    // ── Lecture du fichier image ──────────────────────────────────────────────

    private byte[] readBytes(Uri uri) throws IOException {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("Impossible d'ouvrir l'image.");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        inputStream.close();
        return buffer.toByteArray();
    }

    // ── Upload multipart vers Cloudinary ─────────────────────────────────────

    private String performUpload(byte[] imageBytes) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        OutputStream outputStream = conn.getOutputStream();

        // Champ upload_preset
        writeField(outputStream, "upload_preset", UPLOAD_PRESET);

        // Champ fichier image
        writeFilePart(outputStream, "file", imageBytes);

        // Fermeture du multipart
        outputStream.write(("--" + BOUNDARY + "--" + LINE_FEED).getBytes());
        outputStream.flush();
        outputStream.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("Cloudinary a retourné le code HTTP : " + responseCode);
        }

        // Lecture de la réponse JSON
        InputStream responseStream = conn.getInputStream();
        String json = new String(readStreamBytes(responseStream));
        responseStream.close();
        conn.disconnect();

        return extractSecureUrl(json);
    }

    private void writeField(OutputStream out, String name, String value) throws IOException {
        out.write(("--" + BOUNDARY + LINE_FEED).getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED).getBytes());
        out.write(LINE_FEED.getBytes());
        out.write((value + LINE_FEED).getBytes());
    }

    private void writeFilePart(OutputStream out, String fieldName, byte[] data) throws IOException {
        out.write(("--" + BOUNDARY + LINE_FEED).getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"image.jpg\"" + LINE_FEED).getBytes());
        out.write(("Content-Type: image/jpeg" + LINE_FEED).getBytes());
        out.write(LINE_FEED.getBytes());
        out.write(data);
        out.write(LINE_FEED.getBytes());
    }

    private byte[] readStreamBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    // ── Extraction de secure_url depuis le JSON Cloudinary ───────────────────

    private String extractSecureUrl(String json) throws IOException {
        String key = "\"secure_url\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) throw new IOException("Réponse Cloudinary invalide : " + json);
        int start = json.indexOf("\"", keyIndex + key.length()) + 1;
        int end = json.indexOf("\"", start);
        String url = json.substring(start, end).replace("\\/", "/");
        if (url.isEmpty()) throw new IOException("URL Cloudinary vide.");
        return url;
    }

    // ── Utilitaire : exécuter sur le thread principal ─────────────────────────

    private final android.os.Handler mainHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private void runOnMain(Runnable runnable) {
        mainHandler.post(runnable);
    }
}