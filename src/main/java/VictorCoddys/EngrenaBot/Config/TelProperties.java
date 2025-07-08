package VictorCoddys.EngrenaBot.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;



@ConfigurationProperties(prefix = "contatos")
@Data
public class TelProperties {
    private String telefoneVendas;
    private String telefoneMecanico;
}
