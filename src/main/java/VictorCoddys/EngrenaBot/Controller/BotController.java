package VictorCoddys.EngrenaBot.Controller;

import VictorCoddys.EngrenaBot.Service.BotService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mensagem")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @PostMapping
    public String receberMensagem(@RequestParam String telefone, @RequestParam String texto) {
        return botService.processarMensagem(telefone, texto);
    }
}
