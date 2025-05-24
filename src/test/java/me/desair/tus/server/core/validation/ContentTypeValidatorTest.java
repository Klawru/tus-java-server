package me.desair.tus.server.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidContentTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ContentTypeValidatorTest {

  private ContentTypeValidator validator;

  private MockHttpServletRequest servletRequest;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new ContentTypeValidator();
  }

  @Test
  @SneakyThrows
  void validateValid() {
    servletRequest.addHeader(
        HttpHeader.CONTENT_TYPE, ContentTypeValidator.APPLICATION_OFFSET_OCTET_STREAM);

    assertThatCode(() -> validator.validate(HttpMethod.PATCH, servletRequest, null, null))
            .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateInvalidHeader() {
    servletRequest.addHeader(HttpHeader.CONTENT_TYPE, "application/octet-stream");

    assertThatThrownBy(() -> validator.validate(HttpMethod.PATCH, servletRequest, null, null))
        .isInstanceOf(InvalidContentTypeException.class);
  }

  @Test
  @SneakyThrows
  void validateMissingHeader() {
    // We don't set the header
    // servletRequest.addHeader(HttpHeader.CONTENT_TYPE,
    // ContentTypeValidator.APPLICATION_OFFSET_OCTET_STREAM)

    assertThatThrownBy(() -> validator.validate(HttpMethod.PATCH, servletRequest, null, null))
        .isInstanceOf(InvalidContentTypeException.class);
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
