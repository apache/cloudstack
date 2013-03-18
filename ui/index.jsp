<%--
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
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
  <fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>
<% long now = System.currentTimeMillis(); %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title></title>
    <link type="text/css" rel="stylesheet" href="lib/reset.css"/>
    <link type="text/css" rel="stylesheet" href="css/cloudstack3.css" />
    <c:if test="${!empty cookie.lang && cookie.lang.value != 'en'}">
      <link type="text/css" rel="stylesheet" href="css/cloudstack3.${cookie.lang.value}.css" />
    </c:if>
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
            <input type="submit" value="<fmt:message key="label.login"/>" />
            <!-- Select language -->
            <div class="select-language">
              <select name="language">
                <option value=""></option> <!-- when this blank option is selected, browser's default language will be used -->
                <option value="en"><fmt:message key="label.lang.english"/></option>
                <option value="ja"><fmt:message key="label.lang.japanese"/></option>
		<option value="zh_CN"><fmt:message key="label.lang.chinese"/></option>
                <option value="ru_RU"><fmt:message key="label.lang.russian"/></option>
                <option value="fr_FR"><fmt:message key="label.lang.french"/></option>
                <option value="pt_BR"><fmt:message key="label.lang.brportugese"/></option>
                <option value="ko_KR"><fmt:message key="label.lang.korean"/></option>
              </select>
            </div>
          </div>
        </form>
      </div>
      <!-- Instance wizard -->
      <div class="multi-wizard instance-wizard">
        <div class="progress">
          <ul>
            <li class="first"><span class="number">1</span><span><fmt:message key="label.setup"/></span><span class="arrow"></span></li>
            <li><span class="number">2</span><span class="multiline"><fmt:message key="label.select.a.template"/></span><span class="arrow"></span></li>
            <li><span class="number">3</span><span class="multiline"><fmt:message key="label.compute.offering"/></span><span class="arrow"></span></li>
            <li><span class="number">4</span><span class="multiline"><fmt:message key="label.data.disk.offering"/></span><span class="arrow"></span></li>
            <li><span class="number">5</span><span><fmt:message key="label.menu.network"/></span><span class="arrow"></span></li>
            <li class="last"><span class="number">6</span><span><fmt:message key="label.review"/></span></li>
          </ul>
        </div>
        <form>
          <div class="steps">
            <!-- Step 1: Setup -->
            <div class="step setup" wizard-step-id="setup">
              <div class="content">
                <!-- Select a zone -->
                <div class="section select-zone">
                  <h3><fmt:message key="label.select.a.zone"/></h3>
                  <p><fmt:message key="message.select.a.zone"/></p>
                  <div class="select-area">
                    <div class="desc"></div>
                    <select name="zoneid" class="required">
                      <option default="default" value="" ><fmt:message key="label.select.a.zone"/></option>
                    </select>
                  </div>
                </div>
                <!-- Select template -->
                <div class="section select-template">
                  <h3><fmt:message key="label.select.iso.or.template" /></h3>
                  <p></p>
                  <div class="select-area">
                    <div class="desc"><fmt:message key="message.template.desc"/></div>
                    <input type="radio" name="select-template" value="select-template" />
                    <label><fmt:message key="label.template"/></label>
                  </div>
                  <div class="select-area">
                    <div class="desc"><fmt:message key="message.iso.desc"/></div>
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
                  <p><fmt:message key="message.select.template"/></p>
                </div>
                <div class="template-select content tab-view">
                  <ul>
                    <li class="first"><a href="#instance-wizard-featured-templates"><fmt:message key="label.featured"/></a></li>
                    <li><a href="#instance-wizard-community-templates"><fmt:message key="label.community"/></a></li>
                    <li class="last"><a href="#instance-wizard-my-templates"><fmt:message key="label.my.templates"/></a></li>
                  </ul>

                  <!-- Used for Select Template only -->
                  <input type="hidden" wizard-field="hypervisor" name="hypervisor" value="" disabled="disabled"/>

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
                  <p><fmt:message key="message.select.iso"/></p>
                </div>
                <div class="iso-select content tab-view">
                  <ul>
                    <li class="first"><a href="#instance-wizard-featured-isos"><fmt:message key="label.featured"/></a></li>
                    <li><a href="#instance-wizard-community-isos"><fmt:message key="label.community"/></a></li>
                    <li class="last"><a href="#instance-wizard-my-isos"><fmt:message key="label.menu.my.isos"/></a></li>
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
                  <label><fmt:message key="label.no.thanks"/></label>
                </div>

                <!-- Existing offerings -->
                <div class="select-container">
                </div>

                <!-- Custom size slider -->
                <div class="section custom-size">
                  <label><fmt:message key="label.disk.size"/></label>

                  <!-- Slider -->
                  <label class="size">1 GB</label>
                  <div class="slider custom-size"></div>
                  <label class="size max"><span></span> GB</label>

                  <input type="text" class="required digits" name="size" value="1" />
                  <label class="size">GB</label>
                </div>
              </div>
            </div>

            <!-- Step 5: Network -->
            <div class="step network always-load" wizard-step-id="network">
              <!-- 5a: Network description -->
              <div class="wizard-step-conditional nothing-to-select">     
								<p id="from_instance_page_1"><fmt:message key="message.zone.no.network.selection"/></p>
								<p id="from_instance_page_2"><fmt:message key="message.please.proceed"/></p>
								<p id="from_vpc_tier"></p>     										
              </div>

              <!-- 5b: Select network -->
              <div class="wizard-step-conditional select-network">
                <div class="content">
                  <div class="main-desc">
                    <fmt:message key="message.please.select.networks"/>
                  </div>
                  <div class="select-vpc">
                    <label>VPC:</label>
                    <select name="vpc-filter">
                      <option value="-1">No VPC</option>
                    </select>
                  </div>
                  <div class="select my-networks">
                    <table>
                      <thead>
                        <tr>
                          <th><fmt:message key="label.networks"/></th>
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
                          <th><fmt:message key="label.add.network"/></th>
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
                                  <div class="name"><fmt:message key="label.new"/></div>
                                </div>

                                <!-- Name -->
                                <div class="field name hide-if-unselected">
                                  <div class="name"> <span class="field-required">*</span> <fmt:message key="label.name"/></div>
                                  <div class="value">
                                    <input type="text" class="required" name="new-network-name" />
                                  </div>
                                </div>

                                <!-- Service offering -->
                                <div class="select-desc field service-offering hide-if-unselected">
                                  <div class="name"><fmt:message key="label.network.offering"/></div>
                                  <div class="desc">
                                    <select name="new-network-networkofferingid">
                                    </select>
                                  </div>
                                </div>

                                <div class="secondary-input hide-if-unselected">
                                  <input type="radio" name="defaultNetwork" value="new-network" wizard-field="default-network" />
                                  <div class="name"><fmt:message key="label.default"/></div>
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
                  <fmt:message key="message.select.security.groups"/>
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
                <fmt:message key="message.vm.review.launch"/>
              </div>
              <div class="content">
                <div class="select-container">
                  <!-- Name -->
                  <div class="select vm-instance-name">
                    <div class="name">
                      <span><fmt:message key="label.name"/> (<fmt:message key="label.optional"/>)</span>
                    </div>
                    <div class="value">
                      <input type="text" name="displayname" />
                    </div>
                  </div>
                  <!-- Add to group -->
                  <div class="select odd">
                    <div class="name">
                      <span><fmt:message key="label.add.to.group"/> (<fmt:message key="label.optional"/>)</span>
                    </div>
                    <div class="value">
                      <input type="text" name="groupname" />
                    </div>
                  </div>
                  <!-- Zone -->
                  <div class="select">
                    <div class="name">
                      <span><fmt:message key="label.zone"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="zone"></span>
                    </div>
                    <div class="edit">
                      <a href="1"><fmt:message key="label.edit"/></a>
                    </div>
                  </div>
                  <!-- Hypervisor -->
                  <div class="select odd">
                    <div class="name">
                      <span><fmt:message key="label.hypervisor"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="hypervisor"></span>
                    </div>
                    <div class="edit">
                      <a href="2"><fmt:message key="label.edit"/></a>
                    </div>
                  </div>
                  <!-- Template -->
                  <div class="select">
                    <div class="name">
                      <span><fmt:message key="label.template"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="template"></span>
                    </div>
                    <div class="edit">
                      <a href="2"><fmt:message key="label.edit"/></a>
                    </div>
                  </div>
                  <!-- Service offering -->
                  <div class="select odd">
                    <div class="name">
                      <span><fmt:message key="label.compute.offering"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="service-offering"></span>
                    </div>
                    <div class="edit">
                      <a href="3"><fmt:message key="label.edit"/></a>
                    </div>
                  </div>
                  <!-- Data disk offering -->
                  <div class="select">
                    <div class="name">
                      <span><fmt:message key="label.data.disk.offering"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="disk-offering"></span>
                    </div>
                    <div class="edit">
                      <a href="4"><fmt:message key="label.edit"/></a>
                    </div>
                  </div>
                  <!-- Primary network -->
                  <div class="select odd">
                    <div class="name">
                      <span><fmt:message key="label.network"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="default-network" conditional-field="select-network"></span>
                    </div>
                    <div class="edit">
                      <a href="5"><fmt:message key="label.edit"/></a>
                    </div>
                  </div>
                  
                  <!-- Security groups -->
                  <div class="select odd">
                    <div class="name">
                      <span><fmt:message key="label.security.groups"/></span>
                    </div>
                    <div class="value">
                      <span wizard-field="security-groups" conditional-field="select-security-group"></span>
                    </div>
                    <div class="edit">
                      <a href="5"><fmt:message key="label.edit"/></a>
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
          <div class="button previous"><span><fmt:message key="label.previous"/></span></div>
          <div class="button cancel"><span><fmt:message key="label.cancel"/></span></div>
          <div class="button next"><span><fmt:message key="label.next"/></span></div>
        </div>
      </div>
      <!-- Zone wizard -->
      <div class="multi-wizard zone-wizard">
        <div class="progress">
          <ul>
            <li class="first"><span class="number">1</span><span><fmt:message key="label.zone.type"/></span><span class="arrow"></span></li>
            <li><span class="number">2</span><span><fmt:message key="label.setup.zone"/></span><span class="arrow"></span></li>
            <li><span class="number">3</span><span><fmt:message key="label.setup.network"/></span><span class="arrow"></span></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li><span class="number">4</span><span><fmt:message key="label.add.resources"/></span><span class="arrow"></span></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li style="display:none;"></li>
            <li class="last"><span class="number">5</span><span><fmt:message key="label.launch"/></span></li>
          </ul>
        </div>
        <div class="steps">
          <!-- Step 1: Select network -->
          <div class="select-network" zone-wizard-step-id="selectZoneType">
            <form>
              <div class="content">
                <!-- Select template -->
                <div class="section select-network-model">
                  <h3><fmt:message key="label.set.up.zone.type"/></h3>
                  <p><fmt:message key="message.please.select.a.configuration.for.your.zone"/></p>
                  <div class="select-area basic-zone">
                    <div class="desc">
                      <fmt:message key="message.desc.basic.zone"/>
                    </div>
                    <input type="radio" name="network-model" value="Basic" checked="checked" />
                    <label><fmt:message key="label.basic"/></label>
                  </div>
                  <div class="select-area advanced-zone disabled">
                    <div class="desc">
                      <fmt:message key="message.desc.advanced.zone"/>  
										</div>
                    <input type="radio" name="network-model" value="Advanced" />
                    <label><fmt:message key="label.advanced"/></label>
                    <!-- Isolation mode -->
                    <div class="isolation-mode">
                      <div class="title">
                        <fmt:message key="label.isolation.mode"/>
                      </div>

                      <!-- Security groups -->
                      <div class="select-area">
                        <div class="desc">
                          <fmt:message key="message.advanced.security.group"/>
                        </div>
                        <input type="checkbox" name="zone-advanced-sg-enabled" disabled="disabled" />
                        <label><fmt:message key="label.menu.security.groups"/></label>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </form>
          </div>
          <!-- Step 2: Add zone -->
          <div class="setup-zone" zone-wizard-form="zone"
               zone-wizard-step-id="addZone">
            <div class="info-desc">
              <fmt:message key="message.desc.zone"/> 
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
              <li class="physical-network active"><fmt:message key="label.physical.network"/></li>
              <li class="public-network"><fmt:message key="label.public.traffic"/></li>
              <li class="pod"><fmt:message key="label.pod"/></li>
              <li class="guest-traffic"><fmt:message key="label.guest.traffic"/></li>
              <li class="conditional storage-traffic"><fmt:message key="label.storage.traffic"/></li>
            </ul>
            <div class="info-desc conditional advanced">
              <fmt:message key="message.setup.physical.network.during.zone.creation"/>
            </div>
            <div class="info-desc conditional basic">
              <fmt:message key="message.setup.physical.network.during.zone.creation.basic"/>
            </div>
            <div class="button add new-physical-network"><span class="icon">&nbsp;</span><span><fmt:message key="label.add.physical.network"/></span></div>
            <!-- Traffic types drag area -->
            <div class="traffic-types-drag-area">
              <div class="header"><fmt:message key="label.traffic.types"/></div>
              <ul>
                <li class="management">
                  <ul class="container">
                    <li traffic-type-id="management"
                        title="<fmt:message key="label.zoneWizard.trafficType.management"/>"
                        class="traffic-type-draggable management">
                      <!-- Edit buttton -->
                      <div class="edit-traffic-type">
                        <span class="name"><fmt:message key="label.management"/></span>
                        <span class="icon">&nbsp;</span>
                        <span>Edit</span>
                      </div>
                    </li>
                  </ul>
                  <div class="info">
                    <div class="title"><fmt:message key="label.management"/></div>
                    <div class="desc"></div>
                  </div>
                </li>
                <li class="public">
                  <ul class="container">
                    <li traffic-type-id="public"
                        title="<fmt:message key="label.zoneWizard.trafficType.public"/>"
                        class="traffic-type-draggable public">
                      <!-- Edit buttton -->
                      <div class="edit-traffic-type">
                        <span class="name"><fmt:message key="label.public"/></span>
                        <span class="icon">&nbsp;</span>
                        <span>Edit</span>
                      </div>
                    </li>
                  </ul>
                  <div class="info">
                    <div class="title"><fmt:message key="label.public"/></div>
                    <div class="desc"></div>
                  </div>
                </li>
                <li class="guest">
                  <ul class="container">
                    <li traffic-type-id="guest"
                        title="<fmt:message key="label.zoneWizard.trafficType.guest"/>"
                        class="traffic-type-draggable guest">
                      <!-- Edit buttton -->
                      <div class="edit-traffic-type">
                        <span class="name"><fmt:message key="label.guest"/></span>
                        <span class="icon">&nbsp;</span>
                        <span>Edit</span>
                      </div>
                    </li>
                  </ul>
                  <div class="info">
                    <div class="title"><fmt:message key="label.guest"/></div>
                    <div class="desc"></div>
                  </div>
                </li>
                <li class="storage">
                  <ul class="container">
                    <li traffic-type-id="storage"
                        title="<fmt:message key="label.zoneWizard.trafficType.storage"/>"
                        class="traffic-type-draggable storage">
                      <!-- Edit buttton -->
                      <div class="edit-traffic-type">
                        <span class="name"><fmt:message key="label.storage"/></span>
                        <span class="icon">&nbsp;</span>
                        <span>Edit</span>
                      </div>
                    </li>
                  </ul>
                  <div class="info">
                    <div class="title"><fmt:message key="label.storage"/></div>
                    <div class="desc"></div>
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
              <li class="conditional netscaler physical-network active"><fmt:message key="label.netScaler"/></li>
              <li class="public-network"><fmt:message key="label.public.traffic"/></li>
              <li class="pod"><fmt:message key="label.pod"/></li>
              <li class="guest-traffic"><fmt:message key="label.guest.traffic"/></li>
              <li class="conditional storage-traffic"><fmt:message key="label.storage.traffic"/></li>
            </ul>
            <div class="info-desc"><fmt:message key="label.please.specify.netscaler.info"/></div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>
          <!-- Step 3.2: Configure public traffic -->
          <div class="setup-public-traffic" zone-wizard-prefilter="addPublicNetwork"
               zone-wizard-step-id="configurePublicTraffic">
            <ul class="subnav">
              <li class="conditional netscaler physical-network"><fmt:message key="label.netScaler"/></li>
              <li class="public-network active"><fmt:message key="label.public.traffic"/></li>
              <li class="pod"><fmt:message key="label.pod"/></li>
              <li class="guest-traffic"><fmt:message key="label.guest.traffic"/></li>
              <li class="conditional storage-traffic"><fmt:message key="label.storage.traffic"/></li>
            </ul>
            <div class="info-desc" id="add_zone_public_traffic_desc">
              <span id="for_basic_zone" style="display:none"><fmt:message key="message.public.traffic.in.basic.zone"/></span>
							<span id="for_advanced_zone" style="display:none"><fmt:message key="message.public.traffic.in.advanced.zone"/></span>
            </div>
            <div ui-custom="publicTrafficIPRange"></div>
          </div>
          <!-- Step 3.3: Add pod -->
          <div class="add-pod" zone-wizard-form="pod"
               zone-wizard-step-id="addPod">
            <ul class="subnav">
              <li class="conditional netscaler physical-network"><fmt:message key="label.netScaler"/></li>
              <li class="public-network"><fmt:message key="label.public.traffic"/></li>
              <li class="pod active"><fmt:message key="label.pod"/></li>
              <li class="guest-traffic"><fmt:message key="label.guest.traffic"/></li>
              <li class="conditional storage-traffic"><fmt:message key="label.storage.traffic"/></li>
            </ul>
            <div class="info-desc">
              <fmt:message key="message.add.pod.during.zone.creation"/>
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
              <li class="conditional netscaler physical-network"><fmt:message key="label.netScaler"/></li>
              <li class="public-network"><fmt:message key="label.public.traffic"/></li>
              <li class="pod"><fmt:message key="label.pod"/></li>
              <li class="guest-traffic active"><fmt:message key="label.guest.traffic"/></li>
              <li class="conditional storage-traffic"><fmt:message key="label.storage.traffic"/></li>
            </ul>
            <div class="info-desc" id="add_zone_guest_traffic_desc">
              <span id="for_basic_zone" style="display:none"><fmt:message key="message.guest.traffic.in.basic.zone"/></span>
							<span id="for_advanced_zone" style="display:none"><fmt:message key="message.guest.traffic.in.advanced.zone"/></span>
            </div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>
          <!-- Step 3.5: Configure storage traffic -->
          <div class="setup-storage-traffic" zone-wizard-prefilter="configureStorageTraffic"
               zone-wizard-step-id="configureStorageTraffic">
            <ul class="subnav">
              <li class="conditional netscaler physical-network"><fmt:message key="label.netScaler"/></li>
              <li class="public-network"><fmt:message key="label.public.traffic"/></li>
              <li class="pod"><fmt:message key="label.pod"/><</li>
              <li class="guest-traffic"><fmt:message key="label.guest.traffic"/></li>
              <li class="storage-traffic active"><fmt:message key="label.storage.traffic"/></li>
            </ul>
            <div class="info-desc">
              <fmt:message key="message.storage.traffic"/>
            </div>
            <div ui-custom="storageTrafficIPRange"></div>
          </div>
          <!-- Step 4.1: Add cluster -->
          <div class="add-cluster" zone-wizard-form="cluster"
               zone-wizard-step-id="addCluster">
            <ul class="subnav">
              <li class="cluster active"><fmt:message key="label.cluster"/></li>
              <li class="host"><fmt:message key="label.host"/></li>
              <li class="primary-storage"><fmt:message key="label.primary.storage"/></li>
              <li class="secondary-storage"><fmt:message key="label.secondary.storage"/></li>
            </ul>

            <div class="info-desc">
              <fmt:message key="message.desc.cluster"/>
						</div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>
          <!-- Step 4.2: Add host -->
          <div class="add-cluster" zone-wizard-form="host"
               zone-wizard-step-id="addHost" zone-wizard-prefilter="addHost">
            <ul class="subnav">
              <li class="cluster"><fmt:message key="label.cluster"/></li>
              <li class="host active"><fmt:message key="label.host"/></li>
              <li class="primary-storage"><fmt:message key="label.primary.storage"/></li>
              <li class="secondary-storage"><fmt:message key="label.secondary.storage"/></li>
            </ul>
            <div class="info-desc">
              <fmt:message key="message.desc.host"/>         
						</div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>
          <!-- Step 4.3: Add primary storage -->
          <div class="add-cluster" zone-wizard-form="primaryStorage" zone-wizard-prefilter="addPrimaryStorage"
               zone-wizard-step-id="addPrimaryStorage">
            <ul class="subnav">
              <li class="cluster"><fmt:message key="label.cluster"/></li>
              <li class="host"><fmt:message key="label.host"/></li>
              <li class="primary-storage active"><fmt:message key="label.primary.storage"/></li>
              <li class="secondary-storage"><fmt:message key="label.secondary.storage"/></li>
            </ul>
            <div class="info-desc">
              <fmt:message key="message.desc.primary.storage"/>
						</div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>
          <!-- Step 4.4: Add secondary storage -->
          <div class="add-cluster" zone-wizard-form="secondaryStorage"
               zone-wizard-step-id="addSecondaryStorage">
            <ul class="subnav">
              <li class="cluster"><fmt:message key="label.cluster"/></li>
              <li class="host"><fmt:message key="label.host"/></li>
              <li class="primary-storage"><fmt:message key="label.primary.storage"/></li>
              <li class="secondary-storage active"><fmt:message key="label.secondary.storage"/></li>
            </ul>
            <div class="info-desc">
              <fmt:message key="message.desc.secondary.storage"/>
						</div>
            <div class="content input-area">
              <div class="select-container"></div>
            </div>
          </div>
          <!-- Step 5: Launch -->
          <div class="review" zone-wizard-step-id="launch">
            <div class="main-desc pre-setup"><fmt:message key="message.launch.zone"/></div>
            <div class="main-desc launch" style="display:none;">
              <fmt:message key="message.please.wait.while.zone.is.being.created"/>
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
          <div class="button previous"><span><fmt:message key="label.previous"/></span></div>
          <div class="button cancel"><span><fmt:message key="label.cancel"/></span></div>
          <div class="button next"><span><fmt:message key="label.next"/></span></div>
        </div>
      </div>
      <!-- Network chart -->
      <div class="network-chart normal">
        <ul>
          <li class="firewall">
            <div class="name"><span><fmt:message key="label.firewall"/></span></div>
            <div class="view-details" net-target="firewall"><fmt:message key="label.view.all"/></div>
          </li>
          <li class="loadBalancing">
            <div class="name"><span><fmt:message key="label.load.balancing"/></span></div>
            <div class="view-details" net-target="loadBalancing"><fmt:message key="label.view.all"/></div>
          </li>
          <li class="portForwarding">
            <div class="name"><span><fmt:message key="label.port.forwarding"/></span></div>
            <div class="view-details" net-target="portForwarding"><fmt:message key="label.view.all"/></div>
          </li>
        </ul>
      </div>
      <!-- Static NAT network chart -->
      <div class="network-chart static-nat">
        <ul>
          <li class="static-nat-enabled">
            <div class="name"><span><fmt:message key="label.static.nat.enabled"/></span></div>
            <div class="vmname"></div>
          </li>
          <li class="firewall">
            <div class="name"><span><fmt:message key="label.firewall"/></span></div>
            <!--<div class="view-details" net-target="staticNAT"><fmt:message key="label.view.all"/></div>-->
						<div class="view-details" net-target="firewall"><fmt:message key="label.view.all"/>
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
                <span><fmt:message key="label.compute.and.storage"/></span>
              </div>
              <ul class="status_box good">
                <!-- Virtual Machines -->
                <li class="block virtual-machines">
                  <span class="header"><fmt:message key="label.virtual.machines"/></span>
                  <div class="icon"></div>
                  <div class="overview">
                    <!-- Running -->
                    <div class="overview-item running">
                      <div class="total" data-item="runningInstances">5</div>
                      <div class="label"><fmt:message key="state.Running"/></div>
                    </div>

                    <!-- Stopped -->
                    <div class="overview-item stopped">
                      <div class="total" data-item="stoppedInstances">10</div>
                      <div class="label"><fmt:message key="state.Stopped"/></div>
                    </div>
                  </div>
                </li>

                <!-- Storage -->
                <li class="block storage">
                  <span class="header"><fmt:message key="label.storage"/></span>
                  <div class="icon"></div>
                  <div class="overview">
                    <div class="total" data-item="totalVolumes">10</div>
                    <div class="label"><fmt:message key="label.volumes"/></div>
                  </div>
                </li>

                <!-- Bandwidth -->
                <li class="block storage bandwidth">
                  <span class="header"><fmt:message key="label.bandwidth"/></span>
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
                <span><fmt:message key="label.users"/></span>
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
              <span><fmt:message key="label.networking.and.security"/></span>
            </div>
            <ul>
              <!-- IP addresses -->
              <li class="odd">
                <div class="total"><span data-item="totalIPAddresses"></span></div>
                <div class="desc"><fmt:message key="label.menu.ipaddresses"/></div>
              </li>

              <!-- Load balancing policies -->
              <li>
                <div class="total"><span data-item="totalLoadBalancers"></span></div>
                <div class="desc"><fmt:message key="label.load.balancing.policies"/></div>
              </li>

              <!-- Port forwarding policies -->
              <li class="odd">
                <div class="total"><span data-item="totalPortForwards"></span></div>
                <div class="desc"><fmt:message key="label.port.forwarding.policies"/></div>
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
                    <span><fmt:message key="label.manage.resources"/></span>
                    <span class="arrow"></span>
                  </div>
                </div>
              </li>
            </ul>
          </div>
          <!-- Events -->
          <div class="info-box events">
            <div class="title">
              <span><fmt:message key="label.menu.events"/></span>
              <div class="button view-all">
                <span><fmt:message key="label.view.all"/></span>
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
          <div class="button refresh" id="refresh_button">
            <span><fmt:message key="label.refresh"/></span>
          </div>
          <div id="update_ssl_button" class="button action main-action reduced-hide lock" title="Updates your Console Proxy SSL Certificate">
            <span class="icon">&nbsp;</span>            
            <span><fmt:message key="label.update.ssl.cert"/></span>
          </div>
        </div>

        <!-- Zone dashboard -->
        <div class="system-dashboard zone">
          <div class="head">
            <span><fmt:message key="label.menu.infrastructure"/></span>
          </div>
          <ul class="status_box good">
            <li class="block zones">
              <span class="header"><fmt:message key="label.zones"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="zoneCount"></span>
              <span class="button view-all zones"
                    view-all-title="<fmt:message key="label.zones"/>"
                    view-all-target="zones"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block pods">
              <span class="header"><fmt:message key="label.pods"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="podCount"></span>
              <span class="button view-all pods"
                    view-all-title="<fmt:message key="label.pods"/>"
                    view-all-target="pods"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block clusters">
              <span class="header"><fmt:message key="label.clusters"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="clusterCount"></span>
              <span class="button view-all clusters"
                    view-all-title="<fmt:message key="label.clusters"/>"
                    view-all-target="clusters"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block last hosts">
              <span class="header"><fmt:message key="label.hosts"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="hostCount"></span>
              <span class="button view-all hosts"
                    view-all-title="<fmt:message key="label.hosts"/>"
                    view-all-target="hosts"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block primary-storage">
              <span class="header"><fmt:message key="label.primary.storage"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="primaryStorageCount"></span>
              <span class="button view-all zones"
                    view-all-title="<fmt:message key="label.primary.storage"/>"
                    view-all-target="primaryStorage"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block secondary-storage">
              <span class="header"><fmt:message key="label.secondary.storage"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="secondaryStorageCount"></span>
              <span class="button view-all pods"
                    view-all-title="<fmt:message key="label.secondary.storage"/>"
                    view-all-target="secondaryStorage"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block system-vms">
              <span class="header"><fmt:message key="label.system.vms"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="systemVmCount"></span>
              <span class="button view-all clusters"
                    view-all-title="<fmt:message key="label.system.vms"/>"
                    view-all-target="systemVms"><fmt:message key="label.view.all"/></span>
            </li>
            <li class="block last virtual-routers">
              <span class="header"><fmt:message key="label.virtual.routers"/></span>
              <span class="icon">&nbsp;</span>
              <span class="overview total" data-item="virtualRouterCount"></span>
              <span class="button view-all hosts"
                    view-all-title="<fmt:message key="label.virtual.routers"/>"
                    view-all-target="virtualRouters"><fmt:message key="label.view.all"/></span>
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
				        <p data-list-item="sent">Alert 1</p>
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
            
             <div class="button fetch-latest">
               <span><fmt:message key="label.fetch.latest"/></span>
              </div>

            <div class="selects" style="display:none;">
              <div class="select">
                <label><fmt:message key="label.zone"/>:</label>
                <select>
                </select>
              </div>
              <div class="select">
                <label><fmt:message key="label.pods"/>:</label>
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
                  <fmt:message key="label.zone"/>: <span data-list-item="zoneName"></span>
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
          <div class="title"><span><fmt:message key="label.virtual.machines"/></span></div>

          <div class="content">
            <ul>
              <li class="running">
                <div class="name"><fmt:message key="label.running.vms"/></div>
                <div class="value" data-item="runningInstances"></div>
              </li>
              <li class="stopped">
                <div class="name"><fmt:message key="label.stopped.vms"/></div>
                <div class="value" data-item="stoppedInstances"></div>
              </li>
              <li class="total">
                <div class="name"><fmt:message key="label.total.vms"/></div>
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
                    <th><fmt:message key="label.latest.events"/> <div class="button view-all events"><fmt:message key="label.view.all"/></div></th>
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
                    <th><fmt:message key="label.network"/> <div class="button view-all network"><fmt:message key="label.view.all"/></div></th>
                  </tr>
                </thead>
              </table>
              <table>
                <tbody>
                  <tr>
                    <td>
                      <div class="desc"><span><fmt:message key="label.isolated.networks"/>:</span></div>
                      <div class="value"><span data-item="netTotal"></span></div>
                    </td>
                  </tr>
                  <tr class="odd">
                    <td>
                      <div class="desc"><span><fmt:message key="label.public.ips"/>:</span></div>
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
    <script src="lib/date.js" type="text/javascript"></script>
    <script src="lib/jquery.cookies.js" type="text/javascript"></script>
    <script src="lib/jquery.md5.js" type="text/javascript" ></script>
    <script src="lib/require.js" type="text/javascript"></script>

    <script src="lib/excanvas.js" type="text/javascript"></script>
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
    <!-- CloudStack -->
    <script type="text/javascript" src="scripts/ui/core.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/utils.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/events.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/dialog.js?t=<%=now%>"></script>
    
    <script type="text/javascript" src="scripts/ui/widgets/multiEdit.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/overlay.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/dataTable.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/cloudBrowser.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/listView.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/detailView.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/treeView.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/notifications.js?t=<%=now%>"></script> 
    <script type="text/javascript" src="scripts/ui/widgets/tagger.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui/widgets/toolTip.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloud.core.callbacks.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/sharedFunctions.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/login.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/projects.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/cloudStack.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/lbStickyPolicy.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/autoscaler.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/healthCheck.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/autoscaler.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/zoneChart.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/dashboard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/installWizard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/installWizard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/projects.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/dashboard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/instanceWizard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/instanceWizard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/instances.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/events.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/regions.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/regions.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/ipRules.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/enableStaticNAT.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/securityRules.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/vpc.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/vpc.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/network.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/recurringSnapshots.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/uploadVolume.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/storage.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/templates.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/accounts.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/configuration.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/globalSettings.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/zoneWizard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/physicalResources.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/ui-custom/zoneWizard.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/system.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/domains.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/docs.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/vm_snapshots.js?t=<%=now%>"></script>  

    <!-- Plugins -->
    <script type="text/javascript" src="scripts/ui-custom/plugins.js?t=<%=now%>"></script>
    <script type="text/javascript" src="plugins/plugins.js?t=<%=now%>"></script>
    <script type="text/javascript" src="scripts/plugins.js?t=<%=now%>"></script>
  </body>
</html>
<jsp:include page="dictionary.jsp" />
