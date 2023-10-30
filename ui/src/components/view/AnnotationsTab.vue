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

  <div class="account-center-team" v-if="annotationType && 'listAnnotations' in $store.getters.apis">
    <a-spin :spinning="loadingAnnotations">
      <div class="title">
        {{ $t('label.comments') }} ({{ itemCount }})
      </div>
      <a-divider :dashed="true" />
      <a-list
        v-if="notes.length"
        :dataSource="notes"
        itemLayout="horizontal"
        :pagination="false"
        size="small" >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-comment
              class="comment"
              :content="item.annotation"
              :datetime="$toLocaleDate(item.created)"
              :author="item.username" >
              <template #avatar>
                <a-avatar><template #icon><message-outlined /></template></a-avatar>
              </template>
              <template #actions>
                <a-popconfirm
                  :title="$t('label.make') + ' ' + (item.adminsonly ? $t('label.annotation.everyone') : $t('label.adminsonly')) + ' ?'"
                  v-if="['Admin'].includes($store.getters.userInfo.roletype)"
                  key="visibility"
                  @confirm="updateVisibility(item)"
                  :okText="$t('label.yes')"
                  :cancelText="$t('label.no')" >
                  <eye-outlined
                    :style="[{
                      color: item.adminsonly ? $config.theme['@primary-color'] : $config.theme['@disabled-color']
                    }]" />
                  <span>
                    {{ item.adminsonly ? $t('label.adminsonly') : $t('label.annotation.everyone') }}
                  </span>
                </a-popconfirm>
              </template>
            </a-comment>
            <template #actions>
              <a-popconfirm
                :title="$t('label.remove.annotation')"
                v-if="'removeAnnotation' in $store.getters.apis && isAdminOrAnnotationOwner(item)"
                key="visibility"
                @confirm="deleteNote(item)"
                :okText="$t('label.yes')"
                :cancelText="$t('label.no')" >
                <delete-two-tone shape="circle" two-tone-color="#eb2f96" />
              </a-popconfirm>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-pagination
        class="row-element"
        size="small"
        :current="page"
        :pageSize="pageSize"
        :total="itemCount"
        :showTotal="total => `${$t('label.showing')} ${Math.min(total, 1+((page-1)*pageSize))}-${Math.min(page*pageSize, total)} ${$t('label.of')} ${total} ${$t('label.items')}`"
        :pageSizeOptions="['10']"
        @change="changePage"
        showQuickJumper>
        <template #buildOptionText="props">
          <span>{{ props.value }} / {{ $t('label.page') }}</span>
        </template>
      </a-pagination>

      <a-divider :dashed="true" />
      <a-comment v-if="'addAnnotation' in $store.getters.apis">
        <template #avatar>
          <a-avatar @click="showNotesInput = true">
            <template #icon><edit-outlined /></template>
          </a-avatar>
        </template>
        <template #content>
          <div v-ctrl-enter="saveNote">
            <a-textarea
              :rows="4"
              @change="handleNoteChange"
              v-model:value="annotation"
              :placeholder="$t('label.add.note')" />
            <a-checkbox @change="toggleNoteVisibility" v-if="['Admin'].includes($store.getters.userInfo.roletype)" style="margin-top: 10px">
              {{ $t('label.adminsonly') }}
            </a-checkbox>
            <a-button
              style="margin-top: 10px; float: right"
              @click="saveNote"
              type="primary"
              ref="submit">
              {{ $t('label.submit') }}
            </a-button>
          </div>
        </template>
      </a-comment>
    </a-spin>
  </div>
</template>

<script>

import { api } from '@/api'

