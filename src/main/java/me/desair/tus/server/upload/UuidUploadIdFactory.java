package me.desair.tus.server.upload;

import java.util.UUID;

/**
 * Factory to create unique upload IDs. This factory can also parse the upload identifier from a
 * given upload URL.
 */
public class UuidUploadIdFactory extends UploadIdFactory {

  @Override
  protected UploadId createUploadId(String extractedUrlId) {
    UUID id;
    try {
      id = UUID.fromString(extractedUrlId);
    } catch (IllegalArgumentException ex) {
      id = null;
    }
    if (id != null) return new UploadId(id.toString());
    return null;
  }

  @Override
  public UploadId createId() {
    return new UploadId(UUID.randomUUID().toString());
  }
}
