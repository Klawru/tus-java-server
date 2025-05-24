package me.desair.tus.server.checksum;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Arrays;
import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ChecksumOptionsRequestHandlerTest {

  private ChecksumOptionsRequestHandler handler;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    handler = new ChecksumOptionsRequestHandler();
  }

  @Test
  @SneakyThrows
  void processListExtensions() {

    handler.process(
        HttpMethod.OPTIONS,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        null,
        null);

    assertThat(
        Arrays.asList(servletResponse.getHeader(HttpHeader.TUS_EXTENSION).split(",")),
        containsInAnyOrder("checksum", "checksum-trailer"));

    assertThat(
        Arrays.asList(servletResponse.getHeader(HttpHeader.TUS_CHECKSUM_ALGORITHM).split(",")),
        containsInAnyOrder("md5", "sha1", "sha256", "sha384", "sha512"));
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(handler.supports(HttpMethod.GET), is(false));
    assertThat(handler.supports(HttpMethod.POST), is(false));
    assertThat(handler.supports(HttpMethod.PUT), is(false));
    assertThat(handler.supports(HttpMethod.DELETE), is(false));
    assertThat(handler.supports(HttpMethod.HEAD), is(false));
    assertThat(handler.supports(HttpMethod.OPTIONS), is(true));
    assertThat(handler.supports(HttpMethod.PATCH), is(false));
    assertThat(handler.supports(null), is(false));
  }
}
