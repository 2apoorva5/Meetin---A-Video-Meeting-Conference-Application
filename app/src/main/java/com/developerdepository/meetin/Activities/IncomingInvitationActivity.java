package com.developerdepository.meetin.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.developerdepository.meetin.Network.ApiClient;
import com.developerdepository.meetin.Network.ApiService;
import com.developerdepository.meetin.R;
import com.developerdepository.meetin.Utilities.Constants;
import com.developerdepository.meetin.Utilities.PreferenceManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

import de.hdodenhof.circleimageview.CircleImageView;
import maes.tech.intentanim.CustomIntent;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends AppCompatActivity {

    //View Variables
    private ImageView imageMeetingType;
    private CircleImageView meetingInitiatorProfilePic;
    private TextView meetingInitiatorNameFirstChar, meetingInitiatorName, meetingInitiatorUsername, meetingInitiatorEmail;
    private ConstraintLayout acceptBtn, rejectBtn;

    //Other Variables
    private PreferenceManager preferenceManager;
    private String meetingType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_invitation);

        preferenceManager = new PreferenceManager(getApplicationContext());

        if (!preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(IncomingInvitationActivity.this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            CustomIntent.customType(IncomingInvitationActivity.this, "fadein-to-fadeout");
            finish();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(getColor(R.color.colorMeetingInvitationEnd));

        initViews();
        setActionOnViews();

        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);
        if(meetingType != null) {
            if(meetingType.equals("video")) {
                imageMeetingType.setImageResource(R.drawable.ic_video_call);
            } else if(meetingType.equals("audio")) {
                imageMeetingType.setImageResource(R.drawable.ic_voice_call);
            }
        }

        String image = getIntent().getStringExtra(Constants.KEY_IMAGE);
        if(image != null && image.length() != 0 && !image.isEmpty() && !image.equals("")) {
            meetingInitiatorNameFirstChar.setVisibility(View.INVISIBLE);
            meetingInitiatorProfilePic.setVisibility(View.VISIBLE);
            Glide.with(IncomingInvitationActivity.this).load(Uri.parse(image)).centerCrop().into(meetingInitiatorProfilePic);
        } else {
            meetingInitiatorProfilePic.setVisibility(View.INVISIBLE);
            meetingInitiatorNameFirstChar.setVisibility(View.VISIBLE);
            meetingInitiatorNameFirstChar.setText(getIntent().getStringExtra(Constants.KEY_NAME).substring(0, 1));
        }

        meetingInitiatorName.setText(getIntent().getStringExtra(Constants.KEY_NAME));
        meetingInitiatorUsername.setText(getIntent().getStringExtra(Constants.KEY_USERNAME));
        meetingInitiatorEmail.setText(getIntent().getStringExtra(Constants.KEY_EMAIL));
    }

    private void initViews() {
        imageMeetingType = findViewById(R.id.image_meeting_type);
        meetingInitiatorProfilePic = findViewById(R.id.meeting_initiator_profile_pic);
        meetingInitiatorNameFirstChar = findViewById(R.id.meeting_initiator_name_first_char);
        meetingInitiatorName = findViewById(R.id.meeting_initiator_name);
        meetingInitiatorUsername = findViewById(R.id.meeting_initiator_username);
        meetingInitiatorEmail = findViewById(R.id.meeting_initiator_email);
        acceptBtn = findViewById(R.id.incoming_meeting_invitation_accept_btn);
        rejectBtn = findViewById(R.id.incoming_meeting_invitation_reject_btn);
    }

    private void setActionOnViews() {
        acceptBtn.setOnClickListener(view -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
        ));

        rejectBtn.setOnClickListener(view -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_REJECTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
        ));
    }

    private void sendInvitationResponse(String type, String receiverToken) {
        try {

            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), type);

        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMessageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendRemoteMessage(
                Constants.getRemoteMessageHeaders(), remoteMessageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()) {
                    if(type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                        try {
                            URL serverURL = new URL("https://meet.jit.si");
                            JitsiMeetConferenceOptions.Builder builder = new JitsiMeetConferenceOptions.Builder();
                            builder.setServerURL(serverURL);
                            builder.setWelcomePageEnabled(false);
                            builder.setRoom(getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_ROOM));
                            if(meetingType.equals("audio")) {
                                builder.setVideoMuted(true);
                                builder.setAudioOnly(true);
                            }
                            JitsiMeetActivity.launch(IncomingInvitationActivity.this, builder.build());
                            finish();
                        } catch (Exception exception) {
                            Toast.makeText(IncomingInvitationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else if(type.equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                        Toast.makeText(IncomingInvitationActivity.this, "Invitation Rejected!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if(type != null) {
                if(type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                    Toast.makeText(context, "Invitation Cancelled!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        CustomIntent.customType(IncomingInvitationActivity.this, "fadein-to-fadeout");
    }
}