export default {
  name: 'AnnotationsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    items: {
      type: Array,
      default: () => []
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loadingAnnotations: false,
      notes: [],
      annotation: '',
      annotationType: '',
      annotationAdminsOnly: false,
      showNotesInput: false,
      page: 1,
      pageSize: 10,
      itemCount: 0
    }
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.resourceType = this.$route.meta.resourceType
        this.annotationType = this.generateAnnotationType()
        if (this.annotationType) {
          this.getAnnotations()
        }
      }
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    generateAnnotationType () {
      switch (this.resourceType) {
        case 'UserVm': return 'VM'
        case 'Domain': return 'DOMAIN'
        case 'Host': return 'HOST'
        case 'Volume': return 'VOLUME'
        case 'Snapshot': return 'SNAPSHOT'
        case 'VMSnapshot': return 'VM_SNAPSHOT'
        case 'VMInstanceGroup': return 'INSTANCE_GROUP'
        case 'SSHKeyPair': return 'SSH_KEYPAIR'
        case 'UserData': return 'USER_DATA'
        case 'KubernetesCluster': return 'KUBERNETES_CLUSTER'
        case 'Network': return 'NETWORK'
        case 'Vpc': return 'VPC'
        case 'PublicIpAddress': return 'PUBLIC_IP_ADDRESS'
        case 'VPNCustomerGateway': return 'VPN_CUSTOMER_GATEWAY'
        case 'Template': return 'TEMPLATE'
        case 'ISO': return 'ISO'
        case 'ServiceOffering': return 'SERVICE_OFFERING'
        case 'DiskOffering': return 'DISK_OFFERING'
        case 'NetworkOffering': return 'NETWORK_OFFERING'
        case 'Zone': return 'ZONE'
        case 'Pod': return 'POD'
        case 'Cluster': return 'CLUSTER'
        case 'PrimaryStorage': return 'PRIMARY_STORAGE'
        case 'SecondaryStorage': return 'SECONDARY_STORAGE'
        case 'SystemVm': return 'SYSTEM_VM'
        case 'VirtualRouter': return 'VR'
        case 'AutoScaleVmGroup': return 'AUTOSCALE_VM_GROUP'
        case 'ManagementServer': return 'MANAGEMENT_SERVER'
        default: return ''
      }
    },
    fetchData () {
      this.resourceType = this.$route.meta.resourceType
      this.annotationType = this.generateAnnotationType()
      if (this.items.length) {
        this.notes = this.items
      } else {
        this.getAnnotations()
      }
    },
    changePage (page, pageSize) {
      this.page = page
      this.pagesize = pageSize
      this.getAnnotations()
    },
    getAnnotations () {
      if (!('listAnnotations' in this.$store.getters.apis) || !this.resource || !this.resource.id) {
        return
      }
      this.loadingAnnotations = true
      this.notes = []
      api('listAnnotations', { entityid: this.resource.id, entitytype: this.annotationType, annotationfilter: 'all', page: this.page, pagesize: this.pageSize }).then(json => {
        if (json.listannotationsresponse && json.listannotationsresponse.annotation) {
          this.notes = json.listannotationsresponse.annotation
          this.itemCount = json.listannotationsresponse.count
        }
      }).finally(() => {
        this.loadingAnnotations = false
      })
    },
    handleNoteChange (e) {
      this.annotation = e.target.value
    },
    toggleNoteVisibility () {
      this.annotationAdminsOnly = !this.annotationAdminsOnly
    },
    isAdminOrAnnotationOwner (annotation) {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype) || this.$store.getters.userInfo.id === annotation.userid
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
      args.adminsonly = this.annotationAdminsOnly
      api('addAnnotation', args).catch(error => {
        this.$notifyError(error)
      }).finally(e => {
        this.getAnnotations()
      })
      this.annotation = ''
      this.annotationAdminsOnly = false
    },
    deleteNote (annotation) {
      this.loadingAnnotations = true
      const args = {}
      args.id = annotation.id
      api('removeAnnotation', args).catch(error => {
        this.$notifyError(error)
      }).finally(e => {
        this.getAnnotations()
      })
    },
    updateVisibility (annotation) {
      this.loadingAnnotations = true
      const args = {
        id: annotation.id,
        adminsonly: !annotation.adminsonly
      }
      api('updateAnnotationVisibility', args).catch(error => {
        this.$notifyError(error)
      }).finally(e => {
        this.getAnnotations()
      })
    }
  }
}
</script>

<style lang="scss" scoped>

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

.comment {
  display: inline-block;
  text-overflow: ellipsis;
  width: calc(95%);
}
</style>
