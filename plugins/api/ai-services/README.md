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

# cloud-plugin-api-ai-services

> **This plugin is developed out of tree, at
> [kiwiflyer/CloudStack_AI_Plugin](https://github.com/kiwiflyer/CloudStack_AI_Plugin).**
>
> That repository holds the whole product: this Java plugin, the Python
> engine it talks to over mTLS, the `aiservices` UI section, the
> Terraform provider, the Marvin tests and the specification the command
> map is generated from. **Change it there, not here** — the commands,
> their parameters and their default role grants are generated from
> `aisvc/api/commands.py`, and drift between the two is what the tests
> in that repository exist to catch.
>
> The branch this file is on carries only the two pieces that have to
> live inside CloudStack's own tree: this plugin and the UI section.
> See [the branch note](../../../AI-SERVICES-BRANCH.md) for what was
> copied, what was changed to make it build here, and what has not been
> verified against this version of CloudStack.

Building and deploying this is one part of standing the product up; see
[docs/installation.md](https://github.com/kiwiflyer/CloudStack_AI_Plugin/blob/main/docs/installation.md) for the whole sequence.

The native CloudStack API plugin (SPEC.md section 9), targeting
**CloudStack 4.23.0.0** ([ADR-0003](https://github.com/kiwiflyer/CloudStack_AI_Plugin/blob/main/docs/adr/0003-cloudstack-release.md)).

It has been loaded into a real 4.23.0.0 management server: all 38 v1
commands register (`listApis` 865 → 903), twenty more with
`ai.services.v2.enabled` on, the `ai_deployment` table is created at
startup, the ten `ai.services.*` settings appear in
`listConfigurations`, and commands answer with data from the engine
over mTLS. The per-role Marvin tests in
[`test/integration/plugins/ai-services/`](https://github.com/kiwiflyer/CloudStack_AI_Plugin/tree/main/test/integration/plugins/ai-services/)
cover all 58 commands, allowed and denied, and pass.

**Loading it is how the defects were found.** A Spring context that
did not declare a bean it injected took the whole `/client` webapp
down, not just the plugin; seven write commands sent request bodies
missing exactly the parameters whose names needed no translation; a
PEM parameter was capped at `@Parameter`'s default 255 characters;
and both the sync and async paths threw away the engine's explanation
of a failure. Every one compiled and passed its unit tests.

## Building

CloudStack publishes **no Maven artifacts** — not to Maven Central, not to the ASF
repositories. The dependencies must be built from source into your local repository first:

```bash
git clone --depth 1 --branch 4.23.0.0 --single-branch https://github.com/apache/cloudstack.git
```

```bash
cd cloudstack && mvn install -DskipTests -Dcheckstyle.skip -Dlicense.skip -Drat.skip -pl api,utils,framework/config,framework/db,framework/events,engine/schema -am
```

Then, from the repository root:

```bash
make test-java
```

Needs JDK 17 to build (`brew install openjdk@17 maven`); it targets JDK 11 bytecode, which is
what CloudStack 4.23 sets as `cs.jdk.version`. `openjdk@17` is keg-only, so set
`JAVA_HOME=/opt/homebrew/opt/openjdk@17`.

## Layout

| Path | What it is |
| --- | --- |
| `AiServicesConfig` | The ConfigKeys of section 9. Enablement is zone-scoped, Hugging Face approval domain-scoped |
| `AiServicesService` / `AiServicesManagerImpl` | The `PluggableService` and `Configurable` entry point |
| `engine/EngineClient` | Calls the engine, failing over across replicas |
| `engine/HttpEngineTransport`, `engine/MutualTls` | The mTLS transport |
| `engine/CallerContext` | The identity the engine filters every response by |
| `api/command/`, `api/response/` | API commands and their responses |
| `META-INF/db/schema-ai-services.sql` | The `ai_deployment` ownership table |

## Generated code

Thirty-six of the thirty-eight v1 commands, the twenty v2 ones, and twenty-one
response types differ only in name, verb, engine path, parameters and response shape.
(`deployAiModel` and `deleteAiModelDeployment` are the two written by hand: they own
the `ai_deployment` row CloudStack uses for ownership and cleanup, which the generator
does not model.) Writing them out by hand invites the one transposed path or
wrong role no reviewer would spot, so the leaves are generated:

```bash
python3 tools/generate_commands.py
```

Generated files say so in their header. **Edit `tools/generate_commands.py`, not them.** The
interesting parts — the base classes, the engine client, the async bridge, the DAO — are
hand-written.

The generator also emits `src/test/resources/generated-engine-objects.json`, a fully-populated
engine object per response type, so `GeneratedSurfaceTest` exercises every mapped field
without that list being maintained twice.

## The 4.22 LTS floor

[ADR-0003](https://github.com/kiwiflyer/CloudStack_AI_Plugin/blob/main/docs/adr/0003-cloudstack-release.md) targets 4.23, which is **non-LTS**. The
plugin is held to APIs that also exist in 4.22 LTS so the eventual retarget costs a rebuild
rather than a rewrite. Verify locally by building 4.22's modules the same way and then:

```bash
mvn -Dcs.version=4.22.1.1 test
```

CI runs this on every change.

## Two rules worth keeping

**The caller context is built from `CallContext`, never from request parameters.** The engine
authenticates nobody — it is unreachable from tenant networks and requires a client
certificate — and filters every response by what this plugin asserts. A caller able to
influence it could read another tenant's data.

**The command list is generated, not hand-written.** `aisvc/api/commands.py` holds section 9's
map, and the Python tests check it against the engine's routes. Adding a command there and
forgetting it here should be caught by a test, not by a Marvin run.
