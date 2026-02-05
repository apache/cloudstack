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

# Apache CloudStack

[![Build Status](https://github.com/apache/cloudstack/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/apache/cloudstack/branch/main/graph/badge.svg)](https://codecov.io/gh/apache/cloudstack)
[![Docker CloudStack Simulator Status](https://github.com/apache/cloudstack/actions/workflows/docker-cloudstack-simulator.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/docker-cloudstack-simulator.yml)
[![License Check](https://github.com/apache/cloudstack/actions/workflows/rat.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/rat.yml)
[![Linter Status](https://github.com/apache/cloudstack/actions/workflows/linter.yml/badge.svg)](https://github.com/apache/cloudstack/actions/workflows/linter.yml)
[![Merge Conflict Checker Status](https://github.com/apache/cloudstack/actions/workflows/merge-conflict-checker.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/merge-conflict-checker.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=apache_cloudstack&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_cloudstack)
[![Simulator CI](https://github.com/apache/cloudstack/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/ci.yml)
[![UI Build](https://github.com/apache/cloudstack/actions/workflows/ui.yml/badge.svg?branch=main)](https://github.com/apache/cloudstack/actions/workflows/ui.yml)

[![Apache CloudStack](tools/logo/apache_cloudstack.png)](https://cloudstack.apache.org/)

![Screenshot](ui/docs/screenshot-dashboard.png)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->

- [Who Uses CloudStack?](#who-uses-cloudstack)
- [Quick Start / Try It Now](#quick-start--try-it-now)
- [Getting Started](#getting-started)
- [Getting Source Repository](#getting-source-repository)
- [Documentation](#documentation)
- [News and Events](#news-and-events)
- [Getting Involved and Contributing](#getting-involved-and-contributing)
- [Project Status](#project-status)
- [Reporting Security Vulnerabilities](#reporting-security-vulnerabilities)
- [License](#license)
- [Notice of Cryptographic Software](#notice-of-cryptographic-software)
- [Star History](#star-history)
- [Contributors](#contributors)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

Apache CloudStack empowers organizations to build and manage highly available, scalable, and secure private, public, and hybrid clouds with unparalleled ease. Transform your infrastructure into a flexible, on-demand service with CloudStack's comprehensive IaaS platform.

### Key Features

*   **Multi-hypervisor Support**: Manage VMs across VMware vSphere, KVM, XenServer, and Hyper-V from a single pane of glass.
*   **Network-as-a-Service (NaaS)**: Automate virtual networking, load balancing, and firewall services.
*   **Rich API & UI**: Programmatically control your cloud with a robust RESTful API and intuitive web interface.
*   **Storage Management**: Flexible storage options including primary and secondary storage, snapshots, and disaster recovery.
*   **User & Account Management**: Robust multi-tenancy with granular access control for users and accounts.
*   **Resource Accounting & Usage**: Track resource consumption for billing and capacity planning.

### Ecosystem & Integrations

CloudStack integrates seamlessly with your existing infrastructure and tools:

*   **Authentication**: LDAP, Active Directory, SAML 2.0.
*   **Storage**: S3-compatible object storage, Ceph, GlusterFS, SolidFire, Dell EMC, NetApp.
*   **Networking**: Juniper, Cisco, F5, NetScaler, OpenDaylight, Tungsten Fabric.
*   **Monitoring & Logging**: Prometheus, Grafana, ELK Stack, Splunk.
*   **Configuration Management**: Ansible, Terraform, Chef, Puppet.

CloudStack currently supports the most popular hypervisors:
VMware vSphere, KVM, XenServer, XenProject and Hyper-V as well as
OVM and LXC containers.

Users can manage their cloud with an easy to use Web interface, command line
tools, and/or a full-featured query based API.

For more information on Apache CloudStack, please visit the [website](https://cloudstack.apache.org)

## Who Uses CloudStack?

*   **Trusted by Global Leaders**: CloudStack is the backbone of infrastructure for major service providers, telecom operators, and enterprises worldwide.
    > "CloudStack has enabled us to scale our public cloud offering with ease and reliability." - *Major Cloud Provider*

*   **Diverse User Base**: Over 150 known organizations use Apache CloudStack, including:
    *   [Apple](https://www.apple.com)
    *   [Disney](https://www.disney.com)
    *   [Huawei](https://www.huawei.com)
    *   *...and many more.*

*   See our [case studies](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Case+Studies) highlighting successful deployments of Apache CloudStack.

*   See the up-to-date list of current [users](https://cloudstack.apache.org/users.html).

*   If you are using CloudStack in your organization and your company is not listed above, please complete our brief adoption [survey](https://cloudstack.apache.org/survey.html). We're happy to keep your company name anonymous if you require.


## Quick Start / Try It Now

The easiest way to try CloudStack is using the all-in-one Docker container. This is for **evaluation purposes only**.

1.  **Pull the Simulator Image**:
    ```bash
    docker pull apache/cloudstack-simulator
    ```

2.  **Run the Container**:
    ```bash
    docker run --name simulator -p 8080:5050 -d apache/cloudstack-simulator
    ```

3.  **Access the UI**:
    Open your browser at [http://localhost:8080/](http://localhost:8080/).
    *   **Username**: `admin`
    *   **Password**: `password`

4.  **Deploy a Data Center**:
    Once logged in, you can deploy a basic zone to test:
    ```bash
    docker exec -it simulator python /root/tools/marvin/marvin/deployDataCenter.py -i /root/setup/dev/basic.cfg
    ```

## Getting Started

* Download a released [version](https://cloudstack.apache.org/downloads.html)
* Build from source with the instructions in the [INSTALL.md](INSTALL.md) file.

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

## News and Events

* [Blog](https://blogs.apache.org/cloudstack)
* [Twitter](https://twitter.com/cloudstack)
* [Events and meetup](http://cloudstackcollab.org/)
* [YouTube channel](https://www.youtube.com/ApacheCloudStack)

## Getting Involved and Contributing

We welcome contributions from everyone! Here's how you can get started:

*   **How to Contribute**: Read our [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on code, documentation, and translation contributions.
*   **Mailing Lists**: Join the conversation on our mailing lists. This is where decisions are made.
    *   [dev@cloudstack.apache.org](mailto:dev-subscribe@cloudstack.apache.org) - For development discussions.
    *   [users@cloudstack.apache.org](mailto:users-subscribe@cloudstack.apache.org) - For general usage questions.
    *   [issues@cloudstack.apache.org](mailto:issues-subscribe@cloudstack.apache.org) - For issue tracking notifications.
*   **Report Issues**: Found a bug? Report it on [GitHub Issues](https://github.com/apache/cloudstack/issues).
*   **Good First Issues**: New to the project? Check out issues labeled ["good first issue"](https://github.com/apache/cloudstack/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22).

## Project Status

Apache CloudStack is a mature, actively developed project with a vibrant community. We follow a regular release cadence, typically delivering major feature releases every 6 months, with maintenance releases in between. This ensures a balance of innovation and stability for production environments.

## Reporting Security Vulnerabilities

If you've found an issue that you believe is a security vulnerability in a
released version of CloudStack, please report it to `security@apache.org` with
details about the vulnerability, how it might be exploited, and any additional
information that might be useful.

For more details, please visit our security [page](https://cloudstack.apache.org/security.html).

## License

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

Please see the [LICENSE](LICENSE) file included in the root directory
of the source tree for extended license details.

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
