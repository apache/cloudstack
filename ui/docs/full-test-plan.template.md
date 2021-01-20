---
name: Full Test Plan
about: Create a high level full-test plan
title: "[TESTPLAN] Full Test Plan for $Version for $Role, $Hypervisor, ACS $Version"
labels: testing

---

Note: for User role test exclude after Account/User feature, for DomainAdmin role exclude after Infrastructure (except for Offerings)

**Common**
- [ ] Project selector
- [ ] Language selector
- [ ] Notifications / clear notifications
- [ ] Paginations
- [ ] Profile
- [ ] Help
- [ ] Logout
- [ ] Context-sensitive help

**Dashboard**
- [ ] Fetch latest (only on Admin dashboard)
- [ ] View hosts in alert state
- [ ] View alerts
- [ ] View events

**Compute > Instances**
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Links
- [ ] Filter by
- [ ] Create new instance

**Compute > Kubernetes**
This requires configuring and setting up CKS: http://docs.cloudstack.apache.org/en/latest/plugins/cloudstack-kubernetes-service.html
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Links
- [ ] Filter by
- [ ] Add Kubernetes cluster
- [ ] Start/stop a Kubernetes cluster
- [ ] Scale Kubernetes cluster
- [ ] Upgrade Kubernetes cluster
- [ ] Delete Kubernetes cluster

**Compute > Instances > selected instance**
- [ ] View console
- [ ] Reboot instance
- [ ] Update instance
- [ ] Start/Stop instance
- [ ] Reinstall instance
- [ ] Take snapshot
- [ ] Assign VM to backup offering
- [ ] Attach ISO
- [ ] Scale VM
- [ ] Migrate instance to another host
- [ ] Change affinity
- [ ] Change service offering
- [ ] Reset Instance Password
- [ ] Assign Instance to Another Account (VM must be stopped)
- [ ] Network adapters
- [ ] 	- Add network to VM
- [ ] 	- Set default NIC
- [ ] 	- Add/delete secondary IP address
- [ ] 	- Delete VM network
- [ ] Settings
- [ ] 	- Add setting
- [ ] 	- Update setting
- [ ] 	- Delete setting
- [ ] Add / delete comment
- [ ] Add / delete tags
- [ ] Links

**Compute > Instance groups**
- [ ] Search
- [ ] Sort
- [ ] Links
- [ ] New instance group

**Compute > Instance groups > selected instance group**
- [ ] Links
- [ ] Update instance group
- [ ] Delete instance group

**Compute > SSH Key Pairs**
- [ ] Search
- [ ] Sorting
- [ ] Links
- [ ] New SSH key pair

**Compute > SSH Key Pairs > selected SSH key pair**
- [ ] Links
- [ ] Delete SSH key pair

**Compute > Affinity Groups**
- [ ] Search
- [ ] Sort
- [ ] Links
- [ ] New affinity group

**Compute > Affinity Groups > selected affinity group**
- [ ] Links
- [ ] Delete affinity group

**Storage > Volumes**
- [ ] Basic earch
- [ ] Extended search
- [ ] Sort
- [ ] Links
- [ ] Create volume
- [ ] Upload local volume
- [ ] Upload volume from URL

**Storage > Volumes > selected volume**
- [ ] Detach volume
- [ ] Take snapshot
- [ ] Recurring snapshot
- [ ] Resize volume
- [ ] Migrate volume
- [ ] Download volume
- [ ] Delete volume
- [ ] Links
- [ ] Add/delete tags

**Storage > Snapshots**
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Links

**Storage > Snapshots > selected snapshot**
- [ ] Links
- [ ] Add/delete tags
- [ ] Create template
- [ ] Create volume
- [ ] Revert snapshot
- [ ] Delete snapshot

**Storage > VM Snapshots**
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Links

**Storage > VM Snapshots > selected snapshot**
- [ ] Links
- [ ] Add/delete tags
- [ ] Revert VM snapshot
- [ ] Delete VM snapshot

**Storage > Backups**
- [ ] Import offering
- [ ] Configure backup provider (Veeam)
- [ ] Create backup offering
- [ ] Assign VM to backup offering
- [ ] Revert to backup
- [ ] Delete backup

**Network > Guest networks**
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Links
- [ ] Add network

**Network > Guest networks > selected network**
- [ ] Links
- [ ] Add/delete tags
- [ ] Update network
- [ ] Restart network
- [ ] Delete network
- [ ] Acquire new IP (only for isolated networks)
- [ ] Replace ACL list(only for VPC isolated networks)
- [ ] Delete public IP address (only for isolated networks)
- [ ] Add/delete egress rule (only for isolated networks)

**Network > VPC **
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Links
- [ ] Add VPC

