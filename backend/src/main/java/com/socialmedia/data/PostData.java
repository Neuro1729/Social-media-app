package com.socialmedia.data;

import com.socialmedia.model.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostData extends JpaRepository<Post, Long> {
  List<Post> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

  List<Post> findByOwnerUsernameIgnoreCaseOrderByCreatedAtDesc(String username);
}
