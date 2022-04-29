package com.developerdepository.meetin.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.developerdepository.meetin.Adapters.UserAdapter;
import com.developerdepository.meetin.Helper.LoadingDialog;
import com.developerdepository.meetin.Listeners.UsersListener;
import com.developerdepository.meetin.Models.User;
import com.developerdepository.meetin.R;
import com.developerdepository.meetin.Utilities.Constants;
import com.developerdepository.meetin.Utilities.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.tapadoo.alerter.Alerter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import maes.tech.intentanim.CustomIntent;

public class MainActivity extends AppCompatActivity implements UsersListener {

    //View Variables
    private CircleImageView userProfilePic;
    private TextView homeTitle, userFirstChar, signOutBtn;
    private RecyclerView userRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton conferenceBtn;

    //Firebase Variables
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    //Other Variables
    private PreferenceManager preferenceManager;
    private List<User> users;
    private UserAdapter userAdapter;
    private LoadingDialog loadingDialog;

    private int REQUEST_CODE_BATTERY_OPTIMIZATIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            finish();
        }

        loadingDialog = new LoadingDialog(MainActivity.this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorPrimary));

        initViews();
        initFirebase();
        setActionOnViews();

        sendFCMTokenToDatabase();

        users = new ArrayList<>();
        userAdapter = new UserAdapter(users, this);
        userRecyclerView.setAdapter(userAdapter);

        swipeRefreshLayout.setColorSchemeColors(getColor(R.color.colorPrimary), getColor(R.color.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(this::getUsers);

        getUsers();

        checkForBatteryOptimizations();
    }

    private void initViews() {
        userProfilePic = findViewById(R.id.user_profile_pic);
        userFirstChar = findViewById(R.id.user_name_first_char);
        signOutBtn = findViewById(R.id.sign_out_btn);
        homeTitle = findViewById(R.id.home_title);
        userRecyclerView = findViewById(R.id.user_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        conferenceBtn = findViewById(R.id.conference_btn);
    }

    private void initFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    private void setActionOnViews() {
        signOutBtn.setOnClickListener(view -> {
            if (!isConnectedToInternet(MainActivity.this)) {
                showConnectToInternetDialog();
                return;
            } else {
                signOut();
            }
        });

        //Get the time of day
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int timeOfDay = cal.get(Calendar.HOUR_OF_DAY);

        String name = preferenceManager.getString(Constants.KEY_NAME);
        String[] splitName = name.split(" ", 2);

        if (timeOfDay >= 0 && timeOfDay < 4) {
            homeTitle.setText(String.format("Night, %s", splitName[0]));
        } else if (timeOfDay >= 4 && timeOfDay < 12) {
            homeTitle.setText(String.format("Morning, %s", splitName[0]));
        } else if (timeOfDay >= 12 && timeOfDay < 17) {
            homeTitle.setText(String.format("Afternoon, %s", splitName[0]));
        } else if (timeOfDay >= 17 && timeOfDay < 21) {
            homeTitle.setText(String.format("Evening, %s", splitName[0]));
        } else if (timeOfDay >= 21 && timeOfDay <= 24) {
            homeTitle.setText(String.format("Night, %s", splitName[0]));
        }

        if (preferenceManager.getString(Constants.KEY_IMAGE) != null &&
                !preferenceManager.getString(Constants.KEY_IMAGE).isEmpty() &&
                preferenceManager.getString(Constants.KEY_IMAGE).length() != 0 &&
                !preferenceManager.getString(Constants.KEY_IMAGE).equals("")) {
            userFirstChar.setVisibility(View.INVISIBLE);
            userFirstChar.setEnabled(false);
            userProfilePic.setVisibility(View.VISIBLE);
            userProfilePic.setEnabled(true);
            Glide.with(MainActivity.this).load(Uri.parse(preferenceManager.getString(Constants.KEY_IMAGE))).centerCrop().into(userProfilePic);
        } else {
            userProfilePic.setVisibility(View.INVISIBLE);
            userProfilePic.setEnabled(false);
            userFirstChar.setVisibility(View.VISIBLE);
            userFirstChar.setEnabled(true);
            userFirstChar.setText(preferenceManager.getString(Constants.KEY_NAME).substring(0, 1));
        }
    }

    private void sendFCMTokenToDatabase() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String refreshToken = task.getResult();
                        HashMap<String, Object> token = new HashMap<>();
                        token.put(Constants.KEY_FCM_TOKEN, refreshToken);

                        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
                        DocumentReference documentReference = firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS)
                                .document(preferenceManager.getString(Constants.KEY_EMAIL));

                        documentReference.set(token, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                })
                                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(MainActivity.this, "Some ERROR occurred!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void getUsers() {
        swipeRefreshLayout.setRefreshing(true);
        firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                    String myUserEmail = preferenceManager.getString(Constants.KEY_EMAIL);
                    if (task.isSuccessful() && task.getResult() != null) {
                        users.clear();
                        for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                            if (myUserEmail.equals(documentSnapshot.getId())) {
                                continue;
                            }
                            User user = new User();
                            user.Name = documentSnapshot.getString(Constants.KEY_NAME);
                            user.Email = documentSnapshot.getString(Constants.KEY_EMAIL);
                            user.Mobile = documentSnapshot.getString(Constants.KEY_MOBILE);
                            user.FCMToken = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.Username = documentSnapshot.getString(Constants.KEY_USERNAME);
                            user.About = documentSnapshot.getString(Constants.KEY_ABOUT);
                            user.Image = documentSnapshot.getString(Constants.KEY_IMAGE);
                            users.add(user);
                        }
                        if (users.size() > 0) {
                            userAdapter.notifyDataSetChanged();
                        } else {
                            swipeRefreshLayout.setRefreshing(false);
                            Alerter.create(MainActivity.this)
                                    .setText("No users available at the moment!")
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
                        swipeRefreshLayout.setRefreshing(false);
                        Alerter.create(MainActivity.this)
                                .setText("No users available at the moment!")
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
                })
                .addOnFailureListener(e -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Alerter.create(MainActivity.this)
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

    @Override
    public void initiateVideoMeeting(User user) {
        if (!isConnectedToInternet(MainActivity.this)) {
            showConnectToInternetDialog();
            return;
        } else {
            if (user.FCMToken != null && !user.FCMToken.trim().isEmpty()) {
                Intent intent = new Intent(MainActivity.this, OutgoingInvitationActivity.class);
                intent.putExtra("user", user);
                intent.putExtra("type", "video");
                startActivity(intent);
                CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            } else {
                Alerter.create(MainActivity.this)
                        .setText(user.Name + " is not available for meeting!")
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
            }
        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (!isConnectedToInternet(MainActivity.this)) {
            showConnectToInternetDialog();
            return;
        } else {
            if (user.FCMToken != null && !user.FCMToken.trim().isEmpty()) {
                Intent intent = new Intent(MainActivity.this, OutgoingInvitationActivity.class);
                intent.putExtra("user", user);
                intent.putExtra("type", "audio");
                startActivity(intent);
                CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
            } else {
                Alerter.create(MainActivity.this)
                        .setText(user.Name + " is not available for meeting!")
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
            }
        }
    }

    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {
        if (isMultipleUsersSelected) {
            conferenceBtn.setVisibility(View.VISIBLE);
            conferenceBtn.setOnClickListener(view -> {
                if (!isConnectedToInternet(MainActivity.this)) {
                    showConnectToInternetDialog();
                    return;
                } else {
                    Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("selectedUsers", new Gson().toJson(userAdapter.getSelectedUsers()));
                    intent.putExtra("type", "video");
                    intent.putExtra("isMultiple", true);
                    startActivity(intent);
                    CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                }
            });
        } else {
            conferenceBtn.setVisibility(View.GONE);
        }
    }

    private void signOut() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                .setTitle("Sign out of Meetin'?")
                .setMessage("Are you sure you want to sign out of Meetin'?")
                .setCancelable(false)
                .setPositiveButton("Yes", R.drawable.ic_dialog_yes, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                    loadingDialog.startDialog();
                    DocumentReference documentReference = firebaseFirestore.collection(Constants.KEY_COLLECTION_USERS)
                            .document(preferenceManager.getString(Constants.KEY_EMAIL));

                    HashMap<String, Object> updates = new HashMap<>();
                    updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
                    documentReference.update(updates)
                            .addOnSuccessListener(aVoid -> {
                                loadingDialog.dismissDialog();
                                firebaseAuth.signOut();
                                preferenceManager.clearPreferences();
                                Toast.makeText(MainActivity.this, "Signed Out!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(getApplicationContext(), WelcomeActivity.class));
                                CustomIntent.customType(MainActivity.this, "fadein-to-fadeout");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                loadingDialog.dismissDialog();

                                Alerter.create(MainActivity.this)
                                        .setText("Whoa! Unable to sign out. Try Again!")
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
                            });
                })
                .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
        materialDialog.show();
    }

    private void checkForBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
                    .setTitle("WARNING!")
                    .setMessage("Battery optimization is enabled. It can interrupt running background services for Meetin'. Disabling it will do the trick!")
                    .setCancelable(false)
                    .setPositiveButton("Disable", R.drawable.ic_dialog_disable, (dialogInterface, which) -> {
                        dialogInterface.dismiss();
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivityForResult(intent, REQUEST_CODE_BATTERY_OPTIMIZATIONS);
                    })
                    .setNegativeButton("Cancel", R.drawable.ic_dialog_cancel, (dialogInterface, which) -> dialogInterface.dismiss()).build();
            materialDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATIONS) {
            checkForBatteryOptimizations();
        }
    }

    private boolean isConnectedToInternet(MainActivity mainActivity) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressLint("MissingPermission") NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (null != networkInfo &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)) {
            return true;
        } else {
            return false;
        }
    }

    private void showConnectToInternetDialog() {
        MaterialDialog materialDialog = new MaterialDialog.Builder(MainActivity.this)
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