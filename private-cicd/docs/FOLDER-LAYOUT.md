# private-cicd folder layout

```text
private-cicd/
├── Jenkinsfile                 # Main pipeline (preflight, build-only, build-ontap-fast, …)
├── README.md
├── config/
│   ├── defaults.yaml           # Git URL, profiles
│   ├── build-fast.yaml         # ONTAP -pl -am settings
│   ├── marvin.yaml             # Phase 2 Marvin settings
│   └── qa.yaml.example         # Team override template
├── docker/
│   └── Dockerfile.agent        # Optional Jenkins agent image
├── docs/
│   ├── BRANCH-STRATEGY.md      # Option A: fork vs upstream PRs
│   ├── FOLDER-LAYOUT.md        # This file
│   └── IMPLEMENTATION-PHASES.md
├── marvin/
│   ├── bundles.txt             # Named test lists
│   ├── README.md
│   └── zones/                  # Zone cfg templates (secrets not committed)
├── scripts/
│   ├── install-build-deps-ubuntu.sh
│   ├── setup-ipmitool-wrapper.sh
│   ├── validate-local.sh
│   ├── mvn-ontap-fast.sh       # ONTAP compile + JUnit
│   ├── mvn-full.sh             # Full mvn install
│   └── marvin-run.sh           # Phase 2 (stub)
└── test/                       # (none — Marvin tests under CloudStack test/integration/plugins/ontap/)
```

CloudStack product paths used by CI:

| Path | Role |
|------|------|
| `plugins/storage/volume/ontap/` | ONTAP plugin source + JUnit |
| `test/integration/plugins/ontap/` | Marvin tests |
