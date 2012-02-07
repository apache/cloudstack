<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>CloudStack</title>
    <link type="text/css" rel="stylesheet" href="lib/reset.css"/>
    <link type="text/css" rel="stylesheet" href="css/cloudstack3.css" />

    <!--[if IE 7]>
    <link type="text/css" rel="stylesheet" href="css/cloudstack3-ie7.css" />
    <![endif]-->
  </head>
  <body>
    <!-- CloudStack widget content -->
    <div id="cloudStack3-container"></div>

    <!-- Templates -->
    <div id="template">
      <!-- Login form -->
      <div class="login">
        <form>
          <div class="logo"></div>
          <div class="fields">
            <!-- User name -->
            <div class="field username">
              <label for="username"><fmt:message key="label.username"/></label>
              <input type="text" name="username" class="required" />
            </div>

            <!-- Password -->
            <div class="field password">
              <label for="password"><fmt:message key="label.password"/></label>
              <input type="password" name="password" class="required" />
            </div>

            <!-- Domain -->
            <div class="field domain">
              <label for="domain"><fmt:message key="label.domain"/></label>
              <input type="text" name="domain" />
            </div>

            <!-- Submit (login) -->
            <input type="submit" value="" />
          </div>
        </form>
      </div>

      <!-- Instance wizard -->
      <div class="multi-wizard instance-wizard">
        <div class="progress">
          <ul>
            <li class="first"><span class="number">1</span><span>Setup</span><span class="arrow"></span></li>
            <li><span class="number">2</span><span class="multiline">Select a template</span><span class="arrow"></span></li>
            <li><span class="number">3</span><span class="multiline">Service Offering</span><span class="arrow"></span></li>
            <li><span class="number">4</span><span class="multiline">Data Disk Offering</span><span class="arrow"></span></li>
            <li><span class="number">5</span><span>Network</span><span class="arrow"></span></li>
            <li class="last"><span class="number">6</span><span>Review</span></li>
          </ul>
        </div>
        <form>
          <div class="steps">
            <!-- Step 1: Setup -->
            <div class="step setup" wizard-step-id="setup">
              <div class="content">
                <!-- Select a zone -->
                <div class="section select-zone">
                  <h3>Select a zone</h3>
                  <p>Descriptive text of what a zone is goes here.</p>
                  <div class="select-area">
                    <div class="desc">Description of this select area goes here.</div>
                    <select name="zoneid" class="required">
                      <option default="default" value="" >Select a zone</option>
                    </select>
                  </div>
                </div>

                <!-- Select template -->
                <div class="section select-template">
                  <h3>Select ISO or template</h3>
                  <p>Descriptive text goes here.</p>
                  <div class="select-area">
                    <div class="desc">Description of a template goes here.</div>
                    <input type="radio" name="select-template" value="select-template" />
                    <label>Template</label>
                  </div>
                  <div class="select-area">
                    <div class="desc">Description of a template goes here.</div>
                    <input type="radio" name="select-template" value="select-iso" />
                    <label>ISO</label>
                  </div>
                </div>
              </div>
            </div>

            <!-- Step 2: Select ISO -->
            <div class="step select-iso" wizard-step-id="select-iso">
              <!-- Select template -->
              <div class="wizard-step-conditional select-template">
                <div class="main-desc">
                  <p>
                    Please select a template for your new virtual instance.
                  </p>
                </div>
                <div class="template-select content tab-view">
                  <ul>
                    <li class="first"><a href="#instance-wizard-featured-templates">Featured</a></li>
                    <li><a href="#instance-wizard-community-templates">Community</a></li>
                    <li class="last"><a href="#instance-wizard-my-templates">My Template</a></li>
                  </ul>

                  <div id="instance-wizard-featured-templates">
                    <div class="select-container">
                    </div>
                  </div>
                  <div id="instance-wizard-community-templates">
                    <div class="select-container">
                    </div>
                  </div>
                  <div id="instance-wizard-my-templates">
                    <div class="select-container">
                    </div>
                  </div>
                </div>
              </div>

              <!-- Select ISO -->
              <div class="wizard-step-conditional select-iso">
                <div class="main-desc">
                  <p>
                    Please select an ISO for your new virtual instance.
                    You can also choose to upload your own iso as well.
                  </p>
                </div>
                <div class="iso-select content tab-view">
                  <ul>
                    <li class="first last"><a href="#instance-wizard-all-isos">All ISOs</a></li>
                  </ul>

                  <div id="instance-wizard-all-isos">
                    <div class="select-container">
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Step 3: Service Offering -->
            <div class="step service-offering" wizard-step-id="service-offering">
              <div class="content">
                <div class="select-container">
                </div>
              </div>
            </div>

            <!-- Step 4: Data Disk Offering -->
            <div class="step data-disk-offering" wizard-step-id="data-disk-offering">
              <div class="content">
                <div class="section no-thanks">
                  <input type="radio" name="diskofferingid" value="0" />
                  <label>No thanks</label>
                </div>

                <!-- Existing offerings -->
                <div class="select-container">
                </div>

                <!-- Custom size slider -->
                <div class="section custom-size">
                  <label>Disk size</label>

                  <!-- Slider -->
                  <label class="size">1 GB</label>
                  <div class="slider custom-size"></div>
                  <label class="size">100 GB</label>

                  <input type="text" class="required digits" name="size" value="1" />
                  <label class="size">GB</label>
                </div>
              </div>
            </div>

            <!-- Step 5: Network -->
            <div class="step network" wizard-step-id="network">
              <!-- 5a: Network description -->
              <div class="wizard-step-conditional nothing-to-select">
                <p>The zone you selected does not have any choices for network selection</p>               
                <p>Please proceed to the next step.</p>
              </div>

              <!-- 5b: Select network -->
              <div class="wizard-step-conditional select-network">
                <div class="content">
                  <div class="main-desc">
                    Please select networks for your virtual machine
                  </div>
                  <div class="select my-networks">
                    <table>
                      <thead>
                        <tr>
                          <th>Networks</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>
                            <div class="select-container">
                            </div>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                  <div class="select new-network">
                    <table>
                      <thead>
                        <tr>
                          <th>Add new network</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr>
                          <td>
                            <div class="select-container fixed">
                              <div class="select even">
                                <input type="checkbox" name="new-network"
                                       wizard-field="my-networks"
                                       value="create-new-network"
                                       checked="checked" />
                                <!-- Default (NEW) -->
                                <div class="select-desc hide-if-selected">
                                  <div class="name">NEW</div>
                                </div>

                                <!-- Name -->
                                <div class="field name hide-if-unselected">
                                  <div class="name">Name</div>
                                  <div class="value">
                                    <input type="text" class="required" name="new-network-name" />
                                  </div>
                                </div>

                                <!-- Service offering -->
                                <div class="select-desc field service-offering hide-if-unselected">
                                  <div class="name">Network Offering</div>
                                  <div class="desc">
                                    <select name="new-network-networkofferingid">
                                    </select>
                                  </div>
                                </div>

                                <div class="secondary-input hide-if-unselected">
                                  <input type="radio" name="defaultNetwork" value="new-network" />
                                  <div class="name">Default</div>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              <!-- Step 5c: Select security group -->
              <div class="wizard-step-conditional select-security-group">
                <div class="main-desc">
                  Please select security group(s) for your new virtual instance
                </div>
                <div class="content security-groups">
                  <div class="select-container">
                  </div>
                </div>
              </div>
            </div>

            <!-- Step 6: Review -->
            <div class="step review" wizard-step-id="review">
              <div class="main-desc">
                Please review the following information and confirm that your virtual instance is correct before launch
              </div>
              <div class="content">
                <div class="select-container">
                  <!-- Name -->
                  <div class="select vm-instance-name">
                    <div class="name">
                      <span>Name (optional)</span>
                    </div>
                    <div class="value">
                      <input type="text" name="displayname" />
                    </div>
                  </div>

                  <!-- Add to group -->
                  <div class="select odd">
                    <div class="name">
                      <span>Add to group (optional)</span>
                    </div>
                    <div class="value">
                      <input type="text" name="groupname" />
                    </div>
                  </div>

                  <!-- Zone -->
                  <div class="select">
                    <div class="name">
                      <span>Zone</span>
                    </div>
                    <div class="value">
                      <span wizard-field="zone"></span>
                    </div>
                    <div class="edit">
                      <a href="1">Edit</a>
                    </div>
                  </div>

                  <!-- Hypervisor -->
                  <div class="select odd">
                    <div class="name">
                      <span>Hypervisor</span>
                    </div>
                    <div class="value">
                      <span wizard-field="hypervisor"></span>
                    </div>
                    <div class="edit">
                      <a href="1">Edit</a>
                    </div>
                  </div>

                  <!-- Template -->
                  <div class="select">
                    <div class="name">
                      <span>Template</span>
                    </div>
                    <div class="value">
                      <span wizard-field="template"></span>
                    </div>
                    <div class="edit">
                      <a href="2">Edit</a>
                    </div>
                  </div>

                  <!-- Service offering -->
                  <div class="select odd">
                    <div class="name">
                      <span>Service offering</span>
                    </div>
                    <div class="value">
                      <span wizard-field="service-offering"></span>
                    </div>
                    <div class="edit">
                      <a href="3">Edit</a>
                    </div>
                  </div>

                  <!-- Data disk offering -->
                  <div class="select">
                    <div class="name">
                      <span>Data disk offering</span>
                    </div>
                    <div class="value">
                      <span wizard-field="disk-offering"></span>
                    </div>
                    <div class="edit">
                      <a href="4">Edit</a>
                    </div>
                  </div>

                  <!-- Primary network -->
                  <div class="select odd">
                    <div class="name">
                      <span>Primary network</span>
                    </div>
                    <div class="value">
                      <span wizard-field="default-network"></span>
                    </div>
                    <div class="edit">
                      <a href="5">Edit</a>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </form>

        <!-- Computer diagram -->
        <div class="diagram">
          <div>
            <div class="part zone-plane"></div>
            <div class="part computer-tower-front"></div>
            <div class="part computer-tower-back"></div>
          </div>
          <div class="part os-drive"></div>
          <div class="part cpu"></div>
          <div class="part hd"></div>
          <div class="part network-card"></div>
        </div>

        <!-- Buttons -->
        <div class="buttons">
          <div class="button previous"><span>Previous</span></div>
          <div class="button cancel"><span>Cancel</span></div>
          <div class="button next"><span>Next</span></div>
        </div>
      </div>

      <!-- Zone wizard -->
      <div class="multi-wizard zone-wizard">
        <div class="progress">
          <ul>
            <li class="first"><span class="number">1</span><span>Zone Type</span><span class="arrow"></span></li>
            <li><span class="number">2</span><span>Setup Zone</span><span class="arrow"></span></li>
            <li><span class="number">3</span><span>Setup Network</span><span class="arrow"></span></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li><span class="number">4</span><span>Add Resources</span><span class="arrow"></span></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li class="last"><span class="number">5</span><span>Launch</span></li>
          </ul>
        </div>
        <div class="steps">
          <!-- Step 1: Select network -->
          <div class="select-network" zone-wizard-step-id="selectZoneType">
            <form>
              <div class="content">
                <!-- Select template -->
                <div class="section select-network-model">
                  <h3>Set up zone type</h3>
                  <p>Please select a configuration for your zone.</p>
                  <div class="select-area">
                    <div class="desc">                      
                      Provide a single network where each VM instance is assigned an IP directly from the network. Guest isolation can be provided through layer-3 means such as security groups (IP address source filtering)
                    </div>
                    <input type="radio" name="network-model" value="Basic" checked="checked" />
                    <label>Basic</label>
                  </div>
                  <div class="select-area">
                    <div class="desc">
                      For more sophisticated network topologies. This network model provides the most flexibility in defining guest networks and providing custom network offerings such as firewall, VPN, or load balancer support.
                    </div>
                    <input type="radio" name="network-model" value="Advanced" />
                    <label>Advanced</label>
                  </div>
                </div>
              </div>
            </form>
          </div>

          <!-- Step 2: Add zone -->
          <div class="setup-zone" zone-wizard-form="zone"
               zone-wizard-step-id="addZone">
            <div class="info-desc">
              A zone is the largest organizational unit in CloudStack, and it typically corresponds to a single datacenter. Zones provide physical isolation and redundancy. A zone consists of one or more pods (each of which contains hosts and primary storage servers) and a secondary storage server which is shared by all pods in the zone.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 3.1: Setup Physical Network -->
          <div class="setup-physical-network"
               zone-wizard-step-id="setupPhysicalNetwork"
               zone-wizard-prefilter="setupPhysicalNetwork">
            <ul class="subnav">
              <li class="physical-network active">Physical Network</li>
              <li class="public-network">Public traffic</li>
              <li class="pod">Pod</li>
              <li class="guest-traffic">Guest Traffic</li>
            </ul>
            <div class="info-desc">
              When adding an advanced zone, you need to set up one or more physical networks. Each network corresponds to a NIC on the management server. Each physical network can carry one or more types of traffic, with certain restrictions on how they may be combined.<br/><br/><strong>Drag and drop one or more traffic types</strong> onto each physical network.
            </div>
            <div class="button add new-physical-network"><span class="icon">&nbsp;</span><span>Add physical network</span></div>

            <!-- Traffic types drag area -->
            <div class="traffic-types-drag-area">
              <div class="header">Traffic Types</div>
              <ul>
                <li class="management">
                  <ul class="container">
                    <li traffic-type-id="management"
                        title="Traffic between CloudStack's internal resources, including any components that communicate with the Management Server, such as hosts and CloudStack system VMs"
                        class="traffic-type-draggable management"></li>
                  </ul>
                  <div class="info">
                    <div class="title">Management</div>
                    <div class="desc">Set up the network for traffic between end-user VMs.</div>
                  </div>
                </li>
                <li class="public">
                  <ul class="container">
                    <li traffic-type-id="public"
                        title="Traffic between the internet and virtual machines in the cloud."
                        class="traffic-type-draggable public"></li>
                  </ul>
                  <div class="info">
                    <div class="title">Public</div>
                    <div class="desc">Set up the network for traffic between end-user VMs.</div>
                  </div>
                </li>
                <li class="guest">
                  <ul class="container">
                    <li traffic-type-id="guest"
                        title="Traffic between end-user virtual machines"
                        class="traffic-type-draggable guest clone"></li>
                  </ul>
                  <div class="info">
                    <div class="title">Guest</div>
                    <div class="desc">Set up the network for traffic between end-user VMs.</div>
                  </div>
                </li>
                <li class="storage">
                  <ul class="container">
                    <li traffic-type-id="storage"
                        title="Traffic between primary and secondary storage servers, such as VM templates and snapshots"
                        class="traffic-type-draggable storage"></li>
                  </ul>
                  <div class="info">
                    <div class="title">Storage</div>
                    <div class="desc">Set up the network for traffic between end-user VMs.</div>
                  </div>
                </li>
              </ul>
            </div>

            <div class="drag-helper-icon"></div>
            <div class="content input-area">
              <form></form>
            </div>
          </div>

          <!-- Step 3.1b: Add Netscaler device -->
          <div class="setup-physical-network-basic"
               zone-wizard-step-id="addNetscalerDevice"
               zone-wizard-form="basicPhysicalNetwork"
               zone-wizard-prefilter="addNetscalerDevice">
            <ul class="subnav">
              <li class="physical-network active">Netscaler</li>
              <li class="public-network">Public traffic</li>
              <li class="pod">Pod</li>
              <li class="guest-traffic">Guest Traffic</li>
            </ul>

            <div class="info-desc">Please specify Netscaler info</div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 3.2: Configure public traffic -->
          <div class="setup-public-traffic" zone-wizard-prefilter="addPublicNetwork"
               zone-wizard-step-id="configurePublicTraffic">
            <ul class="subnav">
              <li class="physical-network">Netscaler</li>
              <li class="public-network active">Public traffic</li>
              <li class="pod">Pod</li>
              <li class="guest-traffic">Guest Traffic</li>
            </ul>

            <div class="info-desc">
              Public traffic is generated when VMs in the cloud access the internet. Publicly-accessible IPs must be allocated for this purpose. End users can use the CloudStack UI to acquire these IPs to implement NAT between their guest network and their public network.<br/><br/>Provide at lease one range of IP addresses for internet traffic.
            </div>
            <div ui-custom="publicTrafficIPRange"></div>
          </div>

          <!-- Step 3.3: Add pod -->
          <div class="add-pod" zone-wizard-form="pod"
               zone-wizard-step-id="addPod">
            <ul class="subnav">
              <li class="physical-network">Netscaler</li>
              <li class="public-network">Public traffic</li>
              <li class="pod active">Pod</li>
              <li class="guest-traffic">Guest Traffic</li>
            </ul>

            <div class="info-desc">
              Each zone must contain in one or more pods, and we will add the first pod now. A pod contains hosts and primary storage servers, which you will add in a later step. First, configure a range of reserved IP addresses for CloudStack's internal management traffic. The reserved IP range must be unique for each zone in the cloud.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 3.4: Configure guest traffic -->
          <div class="setup-guest-traffic"
               zone-wizard-form="guestTraffic"
               zone-wizard-step-id="configureGuestTraffic"
               zone-wizard-prefilter="configureGuestTraffic">
            <ul class="subnav">
              <li class="physical-network">Netscaler</li>
              <li class="public-network">Public traffic</li>
              <li class="pod">Pod</li>
              <li class="guest-traffic active">Guest Traffic</li>
            </ul>

            <div class="info-desc">
              Enter the first and last IP addresses that define a range that CloudStack can assign to guest VMs. We strongly recommend the use of multiple NICs. If multiple NICs are used, the guest IPs may be in a separate subnet. If one NIC is used, the guest IPs should be in the same CIDR as the pod's CIDR, but not within the reserved system IP range.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 4.1: Add cluster -->
          <div class="add-cluster" zone-wizard-form="cluster"
               zone-wizard-step-id="addCluster">
            <ul class="subnav">
              <li class="cluster active">Cluster</li>
              <li class="host">Host</li>
              <li class="primary-storage">Primary Storage</li>
              <li class="secondary-storage">Secondary Storage</li>
            </ul>

            <div class="info-desc">
              Each pod must contain one or more clusters, and we will add the first cluster now. A cluster provides a way to group hosts. The hosts in a cluster all have identical hardware, run the same hypervisor, are on the same subnet, and access the same shared storage. Each cluster consists of one or more hosts and one or more primary storage servers.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 4.2: Add host -->
          <div class="add-cluster" zone-wizard-form="host"
               zone-wizard-step-id="addHost" zone-wizard-prefilter="addHost">
            <ul class="subnav">
              <li class="cluster">Cluster</li>
              <li class="host active">Host</li>
              <li class="primary-storage">Primary Storage</li>
              <li class="secondary-storage">Secondary Storage</li>
            </ul>
            <div class="info-desc">
              Each cluster must contain at lease one host (computer) for guest VMs to run on, and we will add the first host now. For a host to function in CloudStack, you must install hypervisor software on the host, assign an IP address to the host, and ensure the host is connected to the CloudStack management server.<br/><br/>Give the host's DNS or IP address, the user name (usually root) and password, and any labels you use to categorize hosts.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 4.3: Add primary storage -->
          <div class="add-cluster" zone-wizard-form="primaryStorage"
               zone-wizard-step-id="addPrimaryStorage">
            <ul class="subnav">
              <li class="cluster">Cluster</li>
              <li class="host">Host</li>
              <li class="primary-storage active">Primary Storage</li>
              <li class="secondary-storage">Secondary Storage</li>
            </ul>
            <div class="info-desc">
              Each cluster must contain one or more primary storage servers, and we will add the first one now. Primary storage contains the disk volumes for all the VMs running on hosts in the cluster. Use any standards-compliant protocol that is supported by the underlying hypervisor.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 4.4: Add secondary storage -->
          <div class="add-cluster" zone-wizard-form="secondaryStorage"
               zone-wizard-step-id="addSecondaryStorage">
            <ul class="subnav">
              <li class="cluster">Cluster</li>
              <li class="host">Host</li>
              <li class="primary-storage">Primary Storage</li>
              <li class="secondary-storage active">Secondary Storage</li>
            </ul>
            <div class="info-desc">
              Each zone must have at lease one NFS or secondary storage server, and we will add the first one now. Secondary storage stores VM templates, ISO images, and VM disk volume snapshots. This server must be available to all hosts in the zone.<br/><br/>Provide the IP address and exported path.
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>

          <!-- Step 5: Launch -->
          <div class="review" zone-wizard-step-id="launch">
            <div class="main-desc">Launch zone</div>
            <div class="main-desc launch" style="display:none;">
              Please wait while your zone is being created; this may take a while...
            </div>
            <form>
            </form>
            <div class="launch-container" style="display: none">
              <ul></ul>
            </div>
          </div>
        </div>

        <!-- Buttons -->
        <div class="buttons">
          <div class="button previous"><span>Previous</span></div>
          <div class="button cancel"><span>Cancel</span></div>
          <div class="button next"><span>Next</span></div>
        </div>
      </div>

      <!-- Network chart -->
      <div class="network-chart normal">
        <ul>
          <li class="firewall">
            <div class="name"><span>Firewall</span></div>
            <div class="view-details" net-target="firewall">View All</div>
          </li>
          <li class="loadBalancing">
            <div class="name"><span>Load Balancing</span></div>
            <div class="view-details" net-target="loadBalancing">View All</div>
          </li>
          <li class="portForwarding">
            <div class="name"><span>Port Forwarding</span></div>
            <div class="view-details" net-target="portForwarding">View All</div>
          </li>
        </ul>
      </div>

      <!-- Static NAT network chart -->
      <div class="network-chart static-nat">
        <ul>
          <li class="static-nat-enabled">
            <div class="name"><span>Static NAT Enabled</span></div>
            <div class="vmname"></div>
          </li>
          <li class="firewall">
            <div class="name"><span>Firewall</span></div>
            <div class="view-details" net-target="staticNAT">View All</div>
          </li>
        </ul>
      </div>

      <!-- Project dashboard -->
      <div class="project-dashboard-view">
        <div class="overview-area">
          <!-- Compute and storage -->
          <div class="compute-and-storage">
            <div class="system-dashboard">
              <div class="head">
                <span>Compute and Storage</span>
              </div>
              <ul class="status_box good">
                <!-- Virtual Machines -->
                <li class="block virtual-machines">
                  <span class="header">Virtual Machines</span>
                  <div class="icon"></div>
                  <div class="overview">
                    <!-- Running -->
                    <div class="overview-item running">
                      <div class="total" data-item="runningInstances">5</div>
                      <div class="label">Running</div>
                    </div>

                    <!-- Stopped -->
                    <div class="overview-item stopped">
                      <div class="total" data-item="stoppedInstances">10</div>
                      <div class="label">Stopped</div>
                    </div>
                  </div>
                </li>

                <!-- Storage -->
                <li class="block storage">
                  <span class="header">Storage</span>
                  <div class="icon"></div>
                  <div class="overview">
                    <div class="total" data-item="totalVolumes">10</div>
                    <div class="label">volumes</div>
                  </div>
                </li>

                <!-- Bandwidth -->
                <li class="block storage bandwidth">
                  <span class="header">Bandwidth</span>
                  <div class="icon"></div>
                  <div class="overview">
                    <div class="total" data-item="totalBandwidth">200</div>
                    <div class="label">mb/s</div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- Users -->
          <div class="users">
            <div class="system-dashboard">
              <div class="head">
                <span>Users</span>
              </div>
              <ul class="status_box good" data-item="users">
                <li class="block user">
                  <span class="header" data-list-item="account"></span>
                  <div class="icon"></div>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div class="info-boxes">
          <!-- Networking and security -->
          <div class="info-box networking-and-security">
            <div class="title">
              <span>Networking and Security</span>
            </div>
            <ul>
              <!-- IP addresses -->
              <li class="odd">
                <div class="total"><span data-item="totalIPAddresses"></span></div>
                <div class="desc">IP addresses</div>
              </li>

              <!-- Load balancing policies -->
              <li>
                <div class="total"><span data-item="totalLoadBalancers"></span></div>
                <div class="desc">Load balancing policies</div>
              </li>

              <!-- Port forwarding policies -->
              <li class="odd">
                <div class="total"><span data-item="totalPortForwards"></span></div>
                <div class="desc">Port forwarding policies</div>
              </li>

              <!-- Blank -->
              <li>
                <div class="total"></div>
                <div class="desc"></div>
              </li>

              <!-- Manage resources -->
              <li class="odd">
                <div class="total"></div>
                <div class="desc">
                  <div class="button manage-resources">
                    <span>Manage Resources</span>
                    <span class="arrow"></span>
                  </div>
                </div>
              </li>
            </ul>
          </div>

          <!-- Events -->
          <div class="info-box events">
            <div class="title">
              <span>Events</span>
              <div class="button view-all">
                <span>View all</span>
                <span class="arrow"></span>
              </div>
            </div>
            <ul data-item="events">
              <li class="odd">
                <div class="date"><span data-list-item="date"></span></div>
                <div class="desc" data-list-item="desc"></div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- System dashboard -->
      <div class="system-dashboard-view">
        <div class="toolbar">
          <div class="button refresh">
            <span>Refresh</span>
          </div>
        </div>

        <!-- Zone dashboard -->
        <div class="system-dashboard">
          <div class="head">
            <span>Zones</span>
            <div class="view-more"><span>View more</span></div>
          </div>
          <ul class="status_box good">
            <li class="block">
              <span class="header">Number of Zones</span>
              <span class="overview total" data-item="zoneCount"></span>
            </li>
            <li class="block">
              <span class="header">Number of Pods</span>
              <span class="overview total" data-item="podCount"></span>
            </li>
            <li class="block">
              <span class="header">Number of Clusters</span>
              <span class="overview total" data-item="clusterCount"></span>
            </li>
            <li class="block last">
              <span class="header">Number of Hosts</span>
              <span class="overview total" data-item="hostCount"></span>
            </li>
          </ul>
        </div>

        <!-- Host dashboard -->
        <div class="system-dashboard">
          <div class="head">
            <span>Hosts</span>
            <div class="view-more"><span>View more</span></div>
          </div>
          <ul class="status_box good">
            <li class="block">
              <span class="header">Total Hosts</span>
              <span class="overview total" data-item="hostCount"></span>
            </li>
            <li class="block capacity">
              <span class="header">Total CPU</span>
              <span class="overview total" data-item="cpuCapacityTotal"></span>
            </li>
            <li class="block capacity">
              <span class="header">Total Memory</span>
              <span class="overview total" data-item="memCapacityTotal"></span>
            </li>
            <li class="block last capacity">
              <span class="header">Total Storage</span>
              <span class="overview total" data-item="storageCapacityTotal"></span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Zone chart -->
      <div class="zone-chart">
        <!-- Side info -- Basic zone -->
        <div class="side-info basic">
          <ul>
            <li>
              <div class="icon"><span>1</span></div>
              <div class="title">Guest</div>
              <p>Set up the network for traffic between end-user VMs.</p>
            </li>
            <li>
              <div class="icon"><span>2</span></div>
              <div class="title">Clusters</div>
              <p>Define one or more clusters to group the compute hosts.</p>
            </li>
            <li>
              <div class="icon"><span>3</span></div>
              <div class="title">Hosts</div>
              <p>Add hosts to clusters. Hosts run hypervisors and VMs.</p>
            </li>
            <li>
              <div class="icon"><span>4</span></div>
              <div class="title">Primary Storage</div>
              <p>Add servers to store VM disk volumes in each cluster.</p>
            </li>
            <li>
              <div class="icon"><span>5</span></div>
              <div class="title">Secondary Storage</div>
              <p>Add servers to store templates, ISOs, and snapshots for the whole zone.</p>
            </li>
          </ul>
        </div>

        <!-- Side info -- Advanced zone -->
        <div class="side-info advanced">
          <ul>
            <li>
              <div class="icon"><span>1</span></div>
              <div class="title">Public</div>
              <p>Set up the network for Internet traffic.</p>
            </li>
            <li>
              <div class="icon"><span>2</span></div>
              <div class="title">Guest</div>
              <p>Set up the network for traffic between end-user VMs.</p>
            </li>
            <li>
              <div class="icon"><span>3</span></div>
              <div class="title">Clusters</div>
              <p>Define one or more clusters to group the compute hosts.</p>
            </li>
            <li>
              <div class="icon"><span>4</span></div>
              <div class="title">Hosts</div>
              <p>Add hosts to clusters. Hosts run hypervisors and VMs.</p>
            </li>
            <li>
              <div class="icon"><span>5</span></div>
              <div class="title">Primary Storage</div>
              <p>Add servers to store VM disk volumes in each cluster.</p>
            </li>
            <li>
              <div class="icon"><span>6</span></div>
              <div class="title">Secondary Storage</div>
              <p>Add servers to store templates, ISOs, and snapshots for the whole zone.</p>
            </li>
          </ul>
        </div>

        <!-- NAAS configuration -->
        <div class="resources naas">
          <div class="head">
            <span>Zone Configuration</span>
          </div>
          <ul class="system-main">
            <li class="main public" rel="public">
              <div class="tooltip-icon advanced"><span>1</span></div>
              <div class="name">Public</div>
              <div class="view-all configure">Configure</div>
            </li>
            <li class="main management" rel="management">
              <div class="name">Management</div>
              <div class="view-all configure">Configure</div>
            </li>
            <li class="main guest" rel="guest">
              <div class="tooltip-icon advanced"><span>2</span></div>
              <div class="tooltip-icon basic"><span>1</span></div>
              <div class="name">Guest</div>
              <div class="view-all configure">Configure</div>
            </li>
          </ul>
        </div>

        <!-- Zone resources -->
        <div class="resources zone">
          <div class="head">
            <div class="add" id="add_resource_button">Add Resource</div>
          </div>
          <ul>
            <li class="pod">
              <div class="name"><span>Pods</span></div>
              <div class="view-all" zone-target="pods">View All</div>
            </li>
            <li class="cluster">
              <div class="tooltip-icon advanced"><span>3</span></div>
              <div class="tooltip-icon basic"><span>2</span></div>
              <div class="name"><span>Clusters</span></div>
              <div class="view-all" zone-target="clusters">View All</div>
            </li>
            <li class="host">
              <div class="tooltip-icon advanced"><span>4</span></div>
              <div class="tooltip-icon basic"><span>3</span></div>
              <div class="name"><span>Hosts</span></div>
              <div class="view-all" zone-target="hosts">View All</div>
            </li>
            <li class="primary-storage">
              <div class="tooltip-icon advanced"><span>5</span></div>
              <div class="tooltip-icon basic"><span>4</span></div>
              <div class="name"><span>Primary Storage</span></div>
              <div class="view-all" zone-target="primary-storage">View All</div>
            </li>
            <li class="secondary-storage">
              <div class="tooltip-icon advanced"><span>6</span></div>
              <div class="tooltip-icon basic"><span>5</span></div>
              <div class="name"><span>Secondary Storage</span></div>
              <div class="view-all" zone-target="secondary-storage">View All</div>
            </li>
          </ul>
        </div>
      </div>

      <!-- Admin dashboard -->
      <div class="dashboard admin">
        <!-- General alerts-->
        <div class="dashboard-container sub alerts first">
          <div class="top">
            <div class="title"><span>General Alerts</span></div>
            <div class="button view-all">view all</div>
          </div>
          <ul data-item="alerts">
            <li class="error" concat-value="50">
              <div class="content">
                <span class="title" data-list-item="name">Alert 1</span>
                <p data-list-item="description">Alert 1</p>
              </div>
            </li>
          </ul>
        </div>

        <!-- Host alerts-->
        <div class="dashboard-container sub alerts last">
          <div class="top">
            <div class="title"><span>Host Alerts</span></div>
            <div class="button view-all">view all</div>
          </div>
          <ul data-item="hostAlerts">
            <li class="error" concat-value="50">
              <div class="content">
                <span class="title" data-list-item="name">Alert 1</span>
                <p data-list-item="description">Alert 1</p>
              </div>
            </li>
          </ul>
        </div>
        <!-- Capacity / stats -->
        <div class="dashboard-container head">
          <div class="top">
            <div class="title">
              <span>System Capacity</span>
            </div>
            <div class="selects" style="display:none;">
              <div class="select">
                <label>Zone:</label>
                <select>
                </select>
              </div>
              <div class="select">
                <label>Pods:</label>
                <select>
                </select>
              </div>
            </div>
          </div>

          <!-- Zone stat charts -->
          <div class="zone-stats">
            <ul data-item="zoneCapacities">
              <li concat-value="25">
                <div class="label">
                  Zone: <span data-list-item="zoneName"></span>
                </div>
                <div class="pie-chart-container">
                  <div class="percent-label"><span data-list-item="percent"></span>%</div>
                  <div class="pie-chart" data-list-item="percent"></div>
                </div>
                <div class="info">
                  <div class="name" data-list-item="type"></div>
                  <div class="value">
                    <span class="used" data-list-item="used"></span>
                    <span class="divider">/</span>
                    <span class="total" data-list-item="total"></span>
                  </div>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- User dashboard-->
      <div class="dashboard user">
        <div class="vm-status">
          <div class="title"><span>Virtual Machines</span></div>

          <div class="content">
            <ul>
              <li class="running">
                <div class="name">Running VMs</div>
                <div class="value" data-item="runningInstances"></div>
              </li>
              <li class="stopped">
                <div class="name">Stopped VMs</div>
                <div class="value" data-item="stoppedInstances"></div>
              </li>
              <li class="total">
                <div class="name">Total VMs</div>
                <div class="value" data-item="totalInstances"></div>
              </li>
            </ul>
          </div>
        </div>

        <div class="status-lists">
          <ul>
            <li class="events">
              <table>
                <thead>
                  <tr>
                    <th>Latest events <div class="button view-all events">view all</div></th>
                  </tr>
                </thead>
              </table>
              <div class="content">
                <ul data-item="events">
                  <li data-list-item="description">
                    <div class="title" data-list-item="type"></div>
                    <span data-list-item="description"></span>
                  </li>
                </ul>
              </div>
            </li>
            <li class="ip-addresses">
              <table>
                <thead>
                  <tr>
                    <th>Network <div class="button view-all network">view all</div></th>
                  </tr>
                </thead>
              </table>
              <table>
                <tbody>
                  <tr>
                    <td>
                      <div class="desc"><span>Owned isolated networks:</span></div>
                      <div class="value"><span data-item="netTotal"></span></div>
                    </td>
                  </tr>
                  <tr class="odd">
                    <td>
                      <div class="desc"><span>Owned public IP addresses:</span></div>
                      <div class="value"><span data-item="ipTotal"></span></div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </li>
          </ul>
        </div>
      </div>

      <!-- Recurring Snapshots -->
      <div class="recurring-snapshots">
        <p class="desc">Description</p>

        <div class="schedule">
          <p>Schedule:</p>

          <div class="forms">
            <ul>
              <li class="hourly"><a href="#recurring-snapshots-hourly">Hourly</a></li>
              <li class="daily"><a href="#recurring-snapshots-daily">Daily</a></li>
              <li class="weekly"><a href="#recurring-snapshots-weekly">Weekly</a></li>
              <li class="monthly"><a href="#recurring-snapshots-monthly">Monthly</a></li>
            </ul>

            <!-- Hourly -->
            <div id="recurring-snapshots-hourly">
              <form>
                <input type="hidden" name="snapshot-type" value="hourly" />

                <!-- Time -->
                <div class="field time">
                  <div class="name">Time</div>
                  <div class="value">
                    <select name="schedule"></select>
                    <label for="schedule">minutes(s) past the hour</label>
                  </div>
                </div>

                <!-- Timezone -->
                <div class="field timezone">
                  <div class="name">Timezone</div>
                  <div class="value">
                    <select name="timezone">
                      <option value="Etc/GMT+12">[UTC-12:00] GMT-12:00</option>
                      <option value="Etc/GMT+11">[UTC-11:00] GMT-11:00</option>
                      <option value="Pacific/Samoa">[UTC-11:00] Samoa Standard Time</option>
                      <option value="Pacific/Honolulu">[UTC-10:00] Hawaii Standard Time</option>
                      <option value="US/Alaska">[UTC-09:00] Alaska Standard Time</option>
                      <option value="America/Los_Angeles">[UTC-08:00] Pacific Standard Time</option>
                      <option value="Mexico/BajaNorte">[UTC-08:00] Baja California</option>
                      <option value="US/Arizona">[UTC-07:00] Arizona</option>
                      <option value="US/Mountain">[UTC-07:00] Mountain Standard Time</option>
                      <option value="America/Chihuahua">[UTC-07:00] Chihuahua, La Paz</option>
                      <option value="America/Chicago">[UTC-06:00] Central Standard Time</option>
                      <option value="America/Costa_Rica">[UTC-06:00] Central America</option>
                      <option value="America/Mexico_City">[UTC-06:00] Mexico City, Monterrey</option>
                      <option value="Canada/Saskatchewan">[UTC-06:00] Saskatchewan</option>
                      <option value="America/Bogota">[UTC-05:00] Bogota, Lima</option>
                      <option value="America/New_York">[UTC-05:00] Eastern Standard Time</option>
                      <option value="America/Caracas">[UTC-04:00] Venezuela Time</option>
                      <option value="America/Asuncion">[UTC-04:00] Paraguay Time</option>
                      <option value="America/Cuiaba">[UTC-04:00] Amazon Time</option>
                      <option value="America/Halifax">[UTC-04:00] Atlantic Standard Time</option>
                      <option value="America/La_Paz">[UTC-04:00] Bolivia Time</option>
                      <option value="America/Santiago">[UTC-04:00] Chile Time</option>
                      <option value="America/St_Johns">[UTC-03:30] Newfoundland Standard Time</option>
                      <option value="America/Araguaina">[UTC-03:00] Brasilia Time</option>
                      <option value="America/Argentina/Buenos_Aires">[UTC-03:00] Argentine Time</option>
                      <option value="America/Cayenne">[UTC-03:00] French Guiana Time</option>
                      <option value="America/Godthab">[UTC-03:00] Greenland Time</option>
                      <option value="America/Montevideo">[UTC-03:00] Uruguay Time]</option>
                      <option value="Etc/GMT+2">[UTC-02:00] GMT-02:00</option>
                      <option value="Atlantic/Azores">[UTC-01:00] Azores Time</option>
                      <option value="Atlantic/Cape_Verde">[UTC-01:00] Cape Verde Time</option>
                      <option value="Africa/Casablanca">[UTC] Casablanca</option>
                      <option value="Etc/UTC">[UTC] Coordinated Universal Time</option>
                      <option value="Atlantic/Reykjavik">[UTC] Reykjavik</option>
                      <option value="Europe/London">[UTC] Western European Time</option>
                      <option value="CET">[UTC+01:00] Central European Time</option>
                      <option value="Europe/Bucharest">[UTC+02:00] Eastern European Time</option>
                      <option value="Africa/Johannesburg">[UTC+02:00] South Africa Standard Time</option>
                      <option value="Asia/Beirut">[UTC+02:00] Beirut</option>
                      <option value="Africa/Cairo">[UTC+02:00] Cairo</option>
                      <option value="Asia/Jerusalem">[UTC+02:00] Israel Standard Time</option>
                      <option value="Europe/Minsk">[UTC+02:00] Minsk</option>
                      <option value="Europe/Moscow">[UTC+03:00] Moscow Standard Time</option>
                      <option value="Africa/Nairobi">[UTC+03:00] Eastern African Time</option>
                      <option value="Asia/Karachi">[UTC+05:00] Pakistan Time</option>
                      <option value="Asia/Kolkata">[UTC+05:30] India Standard Time</option>
                      <option value="Asia/Bangkok">[UTC+05:30] Indochina Time</option>
                      <option value="Asia/Shanghai">[UTC+08:00] China Standard Time</option>
                      <option value="Asia/Kuala_Lumpur">[UTC+08:00] Malaysia Time</option>
                      <option value="Australia/Perth">[UTC+08:00] Western Standard Time (Australia)</option>
                      <option value="Asia/Taipei">[UTC+08:00] Taiwan</option>
                      <option value="Asia/Tokyo">[UTC+09:00] Japan Standard Time</option>
                      <option value="Asia/Seoul">[UTC+09:00] Korea Standard Time</option>
                      <option value="Australia/Adelaide">[UTC+09:30] Central Standard Time (South Australia)</option>
                      <option value="Australia/Darwin">[UTC+09:30] Central Standard Time (Northern Territory)</option>
                      <option value="Australia/Brisbane">[UTC+10:00] Eastern Standard Time (Queensland)</option>
                      <option value="Australia/Canberra">[UTC+10:00] Eastern Standard Time (New South Wales)</option>
                      <option value="Pacific/Guam">[UTC+10:00] Chamorro Standard Time</option>
                      <option value="Pacific/Auckland">[UTC+12:00] New Zealand Standard Time</option>
                    </select>
                  </div>
                </div>

                <!-- Max snapshots -->
                <div class="field maxsnaps">
                  <div class="name">Keep</div>
                  <div class="value">
                    <input type="text" name="maxsnaps" class="required" />
                    <label for="maxsnaps">snapshot(s)</label>
                  </div>
                </div>
              </form>
            </div>

            <!-- Daily -->
            <div id="recurring-snapshots-daily">
              <form>
                <input type="hidden" name="snapshot-type" value="daily" />

                <!-- Time -->
                <div class="field time">
                  <div class="name">Time</div>
                  <div class="value">
                    <select name="time-hour"></select>
                    <select name="time-minute"></select>
                    <select name="time-meridiem"></select>
                  </div>
                </div>

                <!-- Timezone -->
                <div class="field timezone">
                  <div class="name">Timezone</div>
                  <div class="value">
                    <select name="timezone"></select>
                  </div>
                </div>

                <!-- Max snapshots -->
                <div class="field maxsnaps">
                  <div class="name">Keep</div>
                  <div class="value">
                    <input type="text" name="maxsnaps" class="required" />
                    <label for="maxsnaps">snapshot(s)</label>
                  </div>
                </div>
              </form>
            </div>

            <!-- Weekly -->
            <div id="recurring-snapshots-weekly">
              <form>
                <input type="hidden" name="snapshot-type" value="weekly" />

                <!-- Time -->
                <div class="field time">
                  <div class="name">Time</div>
                  <div class="value">
                    <select name="time-hour"></select>
                    <select name="time-minute"></select>
                    <select name="time-meridiem"></select>
                  </div>
                </div>

                <!-- Day of week -->
                <div class="field day-of-week">
                  <div class="name">Day of week</div>
                  <div class="value">
                    <select name="day-of-week"></select>
                  </div>
                </div>

                <!-- Timezone -->
                <div class="field timezone">
                  <div class="name">Timezone</div>
                  <div class="value">
                    <select name="timezone"></select>
                  </div>
                </div>

                <!-- Max snapshots -->
                <div class="field maxsnaps">
                  <div class="name">Keep</div>
                  <div class="value">
                    <input type="text" name="maxsnaps" class="required" />
                    <label for="maxsnaps">snapshot(s)</label>
                  </div>
                </div>
              </form>
            </div>

            <!-- Monthly -->
            <div id="recurring-snapshots-monthly">
              <form>
                <input type="hidden" name="snapshot-type" value="monthly" />

                <!-- Time -->
                <div class="field time">
                  <div class="name">Time</div>
                  <div class="value">
                    <select name="time-hour"></select>
                    <select name="time-minute"></select>
                    <select name="time-meridiem"></select>
                  </div>
                </div>

                <!-- Day of week -->
                <div class="field day-of-month">
                  <div class="name">Day of month</div>
                  <div class="value">
                    <select name="day-of-month"></select>
                  </div>
                </div>

                <!-- Timezone -->
                <div class="field timezone">
                  <div class="name">Timezone</div>
                  <div class="value">
                    <select name="timezone"></select>
                  </div>
                </div>

                <!-- Max snapshots -->
                <div class="field maxsnaps">
                  <div class="name">Keep</div>
                  <div class="value">
                    <input type="text" name="maxsnaps" class="required" />
                    <label for="maxsnaps">snapshot(s)</label>
                  </div>
                </div>
              </form>
            </div>
          </div>

          <div class="add-snapshot-actions">
            <div class="add-snapshot-action add">Add</div>
          </div>
        </div>

        <!-- Scheduled snapshots -->
        <div class="scheduled-snapshots">
          <p>Scheduled Snapshots</p>
          <table>
            <tbody>
              <!-- Hourly -->
              <tr class="hourly">
                <td class="time">Time: <span></span> min past the hr</td>
                <td class="day-of-week"><span></span></td>
                <td class="timezone">Timezone:<br/><span></span></td>
                <td class="keep">Keep: <span></span></td>
                <td class="actions"><div class="action destroy"><span class="icon">&nbsp;</span></div></td>
              </tr>

              <!-- Daily -->
              <tr class="daily">
                <td class="time">Time: <span></span></td>
                <td class="day-of-week"><span></span></td>
                <td class="timezone">Timezone:<br/><span></span></td>
                <td class="keep">Keep: <span></span></td>
                <td class="actions"><div class="action destroy"><span class="icon">&nbsp;</span></div></td>
              </tr>

              <!-- Weekly -->
              <tr class="weekly">
                <td class="time">Time: <span></span></td>
                <td class="day-of-week">Every <span></span></td>
                <td class="timezone">Timezone:<br/><span></span></td>
                <td class="keep">Keep: <span></span></td>
                <td class="actions"><div class="action destroy"><span class="icon">&nbsp;</span></div></td>
              </tr>

              <!-- Monthly -->
              <tr class="monthly">
                <td class="time">Time: <span></span></td>
                <td class="day-of-week">Day <span></span> of month</td>
                <td class="timezone">Timezone:<br/><span></span></td>
                <td class="keep">Keep: <span></span></td>
                <td class="actions"><div class="action destroy"><span class="icon">&nbsp;</span></div></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- jQuery -->
    <script src="lib/jquery.js" type="text/javascript"></script>
    <script src="lib/jquery.easing.js" type="text/javascript"></script>
    <script src="lib/jquery.validate.js" type="text/javascript"></script>
    <script src="lib/jquery-ui/js/jquery-ui.js" type="text/javascript"></script>

    <!-- Flot -->
    <script src="lib/excanvas.js"></script>
    <script src="lib/flot/jquery.flot.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.colorhelpers.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.crosshair.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.fillbetween.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.image.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.navigate.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.pie.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.resize.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.selection.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.stack.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.symbol.js" type="text/javascript"></script>
    <script src="lib/flot/jquery.flot.threshold.js" type="text/javascript"></script>

    <!-- UI -->
    <script src="scripts/ui/core.js" type="text/javascript"></script>
    <script src="scripts/ui/utils.js" type="text/javascript"></script>
    <script src="scripts/ui/events.js" type="text/javascript"></script>
    <script src="scripts/ui/dialog.js" type="text/javascript"></script>

    <!-- UI - Widgets -->
    <script src="scripts/ui/widgets/multiEdit.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/overlay.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/dataTable.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/cloudBrowser.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/listView.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/detailView.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/treeView.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/notifications.js" type="text/javascript"></script>

    <!-- Common libraries -->
    <script src="lib/date.js" type="text/javascript"></script>
    <script src="lib/jquery.cookies.js" type="text/javascript"></script>
    <script src="lib/jquery.timers.js" type="text/javascript"></script>
    <script src="lib/jquery.md5.js" type="text/javascript" ></script>

    <!-- CloudStack -->
    <script src="scripts/cloud.core.callbacks.js" type="text/javascript"></script>
    <script src="scripts/sharedFunctions.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/login.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/projects.js" type="text/javascript"></script>
    <script src="scripts/cloudStack.js" type="text/javascript"></script>
    <script src="scripts/lbStickyPolicy.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/zoneChart.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/dashboard.js" type="text/javascript"></script>
    <script src="scripts/installWizard.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/installWizard.js" type="text/javascript"></script>
    <script src="scripts/projects.js" type="text/javascript"></script>
    <script src="scripts/dashboard.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/instanceWizard.js" type="text/javascript"></script>
    <script src="scripts/instances.js" type="text/javascript"></script>
    <script src="scripts/events.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/ipRules.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/enableStaticNAT.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/securityRules.js" type="text/javascript"></script>
    <script src="scripts/network.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/recurringSnapshots.js" type="text/javascript"></script>
    <script src="scripts/storage.js" type="text/javascript"></script>
    <script src="scripts/templates.js" type="text/javascript"></script>
    <script src="scripts/accounts.js" type="text/javascript"></script>
    <script src="scripts/configuration.js" type="text/javascript"></script>
    <script src="scripts/globalSettings.js" type="text/javascript"></script>
    <script src="scripts/zoneWizard.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/physicalResources.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/zoneWizard.js" type="text/javascript"></script>
    <script src="scripts/system.js" type="text/javascript"></script>
    <script src="scripts/domains.js" type="text/javascript"></script>

    <!-- Local testing-->
    <!--
    <script src="js-test/accounts.js" type="text/javascript"></script>
    <script src="js-test/configuration.js" type="text/javascript"></script>
    <script src="js-test/dashboard.js" type="text/javascript"></script>
    <script src="js-test/domains.js" type="text/javascript"></script>
    <script src="js-test/events.js" type="text/javascript"></script>
    <script src="js-test/instances.js" type="text/javascript"></script>
    <script src="js-test/network.js" type="text/javascript"></script>
    <script src="js-test/storage.js" type="text/javascript"></script>
    <script src="js-test/system.js" type="text/javascript"></script>
    <script src="js-test/templates.js" type="text/javascript"></script>
    -->
  </body>
</html>
