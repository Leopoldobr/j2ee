# Validação da entrega

Verificação local realizada em **29/10/2026**, com JDK 17, Maven 3.9.9 e WildFly 35.0.1.Final.

| Verificação | Resultado |
|---|---|
| Compilação de 34 classes de aplicação | Passou |
| JUnit: 20 testes | Passou; zero falhas, erros ou testes ignorados |
| Empacotamento `j2ee-patterns.war` | Passou |
| Configuração isolada do datasource H2 XA e JMS | Passou |
| Deploy no WildFly Full Profile | Passou |
| Roteiro HTTP: 27 verificações | Passou |
| Oito reenvios concorrentes do evento | Contador do efeito permaneceu em 1 |
| JSP/EL e Dispatcher View | Renderização real conferida pelo roteiro HTTP |
| Resolução JNDI pela rota Service Locator | Passou |
| Links locais da documentação e sintaxe XML/Bash/Python | Conferidos |

Os cenários HTTP e suas asserções estão em [smoke.py](../scripts/smoke.py). As instruções para reproduzi-los estão em [executar e testar](executar-e-testar.md).

O Dockerfile e o Compose foram preparados; o build de contêiner não foi executado neste ambiente. A imagem WildFly indicada foi conferida no registro público. O fluxo equivalente de configuração, deploy e teste foi executado diretamente na mesma versão do servidor.

Os testes não simulam desastre, recuperação XA, falha de disco ou interrupção de rede durante commit distribuído. A execução do GitHub Actions depende da publicação do workflow e das configurações do repositório; o resultado local acima não é uma afirmação de que a execução remota já terminou.
