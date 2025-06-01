package me.desair.tus.server.upload;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Interface for a factory that can create unique upload IDs. This factory can also parse the upload
 * identifier from a given upload URL.
 */
public abstract class UploadIdFactory {

  /**
   * The URI of the main tus upload endpoint. Note that this value possibly contains regex
   * parameters.
   */
  @Getter private String uploadUri = "/";

  private Pattern uploadUriPattern = null;

  /**
   * Set the URI under which the main tus upload endpoint is hosted. Optionally, this URI may
   * contain regex parameters in order to support endpoints that contain URL parameters, for example
   * /users/[0-9]+/files/upload
   *
   * @param uploadUri The URI of the main tus upload endpoint
   */
  public void setUploadUri(String uploadUri) {
    Validate.notBlank(uploadUri, "The upload URI pattern cannot be blank");
    Validate.isTrue(StringUtils.startsWith(uploadUri, "/"), "The upload URI should start with /");
    Validate.isTrue(!StringUtils.endsWith(uploadUri, "$"), "The upload URI should not end with $");
    this.uploadUri = uploadUri;
    this.uploadUriPattern = null;
  }

  /**
   * Read the upload identifier from the given URL. <br>
   * Clients will send requests to upload URLs or provided URLs of completed uploads. This method is
   * able to parse those URLs and provide the user with the corresponding upload ID.
   *
   * @param url The URL provided by the client
   * @return The corresponding Upload identifier
   */
  public UploadId readUploadIdFromUri(String url) {
    Matcher uploadUriMatcher = getUploadUriPattern().matcher(StringUtils.trimToEmpty(url));
    String pathId = uploadUriMatcher.replaceFirst("");

    return readUploadIdFromString(pathId);
  }

  /**
   * Retrieves an {@link UploadId} object based on the provided path value.
   *
   * @param path the path value extracted from the upload URL
   * @return the {@link UploadId} object created from the provided path value, or null if the path
   *     ID is blank.
   */
  public UploadId readUploadIdFromString(String path) {
    if (StringUtils.isNotBlank(path)) {
      return createUploadId(path);
    }
    return null;
  }

  /**
   * Create a new unique upload ID.
   *
   * @return A new unique upload ID
   */
  public abstract UploadId createId();

  /**
   * Transform the extracted path ID value to a value to use for the upload ID object. If the
   * extracted value is not valid, null is returned
   *
   * @param extractedUrlId The ID extracted from the URL
   * @return Value to use in the UploadId object, null if the extracted URL value was not valid
   */
  protected abstract UploadId createUploadId(String extractedUrlId);

  /**
   * Build and retrieve the Upload URI regex pattern.
   *
   * @return A (cached) Pattern to match upload URI's
   */
  protected Pattern getUploadUriPattern() {
    if (uploadUriPattern == null) {
      // We will extract the upload ID's by removing the upload URI from the start of the
      // request URI
      uploadUriPattern =
          Pattern.compile("^.*" + uploadUri + (StringUtils.endsWith(uploadUri, "/") ? "" : "/?"));
    }
    return uploadUriPattern;
  }
}
