<%@ include file="fragments/header.jspf" %>
<p class="eyebrow">Dispatcher View + View Helper</p><h1>Catálogo carregado pela view.</h1>
<p>O controlador só escolhe a JSP. A primeira consulta a <code>catalogView.products</code> invoca o helper CDI, que chama o delegate e guarda o resultado nesta requisição.</p>
<div class="cards"><c:forEach items="${catalogView.products}" var="product"><div><small>Produto <c:out value="${product.id}"/></small>
<strong><c:out value="${product.name}"/></strong><p><c:out value="${format.money(product.price)}"/></p></div></c:forEach></div>
<%@ include file="fragments/footer.jspf" %>
