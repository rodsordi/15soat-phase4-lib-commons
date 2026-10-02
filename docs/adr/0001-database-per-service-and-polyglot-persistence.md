# ADR 0001: Padrão Database-per-Service e Persistência Poliglota (PostgreSQL + MongoDB)

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Global (Todos os Microsserviços da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Arquitetura de Software e Engenharia de Dados

---

## 1. Contexto e Declaração do Problema

Na Fase 3 do Tech Challenge, a aplicação monolítica `api-garage` concentrava todas as operações (clientes, veículos, catálogo, ordens de serviço, faturamento e execução da oficina) em um único banco de dados relacional compartilhado (`garage_db`).

Com a decomposição para a arquitetura de microsserviços distribuídos da Fase 4, manter um banco compartilhado geraria:
* **Forte Acoplamento de Esquema**: Alterações no modelo de dados de um serviço afetariam diretamente outros domínios.
* **Inadequação de Modelo para Operações Dinâmicas**: A execução de oficina mecânica possui formulários de inspeção e checklists com alta variação de atributos, o que geraria complexidade excessiva caso forçado em tabelas relacionais normalizadas.
* **Gargalo Transacional e de Concorrência**: Operações pesadas de leitura de catálogos e escrita transacional de pagamentos concorreriam pelas mesmas conexões de banco.

---

## 2. Decisão Arquitetural

Decidimos adotar o padrão **Database-per-Service** combinado com **Persistência Poliglota**, provisionado via Terraform (`15soat-phase4-iac-db`):

1. **`work_order_db` (PostgreSQL 16 - Relacional / ACID)**:
   - Pertence exclusivamente ao microsserviço `15soat-phase4-api-work-order`.
   - Gerencia entidades relacionais críticas com integridade referencial estrita (`customer`, `vehicle`, `work_order`, `service`, `material`, `work_order_service`, `work_order_material`).

2. **`billing_db` (PostgreSQL 16 - Relacional / ACID)**:
   - Pertence exclusivamente ao microsserviço `15soat-phase4-api-billing`.
   - Gerencia faturamento, cobrança e histórico transacional de webhooks (`invoice`, `payment_webhook`), garantindo isolamento financeiro.

3. **`exec_db` (AWS DocumentDB / MongoDB 7.0 - NoSQL / Orientado a Documentos)**:
   - Pertence exclusivamente ao microsserviço `15soat-phase4-api-exec`.
   - Modela ordens de execução de oficina mecânica com documentos BSON/JSON ricos, embutindo itens de inspeção e materiais aplicados em estruturas dinâmicas.

---

## 3. Consequências

### Positivas:
* **Desacoplamento Total**: Nenhum microsserviço tem acesso direto ou permissão no banco de outro serviço.
* **Escalabilidade Independente**: `work_order_db` pode escalar conexões e IOPS independentemente do `billing_db` ou do cluster `exec_db`.
* **Uso da Ferramenta Certa para a Tarefa Certa**: PostgreSQL para transações ACID e integridade financeira; MongoDB para flexibilidade de documentos operacionais.

### Negativas / Trade-offs:
* **Fim dos JOINs no Banco**: Consultas entre domínios (ex: saber a OS e o status de pagamento) agora devem ser correlacionadas via eventos assíncronos Kafka ou IDs lógicos externos.
* **Operação de Infraestrutura Multi-Engine**: Exige gerenciar backups, replicação e segurança tanto para instâncias RDS PostgreSQL quanto para clusters DocumentDB.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Banco Único Compartilhado (Shared Database Anti-pattern)** | **Rejeitada** | Destrói a independência dos microsserviços e viola os princípios fundamentais da Fase 4. |
| **Apenas Bancos Relacionais (PostgreSQL para todos)** | **Rejeitada** | Ignora a natureza semiestruturada e volátil dos checklists de inspeção da oficina (`api-exec`). |
| **Apenas NoSQL (MongoDB para todos)** | **Rejeitada** | Comprometeria a consistência contábil, cálculos de totais e constraints relacionais de faturamento e ordens de serviço. |
