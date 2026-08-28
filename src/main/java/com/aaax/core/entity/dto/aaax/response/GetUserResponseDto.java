package com.aaax.core.entity.dto.aaax.response;

import com.aaax.core.common.jsonfield.UserMetadata;
import com.aaax.core.constant.enu.UserStatus;
import com.aaax.core.entity.dto.BaseResponseDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class GetUserResponseDto extends BaseResponseDto {
    private String id;
    private String username;
    private List<UserLoginTypesMetadata> loginTypes;
    private UserMetadata metadata;
    private UserStatus status;
    private String role;
    private String code;
    private String alias;
    private String nickname;
    private List<String> sourceSystemTags;
    @JsonProperty("isActive")
    private Boolean isActive;
    private Map<String, Object> metrics;
    private Object userProfile;
}
