package com.socialmedia.modules.users.usecases;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.users.domain.User;
import com.socialmedia.modules.users.infrastructure.UserJpaRepository;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadProfilePicture {

  private final UserJpaRepository users;
  private final MediaStorage mediaStorage;
  private final PublicUserReader publicUserReader;

  public UploadProfilePicture(
      UserJpaRepository users, MediaStorage mediaStorage, PublicUserReader publicUserReader) {
    this.users = users;
    this.mediaStorage = mediaStorage;
    this.publicUserReader = publicUserReader;
  }

  @Transactional
  public Map<String, Object> execute(Long userId, MultipartFile file) throws IOException {
    User user =
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    MediaStorage.UploadResult uploaded = mediaStorage.upload(file, userId);
    user.setProfilePictureMediaId(uploaded.mediaId());
    users.save(user);
    return GetMyProfile.toMap(
        publicUserReader
            .getPublicProfile(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found")));
  }
}
