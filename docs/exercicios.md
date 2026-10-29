# Exercícios guiados

## 1. Percorrer um pedido com o depurador

Coloque breakpoints em `FrontController.doPost`, `OrderDelegate.create`, `OrderFacadeBean.create`, `JpaOrderDao.save`, `OrderPublisher.publish` e `OrderServiceActivator.onMessage`. Crie um pedido com teclado e mouse.

Observe que o servlet recebe strings; o comando transporta IDs/quantidades; a fachada consulta preços do servidor; a entidade protege invariantes; o assembler devolve DTOs; o MDB é executado em outra chamada/transação.

**Resultado esperado:** a apresentação não recebe um `Order` JPA e o contador de processamento chega a 1.

## 2. Comparar os macropadrões de apresentação

Em `/app/orders`, encontre a chamada que prepara `ordersPage` antes do forward. Em `/app/catalog`, encontre a chamada EL que carrega `catalogView.products` durante a renderização.

Mova temporariamente o carregamento do catálogo para o controlador, coloque o resultado em um atributo e altere a JSP para lê-lo. A tela fica parecida, mas a colaboração passa a corresponder a Service to Worker.

**Resultado esperado:** saber explicar a diferença por ordem e responsabilidade, sem depender do nome das classes.

## 3. Manter o histórico de compra

Execute `OrderAssemblerTest`. O catálogo retorna um nome e preço novos, enquanto o pedido preserva o nome e preço capturados na compra.

Estenda o exemplo para incluir um código de SKU no produto e na linha. Decida quais dados pertencem ao histórico e quais devem ser enriquecidos na leitura.

**Resultado esperado:** uma mudança de preço no catálogo não altera o total de um pedido existente.

## 4. Explorar o snapshot

Crie mais de cinco pedidos. Navegue entre as páginas, abra outra sessão e crie um pedido nela. Volte à primeira sessão e compare a lista antes e depois de **Atualizar consulta**.

Compare o custo de um snapshot de 500 DTOs por sessão com paginação no banco em um sistema com muitas sessões. Desenhe uma alternativa com filtro por cliente e paginação por `(createdAt, id)`.

**Resultado esperado:** distinguir cache por sessão, cache global e paginação de consulta.

## 5. Exercitar idempotência

Reenvie o evento do mesmo pedido várias vezes, inclusive em abas simultâneas. O contador deve continuar em 1. Depois envie o formulário de criação duas vezes deliberadamente.

**Resultado esperado:** perceber que duas criações podem produzir dois IDs diferentes, embora duplicatas de mensagem para o mesmo ID não reapliquem o efeito. Como extensão, adicione uma chave de idempotência de criação com restrição única no banco e política de retenção.

## 6. Trocar o mecanismo de acesso

Implemente uma versão JPA de `ProductDao` preservando o contrato e substitua a implementação JDBC. Habilite apenas uma implementação EJB para não criar ambiguidade de injeção.

**Resultado esperado:** fachada e JSPs permanecem sem SQL. A necessidade de validação e transações continua existindo, independentemente do mecanismo de persistência.

## 7. Reconhecer o custo das abstrações

Compare `/app/locator` com o fluxo normal de injeção. Explique por que um cache de proxy stateless pode ser compartilhado, mas uma conexão JDBC ou sessão JMS não deve ser guardada nesse mesmo cache.

Em seguida, identifique quais camadas você manteria em um sistema pequeno. Justifique a decisão por acoplamento, fronteiras de execução e possibilidade real de troca, não apenas por uma lista de padrões.
