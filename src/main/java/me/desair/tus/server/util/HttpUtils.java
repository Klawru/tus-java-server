package me.desair.tus.server.util;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import me.desair.tus.server.HttpHeader;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class HttpUtils {
  /**
   * Get the decoded metadata map provided by the client based on the encoded Tus metadata string
   * received on creation of the upload. The encoded metadata string consists of one or more
   * comma-separated key-value pairs where the key is ASCII encoded and the value Base64 encoded.
   * The key and value MUST be separated by a space. See <a
   * href="https://tus.io/protocols/resumable-upload.html#upload-metadata">upload-metadata</a>
   *
   * @return The encoded metadata string as received from the client
   */
  public static Map<String, String> decodedMetadata(String encodedMetadata) {
    Map<String, String> metadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (String valuePair : splitToArray(encodedMetadata, ",")) {
      String[] keyValue = splitToArray(valuePair, "\\s");
      String key;
      String value = null;
      if (keyValue.length > 0) {
        key = StringUtils.trimToEmpty(keyValue[0]);

        // Skip any blank values
        int i = 1;
        while (keyValue.length > i && StringUtils.isBlank(keyValue[i])) {
          i++;
        }

        if (keyValue.length > i) {
          value = base64decode(keyValue[i]);
        }

        metadata.put(key, value);
      }
    }
    return metadata;
  }

  /**
   * Encode the given metadata map into an encoded Tus metadata string. The encoded metadata string
   * consists of one or more comma-separated key-value pairs where the key is ASCII encoded and the
   * value Base64 encoded.The key and value MUST be separated by a space. See <a
   * href="https://tus.io/protocols/resumable-upload.html#upload-metadata">upload-metadata</a>
   *
   * @param metadata The metadata map to encode.
   * @return The encoded metadata string.
   */
  public static String encodeMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    var encoded = new StringBuilder();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      if (!encoded.isEmpty()) {
        encoded.append(",");
      }
      encoded.append(entry.getKey());
      if (entry.getValue() != null) {
        encoded.append(" ").append(base64encode(entry.getValue()));
      }
    }
    return encoded.toString();
  }

  private String[] splitToArray(String value, String separatorRegex) {
    if (StringUtils.isBlank(value)) {
      return new String[0];
    } else {
      return StringUtils.trimToEmpty(value).split(separatorRegex);
    }
  }

  /**
   * Decodes a given Base64 encoded string into its original form using UTF-8 character encoding.
   *
   * @param encodedValue The Base64 encoded string to decode.
   * @return The decoded string in UTF-8 encoding, or null if the input is null.
   */
  public String base64decode(String encodedValue) {
    if (encodedValue == null) {
      return null;
    } else {
      return new String(Base64.decodeBase64(encodedValue), StandardCharsets.UTF_8);
    }
  }

  /**
   * Encodes the provided string into a Base64 encoded string using UTF-8 character encoding.
   *
   * @param value The string to be Base64 encoded. If the input is null, an empty string will be
   *     returned.
   * @return The Base64 encoded representation of the input string. Returns an empty string if the
   *     input is null.
   */
  public static String base64encode(String value) {
    if (value == null) {
      return "";
    }
    return Base64.encodeBase64String(value.getBytes(StandardCharsets.UTF_8));
  }

  public static String getHeader(HttpServletRequest request, String header) {
    return StringUtils.trimToEmpty(request.getHeader(header));
  }

  public static Long getLongHeader(HttpServletRequest request, String header) {
    try {
      return Long.valueOf(getHeader(request, header));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /**
   * Build a comma-separated list based on the remote address of the request and the
   * X-Forwareded-For header. The list is constructed as "client, proxy1, proxy2".
   *
   * @return A comma-separated list of ip-addresses
   */
  public static String buildRemoteIpList(HttpServletRequest servletRequest) {
    String ipAddresses = servletRequest.getRemoteAddr();
    String xForwardedForHeader = getHeader(servletRequest, HttpHeader.X_FORWARDED_FOR);
    if (!xForwardedForHeader.isEmpty()) {
      ipAddresses = xForwardedForHeader + ", " + ipAddresses;
    }
    return ipAddresses;
  }

  /**
   * Parses an upload concatenation header value and extracts concatenation IDs.
   *
   * @see <a href="https://tus.io/protocols/resumable-upload#upload-concat">TUS upload-concat
   *     headder</a>
   * @see HttpHeader#UPLOAD_CONCAT
   * @param uploadConcatValue TUS Upload-Concat header.
   */
  public static List<String> parseConcatenationIdsFromUploadHeader(String uploadConcatValue) {
    List<String> output = new LinkedList<>();
    String idString = StringUtils.substringAfter(uploadConcatValue, ";");
    Collections.addAll(output, StringUtils.trimToEmpty(idString).split("\\s"));

    return output;
  }

  public static String urlSaveString(String inputValue) {
    URLCodec codec = new URLCodec();
    // Check if value is not encoded already
    try {
      if (inputValue != null
          && inputValue.equals(codec.decode(inputValue, StandardCharsets.UTF_8.name()))) {
        return codec.encode(inputValue, StandardCharsets.UTF_8.name());
      } else {
        // value is already encoded, use as is
        return inputValue;
      }
    } catch (DecoderException | UnsupportedEncodingException e) {
      log.warn("Unable to URL encode upload ID value", e);
      return inputValue;
    }
  }
}
