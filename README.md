# Biblioteca Compartilhada Central (`15soat-phase4-lib-commons`)

Biblioteca multi-módulos centralizada e compartilhada desenvolvida para o Tech Challenge da Fase 4 da pós-graduação em Software Architecture (SOAT) da FIAP.

## Estrutura de Módulos

A biblioteca segue uma rigorosa separação de responsabilidades inspirada nos padrões corporativos para microsserviços:

- **`mod-domain`**: Regras de negócio puras, exceções de domínio canônicas (`DomainException`, `ResourceNotFoundException`, `AlreadyExistsException`, `ValidationException`) e *Value Objects* (`CpfOrCnpj`, `LicensePlate`, `Email`, `Money`). **Zero dependências externas de frameworks** no núcleo puro (sem acoplamento a Spring ou anotações invasivas).
- **`mod-web`**: Suporte transversal à camada Web, incluindo tratamento padronizado de erros RFC 7807/9457 `ProblemDetail` via `CommonsExceptionAdvice`, propagação de `traceId`/`spanId` no MDC do SLF4J via `LogInterceptor`, e DTOs padronizados de resposta (`GenericResponseDto`, `ErrorResponseDto`).
- **`mod-kafka`**: Abstrações canônicas de streaming de eventos (`EventEnvelope<T>`, `EventMetadata`), serializadores/desserializadores JSON robustos e injeção/extração de headers no padrão W3C TraceContext (`traceparent`) para rastreabilidade distribuída de ponta a ponta.
- **`mod-postgres`**: Utilitários para persistência relacional com JPA/Hibernate (`BaseJpaEntity`, `AuditableEntity`), controle de concorrência otimista (`@Version`) e paginação dinâmica padronizada (`PageRequestUtils`).
- **`mod-mongo`**: Utilitários para persistência NoSQL orientada a documentos no MongoDB / AWS DocumentDB (`BaseMongoDocument`, `AuditableDocument`) e conversores UTC para `ZonedDateTime`.
- **`mod-aot`**: Registrador de dicas de execução AOT da GraalVM em Java puro (`CommonsRuntimeHintsRegistrar`) para compilação nativa de alta performance sem necessidade de arquivos externos de metadados JSON.

## Stack Tecnológica & Compatibilidade

- **Linguagem**: Java 25 LTS (OpenJDK)
- **Framework Base**: Spring Boot 4.0.7 / Spring Framework 7
- **Ferramenta de Build**: Apache Maven 3.9+
- **Testes Automatizados**: JUnit 5 Jupiter, AssertJ

## Compilação e Instalação Local

Para compilar, executar a suíte de testes unitários e instalar todos os módulos no repositório Maven local (`~/.m2/repository`):

```bash
mvn clean install
```

## 🏛️ Architecture Decision Records (ADRs)

Catálogo centralizado de decisões arquiteturais transversais aplicadas a todos os microsserviços da Fase 4:

- **[Catálogo Global de ADRs](docs/adr/README.md)**: 6 registros formais de decisão (Padrão Database-per-Service, Arquitetura Orientada a Eventos e Saga Coreografada via Kafka, Concessão Pragmatic DDD com JPA no Domínio, Convenção de Tabelas no Singular, Baseline Java 25 / Spring Boot 4, e Estratégia de Testcontainers & Observabilidade Distribuída).
