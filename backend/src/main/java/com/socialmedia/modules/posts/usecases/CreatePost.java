package com.socialmedia.modules.posts.usecases;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.posts.domain.Post;
import com.socialmedia.modules.posts.infrastructure.PostJpaRepository;
import com.socialmedia.modules.posts.infrastructure.PostResponseAssembler;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CreatePost {

  private final PostJpaRepository posts;
  private final MediaStorage mediaStorage;
  private final PublicUserReader users;

  public CreatePost(
      PostJpaRepository posts, MediaStorage mediaStorage, PublicUserReader users) {
    this.posts = posts;
    this.mediaStorage = mediaStorage;
    this.users = users;
  }

  @Transactional
  public Map<String, Object> execute(Long authorId, String caption, MultipartFile image)
      throws IOException {
    users
        .getPublicProfile(authorId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    String trimmed = caption == null ? "" : caption.trim();
    boolean hasImage = image != null && !image.isEmpty();
    if (trimmed.isEmpty() && !hasImage) {
      throw new IllegalArgumentException("Provide a caption or an image");
    }
    if (trimmed.length() > 2200) {
      throw new IllegalArgumentException("Caption must be at most 2200 characters");
    }

    List<Long> mediaIds = new ArrayList<>();
    if (hasImage) {
      mediaIds.add(mediaStorage.upload(image, authorId).mediaId());
    }

    Post post = new Post();
    post.setAuthorId(authorId);
    post.setCaption(trimmed);
    post.setMediaIds(mediaIds);
    post = posts.save(post);
    return PostResponseAssembler.toResponse(post, authorId, users, mediaStorage);
  }
}
