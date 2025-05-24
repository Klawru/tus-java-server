package me.desair.tus.server.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidTusResumableException;
import me.desair.tus.server.upload.UploadStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TusResumableValidatorTest {

  private MockHttpServletRequest servletRequest;
  private TusResumableValidator validator;
  private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new TusResumableValidator();
  }

  @Test
  @SneakyThrows
  void validateNoVersion() {
    assertThatThrownBy(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .isInstanceOf(InvalidTusResumableException.class);
  }

  @Test
  @SneakyThrows
  void validateInvalidVersion() {
    servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "2.0.0");

    assertThatThrownBy(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .isInstanceOf(InvalidTusResumableException.class);
  }

  @Test
  @SneakyThrows
  void validateValid() {
    servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");

    assertThatCode(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateNullMethod() {
    servletRequest.addHeader(HttpHeader.TUS_RESUMABLE, "1.0.0");

    assertThatCode(() -> validator.validate(null, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(validator.supports(HttpMethod.GET), is(false));
    assertThat(validator.supports(HttpMethod.POST), is(true));
    assertThat(validator.supports(HttpMethod.PUT), is(true));
    assertThat(validator.supports(HttpMethod.DELETE), is(true));
    assertThat(validator.supports(HttpMethod.HEAD), is(true));
    assertThat(validator.supports(HttpMethod.OPTIONS), is(false));
    assertThat(validator.supports(HttpMethod.PATCH), is(true));
    assertThat(validator.supports(null), is(true));
  }
}
