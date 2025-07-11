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
    public String receberMensagem(@RequestBody Map<String, Object> payload) {
        System.out.println("📩 Payload recebido: " + payload); // Debug geral

        if (!"ReceivedCallback".equals(payload.get("type"))) {
            return "🔕 Ignorado (não é mensagem de usuário)";
        }

        String telefone = (String) payload.get("phone");

        Map<String, String> text = (Map<String, String>) payload.get("text");
        if (text == null || text.get("message") == null) {
            return "❌ Texto não encontrado no payload";
        }

        String mensagem = text.get("message");

        System.out.printf("📲 Mensagem de %s: %s%n", telefone, mensagem);
        return botService.processarMensagem(telefone, mensagem);
    }



    // Limpa todos os agendamentos
    // 🗑️ Isso remove todos os agendamentos salvos no JSON local.
    @DeleteMapping("/reset")
    public String resetarAgendamentos() {
        JsonStorage.limparAgendamentos();
        return "✅ Todos os agendamentos foram apagados.";
    }


}