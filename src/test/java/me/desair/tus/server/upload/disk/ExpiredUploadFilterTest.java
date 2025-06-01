package me.desair.tus.server.upload.disk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.SneakyThrows;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadLockingService;
import me.desair.tus.server.upload.UuidUploadIdFactory;
import me.desair.tus.server.util.TestClock;
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

  private final UuidUploadIdFactory idFactory = new UuidUploadIdFactory();

  private ExpiredUploadFilter uploadFilter;

  private final TestClock clock = new TestClock(Instant.ofEpochMilli(1000), ZoneId.of("UTC"));

  @BeforeEach
  void setUp() {
    clock.reset();
    uploadFilter =
        new ExpiredUploadFilter(diskStorageService, uploadLockingService, idFactory, clock);
  }

  @Test
  @SneakyThrows
  void accept() {
    UploadInfo info = new UploadInfo();
    info.setId(UploadId.randomUUID());
    info.setOffset(2L);
    info.setLength(10L);
    info.setExpirationTimestamp(clock.instant().minusSeconds(100));

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
    UploadInfo info = new UploadInfo();
    info.setId(UploadId.randomUUID());
    info.setOffset(10L);
    info.setLength(10L);
    info.setExpirationTimestamp(clock.instant().minusSeconds(100));

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    // Completed uploads also expire
    assertTrue(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptInProgressButNotExpired() {
    UploadInfo info = new UploadInfo();
    info.setId(UploadId.randomUUID());
    info.setOffset(2L);
    info.setLength(10L);
    info.setExpirationTimestamp(clock.instant().plusSeconds(2000));

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    assertFalse(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptLocked() {
    UploadInfo info = new UploadInfo();
    info.setId(UploadId.randomUUID());
    info.setOffset(8L);
    info.setLength(10L);
    info.setExpirationTimestamp(Instant.ofEpochMilli(100));

    when(diskStorageService.getUploadInfo(info.getId())).thenReturn(info);
    when(uploadLockingService.isLocked(info.getId())).thenReturn(true);

    assertFalse(uploadFilter.accept(Paths.get(info.getId().toString())));
  }

  @Test
  @SneakyThrows
  void acceptException() {
    UploadInfo info = new UploadInfo();
    info.setId(UploadId.randomUUID());
    info.setOffset(8L);
    info.setLength(10L);
    info.setExpirationTimestamp(Instant.ofEpochMilli(100));

    when(diskStorageService.getUploadInfo(info.getId())).thenThrow(new IOException());
    when(uploadLockingService.isLocked(info.getId())).thenReturn(false);

    assertFalse(uploadFilter.accept(Paths.get(info.getId().toString())));
  }
}
