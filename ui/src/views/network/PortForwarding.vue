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
  <div>
    <div>
      <div class="form">
        <div class="form__item">
          <div class="form__label">{{ $t('privateport') }}</div>
          <a-input-group class="form__item__input-container" compact>
            <a-input
              v-model="newRule.privateport"
              placeholder="Start"
              style="border-right: 0; width: 60px; margin-right: 0;"></a-input>
            <a-input
              placeholder="-"
              disabled
              style="width: 30px; border-left: 0; border-right: 0; pointer-events: none; backgroundColor: #fff; text-align:
              center; margin-right: 0;"></a-input>
            <a-input
              v-model="newRule.privateendport"
              placeholder="End"
              style="border-left: 0; width: 60px; text-align: right; margin-right: 0;"></a-input>
          </a-input-group>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('publicport') }}</div>
          <a-input-group class="form__item__input-container" compact>
            <a-input
              v-model="newRule.publicport"
              placeholder="Start"
              style="border-right: 0; width: 60px; margin-right: 0;"></a-input>
            <a-input
              placeholder="-"
              disabled
              style="width: 30px; border-left: 0; border-right: 0; pointer-events: none; backgroundColor: #fff;
              text-align: center; margin-right: 0;"></a-input>
            <a-input
              v-model="newRule.publicendport"
              placeholder="End"
              style="border-left: 0; width: 60px; text-align: right; margin-right: 0;"></a-input>
          </a-input-group>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('protocol') }}</div>
          <a-select v-model="newRule.protocol" style="width: 100%;">
            <a-select-option value="tcp">{{ $t('tcp') }}</a-select-option>
            <a-select-option value="udp">{{ $t('udp') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item" style="margin-left: auto;">
          <div class="form__label">{{ $t('label.add.VM') }}</div>
          <a-button type="primary" @click="openAddVMModal">{{ $t('add') }}</a-button>
        </div>
      </div>
    </div>

    <a-divider/>

    <a-list :loading="loading" style="min-height: 25px;">
      <a-list-item v-for="rule in portForwardRules" :key="rule.id" class="rule">
        <div class="rule-container">
          <div class="rule__item">
            <div class="rule__title">{{ $t('privateport') }}</div>
            <div>{{ rule.privateport }} - {{ rule.privateendport }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">{{ $t('publicport') }}</div>
            <div>{{ rule.publicport }} - {{ rule.publicendport }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">{{ $t('protocol') }}</div>
            <div>{{ rule.protocol | capitalise }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">{{ $t('state') }}</div>
            <div>{{ rule.state }}</div>
          </div>
          <div class="rule__item">
            <div class="rule__title">{{ $t('vm') }}</div>
            <div class="rule__title"></div>
            <div><a-icon type="desktop"/> <router-link :to="{ path: '/vm/' + rule.virtualmachineid }">{{ rule.virtualmachinename }}</router-link> ({{ rule.vmguestip }})</div>
          </div>
          <div slot="actions">
            <a-button shape="round" icon="tag" class="rule-action" @click="() => openTagsModal(rule.id)" />
            <a-button shape="round" type="danger" icon="delete" class="rule-action" @click="deleteRule(rule)" />
          </div>
        </div>
      </a-list-item>
    </a-list>

    <a-modal title="Edit Tags" v-model="tagsModalVisible" :footer="null" :afterClose="closeModal">
      <span v-show="tagsModalLoading" class="tags-modal-loading">
        <a-icon type="loading"></a-icon>
      </span>

      <div class="add-tags">
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('key') }}</p>
          <a-input v-model="newTag.key"></a-input>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('value') }}</p>
          <a-input v-model="newTag.value"></a-input>
        </div>
        <a-button type="primary" @click="() => handleAddTag()">{{ $t('label.add') }}</a-button>
      </div>

      <a-divider></a-divider>

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('done') }}</a-button>
    </a-modal>

    <a-modal
      title="Add VM"
      v-model="addVmModalVisible"
      class="vm-modal"
      width="60vw"
      @ok="addRule"
      :okButtonProps="{ props:
        {disabled: newRule.virtualmachineid === null } }"
      @cancel="closeModal"
    >

      <a-icon v-if="addVmModalLoading" type="loading"></a-icon>

      <div v-else>
        <div class="vm-modal__header">
          <span style="min-width: 200px;">{{ $t('name') }}</span>
          <span>{{ $t('instancename') }}</span>
          <span>{{ $t('displayname') }}</span>
          <span>{{ $t('ip') }}</span>
          <span>{{ $t('account') }}</span>
          <span>{{ $t('zone') }}</span>
          <span>{{ $t('state') }}</span>
          <span>{{ $t('select') }}</span>
        </div>

        <a-radio-group v-model="newRule.virtualmachineid" style="width: 100%;" @change="fetchNics">
          <div v-for="(vm, index) in vms" :key="index" class="vm-modal__item">

            <span style="min-width: 200px;">
              <span>
                {{ vm.name }}
              </span>
              <a-icon v-if="addVmModalNicLoading" type="loading"></a-icon>
              <a-select
                v-else-if="!addVmModalNicLoading && newRule.virtualmachineid === vm.id"
                v-model="newRule.vmguestip">
                <a-select-option v-for="(nic, nicIndex) in nics" :key="nic" :value="nic">
                  {{ nic }}{{ nicIndex === 0 ? ' (Primary)' : null }}
                </a-select-option>
              </a-select>
            </span>
            <span>{{ vm.instancename }}</span>
            <span>{{ vm.displayname }}</span>
            <span></span>
            <span>{{ vm.account }}</span>
            <span>{{ vm.zonename }}</span>
            <span>{{ vm.state }}</span>
            <a-radio :value="vm.id" />
          </div>
        </a-radio-group>
      </div>

    </a-modal>

  </div>
