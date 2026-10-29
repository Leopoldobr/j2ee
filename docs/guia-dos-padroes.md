# Guia de implementação dos 15 padrões

Este guia segue o catálogo do **PDF fornecido**, com os nomes usados nessa edição. Os trechos abaixo pertencem à implementação do repositório ou são recortes dela. Os links levam aos arquivos completos. Não é necessário aplicar todos os padrões a todo sistema: eles aparecem juntos aqui para permitir estudo e comparação.

O domínio é simples: um **pedido** possui um cliente e itens; os produtos têm preço no catálogo; o pedido captura o preço na compra e é processado por uma fila. Leia primeiro os padrões 8, 10, 14 e 9 para acompanhar o núcleo; depois examine os padrões de apresentação e a mensageria.

## 1. Intercepting Filter

**Problema:** codificar identificação de requisição, configuração de charset e verificações de entrada em cada servlet gera duplicação.

**Implementação:** [RequestFilter](../src/main/java/br/com/leopoldobr/j2ee/presentation/RequestFilter.java), em `/app/*`.

```java
long start = System.nanoTime();
try {
    chain.doFilter(http, reply);
} finally {
    http.getServletContext().log("request=" + requestId
        + " status=" + reply.getStatus()
        + " durationMs=" + (System.nanoTime() - start) / 1_000_000);
}
```

O trecho fica dentro de `doFilter`. O pré-processamento define UTF-8, cria um ID e verifica o token CSRF em POST. `chain.doFilter` continua a cadeia; o `finally` registra o resultado mesmo quando há exceção. Se o token falha, o filtro devolve 403 e não chama o controlador.

O token é aleatório, armazenado na sessão e incluído no formulário. A comparação usa `MessageDigest.isEqual`. O acesso inicial ao token é sincronizado para evitar sua criação concorrente na mesma sessão.

**Detalhes:** não ler preços nem acessar o banco no filtro; essas são responsabilidades do caso de uso. Não usar `ThreadLocal` como depósito de objetos de sessão sem limpeza. Para ordenar vários filtros de modo determinístico, declare a ordem em `web.xml`; não dependa da ordem de descoberta de anotações. Este laboratório usa um filtro para as políticas mostradas.

**Experimento:** abra `/app/new` e observe `X-Request-Id`; envie um POST sem o token e confira o 403. Há cenários equivalentes em [smoke.py](../scripts/smoke.py).

## 2. Front Controller

**Problema:** navegação, interpretação de parâmetros e tratamento de erros espalhados em JSPs e servlets.

**Implementação:** [FrontController](../src/main/java/br/com/leopoldobr/j2ee/presentation/FrontController.java).

```java
@WebServlet("/app/*")
public class FrontController extends HttpServlet {
    @Inject private OrderDelegate delegate;
    @Inject private ViewDispatcher dispatcher;
    // doGet/doPost interpretam a rota e delegam o trabalho.
}
```

Uma única entrada recebe `/app/orders`, `/app/new`, `/app/order`, `/app/catalog` e `/app/locator`. O controlador interpreta IDs e quantidades, chama um delegate e escolhe uma view. O [ViewDispatcher](../src/main/java/br/com/leopoldobr/j2ee/presentation/ViewDispatcher.java) transforma um enum em um caminho fixo dentro de `WEB-INF`.

Na criação, o controlador monta `CreateOrderCommand`, recebe `OrderDetailsDto` e responde **303 See Other** para a URL dos detalhes. Esse Post/Redirect/Get evita reenvio acidental ao atualizar a página. Ele não garante idempotência se o cliente repetir dois POSTs independentes.

**Detalhes:** parâmetros inválidos geram 400; pedido ausente gera 404; indisponibilidade EJB gera 503 com uma mensagem pública curta. A exceção técnica completa vai para o log, correlacionada com o ID do filtro. Não encaminhe diretamente um parâmetro como `next=qualquer.jsp`: mantenha uma lista de views permitidas.

