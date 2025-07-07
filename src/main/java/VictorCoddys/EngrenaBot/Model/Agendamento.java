package VictorCoddys.EngrenaBot.Model;

import lombok.Data;

//Dados coletados no WhatsApp para o Agendamento

@Data
public class Agendamento {
    private String telefone;
    private String nome;
    private String tipoServico;
    private String dataHora;
}
