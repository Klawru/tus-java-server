package me.desair.tus.server.upload.disk;

import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import me.desair.tus.server.upload.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Directory stream filter that only accepts uploads that are still in progress and expired */
@RequiredArgsConstructor
public class ExpiredUploadFilter implements DirectoryStream.Filter<Path> {

  private static final Logger log = LoggerFactory.getLogger(ExpiredUploadFilter.class);

  private final DiskStorageService diskStorageService;
  private final UploadLockingService uploadLockingService;
  private final UploadIdFactory idFactory;
  private final Clock clock;

  @Override
  public boolean accept(Path upload) {
    UploadId id = null;
    try {
      id = idFactory.readUploadIdFromString(upload.getFileName().toString());
      UploadInfo info = diskStorageService.getUploadInfo(id);

      if (info != null && info.isExpired(clock.instant()) && !uploadLockingService.isLocked(id)) {
        return true;
      }

    } catch (Exception ex) {
      if (log.isDebugEnabled()) {
        log.debug("Not able to determine state of upload " + id, ex);
      }
    }

    return false;
  }
}
