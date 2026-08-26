package com.aaax.server.usecase;

import com.aaax.core.api.UtilApiClient;
import com.aaax.core.entity.dto.FileMetadata;
import com.aaax.core.entity.dto.ImageLink;
import com.aaax.core.entity.dto.util.response.GetCdnResponseDto;
import com.aaax.core.exception.BizException;
import com.aaax.core.response.Result;
import com.aaax.core.utils.RetrofitCallHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import retrofit2.Call;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandleFileUseCaseTest {

    @Mock private UtilApiClient utilApiClient;
    @Mock private Call<Result<List<GetCdnResponseDto>>> call;

    @InjectMocks
    private HandleFileUseCase handleFileUseCase;

    @BeforeEach
    void enableUtil() {
        ReflectionTestUtils.setField(handleFileUseCase, "utilEnabled", true);
    }

    @Test
    @DisplayName("execute should upload file and return metadata")
    void execute_shouldUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "photo.png", "image/png", "bytes".getBytes());
        when(utilApiClient.upload(anyString(), any())).thenReturn(call);

        GetCdnResponseDto cdn = GetCdnResponseDto.builder()
                .id("11")
                .link(ImageLink.builder().url("https://cdn/photo.png").build())
                .build();

        Result<List<GetCdnResponseDto>> result = Result.<List<GetCdnResponseDto>>builder()
                .data(List.of(cdn))
                .build();

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._execute(any())).thenReturn(result);

            FileMetadata metadata = handleFileUseCase.execute(file, "user-profiles/1/");

            assertEquals("https://cdn/photo.png", metadata.getUrl());
            assertEquals("11", metadata.getGid());
            assertEquals("photo.png", metadata.getFileName());
        }
    }

    @Test
    @DisplayName("execute should wrap upload failures")
    void execute_shouldWrapFailures() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "photo.png", "image/png", "bytes".getBytes());
        when(utilApiClient.upload(anyString(), any())).thenReturn(call);

        try (MockedStatic<RetrofitCallHandler> retrofit = mockStatic(RetrofitCallHandler.class)) {
            retrofit.when(() -> RetrofitCallHandler._execute(any()))
                    .thenThrow(new RuntimeException("cdn down"));

            assertThrows(BizException.class, () -> handleFileUseCase.execute(file, "path/"));
        }
    }
}
