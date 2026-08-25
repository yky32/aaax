package com.aaax.core.config.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;


@ConfigurationProperties(prefix = "app")
@Data
public class AppApiProp {
  private Map<String, ApiProp> ext;

  @Data
  public static class ApiProp {
    private String uri;
  }
}
