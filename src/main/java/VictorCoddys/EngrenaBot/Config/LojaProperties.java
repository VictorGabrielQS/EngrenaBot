package VictorCoddys.EngrenaBot.Config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loja")
@Data
public class LojaProperties {
    private int quantidadeServicosDiarioLojaForteVille;
    private int quantidadeServicosDiarioLojaNovoHorizonte;
}
