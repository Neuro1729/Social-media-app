package com.socialmedia.modules.media.infrastructure;

import com.socialmedia.modules.media.domain.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaJpaRepository extends JpaRepository<MediaFile, Long> {}
