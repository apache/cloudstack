#!/usr/bin/env bash
# Local validation for private-cicd only — no CloudStack source required.
# Safe to keep out of upstream Apache PR scope (entire tree under private-cicd/).

set -euo pipefail

usage() {
  cat << 'EOF'
Usage: validate-local.sh [--with-docker]

  Validates shell scripts (bash -n) and YAML under this repo's private-cicd
  (or standalone CI repo) root — independent of CloudStack merge scope.

  --with-docker   Also run: docker build -f docker/Dockerfile.agent .
                  (requires Docker; run from CICD_ROOT or set CICD_ROOT).

Environment:
  CICD_ROOT       Root containing scripts/, config/, docker/ (default: inferred).

Exit status: 0 if all checks pass, non-zero otherwise.
EOF
}

WITH_DOCKER=false
for arg in "$@"; do
  case "$arg" in
    -h|--help) usage; exit 0 ;;
    --with-docker) WITH_DOCKER=true ;;
    *) echo "Unknown option: $arg" >&2; usage >&2; exit 2 ;;
  esac
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -n "${CICD_ROOT:-}" ]]; then
  CICD_ROOT="$(cd "$CICD_ROOT" && pwd)"
else
  CICD_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
fi

echo "==> CICD_ROOT=$CICD_ROOT"

failures=0

run_check() {
  local name="$1"
  shift
  echo "==> $name"
  if "$@"; then
    echo "    OK"
  else
    echo "    FAILED" >&2
    failures=$((failures + 1))
  fi
}

# --- Shell: bash -n on all scripts/*.sh (no CloudStack tree required)
while IFS= read -r -d '' f; do
  run_check "bash -n: ${f#$CICD_ROOT/}" bash -n "$f"
done < <(find "$CICD_ROOT/scripts" -maxdepth 1 -type f -name '*.sh' -print0 2>/dev/null || true)

if [[ ! -d "$CICD_ROOT/scripts" ]]; then
  echo "No scripts directory at $CICD_ROOT/scripts" >&2
  failures=$((failures + 1))
fi

# --- YAML under config/
yaml_ok=false
if command -v ruby >/dev/null 2>&1 && ruby -ryaml -e 'true' >/dev/null 2>&1; then
  yaml_check() { ruby -ryaml -e "YAML.load_file(ARGV[0])" "$1"; }
  yaml_ok=true
elif command -v python3 >/dev/null 2>&1; then
  if python3 -c 'import yaml' >/dev/null 2>&1; then
    yaml_check() { python3 -c "import yaml,sys; yaml.safe_load(open(sys.argv[1]))" "$1"; }
    yaml_ok=true
  fi
elif command -v yq >/dev/null 2>&1; then
  yaml_check() { yq e '.' "$1" >/dev/null; }
  yaml_ok=true
fi

if [[ -d "$CICD_ROOT/config" ]]; then
  shopt -s nullglob
  yfiles=("$CICD_ROOT"/config/*.yaml "$CICD_ROOT"/config/*.yml)
  shopt -u nullglob
  if [[ ${#yfiles[@]} -eq 0 ]]; then
    echo "==> YAML: no *.yaml in $CICD_ROOT/config (skipped)"
  elif [[ "$yaml_ok" == true ]]; then
    for f in "${yfiles[@]}"; do
      run_check "YAML: ${f#$CICD_ROOT/}" yaml_check "$f"
    done
  else
    echo "==> YAML: skipped (install Ruby+psych, PyYAML, or yq to validate)" >&2
  fi
else
  echo "==> YAML: no config directory (skipped)"
fi

# --- Docker (optional)
if [[ "$WITH_DOCKER" == true ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "==> docker: not installed, skipping" >&2
    failures=$((failures + 1))
  else
    df="$CICD_ROOT/docker/Dockerfile.agent"
    if [[ ! -f "$df" ]]; then
      echo "==> docker: missing $df" >&2
      failures=$((failures + 1))
    else
      run_check "docker build (agent image)" docker build -f "$df" -t cloudstack-private-cicd-agent:validate "$CICD_ROOT"
    fi
  fi
fi

if [[ "$failures" -ne 0 ]]; then
  echo "==> Done with $failures failure(s)." >&2
  exit 1
fi

echo "==> All checks passed."
