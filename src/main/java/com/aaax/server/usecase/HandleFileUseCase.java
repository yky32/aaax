package com.aaax.server.usecase;

import static com.aaax.core.utils.FileUtil.getFileExtension;
import static com.aaax.core.utils.FileUtil.getFileName;

import java.time.Instant;
import java.util.Objects;

import com.aaax.core.api.UtilApiClient;
import com.aaax.core.entity.dto.FileMetadata;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.InstantUtil;
import com.aaax.core.utils.RandomHashGenerator;
import com.aaax.core.utils.RetrofitCallHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
@RequiredArgsConstructor
public class HandleFileUseCase {

    private final UtilApiClient utilApiClient;

    @Value("${ext.util-enabled:false}")
    private boolean utilEnabled;

    public FileMetadata execute(MultipartFile file, String path) {
        return __doUploadFile(file, path, false);
    }

    private FileMetadata __doUploadFile(MultipartFile file, String path, boolean isRandomGeneratedFilename) {
        if (!utilEnabled) {
            throw new BizException(
                    SystemResponse.PAM0400,
                    "File upload disabled (set AAAX_UTIL_ENABLED=true and UTIL_SVC_URL to enable CDN util)");
        }
        var extension = getFileExtension(file);
        var fileName = getFileName(file);

        String[] segments = fileName.split("\\.");
        String randomUUID = InstantUtil.parse(Instant.now(), "yyyyMMddHHmmSS");
        String _filename = segments[0];

        var storedFilename = isRandomGeneratedFilename
                ? RandomHashGenerator.generateRandomHash(RandomUtils.nextInt(15, 35)).concat(".").concat(extension)
                : (segments.length < 2)
                        ? fileName
                        : _filename.concat("_").concat(randomUUID).concat(".").concat(segments[1]);

        var fileMetadataBuilder = FileMetadata.builder()
                .fileName(fileName)
                .size(String.valueOf(file.getSize()))
                .lastUpdateDt(Instant.now().toString());

        try {
            var mediaType = MediaType.parse(Objects.requireNonNull(file.getContentType()));
            var requestBody = RequestBody.create(mediaType, file.getBytes());
            var cdnFiles = RetrofitCallHandler._execute(utilApiClient.upload(
                            path, MultipartBody.Part.createFormData("files", storedFilename, requestBody)))
                    .getData();
            var cdnFile = cdnFiles.get(0);
            return fileMetadataBuilder
                    .gid(String.valueOf(cdnFile.getId()))
                    .url(cdnFile.getLink().getUrl())
                    .mimeType(mediaType.toString())
                    .build();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(SystemResponse.SYS9999, "Failed to upload file.");
        }
    }
}
