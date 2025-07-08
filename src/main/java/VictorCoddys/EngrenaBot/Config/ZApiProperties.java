package VictorCoddys.EngrenaBot.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zapi")
@Data
public class ZApiProperties {
    private String instanceId;
    private String token;
}
