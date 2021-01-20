---
name: Smoke Test Plan
about: Create a smoke test plan for a release
title: "[TESTPLAN] Smoketest for $VERSION with $Role, $Hypervisor and ACS $Version"
labels: testing

---

Note: for User role test exclude after Account/User feature, for DomainAdmin role exclude after Infrastructure (except for Offerings)

**Instances**
- [ ] Create instance using template
- [ ] Create instance using ISO image and different parameters than the previous one
- [ ] Test all VM actions - Start/Stop/Reboot/Reinstall/Update, etc
- [ ] Add/modify/delete VM setting
- [ ] Add network to VM, change IP address, make it default, delete
- [ ] Add/delete secondary IP address

**Compute > Kubernetes**
This requires configuring and setting up CKS: http://docs.cloudstack.apache.org/en/latest/plugins/cloudstack-kubernetes-service.html
- [ ] Add Kubernetes cluster
- [ ] Start/stop a Kubernetes cluster
- [ ] Scale Kubernetes cluster
- [ ] Upgrade Kubernetes cluster
- [ ] Delete Kubernetes cluster

**Add Instance groups**
- [ ] Add/modify/delete instance group

**SSH Key Pairs**
- [ ] Add/delete SSH key pair

**Affinity Groups**
- [ ] Add/delete host affinity group
- [ ] Add/delete host anti-affinity group

**Volumes**
- [ ] Create volume
- [ ] Upload local volume
- [ ] Upload volume from URL
- [ ] Volume actions - snapshots, resize, migrate, download, create template

**Snapshots**
- [ ] Snapshot actions - create template/volume, revert, delete

**VM Snapshots**
- [ ] VM Snapshot actions - revert, delete

**Backups**

**Guest networks**
- [ ] Add isolated network
- [ ] Add L2 network
- [ ] Add shared network
- [ ] Network actions - update, restart, replace ACL list, delete
- [ ] Add/delete egress rules
- [ ] Acquire IP address

**VPC**
- [ ] Add VPC
- [ ] VPC actions - updat, restart, delete
- [ ] Add security group
- [ ] Add/delete ingress/egress rule

**Public IP Addresses**
- [ ] Acquire new IP
- [ ] Actions - enable static NAT, release IP, enable VPN

**Templates**
- [ ] Register template
- [ ] Upload local template
- [ ] Template actions - edit, download, update permissions, copy, delete

**ISOs**
- [ ] Register ISO
- [ ] Upload local ISO
- [ ] ISO actions - edit, download update permissions, copy, delete

**Events**
- [ ] Search, archive, delete

**Projects**
- [ ] Add project
- [ ] Project actions - edit, suspend, add account, delete
- [ ] Different projects with different permission

**Accounts, users, roles**
- [ ] Create/modify/check role/delete regular user account
- [ ] Create/modify/check role/delete resource admin account
- [ ] Create/modify/check role/delete domain admin account
- [ ] Create/modify/check role/delete admin user
- [ ] Account actions - edit, disable, lock, delete

**Domains**
- [ ] Create new domain
- [ ] Create subdomain in the new domain
- [ ] Delete the first domain (2nd, not 3rd level)
- [ ] Edit/delete domain
- [ ] Modify domain limits/settings

**Roles**
- [ ] Add new role
- [ ] Role actions - edit, delete

**Infrastructure summary**

**Zones**
- [ ] Add zone
- [ ] Zone actions - edit, enable/disable, enable/disable HA, delete, etc.
- [ ] Modify settings

**Pods**
- [ ] Add pod
- [ ] Pod actions - edit, enable/disable, delete

**Clusters**
- [ ] Add cluster
- [ ] Cluster actions - enable/disable, unmanage, enable/disable HA, delete, etc

**Hosts**
- [ ] Add host
- [ ] Host actions - edit, enable/disable, maintenance mode, enable/disable/configure HA, etc.

**Primary storage**
- [ ] Add primary storage
- [ ] Primary storage actions - edit, enable/disable maintenance mode
- [ ] Settings - modify

**Secondary storage**
- [ ] Add secondary storage
- [ ] Delete secondary storage
- [ ] Settings - modify

**Compute offering**
- [ ] Add shared thin compute offering
- [ ] Add local fat compute offering
- [ ] Offering actions - edit, access, delete

**System offering**
- [ ] Add shared thin system offering for VR
- [ ] Add local sparse system offering for console proxy
- [ ] Offering actions - edit, delete

**Disk offering**
- [ ] Add shared thin disk offering
- [ ] Add local fat disk offering
- [ ] Offering actions - edit, access, delete

**Backup offering**
- [ ] Import offering
- [ ] Configure backup provider (Veeam)
- [ ] Create backup offering
- [ ] Assign VM to backup offering
- [ ] Revert to backup
- [ ] Delete backup
**Network offering**
- [ ] Add isolated network with some supported services
- [ ] Add L2 network
- [ ] Add shared network with some supported services
- [ ] Network actions - edit, enable/disable, access, delete

**VPC offering**
- [ ] Change VPC offerings order
- [ ] Add new VPC offering with some supported services
- [ ] VPC offering actions - edit, enable/disable, access, delete

**Global settings**
- [ ] Search setting
- [ ] Modify setting

**LDAP configuration**
- [ ] Add LDAP configuration
- [ ] Login with LDAP account

**Common functionality**
- [ ] Sorting
- [ ] Pagination
- [ ] Searching
- [ ] Add/remove tags
- [ ] Refresh
- [ ] Links
