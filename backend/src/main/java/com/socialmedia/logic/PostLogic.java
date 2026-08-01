package com.socialmedia.logic;

import com.socialmedia.data.PostData;
import com.socialmedia.data.UserData;
import com.socialmedia.model.Post;
import com.socialmedia.model.User;
import com.socialmedia.upload.ImageUpload;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostLogic {

  private final PostData postData;
  private final UserData userData;
  private final ImageUpload imageUpload;

  public PostLogic(PostData postData, UserData userData, ImageUpload imageUpload) {
    this.postData = postData;
    this.userData = userData;
    this.imageUpload = imageUpload;
  }

  @Transactional
  public Map<String, Object> create(Long userId, String caption, MultipartFile image)
      throws IOException {
    User owner = requireUser(userId);
    String trimmed = caption == null ? "" : caption.trim();
    boolean hasImage = image != null && !image.isEmpty();
    if (trimmed.isEmpty() && !hasImage) {
      throw new IllegalArgumentException("Provide a caption or an image");
    }
    if (trimmed.length() > 2200) {
      throw new IllegalArgumentException("Caption must be at most 2200 characters");
    }

    Post post = new Post();
    post.setOwner(owner);
    post.setCaption(trimmed.isEmpty() ? "" : trimmed);
    if (hasImage) {
      post.setImageUrl(imageUpload.savePostImage(userId, image));
    }
    return toPublicPost(postData.save(post), userId);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> myPosts(Long userId) {
    return postData.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
        .map(post -> toPublicPost(post, userId))
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> postsByUsername(String username, Long viewerId) {
    User owner =
        userData
            .findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    List<Map<String, Object>> posts =
        postData.findByOwnerUsernameIgnoreCaseOrderByCreatedAtDesc(username).stream()
            .map(post -> toPublicPost(post, viewerId))
            .toList();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("user", AuthLogic.toPublicUser(owner));
    body.put("posts", posts);
    return body;
  }

  @Transactional
  public Map<String, Object> editCaption(Long userId, Long postId, String caption) {
    Post post = requireOwnedPost(userId, postId);
    if (caption == null || caption.isBlank()) {
      throw new IllegalArgumentException("Caption is required");
    }
    String trimmed = caption.trim();
    if (trimmed.length() > 2200) {
      throw new IllegalArgumentException("Caption must be at most 2200 characters");
    }
    post.setCaption(trimmed);
    return toPublicPost(postData.save(post), userId);
  }

  @Transactional
  public Map<String, String> delete(Long userId, Long postId) {
    Post post = requireOwnedPost(userId, postId);
    postData.delete(post);
    return Map.of("message", "Post deleted");
  }

  private Post requireOwnedPost(Long userId, Long postId) {
    Post post =
        postData
            .findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    if (!post.getOwner().getId().equals(userId)) {
      throw new IllegalArgumentException("You can only change your own posts");
    }
    return post;
  }

  private User requireUser(Long userId) {
    return userData
        .findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  private Map<String, Object> toPublicPost(Post post, Long viewerId) {
    User owner = post.getOwner();
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", post.getId());
    map.put("caption", post.getCaption());
    map.put("imageUrl", post.getImageUrl());
    map.put("createdAt", post.getCreatedAt());
    map.put("updatedAt", post.getUpdatedAt());
    map.put("ownerId", owner.getId());
    map.put("ownerUsername", owner.getUsername());
    map.put("ownerFullName", owner.getFullName());
    map.put("ownerProfilePicture", owner.getProfilePicture());
    map.put("mine", owner.getId().equals(viewerId));
    return map;
  }
}
