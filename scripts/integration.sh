#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${WILDFLY_HOME:?Defina WILDFLY_HOME com o caminho do WildFly 35.0.1.Final}"
test -f target/j2ee-patterns.war || { echo 'Execute mvn verify primeiro.' >&2; exit 1; }
run_dir="$PWD/target/wildfly-it"
if [ -e "$run_dir" ]; then
  echo 'target/wildfly-it já existe. Execute mvn clean verify para uma nova execução isolada.' >&2
  exit 1
fi
mkdir -p "$run_dir/configuration" "$run_dir/deployments"
cp "$WILDFLY_HOME"/standalone/configuration/* "$run_dir/configuration/"
"$WILDFLY_HOME/bin/jboss-cli.sh" -Djboss.server.base.dir="$run_dir" --file=infra/configure.cli > target/wildfly-configure.log 2>&1
cp target/j2ee-patterns.war "$run_dir/deployments/"
"$WILDFLY_HOME/bin/standalone.sh" -c standalone-full.xml -b 127.0.0.1 -Djboss.server.base.dir="$run_dir" > target/wildfly-it.log 2>&1 &
server_pid=$!
trap 'kill "$server_pid" 2>/dev/null || true; wait "$server_pid" 2>/dev/null || true' EXIT
python3 scripts/smoke.py
