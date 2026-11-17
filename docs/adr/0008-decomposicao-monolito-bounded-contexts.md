# ADR 0008: Decomposição do Monólito por Bounded Contexts, Mensageria Kafka e Persistência Poliglota

* **Status**: Aceito (Accepted)
* **Data**: 2026-11-16
* **Escopo**: Global (Todos os Microsserviços da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Arquitetura de Software, Engenharia de Dados e Sistemas Distribuídos

---

## 1. Contexto e Declaração do Problema

Na Fase 3 do Tech Challenge, o ecossistema operava sob uma arquitetura monolítica centrada na aplicação `api-garage`. Todas as capacidades do negócio — cadastro de clientes e veículos, catálogo de peças e serviços, abertura e ciclo de vida de ordens de serviço, execução operacional na oficina mecânica e faturamento com gateways de pagamento — residiam na mesma base de código e compartilhavam um único banco de dados relacional (`garage_db`).

A operação do monólito apresentou limitações estruturais severas à medida que a complexidade do negócio aumentou:

1. **Acoplamento de Domínio e Violação de Limites Conceituais**:
   - Conceitos com ciclos de vida e atores distintos (o atendente que registra a demanda, o mecânico que realiza inspeções físicas na baia e o cliente que avalia a cobrança financeira) estavam fundidos no mesmo modelo de classes e tabelas.
   - Mudanças nas rotinas operacionais da oficina (como adição de novos itens a um checklist de freios) exigiam deploy integral do monólito, gerando risco de indisponibilidade no faturamento ou no balcão de atendimento.

2. **Acoplamento Temporal e Fragilidade Transacional Síncrona**:
   - As transições de status da Ordem de Serviço dependiam de invocações síncronas em memória ou chamadas acopladas. Uma instabilidade no gateway externo de pagamento ou uma lentidão no cálculo de peças bloqueava a thread de atendimento do cliente, propagando falhas em cascata (*cascading failures*).

3. **Inadequação do Modelo Único de Persistência (Impedância Objeto-Relacional)**:
   - Forçar dados operacionais altamente semiestruturados e variáveis (checklists de oficina mecânica, anotações de inspeção técnica) dentro de tabelas relacionais normalizadas gerava complexidade excessiva de junções (*JOINs*) ou induzia ao anti-pattern *Entity-Attribute-Value (EAV)*.
   - Ao mesmo tempo, dados financeiros de faturamento concorriam pelo mesmo pool de conexões e locks de linha com leituras de catálogo e atualizações de clientes.

---

## 2. Decisão Arquitetural

Decidimos decompor o monólito `api-garage` da Fase 3 em uma arquitetura distribuída orientada a eventos, fundamentada em três pilares essenciais:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                     API GATEWAY / INGRESS                              │
└──────────────┬───────────────────────────┬───────────────────────────┬─────────────────┘
               │                           │                           │
               ▼                           ▼                           ▼
 ┌───────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────┐
 │15soat-phase4-api-work-order│ │ 15soat-phase4-api-exec    │ │ 15soat-phase4-api-billing │
 │       (Core Domain)       │ │    (Supporting Domain)    │ │    (Generic Subdomain)    │
 ├───────────────────────────┤ ├───────────────────────────┤ ├───────────────────────────┤
 │ • Gestão de OS Comercial  │ │ • Operação da Oficina     │ │ • Orçamentos (Estimates)  │
 │ • Clientes e Veículos     │ │ • Alocação de Mecânicos   │ │ • Faturas (Invoices)      │
 │ • Catálogo e Estoque      │ │ • Checklists Dinâmicos    │ │ • Webhooks Mercado Pago   │
 └─────────────┬─────────────┘ └─────────────┬─────────────┘ └─────────────┬─────────────┘
               │                             │                             │
    PostgreSQL (work_order_db)       MongoDB (exec_db)            PostgreSQL (billing_db)
               │                             │                             │
               └──────────────────────┐      │      ┌──────────────────────┘
                                      ▼      ▼      ▼
                        ┌───────────────────────────────────────────┐
                        │      APACHE KAFKA (Saga Coreografada)     │
                        │ • work-order-events                       │
                        │ • execution-events                        │
                        │ • billing-events / payment-events         │
                        └───────────────────────────────────────────┘
```

---

### 2.1. Quebra de Domínio e Delimitação por *Bounded Contexts* (DDD Estratégico)

A decomposição funcional seguiu rigorosamente os padrões de **Domain-Driven Design Estratégico (Eric Evans)** e **Decomposição por Capacidade de Negócio (Sam Newman)**:

#### A. `15soat-phase4-api-work-order` — *Core Domain* (Domínio Principal)
- **Responsabilidade de Negócio**: Centralizar o ciclo de vida comercial da Ordem de Serviço, o cadastro mestre de entidades primárias (`Customer`, `Vehicle`) e o controle transacional do catálogo (`Service`, `Material`) com reserva e baixa física de estoque.
- **Justificativa de Isolamento**: Representa o núcleo de receita e o contrato canônico do atendimento. O ciclo de vida da OS obedece a uma máquina de estados estrita (`RECEIVED` ➔ `DIAGNOSING` ➔ `WAITING_APPROVAL` ➔ `APPROVED` ➔ `EXECUTING` ➔ `COMPLETED` ➔ `PAID` ➔ `DELIVERED` ou `CANCELED`).
- **Segregação de Deveres (*SoD*)**: O atendente cria a OS e gerencia cadastros, mas não atua como mecânico nem aprova financeiramente o serviço.

#### B. `15soat-phase4-api-exec` — *Supporting Subdomain* (Subdomínio de Suporte Operacional)
- **Responsabilidade de Negócio**: Gerenciar a execução física do serviço no chão de oficina, controle de produtividade de técnicos/mecânicos e preenchimento de checklists de inspeção técnica veicular.
- **Justificativa de Isolamento**: A oficina possui rotinas e ciclos de trabalho assíncronos em relação ao atendimento balcão. Um mecânico inspeciona componentes, aponta itens danificados e consome insumos diretamente na baia.
- **Segregação de Deveres (*SoD*)**: Os mecânicos possuem permissão restrita para lançar itens de diagnóstico e registrar conclusão de tarefas, sendo impedidos de conceder descontos, manipular faturamento ou aprovar orçamentos.

#### C. `15soat-phase4-api-billing` — *Generic Subdomain* (Subdomínio Genérico Financeiro)
- **Responsabilidade de Negócio**: Gestão do ciclo financeiro soberano: consolidação do orçamento detalhado (*Estimate*), envio de propostas ao cliente com link seguro de decisão, emissão de faturas (*Invoice*), geração de QR Code PIX e liquidação idempotente via webhooks de gateway de pagamento (Mercado Pago).
- **Justificativa de Isolamento**: Conforme prescrito pelo DDD, finanças e conciliação contábil constituem um subdomínio genérico que não deve contaminar as regras de engenharia mecânica ou atendimento balcão. O isolamento garante conformidade com requisitos rigorosos de auditoria contábil, idempotência financeira e PCI-DSS/LGPD.
- **Decisão Soberana do Cliente**: O cliente aprova ou rejeita o orçamento diretamente na `api-billing`, garantindo transparência financeira sem que funcionários da oficina possam adulterar a decisão.

---

### 2.2. Desacoplamento Temporal e Orquestração por Mensageria (Apache Kafka)

A comunicação entre os microsserviços foi desenhada sob o padrão **Choreographed Saga (Saga Coreografada)** orientada a eventos assíncronos sobre o **Apache Kafka**, em substituição ao encadeamento síncrono HTTP REST:

#### A. Por que o Apache Kafka foi Adotado?
1. **Eliminação do Acoplamento Temporal**:
   - Os serviços produtores de eventos (ex: `api-work-order` ao solicitar orçamento ou `api-billing` ao receber a aprovação do cliente) apenas publicam o evento no respectivo tópico e liberam suas threads imediatamente. Não há dependência de que o serviço consumidor esteja disponível no exato milissegundo da transição.
2. **Resiliência a Picos de Tráfego e Amortecimento de Carga (*Backpressure*)**:
   - Em cenários de alta volumetria (ex: liquidação massiva de pagamentos em fins de semana), mensagens ficam enfileiradas e persistidas nos logs particionados do Kafka sem risco de sobrecarregar ou derrubar os consumidores.
3. **Prevenção de Falhas em Cascata (*Cascading Failures*) e *Thread Starvation***:
   - Se a comunicação entre OS, Faturamento e Oficina ocorresse via HTTP síncrono, uma lentidão de 3 segundos na API de gateway de pagamentos reteria conexões HTTP e threads do tomcat no `api-work-order`, levando rapidamente ao colapso de toda a malha de serviços.
4. **Log Distribuído Imutável e Auditoria Completa**:
   - Os tópicos Kafka (`work-order-events`, `execution-events`, `billing-events`) fornecem um histórico cronológico e imutável de todos os fatos relevantes do negócio, permitindo auditoria, rastreabilidade ponta a ponta e eventuais replays para recuperação de desastres.

#### B. Por que Saga Coreografada e não Orquestração Centralizada?
- **Autonomia Descentralizada**: Não existe um orquestrador central (como Camunda ou Step Functions) atuando como gargalo operacional ou ponto único de falha (*Single Point of Failure - SPOF*).
- **Semântica *At-Least-Once* com Idempotência Transacional**: Cada serviço consome eventos utilizando offset commit manual (`Acknowledgment.acknowledge()`) após persistência local com sucesso, aplicando validação idempotente por chave de negócio (`workOrderId` / `invoiceId`).
- **Transações Compensatórias Determinísticas**: Caso o cliente rejeite o orçamento (`EstimateRejectedEvent`) ou o pagamento falhe (`PaymentFailedEvent`), o evento compensatório propaga o cancelamento da OS e o estorno da reserva de materiais no estoque de forma 100% autônoma.

---

### 2.3. Justificativa da Persistência Poliglota por Serviço (*Database-per-Service*)

Seguindo o padrão canônico **Database-per-Service (Chris Richardson)** e **Persistência Poliglota (Martin Fowler & Pramod Sadalage)**, cada microsserviço recebeu um banco de dados exclusivo, dimensionado para o perfil de acesso e a estrutura dos seus dados:

| Microsserviço | Banco de Dados Adotado | Engine / Tipo | Justificativa Técnica Arquitetural |
| :--- | :--- | :--- | :--- |
| **`15soat-phase4-api-work-order`** | `work_order_db` | **PostgreSQL 16** (Relacional / ACID) | **Integridade Referencial e Consistência Contábil Estrita**: Modelagem de entidades relacionais canônicas (`customer`, `vehicle`, `work_order`, `material`, `service`). Exige *Foreign Keys*, integridade transacional ACID estrita para operações de balcão, constraints de unicidade (`document`, `licensePlate`, `sku`) e controle de concorrência otimista via `@Version` para impedir sobrescritas concorrentes de status. |
| **`15soat-phase4-api-billing`** | `billing_db` | **PostgreSQL 16** (Relacional / ACID) | **Isolamento Financeiro e Precisão Decimal**: Cálculos de orçamentos (*estimates*) e faturas (*invoices*) exigem tipos numéricos de alta precisão (`NUMERIC(12,2)` / `BigDecimal`) para evitar imprecisões de arredondamento em moeda. O isolamento físico do banco previne que consultas administrativas ou relatórios financeiros saturem o banco de atendimento de oficina. Além disso, garante tabelas dedicadas de histórico de webhooks para processamento idempotente. |
| **`15soat-phase4-api-exec`** | `exec_db` | **MongoDB 7.0 / DocumentDB** (NoSQL Orientado a Documentos) | **Natureza Semiestruturada e Volátil de Checklists**: Formulários de inspeção de oficina possuem centenas de tarefas dinâmicas e variáveis conforme a categoria do veículo (freios, suspensão, elétrica, fluidos). No modelo relacional, isso exigiria múltiplas tabelas associativas e dezenas de *JOINs* lentos ou o anti-pattern *EAV*. No MongoDB, a execução mecânica é modelada como um documento BSON rico único (`OrderExecution`), contendo coleções embutidas de checklist e materiais aplicados, persistido e consultado atomicamente com altíssimo throughput de I/O. |

---

## 3. Consequências

### Positivas:
* **Independência Operacional e de Deploy (*Blast Radius* Reduzido)**: Um incidente em uma biblioteca de terceiros na emissão de notas ou no webhook do Mercado Pago não afeta a recepção física de veículos no balcão da oficina nem a execução dos mecânicos na baia.
* **Escalabilidade Elástica Independente**: O `api-billing` pode escalar dinamicamente em datas de alta conciliação de faturas, enquanto o `api-work-order` e o `api-exec` mantêm alocação estável de recursos.
* **Aderência aos Padrões Canônicos da Indústria**: Conformidade estrita com os princípios do DDD Estratégico, Reactive Systems Manifesto, e Twelve-Factor App.

### Negativas / Trade-offs e Mitigações:
* **Consistência Eventual (*Eventual Consistency*)**: Consultas imediatas entre o envio da aprovação e a liberação da OS exigem suporte à consistência eventual.
  - *Mitigação*: Implementação de *resilient polling* nos clientes/testes e garantia de publicação assíncrona com idempotência estrita.
* **Operação de Infraestrutura Poliglota**: A equipe de DevOps gerencia instâncias relacionais RDS PostgreSQL e clusters DocumentDB/MongoDB.
  - *Mitigação*: Automação completa de infraestrutura via Terraform (`15soat-phase4-iac-db`) com parametrização de rede e credenciais isoladas por serviço.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Manutenção do Monólito Modular da Fase 3** | **Rejeitada** | Mantinha o banco compartilhado, o acoplamento de deploy e o risco de indisponibilidade cascata de todo o sistema perante falhas pontuais. |
| **Microsserviços com Banco Único Compartilhado (*Shared Database Anti-Pattern*)** | **Rejeitada** | Cria um "monólito distribuído": destrói o isolamento de domínio, reintroduz contenção de locks de banco e impede a persistência poliglota. |
| **Comunicação Inter-serviços via Síncrono HTTP REST (Webhooks Internos)** | **Rejeitada** | Introduz forte acoplamento temporal, vulnerabilidade a *cascading failures* e consumo descontrolado de pools de threads (*thread starvation*). |
| **Banco Relacional Único para Execução da Oficina (PostgreSQL na Oficina)** | **Rejeitada** | Forçaria dados semiestruturados e dinâmicos de checklist em esquemas rígidos, gerando consultas lentas e manutenibilidade deficiente. |

---

## 5. Referências Canônicas

* **Eric Evans**: *Domain-Driven Design: Tackling Complexity in the Heart of Software* (Addison-Wesley, 2003) — Conceitos de Bounded Context, Core Domain, Supporting Subdomain e Context Mapping.
* **Sam Newman**: *Building Microservices: Designing Fine-Grained Systems* (O'Reilly, 2ª Ed., 2021) & *Monolith to Microservices* (O'Reilly, 2019) — Padrões de decomposição, isolamento de Blast Radius e autonomia de serviços.
* **Chris Richardson**: *Microservices Patterns: With examples in Java* (Manning, 2018) — Padrões Database-per-Service, Saga Pattern e Decompose by Business Capability.
* **Martin Fowler & Pramod Sadalage**: *NoSQL Distilled: A Brief Guide to the Emerging World of Polyglot Persistence* (Addison-Wesley, 2012) — Padrão Polyglot Persistence.
* **Gregor Hohpe & Bobby Woolf**: *Enterprise Integration Patterns: Designing, Building, and Deploying Messaging Solutions* (Addison-Wesley, 2003) — Event-Driven Architecture e Message Brokers.
