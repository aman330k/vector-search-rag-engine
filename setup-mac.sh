#!/usr/bin/env bash
# One-time setup for macOS: install prerequisites and pull the Ollama models.
set -euo pipefail

echo "==> Checking Homebrew..."
if ! command -v brew >/dev/null 2>&1; then
  echo "Homebrew not found. Install it from https://brew.sh then re-run this script."
  exit 1
fi

echo "==> Installing JDK 21 and Maven (if missing)..."
brew list openjdk@21 >/dev/null 2>&1 || brew install openjdk@21
brew list maven      >/dev/null 2>&1 || brew install maven

echo "==> Installing Ollama (if missing)..."
brew list ollama >/dev/null 2>&1 || brew install ollama

echo "==> Pulling models (this can take a while on first run)..."
# llama3.2:1b is the small ~1.3 GB generation model — a good default on Apple Silicon
# laptops (M1/M2/M3) so the LLM responds quickly on CPU/unified memory.
if command -v ollama >/dev/null 2>&1; then
  ollama pull nomic-embed-text
  ollama pull llama3.2:1b
else
  echo "WARN: 'ollama' CLI not found on PATH. Open the Ollama app, then run:"
  echo "      ollama pull nomic-embed-text && ollama pull llama3.2:1b"
fi

echo ""
echo "Setup complete. Next:"
echo "  1) Start Ollama:   ollama serve   (or open the Ollama menu-bar app)"
echo "  2) Start the app:  ./run.sh"
echo "  3) Open:           http://localhost:8081"
