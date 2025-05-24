package me.desair.tus.server.concatenation;

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

class ConcatenationOptionsRequestHandlerTest {

  private ConcatenationOptionsRequestHandler handler;

  private MockHttpServletRequest servletRequest;

  private MockHttpServletResponse servletResponse;

  @BeforeEach
  void setUp() {
    servletRequest = new MockHttpServletRequest();
    servletResponse = new MockHttpServletResponse();
    handler = new ConcatenationOptionsRequestHandler();
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
        containsInAnyOrder("concatenation", "concatenation-unfinished"));
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
