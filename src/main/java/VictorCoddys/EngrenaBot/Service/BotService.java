package VictorCoddys.EngrenaBot.Service;


import VictorCoddys.EngrenaBot.Model.Agendamento;
import VictorCoddys.EngrenaBot.Model.EstadoFluxo;
import VictorCoddys.EngrenaBot.Util.JsonStorage;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;

// Serviço que gerencia o fluxo de mensagens do bot
// Armazena o estado atual de cada usuário e os dados parciais do agendamento

@Service
public class BotService {

    // Mapas para armazenar o estado do fluxo e os dados parciais de cada usuário
    private final Map<String, EstadoFluxo> estados = new HashMap<>();
    private final Map<String, Agendamento> dadosParciais = new HashMap<>();

    /**Processa a mensagem recebida do usuário e retorna a resposta apropriada.
     * - @param telefone - número de telefone do usuário.
     * - @param mensagem - mensagem enviada pelo usuário.
     * - @return - A resposta do bot para o usuário.
     *
     */

    public String processarMensagem(String telefone, String mensagem) {

        // Verifica o estado atual do usuário e processa a mensagem de acordo
        EstadoFluxo estado = estados.getOrDefault(telefone, EstadoFluxo.INICIO);

        switch (estado) {


            case INICIO -> {
                estados.put(telefone, EstadoFluxo.AGUARDANDO_NOME);
                dadosParciais.put(telefone, new Agendamento());
                return "Olá! Qual o seu nome?";
            }
            case AGUARDANDO_NOME -> {
                Agendamento ag = dadosParciais.get(telefone);
                ag.setNome(mensagem);
                estados.put(telefone, EstadoFluxo.AGUARDANDO_SERVICO);
                return "Qual serviço você precisa? " +
                        "(1) Revisão" +
                        "(2) Troca de peças" +
                        "(3) Compra"+
                        "(4) Orçamento técnico" +
                        "(5) Outros serviços";
            }


            case AGUARDANDO_SERVICO -> {
                Agendamento ag = dadosParciais.get(telefone);
                ag.setTipoServico(mensagem);
                estados.put(telefone, EstadoFluxo.AGUARDANDO_DATA);
                return "Para qual dia e horário?";
            }


            case AGUARDANDO_DATA -> {
                Agendamento ag = dadosParciais.get(telefone);
                ag.setDataHora(mensagem);
                ag.setTelefone(telefone);
                JsonStorage.salvarAgendamento(ag);
                estados.remove(telefone);
                dadosParciais.remove(telefone);
                return String.format("""
                📅 Novo Agendamento
                👤 Nome: %s
                🔧 Serviço: %s
                🕐 Data: %s
                Agendamento confirmado!""",
                        ag.getNome(), ag.getTipoServico(), ag.getDataHora());
            }
            default -> {
                estados.put(telefone, EstadoFluxo.INICIO);
                return "Vamos começar! Qual o seu nome?";
            }
        }
    }
}

