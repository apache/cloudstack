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

# Code conventions

Conventions reviewers check. They apply to new and changed code. Old code is
fixed only when it is being changed anyway.

## Contents

- [Repository layout](#repository-layout)
- [Size and shape of a change](#size-and-shape-of-a-change)
- [Java](#java)
- [Database objects and schema](#database-objects-and-schema)
- [API commands and responses](#api-commands-and-responses)
- [Global settings](#global-settings)
- [Logging](#logging)
- [Unit tests](#unit-tests)

## Repository layout

| Path | Contents |
| --- | --- |
| `api/` | API interfaces, `*Cmd` and `*Response` classes |
| `server/` | Manager implementations (business logic) |
| `engine/` | Orchestration; `engine/schema/` holds VO and DAO classes, schema and upgrade SQL |
| `framework/` | Config, DB, security, async jobs |
| `core/` | Shared DTOs, agent `Command` and `Answer` classes |
| `agent/` | The hypervisor-host agent |
| `plugins/` | Hypervisors, storage, network, backup, integrations such as CKS |
| `systemvm/` | System VM scripts and the virtual router's Python |
| `ui/` | Vue.js 3 and Ant Design frontend |
| `client/` | The management server webapp; include it in a build that should reach a running server |
| `test/` | Marvin integration tests |

## Size and shape of a change

| Rule | Why |
| --- | --- |
| One logical change per PR | A reviewer approves one thing |
| Write the least code that solves the task | Every line is read, tested and maintained |
| Handle only states that can occur | A branch for an impossible state is untested code and hides the real change |
| Reuse the helper that exists before writing another | Two copies drift apart |
| Deduplicate before submitting, without losing readability | Copy-pasted blocks are the most common review comment |
| A method does one thing | Extract a block that needs a comment to explain it, and name the method for what it does |
| Refactor only what the change needs | Unrelated cleanup is its own PR, or the diff becomes unreviewable |

## Java

- Code compiles at the Java level in `pom.xml` (`cs.jdk.version`), on every branch the change targets.
- Every new file starts with the Apache License 2.0 header; `pre-commit` checks it.
- Inject collaborators with `@Inject` fields, as the surrounding classes do.
- Throw `InvalidParameterValueException` for bad user input, `PermissionDeniedException` for access, and `CloudRuntimeException` for failures the caller cannot fix. The message says what was refused and what to do instead.

## Database objects and schema

| Rule | Detail |
| --- | --- |
| Name VO getters and setters after their fields | Field `foo` has `getFoo()` and `setFoo()`, so readers and tools find them |
| A VO has an empty constructor | The persistence layer instantiates it |
| A VO generates its UUID in its constructor | Every row gets one without the caller remembering |
| Table names are singular | `vm_instance`, not `vm_instances` |
| A schema change goes in the upgrade script for the next release of the target branch | `engine/schema/src/main/resources/META-INF/db/schema-<from>to<to>.sql` |
| Upgrade SQL is safe to run twice | Use the procedures in `META-INF/db/procedures/`, such as `IDEMPOTENT_ADD_COLUMN`, so a repeated upgrade does not fail |
| Change a view in its file under `META-INF/db/views/` | Every upgrade re-creates the views from these files |
| A released upgrade script is never edited | Installed databases have already run it |

## API commands and responses

- A new command, parameter or response field carries `since` with the release it first ships in.
- A subclass does not redeclare a parameter it inherits.
- Parameter and response names are lower case, as the rest of the API.
- Input is validated before anything changes, so a refused call leaves no partial state.
- Check that the caller may use the resource (its account and domain). A role that allows the API call is not enough.

## Global settings

| Rule | Why |
| --- | --- |
| Add a setting as a `ConfigKey`, never a new `Config` enum entry | `Config` is legacy |
| Declare the `ConfigKey` on the service interface, not the `*Impl` | Other modules read it without depending on the implementation |
| Return it from the class's `getConfigKeys()` | An unregistered key never reaches the database or the API |
| Pick the narrowest scope that works (account, domain, cluster, zone, global) and say whether it is dynamic | Operators can then set it per zone or account; a setting that is not dynamic needs a restart to change |
| Validate the value where it is read, or with a validator on update | A bad value otherwise surfaces as a failure far from its cause |
| Add a setting only when operators need different values | Each one is documented, tested and supported forever |

## Logging

- Use the class `logger` with `{}` placeholders and one argument per placeholder: `logger.debug("Detaching ISO {} from VM {}", iso, vm)`.
- Log the object, not a hand-built string of its fields; CloudStack objects print their identifying fields.

| Level | For |
| --- | --- |
| `DEBUG` | Routine detail, anything per object on a frequent path |
| `INFO` | A state change an operator would want in the log: a VM started, a host put in maintenance |
| `WARN` | Something went wrong and was handled |
| `ERROR` | An operation failed |

- A message logged for every NIC, volume or VM on every start is `DEBUG`.
- Never log passwords, keys, tokens or user data. Mask them in commands that are logged.

## Unit tests

- JUnit 4 with Mockito: `@RunWith(MockitoJUnitRunner.class)`, `@Mock`, `@InjectMocks` or `@Spy`. Static methods are mocked with `Mockito.mockStatic` in try-with-resources; PowerMock is not used.
- A bug fix comes with a test that fails without the fix.
- Every new branch has a test whose input reaches that branch.
- A test is named for the behavior it checks: `testScaleRejectsSizeBelowAddedNodes`, not `testFix2`.
- Verify outcomes (returned values, state, the calls that matter), not every interaction.
- A test does not depend on another test's order or leftovers.
