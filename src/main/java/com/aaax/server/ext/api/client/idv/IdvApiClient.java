package com.aaax.server.ext.api.client.idv;


import com.aaax.core.response.Result;
import com.aaax.server.ext.api.client.idv.dto.CreateIdvRequestDto;
import com.aaax.server.ext.api.client.idv.dto.CreateIdvResponseDto;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface IdvApiClient {

    @POST("create")
    Call<Result<CreateIdvResponseDto>> createIdvRequest(@Body CreateIdvRequestDto dto);
}
