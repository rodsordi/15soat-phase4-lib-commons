# ADR 0005: Adoção do Java 25 e Spring Boot 4 com Suporte a GraalVM Native Image

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Global (Todos os Microsserviços e Bibliotecas da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Engenharia de Plataforma e JVM Modernization

---

## 1. Contexto e Declaração do Problema

A arquitetura de microsserviços em contêineres e Kubernetes (AWS EKS) impõe requisitos rigorosos de inicialização rápida (*fast startup*), baixo consumo de memória (*footprint*) e escalabilidade horizontal elástica via Horizontal Pod Autoscaler (HPA).

Versões legadas de Java e Spring Boot apresentam:
* Tempos de inicialização de 10 a 20 segundos por pod, retardando respostas do HPA em picos de tráfego.
* Consumo elevado de memória em repouso (200MB a 400MB por réplica mínima).
* Recursos modernos de concorrência e sintaxe (Virtual Threads maduras, Pattern Matching completo, Records aprimorados) subutilizados.

---

## 2. Decisão Arquitetural

Decidimos padronizar o ecossistema tecnológico de **todos os microsserviços e bibliotecas compartilhadas da Fase 4** no seguinte baseline de ponta:

1. **Java 25 (OpenJDK / Eclipse Temurin 25)**:
   - Utilização de Java 25 como compilador padrão em todos os `pom.xml` (`<java.version>25</java.version>`).
   - Adoção intensiva de recursos modernos da linguagem (Records, Sealed Types, Pattern Matching, Stream Gatherers).

2. **Spring Boot 4.0.x**:
   - `spring-boot-starter-parent: 4.0.7` como baseline unificado em todos os serviços executáveis.
   - Padrão Jakarta EE 11 com especificações atualizadas.

3. **Módulo Compartilhado de AOT e GraalVM (`mod-aot`)**:
   - Implementação de dicas nativas de compilação (*Runtime Hints*) puramente em código Java (`RuntimeHintsRegistrar`), eliminando a fragilidade de arquivos JSON manuais (`reflect-config.json`).
   - Capacidade de gerar executáveis nativos com tempo de boot inferior a 100ms e consumo de memória inferior a 40MB por pod.

4. **Padronização em Contêineres e CI/CD**:
   - Imagens base nos Dockerfiles padronizadas em `eclipse-temurin:25-jre-noble`.
   - Pipelines GitHub Actions (`.github/workflows/pipeline.yml`) executando compilação e testes estritamente em `java-version: '25'`.

---

## 3. Consequências

### Positivas:
* **Desempenho Extremo**: Utilização de Garbage Collectors modernos e otimizações JIT/AOT do Java 25.
* **Escala Elástica Real no Kubernetes**: Réplicas adicionadas pelo HPA ficam prontas para tráfego instantaneamente.
* **Modernidade e Longevidade**: Arquitetura livre de débito técnico por muitos anos.

### Negativas / Trade-offs:
* **Exigência de Toolchain Atualizado**: Desenvolvedores e agentes de CI precisam ter o JDK 25 e Maven 3.9+ devidamente instalados na máquina/ambiente.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Java 17 LTS / Java 21 LTS** | **Rejeitada** | Superadas pelo avanço tecnológico do projeto e diretriz mandatória de adoção da versão mais recente da JVM. |
| **Spring Boot 3.x** | **Rejeitada** | Versão que logo entrará em ciclo de manutenção legado perante a consolidação do Spring Boot 4.x. |
