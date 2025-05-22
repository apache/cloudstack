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
  <div class="button-container">
    <a-button
      v-if="cancekButtonAllowed"
      @click="handleCancelBtn"
      :disabled="loading"
      class="equal-size-button">
      {{ $t('label.cancel') }}
    </a-button>
    <a-dropdown-button
      v-if="deployButtonMenuOptions"
      type="primary"
      ref="submit"
      @click="handleDeployBtn"
      :loading="loading"
      class="equal-size-button">
      <rocket-outlined />
      {{ deployButtonText }}
      <template #icon><down-outlined /></template>
      <template #overlay>
          <a-menu type="primary" @click="handleMenu" theme="dark" class="btn-stay-on-page">
            <a-menu-item type="primary" v-for="(menuOpt, index) in deployButtonMenuOptions" :key="index + 1">
                <rocket-outlined />
                {{ menuOpt }}
            </a-menu-item>
          </a-menu>
      </template>
    </a-dropdown-button>
    <a-button
      v-else
      class="equal-size-button"
      type="primary"
      :loading="loading"
      @click="handleDeployBtn">
      {{ deployButtonText }}
    </a-button>
  </div>
</template>

<script>
export default {
  name: 'DeployButtons',
  components: {
  },
  props: {
    loading: {
      type: Boolean,
      default: false
    },
    deployButtonText: {
      type: String,
      default: () => this.$t('label.create')
    },
    deployButtonMenuOptions: {
      type: Array,
      default: null
    },
    cancekButtonAllowed: {
      type: Boolean,
      default: false
    }
  },
  emits: ['handle-cancel', 'handle-deploy', 'handle-deploy-menu'],
  methods: {
    handleCancelBtn () {
      this.$emit('handle-cancel')
    },
    handleDeployBtn (e) {
      this.$emit('handle-deploy', e)
    },
    handleMenu (e) {
      this.$emit('handle-deploy-menu', e.key - 1)
    }
  }
}
</script>

<style lang="less" scoped>

.button-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-start;
}

.equal-size-button {
  flex-grow: 1; /* Make each button grow equally */
  min-width: 120px; /* Set a minimum width so that the buttons don't shrink too much */
}

@media (max-width: 600px) {
  .button-container {
    flex-direction: column;
  }

}

.btn-stay-on-page {
  &.ant-dropdown-menu-dark {
    .ant-dropdown-menu-item:hover {
      background: transparent !important;
    }
  }
}
</style>

<style lang="less">

.ant-btn-group > .ant-btn:first-child:not(:last-child) {
  flex-grow: 1; /* Make each button grow equally */
}
</style>
