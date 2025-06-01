package me.desair.tus.server.upload;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Alternative {@link UploadIdFactory} implementation that uses the current system time to generate
 * ID's. Since time is not unique, this upload ID factory should not be used in busy, clustered
 * production systems.
 */
@RequiredArgsConstructor
public class TimeBasedUploadIdFactory extends UploadIdFactory {
  private final Clock clock;

  @Override
  protected UploadId createUploadId(String extractedUrlId) {
    Long id = null;
    if (StringUtils.isNotBlank(extractedUrlId)) {
      try {
        id = Long.parseLong(extractedUrlId);
      } catch (NumberFormatException ignored) {
        id = null;
      }
    }
    if (id != null) {
      return new UploadId(id.toString());
    } else {
      return null;
    }
  }

  @Override
  public synchronized UploadId createId() {
    return new UploadId("" + clock.millis());
  }
}
