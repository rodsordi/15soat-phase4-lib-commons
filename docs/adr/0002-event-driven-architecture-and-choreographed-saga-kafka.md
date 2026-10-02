# ADR 0002: Arquitetura Orientada a Eventos e Orquestração Coreografada de Saga via Apache Kafka

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Global (Todos os Microsserviços da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Arquitetura de Software e Sistemas Distribuídos

---

## 1. Contexto e Declaração do Problema

Na Fase 3, as transições da ordem de serviço dependiam de tópicos SNS e filas SQS para notificação de clientes, enquanto operações críticas de negócio ainda ocorriam de forma predominantemente síncrona dentro da mesma aplicação.

Com a separação em microsserviços na Fase 4, a conclusão de uma Ordem de Serviço requer uma transação distribuída de longa duração (*Long-Running Distributed Transaction*):
1. A Ordem de Serviço é aprovada pelo cliente no `api-work-order`.
2. Uma Fatura precisa ser gerada e cobrada no `api-billing`.
3. Uma vez pago, o serviço de oficina no `api-exec` deve ser enfileirado para os mecânicos.
4. Caso o pagamento falhe ou seja rejeitado pelo gateway, a Ordem de Serviço deve ser cancelada através de uma transação compensatória (*Compensating Transaction*).

Utilizar chamadas síncronas HTTP REST para esse fluxo geraria acoplamento temporal, vulnerabilidade a falhas em cascata e bloqueio de threads (*thread starvation*).

---

## 2. Decisão Arquitetural

Decidimos adotar o padrão **Choreographed Saga (Saga Coreografada)** orientada a eventos assíncronos sobre o **Apache Kafka**:

```
[api-work-order] --(WorkOrderApprovedEvent)--> [Kafka Topic: work-order.approved]
                                                         │
                                                         ▼
                                                  [api-billing]
                                                         │
                        ┌────────────────────────────────┴────────────────────────────────┐
                        ▼                                                                 ▼
           (PaymentConfirmedEvent)                                              (PaymentFailedEvent)
                        │                                                                 │
                        ▼                                                                 ▼
           [Kafka: payment.confirmed]                                           [Kafka: payment.failed]
                        │                                                                 │
                        ▼                                                                 ▼
                   [api-exec]                                                    [api-work-order]
        (Inicia reparo na oficina)                                         (Rollback Compensatório: OS Canceled)
```

### 2.1. Princípios da Coreografia
* **Descentralização Total**: Não há um orquestrador central ou ponto único de falha; cada microsserviço reage autonomamente aos eventos dos tópicos que assina.
* **Garantia de Entrega At-Least-Once com Idempotência**: Consumidores utilizam o módulo compartilhado `mod-kafka` com confirmação manual de offset (`Acknowledgment.acknowledge()`) após commit de banco e validação idempotente por `workOrderId` / `invoiceId`.
* **Rastreabilidade Distribuída**: Todos os eventos propagam cabeçalhos Kafka (`X-Correlation-Id`, `X-Trace-Id`) para correlação unificada de logs.

---

## 3. Consequências

### Positivas:
* **Resiliência e Desacoplamento Temporal**: Se o `api-exec` ou `api-billing` estiver temporariamente indisponível durante um pico de tráfego, as mensagens permanecem retidas e persistidas nos logs do Kafka sem perda de dados.
* **Rollback Automático Determinístico**: A transição para `PAYMENT_REJECTED` ou `CANCELED` na OS ocorre de forma 100% autônoma via consumo do `PaymentFailedEvent`.
* **Auditoria de Eventos**: O log de commits do Kafka serve como histórico imutável das mudanças de estado do sistema.

### Negativas / Trade-offs:
* **Consistência Eventual**: O cliente pode receber um HTTP 200/202 ao aprovar a OS, mas o status final depende da confirmação assíncrona do pagamento.
* **Complexidade Operacional**: Exige monitorar *consumer lag*, partições e retenção do cluster Kafka.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Saga Orquestrada Centralizada (Camunda / Temporal / AWS Step Functions)** | **Rejeitada** | Adicionaria um ponto único de falha e sobrecarga operacional desproporcional para o escopo do desafio. |
| **Encadeamento Síncrono HTTP REST** | **Rejeitada** | Extremamente frágil; uma lentidão no gateway de pagamento derrubaria a thread de aprovação da OS e bloquearia o atendente. |
| **Amazon SNS + SQS (Modelo Fase 3)** | **Rejeitada** | Não oferece replay de mensagens com facilidade de particionamento e ordenação garantida por chave (`workOrderId`) nativa do Kafka. |
