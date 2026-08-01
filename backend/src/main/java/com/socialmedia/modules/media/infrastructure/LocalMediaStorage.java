package com.socialmedia.modules.media.infrastructure;

import com.socialmedia.modules.media.domain.MediaFile;
import com.socialmedia.modules.media.publicapi.MediaStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LocalMediaStorage implements MediaStorage {

  private static final Set<String> ALLOWED =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

  private final Path uploadRoot;
  private final MediaJpaRepository mediaJpaRepository;

  public LocalMediaStorage(
      @Value("${app.upload-dir}") String uploadDir, MediaJpaRepository mediaJpaRepository)
      throws IOException {
    this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    this.mediaJpaRepository = mediaJpaRepository;
    Files.createDirectories(this.uploadRoot);
  }

  @Override
  public UploadResult upload(MultipartFile file, Long uploadedByUserId) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Image file is required");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED.contains(contentType)) {
      throw new IllegalArgumentException("Only JPEG, PNG, WEBP, or GIF images are allowed");
    }

    String extension = extensionFor(contentType);
    String storedName = "media-" + UUID.randomUUID() + extension;
    Files.copy(file.getInputStream(), uploadRoot.resolve(storedName));

    MediaFile media = new MediaFile();
    media.setStoredName(storedName);
    media.setContentType(contentType);
    media.setUploadedByUserId(uploadedByUserId);
    media = mediaJpaRepository.save(media);

    return new UploadResult(media.getId(), toUrl(storedName));
  }

  @Override
  public Optional<String> getUrl(Long mediaId) {
    if (mediaId == null) {
      return Optional.empty();
    }
    return mediaJpaRepository.findById(mediaId).map(m -> toUrl(m.getStoredName()));
  }

  private static String toUrl(String storedName) {
    return "/uploads/" + storedName;
  }

  private static String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      case "image/gif" -> ".gif";
      default -> ".jpg";
    };
  }
}
