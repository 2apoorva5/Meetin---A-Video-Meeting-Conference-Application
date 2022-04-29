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
import com.google.firebase.firestore.FirebaseFirestore;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.regex.Pattern;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class SignUpActivity extends AppCompatActivity {

    //View Variables
    private ImageView closeBtn;
    private TextInputLayout nameField, emailField, mobileField, createPasswordField, confirmPasswordField;
    private TextView signInBtn;
    private ConstraintLayout nextBtn;
    private CardView nextBtnContainer;
    private ProgressBar signUpProgressBar;

    //Firebase Variables
    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef;

    //Other Variables
    private PreferenceManager preferenceManager;

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^" +
                    "(?=.*[0-9])" +                 //at least 1 digit
                    "(?=.*[a-z])" +                 //at least 1 lowercase letter
                    "(?=.*[A-Z])" +                 //at least 1 uppercase letter
                    "(?=.*[!@#$%^&*+=_])" +         //at least 1 special character
                    "(?=\\S+$)" +                   //no white spaces
                    ".{6,}" +                       //at least 6-character long
                    "$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SignUpActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));

        initViews();
        initFirebase();
        setActionOnViews();

        KeyboardVisibilityEvent.setEventListener(SignUpActivity.this, isOpen -> {
            if (!isOpen) {
                nameField.clearFocus();
                emailField.clearFocus();
                mobileField.clearFocus();
                createPasswordField.clearFocus();
                confirmPasswordField.clearFocus();
            }
        });
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        nameField = findViewById(R.id.sign_up_name_field);
        emailField = findViewById(R.id.sign_up_email_field);
        mobileField = findViewById(R.id.sign_up_mobile_field);
        createPasswordField = findViewById(R.id.sign_up_create_password_field);
        confirmPasswordField = findViewById(R.id.sign_up_confirm_password_field);
        nextBtn = findViewById(R.id.next_btn);
        nextBtnContainer = findViewById(R.id.next_btn_container);
        signUpProgressBar = findViewById(R.id.sign_up_progress_bar);
        signInBtn = findViewById(R.id.sign_in_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(view -> onBackPressed());

        signInBtn.setOnClickListener(view -> {
            signUpProgressBar.setVisibility(View.INVISIBLE);
            nextBtnContainer.setVisibility(View.VISIBLE);
            nextBtn.setEnabled(true);

            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            CustomIntent.customType(SignUpActivity.this, "fadein-to-fadeout");
            finish();
        });

        nameField.getEditText().addTextChangedListener(signupTextWatcher);
        emailField.getEditText().addTextChangedListener(signupTextWatcher);
        mobileField.getEditText().addTextChangedListener(signupTextWatcher);
        createPasswordField.getEditText().addTextChangedListener(signupTextWatcher);
        confirmPasswordField.getEditText().addTextChangedListener(signupTextWatcher);
    }

    private TextWatcher signupTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            final String name = nameField.getEditText().getText().toString().trim();
            final String email = emailField.getEditText().getText().toString().trim();
            final String mobile = mobileField.getEditText().getText().toString().trim();
            final String createPassword = createPasswordField.getEditText().getText().toString().trim();
            final String confirmPassword = confirmPasswordField.getEditText().getText().toString().trim();

            if (!name.isEmpty() && !email.isEmpty() && !mobile.isEmpty() && !createPassword.isEmpty() && !confirmPassword.isEmpty()) {
                signUpProgressBar.setVisibility(View.INVISIBLE);
                nextBtnContainer.setVisibility(View.VISIBLE);
                nextBtn.setEnabled(true);
                nextBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(SignUpActivity.this);
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(emailField);
                        emailField.setError("Enter a valid email!");
                        return;
                    } else if (mobile.length() != 10) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(mobileField);
                        mobileField.setError("Enter a valid mobile number!");
                        return;
                    } else if (!PASSWORD_PATTERN.matcher(createPassword).matches()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(createPasswordField);
                        createPasswordField.setError("Minimum 6 characters with at least 1 digit, 1 lowercase & 1 uppercase letter, 1 special character and no white spaces.");
                        return;
                    } else if (!createPassword.equals(confirmPassword)) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(confirmPasswordField);
                        confirmPasswordField.setError("Didn't match the above password!");
                        return;
                    } else {
                        nameField.setError(null);
                        emailField.setError(null);
                        mobileField.setError(null);
                        createPasswordField.setError(null);
                        confirmPasswordField.setError(null);
                        nameField.setErrorEnabled(false);
                        emailField.setErrorEnabled(false);
                        mobileField.setErrorEnabled(false);
                        createPasswordField.setErrorEnabled(false);
                        confirmPasswordField.setErrorEnabled(false);

                        if (!isConnectedToInternet(SignUpActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            nextBtnContainer.setVisibility(View.INVISIBLE);
                            nextBtn.setEnabled(false);
                            signUpProgressBar.setVisibility(View.VISIBLE);

                            firebaseAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (task.getResult().getSignInMethods().isEmpty() &&
                                            task.getResult().getSignInMethods().size() == 0) {
                                        userRef.whereEqualTo(Constants.KEY_MOBILE, "+91" + mobile)
                                                .get().addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()) {
                                                if (task1.getResult().getDocuments().isEmpty() &&
                                                        task1.getResult().getDocuments().size() == 0) {
                                                    signUpProgressBar.setVisibility(View.INVISIBLE);
                                                    nextBtnContainer.setVisibility(View.VISIBLE);
                                                    nextBtn.setEnabled(true);

                                                    Intent intent = new Intent(SignUpActivity.this, SetUsernameActivity.class);
                                                    intent.putExtra("name", name);
                                                    intent.putExtra("email", email);
                                                    intent.putExtra("mobile", "+91" + mobile);
                                                    intent.putExtra("password", createPassword);
                                                    startActivity(intent);
                                                    CustomIntent.customType(SignUpActivity.this, "fadein-to-fadeout");
                                                } else {
                                                    signUpProgressBar.setVisibility(View.INVISIBLE);
                                                    nextBtnContainer.setVisibility(View.VISIBLE);
                                                    nextBtn.setEnabled(true);

                                                    YoYo.with(Techniques.Shake)
                                                            .duration(700)
                                                            .repeat(0)
                                                            .playOn(mobileField);
                                                    Alerter.create(SignUpActivity.this)
                                                            .setText("That mobile has already been registered. Try another!")
                                                            .setTextAppearance(R.style.InfoAlert)
                                                            .setBackgroundColorRes(R.color.infoColor)
                                                            .setIcon(R.drawable.ic_info)
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
                                                signUpProgressBar.setVisibility(View.INVISIBLE);
                                                nextBtnContainer.setVisibility(View.VISIBLE);
                                                nextBtn.setEnabled(true);

                                                Alerter.create(SignUpActivity.this)
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
                                            signUpProgressBar.setVisibility(View.INVISIBLE);
                                            nextBtnContainer.setVisibility(View.VISIBLE);
                                            nextBtn.setEnabled(true);

                                            Alerter.create(SignUpActivity.this)
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
                                        signUpProgressBar.setVisibility(View.INVISIBLE);
                                        nextBtnContainer.setVisibility(View.VISIBLE);
                                        nextBtn.setEnabled(true);

                                        YoYo.with(Techniques.Shake)
                                                .duration(700)
                                                .repeat(0)
                                                .playOn(emailField);
                                        Alerter.create(SignUpActivity.this)
                                                .setText("That email has already been registered. Try another!")
                                                .setTextAppearance(R.style.InfoAlert)
                                                .setBackgroundColorRes(R.color.infoColor)
                                                .setIcon(R.drawable.ic_info)
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
                                    signUpProgressBar.setVisibility(View.INVISIBLE);
                                    nextBtnContainer.setVisibility(View.VISIBLE);
                                    nextBtn.setEnabled(true);

                                    Alerter.create(SignUpActivity.this)
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
                                signUpProgressBar.setVisibility(View.INVISIBLE);
                                nextBtnContainer.setVisibility(View.VISIBLE);
                                nextBtn.setEnabled(true);

                                Alerter.create(SignUpActivity.this)
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
                signUpProgressBar.setVisibility(View.INVISIBLE);
                nextBtnContainer.setVisibility(View.VISIBLE);
                nextBtn.setEnabled(true);
                nextBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(SignUpActivity.this);
                    if (name.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(nameField);
                        nameField.setError("We want a name to setup your account!");
                    } else if (email.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(emailField);
                        emailField.setError("Provide your email!");
                    } else if (mobile.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(mobileField);
                        mobileField.setError("Provide your mobile number!");
                    } else if (createPassword.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(createPasswordField);
                        createPasswordField.setError("Setup a password for your account!");
                    } else if (confirmPassword.isEmpty()) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(confirmPasswordField);
                        confirmPasswordField.setError("Confirm your password!");
                    }
                });
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private boolean isConnectedToInternet(SignUpActivity signUpActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) signUpActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressLint("MissingPermission") NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SignUpActivity.this)
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