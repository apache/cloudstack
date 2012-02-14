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
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
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
                  <p>A zone typically corresponds to a single datacenter. Multiple zones help make the cloud more reliable by providing physical isolation and redundancy.</p>
                  <div class="select-area">
                    <div class="desc"></div>
                    <select name="zoneid" class="required">
                      <option default="default" value="" >Select a zone</option>
                    </select>
                  </div>
                </div>

                <!-- Select template -->
                <div class="section select-template">
                  <h3>Select ISO or template</h3>
                  <p></p>
                  <div class="select-area">
                    <div class="desc">OS image that can be used to boot VMs</div>
                    <input type="radio" name="select-template" value="select-template" />
                    <label>Template</label>
                  </div>
                  <div class="select-area">
                    <div class="desc">Disc image containing data or bootable media for OS</div>
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
									  <li class="first"><a href="#instance-wizard-featured-isos">Featured</a></li>
                    <li><a href="#instance-wizard-community-isos">Community</a></li>
                    <li class="last"><a href="#instance-wizard-my-isos">My ISO</a></li>		
                  </ul>
									<div id="instance-wizard-featured-isos">
                    <div class="select-container">
                    </div>
                  </div>
									<div id="instance-wizard-community-isos">
                    <div class="select-container">
                    </div>
                  </div>
									<div id="instance-wizard-my-isos">
                    <div class="select-container">
                    </div>
                  </div>
																											
									<!--
									<ul>
                    <li class="first last"><a href="#instance-wizard-all-isos">All ISOs</a></li>
                  </ul>
                  <div id="instance-wizard-all-isos">
                    <div class="select-container">
                    </div>
                  </div>
									-->
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
              <li class="conditional elb physical-network active">Netscaler</li>
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
              <li class="conditional elb physical-network">Netscaler</li>
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
              <li class="conditional elb physical-network">Netscaler</li>
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
              <li class="conditional elb physical-network">Netscaler</li>
              <li class="public-network">Public traffic</li>
              <li class="pod">Pod</li>
              <li class="guest-traffic active">Guest Traffic</li>
            </ul>

            <div class="info-desc">
              Guest network traffic is communication between end-user virtual machines.  
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
            <div class="title"><span></span></div>
            <div class="button view-all"></div>
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
            <div class="title"><span></span></div>
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
              <span></span>
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
              <li class="hourly"><a href="#recurring-snapshots-hourly"></a></li>
              <li class="daily"><a href="#recurring-snapshots-daily"></a></li>
              <li class="weekly"><a href="#recurring-snapshots-weekly"></a></li>
              <li class="monthly"><a href="#recurring-snapshots-monthly"></a></li>
            </ul>

            <!-- Hourly -->
            <div id="recurring-snapshots-hourly">
              <form>
                <input type="hidden" name="snapshot-type" value="hourly" />

                <!-- Time -->
                <div class="field time">
                  <div class="name"></div>
                  <div class="value">
                    <select name="schedule"></select>
                    <label for="schedule">minutes(s) past the hour</label>
                  </div>
                </div>

                <!-- Timezone -->
                <div class="field timezone">
                  <div class="name"></div>
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
            <div class="add-snapshot-action add"></div>
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

