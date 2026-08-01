package com.socialmedia.modules.posts.infrastructure;

import com.socialmedia.modules.posts.domain.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostJpaRepository extends JpaRepository<Post, Long> {
  List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
