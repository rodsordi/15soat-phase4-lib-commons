# ADR 0006: Estratégia de Testes Automatizados Testcontainers, Observabilidade e Rastreabilidade Distribuída

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Global (Todos os Microsserviços e Testes da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em QA, SRE e Confiabilidade de Software

---

## 1. Contexto e Declaração do Problema

Sistemas distribuídos compostos por múltiplos bancos de dados heterogêneos (PostgreSQL e MongoDB) e mensageria assíncrona Kafka enfrentam dois riscos críticos:
1. **Falso Positivo em Testes Locais**: Testes de integração utilizando bancos em memória como H2 não simulam comportamentos reais de concorrência, tipos específicos do PostgreSQL (ex: `JSONB`, `UUID` nativo, constraints de FK) e transações assíncronas do Kafka.
2. **Perda de Rastreabilidade em Erros de Produção**: Em uma Saga coreografada assíncrona, diagnosticar onde uma Ordem de Serviço falhou (se no faturamento, no webhook ou na oficina) é inviável sem correlação ponta a ponta.

---

## 2. Decisão Arquitetural

Decidimos estabelecer as seguintes diretrizes mandatórias para testes e observabilidade em todos os projetos:

### 2.1. Estratégia de Testes Realistas com Testcontainers
* **Zero H2 em Memória**: É proibido o uso de bancos H2 ou mocks simulados para testes de integração de persistência.
* **Testcontainers Oficiais**:
  - Testes de repositório PostgreSQL executam sobre contêineres reais `postgres:16-alpine`.
  - Testes de eventos executam sobre instâncias reais do Apache Kafka (`confluentinc/cp-kafka:7.6.0`).
  - Testes do `api-exec` executam sobre instâncias reais do MongoDB (`mongo:7.0`).
* **Graceful Degradation em Ambientes Sem Docker**: As classes base de teste implementam verificação defensiva de ambiente; caso o Docker daemon não esteja acessível (ex: compilações rápidas em ambientes de desenvolvimento restritos), os testes de integração contêinerizados são pulados defensivamente sem quebrar o build, enquanto a suíte completa é garantida nas pipelines CI/CD do GitHub Actions.

### 2.2. Observabilidade e Rastreabilidade Distribuída
* **Correlação via Correlation ID e Trace ID**:
  - Toda requisição HTTP recebe ou gera um `X-Correlation-Id` e `X-Trace-Id` na camada de borda (API Gateway / Lambda Authorizer).
  - O módulo compartilhado `mod-kafka` propaga esses identificadores automaticamente nos cabeçalhos das mensagens publicadas no Kafka.
  - Consumidores assíncronos injetam esses IDs no MDC (Mapped Diagnostic Context) do Logback/SLF4J, garantindo que todos os logs gerados em qualquer microsserviço possuam a mesma chave de rastreamento.
* **Health Checks Padronizados**: Endpoints `/actuator/health` e métricas `/actuator/prometheus` expostos em todos os serviços executáveis para sondas de Liveness e Readiness do Kubernetes.

---

## 3. Consequências

### Positivas:
* **Fidelidade de Produção**: Bugs de dialeto SQL, violações de chave estrangeira e timeouts de commit do Kafka são capturados nos testes automatizados antes do deploy.
* **Troubleshooting Imediato**: Com o `correlationId` é possível filtrar todos os logs da Saga no CloudWatch ou New Relic em segundos.
* **Qualidade Contínua**: Pipelines de CI executam a suíte completa de forma confiável e reproduzível.

### Negativas / Trade-offs:
* **Tempo de Execução dos Testes**: O download e inicialização de contêineres Docker nos testes de integração consomem mais tempo do que testes puramente unitários, compensado pela velocidade extrema dos testes de domínio puro.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Bancos em Memória H2 / Fongo (Mock de MongoDB)** | **Rejeitada** | Omitia erros reais de SQL, não suportava recursos nativos do PostgreSQL 16 e gerava falsa sensação de segurança. |
| **Logs Não Estruturados sem Correlation ID** | **Rejeitada** | Impossibilitaria a correlação de falhas em uma Saga coreografada com múltiplos tópicos Kafka. |
