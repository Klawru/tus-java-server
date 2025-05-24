package me.desair.tus.server.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Test cases for the HttpChunkedEncodingInputStream class. */
class HttpChunkedEncodingInputStreamTest {

  Map<String, List<String>> trailerHeaders;

  @BeforeEach
  void setUp() {
    trailerHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  @Test
  @SneakyThrows
  void chunkedWithoutHeaders() {
    String content =
        """
        4\r
        Wiki\r
        5\r
        pedia\r
        D\r
         in

        \rchunks.\r
        0\r
        \r
        """;

    HttpChunkedEncodingInputStream inputStream =
        new HttpChunkedEncodingInputStream(IOUtils.toInputStream(content, StandardCharsets.UTF_8));

    String expectedContent = "Wikipedia in\n\n\rchunks.";

    StringWriter writer = new StringWriter();
    IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
    inputStream.close();

    assertEquals(expectedContent, writer.toString());
  }

  @Test
  @SneakyThrows
  void chunkedWithHeaders() {
    String content =
        """
        8\r
        Mozilla \r
        A\r
        Developer \r
        7\r
        Network\r
        0\r
        Expires: Wed, 21 Oct 2015 07:28:00 GMT\r
        \r
        """;
    HttpChunkedEncodingInputStream inputStream =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(content, StandardCharsets.UTF_8), trailerHeaders);

    String expectedContent = "Mozilla Developer Network";

    StringWriter writer = new StringWriter();
    IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
    inputStream.close();

    assertEquals(expectedContent, writer.toString());

    assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", trailerHeaders.get("expires").get(0));
  }

  @Test
  @SneakyThrows
  void chunkedWithFoldedHeaders() {
    String content =
        """
        8\r
        Mozilla \r
        A\r
        Developer \r
        7\r
        Network\r
        0\r
        Expires: Wed, 21 Oct 2015
         07:28:00 GMT\r
        Cookie: ABC
        \tDEF\r
        \r
        """;

    HttpChunkedEncodingInputStream inputStream =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(content, StandardCharsets.UTF_8), trailerHeaders);

    String expectedContent = "Mozilla Developer Network";

    StringWriter writer = new StringWriter();
    IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
    inputStream.close();

    assertEquals(expectedContent, writer.toString());

    assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", trailerHeaders.get("expires").get(0));
    assertEquals("ABC DEF", trailerHeaders.get("cookie").get(0));
  }

  @Test
  @SneakyThrows
  void testChunkedInputStream() {
    String correctInput =
        """
        10;key="value\r
        newline"\r
        1234567890123456\r
        5\r
        12345\r
        0\r
        Footer1: abcde\r
        Footer2: fghij\r
        """;

    String correctResult = "123456789012345612345";

    // Test for when buffer is larger than chunk size
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(correctInput, StandardCharsets.UTF_8), trailerHeaders);
    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, StandardCharsets.UTF_8);
    in.close();

    assertEquals(correctResult, writer.toString());

    assertEquals("abcde", trailerHeaders.get("footer1").get(0));
    assertEquals("fghij", trailerHeaders.get("footer2").get(0));
  }

  @Test
  @SneakyThrows
  void testCorruptChunkedInputStream1() {
    // missing \r\n at the end of the first chunk
    String corruptInput =
        """
        10;key="val\\ue"\r
        123456789012345\r
        5\r
        12345\r
        0\r
        Footer1: abcde\r
        Footer2: fghij\r
        """;

    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(corruptInput, StandardCharsets.UTF_8), trailerHeaders);
    StringWriter writer = new StringWriter();
    assertThatThrownBy(() -> IOUtils.copy(in, writer, StandardCharsets.UTF_8))
        .isInstanceOf(IOException.class);
  }

  @Test
  @SneakyThrows
  void testEmptyChunkedInputStream() {
    String input = "0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);
    StringWriter writer = new StringWriter();
    IOUtils.copy(in, writer, StandardCharsets.UTF_8);
    assertEquals(0, writer.toString().length());
  }

  @Test
  @SneakyThrows
  void testReadPartialByteArray() {
    String input = "A\r\n0123456789\r\n0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);

    byte[] byteArray = new byte[5];
    in.read(byteArray);
    in.close();

    assertEquals("01234", new String(byteArray));
  }

  @Test
  @SneakyThrows
  void testReadByte() {
    String input = "4\r\n0123\r\n6\r\n456789\r\n0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);

    assertEquals('0', (char) in.read());
    assertEquals('1', (char) in.read());
    assertEquals('2', (char) in.read());
    assertEquals('3', (char) in.read());
    assertEquals('4', (char) in.read());
    assertEquals('5', (char) in.read());
    assertEquals('6', (char) in.read());
    assertEquals('7', (char) in.read());
    assertEquals('8', (char) in.read());
    assertEquals('9', (char) in.read());
    in.close();
  }

  @Test
  @SneakyThrows
  void testReadEof() {
    String input = "A\r\n0123456789\r\n0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);

    byte[] byteArray = new byte[10];
    in.read(byteArray);

    assertEquals(-1, in.read());
    assertEquals(-1, in.read());
  }

  @Test
  @SneakyThrows
  void testReadEof2() {
    String input = "A\r\n0123456789\r\n0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);

    byte[] byteArray = new byte[10];
    in.read(byteArray);

    assertEquals(-1, in.read(byteArray));
    assertEquals(-1, in.read(byteArray));
  }

  @Test
  @SneakyThrows
  void testReadClosed() {
    String input = "A\r\n0123456789\r\n0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);

    in.close();

    byte[] byteArray = new byte[10];
    assertThatCode(() -> assertEquals(-1, in.read(byteArray))).isInstanceOf(IOException.class);

    assertThatCode(() -> assertEquals(-1, in.read())).isInstanceOf(IOException.class);

    // double close has not effect
    in.close();
  }

  @Test
  @SneakyThrows
  @SuppressWarnings("resource")
  void testNullInputstream() {
    assertThatThrownBy(() -> new HttpChunkedEncodingInputStream(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @SneakyThrows
  void testNegativeChunkSize() {
    String input = "-A\r\n0123456789\r\n0\r\n";
    InputStream in =
        new HttpChunkedEncodingInputStream(
            IOUtils.toInputStream(input, StandardCharsets.UTF_8), trailerHeaders);

    byte[] byteArray = new byte[10];
    assertThatThrownBy(() -> in.read(byteArray)).isInstanceOf(IOException.class);
  }
}
