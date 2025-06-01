package me.desair.tus.server.concatenation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.upload.UploadType;
import me.desair.tus.server.upload.concatenation.UploadConcatenationService;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConcatenationHeadRequestHandlerTest {

  private ConcatenationHeadRequestHandler handler;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  @Mock private UploadStorageService uploadStorageService;

  @Mock private UploadConcatenationService concatenationService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    handler = new ConcatenationHeadRequestHandler();
    when(uploadStorageService.getUploadConcatenationService()).thenReturn(concatenationService);
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(handler.supports(HttpMethod.GET), is(false));
    assertThat(handler.supports(HttpMethod.POST), is(false));
    assertThat(handler.supports(HttpMethod.PUT), is(false));
    assertThat(handler.supports(HttpMethod.DELETE), is(false));
    assertThat(handler.supports(HttpMethod.HEAD), is(true));
    assertThat(handler.supports(HttpMethod.OPTIONS), is(false));
    assertThat(handler.supports(HttpMethod.PATCH), is(false));
    assertThat(handler.supports(null), is(false));
  }

  @Test
  @SneakyThrows
  void testRegularUpload() {
    UploadInfo info1 = new UploadInfo();
    info1.setId(UploadId.randomUUID());
    info1.setUploadConcatHeaderValue("Impossible");
    info1.setUploadType(UploadType.REGULAR);

    when(uploadStorageService.getUploadInfo(eq(info1.getId().toString()), nullable(String.class)))
        .thenReturn(info1);
    servletRequest.setRequestURI(info1.getId().toString());

    handler.process(
        HttpMethod.HEAD,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_CONCAT), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void testPartialUpload() {
    UploadInfo info1 = new UploadInfo();
    info1.setId(UploadId.randomUUID());
    info1.setUploadConcatHeaderValue("partial");
    info1.setUploadType(UploadType.PARTIAL);

    when(uploadStorageService.getUploadInfo(eq(info1.getId().toString()), nullable(String.class)))
        .thenReturn(info1);
    servletRequest.setRequestURI(info1.getId().toString());

    handler.process(
        HttpMethod.HEAD,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_CONCAT), is("partial"));
  }

  @Test
  @SneakyThrows
  void testConcatenatedUploadWithLength() {
    UploadInfo info1 = new UploadInfo();
    info1.setId(UploadId.randomUUID());
    info1.setUploadConcatHeaderValue("final; 123 456");
    info1.setLength(10L);
    info1.setOffset(10L);
    info1.setUploadType(UploadType.CONCATENATED);

    when(uploadStorageService.getUploadInfo(eq(info1.getId().toString()), nullable(String.class)))
        .thenReturn(info1);
    servletRequest.setRequestURI(info1.getId().toString());

    handler.process(
        HttpMethod.HEAD,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_CONCAT), is("final; 123 456"));
    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_LENGTH), is("10"));
    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is("10"));

    verify(concatenationService, never()).merge(info1);
  }

  @Test
  @SneakyThrows
  void testConcatenatedUploadWithoutLength() {
    UploadInfo info1 = new UploadInfo();
    info1.setId(UploadId.randomUUID());
    info1.setUploadConcatHeaderValue("final; 123 456");
    info1.setLength(10L);
    info1.setOffset(8L);
    info1.setUploadType(UploadType.CONCATENATED);

    when(uploadStorageService.getUploadInfo(eq(info1.getId().toString()), nullable(String.class)))
        .thenReturn(info1);
    servletRequest.setRequestURI(info1.getId().toString());

    handler.process(
        HttpMethod.HEAD,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_CONCAT), is("final; 123 456"));
    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_LENGTH), is("10"));
    assertThat(servletResponse.getHeader(HttpHeader.UPLOAD_OFFSET), is(nullValue()));

    verify(concatenationService, times(1)).merge(info1);
  }
}
