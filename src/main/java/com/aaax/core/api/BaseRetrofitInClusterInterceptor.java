package com.aaax.core.api;

import com.aaax.core.exception.BizException;
import com.aaax.core.redis.CoreRedisKey;
import com.aaax.core.response.Result;
import com.aaax.core.response.SystemResponse;
import com.aaax.core.utils.JSONUtil;
import com.aaax.core.utils.JwtUtil;
import com.aaax.core.utils.RedisUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class BaseRetrofitInClusterInterceptor implements Interceptor {

    private Boolean isEnabledSystemToken;
    private String endpoint;
    private String clientId;
    private String clientSecret;
    private RedisUtil redisUtil;

    public BaseRetrofitInClusterInterceptor() {
        this.isEnabledSystemToken = false;
    }

    public BaseRetrofitInClusterInterceptor(Boolean isEnabledSystemToken, String endpoint, String clientId, String clientSecret, RedisUtil redisUtil) {
        this.isEnabledSystemToken = isEnabledSystemToken;
        this.endpoint = endpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redisUtil = redisUtil;
    }

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder();
        if (JwtUtil.mySecurityContext() != null) {
            log.info("--------- JWT from [client-forwarded]... jwt @ {}", JwtUtil.myJwt().getTokenValue());
            builder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ".concat(JwtUtil.myJwt().getTokenValue()));
        }

        if (isEnabledSystemToken) {
            log.info("--------- JWT from [client-secret-grant]... url @ {}", endpoint);
            log.info("--------- JWT from [client-secret-grant]... clientId @ {}", clientId);
            log.info("--------- JWT from [client-secret-grant]... clientSecret @ {}", clientSecret);
            log.info("--------- JWT from [client-secret-grant]... jwt @ {}", "system-token");
            builder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer ".concat(this.obtainAccessToken()));
        }

        Request newRequest = builder.build();
        // Proceed with the modified request
        Response response = chain.proceed(newRequest);
        processAPIReturn(response);
        return response;
    }

    private String obtainAccessToken() throws IOException {
        // check token is redis yet
        String redisKey = CoreRedisKey.SYSTEM_CLIENT_SECRET_GRANT.getKey().concat(clientId);
        if (redisUtil.hasKey(redisKey)) {
            log.info("=======cached:system:token-key=> {}", redisKey);
            log.info("=======cached:system:token=> {}", redisUtil.get(redisKey));
            return (String) redisUtil.get(redisKey);
        }
        // Logic to obtain the access token
        // Example using OkHttp to make a synchronous request
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(endpoint.concat("oauth2/token"))
                .post(new FormBody.Builder()
                        .add("grant_type", "client_credentials")
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                // Assuming the response body contains the access token
                // Parse the access token from the response
                String responseBody = response.body().string();
                Jwt jwt = parseToken(responseBody);
                log.info("=======system:token=> {}", jwt.getAccessToken());
                redisUtil.set(redisKey, jwt.getAccessToken(), 86400); // stored in cache
                return jwt.getAccessToken();
            } else {
                throw new BizException(SystemResponse.SAU0499, request.url());
            }
        }
    }

    private Jwt parseToken(String responseBody) {
        Map map = JSONUtil.readValue(responseBody, Map.class);
        return JSONUtil.convertFromObject(map.get("data"), Jwt.class); // Placeholder
    }

    @SneakyThrows
    private void processAPIReturn(Response response) {
        log.info("--- Retrofit response.code() : {}", response.code());
        // ____ Log response body if present
        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire response body
            Buffer buffer = source.buffer();

            // Check if response is gzipped
            if ("gzip".equalsIgnoreCase(response.headers().get("Content-Encoding"))) {
                GzipSource gzippedSource = new GzipSource(buffer.clone());
                buffer = new Buffer();
                buffer.writeAll(gzippedSource);
            }

            String content = buffer.clone().readUtf8();

            String message = String.format("%s in [%s %s].", response.code(), response.request().method(), response.request().url().url());
            switch (response.code()) {
                case (401) -> throw new BizException(SystemResponse.SYS9401, Map.of("api.response", content, "message", message));
                case (403) -> throw new BizException(SystemResponse.SYS9403, Map.of("api.response", content, "message", message));
            }

            // ____ Globally catch the response from [inter-cross-microservice]
            switch (response.code()) {
                case (200), (201), (301), (302) -> {}
                case (400), (404), (409), (500) -> {
                    Result result = JSONUtil.readValue(content, Result.class);
                    throw new BizException(result.getResponse(), result.getData());
                }
                default -> throw new BizException(SystemResponse.SYS9499, Map.of("content", content, "detail", "in [BaseRetrofitExceptionHandlerInterceptor]."));
            }

            Result result = JSONUtil.readValue(content, Result.class);
            if (!result.getResponse().equals(SystemResponse.SYS0000)){
                throw new BizException(result.getResponse(), result.getData());
            }
        }

        log.info("--- Retrofit no responseBody => {}", response);
    }
}