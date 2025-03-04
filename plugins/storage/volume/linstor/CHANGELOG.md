# Changelog

All notable changes to Linstor CloudStack plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2025-02-21]

### Fixed

- Always try to delete cs-...-rst resource before doing a snapshot backup

## [2025-01-27]

### Fixed

- Use of multiple primary storages on the same linstor controller

## [2025-01-20]

### Fixed

- Volume snapshots on zfs used the wrong dataset path to hide/unhide snapdev

## [2024-12-19]

### Added
- Native CloudStack encryption support

## [2024-12-13]

### Fixed

- Linstor heartbeat check now also ask linstor-controller if there is no connection between nodes

## [2024-12-11]

### Fixed

- Only set allow-two-primaries if a live migration is performed

## [2024-10-28]

### Fixed

- Disable discard="unmap" for ide devices and qemu < 7.0
  https://bugzilla.redhat.com/show_bug.cgi?id=2029980

## [2024-10-14]

### Added

- Support for ISO direct download to primary storage

## [2024-10-04]

### Added

- Enable qemu discard="unmap" for Linstor block disks

## [2024-08-27]

### Changed

- Allow two primaries(+protocol c) is now set on resource-connection level instead of rd
