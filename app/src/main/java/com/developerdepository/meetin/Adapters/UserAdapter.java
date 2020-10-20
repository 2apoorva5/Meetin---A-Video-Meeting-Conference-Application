package com.developerdepository.meetin.Adapters;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.developerdepository.meetin.Listeners.UsersListener;
import com.developerdepository.meetin.Models.User;
import com.developerdepository.meetin.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private List<User> selectedUsers;
    private UsersListener usersListener;

    public UserAdapter(List<User> users, UsersListener usersListener) {
        this.users = users;
        this.usersListener = usersListener;
        selectedUsers = new ArrayList<>();
    }

    public List<User> getSelectedUsers() {
        return selectedUsers;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.layout_item_container_user,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        CircleImageView itemUserProfilePic;
        TextView itemUserNameFirstChar, itemUserName, itemUserUsername;
        ImageView voiceCallBtn, videoCallBtn, imageSelectedUser;
        ConstraintLayout userContainerLayout;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            itemUserNameFirstChar = itemView.findViewById(R.id.item_user_name_first_char);
            itemUserProfilePic = itemView.findViewById(R.id.item_user_profile_pic);
            itemUserName = itemView.findViewById(R.id.item_user_name);
            itemUserUsername = itemView.findViewById(R.id.item_user_username);
            voiceCallBtn = itemView.findViewById(R.id.voice_call_btn);
            videoCallBtn = itemView.findViewById(R.id.video_call_btn);
            userContainerLayout = itemView.findViewById(R.id.user_container_layout);
            imageSelectedUser = itemView.findViewById(R.id.image_selected_user);
        }

        void setUserData(User user) {
            if (user.Image != null && user.Image.length() != 0 && !user.Image.isEmpty() && !user.Image.equals("")) {
                itemUserNameFirstChar.setVisibility(View.INVISIBLE);
                itemUserProfilePic.setVisibility(View.VISIBLE);
                Glide.with(itemUserProfilePic.getContext()).load(Uri.parse(user.Image)).centerCrop().into(itemUserProfilePic);
            } else {
                itemUserProfilePic.setVisibility(View.INVISIBLE);
                itemUserNameFirstChar.setVisibility(View.VISIBLE);
                itemUserNameFirstChar.setText(user.Name.substring(0, 1));
            }
            itemUserName.setText(user.Name);
            itemUserUsername.setText(user.Username);

            voiceCallBtn.setOnClickListener(view -> usersListener.initiateAudioMeeting(user));
            videoCallBtn.setOnClickListener(view -> usersListener.initiateVideoMeeting(user));

            userContainerLayout.setOnLongClickListener(view -> {
                if (imageSelectedUser.getVisibility() != View.VISIBLE) {
                    selectedUsers.add(user);
                    itemUserProfilePic.setVisibility(View.INVISIBLE);
                    itemUserNameFirstChar.setVisibility(View.INVISIBLE);
                    imageSelectedUser.setVisibility(View.VISIBLE);
                    videoCallBtn.setVisibility(View.GONE);
                    voiceCallBtn.setVisibility(View.GONE);
                    usersListener.onMultipleUsersAction(true);
                }
                return true;
            });

            userContainerLayout.setOnClickListener(view -> {
                if (imageSelectedUser.getVisibility() == View.VISIBLE) {
                    selectedUsers.remove(user);
                    imageSelectedUser.setVisibility(View.GONE);
                    if (user.Image != null && user.Image.length() != 0 && !user.Image.isEmpty() && !user.Image.equals("")) {
                        itemUserNameFirstChar.setVisibility(View.INVISIBLE);
                        itemUserProfilePic.setVisibility(View.VISIBLE);
                    } else {
                        itemUserProfilePic.setVisibility(View.INVISIBLE);
                        itemUserNameFirstChar.setVisibility(View.VISIBLE);
                    }
                    videoCallBtn.setVisibility(View.VISIBLE);
                    voiceCallBtn.setVisibility(View.VISIBLE);
                    if (selectedUsers.size() == 0) {
                        usersListener.onMultipleUsersAction(false);
                    }
                } else {
                    if (selectedUsers.size() > 0) {
                        selectedUsers.add(user);
                        itemUserProfilePic.setVisibility(View.INVISIBLE);
                        itemUserNameFirstChar.setVisibility(View.INVISIBLE);
                        imageSelectedUser.setVisibility(View.VISIBLE);
                        videoCallBtn.setVisibility(View.GONE);
                        voiceCallBtn.setVisibility(View.GONE);
                    }
                }
            });
        }
    }
}
