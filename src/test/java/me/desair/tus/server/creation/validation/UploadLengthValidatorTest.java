package me.desair.tus.server.creation.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.MaxUploadLengthExceededException;
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
class UploadLengthValidatorTest {

  private UploadLengthValidator validator;

  private MockHttpServletRequest servletRequest;

  @Mock private UploadStorageService uploadStorageService;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    validator = new UploadLengthValidator();
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

  @SneakyThrows
  @ParameterizedTest(name = "{index} - {0}, maxUploadSize={1}")
  @MethodSource("validateArguments")
  void validate(String testName, Long maxUploadSize) {
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 300L);
    if (maxUploadSize != null) {
      when(uploadStorageService.getMaxUploadSize()).thenReturn(maxUploadSize);
    }
    // When
    assertThatCode(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .doesNotThrowAnyException();
  }

  public static Stream<Arguments> validateArguments() {
    return Stream.of(
        Arguments.of("validateNoMaxUploadLength", 0L),
        Arguments.of("validateBelowMaxUploadLength", 400L),
        Arguments.of("validateEqualMaxUploadLength", 300L),
        Arguments.of("validateNoUploadLength", null));
  }

  @Test
  @SneakyThrows
  void validateAboveMaxUploadLength() {
    when(uploadStorageService.getMaxUploadSize()).thenReturn(200L);
    servletRequest.addHeader(HttpHeader.UPLOAD_LENGTH, 300L);

    assertThatThrownBy(
            () -> validator.validate(HttpMethod.POST, servletRequest, uploadStorageService, null))
        .isInstanceOf(MaxUploadLengthExceededException.class);
  }
}
