# Marvin assets (private CI)

- **`zones/`** — Zone / simulator configuration templates. Copy `*.example` to `*.cfg` locally or inject paths via Jenkins; do not commit secrets.
- **`bundles.txt`** — Named test lists for Jenkins matrix jobs (`bundle-name=path1,path2`).

Product Marvin tests live under CloudStack:

`test/integration/plugins/ontap/`

Those tests can be submitted upstream when appropriate; this directory stays downstream-only.
