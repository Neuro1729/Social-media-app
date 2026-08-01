package com.socialmedia.modules.users.usecases;

import com.socialmedia.modules.users.publicapi.PublicUserReader;
import com.socialmedia.modules.users.publicapi.PublicUserReader.PublicProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetMyProfile {

  private final PublicUserReader users;

  public GetMyProfile(PublicUserReader users) {
    this.users = users;
  }

  public Map<String, Object> execute(Long userId) {
    PublicProfile profile =
        users
            .getPublicProfile(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return toMap(profile);
  }

  public static Map<String, Object> toMap(PublicProfile profile) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", profile.id());
    map.put("username", profile.username());
    map.put("email", profile.email());
    map.put("fullName", profile.fullName());
    map.put("bio", profile.bio());
    map.put("profilePicture", profile.profilePictureUrl());
    map.put("createdAt", profile.createdAt());
    return map;
  }
}
