package me.desair.tus.server.upload.concatenation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.SneakyThrows;
import me.desair.tus.server.exception.UploadNotFoundException;
import me.desair.tus.server.upload.UploadId;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VirtualConcatenationServiceTest {

  @Mock private UploadStorageService uploadStorageService;

  private VirtualConcatenationService concatenationService;

  @BeforeEach
  void setUp() {
    concatenationService = new VirtualConcatenationService(uploadStorageService);
  }

  @Test
  @SneakyThrows
  void concat() {
    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize(5L);
    child1.setOffset(5L);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize(10L);
    child2.setOffset(10L);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child2);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    concatenationService.concat(infoParent);

    assertThat(infoParent.getSize(), is(15L));
    assertThat(infoParent.getOffset(), is(15L));
    assertThat(infoParent.isUploadInProgress(), is(false));

    verify(uploadStorageService, times(1)).update(infoParent);
  }

  @Test
  @SneakyThrows
  void concatNotCompleted() {
    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize(5L);
    child1.setOffset(5L);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize(10L);
    child2.setOffset(8L);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child2);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    concatenationService.concat(infoParent);

    assertThat(infoParent.getSize(), is(15L));
    assertThat(infoParent.getOffset(), is(0L));
    assertThat(infoParent.isUploadInProgress(), is(true));

    verify(uploadStorageService, times(1)).update(infoParent);
  }

  @Test
  @SneakyThrows
  void concatWithoutLength() {
    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize(null);
    child1.setOffset(5L);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize(null);
    child2.setOffset(8L);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child2);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    concatenationService.concat(infoParent);

    assertThat(infoParent.getSize(), is(nullValue()));
    assertThat(infoParent.getOffset(), is(0L));
    assertThat(infoParent.isUploadInProgress(), is(true));

    verify(uploadStorageService, never()).update(infoParent);
  }

  @Test
  @SneakyThrows
  void concatNotFound() {
    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize(5L);
    child1.setOffset(5L);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize(10L);
    child2.setOffset(10L);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(null);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    assertThatThrownBy(() -> concatenationService.concat(infoParent))
        .isInstanceOf(UploadNotFoundException.class);
  }

  @Test
  @SneakyThrows
  void concatWithExpiration() {
    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize(5L);
    child1.setOffset(5L);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize(10L);
    child2.setOffset(8L);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child2);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    when(uploadStorageService.getUploadExpirationPeriod()).thenReturn(500L);

    concatenationService.concat(infoParent);

    assertThat(infoParent.getSize(), is(15L));
    assertThat(infoParent.getOffset(), is(0L));
    assertThat(infoParent.isUploadInProgress(), is(true));

    assertThat(infoParent.getExpirationTimestamp(), is(notNullValue()));
    assertThat(child1.getExpirationTimestamp(), is(notNullValue()));
    // We should not update uploads that are still in progress (as they might still being
    // written)
    assertThat(child2.getExpirationTimestamp(), is(nullValue()));

    verify(uploadStorageService, times(1)).update(infoParent);
    verify(uploadStorageService, times(1)).update(child1);
    // We should not update uploads that are still in progress (as they might still being
    // written)
    verify(uploadStorageService, never()).update(child2);
  }

  @Test
  @SneakyThrows
  void getUploadsEmptyFinal() {
    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(null);

    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    assertThat(concatenationService.getPartialUploads(infoParent), Matchers.empty());

    assertThat(infoParent.getSize(), is(nullValue()));
    assertThat(infoParent.getOffset(), is(0L));
    assertThat(infoParent.isUploadInProgress(), is(true));

    verify(uploadStorageService, never()).update(infoParent);
  }

  @Test
  @SneakyThrows
  void getConcatenatedBytes() {
    String upload1 = "This is a ";
    String upload2 = "concatenated upload!";

    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize((long) upload1.getBytes().length);
    child1.setOffset(upload1.getBytes().length);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize((long) upload2.getBytes().length);
    child2.setOffset(upload2.getBytes().length);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child2);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    when(uploadStorageService.getUploadedBytes(child1.getId()))
        .thenReturn(IOUtils.toInputStream(upload1, StandardCharsets.UTF_8));
    when(uploadStorageService.getUploadedBytes(child2.getId()))
        .thenReturn(IOUtils.toInputStream(upload2, StandardCharsets.UTF_8));

    assertThat(
        IOUtils.toString(
            concatenationService.getConcatenatedBytes(infoParent), StandardCharsets.UTF_8),
        is("This is a concatenated upload!"));
  }

  @Test
  @SneakyThrows
  void getConcatenatedBytesNotComplete() {
    String upload1 = "This is a ";
    String upload2 = "concatenated upload!";

    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize((long) upload1.getBytes().length);
    child1.setOffset((long) upload1.getBytes().length - 2);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize((long) upload2.getBytes().length);
    child2.setOffset((long) upload2.getBytes().length - 2);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child2);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    when(uploadStorageService.getUploadedBytes(child1.getId()))
        .thenReturn(IOUtils.toInputStream(upload1, StandardCharsets.UTF_8));
    when(uploadStorageService.getUploadedBytes(child2.getId()))
        .thenReturn(IOUtils.toInputStream(upload2, StandardCharsets.UTF_8));

    assertThat(concatenationService.getConcatenatedBytes(infoParent), is(nullValue()));
  }

  @Test
  @SneakyThrows
  void getConcatenatedBytesNotFound() {
    String upload1 = "This is a ";
    String upload2 = "concatenated upload!";

    UploadInfo child1 = new UploadInfo();
    child1.setId(UploadId.randomUUID());
    child1.setSize((long) upload1.getBytes().length);
    child1.setOffset((long) upload1.getBytes().length - 2);

    UploadInfo child2 = new UploadInfo();
    child2.setId(UploadId.randomUUID());
    child2.setSize((long) upload2.getBytes().length);
    child2.setOffset((long) upload2.getBytes().length - 2);

    UploadInfo infoParent = new UploadInfo();
    infoParent.setId(UploadId.randomUUID());
    infoParent.setConcatenationPartIds(
        Arrays.asList(child1.getId().toString(), child2.getId().toString()));

    when(uploadStorageService.getUploadInfo(child1.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(child1);
    when(uploadStorageService.getUploadInfo(child2.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(null);
    when(uploadStorageService.getUploadInfo(
            infoParent.getId().toString(), infoParent.getOwnerKey()))
        .thenReturn(infoParent);

    when(uploadStorageService.getUploadedBytes(child1.getId()))
        .thenReturn(IOUtils.toInputStream(upload1, StandardCharsets.UTF_8));
    when(uploadStorageService.getUploadedBytes(child2.getId()))
        .thenReturn(IOUtils.toInputStream(upload2, StandardCharsets.UTF_8));

    assertThatThrownBy(() -> concatenationService.getConcatenatedBytes(infoParent))
        .isInstanceOf(UploadNotFoundException.class);
  }
}
