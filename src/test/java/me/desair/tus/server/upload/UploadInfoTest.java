package me.desair.tus.server.upload;

import static me.desair.tus.server.util.MapMatcher.hasSize;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import me.desair.tus.server.util.HttpUtils;
import me.desair.tus.server.util.TestClock;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test cases for the UploadInfo class. */
class UploadInfoTest {

  private final TestClock testClock = new TestClock(Instant.ofEpochMilli(1000), ZoneId.of("UTC"));
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @SneakyThrows
  void hasMetadata() {
    UploadInfo info = new UploadInfo();
    info.getMetadata().put("Encoded", "Metadata");
    assertTrue(info.hasMetadata());
  }

  @Test
  @SneakyThrows
  void hasMetadataFalse() {
    UploadInfo info = new UploadInfo();
    info.getMetadata().clear();
    assertFalse(info.hasMetadata());
  }

  @Test
  @SneakyThrows
  void testGetMetadataMultipleValues() {
    Map<String, String> decodedMetadata =
        HttpUtils.decodedMetadata(
            "filename d29ybGRfZG9taW5hdGlvbiBwbGFuLnBkZg==,"
                + "filesize MTEya2I=, "
                + "mimetype \tYXBwbGljYXRpb24vcGRm , "
                + "scanned , ,, "
                + "user\t546L5LqU \t    ");

    assertThat(
        decodedMetadata,
        allOf(
            hasSize(5),
            hasEntry("filename", "world_domination plan.pdf"),
            hasEntry("filesize", "112kb"),
            hasEntry("mimetype", "application/pdf"),
            hasEntry("scanned", null),
            hasEntry("user", "王五")));
  }

  @Test
  @SneakyThrows
  void testGetMetadataSingleValues() {
    UploadInfo info = new UploadInfo();
    info.getMetadata()
        .putAll(HttpUtils.decodedMetadata("filename d29ybGRfZG9taW5hdGlvbl9wbGFuLnBkZg=="));

    assertThat(
        info.getMetadata(), allOf(hasSize(1), hasEntry("filename", "world_domination_plan.pdf")));
  }

  @Test
  @SneakyThrows
  void testGetMetadataNull() {
    UploadInfo info = new UploadInfo();
    info.getMetadata().clear();
    assertTrue(info.getMetadata().isEmpty());
  }

  @Test
  @SneakyThrows
  void hasLength() {
    UploadInfo info = new UploadInfo();
    info.setSize(10L);
    assertTrue(info.hasLength());
  }

  @Test
  @SneakyThrows
  void hasLengthFalse() {
    UploadInfo info = new UploadInfo();
    info.setSize(null);
    assertFalse(info.hasLength());
  }

  @Test
  @SneakyThrows
  void isUploadInProgressNoLengthNoOffset() {
    UploadInfo info = new UploadInfo();
    info.setSize(null);
    info.setOffset(0);
    assertTrue(info.isUploadInProgress());
  }

  @Test
  @SneakyThrows
  void isUploadInProgressNoLengthWithOffset() {
    UploadInfo info = new UploadInfo();
    info.setSize(null);
    info.setOffset(10L);
    assertTrue(info.isUploadInProgress());
  }

  @Test
  @SneakyThrows
  void isUploadInProgressOffsetDoesNotMatchLength() {
    UploadInfo info = new UploadInfo();
    info.setSize(10L);
    info.setOffset(8L);
    assertTrue(info.isUploadInProgress());
  }

  @Test
  @SneakyThrows
  void isUploadInProgressOffsetMatchesLength() {
    UploadInfo info = new UploadInfo();
    info.setSize(10L);
    info.setOffset(10L);
    assertFalse(info.isUploadInProgress());
  }

