package VictorCoddys.EngrenaBot.Service;

import VictorCoddys.EngrenaBot.Config.CatalogoProperties;
import VictorCoddys.EngrenaBot.Config.LojaProperties;
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
import java.time.format.DateTimeParseException;
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
    private final LojaProperties lojaProps;
    private final ZApiClient zApiClient;
    private static final String LOJA_FORTE_VILLE = "Loja Forte Ville";
    private static final String LOJA_NOVO_HORIZONTE = "Loja Novo Horizonte";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");


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

                zApiClient.enviarMensagemComBotoes(
                        telefone,
                        "Escolha sua loja preferida",
                        "👋 Olá! Seja muito bem-vindo à *Bike Rogers*, a sua parceira número 1 em cuidados com bicicletas! 🚴‍♀️🔧\n\nVamos começar o seu atendimento:",
                        "Selecione abaixo:",
                        List.of("Loja Forte Ville", "Loja Novo Horizonte")
                );

                return null; // a resposta já foi enviada via Z-API
            }



            // Recebe o número da loja e armazena no agendamento
            // Recebe o número da loja e armazena no agendamento
            case AGUARDANDO_LOJA -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                String opcao = mensagem.trim();

                switch (opcao) {
                    case "1" -> agendamento.setLoja("Loja Forte Ville");
                    case "2" -> agendamento.setLoja("Loja Novo Horizonte");
                    default -> {
                        return """
                    ❌ Não entendi sua escolha.

                    Por favor, selecione uma das opções disponíveis:
                    1️⃣ Loja Forte Ville
                    2️⃣ Loja Novo Horizonte
                    """;
                    }
                }

                estados.put(telefone, EstadoFluxo.AGUARDANDO_NOME);
                return """
            ✅ Ótimo! Loja selecionada com sucesso.

            Agora, por gentileza, nos informe seu *nome completo* para continuarmos. 👇
            """;
            }


            // Recebe o nome do usuário
            // Recebe o nome do usuário
            case AGUARDANDO_NOME -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                agendamento.setNome(mensagem);

                estados.put(telefone, EstadoFluxo.AGUARDANDO_SERVICO);

                // Envia os botões de serviço via Z-API
                zApiClient.enviarBotoesDeServico(telefone);

                return """
            ✅ Nome registrado com sucesso!

            Agora selecione o serviço desejado tocando em uma das opções abaixo. 👇
            (Se os botões não aparecerem, digite o número correspondente)
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
                        StringBuilder sb = new StringBuilder("""
                    📅 Perfeito! Agora escolha o dia que melhor te atende para realizarmos a revisão:

                    """);
                        for (int i = 0; i < diasDisponiveis.size(); i++) {
                            sb.append(i + 1).append(" - ")
                                    .append(diasDisponiveis.get(i).format(DateTimeFormatter.ofPattern("dd/MM"))).append("\n");
                        }
                        return sb.toString();
                    }

                    case "2" -> {
                        agendamento.setTipoServico("Troca de peças");
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_OBSERVACAO);
                        return """
                    🔧 Certo! Para continuarmos, nos diga quais peças você deseja trocar.

                    Quanto mais detalhes, melhor será o nosso atendimento. 😊
                    """;
                    }

                    case "3" -> {
                        agendamento.setTipoServico("Compra");
                        agendamento.setTelefone(telefone);
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_OBSERVACAO);
                        return """
                    🛍️ Legal! Informe abaixo o(s) produto(s) que você tem interesse em comprar.

                    Assim poderemos direcionar seu atendimento de forma mais eficiente!
                    """;
                    }

                    case "4" -> {
                        agendamento.setTipoServico("Outros serviços");
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_OBSERVACAO);
                        return """
                    ✍️ Por favor, descreva com detalhes o serviço que deseja realizar.

                    Após recebermos sua solicitação, nossa equipe verificará a disponibilidade e daremos sequência ao agendamento. 😊
                    """;
                    }

                    default -> {
                        return """
                    ❌ Opção inválida! Por favor, escolha uma das opções abaixo:

                    1️⃣ - Revisão
                    2️⃣ - Troca de peças
                    3️⃣ - Compra
                    4️⃣ - Outros serviços
                    """;
                    }
                }
            }


            // Recebe a observação do usuário (caso tenha escolhido troca de peças ou outros serviços)
            case AGUARDANDO_OBSERVACAO -> {
                Agendamento agendamento = dadosParciais.get(telefone);

                if ("Compra".equalsIgnoreCase(agendamento.getTipoServico())) {
                    // Enviar para setor de vendas e catálogo conforme fluxo anterior
                    enviarParaSetorDeVendas(agendamento);
                    zApiClient.enviarArquivoPdf(
                            telefone,
                            catalogo.getCaminhoPdf(),
                            "🛒 Confira nosso catálogo completo de produtos!"
                    );
                    estados.remove(telefone);
                    dadosParciais.remove(telefone);

                    return """
            📄 Enviamos o nosso catálogo completo para você com as melhores opções de produtos! 

            📝 *Resumo do seu pedido:* 
            "%s"

            🛍️ Sua solicitação foi encaminhada ao nosso setor de vendas, que entrará em contato para te ajudar com todos os detalhes.

            💬 Caso prefira, você também pode falar diretamente com um de nossos atendentes clicando no número abaixo:
            %s

            Agradecemos pelo interesse e estamos à disposição para te atender com excelência! 🤝🚲
            """.formatted(agendamento.getObservacao(), tel.getTelefoneVendas());
                }

                // Para outros tipos, mostrar lista de dias disponíveis para agendamento
                List<LocalDate> diasDisponiveis = obterDiasDisponiveis(7);
                if (diasDisponiveis == null || diasDisponiveis.isEmpty()) {
                    return "⚠️ No momento, não há dias disponíveis para agendamento. Por favor, tente novamente mais tarde.";
                }

                diasDisponiveisMap.put(telefone, diasDisponiveis);

                List<Map<String, String>> opcoes = new ArrayList<>();
                for (int i = 0; i < diasDisponiveis.size(); i++) {
                    LocalDate data = diasDisponiveis.get(i);
                    Map<String, String> item = new HashMap<>();
                    item.put("id", String.valueOf(i + 1));
                    item.put("title", data.format(DateTimeFormatter.ofPattern("dd/MM")));
                    item.put("description", "Agendar nesse dia");
                    opcoes.add(item);
                }

                Map<String, Object> payload = new HashMap<>();
                payload.put("phone", telefone);
                payload.put("message", "📅 Selecione o dia ideal para seu agendamento:");
                payload.put("optionList", Map.of(
                        "title", "Dias disponíveis para agendamento",
                        "buttonLabel", "Escolher dia",
                        "options", opcoes
                ));

                zApiClient.enviarOptionList(payload);

                // Opcional: retornar mensagem para fallback caso API não entregue a lista
                return "✔️ Acabamos de enviar uma lista interativa para você escolher o melhor dia para seu agendamento.";
            }


            // Recebe a data desejada e valida o formato
            case AGUARDANDO_DATA -> {
                try {
                    int opcao = Integer.parseInt(mensagem.trim()) - 1;
                    List<LocalDate> dias = diasDisponiveisMap.get(telefone);
                    if (dias == null || dias.isEmpty()) {
                        return "⚠️ No momento não temos dias disponíveis para agendamento. Por favor, tente novamente mais tarde.";
                    }
                    if (opcao < 0 || opcao >= dias.size()) {
                        return "❌ Opção inválida. Por favor, escolha um número válido da lista de dias disponíveis.";
                    }
                    LocalDate escolhido = dias.get(opcao);
                    Agendamento agendamento = dadosParciais.get(telefone);
                    agendamento.setData(escolhido.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

                    estados.put(telefone, EstadoFluxo.AGUARDANDO_HORARIO);
                    return "📅 Data registrada com sucesso! Agora, informe o horário desejado para o agendamento. Exemplo: 14:00";
                } catch (NumberFormatException e) {
                    return "❌ Entrada inválida. Por favor, digite apenas o número correspondente ao dia desejado.";
                }
            }


            // Recebe o horário desejado e finaliza o agendamento
            case AGUARDANDO_HORARIO -> {
                Agendamento agendamento = dadosParciais.get(telefone);
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
                    LocalTime horarioEscolhido = LocalTime.parse(mensagem.trim(), formatter);

                    // Recupera a data agendada e o dia da semana
                    LocalDate dataEscolhida = LocalDate.parse(agendamento.getData(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    DayOfWeek diaSemana = dataEscolhida.getDayOfWeek();

                    // Define horário de funcionamento conforme dia
                    LocalTime inicio;
                    LocalTime fim;
                    if (diaSemana == DayOfWeek.SATURDAY) {
                        inicio = LocalTime.of(8, 0);
                        fim = LocalTime.of(15, 0);
                    } else {
                        inicio = LocalTime.of(8, 0);
                        fim = LocalTime.of(18, 0);
                    }

                    // Verifica limite diário antes de aceitar o horário
                    if (excedeuLimitePorDia(dataEscolhida, agendamento.getLoja())) {
                        estados.put(telefone, EstadoFluxo.AGUARDANDO_DATA);
                        return String.format("""
                    ❌ *Agenda lotada!*
                    
                    Todos os horários para o dia *%s* já foram preenchidos. 😥
                    Por favor, escolha outro dia disponível para o seu agendamento. 📅
                    """, dataEscolhida.format(DateTimeFormatter.ofPattern("dd/MM")));
                    }

                    // Valida horário dentro do funcionamento
                    if (horarioEscolhido.isBefore(inicio) || horarioEscolhido.isAfter(fim)) {
                        return String.format("❌ Horário fora do funcionamento da loja. Horário permitido: %s às %s",
                                inicio.toString(), fim.toString());
                    }

                    // Salva horário e muda para etapa de confirmação
                    agendamento.setHorario(horarioEscolhido.format(formatter));
                    estados.put(telefone, EstadoFluxo.AGUARDANDO_CONFIRMACAO);

                    return String.format("""
                📝 Confirme os dados abaixo:
                
                📍 Loja: %s
                👤 Nome: %s
                🔧 Serviço: %s
                📋 Observação: %s
                📆 Data: %s às %s
                
                Responda com:
                ✅ Confirmar
                ❌ Cancelar
                """,
                            agendamento.getLoja(),
                            agendamento.getNome(),
                            agendamento.getTipoServico(),
                            agendamento.getObservacao() != null ? agendamento.getObservacao() : "Não informado",
                            agendamento.getData(),
                            agendamento.getHorario());

                } catch (DateTimeParseException e) {
                    return "❌ Horário inválido! Por favor, digite no formato correto, por exemplo: 14:00";
                } catch (Exception e) {
                    return "❌ Ocorreu um erro inesperado. Por favor, tente novamente.";
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

                    return String.format("""
                ✅ Agendamento confirmado com sucesso, *%s*! 🎉

                📍 Loja: *%s*
                📅 Data: *%s* às *%s*
                🔧 Serviço: *%s*

                %s

                Nos vemos em breve! Obrigado por confiar na Bike Rogers! 🚴‍♂️✨
                """,
                            agendamento.getNome(),
                            agendamento.getLoja(),
                            agendamento.getData(),
                            agendamento.getHorario(),
                            agendamento.getTipoServico(),
                            gerarMensagemPromocional()
                    );

                } else if (resposta.equals("❌") || resposta.equalsIgnoreCase("cancelar")) {
                    estados.put(telefone, EstadoFluxo.INICIO);
                    dadosParciais.remove(telefone);

                    return "❌ Agendamento cancelado. Vamos começar novamente. Como posso ajudar você hoje?";
                } else {
                    return "❓ Por favor, responda com ✅ para confirmar ou ❌ para cancelar o agendamento.";
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
        LocalDate dataAtual = LocalDate.now();
        LocalDate dia = dataAtual;
        int tentativas = 0; // para evitar loop infinito

        while (dias.size() < quantidade && tentativas < 30) {
            DayOfWeek diaSemana = dia.getDayOfWeek();
            boolean domingo = diaSemana == DayOfWeek.SUNDAY;
            boolean sabado = diaSemana == DayOfWeek.SATURDAY;

            if (!domingo) {
                if (dia.isEqual(dataAtual)) {
                    LocalTime agora = LocalTime.now();
                    LocalTime horarioFechamento = sabado ? LocalTime.of(15, 0) : LocalTime.of(18, 0);
                    if (agora.isBefore(horarioFechamento.minusHours(1))) {
                        dias.add(dia);
                    }
                } else {
                    dias.add(dia);
                }
            }

            dia = dia.plusDays(1);
            tentativas++;
        }
        return dias;
    }


    /**
     * Verifica se o limite de 5 agendamentos por dia foi excedido.
     *
     * @param data data a ser verificada
     * @return true se o limite foi excedido, false caso contrário
     */
    private boolean excedeuLimitePorDia(LocalDate data, String loja) {
        List<Agendamento> ags = JsonStorage.listarAgendamentos();
        long total = ags.stream()
                .filter(a -> a.getData().equals(data.format(FORMATO_DATA))
                        && a.getLoja().equalsIgnoreCase(loja))
                .count();

        int limite = switch (loja) {
            case LOJA_FORTE_VILLE -> lojaProps.getQuantidadeServicosDiarioLojaForteVille();
            case LOJA_NOVO_HORIZONTE -> lojaProps.getQuantidadeServicosDiarioLojaNovoHorizonte();
            default -> 5;
        };

        return total >= limite;
    }


    private void enviarParaSetorDeVendas(Agendamento agendamento) {
        if (tel == null || tel.getTelefoneVendas() == null) {
            System.err.println("Telefone do setor de vendas não configurado!");
            return;
        }

        String mensagem = construirMensagemVendas(agendamento);
        zApiClient.enviarMensagemTexto(tel.getTelefoneVendas(), mensagem);
    }


    private String construirMensagemVendas(Agendamento agendamento) {
        return String.format("""
            🛍️ *Novo pedido de compra recebido!*

            👤 Nome do cliente: %s
            📱 Telefone: %s
            📝 Produto(s) de interesse: %s
            🏪 Loja: %s
            🧾 Tipo de atendimento: %s

            🚨 Por favor, entre em contato com o cliente para dar continuidade ao atendimento.
            """,
                agendamento.getNome(),
                agendamento.getTelefone(),
                agendamento.getObservacao() != null ? agendamento.getObservacao() : "Não informado",
                agendamento.getLoja(),
                agendamento.getTipoServico());
    }


    private void notificarMecanico(Agendamento agendamento) {
        if (tel == null || tel.getTelefoneMecanico() == null) {
            System.err.println("Telefone do mecânico não configurado!");
            return;
        }

        String mensagem = construirMensagemMecanico(agendamento);
        zApiClient.enviarMensagemTexto(tel.getTelefoneMecanico(), mensagem);
    }


    private String construirMensagemMecanico(Agendamento agendamento) {
        return String.format("""
            🔔 *Novo agendamento recebido!*

            👤 *Cliente:* %s
            🛠️ *Serviço solicitado:* %s
            📅 *Data:* %s
            ⏰ *Horário:* %s
            📝 *Observações:* %s
            🏪 *Loja:* %s
            📞 *Contato:* %s

            Por favor, prepare-se para o atendimento. Qualquer dúvida, entre em contato com o cliente. 🚲✅
            """,
                agendamento.getNome(),
                agendamento.getTipoServico(),
                agendamento.getData(),
                agendamento.getHorario(),
                agendamento.getObservacao() != null ? agendamento.getObservacao() : "Nenhuma",
                agendamento.getLoja(),
                agendamento.getTelefone());
    }


    /**
     * Gera uma mensagem promocional aleatória para o cliente.
     * Pode ser usada em qualquer ponto do fluxo, como após a confirmação do agendamento.
     * <p>
     * - @return mensagem promocional
     */
    private String gerarMensagemPromocional() {
        List<String> frases = List.of(
                "🎁 Dica: clientes que fazem 3 revisões ganham um brinde surpresa!",
                "💡 Lembre-se: manter a bike revisada aumenta a vida útil em até 40%!",
                "⚡ Promoção do mês: ganhe 10% OFF na próxima troca de pneus!",
                "🔧 Faça sua revisão completa e ganhe lubrificação grátis no mesmo dia!",
                "🚲 A cada 5 agendamentos, você ganha uma lavagem especial grátis!",
                "🎯 Dica: agendar revisões regulares reduz em até 60% os gastos com manutenção!",
                "🌦️ Vai pedalar na chuva? Verifique os freios! Agende uma checagem com a gente!",
                "📅 Clientes fiéis recebem prioridade na agenda em períodos de alta demanda!",
                "💬 Quer receber promoções no WhatsApp? Avise a gente e fique por dentro!",
                "🔥 Essa semana: descontos especiais para serviços de freio e transmissão!",
                "🎉 Indique um amigo e ganhe R$10 de crédito para usar em qualquer serviço!",
                "🛑 Notou ruído estranho na bike? Traga pra gente! Avaliação é por nossa conta!",
                "🚴 Mulher que pedala também tem vez! Ganhe um mimo especial na sua revisão 💜",
                "🧼 Bike suja? A gente lava pra você com preço especial pra clientes da semana!",
                "🎨 Deixe sua bike com cara nova: temos pintura personalizada sob consulta!",
                "🛞 Pneus calibrados fazem toda a diferença! Agende uma inspeção expressa grátis!",
                "🏁 Vai competir? Traga sua bike pra uma revisão técnica antes da prova!",
                "👨‍👩‍👧 Pedal em família? Temos kits promocionais para revisão de 2 ou mais bikes!",
                "📊 Você sabia? Clientes que revisam a cada 2 meses têm 80% menos problemas!",
                "📸 Poste sua bike no Instagram com #BikeRogers e concorra a brindes mensais!",
                "🆕 Chegaram novos acessórios! Consulte nosso catálogo e aproveite os preços!",
                "🛠️ Instalação gratuita de acessórios comprados na loja (por tempo limitado!)",
                "👂 Escutou barulho estranho no pedal? Traga pra gente verificar sem custo!",
                "🔁 Troque sua relação completa com 15% OFF esse mês!"
        );
        Collections.shuffle(frases);
        return frases.get(0);
    }


}