**Quando simplificar:** em uma aplicação maior, extraia handlers/comandos por caso de uso ou use o controlador de um framework MVC. O padrão é a centralização da coordenação HTTP, não a obrigação de escrever um `switch` gigante.

## 3. View Helper

**Problema:** misturar acesso a serviços, preparação de dados e formatação em scriptlets JSP dificulta teste e reutilização.

**Implementação:** [CatalogViewHelper](../src/main/java/br/com/leopoldobr/j2ee/presentation/CatalogViewHelper.java) e [FormatHelper](../src/main/java/br/com/leopoldobr/j2ee/presentation/FormatHelper.java).

```java
@Named("catalogView") @RequestScoped
public class CatalogViewHelper {
    @Inject private OrderDelegate delegate;
    private List<ProductDto> products;

    public List<ProductDto> getProducts() {
        if (products == null) products = delegate.products();
        return products;
    }
}
```

```jsp
<c:forEach items="${catalogView.products}" var="product">
    <c:out value="${product.name}"/>
    <c:out value="${format.money(product.price)}"/>
</c:forEach>
```

O helper tem escopo de requisição e evita que avaliações repetidas de EL executem repetidas consultas. `FormatHelper` apresenta `BigDecimal` em reais e `Instant` no fuso `America/Sao_Paulo`. As entidades não são acessadas pelas JSPs.

**Detalhes:** o helper do catálogo obtém conteúdo, além da formatação oferecida pelo outro helper. Ele chama o delegate, nunca o DAO. Não coloque regras como desconto ou elegibilidade de compra dentro de um formatador. A JSP usa `c:out` para escapar conteúdo textual; escape de HTML não é um escape universal para JavaScript, CSS ou URLs.

**Experimento:** altere apenas a apresentação monetária em `FormatHelper`; o cálculo e a persistência do preço não devem mudar.

## 4. Composite View

**Problema:** cabeçalho, navegação, rodapé e tabelas repetidos em várias telas.

**Implementação:** [fragments](../src/main/webapp/WEB-INF/views/fragments), reutilizados por [orders.jsp](../src/main/webapp/WEB-INF/views/orders.jsp) e [locator.jsp](../src/main/webapp/WEB-INF/views/locator.jsp).

```jsp
<%@ include file="fragments/header.jspf" %>
<c:set var="tableOrders" value="${ordersPage.items}" scope="request"/>
<%@ include file="fragments/order-table.jspf" %>
<%@ include file="fragments/footer.jspf" %>
```

A página é composta por partes menores. Aqui, a diretiva `include` faz composição **na tradução da JSP**, apropriada a fragmentos estáticos do layout. `jsp:include` faria inclusão durante a requisição e pode ser usado para componentes dinâmicos; é outra estratégia, com custos e escopos diferentes.

**Detalhes:** defina um contrato pequeno para cada fragmento: `order-table.jspf` recebe `tableOrders`. Ele não consulta banco nem decide regras do negócio. Evite dependências implícitas entre fragmentos e nomes genéricos de atributos que colidem.

**Experimento:** altere a navegação no cabeçalho e veja a mudança em todas as páginas.

## 5. Service to Worker

**Problema:** preparar conteúdo e decidir navegação durante a renderização cria dependências difíceis de controlar em fluxos complexos.

**Implementação:** rota `/app/orders` no [FrontController](../src/main/java/br/com/leopoldobr/j2ee/presentation/FrontController.java), [OrderSearchModel](../src/main/java/br/com/leopoldobr/j2ee/presentation/OrderSearchModel.java) e [ViewDispatcher](../src/main/java/br/com/leopoldobr/j2ee/presentation/ViewDispatcher.java).

```java
request.setAttribute("ordersPage", search.page(
    "true".equals(request.getParameter("refresh")), page));
dispatcher.show(ViewDispatcher.View.ORDERS, request, response);
```