**Network > VPC > selected VPC**
- [ ] Links
- [ ] Update VPC
- [ ] Restart VPC
- [ ] Delete VPC
- [ ] Networks
- [ ] - Links
- [ ] - Paginations
- [ ] - Add network
- [ ] - Add internal LB
- [ ] Public IP addresses
- [ ] - Links
- [ ] - Pagination
- [ ] - Select tier
- [ ] - Acquire new IP
- [ ] - Delete IP address
- [ ] Network ACL Lists
- [ ] - Links
- [ ] - Pagination
- [ ] - Add network ACL list
- [ ] Private Gateways
- [ ] - Links
- [ ] - Pagination
- [ ] - Add private gateway
- [ ] VPN Gateway
- [ ] - Links
- [ ] VPN Connections
- [ ] - Links
- [ ] - Pagination
- [ ] - Create Site-to-site VPN connection
- [ ] Virtual routers
- [ ] - Links
- [ ] Add/delete tags

**Network > Security groups**
- [ ] Search
- [ ] Sort
- [ ] Links
- [ ] Add security group

**Network > Security groups > selected security group**
- [ ] Links
- [ ] Add/delete tags
- [ ] Add ingress rule by CIDR
- [ ] Add ingress rule by Account
- [ ] Ingress rule - add/delete tags
- [ ] Ingress rule - delete
- [ ] Add egress rule by CIDR
- [ ] Add egress rule by Account
- [ ] Egress rule - add/delete tags
- [ ] Egress rule - delete
- [ ] Ingress/egress rules pagination

**Network > Public IP Addresses**
- [ ] Search
- [ ] Sort
- [ ] Links
- [ ] Acquire new IP

**Network > Public IP Addresses > selected IP address**
- [ ] Links
- [ ] Add/delete tags
- [ ] Enable/Disable static NAT
- [ ] Release IP
- [ ] Firewall - add rule
- [ ] Firewall rule - add/delete tags
- [ ] Firewall rule - delete
- [ ] VPN - Enable/Disable VPN
- [ ] VPN - Manage VPN Users

**Network > VPN Users**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add VPN user

**Network > VPN Users > selected VPN user**
- [ ] Links
- [ ] Delete VPN User

**Network > VPN Customer Gateway**
- [ ] Links
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Add VPN Customer Gateway

**Network > VPN Customer Gateway > selected gateway**
- [ ] Links
- [ ] Edit VPN Customer Gateway
- [ ] Delete VPN Customer Gateway
- [ ] Add/delete tags

**Images > Templates**
- [ ] Links
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Change order (move to the top/bottom, move one row up/down)
- [ ] Register template
- [ ] Upload local template

**Images > Templates > selected template**
- [ ] Links
- [ ] Add/delete tags
- [ ] Edit template
- [ ] Copy template
- [ ] Update template permissions
- [ ] Delete template
- [ ] Download template
- [ ] Zones pagination
- [ ] Settings - add/edit/remove setting

**Images > ISOs**
- [ ] Links
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Change order (move to the top/bottom, move one row up/down)
- [ ] Register ISO
- [ ] Upload local ISO

**Images > ISOs > selected ISO**
- [ ] Links
- [ ] Add/delete tags
- [ ] Edit ISO
- [ ] Download ISO
- [ ] Update ISO permissions
- [ ] Copy ISO
- [ ] Delete ISO
- [ ] Zones - pagination

**Images > Kubernetes ISOs**
- [ ] Links
- [ ] Basic search
- [ ] Sort
- [ ] Refresh
- [ ] Pagination
- [ ] Enable/Disable
- [ ] Add Kubernetes Version

**Projects**
- [ ] Links
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Switch to project
- [ ] New project
- [ ] Enter token
- [ ] Project invitations

**Projects > selected project**
- [ ] Links
- [ ] Add/delete tags
- [ ] Edit project
- [ ] Suspend/Activate project
- [ ] Add account to project
- [ ] Accounts - Make account project owner
- [ ] Accounts - Remove account from project
- [ ] Delete project
- [ ] Accounts - pagination
- [ ] Resources - edit

**Events**
- [ ] Links
- [ ] Basic search
- [ ] Extended search
- [ ] Sort
- [ ] Archive event
- [ ] Delete event

**Events > selected event**
- [ ] Links
- [ ] Archive event
- [ ] View event timeline
- [ ] Delete event

**Users**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add user

**Accounts**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add account
- [ ] Add LDAP account

**Accounts > selected account**
- [ ] Links
- [ ] Update account
- [ ] Update resource count
- [ ] Disable/enable account
- [ ] Lock/unlock account
- [ ] Add certificate
- [ ] Delete account
- [ ] Settings

**Users > selected user**
- [ ] Links
- [ ] Edit user
- [ ] Change password
- [ ] Generate keys
- [ ] Disable/enable user
- [ ] Delete user
- [ ] Copy API Key
- [ ] Copy Secret Key

**Domains**
- [ ] Search
- [ ] Expand/collapse
- [ ] Add/delete note
- [ ] Add domain
- [ ] Edit domain
- [ ] Delete domain
- [ ] Update resource count
- [ ] Link domain to LDAP Group/OU
- [ ] Settings

**Roles**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Create role

**Roles > selected role**
- [ ] Edit role
- [ ] Delete role
- [ ] Rules - add new rule
- [ ] Rules - modify rule
- [ ] Rules - delete rule
- [ ] Rules - change rules order

**Infrastructure > Summary**
- [ ] Links
- [ ] Setup SSL certificate

**Infrastructure > Zones**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Pagination
- [ ] Add zone

