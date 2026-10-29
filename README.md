# Core J2EE Patterns: implementação dos 15 padrões

Laboratório executável, com código original e guia em português, baseado no catálogo da edição do **Core J2EE Patterns: Best Practices and Design Strategies** enviada em PDF. Essa edição contém **15 padrões**: seis de apresentação, sete de negócio e dois de integração.

O exemplo é uma aplicação de pedidos. Você cria um pedido com vários produtos, consulta uma lista paginada, abre os detalhes e acompanha o processamento assíncrono. Todos os padrões participam de fluxos executáveis; Service Locator e Dispatcher View têm rotas próprias para comparação.

**Comece pelo [guia de implementação dos 15 padrões](docs/guia-dos-padroes.md).** Ele explica o problema, os participantes, o código, a ordem das chamadas, os cuidados e as alternativas de cada padrão.

## Executar

Com Docker e Docker Compose:

```bash
git clone https://github.com/Leopoldobr/j2ee.git
cd j2ee
docker compose up --build
```

Abra **http://localhost:8080/j2ee-patterns/app/orders**. O primeiro build baixa dependências e o servidor leva alguns segundos para iniciar.

Sem Docker, use JDK 17, Maven 3.9+ e WildFly **35.0.1.Final**. As instruções completas, incluindo a configuração do datasource XA e dos testes em servidor, estão em [executar e testar](docs/executar-e-testar.md).

## O que está implementado

| # | Padrão do PDF | Implementação | Como observar |
|---|---|---|---|
| 1 | Intercepting Filter | [RequestFilter](src/main/java/br/com/leopoldobr/j2ee/presentation/RequestFilter.java) | Cabeçalho `X-Request-Id`, tempo de execução e validação CSRF |
| 2 | Front Controller | [FrontController](src/main/java/br/com/leopoldobr/j2ee/presentation/FrontController.java) | Todas as rotas `/app/*` |
| 3 | View Helper | [CatalogViewHelper](src/main/java/br/com/leopoldobr/j2ee/presentation/CatalogViewHelper.java), [FormatHelper](src/main/java/br/com/leopoldobr/j2ee/presentation/FormatHelper.java) | Catálogo e formatação das telas |
| 4 | Composite View | [Fragmentos JSP](src/main/webapp/WEB-INF/views/fragments) | Cabeçalho, rodapé e tabela de pedidos reutilizados |
| 5 | Service to Worker | [FrontController](src/main/java/br/com/leopoldobr/j2ee/presentation/FrontController.java) + [ViewDispatcher](src/main/java/br/com/leopoldobr/j2ee/presentation/ViewDispatcher.java) | `/app/orders`, `/app/new`, `/app/order?id=...` |
| 6 | Dispatcher View | [catalog.jsp](src/main/webapp/WEB-INF/views/catalog.jsp) + CatalogViewHelper | `/app/catalog` carrega os dados durante a renderização |
| 7 | Business Delegate | [OrderDelegate](src/main/java/br/com/leopoldobr/j2ee/presentation/OrderDelegate.java) | Apresentação chama fachada por um adaptador |
| 8 | Value Object | [DTOs](src/main/java/br/com/leopoldobr/j2ee/dto) | Dados retornados sem expor entidades JPA |
| 9 | Session Facade | [OrderFacadeBean](src/main/java/br/com/leopoldobr/j2ee/business/OrderFacadeBean.java) | Criação do pedido dentro de uma transação JTA |
| 10 | Composite Entity | [Order](src/main/java/br/com/leopoldobr/j2ee/domain/Order.java) + [OrderLine](src/main/java/br/com/leopoldobr/j2ee/domain/OrderLine.java) | Pedido controla itens, quantidades e ciclo de vida |
| 11 | Value Object Assembler | [OrderAssembler](src/main/java/br/com/leopoldobr/j2ee/business/OrderAssembler.java) | Detalhes unem dados do pedido e do catálogo |
| 12 | Value List Handler | [ValueListHandler](src/main/java/br/com/leopoldobr/j2ee/business/ValueListHandler.java) + [OrderSearchModel](src/main/java/br/com/leopoldobr/j2ee/presentation/OrderSearchModel.java) | Cache de consulta na sessão, próximo/anterior, até 500 resultados |
| 13 | Service Locator | [ServiceLocator](src/main/java/br/com/leopoldobr/j2ee/integration/ServiceLocator.java) + [LegacyOrderDelegate](src/main/java/br/com/leopoldobr/j2ee/presentation/LegacyOrderDelegate.java) | `/app/locator` resolve a fachada com JNDI |
| 14 | Data Access Object | [JpaOrderDao](src/main/java/br/com/leopoldobr/j2ee/integration/JpaOrderDao.java), [JdbcProductDao](src/main/java/br/com/leopoldobr/j2ee/integration/JdbcProductDao.java) | Persistência JPA do agregado e consultas JDBC parametrizadas |
| 15 | Service Activator | [OrderServiceActivator](src/main/java/br/com/leopoldobr/j2ee/integration/OrderServiceActivator.java) | MDB consome fila JMS e chama o serviço de negócio |

