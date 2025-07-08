package VictorCoddys.EngrenaBot.Service;

import VictorCoddys.EngrenaBot.Config.CatalogoProperties;
import VictorCoddys.EngrenaBot.Config.TelProperties;
import VictorCoddys.EngrenaBot.Config.ZApiProperties;
import VictorCoddys.EngrenaBot.Model.Agendamento;
import VictorCoddys.EngrenaBot.Model.EstadoFluxo;
import VictorCoddys.EngrenaBot.Util.JsonStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Serviço que gerencia o fluxo de mensagens do bot.
 * Armazena o estado atual de cada usuário e os dados parciais do agendamento.
 */


@Service
@RequiredArgsConstructor
public class BotService {


    // Propriedades de configuração
    private final ZApiProperties zApi;
    private final CatalogoProperties catalogo;
    private final TelProperties tel;


    // Mapa que armazena o estado atual da conversa por telefone
    private final Map<String, EstadoFluxo> estados = new HashMap<>();


    // Mapa que armazena os dados preenchidos até agora por telefone
    private final Map<String, Agendamento> dadosParciais = new HashMap<>();


    // Mapa que armazena os dias disponíveis para agendamento por loja
    private final Map<String, List<LocalDate>> diasDisponiveisMap = new HashMap<>();


    /**
     * Processa a mensagem recebida do usuário e retorna a resposta apropriada.
     *
     * @param telefone número do telefone do usuário
     * @param mensagem mensagem enviada pelo usuário
     * @return resposta do bot
     */
    public String processarMensagem(String telefone, String mensagem) {
        EstadoFluxo estado = estados.getOrDefault(telefone, EstadoFluxo.INICIO);

        switch (estado) {

            // Início do atendimento: perguntar qual loja
            case INICIO -> {

                estados.put(telefone, EstadoFluxo.AGUARDANDO_LOJA);
                dadosParciais.put(telefone, new Agendamento());

                return """
                        Olá! Bem-vindo à Bike Rogers, a sua oficina de bicicletas! 🚴‍♂️
                        Escolha a loja para atendimento:
                        
                        1️⃣ Loja Forte Ville
                        2️⃣ Loja Novo Horizonte
                        """;
            }


            // Recebe o número da loja e armazena no agendamento
            case AGUARDANDO_LOJA -> {
                Agendamento agendamento = dadosParciais.get(telefone);

                switch (mensagem.trim()) {
                    case "1" -> agendamento.setLoja("Loja Forte Ville");
                    case "2" -> agendamento.setLoja("Loja Novo Horizonte");
                    default -> {
                        return "❌ Opção inválida. Por favor, digite:\n1 para Loja Forte Ville\n2 para Loja Novo Horizonte";
                    }
                }

                estados.put(telefone, EstadoFluxo.AGUARDANDO_NOME);
                return "Perfeito! Agora, qual o seu nome?";
            }


            // Recebe o nome do usuário
            case AGUARDANDO_NOME -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                agendamento.setNome(mensagem);

                estados.put(telefone, EstadoFluxo.AGUARDANDO_SERVICO);
                return """
                        Qual serviço você precisa?
                        1️⃣ Revisão
                        2️⃣ Troca de peças
                        3️⃣ Compra
                        4️⃣ Outros serviços
                        """;
            }


            // Recebe o tipo de serviço
            case AGUARDANDO_SERVICO -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                String servico = mensagem.trim();
                switch (servico) {
                    case "1" -> {
                        agendamento.setTipoServico("Revisão");
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_DATA);
                        List<LocalDate> diasDisponiveis = obterDiasDisponiveis(7);
                        diasDisponiveisMap.put(telefone, diasDisponiveis);
                        StringBuilder sb = new StringBuilder("Digite o número do dia que melhor te atende:\n\n");
                        for (int i = 0; i < diasDisponiveis.size(); i++) {
                            sb.append(i + 1).append(" - ")
                                    .append(diasDisponiveis.get(i).format(DateTimeFormatter.ofPattern("dd/MM"))).append("\n");
                        }
                        return sb.toString();
                    }
                    case "2" -> {
                        agendamento.setTipoServico("Troca de peças");
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_OBSERVACAO);
                        return "Por favor, descreva quais peças você deseja trocar.";
                    }
                    case "3" -> {
                        agendamento.setTipoServico("Compra");

                        agendamento.setTelefone(telefone);

                        enviarParaSetorDeVendas(agendamento);

                        enviarArquivoPdfParaCliente(
                                telefone,
                                catalogo.getCaminhoPdf(),
                                """
                                        🛒 Confira nosso catálogo completo de produtos!
                                        """
                        );

                        return """
                                Enviamos o catálogo completo para você! 📄
                                
                                📞 Fale com o setor de vendas, Clicando no número abaixo: %s
                                """.formatted(tel.getTelefoneVendas());
                    }


