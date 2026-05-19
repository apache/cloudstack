# Private CI/CD (downstream only)

This directory is **not** part of Apache CloudStack upstream. Do **not** include it in pull requests to `apache/cloudstack`.

## Option A (default): committed on the NetApp fork

`private-cicd/` is **tracked on integration branches** (e.g. `dev_branch`, `netapp/main`). Jenkins uses the same checkout as CloudStack.

- **Script Path:** `private-cicd/Jenkinsfile`
- **Clone CloudStack separately:** leave unchecked (`CLONE_SEPARATE = false`)
- **Upstream PRs:** use branches without `private-cicd/` commits — see [`docs/BRANCH-STRATEGY.md`](docs/BRANCH-STRATEGY.md)
- **Layout:** [`docs/FOLDER-LAYOUT.md`](docs/FOLDER-LAYOUT.md)

## Rollout phases

| Phase | `PIPELINE_PHASE` | Status |
|-------|------------------|--------|
| **-1 — Preflight** | `preflight` | Implemented |
| **1a — ONTAP fast** | `build-ontap-fast` | Implemented (`scripts/mvn-ontap-fast.sh`) |
| **1 — Full build** | `build-only` | Implemented |
| **2 — Marvin** | `marvin` | Stub — see `config/marvin.yaml`, `scripts/marvin-run.sh` |
| **3 — CD** | `delivery` | Planned |

## Quick start

```bash
# Validate CI tree only (no Maven)
./private-cicd/scripts/validate-local.sh

# ONTAP plugin compile + JUnit (from repo root; only ontap *Test.java, not whole tree)
CLOUDSTACK_DIR=$PWD SKIP_TESTS=false ./private-cicd/scripts/mvn-ontap-fast.sh

# Full build (long)
CLOUDSTACK_DIR=$PWD SKIP_TESTS=true ./private-cicd/scripts/mvn-full.sh
```

Do not use `mvn -pl :cloud-plugin-storage-volume-ontap -am test` alone — `-am test` runs upstream module tests (e.g. `engine/schema`), which may call `sudo mount` and block on `Password:`.

## Configuration

| File | Purpose |
|------|---------|
| [`config/defaults.yaml`](config/defaults.yaml) | Git URL, branch, profiles |
| [`config/build-fast.yaml`](config/build-fast.yaml) | ONTAP `-pl -am` settings |
| [`config/marvin.yaml`](config/marvin.yaml) | Marvin phase (Phase 2) |
| [`config/qa.yaml.example`](config/qa.yaml.example) | Copy to `qa.yaml` for local overrides (gitignored) |

## Marvin tests

- Product tests: `test/integration/plugins/ontap/`
- CI bundles: [`marvin/bundles.txt`](marvin/bundles.txt)
- Zone templates: [`marvin/zones/`](marvin/zones/)

## Alternative: separate CI repository

Copy this tree to a dedicated repo and set `CLONE_SEPARATE = true` in Jenkins. See historical note in git history if you migrate from Option A.

## License

Scripts and the Jenkinsfile are for internal use only; not submitted to the ASF as part of CloudStack.
