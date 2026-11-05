# ADR 0007: Padronização de DTOs com Nested Records, Contratos de Interface, HATEOAS e Paginação

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-06
* **Escopo**: Global (Todos os Microsserviços e Bibliotecas da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Arquitetura de Software e Engenharia de Plataforma

---

## 1. Contexto e Declaração do Problema

Durante a evolução da plataforma (notadamente na Fase 3 e em projetos legados mapeados em `c:/git`), a modelagem e o transporte de dados através de DTOs (*Data Transfer Objects*) apresentavam variações estruturais e alguns acoplamentos que demandam padronização definitiva:

1. **Proliferação Desordenada de Arquivos**: Múltiplos DTOs de uma mesma entidade (`CustomerRequestDto`, `CustomerResponseDto`, `CustomerPatchRequestDto`, `CustomerDetailDto`) dispersos na raiz de pacotes, dificultando a navegação e a coesão semântica.
2. **Duplicação de Contratos e Anotações**: Redundância na declaração de constraints de validação (`Jakarta Bean Validation`) e anotações Swagger/OpenAPI entre requisições de criação, atualização e respostas.
3. **Uso de Classes Mutáveis vs. Records**: Implementações legadas com classes POJO convencionais sustentadas por `@Getter`, `@Setter` e construtores complexos, sujeitas a efeitos colaterais de mutabilidade em tempo de execução.
4. **Vício de Comportamento em DTOs (DTOs Não-Anêmicos)**: Na Fase 3, observou-se a introdução de métodos de conversão direta dentro dos DTOs que invocavam instâncias estáticas de `MapStruct` (ex: `public Customer buildCustomer() { return MAPPER.convert(this); }`), violando o Princípio de Responsabilidade Única (SRP) e o papel estrito do DTO como transportador de dados.
5. **Inconsistência em Hipermídia (HATEOAS) e Paginação**: Ausência de diretriz uniforme sobre como enriquecer payloads com links hipermídia (especialmente com a imutabilidade inerente aos `record`s do Java) e padronizar envelopes de paginação.

---

## 2. Decisão Arquitetural

Decidimos padronizar a modelagem de DTOs em **todos os microsserviços e bibliotecas compartilhadas da Fase 4** seguindo os pilares abaixo:

### 2.1. Estrutura de Classes Aninhadas (*Outer Container Class*)
Cada entidade ou agregado terá uma única classe externa utilitária imutável marcada como `final`, com construtor privado gerado pelo Lombok (`@NoArgsConstructor(access = PRIVATE)`), atuando como namespace delimitador para todas as variações de payload daquele recurso:
* `EntityDto.Request`: Criação ou substituição integral.
* `EntityDto.Response`: Resposta canônica detalhada.
* `EntityDto.Representation`: Resposta resumida ou enriquecida com hipermídia.
* `EntityDto.PatchRequest`: Atualização parcial.
* `EntityDto.Filter`: Parâmetros de consulta e filtros.

### 2.2. Contratos Abstratos de Interface (`*Def`)
Para garantir reuso, integridade de assinatura e segregação de interfaces (ISP - *Interface Segregation Principle*), os atributos, validações (`jakarta.validation.constraints.*`) e documentação Swagger são declarados em interfaces de definição canônica (`EntityDef`):
* As interfaces definem métodos acessores (estilo record: `String name()`, `UUID id()`).
* Suportam herança hierárquica limpa:
  - `Represented`: Atributos fundamentais públicos compartilhados.
  - `Detailed extends Represented`: Atributos completos do recurso.
  - `RepresentedPersisted`: Metadados de persistência (`id`, `createdAt`, `updatedAt`).
  - `DetailedPersisted extends RepresentedPersisted`: Metadados completos de persistência.
  - `Request extends Detailed`: Contrato para entrada.
  - `Response extends Detailed, DetailedPersisted`: Contrato para saída.

### 2.3. Adoção Estrita de Java Records
Todas as classes aninhadas de DTOs concretos devem ser implementadas como `record`s Java (baseline Java 21 / Java 25):
* **Imutabilidade Garantida**: Estado imutável por definição (atributos `private final` gerados pelo compilador).
* **Compacidade**: Redução drástica de boilerplate sem perda de legibilidade.
* **Conformidade de Métodos**: Métodos canônicos `equals()`, `hashCode()`, `toString()` e acessores gerados de forma otimizada.
* Os records implementam as interfaces contratuais correspondentes (ex: `public record Request(...) implements EntityDef.Request`).

### 2.4. Suporte ao Lombok
* **Na Outer Class**: Uso de `@NoArgsConstructor(access = PRIVATE)` para impedir instanciação da classe delimitadora.
* **Nos Records Aninhados**: Uso de `@Builder` nos records para possibilitar instanciação fluente e expressiva em testes unitários, factories e mappers.

### 2.5. Ausência Estrita de Comportamento (Pure Anemic DTOs)
* Os DTOs **NÃO DEVEM CONTER MÉTODOS DE NEGÓCIO OU MAPEAMENTO**.
* É terminantemente proibido referenciar `Mappers` do MapStruct ou métodos como `buildEntity()` dentro dos records.
* A responsabilidade de conversão entre DTOs e entidades de domínio/persistência pertence **exclusivamente aos mappers dedicados** (ex: `CustomerMapper`) injetados nos Use Cases, Adaptadores ou Controllers.

### 2.6. Suporte a HATEOAS em Records
Como records em Java não podem herdar de classes base como `RepresentationModel<T>` do Spring HATEOAS, a hipermídia RESTful (Nível 3 de Richardson) deve ser implementada através de:
1. **Composição via `EntityModel<T>`**: Emissão de `EntityModel<EntityDto.Response>` onde os links de hipermídia são adicionados no controller/assembler via `WebMvcLinkBuilder.linkTo(...)`.
2. **Atributo `Links` no Record**: Quando exigido no contrato do payload, incluir um campo `Links links` opcional no record de representação.

### 2.7. Suporte a Paginação Padronizada
* **Entrada**: Paginação e ordenação recebidas via Spring Data `Pageable` ou DTOs aninhados de filtro (`EntityDto.Filter`).
* **Saída**: Para coleções paginadas enriquecidas com hipermídia, utilizar `PagedModel<EntityModel<EntityDto.Response>>` do Spring HATEOAS, ou o envelope de paginação padronizado do `mod-web` (`GenericPageResponseDto<T>`).

### 2.8. Definições Swagger/OpenAPI (Cenários Code-First)
* Nos microsserviços onde não se utiliza a geração automática a partir de especificações OpenAPI YAML (*Contract-First*), todas as anotações do Swagger/OpenAPI 3 (`@Schema`, `@ArraySchema`, `description`, `example`, `requiredMode`) devem ser declaradas nas interfaces de contrato (`*Def`) ou nos componentes do record.
* Isso garante documentação precisa e viva gerada via SpringDoc OpenAPI.

---

## 3. Especificação Canônica de Implementação

### 3.1. Interface de Contrato (`CustomerDef.java`)

```java
package br.com.fiap.phase4.workorder.application.v1.def;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CustomerDef {

    interface Represented {
        @Schema(description = "Customer full name", example = "John Doe")
        @NotBlank(message = "Customer name is required")
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name();

        @Schema(description = "Customer email address", example = "john.doe@example.com")
        @NotBlank(message = "Customer email is required")
        @Email(message = "Customer email format is invalid")
        @Size(max = 255, message = "Customer email must not exceed 255 characters")
        String email();

        @Schema(description = "Customer national document (CPF/CNPJ)", example = "77790794000169")
        @NotBlank(message = "Customer document is required")
        @Size(min = 11, max = 18, message = "Customer document must be between 11 and 18 characters")
        String document();
    }

    interface Detailed extends Represented {
    }

    interface RepresentedPersisted {
        @Schema(description = "Unique customer identifier", example = "1029deef-ed87-46c3-b345-a13b30168659")
        @NotNull(message = "Customer id is required")
        UUID id();

        @Schema(description = "Record creation timestamp", example = "2026-10-06T19:30:00")
        LocalDateTime createdAt();
    }

    interface DetailedPersisted extends RepresentedPersisted {
        @Schema(description = "Record last update timestamp", example = "2026-10-06T19:35:00")
        LocalDateTime updatedAt();
    }

    interface Request extends Detailed {
    }

    interface Response extends Detailed, DetailedPersisted {
    }

    interface PatchRequest {
        @Schema(description = "Customer full name", example = "John Doe")
        @Size(max = 255, message = "Customer name must not exceed 255 characters")
        String name();

        @Schema(description = "Customer email address", example = "john.doe@example.com")
        @Email(message = "Customer email format is invalid")
        @Size(max = 255, message = "Customer email must not exceed 255 characters")
        String email();
    }
}
```

### 3.2. Container DTO com Nested Records (`CustomerDto.java`)

```java
package br.com.fiap.phase4.workorder.application.v1.dto;

import br.com.fiap.phase4.workorder.application.v1.def.CustomerDef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
@Schema(description = "Namespace containing Customer Data Transfer Objects")
public final class CustomerDto {

    @Builder
    @Schema(name = "Customer.Request", description = "Payload for customer creation")
    public record Request(
            @Schema(description = "Customer full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Customer name is required")
            @Size(max = 255, message = "Customer name must not exceed 255 characters")
            String name,

            @Schema(description = "Customer email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Customer email is required")
            @Email(message = "Customer email format is invalid")
            @Size(max = 255, message = "Customer email must not exceed 255 characters")
            String email,

            @Schema(description = "Customer national document (CPF/CNPJ)", example = "77790794000169", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "Customer document is required")
            @Size(min = 11, max = 18, message = "Customer document must be between 11 and 18 characters")
            String document
    ) implements CustomerDef.Request {
    }

    @Builder
    @Schema(name = "Customer.Response", description = "Standard customer representation payload")
    public record Response(
            @Schema(description = "Unique customer identifier", example = "1029deef-ed87-46c3-b345-a13b30168659")
            UUID id,

            @Schema(description = "Customer full name", example = "John Doe")
            String name,

            @Schema(description = "Customer email address", example = "john.doe@example.com")
            String email,

            @Schema(description = "Customer national document", example = "77790794000169")
            String document,

            @Schema(description = "Record creation timestamp", example = "2026-10-06T19:30:00")
            LocalDateTime createdAt,

            @Schema(description = "Record last update timestamp", example = "2026-10-06T19:35:00")
            LocalDateTime updatedAt
    ) implements CustomerDef.Response {
    }

    @Builder
    @Schema(name = "Customer.PatchRequest", description = "Payload for partial customer update")
    public record PatchRequest(
            @Schema(description = "Customer full name", example = "John Doe")
            @Size(max = 255, message = "Customer name must not exceed 255 characters")
            String name,

            @Schema(description = "Customer email address", example = "john.doe@example.com")
            @Email(message = "Customer email format is invalid")
            @Size(max = 255, message = "Customer email must not exceed 255 characters")
            String email
    ) implements CustomerDef.PatchRequest {
    }

    @Builder
    @Schema(name = "Customer.Filter", description = "Query filter criteria for customers")
    public record Filter(
            @Schema(description = "Name search query pattern", example = "John")
            String name,

            @Schema(description = "Document search filter", example = "77790794000169")
            String document
    ) {
    }
}
```

### 3.3. Uso Idiomático com Spring HATEOAS e Paginação

```java
package br.com.fiap.phase4.workorder.application.v1.assembler;

import br.com.fiap.phase4.workorder.application.v1.controller.CustomerController;
import br.com.fiap.phase4.workorder.application.v1.dto.CustomerDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class CustomerModelAssembler implements RepresentationModelAssembler<CustomerDto.Response, EntityModel<CustomerDto.Response>> {

    @Override
    public EntityModel<CustomerDto.Response> toModel(CustomerDto.Response customer) {
        return EntityModel.of(customer,
                linkTo(methodOn(CustomerController.class).findById(customer.id())).withSelfRel(),
                linkTo(methodOn(CustomerController.class).findAll(null, null)).withRel("customers"));
    }
}
```

Na camada de Controller com paginação:

```java
@GetMapping
public ResponseEntity<PagedModel<EntityModel<CustomerDto.Response>>> findAll(
        CustomerDto.Filter filter,
        @PageableDefault(size = 20, sort = "name") Pageable pageable,
        PagedResourcesAssembler<CustomerDto.Response> pagedAssembler) {

    Page<CustomerDto.Response> page = customerService.findAll(filter, pageable);
    PagedModel<EntityModel<CustomerDto.Response>> pagedModel = pagedAssembler.toModel(page, customerModelAssembler);

    return ResponseEntity.ok(pagedModel);
}
```

---

## 4. Consequências

### Positivas:
* **Alta Coesão e Navegabilidade**: Elimina dezenas de arquivos isolados por agregados; todas as formas de transporte de uma entidade coexistem na mesma outer class `EntityDto`.
* **Imutabilidade e Segurança Concorrente**: Java Records garantem que dados trafegados entre camadas não sejam acidentalmente mutados.
* **Pureza Arquitetural**: DTOs tornam-se 100% anêmicos, eliminando o acoplamento espúrio com MapStruct e regras de domínio no payload.
* **Interoperabilidade Total com HATEOAS e Paginação**: O uso de `EntityModel<T>` do Spring HATEOAS viabiliza hiperlinks sem necessidade de herança de classes no record.
* **Documentação Viva e Consistente**: Suporte a Swagger/OpenAPI explícito nas interfaces `*Def` e records quando em modelo Code-First.

### Negativas / Trade-offs:
* **Incompatibilidade de Herança Direta de Classes**: Como Records não estendem classes abstratas, não é possível estender `RepresentationModel` diretamente no record; a hipermídia exige composição com `EntityModel<T>`.
* **Verbosidade em Declaração de Schema Code-First**: Declarações manuais de `@Schema` e validações exigem rigor no alinhamento entre as interfaces e os records quando não gerados via OpenAPI Generator.

---

## 5. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Arquivos de Classes Separadas (POJOs soltos)** | **Rejeitada** | Causa poluição visual e fragmentação no projeto, dispersando `CreateCustomerRequest`, `UpdateCustomerRequest`, etc., em múltiplos arquivos de manutenção difícil. |
| **POJOs Mutáveis com `@Data` / `@Setter`** | **Rejeitada** | Permite mutação indevida de dados após deserialização, dificultando auditoria e quebrando a integridade de requisições concorrentes. |
| **DTOs com Métodos de Negócio e Conversão (`buildEntity()`)** | **Rejeitada** | Padrão legado observado na Fase 3 que acoplava o DTO a frameworks de mapeamento e violava a responsabilidade exclusiva de transporte de dados. |
| **Herança de Classes Tradicionais para HATEOAS** | **Rejeitada** | Impede o aproveitamento de `record`s modernos do Java e força a criação de classes abstratas verbosas. |
