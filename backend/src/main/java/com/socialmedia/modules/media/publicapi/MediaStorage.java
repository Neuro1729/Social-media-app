package com.socialmedia.modules.media.publicapi;

import java.io.IOException;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/** Public contract for other modules. Do not depend on media internals. */
public interface MediaStorage {

  record UploadResult(Long mediaId, String url) {}

  UploadResult upload(MultipartFile file, Long uploadedByUserId) throws IOException;

  Optional<String> getUrl(Long mediaId);
}
