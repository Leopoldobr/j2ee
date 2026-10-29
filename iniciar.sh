#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'HELP'
Uso: ./iniciar.sh [start|stop|restart|logs|status|help]

  start    Constrói a imagem, inicia a aplicação e aguarda o deploy (padrão).
  stop     Encerra e remove os contêineres e a rede deste projeto.
  restart  Para a aplicação e inicia novamente, reconstruindo se necessário.
  logs     Acompanha os logs; Ctrl+C sai dos logs e mantém a aplicação ativa.
  status   Mostra o estado dos contêineres e do healthcheck.

Variáveis opcionais:
  APP_PORT=8080         Porta HTTP local, entre 1 e 65535.
  STARTUP_TIMEOUT=180   Espera pelo serviço saudável, entre 1 e 3600 segundos.

Exemplo: APP_PORT=9090 STARTUP_TIMEOUT=300 ./iniciar.sh
Pré-requisitos: Bash, Docker em execução e Docker Compose v2 com --wait-timeout.
HELP
}

fail() { printf 'Erro: %s\n' "$*" >&2; exit 1; }

if (( $# > 1 )); then usage >&2; exit 2; fi
action="${1:-start}"
case "$action" in
  help|-h|--help) usage; exit 0 ;;
  start|stop|restart|logs|status) ;;
  *) usage >&2; exit 2 ;;
esac

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_dir"

APP_PORT="${APP_PORT:-8080}"
STARTUP_TIMEOUT="${STARTUP_TIMEOUT:-180}"
if [[ ! "$APP_PORT" =~ ^[0-9]{1,5}$ ]] || (( 10#$APP_PORT < 1 || 10#$APP_PORT > 65535 )); then
  fail 'APP_PORT deve estar entre 1 e 65535.'
fi
if [[ ! "$STARTUP_TIMEOUT" =~ ^[0-9]{1,4}$ ]] || (( 10#$STARTUP_TIMEOUT < 1 || 10#$STARTUP_TIMEOUT > 3600 )); then
  fail 'STARTUP_TIMEOUT deve estar entre 1 e 3600 segundos.'
fi
# Normaliza zeros à esquerda antes de enviar os valores ao Compose.
APP_PORT="$((10#$APP_PORT))"
STARTUP_TIMEOUT="$((10#$STARTUP_TIMEOUT))"
export APP_PORT

command -v docker >/dev/null 2>&1 || fail 'Docker não encontrado. Instale o Docker Engine ou o Docker Desktop.'
docker compose version >/dev/null 2>&1 || fail 'Docker Compose v2 não encontrado. Instale ou atualize o plugin Compose.'
docker info >/dev/null 2>&1 || fail 'Docker indisponível. Inicie o Docker Desktop/serviço Docker e confira suas permissões.'
compose_cmd=(docker compose --project-directory "$project_dir" --file "$project_dir/compose.yaml")

start_app() {
  local up_help
  up_help="$(docker compose up --help)"
  [[ "$up_help" == *'--wait-timeout'* ]] || fail 'Atualize o Docker Compose v2 para uma versão com --wait-timeout.'
  printf 'Construindo a imagem e iniciando WildFly, banco H2 e fila JMS...\n'
  if ! "${compose_cmd[@]}" up --build --detach --wait --wait-timeout "$STARTUP_TIMEOUT"; then
    printf '\nFalha na inicialização. Estado e últimos logs:\n' >&2
    "${compose_cmd[@]}" ps >&2 || true
    "${compose_cmd[@]}" logs --no-color --tail=80 app >&2 || true
    return 1
  fi
  printf '\nAplicação pronta: http://localhost:%s/j2ee-patterns/app/orders\n' "$APP_PORT"
  printf 'Logs: ./iniciar.sh logs\nParar: ./iniciar.sh stop\n'
}

case "$action" in
  start) start_app ;;
  stop) "${compose_cmd[@]}" down ;;
  restart) "${compose_cmd[@]}" stop app; start_app ;;
  logs) exec "${compose_cmd[@]}" logs --follow --tail=100 app ;;
  status) "${compose_cmd[@]}" ps ;;
esac
