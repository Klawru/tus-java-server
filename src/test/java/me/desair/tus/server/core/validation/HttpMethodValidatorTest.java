package me.desair.tus.server.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.UnsupportedMethodException;
import me.desair.tus.server.upload.UploadStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Test cases for the {@link HttpMethodValidator}. */
class HttpMethodValidatorTest {

  private MockHttpServletRequest servletRequest;
  private HttpMethodValidator validator;
  private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new HttpMethodValidator();
  }

  @Test
  @SneakyThrows
  void validateValid() {
    assertThatCode(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateInvalid() {
    assertThatThrownBy(() -> validator.validate(null, servletRequest, uploadStorageService, null))
        .isInstanceOf(UnsupportedMethodException.class);
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(validator.supports(HttpMethod.GET), is(true));
    assertThat(validator.supports(HttpMethod.POST), is(true));
    assertThat(validator.supports(HttpMethod.PUT), is(true));
    assertThat(validator.supports(HttpMethod.DELETE), is(true));
    assertThat(validator.supports(HttpMethod.HEAD), is(true));
    assertThat(validator.supports(HttpMethod.OPTIONS), is(true));
    assertThat(validator.supports(HttpMethod.PATCH), is(true));
    assertThat(validator.supports(null), is(true));
  }
}
