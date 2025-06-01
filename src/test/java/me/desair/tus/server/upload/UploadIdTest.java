package me.desair.tus.server.upload;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

/** Test class for the UploadId class. */
class UploadIdTest {
  @Test
  @SneakyThrows
  void testNullConstructor() {
    assertThatThrownBy(() -> new UploadId(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  @SneakyThrows
  void testBlankConstructor() {
    assertThatThrownBy(() -> new UploadId(" \t")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SneakyThrows
  void toStringNotYetUrlSafe() {
    UploadId uploadId = new UploadId("my test id/1");
    assertEquals("my+test+id%2F1", uploadId.toString());
  }

  @Test
  @SneakyThrows
  void toStringNotYetUrlSafe2() {
    UploadId uploadId = new UploadId("id+%2F1+/+1");
    assertEquals("id+%2F1+/+1", uploadId.toString());
  }

  @Test
  @SneakyThrows
  void toStringAlreadyUrlSafe() {
    UploadId uploadId = new UploadId("my+test+id%2F1");
    assertEquals("my+test+id%2F1", uploadId.toString());
  }

  @Test
  @SneakyThrows
  void toStringWithInternalDecoderException() {
    String test = "Invalid % value";
    UploadId id = new UploadId(test);
    assertEquals("Invalid % value", id.toString());
  }

  @Test
  @SneakyThrows
  void equalsSameUrlSafeValue() {
    UploadId id1 = new UploadId("id%2F1");
    UploadId id2 = new UploadId("id/1");
    UploadId id3 = new UploadId("id/1");

    assertEquals(id1, id2);
    assertEquals(id2, id3);
    assertEquals(id1, id1);
    assertNotEquals(null, id1);
    assertNotEquals(id1, UUID.randomUUID());
  }

  @Test
  @SneakyThrows
  void hashCodeSameUrlSafeValue() {
    UploadId id1 = new UploadId("id%2F1");
    UploadId id2 = new UploadId("id/1");
    UploadId id3 = new UploadId("id/1");

    assertEquals(id1.hashCode(), id2.hashCode());
    assertEquals(id2.hashCode(), id3.hashCode());
  }
}
