package me.desair.tus.server.upload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.time.Instant;
import java.time.ZoneId;
import lombok.SneakyThrows;
import me.desair.tus.server.util.TestClock;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeBasedUploadIdFactoryTest {

  private UploadIdFactory idFactory;
  private final TestClock clock = new TestClock(Instant.ofEpochMilli(1000), ZoneId.of("UTC"));

  @BeforeEach
  void setUp() {
    idFactory = new TimeBasedUploadIdFactory(clock);
  }

  @Test
  @SneakyThrows
  void setUploadUriNull() {
    assertThatThrownBy(() -> idFactory.setUploadUri(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @SneakyThrows
  void setUploadUriNoTrailingSlash() {
    idFactory.setUploadUri("/test/upload");
    assertThat(idFactory.getUploadUri(), is("/test/upload"));
  }

  @Test
  @SneakyThrows
  void setUploadUriWithTrailingSlash() {
    idFactory.setUploadUri("/test/upload/");
    assertThat(idFactory.getUploadUri(), is("/test/upload/"));
  }

  @Test
  @SneakyThrows
  void setUploadUriBlank() {
    assertThatThrownBy(() -> idFactory.setUploadUri(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SneakyThrows
  void setUploadUriNoStartingSlash() {
    assertThatThrownBy(() -> idFactory.setUploadUri("test/upload/"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SneakyThrows
  void setUploadUriEndsWithDollar() {
    assertThatThrownBy(() -> idFactory.setUploadUri("/test/upload$"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SneakyThrows
  void readUploadId() {
    idFactory.setUploadUri("/test/upload");

    assertThat(
        idFactory.readUploadIdFromUri("/test/upload/1546152320043"), hasToString("1546152320043"));
  }

  @Test
  @SneakyThrows
  void readUploadIdRegex() {
    idFactory.setUploadUri("/users/[0-9]+/files/upload");

    assertThat(
        idFactory.readUploadIdFromUri("/users/1337/files/upload/1546152320043"),
        hasToString("1546152320043"));
  }

  @Test
  @SneakyThrows
  void readUploadIdTrailingSlash() {
    idFactory.setUploadUri("/test/upload/");

    assertThat(
        idFactory.readUploadIdFromUri("/test/upload/1546152320043"), hasToString("1546152320043"));
  }

  @Test
  @SneakyThrows
  void readUploadIdRegexTrailingSlash() {
    idFactory.setUploadUri("/users/[0-9]+/files/upload/");

    assertThat(
        idFactory.readUploadIdFromUri("/users/123456789/files/upload/1546152320043"),
        hasToString("1546152320043"));
  }

  @Test
  @SneakyThrows
  void readUploadIdNoUuid() {
    idFactory.setUploadUri("/test/upload");

    assertThat(idFactory.readUploadIdFromUri("/test/upload/not-a-time-value"), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void readUploadIdRegexNoMatch() {
    idFactory.setUploadUri("/users/[0-9]+/files/upload");

    assertThat(idFactory.readUploadIdFromUri("/users/files/upload/1546152320043"), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void createId() {
    UploadId id = idFactory.createId();
    assertThat(id, not(nullValue()));

    Assertions.assertThat(id).hasToString("" + clock.instant().toEpochMilli());
  }
}
