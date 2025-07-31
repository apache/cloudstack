
# Apache CloudStack: Effortless Cloud Management at Scale

[![Build Status](https://github.com/apache/cloudstack/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/build.yml) [![UI Build](https://github.com/apache/cloudstack/actions/workflows/ui.yml/badge.svg)](https://github.com/apache/cloudstack/actions/workflows/ui.yml) [![License Check](https://github.com/apache/cloudstack/actions/workflows/rat.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/rat.yml) [![Simulator CI](https://github.com/apache/cloudstack/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/ci.yml) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_cloudstack&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_cloudstack) [![codecov](https://codecov.io/gh/apache/cloudstack/branch/main/graph/badge.svg)](https://codecov.io/gh/apache/cloudstack)

[![Apache CloudStack](tools/logo/apache_cloudstack.png)](https://cloudstack.apache.org/)

**Apache CloudStack is an open-source, production-ready platform to build, deploy, and manage highly available, scalable, and secure private, public, or hybrid cloud infrastructure.**

Designed for simplicity without sacrificing power, CloudStack delivers a comprehensive Infrastructure-as-a-Service (IaaS) solution. Its intuitive UI, rich API, and seamless multi-hypervisor support put complete cloud control in your hands.

Whether you're a cloud service provider or an enterprise modernizing infrastructure, CloudStack empowers you to run your cloud your way—faster, safer, and smarter.

For more information on Apache CloudStack, please visit the [website](http://cloudstack.apache.org)
---


## Table of Contents

1. [Key Features at a Glance](#key-features-at-a-glance)
2. [Who Uses CloudStack?](#who-uses-cloudstack)
3. [Demo](#demo)
4. [Try CloudStack in Minutes](#try-cloudstack-in-minutes)
  
5. [Source Code](#source-code)
6. [Getting Source Repository](#getting-source-repository)
7. [Documentation](#documentation)
8. [Ecosystem & Integrations](#ecosystem--integrations)
9. [Project Status & Release Cycle](#project-status--release-cycle)
10. [News and Events](#news-and-events)
11. [Getting Involved & Contributing](#getting-involved--contributing)
    - [How to Contribute](#how-to-contribute)
    - [Mailing Lists](#mailing-lists)
    - [Issue Tracker](#issue-tracker)
12. [License & Security Policy](#license--security-policy)
    
13. [Notice of Cryptographic Software](#notice-of-cryptographic-software)
14. [Star History](#star-history)
15. [Contributors](#contributors)


---

##  Key Features at a Glance

* **Multi-Hypervisor Management**
Control VMs across KVM, VMware vSphere, XenServer, and Hyper-V — all from one UI.
* **Built-in Network-as-a-Service (NaaS)**
Automate virtual networks, firewalls, VPNs, and load balancers without vendor lock-in.
* **Powerful API & Intuitive UI**
Seamlessly integrate via RESTful API or manage everything visually through a clean web interface.
* **Flexible Storage Architecture**
Supports primary/secondary storage, snapshots, volume management, and disaster recovery.
* **Granular User & Account Controls**
Fine-tuned multi-tenant access control for service providers and enterprise clouds.
* **Real-Time Resource Usage & Accounting**
Built-in metering for chargeback, quota management, and capacity planning.

---

## Who Uses CloudStack?

* There are more than 150 known organizations using Apache CloudStack (or a commercial distribution of CloudStack). Our users include many major service providers running CloudStack to offer public cloud services, product vendors who incorporate or integrate with CloudStack in their own products, organizations who have used CloudStack to build their own private clouds, and systems integrators that offer CloudStack related services.

* See our [case studies](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Case+Studies) highlighting successful deployments of Apache CloudStack.

* See the up-to-date list of current [users](https://cloudstack.apache.org/users.html).

* If you are using CloudStack in your organization and your company is not listed above, please complete our brief adoption [survey](https://cloudstack.apache.org/survey.html). We're happy to keep your company name anonymous if you require.

## Demo

![Screenshot](ui/docs/screenshot-dashboard.png)

See the project user-interface QA website that runs CloudStack against simulator hypervisor:
https://qa.cloudstack.cloud/simulator/ (admin:password)

---

##  Try CloudStack in Minutes

Spin up a working CloudStack instance with zero hassle. Perfect for demos, dev, or quick evaluations.

### Quick Install (All-in-One)

Get a full CloudStack environment running fast:

```bash
git clone [https://github.com/apache/cloudstack-installer.git](https://github.com/apache/cloudstack-installer.git)
cd cloudstack-installer
sudo ./cloudstack-installer.sh -i

```

Access UI: http://<your-ip>:8080/client

This setup is for testing only — not production-ready.

 

### Docker Sandbox (Community)
 

Explore the UI and APIs in a lightweight, local Docker environment:

```Bash 
git clone [https://github.com/apache/cloudstack-docker.git](https://github.com/apache/cloudstack-docker.git)
cd cloudstack-docker
docker-compose up
```
 

### Full Setup
 

For production-ready deployments and detailed configurations, refer to our:

Installation Guide
Ansible Deployment Guide

 

## Source Code
 

Join our open-source journey! Apache CloudStack's official Git repository:

    https://gitbox.apache.org/repos/asf/cloudstack.git

A convenient, read-only mirror on GitHub, perfect for exploring code and submitting contributions:

    https://github.com/apache/cloudstack

## Getting Source Repository

Apache CloudStack project uses Git. The official Git repository is at:

    https://gitbox.apache.org/repos/asf/cloudstack.git

And a mirror is hosted on GitHub:

    https://github.com/apache/cloudstack

The GitHub mirror is strictly read only and provides convenience to users and
developers to explore the code and for the community to accept contributions
via GitHub pull requests.


## Documentation

* [Project Documentation](https://docs.cloudstack.apache.org)
* [Release notes](https://docs.cloudstack.apache.org/en/latest/releasenotes/index.html)
* Developer [wiki](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Home)
* Design [documents](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Design)
* API [documentation](https://cloudstack.apache.org/api.html)
* How to [contribute](CONTRIBUTING.md)


## Ecosystem & Integrations

Apache CloudStack is built to fit into your existing infrastructure — not replace it. Its modular architecture ensures seamless integration across your IT ecosystem:

- **Hypervisors**: Native support for KVM, VMware vSphere, and XenServer.  
- **Authentication**: Integrates with LDAP, Active Directory, and SAML-based SSO providers.  
- **Storage**: Supports NFS, iSCSI, Ceph, and S3-compatible object storage for secondary storage.  
- **Networking**: Compatible with industry-leading appliances like Cisco, Juniper, and Open vSwitch.  
- **Monitoring & Observability**: Works with Prometheus, Nagios, Zabbix, and other external tools.  
- **Billing & Usage**: Rich APIs and metering to connect with billing, CRM, and provisioning systems.  
- **DevOps-Ready**: Exposes REST APIs, Python bindings, and CLI tools to support CI/CD pipelines and automation frameworks.

> **Why it matters**: CloudStack doesn’t require you to rip and replace. It works with the technologies you already use, helping you build a future-ready, fully interoperable cloud.

## Project Status & Release Cycle

Apache CloudStack is a **mature and actively maintained open-source project** governed by the Apache Software Foundation. With over a decade of development, it powers mission-critical cloud infrastructure for service providers, enterprises, and governments worldwide.

- **Actively Maintained**: Backed by a global community of developers and users.
- **Predictable Releases**: CloudStack follows a regular release cadence — typically **every 4–6 months**, delivering new features, enhancements, and bug fixes.
- **Production-Grade Stability**: Battle-tested in diverse environments, from private enterprise clouds to large-scale public cloud deployments.

> **Why it matters**: CloudStack’s proven track record, transparent development process, and consistent release cycle provide long-term confidence for operators and developers alike.


## News and Events

* [Blog](https://blogs.apache.org/cloudstack)
* [Twitter](https://twitter.com/cloudstack)
* [Events and meetup](http://cloudstackcollab.org/)
* [YouTube channel](https://www.youtube.com/ApacheCloudStack)

## Getting Involved & Contributing

Apache CloudStack thrives on community collaboration — and we welcome contributions of all kinds!

### How to Contribute

Want to help improve CloudStack? Whether you're writing code, fixing bugs, improving documentation, or translating content — we’d love your support.

-  [Read our CONTRIBUTING.md](https://github.com/apache/cloudstack/blob/main/CONTRIBUTING.md) for guidelines and best practices.
- Fork the repo, make your changes, and open a pull request. All contributions go through community review.

### Mailing Lists

Stay connected with the community and get involved in discussions:

- **Development Discussions**: [dev@cloudstack.apache.org](mailto:dev@cloudstack.apache.org)
- **User Support**: [users@cloudstack.apache.org](mailto:users@cloudstack.apache.org)
- **Commits & Updates**: [commits@cloudstack.apache.org](mailto:commits@cloudstack.apache.org)

###  Issue Tracker

Report bugs, suggest features, or track project progress through the official issue tracker:

- [CloudStack Jira Tracker](https://issues.apache.org/jira/projects/CLOUDSTACK)

> **Note**: If you're unsure where to start, check the mailing lists or open issues — there's always something to help with!


## License & Security Policy

### License

Apache CloudStack is released under the **[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)**.

For full details, see the [LICENSE](./LICENSE) file in the repository.

---

### Security

We take security seriously. If you discover a potential vulnerability in a released version of CloudStack:

- **Report it confidentially** by emailing: [security@apache.org](mailto:security@apache.org)
- Include details of the issue, how it might be exploited, and any relevant supporting information.

Please review our full [Security Policy](https://cloudstack.apache.org/security.html) for responsible disclosure guidelines.

---

Your feedback and cooperation help us keep CloudStack safe and reliable for everyone.


## Notice of Cryptographic Software

This distribution includes cryptographic software. The country in which you currently
reside may have restrictions on the import, possession, use, and/or re-export to another
country, of encryption software. BEFORE using any encryption software, please check your
country's laws, regulations and policies concerning the import, possession, or use, and
re-export of encryption software, to see if this is permitted. See [The Wassenaar Arrangement](http://www.wassenaar.org/)
for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has
classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which
includes information security software using or performing cryptographic functions with
asymmetric algorithms. The form and manner of this Apache Software Foundation distribution
makes it eligible for export under the License Exception ENC Technology Software
Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section
740.13) for both object code and source code.

The following provides more details on the included cryptographic software:

* CloudStack makes use of JaSypt cryptographic libraries.
* CloudStack has a system requirement of MySQL, and uses native database encryption functionality.
* CloudStack makes use of the Bouncy Castle general-purpose encryption library.
* CloudStack can optionally interact with and control OpenSwan-based VPNs.
* CloudStack has a dependency on and makes use of JSch - a java SSH2 implementation.

## Star History

[![Apache CloudStack Star History](https://api.star-history.com/svg?repos=apache/cloudstack&type=Date)](https://www.star-history.com/#apache/cloudstack&Date)

## Contributors

[![Apache CloudStack Contributors](https://contrib.rocks/image?repo=apache/cloudstack&anon=0&max=500)](https://github.com/apache/cloudstack/graphs/contributors)