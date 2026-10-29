<%@ include file="fragments/header.jspf" %>
<p class="eyebrow">Composite Entity + Value Object Assembler + Service Activator</p><h1>Detalhes do pedido.</h1>
<p data-order-id="${order.id}">Identificador: <c:out value="${order.id}"/></p>
<div class="cards"><div><small>Cliente</small><strong><c:out value="${order.customer}"/></strong></div>
<div><small>Situação</small><strong id="order-status"><c:out value="${order.status}"/></strong></div>
<div><small>Total</small><strong id="order-total"><c:out value="${format.money(order.total)}"/></strong></div>
<div><small>Processamentos aplicados</small><strong id="processing-count"><c:out value="${order.processingCount}"/></strong></div></div>
<div class="table-wrap"><table><thead><tr><th>Nome na compra</th><th>Nome atual no catálogo</th><th>Quantidade</th><th>Preço na compra</th><th>Subtotal</th></tr></thead>
<tbody><c:forEach items="${order.lines}" var="line"><tr><td><c:out value="${line.nameAtPurchase}"/></td>
<td><c:out value="${line.currentName}"/></td><td><c:out value="${line.quantity}"/></td>
<td><c:out value="${format.money(line.unitPrice)}"/></td><td><c:out value="${format.money(line.total)}"/></td></tr></c:forEach></tbody></table></div>
<p>A fila processa o pedido após o commit. Recarregue para acompanhar a situação. Reenviar o evento deve manter o contador em 1.</p>
<div class="actions"><a href="${pageContext.request.contextPath}/app/order?id=${order.id}">Recarregar</a>
<form method="post" action="${pageContext.request.contextPath}/app/replay">
<input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
<input type="hidden" name="id" value="${order.id}"><button type="submit">Reenviar evento (idempotência)</button></form></div>
<%@ include file="fragments/footer.jspf" %>
