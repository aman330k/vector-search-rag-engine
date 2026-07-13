#!/usr/bin/env bash
# Start the VectorDB-Java app on macOS.
set -euo pipefail

# Prefer a modern JDK if one is available via /usr/libexec/java_home.
if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
elif /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi

echo "Using JAVA_HOME=${JAVA_HOME:-<default>}"
java -version

if ! curl -s http://127.0.0.1:11434/api/tags >/dev/null 2>&1; then
  echo "NOTE: Ollama does not appear to be running on :11434."
  echo "      RAG features need it. Start it with:  ollama serve"
fi

exec mvn spring-boot:run
