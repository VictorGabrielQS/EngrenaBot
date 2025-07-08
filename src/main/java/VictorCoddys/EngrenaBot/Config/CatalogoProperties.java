package VictorCoddys.EngrenaBot.Config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "catalogo")
@Data
public class CatalogoProperties {
    private String caminhoPdf; // deve bater com catalogo.caminho-pdf
}
