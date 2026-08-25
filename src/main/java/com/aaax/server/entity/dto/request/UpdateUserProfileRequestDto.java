package com.aaax.server.entity.dto.request;

import com.aaax.server.entity.enu.UserProfileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateUserProfileRequestDto {
    private String type = UserProfileType.DEFAULT.name(); // assign default first
    private Map context;
    private MultipartFile icon = null;
}
