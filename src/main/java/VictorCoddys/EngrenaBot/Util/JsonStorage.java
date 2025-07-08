package VictorCoddys.EngrenaBot.Util;

import VictorCoddys.EngrenaBot.Model.Agendamento;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonStorage {

    //Arquivo JSON para armazenar os agendamentos
    private static final String FILE_PATH = "agendamentos.json";



    /** Obtém o caminho do arquivo JSON específico para a loja.
     *
     * / @param loja O nome da loja para determinar o arquivo.
     * / @return O caminho do arquivo JSON correspondente à loja.
     */
    private static String getPathArquivoPorLoja(String loja) {
        String nomeArquivo = switch (loja.toLowerCase()) {
            case "loja forte ville" -> "loja_forte_ville.json";
            case "loja novo horizonte" -> "loja_novo_horizonte.json";
            default -> "outros.json";
        };
        return "agendamentos/" + nomeArquivo;
    }




    /** Salva um agendamento no arquivo JSON.
     *
     * / @param agendamento O objeto Agendamento a ser salvo.
     */
    public static void salvarAgendamento(Agendamento agendamento) {
        try {
            String path = getPathArquivoPorLoja(agendamento.getLoja());
            File file = new File(path);
            ObjectMapper mapper = new ObjectMapper();
            List<Agendamento> agendamentos = new ArrayList<>();

            if (file.exists() && file.length() > 0) {
                agendamentos = mapper.readValue(file, new TypeReference<>() {});
            } else {
                file.getParentFile().mkdirs(); // Garante que a pasta existe
                file.createNewFile();
            }

            agendamentos.add(agendamento);
            mapper.writeValue(file, agendamentos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    /** Lista todos os agendamentos armazenados no arquivo JSON.
     *
     * /@return Uma lista de objetos Agendamento.
     */
    public static List<Agendamento> listarAgendamentos() {
        List<Agendamento> todos = new ArrayList<>();
        File pasta = new File("agendamentos");

        if (pasta.exists()) {
            File[] arquivos = pasta.listFiles((dir, name) -> name.endsWith(".json"));
            ObjectMapper mapper = new ObjectMapper();

            if (arquivos != null) {
                for (File file : arquivos) {
                    try {
                        List<Agendamento> ags = mapper.readValue(file, new TypeReference<>() {});
                        todos.addAll(ags);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return todos;
    }




    /** Limpa todos os agendamentos armazenados no arquivo JSON.
     *
     * / Isso remove todos os agendamentos, deixando o arquivo vazio.
     */
    public static void limparAgendamentos() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File("agendamentos.json"), new ArrayList<>()); // limpa
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}



