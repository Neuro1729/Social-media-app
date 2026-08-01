package com.socialmedia.modules.posts.infrastructure;

import com.socialmedia.modules.media.publicapi.MediaStorage;
import com.socialmedia.modules.posts.domain.Post;
import com.socialmedia.modules.users.publicapi.PublicUserReader;
import com.socialmedia.modules.users.publicapi.PublicUserReader.PublicProfile;
import com.socialmedia.modules.users.usecases.GetMyProfile;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Assembles post + author for API responses using only public contracts. */
public final class PostResponseAssembler {

  private PostResponseAssembler() {}

  public static Map<String, Object> toResponse(
      Post post, Long viewerId, PublicUserReader users, MediaStorage media) {
    PublicProfile author =
        users
            .getPublicProfile(post.getAuthorId())
            .orElseThrow(() -> new IllegalArgumentException("Author not found"));

    String imageUrl =
        post.getMediaIds().stream()
            .findFirst()
            .flatMap(media::getUrl)
            .orElse(null);

    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", post.getId());
    map.put("caption", post.getCaption());
    map.put("imageUrl", imageUrl);
    map.put("mediaIds", post.getMediaIds());
    map.put("createdAt", post.getCreatedAt());
    map.put("updatedAt", post.getUpdatedAt());
    map.put("ownerId", author.id());
    map.put("ownerUsername", author.username());
    map.put("ownerFullName", author.fullName());
    map.put("ownerProfilePicture", author.profilePictureUrl());
    map.put("mine", author.id().equals(viewerId));
    map.put("author", GetMyProfile.toMap(author));
    return map;
  }

  public static List<Map<String, Object>> toResponseList(
      List<Post> posts, Long viewerId, PublicUserReader users, MediaStorage media) {
    return posts.stream().map(p -> toResponse(p, viewerId, users, media)).toList();
  }
}
