package me.desair.tus.server.concatenation;

import java.io.IOException;
import me.desair.tus.server.HttpHeader;
import me.desair.tus.server.HttpMethod;
import me.desair.tus.server.exception.TusException;
import me.desair.tus.server.upload.UploadInfo;
import me.desair.tus.server.upload.UploadStorageService;
import me.desair.tus.server.upload.UploadType;
import me.desair.tus.server.util.*;
import org.apache.commons.lang3.StringUtils;

/**
 * The Server MUST acknowledge a successful upload creation with the 201 Created status. The Server
 * MUST set the Location header to the URL of the created resource. This URL MAY be absolute or
 * relative.
 */
public class ConcatenationPostRequestHandler extends AbstractRequestHandler {

  @Override
  public boolean supports(HttpMethod method) {
    return HttpMethod.POST.equals(method);
  }

  @Override
  public void process(
      HttpMethod method,
      TusServletRequest servletRequest,
      TusServletResponse servletResponse,
      UploadStorageService uploadStorageService,
      String ownerKey)
      throws IOException, TusException {

    // For post requests, the upload URI is part of the response
    String uploadUri = servletResponse.getHeader(HttpHeader.LOCATION);
    UploadInfo uploadInfo = uploadStorageService.getUploadInfo(uploadUri, ownerKey);

    if (uploadInfo != null) {

      String uploadConcatValue = servletRequest.getHeader(HttpHeader.UPLOAD_CONCAT);
      if (StringUtils.equalsIgnoreCase(uploadConcatValue, "partial")) {
        uploadInfo.setUploadType(UploadType.PARTIAL);

      } else if (StringUtils.startsWithIgnoreCase(uploadConcatValue, "final")) {
        // reset the length, just to be sure
        uploadInfo.setSize(null);
        uploadInfo.setUploadType(UploadType.CONCATENATED);
        uploadInfo.setConcatenationPartIds(
            HttpUtils.parseConcatenationIdsFromUploadHeader(uploadConcatValue));

        uploadStorageService.getUploadConcatenationService().concat(uploadInfo);

      } else {
        uploadInfo.setUploadType(UploadType.REGULAR);
      }

      uploadInfo.setUploadConcatHeaderValue(uploadConcatValue);

      uploadStorageService.update(uploadInfo);
    }
  }
}
