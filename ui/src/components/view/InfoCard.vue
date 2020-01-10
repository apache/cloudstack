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
            <div class="avatar">
              <slot name="avatar">
                <os-logo v-if="resource.ostypeid || resource.ostypename" :osId="resource.ostypeid" :osName="resource.ostypename" size="4x" />
                <a-icon v-else style="font-size: 36px" :type="$route.meta.icon" />
                <console style="margin-left: -15px" :resource="resource" size="default" v-if="resource.id" />
              </slot>
            </div>
            <slot name="name">
              <h4 class="name">
                {{ resource.displayname || resource.name }}
              </h4>
            </slot>
          </div>
          <slot name="actions">
            <div class="tags">
              <a-tag v-if="resource.instancename">
                {{ resource.instancename }}
              </a-tag>
              <a-tag v-if="resource.type">
                {{ resource.type }}
              </a-tag>
              <a-tag v-if="resource.broadcasturi">
                {{ resource.broadcasturi }}
              </a-tag>
              <a-tag v-if="resource.hypervisor">
                {{ resource.hypervisor }}
              </a-tag>
              <a-tag v-if="'haenable' in resource" :color="resource.haenable ? 'green': 'red'">
                {{ $t('haenable') }}
              </a-tag>
              <a-tag v-if="'isdynamicallyscalable' in resource" :color="resource.isdynamicallyscalable ? 'green': 'red'">
                {{ $t('isdynamicallyscalable') }}
              </a-tag>
            </div>
          </slot>
        </div>

        <a-divider/>

        <div class="resource-detail-item" v-if="resource.state || resource.status">
          <div class="resource-detail-item__label">{{ $t('status') }}</div>
          <div class="resource-detail-item__details">
            <status :text="resource.state || resource.status"/>
            <span>{{ resource.state || resource.status }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.id">
          <div class="resource-detail-item__label">{{ $t('id') }}</div>
          <div class="resource-detail-item__details">
            <a-tooltip placement="right" >
              <template slot="title">
                <span>Copy ID</span>
              </template>
              <a-button shape="circle" type="dashed" size="small" v-clipboard:copy="resource.id" style="margin-left: -5px">
                <a-icon type="barcode" style="padding-left: 4px; margin-top: 4px"/>
              </a-button>
            </a-tooltip>
            <span style="margin-left: 5px;">{{ resource.id }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ostypename && resource.ostypeid">
          <div class="resource-detail-item__label">{{ $t('ostypename') }}</div>
          <div class="resource-detail-item__details">
            <os-logo :osId="resource.ostypeid" :osName="resource.ostypename" size="lg" style="margin-left: -1px" />
            <span style="margin-left: 8px">{{ resource.ostypename }}</span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="(resource.cpunumber && resource.cpuspeed) || resource.cputotal">
          <div class="resource-detail-item__label">{{ $t('cpu') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="appstore" />
            <span v-if="resource.cpunumber && resource.cpuspeed">{{ resource.cpunumber }} CPU x {{ parseFloat(resource.cpuspeed / 1000.0).toFixed(2) }} Ghz</span>
            <span v-else-if="resource.cputotal">{{ resource.cputotal }}</span>
          </div>
          <div>
            <span v-if="resource.cpuused">
              <a-progress
                v-if="resource.cpuused"
                style="width: 85%"
                size="small"
                status="active"
                :percent="parseFloat(resource.cpuused)"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('cpuusedghz')"
              />
            </span>
            <span v-if="resource.cpuallocated">
              <a-progress
                style="width: 85%"
                size="small"
                :percent="parseFloat(resource.cpuallocated)"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('cpuallocatedghz')"
              />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.memory">
          <div class="resource-detail-item__label">{{ $t('memory') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="bulb" />{{ resource.memory }} MB Memory
          </div>
          <div>
            <span v-if="resource.memorykbs && resource.memoryintfreekbs">
              <a-progress
                style="width: 85%"
                size="small"
                status="active"
                :percent="Number(parseFloat(100.0 * (resource.memorykbs - resource.memoryintfreekbs) / resource.memorykbs).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('memoryusedgb')"
              />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-else-if="resource.memorytotalgb">
          <div class="resource-detail-item__label">{{ $t('memory') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="bulb" />{{ resource.memorytotalgb }} Memory
          </div>
          <div>
            <span v-if="resource.memoryusedgb">
              <a-progress
                style="width: 85%"
                size="small"
                status="active"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.memoryusedgb) / parseFloat(resource.memorytotalgb)).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('memoryusedgb')"
              />
            </span>
            <span v-if="resource.memoryallocatedgb">
              <a-progress
                style="width: 85%"
                size="small"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.memoryallocatedgb) / parseFloat(resource.memorytotalgb)).toFixed(2))"
                :format="(percent, successPercent) => parseFloat(percent).toFixed(2) + '% ' + $t('memoryallocatedgb')"
              />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-else-if="resource.memorytotal">
          <div class="resource-detail-item__label">{{ $t('memory') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="bulb" />{{ resource.memorytotal }} Memory
            <span
              v-if="resource.memoryused"
              style="display: flex; padding-left: 25px">
              {{ $t('memoryused') }}
              <a-progress
                style="padding-left: 10px"
                size="small"
                status="active"
                :percent="parseFloat(resource.memoryused)" />
            </span>
            <span
              v-if="resource.memoryallocated"
              style="display: flex; padding-left: 25px">
              {{ $t('memoryallocatedgb') }}
              <a-progress
                style="padding-left: 10px"
                size="small"
                :percent="parseFloat(resource.memoryallocated)" />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.volumes || resource.sizegb">
          <div class="resource-detail-item__label">{{ $t('disksize') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="hdd" />
            <span v-if="resource.volumes">{{ (resource.volumes.reduce((total, item) => total += item.size, 0) / (1024 * 1024 * 1024.0)).toFixed(2) }} GB Storage</span>
            <span v-else-if="resource.sizegb">{{ resource.sizegb }}</span>
            <div style="margin-left: 25px" v-if="resource.diskkbsread && resource.diskkbswrite && resource.diskioread && resource.diskiowrite">
              <a-tag>Read {{ toSize(resource.diskkbsread) }}</a-tag>
              <a-tag>Write {{ toSize(resource.diskkbswrite) }}</a-tag><br/>
              <a-tag>Read (IO) {{ resource.diskioread }}</a-tag>
              <a-tag>Write (IO) {{ resource.diskiowrite }}</a-tag>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-else-if="resource.disksizetotalgb">
          <div class="resource-detail-item__label">{{ $t('disksize') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="database" />{{ resource.disksizetotalgb }}
            <span
              v-if="resource.disksizeusedgb"
              style="display: flex; padding-left: 25px">
              {{ $t('disksizeusedgb') }}
              <a-progress
                style="padding-left: 10px"
                size="small"
                status="active"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.disksizeusedgb) / parseFloat(resource.disksizetotalgb)).toFixed(2))" />
            </span>
            <span
              v-if="resource.disksizeallocatedgb"
              style="display: flex; padding-left: 25px">
              {{ $t('disksizeallocatedgb') }}
              <a-progress
                style="padding-left: 10px"
                size="small"
                :percent="Number(parseFloat(100.0 * parseFloat(resource.disksizeallocatedgb) / parseFloat(resource.disksizetotalgb)).toFixed(2))" />
            </span>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.nic || ('networkkbsread' in resource && 'networkkbswrite' in resource)">
          <div class="resource-detail-item__label">{{ $t('network') }}</div>
          <div class="resource-detail-item__details resource-detail-item__details--start">
            <a-icon type="wifi" />
            <div>
              <div v-if="'networkkbsread' in resource && 'networkkbswrite' in resource">
                <a-tag><a-icon type="arrow-down" /> RX {{ toSize(resource.networkkbsread) }}</a-tag>
                <a-tag><a-icon type="arrow-up" /> TX {{ toSize(resource.networkkbswrite) }}</a-tag>
              </div>
              <div v-else>{{ resource.nic.length }} NIC(s)</div>
              <div
                v-if="resource.nic"
                v-for="(eth, index) in resource.nic"
                :key="eth.id"
                style="margin-left: -24px; margin-top: 5px;">
                <a-icon type="api" />eth{{ index }} {{ eth.ipaddress }}
                <router-link v-if="eth.networkname && eth.networkid" :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link>
              </div>
            </div>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ipaddress">
          <div class="resource-detail-item__label">{{ $t('ip') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="environment" />
            <span v-if="resource.nic && resource.nic.length > 0">{{ resource.nic.filter(e => { return e.ipaddress }).map(e => { return e.ipaddress }).join(', ') }}</span>
            <span v-else>{{ resource.ipaddress }}</span>
          </div>
        </div>

        <div class="resource-detail-item">
          <slot name="details">
          </slot>
        </div>

        <div class="resource-detail-item" v-if="resource.groupid">
          <div class="resource-detail-item__label">{{ $t('group') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="gold" />
            <router-link :to="{ path: '/vmgroup/' + resource.groupid }">{{ resource.group || resource.groupid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.keypair">
          <div class="resource-detail-item__label">{{ $t('keypair') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="key" />
            <router-link :to="{ path: '/ssh/' + resource.keypair }">{{ resource.keypair }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.virtualmachineid">
          <div class="resource-detail-item__label">{{ $t('vmname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="desktop" />
            <router-link :to="{ path: '/vm/' + resource.virtualmachineid }">{{ resource.vmname || resource.vm || resource.virtualmachinename || resource.virtualmachineid }} </router-link>
            <status style="margin-top: -5px" :text="resource.vmstate" v-if="resource.vmstate"/>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.volumeid">
          <div class="resource-detail-item__label">{{ $t('volume') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="hdd" />
            <router-link :to="{ path: '/volume/' + resource.volumeid }">{{ resource.volumename || resource.volume || resource.volumeid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.associatednetworkid">
          <div class="resource-detail-item__label">{{ $t('associatednetwork') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="wifi" />
            <router-link :to="{ path: '/guestnetwork/' + resource.associatednetworkid }">{{ resource.associatednetworkname || resource.associatednetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.guestnetworkid">
          <div class="resource-detail-item__label">{{ $t('guestNetwork') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="gateway" />
            <router-link :to="{ path: '/guestnetwork/' + resource.guestnetworkid }">{{ resource.guestnetworkname || resource.guestnetworkid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcid">
          <div class="resource-detail-item__label">{{ $t('vpcname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="deployment-unit" />
            <router-link :to="{ path: '/vpc/' + resource.vpcid }">{{ resource.vpcname || resource.vpcid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.serviceofferingname && resource.serviceofferingid">
          <div class="resource-detail-item__label">{{ $t('serviceCapabilities') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="cloud" />
            <router-link :to="{ path: '/computeoffering/' + resource.serviceofferingid }">{{ resource.serviceofferingname || resource.serviceofferingid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.templateid">
          <div class="resource-detail-item__label">{{ $t('templatename') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="picture" />
            <router-link :to="{ path: '/template/' + resource.templateid }">{{ resource.templatename || resource.templateid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.diskofferingname && resource.diskofferingid">
          <div class="resource-detail-item__label">{{ $t('diskOffering') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="hdd" />
            <router-link :to="{ path: '/diskoffering/' + resource.diskofferingid }">{{ resource.diskofferingname || resource.diskofferingid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.networkofferingid">
          <div class="resource-detail-item__label">{{ $t('networkofferingdisplaytext') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="wifi" />
            <router-link :to="{ path: '/networkoffering/' + resource.networkofferingid }">{{ resource.networkofferingname || resource.networkofferingid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcofferingid">
          <div class="resource-detail-item__label">{{ $t('vpcoffering') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="deployment-unit" />
            <router-link :to="{ path: '/vpcoffering/' + resource.vpcofferingid }">{{ resource.vpcofferingname || resource.vpcofferingid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.storageid">
          <div class="resource-detail-item__label">{{ $t('Storage') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="database" />
            <router-link :to="{ path: '/storagepool/' + resource.storageid }">{{ resource.storage || resource.storageid }} </router-link>
            <a-tag v-if="resource.storagetype">
              {{ resource.storagetype }}
            </a-tag>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.hostid">
          <div class="resource-detail-item__label">{{ $t('hostname') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="desktop" />
            <router-link :to="{ path: '/host/' + resource.hostid }">{{ resource.hostname || resource.hostid }} </router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.clusterid">
          <div class="resource-detail-item__label">{{ $t('clusterid') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="cluster" />
            <router-link :to="{ path: '/cluster/' + resource.clusterid }">{{ resource.clustername || resource.cluster || resource.clusterid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.podid">
          <div class="resource-detail-item__label">{{ $t('podId') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="appstore" />
            <router-link :to="{ path: '/pod/' + resource.podid }">{{ resource.podname || resource.pod || resource.podid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.zoneid">
          <div class="resource-detail-item__label">{{ $t('zone') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="global" />
            <router-link :to="{ path: '/zone/' + resource.zoneid }">{{ resource.zonename || resource.zoneid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.account">
          <div class="resource-detail-item__label">{{ $t('account') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="user" />
            <router-link :to="{ path: '/account', query: { name: resource.account, domainid: resource.domainid } }">{{ resource.account }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.roleid">
          <div class="resource-detail-item__label">{{ $t('role') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="idcard" />
            <router-link :to="{ path: '/role/' + resource.roleid }">{{ resource.rolename || resource.role || resource.roleid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.domainid">
          <div class="resource-detail-item__label">{{ $t('domain') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="block" />
            <router-link :to="{ path: '/domain/' + resource.domainid }">{{ resource.domain || resource.domainid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.managementserverid">
          <div class="resource-detail-item__label">{{ $t('Management Servers') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="rocket" />
            <router-link :to="{ path: '/managementserver/' + resource.managementserverid }">{{ resource.managementserver || resource.managementserverid }}</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.created">
          <div class="resource-detail-item__label">{{ $t('created') }}</div>
          <div class="resource-detail-item__details">
            <a-icon type="calendar" />{{ resource.created }}
          </div>
        </div>
      </div>

      <div class="account-center-tags" v-if="$route.meta.related">
        <a-divider/>
        <div v-for="item in $route.meta.related" :key="item.path">
          <router-link
            v-if="$router.resolve('/' + item.name).route.name !== '404'"
            :to="{ path: '/' + item.name + '?' + item.param + '=' + (item.param === 'account' ? resource.name + '&domainid=' + resource.domainid : resource.id) }">
            <a-button style="margin-right: 10px" :icon="$router.resolve('/' + item.name).route.meta.icon" >
              View {{ $t(item.title) }}
            </a-button>
          </router-link>
        </div>
      </div>

      <div class="account-center-tags" v-if="showKeys">
        <a-divider/>
        <div class="user-keys">
          <a-icon type="key" />
          <strong>
            {{ $t('apikey') }}
            <a-tooltip placement="right" >
              <template slot="title">
                <span>Copy {{ $t('apikey') }}</span>
              </template>
              <a-button shape="circle" type="dashed" size="small" v-clipboard:copy="resource.apikey">
                <a-icon type="copy"/>
              </a-button>
            </a-tooltip>
          </strong>
          {{ resource.apikey }}
        </div> <br/>
        <div class="user-keys">
          <a-icon type="lock" />
          <strong>
            {{ $t('secretkey') }}
            <a-tooltip placement="right" >
              <template slot="title">
                <span>Copy {{ $t('secretkey') }}</span>
              </template>
              <a-button shape="circle" type="dashed" size="small" v-clipboard:copy="resource.apikey">
                <a-icon type="copy"/>
              </a-button>
            </a-tooltip>
          </strong>
          {{ resource.secretkey }}
        </div>
      </div>

      <div class="account-center-tags" v-if="resourceType && 'listTags' in $store.getters.apis">
        <a-divider/>
        <div class="title">Tags</div>
        <div>
          <template v-for="(tag, index) in tags">
            <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
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
              <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 100px; text-align: center" placeholder="Key" />
              <a-input style=" width: 30px; border-left: 0; pointer-events: none; backgroundColor: #fff" placeholder="=" disabled />
              <a-input :value="inputValue" @change="handleValueChange" style="width: 100px; text-align: center; border-left: 0" placeholder="Value" />
              <a-button shape="circle" size="small" @click="handleInputConfirm">
                <a-icon type="check"/>
              </a-button>
              <a-button shape="circle" size="small" @click="inputVisible=false">
                <a-icon type="close"/>
              </a-button>
            </a-input-group>
          </div>
          <a-tag v-else @click="showInput" style="background: #fff; borderStyle: dashed;">
            <a-icon type="plus" /> New Tag
          </a-tag>
        </div>
      </div>

      <div class="account-center-team" v-if="annotationType && 'listAnnotations' in $store.getters.apis">
        <a-divider :dashed="true"/>
        <div class="title">
          Comments ({{ notes.length }})
        </div>
        <a-list
          v-if="notes.length"
          :dataSource="notes"
          itemLayout="horizontal"
          size="small"
        >
          <a-list-item slot="renderItem" slot-scope="item">
            <a-comment
              :content="item.annotation"
              :datetime="item.created"
            >
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
            @click="showNotesInput = true"
          />
          <div slot="content">
            <a-textarea
              rows="4"
              @change="handleNoteChange"
              :value="annotation"
              placeholder="Add Note" />
            <a-button
              style="margin-top: 10px"
              @click="saveNote"
              type="primary"
            >
              Save
            </a-button>
          </div>
        </a-comment>
      </div>
    </a-card>
  </a-spin>
</template>

<script>

import { api } from '@/api'
import Console from '@/components/widgets/Console'
import OsLogo from '@/components/widgets/OsLogo'
import Status from '@/components/widgets/Status'

export default {
  name: 'InfoCard',
  components: {
    Console,
    OsLogo,
    Status
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
      resourceType: '',
      annotationType: '',
      inputVisible: false,
      inputKey: '',
      inputValue: '',
      tags: [],
      notes: [],
      annotation: '',
      showKeys: false,
      showNotesInput: false
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
      if (newItem.id === oldItem.id) {
        return
      }

      this.resourceType = this.$route.meta.resourceType
      this.annotationType = ''
      this.showKeys = false

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
      }
      if (this.resourceType) {
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
  methods: {
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
      this.tags = []
      api('listTags', { listall: true, resourceid: this.resource.id, resourcetype: this.resourceType }).then(json => {
        if (json.listtagsresponse && json.listtagsresponse.tag) {
          this.tags = json.listtagsresponse.tag
        }
      })
    },
    getNotes () {
      if (!('listAnnotations' in this.$store.getters.apis)) {
        return
      }
      this.notes = []
      api('listAnnotations', { entityid: this.resource.id, entitytype: this.annotationType }).then(json => {
        if (json.listannotationsresponse && json.listannotationsresponse.annotation) {
          this.notes = json.listannotationsresponse.annotation
        }
      })
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
      args.resourceids = tag.resourceid
      args.resourcetype = tag.resourcetype
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

      img {
        height: 100%;
        width: 100%;
      }
    }

    .name {
      margin-bottom: 0;
      font-size: 18px;
      line-height: 1;
      word-wrap: break-word;
      text-align: left;
    }

  }
}
.resource-detail-item {
  margin-bottom: 20px;
  word-break: break-word;

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

  .ant-tag {
    margin-right: 10px;
    margin-bottom: 10px;
    height: auto;
  }

}
</style>
