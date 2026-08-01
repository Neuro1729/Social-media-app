package com.socialmedia.modules.posts.usecases;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.posts.infrastructure.PostJpaRepository;
import com.socialmedia.modules.posts.infrastructure.PostResponseAssembler;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import com.socialmedia.modules.users.publicapi.PublicUserReader.PublicProfile;
import com.socialmedia.modules.users.usecases.GetMyProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetUserPosts {

  private final PostJpaRepository posts;
  private final PublicUserReader users;
  private final MediaStorage mediaStorage;

  public GetUserPosts(
      PostJpaRepository posts, PublicUserReader users, MediaStorage mediaStorage) {
    this.posts = posts;
    this.users = users;
    this.mediaStorage = mediaStorage;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> execute(String username, Long viewerId) {
    PublicProfile author =
        users
            .getPublicProfileByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    List<Map<String, Object>> postList =
        PostResponseAssembler.toResponseList(
            posts.findByAuthorIdOrderByCreatedAtDesc(author.id()),
            viewerId,
            users,
            mediaStorage);

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("user", GetMyProfile.toMap(author));
    body.put("posts", postList);
    return body;
  }
}