O controlador interpreta a intenção, o helper/modelo prepara o conteúdo, e o dispatcher só então encaminha para a view. A JSP de pedidos recebe uma página pronta. Ela ainda chama o formatador, mas não dispara a busca de pedidos.

**Colaboração:** Front Controller + dispatcher + helpers + view. É um **macropadrão**, uma combinação de outros padrões. Não é uma nova camada obrigatória nem um serviço de fila; o nome não se refere a workers assíncronos.

**Detalhes:** trate erros de recuperação antes de enviar a resposta. Uma aplicação com decisão de view dependente de dados, formulários validados e múltiplos caminhos de navegação costuma se beneficiar dessa ordem.

**Experimento:** coloque um breakpoint em `search.page` e na JSP; a consulta acontece antes do forward.

## 6. Dispatcher View

**Problema:** em navegação simples, o controlador pode apenas selecionar a view, deixando a obtenção do conteúdo para helpers usados por ela.

**Implementação:** rota `/app/catalog` e [catalog.jsp](../src/main/webapp/WEB-INF/views/catalog.jsp).

```java
case "/catalog" ->
    dispatcher.show(ViewDispatcher.View.CATALOG, request, response);
```

```jsp
<c:forEach items="${catalogView.products}" var="product">
    <!-- apresentação dos DTOs -->
</c:forEach>
```

O controlador não chama `delegate.products()` nessa rota. A avaliação de `catalogView.products` durante a renderização invoca o helper, que recupera e guarda os dados. Isso torna a diferença em relação a Service to Worker observável.

**Detalhes:** a recuperação tardia pode dificultar a troca de status/view quando ocorre falha após o envio de parte da resposta. Um helper não deve produzir efeitos de gravação ao ser lido pela JSP. Evite consultas em um loop de itens, que gerariam N+1 chamadas.

**Escolha:** use o exemplo para entender a combinação descrita pelo livro; em formulários e fluxos com tratamento de erros importante, prepare os dados antes do forward.

## 7. Business Delegate

**Problema:** a apresentação fica acoplada ao mecanismo de acesso ao serviço e às exceções técnicas EJB.

**Implementação:** [OrderDelegate](../src/main/java/br/com/leopoldobr/j2ee/presentation/OrderDelegate.java).

```java
@RequestScoped
public class OrderDelegate {
    @EJB private OrderFacade facade;

    public OrderDetailsDto create(CreateOrderCommand command) {
        return invoke(() -> facade.create(command));
    }
    private <T> T invoke(Supplier<T> call) {
        try { return call.get(); }
        catch (EJBException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }
}
```

O controlador conhece o delegate e DTOs. O delegate conhece a interface de negócio e traduz falhas de infraestrutura. As exceções de negócio marcadas como application exceptions passam intactas, permitindo respostas específicas de validação.

O livro discute muito o desacoplamento de clientes de serviços remotos. Aqui usamos EJB **local** em um WAR para simplificar a execução. A responsabilidade de adaptação aparece, mas não há rede entre delegate e fachada. Para um cliente remoto real, seria necessário fornecer interface remota, configuração e transporte apropriados.

**Detalhes:** não implemente regras de pedido no delegate. Não repita `create` automaticamente depois de uma falha de comunicação: o servidor pode já ter confirmado a primeira chamada. Em um sistema pequeno com injeção e mesma camada de execução, uma classe que só encaminha métodos pode ser dispensada.

**Experimento:** compare o delegate principal, injetado, com [LegacyOrderDelegate](../src/main/java/br/com/leopoldobr/j2ee/presentation/LegacyOrderDelegate.java), que resolve o serviço por JNDI.

## 8. Value Object

**Problema:** chamar getters remotos repetidamente e enviar entidades persistentes para a apresentação aumenta acoplamento, tráfego e problemas com lazy loading.

