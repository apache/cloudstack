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
  <a-spin :spinning="fetchLoading">
    <div style="width: 100%; display: flex">
      <a-button type="dashed" icon="plus" style="width: 100%; margin-right: 10px" @click="openAddRuleModal">
        {{ $t('add') }} {{ $t('aclid') }}
      </a-button>

      <a-button type="dashed" @click="exportAclList" style="width: 100%" icon="download">
        Export ACLs
      </a-button>
    </div>

    <div class="list">
      <draggable
        v-model="acls"
        @change="changeOrder"
        handle=".drag-handle"
        animation="200"
        ghostClass="drag-ghost">
        <transition-group type="transition">
          <div v-for="acl in acls" :key="acl.id" class="list__item">
            <div class="drag-handle">
              <a-icon type="drag"></a-icon>
            </div>
            <div class="list__container">
              <div class="list__col">
                <div class="list__label">{{ $t('number') }}</div>
                <div>{{ acl.number }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('cidrlist') }}</div>
                <div>{{ acl.cidrlist }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('action') }}</div>
                <div>{{ acl.action }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('protocol') }}</div>
                <div>{{ acl.protocol }}</div>
              </div>
              <div class="list__col" v-if="acl.startport">
                <div class="list__label">{{ $t('startport') }}</div>
                <div>{{ acl.startport }}</div>
              </div>
              <div class="list__col" v-if="acl.endport">
                <div class="list__label">{{ $t('endport') }}</div>
                <div>{{ acl.endport }}</div>
              </div>
              <div class="list__col" v-if="acl.icmpcode">
                <div class="list__label">{{ $t('icmpcode') }}</div>
                <div>{{ acl.icmpcode }}</div>
              </div>
              <div class="list__col" v-if="acl.icmptype">
                <div class="list__label">{{ $t('icmptype') }}</div>
                <div>{{ acl.icmptype }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('traffictype') }}</div>
                <div>{{ acl.traffictype }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('reason') }}</div>
                <div>{{ acl.reason }}</div>
              </div>
            </div>
            <div class="list__actions">
              <a-button shape="round" icon="tag" @click="() => openTagsModal(acl)"></a-button>
              <a-button shape="round" icon="edit" @click="() => openEditRuleModal(acl)"></a-button>
              <a-button shape="round" icon="delete" type="danger" @click="() => handleDeleteRule(acl.id)"></a-button>
            </div>
          </div>
        </transition-group>
      </draggable>
    </div>

    <a-modal title="Edit Tags" v-model="tagsModalVisible" :footer="null">
      <a-spin v-if="tagsLoading"></a-spin>

      <div v-else>
        <a-form :form="newTagsForm" class="add-tags" @submit="handleAddTag">
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('key') }}</p>
            <a-form-item>
              <a-input v-decorator="['key', { rules: [{ required: true, message: 'Please specify a tag key'}] }]" />
            </a-form-item>
          </div>
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('value') }}</p>
            <a-form-item>
              <a-input v-decorator="['value', { rules: [{ required: true, message: 'Please specify a tag value'}] }]" />
            </a-form-item>
          </div>
          <a-button type="primary" html-type="submit">{{ $t('label.add') }}</a-button>
        </a-form>

        <a-divider style="margin-top: 0;"></a-divider>

        <div class="tags-container">
          <div class="tags" v-for="(tag, index) in tags" :key="index">
            <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
              {{ tag.key }} = {{ tag.value }}
            </a-tag>
          </div>
        </div>

        <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('OK') }}</a-button>
      </div>

    </a-modal>
    <a-modal :title="ruleModalTitle" v-model="ruleModalVisible" @ok="handleRuleModalForm">
      <a-form :form="ruleForm" @submit="handleRuleModalForm">
        <a-form-item :label="$t('number')">
          <a-input v-decorator="['number']" />
        </a-form-item>
        <a-form-item :label="$t('cidrlist')">
          <a-input v-decorator="['cidr']" />
        </a-form-item>
        <a-form-item :label="$t('action')">
          <a-select v-decorator="['action']">
            <a-select-option value="allow">Allow</a-select-option>
            <a-select-option value="deny">Deny</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('protocol')">
          <a-select v-decorator="['protocol']">
            <a-select-option value="tcp">TCP</a-select-option>
            <a-select-option value="udp">UDP</a-select-option>
            <a-select-option value="icmp">ICMP</a-select-option>
            <a-select-option value="all">ALL</a-select-option>
            <a-select-option value="protocolnumber">Protocol number</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item v-if="ruleForm.getFieldValue('protocol') === 'protocolnumber'" :label="$t('protocolnumber')">
          <a-input v-decorator="['protocolnumber' , { rules: [{ required: true, message: 'required' }]}]" />
        </a-form-item>

        <div v-if="ruleForm.getFieldValue('protocol') === 'icmp' || ruleForm.getFieldValue('protocol') === 'protocolnumber'">
          <a-form-item :label="$t('icmptype')">
            <a-input v-decorator="['icmptype']" placeholder="Please specify -1 if you want to allow all ICMP types." />
          </a-form-item>
          <a-form-item :label="$t('icmpcode')">
            <a-input v-decorator="['icmpcode']" placeholder="Please specify -1 if you want to allow all ICMP types." />
          </a-form-item>
        </div>

        <div v-if="ruleForm.getFieldValue('protocol') === 'tcp' || ruleForm.getFieldValue('protocol') === 'udp' || ruleForm.getFieldValue('protocol') === 'protocolnumber'">
          <a-form-item :label="$t('startport')">
            <a-input v-decorator="['startport']" />
          </a-form-item>
          <a-form-item :label="$t('endport')">
            <a-input v-decorator="['endport']" />
          </a-form-item>
        </div>

        <a-form-item :label="$t('traffictype')">
          <a-select v-decorator="['traffictype']">
            <a-select-option value="ingress">Ingress</a-select-option>
            <a-select-option value="egress">Egress</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('reason')">
          <a-textarea
            v-decorator="['reason']"
            :autosize="{ minRows: 2 }"
            placeholder="Enter the reason behind an ACL rule" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-spin>
