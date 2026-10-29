# Executar e testar

## Caminho 1: Docker Compose

Pré-requisitos: Docker com Compose. O build usa Maven e JDK 17 dentro da imagem; o runtime é WildFly 35.0.1.Final com Full Profile.

```bash
git clone https://github.com/Leopoldobr/j2ee.git
cd j2ee
docker compose up --build
```

Abra [a lista de pedidos](http://localhost:8080/j2ee-patterns/app/orders). Use **Novo pedido**, informe cliente e quantidades maiores que zero e envie. O sistema captura os preços do catálogo, salva pedido/itens e envia o evento JMS. Nos detalhes, recarregue para observar `PROCESSED` e contador `1`.

```bash
docker compose logs -f app
docker compose down
```

O datasource XA e o limite de redelivery são criados durante a construção da imagem por `infra/configure.cli`. A imagem usa as credenciais locais de demonstração do H2. Não há conta de administração criada nem porta de gerenciamento publicada pelo Compose.

O Compose não configura volume persistente. Além disso, o JPA está em `drop-and-create`: **redeploy recria as tabelas**, mesmo que você adicione um volume posteriormente. O objetivo é estudo reproduzível.

## Caminho 2: JDK, Maven e WildFly locais

Pré-requisitos:

- JDK 17 com `java` e `javac` no `PATH`.
- Maven 3.9 ou superior.
- WildFly **35.0.1.Final**, distribuição padrão completa, extraída.
- Portas 8080 e 9990 disponíveis.

Uma URL fixa da distribuição, também usada na CI:

```bash
curl --fail --location https://repo.maven.apache.org/maven2/org/wildfly/wildfly-dist/35.0.1.Final/wildfly-dist-35.0.1.Final.zip -o wildfly.zip
unzip -q wildfly.zip
export WILDFLY_HOME="$PWD/wildfly-35.0.1.Final"
```

Baixe/extraia o servidor fora do checkout ou em uma pasta temporária. Não versione a distribuição.

No diretório do repositório:

```bash
mvn clean verify
# Execute uma única vez em uma instalação de laboratório, com servidor parado:
"$WILDFLY_HOME/bin/jboss-cli.sh" --file=infra/configure.cli
cp target/j2ee-patterns.war "$WILDFLY_HOME/standalone/deployments/"
"$WILDFLY_HOME/bin/standalone.sh" -c standalone-full.xml -b 127.0.0.1
```

O CLI cria um datasource XA `java:/jdbc/PatternsDS` usando o driver H2 já incluído no servidor. Se executado novamente na mesma configuração, o comando de criação falha porque o recurso já existe; não é um script de migração idempotente. O teste de integração usa uma configuração isolada para evitar essa situação.

Use `standalone-full.xml`: a configuração padrão `standalone.xml` não inclui JMS. Um Tomcat isolado também não é suficiente para executar este WAR, pois são usados EJB, CDI, JMS, JPA e JTA.

No Windows, use `jboss-cli.bat`, `standalone.bat`, ajuste `WILDFLY_HOME` e use o comando de cópia equivalente. Para os scripts Bash, use WSL/Git Bash, ou rode o smoke test em Python contra um servidor iniciado manualmente.

## Testes unitários

```bash
mvn clean verify
```

Esse comando compila o Java, executa os testes JUnit e produz `target/j2ee-patterns.war`. **Não inicia o servidor** e não comprova sozinho deploy, JSP, JNDI, transações do contêiner ou consumo JMS.

| Teste | Propriedade exercitada |
|---|---|
| `OrderTest` | Precisão monetária, controle dos itens, invariantes e idempotência do efeito |
| `CreateOrderCommandTest` | Cópia defensiva, rejeição de lista vazia, nulos e duplicatas |
| `ValueListHandlerTest` | Próximo/anterior, limites, resultado vazio, cache limitado e cópias |
| `FrontControllerTest` | Parâmetros inconsistentes/malformados, itens não selecionados e CSRF |
| `OrderAssemblerTest` | Uma consulta em lote, produto renomeado/removido e preservação do histórico |

## Teste de integração em servidor real

Com JDK 17, Python 3, distribuição do WildFly extraída e portas livres:

```bash
mvn clean verify
export WILDFLY_HOME=/caminho/wildfly-35.0.1.Final
bash scripts/integration.sh
```

O script cria `target/wildfly-it` com configuração independente, aplica o CLI, faz o deploy do WAR, inicia o servidor, executa os cenários HTTP e encerra o processo. Não altera a instalação original do servidor. Para repetir, use `mvn clean verify` antes. Não rode um `mvn clean` enquanto o servidor de integração estiver ativo.

São exercitados:

1. Deploy e renderização real de JSP/EL, catálogo JDBC e cabeçalho do filtro.
2. Rejeição de POST sem token CSRF.
3. Criação pela fachada e redirecionamento 303.
4. Cálculo com preços do servidor, itens persistidos e DTO composto.
5. Commit do pedido, entrega JMS e processamento pelo MDB.
6. Oito reenvios concorrentes do evento, mantendo `processingCount=1`.
7. Erros de negócio e parâmetros inválidos com 400/404.
8. Sete pedidos em snapshot, navegação e rejeição de página inválida.
9. Recuperação de dados durante a renderização em Dispatcher View.
10. Resolução da fachada via JNDI e bloqueio de acesso direto às JSPs.

O cenário que usa produto inexistente verifica rejeição sem criação de pedido. **Não é uma simulação de falha de infraestrutura no meio de um commit XA.** A configuração transacional e o teste de sucesso não substituem um teste de recuperação distribuída.

Para testar uma aplicação já iniciada, inclusive pelo Compose:

```bash
BASE_URL=http://localhost:8080/j2ee-patterns python3 scripts/smoke.py
```

Use uma instância limpa: o roteiro cria dados e espera uma contagem conhecida de pedidos. Ele não apaga dados anteriores. O build da imagem executa os testes unitários; os testes HTTP precisam da aplicação iniciada.

## Inspecionar a fila

Com o servidor local iniciado:

```bash
"$WILDFLY_HOME/bin/jboss-cli.sh" --connect
```

Na CLI, confira o nome efetivo das filas e os recursos antes de consultar estatísticas:

```text
/subsystem=messaging-activemq/server=default:read-children-names(child-type=jms-queue)
/subsystem=messaging-activemq/server=default/jms-queue=DLQ:read-resource(include-runtime=true)
```

A fila da aplicação é declarada por `@JMSDestinationDefinition`. Não coloque regras de negócio no listener nem confirme manualmente uma mensagem em uma transação gerenciada pelo contêiner.

## Diagnóstico

| Sintoma | Conferir |
|---|---|
| `javac` ausente | Instale um JDK, não apenas um JRE |
| Falha de download Maven | Acesso ao Maven Central e configuração de proxy/certificados da sua rede |
| `PatternsDS` não encontrado | Execute `infra/configure.cli` antes do deploy |
| Subsistema JMS ausente | Inicie com `-c standalone-full.xml` |
| `.failed` no diretório de deploy | Leia o arquivo e o log do servidor |
| 403 no formulário | Abra novamente `/app/new` na mesma sessão e envie o token atualizado |
| Pedidos não mudam na listagem | Clique em **Atualizar consulta** para recriar o snapshot |
| MDB não conclui | Verifique exceções, transação, fila e DLQ no log |

Logs do roteiro: `target/wildfly-configure.log`, `target/wildfly-it.log` e `target/wildfly-it/log/server.log`. Relatórios JUnit: `target/surefire-reports`. A CI salva os logs se um cenário falhar.
