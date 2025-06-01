package me.desair.tus.server.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

import lombok.SneakyThrows;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.upload.UploadInfo;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class HttpUtilsTest {

  @Test
  void testGetCreatorIpAddressesWithoutXforwardedFor() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setRemoteAddr("10.11.12.13");

    String ip = HttpUtils.buildRemoteIpList(servletRequest);
    assertThat(ip).isEqualTo("10.11.12.13");
  }

  @Test
  @SneakyThrows
  void testGetCreatorIpAddressesWithXforwardedFor() {
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setRemoteAddr("10.11.12.13");
    servletRequest.addHeader(HttpHeader.X_FORWARDED_FOR, "24.23.22.21, 192.168.1.1");

    UploadInfo info = new UploadInfo(HttpUtils.buildRemoteIpList(servletRequest));
    MatcherAssert.assertThat(
        info.getCreatorIpAddresses(), is("24.23.22.21, 192.168.1.1, 10.11.12.13"));
  }

  @Test
  void testUrlSaveStringWithDecodableInput() {
    String input = "test/value";

    String result = HttpUtils.urlSaveString(input);

    assertThat(result).isEqualTo("test%2Fvalue");
  }

  @Test
  void testUrlSaveStringWithUndecodableInput() {
    String input = "%INVALID";
    String result = HttpUtils.urlSaveString(input);

    // The input is not a valid URL-encoded string, so it should be returned unchanged
    assertThat(result).isEqualTo(input);
  }

  @Test
  void testUrlSaveStringWithNullInput() {
    String result = HttpUtils.urlSaveString(null);

    // Null input should return null
    assertNull(result);
  }
}
