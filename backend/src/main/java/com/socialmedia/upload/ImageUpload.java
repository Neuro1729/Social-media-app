package com.socialmedia.upload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageUpload {

  private static final Set<String> ALLOWED = Set.of(
      "image/jpeg", "image/png", "image/webp", "image/gif");

  private final Path uploadRoot;

  public ImageUpload(@Value("${app.upload-dir}") String uploadDir) throws IOException {
    this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    Files.createDirectories(this.uploadRoot);
  }

  public String saveProfilePicture(Long userId, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Image file is required");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED.contains(contentType)) {
      throw new IllegalArgumentException("Only JPEG, PNG, WEBP, or GIF images are allowed");
    }

    String extension = extensionFor(contentType);
    String filename = "user-" + userId + "-" + UUID.randomUUID() + extension;
    Path destination = uploadRoot.resolve(filename);
    Files.copy(file.getInputStream(), destination);
    return "/uploads/" + filename;
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
