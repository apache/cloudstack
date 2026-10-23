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
    <div class="form" v-ctrl-enter="submitData">

      <div v-if="loading" class="loading">
        <loading-outlined style="color: #1890ff;" />
      </div>

      <ownership-selection @fetch-owner="fetchOwnerOptions" accountId/>

      <div class="submit-btn">
        <a-button @click="closeAction">
          {{ $t('label.cancel') }}
        </a-button>
        <a-button type="primary" @click="submitData" ref="submit">
          {{ $t('label.submit') }}
        </a-button>
      </div>

    </div>

  </div>
</template>

<script>
import { postAPI } from '@/api'
import ResourceIcon from '@/components/view/ResourceIcon'
import OwnershipSelection from '@/views/compute/wizard/OwnershipSelection.vue'

export default {
  name: 'AssignVolume',
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  components: {
    ResourceIcon,
    OwnershipSelection
  },
  inject: ['parentFetchData'],
  data () {
    return {
      domains: [],
      accounts: [],
      projects: [],
      selectedAccountType: 'Account',
      selectedDomain: null,
      selectedAccount: null,
      selectedAccountId: null,
      selectedProject: null,
      accountError: false,
      projectError: false,
      loading: false
    }
  },
  methods: {
    fetchOwnerOptions (selectedOptions) {
      this.selectedAccountType = selectedOptions.selectedAccountType
      this.selectedAccount = selectedOptions.selectedAccount
      this.selectedAccountId = selectedOptions.selectedAccountId
      this.selectedDomain = selectedOptions.selectedDomain
      this.selectedProject = selectedOptions.selectedProject
    },
    closeAction () {
      this.$emit('close-action')
    },
    submitData () {
      if (this.loading) return
      let variableKey = ''
      let variableValue = ''

      if (this.selectedAccountType === 'Account') {
        if (!this.selectedAccountId) {
          this.accountError = true
          return
        }
        variableKey = 'accountid'
        variableValue = this.selectedAccountId
      } else if (this.selectedAccountType === 'Project') {
        if (!this.selectedProject) {
          this.projectError = true
          return
        }
        variableKey = 'projectid'
        variableValue = this.selectedProject
      }

      this.loading = true
      postAPI('assignVolume', {
        response: 'json',
        volumeid: this.resource.id,
        [variableKey]: variableValue,
        ignoreproject: true
      }).then(() => {
        this.$notification.success({
          message: this.$t('message.success.assign.volume')
        })
        this.$emit('close-action')
        this.parentFetchData()
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>

<style scoped lang="scss">
.form {
  width: 85vw;

  @media (min-width: 760px) {
    width: 500px;
  }

  display: flex;
  flex-direction: column;

  &__item {
    display: flex;
    flex-direction: column;
    width: 100%;
    margin-bottom: 10px;
  }

  &__label {
    display: flex;
    font-weight: bold;
    margin-bottom: 5px;
  }

}

.submit-btn {
  margin-top: 10px;
  align-self: flex-end;

  button {
    margin-left: 10px;
  }
}

.loading {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
}
</style>
