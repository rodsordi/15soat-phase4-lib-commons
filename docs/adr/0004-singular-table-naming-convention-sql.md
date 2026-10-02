# ADR 0004: Convenção Singular e Padrão de Nomenclatura para Modelagem de Dados Relacional

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Global (Bancos de Dados Relacionais da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Modelagem de Dados e Engenharia de Banco

---

## 1. Contexto e Declaração do Problema

Durante a evolução dos schemas do PostgreSQL nos microsserviços `15soat-phase4-api-work-order` e `15soat-phase4-api-billing`, foram inicialmente adotadas tabelas no plural (ex: `work_orders`, `services`, `materials`, `invoices`).

No entanto:
* Na **Fase 3 do Tech Challenge** (`15-soat-tech-challenge-garage`), todas as tabelas foram padronizadas rigorosamente no **singular** (`customer`, `vehicle`, `employee`, `material`, `service`, `work_order`, `estimated_service`, `estimated_material`).
* A utilização do plural no SQL gera fricção com linguagens orientadas a objetos como Java, onde as classes são essencialmente singulares (`Customer`, `WorkOrder`), exigindo mapeamento forçado de nomes ou regras complexas de pluralização no inglês (`Category` -> `categories`, `Status` -> `statuses`, `Person` -> `people`).

---

## 2. Decisão Arquitetural

Decidimos padronizar **todas as tabelas e índices relacionais no singular** em todos os bancos de dados dos microsserviços da Fase 4:

### 2.1. Regras de Nomenclatura
1. **Nome de Tabelas no Singular**:
   - `customer`, `vehicle`, `work_order`, `work_order_service`, `work_order_material`, `service`, `material`, `invoice`, `payment_webhook`.
2. **Padrão Snake_Case**:
   - Todas as tabelas e colunas utilizam minúsculas separadas por sublinhado (`snake_case`).
3. **Chaves Estrangeiras Previsíveis**:
   - Nomenclatura no formato `[tabela_destino]_id` (ex: `customer_id`, `vehicle_id`, `service_id`, `material_id`).
4. **Convenção de Constraints e Índices**:
   - Primary Keys: `pk_[tabela]` ou implícito via DDL.
   - Foreign Keys: `fk_[origem]_[destino]` (ex: `fk_vehicle_customer`, `fk_wos_work_order`).
   - Índices: `idx_[tabela]_[coluna]` (ex: `idx_work_order_status`, `idx_customer_document`).

---

## 3. Consequências

### Positivas:
* **Paridade de 100% com a Fase 3**: Alinhamento histórico sem quebras de convenção em relação aos projetos anteriores da pós-graduação.
* **Mapeamento 1:1 Direto com o Código Java**: Eliminação de anotações complexas de `@Table` com pluralizações irregulares.
* **Legibilidade e Coerência Conceitual**: Segue a definição formal do Modelo Entidade-Relacionamento (ER / ISO SQL), onde cada tabela representa a definição do tipo de uma entidade.

### Negativas / Trade-offs:
* Nenhuma desvantagem técnica identificada; exigiu apenas refatoração das migrações Flyway e scripts DDL do banco.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Padrão Plural (work_orders, services, invoices)** | **Rejeitada** | Quebrava a consistência com a Fase 3 e introduzia irregularidades de pluralização da língua inglesa no mapeamento JPA. |
| **Padrão Misto (Algumas tabelas no singular e associativas no plural)** | **Rejeitada** | Péssima prática de governança que prejudicaria a previsibilidade do código para o time. |
