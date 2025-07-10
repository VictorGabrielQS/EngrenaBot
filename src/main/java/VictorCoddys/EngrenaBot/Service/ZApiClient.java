package VictorCoddys.EngrenaBot.Service;

import VictorCoddys.EngrenaBot.Config.ZApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ZApiClient {

    private final ZApiProperties zApi;

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
}
