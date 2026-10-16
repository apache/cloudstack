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

# Structured Error Messages

This package (`org.apache.cloudstack.error`) and `org.apache.cloudstack.context.ResponseMessageResolver`
implement CloudStack's structured error-message framework: exceptions carry a stable key and a
metadata map instead of an ad-hoc string, so the message shown to a caller can be customized,
localized client-side, and role-aware (admins can see more detail than regular users) without
changing Java code.

Full design background and rationale: [Error Message Consistency, Customization, and
Localization Framework](https://cwiki.apache.org/confluence/spaces/CLOUDSTACK/pages/406618482/Error+Message+Consistency+Customization+and+Localization+Framework)
(Confluence). This README is the quick, in-code reference for developers touching exception
call sites; the wiki page covers the "why."

## Raising a keyed exception

Use the static factories on `Exceptions`, never construct the exception type directly with a
literal string:

```java
throw Exceptions.invalidParameterValueException("vm.deploy.template.not.found");
throw Exceptions.cloudRuntimeException("vm.stop.vm.not.found", Map.of("instance", vm));
throw Exceptions.permissionDeniedException("vm.migrate.permission.denied");
```

Available factories: `invalidParameterValueException`, `cloudRuntimeException`,
`permissionDeniedException`, `serverApiException`, `concurrentOperationException`,
`unsupportedServiceException`, each with a `(key)` and a `(key, metadata)` overload;
`cloudRuntimeException` also has a `(key, metadata, cause)` overload.

**Metadata rule**: pass the actual object (VO, TO, enum) as the metadata value whenever it's in
scope at the throw site, not a pre-extracted `.getUuid()`/`.getName()`/`.getId()` string.
`ResponseMessageResolver` reflects over the object (`getDisplayName()`/`getName()`/
`getDisplayText()`, plus UUID/ID for `Identity`/`InternalIdentity` implementers) to render it;
passing a raw string bypasses all of that. Only pass a raw id/uuid when the lookup that would
have produced the object failed (e.g. `dao.findById(id)` returned null).

**Scope**: this only works for `CloudRuntimeException`-derived (unchecked) exceptions.
Checked exceptions deriving from `CloudException` (`VirtualMachineMigrationException`,
`InsufficientCapacityException`, `ManagementServerException`, ...) are out of scope:
`CloudException` was not given `messageKey`/`metadata` fields.

## Templates: `error-messages.json`

Keys resolve to templates in `client/conf/messages/error-messages.json.in`, deployed to
`/etc/cloudstack/management/messages/error-messages.json`. Key naming:
`<actionable_resource>.<action>.<failing_resource>.<cause>`, e.g. `vm.deploy.template.not.found`.
Templates use `{{placeholder}}` substitution from the metadata map. An admin-only variant of any
key is defined by appending `.admin`, used automatically when the caller is a root admin, with
the base key falling back for everyone else.

Before adding a new key, check whether an existing one already says the same thing; reuse
before adding. `scripts/util/error_messages_tool.py` checks the file for structural errors,
duplicate keys, and keys with no code references (`unused` command).

### Plugin/operator override files

Alongside the main file, an operator or plugin may drop `error-messages-<suffix>.json` files
into the same `messages/` directory at runtime (never bundled in any package; this is purely a
runtime/environment mechanism). They're merged in on top of the main file, sorted alphabetically,
with plugin values taking precedence per-key (not per-file: a plugin overriding only a base key
leaves that key's separately-defined `.admin` variant, if any, untouched). Both the main file and
any plugin files are hot-reloaded on the next request with no management server restart.

A plugin that wants to ship its own `error-messages-<plugin>.json` (rather than relying on an
operator to hand-author one) has to get that file onto disk at
`/etc/cloudstack/management/messages/` itself; there's no code-level registration step, only a
file-placement convention. That means packaging: the plugin's packaging/install step needs to
copy the file to that path, the same way the main `error-messages.json` is packaged today. Check
the relevant packaging scripts/specs when adding a plugin-owned override file; without that
change, a file that only exists in the plugin's source tree will never reach a real deployment.

## Example API response

`ExceptionResponse` carries the legacy `errortext` alongside the new `errortextkey` and
`errormetadata` fields, e.g. from `deployVirtualMachine` hitting a resource limit:

```json
{
    "deployvirtualmachineresponse": {
        "uuidList": [],
        "errorcode": 535,
        "cserrorcode": 9999,
        "errortext": "Unable to deploy Instance because allocating 1 more Instance would exceed the Account limits. Current: 2, Reserved: 0, Limit: 2. Release unused resources, then retry.",
        "errortextkey": "vm.deploy.resourcelimit.exceeded.account",
        "errormetadata": {
            "resourceRequested": "1",
            "resourceTypeDisplay": "Instance",
            "resourceOwnerType": "Account",
            "resourceAmount": "2",
            "resourceReserved": "0",
            "resourceLimit": "2"
        }
    }
}
```

`errortextkey` and the shape of `errormetadata` are stable regardless of how `error-messages.json`
is customized; `errortext` is only the currently-resolved template rendering and shouldn't be
matched on by API clients that care about the specific error condition.

## Localizing in the UI

`error-messages.json` is a server-side concern; the UI has a separate, independent localization
path built on the same `errortextkey`. `ui/src/utils/plugins.js`'s `localeErrorUtilPlugin`
(`$toLocaleError(msg, key, params)`, called from both `$pollJob`'s async job failure handling and
`$notifyError`) looks up `key` (the `errortextkey`) in the current locale's i18n bundle
(`ui/public/locales/<locale>.json`, e.g. `hi.json`, `fr_FR.json`: flat key/value maps, same ones
used for every other UI string). If a matching entry exists, it's used instead of the
server-rendered `errortext`, with `{{placeholder}}` tokens substituted from `errormetadata`;
otherwise it falls back to `errortext` unchanged.

**Admin variant**: like the server, before trying the base key, `$toLocaleError` first tries
`<key>.admin` when the current user is a root admin (`roletype === 'Admin'`), falling back to the
base key if no such entry exists. Only this root-admin case is special-cased, matching
`error-messages.json`'s own `.admin`-suffix behavior; there's no equivalent variant for resource
admins, domain admins, or regular users.

Practically: to ship a UI-side translation for a specific error, add a key equal to its
`errortextkey` (optionally suffixed `.admin` for a root-admin-specific variant) to the relevant
locale file (e.g. `ui/public/locales/hi.json` for Hindi). No server-side change is needed.

## Global settings

Two `ConfigKey`s on `ApiServiceConfiguration` control metadata rendering, both non-dynamic
(management server restart required to change: they're read on every metadata value rendered,
so making them dynamic would add avoidable config-depot load):
- `error.message.metadata.prefer.tostring`: prefer `toString()` over reflection-based
  display-name lookup (default `false`).
- `error.message.metadata.include.id.for.admins`: include the internal DB ID (alongside the
  UUID) for admins (default `true`).

## Tests

- `api/src/test/java/org/apache/cloudstack/context/ResponseMessageResolverTest.java`: unit
  tests for resolution, placeholder substitution, admin/non-admin selection, metadata
  conversion, and the plugin-file merge/priority/collision/hot-reload behavior.
- `test/integration/smoke/test_error_message_framework.py`: a Marvin integration test that
  edits `error-messages.json` on a live management server at runtime and verifies both a
  regular user and the root admin see the right variant, with no restart.