**Infrastructure > Zones > selected zone**
- [ ] Links
- [ ] Edit zone
- [ ] Enable/disable zone
- [ ] Enable/disable out-of-band management
- [ ] Enable HA (disable?)
- [ ] Add VMWare datacenter
- [ ] Delete zone
- [ ] Settings - edit

**Infrastructure > Pods**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add Pod

**Infrastructure > Pods > selected Pod**
- [ ] Links
- [ ] Dedicate/Release Pod
- [ ] Edit Pod
- [ ] Disable/enable Pod
- [ ] Delete Pod

**Infrastructure > Clusters**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add Cluster

**Infrastructure > Clusters > selected cluster**
- [ ] Links
- [ ] Dedicate/Release cluster
- [ ] Enable/disable cluster
- [ ] Manage/unmanage cluster
- [ ] Enable/disable out-of-band management
- [ ] Enable/disable HA
- [ ] Configure HA
- [ ] Delete cluster
- [ ] Settings - edit

**Infrastructure > Hosts**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add host

**Infrastructure > Hosts > selected host**
- [ ] Links
- [ ] Add/delete notes
- [ ] Dedicate/release host
- [ ] Edit host
- [ ] Force reconnect
- [ ] Disable/enable host
- [ ] Enable/cancel maintenance mode
- [ ] Enable/disable out-of-band management
- [ ] Enable/disale HA
- [ ] Delete host (only if disabled)

**Infrastructure > Primary Storage**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add Primary storage

**Infrastructure > Primary Storage > selected primary storage**
- [ ] Links
- [ ] Edit primary storage
- [ ] Enable/cancel maintenance mode
- [ ] Delete primary storage
- [ ] Settings - edit

**Infrastructure > Secondary Storage**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add Secondary storage

**Infrastructure > Secondary Storage > selected secondary storage**
- [ ] Links
- [ ] Delete secondary storage
- [ ] Settings - edit

**Infrastructure > System VMs**
- [ ] Links
- [ ] Search
- [ ] Sort

**Infrastructure > System VMs > selected system VM**
- [ ] Links
- [ ] View console
- [ ] Start/Stop system VM
- [ ] Reboot system VM
- [ ] Change service offering
- [ ] Migrate system VM
- [ ] Run diagnostics
- [ ] Get diagnostics data
- [ ] Destroy system VM

**Infrastructure > Virtual routers**
- [ ] Links
- [ ] Search
- [ ] Sort

**Infrastructure > Virtual routers > selected virtual router**
- [ ] Links
- [ ] View console (running)
- [ ] Start/Stop router
- [ ] Reboot router
- [ ] Change service offering
- [ ] Migrate router (running)
- [ ] Run diagnostics (running)
- [ ] Get diagnostics data
- [ ] Destroy router

**Infrastructure > Internal LB VMs**
- [ ] Links
- [ ] Search
- [ ] Sort

**Infrastructure > Internal LB VMs > selected internal LB VM**
- [ ] Links
- [ ] View console
- [ ] Stop router
- [ ] Migrate router

**Infrastructure > CPU Sockets**
- [ ] Search
- [ ] Sort

**Infrastructure > Management servers**
- [ ] Links
- [ ] Search
- [ ] Sort

**Infrastructure > Management servers > selected management server**

**Infrastructure > Alerts**
- [ ] Links
- [ ] Search
- [ ] Sort

**Infrastructure > Alerts > selected alert**
- [ ] Archive alert
- [ ] Delete alert

**Offerings > Compute offerings**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Add offering

**Offerings > Compute offerings > selected offering**
- [ ] Links
- [ ] Edit offering
- [ ] Update offering access
- [ ] Delete offering

**Offerings > System offerings**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Change order (move to the top/bottom, move one row up/down)
- [ ] Add offering

**Offerings > System offerings > selected offering**
- [ ] Edit offering
- [ ] Delete offering

**Offerings > Disk offerings**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Change order (move to the top/bottom, move one row up/down)
- [ ] Add offering

**Offerings > Disk offerings > selected offering**
- [ ] Links
- [ ] Edit offering
- [ ] Update offering access
- [ ] Delete offering

**Offerings > Backup offerings**

**Offerings > Network offerings**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Change order (move to the top/bottom, move one row up/down)
- [ ] Add offering

**Offerings > Network offerings > selected offering**
- [ ] Edit offering
- [ ] Enable/Disable offering
- [ ] Update offering access
- [ ] Delete offering

**Offerings > VPC offerings**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Change order
- [ ] Add offering

**Offerings > VPC offerings > selected offering**
- [ ] Links
- [ ] Add / delete tags
- [ ] Edit offering
- [ ] Enable/Disable offering
- [ ] Update offering access
- [ ] Delete offering

**Configuration > Global settings**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Edit value

**Configuration > LDAP Configuration**
- [ ] Links
- [ ] Search
- [ ] Sort
- [ ] Configure LDAP

**Configuration > LDAP Configuration > selected LDAP configuration**
- [ ] TBD

**Configuration > Hypervisor capabilities**
- [ ] Data
- [ ] Search
- [ ] Sort
