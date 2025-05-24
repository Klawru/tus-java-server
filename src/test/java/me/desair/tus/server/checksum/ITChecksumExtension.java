package me.desair.tus.server.checksum;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import lombok.SneakyThrows;
import me.desair.tus.server.AbstractTusExtensionIntegrationTest;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.ChecksumAlgorithmNotSupportedException;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadChecksumMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ITChecksumExtension extends AbstractTusExtensionIntegrationTest {

  @BeforeEach
  void setUp() {
    servletRequest = spy(new MockHttpServletRequest());
    servletResponse = new MockHttpServletResponse();
    tusFeature = new ChecksumExtension();
    uploadInfo = null;
  }

  @Test
  @SneakyThrows
  void testOptions() {
    setRequestHeaders();

    executeCall(HttpMethod.OPTIONS, false);

    assertResponseHeader(HttpHeader.TUS_EXTENSION, "checksum", "checksum-trailer");
    assertResponseHeader(
        HttpHeader.TUS_CHECKSUM_ALGORITHM, "md5", "sha1", "sha256", "sha384", "sha512");
  }

  @Test
  @SneakyThrows
  void testInvalidAlgorithm() {
    servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "test 1234567890");
    servletRequest.setContent("Test content".getBytes());

    assertThatThrownBy(() -> executeCall(HttpMethod.PATCH, false))
        .isInstanceOf(ChecksumAlgorithmNotSupportedException.class);
  }

  @Test
  @SneakyThrows
  void testValidChecksumTrailerHeader() {
    String content =
        """
        8\r
        Mozilla \r
        A\r
        Developer \r
        7\r
        Network\r
        0\r
        Upload-Checksum: sha1 zYR9iS5Rya+WoH1fEyfKqqdPWWE=\r
        \r
        """;

    servletRequest.addHeader(HttpHeader.TRANSFER_ENCODING, "chunked");
    servletRequest.setContent(content.getBytes());

    assertThatCode(() -> executeCall(HttpMethod.PATCH, true)).doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void testValidChecksumNormalHeader() throws TusException, IOException {
    String content = "Mozilla Developer Network";

    servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "sha1 zYR9iS5Rya+WoH1fEyfKqqdPWWE=");
    servletRequest.setContent(content.getBytes());

    executeCall(HttpMethod.PATCH, true);

    verify(servletRequest, atLeastOnce()).getHeader(HttpHeader.UPLOAD_CHECKSUM);
  }

  @Test
  @SneakyThrows
  void testInvalidChecksumTrailerHeader() {
    String content =
        """
        8\r
        Mozilla \r
        A\r
        Developer \r
        7\r
        Network\r
        0\r
        Upload-Checksum: sha1 zYR9iS5Rya+WoH1fEyfKqqdPWW=\r
        \r
        """;

    servletRequest.addHeader(HttpHeader.TRANSFER_ENCODING, "chunked");
    servletRequest.setContent(content.getBytes());
    assertThatThrownBy(() -> executeCall(HttpMethod.PATCH, true))
        .isInstanceOf(UploadChecksumMismatchException.class);
  }

  @Test
  @SneakyThrows
  void testInvalidChecksumNormalHeader() {
    String content = "Mozilla Developer Network";

    servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "sha1 zYR9iS5Rya+WoH1fEyfKqqdPWW=");
    servletRequest.setContent(content.getBytes());

    assertThatThrownBy(() -> executeCall(HttpMethod.PATCH, true))
        .isInstanceOf(UploadChecksumMismatchException.class);
  }

  @Test
  @SneakyThrows
  void testNoChecksum() {
    String content = "Mozilla Developer Network";

    servletRequest.setContent(content.getBytes());

    assertThatCode(() -> executeCall(HttpMethod.PATCH, true)).doesNotThrowAnyException();
  }
}
