<%@ include file="fragments/header.jspf" %>
<p class="eyebrow">Business Delegate → Session Facade → DAO</p><h1>Criar pedido.</h1>
<p>Escolha quantidades. O servidor consulta o preço; o navegador envia somente cliente, IDs e quantidades.</p>
<form method="post" action="${pageContext.request.contextPath}/app/order">
<input type="hidden" name="csrf" value="<c:out value='${sessionScope.csrfToken}'/>">
<label for="customer">Cliente</label><input id="customer" name="customer" maxlength="100" required autocomplete="name">
<div class="table-wrap"><table><thead><tr><th>Produto</th><th>Preço unitário</th><th>Quantidade</th></tr></thead><tbody>
<c:forEach items="${products}" var="product"><tr><td><c:out value="${product.name}"/>
<input type="hidden" name="productId" value="${product.id}"></td><td><c:out value="${format.money(product.price)}"/></td>
<td><input aria-label="Quantidade de <c:out value='${product.name}'/>" type="number" name="quantity" min="0" max="100" value="0" required></td></tr></c:forEach>
</tbody></table></div><button type="submit">Salvar e enviar para processamento</button></form>
<%@ include file="fragments/footer.jspf" %>
