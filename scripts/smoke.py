#!/usr/bin/env python3
"""Testes HTTP reais de Servlet, JSP/EL, CDI, EJB, JPA, JDBC, JTA, JNDI e JMS.

Executar contra um deploy de laboratório: cria pedidos de teste no banco.
"""
import concurrent.futures
import http.cookiejar
import os
import re
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get('BASE_URL', 'http://127.0.0.1:8080/j2ee-patterns')
jar = http.cookiejar.CookieJar()


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


client = urllib.request.build_opener(urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(jar), NoRedirect)


def request(path, fields=None):
    data = None if fields is None else urllib.parse.urlencode(fields, doseq=True).encode()
    req = urllib.request.Request(BASE + path, data=data)
    try:
        response = client.open(req, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        return response.status, response.headers, response.read().decode('utf-8')


def check(condition, message):
    if not condition:
        raise AssertionError(message)
    print('OK:', message, flush=True)


for attempt in range(120):
    try:
        if request('/app/new')[0] == 200:
            break
    except (OSError, urllib.error.URLError):
        pass
    time.sleep(1)
else:
    raise RuntimeError('Aplicação indisponível após 120 segundos; veja target/wildfly-it.log')

status, headers, form = request('/app/new')
check(status == 200 and 'Teclado' in form and 'Mouse' in form, 'JSP renderiza catálogo do DAO JDBC')
token = re.search(r'name="csrf" value="([^"]+)"', form).group(1)
check(bool(headers.get('X-Request-Id')), 'Intercepting Filter fornece identificador de requisição')
check(request('/app/order', {'customer': 'Sem token', 'productId': 1, 'quantity': 1})[0] == 403,
      'POST sem token CSRF é rejeitado')
fields = {'csrf': token, 'customer': '<script>alert(1)</script>', 'productId': [1, 2], 'quantity': [2, 3], 'price': '0.01'}
status, headers, _ = request('/app/order', fields)
check(status == 303, 'Session Facade cria pedido e usa Post/Redirect/Get')
location = headers['Location']
order_id = urllib.parse.parse_qs(urllib.parse.urlsplit(location).query)['id'][0]


def details():
    return request('/app/order?id=' + order_id)[2]


for attempt in range(60):
    body = details()
    if 'id="order-status">PROCESSED' in body:
        break
    time.sleep(0.25)
check('id="order-status">PROCESSED' in body, 'JMS entrega mensagem e MDB processa após o commit')
check('id="processing-count">1' in body, 'Efeito de negócio foi aplicado uma vez')
check('586,50' in body, 'Total 586,50 usa BigDecimal e ignora preço enviado pelo navegador')
check('<script>alert(1)</script>' not in body and '&lt;script&gt;' in body, 'View escapa o nome do cliente')
check('Nome atual no catálogo' in body and 'Teclado' in body, 'Assembler reúne histórico JPA e catálogo JDBC')

# Eventos duplicados concorrentes testam o lock do banco e a idempotência do consumidor.
def replay(_):
    return request('/app/replay', {'csrf': token, 'id': order_id})[0]


with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    check(all(code == 303 for code in pool.map(replay, range(8))), 'Oito reenvios concorrentes aceitos')
time.sleep(1)
check('id="processing-count">1' in details(), 'Reentregas preservam processingCount=1')

bad_fields = {'csrf': token, 'customer': 'Produto inexistente', 'productId': [1, 999], 'quantity': [1, 1]}
check(request('/app/order', bad_fields)[0] == 400, 'Falha de negócio com produto inexistente retorna 400')
check(request('/app/order', {'csrf': token, 'customer': 'Sem itens', 'productId': 1, 'quantity': 0})[0] == 400,
      'Pedido vazio é rejeitado')
check(request('/app/order?id=00000000-0000-0000-0000-000000000000')[0] == 404, 'Pedido inexistente retorna 404')
check(request('/app/order?id=abc')[0] == 400, 'UUID inválido retorna 400')

for index in range(6):
    code, _, _ = request('/app/order', {'csrf': token, 'customer': f'Paginação {index}', 'productId': 1, 'quantity': 1})
    check(code == 303, f'Pedido adicional {index + 1} criado')
status, _, first = request('/app/orders?refresh=true')
check(status == 200 and '7 pedidos no snapshot' in first and 'Próxima' in first,
      'Value List Handler mantém snapshot de 7 pedidos; falhas não criaram pedidos')
status, _, second = request('/app/orders?page=1')
check(status == 200 and 'Anterior' in second, 'Snapshot permite navegar para a segunda página')
check(request('/app/orders?page=999')[0] == 400, 'Página fora do snapshot é rejeitada')
check(request('/app/catalog')[0] == 200 and 'Monitor' in request('/app/catalog')[2],
      'Dispatcher View carrega dados pelo helper durante a renderização')
check(request('/app/locator')[0] == 200, 'Service Locator resolve a fachada por JNDI')
check(request('/WEB-INF/views/order.jsp')[0] == 404, 'JSP não pode ser acessada diretamente')
print('Todos os cenários HTTP passaram.', flush=True)
