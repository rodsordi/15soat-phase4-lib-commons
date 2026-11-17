# Plano de Implementação: Módulo de Testes de Integração (mod-integration-test) no Commons da Fase 4

> **Decisão do Usuário**: Opção 1 - Criar o submódulo dedicado mod-integration-test dentro do 15soat-phase4-lib-commons
> **Status**: Aprovado para Execução

---

## 1. Objetivo
Estruturar e implementar o submódulo `mod-integration-test` dentro da biblioteca compartilhada `15soat-phase4-lib-commons`, utilizando como referência direta o módulo `mod-integration-test` da Magalu (`C:\git\magalu\lib-commons-parent\mod-integration-test`) e os padrões de testes de integração com Testcontainers da Phase 3 (`iandt`).

---

## 2. Escopo de Implementação

### 2.1. Atualização do Parent POM (`15soat-phase4-lib-commons/pom.xml`)
- Declarar `<module>mod-integration-test</module>` na seção `<modules>`.
- Declarar as propriedades de versões canônicas:
  - `<testcontainers-bom.version>2.0.5</testcontainers-bom.version>` (ou alinhada com Spring Boot 4)
  - `<rest-assured.version>6.0.0</rest-assured.version>`
  - `<wiremock-spring-boot.version>4.2.0</wiremock-spring-boot.version>`
- Adicionar o `testcontainers-bom` e o artefato interno `mod-integration-test` ao `<dependencyManagement>`.

### 2.2. Criação do Submódulo `mod-integration-test`
- Criar a pasta `15soat-phase4-lib-commons/mod-integration-test`.
- Criar o `pom.xml` do módulo contendo as dependências de testes:
  - `spring-boot-starter-test`
  - `spring-boot-testcontainers`
  - `org.testcontainers:testcontainers-junit-jupiter`
  - `org.testcontainers:testcontainers-postgresql`
  - `org.testcontainers:testcontainers-kafka`
  - `org.testcontainers:testcontainers-mongodb`
  - `org.testcontainers:testcontainers-localstack`
  - `org.wiremock.integrations:wiremock-spring-boot`
  - `io.rest-assured:rest-assured` (e módulos auxiliares `json-path`, `xml-path`)
  - `org.projectlombok:lombok` (provided)

### 2.3. Implementação das Fixtures de Setup em Interfaces
Criar o pacote `br.com.fiap.phase4.commons.integration.setup` com as interfaces de herança múltipla para testes de integração no JUnit 5:
1. **`PostgresSetup.java`**:
   - Inicialização estática de `PostgreSQLContainer` (`postgres:17.6-alpine3.22`).
   - Mapeamento dinâmico `@DynamicPropertySource` para `spring.datasource.url`, `username` e `password`.
2. **`KafkaSetup.java`**:
   - Inicialização estática de `KafkaContainer` (`confluentinc/cp-kafka:7.6.0` ou imagem canônica compatível com ADR 0002).
   - Mapeamento dinâmico `@DynamicPropertySource` para `spring.kafka.bootstrap-servers`.
3. **`MongoSetup.java`**:
   - Inicialização estática de `MongoDBContainer` (`mongo:7.0`).
   - Mapeamento dinâmico `@DynamicPropertySource` para `spring.data.mongodb.uri`.
4. **`WiremockSetup.java`**:
   - Inicialização estática de `WireMockServer` (porta 9090) com limpeza de stubs `@AfterEach`.
5. **`LocalStackSetup.java`**:
   - Inicialização estática de `LocalStackContainer` (compatibilidade com serviços AWS da Phase 3).

### 2.4. Validação e Quality Gate
- Executar o build Maven (`mvn clean install -DskipTests`) no `15soat-phase4-lib-commons` para validar a compilação de todos os módulos.
- Validar conformidade de código estritamente em inglês.
