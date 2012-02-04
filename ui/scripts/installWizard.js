(function($, cloudStack) {
  cloudStack.installWizard = {
    // Check if install wizard should be invoked
    check: function(args) {
      $.ajax({
        url: createURL('listZones'),
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            doInstall: !data.listzonesresponse.zone
          });
        }
      });
    },

    changeUser: function(args) {
      $.ajax({
        url: createURL('updateUser'),
        data: {
          id: cloudStack.context.users[0].userid,
          password: md5Hashed ? $.md5(args.data.password) : todb(args.data.password)
        },
        dataType: 'json',
        async: true,
        success: function(data) {
          args.response.success({
            data: { newUser: data.updateuserresponse.user }
          });
        }
      });
    },

    // Copy text
    copy: {
      // Tooltips
      'tooltip.addZone.name': function(args) {
        args.response.success({
          text: 'A name for the zone.'
        });
      },

      'tooltip.addZone.dns1': function(args) {
        args.response.success({
          text: 'These are DNS servers for use by guest VMs in the zone. These DNS servers will be accessed via the public network you will add later. The public IP addresses for the zone must have a route to the DNS server named here.'
        });
      },

      'tooltip.addZone.dns2': function(args) {
        args.response.success({
          text: 'These are DNS servers for use by guest VMs in the zone. These DNS servers will be accessed via the public network you will add later. The public IP addresses for the zone must have a route to the DNS server named here.'
        });
      },

      'tooltip.addZone.internaldns1': function(args) {
        args.response.success({
          text: 'These are DNS servers for use by system VMs in the zone. These DNS servers will be accessed via the private network interface of the System VMs. The private IP address you provide for the pods must have a route to the DNS server named here.'
        });
      },

      'tooltip.addZone.internaldns2': function(args) {
        args.response.success({
          text: 'These are DNS servers for use by system VMs in the zone. These DNS servers will be accessed via the private network interface of the System VMs. The private IP address you provide for the pods must have a route to the DNS server named here.'
        });
      },

      'tooltip.configureGuestTraffic.name': function(args) {
        args.response.success({
          text: 'A name for your network'
        });
      },

      'tooltip.configureGuestTraffic.description': function(args) {
        args.response.success({
          text: 'A description for your network'
        });
      },

      'tooltip.configureGuestTraffic.guestGateway': function(args) {
        args.response.success({
          text: 'The gateway that the guests should use'
        });
      },

      'tooltip.configureGuestTraffic.guestNetmask': function(args) {
        args.response.success({
          text: 'The netmask in use on the subnet that the guests should use'
        });
      },

      'tooltip.configureGuestTraffic.guestStartIp': function(args) {
        args.response.success({
          text: 'The range of IP addresses that will be available for allocation to guests in this zone.  If one NIC is used, these IPs should be in the same CIDR as the pod CIDR.'
        });
      },

      'tooltip.configureGuestTraffic.guestEndIp': function(args) {
        args.response.success({
          text: 'The range of IP addresses that will be available for allocation to guests in this zone.  If one NIC is used, these IPs should be in the same CIDR as the pod CIDR.'
        });
      },

      'tooltip.addPod.name': function(args) {
        args.response.success({
          text: 'A name for the pod'
        });
      },

      'tooltip.addPod.reservedSystemGateway': function(args) {
        args.response.success({
          text: 'The gateway for the hosts in that pod.'
        });
      },

      'tooltip.addPod.reservedSystemNetmask': function(args) {
        args.response.success({
          text: 'The netmask in use on the subnet the guests will use.'
        });
      },

      'tooltip.addPod.reservedSystemStartIp': function(args) {
        args.response.success({
          text: 'This is the IP range in the private network that the CloudStack uses to manage Secondary Storage VMs and Console Proxy VMs. These IP addresses are taken from the same subnet as computing servers.'
        });
      },

      'tooltip.addPod.reservedSystemEndIp': function(args) {
        args.response.success({
          text: 'This is the IP range in the private network that the CloudStack uses to manage Secondary Storage VMs and Console Proxy VMs. These IP addresses are taken from the same subnet as computing servers.'
        });
      },

      'tooltip.addCluster.name': function(args) {
        args.response.success({
          text: 'A name for the cluster.  This can be text of your choosing and is not used by CloudStack.'
        });
      },

      'tooltip.addHost.hostname': function(args) {
        args.response.success({
          text: 'The DNS name or IP address of the host.'
        });
      },

      'tooltip.addHost.username': function(args) {
        args.response.success({
          text: 'Usually root.'
        });
      },

      'tooltip.addHost.password': function(args) {
        args.response.success({
          text: 'This is the password for the user named above (from your XenServer install).'
        });
      },

      'tooltip.addPrimaryStorage.name': function(args) {
        args.response.success({
          text: 'The name for the storage device.'
        });
      },

      'tooltip.addPrimaryStorage.server': function(args) {
        args.response.success({
          text: '(for NFS, iSCSI, or PreSetup) The IP address or DNS name of the storage device.'
        });
      },

      'tooltip.addPrimaryStorage.path': function(args) {
        args.response.success({
          text: '(for NFS) In NFS this is the exported path from the server. Path (for SharedMountPoint).  With KVM this is the path on each host that is where this primary storage is mounted.  For example, "/mnt/primary".'
        });
      },

      'tooltip.addSecondaryStorage.nfsServer': function(args) {
        args.response.success({
          text: 'The IP address of the NFS server hosting the secondary storage'
        });
      },

      'tooltip.addSecondaryStorage.path': function(args) {
        args.response.success({
          text: 'The exported path, located on the server you specified above'
        });
      },

      // Intro text
      whatIsCloudStack: function(args) {
        args.response.success({
          text: 'CloudStack&#8482 is a software platform that pools computing resources to build public, private, and hybrid Infrastructure as a Service (IaaS) clouds. CloudStack&#8482 manages the network, storage, and compute nodes that make up a cloud infrastructure. Use CloudStack&#8482 to deploy, manage, and configure cloud computing environments.<br/><br/>Extending beyond individual virtual machine images running on commodity hardware, CloudStack&#8482 provides a turnkey cloud infrastructure software stack for delivering virtual datacenters as a service - delivering all of the essential components to build, deploy, and manage multi-tier and multi-tenant cloud applications. Both open-source and Premium versions are available, with the open-source version offering nearly identical features. '
        });
      },

      // EULA
      eula: function(args) {
        args.response.success({
          text: '<p>CITRIX&reg; LICENSE AGREEMENT</p> <p>This is a legal agreement ("AGREEMENT") between you, the Licensed User, and Citrix Systems, Inc., Citrix Systems International GmbH, or Citrix Systems Asia Pacific Pty Ltd. Your location of receipt of this product or feature release (both hereinafter "PRODUCT") or technical support (hereinafter "SUPPORT") determines the providing entity hereunder (the applicable entity is hereinafter referred to as "CITRIX"). Citrix Systems, Inc., a Delaware corporation, licenses this PRODUCT in the Americas and Japan and provides SUPPORT in the Americas. Citrix Systems International GmbH, a Swiss company wholly owned by Citrix Systems, Inc., licenses this PRODUCT and provides SUPPORT in Europe, the Middle East, and Africa, and licenses the PRODUCT in Asia and the Pacific (excluding Japan). Citrix Systems Asia Pacific Pty Ltd. provides SUPPORT in Asia and the Pacific (excluding Japan). Citrix Systems Japan KK provides SUPPORT in Japan. BY INSTALLING AND/OR USING THE PRODUCT, YOU ARE AGREEING TO BE BOUND BY THE TERMS OF THIS AGREEMENT. IF YOU DO NOT AGREE TO THE TERMS OF THIS AGREEMENT, DO NOT INSTALL AND/OR USE THE PRODUCT.</p> <p>1.     GRANT OF LICENSE. The PRODUCT is the Citrix proprietary software program in object code form distributed hereunder. This PRODUCT is licensed under a CPU socket model.  The PRODUCT is activated by licenses that allow use of the Software in increments defined by the license model ("Licenses").</p> <p>Under the CPU socket model, a "CPU socket" is an individual CPU socket on a server running the PRODUCT, regardless of whether or not the socket contains a CPU.</p> <p>Licenses for other CITRIX PRODUCTS or other editions of the same PRODUCT may not be used to increase the allowable use for the PRODUCT. Licenses are version specific for the PRODUCT. They must be the same version or later than the PRODUCT being accessed.  CITRIX grants to you the following worldwide and non-exclusive rights to the PRODUCT and accompanying documentation (collectively called the "SOFTWARE"):</p> <p>a.      License. You may install the SOFTWARE on servers containing up to the number of CPU sockets for which you have purchased Licenses ("Production Servers"). In addition, you may install the management portion of the SOFTWARE on management servers as required to support the SOFTWARE running on the Production Servers. You may use the SOFTWARE to provide cloud services for internal users or third parties. Each License that is installed in both a production and disaster recovery environment may be used only in one of the environments at any one time, except for duplicate use during routine testing of the disaster recovery environment.  You have the right to customize the SOFTWARE Web user interface only.</p> <p>b.   Perpetual License. If the SOFTWARE is "Perpetual License SOFTWARE," the SOFTWARE is licensed on a perpetual basis and includes the right to receive Subscription (as defined in Section 2 below).</p> <p>c.    Annual PRODUCT. If the SOFTWARE is "Annual License SOFTWARE," your license is for one (1) year and includes the right to receive Updates for that period (but not under Subscription as defined in Section 2 below). For the purposes of this AGREEMENT, an "Update" shall mean a generally available release of the same SOFTWARE. To extend an Annual License, you must purchase and install a new license prior to the expiration of the current License. Note that if a new License is not purchased and installed, Annual License SOFTWARE is not licensed for use beyond the expiration of the license period.  Annual License SOFTWARE may disable itself upon expiration of the license period. </p> <p>d.   Partner Demo. If this SOFTWARE is labeled "Partner Demo," notwithstanding any term to the contrary in this AGREEMENT, your License permits use only if you are a current CITRIX authorized distributor or reseller and then only for demonstration, test, or evaluation purposes in support of your customers. Partner Demo SOFTWARE may not be used for customer training. Note that Partner Demo SOFTWARE disables itself on the "time-out" date identified on the SOFTWARE packaging.</p> <p>e.    Evaluation. If this SOFTWARE is labeled "Evaluation," notwithstanding any term to the contrary in this AGREEMENT, your License permits use only for your internal demonstration, test, or evaluation purposes. Note that Evaluation SOFTWARE disables itself on the "time-out" date identified on the SOFTWARE packaging.</p> <p>f.    Archive Copy. You may make one (1) copy of the SOFTWARE in machine-readable form solely for back-up purposes, provided that you reproduce all proprietary notices on the copy.</p> <p>2.   SUBSCRIPTION RIGHTS. Your initial subscription for Perpetual License SOFTWARE ("Subscription"), including SUPPORT, shall begin on the date the Licenses are delivered to you by email. Subscription shall continue for a one (1) year term subject to your purchase of annual renewals (the "Subscription Term"). During the initial or a renewal Subscription Term, CITRIX may, from time to time, generally make Updates available for licensing to the public. Upon general availability of Updates during the Subscription Term, CITRIX shall provide you with Updates for covered Licenses. Any such Updates so delivered to you shall be considered SOFTWARE under the terms of this AGREEMENT, except they are not covered by the Limited Warranty applicable to SOFTWARE, to the extent permitted by applicable law. Subscription Advantage may be purchased for the SOFTWARE until it is no longer offered in accordance with the CITRIX PRODUCT Support Lifecycle Policy posted at www.citrix.com.</p> <p>You acknowledge that CITRIX may develop and market new or different computer programs or editions of the SOFTWARE that use portions of the SOFTWARE and that perform all or part of the functions performed by the SOFTWARE. Nothing contained in this AGREEMENT shall give you any rights with respect to such new or different computer programs or editions. You also acknowledge that CITRIX is not obligated under this AGREEMENT to make any Updates available to the public. Any deliveries of Updates shall be Ex Works CITRIX (Incoterms 2000).</p> <p>3.   SUPPORT. SUPPORT is sold including various combinations of Incidents, technical contacts, coverage hours, geographic coverage areas, technical relationship management coverage, and infrastructure assessment options. The offering you purchase determines your entitlement.  An "Incident" is defined as a single SUPPORT issue and reasonable effort(s) needed to resolve it. An Incident may require multiple telephone calls and offline research to achieve final resolution. The Incident severity will determine the response levels for the SOFTWARE. Unused Incidents and other entitlements expire at the end of each annual term. SUPPORT may be purchased for the SOFTWARE until it is no longer offered in accordance with the CITRIX PRODUCT Support Lifecycle Policy posted at www.citrix.com. SUPPORT will be provided remotely from CITRIX to your locations. Where on-site visits are mutually agreed, you will be billed for reasonable travel and living expenses in accordance with your travel policy. CITRIX\' performance is predicated upon the following responsibilities being fulfilled by you: (i) you will designate a Customer Support Manager ("CSM") who will be the primary administrative contact; (ii) you will designate Named Contacts (including a CSM), preferably each CITRIX certified, and each Named Contact (excluding CSM) will be supplied with an individual service ID number for contacting SUPPORT; (iii) you agree to perform reasonable problem determination activities and to perform reasonable problem resolution activities as suggested by CITRIX. You agree to cooperate with such requests; (iv) you are responsible for implementing procedures necessary to safeguard the integrity and security of SOFTWARE and data from unauthorized access and for reconstructing any lost or altered files resulting from catastrophic failures; (v) you are responsible for procuring, installing, and maintaining all equipment, telephone lines, communications interfaces, and other hardware at your site and providing CITRIX with access to your facilities as required to operate the SOFTWARE and permitting CITRIX to perform the service called for by this AGREEMENT; and (vi) you are required to implement all currently available and applicable hotfixes, hotfix rollup packs, and service packs or their equivalent to the SOFTWARE in a timely manner. CITRIX is not required to provide any SUPPORT relating to problems arising out of: (i) your or any third party\'s alterations or additions to the SOFTWARE, operating system or environment that adversely affects the SOFTWARE (ii) Citrix provided alterations or additions to the SOFTWARE that do not address Errors or Defects; (ii) any functionality not defined in the PRODUCT documentation published by CITRIX and included with the PRODUCT; (iii) use of the SOFTWARE on a processor and peripherals other than the processor and peripherals defined in the documentation; (iv) SOFTWARE that has reached End-of-Life; and (v) any consulting deliverables from any party.  An "Error" is defined as a failure in the SOFTWARE to materially conform to the functionality defined in the documentation.  A "Defect" is defined as a failure in the SOFTWARE to conform to the specifications in the documentation.  In situations where CITRIX cannot provide a satisfactory resolution to your critical problem through normal SUPPORT methods, CITRIX may engage its product development team to create a private fix. Private fixes are designed to address your specific situation and may not be distributed by you outside your organization without written consent from CITRIX. CITRIX retains all right, title, and interest in and to all private fixes. Any hotfixes or private fixes are not SOFTWARE under the terms of this AGREEMENT and they are not covered by the Limited Warranty or Infringement Indemnification applicable to SOFTWARE, to the extent permitted by applicable law. With respect to infrastructure assessments or other consulting services, all intellectual property rights in all reports, preexisting works and derivative works of such preexisting works, as well as installation scripts and other deliverables and developments made, conceived, created, discovered, invented, or reduced to practice in the performance of the assessment are and shall remain the sole and absolute property of CITRIX, subject to a worldwide, nonexclusive License to you for internal use.</p> <p>4.   DESCRIPTION OF OTHER RIGHTS, LIMITATIONS, AND OBLIGATIONS. You may not transfer, rent, timeshare, grant rights in or lease the SOFTWARE except to the extent such foregoing restriction is expressly prohibited by applicable law. If you purchased Licenses for the SOFTWARE to replace other CITRIX Licenses for other CITRIX SOFTWARE and such replacement is a condition of the transaction, you agree to destroy those other CITRIX Licenses and retain no copies after installation of the new Licenses and SOFTWARE. You shall provide the serial numbers of such replaced Licenses and corresponding replacement Licenses to the reseller, and upon request, directly to CITRIX for license tracking purposes. You may not modify, translate, reverse engineer, decompile, disassemble, create derivative works based on, or copy the SOFTWARE except as specifically licensed herein or to the extent such foregoing restriction is expressly prohibited by applicable law. You may not remove any proprietary notices, labels, or marks on any SOFTWARE. To the extent permitted by applicable law, you agree to allow CITRIX to audit your compliance with the terms of this AGREEMENT upon prior written notice during normal business hours. Notwithstanding the foregoing, this AGREEMENT shall not prevent or restrict you from exercising additional or different rights to any free, open source code, documentation and materials contained in or provided with the SOFTWARE in accordance with the applicable free or open source license for such code, documentation, and materials.</p> <p>ALL RIGHTS IN THE SOFTWARE NOT EXPRESSLY GRANTED ARE RESERVED BY CITRIX OR ITS SUPPLIERS.</p> <p>You hereby agree, that to the extent that any applicable mandatory laws (such as, for example, national laws implementing EC Directive 91/250 on the Legal Protection of Computer Programs) give you the right to perform any of the aforementioned activities without the consent of CITRIX to gain certain information about the SOFTWARE, before you exercise any such rights, you shall first request such information from CITRIX in writing detailing the purpose for which you need the information. Only if and after CITRIX, at its sole discretion, partly or completely denies your request, shall you exercise your statutory rights.</p> <p>5.   INFRINGEMENT INDEMNIFICATION. CITRIX shall indemnify and defend, or at its option, settle any claim, suit, or proceeding brought against you based on an allegation that the SOFTWARE (excluding Open Source Software) infringes upon any patent or copyright of any third party ("Infringement Claim"), provided you promptly notify CITRIX in writing of your notification or discovery of an Infringement Claim such that CITRIX is not prejudiced by any delay in such notification.  For purposes of this Section 5, "Open Source Software" means software distributed by Citrix under an open source licensing model (e.g., the GNU General Public License, BSD or a license similar to those approved by the Open Source Initiative). CITRIX will have sole control over the defense or settlement of any Infringement Claim and you will provide reasonable assistance in the defense of the same. Following notice of an Infringement Claim, or if CITRIX believes such a claim is likely, CITRIX may at its sole expense and option: (i) procure for you the right to continue to use the alleged infringing SOFTWARE; (ii) replace or modify the SOFTWARE to make it non-infringing; or (iii) accept return of the SOFTWARE and provide you with a refund as appropriate. CITRIX assumes no liability for any Infringement Claims or allegations of infringement based on: (i) your use of any SOFTWARE after notice that you should cease use of such SOFTWARE due to an Infringement Claim; (ii) any modification of the SOFTWARE by you or at your direction; or (iii) your combination of SOFTWARE with non-CITRIX programs, data, hardware, or other materials, if such Infringement Claim would have been avoided by the use of the SOFTWARE alone. THE FOREGOING STATES YOUR EXCLUSIVE REMEDY WITH RESPECT TO ANY INFRINGEMENT CLAIM.</p> <p>6.   LIMITED WARRANTY AND DISCLAIMER. CITRIX warrants that for a period of ninety (90) days from the date of delivery of the SOFTWARE to you, the SOFTWARE will perform substantially in accordance with the PRODUCT documentation published by CITRIX and included with the PRODUCT. CITRIX and its suppliers\' entire liability and your exclusive remedy under this warranty (which is subject to you returning the SOFTWARE to CITRIX or an authorized reseller) will be, at the sole option of CITRIX and subject to applicable law, to replace the media and/or SOFTWARE or to refund the purchase price and terminate this AGREEMENT. This limited warranty does not cover any modification of the SOFTWARE by you or related issues. CITRIX will provide the SUPPORT requested by you in a professional and workmanlike manner, but CITRIX cannot guarantee that every question or problem raised by you will be resolved or resolved in a certain amount of time. With respect to consulting services, CITRIX and its suppliers\' entire liability and your exclusive remedy under this warranty is re-performance of the services.</p> <p>TO THE EXTENT PERMITTED BY APPLICABLE LAW AND EXCEPT FOR THE ABOVE LIMITED WARRANTY FOR SOFTWARE, CITRIX AND ITS SUPPLIERS MAKE AND YOU RECEIVE NO WARRANTIES OR CONDITIONS, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE; AND CITRIX AND ITS SUPPLIERS SPECIFICALLY DISCLAIM WITH RESPECT TO SOFTWARE, UPDATES, SUBSCRIPTION ADVANTAGE, AND SUPPORT ANY CONDITIONS OF QUALITY, AVAILABILITY, RELIABILITY, SECURITY, LACK OF VIRUSES, BUGS, OR ERRORS, AND ANY IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, ANY WARRANTY OF TITLE, QUIET ENJOYMENT, QUIET POSSESSION, MERCHANTABILITY, NONINFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. THE SOFTWARE IS NOT DESIGNED, MANUFACTURED, OR INTENDED FOR USE OR DISTRIBUTION WITH ANY EQUIPMENT THE FAILURE OF WHICH COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE PHYSICAL OR ENVIRONMENTAL DAMAGE. YOU ASSUME THE RESPONSIBILITY FOR THE SELECTION OF THE SOFTWARE AND HARDWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE INSTALLATION OF, USE OF, AND RESULTS OBTAINED FROM THE SOFTWARE AND HARDWARE.</p> <p>7.   PROPRIETARY RIGHTS. No title to or ownership of the SOFTWARE is transferred to you. CITRIX and/or its licensors own and retain all title and ownership of all intellectual property rights in and to the SOFTWARE, including any adaptations, modifications, translations, derivative works or copies. You acquire only a limited License to use the SOFTWARE.</p> <p>8.   EXPORT RESTRICTION. You agree that you will not export, re-export, or import the SOFTWARE in any form without the appropriate government licenses. You understand that under no circumstances may the SOFTWARE be exported to any country subject to U.S. embargo or to U.S.-designated denied persons or prohibited entities or U.S. specially designated nationals.</p> <p>9.   LIMITATION OF LIABILITY. TO THE EXTENT PERMITTED BY APPLICABLE LAW, YOU AGREE THAT NEITHER CITRIX NOR ITS AFFILIATES, SUPPLIERS, OR AUTHORIZED DISTRIBUTORS SHALL BE LIABLE FOR ANY LOSS OF DATA OR PRIVACY, LOSS OF INCOME, LOSS OF OPPORTUNITY OR PROFITS, COST OF RECOVERY, LOSS ARISING FROM YOUR USE OF THE SOFTWARE OR SUPPORT, OR DAMAGE ARISING FROM YOUR USE OF THIRD PARTY SOFTWARE OR HARDWARE OR ANY OTHER SPECIAL, INCIDENTAL, CONSEQUENTIAL, OR INDIRECT DAMAGES ARISING OUT OF OR IN CONNECTION WITH THIS AGREEMENT; OR THE USE OF THE SOFTWARE OR SUPPORT, REFERENCE MATERIALS, OR ACCOMPANYING DOCUMENTATION; OR YOUR EXPORTATION, REEXPORTATION, OR IMPORTATION OF THE SOFTWARE, HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY. THIS LIMITATION WILL APPLY EVEN IF CITRIX, ITS AFFILIATES, SUPPLIERS, OR AUTHORIZED DISTRIBUTORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. TO THE EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE LIABILITY OF CITRIX, ITS AFFILIATES, SUPPLIERS, OR AUTHORIZED DISTRIBUTORS EXCEED THE AMOUNT PAID FOR THE SOFTWARE, SUBSCRIPTION (INCLUDING SUBSCRIPTION WITH SUPPORT) OR SUPPORT AT ISSUE. YOU ACKNOWLEDGE THAT THE LICENSE OR SUPPORT FEE REFLECTS THIS ALLOCATION OF RISK. SOME JURISDICTIONS DO NOT ALLOW THE LIMITATION OR EXCLUSION OF LIABILITY FOR INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THE ABOVE LIMITATION OR EXCLUSION MAY NOT APPLY TO YOU. For purposes of this AGREEMENT, the term "CITRIX AFFILIATE" shall mean any legal entity fifty percent (50%) or more of the voting interests in which are owned directly or indirectly by Citrix Systems, Inc. Affiliates, suppliers, and authorized distributors are intended to be third party beneficiaries of this AGREEMENT.</p> <p>10.  TERMINATION. This AGREEMENT is effective until terminated. You may terminate this AGREEMENT at any time by removing the SOFTWARE from your computers and destroying all copies and providing written notice to CITRIX with the serial numbers of the terminated licenses. CITRIX may terminate this AGREEMENT at any time for your breach of this AGREEMENT. Unauthorized copying of the SOFTWARE or the accompanying documentation or otherwise failing to comply with the license grant of this AGREEMENT will result in automatic termination of this AGREEMENT and will make available to CITRIX all other legal remedies. You agree and acknowledge that your material breach of this AGREEMENT shall cause CITRIX irreparable harm for which monetary damages alone would be inadequate and that, to the extent permitted by applicable law, CITRIX shall be entitled to injunctive or equitable relief without the need for posting a bond. Upon termination of this AGREEMENT, the License granted herein will terminate and you must immediately destroy the SOFTWARE and accompanying documentation, and all backup copies thereof.</p> <p>11.  U.S. GOVERNMENT END-USERS. If you are a U.S. Government agency, in accordance with Section 12.212 of the Federal Acquisition Regulation (48 CFR 12.212 (October 1995)) and Sections 227.7202-1 and 227.7202-3 of the Defense Federal Acquisition Regulation Supplement (48 CFR 227.7202-1, 227.7202-3 (June 1995)), you hereby acknowledge that the SOFTWARE constitutes "Commercial Computer Software" and that the use, duplication, and disclosure of the SOFTWARE by the U.S. Government or any of its agencies is governed by, and is subject to, all of the terms, conditions, restrictions, and limitations set forth in this standard commercial license AGREEMENT. In the event that, for any reason, Sections 12.212, 227.7202-1 or 227.7202-3 are deemed not applicable, you hereby acknowledge that the Government\'s right to use, duplicate, or disclose the SOFTWARE are "Restricted Rights" as defined in 48 CFR Section 52.227-19(c)(1) and (2) (June 1987), or DFARS 252.227-7014(a)(14) (June 1995), as applicable. Manufacturer is Citrix Systems, Inc., 851 West Cypress Creek Road, Fort Lauderdale, Florida, 33309.</p> <p>12.  AUTHORIZED DISTRIBUTORS AND RESELLERS. CITRIX authorized distributors and resellers do not have the right to make modifications to this AGREEMENT or to make any additional representations, commitments, or warranties binding on CITRIX.</p> <p>13.  CHOICE OF LAW AND VENUE. If provider is Citrix Systems, Inc., this AGREEMENT will be governed by the laws of the State of Florida without reference to conflict of laws principles and excluding the United Nations Convention on Contracts for the International Sale of Goods, and in any dispute arising out of this AGREEMENT, you consent to the exclusive personal jurisdiction and venue in the State and Federal courts within Broward County, Florida. If provider is Citrix Systems International GmbH, this AGREEMENT will be governed by the laws of Switzerland without reference to the conflict of laws principles, and excluding the United Nations Convention on Contracts for the International Sale of Goods, and in any dispute arising out of this AGREEMENT, you consent to the exclusive personal jurisdiction and venue of the competent courts in the Canton of Zurich. If provider is Citrix Systems Asia Pacific Pty Ltd, this AGREEMENT will be governed by the laws of the State of New South Wales, Australia and excluding the United Nations Convention on Contracts for the International Sale of Goods, and in any dispute arising out of this AGREEMENT, you consent to the exclusive personal jurisdiction and venue of the competent courts sitting in the State of New South Wales. If any provision of this AGREEMENT is invalid or unenforceable under applicable law, it shall be to that extent deemed omitted and the remaining provisions will continue in full force and effect. To the extent a provision is deemed omitted, the parties agree to comply with the remaining terms of this AGREEMENT in a manner consistent with the original intent of the AGREEMENT.</p> <p>14.  HOW TO CONTACT CITRIX. Should you have any questions concerning this AGREEMENT or want to contact CITRIX for any reason, write to CITRIX at the following address: Citrix Systems, Inc., Customer Service, 851 West Cypress Creek Road, Ft. Lauderdale, Florida 33309; Citrix Systems International GmbH, Rheinweg 9, CH-8200 Schaffhausen, Switzerland; or Citrix Systems Asia Pacific Pty Ltd., Level 3, 1 Julius Ave., Riverside Corporate Park, North Ryde NSW 2113, Sydney, Australia.</p> <p>15.  TRADEMARKS. Citrixis a trademark and/or registered trademark of Citrix Systems, Inc., in the U.S. and other countries.</p> '
        });
      },

      whatIsAZone: function(args) {
        args.response.success({
          text: 'A zone is the largest organizational unit within a CloudStack&#8482; deployment. A zone typically corresponds to a single datacenter, although it is permissible to have multiple zones in a datacenter. The benefit of organizing infrastructure into zones is to provide physical isolation and redundancy. For example, each zone can have its own power supply and network uplink, and the zones can be widely separated geographically (though this is not required).'
        });
      },

      whatIsAPod: function(args) {
        args.response.success({
          text: 'A pod often represents a single rack. Hosts in the same pod are in the same subnet.<br/><br/>A pod is the second-largest organizational unit within a CloudStack&#8482; deployment. Pods are contained within zones. Each zone can contain one or more pods; in the Basic Installation, you will have just one pod in your zone'
        });
      },

      whatIsACluster: function(args) {
        args.response.success({
          text: 'A cluster provides a way to group hosts. The hosts in a cluster all have identical hardware, run the same hypervisor, are on the same subnet, and access the same shared storage. Virtual machine instances (VMs) can be live-migrated from one host to another within the same cluster, without interrupting service to the user. A cluster is the third-largest organizational unit within a CloudStack&#8482; deployment. Clusters are contained within pods, and pods are contained within zones.<br/><br/>CloudStack&#8482; allows multiple clusters in a cloud deployment, but for a Basic Installation, we only need one cluster. '
        });
      },

      whatIsAHost: function(args) {
        args.response.success({
          text: 'A host is a single computer. Hosts provide the computing resources that run the guest virtual machines. Each host has hypervisor software installed on it to manage the guest VMs (except for bare metal hosts, which are a special case discussed in the Advanced Installation Guide). For example, a Linux KVM-enabled server, a Citrix XenServer server, and an ESXi server are hosts. In a Basic Installation, we use a single host running XenServer.<br/><br/>The host is the smallest organizational unit within a CloudStack&#8482; deployment. Hosts are contained within clusters, clusters are contained within pods, and pods are contained within zones. '
        });
      },

      whatIsPrimaryStorage: function(args) {
        args.response.success({
          text: 'A CloudStack&#8482; cloud infrastructure makes use of two types of storage: primary storage and secondary storage. Both of these can be iSCSI or NFS servers, or localdisk.<br/><br/><strong>Primary storage</strong> is associated with a cluster, and it stores the disk volumes of each guest VM for all the VMs running on hosts in that cluster. The primary storage server is typically located close to the hosts. '
        });
      },

      whatIsSecondaryStorage: function(args) {
        args.response.success({
          text: 'Secondary storage is associated with a zone, and it stores the following:<ul><li>Templates - OS images that can be used to boot VMs and can include additional configuration information, such as installed applications</li><li>ISO images - OS images that can be bootable or non-bootable</li><li>Disk volume snapshots - saved copies of VM data which can be used for data recovery or to create new templates</ul>'
        });
      }
    },

    action: function(args) {
      var success = args.response.success;
      var message = args.response.message;
      
      // Get default network offering
      var selectedNetworkOffering;
      $.ajax({
        url: createURL("listNetworkOfferings&state=Enabled&guestiptype=Shared"),
        dataType: "json",
        async: false,
        success: function(json) {
          selectedNetworkOffering = $.grep(
            json.listnetworkofferingsresponse.networkoffering,
            function(networkOffering) {
              var services = $.map(networkOffering.service, function(service) {
                return service.name;
              });

              return $.inArray('SecurityGroup', services) == -1;
            }
          )[0];
        }
      });
      
      cloudStack.zoneWizard.action($.extend(true, {}, args, {
        // Plug in hard-coded values specific to quick install
        data: {
          zone: {
            networkType: 'Basic',
            domain: 1,
            networkOfferingId: selectedNetworkOffering.id
          }
        },
        response: {
          success: function(args) {
            var enableZone = function() {
              message('Enabling zone...');
              cloudStack.zoneWizard.enableZoneAction({
                data: args.data,
                formData: args.data,
                launchData: args.data,
                response: {
                  success: function(args) {
                    pollSystemVMs();
                  }
                }
              });              
            };

            var pollSystemVMs = function() {
              // Poll System VMs, then enable zone
              message('Creating system VMs (this may take a while)');
              var poll = setInterval(function() {
                $.ajax({
                  url: createURL('listSystemVms'),
                  success: function(data) {
                    var systemVMs = data.listsystemvmsresponse.systemvm;

                    if (systemVMs && systemVMs.length > 1) {
                      if (systemVMs.length == $.grep(systemVMs, function(vm) {
                        return vm.state == 'Running';
                      }).length) {
                        clearInterval(poll);
                        message('System VMs ready.');
                        setTimeout(pollBuiltinTemplates, 500);
                      }
                    }
                  }
                });
              }, 5000);
            };

            // Wait for builtin template to be present -- otherwise VMs cannot launch
            var pollBuiltinTemplates = function() {
              message('Waiting for builtin templates to load...');
              var poll = setInterval(function() {
                $.ajax({
                  url: createURL('listTemplates'),
                  data: {
                    templatefilter: 'all'
                  },
                  success: function(data) {
                    var templates = data.listtemplatesresponse.template ?
                      data.listtemplatesresponse.template : [];
                    var builtinTemplates = $.grep(templates, function(template) {
                      return template.templatetype == 'BUILTIN';
                    });

                    if (builtinTemplates.length) {
                      clearInterval(poll);
                      message('Your CloudStack is ready!');
                      setTimeout(success, 1000);
                    }
                  }
                });
              }, 5000);
            };

            enableZone();
          }
        }
      }));
    }
  };
}(jQuery, cloudStack));
