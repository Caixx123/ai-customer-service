#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/src/main/resources/onnx"
mkdir -p "$DEST"

curl -L --fail --retry 2 \
  -o "$DEST/model_quantized.onnx" \
  "https://hf-mirror.com/Xenova/bge-small-zh-v1.5/resolve/main/onnx/model_quantized.onnx"

curl -L --fail --retry 2 \
  -o "$DEST/tokenizer.json" \
  "https://hf-mirror.com/Xenova/bge-small-zh-v1.5/resolve/main/tokenizer.json"

ls -lh "$DEST"
