package me.desair.tus.server.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.UploadNotFoundException;
import me.desair.tus.server.upload.UploadInfo;
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
class IdExistsValidatorTest {

  private IdExistsValidator validator;

  private MockHttpServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new IdExistsValidator();
  }

  @Test
  @SneakyThrows
  void validateValid() {
    UploadInfo info = new UploadInfo();
    info.setOffset(0L);
    info.setLength(10L);
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(info);

    // When we validate the request

    assertThatCode(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  @Test
  @SneakyThrows
  void validateInvalid() {
    when(uploadStorageService.getUploadInfo(nullable(String.class), nullable(String.class)))
        .thenReturn(null);

    // When we validate the request
    assertThatThrownBy(
            () -> validator.validate(HttpMethod.PATCH, servletRequest, uploadStorageService, null))
        .isInstanceOf(UploadNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(validator.supports(HttpMethod.GET), is(true));
    assertThat(validator.supports(HttpMethod.POST), is(false));
    assertThat(validator.supports(HttpMethod.PUT), is(false));
    assertThat(validator.supports(HttpMethod.DELETE), is(true));
    assertThat(validator.supports(HttpMethod.HEAD), is(true));
    assertThat(validator.supports(HttpMethod.OPTIONS), is(false));
    assertThat(validator.supports(HttpMethod.PATCH), is(true));
    assertThat(validator.supports(null), is(false));
  }
}