                    case "4" -> {
                        agendamento.setTipoServico("Outros serviços");
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_OBSERVACAO);
                        return "Por favor, descreva qual serviço você deseja realizar.";
                    }
                    default -> {
                        return "❌ Opção inválida. Escolha:\n1 - Revisão\n2 - Troca de peças\n3 - Compra\n4 - Outros serviços";
                    }
                }
            }


            // Recebe a observação do usuário (caso tenha escolhido troca de peças ou outros serviços)
            case AGUARDANDO_OBSERVACAO -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                agendamento.setObservacao(mensagem);
                estados.put(telefone, EstadoFluxo.AGUARDANDO_DATA);
                List<LocalDate> diasDisponiveis = obterDiasDisponiveis(7);
                diasDisponiveisMap.put(telefone, diasDisponiveis);
                StringBuilder sb = new StringBuilder("Digite o número do dia que melhor te atende:\n\n");
                for (int i = 0; i < diasDisponiveis.size(); i++) {
                    sb.append(i + 1).append(" - ")
                            .append(diasDisponiveis.get(i).format(DateTimeFormatter.ofPattern("dd/MM"))).append("\n");
                }
                return sb.toString();
            }


            // Recebe a data desejada e valida o formato
            case AGUARDANDO_DATA -> {
                try {
                    int opcao = Integer.parseInt(mensagem.trim()) - 1;
                    List<LocalDate> dias = diasDisponiveisMap.get(telefone);
                    if (opcao < 0 || opcao >= dias.size()) {
                        return "❌ Opção inválida. Escolha um número da lista de dias disponíveis.";
                    }
                    LocalDate escolhido = dias.get(opcao);
                    Agendamento agendamento = dadosParciais.get(telefone);
                    agendamento.setData(escolhido.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    estados.put(telefone, EstadoFluxo.AGUARDANDO_HORARIO);
                    return "Agora informe o horário desejado. Ex: 14:00";
                } catch (NumberFormatException e) {
                    return "❌ Por favor, digite apenas o número correspondente ao dia.";
                }
            }


            // Recebe o horário desejado e finaliza o agendamento
            case AGUARDANDO_HORARIO -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
                    LocalTime horarioEscolhido = LocalTime.parse(mensagem, formatter);

                    // Recupera o dia agendado
                    LocalDate dataEscolhida = LocalDate.parse(agendamento.getData(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    DayOfWeek diaSemana = dataEscolhida.getDayOfWeek();

                    LocalTime inicio;
                    LocalTime fim;

                    if (diaSemana == DayOfWeek.SATURDAY) {
                        inicio = LocalTime.of(8, 0);
                        fim = LocalTime.of(15, 0);
                    } else {
                        inicio = LocalTime.of(8, 0);
                        fim = LocalTime.of(18, 0);
                    }

                    // Verifica se está dentro do horário de funcionamento
                    if (horarioEscolhido.isBefore(inicio) || horarioEscolhido.isAfter(fim)) {
                        return String.format("❌ Horário fora do funcionamento da loja. Horário permitido: %s às %s",
                                inicio.toString(), fim.toString());
                    }

                    agendamento.setHorario(mensagem);
                    estados.put(telefone, EstadoFluxo.AGUARDANDO_CONFIRMACAO);

                    return String.format("""
                                    📝 Confirme os dados abaixo:
                                    
                                    📍 Loja: %s
                                    👤 Nome: %s
                                    🔧 Serviço: %s
                                    📋 Observação: %s
                                    📆 Data: %s às %s
                                    
                                    Responda:
                                    
                                    ✅ Confirmar
                                    ❌ Cancelar
                                    
                                    """,
                            agendamento.getLoja(), agendamento.getNome(), agendamento.getTipoServico(),
                            agendamento.getObservacao() != null ? agendamento.getObservacao() : "Não informado",
                            agendamento.getData(), agendamento.getHorario());

                } catch (Exception e) {
                    return "❌ Horário inválido! (ex: 14:00)";
                }
            }


            // Confirmação do agendamento
            case AGUARDANDO_CONFIRMACAO -> {
                String resposta = mensagem.trim().toLowerCase();
                Agendamento agendamento = dadosParciais.get(telefone);

                if (resposta.equals("✅") || resposta.equalsIgnoreCase("confirmar")) {
                    agendamento.setTelefone(telefone);
                    JsonStorage.salvarAgendamento(agendamento);
                    estados.remove(telefone);
                    dadosParciais.remove(telefone);
                    notificarMecanico(agendamento);
                    return "✅ Agendamento confirmado com sucesso! Obrigado por escolher a Bike Rogers 🚴‍♂️\n" +
                            "Te esperamos na loja " + agendamento.getLoja() + " no dia " + agendamento.getData() + " às " + agendamento.getHorario() + ".\n" +
                            "Se precisar de mais alguma coisa, é só chamar! 😊"
                            ;

                } else if (resposta.equals("❌") || resposta.equalsIgnoreCase("cancelar")) {
                    estados.put(telefone, EstadoFluxo.INICIO);
                    dadosParciais.remove(telefone);

                    // Limpa os dados parciais do usuário
                    return "❌ Agendamento cancelado. Vamos começar novamente.";
                } else {
                    return "Por favor, responda com ✅ para confirmar ou ❌ para cancelar.";
                }
            }


            // Estado padrão para reiniciar o fluxo em caso de erro
            default -> {
                estados.put(telefone, EstadoFluxo.INICIO);
                return "Algo deu errado! Vamos começar novamente. Qual loja deseja atender?";
            }
        }
    }


    /**
     * Obtém uma lista de dias disponíveis para agendamento, considerando o limite de 5 agendamentos por dia.
     *
     * @param quantidade número de dias a serem retornados
     * @return lista de LocalDate com os dias disponíveis
     */
    private List<LocalDate> obterDiasDisponiveis(int quantidade) {
        List<LocalDate> dias = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        while (dias.size() < quantidade) {
            DayOfWeek diaSemana = hoje.getDayOfWeek();

            boolean domingo = diaSemana == DayOfWeek.SUNDAY;
            boolean sabado = diaSemana == DayOfWeek.SATURDAY;

            if (!domingo) {
                // Se for hoje, verificar horário atual (exemplo: aceitar agendamento só se agora for antes de 17:00)
                if (hoje.isEqual(LocalDate.now())) {
                    LocalTime agora = LocalTime.now();
                    LocalTime horarioFechamento = sabado ? LocalTime.of(15, 0) : LocalTime.of(18, 0);
                    if (agora.isBefore(horarioFechamento.minusHours(1))) {
                        dias.add(hoje);
                    }
                } else {
                    dias.add(hoje);
                }
            }
            hoje = hoje.plusDays(1);
        }
        return dias;
    }


    /**
     * Verifica se o limite de 5 agendamentos por dia foi excedido.
     *
     * @param data data a ser verificada
     * @return true se o limite foi excedido, false caso contrário
     */
    private boolean excedeuLimitePorDia(LocalDate data) {
        List<Agendamento> ags = JsonStorage.listarAgendamentos();
        long total = ags.stream()
                .filter(a -> a.getData().equals(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
                .count();
        return total >= 5;
    }


    /**
     * Envia os dados do agendamento para o setor de vendas.
     * Futuramente, pode ser integrado com a Z-API para envio automático.
     *
     * @param agendamento objeto Agendamento com os dados do cliente
     */
    private void enviarParaSetorDeVendas(Agendamento agendamento) {
        // Aqui você pode usar a Z-API futuramente para enviar os dados automaticamente

        String mensagem = String.format("""
                📦 Novo cliente interessado em compra!
                
                👤 Nome: %s
                📱 Telefone: %s
                Loja: %s
                Serviço: %s
                
                ⚠️ Favor entrar em contato para finalizar a compra.
                """, agendamento.getNome(), agendamento.getTelefone(), agendamento.getLoja(), agendamento.getTipoServico());

        // Exemplo: usar Z-API futuramente para envio automático:
        // zApiClient.enviarMensagem(numeroVendas, mensagem);

        System.out.println("📤 Mensagem enviada ao setor de vendas:\n" + mensagem); // log no console
    }


    /**
     * Envia um arquivo PDF para o cliente via Z-API.
     *
     * @param telefone   número do telefone do cliente
     * @param caminhoPdf caminho do arquivo PDF a ser enviado
     * @param legenda    legenda que acompanha o arquivo
     */
    public void enviarArquivoPdfParaCliente(String telefone, String caminhoPdf, String legenda) {
        String url = "https://zapi.z-api.io/instances/" + zApi.getInstanceId() + "/token/" + zApi.getToken() + "/send-file";

        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(caminhoPdf));
            String base64 = Base64.getEncoder().encodeToString(fileContent);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {
                                "phone": "%s",
                                "filename": "catalogo.pdf",
                                "base64": "%s",
                                "caption": "%s"
                            }
                            """.formatted(telefone, base64, legenda)))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Notifica o mecânico sobre um novo agendamento.
     * Futuramente, pode ser integrado com a Z-API para envio automático.
     *
     * @param agendamento objeto Agendamento com os dados do cliente
     */
    private void notificarMecanico(Agendamento agendamento) {
        String mensagem = String.format("""
                        📅 Novo Agendamento
                        👤 Nome: %s
                        🔧 Serviço: %s
                        📆 Data: %s às %s
                        📋 Observação: %s
                        🏪 Loja: %s
                        📱 Telefone: %s
                        """,
                agendamento.getNome(),
                agendamento.getTipoServico(),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getObservacao() != null ? agendamento.getObservacao() : "Nenhuma",
                agendamento.getLoja(),
                agendamento.getTelefone()
        );

        System.out.println("📤 Mensagem enviada ao mecânico:\n" + mensagem);

        // Futuro: enviar via Z-API
    }


}
