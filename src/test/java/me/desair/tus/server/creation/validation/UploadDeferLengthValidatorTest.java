package me.desair.tus.server.creation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidUploadLengthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * The request MUST include one of the following headers: a) Upload-Length to indicate the size of
 * an entire upload in bytes. b) Upload-Defer-Length: 1 if upload size is not known at the time.
 */
class UploadDeferLengthValidatorTest {

  private UploadDeferLengthValidator validator;

  private MockHttpServletRequest servletRequest;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new UploadDeferLengthValidator();
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
  void validateUploadLengthPresent() {
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 300L);

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateUploadDeferLength1Present() {
    servletRequest.addHeader(HttpHeader.UPLOAD_DEFER_LENGTH, 1);

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateUploadLengthAndUploadDeferLength1Present() {
    servletRequest.addHeader(HttpHeader.UPLOAD_DEFER_LENGTH, 1);
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 300L);

    // When we validate the request
    assertThatThrownBy(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .isInstanceOf(InvalidUploadLengthException.class);

    //  InvalidUploadLengthException
  }

  @Test
  @SneakyThrows
  void validateUploadDeferLengthNot1() {
    servletRequest.addHeader(HttpHeader.UPLOAD_DEFER_LENGTH, 2);

    // When we validate the request
    assertThatThrownBy(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .isInstanceOf(InvalidUploadLengthException.class);
  }

  @Test
  @SneakyThrows
  void validateUploadLengthNotPresent() {
    // servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 300L)
    // When we validate the request
    assertThatThrownBy(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .isInstanceOf(InvalidUploadLengthException.class);
  }

  @Test
  @SneakyThrows
  void validateUploadLengthNotPresentOnFinal() {
    // servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 300L)
    servletRequest.addHeader(HttpHeader.UPLOAD_CONCAT, "final;1234 5678");

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateUploadLengthNotNumeric() {
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, "TEST");

    // When we validate the request
    assertThatThrownBy(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .isInstanceOf(InvalidUploadLengthException.class);
  }
}
