package com.example.android_project_eijv_25;

import android.os.Bundle;

import java.util.Objects;

public class AProposActivity extends BaseDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_a_propos);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setupDrawer(R.id.drawer_layout);
    }
}