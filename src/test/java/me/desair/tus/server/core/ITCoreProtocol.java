package me.desair.tus.server.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import lombok.SneakyThrows;
import me.desair.tus.server.AbstractTusExtensionIntegrationTest;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidContentLengthException;
import me.desair.tus.server.exception.InvalidContentTypeException;
import me.desair.tus.server.exception.InvalidTusResumableException;
import me.desair.tus.server.exception.UnsupportedMethodException;
import me.desair.tus.server.exception.UploadNotFoundException;
import me.desair.tus.server.exception.UploadOffsetMismatchException;
import me.desair.tus.server.upload.UploadInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ITCoreProtocol extends AbstractTusExtensionIntegrationTest {

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    tusFeature = new CoreProtocol();
    uploadInfo = null;
  }

  @Test
  @SneakyThrows
  void getName() {
    assertThat(tusFeature.getName(), is("core"));
  }

  @Test
  @SneakyThrows
  void testUnsupportedHttpMethod() {
    prepareUploadInfo(2L, 10L);
    setRequestHeaders(HttpHeader.TUS_RESUMABLE);

    assertThatThrownBy(() -> executeCall(HttpMethod.forName("TEST"), false))
        .isInstanceOf(UnsupportedMethodException.class);
  }

  @Test
  @SneakyThrows
  void testHeadWithLength() {
    prepareUploadInfo(2L, 10L);
    setRequestHeaders(HttpHeader.TUS_RESUMABLE);

    executeCall(HttpMethod.HEAD, false);

    assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
    assertResponseHeader(HttpHeader.UPLOAD_OFFSET, "2");
    assertResponseHeader(HttpHeader.UPLOAD_LENGTH, "10");
    assertResponseHeader(HttpHeader.CACHE_CONTROL, "no-store");
    assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  @SneakyThrows
  void testHeadWithoutLength() {
    prepareUploadInfo(2L, null);
    setRequestHeaders(HttpHeader.TUS_RESUMABLE);

    executeCall(HttpMethod.HEAD, false);

    assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
    assertResponseHeader(HttpHeader.UPLOAD_OFFSET, "2");
    assertResponseHeader(HttpHeader.UPLOAD_LENGTH, (String) null);
    assertResponseHeader(HttpHeader.CACHE_CONTROL, "no-store");
    assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  @SneakyThrows
  void testHeadNotFound() {
    // We don't prepare an upload info
    setRequestHeaders(HttpHeader.TUS_RESUMABLE);

    assertThatThrownBy(() -> executeCall(HttpMethod.HEAD, false))
        .isInstanceOf(UploadNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void testHeadInvalidVersion() {
    setRequestHeaders();
    prepareUploadInfo(2L, null);
    servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "2.0.0");

    assertThatThrownBy(() -> executeCall(HttpMethod.HEAD, false))
        .isInstanceOf(InvalidTusResumableException.class);
  }

  @Test
  @SneakyThrows
  void testPatchSuccess() {
    prepareUploadInfo(2L, 10L);
    setRequestHeaders(
        HttpHeader.TUS_RESUMABLE,
        HttpHeader.CONTENT_TYPE,
        HttpHeader.UPLOAD_OFFSET,
        HttpHeader.CONTENT_LENGTH);

    executeCall(HttpMethod.PATCH, false);

    verify(uploadStorageService, times(1)).append(any(UploadInfo.class), any(InputStream.class));

    assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
    assertResponseHeader(HttpHeader.UPLOAD_OFFSET, "2");
    assertResponseHeader(HttpHeader.UPLOAD_LENGTH, (String) null);
    assertResponseHeader(HttpHeader.CACHE_CONTROL, (String) null);
    assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  @SneakyThrows
  void testPatchInvalidContentType() {
    prepareUploadInfo(2L, 10L);
    setRequestHeaders(
        HttpHeader.TUS_RESUMABLE, HttpHeader.UPLOAD_OFFSET, HttpHeader.CONTENT_LENGTH);

    assertThatThrownBy(() -> executeCall(HttpMethod.PATCH, false))
        .isInstanceOf(InvalidContentTypeException.class);
  }

  @Test
  @SneakyThrows
  void testPatchInvalidUploadOffset() {
    prepareUploadInfo(2L, 10L);
    setRequestHeaders(HttpHeader.TUS_RESUMABLE, HttpHeader.CONTENT_TYPE, HttpHeader.CONTENT_LENGTH);
    servletRequest.addHeader(HttpHeader.UPLOAD_OFFSET, 5);

    assertThatThrownBy(() -> executeCall(HttpMethod.PATCH, false))
        .isInstanceOf(UploadOffsetMismatchException.class);
  }

  @Test
  @SneakyThrows
  void testPatchInvalidContentLength() {
    prepareUploadInfo(2L, 10L);
    setRequestHeaders(HttpHeader.TUS_RESUMABLE, HttpHeader.CONTENT_TYPE, HttpHeader.UPLOAD_OFFSET);
    servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 9);

    assertThatThrownBy(() -> executeCall(HttpMethod.PATCH, false))
        .isInstanceOf(InvalidContentLengthException.class);
  }

  @Test
  @SneakyThrows
  void testOptionsWithMaxSize() {
    when(uploadStorageService.getMaxUploadSize()).thenReturn(107374182400L);

    setRequestHeaders();

    executeCall(HttpMethod.OPTIONS, false);

    assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
    assertResponseHeader(HttpHeader.TUS_VERSION, "1.0.0");
    assertResponseHeader(HttpHeader.TUS_MAX_SIZE, "107374182400");
    assertResponseHeader(HttpHeader.TUS_EXTENSION, (String) null);
    assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  @SneakyThrows
  void testOptionsWithNoMaxSize() {
    when(uploadStorageService.getMaxUploadSize()).thenReturn(0L);

    setRequestHeaders();

    executeCall(HttpMethod.OPTIONS, false);

    assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
    assertResponseHeader(HttpHeader.TUS_VERSION, "1.0.0");
    assertResponseHeader(HttpHeader.TUS_MAX_SIZE, (String) null);
    assertResponseHeader(HttpHeader.TUS_EXTENSION, (String) null);
    assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Test
  @SneakyThrows
  void testOptionsIgnoreTusResumable() {
    when(uploadStorageService.getMaxUploadSize()).thenReturn(10L);

    setRequestHeaders();
    servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "2.0.0");

    executeCall(HttpMethod.OPTIONS, false);

    assertResponseHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");
    assertResponseHeader(HttpHeader.TUS_VERSION, "1.0.0");
    assertResponseHeader(HttpHeader.TUS_MAX_SIZE, "10");
    assertResponseHeader(HttpHeader.TUS_EXTENSION, (String) null);
    assertResponseStatus(HttpServletResponse.SC_NO_CONTENT);
  }
}