## Um exemplo central: Session Facade

Trecho de `OrderFacadeBean.create`:

```java
orders.save(order);
publisher.publish(order.getId());
return assembler.details(order);
```

A classe é um EJB `@Stateless` com `@TransactionAttribute(REQUIRED)`. O contêiner inicia a transação, o DAO persiste o pedido e seus itens, e o publisher envia o evento JMS na mesma transação. O datasource H2 está configurado como **XA**, assim como a conexão JMS gerenciada. O consumidor recebe a mensagem depois do commit e usa outra transação para processar o pedido.

O [guia](docs/guia-dos-padroes.md) mostra a implementação completa em contexto e os limites dessa garantia, incluindo a distinção entre idempotência do consumidor e duplicação de submissões HTTP.

## Tecnologias e adaptação do livro

| Livro: J2EE | Laboratório: Java 17 + Jakarta EE 10 |
|---|---|
| Servlets / JSP | Servlet 6.0 / Pages 3.1 / Tags 3.0 |
| EJB Session Beans | Enterprise Beans 4.0 com anotações e interfaces locais |
| Entity Beans / Composite Entity | Persistence 3.1: agregado JPA com raiz e dependentes |
| JMS | Messaging 3.1, fila e Message-Driven Bean |
| JTA | Transactions 2.0, transação gerenciada e recursos XA |
| JNDI e JDBC | APIs do Java SE: `javax.naming` e `javax.sql` permanecem com esses nomes |
| Value Object | DTO; o termo do livro tem sentido diferente do Value Object de DDD |

Esta é uma **adaptação dos padrões**, não um projeto binariamente compatível com J2EE 1.x. Não é necessário implementar `EJBHome`, `EJBObject`, Entity Beans EJB 2.x nem usar `javax.servlet` para aprender as responsabilidades arquiteturais. As diferenças estão detalhadas em [arquitetura e decisões](docs/arquitetura.md).

## Estrutura

```text
src/main/java/br/com/leopoldobr/j2ee/
  presentation/  Servlet, filtro, delegates e helpers
  business/      Fachada, assembler, serviço e handler de listas
  domain/        Pedido, itens, produto e invariantes
  dto/           Objetos de transferência
  integration/   DAOs, JNDI, publisher e MDB
src/main/webapp/WEB-INF/views/  JSPs e fragmentos
src/test/                     Testes unitários
scripts/                      Testes HTTP em servidor real
infra/                        Configuração XA do WildFly
docs/                         Guia, execução, arquitetura e exercícios
```

## Testar e estudar

```bash
mvn clean verify
# Opcional, com WildFly extraído e as portas 8080/9990 livres:
export WILDFLY_HOME=/caminho/wildfly-35.0.1.Final
bash scripts/integration.sh
```

Os testes unitários verificam invariantes do agregado, precisão monetária, limites de paginação, composição dos DTOs e interpretação de formulários. O roteiro em servidor verifica JSP/EL, CDI/EJB, JDBC/JPA, fila JMS, mensagens duplicadas concorrentes, JNDI, validações e navegação. A [CI](.github/workflows/ci.yml) executa ambos.

Siga também os [exercícios guiados](docs/exercicios.md) e consulte os [resultados de validação](docs/validacao.md).

## Escopo do laboratório

O banco é recriado **a cada deploy** por `drop-and-create`. São usados dados de demonstração, sem autenticação, autorização por usuário, pagamento ou integração externa. O Compose publica somente a porta HTTP em `127.0.0.1`. Antes de evoluir para uma aplicação real, veja as decisões e limites descritos na documentação.

Os exemplos e as explicações deste repositório foram escritos para o laboratório. O PDF e os exemplos originais do livro não são redistribuídos. A referência conceitual é *Core J2EE Patterns: Best Practices and Design Strategies*, de Deepak Alur, John Crupi e Dan Malks, capítulos 7, 8 e 9 da edição fornecida.
