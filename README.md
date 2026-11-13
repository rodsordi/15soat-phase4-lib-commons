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
- **`mod-test`**: Regras canônicas de governança e arquitetura com ArchUnit (`CommonArchitectureTest`) para validação estática de conformidade arquitetural.
- **`mod-integration-test`**: Fixtures padronizadas de testes de integração com Testcontainers (`PostgresSetup`, `KafkaSetup`, `MongoSetup`, `LocalStackSetup`), WireMock (`WiremockSetup`) e RestAssured para suítes de testes automatizados com infraestrutura real em contêineres.

---

## 🏛️ Padrão Arquitetural: Arquitetura Hexagonal em 2 Módulos (`domain` & `application`)

Todos os microsserviços do ecossistema da Fase 4 adotam a **Arquitetura Hexagonal (Ports and Adapters)** implementada estritamente através de **2 Módulos Físicos Maven**, garantindo a separação cristalina entre as regras puras de negócio e os adaptadores técnicos:

```text
├── <microservice>-domain       -> Núcleo do Hexágono: Regras de Negócio Puras, Casos de Uso e Portas (Zero Frameworks)
└── <microservice>-application  -> Adaptadores Hexagonais: REST, Kafka, Persistência (JPA/Mongo) e Runtime Spring Boot
```

1. **Módulo `domain` (Núcleo do Hexágono - Pureza POJO)**:
   - `domain.model`: Entidades puras de negócio, agregados, invariantes e *Value Objects*. **Zero dependências de JPA/Hibernate, Spring ou banco de dados**.
   - `domain.port.in`: Portas de Entrada (*Driving Ports* - interfaces dos Casos de Uso).
   - `domain.port.out`: Portas de Saída (*Driven Ports* - interfaces SPI para Repositórios, Event Publishers e Gateways).
   - `domain.usecase`: Implementação dos Casos de Uso (orquestração do fluxo puro de negócio).
2. **Módulo `application` (Adaptadores de Entrada/Saída & Runtime)**:
   - `adapter.in.*`: Adaptadores Primários (*Driving Adapters* - REST Controllers OpenAPI/RFC 7807, Consumidores Kafka).
   - `adapter.out.persistence.*`: Adaptadores de Persistência (*Driven Adapters* - JPA/Mongo Entities, Mappers, Spring Data Repositories e Repository Adapters).
   - `adapter.out.kafka.*` / `adapter.out.client.*`: Adaptadores Secundários de Mensageria e Clientes HTTP externos.
   - `config`: Configurações do Spring Boot, injeção de dependências IoC, Virtual Threads e hints GraalVM AOT.

---

### 1. Diagrama Hexagonal Canônico nos 2 Módulos Maven

O diagrama abaixo ilustra a visão arquitetural onde o módulo **`application`** atua como a casca executável e provedora de adaptadores, encapsulando no seu centro o módulo **`domain`** como o núcleo puro de negócio via Portas e Inversão de Dependência (DIP):

<div style="overflow-x: auto; width: 100%;">
<style>
  .mermaid svg {
    max-width: none !important;
    min-width: 1550px !important;
    width: 1550px !important;
  }
</style>

