#!/usr/bin/env bash
# Usage: setup_tools.sh <working_dir>
# Creates <W>/venv with pypdf, copies helper tools to <W>/tools, prepares <W>/research and <W>/dl.
set -euo pipefail
W="${1:?usage: setup_tools.sh <working_dir>}"
HERE="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$W/research" "$W/dl" "$W/tools"
if [ ! -x "$W/venv/bin/python" ]; then python3 -m venv "$W/venv"; fi
"$W/venv/bin/pip" install -q pypdf
cp "$HERE/pdf2txt.py" "$HERE/openalex_citers.py" "$W/tools/"
echo "pypdf: $("$W/venv/bin/python" -c 'import pypdf;print(pypdf.__version__)')"
for t in gs node pdftotext; do
  if command -v "$t" >/dev/null 2>&1; then echo "$t: available"; else echo "$t: MISSING"; fi
done
echo "PDF -> text: $W/venv/bin/python -I $W/tools/pdf2txt.py <file.pdf> > <file.txt>"
echo "Citers:      python3 $W/tools/openalex_citers.py <arXiv-ID|DOI>"
