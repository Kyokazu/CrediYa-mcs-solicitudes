package co.com.crediya.consumer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "adapters.users")
public class AdaptersPath {

    private String baseUrl;
    private String timeoutMs;
    private String userByIdentification;
}
