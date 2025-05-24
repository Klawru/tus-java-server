package me.desair.tus.server;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.EnumSet;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HttpMethodTest {

  @Test
  @SneakyThrows
  void forName() {
    assertEquals(HttpMethod.DELETE, HttpMethod.forName("delete"));
    assertEquals(HttpMethod.GET, HttpMethod.forName("get"));
    assertEquals(HttpMethod.HEAD, HttpMethod.forName("head"));
    assertEquals(HttpMethod.PATCH, HttpMethod.forName("patch"));
    assertEquals(HttpMethod.POST, HttpMethod.forName("post"));
    assertEquals(HttpMethod.PUT, HttpMethod.forName("put"));
    assertEquals(HttpMethod.OPTIONS, HttpMethod.forName("options"));
    assertNull(HttpMethod.forName("test"));
  }

  @Test
  @SneakyThrows
  void getMethodNormal() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("patch");

    assertEquals(
        HttpMethod.PATCH,
        HttpMethod.getMethodIfSupported(servletRequest, EnumSet.allOf(HttpMethod.class)));
  }

  @Test
  @SneakyThrows
  void getMethodOverridden() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("post");
    servletRequest.addHeader(HttpHeader.METHOD_OVERRIDE, "patch");

    assertEquals(
        HttpMethod.PATCH,
        HttpMethod.getMethodIfSupported(servletRequest, EnumSet.allOf(HttpMethod.class)));
  }

  @Test
  @SneakyThrows
  void getMethodOverriddenDoesNotExist() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("post");
    servletRequest.addHeader(HttpHeader.METHOD_OVERRIDE, "test");

    assertEquals(
        HttpMethod.POST,
        HttpMethod.getMethodIfSupported(servletRequest, EnumSet.allOf(HttpMethod.class)));
  }

  @Test
  @SneakyThrows
  void getMethodNull() {
    EnumSet<HttpMethod> httpMethods = EnumSet.allOf(HttpMethod.class);

    assertThatThrownBy(() -> HttpMethod.getMethodIfSupported(null, httpMethods))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @SneakyThrows
  void getMethodNotSupported() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("put");

    assertNull(HttpMethod.getMethodIfSupported(servletRequest, EnumSet.noneOf(HttpMethod.class)));
  }

  @Test
  @SneakyThrows
  void getMethodRequestNotExists() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setMethod("test");

    assertNull(HttpMethod.getMethodIfSupported(servletRequest, EnumSet.noneOf(HttpMethod.class)));
  }
}
