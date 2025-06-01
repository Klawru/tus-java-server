package me.desair.tus.server.upload;

import static me.desair.tus.server.util.HttpUtils.urlSaveString;

import java.io.Serializable;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.Validate;

@EqualsAndHashCode
public class UploadId implements Serializable {

  @Getter final String id;

  public UploadId(@NonNull String id) {
    Validate.notBlank(id, "The upload ID value cannot be blank");
    this.id = urlSaveString(id);
  }

  @Override
  public String toString() {
    return id;
  }

  public static UploadId randomUUID() {
    return new UploadId(UUID.randomUUID().toString());
  }
}
