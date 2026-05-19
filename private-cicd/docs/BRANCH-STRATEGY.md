# Branch strategy (Option A — private-cicd on the fork)

`private-cicd/` is **committed** on NetApp integration branches (e.g. `dev_branch`, `netapp/main`). It is **not** included in pull requests to `apache/cloudstack`.

## Recommended workflow

1. **Integration branch** (`dev_branch`, `netapp/main`) — contains `private-cicd/` and ONTAP plugin work.
2. **Upstream PR branch** — create from `apache/cloudstack` (or rebase) **without** `private-cicd/` commits.
3. **Jenkins** — multibranch on the fork; **Script Path** = `private-cicd/Jenkinsfile`; `CLONE_SEPARATE` = false.

## Before opening an Apache PR

```bash
# Ensure private-cicd is not in the commits you push upstream
git log --oneline apache/main..HEAD -- private-cicd/
```

If those commits appear, recreate the PR branch from upstream and cherry-pick only product changes.

## Files that may stay local only

- `config/qa.yaml` (copy from `config/qa.yaml.example`)
- `marvin/zones/*.cfg` (non-`.example` configs with secrets)

Optional: add `config/qa.yaml` and `marvin/zones/*.cfg` to `.gitignore` while keeping `private-cicd/` tracked.
