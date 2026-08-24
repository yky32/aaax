package com.aaax.core.api;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class BaseRetrofitOutClusterInterceptor implements Interceptor {

    private int retryCounter;
    private int retryInterval;

    public BaseRetrofitOutClusterInterceptor(int retryCounter) {
        this.retryCounter = retryCounter;
        this.retryInterval = 5;
    }

    public BaseRetrofitOutClusterInterceptor(int retryCounter, int retryInterval) {
        this.retryCounter = retryCounter;
        this.retryInterval = retryInterval;
    }

    @SneakyThrows
    @NotNull
    @Override
    public Response intercept(Chain chain) {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // Add any headers you need
        Request request = requestBuilder.build();
        Response response = chain.proceed(request);
        log.info("--- Retrofit response.code() : {}", response.code());

        // Retry if the response is not successful (non-200)
        int retryCount = 0;
        while (!response.isSuccessful() && retryCount < retryCounter) {

            log.info("--- Retrofit retry->{} : {} ", retryCount, response.code());
            Thread.sleep(retryInterval * 1000L);
            retryCount++;
            response.close();
            response = chain.proceed(request);
        }

        // Return the final response
        return response;
    }
}