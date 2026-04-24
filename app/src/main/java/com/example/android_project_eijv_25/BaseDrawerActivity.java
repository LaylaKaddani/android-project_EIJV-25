package com.example.android_project_eijv_25;

import android.content.Intent;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Activité de base : gère le menu tiroir commun à toutes les activités.
 * Toutes les activités avec un drawer doivent étendre cette classe.
 */
public abstract class BaseDrawerActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;

    protected void navigateIfNot(Class<?> targetActivity) {
        if (!this.getClass().equals(targetActivity)) {
            startActivity(new Intent(this, targetActivity));
        }
    }
    protected void setupDrawer(int drawerLayoutId) {
        drawerLayout = findViewById(drawerLayoutId);

        if (drawerLayout == null) return;

        safeClick(R.id.btnMenu, v ->
                drawerLayout.openDrawer(GravityCompat.START));

        safeClick(R.id.btnCloseDrawer, v ->
                drawerLayout.closeDrawer(GravityCompat.START));

        safeClick(R.id.menuAccueil, v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navigateIfNot(MainActivity.class);
        });

        safeClick(R.id.menuMesEvenements, v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navigateIfNot(ListEvenementActivity.class);
        });

        safeClick(R.id.menuAPropos, v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navigateIfNot(AProposActivity.class);
        });

        safeClick(R.id.menuDeconnecter, v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void safeClick(int id, android.view.View.OnClickListener listener) {
        android.view.View v = findViewById(id);
        if (v != null) v.setOnClickListener(listener);
    }
}