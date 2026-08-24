package com.aaax.core.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordWebhookMessage {
    private String content;
    private String username;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    private List<DiscordEmbed> embeds;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class DiscordEmbed {
        private Author author;
        private String title;
        private String url;
        private String description;
        private int color;
        private List<Field> fields;
        private Thumbnail thumbnail;
        private Image image;
        private Footer footer;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Author{
        private String name;
        private String url;
        @JsonProperty("icon_url")
        private String iconUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Field{
        private String name;
        private String value;
        private boolean inline;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Footer{
        private String text;
        @JsonProperty("icon_url")
        private String iconUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Image{
        private String url;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Thumbnail{
        private String url;
    }
}