**Implementação:** [OrderDetailsDto](../src/main/java/br/com/leopoldobr/j2ee/dto/OrderDetailsDto.java), [OrderLineDto](../src/main/java/br/com/leopoldobr/j2ee/dto/OrderLineDto.java), [OrderSummaryDto](../src/main/java/br/com/leopoldobr/j2ee/dto/OrderSummaryDto.java) e [ProductDto](../src/main/java/br/com/leopoldobr/j2ee/dto/ProductDto.java).

```java
public final class OrderDetailsDto implements Serializable {
    private final List<OrderLineDto> lines;
    // Outros campos finais: id, customer, status, createdAt, total e processingCount.

    public List<OrderLineDto> getLines() { return lines; }
    // No construtor: this.lines = List.copyOf(lines);
}
```

Uma chamada devolve os dados necessários à tela. Os DTOs não possuem `@Entity`, referências ao `EntityManager` ou coleções lazy. Os campos finais e cópias de listas impedem alterações pela apresentação. `BigDecimal` representa valores monetários; `Instant` representa o momento da criação.

O comando de entrada [CreateOrderCommand](../src/main/java/br/com/leopoldobr/j2ee/dto/CreateOrderCommand.java) usa records e valida cliente, IDs, quantidades, duplicatas e tamanho da lista. DTOs apresentados nas JSPs usam getters JavaBeans para compatibilidade com EL.

**Terminologia:** o **Value Object deste livro corresponde ao que hoje frequentemente chamamos DTO/Transfer Object**. Não confunda com um Value Object de DDD, como `Dinheiro`, que modela um conceito com igualdade por valor. Ser um record não torna uma classe automaticamente um Value Object de domínio.

**Detalhes:** copiar uma lista protege o contêiner; os elementos também precisam ser imutáveis. Defina DTOs por necessidade do caso de uso para não transformar todo acesso em transferência de grafos enormes.

## 9. Session Facade

**Problema:** o cliente coordena DAOs, objetos de negócio e envio de eventos, conhecendo demais a ordem das operações e os limites transacionais.

**Implementação:** [OrderFacade](../src/main/java/br/com/leopoldobr/j2ee/business/OrderFacade.java) e [OrderFacadeBean](../src/main/java/br/com/leopoldobr/j2ee/business/OrderFacadeBean.java).

```java
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderFacadeBean implements OrderFacade {
    @EJB private OrderDao orders;
    @EJB private ProductDao products;
    @EJB private OrderAssembler assembler;
    @EJB private OrderPublisher publisher;
    // create recebe o comando e consulta o catálogo em lote.
}
```

Dentro de `create`, a fachada:

1. Reúne os IDs dos produtos e consulta o catálogo pelo DAO.
2. Cria a raiz `Order` e adiciona itens usando preço e nome do servidor.
3. Chama `orders.save(order)`; o JPA persiste a raiz e os dependentes.
4. Chama `publisher.publish(order.getId())` na mesma transação.
5. Retorna `assembler.details(order)`.
6. O contêiner confirma a transação após o retorno do método de negócio.

O datasource e a fábrica JMS gerenciada usam XA para coordenação JTA. `MANDATORY` nos colaboradores impede que sejam chamados sem transação. A fachada não executa `begin`, `commit` ou `rollback` manualmente. Uma chamada interna `this.metodo()` não atravessa o proxy EJB e não aplica um novo atributo transacional; para outra semântica transacional, passe por outro EJB/proxy.

**Detalhes:** a fachada coordena o caso de uso, enquanto invariantes como quantidade válida ficam em `Order`/`OrderLine`. Não transforme a fachada em um objeto que concentra todas as regras e deixa as entidades sem comportamento.

**Limite:** a configuração fornece coordenação XA, mas o smoke test não simula falhas de rede durante um commit distribuído. Veja [arquitetura](arquitetura.md) para durabilidade e recuperação.

## 10. Composite Entity

**Problema:** tratar cada pequeno objeto dependente como um componente remoto independente multiplica chamadas e dificulta a consistência do conjunto.

