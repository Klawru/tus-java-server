package me.desair.tus.server.upload.disk;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import lombok.SneakyThrows;
import me.desair.tus.server.upload.*;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiskLockingServiceTest {

  public static final String UPLOAD_URL = "/upload/test";
  private DiskLockingService lockingService;

  @Spy private UploadIdFactory idFactory = new UuidUploadIdFactory();

  private static Path storagePath;

  @BeforeAll
  static void setupDataFolder() throws IOException {
    storagePath = Paths.get("target", "tus", "data").toAbsolutePath();
    Files.createDirectories(storagePath);
  }

  @AfterAll
  static void destroyDataFolder() throws IOException {
    FileUtils.deleteDirectory(storagePath.toFile());
  }

  @BeforeEach
  void setUp() {
    lockingService = new DiskLockingService(idFactory, storagePath.toString());
  }

  @Test
  @SneakyThrows
  void lockUploadByUri() {
    UploadLock uploadLock =
        lockingService.lockUploadByUri("/upload/test/000003f1-a850-49de-af03-997272d834c9");

    assertThat(uploadLock, not(nullValue()));

    uploadLock.release();
  }

  @Test
  @SneakyThrows
  void isLockedTrue() {
    UploadLock uploadLock =
        lockingService.lockUploadByUri("/upload/test/000003f1-a850-49de-af03-997272d834c9");

    assertThat(
        lockingService.isLocked(new UploadId("000003f1-a850-49de-af03-997272d834c9")), is(true));

    uploadLock.release();
  }

  @Test
  @SneakyThrows
  void isLockedFalse() {
    UploadLock uploadLock =
        lockingService.lockUploadByUri("/upload/test/000003f1-a850-49de-af03-997272d834c9");
    uploadLock.release();

    assertThat(
        lockingService.isLocked(new UploadId("000003f1-a850-49de-af03-997272d834c9")), is(false));
  }

  @Test
  @SneakyThrows
  void lockUploadNotExists() {
    when(idFactory.readUploadIdFromUri(nullable(String.class))).thenReturn(null);

    UploadLock uploadLock =
        lockingService.lockUploadByUri("/upload/test/000003f1-a850-49de-af03-997272d834c9");

    assertThat(uploadLock, nullValue());
  }

  @Test
  @SneakyThrows
  void cleanupStaleLocks() {
    Path locksPath = storagePath.resolve("locks");

    String activeLock = "000003f1-a850-49de-af03-997272d83111";
    UploadLock uploadLock = lockingService.lockUploadByUri("/upload/test/" + activeLock);

    assertThat(uploadLock, not(nullValue()));

    String staleLock = "8fb7c41c-8900-46d0-a5e7-9ac6dab6b000";
    Files.createFile(locksPath.resolve(staleLock));

    String recentLock = "1c1ccf80-f2a0-4286-9cf4-39e0cdb2a999";
    Files.createFile(locksPath.resolve(recentLock));

    Files.setLastModifiedTime(
        locksPath.resolve(staleLock), FileTime.fromMillis(System.currentTimeMillis() - 20000));
    Files.setLastModifiedTime(
        locksPath.resolve(activeLock), FileTime.fromMillis(System.currentTimeMillis() - 20000));

    assertTrue(Files.exists(locksPath.resolve(staleLock)));
    assertTrue(Files.exists(locksPath.resolve(activeLock)));
    assertTrue(Files.exists(locksPath.resolve(recentLock)));

    lockingService.cleanupStaleLocks();

    assertFalse(Files.exists(locksPath.resolve(staleLock)));
    assertTrue(Files.exists(locksPath.resolve(activeLock)));
    assertTrue(Files.exists(locksPath.resolve(recentLock)));

    uploadLock.release();
  }
}
