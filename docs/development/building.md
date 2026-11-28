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

# Building and testing

This covers what the existing guides don't. For everything else:

| Subject | Guide |
| --- | --- |
| Machine setup, full build, database, running the management server, packaging | [INSTALL.md](../../INSTALL.md) |
| UI build, development server, lint and unit tests | [ui/README.md](../../ui/README.md) |
| Lint and license checks | [PRE_COMMIT.md](../../PRE_COMMIT.md) |
| Simulator in Docker | [tools/docker/README.md](../../tools/docker/README.md) |

## Contents

- [Incremental builds](#incremental-builds)
- [Unit tests](#unit-tests)
- [Simulator from source](#simulator-from-source)
- [Build pitfalls](#build-pitfalls)

## Incremental builds

```bash
mvn install -Dnoredist -P developer -pl api,server,client -am -DskipTests
```

- `-am` also builds the modules the listed ones depend on.
- Include `client` when the change should reach a running management server; it is the module `jetty:run` serves.

## Unit tests

| Task | Command |
| --- | --- |
| One test class | `mvn test -pl server -am -Dtest=FooManagerImplTest -Dsurefire.failIfNoSpecifiedTests=false` |
| A pattern | `-Dtest='KubernetesCluster*Test'` |
| One method | `-Dtest=FooManagerImplTest#testBar` |

- Without `-am` the test runs against whatever jars are in `~/.m2`, which may be older than the source.
- `-Dsurefire.failIfNoSpecifiedTests=false` keeps the run from failing in the `-am` modules that have no matching test.
- Results are in `<module>/target/surefire-reports/`.

## Simulator from source

The simulator runs a management server against fake hypervisors, so API-level
behavior can be tested without hardware. CI runs the smoke tests this way
(`.github/workflows/ci.yml`).

1. Deploy both databases: `mvn -P developer -pl developer -Ddeploydb`, then `mvn -P developer -pl developer -Ddeploydb-simulator`.
2. Install Marvin from the build: `pip install --user tools/marvin/dist/[mM]arvin-*.tar.gz`.
3. Start the server with `mvn -Dsimulator -Djava.net.preferIPv4Stack=true -pl :cloud-client-ui jetty:run` and wait for port 8096.
4. Deploy a zone: `python3 tools/marvin/marvin/deployDataCenter.py -i setup/dev/advdualzone.cfg`.
5. Run a suite:

```bash
nosetests --with-marvin --marvin-config=setup/dev/advdualzone.cfg \
  test/integration/smoke/test_vm_life_cycle.py -s \
  -a tags=advanced,required_hardware=false --zone=zim1 --hypervisor=simulator
```

- A test tagged `required_hardware=true` needs real hypervisors: use a lab ([test-lab.md](./test-lab.md)) or ask a committer for a Trillian run on the PR.
- Some simulator smoke tests fail intermittently on `main` as well. Compare a failure with the same suite on a recent base-branch run before treating it as caused by a change.

## Build pitfalls

| Symptom | Cause and fix |
| --- | --- |
| `engine/schema` fails offline | It downloads files during the build; don't build with `-o` |
| A test passes locally and fails in CI, or the reverse | Stale module jars in `~/.m2`; rebuild with `-am` |
| A build in a `git worktree` fails reading git metadata | Add `-Dmaven.gitcommitid.skip=true` |
| Out of memory with `-T` | Each thread runs its own compiler; lower `-T` or raise `MAVEN_OPTS=-Xmx` |
