<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
          "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>CloudStack</title>
    <link type="text/css" rel="stylesheet" href="lib/reset.css"/>
    <link type="text/css" rel="stylesheet" href="css/cloudstack3.css" />
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
              <label for="username">User Name</label>
              <input type="text" name="username" class="required" />
            </div>

            <!-- Password -->
            <div class="field password">
              <label for="password">Password</label>
              <input type="password" name="password" class="required" />
            </div>

            <!-- Domain -->
            <div class="field domain">
              <label for="domain">Domain</label>
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
            <div class="setup" wizard-step-id="setup">
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
            <div class="select-iso" wizard-step-id="select-iso">
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
                    <li class="first last"><a href="#instance-wizard-my-isos">My ISOs</a></li>
                  </ul>

                  <div id="instance-wizard-my-isos">
                    <div class="select-container">
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Step 3: Service Offering -->
            <div class="service-offering" wizard-step-id="service-offering">
              <div class="content">
                <div class="select-container">
                </div>
              </div>
            </div>

            <!-- Step 4: Data Disk Offering -->
            <div class="data-disk-offering" wizard-step-id="data-disk-offering">
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

                  <input type="text" name="size" value="1" />
                  <label class="size">GB</label>
                </div>
              </div>
            </div>

            <!-- Step 5: Network -->
            <div class="network" wizard-step-id="network">
              <!-- 5a: Network description -->
              <div class="wizard-step-conditional nothing-to-select">
                <p>Either you select a basic zone whose guest network does not include security group service or your selected hypervisor does not have any additional network features or you don't have any zone that has security group enabled.</p>
                <p>Thus, no additional network features.</p>
                <p>Please proceed to the next step.</p>
              </div>

              <!-- 5b: Select network -->
              <div class="wizard-step-conditional select-network">
                <div class="content">
                  <div class="main-desc">
                    Please select networks for your virtual machine
                  </div>
                  <div class="select default-network">
                    <table>
                      <thead>
                        <tr>
                          <th>Default network</th>
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
                  <div class="select optional-networks">
                    <table>
                      <thead>
                        <tr>
                          <th>Optional networks</th>
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
            <div class="review" wizard-step-id="review">
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
            <li class="first"><span class="number">1</span><span>Select Type</span><span class="arrow"></span></li>
            <li class="last"><span class="number">2</span><span>Setup Zone</span></li>
          </ul>
        </div>
        <div class="steps">
          <!-- Step 1: Select network -->
          <div class="select-network">
            <form>
              <div class="content">
                <!-- Select template -->
                <div class="section select-network-model">
                  <h3>Setup zone type</h3>
                  <p>Please select a configuration for your zone.</p>
                  <div class="select-area">
                    <div class="desc">Choose this network model if you do <em>not</em> want to enable any VLAN support. All virtual instances created under this network model will be assigned an IP directly from the network and security groups are used to provide security and segregation.</div>
                    <input type="radio" name="network-model" value="Basic" checked="checked" />
                    <label>Basic</label>
                  </div>
                  <div class="select-area">
                    <div class="desc">Choose this network model if you wish to enable VLAN support. This network model provides the most flexibility in allowing administrators to provide custom network offerings such as providing firewall, vpn, or load balancer support as well as enabling direct vs virtual networking.</div>
                    <input type="radio" name="network-model" value="Advanced" />
                    <label>Advanced</label>

                    <div class="isolation-mode">
                      <h3>Isolation mode</h3>
                      <div class="select-area">
                        <input type="radio" name="isolation-mode" value="vlan"
                               checked="checked" disabled="disabled" />
                        <label>VLAN</label>
                        <div class="desc">Choose this if you wish to use zone-wide VLANs to provide guest VM isolation.</div>
                      </div>
                      <div class="select-area">
                        <input type="radio" name="isolation-mode" value="security-groups"
                               disabled="disabled" />
                        <label>Security Groups</label>
                        <div class="desc">Choose this if you wish to use security groups to provide guest VM isolation.</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </div>

          <!-- Step 2: Setup zone -->
          <div class="setup-zone">
            <div class="main-desc">Please enter in the information to make a new zone.</div>
            <div class="content input-area">
              <div class="select-container">
                <form>
                  <!-- Name -->
                  <div class="field">
                    <div class="name">
                      <span>Name</span>
                    </div>
                    <div class="value">
                      <input type="text" name="name" class="required" />
                    </div>
                  </div>

                  <!-- DNS 1 -->
                  <div class="field odd">
                    <div class="name">
                      <span>DNS 1</span>
                    </div>
                    <div class="value">
                      <input type="text" name="dns1" class="required" />
                    </div>
                  </div>

                  <!-- DNS 2-->
                  <div class="field">
                    <div class="name">
                      <span>DNS 2</span>
                    </div>
                    <div class="value">
                      <input type="text" name="dns2" />
                    </div>
                  </div>

                  <!-- Internal DNS 1 -->
                  <div class="field odd">
                    <div class="name">
                      <span>Internal DNS 1</span>
                    </div>
                    <div class="value">
                      <input type="text" name="internaldns1" class="required" />
                    </div>
                  </div>

                  <!-- Internal DNS 2 -->
                  <div class="field">
                    <div class="name">
                      <span>Internal DNS 2</span>
                    </div>
                    <div class="value">
                      <input type="text" name="internaldns2" />
                    </div>
                  </div>

                  <div class="conditional vlan">
                    <!-- Guest CIDR -->
                    <div class="field">
                      <div class="name">
                        <span>Guest CIDR</span>
                      </div>
                      <div class="value">
                        <input type="text" name="guest-cidr" class="required"
                               value="10.1.1.0/24" />
                      </div>
                    </div>
                  </div>

                  <!-- Public -->
                  <div class="field odd">
                    <div class="name">
                      <span>Public</span>
                    </div>
                    <div class="value">
                      <input name="public" type="checkbox" checked="checked" />
                    </div>
                  </div>

                  <div class="conditional public">
                    <!-- Domain -->
                    <div class="field">
                      <div class="name">
                        <span>Domain</span>
                      </div>
                      <div class="value">
                        <select name="zone-domain" class="domain required"></select>
                      </div>
                    </div>
                  </div>
                </form>
              </div>
            </div>
          </div>

          <!-- Step 3: Setup pod -->
          <div class="setup-pod disabled">
            <form>
              <div class="main-desc">Please enter the following information to add a new pod</div>
              <div class="content input-area">
                <div class="select-container">
                  <!-- Name -->
                  <div class="field">
                    <div class="name">
                      <span>Name</span>
                    </div>
                    <div class="value">
                      <input type="text" name="pod-name" class="required" />
                    </div>
                  </div>

                  <!-- Gateway -->
                  <div class="field odd">
                    <div class="name">
                      <span>Gateway</span>
                    </div>
                    <div class="value">
                      <input type="text" name="pod-gateway" class="required" />
                    </div>
                  </div>

                  <!-- Netmask -->
                  <div class="field">
                    <div class="name">
                      <span>Netmask</span>
                    </div>
                    <div class="value">
                      <input type="text" name="pod-netmask" class="required" />
                    </div>
                  </div>

                  <!-- Reserved System IP -->
                  <div class="field odd">
                    <div class="name">
                      <span>Reserved IP</span>
                    </div>
                    <div class="value multi-range">
                      <input type="text" name="pod-ip-range-start" class="required" />
                      <span class="divider">-</span>
                      <input type="text" name="pod-ip-range-end" />
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </div>

          <!-- Step 4: Add IP range -->
          <div class="add-ip-range disabled">
            <form>
              <div class="main-desc">Please add an IP range for your zone</div>
              <div class="content input-area">
                <div class="select-container">
                  <!-- VLAN Type -->
                  <div class="field conditional vlan">
                    <div class="name">
                      <span>VLAN</span>
                    </div>
                    <div class="value">
                      <select name="vlan-type">
                        <option value="untagged">Untagged</option>
                        <option value="tagged">Tagged</option>
                      </select>
                    </div>
                  </div>

                  <!-- VLAN ID -->
                  <div class="field conditional security-groups vlan-type-tagged">
                    <div class="name">
                      <span>VLAN ID</span>
                    </div>
                    <div class="value">
                      <input type="text" name="vlan-id" class="required"/>
                    </div>
                  </div>

                  <!-- Scope -->
                  <div class="field conditional vlan-type-untagged">
                    <div class="name">
                      <span>Scope</span>
                    </div>
                    <div class="value">
                      <select class="ip-scope" name="ip-scope-untagged">
                        <option value="zone-wide">Zone-wide</option>
                      </select>
                    </div>
                  </div>

                  <!-- Scope (tagged) -->
                  <div class="field conditional vlan-type-tagged">
                    <div class="name">
                      <span>Scope</span>
                    </div>
                    <div class="value">
                      <select class="ip-scope" name="ip-scope-tagged">
                        <option value="zone-wide">Zone-wide</option>
                        <option value="account-specific">Account-specific</option>
                      </select>
                    </div>
                  </div>

                  <!-- Domain -->
                  <div class="field conditional ip-scope-account-specific">
                    <div class="name">
                      <span>Domain</span>
                    </div>
                    <div class="value">
                      <select name="ip-domain" class="domain required"></select>
                    </div>
                  </div>

                  <!-- Account -->
                  <div class="field conditional ip-scope-account-specific">
                    <div class="name">
                      <span>Account</span>
                    </div>
                    <div class="value">
                      <input type="text" name="account" />
                    </div>
                  </div>

                  <!-- Gateway -->
                  <div class="field">
                    <div class="name">
                      <span>Guest Gateway</span>
                    </div>
                    <div class="value">
                      <input type="text" name="guest-gateway" class="required" />
                    </div>
                  </div>

                  <!-- Netmask -->
                  <div class="field">
                    <div class="name">
                      <span>Guest Netmask</span>
                    </div>
                    <div class="value">
                      <input type="text" name="guest-netmask" class="required" />
                    </div>
                  </div>

                  <!-- IP Range -->
                  <div class="field">
                    <div class="name">
                      <span>Guest IP Range</span>
                    </div>
                    <div class="value multi-range">
                      <input type="text" name="guest-ip-range-start" class="required" />
                      <span class="divider">-</span>
                      <input type="text" name="guest-ip-range-end" />
                    </div>
                  </div>
                </div>
              </div>
            </form>
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

      <!-- System dashboard -->
      <div class="system-dashboard-view">
        <div class="toolbar"></div>

        <!-- Zone dashboard -->
        <div class="system-dashboard">
          <div class="head">
            <span>Zones</span>
            <div class="view-more"><span>View more</span></div>
          </div>
          <ul class="status_box good">
            <li class="block">
              <span class="header">Number of Zones</span>
              <span class="overview total">0</span>
            </li>
            <li class="block">
              <span class="header">Number of Pods</span>
              <span class="overview total">0</span>
            </li>
            <li class="block">
              <span class="header">Number of Clusters</span>
              <span class="overview total">0</span>
            </li>
            <li class="block last">
              <span class="header">Number of Hosts</span>
              <span class="overview total">0</span>
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
              <span class="overview total">0</span>
            </li>
            <li class="block">
              <span class="header">Total CPU</span>
              <span class="overview total">500</span>
            </li>
            <li class="block">
              <span class="header">Total Memory</span>
              <span class="overview total">60</span>
            </li>
            <li class="block last">
              <span class="header">Total Storage</span>
              <span class="overview total">10</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Zone chart -->
      <div class="zone-chart">
        <!-- NAAS configuration -->
        <div class="resources naas">
          <div class="tooltips">
            <div class="tooltip-info">
              <div class="arrow"></div>
              <div class="title">Hints</div>
              <div class="content">
                <p>
                  Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                </p>
              </div>
            </div>
          </div>
          <div class="head">
            <span>Zone Configuration</span>
            <div class="add">Add Network</div>
          </div>
          <ul class="system-main">
            <li class="main public" rel="public">
              <div class="name">Public</div>
              <div class="view-all configure">Configure</div>
            </li>
            <li class="main management" rel="management">
              <div class="name">Management</div>
              <div class="view-all configure">Configure</div>
            </li>
            <li class="main guest" rel="guest">
              <div class="name">Guest</div>
              <div class="view-all configure">Configure</div>
            </li>
          </ul>
        </div>

        <!-- Zone resources -->
        <div class="resources zone">
          <div class="head">
            <div class="add">Add Resource</div>
          </div>
          <ul>
            <li class="pod">
              <div class="name"><span>Pods</span></div>
              <div class="view-all" zone-target="pods">View All</div>
            </li>
            <li class="cluster">
              <div class="name"><span>Clusters</span></div>
              <div class="view-all" zone-target="clusters">View All</div>
            </li>
            <li class="host">
              <div class="name"><span>Hosts</span></div>
              <div class="view-all" zone-target="hosts">View All</div>
            </li>
            <li class="primary-storage">
              <div class="name"><span>Primary Storage</span></div>
              <div class="view-all" zone-target="primary-storage">View All</div>
            </li>
            <li class="secondary-storage">
              <div class="name"><span>Secondary Storage</span></div>
              <div class="view-all" zone-target="secondary-storage">View All</div>
            </li>
          </ul>
        </div>
      </div>

      <!-- Admin dashboard -->
      <div class="dashboard admin">
        <!-- Capacity / stats -->
        <div class="dashboard-container head">
          <div class="top">
            <div class="title">
              <span>System-wide capacity</span>
            </div>
            <div class="selects">
              <div class="select">
                <label>Zone:</label>
                <select>
                  <option>San Jose</option>
                </select>
              </div>
              <div class="select">
                <label>Pods:</label>
                <select>
                  <option>All Pods</option>
                  <option>Pod A</option>
                  <option>Pod B</option>
                </select>
              </div>
            </div>
          </div>
          <div class="stats">
            <ul>
              <li>
                <div class="name">Public IP Addresses</div>
                <div class="value">
                  <div class="content">
                    Allocated: <span class="allocated">35</span> / <span class="total">100</span>
                  </div>
                </div>
                <div class="chart">
                  <div class="chart-line" style="width: 35%;"></div>
                </div>
                <div class="percentage"><span class="value">35</span>%</div>
              </li>
              <li>
                <div class="name">Private IP Addresses</div>
                <div class="value">
                  <div class="content">
                    Allocated: <span class="allocated">15</span> / <span class="total">100</span>
                  </div>
                </div>
                <div class="chart">
                  <div class="chart-line" style="width: 15%;"></div>
                </div>
                <div class="percentage"><span class="value">15</span>%</div>
              </li>
              <li>
                <div class="name">Memory Allocated</div>
                <div class="value">
                  <div class="content">
                    Allocated: <span class="allocated">23mb</span> / <span class="total">100mb</span>
                  </div>
                </div>
                <div class="chart">
                  <div class="chart-line" style="width: 23%;"></div>
                </div>
                <div class="percentage"><span class="value">23</span>%</div>
              </li>
              <li>
                <div class="name">CPU Allocated</div>
                <div class="value">
                  <div class="content">
                    Allocated: <span class="allocated">75</span> / <span class="total">100</span>
                  </div>
                </div>
                <div class="chart">
                  <div class="chart-line" style="width: 75%;"></div>
                </div>
                <div class="percentage"><span class="value">75</span>%</div>
              </li>
            </ul>
          </div>
        </div>

        <!-- General alerts-->
        <div class="dashboard-container sub alerts first">
          <div class="top">
            <div class="title"><span>General Alerts</span></div>
            <div class="button view-all">view all</div>
          </div>
          <ul>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
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
          <ul>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
            <li class="error">
              <div class="content">
                <span class="title">Monitoring - Console Proxy</span>
                <p>Unable to restart v-14-VM</p>
              </div>
            </li>
          </ul>
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
                <div class="value">110</div>
              </li>
              <li class="stopped">
                <div class="name">Stopped VMs</div>
                <div class="value">10</div>
              </li>
              <li class="total">
                <div class="name">Total VMs</div>
                <div class="value">120</div>
              </li>
            </ul>
          </div>
        </div>

        <div class="status-lists">
          <ul>
            <li class="recent-errors">
              <table>
                <thead>
                  <tr>
                    <th>Recent Errors</th>
                  </tr>
                </thead>
              </table>
              <div class="content">
                <table>
                  <tbody>
                    <tr><td class="desc">Error 1</td><td class="value">Desc</td></tr>
                    <tr class="odd"><td>Error 2</td><td class="value">Desc</td></tr>
                    <tr><td class="desc">Error 3</td><td class="value">Desc</td></tr>
                    <tr class="odd"><td>Error 4</td><td class="value">Desc</td></tr>
                    <tr><td class="desc">Error 5</td><td class="value">Desc</td></tr>
                    <tr class="odd"><td>Error 6</td><td class="value">Desc</td></tr>
                    <tr><td class="desc">Error 7</td><td class="value">Desc</td></tr>
                    <tr class="odd"><td>Error 7</td><td class="value">Desc</td></tr>
                    <tr><td class="desc">Error 9</td><td class="value">Desc</td></tr>
                  </tbody>
                </table>
              </div>
            </li>
            <li class="my-account">
              <table>
                <thead>
                  <tr>
                    <th>My Account</th>
                  </tr>
                </thead>
              </table>
              <div class="content">
                <table>
                  <tbody>
                    <tr><td class="desc">Account ID</td><td class="value">Desc</td></tr>
                    <tr class="odd"><td>Account</td><td class="value">Desc</td></tr>
                    <tr><td>Type</td><td class="value">Desc</td></tr>
                    <tr class="odd"><td class="desc">Domain</td><td class="value">Desc</td></tr>
                    <tr><td class="desc">Some other value</td><td class="value">Desc</td></tr>
                  </tbody>
                </table>
              </div>
            </li>
            <li class="ip-addresses">
              <table>
                <thead>
                  <tr>
                    <th>IP Addresses</th>
                  </tr>
                </thead>
              </table>
              <table>
                <tbody>
                  <tr>
                    <td>
                      <div class="desc"><span>Available public IP addresses:</span></div>
                      <div class="value"><span>18</span></div>
                    </td>
                  </tr>
                  <tr class="odd">
                    <td>
                      <div class="desc"><span>Owned public IP addresses:</span></div>
                      <div class="value"><span>03</span></div>
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

    <!-- UI -->
    <script src="scripts/ui/core.js" type="text/javascript"></script>
    <script src="scripts/ui/utils.js" type="text/javascript"></script>
    <script src="scripts/ui/events.js" type="text/javascript"></script>
    <script src="scripts/ui/dialog.js" type="text/javascript"></script>
    <script src="scripts/ui/multiEdit.js" type="text/javascript"></script>

    <!-- UI - Widgets -->
    <script src="scripts/ui/widgets/overlay.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/dataTable.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/cloudBrowser.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/listView.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/detailView.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/treeView.js" type="text/javascript"></script>
    <script src="scripts/ui/widgets/notifications.js" type="text/javascript"></script>

    <script src="scripts-test/test-data.js" type="text/javascript"></script>

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
    <script src="scripts/ui-custom/physicalResources.js" type="text/javascript"></script>
    <script src="scripts/ui-custom/zoneChart.js" type="text/javascript"></script>
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