**Implementação adaptada:** [Order](../src/main/java/br/com/leopoldobr/j2ee/domain/Order.java) é a raiz; [OrderLine](../src/main/java/br/com/leopoldobr/j2ee/domain/OrderLine.java) depende dela.

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
@OrderBy("id")
private List<OrderLine> lines = new ArrayList<>();

public void removeLine(long productId) {
    requirePending();
    OrderLine line = findLine(productId);
    if (lines.size() == 1)
        throw new InvalidOrderException("O pedido precisa de pelo menos um item.");
    lines.remove(line);
}
```

O lado `OrderLine.order` contém a FK; o construtor do item recebe a raiz. O construtor e o método que altera quantidade têm visibilidade de pacote, e a lista pública não permite alteração direta. A raiz oferece operações e protege os limites: sem duplicatas, até 20 produtos, quantidades de 1 a 100, nenhum pedido enviado vazio e nenhum item alterado após processamento.

`cascade=ALL` propaga o ciclo de vida. `orphanRemoval=true` remove do banco um item retirado da coleção quando o contexto persistente fizer flush. Não crie um DAO público de item que permita contornar essas regras. `Product` pertence ao catálogo e **não** é dependente do pedido: não se usa cascade de remoção para produto.

**Diferença histórica:** no livro, Composite Entity trata Entity Beans de granularidade grossa. O exemplo preserva o agrupamento e o controle do ciclo de vida usando um **agregado JPA**, sem fingir implementar CMP/BMP/EJB 2.x.

**Experimento:** execute `OrderTest.rootControlsDependentObjects`. A operação de alteração/remoção é demonstrada no domínio e nos testes, sem adicionar edição HTTP de pedidos já enviados.

## 11. Value Object Assembler

**Problema:** a tela precisa de dados que não pertencem a um único objeto/fonte; fazer essa junção na apresentação gera acoplamento e excesso de chamadas.

**Implementação:** [OrderAssembler](../src/main/java/br/com/leopoldobr/j2ee/business/OrderAssembler.java).

```java
Set<Long> ids = order.getLines().stream()
    .map(OrderLine::getProductId).collect(Collectors.toSet());
Map<Long, Product> catalog = products.findByIds(ids);
```

O assembler combina os itens do pedido lidos por JPA com o catálogo consultado por JDBC. Para cada item, o DTO contém o nome e o preço **na compra**, além do nome **atual** do produto. Se o produto sair do catálogo, o histórico continua íntegro e o nome atual recebe um texto indicativo.

Os dados vêm de duas fontes lógicas no mesmo banco de demonstração; não há dois serviços remotos ou bancos separados. A mesma responsabilidade poderia compor dados de serviços independentes, mas isso exigiria outra análise de consistência e falhas.

**Detalhes:** a consulta de catálogo recebe todos os IDs em uma chamada, evitando uma busca para cada item. O assembler roda dentro da transação e acessa a coleção lazy antes de construir o DTO. A view recebe uma lista já materializada.

**Experimento:** `OrderAssemblerTest` simula produto renomeado e produto removido, verifica o preço histórico e garante uma única consulta em lote. O assembler não recalcula a compra com o preço atual.

## 12. Value List Handler

**Problema:** transferir milhares de objetos de uma consulta para cada página custa memória e chamadas; o cliente precisa navegar por partes do resultado.

**Implementação:** [ValueListHandler](../src/main/java/br/com/leopoldobr/j2ee/business/ValueListHandler.java) e [OrderSearchModel](../src/main/java/br/com/leopoldobr/j2ee/presentation/OrderSearchModel.java).

```java
if (handler == null || refresh) {
    handler = new ValueListHandler<>(
        delegate.recent(ValueListHandler.MAX_RESULTS + 1), 5);
}
handler.moveTo(page);
```

O DAO devolve uma projeção de resumos, ordenada por data e ID e limitada a 501 linhas. O handler copia até 500 DTOs e mantém índice, tamanho de página e indicação de truncamento. `current`, `next`, `previous`, `reset` e `moveTo` navegam no snapshot sem nova consulta. `OrderSearchModel` guarda o handler na sessão e fornece um modelo pronto por requisição.

Essa variante inclui **cache e cursor**, como descrito no catálogo. Apenas implementar `setFirstResult/setMaxResults` em toda chamada resolveria paginação no banco, mas não demonstraria o snapshot/cursor do padrão escolhido.

**Detalhes:** cinco itens por página; cache limitado; sessão de 20 minutos; cópias imutáveis; sincronização das operações. O snapshot pode ficar desatualizado. Clique em **Atualizar consulta** para recriá-lo; criar um pedido invalida o cache da própria sessão. Abas compartilham sessão; uma lista por aba exigiria IDs de consultas e gerenciamento adicional de expiração.

**Quando trocar:** resultados enormes, muitas sessões ou alta taxa de atualização favorecem paginação no banco, frequentemente por chave (`keyset`), com índices e filtros. Não manter um `ResultSet` ou conexão JDBC abertos durante a sessão do usuário.

## 13. Service Locator

**Problema:** clientes precisam resolver nomes JNDI e encontrar proxies de serviços, mas não deveriam duplicar o lookup.

**Implementação:** [ServiceLocator](../src/main/java/br/com/leopoldobr/j2ee/integration/ServiceLocator.java), usado apenas pelo [LegacyOrderDelegate](../src/main/java/br/com/leopoldobr/j2ee/presentation/LegacyOrderDelegate.java) na rota `/app/locator`.

```java
private static final String ORDER_FACADE =
    "java:module/OrderFacadeBean!br.com.leopoldobr.j2ee.business.OrderFacade";

