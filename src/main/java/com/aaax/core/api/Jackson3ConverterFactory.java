package com.aaax.core.api;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Retrofit converter for Jackson 3. Square's {@code converter-jackson} is still Jackson 2.
 */
public final class Jackson3ConverterFactory extends Converter.Factory {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");

    private final ObjectMapper mapper;

    public static Jackson3ConverterFactory create() {
        return create(JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build());
    }

    public static Jackson3ConverterFactory create(ObjectMapper mapper) {
        return new Jackson3ConverterFactory(Objects.requireNonNull(mapper, "mapper"));
    }

    private Jackson3ConverterFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectReader reader = mapper.readerFor(javaType);
        return (Converter<ResponseBody, Object>) body -> {
            try (body) {
                return reader.readValue(body.byteStream());
            }
        };
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        ObjectWriter writer = mapper.writerFor(javaType);
        return (Converter<Object, RequestBody>) value ->
                RequestBody.create(MEDIA_TYPE, writer.writeValueAsBytes(value));
    }
}
