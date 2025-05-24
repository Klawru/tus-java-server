package me.desair.tus.server.concatenation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.UploadLengthNotAllowedOnConcatenationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class NoUploadLengthOnFinalValidatorTest {

  private NoUploadLengthOnFinalValidator validator;

  private MockHttpServletRequest servletRequest;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new NoUploadLengthOnFinalValidator();
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
  void validateFinalUploadValid() {
    servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "final;12345 235235 253523");
    // servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, "10L")

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateFinalUploadInvalid() {
    servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "final;12345 235235 253523");
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, "10L");

    // When we validate the request
    assertThatThrownBy(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .isInstanceOf(UploadLengthNotAllowedOnConcatenationException.class);
  }

  @Test
  @SneakyThrows
  void validateNotFinal1() {
    servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "partial");
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, "10L");

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateNotFinal2() {
    // servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "partial")
    // servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, "10L")

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }
}
