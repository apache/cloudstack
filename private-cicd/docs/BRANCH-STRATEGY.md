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