</template>

<script>
import { api } from '@/api'

export default {
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  data () {
    return {
      loading: true,
      portForwardRules: [],
      newRule: {
        protocol: 'tcp',
        privateport: null,
        privateendport: null,
        publicport: null,
        publicendport: null,
        openfirewall: false,
        vmguestip: null,
        virtualmachineid: null
      },
      tagsModalVisible: false,
      selectedRule: null,
      tags: [],
      newTag: {
        key: null,
        value: null
      },
      tagsModalLoading: false,
      addVmModalVisible: false,
      addVmModalLoading: false,
      addVmModalNicLoading: false,
      vms: [],
      nics: []
    }
  },
  mounted () {
    this.fetchData()
  },
  watch: {
    resource: function (newItem, oldItem) {
      if (!newItem || !newItem.id) {
        return
      }
      this.resource = newItem
      this.fetchData()
    }
  },
  filters: {
    capitalise: val => {
      if (val === 'all') return 'All'
      return val.toUpperCase()
    }
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listPortForwardingRules', {
        listAll: true,
        ipaddressid: this.resource.id
      }).then(response => {
        this.portForwardRules = response.listportforwardingrulesresponse.portforwardingrule
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
      }).finally(() => {
        this.loading = false
      })
    },
    deleteRule (rule) {
      this.loading = true
      api('deletePortForwardingRule', { id: rule.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteportforwardingruleresponse.jobid,
          successMessage: `Successfully removed Port Forwarding rule`,
          successMethod: () => this.fetchData(),
          errorMessage: 'Removing Port Forwarding rule failed',
          errorMethod: () => this.fetchData(),
          loadingMessage: `Deleting Port Forwarding rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => this.fetchData()
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.fetchData()
      })
    },
    addRule () {
      this.loading = true
      this.addVmModalVisible = false
      api('createPortForwardingRule', {
        ...this.newRule,
        ipaddressid: this.resource.id,
        networkid: this.resource.associatednetworkid
      }).then(response => {
        this.$pollJob({
          jobId: response.createportforwardingruleresponse.jobid,
          successMessage: `Successfully added new Port Forwarding rule`,
          successMethod: () => {
            this.closeModal()
            this.fetchData()
          },
          errorMessage: 'Adding new Port Forwarding rule failed',
          errorMethod: () => {
            this.closeModal()
            this.fetchData()
          },
          loadingMessage: `Adding new Port Forwarding rule...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.closeModal()
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.createportforwardingruleresponse.errortext
        })
        this.closeModal()
        this.fetchData()
      })
    },
    resetAllRules () {
      this.newRule.protocol = 'tcp'
      this.newRule.privateport = null
      this.newRule.privateendport = null
      this.newRule.publicport = null
      this.newRule.publicendport = null
      this.newRule.openfirewall = false
      this.newRule.vmguestip = null
      this.newRule.virtualmachineid = null
    },
    resetTagInputs () {
      this.newTag.key = null
      this.newTag.value = null
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.addVmModalVisible = false
      this.newRule.virtualmachineid = null
      this.addVmModalLoading = false
      this.addVmModalNicLoading = false
      this.nics = []
      this.resetTagInputs()
      this.resetAllRules()
    },
    openTagsModal (id) {
      this.tagsModalLoading = true
      this.selectedRule = id
      this.tagsModalVisible = true
      this.tags = []
      this.resetTagInputs()
      api('listTags', {
        resourceId: id,
        resourceType: 'PortForwardingRule',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag
        this.tagsModalLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.closeModal()
      })
    },
    handleAddTag () {
      this.tagsModalLoading = true
      api('createTags', {
        'tags[0].key': this.newTag.key,
        'tags[0].value': this.newTag.value,
        resourceIds: this.selectedRule,
        resourceType: 'PortForwardingRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.createtagsresponse.jobid,
          successMessage: `Successfully added tag`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: 'Failed to add new tag',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          },
          loadingMessage: `Adding tag...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.createtagsresponse.errortext
        })
        this.closeModal()
      })
    },
    handleDeleteTag (tag) {
      this.tagsModalLoading = true
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: this.selectedRule,
        resourceType: 'PortForwardingRule'
      }).then(response => {
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: `Successfully removed tag`,
          successMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: 'Failed to remove tag',
          errorMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          },
          loadingMessage: `Removing tag...`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.deletetagsresponse.errortext
        })
        this.closeModal()
      })
    },
    openAddVMModal () {
      this.addVmModalVisible = true
      this.addVmModalLoading = true
      api('listVirtualMachines', {
        listAll: true,
        page: 1,
        pagesize: 500,
        networkid: this.resource.associatednetworkid,
        account: this.resource.account,
        domainid: this.resource.domainid
      }).then(response => {
        this.vms = response.listvirtualmachinesresponse.virtualmachine
        this.addVmModalLoading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.closeModal()
      })
    },
    fetchNics (e) {
      this.addVmModalNicLoading = true
      api('listNics', {
        virtualmachineid: e.target.value,
        networkid: this.resource.associatednetworkid
      }).then(response => {
        if (!response.listnicsresponse.nic || response.listnicsresponse.nic.length < 1) return
        const nic = response.listnicsresponse.nic[0]
        this.nics.push(nic.ipaddress)
        if (nic.secondaryip && nic.secondaryip.length > 0) {
          this.nics.push(...nic.secondaryip.map(ip => ip.ipaddress))
        }
        this.newRule.vmguestip = this.nics[0]
        this.addVmModalNicLoading = false
      }).catch(error => {
        console.log(error)
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.closeModal()
      })
    }
  }
}
</script>

<style scoped lang="scss">
  .rule {

    &-container {
      display: flex;
      width: 100%;
      flex-wrap: wrap;
      margin-right: -20px;
      margin-bottom: -10px;
    }

    &__item {
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        flex: 1;
      }

    }

    &__title {
      font-weight: bold;
    }

  }

  .add-btn {
    width: 100%;
    padding-top: 15px;
    padding-bottom: 15px;
    height: auto;
  }

  .add-actions {
    display: flex;
    justify-content: flex-end;
    margin-right: -20px;
    margin-bottom: 20px;

    @media (min-width: 760px) {
      margin-top: 20px;
    }

    button {
      margin-right: 20px;
    }

  }

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__item {
      display: flex;
      flex-direction: column;
      /*flex: 1;*/
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        margin-bottom: 0;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

      &__input-container {
        display: flex;

        input {

          &:not(:last-child) {
            margin-right: 10px;
          }

        }

      }

    }

    &__label {
      font-weight: bold;
    }

  }

  .rule-action {
    margin-bottom: 20px;

    &:not(:last-of-type) {
      margin-right: 10px;
    }

  }

  .tags {
    margin-bottom: 10px;
  }

  .add-tags {
    display: flex;
    align-items: flex-end;
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

  .tags-modal-loading {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(0,0,0,0.5);
    z-index: 1;
    color: #1890ff;
    font-size: 2rem;
  }

  .vm-modal {

    &__header {
      display: flex;

      span {
        flex: 1;
        font-weight: bold;
        margin-right: 10px;
      }

    }

    &__item {
      display: flex;
      margin-top: 10px;

      span,
      label {
        display: block;
        flex: 1;
        margin-right: 10px;
      }

    }

  }

</style>
