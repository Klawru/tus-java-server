package me.desair.tus.server.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidContentLengthException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentLengthValidatorTest {

  private ContentLengthValidator validator;

  private MockHttpServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new ContentLengthValidator();
  }

  @SneakyThrows
  @ParameterizedTest(name = "{index} - {0}, offset={1}, contentLength={2},")
  @MethodSource("validateValidLengthArguments")
  void validateValidLength(String testName, long offset, long contentLength) {
    UploadInfo info = new UploadInfo();
    info.setOffset(offset);
    info.setLength(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);

    servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, contentLength);

    // When we validate the request
    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  public static Stream<Arguments> validateValidLengthArguments() {
    return Stream.of(
        Arguments.of("WhenInitialUpload", 0L, 10L),
        Arguments.of("WhenInProgressUpload", 5L, 5L),
        Arguments.of("WhenPartialUpload", 2L, 3L));
  }

  @SneakyThrows
  @ParameterizedTest(name = "{index} - {0}, offset={1}, contentLength={2},")
  @MethodSource("validateInvalidLengthArguments")
  void validateInvalidLength(String testName, long offset, long contentLength) {
    UploadInfo info = new UploadInfo();
    info.setOffset(offset);
    info.setLength(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);

    servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, contentLength);

    // When we validate the request
    assertThatThrownBy(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .isInstanceOf(InvalidContentLengthException.class);
  }

  public static Stream<Arguments> validateInvalidLengthArguments() {
    return Stream.of(
        Arguments.of("WhenInitialUpload", 0L, 11L),
        Arguments.of("WhenInProgressUpload", 5L, 6L),
        Arguments.of("WhenPartialUpload", 2L, 10L));
  }

  @Test
  @SneakyThrows
  void validateMissingUploadInfo() {
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(null);

    servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 3L);

    // When we validate the request
    assertThatCode(()-> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
            .doesNotThrowAnyException();
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
}
