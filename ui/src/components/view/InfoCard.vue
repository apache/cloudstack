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
    <a-card class="spin-content" :bordered="true" :title="title">
      <div>
        <div class="resource-details">
          <div class="avatar">
            <slot name="avatar">
              <os-logo v-if="resource.ostypeid || resource.ostypename" :osId="resource.ostypeid" :osName="resource.ostypename" size="4x" />
              <a-icon v-else style="font-size: 36px" :type="$route.meta.icon" />
            </slot>
          </div>
          <div class="name">
            <slot name="name">
              <h4>
                {{ resource.displayname || resource.name }}
                <console :resource="resource" size="default" v-if="resource.id" />
              </h4>
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
            </slot>
          </div>
        </div>
        <div class="resource-detail-item" style="margin-bottom: 4px" v-if="resource.state || resource.status">
          <status :text="resource.state || resource.status" class="resource-detail-item" />
          <span style="margin-left: -5px">{{ resource.state || resource.status }}</span>
        </div>
        <div class="resource-detail-item" v-if="resource.id">
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
        <div class="resource-detail-item" v-if="resource.ostypename && resource.ostypeid">
          <os-logo :osId="resource.ostypeid" :osName="resource.ostypename" size="lg" style="margin-left: -1px" />
          <span style="margin-left: 8px">{{ resource.ostypename }}</span>
        </div>

        <div class="resource-detail-item" v-if="resource.keypair">
          <a-icon type="key" />
          <router-link :to="{ path: '/ssh/' + resource.keypair }">{{ resource.keypair }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.group">
          <a-icon type="gold" />{{ resource.group }}
        </div>
        <div class="resource-detail-item" v-if="resource.cpunumber && resource.cpuspeed">
          <a-icon type="appstore" />{{ resource.cpunumber }} CPU x {{ parseFloat(resource.cpuspeed / 1000.0).toFixed(2) }} Ghz
          <span
            v-if="resource.cpuused"
            style="display: flex; padding-left: 25px">
            {{ $t('cpuusedghz') }}
            <a-progress
              v-if="resource.cpuused"
              style="padding-left: 10px"
              size="small"
              status="active"
              :percent="parseFloat(resource.cpuused)" />
          </span>
          <span
            v-if="resource.cpuallocated"
            style="display: flex; padding-left: 25px">
            {{ $t('cpuallocatedghz') }}
            <a-progress
              style="padding-left: 10px"
              size="small"
              :percent="parseFloat(resource.cpuallocated)" />
          </span>
        </div>
        <div class="resource-detail-item" v-if="resource.memory">
          <a-icon type="bulb" />{{ resource.memory }} MB Memory
          <span
            v-if="resource.memorykbs && resource.memoryintfreekbs"
            style="display: flex; padding-left: 25px">
            {{ $t('memoryusedgb') }}
            <a-progress
              style="padding-left: 10px"
              size="small"
              status="active"
              :percent="Number(parseFloat(100.0 * (resource.memorykbs - resource.memoryintfreekbs) / resource.memorykbs).toFixed(2))" />
          </span>
        </div>
        <div class="resource-detail-item" v-else-if="resource.memorytotal">
          <a-icon type="bulb" />{{ parseFloat(resource.memorytotal / (1024.0 * 1024.0 * 1024.0)).toFixed(2) }} GB Memory
          <span
            v-if="resource.memoryused"
            style="display: flex; padding-left: 25px">
            {{ $t('memoryusedgb') }}
            <a-progress
              style="padding-left: 10px"
              size="small"
              status="active"
              :percent="Number(parseFloat(100.0 * (resource.memoryused) / resource.memorytotal).toFixed(2))" />
          </span>
          <span
            v-if="resource.memoryallocated"
            style="display: flex; padding-left: 25px">
            {{ $t('memoryallocatedgb') }}
            <a-progress
              style="padding-left: 10px"
              size="small"
              :percent="Number(parseFloat(100.0 * (resource.memoryallocated) / resource.memorytotal).toFixed(2))" />
          </span>
        </div>
        <div class="resource-detail-item" v-if="resource.volumes">
          <a-icon type="hdd" />{{ (resource.volumes.reduce((total, item) => total += item.size, 0) / (1024 * 1024 * 1024.0)).toFixed(2) }} GB Storage
          <div style="margin-left: 25px" v-if="resource.diskkbsread && resource.diskkbswrite && resource.diskioread && resource.diskiowrite">
            <a-tag>Read {{ toSize(resource.diskkbsread) }}</a-tag>
            <a-tag>Write {{ toSize(resource.diskkbswrite) }}</a-tag><br/>
            <a-tag>Read (IO) {{ resource.diskioread }}</a-tag>
            <a-tag>Write (IO) {{ resource.diskiowrite }}</a-tag>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.nic || ('networkkbsread' in resource && 'networkkbswrite' in resource)">
          <a-icon type="wifi" />
          <span v-if="'networkkbsread' in resource && 'networkkbswrite' in resource">
            <a-tag><a-icon type="arrow-down" /> RX {{ toSize(resource.networkkbsread) }}</a-tag>
            <a-tag><a-icon type="arrow-up" /> TX {{ toSize(resource.networkkbswrite) }}</a-tag>
          </span>
          <span v-else>{{ resource.nic.length }} NIC(s)
          </span>
          <div style="margin-left: 25px" v-if="resource.nic" v-for="(eth, index) in resource.nic" :key="eth.id">
            <a-icon type="api" />eth{{ index }} {{ eth.ipaddress }}
            <router-link v-if="eth.networkname && eth.networkid" :to="{ path: '/guestnetwork/' + eth.networkid }">({{ eth.networkname }})</router-link>
          </div>
        </div>
        <div class="resource-detail-item" v-if="resource.ipaddress">
          <a-icon type="environment" />
          <span v-if="resource.nic && resource.nic.length > 0">{{ resource.nic.filter(e => { return e.ipaddress }).map(e => { return e.ipaddress }).join(', ') }}</span>
          <span v-else>{{ resource.ipaddress }}</span>
        </div>

        <div class="resource-detail-item">
          <slot name="details">
          </slot>
        </div>

        <div class="resource-detail-item" v-if="resource.virtualmachineid">
          <a-icon type="desktop" />
          <router-link :to="{ path: '/vm/' + resource.virtualmachineid }">{{ resource.vmname || resource.vm || resource.virtualmachinename || resource.virtualmachineid }} </router-link>
          <status style="margin-top: -5px" :text="resource.vmstate" v-if="resource.vmstate"/>
        </div>
        <div class="resource-detail-item" v-if="resource.volumeid">
          <a-icon type="hdd" />
          <router-link :to="{ path: '/volume/' + resource.volumeid }">{{ resource.volumename || resource.volume || resource.volumeid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.serviceofferingname && resource.serviceofferingid">
          <a-icon type="cloud" />
          <router-link :to="{ path: '/computeoffering/' + resource.serviceofferingid }">{{ resource.serviceofferingname || resource.serviceofferingid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.templateid">
          <a-icon type="picture" />
          <router-link :to="{ path: '/template/' + resource.templateid }">{{ resource.templatename || resource.templateid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.diskofferingname && resource.diskofferingid">
          <a-icon type="hdd" />
          <router-link :to="{ path: '/diskoffering/' + resource.diskofferingid }">{{ resource.diskofferingname || resource.diskofferingid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.networkofferingid">
          <a-icon type="wifi" />
          <router-link :to="{ path: '/networkoffering/' + resource.networkofferingid }">{{ resource.networkofferingname || resource.networkofferingid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.associatednetworkid">
          <a-icon type="wifi" />
          <router-link :to="{ path: '/guestnetwork/' + resource.associatednetworkid }">{{ resource.associatednetworkname || resource.associatednetworkid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.vpcofferingid">
          <a-icon type="deployment-unit" />
          <router-link :to="{ path: '/vpcoffering/' + resource.vpcofferingid }">{{ resource.vpcofferingname || resource.vpcofferingid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.guestnetworkid">
          <a-icon type="gateway" />
          <router-link :to="{ path: '/guestnetwork/' + resource.guestnetworkid }">{{ resource.guestnetworkname || resource.guestnetworkid }} </router-link>
        </div>

        <div class="resource-detail-item" v-if="resource.storageid">
          <a-icon type="database" />
          <router-link :to="{ path: '/storagepool/' + resource.storageid }">{{ resource.storage || resource.storageid }} </router-link>
          <a-tag v-if="resource.storagetype">
            {{ resource.storagetype }}
          </a-tag>
        </div>
        <div class="resource-detail-item" v-if="resource.hostid">
          <a-icon type="desktop" />
          <router-link :to="{ path: '/host/' + resource.hostid }">{{ resource.hostname || resource.hostid }} </router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.clusterid">
          <a-icon type="cluster" />
          <router-link :to="{ path: '/cluster/' + resource.clusterid }">{{ resource.clustername || resource.cluster || resource.clusterid }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.podid">
          <a-icon type="appstore" />
          <router-link :to="{ path: '/pod/' + resource.podid }">{{ resource.podname || resource.pod || resource.podid }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.zoneid">
          <a-icon type="global" />
          <router-link :to="{ path: '/zone/' + resource.zoneid }">{{ resource.zonename || resource.zoneid }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.account">
          <a-icon type="user" />
          <router-link :to="{ path: '/account', query: { name: resource.account, domainid: resource.domainid } }">{{ resource.account }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.roleid">
          <a-icon type="idcard" />
          <router-link :to="{ path: '/role/' + resource.roleid }">{{ resource.rolename || resource.role || resource.roleid }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.domainid">
          <a-icon type="block" />
          <router-link :to="{ path: '/domain/' + resource.domainid }">{{ resource.domain || resource.domainid }}</router-link>
        </div>
        <div class="resource-detail-item" v-if="resource.managementserverid">
          <a-icon type="rocket" />
          <router-link :to="{ path: '/managementserver/' + resource.managementserverid }">{{ resource.managementserver || resource.managementserverid }}</router-link>
        </div>

        <div class="resource-detail-item" v-if="resource.created">
          <a-icon type="calendar" />{{ resource.created }}
        </div>
      </div>

      <div class="account-center-tags" v-if="$route.meta.related">
        <span v-for="item in $route.meta.related" :key="item.path">
          <router-link :to="{ path: '/' + item.name + '?' + item.param + '=' + resource.id }">
            <a-button style="margin-right: 10px">
              View {{ $t(item.title) }}
            </a-button>
          </router-link>
        </span>
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
        <div class="tagsTitle">Tags</div>
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
        <div class="teamTitle">
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
      api('listTags', { 'listall': true, 'resourceid': this.resource.id, 'resourcetype': this.resourceType }).then(json => {
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
      api('listAnnotations', { 'entityid': this.resource.id, 'entitytype': this.annotationType }).then(json => {
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
      args['resourceids'] = this.resource.id
      args['resourcetype'] = this.resourceType
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
      args['resourceids'] = tag.resourceid
      args['resourcetype'] = tag.resourcetype
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
      args['entityid'] = this.resource.id
      args['entitytype'] = this.annotationType
      args['annotation'] = this.annotation
      api('addAnnotation', args).then(json => {
      }).finally(e => {
        this.getNotes()
      })
      this.annotation = ''
    },
    deleteNote (annotation) {
      const args = {}
      args['id'] = annotation.id
      api('removeAnnotation', args).then(json => {
      }).finally(e => {
        this.getNotes()
      })
    }
  }
}
</script>

<style lang="less" scoped>

.resource-details {
  text-align: center;
  margin-bottom: 24px;
  & > .avatar {
    margin: 0 auto;
    padding-top: 20px;
    width: 104px;
    //height: 104px;
    margin-bottom: 20px;
    border-radius: 50%;
    overflow: hidden;
    img {
      height: 100%;
      width: 100%;
    }
  }
  .name {
    color: rgba(0, 0, 0, 0.85);
    font-size: 20px;
    line-height: 28px;
    font-weight: 500;
    margin-bottom: 4px;
    word-wrap: break-word;
  }
}
.resource-detail-item {
  .anticon {
    margin-right: 10px;
  }
  margin-right: 10px;
  margin-bottom: 12px;
}
.user-keys {
  word-wrap: break-word;
}
.account-center-tags {
  .ant-tag {
    margin-bottom: 8px;
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
.tagsTitle,
.teamTitle {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}
</style>
