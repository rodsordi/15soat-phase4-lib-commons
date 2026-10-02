# ADR 0003: Concessão Arquitetural Pragmatic Domain-Driven Design (Fusão de JPA no Módulo Domain)

* **Status**: Aceito (Accepted)
* **Data**: 2026-10-02
* **Escopo**: Global (Todos os Microsserviços da Fase 4)
* **Autores**: FIAP SOAT Tech Challenge Team
* **Decisores Técnicos**: Especialistas em Engenharia de Software e Arquitetura Hexagonal

---

## 1. Contexto e Declaração do Problema

Em abordagens puristas de Arquitetura Hexagonal (Ports & Adapters) e Clean Architecture, o módulo `domain` deve ser 100% agnóstico a qualquer biblioteca de terceiros ou framework de persistência.

Sob esse padrão purista:
1. O módulo `domain` define objetos de domínio puros (ex: `Customer`, `WorkOrder`, `Vehicle`).
2. O módulo `application` é forçado a criar classes espelhadas de ORM (ex: `CustomerJpaEntity`, `WorkOrderJpaEntity`, `VehicleJpaEntity`).
3. Uma extensa camada de mapeadores manuais (ou MapStruct) precisa ser mantida para converter entidades de domínio em entidades JPA e vice-versa em toda operação de leitura e gravação.

### Sintomas do Padrão Purista no Projeto:
* **Duplicação Excessiva de Código**: As classes `*JpaEntity` possuíam exatamente os mesmos campos, tipos e relacionamentos que as classes de domínio.
* **Sobrecarga de Manutenção**: Qualquer novo atributo de negócio exigia alterar o Domain Model, o JPA Model, o Mapper de ida, o Mapper de volta e os testes de conversão.
* **Perda de Recursos Nativos do Hibernate**: Recursos avançados de persistência (como *lazy loading*, controle transacional atômico de coleções `@OneToMany` com *orphan removal* e *dirty checking*) tornavam-se complexos ou ineficazes com entidades desconectadas do ciclo de vida do EntityManager.

---

## 2. Decisão Arquitetural

Decidimos adotar uma **concessão arquitetural pragmática** (*Pragmatic Domain-Driven Design*):

1. **Fusão de Domain Objects com Anotações JPA**:
   - As anotações do padrão Jakarta Persistence (`@Entity`, `@Table`, `@Id`, `@Column`, `@ManyToOne`, `@OneToMany`, `@Version`) são aplicadas diretamente sobre os objetos de negócio no módulo `domain`.
   - As classes duplicadas `*JpaEntity` no módulo `application` são **completamente eliminadas**.

2. **Preservação do Isolamento Estrutural Restante**:
   - O módulo `domain` **NUNCA** depende de Spring Framework, Spring Web (`@RestController`) ou Spring Data (`JpaRepository`).
   - A dependência adicionada ao `domain` restringe-se estritamente à especificação canônica de anotações: `jakarta.persistence:jakarta.persistence-api`.

3. **Governança via ArchUnit**:
   - Os testes de arquitetura automatizados no módulo de testes foram calibrados para garantir que nenhuma classe de infraestrutura ou adaptador externo seja referenciada pelo domínio, autorizando apenas as anotações canônicas de persistência.

---

## 3. Consequências

### Positivas:
* **Eliminação de Código Boilerplate**: Redução imediata de centenas de linhas de código redundante entre entidades e mappers duplicados.
* **Ciclo de Vida Transacional Confiável**: Operações em agregados DDD com coleções filhas (como `WorkOrder` contendo `WorkOrderServiceItem` e `WorkOrderMaterialItem`) são persistidas e sincronizadas pelo Hibernate em uma única transação atômica.
* **Produtividade do Desenvolvedor**: Adicionar ou refatorar atributos de negócio requer alteração em um único arquivo de classe.

### Negativas / Trade-offs:
* **Acoplamento Tecnológico Parcial no Domínio**: As classes do domínio passam a carregar dependência de compilação da biblioteca de anotações do Jakarta Persistence. Caso haja uma migração hipotética para um banco não-relacional ou outro mecanismo de persistência, as anotações JPA precisariam ser revistas.

---

## 4. Alternativas Consideradas e Rejeitadas

| Alternativa | Veredito | Motivo da Rejeição |
| :--- | :--- | :--- |
| **Purismo Hexagonal Rigoroso (Domain Puro + *JpaEntity Duplicada)** | **Rejeitada** | Provocou ineficiência de desenvolvimento, bugs de mapeamento em coleções filhas e manutenção onerosa de código duplicado. |
| **Módulo Único de Aplicação e Domínio (Camada Única sem Maven Submodules)** | **Rejeitada** | Quebraria a modularização Maven e permitiria vazamento acidental de dependências HTTP/Web para as regras de negócio. |
