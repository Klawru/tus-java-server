package me.desair.tus.server.checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class ChecksumAlgorithmTest {

  @Test
  @SneakyThrows
  void getMessageDigest() {
    assertNotNull(ChecksumAlgorithm.MD5.getMessageDigest());
    assertNotNull(ChecksumAlgorithm.SHA1.getMessageDigest());
    assertNotNull(ChecksumAlgorithm.SHA256.getMessageDigest());
    assertNotNull(ChecksumAlgorithm.SHA384.getMessageDigest());
    assertNotNull(ChecksumAlgorithm.SHA512.getMessageDigest());
  }

  @Test
  @SneakyThrows
  void forTusName() {
    assertEquals(ChecksumAlgorithm.MD5, ChecksumAlgorithm.forTusName("md5"));
    assertEquals(ChecksumAlgorithm.SHA1, ChecksumAlgorithm.forTusName("sha1"));
    assertEquals(ChecksumAlgorithm.SHA256, ChecksumAlgorithm.forTusName("sha256"));
    assertEquals(ChecksumAlgorithm.SHA384, ChecksumAlgorithm.forTusName("sha384"));
    assertEquals(ChecksumAlgorithm.SHA512, ChecksumAlgorithm.forTusName("sha512"));
    assertNull(ChecksumAlgorithm.forTusName("test"));
  }

  @Test
  @SneakyThrows
  void forUploadChecksumHeader() {
    assertEquals(
        ChecksumAlgorithm.MD5, ChecksumAlgorithm.forUploadChecksumHeader("md5 1234567890"));
    assertEquals(
        ChecksumAlgorithm.SHA1, ChecksumAlgorithm.forUploadChecksumHeader("sha1 1234567890"));
    assertEquals(
        ChecksumAlgorithm.SHA256, ChecksumAlgorithm.forUploadChecksumHeader("sha256 1234567890"));
    assertEquals(
        ChecksumAlgorithm.SHA384, ChecksumAlgorithm.forUploadChecksumHeader("sha384 1234567890"));
    assertEquals(
        ChecksumAlgorithm.SHA512, ChecksumAlgorithm.forUploadChecksumHeader("sha512 1234567890"));
    assertNull(ChecksumAlgorithm.forUploadChecksumHeader("test 1234567890"));
  }

  @Test
  @SneakyThrows
  void testToString() {
    assertEquals("md5", ChecksumAlgorithm.MD5.toString());
    assertEquals("sha1", ChecksumAlgorithm.SHA1.toString());
    assertEquals("sha256", ChecksumAlgorithm.SHA256.toString());
    assertEquals("sha384", ChecksumAlgorithm.SHA384.toString());
    assertEquals("sha512", ChecksumAlgorithm.SHA512.toString());
  }
}
