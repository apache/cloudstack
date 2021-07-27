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
      <a-button
        type="dashed"
        icon="plus"
        style="width: 100%; margin-right: 10px"
        :disabled="!('createNetworkACL' in $store.getters.apis)"
        @click="openAddRuleModal">
        {{ $t('label.add') }} {{ $t('label.aclid') }}
      </a-button>

      <a-button type="dashed" @click="exportAclList" style="width: 100%">
        <template #icon><download-outlined /></template>
        {{ $t('label.acl.export') }}
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
              <drag-outlined />
            </div>
            <div class="list__container">
              <div class="list__col">
                <div class="list__label">{{ $t('label.rule.number') }}</div>
                <div>{{ acl.number }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('label.cidrlist') }}</div>
                <div>{{ acl.cidrlist }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('label.action') }}</div>
                <div>{{ acl.action }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('label.protocol') }}</div>
                <div>{{ acl.protocol }}</div>
              </div>
              <div class="list__col" v-if="acl.startport">
                <div class="list__label">{{ $t('label.startport') }}</div>
                <div>{{ acl.startport }}</div>
              </div>
              <div class="list__col" v-if="acl.endport">
                <div class="list__label">{{ $t('label.endport') }}</div>
                <div>{{ acl.endport }}</div>
              </div>
              <div class="list__col" v-if="acl.icmpcode">
                <div class="list__label">{{ $t('label.icmpcode') }}</div>
                <div>{{ acl.icmpcode }}</div>
              </div>
              <div class="list__col" v-if="acl.icmptype">
                <div class="list__label">{{ $t('label.icmptype') }}</div>
                <div>{{ acl.icmptype }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('label.traffictype') }}</div>
                <div>{{ acl.traffictype }}</div>
              </div>
              <div class="list__col">
                <div class="list__label">{{ $t('label.description') }}</div>
                <div>{{ acl.reason }}</div>
              </div>
            </div>
            <div class="list__actions">
              <tooltip-button :tooltip="$t('label.tags')" icon="tag-outlined" @onClick="() => openTagsModal(acl)" />
              <tooltip-button :tooltip="$t('label.edit')" icon="edit-outlined" @onClick="() => openEditRuleModal(acl)" />
              <tooltip-button :tooltip="$t('label.delete')" icon="delete-outlined" type="danger" :disabled="!('deleteNetworkACL' in $store.getters.apis)" @onClick="() => handleDeleteRule(acl.id)" />
            </div>
          </div>
        </transition-group>
      </draggable>
    </div>

    <a-modal :title="$t('label.edit.tags')" v-model="tagsModalVisible" :footer="null" :maskClosable="false">
      <a-spin v-if="tagsLoading"></a-spin>

      <div v-else>
        <a-form :ref="tagRef" :model="newTagsForm" :rules="tagRules" class="add-tags">
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.key') }}</p>
            <a-form-item ref="key" name="key">
              <a-input
                autoFocus
                v-model:value="newTagsForm.key" />
            </a-form-item>
          </div>
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.value') }}</p>
            <a-form-item  ref="value" name="value">
              <a-input v-model:value="newTagsForm.value" />
            </a-form-item>
          </div>
          <a-button type="primary" @click="handleAddTag">{{ $t('label.add') }}</a-button>
        </a-form>

        <a-divider style="margin-top: 0;"></a-divider>

        <div class="tags-container">
          <div class="tags" v-for="(tag, index) in tags" :key="index">
            <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
              {{ tag.key }} = {{ tag.value }}
            </a-tag>
          </div>
        </div>

        <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.ok') }}</a-button>
      </div>

    </a-modal>
    <a-modal :title="ruleModalTitle" :maskClosable="false" v-model="ruleModalVisible" @ok="handleRuleModalForm">
      <a-form :ref="ruleFormRef" :model="ruleForm" :rules="ruleFormRules" @submit="handleRuleModalForm">
        <a-form-item :label="$t('label.number')" ref="number" name="number">
          <a-input-number autoFocus style="width: 100%" v-model:value="form.number" />
        </a-form-item>
        <a-form-item :label="$t('label.cidrlist')" ref="cidrlist" name="cidrlist">
          <a-input v-model:value="form.cidrlist" />
        </a-form-item>
        <a-form-item :label="$t('label.action')" ref="action" name="action">
          <a-select v-model:value="form.action">
            <a-select-option value="allow">{{ $t('label.allow') }}</a-select-option>
            <a-select-option value="deny">{{ $t('label.deny') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.protocol')" ref="protocol" name="protocol">
          <a-select v-model:value="form.protocol">
            <a-select-option value="tcp">{{ capitalise($t('label.tcp')) }}</a-select-option>
            <a-select-option value="udp">{{ capitalise($t('label.udp')) }}</a-select-option>
            <a-select-option value="icmp">{{ capitalise($t('label.icmp')) }}</a-select-option>
            <a-select-option value="all">{{ $t('label.all') }}</a-select-option>
            <a-select-option value="protocolnumber">{{ $t('label.protocol.number') }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="ruleForm.protocol === 'protocolnumber'"
          :label="$t('label.protocolnumber')"
          ref="protocolnumber"
          name="protocolnumber">
          <a-input v-model:value="form.protocolnumber" />
        </a-form-item>

        <div v-if="['icmp', 'protocolnumber'].includes(ruleForm.protocol)">
          <a-form-item :label="$t('label.icmptype')" ref="icmptype" name="icmptype">
            <a-input v-model:value="form.icmptype" :placeholder="$t('icmp.type.desc')" />
          </a-form-item>
          <a-form-item :label="$t('label.icmpcode')" ref="icmpcode" name="icmpcode">
            <a-input v-model:value="form.icmpcode" :placeholder="$t('icmp.code.desc')" />
          </a-form-item>
        </div>

        <div v-show="['tcp', 'udp', 'protocolnumber'].includes(ruleForm.protocol)">
          <a-form-item :label="$t('label.startport')" ref="startport" name="startport">
            <a-input-number style="width: 100%" v-model:value="form.startport" />
          </a-form-item>
          <a-form-item :label="$t('label.endport')" ref="endport" name="endport">
            <a-input-number style="width: 100%" v-model:value="form.endport" />
          </a-form-item>
        </div>

        <a-form-item :label="$t('label.traffictype')" ref="traffictype" name="traffictype">
          <a-select v-model:value="form.traffictype">
            <a-select-option value="ingress">{{ $t('label.ingress') }}</a-select-option>
            <a-select-option value="egress">{{ $t('label.egress') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('label.description')" ref="reason" name="reason">
          <a-textarea
            v-model:value="form.reason"
            :autosize="{ minRows: 2 }"
            :placeholder="$t('label.acl.reason.description')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { api } from '@/api'
import draggable from 'vuedraggable'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'AclListRulesTab',
  components: {
    draggable,
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
    }
  },
  data () {
    return {
      acls: [],
      fetchLoading: false,
      tags: [],
      selectedAcl: null,
      tagsModalVisible: false,
      tagsLoading: false,
      ruleModalVisible: false,
      ruleModalTitle: this.$t('label.edit.rule'),
      ruleFormMode: 'edit'
    }
  },
  created () {
    this.initForm()
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
    initForm () {
      this.tagRef = ref()
      this.ruleFormRef = ref()
      this.newTagsForm = reactive({})
      this.ruleForm = reactive({})
      this.tagRules = reactive({
        key: [{ required: true, message: this.$t('message.specifiy.tag.key') }],
        value: [{ required: true, message: this.$t('message.specifiy.tag.value') }]
      })
      this.ruleFormRules = reactive({
        protocolnumber: [{ required: true, message: this.$t('label.required') }]
      })
    },
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
        this.$notifyError(error)
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
        this.$notifyError(error)
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
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          title: this.$t('message.delete.tag.for.networkacl'),
          description: `${tag.key} = ${tag.value}`,
          successMessage: this.$t('message.success.delete.tag'),
          successMethod: () => {
            this.fetchTags(this.selectedAcl)
            this.tagsLoading = false
          },
          errorMessage: this.$t('message.delete.tag.failed'),
          errorMethod: () => {
            this.fetchTags(this.selectedAcl)
            this.tagsLoading = false
          },
          loadingMessage: this.$t('message.delete.tag.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchTags(this.selectedAcl)
            this.tagsLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.tagsLoading = false
      })
    },
    handleAddTag () {
      this.tagsLoading = true

      this.tagRef.value.validate().then(() => {
        const values = toRaw(this.newTagsForm)

        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedAcl.id,
          resourceType: 'NetworkACL'
        }).then(response => {
          this.$pollJob({
            jobId: response.createtagsresponse.jobid,
            title: this.$t('message.add.tag.for.networkacl'),
            description: `${values.key} = ${values.value}`,
            successMessage: this.$t('message.success.add.tag'),
            successMethod: () => {
              this.fetchTags(this.selectedAcl)
              this.tagsLoading = false
            },
            errorMessage: this.$t('message.add.tag.failed'),
            errorMethod: () => {
              this.fetchTags(this.selectedAcl)
              this.tagsLoading = false
            },
            loadingMessage: this.$t('message.add.tag.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchTags(this.selectedAcl)
              this.tagsLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
          this.tagsLoading = false
        })
      }).finally(() => {
        this.tagsLoading = false
      })
    },
    openEditRuleModal (acl) {
      const self = this
      this.ruleModalTitle = this.$t('label.edit.rule')
      this.ruleFormMode = 'edit'
      this.ruleFormRef.value.resetFields()
      this.ruleModalVisible = true
      this.selectedAcl = acl
      setTimeout(() => {
        self.ruleForm.number = acl.number
        self.ruleForm.cidrlist = acl.cidrlist
        self.ruleForm.action = acl.action
        self.ruleForm.protocol = acl.protocol
        self.ruleForm.startport = acl.startport
        self.ruleForm.endport = acl.endport
        self.ruleForm.traffictype = acl.traffictype
        self.ruleForm.reason = acl.reason
      }, 200)
    },
    getDataFromForm (values) {
      const data = {
        cidrlist: values.cidrlist || '',
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

      return data
    },
    handleEditRule () {
      this.ruleFormRef.value.validate().then(() => {
        const values = toRaw(this.ruleForm)
        this.fetchLoading = true
        this.ruleModalVisible = false

        const data = this.getDataFromForm(values)
        data.id = this.selectedAcl.id
        data.partialupgrade = false

        api('updateNetworkACLItem', {}, 'POST', data).then(response => {
          this.$pollJob({
            jobId: response.createnetworkaclresponse.jobid,
            title: this.$t('label.edit.acl.rule'),
            description: this.selectedAcl.id,
            successMessage: this.$t('message.success.edit.acl'),
            successMethod: () => {
              this.fetchData()
              this.fetchLoading = false
            },
            errorMessage: this.$t('message.edit.acl.failed'),
            errorMethod: () => {
              this.fetchData()
              this.fetchLoading = false
            },
            loadingMessage: this.$t('message.edit.acl.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchData()
              this.fetchLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
          this.fetchLoading = false
        })
      })
    },
    handleDeleteRule (id) {
      this.fetchLoading = true
      api('deleteNetworkACL', { id }).then(response => {
        this.$pollJob({
          jobId: response.deletenetworkaclresponse.jobid,
          title: this.$t('message.delete.acl.rule'),
          description: id,
          successMessage: this.$t('message.success.delete.acl.rule'),
          successMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          errorMessage: this.$t('message.delete.acl.rule.failed'),
          errorMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          loadingMessage: this.$t('message.delete.acl.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
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
      this.ruleModalTitle = this.$t('label.add.rule')
      this.ruleModalVisible = true
      this.ruleFormMode = 'add'
      this.ruleFormRef.value.resetFields()
      setTimeout(() => {
        this.ruleForm.action = 'allow'
        this.ruleForm.protocol = 'tcp'
        this.ruleForm.traffictype = 'ingress'
      }, 200)
    },
    handleAddRule (e) {
      this.ruleFormRef.value.validate().then(() => {
        const values = toRaw(this.ruleForm)
        this.fetchLoading = true
        this.ruleModalVisible = false

        const data = this.getDataFromForm(values)
        data.aclid = this.resource.id

        api('createNetworkACL', {}, 'POST', data).then(() => {
          this.$notification.success({
            message: this.$t('label.success'),
            description: this.$t('message.success.add.rule')
          })
        }).catch(error => {
          this.$notifyError(error)
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
        this.$pollJob({
          jobId: response.moveNetworkAclItemResponse.jobid,
          title: this.$t('message.move.acl.order'),
          description: id,
          successMessage: this.$t('message.success.move.acl.order'),
          successMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          errorMessage: this.$t('message.move.acl.order.failed'),
          errorMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          },
          loadingMessage: this.$t('message.move.acl.order.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.fetchLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
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
    },
    capitalise (val) {
      if (val === 'all') return this.$t('label.all')
      return val.toUpperCase()
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
