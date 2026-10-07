# Catálogo Global de ADRs (Architecture Decision Records) - Fase 4

Este diretório centraliza os registros formais de decisões de arquitetura (**Architecture Decision Records - ADRs**) que estabelecem os padrões técnicos universais, invariantes e mandatórios compartilhados por **todos os microsserviços da Fase 4**:

---

## 📚 Índice de Decisões Arquiteturais Globais

| ADR | Título | Status | Data | Escopo |
| :---: | :--- | :---: | :---: | :--- |
| **[ADR 0001](0001-database-per-service-and-polyglot-persistence.md)** | Padrão Database-per-Service e Persistência Poliglota (PostgreSQL + MongoDB) | **Aceito** | 2026-10-02 | Global (Bancos de Dados) |
| **[ADR 0002](0002-event-driven-architecture-and-choreographed-saga-kafka.md)** | Arquitetura Orientada a Eventos e Orquestração Coreografada de Saga via Apache Kafka | **Aceito** | 2026-10-02 | Global (Mensageria & Saga) |
| **[ADR 0003](0003-pragmatic-domain-driven-design-jpa-annotations-concession.md)** | Concessão Arquitetural Pragmatic Domain-Driven Design (Fusão de JPA no Módulo Domain) | **Aceito** | 2026-10-02 | Global (Domínio & ORM) |
| **[ADR 0004](0004-singular-table-naming-convention-sql.md)** | Convenção Singular e Padrão de Nomenclatura para Modelagem de Dados Relacional | **Aceito** | 2026-10-02 | Global (Schemas SQL) |
| **[ADR 0005](0005-java-25-spring-boot-4-graalvm-baseline.md)** | Adoção do Java 25 e Spring Boot 4 com Suporte a GraalVM Native Image | **Aceito** | 2026-10-02 | Global (Stack & Runtime) |
| **[ADR 0006](0006-observability-traceability-and-testcontainers-strategy.md)** | Estratégia de Testes Automatizados Testcontainers, Observabilidade e Rastreabilidade | **Aceito** | 2026-10-02 | Global (QA & SRE) |
| **[ADR 0007](0007-dto-specification-contract-records-hateoas-pagination.md)** | Padronização de DTOs com Nested Records, Contratos de Interface, HATEOAS e Paginação | **Aceito** | 2026-10-06 | Global (APIs & Contratos DTO) |

---

## 🏛️ ADRs de Decisões Específicas dos Microsserviços

Para decisões restritas ao domínio ou implementação técnica de um serviço em particular, consulte a documentação local de cada projeto:
* **`api-work-order`**: [Catálogo de ADRs de Ordem de Serviço](../../15soat-phase4-api-work-order/docs/adr/README.md)
* **`api-billing`**: [Catálogo de ADRs de Faturamento](../../15soat-phase4-api-billing/docs/adr/README.md)
* **`api-exec`**: [Catálogo de ADRs de Execução de Oficina](../../15soat-phase4-api-exec/docs/adr/README.md)
