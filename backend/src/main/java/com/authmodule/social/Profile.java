package com.authmodule.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 160)
    private String bio;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    protected Profile() {
    }

    public Profile(UUID userId, String bio, String profilePictureUrl, boolean isPrivate) {
        this.userId = userId;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
        this.isPrivate = isPrivate;
    }

    public static Profile empty(UUID userId) {
        return new Profile(userId, null, null, false);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }
}
