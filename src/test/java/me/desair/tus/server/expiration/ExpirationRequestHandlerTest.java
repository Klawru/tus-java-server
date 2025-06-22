package me.desair.tus.server.expiration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.TimeZone;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.TestClock;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.apache.commons.lang3.time.TimeZones;
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
class ExpirationRequestHandlerTest {

  private ExpirationRequestHandler handler;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  @Mock private UploadStorageService uploadStorageService;

  private final TestClock clock =
      new TestClock(
          Instant.parse("2018-01-20T10:43:11Z"), TimeZone.getTimeZone(TimeZones.GMT_ID).toZoneId());

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    handler = new ExpirationRequestHandler(clock);
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(handler.supports(HttpMethod.GET), is(false));
    assertThat(handler.supports(HttpMethod.POST), is(true));
    assertThat(handler.supports(HttpMethod.PUT), is(false));
    assertThat(handler.supports(HttpMethod.DELETE), is(false));
    assertThat(handler.supports(HttpMethod.HEAD), is(false));
    assertThat(handler.supports(HttpMethod.OPTIONS), is(false));
    assertThat(handler.supports(HttpMethod.PATCH), is(true));
    assertThat(handler.supports(null), is(false));
  }

  @Test
  @SneakyThrows
  void testCreatedUpload() {
    UploadInfo info = new UploadInfo();
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(172800000L);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    tusResponse.setHeader(HttpHeader.LOCATION, "/tus/upload/12345");
    handler.process(
        HttpMethod.POST,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    verify(uploadStorageService, times(1)).update(info);
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is("1516617791000"));
  }

  @Test
  @SneakyThrows
  void testInProgressUpload() {
    UploadInfo info = new UploadInfo();
    info.setOffset(2L);
    info.setSize(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(172800000L);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    verify(uploadStorageService, times(1)).update(info);
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is("1516617791000"));
  }

  @Test
  @SneakyThrows
  void testNoUpload() {
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(null);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(172800000L);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    verify(uploadStorageService, never()).update(any(UploadInfo.class));
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void testFinishedUpload() {
    UploadInfo info = new UploadInfo();
    info.setOffset(10L);
    info.setSize(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(172800000L);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    // Upload Expires header must always be set
    verify(uploadStorageService, times(1)).update(info);
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is("1516617791000"));
  }

  @Test
  @SneakyThrows
  void testNullExpiration() {
    UploadInfo info = new UploadInfo();
    info.setOffset(8L);
    info.setSize(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(null);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    verify(uploadStorageService, never()).update(any(UploadInfo.class));
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void testZeroExpiration() {
    UploadInfo info = new UploadInfo();
    info.setOffset(8L);
    info.setSize(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(0L);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    verify(uploadStorageService, never()).update(any(UploadInfo.class));
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void testNegativeExpiration() {
    UploadInfo info = new UploadInfo();
    info.setOffset(8L);
    info.setSize(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);
    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(-10L);

    TusServletResponse tusResponse = new TusServletResponse(this.servletResponse);
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        tusResponse,
        uploadStorageService,
        null);

    verify(uploadStorageService, never()).update(any(UploadInfo.class));
    assertThat(tusResponse.getHeader(HttpHeader.UPLOAD_EXPIRES), is(nullValue()));
  }
}
