package com.developerdepository.meetin.Activities;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.chaos.view.PinView;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.developerdepository.meetin.R;
import com.developerdepository.meetin.Utilities.Constants;
import com.developerdepository.meetin.Utilities.PreferenceManager;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.util.HashMap;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import maes.tech.intentanim.CustomIntent;

public class VerifyOTPActivity extends AppCompatActivity {

    //View Variables
    private ImageView closeBtn;
    private TextView verifyOtpSubtitle, resendOtp;
    private PinView otpPinView;
    private ConstraintLayout verifyBtn;
    private CardView verifyBtnContainer;
    private ProgressBar verifyOtpProgressBar;

    //Firebase Variables
    private FirebaseAuth firebaseAuth;
    private CollectionReference userRef;
    private StorageReference storageReference;
    private PhoneAuthProvider.ForceResendingToken resendingToken;

    //Other Variables
    private String name, email, mobile, password, username, about, image;
    private String codeBySystem;
    private Timer timer;
    private int count = 60;
    private Uri imageUri = null;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(VerifyOTPActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        email = intent.getStringExtra("email");
        mobile = intent.getStringExtra("mobile");
        password = intent.getStringExtra("password");
        username = intent.getStringExtra("username");
        about = intent.getStringExtra("about");
        image = intent.getStringExtra("image");

        initViews();
        initFirebase();
        setActionOnViews();

        KeyboardVisibilityEvent.setEventListener(VerifyOTPActivity.this, isOpen -> {
            if (!isOpen) {
                otpPinView.clearFocus();
            }
        });

        sendVerificationCodeToUser(mobile);
    }

    private void initViews() {
        closeBtn = findViewById(R.id.close_btn);
        verifyOtpSubtitle = findViewById(R.id.verify_otp_subtitle);
        resendOtp = findViewById(R.id.resend_otp);
        otpPinView = findViewById(R.id.verify_otp_pinview);
        verifyBtn = findViewById(R.id.verify_btn);
        verifyBtnContainer = findViewById(R.id.verify_btn_container);
        verifyOtpProgressBar = findViewById(R.id.verify_otp_progress_bar);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
        storageReference = FirebaseStorage.getInstance().getReference("UserProfilePics/");
    }

