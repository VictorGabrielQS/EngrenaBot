package VictorCoddys.EngrenaBot;

import VictorCoddys.EngrenaBot.Config.CatalogoProperties;
import VictorCoddys.EngrenaBot.Config.TelProperties;
import VictorCoddys.EngrenaBot.Config.ZApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({ZApiProperties.class, CatalogoProperties.class, TelProperties.class})
@SpringBootApplication
public class EngrenaBotApplication {
	public static void main(String[] args) {
		SpringApplication.run(EngrenaBotApplication.class, args);
	}
}
