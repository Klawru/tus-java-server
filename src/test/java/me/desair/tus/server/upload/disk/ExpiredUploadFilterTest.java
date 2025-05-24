package me.desair.tus.server.upload.disk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.UUID;
import lombok.SneakyThrows;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadLockingService;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpiredUploadFilterTest {

  @Mock private DiskStorageService diskStorageService;

  @Mock private UploadLockingService uploadLockingService;

  private ExpiredUploadFilter uploadFilter;

  @BeforeEach
  void setUp() {
    uploadFilter = new ExpiredUploadFilter(diskStorageService, uploadLockingService);
  }

  @Test
  @SneakyThrows
  void accept() {
    UploadInfo info = createExpiredUploadInfo();
    info.setId(new UploadId(UUID.randomUUID()));
    info.setOffset(2L);
    info.setLength(10L);
    info.updateExpiration(100L);

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    assertTrue(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptNotFound() {
    when(diskStorageService.getUploadInfo(any(UploadId.class))).thenReturn(null);
    when(uploadLockingService.isLocked(any(UploadId.class))).thenReturn(false);

    assertFalse(uploadFilter.accept(Paths.get(UUID.randomUUID().toString())));
  }

  @Test
  @SneakyThrows
  void acceptCompletedUpload() {
    UploadInfo info = createExpiredUploadInfo();
    info.setId(new UploadId(UUID.randomUUID()));
    info.setOffset(10L);
    info.setLength(10L);
    info.updateExpiration(100L);

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    // Completed uploads also expire
    assertTrue(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptInProgressButNotExpired() {
    UploadInfo info = new UploadInfo();
    info.setId(new UploadId(UUID.randomUUID()));
    info.setOffset(2L);
    info.setLength(10L);
    info.updateExpiration(172800000L);

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    assertFalse(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptLocked() {
    UploadInfo info = createExpiredUploadInfo();
    info.setId(new UploadId(UUID.randomUUID()));
    info.setOffset(8L);
    info.setLength(10L);
    info.updateExpiration(100L);

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(true);

    assertFalse(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptException() {
    UploadInfo info = createExpiredUploadInfo();
    info.setId(new UploadId(UUID.randomUUID()));
    info.setOffset(8L);
    info.setLength(10L);
    info.updateExpiration(100L);

    when(diskStorageService.getUploadInfo(info.getId())).thenThrow(new IOException());
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    assertFalse(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  private UploadInfo createExpiredUploadInfo() throws ParseException {
    final long time =
        DateFormatUtils.ISO_8601_EXTENDED_DATETIME_FORMAT.parse("2018-01-20T10:43:11").getTime();

    return new UploadInfo() {
      @Override
      protected long getCurrentTime() {
        return getExpirationTimestamp() == null ? time : time + 10000L;
      }
    };
  }
}
