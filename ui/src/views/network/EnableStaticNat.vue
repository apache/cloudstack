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
  <a-list class="list" :loading="loading">
    <div slot="header" class="list__header">
      <a-input-search
        placeholder="Search"
        v-model="searchQuery"
        @search="fetchData" />
    </div>

    <a-list-item
      v-for="vm in vmsList"
      :key="vm.id"
      class="list__item"
      :class="{ 'list__item--selected' : selectedVm && selectedVm.id === vm.id }">

      <div class="list__outer-container">
        <div class="list__container" @click="() => handleSelectItem(vm)">
          <div class="list__row">
            <div class="list__title">{{ $t('name') }}</div>
            <div>{{ vm.name }}</div>
          </div>
          <div class="list__row">
            <div class="list__title">{{ $t('instancename') }}</div>
            <div>{{ vm.instancename }}</div>
          </div>
          <div class="list__row">
            <div class="list__title">{{ $t('displayname') }}</div>
            <div>{{ vm.displayname }}</div>
          </div>
          <div class="list__row">
            <div class="list__title">{{ $t('account') }}</div>
            <div>{{ vm.account }}</div>
          </div>
          <div class="list__row">
            <div class="list__title">{{ $t('zonenamelabel') }}</div>
            <div>{{ vm.zonename }}</div>
          </div>
          <div class="list__row">
            <div class="list__title">{{ $t('state') }}</div>
            <div>{{ vm.state }}</div>
          </div>
          <a-radio
            class="list__radio"
            :checked="selectedVm && selectedVm.id === vm.id"
            @change="fetchNics"></a-radio>
        </div>

        <a-select
          v-if="nicsList.length && selectedVm && selectedVm.id === vm.id"
          class="nic-select"
          :defaultValue="selectedNic.ipaddress">
          <a-select-option
            @click="selectedNic = item"
            v-for="item in nicsList"
            :key="item.id">
            {{ item.ipaddress }}
          </a-select-option>
        </a-select>
      </div>

    </a-list-item>

    <div slot="footer" class="list__footer">
      <a-button @click="handleClose">{{ $t('cancel') }}</a-button>
      <a-button @click="handleSubmit" type="primary" :disabled="!selectedVm || !selectedNic">{{ $t('ok') }}</a-button>
    </div>

  </a-list>
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
  inject: ['parentFetchData'],
  data () {
    return {
      loading: false,
      vmsList: [],
      selectedVm: null,
      nicsList: [],
      searchQuery: null,
      selectedNic: null
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('listVirtualMachines', {
        page: 1,
        pageSize: 500,
        listAll: true,
        networkid: this.resource.associatednetworkid,
        account: this.resource.account,
        domainid: this.resource.domainid,
        keyword: this.searchQuery
      }).then(response => {
        this.vmsList = response.listvirtualmachinesresponse.virtualmachine
        this.loading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.loading = false
      })
    },
    fetchNics () {
      this.loading = true
      this.nicsList = []
      api('listNics', {
        virtualmachineid: this.selectedVm.id,
        networkid: this.resource.associatednetworkid
      }).then(response => {
        this.nicsList = response.listnicsresponse.nic

        let secondaryIps = this.nicsList.map(item => item.secondaryip)

        if (secondaryIps[0]) {
          secondaryIps = secondaryIps[0]
          this.nicsList = [...this.nicsList, ...secondaryIps]
        }

        this.selectedNic = this.nicsList[0]
        this.loading = false
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.loading = false
      })
    },
    handleSelectItem (vm) {
      this.selectedVm = vm
      this.fetchNics()
    },
    handleSubmit () {
      this.loading = true
      api('enableStaticNat', {
        ipaddressid: this.resource.id,
        virtualmachineid: this.selectedVm.id,
        vmguestip: this.selectedNic.ipaddress
      }).then(() => {
        this.parentFetchData()
        this.loading = false
        this.handleClose()
      }).catch(error => {
        this.$notification.error({
          message: `Error ${error.response.status}`,
          description: error.response.data.errorresponse.errortext
        })
        this.loading = false
        this.handleClose()
      })
    },
    handleClose () {
      this.$parent.$parent.close()
    }
  }
}
</script>

<style scoped lang="scss">

  .list {
    max-height: 95vh;
    width: 95vw;
    overflow-y: scroll;
    margin: -24px;

    @media (min-width: 1000px) {
      max-height: 70vh;
      width: 60vw;
    }

    &__header,
    &__footer {
      padding-right: 20px;
      padding-left: 20px;
    }

    &__footer {
      display: flex;
      justify-content: flex-end;

      button {
        &:not(:last-child) {
          margin-right: 10px;
        }
      }
    }

    &__item {
      padding-right: 20px;
      padding-left: 20px;

      &--selected {
        background-color: #e6f7ff;
      }

    }

    &__title {
      font-weight: bold;
    }

    &__outer-container {
      width: 100%;
      display: flex;
      flex-direction: column;
    }

    &__container {
      display: flex;
      flex-direction: column;
      width: 100%;
      cursor: pointer;

      @media (min-width: 760px) {
        flex-direction: row;
        align-items: center;
      }

    }

    &__row {
      margin-bottom: 10px;

      @media (min-width: 760px) {
        margin-right: 20px;
        margin-bottom: 0;
      }
    }

    &__radio {

      @media (min-width: 760px) {
        margin-left: auto;
      }

    }

  }

  .nic-select {
    margin-top: 10px;
    margin-right: auto;
    min-width: 150px;
  }
</style>
