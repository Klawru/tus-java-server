package me.desair.tus.server.checksum.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.ChecksumAlgorithmNotSupportedException;
import me.desair.tus.server.upload.UploadStorageService;
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
class ChecksumAlgorithmValidatorTest {

  private ChecksumAlgorithmValidator validator;

  private MockHttpServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = spy(new MockHttpServletRequest());
    validator = new ChecksumAlgorithmValidator();
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
    servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "sha1 1234567890");

    validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null);

    verify(servletRequest, times(1)).getHeader(HttpHeader.UPLOAD_CHECKSUM);
  }

  @Test
  @SneakyThrows
  void testNoHeader() {
    // Should contain servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, null)
    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void testInvalidHeader() {
    servletRequest.addHeader(HttpHeader.UPLOAD_CHECKSUM, "test 1234567890");

    assertThatThrownBy(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .isInstanceOf(ChecksumAlgorithmNotSupportedException.class);
  }
}
