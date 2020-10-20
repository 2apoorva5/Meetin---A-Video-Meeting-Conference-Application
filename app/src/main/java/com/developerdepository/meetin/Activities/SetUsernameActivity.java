package com.developerdepository.meetin.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.developerdepository.meetin.R;
import com.developerdepository.meetin.Utilities.Constants;
import com.developerdepository.meetin.Utilities.PreferenceManager;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shreyaspatil.MaterialDialog.MaterialDialog;
import com.tapadoo.alerter.Alerter;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import de.hdodenhof.circleimageview.CircleImageView;
import maes.tech.intentanim.CustomIntent;

public class SetUsernameActivity extends AppCompatActivity {

    //View Variables
    private ImageView backBtn;
    private CircleImageView userProfilePic, choosePhoto;
    private TextInputLayout usernameField, aboutField;
    private TextView signInBtn;
    private ConstraintLayout signUpBtn;
    private CardView signUpBtnContainer;
    private ProgressBar setUsernameProgressBar;

    //Firebase Variables
    private CollectionReference userRef;

    //Other Variables
    private PreferenceManager preferenceManager;
    private Uri imageUri = null;
    private String name, email, mobile, password;

    private final static String USERNAME_PATTERN = "^(?=\\S+$)[a-z0-9_.]{4,20}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_username);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(SetUsernameActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));

        Intent intent = getIntent();
        name = intent.getStringExtra("name");
        email = intent.getStringExtra("email");
        mobile = intent.getStringExtra("mobile");
        password = intent.getStringExtra("password");

        initViews();
        initFirebase();
        setActionOnViews();