</template>

<script>
import { api } from '@/api'
import draggable from 'vuedraggable'

export default {
  name: 'AclListRulesTab',
  components: {
    draggable
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      acls: [],
      fetchLoading: false,
      tags: [],
      selectedAcl: null,
      tagsModalVisible: false,
      newTagsForm: this.$form.createForm(this),
      ruleForm: this.$form.createForm(this),
      tagsLoading: false,
      ruleModalVisible: false,
      ruleModalTitle: 'Edit rule',
      ruleFormMode: 'edit'
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    csv ({ data = null, columnDelimiter = ',', lineDelimiter = '\n' }) {
      let result = null
      let ctr = null
      let keys = null

      if (data === null || !data.length) {
        return null
      }

      keys = Object.keys(data[0])

      result = ''
      result += keys.join(columnDelimiter)
      result += lineDelimiter

      data.forEach(item => {
        ctr = 0
        keys.forEach(key => {
          if (ctr > 0) {
            result += columnDelimiter
          }

          result += typeof item[key] === 'string' && item[key].includes(columnDelimiter) ? `"${item[key]}"` : item[key]
          ctr++
        })
        result += lineDelimiter
      })

      return result
    },
    fetchData () {
      this.fetchLoading = true
      api('listNetworkACLs', { aclid: this.resource.id }).then(json => {
        this.acls = json.listnetworkaclsresponse.networkacl || []
        if (this.acls.length > 0) {
          this.acls.sort((a, b) => a.number - b.number)
        }
      }).catch(error => {
        this.$notification.error({
          message: 'Request Failed',
          description: error.response.headers['x-description']
        })
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    fetchTags (acl) {
      api('listTags', {
        resourceId: acl.id,
        resourceType: 'NetworkACL',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag || []
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      })
    },
    openTagsModal (acl) {
      this.selectedAcl = acl
      this.newTagsForm.resetFields()
      this.fetchTags(this.selectedAcl)
      this.tagsModalVisible = true
    },
    handleDeleteTag (tag) {
      this.tagsLoading = true
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: this.selectedAcl.id,
        resourceType: 'NetworkACL'
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `Remove tag for NetworkACL`,
          jobid: response.deletetagsresponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: `Successfully deleted tag`,
          successMethod: () => {
            this.fetchTags(this.selectedAcl)
            this.tagsLoading = false
          },
          errorMessage: 'Failed to delete tag',
          errorMethod: () => {
            this.fetchTags(this.selectedAcl)
            this.tagsLoading = false
          },
          loadingMessage: `Deleting tag...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.fetchTags(this.selectedAcl)
            this.tagsLoading = false
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.deletetagsresponse.errortext
        })
        this.tagsLoading = false
      })
    },
    handleAddTag (e) {
      this.tagsLoading = true

      e.preventDefault()
      this.newTagsForm.validateFields((err, values) => {
        if (err) {
          this.tagsLoading = false
          return
        }

        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedAcl.id,
          resourceType: 'NetworkACL'
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: `Add tag for NetworkACL`,
            jobid: response.createtagsresponse.jobid,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.createtagsresponse.jobid,
            successMessage: `Successfully added new tag`,
            successMethod: () => {
              this.fetchTags(this.selectedAcl)
              this.tagsLoading = false
            },
            errorMessage: 'Failed to add new tag',
            errorMethod: () => {
              this.fetchTags(this.selectedAcl)
              this.tagsLoading = false
            },
            loadingMessage: `Adding new tag...`,
            catchMessage: 'Error encountered while fetching async job result',
            catchMethod: () => {
              this.fetchTags(this.selectedAcl)
              this.tagsLoading = false
            }
          })
        }).catch(error => {
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.createtagsresponse.errortext
          })
          this.tagsLoading = false
        })
      })
    },
    openEditRuleModal (acl) {
      this.ruleModalTitle = 'Edit rule'
      this.ruleFormMode = 'edit'
      this.ruleForm.resetFields()
      this.ruleModalVisible = true
      this.selectedAcl = acl
      setTimeout(() => {
        this.ruleForm.setFieldsValue({
          number: acl.number,
          cidr: acl.cidrlist,
          action: acl.action,
          protocol: acl.protocol,
          startport: acl.startport,
          endport: acl.endport,
          traffictype: acl.traffictype,
          reason: acl.reason
        })
      }, 200)
    },
    handleEditRule (e) {
      e.preventDefault()
      this.ruleForm.validateFields((err, values) => {
        if (err) return

        this.fetchLoading = true
        this.ruleModalVisible = false
        api('updateNetworkACLItem', {}, 'POST', {
          id: this.selectedAcl.id,
          cidrlist: values.cidr,
          number: values.number,
          protocol: values.protocol,
          traffictype: values.traffictype,
          action: values.action,
          reason: values.reason,
          startport: values.startport,
          endport: values.endport,
          partialupgrade: false
        }).then(response => {
          this.$store.dispatch('AddAsyncJob', {
            title: `Edit ACL rule`,
            jobid: response.createnetworkaclresponse.jobid,
            status: 'progress'
          })
          this.$pollJob({
            jobId: response.createnetworkaclresponse.jobid,
            successMessage: `Successfully edited ACL rule`,
            successMethod: () => {
              this.fetchData()
              this.fetchLoading = false
            },
            errorMessage: 'Failed to edit ACL rule',
            errorMethod: () => {
              this.fetchData()
              this.fetchLoading = false
            },
            loadingMessage: `Editing ACL rule...`,
            catchMessage: 'Error encountered while fetching async job result',
            catchMethod: () => {
              this.fetchData()
              this.fetchLoading = false
            }
          })
        }).catch(error => {
          this.$notification.error({
            message: `Error ${error.response.status}`,
            description: error.response.data.createnetworkaclresponse.errortext
          })
          this.fetchLoading = false
        })
      })
    },
    handleDeleteRule (id) {
      this.fetchLoading = true
      api('deleteNetworkACL', { id }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `Remove ACL rule`,
          jobid: response.deletenetworkaclresponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.deletenetworkaclresponse.jobid,
          successMessage: `Successfully removed ACL rule`,
          successMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          errorMessage: 'Failed to remove ACL rule',
          errorMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          loadingMessage: `Removing ACL rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.deletenetworkaclresponse.errortext
        })
        this.fetchLoading = false
      })
    },
    handleRuleModalForm (e) {
      if (this.ruleFormMode === 'edit') {
        this.handleEditRule(e)
        return
      }
      if (this.ruleFormMode === 'add') this.handleAddRule(e)
    },
    openAddRuleModal () {
      this.ruleModalTitle = 'Add rule'
      this.ruleModalVisible = true
      this.ruleFormMode = 'add'
      this.ruleForm.resetFields()
      setTimeout(() => {
        this.ruleForm.setFieldsValue({
          action: 'allow',
          protocol: 'tcp',
          traffictype: 'ingress'
        })
      }, 200)
    },
    handleAddRule (e) {
      e.preventDefault()
      this.ruleForm.validateFields((err, values) => {
        if (err) return

        this.fetchLoading = true
        this.ruleModalVisible = false

        const data = {
          aclid: this.resource.id,
          cidrlist: values.cidr || '',
          number: values.number || '',
          protocol: values.protocol || '',
          traffictype: values.traffictype || '',
          action: values.action || '',
          reason: values.reason || ''
        }

        if (values.protocol === 'tcp' || values.protocol === 'udp' || values.protocol === 'protocolnumber') {
          data.startport = values.startport || ''
          data.endport = values.endport || ''
        }

        if (values.protocol === 'icmp') {
          data.icmptype = values.icmptype || -1
          data.icmpcode = values.icmpcode || -1
        }

        if (values.protocol === 'protocolnumber') {
          data.protocol = values.protocolnumber
        }

        api('createNetworkACL', {}, 'POST', data).then(() => {
          this.$notification.success({
            message: 'Success',
            description: 'Successfully added new rule'
          })
        }).catch(error => {
          this.$notification.error({
            message: 'Request Failed',
            description: error.response.headers['x-description']
          })
        }).finally(() => {
          this.fetchLoading = false
          this.fetchData()
        })
      })
    },
    changeOrder (e) {
      const id = e.moved.element.id
      let previousaclruleid = null
      let nextaclruleid = null

      if (e.moved.newIndex - 1 >= 0) previousaclruleid = this.acls[e.moved.newIndex - 1].id

      if (e.moved.newIndex + 1 < this.acls.length) nextaclruleid = this.acls[e.moved.newIndex + 1].id

      this.fetchLoading = true
      api('moveNetworkAclItem', {
        id,
        previousaclruleid,
        nextaclruleid
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `Move ACL rule order`,
          jobid: response.moveNetworkAclItemResponse.jobid,
          status: 'progress'
        })
        this.$pollJob({
          jobId: response.moveNetworkAclItemResponse.jobid,
          successMessage: `Successfully moved ACL rule`,
          successMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          errorMessage: 'Failed to move ACL rule',
          errorMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          loadingMessage: `Moving ACL rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.headers['x-description']
        })
        this.fetchLoading = false
      })
    },
    exportAclList () {
      const csvData = this.csv({ data: this.acls })

      const hiddenElement = document.createElement('a')
      hiddenElement.href = 'data:text/csv;charset=utf-8,' + encodeURI(csvData)
      hiddenElement.target = '_blank'
      hiddenElement.download = 'AclRules-' + this.resource.name + '-' + this.resource.id + '.csv'
      hiddenElement.click()
    }
  }
}
</script>

<style lang="scss" scoped>
.list {

  &__item {
    display: flex;
    padding-top: 20px;
    padding-bottom: 20px;

    &:not(:last-child) {
      border-bottom: 1px solid #d9d9d9;
    }

  }

  &__container {
    display: flex;
    flex-wrap: wrap;
    width: 100%;
  }

  &__col {
    margin-right: 20px;
    margin-bottom: 20px;
    flex-basis: calc(50% - 20px);

    @media (min-width: 760px) {
      flex-basis: calc(25% - 20px);
    }

  }

  &__label {
    font-weight: bold;
  }

  &__actions {
    display: flex;
    flex-direction: column;

    button {
      &:not(:last-child) {
        margin-bottom: 10px;
      }
    }

  }

}

.tags {
  margin-bottom: 10px;
}
.add-tags {
  display: flex;
  align-items: center;
  justify-content: space-between;
  &__input {
    margin-right: 10px;
  }
  &__label {
    margin-bottom: 5px;
    font-weight: bold;
  }
}
.tags-container {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.add-tags-done {
  display: block;
  margin-left: auto;
}

.drag-handle {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding-right: 20px;
}

.drag-ghost {
  opacity: 0.5;
  background: #f0f2f5;
}

.download {
  display: block;
  margin-top: 10px;
  margin-bottom: 10px;
  margin-left: auto;
}
</style>
