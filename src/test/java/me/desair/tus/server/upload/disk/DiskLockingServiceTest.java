package me.desair.tus.server.upload.disk;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.UUID;
import lombok.SneakyThrows;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadIdFactory;
import me.desair.tus.server.upload.UploadLock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiskLockingServiceTest {

  public static final String UPLOAD_URL = "/upload/test";
  private DiskLockingService lockingService;

  @Mock private UploadIdFactory idFactory;

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
    reset(idFactory);
    when(idFactory.getUploadUri()).thenReturn(UPLOAD_URL);
    when(idFactory.createId()).thenReturn(new UploadId(UUID.randomUUID()));
    when(idFactory.readUploadId(nullable(String.class)))
        .then(
            new Answer<UploadId>() {
              @Override
              public UploadId answer(InvocationOnMock invocation) throws Throwable {
                return new UploadId(
                    StringUtils.substringAfter(
                        invocation.getArguments()[0].toString(), UPLOAD_URL + "/"));
              }
            });

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
    reset(idFactory);
    when(idFactory.readUploadId(nullable(String.class))).thenReturn(null);

    UploadLock uploadLock =
        lockingService.lockUploadByUri("/upload/test/000003f1-a850-49de-af03-997272d834c9");

    assertThat(uploadLock, nullValue());
  }

  @Test
  @SneakyThrows
  void cleanupStaleLocks() {
    Path locksPath = storagePath.resolve("locks");

    String activeLock = "000003f1-a850-49de-af03-997272d834c9";
    UploadLock uploadLock = lockingService.lockUploadByUri("/upload/test/" + activeLock);

    assertThat(uploadLock, not(nullValue()));

    String staleLock = UUID.randomUUID().toString();
    Files.createFile(locksPath.resolve(staleLock));

    String recentLock = UUID.randomUUID().toString();
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
