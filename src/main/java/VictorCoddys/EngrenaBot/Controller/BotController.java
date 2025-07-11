package VictorCoddys.EngrenaBot.Controller;

import VictorCoddys.EngrenaBot.Model.Agendamento;
import VictorCoddys.EngrenaBot.Service.BotService;
import VictorCoddys.EngrenaBot.Util.JsonStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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


    // Receber mensagem do WhatsApp
    // 📩 Isso processa mensagens recebidas do WhatsApp e executa ações baseadas nelas.
    @PostMapping
    public void receberMensagem(@RequestBody Map<String, Object> payload) {
        // Verifica se é o tipo de mensagem esperada (evita status, presença, etc.)
        if (!"ReceivedCallback".equals(payload.get("type"))) return;

        String telefone = (String) payload.get("phone");

        // Extrai o texto da mensagem enviada pelo usuário
        Map<String, String> text = (Map<String, String>) payload.get("text");
        if (text == null || text.get("message") == null) return;

        String mensagem = text.get("message");

        botService.processarMensagem(telefone, mensagem); // Já responde via Z-API
    }



    // Limpa todos os agendamentos
    // 🗑️ Isso remove todos os agendamentos salvos no JSON local.
    @DeleteMapping("/reset")
    public String resetarAgendamentos() {
        JsonStorage.limparAgendamentos();
        return "✅ Todos os agendamentos foram apagados.";
    }


}