    private void setActionOnViews() {
        closeBtn.setOnClickListener(v -> onBackPressed());

        verifyOtpSubtitle.setText(String.format("Enter below the verification code (OTP) sent to %s.", mobile));

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                VerifyOTPActivity.this.runOnUiThread(() -> {
                    if (count == 0) {
                        resendOtp.setText("Resend OTP");
                        resendOtp.setAlpha(1.0f);
                        resendOtp.setEnabled(true);
                        resendOtp.setOnClickListener(v -> {
                            if (!isConnectedToInternet(VerifyOTPActivity.this)) {
                                showConnectToInternetDialog();
                                return;
                            } else {
                                resendOTP();
                                resendOtp.setEnabled(false);
                                resendOtp.setAlpha(0.5f);
                                count = 60;
                            }
                        });
                    } else {
                        resendOtp.setText(String.format("Resend OTP in %d", count));
                        resendOtp.setAlpha(0.5f);
                        resendOtp.setEnabled(false);
                        count--;
                    }
                });
            }
        }, 0, 1000);

        String codeEnteredByUser = Objects.requireNonNull(otpPinView.getText()).toString();

        if (codeEnteredByUser.length() == 6) {
            verifyOtpProgressBar.setVisibility(View.INVISIBLE);
            verifyBtnContainer.setVisibility(View.VISIBLE);
            verifyBtn.setEnabled(true);
            verifyBtn.setOnClickListener(view -> {
                UIUtil.hideKeyboard(VerifyOTPActivity.this);
                if (!isConnectedToInternet(VerifyOTPActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    verifyCode(codeEnteredByUser);
                }
            });
        } else {
            verifyOtpProgressBar.setVisibility(View.INVISIBLE);
            verifyBtnContainer.setVisibility(View.VISIBLE);
            verifyBtn.setEnabled(true);
            verifyBtn.setOnClickListener(view -> {
                UIUtil.hideKeyboard(VerifyOTPActivity.this);
                YoYo.with(Techniques.Shake)
                        .duration(700)
                        .repeat(0)
                        .playOn(otpPinView);
                Alerter.create(VerifyOTPActivity.this)
                        .setText("Provide in the valid OTP received on " + mobile + "!")
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
            });
        }
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    super.onCodeSent(s, forceResendingToken);
                    resendingToken = forceResendingToken;
                    codeBySystem = s;
                }

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                    String code = phoneAuthCredential.getSmsCode();
                    if (code != null) {
                        otpPinView.setText(code);
                        verifyCode(code);
                    }
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                        verifyBtnContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setEnabled(true);

                        Alerter.create(VerifyOTPActivity.this)
                                .setText("Whoa! It seems you've got an invalid code!")
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
                    } else if (e instanceof FirebaseTooManyRequestsException) {
                        verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                        verifyBtnContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setEnabled(true);

                        Alerter.create(VerifyOTPActivity.this)
                                .setText("Too many requests at the moment. Try again after some time!")
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
                }
            };

    private void sendVerificationCodeToUser(String mobile) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobile,
                60,
                TimeUnit.SECONDS,
                VerifyOTPActivity.this,
                mCallbacks
        );
    }

    private void resendOTP() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mobile,
                60,
                TimeUnit.SECONDS,
                VerifyOTPActivity.this,
                mCallbacks, resendingToken);
    }

    private void verifyCode(String code) {
        verifyBtnContainer.setVisibility(View.INVISIBLE);
        verifyBtn.setEnabled(false);
        verifyOtpProgressBar.setVisibility(View.VISIBLE);
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codeBySystem, code);
        signInUsingCredential(credential);
    }

    private void signInUsingCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = task.getResult().getUser();
                        AuthCredential authCredential = EmailAuthProvider.getCredential(email, password);
                        assert user != null;
                        user.linkWithCredential(authCredential).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                if (!image.isEmpty() && image.length() != 0 && image != null && !image.equals("")) {
                                    imageUri = Uri.parse(image);

                                    final StorageReference fileRef = storageReference.child(username + ".img");

                                    fileRef.putFile(imageUri)
                                            .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                                                    .addOnSuccessListener(uri -> {
                                                        final String imageValue = uri.toString();

                                                        HashMap<String, Object> newUser = new HashMap<>();
                                                        newUser.put(Constants.KEY_NAME, name);
                                                        newUser.put(Constants.KEY_EMAIL, email);
                                                        newUser.put(Constants.KEY_MOBILE, mobile);
                                                        newUser.put(Constants.KEY_USERNAME, username);
                                                        newUser.put(Constants.KEY_ABOUT, about);
                                                        newUser.put(Constants.KEY_IMAGE, imageValue);

                                                        userRef.document(email).set(newUser)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    timer.cancel();

                                                                    verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                                                    verifyBtnContainer.setVisibility(View.VISIBLE);
                                                                    verifyBtn.setEnabled(true);

                                                                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                                                    preferenceManager.putString(Constants.KEY_NAME, name);
                                                                    preferenceManager.putString(Constants.KEY_EMAIL, email);
                                                                    preferenceManager.putString(Constants.KEY_MOBILE, mobile);
                                                                    preferenceManager.putString(Constants.KEY_USERNAME, username);
                                                                    preferenceManager.putString(Constants.KEY_ABOUT, about);
                                                                    preferenceManager.putString(Constants.KEY_IMAGE, imageValue);
                                                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                    startActivity(intent);
                                                                    CustomIntent.customType(VerifyOTPActivity.this, "fadein-to-fadeout");
                                                                    finish();
                                                                }).addOnFailureListener(e -> {
                                                            verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                                            verifyBtnContainer.setVisibility(View.VISIBLE);
                                                            verifyBtn.setEnabled(true);

                                                            Alerter.create(VerifyOTPActivity.this)
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
                                                    }).addOnFailureListener(e -> {
                                                verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                                verifyBtnContainer.setVisibility(View.VISIBLE);
                                                verifyBtn.setEnabled(true);

                                                Alerter.create(VerifyOTPActivity.this)
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
                                            })).addOnFailureListener(e -> {
                                        verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                        verifyBtnContainer.setVisibility(View.VISIBLE);
                                        verifyBtn.setEnabled(true);

                                        Alerter.create(VerifyOTPActivity.this)
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
                                    HashMap<String, Object> newUser = new HashMap<>();
                                    newUser.put(Constants.KEY_NAME, name);
                                    newUser.put(Constants.KEY_EMAIL, email);
                                    newUser.put(Constants.KEY_MOBILE, mobile);
                                    newUser.put(Constants.KEY_USERNAME, username);
                                    newUser.put(Constants.KEY_ABOUT, about);
                                    newUser.put(Constants.KEY_IMAGE, "");

                                    userRef.document(email).set(newUser)
                                            .addOnSuccessListener(aVoid -> {
                                                timer.cancel();

                                                verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                                verifyBtnContainer.setVisibility(View.VISIBLE);
                                                verifyBtn.setEnabled(true);

                                                preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                                                preferenceManager.putString(Constants.KEY_NAME, name);
                                                preferenceManager.putString(Constants.KEY_EMAIL, email);
                                                preferenceManager.putString(Constants.KEY_MOBILE, mobile);
                                                preferenceManager.putString(Constants.KEY_USERNAME, username);
                                                preferenceManager.putString(Constants.KEY_ABOUT, about);
                                                preferenceManager.putString(Constants.KEY_IMAGE, "");
                                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                                CustomIntent.customType(VerifyOTPActivity.this, "fadein-to-fadeout");
                                                finish();
                                            }).addOnFailureListener(e -> {
                                        verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                        verifyBtnContainer.setVisibility(View.VISIBLE);
                                        verifyBtn.setEnabled(true);

                                        Alerter.create(VerifyOTPActivity.this)
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
                                verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                                verifyBtnContainer.setVisibility(View.VISIBLE);
                                verifyBtn.setEnabled(true);

                                Alerter.create(VerifyOTPActivity.this)
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
                            verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                            verifyBtnContainer.setVisibility(View.VISIBLE);
                            verifyBtn.setEnabled(true);

                            Alerter.create(VerifyOTPActivity.this)
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
                        verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                        verifyBtnContainer.setVisibility(View.VISIBLE);
                        verifyBtn.setEnabled(true);

                        Alerter.create(VerifyOTPActivity.this)
                                .setText("Whoa! Code verification failed. Try again!")
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
                    verifyOtpProgressBar.setVisibility(View.INVISIBLE);
                    verifyBtnContainer.setVisibility(View.VISIBLE);
                    verifyBtn.setEnabled(true);

                    Alerter.create(VerifyOTPActivity.this)
                            .setText("Whoa! Code verification failed. Try again!")
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

    private boolean isConnectedToInternet(VerifyOTPActivity verifyOTPActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) verifyOTPActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(VerifyOTPActivity.this)
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
    protected void onStop() {
        super.onStop();
        timer.cancel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        timer.cancel();
        CustomIntent.customType(VerifyOTPActivity.this, "fadein-to-fadeout");
    }
}