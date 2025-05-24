package me.desair.tus.server.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.time.TimeZones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class TusServletResponseTest {

  private static final FastDateFormat DATE_FORMAT =
      FastDateFormat.getInstance(
          "yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone(TimeZones.GMT_ID), Locale.US);

  private static final DateFormat mockDateFormat =
      new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);

  private TusServletResponse tusServletResponse;
  private MockHttpServletResponse servletResponse;

  @BeforeEach
  void setUp() {
    servletResponse = new MockHttpServletResponse();
    tusServletResponse = new TusServletResponse(servletResponse);
  }

  @Test
  @SneakyThrows
  void setDateHeader() {
    tusServletResponse.setDateHeader("TEST", DATE_FORMAT.parse("2018-01-03 22:34:14").getTime());
    tusServletResponse.setDateHeader("TEST", DATE_FORMAT.parse("2018-01-03 22:38:14").getTime());

    assertThat(
        tusServletResponse.getHeader("TEST"),
        is("" + DATE_FORMAT.parse("2018-01-03 22:38:14").getTime()));
    List<String> responseDateHeaders =
        servletResponse.getHeaders("TEST").stream()
            .map(
                s -> {
                  try {
                    return "" + mockDateFormat.parse(s).getTime();
                  } catch (ParseException e) {
                    return "" + new Date().getTime();
                  }
                })
            .collect(Collectors.toList());
    assertThat(
        responseDateHeaders, contains("" + DATE_FORMAT.parse("2018-01-03 22:38:14").getTime()));
  }

  @Test
  @SneakyThrows
  void addDateHeader() {
    tusServletResponse.addDateHeader("TEST", DATE_FORMAT.parse("2018-01-03 22:34:12").getTime());
    tusServletResponse.addDateHeader("TEST", DATE_FORMAT.parse("2018-01-03 22:38:14").getTime());

    assertThat(
        tusServletResponse.getHeader("TEST"),
        is("" + DATE_FORMAT.parse("2018-01-03 22:34:12").getTime()));
    List<String> responseDateHeaders =
        servletResponse.getHeaders("TEST").stream()
            .map(
                s -> {
                  try {
                    return "" + mockDateFormat.parse(s).getTime();
                  } catch (ParseException e) {
                    return "" + new Date().getTime();
                  }
                })
            .collect(Collectors.toList());
    assertThat(
        responseDateHeaders,
        containsInAnyOrder(
            "" + DATE_FORMAT.parse("2018-01-03 22:34:12").getTime(),
            "" + DATE_FORMAT.parse("2018-01-03 22:38:14").getTime()));
  }

  @Test
  @SneakyThrows
  void setHeader() {
    tusServletResponse.setHeader("TEST", "foo");
    tusServletResponse.setHeader("TEST", "bar");

    assertThat(tusServletResponse.getHeader("TEST"), is("bar"));
    assertThat(servletResponse.getHeaders("TEST"), contains("bar"));
  }

  @Test
  @SneakyThrows
  void addHeader() {
    tusServletResponse.addHeader("TEST", "foo");
    tusServletResponse.addHeader("TEST", "bar");

    assertThat(tusServletResponse.getHeader("TEST"), is("foo"));
    assertThat(servletResponse.getHeaders("TEST"), containsInAnyOrder("foo", "bar"));
  }

  @Test
  @SneakyThrows
  void setIntHeader() {
    tusServletResponse.setIntHeader("TEST", 1);
    tusServletResponse.setIntHeader("TEST", 2);

    assertThat(tusServletResponse.getHeader("TEST"), is("2"));
    assertThat(servletResponse.getHeaders("TEST"), contains("2"));
  }

  @Test
  @SneakyThrows
  void addIntHeader() {
    tusServletResponse.addIntHeader("TEST", 1);
    tusServletResponse.addIntHeader("TEST", 2);

    assertThat(tusServletResponse.getHeader("TEST"), is("1"));
    assertThat(servletResponse.getHeaders("TEST"), contains("1", "2"));
  }

  @Test
  @SneakyThrows
  void getHeaderNull() {
    assertThat(tusServletResponse.getHeader("TEST"), is(nullValue()));
  }
}
