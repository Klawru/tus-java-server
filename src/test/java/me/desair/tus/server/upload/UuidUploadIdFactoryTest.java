package me.desair.tus.server.upload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test cases for the UuidUploadIdFactory. */
class UuidUploadIdFactoryTest {

  private UploadIdFactory idFactory;

  @BeforeEach
  void setUp() {
    idFactory = new UuidUploadIdFactory();
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
        idFactory.readUploadId("/test/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"),
        hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
  }

  @Test
  @SneakyThrows
  void readUploadIdRegex() {
    idFactory.setUploadUri("/users/[0-9]+/files/upload");

    assertThat(
        idFactory.readUploadId("/users/1337/files/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"),
        hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
  }

  @Test
  @SneakyThrows
  void readUploadIdTrailingSlash() {
    idFactory.setUploadUri("/test/upload/");

    assertThat(
        idFactory.readUploadId("/test/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"),
        hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
  }

  @Test
  @SneakyThrows
  void readUploadIdRegexTrailingSlash() {
    idFactory.setUploadUri("/users/[0-9]+/files/upload/");

    assertThat(
        idFactory.readUploadId(
            "/users/123456789/files/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"),
        hasToString("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
  }

  @Test
  @SneakyThrows
  void readUploadIdNoUuid() {
    idFactory.setUploadUri("/test/upload");

    assertThat(idFactory.readUploadId("/test/upload/not-a-uuid-value"), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void readUploadIdRegexNoMatch() {
    idFactory.setUploadUri("/users/[0-9]+/files/upload");

    assertThat(
        idFactory.readUploadId("/users/files/upload/1911e8a4-6939-490c-b58b-a5d70f8d91fb"),
        is(nullValue()));
  }

  @Test
  @SneakyThrows
  void createId() {
    assertThat(idFactory.createId(), not(nullValue()));
  }
}
