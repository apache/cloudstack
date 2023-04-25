// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
// $message.success(`${$t('label.copied.clipboard')} : ${name}`)
<template>
  <a-spin :spinning="loading">
    <a-card class="spin-content" :bordered="bordered" :title="title">
      <div>
        <div class="resource-details">
          <div class="resource-details__name">
            <div
              class="avatar"
              @click="showUploadModal(true)"
              v-clipboard:copy="name" >
              <upload-resource-icon v-if="'uploadResourceIcon' in $store.getters.apis" :visible="showUpload" :resource="resource" @handle-close="showUpload(false)"/>
              <div class="ant-upload-preview" v-if="$showIcon()">
                <camera-outlined class="upload-icon"/>
              </div>
              <slot name="avatar">
                <span v-if="(resource.icon && resource.icon.base64image || images.template || images.iso || resourceIcon) && !['router', 'systemvm', 'volume'].includes($route.path.split('/')[1])">
                  <resource-icon :image="getImage(resource.icon && resource.icon.base64image || images.template || images.iso || resourceIcon)" size="4x" style="margin-right: 5px"/>
                </span>
                <span v-else>
                  <os-logo v-if="resource.ostypeid || resource.ostypename" :osId="resource.ostypeid" :osName="resource.ostypename" size="4x" @update-osname="setResourceOsType"/>
                  <render-icon v-else-if="typeof $route.meta.icon ==='string'" style="font-size: 36px" :icon="$route.meta.icon" />
                  <render-icon v-else style="font-size: 36px" :svgIcon="$route.meta.icon" />
                </span>
              </slot>
            </div>
            <slot name="name">
              <div v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(resource.name)">{{ $t(resource.name.toLowerCase()) }}</div>
              <div v-else>
                <h4 class="name">
                  {{ name }}
                </h4>
              </div>
            </slot>
          </div>
          <slot name="actions">
            <div class="tags">
              <a-tag v-if="resource.instancename">
                {{ resource.instancename }}
              </a-tag>
              <a-tag v-if="resource.type">
                <span v-if="['USER.LOGIN', 'USER.LOGOUT', 'ROUTER.HEALTH.CHECKS', 'FIREWALL.CLOSE', 'ALERT.SERVICE.DOMAINROUTER'].includes(resource.type)">{{ $t(resource.type.toLowerCase()) }}</span>
                <span v-else>
                  {{ resource.type }}
                </span>
              </a-tag>
              <a-tag v-if="resource.issourcenat">
                {{ $t('label.issourcenat') }}
              </a-tag>
              <a-tag v-if="resource.broadcasturi">
                {{ resource.broadcasturi }}
              </a-tag>
              <a-tag v-if="resource.hypervisor">
                {{ resource.hypervisor }}
              </a-tag>
              <a-tag v-if="resource.haenable">
                {{ $t('label.haenable') }}
              </a-tag>
              <a-tag v-if="resource.isdynamicallyscalable">
                {{ $t('label.isdynamicallyscalable') }}
              </a-tag>
              <a-tag v-if="resource.scope">
                {{ resource.scope }}
              </a-tag>
              <a-tag v-if="resource.version">
                {{ resource.version }}
              </a-tag>
              <a-tag v-if="resource.internetprotocol && ['IPv6', 'DualStack'].includes(resource.internetprotocol)">
                {{ resource.internetprotocol ? $t('label.ip.v4.v6') : resource.internetprotocol }}
              </a-tag>
              <a-tag v-if="resource.archived" :color="this.$config.theme['@warning-color']">
                {{ $t('label.archived') }}
              </a-tag>
              <a-tooltip placement="right" >
                <template #title>
                  <span>{{ $t('label.view.console') }}</span>
                </template>
                <console
                  style="margin-top: -5px;"
                  :resource="resource"
                  size="default"
                  v-if="resource.id"
                />
              </a-tooltip>
              <a-tooltip placement="right" >
                <template #title>
                  <span>{{ $t('label.copy.consoleurl') }}</span>
                </template>
                <console
                  copyUrlToClipboard
                  style="margin-top: -5px;"
                  :resource="resource"
                  size="default"
                  v-if="resource.id"
                />
              </a-tooltip>
            </div>
          </slot>
        </div>

        <a-divider/>

        <div class="resource-detail-item" v-if="(resource.state || resource.status) && $route.meta.name !== 'zone'">
          <div class="resource-detail-item__label">{{ $t('label.status') }}</div>
          <div class="resource-detail-item__details">
            <status class="status" :text="resource.state || resource.status" displayText/>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.allocationstate">
          <div class="resource-detail-item__label">{{ $t('label.allocationstate') }}</div>
          <div class="resource-detail-item__details">
            <status class="status" :text="resource.allocationstate" displayText/>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.resourcestate">
          <div class="resource-detail-item__label">{{ $t('label.resourcestate') }}</div>
          <div class="resource-detail-item__details">
            <status class="status" :text="resource.resourcestate" displayText/>
          </div>
        </div>

        <div class="resource-detail-item" v-if="resource.id">
          <div class="resource-detail-item__label">{{ $t('label.id') }}</div>
          <div class="resource-detail-item__details">
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copyid')"
              icon="barcode-outlined"
              type="dashed"
              size="small"
              :copyResource="String(resource.id)"
              @onClick="$message.success($t('label.copied.clipboard'))" />
            <span style="margin-left: 10px;">{{ resource.id }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ostypename && resource.ostypeid">
          <div class="resource-detail-item__label">{{ $t('label.ostypename') }}</div>
          <div class="resource-detail-item__details">
            <span v-if="resource.icon && resource.icon.base64image || images.template || images.iso">
              <resource-icon :image="getImage(images.template || images.iso)" size="1x" style="margin-right: 5px"/>
            </span>
            <os-logo v-else :osId="resource.ostypeid" :osName="resource.ostypename" size="lg" style="margin-left: -1px" />
            <span style="margin-left: 8px">{{ resource.ostypename }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="('cpunumber' in resource && 'cpuspeed' in resource) || resource.cputotal">
          <div class="resource-detail-item__label">{{ $t('label.cpu') }}</div>
          <div class="resource-detail-item__details">
            <appstore-outlined />
            <span v-if="'cpunumber' in resource && 'cpuspeed' in resource">{{ resource.cpunumber }} CPU x {{ parseFloat(resource.cpuspeed / 1000.0).toFixed(2) }} Ghz</span>
            <span v-else>{{ resource.cputotal }}</span>
          </div>
          <div>
            <span v-if="resource.cpuused">
              <a-progress
                v-if="resource.cpuused"
                class="progress-bar"
                size="small"
                status="active"
                :percent="parseFloat(resource.cpuused)"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.used')"
              />
            </span>
            <span v-if="resource.cpuallocated">
              <a-progress
                class="progress-bar"
                size="small"
                :percent="parseFloat(resource.cpuallocated)"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.allocated')"
              />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="'memory' in resource">
          <div class="resource-detail-item__label">{{ $t('label.memory') }}</div>
          <div class="resource-detail-item__details">
            <bulb-outlined />{{ resource.memory + ' ' + $t('label.mb.memory') }}
          </div>
          <div>
            <span v-if="resource.memorykbs && resource.memoryintfreekbs">
              <a-progress
                class="progress-bar"
                size="small"
                status="active"
                :percent="Number(parseFloat(100.0 * (resource.memorykbs - resource.memoryintfreekbs) / resource.memorykbs).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.used')"
              />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-else-if="resource.memorytotalgb">
          <div class="resource-detail-item__label">{{ $t('label.memory') }}</div>
          <div class="resource-detail-item__details">
            <bulb-outlined />{{ resource.memorytotalgb + ' ' + $t('label.memory') }}
          </div>
          <div>
            <span v-if="resource.memoryusedgb">
              <a-progress
                class="progress-bar"
                size="small"
                status="active"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.memoryusedgb) / parseFloat(resource.memorytotalgb)).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.used')"
              />
            </span>
            <span v-if="resource.memoryallocatedgb">
              <a-progress
                class="progress-bar"
                size="small"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.memoryallocatedgb) / parseFloat(resource.memorytotalgb)).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.allocated')"
              />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-else-if="resource.memorytotal">
          <div class="resource-detail-item__label">{{ $t('label.memory') }}</div>
          <div class="resource-detail-item__details">

            <div style="display: flex; flex-direction: column; width: 100%;">
              <div>
                <bulb-outlined />{{ resource.memorytotal + ' ' + $t('label.memory') }}
              </div>
              <div>
                <span
                  v-if="resource.memoryused">
                  <a-progress
                    class="progress-bar"
                    size="small"
                    status="active"
                    :percent="parseFloat(resource.memoryused)"
                    :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.used')" />
                </span>
                <span
                  v-if="resource.memoryallocated">
                  <a-progress
                    class="progress-bar"
                    size="small"
                    :percent="parseFloat(resource.memoryallocated)"
                    :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.allocated')" />
                </span>
              </div>
            </div>

          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.volumes || resource.sizegb">
          <div class="resource-detail-item__label">{{ $t('label.disksize') }}</div>
          <div class="resource-detail-item__details">
            <hdd-outlined />
            <span style="width: 100%;" v-if="$route.meta.name === 'vm' && resource.volumes">{{ (resource.volumes.reduce((total, item) => total += item.size, 0) / (1024 * 1024 * 1024.0)).toFixed(2) }} GB Storage</span>
            <span style="width: 100%;" v-else-if="resource.sizegb || resource.size">{{ resource.sizegb || (resource.size/1024.0) }}</span>
          </div>
          <div style="margin-left: 25px; margin-top: 5px" v-if="resource.diskkbsread && resource.diskkbswrite && resource.diskioread && resource.diskiowrite">
            <a-tag style="margin-bottom: 5px;">{{ $t('label.read') + ' ' + toSize(resource.diskkbsread) }}</a-tag>
            <a-tag style="margin-bottom: 5px;">{{ $t('label.write') + ' ' + toSize(resource.diskkbswrite) }}</a-tag><br/>
            <a-tag style="margin-bottom: 5px;">{{ $t('label.read.io') + ' ' + resource.diskioread }}</a-tag>
            <a-tag>{{ $t('label.writeio') + ' ' + resource.diskiowrite }}</a-tag>
          </div>
        </div>
        <div class="resource-detail-item" v-else-if="resource.disksizetotalgb">
          <div class="resource-detail-item__label">{{ $t('label.disksize') }}</div>
          <div class="resource-detail-item__details">
            <database-outlined />{{ resource.disksizetotalgb }}
          </div>
          <div>
            <span v-if="resource.disksizeusedgb">
              <a-progress
                class="progress-bar"
                size="small"
                status="active"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.disksizeusedgb) / parseFloat(resource.disksizetotalgb)).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.disksizeusedgb')" />
            </span>
            <span v-if="resource.disksizeallocatedgb">
              <a-progress
                class="progress-bar"
                size="small"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.disksizeallocatedgb) / (parseFloat(resource.disksizetotalgb) *
                  (parseFloat(resource.overprovisionfactor) || 1.0))).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('label.disksizeallocatedgb')" />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.nic || ('networkkbsread' in resource && 'networkkbswrite' in resource)">
          <div class="resource-detail-item__label">{{ $t('label.network') }}</div>
          <div class="resource-detail-item__details resource-detail-item__details--start">
            <wifi-outlined />
            <div>
              <div v-if="'networkkbsread' in resource && 'networkkbswrite' in resource">
                <a-tag><ArrowDownOutlined />RX {{ toSize(resource.networkkbsread) }}</a-tag>
                <a-tag><ArrowUpOutlined />TX {{ toSize(resource.networkkbswrite) }}</a-tag>
              </div>
              <div v-else>{{ resource.nic.length }} NIC(s)</div>
              <div
                v-for="(eth, index) in resource.nic"
                :key="eth.id"
                style="margin-left: -24px; margin-top: 5px;">
                <api-outlined /><strong>eth{{ index }}</strong> {{ eth.ip6address ? eth.ipaddress + ', ' + eth.ip6address : eth.ipaddress }}
                <router-link v-if="!isStatic && eth.networkname && eth.networkid" :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link>
                <a-tag v-if="eth.isdefault">
                  {{ $t('label.default') }}
                </a-tag >
              </div>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.networks && resource.networks.length > 0">
          <div class="resource-detail-item__label">{{ $t('label.networks') }}</div>
          <div class="resource-detail-item__details resource-detail-item__details--start">
            <div>
              <div
                v-for="network in resource.networks"
                :key="network.id"
                style="margin-top: 5px;">
                <api-outlined />{{ network.name }}
                <span v-if="resource.defaultnetworkid === network.id">
                  ({{ $t('label.default') }})
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.loadbalancer">
          <div class="resource-detail-item__label">{{ $t('label.loadbalancerrule') }}</div>
          <div class="resource-detail-item__details">
            <api-outlined />
            <span>{{ resource.loadbalancer.name }} ( {{ resource.loadbalancer.publicip }}:{{ resource.loadbalancer.publicport }})</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ipaddress">
          <div class="resource-detail-item__label">{{ $t('label.ip') }}</div>
          <div class="resource-detail-item__details">
            <environment-outlined
              @click="$message.success(`${$t('label.copied.clipboard')} : ${ ipaddress }`)"
              v-clipboard:copy="ipaddress" />
            <router-link v-if="!isStatic && resource.ipaddressid" :to="{ path: '/publicip/' + resource.ipaddressid }">{{ ipaddress }}</router-link>
            <span v-else>{{ ipaddress }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.projectid || resource.projectname">
          <div class="resource-detail-item__label">{{ $t('label.project') }}</div>
          <div class="resource-detail-item__details">
            <span v-if="images.project">
              <resource-icon :image="getImage(images.project)" size="1x" style="margin-right: 5px"/>
            </span>
            <project-outlined v-else />
            <router-link v-if="!isStatic && resource.projectid" :to="{ path: '/project/' + resource.projectid }">{{ resource.project || resource.projectname || resource.projectid }}</router-link>
            <router-link v-else :to="{ path: '/project', query: { name: resource.projectname }}">{{ resource.projectname }}</router-link>
          </div>
        </div>

        <div class="resource-detail-item">
          <slot name="details">
          </slot>
        </div>

        <div class="resource-detail-item" v-if="resource.groupid">
          <div class="resource-detail-item__label">{{ $t('label.group') }}</div>
          <div class="resource-detail-item__details">
            <gold-outlined />
            <router-link :to="{ path: '/vmgroup/' + resource.groupid }">{{ resource.group || resource.groupid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.autoscalevmgroupid">
          <div class="resource-detail-item__label">{{ $t('label.autoscalevmgroupname') }}</div>
          <div class="resource-detail-item__details">
            <gold-outlined />
            <router-link :to="{ path: '/autoscalevmgroup/' + resource.autoscalevmgroupid }">{{ resource.autoscalevmgroupname || resource.autoscalevmgroupid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.keypairs && resource.keypairs.length > 0">
          <div class="resource-detail-item__label">{{ $t('label.keypairs') }}</div>
          <div class="resource-detail-item__details">
            <key-outlined />
            <li v-for="keypair in keypairs" :key="keypair">
              <router-link :to="{ path: '/ssh/' + keypair }" style="margin-right: 5px">{{ keypair }}</router-link>
            </li>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.resourcetype && resource.resourceid && routeFromResourceType">
          <div class="resource-detail-item__label">{{ $t('label.resource') }}</div>
          <div class="resource-detail-item__details">
            <resource-label :resourceType="resource.resourcetype" :resourceId="resource.resourceid" :resourceName="resource.resourcename" />
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.virtualmachineid">
          <div class="resource-detail-item__label">{{ $t('label.vmname') }}</div>
          <div class="resource-detail-item__details">
            <desktop-outlined />
            <router-link :to="{ path: createPathBasedOnVmType(resource.vmtype, resource.virtualmachineid) }">{{ resource.vmname || resource.vm || resource.virtualmachinename || resource.virtualmachineid }} </router-link>
            <status class="status status--end" :text="resource.vmstate" v-if="resource.vmstate"/>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.volumeid">
          <div class="resource-detail-item__label">{{ $t('label.volume') }}</div>
          <div class="resource-detail-item__details">
            <hdd-outlined />
            <router-link :to="{ path: '/volume/' + resource.volumeid }">{{ resource.volumename || resource.volume || resource.volumeid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.associatednetworkid">
          <div class="resource-detail-item__label">{{ $t('label.associatednetwork') }}</div>
          <div class="resource-detail-item__details">
            <wifi-outlined />
            <router-link :to="{ path: '/guestnetwork/' + resource.associatednetworkid }">{{ resource.associatednetworkname || resource.associatednetwork || resource.associatednetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.sourceipaddressnetworkid">
          <div class="resource-detail-item__label">{{ $t('label.network') }}</div>
          <div class="resource-detail-item__details">
            <wifi-outlined />
            <router-link :to="{ path: '/guestnetwork/' + resource.sourceipaddressnetworkid }">{{ resource.sourceipaddressnetworkname || resource.sourceipaddressnetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.guestnetworkid">
          <div class="resource-detail-item__label">{{ $t('label.guestnetwork') }}</div>
          <div class="resource-detail-item__details">
            <gateway-outlined />
            <router-link :to="{ path: '/guestnetwork/' + resource.guestnetworkid }">{{ resource.guestnetworkname || resource.guestnetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.publicip">
          <div class="resource-detail-item__label">{{ $t('label.publicip') }}</div>
          <div class="resource-detail-item__details">
            <gateway-outlined />
            <router-link :to="{ path: '/publicip/' + resource.publicipid }">{{ resource.publicip }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcid">
          <div class="resource-detail-item__label">{{ $t('label.vpcname') }}</div>
          <div class="resource-detail-item__details">
            <span v-if="images.vpc">
              <resource-icon :image="getImage(images.vpc)" size="1x" style="margin-right: 5px"/>
            </span>
            <deployment-unit-outlined v-else />
            <router-link :to="{ path: '/vpc/' + resource.vpcid }">{{ resource.vpcname || resource.vpcid }}</router-link>
          </div>
        </div>

        <div class="resource-detail-item" v-if="resource.aclid">
          <div class="resource-detail-item__label">{{ $t('label.aclid') }}</div>
          <div class="resource-detail-item__details">
            <span v-if="images.acl">
              <resource-icon :image="getImage(images.acl)" size="1x" style="margin-right: 5px"/>
            </span>
            <deployment-unit-outlined v-else />
            <router-link :to="{ path: '/acllist/' + resource.aclid }">{{ resource.aclname || resource.aclid }}</router-link>
          </div>
        </div>

        <div class="resource-detail-item" v-if="resource.affinitygroup && resource.affinitygroup.length > 0">
          <div class="resource-detail-item__label">{{ $t('label.affinitygroup') }}</div>
          <SwapOutlined />
          <span
            v-for="(group, index) in resource.affinitygroup"
            :key="group.id"
          >
            <router-link :to="{ path: '/affinitygroup/' + group.id }">{{ group.name }}</router-link>
            <span v-if="index + 1 < resource.affinitygroup.length">, </span>
          </span>
        </div>
        <div class="resource-detail-item" v-if="resource.templateid">
          <div class="resource-detail-item__label">{{ resource.isoid ? $t('label.iso') : $t('label.templatename') }}</div>
          <div class="resource-detail-item__details">
            <resource-icon v-if="resource.icon" :image="getImage(resource.icon.base64image)" size="1x" style="margin-right: 5px"/>
            <PictureOutlined v-else />
            <div v-if="resource.isoid">
              <router-link :to="{ path: '/iso/' + resource.isoid }">{{ resource.isodisplaytext || resource.isoname || resource.isoid }} </router-link>
            </div>
            <div v-else>
              <router-link :to="{ path: '/template/' + resource.templateid }">{{ resource.templatedisplaytext || resource.templatename || resource.templateid }} </router-link>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.serviceofferingname && resource.serviceofferingid">
          <div class="resource-detail-item__label">{{ $t('label.serviceofferingname') }}</div>
          <div class="resource-detail-item__details">
            <cloud-outlined />
            <router-link v-if="!isStatic && $route.meta.name === 'router'" :to="{ path: '/computeoffering/' + resource.serviceofferingid, query: { issystem: true } }">{{ resource.serviceofferingname || resource.serviceofferingid }} </router-link>
            <router-link v-else-if="$router.resolve('/computeoffering/' + resource.serviceofferingid).matched[0].redirect !== '/exception/404'" :to="{ path: '/computeoffering/' + resource.serviceofferingid }">{{ resource.serviceofferingname || resource.serviceofferingid }} </router-link>
            <span v-else>{{ resource.serviceofferingname || resource.serviceofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.diskofferingname && resource.diskofferingid">
          <div class="resource-detail-item__label">{{ $t('label.diskoffering') }}</div>
          <div class="resource-detail-item__details">
            <hdd-outlined />
            <router-link v-if="!isStatic && $router.resolve('/diskoffering/' + resource.diskofferingid).matched[0].redirect !== '/exception/404'" :to="{ path: '/diskoffering/' + resource.diskofferingid }">{{ resource.diskofferingname || resource.diskofferingid }} </router-link>
            <span v-else>{{ resource.diskofferingname || resource.diskofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.backupofferingid">
          <div class="resource-detail-item__label">{{ $t('label.backupofferingid') }}</div>
          <cloud-upload-outlined />
          <router-link v-if="!isStatic && $router.resolve('/backupoffering/' + resource.backupofferingid).matched[0].redirect !== '/exception/404'" :to="{ path: '/backupoffering/' + resource.backupofferingid }">{{ resource.backupofferingname || resource.backupofferingid }} </router-link>
          <span v-else>{{ resource.backupofferingname || resource.backupofferingid }}</span>
        </div>
        <div class="resource-detail-item" v-if="resource.networkofferingid">
          <div class="resource-detail-item__label">{{ $t('label.networkofferingid') }}</div>
          <div class="resource-detail-item__details">
            <wifi-outlined />
            <router-link v-if="!isStatic && $router.resolve('/networkoffering/' + resource.networkofferingid).matched[0].redirect !== '/exception/404'" :to="{ path: '/networkoffering/' + resource.networkofferingid }">{{ resource.networkofferingname || resource.networkofferingid }} </router-link>
            <span v-else>{{ resource.networkofferingname || resource.networkofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcofferingid">
          <div class="resource-detail-item__label">{{ $t('label.vpcoffering') }}</div>
          <div class="resource-detail-item__details">
            <DeploymentUnitOutlined />
            <router-link v-if="!isStatic && $router.resolve('/vpcoffering/' + resource.vpcofferingid).matched[0].redirect !== '/exception/404'" :to="{ path: '/vpcoffering/' + resource.vpcofferingid }">{{ resource.vpcofferingname || resource.vpcofferingid }} </router-link>
            <span v-else>{{ resource.vpcofferingname || resource.vpcofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.storageid">
          <div class="resource-detail-item__label">{{ $t('label.storagepool') }}</div>
          <div class="resource-detail-item__details">
            <database-outlined />
            <router-link v-if="!isStatic && $router.resolve('/storagepool/' + resource.storageid).matched[0].redirect !== '/exception/404'" :to="{ path: '/storagepool/' + resource.storageid }">{{ resource.storage || resource.storageid }} </router-link>
            <span v-else>{{ resource.storage || resource.storageid }}</span>
            <a-tag style="margin-left: 5px;" v-if="resource.storagetype">
              {{ resource.storagetype }}
            </a-tag>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.hostid">
          <div class="resource-detail-item__label">{{ $t('label.hostname') }}</div>
          <div class="resource-detail-item__details">
            <desktop-outlined />
            <router-link v-if="!isStatic && $router.resolve('/host/' + resource.hostid).matched[0].redirect !== '/exception/404'" :to="{ path: '/host/' + resource.hostid }">{{ resource.hostname || resource.hostid }} </router-link>
            <span v-else>{{ resource.hostname || resource.hostid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.clusterid">
          <div class="resource-detail-item__label">{{ $t('label.clusterid') }}</div>
          <div class="resource-detail-item__details">
            <cluster-outlined />
            <router-link v-if="!isStatic && $router.resolve('/cluster/' + resource.clusterid).matched[0].redirect !== '/exception/404'" :to="{ path: '/cluster/' + resource.clusterid }">{{ resource.clustername || resource.cluster || resource.clusterid }}</router-link>
            <span v-else>{{ resource.clustername || resource.cluster || resource.clusterid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.podid">
          <div class="resource-detail-item__label">{{ $t('label.podid') }}</div>
          <div class="resource-detail-item__details">
            <appstore-outlined />
            <router-link v-if="!isStatic && $router.resolve('/pod/' + resource.podid).matched[0].redirect !== '/exception/404'" :to="{ path: '/pod/' + resource.podid }">{{ resource.podname || resource.pod || resource.podid }}</router-link>
            <span v-else>{{ resource.podname || resource.pod || resource.podid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.zoneid">
          <div class="resource-detail-item__label">{{ $t('label.zone') }}</div>
          <div class="resource-detail-item__details">
            <span v-if="images.zone">
              <resource-icon :image="getImage(images.zone)" size="1x" style="margin-right: 5px"/>
            </span>
            <global-outlined v-else />
            <router-link v-if="!isStatic && $router.resolve('/zone/' + resource.zoneid).matched[0].redirect !== '/exception/404'" :to="{ path: '/zone/' + resource.zoneid }">{{ resource.zone || resource.zonename || resource.zoneid }}</router-link>
            <span v-else>{{ resource.zone || resource.zonename || resource.zoneid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.userdataname">
          <div class="resource-detail-item__label">{{ $t('label.userdata') }}</div>
          <div class="resource-detail-item__details">
            <solution-outlined />
            <router-link v-if="!isStatic && $router.resolve('/userdata/' + resource.userdataid).matched[0].redirect !== '/exception/404'" :to="{ path: '/userdata/' + resource.userdataid }">{{ resource.userdataname || resource.userdataid }}</router-link>
            <span v-else>{{ resource.userdataname || resource.userdataid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.owner">
          <div class="resource-detail-item__label">{{ $t('label.owners') }}</div>
          <div class="resource-detail-item__details">
            <user-outlined />
            <template v-for="(item, idx) in resource.owner" :key="idx">
              <span style="margin-right:5px">
                <span v-if="$store.getters.userInfo.roletype !== 'User'">
                  <router-link v-if="!isStatic && 'user' in item" :to="{ path: '/accountuser', query: { username: item.user, domainid: resource.domainid }}">{{ item.account + '(' + item.user + ')' }}</router-link>
                  <router-link v-else :to="{ path: '/account', query: { name: item.account, domainid: resource.domainid } }">{{ item.account }}</router-link>
                </span>
                <span v-else>{{ item.user ? item.account + '(' + item.user + ')' : item.account }}</span>
              </span>
            </template>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.account && !resource.account.startsWith('PrjAcct-')">
          <div class="resource-detail-item__label">{{ $t('label.account') }}</div>
          <div class="resource-detail-item__details">
            <span v-if="images.account">
              <resource-icon :image="getImage(images.account)" size="1x" style="margin-right: 5px"/>
            </span>
            <user-outlined v-else />
            <router-link v-if="!isStatic && $store.getters.userInfo.roletype !== 'User'" :to="{ path: '/account', query: { name: resource.account, domainid: resource.domainid } }">{{ resource.account }}</router-link>
            <span v-else>{{ resource.account }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.roleid">
          <div class="resource-detail-item__label">{{ $t('label.role') }}</div>
          <div class="resource-detail-item__details">
            <idcard-outlined />
            <router-link v-if="!isStatic && $router.resolve('/role/' + resource.roleid).matched[0].redirect !== '/exception/404'" :to="{ path: '/role/' + resource.roleid }">{{ resource.rolename || resource.role || resource.roleid }}</router-link>
            <span v-else>{{ resource.rolename || resource.role || resource.roleid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.domainid">
          <div class="resource-detail-item__label">{{ $t('label.domain') }}</div>
          <div class="resource-detail-item__details">
            <resource-icon v-if="images.domain" :image="getImage(images.domain)" size="1x" style="margin-right: 5px"/>
            <block-outlined v-else />
            <router-link v-if="!isStatic && $store.getters.userInfo.roletype !== 'User'" :to="{ path: '/domain/' + resource.domainid, query: { tab: 'details'}  }">{{ resource.domain || resource.domainid }}</router-link>
            <span v-else>{{ resource.domain || resource.domainid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.managementserverid">
          <div class="resource-detail-item__label">{{ $t('label.management.servers') }}</div>
          <div class="resource-detail-item__details">
            <rocket-outlined />
            <router-link v-if="!isStatic && $router.resolve('/managementserver/' + resource.managementserverid).matched[0].redirect !== '/exception/404'" :to="{ path: '/managementserver/' + resource.managementserverid }">{{ resource.managementserver || resource.managementserverid }}</router-link>
            <span v-else>{{ resource.managementserver || resource.managementserverid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.created">
          <div class="resource-detail-item__label">{{ $t('label.created') }}</div>
          <div class="resource-detail-item__details">
            <calendar-outlined />{{ $toLocaleDate(resource.created) }}
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.lastupdated">
          <div class="resource-detail-item__label">{{ $t('label.last.updated') }}</div>
          <div class="resource-detail-item__details">
            <calendar-outlined />{{ $toLocaleDate(resource.lastupdated) }}
          </div>
        </div>
      </div>

      <div class="account-center-tags" v-if="$route.meta.related">
        <a-divider/>
        <div v-for="item in $route.meta.related" :key="item.path">
          <router-link
            v-if="(item.show === undefined || item.show(resource)) && $router.resolve('/' + item.name).matched[0].redirect !== '/exception/404'"
            :to="{ name: item.name, query: getRouterQuery(item) }">
            <a-button style="margin-right: 10px">
              <template #icon>
                <render-icon :icon="$router.resolve('/' + item.name).meta.icon" />
              </template>
              {{ $t('label.view') + ' ' + $t(item.title) }}
            </a-button>
          </router-link>
        </div>
      </div>

      <div class="account-center-tags" v-if="showKeys">
        <a-divider/>
        <div class="user-keys">
          <key-outlined />
          <strong>
            {{ $t('label.apikey') }}
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy') + ' ' + $t('label.apikey')"
              icon="CopyOutlined"
              type="dashed"
              size="small"
              @onClick="$message.success($t('label.copied.clipboard'))"
              :copyResource="resource.apikey" />
          </strong>
          <div>
            {{ resource.apikey.substring(0, 20) }}...
          </div>
        </div> <br/>
        <div class="user-keys">
          <lock-outlined />
          <strong>
            {{ $t('label.secretkey') }}
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy') + ' ' + $t('label.secretkey')"
              icon="CopyOutlined"
              type="dashed"
              size="small"
              @onClick="$message.success($t('label.copied.clipboard'))"
              :copyResource="resource.secretkey" />
          </strong>
          <div>
            {{ resource.secretkey.substring(0, 20) }}...
          </div>
        </div>
      </div>

      <div class="account-center-tags" v-if="!isStatic && resourceType && tagsSupportingResourceTypes.includes(this.resourceType) && 'listTags' in $store.getters.apis">
        <a-divider/>
        <a-spin :spinning="loadingTags">
          <div class="title">{{ $t('label.tags') }}</div>
          <div>
            <template v-for="(tag, index) in tags" :key="index">
              <a-tag :closable="isAdminOrOwner() && 'deleteTags' in $store.getters.apis" @close="() => handleDeleteTag(tag)">
                {{ tag.key }} = {{ tag.value }}
              </a-tag>
            </template>

            <div v-if="inputVisible">
              <a-input-group
                type="text"
                size="small"
                @blur="handleInputConfirm"
                @keyup.enter="handleInputConfirm"
                compact>
                <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 30%; text-align: center" :placeholder="$t('label.key')" />
                <a-input
                  class="tag-disabled-input"
                  style="width: 30px; border-left: 0; pointer-events: none; text-align: center"
                  placeholder="="
                  disabled />
                <a-input :value="inputValue" @change="handleValueChange" style="width: 30%; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
                <tooltip-button :tooltip="$t('label.ok')" icon="CheckOutlined" size="small" @onClick="handleInputConfirm" />
                <tooltip-button :tooltip="$t('label.cancel')" icon="CloseOutlined" size="small" @onClick="inputVisible=false" />
              </a-input-group>
            </div>
            <a-tag
              @click="showInput"
              class="btn-add-tag"
              style="borderStyle: dashed;"
              v-else-if="isAdminOrOwner() && 'createTags' in $store.getters.apis">
              <plus-outlined  /> {{ $t('label.new.tag') }}
            </a-tag>
          </div>
        </a-spin>
      </div>
    </a-card>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import { createPathBasedOnVmType } from '@/utils/plugins'
import Console from '@/components/widgets/Console'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import UploadResourceIcon from '@/components/view/UploadResourceIcon'
import eventBus from '@/config/eventBus'
import ResourceIcon from '@/components/view/ResourceIcon'
import ResourceLabel from '@/components/widgets/ResourceLabel'

export default {
  name: 'InfoCard',
  components: {
    Console,
    OsLogo,
    Status,
    TooltipButton,
    UploadResourceIcon,
    ResourceIcon,
    ResourceLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    title: {
      type: String,
      default: ''
    },
    bordered: {
      type: Boolean,
      default: true
    },
    isStatic: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      ipaddress: '',
      resourceType: '',
      inputVisible: false,
      inputKey: '',
      inputValue: '',
      tags: [],
      showKeys: false,
      loadingTags: false,
      showUpload: false,
      images: {
        zone: '',
        template: '',
        iso: '',
        domain: '',
        account: '',
        project: '',
        vpc: '',
        network: ''
      },
      newResource: {}
    }
  },
  watch: {
    '$route.fullPath': function (path) {
      if (path === '/user/login') {
        return
      }
      this.getIcons()
    },
    resource: {
      deep: true,
      handler (newData, oldData) {
        if (newData === oldData) return
        this.newResource = newData
        this.showKeys = false
        this.setData()

        if ('apikey' in this.resource) {
          this.getUserKeys()
        }
        this.updateResourceAdditionalData()
      }
    },
    async templateIcon () {
      this.getIcons()
    }
  },
  created () {
    this.setData()
    eventBus.on('handle-close', (showModal) => {
      this.showUploadModal(showModal)
    })
    this.updateResourceAdditionalData()
  },
  computed: {
    tagsSupportingResourceTypes () {
      return ['UserVm', 'Template', 'ISO', 'Volume', 'Snapshot', 'Backup', 'Network',
        'LoadBalancer', 'PortForwardingRule', 'FirewallRule', 'SecurityGroup', 'SecurityGroupRule',
        'PublicIpAddress', 'Project', 'Account', 'Vpc', 'NetworkACL', 'StaticRoute', 'VMSnapshot',
        'RemoteAccessVpn', 'User', 'SnapshotPolicy', 'VpcOffering']
    },
    name () {
      return this.resource.displayname || this.resource.name || this.resource.displaytext || this.resource.username ||
        this.resource.ipaddress || this.resource.virtualmachinename || this.resource.templatetype
    },
    keypairs () {
      if (!this.resource.keypairs) {
        return null
      }
      if (typeof this.resource.keypairs === 'string' || this.resource.keypairs instanceof String) {
        return this.resource.keypairs.split(',')
      }
      return [this.resource.keypairs.toString()]
    },
    templateIcon () {
      return this.resource.templateid
    },
    resourceIcon () {
      if (this.$showIcon()) {
        if (this.resource?.icon?.base64image) {
          return this.resource.icon.base64image
        }
        if (this.resource?.resourceIcon?.base64image) {
          return this.resource.resourceIcon.base64image
        }
      }
      return null
    },
    routeFromResourceType () {
      return this.$getRouteFromResourceType(this.resource.resourcetype)
    }
  },
  methods: {
    createPathBasedOnVmType: createPathBasedOnVmType,
    updateResourceAdditionalData () {
      if (!this.resource) return
      this.resourceType = this.$route.meta.resourceType
      if (this.tagsSupportingResourceTypes.includes(this.resourceType)) {
        if ('tags' in this.resource) {
          this.tags = this.resource.tags
        } else if (this.resourceType) {
          this.getTags()
        }
      }
      this.getIcons()
    },
    showUploadModal (show) {
      if (show) {
        if (this.$showIcon()) {
          this.showUpload = true
        }
      } else {
        this.showUpload = false
      }
    },
    getImage (image) {
      return (image || this.resource?.icon?.base64image)
    },
    async getIcons () {
      this.images = {
        zone: '',
        template: '',
        iso: '',
        domain: '',
        account: '',
        project: '',
        vpc: '',
        network: ''
      }
      if (this.resource.templateid) {
        await this.fetchResourceIcon(this.resource.templateid, 'template')
      }
      if (this.resource.isoid) {
        await this.fetchResourceIcon(this.resource.isoid, 'iso')
      }
      if (this.resource.zoneid) {
        await this.fetchResourceIcon(this.resource.zoneid, 'zone')
      }
      if (this.resource.domainid) {
        await this.fetchResourceIcon(this.resource.domainid, 'domain')
      }
      if (this.resource.account) {
        await this.fetchAccount()
      }
      if (this.resource.projectid) {
        await this.fetchResourceIcon(this.resource.projectid, 'project')
      }
      if (this.resource.vpcid) {
        await this.fetchResourceIcon(this.resource.vpcid, 'vpc')
      }
      if (this.resource.networkid) {
        await this.fetchResourceIcon(this.resource.networkid, 'network')
      }
    },
    fetchAccount () {
      return new Promise((resolve, reject) => {
        api('listAccounts', {
          name: this.resource.account,
          domainid: this.resource.domainid,
          showicon: true
        }).then(async json => {
          const response = json?.listaccountsresponse?.account || []
          if (response?.[0]?.icon) {
            this.images.account = response[0].icon.base64image
          }
        })
      })
    },
    fetchResourceIcon (resourceid, type) {
      if (resourceid) {
        return new Promise((resolve, reject) => {
          api('listResourceIcon', {
            resourceids: resourceid,
            resourcetype: type
          }).then(json => {
            const response = json.listresourceiconresponse.icon || []
            if (response?.[0]) {
              this.images[type] = response[0].base64image
              resolve(this.images)
            } else {
              this.images[type] = ''
              resolve(this.images)
            }
          }).catch(error => {
            reject(error)
          })
        })
      } else {
        this.images.type = ''
      }
    },
    setData () {
      if (this.resource.nic && this.resource.nic.length > 0) {
        this.ipaddress = this.resource.nic.filter(e => { return e.ipaddress }).map(e => { return e.ipaddress }).join(', ')
      } else {
        this.ipaddress = this.resource.ipaddress
      }
    },
    toSize (kb) {
      if (!kb) {
        return '0 KB'
      }
      if (kb < 1024) {
        return kb + ' KB'
      }
      if (kb < 1024 * 1024) {
        return parseFloat(kb / 1024.0).toFixed(2) + ' MB'
      }
      return parseFloat(kb / (1024.0 * 1024.0)).toFixed(2) + ' GB'
    },
    getUserKeys () {
      if (!('getUserKeys' in this.$store.getters.apis)) {
        return
      }
      api('getUserKeys', { id: this.resource.id }).then(json => {
        this.showKeys = true
        this.newResource.secretkey = json.getuserkeysresponse.userkeys.secretkey
        this.$emit('change-resource', this.newResource)
      })
    },
    getTags () {
      if (!('listTags' in this.$store.getters.apis) || !this.resource || !this.resource.id) {
        return
      }
      this.loadingTags = true
      this.tags = []
      const params = {
        listall: true,
        resourceid: this.resource.id,
        resourcetype: this.resourceType
      }
      if (this.$route.meta.name === 'project') {
        params.projectid = this.resource.id
      }
      api('listTags', params).then(json => {
        if (json.listtagsresponse && json.listtagsresponse.tag) {
          this.tags = json.listtagsresponse.tag
        }
      }).finally(() => {
        this.loadingTags = false
      })
    },
    isAdminOrOwner () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype) ||
        (this.resource.domainid === this.$store.getters.userInfo.domainid && this.resource.account === this.$store.getters.userInfo.account) ||
        (this.resource.project && this.resource.projectid === this.$store.getters.project.id)
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    handleKeyChange (e) {
      this.inputKey = e.target.value
    },
    handleValueChange (e) {
      this.inputValue = e.target.value
    },
    handleInputConfirm () {
      const args = {}
      this.loadingTags = true
      args.resourceids = this.resource.id
      args.resourcetype = this.resourceType
      args['tags[0].key'] = this.inputKey
      args['tags[0].value'] = this.inputValue
      api('createTags', args).then(json => {
      }).finally(e => {
        this.getTags()
      })

      this.inputVisible = false
      this.inputKey = ''
      this.inputValue = ''
    },
    handleDeleteTag (tag) {
      const args = {}
      this.loadingTags = true
      args.resourceids = this.resource.id
      args.resourcetype = this.resourceType
      args['tags[0].key'] = tag.key
      args['tags[0].value'] = tag.value
      api('deleteTags', args).then(json => {
      }).finally(e => {
        this.getTags()
      })
    },
    setResourceOsType (name) {
      this.newResource.ostypename = name
      this.$emit('change-resource', this.newResource)
    },
    getRouterQuery (item) {
      const query = {}
      if (item.value) {
        query[item.param] = this.resource[item.value]
      } else {
        if (item.param === 'account') {
          query[item.param] = this.resource.name
          query.domainid = this.resource.domainid
        } else if (item.param === 'keypair') {
          query[item.param] = this.resource.name
        } else {
          query[item.param] = this.resource.id
        }
      }

      return query
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.ant-card-body) {
  padding: 30px;
}

.resource-details {
  text-align: center;
  margin-bottom: 20px;

  &__name {
    display: flex;
    align-items: center;

    .avatar {
      margin-right: 20px;
      overflow: hidden;
      min-width: 50px;
      cursor: pointer;

      img {
        height: 100%;
        width: 100%;
      }
    }

    .name {
      margin-bottom: 0;
      font-size: 18px;
      line-height: 1;
      word-break: break-all;
      text-align: left;
    }

  }
}
.resource-detail-item {
  margin-bottom: 20px;
  word-break: break-all;

  &__details {
    display: flex;
    align-items: center;

    &--start {
      align-items: flex-start;

      i {
        margin-top: 4px;
      }

    }

  }

  .anticon {
    margin-right: 10px;
  }

  &__label {
    margin-bottom: 5px;
    font-weight: bold;
  }

}
.user-keys {
  word-wrap: break-word;
}
.account-center-tags {
  .ant-tag {
    margin-bottom: 8px;
  }

  a {
    display: block;
    margin-bottom: 10px;
  }

}
.title {
  margin-bottom: 5px;
  font-weight: bold;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  margin-top: 20px;
  margin-bottom: -10px;

  .ant-tag {
    margin-right: 10px;
    margin-bottom: 10px;
    height: auto;
  }

}

.progress-bar {
  padding-right: 60px;
  width: 100%;
}

.upload-icon {
  position: absolute;
  top: 70px;
  opacity: 0.75;
  left: 70px;
  font-size: 0.75em;
  padding: 0.25rem;
  background: rgba(247, 245, 245, 0.767);
  border-radius: 50%;
  border: 1px solid rgba(177, 177, 177, 0.788);
}
</style>