```mermaid
flowchart TD
    %% =========================================================================
    %% MÓDULO MAVEN: APPLICATION (Contém os adaptadores e envelopa o núcleo)
    %% =========================================================================
    subgraph APP_MODULE["Módulo Maven: application (Adaptadores Hexagonais & Runtime de Entrega)"]
        direction TB

        subgraph ADAPTERS_IN["Adaptadores de Entrada (Inbound / Driving Adapters)"]
            REST["adapter.in.web\nREST Controllers (@RestController)\nOpenAPI 3.0 / RFC 7807 ProblemDetails"]
            KAFKA_IN["adapter.in.kafka\nKafka Consumers (@KafkaListener)\nEvent Handlers & Deduplicação"]
        end

        %% =====================================================================
        %% MÓDULO MAVEN: DOMAIN (Núcleo Puro Centralizado no Meio do Application)
        %% =====================================================================
        subgraph DOMAIN_MODULE["Módulo Maven: domain (Núcleo Puro do Hexágono - Zero Frameworks)"]
            direction TB

            subgraph PORTS_IN["Portas de Entrada (Driving / Inbound Ports)"]
                IN_PORT["domain.port.in\nInterfaces dos Casos de Uso\n(ex: CreateInvoiceUseCase, ProcessPaymentUseCase)"]
            end

            subgraph USECASES["Implementação dos Casos de Uso (Application Services)"]
                UC_IMPL["domain.usecase\nUse Case Implementations\n(Coordenação do Fluxo de Negócio Puro)"]
            end

            subgraph DOMAIN_CORE["Modelo de Negócio Puro (Domain Model)"]
                ENTITIES["domain.model\nEntities & Aggregates (Pure POJOs)\n(Invariantes, Ciclo de Vida & Estados)"]
                VO["Value Objects (mod-domain)\n(Money, CpfOrCnpj, LicensePlate)"]
                EXCEPTIONS["Domain Exceptions\n(DomainException, ValidationException)"]
            end

            subgraph PORTS_OUT["Portas de Saída (Driven / Outbound Ports / SPI)"]
                REPO_PORT["domain.port.out\nRepository Ports\n(ex: InvoiceRepositoryPort)"]
                EVENT_PORT["domain.port.out\nEvent Publisher Ports\n(ex: BillingEventPublisherPort)"]
                GATEWAY_PORT["domain.port.out\nGateway Ports\n(ex: MercadoPagoGatewayPort)"]
            end
        end

        subgraph ADAPTERS_OUT["Adaptadores de Saída (Outbound / Driven Adapters)"]
            direction TB
            subgraph ADAPTERS_PERSISTENCE["adapter.out.persistence (Adaptador de Persistência JPA / Mongo)"]
                REPO_ADAPTER["adapter.out.persistence\nRepository Adapter (ex: InvoiceRepositoryAdapter)\n(Implementa InvoiceRepositoryPort)"]
                MAPPER["adapter.out.persistence.mapper\nEntity Mapper (ex: InvoiceMapper)\n(toDomain <--> toEntity)"]
                JPA_ENTITY["adapter.out.persistence.entity\nORM Entity (@Entity, @Table, @Version)\n(ex: InvoiceJpaEntity - Exclusivo do Application)"]
                SPRING_REPO["adapter.out.persistence.repository\nSpring Data JpaRepository / MongoRepository\n(PostgreSQL / MongoDB)"]
            end

            subgraph ADAPTERS_OTHER_OUT["Outros Adaptadores de Saída"]
                KAFKA_OUT["adapter.out.kafka\nKafka Producers (KafkaTemplate)\nEventEnvelope & W3C TraceContext"]
                EXT_CLIENT["adapter.out.client\nExternal HTTP Clients\n(Mercado Pago Client / REST APIs)"]
            end
        end

        subgraph APP_CONFIG["Configuração & Runtime"]
            SPRING_BOOT["config\nSpring Boot App (@SpringBootApplication)\nIoC Container & Virtual Threads"]
        end
    end

    %% =========================================================================
    %% RELACIONAMENTOS E FLUXO DE CHAMADAS (DIP & REGRA DE DEPENDÊNCIA)
    %% =========================================================================
    REST -->|"1. Invoca Input Port"| IN_PORT
    KAFKA_IN -->|"1. Invoca Input Port"| IN_PORT

    IN_PORT -.->|"Implementado por"| UC_IMPL
    UC_IMPL -->|"2. Manipula modelo puro"| ENTITIES
    ENTITIES --- VO
    ENTITIES --- EXCEPTIONS

    UC_IMPL -->|"3. Invoca Output Port"| REPO_PORT
    UC_IMPL -->|"3. Invoca Output Port"| EVENT_PORT
    UC_IMPL -->|"3. Invoca Output Port"| GATEWAY_PORT

    %% Adaptador de Persistência no application implementando a porta do domain
    REPO_ADAPTER -.->|"Implementa (DIP)"| REPO_PORT
    REPO_ADAPTER -->|"Usa para converter"| MAPPER
    MAPPER -->|"Converte para/de"| JPA_ENTITY
    REPO_ADAPTER -->|"Delega persistência para"| SPRING_REPO
    SPRING_REPO -->|"Persiste no banco"| JPA_ENTITY

    %% Outros adaptadores no application
    KAFKA_OUT -.->|"Implementa (DIP)"| EVENT_PORT
    EXT_CLIENT -.->|"Implementa (DIP)"| GATEWAY_PORT

    classDef c1 fill:#e1f5fe,stroke:#0288d1,stroke-width:3px,color:#01579b,font-size:26px;
    classDef c2 fill:#fff3e0,stroke:#f57c00,stroke-width:3px,color:#e65100,font-size:24px;
    classDef ports fill:#e8f5e9,stroke:#388e3c,stroke-width:3px,color:#1b5e20,font-size:24px;

    class DOMAIN_MODULE,ENTITIES,VO,EXCEPTIONS,UC_IMPL c1;
    class PORTS_IN,PORTS_OUT,IN_PORT,REPO_PORT,EVENT_PORT,GATEWAY_PORT ports;
    class APP_MODULE,ADAPTERS_IN,ADAPTERS_OUT,ADAPTERS_PERSISTENCE,ADAPTERS_OTHER_OUT,APP_CONFIG,REST,KAFKA_IN,REPO_ADAPTER,MAPPER,JPA_ENTITY,SPRING_REPO,KAFKA_OUT,EXT_CLIENT,SPRING_BOOT c2;
```

</div>

---

### 2. Responsabilidades dos Módulos Maven

