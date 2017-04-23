# Vyos Router Plugin

Vyos (formerly Vyatta) is an open source routing platform built upon Debian Linux.
It has been modeled to operate in a manner that mirrors more traditional enterprise
grade routing hardware. It has a lively development community as well as a track
record of stable production operation stretching back many years. Vyos can
efficiently operate in production environments either deployed to bare metal or
as a VM.
  The Vyos Project website can be found [here](https://vyos.io/ "https://vyos.io/") .


## Introduction to the Plugin

The ultimate goal of this plugin is to provide a viable drop in alternative to the
current VRouter. Vyos should be able to reach almost 100% feature parity with the
VRouter without having to make any changes to Vyos itself. This has the significant
advantage of providing end users with the option to use Vyos or the VRouter in
production as they see fit. For Cloudstack, using Vyos means that there is a
lively and dedicated community of developers focused solely on the routing
functionality. This should help to ensure that issues on the router portion of the
Cloudstack ecosystem are addressed promptly by developers who have years of experience
contributing to networking projects.

## Current Release

The current version of the Vyos Plugin should be considered a proof of concept only.
It has some missing features, and design limitations that will need to be
addressed before it will be ready for review as a candidate for inclusion in cloudstack
proper.

This first release, (including this document) was developed using the existing Palo Alto Firewall plugin
as a pattern. The goal was to produce a proof of concept release that attained feature parity with the Palo
Alto plugin for review and comment by the community. The remaining features of the VRouter will then be
added to the plugin in an iterative manner.

### Supported Operations

  * List/Add/Delete Vyos service provider
  * List/Add/Delete Vyos network service offering
  * List/Add/Delete Vyos network with above service offering
  * Implement Guest Network
  * List/Add/Delete Ingress Firewall rule
  * List/Add/Delete Egress Firewall rule
  * List/Add/Delete Port Forwarding rule
  * List/Add/Delete Static Nat rule
  * Shutdown Guest Network
  * Basic Unit Tests
  * Basic Functional Tests
  * Basic Integration Tests

### Limitations and Design Liabilities

  * **SSH Access to the Vyos Router is not properly secured. It is open to all incoming
    connections and must be limited to only allow connections from the management network
    (I think). THIS IS A MAJOR SECURITY CONCERN AND MUST BE ADDRESSED. It was left open
    for now only because I did not know how to gather the required information from
    Cloudstack and decided to focus on getting a working POC out the door instead of
    digging into this at the current juncture.**
  
  * Only supports ETHX devices on the Vyos Router. Support for bonded and pseudo
    ethernet devices can be added with no major structural changes to the code.
    It was decided that getting a basic functional POC out the door was higher
    priority.

  * The plugin uses SSH to interact with Vyos and execute commands. This has been
    stable during all testing but an http based API would be better. Vyos is
    working on such an implementation through their [VyConf](https://wiki.vyos.net/wiki/Vyconf "Vyconf")
    initiative. The plugin will be refactored to use VyConf as soon as it that
    API is ready for release though VyConf is a long term goal for Vyos and will not be available for some time. .

  * Currently, Egress Firewall rules must be manually created that allow the DNS provider
    for the Guest Network to communicate on port 53. I need to determine how to
    identify the IP for the DNS provider and add firewall rules for it during Guest
    Network setup.

## Feature Specifcations

### In-Scope

  * Support of CloudStack advanced network topology.
  * Support of multiple Vyos Routers.
  * Support of multiple guest networks per Vyos Router.
  * Support of multiple public networks per Vyos Router.
  * Support for inter-vlan routing.
  * Configuration of connectivity with Vyos Router through CloudStack UI and persistence of this information.
  * Allow selection of Vyos Router when defining CloudStack network service offering for:
    * Firewall (Ingress & Egress)
    * Source NAT
    * Static NAT
    * Port forwarding
  * Support of virtual Vyos Routers.
  * Support of parallel deployment with hardware load-balancer.
  * Communication layer with Vyos Router's custom bash shell.
  * Mapping of CloudStack APIs to corresponding Vyos Router commands.
  * Proper display of Vyos Router connectivity status in CloudStack UI.
  * Functional/Integration testing on Vyos Stable (1.1.7)
  * Functional/Integration testing on Vyos Beta (1.2.0)
    * This is their new version based on Debian Jesse.
  * Full documentation of the solution (architecture, design, APIs)

### Out-of-scope

  * Support of inline deployment with hardware load-balancer (e.g.: Netscaler).
  * Exposing any Vyos Router features having no equivalent UI/API in CloudStack.
  * All other VRouter funconalities (EG DHCP, DNS, User Data...etc).

## Instalation Instructions

### Vyos Pre-Configuration Requirements

  * See Vyos User Guide linked below.
  * Deploy a Vyos Router instance as either a bare metal install or as a VM.
    * Currently, I think, Vyos must be deployed outside of Cloudstack.
  * For each public IP range in Cloudstack, configure the Vyos Public Network
    Interface with an IP that is outside the range configured in Cloudstack but
    inside the CIDR of the range.
  * If VLANs are used on public ip ranges in Cloudstack make sure matching VLANs
    are configured in the Vyos Router.
  * Create an interface for the Guest Networks. No IP needs to be assigned to this
    Interface. This can be the same interface used for the public networks. 
  * Set the system default gateway. This is the upstream network device allowing
    access to the internet for the Router's subnet. This must be accessible from
    the Vyos Router public interface IP.
  * Set the upstream DNS server entries. Again, these must be accessible from the Vyos
    Router public interface IP.
  * Enable ssh access on the Vyos Router.
  * Ensure that the Cloudstack Management Server can successfully ssh to the
    Vyos Router. **??Is that the correct and only source of configuration connections
    from Cloudstack to the Router??**

### Vyos Configuration Example
  * This configures a Vyos router with the following properties:
    * Vyos Router IP: 192.168.99.91/24
    * eth0: Untagged Public IP Range of 192.168.99.0/24
    * eth1: Guest Network Interface
    * Default Gateway: 192.168.2.1
    * Upstream DNS: 192.168.2.1

* SSH to the Vyos Router.
* Enter Configuration mode
```
configure
```
* Configure Public/Guest Interfaces
```
set interfaces ethernet eth0 address '192.168.2.91/24'
set interfaces ethernet eth0 description 'Public Interface'
set interfaces ethernet eth1 description 'Guest Interface'
```
* Configure Default Gateway and DNS
```
set system gateway-address '192.168.2.1'
set system name-server '192.168.2.1'
```
* Commit the changes, save to the boot config, and exit configuration mode.
```
commit
save
exit
```

### Cloudstack Configuration Steps
  * These are identical to the steps show in the Palo Alto documentation in [link](https://cwiki.apache.org/confluence/display/CLOUDSTACK/Palo+Alto+Firewall+Integration "Confluence")
  starting at "UI Flow"
1. Add Vyos Router as a Network Service Provider by going into Infrastructure > Zones > YOURZONE > Physical Network > YOURGUESTNETWORK
  and clicking "Configure" inside the "Network Service Providers" box.
1. Fill in the form with the proper information for your Vyos Router.
1. Once done, enable the Vyos network offering.
1. Create a Network Service Offering for the Vyos Router.
1. Currently, you can select your Vyos device in the drop downs for the for the following functions:
      * Firewall
      * Source Nat
      * Static Nat
      * Port Forwarding
1. Any or all of the other functions can use the VRouter or an alternate provider.
1. Enable the Vyos Network Service Offering.
1. Create a Network and select the Vyos Network Service Offering.
1. Deploy a VM to that network.

## Vyos Reference Materials
* [Vyos Website](https://vios.io "Vyos Website")
* [Vyos Wiki](https://wiki.vyos.net/wiki/Main_Page "Vyos Wiki")
* [Vyos User Guide](https://wiki.vyos.net/wiki/User_Guide "Vyos User Guide")
* [Vyos Address Translation](http://onebadpixel.com/blog/2014/01/22/part-5-nat-translation "Vyos NAT/Firewall") .
  A great article describing the order of operations of NAT and Firewall operations in vyos.
  Pay attention to the diagram in this article as it has major implications for Cloudstack integration:
