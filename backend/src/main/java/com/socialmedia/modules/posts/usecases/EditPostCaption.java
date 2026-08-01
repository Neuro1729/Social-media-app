package com.socialmedia.modules.posts.usecases;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.posts.domain.Post;
import com.socialmedia.modules.posts.infrastructure.PostJpaRepository;
import com.socialmedia.modules.posts.infrastructure.PostResponseAssembler;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EditPostCaption {

  private final PostJpaRepository posts;
  private final PublicUserReader users;
  private final MediaStorage mediaStorage;

  public EditPostCaption(
      PostJpaRepository posts, PublicUserReader users, MediaStorage mediaStorage) {
    this.posts = posts;
    this.users = users;
    this.mediaStorage = mediaStorage;
  }

  @Transactional
  public Map<String, Object> execute(Long postId, Long currentUserId, String caption) {
    Post post =
        posts.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
    if (!post.getAuthorId().equals(currentUserId)) {
      throw new IllegalArgumentException("You can only change your own posts");
    }
    if (caption == null || caption.isBlank()) {
      throw new IllegalArgumentException("Caption is required");
    }
    String trimmed = caption.trim();
    if (trimmed.length() > 2200) {
      throw new IllegalArgumentException("Caption must be at most 2200 characters");
    }
    post.setCaption(trimmed);
    posts.save(post);
    return PostResponseAssembler.toResponse(post, currentUserId, users, mediaStorage);
  }
}
