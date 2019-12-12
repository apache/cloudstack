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
  <a-list :dataSource="hosts" itemLayout="vertical" class="list" :loading="loading">
    <div slot="header" class="list__header">
      <a-input-search
        placeholder="Search"
        v-model="searchQuery"
        @search="fetchData" />
    </div>
    <a-list-item
      slot="renderItem"
      slot-scope="host, index"
      class="host-item"
      :class="{ 'host-item--selected' : selectedIndex === index }"
    >
      <div class="host-item__row">
        <div class="host-item__value">
          <span class="host-item__title">{{ $t('name') }}</span>
          {{ host.name }}
        </div>
        <div class="host-item__value host-item__value--small">
          <span class="host-item__title">Suitability</span>
          <a-icon
            class="host-item__suitability-icon"
            type="check-circle"
            theme="twoTone"
            twoToneColor="#52c41a"
            v-if="host.suitableformigration" />
          <a-icon
            class="host-item__suitability-icon"
            type="close-circle"
            theme="twoTone"
            twoToneColor="#f5222d"
            v-else />
        </div>
        <div class="host-item__value host-item__value--full">
          <span class="host-item__title">{{ $t('cpuused') }}</span>
          {{ host.cpuused }}
        </div>
        <div class="host-item__value">
          <span class="host-item__title">{{ $t('memused') }}</span>
          {{ host.memoryused | byteToGigabyte }} GB
        </div>
        <a-radio
          class="host-item__radio"
          @click="selectedIndex = index"
          :checked="selectedIndex === index"
          :disabled="!host.suitableformigration"></a-radio>
      </div>
    </a-list-item>
    <div slot="footer" class="list__footer">
      <a-button type="primary" :disabled="selectedIndex === null" @click="submitForm">
        {{ $t('OK') }}
      </a-button>
    </div>
  </a-list>
</template>

<script>
import { api } from '@/api'
import { pollActionCompletion } from '@/utils/methods'

export default {
  name: 'VMMigrateWizard',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  data () {
    return {
      loading: true,
      hosts: [],
      selectedIndex: null,
      searchQuery: ''
    }
  },
  mounted () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.loading = true
      api('findHostsForMigration', {
        virtualmachineid: this.resource.id,
        keyword: this.searchQuery,
        page: 1,
        pagesize: 500
      }).then(response => {
        this.hosts = response.findhostsformigrationresponse.host
        this.loading = false
      }).catch(error => {
        this.$message.error('Failed to load hosts: ' + error)
      })
    },
    submitForm () {
      this.loading = true
      api('migrateVirtualMachine', {
        hostid: this.hosts[this.selectedIndex].id,
        virtualmachineid: this.resource.id
      }).then(response => {
        this.$store.dispatch('AddAsyncJob', {
          title: `Migrating ${this.resource.name}`,
          jobid: response.migratevirtualmachineresponse.jobid,
          description: this.resource.name,
          status: 'progress'
        })
        pollActionCompletion({
          jobId: response.migratevirtualmachineresponse.jobid,
          successMessage: `Migration completed successfully for ${this.resource.name}`,
          successMethod: () => {
            this.$parent.$parent.close()
          },
          errorMessage: 'Migration failed',
          errorMethod: () => {
            this.$parent.$parent.close()
          },
          loadingMessage: `Migration in progress for ${this.resource.name}`,
          catchMessage: 'Error encountered while fetching async job result',
          catchMethod: () => {
            this.$parent.$parent.close()
          }
        })
        this.$parent.$parent.close()
      }).catch(error => {
        console.error(error)
        this.$message.error('Failed to migrate host.')
      })
    }
  },
  filters: {
    byteToGigabyte: value => {
      if (!value) return ''
      value = value / Math.pow(10, 9)
      return value.toFixed(2)
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
      padding-left: 20px;
      padding-right: 20px;
    }

    &__footer {
      display: flex;
      justify-content: flex-end;
    }

  }

  .host-item {
    padding-right: 20px;
    padding-bottom: 0;
    padding-left: 20px;

    &--selected {
      background-color: #e6f7ff;
    }

    &__row {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 760px) {
        flex-direction: row;
      }

    }

    &__value {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      flex: 1;
      margin-bottom: 10px;

      &--small {

        @media (min-width: 760px) {
          flex: none;
          margin-right: 40px;
          margin-left: 40px;
        }

      }

    }

    &__title {
      font-weight: bold;
    }

    &__suitability-icon {
      margin-top: 5px;
    }

    &__radio {
      display: flex;
      align-items: center;
    }

  }
</style>
