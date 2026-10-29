<%@ include file="fragments/header.jspf" %>
<p class="eyebrow">Service Locator + Business Delegate</p><h1>Localização explícita via JNDI.</h1>
<p>Esta rota usa um delegate alternativo: ele resolve e reutiliza o proxy da fachada com <code>ServiceLocator</code>. Compare com a injeção usada no fluxo principal.</p>
<c:set var="tableOrders" value="${recent}" scope="request"/>
<%@ include file="fragments/order-table.jspf" %>
<%@ include file="fragments/footer.jspf" %>
