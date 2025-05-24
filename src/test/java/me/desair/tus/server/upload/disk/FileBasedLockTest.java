package me.desair.tus.server.upload.disk;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import lombok.Cleanup;
import lombok.SneakyThrows;
import me.desair.tus.server.exception.UploadAlreadyLockedException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FileBasedLockTest {

  private static Path storagePath;

  @BeforeAll
  static void setupDataFolder() throws IOException {
    storagePath = Paths.get("target", "tus", "locks").toAbsolutePath();
    Files.createDirectories(storagePath);
  }

  @Test
  @SneakyThrows
  void testLockRelease() {
    UUID test = UUID.randomUUID();
    FileBasedLock lock =
        new FileBasedLock("/test/upload/" + test, storagePath.resolve(test.toString()));
    lock.close();
    assertFalse(Files.exists(storagePath.resolve(test.toString())));
  }

  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void testOverlappingLock() {
    UUID test = UUID.randomUUID();
    Path path = storagePath.resolve(test.toString());
    @Cleanup FileBasedLock lock1 = new FileBasedLock("/test/upload/" + test, path);
    assertThatThrownBy(() -> new FileBasedLock("/test/upload/" + test, path))
        .isInstanceOf(UploadAlreadyLockedException.class);
    lock1.close();
  }

  @Test
  @SneakyThrows
  void testAlreadyLocked() {
    UUID test1 = UUID.randomUUID();
    Path path1 = storagePath.resolve(test1.toString());
    @Cleanup FileBasedLock lock1 = new FileBasedLock("/test/upload/" + test1, path1);

    assertThatThrownBy(
            () -> {
              FileBasedLock lock2 =
                  new FileBasedLock("/test/upload/" + test1, path1) {
                    @Override
                    protected FileChannel createFileChannel() throws IOException {
                      FileChannel channel = createFileChannelMock();
                      doReturn(null).when(channel).tryLock(anyLong(), anyLong(), anyBoolean());
                      return channel;
                    }
                  };
              lock2.close();
            })
        .isInstanceOf(UploadAlreadyLockedException.class);

    lock1.close();
  }

  @Test
  @SneakyThrows
  void testLockReleaseLockRelease() {
    UUID test = UUID.randomUUID();
    Path path = storagePath.resolve(test.toString());
    FileBasedLock lock = new FileBasedLock("/test/upload/" + test, path);
    lock.close();
    assertFalse(Files.exists(path));
    lock = new FileBasedLock("/test/upload/" + test, path);
    lock.close();
    assertFalse(Files.exists(path));
  }

  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void testLockIoException() {
    // Create directory on place where lock file will be
    UUID test = UUID.randomUUID();
    Path path = storagePath.resolve(test.toString());
    assertThatCode(() -> Files.createDirectories(path)).doesNotThrowAnyException();
    assertThatThrownBy(() -> new FileBasedLock("/test/upload/" + test, path))
        .isInstanceOf(IOException.class);
  }

  private FileChannel createFileChannelMock() {
    return spy(FileChannel.class);
  }
}
