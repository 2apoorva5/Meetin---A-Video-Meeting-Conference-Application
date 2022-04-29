package com.developerdepository.meetin.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.developerdepository.meetin.R;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class ForgotPasswordActivity extends AppCompatActivity {

    //View Variables
    private ImageView backBtn;
    private TextInputLayout emailField;
    private ConstraintLayout sendBtn;
    private CardView sendBtnContainer;
    private ProgressBar forgotPasswordProgressBar;

    //Firebase Variables
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));

        initViews();
        initFirebase();
        setActionOnViews();

        KeyboardVisibilityEvent.setEventListener(ForgotPasswordActivity.this, isOpen -> {
            if (!isOpen) {
                emailField.clearFocus();
            }
        });
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        emailField = findViewById(R.id.forgot_password_email_field);
        sendBtn = findViewById(R.id.send_btn);
        sendBtnContainer = findViewById(R.id.send_btn_container);
        forgotPasswordProgressBar = findViewById(R.id.forgot_password_progress_bar);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void setActionOnViews() {
        backBtn.setOnClickListener(v -> onBackPressed());

        emailField.getEditText().addTextChangedListener(forgotPasswordTextWatcher);
    }

    private TextWatcher forgotPasswordTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            final String email = emailField.getEditText().getText().toString().trim();

            if (!email.isEmpty()) {
                forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                sendBtnContainer.setVisibility(View.VISIBLE);
                sendBtn.setEnabled(true);
                sendBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(ForgotPasswordActivity.this);
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(emailField);
                        emailField.setError("Enter a valid email!");
                        return;
                    } else {
                        emailField.setError(null);
                        emailField.setErrorEnabled(false);
                        if (!isConnectedToInternet(ForgotPasswordActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            sendBtnContainer.setVisibility(View.INVISIBLE);
                            sendBtn.setEnabled(false);
                            forgotPasswordProgressBar.setVisibility(View.VISIBLE);

                            firebaseAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (!task.getResult().getSignInMethods().isEmpty() &&
                                            task.getResult().getSignInMethods().size() != 0) {
                                        firebaseAuth.sendPasswordResetEmail(email)
                                                .addOnCompleteListener(task1 -> {
                                                    if (task1.isSuccessful()) {
                                                        forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                                                        sendBtnContainer.setVisibility(View.VISIBLE);
                                                        sendBtn.setEnabled(true);

                                                        MaterialDialog materialDialog = new MaterialDialog.Builder(ForgotPasswordActivity.this)
                                                                .setTitle("Check your mail!")
                                                                .setMessage("A password reset link has been sent to " + email + ". Go check your mail!")
                                                                .setAnimation(R.raw.sent_email)
                                                                .setCancelable(false)
                                                                .setPositiveButton("Go", R.drawable.ic_dialog_yes, (dialogInterface, which) -> {
                                                                    dialogInterface.dismiss();
                                                                    Intent intent = ForgotPasswordActivity.this.getPackageManager().getLaunchIntentForPackage("com.google.android.gm");

                                                                    if (intent != null) {
                                                                        startActivity(intent);
                                                                        finish();
                                                                    } else {
                                                                        Toast.makeText(ForgotPasswordActivity.this, "Didn't find Gmail on your mobile!", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                })
                                                                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> {
                                                                    dialogInterface.dismiss();
                                                                    finish();
                                                                })
                                                                .build();
                                                        materialDialog.show();
                                                    } else {
                                                        forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                                                        sendBtnContainer.setVisibility(View.VISIBLE);
                                                        sendBtn.setEnabled(true);

                                                        Alerter.create(ForgotPasswordActivity.this)
                                                                .setText("Whoa! That ran into some ERROR. Try again!")
                                                                .setTextAppearance(R.style.ErrorAlert)
                                                                .setBackgroundColorRes(R.color.errorColor)
                                                                .setIcon(R.drawable.ic_error)
                                                                .setDuration(3000)
                                                                .enableIconPulse(true)
                                                                .enableVibration(true)
                                                                .disableOutsideTouch()
                                                                .enableProgress(true)
                                                                .setProgressColorInt(getColor(android.R.color.white))
                                                                .show();
                                                        return;
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                                                    sendBtnContainer.setVisibility(View.VISIBLE);
                                                    sendBtn.setEnabled(true);

                                                    Alerter.create(ForgotPasswordActivity.this)
                                                            .setText("Whoa! That ran into some ERROR. Try again!")
                                                            .setTextAppearance(R.style.ErrorAlert)
                                                            .setBackgroundColorRes(R.color.errorColor)
                                                            .setIcon(R.drawable.ic_error)
                                                            .setDuration(3000)
                                                            .enableIconPulse(true)
                                                            .enableVibration(true)
                                                            .disableOutsideTouch()
                                                            .enableProgress(true)
                                                            .setProgressColorInt(getColor(android.R.color.white))
                                                            .show();
                                                    return;
                                                });
                                    } else {
                                        forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                                        sendBtnContainer.setVisibility(View.VISIBLE);
                                        sendBtn.setEnabled(true);

                                        YoYo.with(Techniques.Shake)
                                                .duration(700)
                                                .repeat(0)
                                                .playOn(emailField);
                                        Alerter.create(ForgotPasswordActivity.this)
                                                .setText("We didn't find any account with that email.")
                                                .setTextAppearance(R.style.ErrorAlert)
                                                .setBackgroundColorRes(R.color.errorColor)
                                                .setIcon(R.drawable.ic_error)
                                                .setDuration(3000)
                                                .enableIconPulse(true)
                                                .enableVibration(true)
                                                .disableOutsideTouch()
                                                .enableProgress(true)
                                                .setProgressColorInt(getColor(android.R.color.white))
                                                .show();
                                        return;
                                    }
                                } else {
                                    forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                                    sendBtnContainer.setVisibility(View.VISIBLE);
                                    sendBtn.setEnabled(true);

                                    Alerter.create(ForgotPasswordActivity.this)
                                            .setText("Whoa! That ran into some ERROR. Try again!")
                                            .setTextAppearance(R.style.ErrorAlert)
                                            .setBackgroundColorRes(R.color.errorColor)
                                            .setIcon(R.drawable.ic_error)
                                            .setDuration(3000)
                                            .enableIconPulse(true)
                                            .enableVibration(true)
                                            .disableOutsideTouch()
                                            .enableProgress(true)
                                            .setProgressColorInt(getColor(android.R.color.white))
                                            .show();
                                    return;
                                }
                            }).addOnFailureListener(e -> {
                                forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                                sendBtnContainer.setVisibility(View.VISIBLE);
                                sendBtn.setEnabled(true);

                                Alerter.create(ForgotPasswordActivity.this)
                                        .setText("Whoa! That ran into some ERROR. Try again!")
                                        .setTextAppearance(R.style.ErrorAlert)
                                        .setBackgroundColorRes(R.color.errorColor)
                                        .setIcon(R.drawable.ic_error)
                                        .setDuration(3000)
                                        .enableIconPulse(true)
                                        .enableVibration(true)
                                        .disableOutsideTouch()
                                        .enableProgress(true)
                                        .setProgressColorInt(getColor(android.R.color.white))
                                        .show();
                                return;
                            });
                        }
                    }
                });
            } else {
                forgotPasswordProgressBar.setVisibility(View.INVISIBLE);
                sendBtnContainer.setVisibility(View.VISIBLE);
                sendBtn.setEnabled(true);
                sendBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(ForgotPasswordActivity.this);
                    YoYo.with(Techniques.Shake)
                            .duration(700)
                            .repeat(0)
                            .playOn(emailField);
                    emailField.setError("Provide your email first!");
                });
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private boolean isConnectedToInternet(ForgotPasswordActivity forgotPasswordActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) forgotPasswordActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressLint("MissingPermission") NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(ForgotPasswordActivity.this)
                .setTitle("No Internet Connection!")
                .setMessage("Please connect to a network first to proceed from here!")
                .setCancelable(false)
                .setAnimation(R.raw.no_internet_connection)
                .setPositiveButton("Connect", R.drawable.ic_dialog_connect, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
        materialDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(ForgotPasswordActivity.this, "fadein-to-fadeout");
    }
}