public OrderFacade orderFacade() {
    return OrderFacade.class.cast(
        proxies.computeIfAbsent(ORDER_FACADE, this::lookup));
}
```

O lookup cria um `InitialContext`, resolve o nome e fecha o contexto em `finally`. O cache concorrente guarda o **proxy da interface de negócio**, não resultados de pedidos nem conexões. Esse proxy é de um EJB stateless e pode ser compartilhado; não generalize o cache para clientes stateful ou recursos não seguros para concorrência.

O nome é constante e interno; não vem de parâmetros HTTP. O delegate invalida o cache em falhas técnicas e retorna indisponibilidade, sem repetir gravações. O lifecycle do cache acompanha o deploy da aplicação.

**Detalhes:** Service Locator era especialmente útil no contexto de homes EJB e lookups repetidos. Em código gerenciado moderno, injeção resolve grande parte dessa necessidade. Por isso o laboratório compara o locator com `@EJB` e não introduz lookup manual em todo o fluxo.

**Experimento:** `/app/locator` deve mostrar os cinco pedidos recentes; se o nome JNDI estiver errado, essa rota falha enquanto a rota que usa injeção continua separada.

## 14. Data Access Object

**Problema:** SQL, JPA, pool e detalhes de persistência se espalham pelo negócio.

**Implementação:** contratos [OrderDao](../src/main/java/br/com/leopoldobr/j2ee/integration/OrderDao.java) e [ProductDao](../src/main/java/br/com/leopoldobr/j2ee/integration/ProductDao.java), com [JpaOrderDao](../src/main/java/br/com/leopoldobr/j2ee/integration/JpaOrderDao.java) e [JdbcProductDao](../src/main/java/br/com/leopoldobr/j2ee/integration/JdbcProductDao.java).

```java
@Stateless
@TransactionAttribute(TransactionAttributeType.MANDATORY)
public class JpaOrderDao implements OrderDao {
    @PersistenceContext private EntityManager em;
    public void save(Order order) {
        order.validateForSubmission();
        em.persist(order);
    }
}
```

O JDBC consulta o catálogo com `PreparedStatement`. Em `findByIds`, só a quantidade de placeholders é montada dinamicamente; os IDs usam `setLong`, não concatenação. `try-with-resources` fecha os recursos e devolve a conexão ao pool mesmo em falhas.

O DAO JPA contém a consulta de projeção de resumos e o lookup com `PESSIMISTIC_WRITE`. O DAO não abre transações próprias nem fecha o `EntityManager` gerenciado. A camada de negócio decide quando executar um conjunto de operações atômicas.

**Detalhes:** não criar uma DAO genérica apenas para esconder todos os métodos do `EntityManager`. Os contratos aqui expõem operações necessárias ao domínio. Trocar o mecanismo de armazenamento exige outra implementação com o mesmo contrato, mas mudanças de banco também podem exigir migrações, revisão de consultas e testes; uma interface não garante portabilidade completa.

**DAO versus Repository:** há sobreposição. O DAO abstrai acesso à fonte; Repository costuma representar uma coleção de agregados do domínio. A escolha dos nomes não substitui limites consistentes. Aqui preservamos o termo do livro e não expomos persistência de itens isolados.

## 15. Service Activator

**Problema:** o cliente precisa disparar uma operação assíncrona sem manter uma chamada síncrona aberta até a conclusão.

**Implementação:** [OrderPublisher](../src/main/java/br/com/leopoldobr/j2ee/integration/OrderPublisher.java), [OrderServiceActivator](../src/main/java/br/com/leopoldobr/j2ee/integration/OrderServiceActivator.java) e [OrderProcessingService](../src/main/java/br/com/leopoldobr/j2ee/business/OrderProcessingService.java).

```java
@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup",
        propertyValue = "java:/jms/queue/OrderEvents"),
    @ActivationConfigProperty(propertyName = "destinationType",
        propertyValue = "jakarta.jms.Queue")
})
@TransactionAttribute(TransactionAttributeType.REQUIRED)
public class OrderServiceActivator implements MessageListener {
    @EJB private OrderProcessingService service;
    // onMessage valida texto/UUID e chama service.process(id).
}
```

O produtor envia um texto com o UUID do pedido para uma fila persistente. O MDB valida o formato e delega a operação ao EJB de negócio. O serviço busca a raiz com lock e chama:

```java
public boolean process() {
    if (status == OrderStatus.PROCESSED) return false;
    validateForSubmission();
    status = OrderStatus.PROCESSED;
    processingCount++;
    return true;
}
```

O commit do MDB confirma a atualização e a entrega da mensagem. Uma falha de sistema causa rollback e redelivery. Mensagens inválidas não são engolidas: após o limite de tentativas, o broker usa a DLQ. Neste laboratório o limite é três, configurado em [configure.cli](../infra/configure.cli).

**Detalhes:** a regra continua no negócio; o listener é um adaptador. O lock no banco evita que duas mensagens do mesmo pedido passem simultaneamente pela checagem. A idempotência demonstrada vale para a atualização desse pedido no banco, não para pagamentos ou outros efeitos externos. Não use threads manuais nem `new` para criar EJBs.

**Experimento:** crie o pedido e recarregue até `PROCESSED`. Clique em **Reenviar evento**; o contador continua em 1. O smoke test faz oito reenvios concorrentes. Para inspecionar mensagens que falharam, consulte a fila `DLQ` com a CLI do WildFly.

## Relações e sequência de estudo

| Caminho | Padrões observados |
|---|---|
| Entrada HTTP e layout | Intercepting Filter, Front Controller, View Helper, Composite View |
| Lista preparada pelo controlador | Service to Worker, Value List Handler, Value Object |
| Catálogo recuperado durante a renderização | Dispatcher View, View Helper, Business Delegate |
| Criação de pedido | Business Delegate, Session Facade, Composite Entity, DAO, Assembler |
| Consulta por nome de serviço | Service Locator + delegate alternativo |
| Processamento da fila | Service Activator + serviço de negócio + DAO |

Prossiga para [execução e testes](executar-e-testar.md), [arquitetura](arquitetura.md) e [exercícios](exercicios.md).