<script language="javascript">
dictionary = {
'label.add.vm': '<fmt:message key="label.add.vm"/>',
'label.full.path': '<fmt:message key="label.full.path"/>',
'message.add.domain': '<fmt:message key="message.add.domain"/>',
'message.delete.user': '<fmt:message key="message.delete.user"/>',
'message.enable.user': '<fmt:message key="message.enable.user"/>',
'message.disable.user': '<fmt:message key="message.disable.user"/>',
'message.generate.keys': '<fmt:message key="message.generate.keys"/>',
'message.update.resource.count': '<fmt:message key="message.update.resource.count"/>',
'message.edit.account': '<fmt:message key="message.edit.account"/>',
'label.totoal.of.ip': '<fmt:message key="label.totoal.of.ip"/>',
'label.total.of.vm': '<fmt:message key="label.total.of.vm"/>',
'state.enabled': '<fmt:message key="state.enabled"/>',
'message.action.download.iso': '<fmt:message key="message.action.download.iso"/>',
'message.action.download.template': '<fmt:message key="message.action.download.template"/>',
'label.destination.zone': '<fmt:message key="label.destination.zone"/>',
'label.nic.adapter.type': '<fmt:message key="label.nic.adapter.type"/>',
'label.keyboard.type': '<fmt:message key="label.keyboard.type"/>',
'label.root.disk.controller': '<fmt:message key="label.root.disk.controller"/>',
'label.community': '<fmt:message key="label.community"/>',
'label.remove.egress.rule': '<fmt:message key="label.remove.egress.rule"/>',
'label.add.egress.rule': '<fmt:message key="label.add.egress.rule"/>',
'label.egress.rule': '<fmt:message key="label.egress.rule"/>',
'label.remove.ingress.rule': '<fmt:message key="label.remove.ingress.rule"/>',
'label.delete.vpn.user': '<fmt:message key="label.delete.vpn.user"/>',
'label.add.vpn.user': '<fmt:message key="label.add.vpn.user"/>',
'label.remove.pf': '<fmt:message key="label.remove.pf"/>',
'label.remove.vm.from.lb': '<fmt:message key="label.remove.vm.from.lb"/>',
'label.add.vms.to.lb': '<fmt:message key="label.add.vms.to.lb"/>',
'label.remove.static.nat.rule': '<fmt:message key="label.remove.static.nat.rule"/>',
'label.remove.rule': '<fmt:message key="label.remove.rule"/>',
'label.add.static.nat.rule': '<fmt:message key="label.add.static.nat.rule"/>',
'label.add.rule': '<fmt:message key="label.add.rule"/>',
'label.configuration': '<fmt:message key="label.configuration"/>',
'message.disable.vpn': '<fmt:message key="message.disable.vpn"/>',
'label.disable.vpn': '<fmt:message key="label.disable.vpn"/>',
'message.enable.vpn': '<fmt:message key="message.enable.vpn"/>',
'label.enable.vpn': '<fmt:message key="label.enable.vpn"/>',
'message.acquire.new.ip': '<fmt:message key="message.acquire.new.ip"/>',
'label.elastic': '<fmt:message key="label.elastic"/>',
'label.my.network': '<fmt:message key="label.my.network" />',
'label.add.vms': '<fmt:message key="label.add.vms" />',
'label.configure': '<fmt:message key="label.configure" />',
'label.stickiness': '<fmt:message key="label.stickiness" />',
'label.source': '<fmt:message key="label.source" />',
'label.least.connections': '<fmt:message key="label.least.connections" />',
'label.round.robin': '<fmt:message key="label.round.robin" />',
'label.network.domain.text': '<fmt:message key="label.network.domain.text" />',
'label.restart.required': '<fmt:message key="label.restart.required" />',
'label.clean.up': '<fmt:message key="label.clean.up" />',
'message.restart.network': '<fmt:message key="message.restart.network" />',
'label.restart.network': '<fmt:message key="label.restart.network" />',
'label.edit.network.details': '<fmt:message key="label.edit.network.details" />',
'message.add.guest.network': '<fmt:message key="message.add.guest.network" />',
'label.add.guest.network': '<fmt:message key="label.add.guest.network" />',
'label.guest.networks': '<fmt:message key="label.guest.networks" />',
'message.ip.address.changed': '<fmt:message key="message.ip.address.changed" />',
'state.BackingUp': '<fmt:message key="state.BackingUp" />',
'state.BackedUp': '<fmt:message key="state.BackedUp" />',
'label.done': '<fmt:message key="label.done" />',
'label.vm.name': '<fmt:message key="label.vm.name" />',
'message.migrate.volume': '<fmt:message key="message.migrate.volume" />',
'label.migrate.volume': '<fmt:message key="label.migrate.volume" />',
'message.create.template': '<fmt:message key="message.create.template" />',
'label.create.template': '<fmt:message key="label.create.template" />',
'message.download.volume.confirm': '<fmt:message key="message.download.volume.confirm" />',
'message.detach.disk': '<fmt:message key="message.detach.disk" />',
'state.Ready': '<fmt:message key="state.Ready" />',
'label.vm.display.name': '<fmt:message key="label.vm.display.name" />',
'label.select-view': '<fmt:message key="label.select-view" />',
'label.local.storage': '<fmt:message key="label.local.storage" />',
'label.direct.ips': '<fmt:message key="label.direct.ips" />',
'label.view.all': '<fmt:message key="label.view.all" />',
'label.zone.details': '<fmt:message key="label.zone.details" />',
'message.alert.state.detected': '<fmt:message key="message.alert.state.detected" />',
'state.Starting': '<fmt:message key="state.Starting" />',
'state.Expunging': '<fmt:message key="state.Expunging" />',
'state.Creating': '<fmt:message key="state.Creating" />',
'message.decline.invitation': '<fmt:message key="message.decline.invitation" />',
'label.decline.invitation': '<fmt:message key="label.decline.invitation" />',
'message.confirm.join.project': '<fmt:message key="message.confirm.join.project" />',
'message.join.project': '<fmt:message key="message.join.project" />',
'label.accept.project.invitation': '<fmt:message key="label.accept.project.invitation" />',
'label.token': '<fmt:message key="label.token" />',
'label.project.id': '<fmt:message key="label.project.id" />',
'message.enter.token': '<fmt:message key="message.enter.token" />',
'label.enter.token': '<fmt:message key="label.enter.token" />',
'state.Accepted': '<fmt:message key="state.Accepted" />',
'state.Pending': '<fmt:message key="state.Pending" />',
'state.Completed': '<fmt:message key="state.Completed" />',
'state.Declined': '<fmt:message key="state.Declined" />',
'label.project': '<fmt:message key="label.project" />',
'label.invitations': '<fmt:message key="label.invitations" />',
'label.delete.project': '<fmt:message key="label.delete.project" />',
'message.delete.project': '<fmt:message key="message.delete.project" />',
'message.activate.project': '<fmt:message key="message.activate.project" />',
'label.activate.project': '<fmt:message key="label.activate.project" />',
'label.suspend.project': '<fmt:message key="label.suspend.project" />',
'message.suspend.project': '<fmt:message key="message.suspend.project" />',
'state.Suspended': '<fmt:message key="state.Suspended" />',
'label.edit.project.details': '<fmt:message key="label.edit.project.details" />',
'label.new.project': '<fmt:message key="label.new.project" />',
'state.Active': '<fmt:message key="state.Active" />',
'state.Disabled': '<fmt:message key="state.Disabled" />',
'label.projects': '<fmt:message key="label.projects" />',
'label.make.project.owner': '<fmt:message key="label.make.project.owner" />',
'label.remove.project.account': '<fmt:message key="label.remove.project.account" />',
'message.project.invite.sent': '<fmt:message key="message.project.invite.sent" />',
'label.add.account.to.project': '<fmt:message key="label.add.account.to.project" />',
'label.revoke.project.invite': '<fmt:message key="label.revoke.project.invite" />',
'label.project.invite': '<fmt:message key="label.project.invite" />',
'label.select.project': '<fmt:message key="label.select.project" />',
'message.no.projects': '<fmt:message key="message.no.projects" />',
'message.no.projects.adminOnly': '<fmt:message key="message.no.projects.adminOnly" />',
'message.pending.projects.1': '<fmt:message key="message.pending.projects.1" />',
'message.pending.projects.2': '<fmt:message key="message.pending.projects.2" />',
'message.instanceWizard.noTemplates': '<fmt:message key="message.instanceWizard.noTemplates" />',
'label.view': '<fmt:message key="label.view" />',
'create.template.complete': '<fmt:message key="create.template.complete" />',
'create.template.confirm': '<fmt:message key="create.template.confirm" />',
'create.template.notification': '<fmt:message key="create.template.notification" />',
'create.template.success': '<fmt:message key="create.template.success" />',
'instances.actions.reboot.label': '<fmt:message key="instances.actions.reboot.label" />',
'label.filterBy': '<fmt:message key="label.filterBy" />',
'label.ok': '<fmt:message key="label.ok" />',
'notification.reboot.instance': '<fmt:message key="notification.reboot.instance" />',
'notification.start.instance': '<fmt:message key="notification.start.instance" />',
'notification.stop.instance': '<fmt:message key="notification.stop.instance" />',
'label.display.name': '<fmt:message key="label.display.name" />',
'label.zone.name': '<fmt:message key="label.zone.name" />',
'ui.listView.filters.all': '<fmt:message key="ui.listView.filters.all" />',
'ui.listView.filters.mine': '<fmt:message key="ui.listView.filters.mine" />',
'state.Running': '<fmt:message key="state.Running" />',
'state.Stopped': '<fmt:message key="state.Stopped" />',
'state.Destroyed': '<fmt:message key="state.Destroyed" />',
'state.Error': '<fmt:message key="state.Error" />',
'message.reset.password.warning.notPasswordEnabled': '<fmt:message key="message.reset.password.warning.notPasswordEnabled" />',
'message.reset.password.warning.notStopped': '<fmt:message key="message.reset.password.warning.notStopped" />',
'label.notifications': '<fmt:message key="label.notifications" />',
'label.default.view': '<fmt:message key="label.default.view" />',
'label.project.view': '<fmt:message key="label.project.view" />',
'label.action.migrate.router': '<fmt:message key="label.action.migrate.router" />',
'label.action.migrate.router.processing': '<fmt:message key="label.action.migrate.router.processing" />',
'message.migrate.router.confirm': '<fmt:message key="message.migrate.router.confirm" />',
'label.migrate.router.to': '<fmt:message key="label.migrate.router.to" />',
'label.action.migrate.systemvm': '<fmt:message key="label.action.migrate.systemvm" />',
'label.action.migrate.systemvm.processing': '<fmt:message key="label.action.migrate.systemvm.processing" />',
'message.migrate.systemvm.confirm': '<fmt:message key="message.migrate.systemvm.confirm" />',
'label.migrate.systemvm.to': '<fmt:message key="label.migrate.systemvm.to" />',
'mode': '<fmt:message key="mode" />',
'side.by.side': '<fmt:message key="side.by.side" />',
'inline': '<fmt:message key="inline" />',
'extractable': '<fmt:message key="extractable" />',
'label.ocfs2': '<fmt:message key="label.ocfs2" />',
'label.action.edit.host': '<fmt:message key="label.action.edit.host" />',
'network.rate': '<fmt:message key="network.rate" />',
'ICMP.type': '<fmt:message key="ICMP.type" />',
'ICMP.code': '<fmt:message key="ICMP.code" />',
'image.directory': '<fmt:message key="image.directory" />',
'label.action.create.template.from.vm': '<fmt:message key="label.action.create.template.from.vm" />',
'label.action.create.template.from.volume': '<fmt:message key="label.action.create.template.from.volume" />',
'message.vm.create.template.confirm': '<fmt:message key="message.vm.create.template.confirm" />',
'label.action.manage.cluster': '<fmt:message key="label.action.manage.cluster" />',
'message.action.manage.cluster': '<fmt:message key="message.action.manage.cluster" />',
'label.action.manage.cluster.processing': '<fmt:message key="label.action.manage.cluster.processing" />',
'label.action.unmanage.cluster': '<fmt:message key="label.action.unmanage.cluster" />',
'message.action.unmanage.cluster': '<fmt:message key="message.action.unmanage.cluster" />',
'label.action.unmanage.cluster.processing': '<fmt:message key="label.action.unmanage.cluster.processing" />',
'allocation.state': '<fmt:message key="allocation.state" />',
'managed.state': '<fmt:message key="managed.state" />',
'label.default.use': '<fmt:message key="label.default.use" />',
'label.host.tags': '<fmt:message key="label.host.tags" />',
'label.cidr': '<fmt:message key="label.cidr" />',
'label.cidr.list': '<fmt:message key="label.cidr.list" />',
'label.storage.tags': '<fmt:message key="label.storage.tags" />',
'label.redundant.router': '<fmt:message key="label.redundant.router" />',
'label.is.redundant.router': '<fmt:message key="label.is.redundant.router" />',
'force.delete': '<fmt:message key="force.delete" />',
'force.delete.domain.warning': '<fmt:message key="force.delete.domain.warning" />',
'force.remove': '<fmt:message key="force.remove" />',
'force.remove.host.warning': '<fmt:message key="force.remove.host.warning" />',
'force.stop': '<fmt:message key="force.stop" />',
'force.stop.instance.warning': '<fmt:message key="force.stop.instance.warning" />',
'label.PreSetup': '<fmt:message key="label.PreSetup" />',
'label.SR.name ': '<fmt:message key="label.SR.name " />',
'label.SharedMountPoint': '<fmt:message key="label.SharedMountPoint" />',
'label.clvm': '<fmt:message key="label.clvm" />',
'label.volgroup': '<fmt:message key="label.volgroup" />',
'label.VMFS.datastore': '<fmt:message key="label.VMFS.datastore" />',
'label.network.device': '<fmt:message key="label.network.device" />',
'label.add.network.device': '<fmt:message key="label.add.network.device" />',
'label.network.device.type': '<fmt:message key="label.network.device.type" />',
'label.DHCP.server.type': '<fmt:message key="label.DHCP.server.type" />',
'label.Pxe.server.type': '<fmt:message key="label.Pxe.server.type" />',
'label.PING.storage.IP': '<fmt:message key="label.PING.storage.IP" />',
'label.PING.dir': '<fmt:message key="label.PING.dir" />',
'label.TFTP.dir': '<fmt:message key="label.TFTP.dir" />',
'label.PING.CIFS.username': '<fmt:message key="label.PING.CIFS.username" />',
'label.PING.CIFS.password': '<fmt:message key="label.PING.CIFS.password" />',
'label.CPU.cap': '<fmt:message key="label.CPU.cap" />',
'label.action.enable.zone': '<fmt:message key="label.action.enable.zone" />',
'label.action.enable.zone.processing': '<fmt:message key="label.action.enable.zone.processing" />',
'message.action.enable.zone': '<fmt:message key="message.action.enable.zone" />',
'label.action.disable.zone': '<fmt:message key="label.action.disable.zone" />',
'label.action.disable.zone.processing': '<fmt:message key="label.action.disable.zone.processing" />',
'message.action.disable.zone': '<fmt:message key="message.action.disable.zone" />',
'label.action.enable.pod': '<fmt:message key="label.action.enable.pod" />',
'label.action.enable.pod.processing': '<fmt:message key="label.action.enable.pod.processing" />',
'message.action.enable.pod': '<fmt:message key="message.action.enable.pod" />',
'label.action.disable.pod': '<fmt:message key="label.action.disable.pod" />',
'label.action.disable.pod.processing': '<fmt:message key="label.action.disable.pod.processing" />',
'message.action.disable.pod': '<fmt:message key="message.action.disable.pod" />',
'label.action.enable.cluster': '<fmt:message key="label.action.enable.cluster" />',
'label.action.enable.cluster.processing': '<fmt:message key="label.action.enable.cluster.processing" />',
'message.action.enable.cluster': '<fmt:message key="message.action.enable.cluster" />',
'label.action.disable.cluster': '<fmt:message key="label.action.disable.cluster" />',
'label.action.disable.cluster.processing': '<fmt:message key="label.action.disable.cluster.processing" />',
'message.action.disable.cluster': '<fmt:message key="message.action.disable.cluster" />',
'label.account.id': '<fmt:message key="label.account.id" />',
'label.account.name': '<fmt:message key="label.account.name" />',
'label.account.specific': '<fmt:message key="label.account.specific" />',
'label.account': '<fmt:message key="label.account" />',
'label.accounts': '<fmt:message key="label.accounts" />',
'label.acquire.new.ip': '<fmt:message key="label.acquire.new.ip" />',
'label.show.ingress.rule': '<fmt:message key="label.show.ingress.rule" />',
'label.hide.ingress.rule': '<fmt:message key="label.hide.ingress.rule" />',
'label.action.attach.disk.processing': '<fmt:message key="label.action.attach.disk.processing" />',
'label.action.attach.disk': '<fmt:message key="label.action.attach.disk" />',
'label.action.attach.iso.processing': '<fmt:message key="label.action.attach.iso.processing" />',
'label.action.attach.iso': '<fmt:message key="label.action.attach.iso" />',
'label.action.cancel.maintenance.mode.processing': '<fmt:message key="label.action.cancel.maintenance.mode.processing" />',
'label.action.cancel.maintenance.mode': '<fmt:message key="label.action.cancel.maintenance.mode" />',
'label.action.change.password': '<fmt:message key="label.action.change.password" />',
'label.action.change.service.processing': '<fmt:message key="label.action.change.service.processing" />',
'label.action.change.service': '<fmt:message key="label.action.change.service" />',
'label.action.copy.ISO.processing': '<fmt:message key="label.action.copy.ISO.processing" />',
'label.action.copy.ISO': '<fmt:message key="label.action.copy.ISO" />',
'label.action.copy.template.processing': '<fmt:message key="label.action.copy.template.processing" />',
'label.action.copy.template': '<fmt:message key="label.action.copy.template" />',
'label.action.create.template.processing': '<fmt:message key="label.action.create.template.processing" />',
'label.action.create.template': '<fmt:message key="label.action.create.template" />',
'label.action.create.vm.processing': '<fmt:message key="label.action.create.vm.processing" />',
'label.action.create.vm': '<fmt:message key="label.action.create.vm" />',
'label.action.create.volume.processing': '<fmt:message key="label.action.create.volume.processing" />',
'label.action.create.volume': '<fmt:message key="label.action.create.volume" />',
'label.action.delete.IP.range.processing': '<fmt:message key="label.action.delete.IP.range.processing" />',
'label.action.delete.IP.range': '<fmt:message key="label.action.delete.IP.range" />',
'label.action.delete.ISO.processing': '<fmt:message key="label.action.delete.ISO.processing" />',
'label.action.delete.ISO': '<fmt:message key="label.action.delete.ISO" />',
'label.action.delete.account.processing': '<fmt:message key="label.action.delete.account.processing" />',
'label.action.delete.account': '<fmt:message key="label.action.delete.account" />',
'label.action.delete.cluster.processing': '<fmt:message key="label.action.delete.cluster.processing" />',
'label.action.delete.cluster': '<fmt:message key="label.action.delete.cluster" />',
'label.action.delete.disk.offering.processing': '<fmt:message key="label.action.delete.disk.offering.processing" />',
'label.action.delete.disk.offering': '<fmt:message key="label.action.delete.disk.offering" />',
'label.action.update.resource.count': '<fmt:message key="label.action.update.resource.count" />',
'label.action.update.resource.count.processing': '<fmt:message key="label.action.update.resource.count.processing" />',
'label.action.delete.domain': '<fmt:message key="label.action.delete.domain" />',
'label.action.delete.domain.processing': '<fmt:message key="label.action.delete.domain.processing" />',
'label.action.delete.firewall.processing': '<fmt:message key="label.action.delete.firewall.processing" />',
'label.action.delete.firewall': '<fmt:message key="label.action.delete.firewall" />',
'label.action.delete.ingress.rule.processing': '<fmt:message key="label.action.delete.ingress.rule.processing" />',
'label.action.delete.ingress.rule': '<fmt:message key="label.action.delete.ingress.rule" />',
'label.action.delete.load.balancer.processing': '<fmt:message key="label.action.delete.load.balancer.processing" />',
'label.action.delete.load.balancer': '<fmt:message key="label.action.delete.load.balancer" />',
'label.action.edit.network.processing': '<fmt:message key="label.action.edit.network.processing" />',
'label.action.edit.network': '<fmt:message key="label.action.edit.network" />',
'label.action.delete.network.processing': '<fmt:message key="label.action.delete.network.processing" />',
'label.action.delete.network': '<fmt:message key="label.action.delete.network" />',
'label.action.delete.pod.processing': '<fmt:message key="label.action.delete.pod.processing" />',
'label.action.delete.pod': '<fmt:message key="label.action.delete.pod" />',
'label.action.delete.primary.storage.processing': '<fmt:message key="label.action.delete.primary.storage.processing" />',
'label.action.delete.primary.storage': '<fmt:message key="label.action.delete.primary.storage" />',
'label.action.delete.secondary.storage.processing': '<fmt:message key="label.action.delete.secondary.storage.processing" />',
'label.action.delete.secondary.storage': '<fmt:message key="label.action.delete.secondary.storage" />',
'label.action.delete.security.group.processing': '<fmt:message key="label.action.delete.security.group.processing" />',
'label.action.delete.security.group': '<fmt:message key="label.action.delete.security.group" />',
'label.action.delete.service.offering.processing': '<fmt:message key="label.action.delete.service.offering.processing" />',
'label.action.delete.service.offering': '<fmt:message key="label.action.delete.service.offering" />',
'label.action.delete.snapshot.processing': '<fmt:message key="label.action.delete.snapshot.processing" />',
'label.action.delete.snapshot': '<fmt:message key="label.action.delete.snapshot" />',
'label.action.delete.template.processing': '<fmt:message key="label.action.delete.template.processing" />',
'label.action.delete.template': '<fmt:message key="label.action.delete.template" />',
'label.action.delete.user.processing': '<fmt:message key="label.action.delete.user.processing" />',
'label.action.delete.user': '<fmt:message key="label.action.delete.user" />',
'label.action.delete.volume.processing': '<fmt:message key="label.action.delete.volume.processing" />',
'label.action.delete.volume': '<fmt:message key="label.action.delete.volume" />',
'label.action.delete.zone.processing': '<fmt:message key="label.action.delete.zone.processing" />',
'label.action.delete.zone': '<fmt:message key="label.action.delete.zone" />',
'label.action.destroy.instance.processing': '<fmt:message key="label.action.destroy.instance.processing" />',
'label.action.destroy.instance': '<fmt:message key="label.action.destroy.instance" />',
'label.action.destroy.systemvm.processing': '<fmt:message key="label.action.destroy.systemvm.processing" />',
'label.action.destroy.systemvm': '<fmt:message key="label.action.destroy.systemvm" />',
'label.action.detach.disk.processing': '<fmt:message key="label.action.detach.disk.processing" />',
'label.action.detach.disk': '<fmt:message key="label.action.detach.disk" />',
'label.action.detach.iso.processing': '<fmt:message key="label.action.detach.iso.processing" />',
'label.action.detach.iso': '<fmt:message key="label.action.detach.iso" />',
'label.action.disable.account.processing': '<fmt:message key="label.action.disable.account.processing" />',
'label.action.disable.account': '<fmt:message key="label.action.disable.account" />',
'label.action.disable.static.NAT.processing': '<fmt:message key="label.action.disable.static.NAT.processing" />',
'label.action.disable.static.NAT': '<fmt:message key="label.action.disable.static.NAT" />',
'label.action.disable.user.processing': '<fmt:message key="label.action.disable.user.processing" />',
'label.action.disable.user': '<fmt:message key="label.action.disable.user" />',
'label.action.download.ISO': '<fmt:message key="label.action.download.ISO" />',
'label.action.download.template': '<fmt:message key="label.action.download.template" />',
'label.action.download.volume.processing': '<fmt:message key="label.action.download.volume.processing" />',
'label.action.download.volume': '<fmt:message key="label.action.download.volume" />',
'label.action.edit.ISO': '<fmt:message key="label.action.edit.ISO" />',
'label.action.edit.account': '<fmt:message key="label.action.edit.account" />',
'label.action.edit.disk.offering': '<fmt:message key="label.action.edit.disk.offering" />',
'label.action.edit.domain': '<fmt:message key="label.action.edit.domain" />',
'label.action.edit.global.setting': '<fmt:message key="label.action.edit.global.setting" />',
'label.action.edit.instance': '<fmt:message key="label.action.edit.instance" />',
'label.action.edit.network.offering': '<fmt:message key="label.action.edit.network.offering" />',
'label.action.edit.pod': '<fmt:message key="label.action.edit.pod" />',
'label.action.edit.primary.storage': '<fmt:message key="label.action.edit.primary.storage" />',
'label.action.edit.resource.limits': '<fmt:message key="label.action.edit.resource.limits" />',
'label.action.edit.service.offering': '<fmt:message key="label.action.edit.service.offering" />',
'label.action.edit.template': '<fmt:message key="label.action.edit.template" />',
'label.action.edit.user': '<fmt:message key="label.action.edit.user" />',
'label.action.edit.zone': '<fmt:message key="label.action.edit.zone" />',
'label.action.enable.account.processing': '<fmt:message key="label.action.enable.account.processing" />',
'label.action.enable.account': '<fmt:message key="label.action.enable.account" />',
'label.action.enable.maintenance.mode.processing': '<fmt:message key="label.action.enable.maintenance.mode.processing" />',
'label.action.enable.maintenance.mode': '<fmt:message key="label.action.enable.maintenance.mode" />',
'label.action.enable.static.NAT.processing': '<fmt:message key="label.action.enable.static.NAT.processing" />',
'label.action.enable.static.NAT': '<fmt:message key="label.action.enable.static.NAT" />',
'label.action.enable.user.processing': '<fmt:message key="label.action.enable.user.processing" />',
'label.action.enable.user': '<fmt:message key="label.action.enable.user" />',
'label.action.force.reconnect.processing': '<fmt:message key="label.action.force.reconnect.processing" />',
'label.action.force.reconnect': '<fmt:message key="label.action.force.reconnect" />',
'label.action.generate.keys.processing': '<fmt:message key="label.action.generate.keys.processing" />',
'label.action.generate.keys': '<fmt:message key="label.action.generate.keys" />',
'label.action.lock.account.processing': '<fmt:message key="label.action.lock.account.processing" />',
'label.action.lock.account': '<fmt:message key="label.action.lock.account" />',
'label.action.migrate.instance': '<fmt:message key="label.action.migrate.instance" />',
'label.action.migrate.instance.processing': '<fmt:message key="label.action.migrate.instance.processing" />',
'label.action.reboot.instance.processing': '<fmt:message key="label.action.reboot.instance.processing" />',
'label.action.reboot.instance': '<fmt:message key="label.action.reboot.instance" />',
'label.action.reboot.router.processing': '<fmt:message key="label.action.reboot.router.processing" />',
'label.action.reboot.router': '<fmt:message key="label.action.reboot.router" />',
'label.action.reboot.systemvm.processing': '<fmt:message key="label.action.reboot.systemvm.processing" />',
'label.action.reboot.systemvm': '<fmt:message key="label.action.reboot.systemvm" />',
'label.action.recurring.snapshot': '<fmt:message key="label.action.recurring.snapshot" />',
'label.action.release.ip.processing': '<fmt:message key="label.action.release.ip.processing" />',
'label.action.release.ip': '<fmt:message key="label.action.release.ip" />',
'label.action.remove.host.processing': '<fmt:message key="label.action.remove.host.processing" />',
'label.action.remove.host': '<fmt:message key="label.action.remove.host" />',
'label.action.reset.password.processing': '<fmt:message key="label.action.reset.password.processing" />',
'label.action.reset.password': '<fmt:message key="label.action.reset.password" />',
'label.action.resource.limits': '<fmt:message key="label.action.resource.limits" />',
'label.action.restore.instance.processing': '<fmt:message key="label.action.restore.instance.processing" />',
'label.action.restore.instance': '<fmt:message key="label.action.restore.instance" />',
'label.action.start.instance.processing': '<fmt:message key="label.action.start.instance.processing" />',
'label.action.start.instance': '<fmt:message key="label.action.start.instance" />',
'label.action.start.router.processing': '<fmt:message key="label.action.start.router.processing" />',
'label.action.start.router': '<fmt:message key="label.action.start.router" />',
'label.action.start.systemvm.processing': '<fmt:message key="label.action.start.systemvm.processing" />',
'label.action.start.systemvm': '<fmt:message key="label.action.start.systemvm" />',
'label.action.stop.instance.processing': '<fmt:message key="label.action.stop.instance.processing" />',
'label.action.stop.instance': '<fmt:message key="label.action.stop.instance" />',
'label.action.stop.router.processing': '<fmt:message key="label.action.stop.router.processing" />',
'label.action.stop.router': '<fmt:message key="label.action.stop.router" />',
'label.action.stop.systemvm.processing': '<fmt:message key="label.action.stop.systemvm.processing" />',
'label.action.stop.systemvm': '<fmt:message key="label.action.stop.systemvm" />',
'label.action.take.snapshot.processing': '<fmt:message key="label.action.take.snapshot.processing" />',
'label.action.take.snapshot': '<fmt:message key="label.action.take.snapshot" />',
'label.action.update.OS.preference.processing': '<fmt:message key="label.action.update.OS.preference.processing" />',
'label.action.update.OS.preference': '<fmt:message key="label.action.update.OS.preference" />',
'label.actions': '<fmt:message key="label.actions" />',
'label.active.sessions': '<fmt:message key="label.active.sessions" />',
'label.add.account': '<fmt:message key="label.add.account" />',
'label.add.by.cidr': '<fmt:message key="label.add.by.cidr" />',
'label.add.by.group': '<fmt:message key="label.add.by.group" />',
'label.add.cluster': '<fmt:message key="label.add.cluster" />',
'label.add.direct.iprange': '<fmt:message key="label.add.direct.iprange" />',
'label.add.disk.offering': '<fmt:message key="label.add.disk.offering" />',
'label.add.domain': '<fmt:message key="label.add.domain" />',
'label.add.firewall': '<fmt:message key="label.add.firewall" />',
'label.add.host': '<fmt:message key="label.add.host" />',
'label.add.ingress.rule': '<fmt:message key="label.add.ingress.rule" />',
'label.add.ip.range': '<fmt:message key="label.add.ip.range" />',
'label.add.iso': '<fmt:message key="label.add.iso" />',
'label.add.load.balancer': '<fmt:message key="label.add.load.balancer" />',
'label.add.more': '<fmt:message key="label.add.more" />',
'label.add.network': '<fmt:message key="label.add.network" />',
'label.add.pod': '<fmt:message key="label.add.pod" />',
'label.add.primary.storage': '<fmt:message key="label.add.primary.storage" />',
'label.add.secondary.storage': '<fmt:message key="label.add.secondary.storage" />',
'label.add.security.group': '<fmt:message key="label.add.security.group" />',
'label.add.service.offering': '<fmt:message key="label.add.service.offering" />',
'label.add.system.service.offering': '<fmt:message key="label.add.system.service.offering" />',
'label.add.template': '<fmt:message key="label.add.template" />',
'label.add.user': '<fmt:message key="label.add.user" />',
'label.add.vlan': '<fmt:message key="label.add.vlan" />',
'label.add.volume': '<fmt:message key="label.add.volume" />',
'label.add.zone': '<fmt:message key="label.add.zone" />',
'label.add': '<fmt:message key="label.add" />',
'label.adding.cluster': '<fmt:message key="label.adding.cluster" />',
'label.adding.failed': '<fmt:message key="label.adding.failed" />',
'label.adding.pod': '<fmt:message key="label.adding.pod" />',
'label.adding.processing': '<fmt:message key="label.adding.processing" />',
'label.adding.succeeded': '<fmt:message key="label.adding.succeeded" />',
'label.adding.user': '<fmt:message key="label.adding.user" />',
'label.adding.zone': '<fmt:message key="label.adding.zone" />',
'label.adding': '<fmt:message key="label.adding" />',
'label.additional.networks': '<fmt:message key="label.additional.networks" />',
'label.admin.accounts': '<fmt:message key="label.admin.accounts" />',
'label.admin': '<fmt:message key="label.admin" />',
'label.advanced.mode': '<fmt:message key="label.advanced.mode" />',
'label.advanced.search': '<fmt:message key="label.advanced.search" />',
'label.advanced': '<fmt:message key="label.advanced" />',
'label.alert': '<fmt:message key="label.alert" />',
'label.algorithm': '<fmt:message key="label.algorithm" />',
'label.allocated': '<fmt:message key="label.allocated" />',
'label.api.key': '<fmt:message key="label.api.key" />',
'label.assign.to.load.balancer': '<fmt:message key="label.assign.to.load.balancer" />',
'label.assign': '<fmt:message key="label.assign" />',
'label.associated.network.id': '<fmt:message key="label.associated.network.id" />',
'label.attached.iso': '<fmt:message key="label.attached.iso" />',
'label.availability.zone': '<fmt:message key="label.availability.zone" />',
'label.availability': '<fmt:message key="label.availability" />',
'label.available.public.ips': '<fmt:message key="label.available.public.ips" />',
'label.available': '<fmt:message key="label.available" />',
'label.back': '<fmt:message key="label.back" />',
'label.basic.mode': '<fmt:message key="label.basic.mode" />',
'label.bootable': '<fmt:message key="label.bootable" />',
'label.broadcast.domain.type': '<fmt:message key="label.broadcast.domain.type" />',
'label.by.account': '<fmt:message key="label.by.account" />',
'label.by.availability': '<fmt:message key="label.by.availability" />',
'label.by.domain': '<fmt:message key="label.by.domain" />',
'label.by.end.date': '<fmt:message key="label.by.end.date" />',
'label.by.level': '<fmt:message key="label.by.level" />',
'label.by.pod': '<fmt:message key="label.by.pod" />',
'label.by.role': '<fmt:message key="label.by.role" />',
'label.by.start.date': '<fmt:message key="label.by.start.date" />',
'label.by.state': '<fmt:message key="label.by.state" />',
'label.by.traffic.type': '<fmt:message key="label.by.traffic.type" />',
'label.by.type.id': '<fmt:message key="label.by.type.id" />',
'label.by.type': '<fmt:message key="label.by.type" />',
'label.by.zone': '<fmt:message key="label.by.zone" />',
'label.bytes.received': '<fmt:message key="label.bytes.received" />',
'label.bytes.sent': '<fmt:message key="label.bytes.sent" />',
'label.cancel': '<fmt:message key="label.cancel" />',
'label.certificate': '<fmt:message key="label.certificate" />',
'label.privatekey': '<fmt:message key="label.privatekey" />',
'label.domain.suffix': '<fmt:message key="label.domain.suffix" />',
'label.character': '<fmt:message key="label.character" />',
'label.cidr.account': '<fmt:message key="label.cidr.account" />',
'label.close': '<fmt:message key="label.close" />',
'label.cloud.console': '<fmt:message key="label.cloud.console" />',
'label.cloud.managed': '<fmt:message key="label.cloud.managed" />',
'label.cluster.type': '<fmt:message key="label.cluster.type" />',
'label.cluster': '<fmt:message key="label.cluster" />',
'label.code': '<fmt:message key="label.code" />',
'label.confirmation': '<fmt:message key="label.confirmation" />',
'label.cpu.allocated.for.VMs': '<fmt:message key="label.cpu.allocated.for.VMs" />',
'label.cpu.allocated': '<fmt:message key="label.cpu.allocated" />',
'label.cpu.mhz': '<fmt:message key="label.cpu.mhz" />',
'label.cpu.utilized': '<fmt:message key="label.cpu.utilized" />',
'label.cpu': '<fmt:message key="label.cpu" />',
'label.created': '<fmt:message key="label.created" />',
'label.cross.zones': '<fmt:message key="label.cross.zones" />',
'label.custom.disk.size': '<fmt:message key="label.custom.disk.size" />',
'label.daily': '<fmt:message key="label.daily" />',
'label.data.disk.offering': '<fmt:message key="label.data.disk.offering" />',
'label.date': '<fmt:message key="label.date" />',
'label.day.of.month': '<fmt:message key="label.day.of.month" />',
'label.day.of.week': '<fmt:message key="label.day.of.week" />',
'label.delete': '<fmt:message key="label.delete" />',
'label.deleting.failed': '<fmt:message key="label.deleting.failed" />',
'label.deleting.processing': '<fmt:message key="label.deleting.processing" />',
'label.description': '<fmt:message key="label.description" />',
'label.detaching.disk': '<fmt:message key="label.detaching.disk" />',
'label.details': '<fmt:message key="label.details" />',
'label.device.id': '<fmt:message key="label.device.id" />',
'label.disabled': '<fmt:message key="label.disabled" />',
'label.disabling.vpn.access': '<fmt:message key="label.disabling.vpn.access" />',
'label.disk.allocated': '<fmt:message key="label.disk.allocated" />',
'label.disk.offering': '<fmt:message key="label.disk.offering" />',
'label.disk.size.gb': '<fmt:message key="label.disk.size.gb" />',
'label.disk.size': '<fmt:message key="label.disk.size" />',
'label.disk.total': '<fmt:message key="label.disk.total" />',
'label.disk.volume': '<fmt:message key="label.disk.volume" />',
'label.display.text': '<fmt:message key="label.display.text" />',
'label.dns.1': '<fmt:message key="label.dns.1" />',
'label.dns.2': '<fmt:message key="label.dns.2" />',
'label.domain.admin': '<fmt:message key="label.domain.admin" />',
'label.domain.id': '<fmt:message key="label.domain.id" />',
'label.domain.name': '<fmt:message key="label.domain.name" />',
'label.domain': '<fmt:message key="label.domain" />',
'label.double.quotes.are.not.allowed': '<fmt:message key="label.double.quotes.are.not.allowed" />',
'label.download.progress': '<fmt:message key="label.download.progress" />',
'label.edit': '<fmt:message key="label.edit" />',
'label.email': '<fmt:message key="label.email" />',
'label.enabling.vpn.access': '<fmt:message key="label.enabling.vpn.access" />',
'label.enabling.vpn': '<fmt:message key="label.enabling.vpn" />',
'label.end.port': '<fmt:message key="label.end.port" />',
'label.endpoint.or.operation': '<fmt:message key="label.endpoint.or.operation" />',
'label.error.code': '<fmt:message key="label.error.code" />',
'label.error': '<fmt:message key="label.error" />',
'label.esx.host': '<fmt:message key="label.esx.host" />',
'label.example': '<fmt:message key="label.example" />',
'label.failed': '<fmt:message key="label.failed" />',
'label.featured': '<fmt:message key="label.featured" />',
'label.firewall': '<fmt:message key="label.firewall" />',
'label.first.name': '<fmt:message key="label.first.name" />',
'label.format': '<fmt:message key="label.format" />',
'label.friday': '<fmt:message key="label.friday" />',
'label.full': '<fmt:message key="label.full" />',
'label.gateway': '<fmt:message key="label.gateway" />',
'label.general.alerts': '<fmt:message key="label.general.alerts" />',
'label.generating.url': '<fmt:message key="label.generating.url" />',
'label.go.step.2': '<fmt:message key="label.go.step.2" />',
'label.go.step.3': '<fmt:message key="label.go.step.3" />',
'label.go.step.4': '<fmt:message key="label.go.step.4" />',
'label.go.step.5': '<fmt:message key="label.go.step.5" />',
'label.group.optional': '<fmt:message key="label.group.optional" />',
'label.group': '<fmt:message key="label.group" />',
'label.guest.cidr': '<fmt:message key="label.guest.cidr" />',
'label.guest.gateway': '<fmt:message key="label.guest.gateway" />',
'label.guest.ip.range': '<fmt:message key="label.guest.ip.range" />',
'label.guest.ip': '<fmt:message key="label.guest.ip" />',
'label.guest.netmask': '<fmt:message key="label.guest.netmask" />',
'label.ha.enabled': '<fmt:message key="label.ha.enabled" />',
'label.help': '<fmt:message key="label.help" />',
'label.host.alerts': '<fmt:message key="label.host.alerts" />',
'label.host.name': '<fmt:message key="label.host.name" />',
'label.host': '<fmt:message key="label.host" />',
'label.hosts': '<fmt:message key="label.hosts" />',
'label.hourly': '<fmt:message key="label.hourly" />',
'label.hypervisor.type': '<fmt:message key="label.hypervisor.type" />',
'label.hypervisor': '<fmt:message key="label.hypervisor" />',
'label.id': '<fmt:message key="label.id" />',
'label.info': '<fmt:message key="label.info" />',
'label.ingress.rule': '<fmt:message key="label.ingress.rule" />',
'label.initiated.by': '<fmt:message key="label.initiated.by" />',
'label.instance.limits': '<fmt:message key="label.instance.limits" />',
'label.instance.name': '<fmt:message key="label.instance.name" />',
'label.instance': '<fmt:message key="label.instance" />',
'label.instances': '<fmt:message key="label.instances" />',
'label.internal.dns.1': '<fmt:message key="label.internal.dns.1" />',
'label.internal.dns.2': '<fmt:message key="label.internal.dns.2" />',
'label.interval.type': '<fmt:message key="label.interval.type" />',
'label.invalid.integer': '<fmt:message key="label.invalid.integer" />',
'label.invalid.number': '<fmt:message key="label.invalid.number" />',
'label.ip.address': '<fmt:message key="label.ip.address" />',
'label.ip.allocations': '<fmt:message key="label.ip.allocations" />',
'label.ip.limits': '<fmt:message key="label.ip.limits" />',
'label.ip.or.fqdn': '<fmt:message key="label.ip.or.fqdn" />',
'label.ip.range': '<fmt:message key="label.ip.range" />',
'label.ip': '<fmt:message key="label.ip" />',
'label.ips': '<fmt:message key="label.ips" />',
'label.is.default': '<fmt:message key="label.is.default" />',
'label.is.shared': '<fmt:message key="label.is.shared" />',
'label.is.system': '<fmt:message key="label.is.system" />',
'label.iscsi': '<fmt:message key="label.iscsi" />',
'label.iso.boot': '<fmt:message key="label.iso.boot" />',
'label.iso': '<fmt:message key="label.iso" />',
'label.isolation.mode': '<fmt:message key="label.isolation.mode" />',
'label.keep': '<fmt:message key="label.keep" />',
'label.lang.chinese': '<fmt:message key="label.lang.chinese" />',
'label.lang.english': '<fmt:message key="label.lang.english" />',
'label.lang.japanese': '<fmt:message key="label.lang.japanese" />',
'label.lang.spanish': '<fmt:message key="label.lang.spanish" />',
'label.last.disconnected': '<fmt:message key="label.last.disconnected" />',
'label.last.name': '<fmt:message key="label.last.name" />',
'label.level': '<fmt:message key="label.level" />',
'label.linklocal.ip': '<fmt:message key="label.linklocal.ip" />',
'label.load.balancer': '<fmt:message key="label.load.balancer" />',
'label.loading': '<fmt:message key="label.loading" />',
'label.local': '<fmt:message key="label.local" />',
'label.login': '<fmt:message key="label.login" />',
'label.logout': '<fmt:message key="label.logout" />',
'label.lun': '<fmt:message key="label.lun" />',
'label.manage': '<fmt:message key="label.manage" />',
'label.maximum': '<fmt:message key="label.maximum" />',
'label.memory.allocated': '<fmt:message key="label.memory.allocated" />',
'label.memory.mb': '<fmt:message key="label.memory.mb" />',
'label.memory.total': '<fmt:message key="label.memory.total" />',
'label.memory.used': '<fmt:message key="label.memory.used" />',
'label.memory': '<fmt:message key="label.memory" />',
'label.menu.accounts': '<fmt:message key="label.menu.accounts" />',
'label.menu.alerts': '<fmt:message key="label.menu.alerts" />',
'label.menu.all.accounts': '<fmt:message key="label.menu.all.accounts" />',
'label.menu.all.instances': '<fmt:message key="label.menu.all.instances" />',
'label.menu.community.isos': '<fmt:message key="label.menu.community.isos" />',
'label.menu.community.templates': '<fmt:message key="label.menu.community.templates" />',
'label.menu.configuration': '<fmt:message key="label.menu.configuration" />',
'label.menu.dashboard': '<fmt:message key="label.menu.dashboard" />',
'label.menu.destroyed.instances': '<fmt:message key="label.menu.destroyed.instances" />',
'label.menu.disk.offerings': '<fmt:message key="label.menu.disk.offerings" />',
'label.menu.domains': '<fmt:message key="label.menu.domains" />',
'label.menu.events': '<fmt:message key="label.menu.events" />',
'label.menu.featured.isos': '<fmt:message key="label.menu.featured.isos" />',
'label.menu.featured.templates': '<fmt:message key="label.menu.featured.templates" />',
'label.menu.global.settings': '<fmt:message key="label.menu.global.settings" />',
'label.menu.instances': '<fmt:message key="label.menu.instances" />',
'label.menu.ipaddresses': '<fmt:message key="label.menu.ipaddresses" />',
'label.menu.isos': '<fmt:message key="label.menu.isos" />',
'label.menu.my.accounts': '<fmt:message key="label.menu.my.accounts" />',
'label.menu.my.instances': '<fmt:message key="label.menu.my.instances" />',
'label.menu.my.isos': '<fmt:message key="label.menu.my.isos" />',
'label.menu.my.templates': '<fmt:message key="label.menu.my.templates" />',
'label.menu.network.offerings': '<fmt:message key="label.menu.network.offerings" />',
'label.menu.network': '<fmt:message key="label.menu.network" />',
'label.menu.physical.resources': '<fmt:message key="label.menu.physical.resources" />',
'label.menu.running.instances': '<fmt:message key="label.menu.running.instances" />',
'label.menu.security.groups': '<fmt:message key="label.menu.security.groups" />',
'label.menu.service.offerings': '<fmt:message key="label.menu.service.offerings" />',
'label.menu.snapshots': '<fmt:message key="label.menu.snapshots" />',
'label.menu.stopped.instances': '<fmt:message key="label.menu.stopped.instances" />',
'label.menu.storage': '<fmt:message key="label.menu.storage" />',
'label.menu.system.vms': '<fmt:message key="label.menu.system.vms" />',
'label.menu.system': '<fmt:message key="label.menu.system" />',
'label.menu.templates': '<fmt:message key="label.menu.templates" />',
'label.menu.virtual.appliances': '<fmt:message key="label.menu.virtual.appliances" />',
'label.menu.virtual.resources': '<fmt:message key="label.menu.virtual.resources" />',
'label.menu.volumes': '<fmt:message key="label.menu.volumes" />',
'label.migrate.instance.to': '<fmt:message key="label.migrate.instance.to" />',
'label.minimum': '<fmt:message key="label.minimum" />',
'label.minute.past.hour': '<fmt:message key="label.minute.past.hour" />',
'label.monday': '<fmt:message key="label.monday" />',
'label.monthly': '<fmt:message key="label.monthly" />',
'label.more.templates': '<fmt:message key="label.more.templates" />',
'label.my.account': '<fmt:message key="label.my.account" />',
'label.name.optional': '<fmt:message key="label.name.optional" />',
'label.name': '<fmt:message key="label.name" />',
'label.netmask': '<fmt:message key="label.netmask" />',
'label.network.desc': '<fmt:message key="label.network.desc" />',
'label.network.domain': '<fmt:message key="label.network.domain" />',
'label.network.id': '<fmt:message key="label.network.id" />',
'label.network.name': '<fmt:message key="label.network.name" />',
'label.network.offering.display.text': '<fmt:message key="label.network.offering.display.text" />',
'label.network.offering.id': '<fmt:message key="label.network.offering.id" />',
'label.network.offering.name': '<fmt:message key="label.network.offering.name" />',
'label.network.offering': '<fmt:message key="label.network.offering" />',
'label.network.rate': '<fmt:message key="label.network.rate" />',
'label.network.read': '<fmt:message key="label.network.read" />',
'label.network.type': '<fmt:message key="label.network.type" />',
'label.network.write': '<fmt:message key="label.network.write" />',
'label.network': '<fmt:message key="label.network" />',
'label.new.password': '<fmt:message key="label.new.password" />',
'label.next': '<fmt:message key="label.next" />',
'label.nfs.server': '<fmt:message key="label.nfs.server" />',
'label.nfs.storage': '<fmt:message key="label.nfs.storage" />',
'label.nfs': '<fmt:message key="label.nfs" />',
'label.nics': '<fmt:message key="label.nics" />',
'label.no.actions': '<fmt:message key="label.no.actions" />',
'label.no.alerts': '<fmt:message key="label.no.alerts" />',
'label.no.errors': '<fmt:message key="label.no.errors" />',
'label.no.isos': '<fmt:message key="label.no.isos" />',
'label.no.items': '<fmt:message key="label.no.items" />',
'label.no.security.groups': '<fmt:message key="label.no.security.groups" />',
'label.no.thanks': '<fmt:message key="label.no.thanks" />',
'label.no': '<fmt:message key="label.no" />',
'label.none': '<fmt:message key="label.none" />',
'label.not.found': '<fmt:message key="label.not.found" />',
'label.num.cpu.cores': '<fmt:message key="label.num.cpu.cores" />',
'label.numretries ': '<fmt:message key="label.numretries " />',
'label.offer.ha': '<fmt:message key="label.offer.ha" />',
'label.optional': '<fmt:message key="label.optional" />',
'label.os.preference': '<fmt:message key="label.os.preference" />',
'label.os.type': '<fmt:message key="label.os.type" />',
'label.owned.public.ips': '<fmt:message key="label.owned.public.ips" />',
'label.owner.account': '<fmt:message key="label.owner.account" />',
'label.owner.domain': '<fmt:message key="label.owner.domain" />',
'label.parent.domain': '<fmt:message key="label.parent.domain" />',
'label.password.enabled': '<fmt:message key="label.password.enabled" />',
'label.password': '<fmt:message key="label.password" />',
'label.path': '<fmt:message key="label.path" />',
'label.please.wait': '<fmt:message key="label.please.wait" />',
'label.pod': '<fmt:message key="label.pod" />',
'label.port.forwarding': '<fmt:message key="label.port.forwarding" />',
'label.port.range': '<fmt:message key="label.port.range" />',
'label.prev': '<fmt:message key="label.prev" />',
'label.primary.allocated': '<fmt:message key="label.primary.allocated" />',
'label.primary.network': '<fmt:message key="label.primary.network" />',
'label.primary.storage': '<fmt:message key="label.primary.storage" />',
'label.primary.used': '<fmt:message key="label.primary.used" />',
'label.private.interface': '<fmt:message key="label.private.interface" />',
'label.private.ip.range': '<fmt:message key="label.private.ip.range" />',
'label.private.ip': '<fmt:message key="label.private.ip" />',
'label.private.ips': '<fmt:message key="label.private.ips" />',
'label.private.port': '<fmt:message key="label.private.port" />',
'label.private.zone': '<fmt:message key="label.private.zone" />',
'label.protocol': '<fmt:message key="label.protocol" />',
'label.public.interface': '<fmt:message key="label.public.interface" />',
'label.public.ip': '<fmt:message key="label.public.ip" />',
'label.public.ips': '<fmt:message key="label.public.ips" />',
'label.public.port': '<fmt:message key="label.public.port" />',
'label.public.zone': '<fmt:message key="label.public.zone" />',
'label.public': '<fmt:message key="label.public" />',
'label.recent.errors': '<fmt:message key="label.recent.errors" />',
'label.refresh': '<fmt:message key="label.refresh" />',
'label.related': '<fmt:message key="label.related" />',
'label.remove.from.load.balancer': '<fmt:message key="label.remove.from.load.balancer" />',
'label.removing.user': '<fmt:message key="label.removing.user" />',
'label.required': '<fmt:message key="label.required" />',
'label.reserved.system.ip': '<fmt:message key="label.reserved.system.ip" />',
'label.resource.limits': '<fmt:message key="label.resource.limits" />',
'label.resource': '<fmt:message key="label.resource" />',
'label.resources': '<fmt:message key="label.resources" />',
'label.role': '<fmt:message key="label.role" />',
'label.root.disk.offering': '<fmt:message key="label.root.disk.offering" />',
'label.running.vms': '<fmt:message key="label.running.vms" />',
'label.saturday': '<fmt:message key="label.saturday" />',
'label.save': '<fmt:message key="label.save" />',
'label.saving.processing': '<fmt:message key="label.saving.processing" />',
'label.scope': '<fmt:message key="label.scope" />',
'label.search': '<fmt:message key="label.search" />',
'label.secondary.storage': '<fmt:message key="label.secondary.storage" />',
'label.secondary.used': '<fmt:message key="label.secondary.used" />',
'label.secret.key': '<fmt:message key="label.secret.key" />',
'label.security.group.name': '<fmt:message key="label.security.group.name" />',
'label.security.group': '<fmt:message key="label.security.group" />',
'label.security.groups.enabled': '<fmt:message key="label.security.groups.enabled" />',
'label.security.groups': '<fmt:message key="label.security.groups" />',
'label.sent': '<fmt:message key="label.sent" />',
'label.server': '<fmt:message key="label.server" />',
'label.service.offering': '<fmt:message key="label.service.offering" />',
'label.system.service.offering': '<fmt:message key="label.system.service.offering" />',
'label.session.expired': '<fmt:message key="label.session.expired" />',
'label.shared': '<fmt:message key="label.shared" />',
'label.size': '<fmt:message key="label.size" />',
'label.snapshot.limits': '<fmt:message key="label.snapshot.limits" />',
'label.snapshot.name': '<fmt:message key="label.snapshot.name" />',
'label.snapshot.s': '<fmt:message key="label.snapshot.s" />',
'label.snapshot.schedule': '<fmt:message key="label.snapshot.schedule" />',
'label.snapshot': '<fmt:message key="label.snapshot" />',
'label.snapshots': '<fmt:message key="label.snapshots" />',
'label.source.nat': '<fmt:message key="label.source.nat" />',
'label.specify.vlan': '<fmt:message key="label.specify.vlan" />',
'label.start.port': '<fmt:message key="label.start.port" />',
'label.state': '<fmt:message key="label.state" />',
'label.static.nat.to': '<fmt:message key="label.static.nat.to" />',
'label.static.nat': '<fmt:message key="label.static.nat" />',
'label.statistics': '<fmt:message key="label.statistics" />',
'label.status': '<fmt:message key="label.status" />',
'label.step.1.title': '<fmt:message key="label.step.1.title" />',
'label.step.1': '<fmt:message key="label.step.1" />',
'label.step.2.title': '<fmt:message key="label.step.2.title" />',
'label.step.2': '<fmt:message key="label.step.2" />',
'label.step.3.title': '<fmt:message key="label.step.3.title" />',
'label.step.3': '<fmt:message key="label.step.3" />',
'label.step.4.title': '<fmt:message key="label.step.4.title" />',
'label.step.4': '<fmt:message key="label.step.4" />',
'label.step.5.title': '<fmt:message key="label.step.5.title" />',
'label.step.5': '<fmt:message key="label.step.5" />',
'label.stopped.vms': '<fmt:message key="label.stopped.vms" />',
'label.storage.type': '<fmt:message key="label.storage.type" />',
'label.storage': '<fmt:message key="label.storage" />',
'label.submit': '<fmt:message key="label.submit" />',
'label.submitted.by': '<fmt:message key="label.submitted.by" />',
'label.succeeded': '<fmt:message key="label.succeeded" />',
'label.sunday': '<fmt:message key="label.sunday" />',
'label.system.capacity': '<fmt:message key="label.system.capacity" />',
'label.system.vm.type': '<fmt:message key="label.system.vm.type" />',
'label.system.vm': '<fmt:message key="label.system.vm" />',
'label.system.vms': '<fmt:message key="label.system.vms" />',
'label.tagged': '<fmt:message key="label.tagged" />',
'label.tags': '<fmt:message key="label.tags" />',
'label.target.iqn': '<fmt:message key="label.target.iqn" />',
'label.template.limits': '<fmt:message key="label.template.limits" />',
'label.template': '<fmt:message key="label.template" />',
'label.theme.default': '<fmt:message key="label.theme.default" />',
'label.theme.grey': '<fmt:message key="label.theme.grey" />',
'label.theme.lightblue': '<fmt:message key="label.theme.lightblue" />',
'label.thursday': '<fmt:message key="label.thursday" />',
'label.time.zone': '<fmt:message key="label.time.zone" />',
'label.time': '<fmt:message key="label.time" />',
'label.timeout.in.second ': '<fmt:message key="label.timeout.in.second " />',
'label.timezone': '<fmt:message key="label.timezone" />',
'label.total.cpu': '<fmt:message key="label.total.cpu" />',
'label.total.vms': '<fmt:message key="label.total.vms" />',
'label.traffic.type': '<fmt:message key="label.traffic.type" />',
'label.tuesday': '<fmt:message key="label.tuesday" />',
'label.type.id': '<fmt:message key="label.type.id" />',
'label.type': '<fmt:message key="label.type" />',
'label.unavailable': '<fmt:message key="label.unavailable" />',
'label.unlimited': '<fmt:message key="label.unlimited" />',
'label.untagged': '<fmt:message key="label.untagged" />',
'label.update.ssl.cert': '<fmt:message key="label.update.ssl.cert" />',
'label.update.ssl': '<fmt:message key="label.update.ssl" />',
'label.updating': '<fmt:message key="label.updating" />',
'label.url': '<fmt:message key="label.url" />',
'label.usage.interface': '<fmt:message key="label.usage.interface" />',
'label.used': '<fmt:message key="label.used" />',
'label.user': '<fmt:message key="label.user" />',
'label.username': '<fmt:message key="label.username" />',
'label.users': '<fmt:message key="label.users" />',
'label.value': '<fmt:message key="label.value" />',
'label.vcenter.cluster': '<fmt:message key="label.vcenter.cluster" />',
'label.vcenter.datacenter': '<fmt:message key="label.vcenter.datacenter" />',
'label.vcenter.datastore': '<fmt:message key="label.vcenter.datastore" />',
'label.vcenter.host': '<fmt:message key="label.vcenter.host" />',
'label.vcenter.password': '<fmt:message key="label.vcenter.password" />',
'label.vcenter.username': '<fmt:message key="label.vcenter.username" />',
'label.version': '<fmt:message key="label.version" />',
'label.virtual.appliance': '<fmt:message key="label.virtual.appliance" />',
'label.virtual.appliances': '<fmt:message key="label.virtual.appliances" />',
'label.virtual.network': '<fmt:message key="label.virtual.network" />',
'label.vlan.id': '<fmt:message key="label.vlan.id" />',
'label.vlan.range': '<fmt:message key="label.vlan.range" />',
'label.vlan': '<fmt:message key="label.vlan" />',
'label.vm.add': '<fmt:message key="label.vm.add" />',
'label.vm.destroy': '<fmt:message key="label.vm.destroy" />',
'label.vm.reboot': '<fmt:message key="label.vm.reboot" />',
'label.vm.start': '<fmt:message key="label.vm.start" />',
'label.vm.stop': '<fmt:message key="label.vm.stop" />',
'label.vmfs': '<fmt:message key="label.vmfs" />',
'label.vms': '<fmt:message key="label.vms" />',
'label.volume.limits': '<fmt:message key="label.volume.limits" />',
'label.volume.name': '<fmt:message key="label.volume.name" />',
'label.volume': '<fmt:message key="label.volume" />',
'label.volumes': '<fmt:message key="label.volumes" />',
'label.vpn': '<fmt:message key="label.vpn" />',
'label.vsphere.managed': '<fmt:message key="label.vsphere.managed" />',
'label.waiting': '<fmt:message key="label.waiting" />',
'label.warn': '<fmt:message key="label.warn" />',
'label.wednesday': '<fmt:message key="label.wednesday" />',
'label.weekly': '<fmt:message key="label.weekly" />',
'label.welcome.cloud.console': '<fmt:message key="label.welcome.cloud.console" />',
'label.welcome': '<fmt:message key="label.welcome" />',
'label.yes': '<fmt:message key="label.yes" />',
'label.zone.id': '<fmt:message key="label.zone.id" />',
'label.zone.step.1.title': '<fmt:message key="label.zone.step.1.title" />',
'label.zone.step.2.title': '<fmt:message key="label.zone.step.2.title" />',
'label.zone.step.3.title': '<fmt:message key="label.zone.step.3.title" />',
'label.zone.step.4.title': '<fmt:message key="label.zone.step.4.title" />',
'label.zone.wide': '<fmt:message key="label.zone.wide" />',
'label.zone': '<fmt:message key="label.zone" />',
'message.acquire.public.ip': '<fmt:message key="message.acquire.public.ip" />',
'message.action.cancel.maintenance.mode': '<fmt:message key="message.action.cancel.maintenance.mode" />',
'message.action.cancel.maintenance': '<fmt:message key="message.action.cancel.maintenance" />',
'message.action.delete.ISO.for.all.zones': '<fmt:message key="message.action.delete.ISO.for.all.zones" />',
'message.action.delete.ISO': '<fmt:message key="message.action.delete.ISO" />',
'message.action.delete.cluster': '<fmt:message key="message.action.delete.cluster" />',
'message.action.delete.disk.offering': '<fmt:message key="message.action.delete.disk.offering" />',
'message.action.delete.domain': '<fmt:message key="message.action.delete.domain" />',
'message.action.delete.external.firewall': '<fmt:message key="message.action.delete.external.firewall" />',
'message.action.delete.external.load.balancer': '<fmt:message key="message.action.delete.external.load.balancer" />',
'message.action.delete.ingress.rule': '<fmt:message key="message.action.delete.ingress.rule" />',
'message.action.delete.network': '<fmt:message key="message.action.delete.network" />',
'message.action.delete.pod': '<fmt:message key="message.action.delete.pod" />',
'message.action.delete.primary.storage': '<fmt:message key="message.action.delete.primary.storage" />',
'message.action.delete.secondary.storage': '<fmt:message key="message.action.delete.secondary.storage" />',
'message.action.delete.security.group': '<fmt:message key="message.action.delete.security.group" />',
'message.action.delete.service.offering': '<fmt:message key="message.action.delete.service.offering" />',
'message.action.delete.snapshot': '<fmt:message key="message.action.delete.snapshot" />',
'message.action.delete.template.for.all.zones': '<fmt:message key="message.action.delete.template.for.all.zones" />',
'message.action.delete.template': '<fmt:message key="message.action.delete.template" />',
'message.action.delete.volume': '<fmt:message key="message.action.delete.volume" />',
'message.action.delete.zone': '<fmt:message key="message.action.delete.zone" />',
'message.action.destroy.instance': '<fmt:message key="message.action.destroy.instance" />',
'message.action.destroy.systemvm': '<fmt:message key="message.action.destroy.systemvm" />',
'message.action.disable.static.NAT': '<fmt:message key="message.action.disable.static.NAT" />',
'message.action.enable.maintenance': '<fmt:message key="message.action.enable.maintenance" />',
'message.action.force.reconnect': '<fmt:message key="message.action.force.reconnect" />',
'message.action.host.enable.maintenance.mode': '<fmt:message key="message.action.host.enable.maintenance.mode" />',
'message.action.instance.reset.password': '<fmt:message key="message.action.instance.reset.password" />',
'message.action.primarystorage.enable.maintenance.mode': '<fmt:message key="message.action.primarystorage.enable.maintenance.mode" />',
'message.action.reboot.instance': '<fmt:message key="message.action.reboot.instance" />',
'message.action.reboot.router': '<fmt:message key="message.action.reboot.router" />',
'message.action.reboot.systemvm': '<fmt:message key="message.action.reboot.systemvm" />',
'message.action.release.ip': '<fmt:message key="message.action.release.ip" />',
'message.action.remove.host': '<fmt:message key="message.action.remove.host" />',
'message.action.restore.instance': '<fmt:message key="message.action.restore.instance" />',
'message.action.start.instance': '<fmt:message key="message.action.start.instance" />',
'message.action.start.router': '<fmt:message key="message.action.start.router" />',
'message.action.start.systemvm': '<fmt:message key="message.action.start.systemvm" />',
'message.action.stop.instance': '<fmt:message key="message.action.stop.instance" />',
'message.action.stop.router': '<fmt:message key="message.action.stop.router" />',
'message.action.stop.systemvm': '<fmt:message key="message.action.stop.systemvm" />',
'message.action.take.snapshot': '<fmt:message key="message.action.take.snapshot" />',
'message.add.cluster.zone': '<fmt:message key="message.add.cluster.zone" />',
'message.add.cluster': '<fmt:message key="message.add.cluster" />',
'message.add.disk.offering': '<fmt:message key="message.add.disk.offering" />',
'message.add.firewall': '<fmt:message key="message.add.firewall" />',
'message.add.host': '<fmt:message key="message.add.host" />',
'message.add.ip.range.direct.network': '<fmt:message key="message.add.ip.range.direct.network" />',
'message.add.ip.range.to.pod': '<fmt:message key="message.add.ip.range.to.pod" />',
'message.add.ip.range': '<fmt:message key="message.add.ip.range" />',
'message.add.load.balancer': '<fmt:message key="message.add.load.balancer" />',
'message.add.network': '<fmt:message key="message.add.network" />',
'message.add.pod': '<fmt:message key="message.add.pod" />',
'message.add.primary.storage': '<fmt:message key="message.add.primary.storage" />',
'message.add.primary': '<fmt:message key="message.add.primary" />',
'message.add.secondary.storage': '<fmt:message key="message.add.secondary.storage" />',
'message.add.service.offering': '<fmt:message key="message.add.service.offering" />',
'message.add.template': '<fmt:message key="message.add.template" />',
'message.add.volume': '<fmt:message key="message.add.volume" />',
'message.additional.networks.desc': '<fmt:message key="message.additional.networks.desc" />',
'message.advanced.mode.desc': '<fmt:message key="message.advanced.mode.desc" />',
'message.advanced.security.group': '<fmt:message key="message.advanced.security.group" />',
'message.advanced.virtual': '<fmt:message key="message.advanced.virtual" />',
'message.allow.vpn.access': '<fmt:message key="message.allow.vpn.access" />',
'message.attach.iso.confirm': '<fmt:message key="message.attach.iso.confirm" />',
'message.attach.volume': '<fmt:message key="message.attach.volume" />',
'message.basic.mode.desc': '<fmt:message key="message.basic.mode.desc" />',
'message.change.offering.confirm': '<fmt:message key="message.change.offering.confirm" />',
'message.copy.iso.confirm': '<fmt:message key="message.copy.iso.confirm" />',
'message.copy.template': '<fmt:message key="message.copy.template" />',
'message.create.template.vm': '<fmt:message key="message.create.template.vm" />',
'message.create.template.volume': '<fmt:message key="message.create.template.volume" />',
'message.delete.account': '<fmt:message key="message.delete.account" />',
'message.detach.iso.confirm': '<fmt:message key="message.detach.iso.confirm" />',
'message.disable.account': '<fmt:message key="message.disable.account" />',
'message.disable.vpn.access': '<fmt:message key="message.disable.vpn.access" />',
'message.download.ISO': '<fmt:message key="message.download.ISO" />',
'message.download.template': '<fmt:message key="message.download.template" />',
'message.download.volume': '<fmt:message key="message.download.volume" />',
'message.edit.confirm': '<fmt:message key="message.edit.confirm" />',
'message.edit.limits': '<fmt:message key="message.edit.limits" />',
'message.enable.account': '<fmt:message key="message.enable.account" />',
'message.enable.vpn.access': '<fmt:message key="message.enable.vpn.access" />',
'message.enable.vpn': '<fmt:message key="message.enable.vpn" />',
'message.enabled.vpn.ip.sec': '<fmt:message key="message.enabled.vpn.ip.sec" />',
'message.enabled.vpn': '<fmt:message key="message.enabled.vpn" />',
'message.launch.vm.on.private.network': '<fmt:message key="message.launch.vm.on.private.network" />',
'message.lock.account': '<fmt:message key="message.lock.account" />',
'message.migrate.instance.confirm': '<fmt:message key="message.migrate.instance.confirm" />',
'message.new.user': '<fmt:message key="message.new.user" />',
'message.no.network.support.configuration.not.true': '<fmt:message key="message.no.network.support.configuration.not.true" />',
'message.no.network.support': '<fmt:message key="message.no.network.support" />',
'message.number.clusters': '<fmt:message key="message.number.clusters" />',
'message.number.hosts': '<fmt:message key="message.number.hosts" />',
'message.number.pods': '<fmt:message key="message.number.pods" />',
'message.number.storage': '<fmt:message key="message.number.storage" />',
'message.number.zones': '<fmt:message key="message.number.zones" />',
'message.remove.vpn.access': '<fmt:message key="message.remove.vpn.access" />',
'message.restart.mgmt.server': '<fmt:message key="message.restart.mgmt.server" />',
'message.security.group.usage': '<fmt:message key="message.security.group.usage" />',
'message.snapshot.schedule': '<fmt:message key="message.snapshot.schedule" />',
'message.step.1.continue': '<fmt:message key="message.step.1.continue" />',
'message.step.1.desc': '<fmt:message key="message.step.1.desc" />',
'message.step.2.continue': '<fmt:message key="message.step.2.continue" />',
'message.step.2.desc': '<fmt:message key="message.step.2.desc" />',
'message.step.3.continue': '<fmt:message key="message.step.3.continue" />',
'message.step.3.desc': '<fmt:message key="message.step.3.desc" />',
'message.step.4.continue': '<fmt:message key="message.step.4.continue" />',
'message.step.4.desc': '<fmt:message key="message.step.4.desc" />',
'message.update.os.preference': '<fmt:message key="message.update.os.preference" />',
'message.update.ssl': '<fmt:message key="message.update.ssl" />',
'message.virtual.network.desc': '<fmt:message key="message.virtual.network.desc" />',
'message.volume.create.template.confirm': '<fmt:message key="message.volume.create.template.confirm" />',
'message.zone.step.1.desc': '<fmt:message key="message.zone.step.1.desc" />',
'message.zone.step.2.desc': '<fmt:message key="message.zone.step.2.desc" />',
'message.zone.step.3.desc': '<fmt:message key="message.zone.step.3.desc" />',
'message.apply.snapshot.policy': '<fmt:message key="message.apply.snapshot.policy" />',
'message.disable.snapshot.policy': '<fmt:message key="message.disable.snapshot.policy" />',
'message.action.change.service.warning.for.instance': '<fmt:message key="message.action.change.service.warning.for.instance" />',
'message.action.change.service.warning.for.router': '<fmt:message key="message.action.change.service.warning.for.router" />',
'message.action.reset.password.warning': '<fmt:message key="message.action.reset.password.warning" />',
'message.action.reset.password.off': '<fmt:message key="message.action.reset.password.off" />',
'error.login': '<fmt:message key="error.login" />',
'error.menu.select': '<fmt:message key="error.menu.select" />',
'error.mgmt.server.inaccessible': '<fmt:message key="error.mgmt.server.inaccessible" />',
'error.session.expired': '<fmt:message key="error.session.expired" />',
'error.unresolved.internet.name': '<fmt:message key="error.unresolved.internet.name" />',
'message.add.system.service.offering': '<fmt:message key="message.add.system.service.offering" />', //Jes
'message.action.delete.system.service.offering': '<fmt:message key="message.action.delete.system.service.offering" />',
'label.action.delete.system.service.offering': '<fmt:message key="label.action.delete.system.service.offering" />',
'hypervisor.capabilities': '<fmt:message key="hypervisor.capabilities" />',
'hypervisor.version': '<fmt:message key="hypervisor.version" />',
'max.guest.limit': '<fmt:message key="max.guest.limit" />',
'security.group.enabled': '<fmt:message key="security.group.enabled" />',
'add.network.offering': '<fmt:message key="add.network.offering" />',
'supported.services': '<fmt:message key="supported.services" />',
'service.capabilities': '<fmt:message key="service.capabilities" />',
'guest.type': '<fmt:message key="guest.type" />',
'specify.IP.ranges': '<fmt:message key="specify.IP.ranges" />',
'conserve.mode': '<fmt:message key="conserve.mode" />',
'created.by.system': '<fmt:message key="created.by.system" />',
'label.menu.system.service.offerings': '<fmt:message key="label.menu.system.service.offerings" />',
'label.add.system.service.offering': '<fmt:message key="label.add.system.service.offering" />',
'redundant.router.capability': '<fmt:message key="redundant.router.capability" />',
'supported.source.NAT.type': '<fmt:message key="supported.source.NAT.type" />',
'elastic.LB': '<fmt:message key="elastic.LB" />',
'LB.isolation': '<fmt:message key="LB.isolation" />',
'elastic.IP': '<fmt:message key="elastic.IP" />'
};
</script>
