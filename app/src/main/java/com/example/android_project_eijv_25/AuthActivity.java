package com.example.android_project_eijv_25;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class AuthActivity extends AppCompatActivity {

    // ── Vues formulaire de connexion ──────────────────────────────────────────
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnOpenRegister;
    private TextView tvErrorMessage;

    // ── Vues du popup d'inscription ───────────────────────────────────────────
    private CardView cardRegisterDialog;
    private View viewOverlay;
    private TextInputLayout tilRegisterEmail, tilRegisterPassword, tilRegisterConfirmPassword;
    private TextInputEditText etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private MaterialButton btnRegisterSubmit;
    private TextView tvRegisterSuccess, tvRegisterError;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        Objects.requireNonNull(getSupportActionBar()).hide();

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        // Si un utilisateur est déjà connecté, on redirige directement vers MainActivity
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null) {
//            navigateToMain();
//        }
//    }

    // ── Initialisation des vues ───────────────────────────────────────────────

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnOpenRegister = findViewById(R.id.btnOpenRegister);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);

        cardRegisterDialog = findViewById(R.id.cardRegisterDialog);
        viewOverlay = findViewById(R.id.viewOverlay);
        tilRegisterEmail = findViewById(R.id.tilRegisterEmail);
        tilRegisterPassword = findViewById(R.id.tilRegisterPassword);
        tilRegisterConfirmPassword = findViewById(R.id.tilRegisterConfirmPassword);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvRegisterSuccess = findViewById(R.id.tvRegisterSuccess);
        tvRegisterError = findViewById(R.id.tvRegisterError);

        findViewById(R.id.btnCloseDialog).setOnClickListener(v -> closeRegisterDialog());
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnOpenRegister.setOnClickListener(v -> openRegisterDialog());
        btnRegisterSubmit.setOnClickListener(v -> handleRegister());
        viewOverlay.setOnClickListener(v -> closeRegisterDialog());
    }

    // ── Connexion ─────────────────────────────────────────────────────────────

    private void handleLogin() {
        hideLoginError();
        clearLoginFieldErrors();

        String email = getTextFrom(etEmail);
        String password = getTextFrom(etPassword);

        if (!validateLoginFields(email, password)) return;

        hideKeyboard();
        setLoginLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoginLoading(false);
                    if (task.isSuccessful()) {
                        navigateToMain();
                    } else {
                        showLoginError(getLoginErrorMessage(task.getException()));
                    }
                });
    }

    private boolean validateLoginFields(String email, String password) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_empty_fields));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_fields));
            valid = false;
        }
        return valid;
    }

    private String getLoginErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return getString(R.string.error_user_not_found);
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.error_wrong_password);
        }
        return getString(R.string.error_generic);
    }

    private void showLoginError(String message) {
        tvErrorMessage.setText(message);
        tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void hideLoginError() {
        tvErrorMessage.setVisibility(View.GONE);
    }

    private void clearLoginFieldErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void setLoginLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnOpenRegister.setEnabled(!isLoading);
        btnLogin.setText(isLoading
                ? getString(R.string.btn_login_loading)
                : getString(R.string.btn_login));
    }

    // ── Inscription (popup) ───────────────────────────────────────────────────

    private void openRegisterDialog() {
        resetRegisterDialog();
        viewOverlay.setVisibility(View.VISIBLE);
        cardRegisterDialog.setVisibility(View.VISIBLE);
    }

    private void closeRegisterDialog() {
        cardRegisterDialog.setVisibility(View.GONE);
        viewOverlay.setVisibility(View.GONE);
        hideKeyboard();
    }

    private void resetRegisterDialog() {
        etRegisterEmail.setText("");
        etRegisterPassword.setText("");
        etRegisterConfirmPassword.setText("");
        tilRegisterEmail.setError(null);
        tilRegisterPassword.setError(null);
        tilRegisterConfirmPassword.setError(null);
        tvRegisterSuccess.setVisibility(View.GONE);
        tvRegisterError.setVisibility(View.GONE);
        btnRegisterSubmit.setEnabled(true);
        btnRegisterSubmit.setText(getString(R.string.btn_register_submit));
    }

    private void handleRegister() {
        hideRegisterMessages();
        clearRegisterFieldErrors();

        String email = getTextFrom(etRegisterEmail);
        String password = getTextFrom(etRegisterPassword);
        String confirm = getTextFrom(etRegisterConfirmPassword);

        if (!validateRegisterFields(email, password, confirm)) return;

        hideKeyboard();
        setRegisterLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setRegisterLoading(false);
                    if (task.isSuccessful()) {
                        // Inscription réussie : on déconnecte immédiatement pour
                        // forcer l'utilisateur à se connecter manuellement
                        mAuth.signOut();
                        showRegisterSuccess();
                    } else {
                        showRegisterError(getRegisterErrorMessage(task.getException()));
                    }
                });
    }

    private boolean validateRegisterFields(String email, String password, String confirm) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            tilRegisterEmail.setError(getString(R.string.error_empty_fields));
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilRegisterEmail.setError(getString(R.string.error_invalid_email));
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilRegisterPassword.setError(getString(R.string.error_empty_fields));
            valid = false;
        } else if (password.length() < 6) {
            tilRegisterPassword.setError(getString(R.string.error_password_too_short));
            valid = false;
        }

        if (TextUtils.isEmpty(confirm)) {
            tilRegisterConfirmPassword.setError(getString(R.string.error_empty_fields));
            valid = false;
        } else if (!password.equals(confirm)) {
            tilRegisterConfirmPassword.setError(getString(R.string.error_passwords_not_match));
            valid = false;
        }

        return valid;
    }

    private String getRegisterErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthUserCollisionException) {
            return getString(R.string.error_email_already_used);
        } else if (exception instanceof FirebaseAuthWeakPasswordException) {
            return getString(R.string.error_password_too_short);
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return getString(R.string.error_invalid_email);
        }
        return getString(R.string.error_generic);
    }

    private void showRegisterSuccess() {
        tvRegisterSuccess.setVisibility(View.VISIBLE);
        tvRegisterError.setVisibility(View.GONE);
        btnRegisterSubmit.setEnabled(false);

        // Pré-remplir le champ email dans le formulaire de connexion
        String registeredEmail = getTextFrom(etRegisterEmail);
        etEmail.setText(registeredEmail);
        etPassword.setText("");

        // Fermer automatiquement le popup après 2 secondes
        cardRegisterDialog.postDelayed(this::closeRegisterDialog, 2000);
    }

    private void showRegisterError(String message) {
        tvRegisterError.setText(message);
        tvRegisterError.setVisibility(View.VISIBLE);
        tvRegisterSuccess.setVisibility(View.GONE);
    }

    private void hideRegisterMessages() {
        tvRegisterSuccess.setVisibility(View.GONE);
        tvRegisterError.setVisibility(View.GONE);
    }

    private void clearRegisterFieldErrors() {
        tilRegisterEmail.setError(null);
        tilRegisterPassword.setError(null);
        tilRegisterConfirmPassword.setError(null);
    }

    private void setRegisterLoading(boolean isLoading) {
        btnRegisterSubmit.setEnabled(!isLoading);
        btnRegisterSubmit.setText(isLoading
                ? getString(R.string.btn_register_loading)
                : getString(R.string.btn_register_submit));
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void navigateToMain() {
        Toast.makeText(this, getString(R.string.toast_welcome), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String getTextFrom(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}