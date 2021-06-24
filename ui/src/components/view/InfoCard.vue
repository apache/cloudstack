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

<template>
  <a-spin :spinning="loading">
    <a-card class="spin-content" :bordered="bordered" :title="title">
      <div>
        <div class="resource-details">
          <div class="resource-details__name">
            <div
              class="avatar"
              @click="$message.success(`${$t('label.copied.clipboard')} : ${name}`)"
              v-clipboard:copy="name" >
              <slot name="avatar">
                <os-logo v-if="resource.ostypeid || resource.ostypename" :osId="resource.ostypeid" :osName="resource.ostypename" size="4x" @update-osname="(name) => this.resource.ostypename = name"/>
                <a-icon v-else-if="typeof $route.meta.icon ==='string'" style="font-size: 36px" :type="$route.meta.icon" />
                <a-icon v-else style="font-size: 36px" :component="$route.meta.icon" />
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
              <a-tooltip placement="right" >
                <template slot="title">
                  <span>{{ $t('label.view.console') }}</span>
                </template>
                <console style="margin-top: -5px;" :resource="resource" size="default" v-if="resource.id" />
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
              style="margin-left: -5px"
              icon="barcode"
              type="dashed"
              size="small"
              @click="$message.success($t('label.copied.clipboard'))"
              v-clipboard:copy="resource.id" />
            <span style="margin-left: 10px;">{{ resource.id }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ostypename && resource.ostypeid">
          <div class="resource-detail-item__label">{{ $t('label.ostypename') }}</div>
          <div class="resource-detail-item__details">
            <os-logo :osId="resource.ostypeid" :osName="resource.ostypename" size="lg" style="margin-left: -1px" />
            <span style="margin-left: 8px">{{ resource.ostypename }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="('cpunumber' in resource && 'cpuspeed' in resource) || resource.cputotal">
          <div class="resource-detail-item__label">{{ $t('label.cpu') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="appstore" />
            <span v-if="resource.cputotal">{{ resource.cputotal }}</span>
            <span v-else>{{ resource.cpunumber }} CPU x {{ parseFloat(resource.cpuspeed / 1000.0).toFixed(2) }} Ghz</span>
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
            <a-icon type="bulb" />{{ resource.memory + ' ' + $t('label.mb.memory') }}
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
            <a-icon type="bulb" />{{ resource.memorytotalgb + ' ' + $t('label.memory') }}
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
                <a-icon type="bulb" />{{ resource.memorytotal + ' ' + $t('label.memory') }}
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
            <a-icon type="hdd" />
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
            <a-icon type="database" />{{ resource.disksizetotalgb }}
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
            <a-icon type="wifi" />
            <div>
              <div v-if="'networkkbsread' in resource && 'networkkbswrite' in resource">
                <a-tag><a-icon type="arrow-down" />RX {{ toSize(resource.networkkbsread) }}</a-tag>
                <a-tag><a-icon type="arrow-up" />TX {{ toSize(resource.networkkbswrite) }}</a-tag>
              </div>
              <div v-else>{{ resource.nic.length }} NIC(s)</div>
              <div
                v-if="resource.nic"
                v-for="(eth, index) in resource.nic"
                :key="eth.id"
                style="margin-left: -24px; margin-top: 5px;">
                <a-icon type="api" />eth{{ index }} {{ eth.ipaddress }}
                <router-link v-if="eth.networkname && eth.networkid" :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link>
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
                <a-icon type="api" />{{ network.name }}
                <span v-if="resource.defaultnetworkid === network.id">
                  ({{ $t('label.default') }})
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ipaddress">
          <div class="resource-detail-item__label">{{ $t('label.ip') }}</div>
          <div class="resource-detail-item__details">
            <a-icon
              type="environment"
              @click="$message.success(`${$t('label.copied.clipboard')} : ${ ipaddress }`)"
              v-clipboard:copy="ipaddress" />
            <router-link v-if="resource.ipaddressid" :to="{ path: '/publicip/' + resource.ipaddressid }">{{ ipaddress }}</router-link>
            <span v-else>{{ ipaddress }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="ipV6Address && ipV6Address !== null">
          <div class="resource-detail-item__label">{{ $t('label.ip6address') }}</div>
          <div class="resource-detail-item__details">
            <a-icon
              type="environment"
              @click="$message.success(`${$t('label.copied.clipboard')} : ${ ipV6Address }`)"
              v-clipboard:copy="ipV6Address" />
            {{ ipV6Address }}
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.projectid || resource.projectname">
          <div class="resource-detail-item__label">{{ $t('label.project') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="project" />
            <router-link v-if="resource.projectid" :to="{ path: '/project/' + resource.projectid }">{{ resource.project || resource.projectname || resource.projectid }}</router-link>
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
            <a-icon type="gold" />
            <router-link :to="{ path: '/vmgroup/' + resource.groupid }">{{ resource.group || resource.groupid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.keypair">
          <div class="resource-detail-item__label">{{ $t('label.keypair') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="key" />
            <router-link :to="{ path: '/ssh/' + resource.keypair }">{{ resource.keypair }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.virtualmachineid">
          <div class="resource-detail-item__label">{{ $t('label.vmname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="desktop" />
            <router-link :to="{ path: '/vm/' + resource.virtualmachineid }">{{ resource.vmname || resource.vm || resource.virtualmachinename || resource.virtualmachineid }} </router-link>
            <status class="status status--end" :text="resource.vmstate" v-if="resource.vmstate"/>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.volumeid">
          <div class="resource-detail-item__label">{{ $t('label.volume') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="hdd" />
            <router-link :to="{ path: '/volume/' + resource.volumeid }">{{ resource.volumename || resource.volume || resource.volumeid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.associatednetworkid">
          <div class="resource-detail-item__label">{{ $t('label.associatednetwork') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="wifi" />
            <router-link :to="{ path: '/guestnetwork/' + resource.associatednetworkid }">{{ resource.associatednetworkname || resource.associatednetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.sourceipaddressnetworkid">
          <div class="resource-detail-item__label">{{ $t('label.network') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="wifi" />
            <router-link :to="{ path: '/guestnetwork/' + resource.sourceipaddressnetworkid }">{{ resource.sourceipaddressnetworkname || resource.sourceipaddressnetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.guestnetworkid">
          <div class="resource-detail-item__label">{{ $t('label.guestnetwork') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="gateway" />
            <router-link :to="{ path: '/guestnetwork/' + resource.guestnetworkid }">{{ resource.guestnetworkname || resource.guestnetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcid">
          <div class="resource-detail-item__label">{{ $t('label.vpcname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="deployment-unit" />
            <router-link :to="{ path: '/vpc/' + resource.vpcid }">{{ resource.vpcname || resource.vpcid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.affinitygroup && resource.affinitygroup.length > 0">
          <div class="resource-detail-item__label">{{ $t('label.affinitygroup') }}</div>
          <a-icon type="swap" />
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
            <a-icon type="picture" />
            <div v-if="resource.isoid">
              <router-link :to="{ path: '/iso/' + resource.isoid }">{{ resource.isoname || resource.isoid }} </router-link>
            </div>
            <div v-else>
              <router-link :to="{ path: '/template/' + resource.templateid }">{{ resource.templatename || resource.templateid }} </router-link>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.serviceofferingname && resource.serviceofferingid">
          <div class="resource-detail-item__label">{{ $t('label.serviceofferingname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="cloud" />
            <router-link v-if="$route.meta.name === 'router'" :to="{ path: '/computeoffering/' + resource.serviceofferingid, query: { issystem: true } }">{{ resource.serviceofferingname || resource.serviceofferingid }} </router-link>
            <router-link v-else-if="$router.resolve('/computeoffering/' + resource.serviceofferingid).route.name !== '404'" :to="{ path: '/computeoffering/' + resource.serviceofferingid }">{{ resource.serviceofferingname || resource.serviceofferingid }} </router-link>
            <span v-else>{{ resource.serviceofferingname || resource.serviceofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.diskofferingname && resource.diskofferingid">
          <div class="resource-detail-item__label">{{ $t('label.diskoffering') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="hdd" />
            <router-link v-if="$router.resolve('/diskoffering/' + resource.diskofferingid).route.name !== '404'" :to="{ path: '/diskoffering/' + resource.diskofferingid }">{{ resource.diskofferingname || resource.diskofferingid }} </router-link>
            <span v-else>{{ resource.diskofferingname || resource.diskofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.backupofferingid">
          <div class="resource-detail-item__label">{{ $t('label.backupofferingid') }}</div>
          <a-icon type="cloud-upload" />
          <router-link v-if="$router.resolve('/backupoffering/' + resource.backupofferingid).route.name !== '404'" :to="{ path: '/backupoffering/' + resource.backupofferingid }">{{ resource.backupofferingname || resource.backupofferingid }} </router-link>
          <span v-else>{{ resource.backupofferingname || resource.backupofferingid }}</span>
        </div>
        <div class="resource-detail-item" v-if="resource.networkofferingid">
          <div class="resource-detail-item__label">{{ $t('label.networkofferingid') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="wifi" />
            <router-link v-if="$router.resolve('/networkoffering/' + resource.networkofferingid).route.name !== '404'" :to="{ path: '/networkoffering/' + resource.networkofferingid }">{{ resource.networkofferingname || resource.networkofferingid }} </router-link>
            <span v-else>{{ resource.networkofferingname || resource.networkofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcofferingid">
          <div class="resource-detail-item__label">{{ $t('label.vpcoffering') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="deployment-unit" />
            <router-link v-if="$router.resolve('/vpcoffering/' + resource.vpcofferingid).route.name !== '404'" :to="{ path: '/vpcoffering/' + resource.vpcofferingid }">{{ resource.vpcofferingname || resource.vpcofferingid }} </router-link>
            <span v-else>{{ resource.vpcofferingname || resource.vpcofferingid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.storageid">
          <div class="resource-detail-item__label">{{ $t('label.storagepool') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="database" />
            <router-link v-if="$router.resolve('/storagepool/' + resource.storageid).route.name !== '404'" :to="{ path: '/storagepool/' + resource.storageid }">{{ resource.storage || resource.storageid }} </router-link>
            <span v-else>{{ resource.storage || resource.storageid }}</span>
            <a-tag style="margin-left: 5px;" v-if="resource.storagetype">
              {{ resource.storagetype }}
            </a-tag>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.hostid">
          <div class="resource-detail-item__label">{{ $t('label.hostname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="desktop" />
            <router-link v-if="$router.resolve('/host/' + resource.hostid).route.name !== '404'" :to="{ path: '/host/' + resource.hostid }">{{ resource.hostname || resource.hostid }} </router-link>
            <span v-else>{{ resource.hostname || resource.hostid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.clusterid">
          <div class="resource-detail-item__label">{{ $t('label.clusterid') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="cluster" />
            <router-link v-if="$router.resolve('/cluster/' + resource.clusterid).route.name !== '404'" :to="{ path: '/cluster/' + resource.clusterid }">{{ resource.clustername || resource.cluster || resource.clusterid }}</router-link>
            <span v-else>{{ resource.clustername || resource.cluster || resource.clusterid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.podid">
          <div class="resource-detail-item__label">{{ $t('label.podid') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="appstore" />
            <router-link v-if="$router.resolve('/pod/' + resource.podid).route.name !== '404'" :to="{ path: '/pod/' + resource.podid }">{{ resource.podname || resource.pod || resource.podid }}</router-link>
            <span v-else>{{ resource.podname || resource.pod || resource.podid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.zoneid">
          <div class="resource-detail-item__label">{{ $t('label.zone') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="global" />
            <router-link v-if="$router.resolve('/zone/' + resource.zoneid).route.name !== '404'" :to="{ path: '/zone/' + resource.zoneid }">{{ resource.zone || resource.zonename || resource.zoneid }}</router-link>
            <span v-else>{{ resource.zone || resource.zonename || resource.zoneid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.owner">
          <div class="resource-detail-item__label">{{ $t('label.owners') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="user" />
            <template v-for="(item,idx) in resource.owner">
              <span style="margin-right:5px" :key="idx">
                <span v-if="$store.getters.userInfo.roletype !== 'User'">
                  <router-link v-if="'user' in item" :to="{ path: '/accountuser', query: { username: item.user, domainid: resource.domainid }}">{{ item.account + '(' + item.user + ')' }}</router-link>
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
            <a-icon type="user" />
            <router-link v-if="$store.getters.userInfo.roletype !== 'User'" :to="{ path: '/account', query: { name: resource.account, domainid: resource.domainid } }">{{ resource.account }}</router-link>
            <span v-else>{{ resource.account }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.roleid">
          <div class="resource-detail-item__label">{{ $t('label.role') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="idcard" />
            <router-link v-if="$router.resolve('/role/' + resource.roleid).route.name !== '404'" :to="{ path: '/role/' + resource.roleid }">{{ resource.rolename || resource.role || resource.roleid }}</router-link>
            <span v-else>{{ resource.rolename || resource.role || resource.roleid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.domainid">
          <div class="resource-detail-item__label">{{ $t('label.domain') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="block" />
            <router-link v-if="$store.getters.userInfo.roletype !== 'User'" :to="{ path: '/domain/' + resource.domainid }">{{ resource.domain || resource.domainid }}</router-link>
            <span v-else>{{ resource.domain || resource.domainid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.managementserverid">
          <div class="resource-detail-item__label">{{ $t('label.management.servers') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="rocket" />
            <router-link v-if="$router.resolve('/managementserver/' + resource.managementserverid).route.name !== '404'" :to="{ path: '/managementserver/' + resource.managementserverid }">{{ resource.managementserver || resource.managementserverid }}</router-link>
            <span v-else>{{ resource.managementserver || resource.managementserverid }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.created">
          <div class="resource-detail-item__label">{{ $t('label.created') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="calendar" />{{ $toLocaleDate(resource.created) }}
          </div>
        </div>
      </div>

      <div class="account-center-tags" v-if="$route.meta.related">
        <a-divider/>
        <div v-for="item in $route.meta.related" :key="item.path">
          <router-link
            v-if="$router.resolve('/' + item.name).route.name !== '404'"
            :to="{ path: '/' + item.name + '?' + item.param + '=' + (item.value ? resource[item.value] : item.param === 'account' ? resource.name + '&domainid=' + resource.domainid : resource.id) }">
            <a-button style="margin-right: 10px" :icon="$router.resolve('/' + item.name).route.meta.icon" >
              {{ $t('label.view') + ' ' + $t(item.title) }}
            </a-button>
          </router-link>
        </div>
      </div>

      <div class="account-center-tags" v-if="showKeys">
        <a-divider/>
        <div class="user-keys">
          <a-icon type="key" />
          <strong>
            {{ $t('label.apikey') }}
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy') + ' ' + $t('label.apikey')"
              icon="copy"
              type="dashed"
              size="small"
              @click="$message.success($t('label.copied.clipboard'))"
              v-clipboard:copy="resource.apikey" />
          </strong>
          <div>
            {{ resource.apikey.substring(0, 20) }}...
          </div>
        </div> <br/>
        <div class="user-keys">
          <a-icon type="lock" />
          <strong>
            {{ $t('label.secretkey') }}
            <tooltip-button
              tooltipPlacement="right"
              :tooltip="$t('label.copy') + ' ' + $t('label.secretkey')"
              icon="copy"
              type="dashed"
              size="small"
              @click="$message.success($t('label.copied.clipboard'))"
              v-clipboard:copy="resource.secretkey" />
          </strong>
          <div>
            {{ resource.secretkey.substring(0, 20) }}...
          </div>
        </div>
      </div>

      <div class="account-center-tags" v-if="resourceType && 'listTags' in $store.getters.apis">
        <a-divider/>
        <a-spin :spinning="loadingTags">
          <div class="title">{{ $t('label.tags') }}</div>
          <div>
            <template v-for="(tag, index) in tags">
              <a-tag :key="index" :closable="isAdminOrOwner() && 'deleteTags' in $store.getters.apis" :afterClose="() => handleDeleteTag(tag)">
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
                <a-input style=" width: 30px; border-left: 0; pointer-events: none; backgroundColor: #fff" placeholder="=" disabled />
                <a-input :value="inputValue" @change="handleValueChange" style="width: 30%; text-align: center; border-left: 0" :placeholder="$t('label.value')" />
                <tooltip-button :tooltip="$t('label.ok')" icon="check" size="small" @click="handleInputConfirm" />
                <tooltip-button :tooltip="$t('label.cancel')" icon="close" size="small" @click="inputVisible=false" />
              </a-input-group>
            </div>
            <a-tag @click="showInput" style="background: #fff; borderStyle: dashed;" v-else-if="isAdminOrOwner() && 'createTags' in $store.getters.apis">
              <a-icon type="plus" /> {{ $t('label.new.tag') }}
            </a-tag>
          </div>
        </a-spin>
      </div>

      <div class="account-center-team" v-if="annotationType && 'listAnnotations' in $store.getters.apis">
        <a-divider :dashed="true"/>
        <a-spin :spinning="loadingAnnotations">
          <div class="title">
            {{ $t('label.comments') }} ({{ notes.length }})
          </div>
          <a-list
            v-if="notes.length"
            :dataSource="notes"
            itemLayout="horizontal"
            size="small" >
            <a-list-item slot="renderItem" slot-scope="item">
              <a-comment
                :content="item.annotation"
                :datetime="$toLocaleDate(item.created)" >
                <a-button
                  v-if="'removeAnnotation' in $store.getters.apis"
                  slot="avatar"
                  type="danger"
                  shape="circle"
                  size="small"
                  @click="deleteNote(item)">
                  <a-icon type="delete"/>
                </a-button>
              </a-comment>
            </a-list-item>
          </a-list>

          <a-comment v-if="'addAnnotation' in $store.getters.apis">
            <a-avatar
              slot="avatar"
              icon="edit"
              @click="showNotesInput = true" />
            <div slot="content">
              <a-textarea
                rows="4"
                @change="handleNoteChange"
                :value="annotation"
                :placeholder="$t('label.add.note')" />
              <a-button
                style="margin-top: 10px"
                @click="saveNote"
                type="primary"
              >
                {{ $t('label.save') }}
              </a-button>
            </div>
          </a-comment>
        </a-spin>
      </div>
    </a-card>
  </a-spin>
</template>

<script>

import { api } from '@/api'
import Console from '@/components/widgets/Console'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'InfoCard',
  components: {
    Console,
    OsLogo,
    Status,
    TooltipButton
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
    }
  },
  data () {
    return {
      ipaddress: '',
      resourceType: '',
      annotationType: '',
      inputVisible: false,
      inputKey: '',
      inputValue: '',
      tags: [],
      notes: [],
      annotation: '',
      showKeys: false,
      showNotesInput: false,
      loadingTags: false,
      loadingAnnotations: false
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
      this.resourceType = this.$route.meta.resourceType
      this.annotationType = ''
      this.showKeys = false
      this.setData()

      switch (this.resourceType) {
        case 'UserVm':
          this.annotationType = 'VM'
          break
        case 'Domain':
          this.annotationType = 'DOMAIN'
          // Domain resource type is not supported for tags
          this.resourceType = ''
          break
        case 'Host':
          this.annotationType = 'HOST'
          // Host resource type is not supported for tags
          this.resourceType = ''
          break
      }

      if ('tags' in this.resource) {
        this.tags = this.resource.tags
      } else if (this.resourceType) {
        this.getTags()
      }
      if (this.annotationType) {
        this.getNotes()
      }
      if ('apikey' in this.resource) {
        this.getUserKeys()
      }
    }
  },
  created () {
    this.setData()
  },
  computed: {
    name () {
      return this.resource.displayname || this.resource.displaytext || this.resource.name || this.resource.username ||
        this.resource.ipaddress || this.resource.virtualmachinename || this.resource.templatetype
    },
    ipV6Address () {
      if (this.resource.nic && this.resource.nic.length > 0) {
        return this.resource.nic.filter(e => { return e.ip6address }).map(e => { return e.ip6address }).join(', ')
      }

      return null
    }
  },
  methods: {
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
        this.resource.secretkey = json.getuserkeysresponse.userkeys.secretkey
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
    getNotes () {
      if (!('listAnnotations' in this.$store.getters.apis)) {
        return
      }
      this.loadingAnnotations = true
      this.notes = []
      api('listAnnotations', { entityid: this.resource.id, entitytype: this.annotationType }).then(json => {
        if (json.listannotationsresponse && json.listannotationsresponse.annotation) {
          this.notes = json.listannotationsresponse.annotation
        }
      }).finally(() => {
        this.loadingAnnotations = false
      })
    },
    isAdminOrOwner () {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype) ||
        (this.resource.domainid === this.$store.getters.userInfo.domainid && this.resource.account === this.$store.getters.userInfo.account) ||
        this.resource.project && this.resource.projectid === this.$store.getters.project.id
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
    handleNoteChange (e) {
      this.annotation = e.target.value
    },
    saveNote () {
      if (this.annotation.length < 1) {
        return
      }
      this.loadingAnnotations = true
      this.showNotesInput = false
      const args = {}
      args.entityid = this.resource.id
      args.entitytype = this.annotationType
      args.annotation = this.annotation
      api('addAnnotation', args).then(json => {
      }).finally(e => {
        this.getNotes()
      })
      this.annotation = ''
    },
    deleteNote (annotation) {
      this.loadingAnnotations = true
      const args = {}
      args.id = annotation.id
      api('removeAnnotation', args).then(json => {
      }).finally(e => {
        this.getNotes()
      })
    }
  }
}
</script>

<style lang="scss" scoped>

/deep/ .ant-card-body {
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
.account-center-team {
  .members {
    a {
      display: block;
      margin: 12px 0;
      line-height: 24px;
      height: 24px;
      .member {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.65);
        line-height: 24px;
        max-width: 100px;
        vertical-align: top;
        margin-left: 12px;
        transition: all 0.3s;
        display: inline-block;
      }
      &:hover {
        span {
          color: #1890ff;
        }
      }
    }
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

.status {
  margin-top: -5px;

  &--end {
    margin-left: 5px;
  }

}
</style>
