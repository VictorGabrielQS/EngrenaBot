# 🤖 EngrenaBot — Bot de Agendamento para Oficinas Bike Rogers

EngrenaBot é um sistema de atendimento automatizado via WhatsApp que permite aos clientes agendarem serviços, comprarem produtos e receberem catálogos em PDF de forma prática. Integrado com a **Z-API**, o bot realiza todo o fluxo de conversa, valida datas e horários, armazena agendamentos localmente em arquivos JSON e notifica o mecânico ou setor responsável.

---

## 📦 Funcionalidades

✅ Fluxo de agendamento completo com:

- Escolha da loja
- Coleta de nome
- Tipos de serviço (revisão, peças, compra, outros)
- Validação de dias úteis e horários
- Envio de catálogo PDF para clientes
- Envio de informações para setor de vendas
- Armazenamento local dos agendamentos

---

## 🚀 Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.5.3**
- **Z-API** (integração com WhatsApp)
- **Armazenamento em JSON local**
- **iText7** (para geração de PDFs estilizados)

---

## ⚙️ Como Rodar Localmente

### Pré-requisitos:

- Java 17+ 
- Maven 3.9+
- (Opcional) Conta Z-API ativa

### Passos:

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/engrenabot.git
cd engrenabot

# Rode a aplicação
./mvnw spring-boot:run
```

A aplicação será iniciada em `http://localhost:8585`

---

## 🛠️ Estrutura do Projeto

```
src/main/java/
├── controller/          # Endpoint que recebe mensagens do cliente
├── service/             # Lógica do fluxo de atendimento (BotService)
├── util/                # Classe utilitária para salvar JSON
├── model/               # Agendamento, EstadoFluxo (enum)
├── config/              # Propriedades externas (Z-API, vendas, catálogo)
```

---

## 🔐 Configurações Externas

### application.properties:

```yaml

# Porta padrão
server.port=8585

# Configurações do ZAPI
zapi.instance-id=SUA_INSTANCIA
zapi.token=SEU_TOKEN

# Configurações do Bot
catalogo.caminho-pdf=src/main/resources/static/catalogo.pdf
contatos.telefone-vendas=(62) 98186-6691
contatos.telefone-mecanico=(62) 98186-6691

```

---

## 📄 Formato de Armazenamento (JSON)

Os agendamentos são armazenados no arquivo:

```
data/loja-forte-ville/agendamentos.json
```

Formato:

```json
{
  "nome": "João",
  "telefone": "62999999999",
  "loja": "Loja Forte Ville",
  "tipoServico": "Revisão",
  "data": "10/07/2025",
  "horario": "14:00",
  "observacao": "Revisão geral"
}
```

---

## 📲 Integração com Z-API

Para envio de mensagens e arquivos pelo WhatsApp, a aplicação faz requisições HTTP para a API oficial da Z-API.

Você deve possuir uma **instância ativa** e preencher os dados no `application.properties`.

---


## 📧 Contato

Em caso de dúvidas ou suporte:

**Victor Gabriel**  
📧 victorgabriel.codes@gmail.com  
📱 WhatsApp: (62) 99426-7940

---

## 📜 Licença

Projeto de uso privado para fins comerciais da oficina **Bike Rogers**.