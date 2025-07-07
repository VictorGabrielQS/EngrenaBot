package VictorCoddys.EngrenaBot.Model;

import lombok.Data;

//Dados coletados no WhatsApp para o Agendamento

@Data
public class Agendamento {
    private String telefone;
    private String loja;         // Ex: "Loja Forte ville", "Loja Novo Horizonte"
    private String nome;
    private String tipoServico;
    private String observacao;   // Ex: "Ajuste de freio"
    private String data;         // Ex: "08/07/2025"
    private String horario;      // Ex: "14:30"
}
