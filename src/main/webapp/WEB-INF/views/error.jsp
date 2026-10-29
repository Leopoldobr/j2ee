<%@ include file="fragments/header.jspf" %>
<p class="eyebrow">Solicitação não concluída</p><h1>Não foi possível continuar.</h1>
<p role="alert"><c:out value="${errorMessage}"/></p><p><a href="${pageContext.request.contextPath}/app/new">Voltar ao formulário</a></p>
<%@ include file="fragments/footer.jspf" %>
