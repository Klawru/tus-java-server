package me.desair.tus.server.creation.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.InvalidContentLengthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PostEmptyRequestValidatorTest {

  private PostEmptyRequestValidator validator;

  private MockHttpServletRequest servletRequest;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new PostEmptyRequestValidator();
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
  void validateMissingContentLength() {
    // We don't set a content length header
    // servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 3L)

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateContentLengthZero() {
    servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 0L);

    // When we validate the request
    assertThatCode(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateContentLengthNotZero() {
    servletRequest.addHeader(HttpHeader.CONTENT_LENGTH, 10L);

    // When we validate the request
    assertThatThrownBy(() -> validator.validate(HttpMethod.POST, servletRequest, null, null))
        .isInstanceOf(InvalidContentLengthException.class);
  }
}
