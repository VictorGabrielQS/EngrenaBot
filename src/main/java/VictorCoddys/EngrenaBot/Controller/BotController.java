package VictorCoddys.EngrenaBot.Controller;

import VictorCoddys.EngrenaBot.Model.Agendamento;
import VictorCoddys.EngrenaBot.Service.BotService;
import VictorCoddys.EngrenaBot.Util.JsonStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mensagem")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;


    // Listar os agendamentos
    // 🔍 Isso permite verificar todos os agendamentos salvos no JSON local.
    @GetMapping("/agendamentos")
    public List<Agendamento> listarAgendamentos() {
        return JsonStorage.listarAgendamentos();
    }


    @PostMapping
    public String receberMensagem(@RequestParam String telefone, @RequestParam String texto) {
        return botService.processarMensagem(telefone, texto);
    }


}
