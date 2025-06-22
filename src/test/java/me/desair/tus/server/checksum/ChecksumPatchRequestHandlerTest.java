package me.desair.tus.server.checksum;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.ChecksumAlgorithmNotSupportedException;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.exception.UploadChecksumMismatchException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.TusServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChecksumPatchRequestHandlerTest {

  private ChecksumPatchRequestHandler handler;

  @Mock private TusServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() throws IOException, TusException {
    handler = new ChecksumPatchRequestHandler();

    UploadInfo info = new UploadInfo();
    info.setOffset(2L);
    info.setLength(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(handler.supports(HttpMethod.GET), is(false));
    assertThat(handler.supports(HttpMethod.POST), is(false));
    assertThat(handler.supports(HttpMethod.PUT), is(false));
    assertThat(handler.supports(HttpMethod.DELETE), is(false));
    assertThat(handler.supports(HttpMethod.HEAD), is(false));
    assertThat(handler.supports(HttpMethod.OPTIONS), is(false));
    assertThat(handler.supports(HttpMethod.PATCH), is(true));
    assertThat(handler.supports(null), is(false));
  }

  @Test
  @SneakyThrows
  void testValidHeaderAndChecksum() throws TusException, IOException {
    when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn("sha1 1234567890");
    when(servletRequest.getCalculatedChecksum(ArgumentMatchers.any(ChecksumAlgorithm.class)))
        .thenReturn("1234567890");
    when(servletRequest.hasCalculatedChecksum()).thenReturn(true);

    handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null);

    verify(servletRequest, times(1)).getCalculatedChecksum(any(ChecksumAlgorithm.class));
  }

  @Test
  @SneakyThrows
  void testValidHeaderAndInvalidChecksum() {
    when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn("sha1 1234567890");
    when(servletRequest.getCalculatedChecksum(ArgumentMatchers.any(ChecksumAlgorithm.class)))
        .thenReturn("0123456789");
    when(servletRequest.hasCalculatedChecksum()).thenReturn(true);

    assertThatThrownBy(
            () ->
                handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null))
        .isInstanceOf(UploadChecksumMismatchException.class);
  }

  @Test
  @SneakyThrows
  void testNoHeader() {
    when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn(null);

    handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null);

    verify(servletRequest, never()).getCalculatedChecksum(any(ChecksumAlgorithm.class));
  }

  @Test
  @SneakyThrows
  void testInvalidHeader() {
    when(servletRequest.getHeader(HttpHeader.UPLOAD_CHECKSUM)).thenReturn("test 1234567890");
    when(servletRequest.hasCalculatedChecksum()).thenReturn(true);

    assertThatThrownBy(
            () ->
                handler.process(HttpMethod.PATCH, servletRequest, null, uploadStorageService, null))
        .isInstanceOf(ChecksumAlgorithmNotSupportedException.class);
  }
}
