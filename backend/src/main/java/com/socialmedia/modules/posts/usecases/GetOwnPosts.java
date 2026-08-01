package com.socialmedia.modules.posts.usecases;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.posts.infrastructure.PostJpaRepository;
import com.socialmedia.modules.posts.infrastructure.PostResponseAssembler;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetOwnPosts {

  private final PostJpaRepository posts;
  private final PublicUserReader users;
  private final MediaStorage mediaStorage;

  public GetOwnPosts(
      PostJpaRepository posts, PublicUserReader users, MediaStorage mediaStorage) {
    this.posts = posts;
    this.users = users;
    this.mediaStorage = mediaStorage;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> execute(Long currentUserId) {
    return PostResponseAssembler.toResponseList(
        posts.findByAuthorIdOrderByCreatedAtDesc(currentUserId),
        currentUserId,
        users,
        mediaStorage);
  }
}
