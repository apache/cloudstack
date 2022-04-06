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
  <div class="form" v-ctrl-enter="handleKeyboardSubmit">
    <storage-pool-select-view
      ref="selectionView"
      :resource="resource"
      :clusterId="clusterId"
      :suitabilityEnabled="suitabilityEnabled"
      :autoAssignAllowed="autoAssignAllowed"
      @select="handleSelect" />

    <a-divider />

    <div class="actions">
      <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
      <a-button type="primary" ref="submit" :disabled="!selectedStoragePool" @click="submitForm">{{ $t('label.ok') }}</a-button>
    </div>

  </div>
</template>

<script>
import StoragePoolSelectView from '@/components/view/StoragePoolSelectView'

export default {
  name: 'VolumeStoragePoolSelectionForm',
  components: {
    StoragePoolSelectView
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    clusterId: {
      type: String,
      required: false,
      default: null
    },
    suitabilityEnabled: {
      type: Boolean,
      required: false,
      default: false
    },
    autoAssignAllowed: {
      type: Boolean,
      required: false,
      default: false
    },
    isOpen: {
      type: Boolean,
      required: false
    }
  },
  data () {
    return {
      selectedStoragePool: null
    }
  },
  watch: {
    isOpen (newValue) {
      if (newValue) {
        setTimeout(() => {
          this.$refs.selectionView.reset()
        }, 50)
      }
    }
  },
  methods: {
    handleSelect (storagePool) {
      this.selectedStoragePool = storagePool
    },
    closeModal () {
      this.$emit('close-action')
    },
    handleKeyboardSubmit () {
      if (this.selectedStoragePool != null) {
        this.submitForm()
      }
    },
    submitForm () {
      this.$emit('select', this.resource.id, this.selectedStoragePool)
      this.closeModal()
    }
  }
}
</script>

<style scoped lang="scss">
  .form {
    width: 80vw;

    @media (min-width: 900px) {
      width: 850px;
    }
  }

  .top-spaced {
    margin-top: 20px;
  }

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
