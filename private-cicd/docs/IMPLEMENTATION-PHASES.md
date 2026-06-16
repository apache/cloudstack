<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

# Private CI/CD — three-phase rollout

This file is under `private-cicd/` only (not for Apache CloudStack upstream).

## Phase -1 — Preflight (`preflight`)

**Goal:** Validate the private CI tree itself (shell syntax, YAML) on the **same** Jenkins agent you will use for builds — **no** CloudStack `pom.xml`, **no** Maven, **no** clone (unless you later add optional checks).

**Implemented:**

- Jenkins: set `PIPELINE_PHASE` to **`preflight`**. After `Resolve CloudStack directory`, the job runs `scripts/validate-local.sh` with `CICD_ROOT` pointing at this tree.
- Locally: `./private-cicd/scripts/validate-local.sh` (same checks).

**Agent needs:** `bash`; for YAML parsing, one of Ruby (stdlib `yaml`), Python with PyYAML, or `yq`. If none are present, the script skips YAML with a message (non-fatal); install a parser on agents for strict validation.

**How to validate behaviour:** Run a Jenkins job with `preflight` and confirm the **Phase -1: validate private-cicd** stage is green and no Maven stages run. Then run `build-only` for Phase 1.

## Phase 1a — ONTAP fast build (`build-ontap-fast`)

**Goal:** Fast PR feedback — compile and JUnit for `cloud-plugin-storage-volume-ontap` only (`-pl -am test`).

**Implemented:**

- `scripts/mvn-ontap-fast.sh`, `config/build-fast.yaml`
- Jenkins stage **Phase 1a: ONTAP fast build**
- JUnit glob: `plugins/storage/volume/ontap/target/surefire-reports/*.xml`

**Local:**

```bash
CLOUDSTACK_DIR=$PWD SKIP_TESTS=false ./private-cicd/scripts/mvn-ontap-fast.sh
```

**Note:** Does not replace full build before Marvin or release; run `build-only` on merge/nightly.

**Important:** `mvn-ontap-fast.sh` uses two Maven invocations: `(-am -DskipTests install)` then `(-pl ontap test)`. A single `mvn -pl ontap -am test` runs upstream tests (e.g. `engine/schema` / `SystemVmTemplateRegistrationTest`) and can hang on `sudo` `Password:`.

## Phase 1 — Build only (full)

**Goal:** Prove a reproducible compile on your Jenkins agents (same intent as upstream `.github/workflows/build.yml`, without living in `.github/`).

**Implemented:**

- Declarative pipeline: `PIPELINE_PHASE` = **build-only**.
- Optional fast path: **build-ontap-fast** (default first choice in Jenkins parameter list).
- Checkout modes: multibranch workspace vs separate clone (`CLONE_SEPARATE`).
- Config-driven Git URL/branch via `config/defaults.yaml` + optional `CONFIG_PROFILE`.
- OS packages script, optional ipmitool wrapper, optional noredist, Maven `developer,systemvm` + `simulator`, JUnit collection from Surefire.
- Local checks: `scripts/validate-local.sh`.
- Optional agent image: `docker/Dockerfile.agent`.

**Your checklist to “done” for Phase 1:**

1. Jenkins: Pipeline + Git + Pipeline Utility Steps (`readYaml`); agent with JDK 17, Maven, RAM/disk.
2. One successful run with your real `defaults.yaml` / profile and `SKIP_TESTS=true`, then decide on `SKIP_TESTS=false`.
3. Optional: bake deps into the Docker agent and disable `INSTALL_APT_DEPS` on agents without sudo.

## Phase 2 — Marvin / simulator integration (next)

**Goal:** Run a subset (or matrix) of `test/integration/` against management server in simulator mode, aligned with upstream `ci.yml` patterns.

**Planned work (not implemented yet):**

- MySQL service, DB deploy goals, Marvin wheel install, Jacoco (optional), `nosetests` with xUnit output.
- Jenkins **matrix** or parallel branches for test bundles; timeouts and log archival (`MarvinLogs`).
- Extend `config/defaults.yaml` (or a dedicated `config/marvin.yaml`) for Python version, test lists, `MAVEN_OPTS`, zone config paths.
- Either new stages in a second `Jenkinsfile` (e.g. `Jenkinsfile.marvin`) or the same repo with `PIPELINE_PHASE=marvin` wired to those stages.

**Prerequisite:** Phase 1 green on the same (or larger) agent class.

## Phase 3 — CD (delivery / deploy)

**Goal:** Promote built artifacts (DEB/RPM, images, internal packages) to repositories and optionally to environments with approvals.

**Planned work (not implemented yet):**

- Package or image build stage; push to internal registry/artifact server (credentials via Jenkins).
- Promotion model: dev → staging → prod; manual `input` or external approval gate.
- Rollback notes and config per environment (separate YAML or Jenkins credentials).

**Prerequisite:** Stable artifact identity from Phase 1 (and usually test confidence from Phase 2).

---

## Jenkins parameter `PIPELINE_PHASE`

| Value       | Behavior |
|------------|----------|
| `build-ontap-fast` | Phase 1a: ONTAP `-pl -am test` via `mvn-ontap-fast.sh`. |
| `build-only` | Phase 1: full `mvn clean install`. |
| `preflight`  | Phase -1: runs `validate-local.sh` only; skips Maven and CloudStack tree validation. |
| `marvin`     | Fails fast until Phase 2 is implemented (reserved). |
| `delivery`   | Fails fast until Phase 3 is implemented (reserved). |

This keeps one job definition while you extend the repo over time.
