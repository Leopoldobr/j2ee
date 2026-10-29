<%@ include file="fragments/header.jspf" %>
<p class="eyebrow">Service to Worker + Value List Handler</p><h1>Pedidos em uma consulta.</h1>
<p>O controlador preparou os dados antes de encaminhar esta JSP. A navegação usa um snapshot da sua sessão.</p>
<div class="actions"><a class="button" href="${pageContext.request.contextPath}/app/new">Criar pedido</a>
<a href="${pageContext.request.contextPath}/app/orders?refresh=true">Atualizar consulta</a></div>
<c:set var="tableOrders" value="${ordersPage.items}" scope="request"/>
<%@ include file="fragments/order-table.jspf" %>
<div class="actions"><c:if test="${ordersPage.index > 0}"><a href="?page=${ordersPage.index - 1}">Anterior</a></c:if>
<span>Página <c:out value="${ordersPage.index + 1}"/> de <c:out value="${ordersPage.pageCount}"/> · <c:out value="${ordersPage.size}"/> pedidos no snapshot</span>
<c:if test="${ordersPage.index + 1 < ordersPage.pageCount}"><a href="?page=${ordersPage.index + 1}">Próxima</a></c:if></div>
<c:if test="${ordersPage.truncated}"><p class="notice">Exibindo os 500 pedidos mais recentes. Este limite mantém o cache da sessão pequeno.</p></c:if>
<%@ include file="fragments/footer.jspf" %>
