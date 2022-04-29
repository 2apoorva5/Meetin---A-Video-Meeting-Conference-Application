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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.developerdepository.meetin.R;
import com.developerdepository.meetin.Utilities.Constants;
import com.developerdepository.meetin.Utilities.PreferenceManager;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.List;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class SignInActivity extends AppCompatActivity {

    //View Variables
    private ImageView closeBtn;
    private TextInputLayout emailOrMobileField, passwordField;
    private TextView forgotPassword, signUpBtn;
    private ConstraintLayout signInBtn;
    private CardView signInBtnContainer;
    private ProgressBar signInProgressBar;

    //Firebase Variables
    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef;

    //Other Variables
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SignInActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));

        initViews();
        initFirebase();
        setActionOnViews();

        KeyboardVisibilityEvent.setEventListener(SignInActivity.this, isOpen -> {
            if (!isOpen) {
                emailOrMobileField.clearFocus();
                passwordField.clearFocus();
            }
        });
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        emailOrMobileField = findViewById(R.id.sign_in_email_or_mobile_field);
        passwordField = findViewById(R.id.sign_in_password_field);
        forgotPassword = findViewById(R.id.forgot_password);
        signUpBtn = findViewById(R.id.sign_up_btn);
        signInBtn = findViewById(R.id.sign_in_btn);
        signInBtnContainer = findViewById(R.id.sign_in_btn_container);
        signInProgressBar = findViewById(R.id.sign_in_progress_bar);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        forgotPassword.setOnClickListener(view -> {
            signInProgressBar.setVisibility(View.INVISIBLE);
            signInBtnContainer.setVisibility(View.VISIBLE);
            signInBtn.setEnabled(true);

            startActivity(new Intent(SignInActivity.this, ForgotPasswordActivity.class));
            CustomIntent.customType(SignInActivity.this, "fadein-to-fadeout");
        });

        signUpBtn.setOnClickListener(view -> {
            signInProgressBar.setVisibility(View.INVISIBLE);
            signInBtnContainer.setVisibility(View.VISIBLE);
            signInBtn.setEnabled(true);

            startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
            CustomIntent.customType(SignInActivity.this, "fadein-to-fadeout");
            finish();
        });

        emailOrMobileField.getEditText().addTextChangedListener(signinTextWatcher);
        passwordField.getEditText().addTextChangedListener(signinTextWatcher);
    }

    private TextWatcher signinTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            final String emailOrMobile = emailOrMobileField.getEditText().getText().toString().trim();
            final String password = passwordField.getEditText().getText().toString().trim();

            if (!emailOrMobile.isEmpty() && !password.isEmpty()) {
                signInProgressBar.setVisibility(View.INVISIBLE);
                signInBtnContainer.setVisibility(View.VISIBLE);
                signInBtn.setEnabled(true);
                signInBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(SignInActivity.this);
                    if (Patterns.EMAIL_ADDRESS.matcher(emailOrMobile).matches()) {
                        emailOrMobileField.setError(null);
                        passwordField.setError(null);
                        emailOrMobileField.setErrorEnabled(false);
                        passwordField.setErrorEnabled(false);

                        if (!isConnectedToInternet(SignInActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            login(emailOrMobile);
                        }
                    } else if (emailOrMobile.matches("\\d{10}")) {
                        emailOrMobileField.setError(null);
                        passwordField.setError(null);
                        emailOrMobileField.setErrorEnabled(false);
                        passwordField.setErrorEnabled(false);

                        if (!isConnectedToInternet(SignInActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            signInBtnContainer.setVisibility(View.INVISIBLE);
                            signInBtn.setEnabled(false);
                            signInProgressBar.setVisibility(View.VISIBLE);

                            userRef.whereEqualTo(Constants.KEY_MOBILE, "+91" + emailOrMobile)
                                    .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    List<DocumentSnapshot> documentSnapshots = task.getResult().getDocuments();
                                    if (documentSnapshots.isEmpty()) {
                                        signInProgressBar.setVisibility(View.INVISIBLE);
                                        signInBtnContainer.setVisibility(View.VISIBLE);
                                        signInBtn.setEnabled(true);

                                        YoYo.with(Techniques.Shake)
                                                .duration(700)
                                                .repeat(0)
                                                .playOn(emailOrMobileField);
                                        Alerter.create(SignInActivity.this)
                                                .setText("We didn't find any account with that mobile number!")
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
                                    } else {
                                        String email = documentSnapshots.get(0).get(Constants.KEY_EMAIL).toString().trim();
                                        login(email);
                                    }
                                } else {
                                    signInProgressBar.setVisibility(View.INVISIBLE);
                                    signInBtnContainer.setVisibility(View.VISIBLE);
                                    signInBtn.setEnabled(true);

                                    Alerter.create(SignInActivity.this)
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
                                signInProgressBar.setVisibility(View.INVISIBLE);
                                signInBtnContainer.setVisibility(View.VISIBLE);
                                signInBtn.setEnabled(true);

                                Alerter.create(SignInActivity.this)
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
                    } else {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(emailOrMobileField);
                        emailOrMobileField.setError("Enter a valid email or mobile!");
                    }
                });
            } else {
                signInProgressBar.setVisibility(View.INVISIBLE);
                signInBtnContainer.setVisibility(View.VISIBLE);
                signInBtn.setEnabled(true);
                signInBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(SignInActivity.this);
                    if (emailOrMobile.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(emailOrMobileField);
                        emailOrMobileField.setError("Enter your email or mobile number!");
                    } else if (password.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(passwordField);
                        passwordField.setError("Without a password? Ehh!");
                    }
                });
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void login(final String email) {
        signInBtnContainer.setVisibility(View.INVISIBLE);
        signInBtn.setEnabled(false);
        signInProgressBar.setVisibility(View.VISIBLE);

        firebaseAuth.signInWithEmailAndPassword(email, passwordField.getEditText().getText().toString().trim())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userRef.document(email)
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        signInProgressBar.setVisibility(View.INVISIBLE);
                                        signInBtnContainer.setVisibility(View.VISIBLE);
                                        signInBtn.setEnabled(true);

                                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                        preferenceManager.putString(Constants.KEY_NAME, task1.getResult().getString(Constants.KEY_NAME));
                                        preferenceManager.putString(Constants.KEY_EMAIL, task1.getResult().getString(Constants.KEY_EMAIL));
                                        preferenceManager.putString(Constants.KEY_MOBILE, task1.getResult().getString(Constants.KEY_MOBILE));
                                        preferenceManager.putString(Constants.KEY_USERNAME, task1.getResult().getString(Constants.KEY_USERNAME));
                                        preferenceManager.putString(Constants.KEY_ABOUT, task1.getResult().getString(Constants.KEY_ABOUT));
                                        preferenceManager.putString(Constants.KEY_IMAGE, task1.getResult().getString(Constants.KEY_IMAGE));
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        CustomIntent.customType(SignInActivity.this, "fadein-to-fadeout");
                                        finish();
                                    } else {
                                        signInProgressBar.setVisibility(View.INVISIBLE);
                                        signInBtnContainer.setVisibility(View.VISIBLE);
                                        signInBtn.setEnabled(true);

                                        Alerter.create(SignInActivity.this)
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
                            signInProgressBar.setVisibility(View.INVISIBLE);
                            signInBtnContainer.setVisibility(View.VISIBLE);
                            signInBtn.setEnabled(true);

                            Alerter.create(SignInActivity.this)
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
                        signInProgressBar.setVisibility(View.INVISIBLE);
                        signInBtnContainer.setVisibility(View.VISIBLE);
                        signInBtn.setEnabled(true);

                        Alerter.create(SignInActivity.this)
                                .setText("Whoa! It seems you've got invalid credentials. Try again!")
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
            signInProgressBar.setVisibility(View.INVISIBLE);
            signInBtnContainer.setVisibility(View.VISIBLE);
            signInBtn.setEnabled(true);

            Alerter.create(SignInActivity.this)
                    .setText("Whoa! It seems you've got invalid credentials. Try again!")
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

    private boolean isConnectedToInternet(SignInActivity signInActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) signInActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressLint("MissingPermission") NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SignInActivity.this)
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
        finishAffinity();
    }
}