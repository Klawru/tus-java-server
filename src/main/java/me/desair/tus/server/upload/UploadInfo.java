package me.desair.tus.server.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.beans.Transient;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import lombok.*;

/**
 * Class that contains all metadata on an upload process. This class also holds the metadata
 * provided by the client when creating the upload.
 */
@EqualsAndHashCode
@AllArgsConstructor(onConstructor_ = {@JsonCreator})
public class UploadInfo implements Serializable {

  private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  private static final List<String> fileNameKeys = Arrays.asList("filename", "name");
  private static final List<String> mimeTypeKeys = Arrays.asList("mimetype", "filetype", "type");

  /**
   * The unique identifier of this upload process The unique identifier is represented by a
   * instance.
   */
  @Getter @Setter private UploadId id;

  /**
   * The total length of the byte array that the client wants to upload. This value is provided by
   * the client when creating the upload (POST) or when uploading a new set of bytes (PATCH) If null
   * size is unknown.
   */
  @Getter private Long length;

  /**
   * Offset in bytes (zero-based) The current byte offset of the bytes that already have been stored
   * for this upload on the server. The offset is the position where the next newly received byte
   * should be stored.
   */
  @Getter @Setter private long offset;

  /**
   * The encoded Tus metadata string as it was provided by the Tus client at creation of the upload.
   * The encoded metadata string consists of one or more comma-separated key-value pairs where the
   * key is ASCII encoded and the value Base64 encoded. See <a
   * href="https://tus.io/protocols/resumable-upload.html#upload-metadata">Tus-protocol site</a>
   *
   * @return The encoded metadata string as received from the client
   */
  @NonNull @Getter private final Map<String, String> metadata = new HashMap<>();

  /**
   * Storage contains information about where the data storage saves the upload, for example a file
   * path. The available values vary depending on what data store is used.
   */
  @NonNull @Getter private final Map<String, String> storage = new HashMap<>();

  /**
   * Return the type of this upload. An upload can have types specified in {@link UploadType}. The
   * type of an upload depends on the Tus concatenation extension: <a
   * href="https://tus.io/protocols/resumable-upload.html#concatenation">site</a>
   *
   * @return The type of this upload as specified in {@link UploadType}
   * @param uploadType The type to set on this upload
   */
  @Setter @Getter private UploadType uploadType;

  /**
   * The owner key for this upload. This key uniquely identifies the owner of the uploaded bytes.
   * The user of this library is free to interpret the meaning of "owner". This can be a user ID, a
   * company division, a group of users, a tenant, etc.
   *
   * @param ownerKey The owner key to assign to this upload
   * @return The unique identifying key of the owner of this upload
   */
  @Getter @Setter private String ownerKey;

  @EqualsAndHashCode.Exclude @Getter private final Instant creationTimestamp;

  /**
   * The ip-addresses involved when this upload was created. The returned value is a comma-separated
   * list based on the remote address of the request and the X-Forwareded-For header. The list is
   * constructed as "client, proxy1, proxy2".
   *
   * @return A comma-separated list of ip-addresses
   */
  @Getter private String creatorIpAddresses;

  /**
   * Indicates the timestamp after which the upload expires in point time.
   *
   * @return The expiration timestamp in milliseconds
   * @param expirationPeriod The period the upload should remain valid
   */
  @Setter @Getter private Instant expirationTimestamp;

  /**
   * -- SETTER -- Set the list of upload identifiers of which this upload is composed of.
   *
   * <p>-- GETTER -- Get the list of upload identifiers of which this upload is composed of.
   *
   * @param concatenationPartIds The list of child upload identifiers
   * @return The list of child upload identifiers
   */
  @Getter @Setter private List<String> concatenationPartIds;

  /**
   * The original value of the "Upload-Concat" HTTP header that was provided by the client.
   *
   * @param uploadConcatHeaderValue The original value of the "Upload-Concat" HTTP header
   * @return The original value of the "Upload-Concat" HTTP header
   */
  @Getter @Setter private String uploadConcatHeaderValue;

  /** Default constructor to use if an upload is created without HTTP request. */
  public UploadInfo() {
    this.creationTimestamp = Instant.now();
    this.offset = 0L;
    this.length = null;
  }

  /**
   * Constructor to use if the upload is created using an HTTP request (which is usually the case).
   *
   * @param ipAddress
   */
  public UploadInfo(String ipAddress) {
    this();
    this.creatorIpAddresses = ipAddress;
  }

  /**
   * Check if the client provided any metadata when creating this upload.
   *
   * @return True if metadata is present, false otherwise
   */
  public boolean hasMetadata() {
    return !metadata.isEmpty();
  }

  /**
   * Set the total length of the byte array that the client wants to upload. The client can provided
   * this value when creating the upload (POST) or when uploading a new set of bytes (PATCH).
   *
   * @param length The number of bytes that the client specified he will upload
   */
  public void setLength(Long length) {
    this.length = (length != null && length > 0 ? length : null);
  }

  /**
   * Check if the client already provided a total upload length.
   *
   * @return True if the total upload length is known, false otherwise
   */
  public boolean hasLength() {
    return length != null;
  }

  /**
   * An upload is still in progress: - as long as we did not receive information on the total length
   * (see {@link UploadInfo#getLength()}) - the total length does not match the current offset.
   *
   * @return true if the upload is still in progress, false otherwise
   */
  @Transient
  public boolean isUploadInProgress() {
    return !Objects.equals(offset, length);
  }

  /**
   * Try to guess the filename of the uploaded data. If we cannot guess the name we fall back to the
   * ID. <br>
   * NOTE: This is only a guess, there are no guarantees that the return value is correct
   *
   * @return A potential file name
   */
  @Transient
  public String getFileName() {
    for (String fileNameKey : fileNameKeys) {
      if (metadata.containsKey(fileNameKey)) {
        return metadata.get(fileNameKey);
      }
    }

    return getId().toString();
  }

  /**
   * Try to guess the mime-type of the uploaded data. <br>
   * NOTE: This is only a guess, there are no guarantees that the return value is correct
   *
   * @return A potential file name
   */
  @Transient
  public String getFileMimeType() {
    for (String fileNameKey : mimeTypeKeys) {
      if (metadata.containsKey(fileNameKey)) {
        return metadata.get(fileNameKey);
      }
    }

    return APPLICATION_OCTET_STREAM;
  }

  /**
   * Check if this upload is expired.
   *
   * @return True if the upload is expired, false otherwise
   */
  @Transient
  public boolean isExpired(Instant currentTime) {
    return expirationTimestamp != null && expirationTimestamp.compareTo(currentTime) < 0;
  }
}
