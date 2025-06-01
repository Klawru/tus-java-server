package me.desair.tus.server.creation;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The Server MUST acknowledge a successful upload creation with the 201 Created status. The Server
 * MUST set the Location header to the URL of the created resource. This URL MAY be absolute or
 * relative.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreationPostRequestHandlerTest {

  private CreationPostRequestHandler handler;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    handler = new CreationPostRequestHandler();
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
    assertThat(handler.supports(HttpMethod.PATCH), is(false));
    assertThat(handler.supports(null), is(false));
  }

  @Test
  @SneakyThrows
  void processWithLengthAndMetadata() {
    servletRequest.setRequestURI("/test/upload");
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 10L);
    servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, "encoded-metadata");

    UploadId id = UploadId.randomUUID();
    when(uploadStorageService.create(
            ArgumentMatchers.any(UploadInfo.class), nullable(String.class)))
        .then(
            new Answer<UploadInfo>() {
              @Override
              public UploadInfo answer(InvocationOnMock invocation) throws Throwable {
                UploadInfo upload = invocation.getArgument(0);
                assertThat(upload.getLength(), is(10L));
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("encoded-metadata", null);
                assertThat(upload.getMetadata(), is(map));

                upload.setId(id);

                return upload;
              }
            });

    handler.process(
        HttpMethod.POST,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    verify(uploadStorageService, times(1))
        .create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class));
    assertThat(servletResponse.getHeader(HttpHeader.LOCATION), endsWith("/test/upload/" + id));
    assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_CREATED));
  }

  @Test
  @SneakyThrows
  void processWithLengthAndNoMetadata() {
    servletRequest.setRequestURI("/test/upload");
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 10L);
    // servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, null)

    final UploadId id = UploadId.randomUUID();
    when(uploadStorageService.create(
            ArgumentMatchers.any(UploadInfo.class), nullable(String.class)))
        .then(
            new Answer<UploadInfo>() {
              @Override
              public UploadInfo answer(InvocationOnMock invocation) throws Throwable {
                UploadInfo upload = invocation.getArgument(0);
                assertThat(upload.getLength(), is(10L));
                Assertions.assertThat(upload.getMetadata()).isEmpty();

                upload.setId(id);

                return upload;
              }
            });

    handler.process(
        HttpMethod.POST,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    verify(uploadStorageService, times(1))
        .create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class));
    assertThat(servletResponse.getHeader(HttpHeader.LOCATION), endsWith("/test/upload/" + id));
    assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_CREATED));
  }

  @Test
  @SneakyThrows
  void processWithNoLengthAndMetadata() {
    servletRequest.setRequestURI("/test/upload");
    // servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, null)
    servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, "encoded-metadata");

    final UploadId id = UploadId.randomUUID();
    when(uploadStorageService.create(
            ArgumentMatchers.any(UploadInfo.class), nullable(String.class)))
        .then(
            new Answer<UploadInfo>() {
              @Override
              public UploadInfo answer(InvocationOnMock invocation) throws Throwable {
                UploadInfo upload = invocation.getArgument(0);
                assertThat(upload.getLength(), is(nullValue()));
                assertThat(upload.getMetadata(), hasEntry("encoded-metadata", null));

                upload.setId(id);

                return upload;
              }
            });

    handler.process(
        HttpMethod.POST,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    verify(uploadStorageService, times(1))
        .create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class));
    assertThat(servletResponse.getHeader(HttpHeader.LOCATION), endsWith("/test/upload/" + id));
    assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_CREATED));
  }

  @Test
  @SneakyThrows
  void processWithNoLengthAndNoMetadata() {
    servletRequest.setRequestURI("/test/upload");
    // servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, null)
    // servletRequest.addHeader(HttpHeader.UPLOAD_METADATA, null)

    final UploadId id = UploadId.randomUUID();
    when(uploadStorageService.create(
            ArgumentMatchers.any(UploadInfo.class), nullable(String.class)))
        .then(
            new Answer<UploadInfo>() {
              @Override
              public UploadInfo answer(InvocationOnMock invocation) throws Throwable {
                UploadInfo upload = invocation.getArgument(0);
                assertThat(upload.getLength(), is(nullValue()));
                Assertions.assertThat(upload.getMetadata()).isEmpty();

                upload.setId(id);

                return upload;
              }
            });

    handler.process(
        HttpMethod.POST,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        uploadStorageService,
        null);

    verify(uploadStorageService, times(1))
        .create(ArgumentMatchers.any(UploadInfo.class), nullable(String.class));
    assertThat(servletResponse.getHeader(HttpHeader.LOCATION), endsWith("/test/upload/" + id));
    assertThat(servletResponse.getStatus(), is(HttpServletResponse.SC_CREATED));
  }
}