        KeyboardVisibilityEvent.setEventListener(SetUsernameActivity.this, isOpen -> {
            if (!isOpen) {
                usernameField.clearFocus();
                aboutField.clearFocus();
            }
        });
    }

    private void initViews() {
        backBtn = findViewById(R.id.back_btn);
        choosePhoto = findViewById(R.id.choose_photo);
        userProfilePic = findViewById(R.id.user_profile_pic);
        usernameField = findViewById(R.id.set_username_field);
        aboutField = findViewById(R.id.set_username_about_field);
        signUpBtn = findViewById(R.id.sign_up_btn);
        signUpBtnContainer = findViewById(R.id.sign_up_btn_container);
        setUsernameProgressBar = findViewById(R.id.set_username_progress_bar);
        signInBtn = findViewById(R.id.sign_in_btn);
    }

    private void initFirebase() {
        userRef = FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS);
    }


    private void setActionOnViews() {
        backBtn.setOnClickListener(view -> onBackPressed());

        choosePhoto.setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog.Builder(SetUsernameActivity.this)
                    .setTitle("Edit Profile Photo?")
                    .setMessage("Choose an action to continue!")
                    .setCancelable(false)
                    .setAnimation(R.raw.choose_photo)
                    .setPositiveButton("Edit", R.drawable.ic_dialog_camera, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        selectImage();
                    })
                    .setNegativeButton("Remove", R.drawable.ic_dialog_remove, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        imageUri = null;
                        userProfilePic.setImageResource(R.drawable.illustration_user_avatar);
                    }).build();
            dialog.show();
        });

        signInBtn.setOnClickListener(view -> {
            setUsernameProgressBar.setVisibility(View.INVISIBLE);
            signUpBtnContainer.setVisibility(View.VISIBLE);
            signUpBtn.setEnabled(true);

            startActivity(new Intent(SetUsernameActivity.this, SignInActivity.class));
            CustomIntent.customType(SetUsernameActivity.this, "fadein-to-fadeout");
            finish();
        });

        usernameField.getEditText().addTextChangedListener(usernameTextWatcher);
    }

    private TextWatcher usernameTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            final String username = usernameField.getEditText().getText().toString().trim();

            if (!username.isEmpty()) {
                setUsernameProgressBar.setVisibility(View.INVISIBLE);
                signUpBtnContainer.setVisibility(View.VISIBLE);
                signUpBtn.setEnabled(true);
                signUpBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(SetUsernameActivity.this);
                    if (!username.matches(USERNAME_PATTERN)) {
                        YoYo.with(Techniques.Shake)
                                .duration(700)
                                .repeat(0)
                                .playOn(usernameField);
                        usernameField.setError("Should be of 4-20 characters with only lowercase letters (no white spaces). Digits, (_) & (.) allowed.");
                        return;
                    } else {
                        usernameField.setError(null);
                        usernameField.setErrorEnabled(false);
                        if (!isConnectedToInternet(SetUsernameActivity.this)) {
                            showConnectToInternetDialog();
                            return;
                        } else {
                            signUpBtnContainer.setVisibility(View.INVISIBLE);
                            signUpBtn.setEnabled(false);
                            setUsernameProgressBar.setVisibility(View.VISIBLE);

                            userRef.whereEqualTo(Constants.KEY_USERNAME, username)
                                    .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    if (task.getResult().getDocuments().isEmpty() &&
                                            task.getResult().getDocuments().size() == 0) {
                                        openVerifyOTP();
                                    } else {
                                        setUsernameProgressBar.setVisibility(View.INVISIBLE);
                                        signUpBtnContainer.setVisibility(View.VISIBLE);
                                        signUpBtn.setEnabled(true);

                                        YoYo.with(Techniques.Shake)
                                                .duration(700)
                                                .repeat(0)
                                                .playOn(usernameField);
                                        Alerter.create(SetUsernameActivity.this)
                                                .setText("That username is already taken. Try another!")
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
                                    setUsernameProgressBar.setVisibility(View.INVISIBLE);
                                    signUpBtnContainer.setVisibility(View.VISIBLE);
                                    signUpBtn.setEnabled(true);

                                    Alerter.create(SetUsernameActivity.this)
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
                                setUsernameProgressBar.setVisibility(View.INVISIBLE);
                                signUpBtnContainer.setVisibility(View.VISIBLE);
                                signUpBtn.setEnabled(true);

                                Alerter.create(SetUsernameActivity.this)
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
                setUsernameProgressBar.setVisibility(View.INVISIBLE);
                signUpBtnContainer.setVisibility(View.VISIBLE);
                signUpBtn.setEnabled(true);
                signUpBtn.setOnClickListener(view -> {
                    UIUtil.hideKeyboard(SetUsernameActivity.this);
                    YoYo.with(Techniques.Shake)
                            .duration(700)
                            .repeat(0)
                            .playOn(usernameField);
                    usernameField.setError("Enter your username first!");
                });
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    };

    private void selectImage() {
        ImagePicker.Companion.with(SetUsernameActivity.this)
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            imageUri = data.getData();
            Glide.with(SetUsernameActivity.this).load(imageUri).centerCrop().into(userProfilePic);
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Alerter.create(SetUsernameActivity.this)
                    .setText("Whoa! That ran into some error.")
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
            return;
        }
    }

    private void openVerifyOTP() {
        final String username = usernameField.getEditText().getText().toString().trim();
        final String about = aboutField.getEditText().getText().toString().trim();

        if (imageUri != null) {
            setUsernameProgressBar.setVisibility(View.INVISIBLE);
            signUpBtnContainer.setVisibility(View.VISIBLE);
            signUpBtn.setEnabled(true);

            Intent intent = new Intent(SetUsernameActivity.this, VerifyOTPActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("email", email);
            intent.putExtra("mobile", mobile);
            intent.putExtra("password", password);
            intent.putExtra("username", username);
            intent.putExtra("about", about);
            intent.putExtra("image", imageUri.toString());
            startActivity(intent);
            CustomIntent.customType(SetUsernameActivity.this, "fadein-to-fadeout");
        } else {
            setUsernameProgressBar.setVisibility(View.INVISIBLE);
            signUpBtnContainer.setVisibility(View.VISIBLE);
            signUpBtn.setEnabled(true);

            Intent intent = new Intent(SetUsernameActivity.this, VerifyOTPActivity.class);
            intent.putExtra("name", name);
            intent.putExtra("email", email);
            intent.putExtra("mobile", mobile);
            intent.putExtra("password", password);
            intent.putExtra("username", username);
            intent.putExtra("about", about);
            intent.putExtra("image", "");
            startActivity(intent);
            CustomIntent.customType(SetUsernameActivity.this, "fadein-to-fadeout");
        }
    }

    private boolean isConnectedToInternet(SetUsernameActivity setUsernameActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) setUsernameActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(SetUsernameActivity.this)
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
        CustomIntent.customType(SetUsernameActivity.this, "fadein-to-fadeout");
    }
}