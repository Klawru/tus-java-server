package me.desair.tus.server.core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.TusFileUploadService;
import me.desair.tus.server.util.TusServletRequest;
import me.desair.tus.server.util.TusServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CoreDefaultResponseHeadersHandlerTest {

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  private CoreDefaultResponseHeadersHandler handler;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    handler = new CoreDefaultResponseHeadersHandler();
  }

  @Test
  @SneakyThrows
  void supports() {
    assertThat(handler.supports(HttpMethod.GET), is(true));
    assertThat(handler.supports(HttpMethod.POST), is(true));
    assertThat(handler.supports(HttpMethod.PUT), is(true));
    assertThat(handler.supports(HttpMethod.DELETE), is(true));
    assertThat(handler.supports(HttpMethod.HEAD), is(true));
    assertThat(handler.supports(HttpMethod.OPTIONS), is(true));
    assertThat(handler.supports(HttpMethod.PATCH), is(true));
    assertThat(handler.supports(null), is(true));
  }

  @Test
  @SneakyThrows
  void process() {
    handler.process(
        HttpMethod.PATCH,
        new TusServletRequest(servletRequest),
        new TusServletResponse(servletResponse),
        null,
        null);

    assertThat(
        servletResponse.getHeader(HttpHeader.TUS_RESUMABLE),
        is(TusFileUploadService.TUS_API_VERSION));
    assertThat(servletResponse.getHeader(HttpHeader.CONTENT_LENGTH), is("0"));
  }
}
