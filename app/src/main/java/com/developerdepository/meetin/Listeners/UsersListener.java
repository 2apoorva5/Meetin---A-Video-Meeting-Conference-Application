package com.developerdepository.meetin.Listeners;

import com.developerdepository.meetin.Models.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);
    void initiateAudioMeeting(User user);
    void onMultipleUsersAction(Boolean isMultipleUsersSelected);
}
