package com.socialmedia.modules.posts.usecases;

import com.socialmedia.modules.posts.domain.Post;
import com.socialmedia.modules.posts.infrastructure.PostJpaRepository;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeletePost {

  private final PostJpaRepository posts;

  public DeletePost(PostJpaRepository posts) {
    this.posts = posts;
  }

  @Transactional
  public Map<String, String> execute(Long postId, Long currentUserId) {
    Post post =
        posts.findById(postId).orElseThrow(() -> new IllegalArgumentException("Post not found"));
    if (!post.getAuthorId().equals(currentUserId)) {
      throw new IllegalArgumentException("You can only change your own posts");
    }
    posts.delete(post);
    return Map.of("message", "Post deleted");
  }
}
