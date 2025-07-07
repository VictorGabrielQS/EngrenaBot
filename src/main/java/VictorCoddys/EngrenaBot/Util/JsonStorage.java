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



    /** Salva um agendamento no arquivo JSON.
     *
     * / @param agendamento O objeto Agendamento a ser salvo.
     */
    public static void salvarAgendamento(Agendamento agendamento) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Agendamento> agendamentos = listarAgendamentos();
            agendamentos.add(agendamento);
            mapper.writeValue(new File(FILE_PATH), agendamentos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /** Lista todos os agendamentos armazenados no arquivo JSON.
     *
     * /@return Uma lista de objetos Agendamento.
     */
    public static List<Agendamento> listarAgendamentos() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            File file = new File(FILE_PATH);
            if (!file.exists()) return new ArrayList<>();
            return mapper.readValue(file, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}



