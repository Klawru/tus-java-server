package me.desair.tus.server.creation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.PostOnInvalidRequestURIException;
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
class PostUriValidatorTest {

  private PostUriValidator validator;

  private MockHttpServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new PostUriValidator();
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(validator.supports(HttpMethod.GET), is(false));
    assertThat(validator.supports(HttpMethod.POST), is(true));
    assertThat(validator.supports(HttpMethod.PUT), is(false));
    assertThat(validator.supports(HttpMethod.DELETE), is(false));
    assertThat(validator.supports(HttpMethod.HEAD), is(false));
    assertThat(validator.supports(HttpMethod.OPTIONS), is(false));
    assertThat(validator.supports(HttpMethod.PATCH), is(false));
    assertThat(validator.supports(null), is(false));
  }

  @Test
  @SneakyThrows
  void validateMatchingUrl() {
    servletRequest.setRequestURI("/test/upload");
    when(uploadStorageService.getUploadUri()).thenReturn("/test/upload");

    assertThatCode(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @SneakyThrows
  @ParameterizedTest(name = "{index} - {0}, requestUri={1}, storageUri={2},")
  @MethodSource("validateInvalidUrlArguments")
  void validateInvalidUrl() {
    servletRequest.setRequestURI("/test/upload/12");
    when(uploadStorageService.getUploadUri()).thenReturn("/test/upload");

    assertThatThrownBy(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .isInstanceOf(PostOnInvalidRequestURIException.class);
  }

  public static Stream<Arguments> validateInvalidUrlArguments() {
    return Stream.of(
        Arguments.of("validateInvalidUrl", "/test/upload/12", "/test/upload"),
        Arguments.of(
            "validateInvalidRegexUrl", "/users/abc123/files/upload", "/users/[0-9]+/files/upload"),
        Arguments.of(
            "validateInvalidRegexUrlPatchUrl",
            "/users/1234/files/upload/7669c72a-3f2a-451f-a3b9-9210e7a4c02f",
            "/users/[0-9]+/files/upload"));
  }

  @Test
  @SneakyThrows
  void validateMatchingRegexUrl() {
    servletRequest.setRequestURI("/users/1234/files/upload");
    when(uploadStorageService.getUploadUri()).thenReturn("/users/[0-9]+/files/upload");

    assertThatCode(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }
}
