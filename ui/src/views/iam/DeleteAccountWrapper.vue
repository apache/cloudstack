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
    <!-- Show error message if account is not disabled -->
    <template v-if="resource.state !== 'disabled'">
      <div style="margin-bottom: 10px">
        <a-alert type="error">
          <template #message>
            <div>{{ $t('message.delete.account.not.disabled') }}</div>
          </template>
        </a-alert>
      </div>
      <div :span="24" class="actions">
        <a-button type="primary" @click="closeModal">{{ $t('label.ok') }}</a-button>
      </div>
    </template>
    <!-- Show delete form if account is disabled -->
    <delete-account
      v-else
      :resource="resource"
      @close-action="closeModal" />
  </div>
</template>

<script>
import DeleteAccount from './DeleteAccount.vue'

export default {
  name: 'DeleteAccountWrapper',
  components: {
    DeleteAccount
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  methods: {
    closeModal () {
      this.$emit('close-action')
    }
  }
}
</script>

<style lang="scss" scoped>
.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  button {
    &:not(:last-child) {
      margin-right: 10px;
    }
  }
}
</style>
