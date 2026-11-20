# Design: Per-Bucket Object Storage Credentials and Key Rotation

## Status

Draft — seeking maintainer/community feedback.

This document proposes a design for per-bucket object storage
credentials and credential rotation. No implementation is included
in this PR.

## 1. Problem

Currently, object-storage credentials are provisioned at the
CloudStack account level.

When multiple buckets belong to the same account, the buckets can
therefore use the same object-storage credentials.

This creates several problems:

- A leaked credential can provide access beyond a single bucket.
- A credential cannot safely be delegated to an application for
  only one bucket.
- Rotating credentials for one bucket can affect other buckets.
- There is no bucket-level credential rotation/revocation API.

The credentials are intended for external clients such as backup
tools, CI/CD systems, and third-party applications, so a bucket-level
security boundary is desirable.

## 2. Current Architecture

The current bucket creation flow is approximately:

CreateBucket API
    |
    v
BucketApiServiceImpl
    |
    v
ObjectStoreEntity
    |
    v
ObjectStoreDriver
    |
    +-- Ceph
    +-- MinIO
    +-- Cloudian
    +-- Simulator

`BucketVO` already contains:

- `accessKey`
- `secretKey`

However, the current provider implementations populate these
credentials from the account-level object-store identity.

The existing object-store abstraction also contains:

`createUser(accountId)`

which ultimately delegates to the provider driver.

## 3. Goals

The proposed design should:

1. Give each bucket its own object-store credential scope.
2. Support two credential slots for safe rotation.
3. Allow credentials to be explicitly revoked.
4. Preserve existing bucket behavior after upgrade.
5. Provide an explicit migration path for existing buckets.
6. Keep provider-specific implementation behind the object-store
   abstraction.
7. Protect credential values as sensitive API information.

## 4. Non-Goals

This proposal does not attempt to introduce:

- A general CloudStack IAM system.
- General-purpose service accounts.
- Identities independent of buckets.
- A replacement for CloudStack account/user authentication.
- Unrelated changes to bucket versioning, encryption, quota, etc.

## 5. Proposed Credential Model

Following the two-key approach discussed in the proposal, each bucket
should be able to have two independently managed credential slots.

Conceptually:

Bucket
 |
 +-- Credential 1
 |
 +-- Credential 2

The two credentials allow one credential to be replaced while the
other remains usable during rotation.

Each slot needs enough information to represent its credential and
lifecycle state.

The exact database representation and lifecycle state model are
intentionally left open for maintainer review.

## 6. Rotation

The intended rotation flow is:

1. Existing credential remains usable.
2. Generate a replacement credential in the second slot.
3. Client changes to the new credential.
4. Old credential is explicitly revoked.

For example:

Credential A: active
Credential B: unused

        |
        | rotate
        v

Credential A: active
Credential B: active

        |
        | client switches to B
        v

Credential A: revoked
Credential B: active

This avoids requiring clients to change credentials at exactly the
same time that the old credential is invalidated.

## 7. Revocation

A credential should be explicitly revocable.

The operation must invalidate the credential at the backing
object-storage provider.

The behavior when attempting to revoke the final usable credential
needs maintainer agreement.

## 8. API

The design requires bucket-level operations for:

- retrieving credentials
- creating/initializing credentials
- rotating credentials
- revoking credentials

The exact CloudStack API command names and parameters should follow
existing API conventions and be finalized during review.

Credential responses must use CloudStack's existing sensitive-data
handling conventions.

## 9. Persistence

The existing bucket model contains one access-key/secret-key pair.

Supporting two independent credential slots requires a persistence
change.

Two approaches are possible:

### Option A — Extend BucketVO

Add fields for the second credential and required lifecycle state.

### Option B — Separate bucket credential entity

Introduce a separate bucket-credential model associated with the
bucket.

The preferred option should be decided after maintainer feedback,
taking future credential lifecycle requirements into account.

## 10. Backward Compatibility

Existing buckets should continue to work after upgrading CloudStack.

The proposed compatibility model is:

Existing bucket
    |
    +-- bucket-specific credentials?
          |
          +-- yes -> use bucket credentials
          |
          +-- no  -> retain existing account-level behavior

Existing buckets should not be forcibly migrated during database
upgrade.

## 11. Migration

Existing buckets should have an explicit migration mechanism.

A migration would:

1. Create the bucket-specific backing identity.
2. Create the required credential(s).
3. Associate the credential with the bucket.
4. Preserve service availability during migration.

The exact individual/bulk migration mechanism should be finalized
during design review.

## 12. Provider Changes

The common object-store abstraction will need operations for the
bucket credential lifecycle.

### Ceph

Use the existing object-store user/access-key primitives to create
a bucket-specific backing identity and manage its credentials.

### MinIO

Implement the equivalent bucket-specific credential lifecycle using
the capabilities supported by the MinIO provider.

### Cloudian

Adapt the current account-level credential behavior to support
bucket-specific identities/credentials and the required rotation
operations.

### Simulator

Provide a corresponding implementation sufficient for automated
testing.

## 13. Security

The implementation must:

- enforce bucket authorization;
- treat credentials as sensitive API data;
- never log secret values;
- avoid exposing credentials through debug/toString output;
- prevent one bucket's credential from automatically granting access
  to another bucket;
- correctly reflect provider-side revocation.

## 14. Testing

Testing should cover:

### Credential creation

Create bucket → create bucket-specific credential.

### Isolation

Credential A → Bucket A: allowed

Credential A → Bucket B: denied

### Rotation

Old credential works → create new credential → client changes →
revoke old credential → new credential works.

### Failure handling

Failed credential creation/rotation must not destroy the currently
working credential.

### Backward compatibility

Existing buckets without bucket-specific credentials continue to
work using the existing behavior.

### Migration

Existing bucket → migrate → bucket-specific credential works.

Provider-specific integration testing should be performed where the
provider can be run in the available test environment.

## 15. Implementation Plan

Implementation should begin only after the design is reviewed.

Proposed order:

1. Finalize design/API/persistence decisions.
2. Implement common credential model and abstraction.
3. Implement Ceph.
4. Implement MinIO.
5. Implement Cloudian.
6. Implement API/service layer.
7. Implement migration/upgrade support.
8. Add UI support if required.
9. Complete unit/integration/provider testing.

The final PR split should be discussed with maintainers.

## 16. Open Questions

Before implementation, feedback is requested on:

1. Should bucket-specific credentials be the default for new buckets,
   or initially opt-in?
2. Should credentials live directly in `BucketVO` or in a separate
   credential entity?
3. What exact credential states are required?
4. What should happen when the final active credential is revoked?
5. Should rotation always create the replacement credential in the
   inactive slot?
6. Should secrets be returned only when newly generated?
7. Should migration support individual buckets, bulk migration, or
   both?
8. What should happen when a provider cannot support an operation?
9. What should the exact API commands/parameters be?
10. Should provider implementations be separate PRs?

## 17. Proposed Direction

Subject to maintainer feedback, the proposed direction is:

- bucket-level security boundary;
- two credential slots;
- non-disruptive rotation;
- explicit revocation;
- backward-compatible fallback for existing buckets;
- explicit migration;
- provider-specific implementation behind the common object-store
  abstraction;
- sensitive handling of credential responses.

This document is intentionally a design proposal. Implementation
should follow after the architecture and open questions have been
reviewed by the CloudStack maintainers/community.
