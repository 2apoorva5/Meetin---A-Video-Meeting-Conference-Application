package com.developerdepository.meetin.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.developerdepository.meetin.R;
import com.developerdepository.meetin.Utilities.Constants;
import com.developerdepository.meetin.Utilities.PreferenceManager;

import maes.tech.intentanim.CustomIntent;

public class WelcomeActivity extends AppCompatActivity {

    //View Variables
    private ImageView closeBtn;
    private ConstraintLayout signUpBtn, loginBtn;
    private TextView privacyPolicies;

    //Other Variables
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(android.R.color.black));

        initViews();
        setActionOnViews();
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        signUpBtn = findViewById(R.id.sign_up_btn);
        loginBtn = findViewById(R.id.login_btn);
        privacyPolicies = findViewById(R.id.privacy_policies);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        signUpBtn.setOnClickListener(view -> {
            startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        });

        loginBtn.setOnClickListener(view -> {
            startActivity(new Intent(WelcomeActivity.this, SignInActivity.class));
            CustomIntent.customType(WelcomeActivity.this, "fadein-to-fadeout");
            finish();
        });

        privacyPolicies.setOnClickListener(view -> {

        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}