package VictorCoddys.EngrenaBot.Service;

import VictorCoddys.EngrenaBot.Config.ZApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ZApiClient {

    private final ZApiProperties zApi;

    private final RestTemplate restTemplate = new RestTemplate();


    public void enviarMensagemTexto(String telefone, String mensagem) {
        String url = "https://zapi.z-api.io/instances/" + zApi.getInstanceId()
                + "/token/" + zApi.getToken() + "/send-message";

        try {
            String payload = """
                    {
                        "phone": "%s",
                        "message": "%s"
                    }
                    """.formatted(telefone, mensagem);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Envia um arquivo PDF para o cliente via Z-API.
     *
     * @param telefone   número do telefone do cliente
     * @param caminhoPdf caminho do arquivo PDF a ser enviado
     * @param legenda    legenda que acompanha o arquivo
     */

    public void enviarArquivoPdf(String telefone, String caminhoPdf, String legenda) {
        String url = "https://zapi.z-api.io/instances/" + zApi.getInstanceId()
                + "/token/" + zApi.getToken() + "/send-file";

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(caminhoPdf));
            String base64 = Base64.getEncoder().encodeToString(bytes);

            String payload = """
                    {
                        "phone": "%s",
                        "filename": "catalogo.pdf",
                        "base64": "%s",
                        "caption": "%s"
                    }
                    """.formatted(telefone, base64, legenda);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Envia uma mensagem com botões interativos para o cliente via Z-API.
     *
     * @param telefone número do telefone do cliente
     * @param titulo   título da mensagem
     * @param corpo    corpo da mensagem
     * @param rodape   rodapé da mensagem
     * @param opcoes   lista de opções para os botões
     */
    public void enviarMensagemComBotoes(String telefone, String titulo, String corpo, String rodape, List<String> opcoes) {
        String url = "https://api.z-api.io/instances/" + zApi.getInstanceId()
                + "/token/" + zApi.getToken() + "/send-button-list";

        StringBuilder sections = new StringBuilder();
        sections.append("""
            [
              {
                "title": "Escolha uma loja",
                "rows": [
            """);

        for (int i = 0; i < opcoes.size(); i++) {
            String opcao = opcoes.get(i);
            sections.append(String.format("""
                {
                  "rowId": "%d",
                  "title": "%s"
                }%s
                """, i + 1, opcao, (i < opcoes.size() - 1 ? "," : "")));
        }

        sections.append("""
              ]
            }
            ]
            """);

        String payload = String.format("""
            {
              "phone": "%s",
              "body": "%s",
              "footer": "%s",
              "buttonText": "Selecionar",
              "sections": %s
            }
            """, telefone, corpo, rodape, sections.toString());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Client-Token", zApi.getToken()) // se necessário
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Envia uma mensagem com botões de serviço para o cliente via Z-API.
     *
     * @param telefone número do telefone do cliente
     */
    public void enviarBotoesDeServico(String telefone) {
        String url = "https://api.z-api.io/instances/" + zApi.getInstanceId()
                + "/token/" + zApi.getToken() + "/send-button-list";

        try {
            String payload = """
                {
                    "phone": "%s",
                    "body": "🚲 Qual serviço você deseja agendar hoje?",
                    "footer": "Escolha abaixo o serviço desejado",
                    "buttonText": "Selecionar serviço",
                    "sections": [
                        {
                            "title": "Serviços disponíveis",
                            "rows": [
                                {
                                    "rowId": "1",
                                    "title": "Revisão completa",
                                    "description": "Deixe sua bike como nova!"
                                },
                                {
                                    "rowId": "2",
                                    "title": "Troca de peças",
                                    "description": "Pneus, câmbios, freios e mais!"
                                },
                                {
                                    "rowId": "3",
                                    "title": "Compra de produtos",
                                    "description": "Acesse nosso catálogo!"
                                },
                                {
                                    "rowId": "4",
                                    "title": "Outros serviços",
                                    "description": "Personalizados para você"
                                }
                            ]
                        }
                    ]
                }
                """.formatted(telefone);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Client-Token", zApi.getToken()) // se necessário
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Envia uma lista de opções para o cliente via Z-API.
     *
     * @param payload mapa contendo os dados da mensagem
     */
    public void enviarOptionList(Map<String, Object> payload) {
        try {
            String url = "https://api.z-api.io/instances/" + zApi.getInstanceId()
                    + "/token/" + zApi.getToken() + "/send-option-list";

            String json = new ObjectMapper().writeValueAsString(payload); // precisa da lib Jackson

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Client-Token", zApi.getToken())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("Erro ao enviar option list: " + e.getMessage());
        }
    }

}