#### 🟢 Módulo `domain` (Núcleo do Hexágono)
- **`domain.model`**: Entidades e Agregados em POJOs puros (sem `@Entity`, sem `@Table`). Encapsulam regras e transições de estado (ex: `invoice.markAsPaid()`).
- **`domain.port.in`**: Interfaces que definem as operações disponibilizadas aos clientes externos.
- **`domain.usecase`**: Implementação das regras de orquestração do caso de uso.
- **`domain.port.out`**: Interfaces SPI que definem o contrato de persistência (`InvoiceRepositoryPort`), mensageria e gateways externos.
- **Regra Fundamental**: Zero acoplamento a Spring, JPA, Hibernate ou banco de dados.

#### 🟠 Módulo `application` (Adaptadores & Entrega)
- **`adapter.in.*`**: Adaptadores condutores (Driving Adapters). Recebem requisições HTTP REST ou mensagens Kafka e invocam as portas de entrada do `domain`.
- **`adapter.out.persistence.*`**: Adaptador de persistência (Driven Adapter). Contém o modelo ORM (`*JpaEntity` ou `*MongoDocument`), o mapeador (`*Mapper`), a interface Spring Data (`JpaRepository`) e o adaptador (`*RepositoryAdapter`) que implementa a porta de saída do `domain`.
- **`adapter.out.kafka.*` / `adapter.out.client.*`**: Publicadores de eventos Kafka e clientes de integração HTTP (ex: Mercado Pago).
- **`config`**: Bootstrap da aplicação Spring Boot (`@SpringBootApplication`), injeção de dependências IoC, Virtual Threads e hints GraalVM AOT.

---

### 3. Como o JPA / Banco de Dados é Isolado na Estrutura de 2 Módulos

A separação nos 2 módulos Maven resolve o acoplamento do JPA de forma elegante e padronizada:

1. **Separação Física e Lógica de Modelos**:
   - No módulo **`domain`**: O agregado `Invoice` é um POJO puro de negócio (somente campos, regras de negócio e validações).
   - No módulo **`application`**: A entidade `InvoiceJpaEntity` (em `adapter.out.persistence.entity`) contém as anotações do Jakarta Persistence (`@Entity`, `@Table`, `@Version`).
2. **Fluxo de Persistência com Inversão de Dependência (DIP)**:
   - O Caso de Uso no `domain` invoca `InvoiceRepositoryPort.save(invoice)`.
   - O `InvoiceRepositoryAdapter` (no `application`) recebe o `Invoice` puro de domínio.
   - O `InvoiceMapper` traduz `Invoice` $\longrightarrow$ `InvoiceJpaEntity`.
   - O `InvoiceJpaRepository` (Spring Data) persiste a entidade no PostgreSQL.
   - O retorno é convertido de volta para o objeto puro `Invoice` do domínio.
3. **Isolamento no `pom.xml`**:
   - O `domain/pom.xml` não possui dependências de JPA, Hibernate ou Spring Data.
   - O `application/pom.xml` possui dependência de compilação sobre o `domain`, além das dependências de infraestrutura (`spring-boot-starter-data-jpa`, `postgresql`, `spring-kafka`, etc.).

---

### 4. Mapeamento com os Módulos do `lib-commons`

| Módulo do Commons | Módulo Maven de Destino | Papel no Ecossistema |
| :--- | :--- | :--- |
| **`mod-domain`** | **`domain`** | Value Objects (`Money`, `CpfOrCnpj`, `LicensePlate`), Exceções Canônicas e Validações de Domínio. |
| **`mod-web`** | **`application`** (`adapter.in.web`) | Handler global RFC 7807/9457 (`ProblemDetail`), Log MDC Interceptor (`traceId`/`spanId`) e DTOs base. |
| **`mod-kafka`** | **`application`** (`adapter.in/out.kafka`) | Envelope canônico de streaming (`EventEnvelope<T>`), propagação W3C TraceContext e serialização JSON. |
| **`mod-postgres`** | **`application`** (`adapter.out.persistence`) | Entidades base JPA (`BaseJpaEntity`), controle de concorrência otimista (`@Version`) e paginação padronizada. |
| **`mod-mongo`** | **`application`** (`adapter.out.persistence`) | Documentos base MongoDB (`BaseMongoDocument`), auditoria e serializadores UTC. |
| **`mod-aot`** | **`application`** (`config`) | Runtime hints e metadados de reflexão para compilação nativa GraalVM. |
| **`mod-test`** | **`domain`** / **`application`** (`src/test`) | Regras de governança arquitetural com ArchUnit. |
| **`mod-integration-test`** | **`application`** / **`iandt`** (`src/test`) | Fixtures e setups de Testcontainers (PostgreSQL, Kafka, Mongo, LocalStack) e WireMock. |

> 📚 **Fontes / Referências Canônicas**:
> - **Alistair Cockburn**: *"Hexagonal Architecture - Ports and Adapters Pattern"*, Alistair.Cockburn.us, 2005.
> - **Robert C. Martin (Uncle Bob)**: *"Clean Architecture: A Craftsman's Guide to Software Structure and Design"*, Pearson Education, 2017 (Capítulo 22: The Clean Architecture).
> - **Eric Evans**: *"Domain-Driven Design: Tackling Complexity in the Heart of Software"*, Addison-Wesley, 2003 (Capítulo 4: Isolating the Domain).


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
