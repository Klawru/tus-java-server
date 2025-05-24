package me.desair.tus.server.concatenation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.UUID;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.PatchOnFinalUploadNotAllowedException;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.upload.UploadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatchFinalUploadValidatorTest {

  private PatchFinalUploadValidator validator;

  private MockHttpServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new PatchFinalUploadValidator();
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(validator.supports(HttpMethod.GET), is(false));
    assertThat(validator.supports(HttpMethod.POST), is(false));
    assertThat(validator.supports(HttpMethod.PUT), is(false));
    assertThat(validator.supports(HttpMethod.DELETE), is(false));
    assertThat(validator.supports(HttpMethod.HEAD), is(false));
    assertThat(validator.supports(HttpMethod.OPTIONS), is(false));
    assertThat(validator.supports(HttpMethod.PATCH), is(true));
    assertThat(validator.supports(null), is(false));
  }

  @Test
  @SneakyThrows
  void testValid() {
    UploadInfo info1 = new UploadInfo();
    info1.setId(new UploadId(UUID.randomUUID()));
    info1.setUploadType(UploadType.REGULAR);

    UploadInfo info2 = new UploadInfo();
    info2.setId(new UploadId(UUID.randomUUID()));
    info2.setUploadType(UploadType.PARTIAL);

    UploadInfo info3 = new UploadInfo();
    info3.setId(new UploadId(UUID.randomUUID()));
    info3.setUploadType(null);

    when(uploadStorageService.getUploadInfo(eq(info1.getId().toString()), nullable(String.class)))
        .thenReturn(info1);
    when(uploadStorageService.getUploadInfo(eq(info2.getId().toString()), nullable(String.class)))
        .thenReturn(info2);
    when(uploadStorageService.getUploadInfo(eq(info3.getId().toString()), nullable(String.class)))
        .thenReturn(info3);

    // When we validate the requests
    servletRequest.setRequestURI(info1.getId().toString());
    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();

    servletRequest.setRequestURI(info2.getId().toString());
    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();

    servletRequest.setRequestURI(info3.getId().toString());
    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void testValidNotFound() {
    // When we validate the request
    servletRequest.setRequestURI("/upload/test");
    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void testInvalidFinal() {
    UploadInfo info1 = new UploadInfo();
    info1.setId(new UploadId(UUID.randomUUID()));
    info1.setUploadType(UploadType.CONCATENATED);

    when(uploadStorageService.getUploadInfo(eq(info1.getId().toString()), nullable(String.class)))
        .thenReturn(info1);

    // When we validate the request
    servletRequest.setRequestURI(info1.getId().toString());
    assertThatThrownBy(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .isInstanceOf(PatchOnFinalUploadNotAllowedException.class);
  }
}