  @Test
  @SneakyThrows
  void testEquals() {
    UploadInfo info1 = new UploadInfo();
    info1.setSize(10L);
    info1.setOffset(5L);
    info1.getMetadata().put("Encoded", "Metadata");
    info1.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    UploadInfo info2 = new UploadInfo();
    info2.setSize(10L);
    info2.setOffset(5L);
    info2.getMetadata().put("Encoded", "Metadata");
    info2.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    UploadInfo info3 = new UploadInfo();
    info3.setSize(9L);
    info3.setOffset(5L);
    info2.getMetadata().put("Encoded", "Metadata");
    info3.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    UploadInfo info4 = new UploadInfo();
    info4.setSize(10L);
    info4.setOffset(6L);
    info2.getMetadata().put("Encoded", "Metadata");
    info4.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    UploadInfo info5 = new UploadInfo();
    info5.setSize(10L);
    info5.setOffset(5L);
    info5.getMetadata().put("Encoded", "Any");
    info5.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    UploadInfo info6 = new UploadInfo();
    info6.setSize(10L);
    info6.setOffset(5L);
    info6.getMetadata().put("Encoded", "Metadata");
    info6.setId(new UploadId("1911e8a4-6939-490c-c58b-a5d70f8d91fb"));

    assertEquals(info1, info1);
    assertEquals(info1, info2);
    assertNotEquals(null, info1);
    assertNotEquals(info1, new Object());
    assertNotEquals(info1, info3);
    assertNotEquals(info1, info4);
    assertNotEquals(info1, info5);
    assertNotEquals(info1, info6);
  }

  @Test
  @SneakyThrows
  void testHashCode() {
    UploadInfo info1 = new UploadInfo();
    info1.setSize(10L);
    info1.setOffset(5L);
    info1.getMetadata().put("Encoded", "Metadata");
    info1.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    UploadInfo info2 = new UploadInfo();
    info2.setSize(10L);
    info2.setOffset(5L);
    info2.getMetadata().put("Encoded", "Metadata");
    info2.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));

    assertEquals(info1.hashCode(), info2.hashCode());
  }

  @Test
  @SneakyThrows
  void testGetNameAndTypeWithMetadata() {
    UploadInfo info = new UploadInfo();
    info.getMetadata().putAll(HttpUtils.decodedMetadata("name dGVzdC5qcGc=,type aW1hZ2UvanBlZw=="));

    assertThat(info.getFileName(), is("test.jpg"));
    assertThat(info.getFileMimeType(), is("image/jpeg"));
  }

  @Test
  @SneakyThrows
  void testGetNameAndTypeWithoutMetadata() {
    UploadInfo info = new UploadInfo();
    final UploadId id = UploadId.randomUUID();
    info.setId(id);

    assertThat(info.getFileName(), is(id.toString()));
    assertThat(info.getFileMimeType(), is("application/octet-stream"));
  }

  @Test
  @SneakyThrows
  void testExpiration() {
    UploadInfo info1 = new UploadInfo();
    assertFalse(info1.isExpired(testClock.instant()));

    UploadInfo info2 = new UploadInfo();
    info2.setExpirationTimestamp(testClock.instant().plus(2, ChronoUnit.DAYS));
    assertFalse(info2.isExpired(testClock.instant()));

    UploadInfo info3 = new UploadInfo();
    info3.setExpirationTimestamp(testClock.instant());
    assertTrue(info3.isExpired(testClock.instant().plus(2, ChronoUnit.DAYS)));
  }

  @Test
  @SneakyThrows
  void testGetCreationTimestamp() {
    UploadInfo info = new UploadInfo();

    Assertions.assertThat(info.getCreationTimestamp())
        .isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
  }

  @Test
  @SneakyThrows
  void testGetCreatorIpAddressesNull() {
    UploadInfo info = new UploadInfo();
    assertThat(info.getCreatorIpAddresses(), nullValue());
  }

  @Test
  void checkJsonSerialization() throws JsonProcessingException {
    // Check that the object can be serialized and descended to JSON
    UploadInfo uploadInfo = new UploadInfo("10.11.12.13");
    uploadInfo.setId(new UploadId("1911e8a4-6939-490c-b58b-a5d70f8d91fb"));
    uploadInfo.setSize(100L);
    uploadInfo.setOffset(10L);
    uploadInfo.getMetadata().put("Encoded", "Metadata");
    uploadInfo.setUploadType(UploadType.REGULAR);
    uploadInfo.setOwnerKey("ownerKey");
    uploadInfo.setExpirationTimestamp(testClock.instant().plus(2, ChronoUnit.DAYS));
    uploadInfo.setConcatenationPartIds(List.of("1", "2", "3"));
    uploadInfo.setUploadConcatHeaderValue("header_value");
    uploadInfo.getStorage().put("foo", "bar");
    // When
    String jsonString = objectMapper.writeValueAsString(uploadInfo);
    UploadInfo actual = objectMapper.readValue(jsonString, UploadInfo.class);
    // Then
    Assertions.assertThat(actual)
        .isEqualTo(uploadInfo)
        .usingRecursiveComparison()
        .isEqualTo(uploadInfo);
  }
}
