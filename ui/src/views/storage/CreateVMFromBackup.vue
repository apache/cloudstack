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
  <div v-if="!configure">
            <div class="card-footer">
              <a-button @click="closeAction">
                {{ $t('label.cancel') }}
              </a-button>
              <a-button @click="setConfigure">
                {{ $t('label.configure.instance') }}
              </a-button>
              <a-button style="margin-left: 10px" type="primary" ref="submit" @click="handleSubmit">
                {{ $t('label.ok') }}
              </a-button>
            </div>
  </div>
  <div v-else>
    <DeployVMFromBackup
      :preFillContent="dataPreFill"
      @close-action="closeModal"/>
  </div>
</template>

<script>

import DeployVMFromBackup from '@/views/compute/DeployVMFromBackup'

export default {
  name: 'CreateVMFromBackup',
  components: {
    DeployVMFromBackup
  },
  data () {
    return {
      configure: false,
      dataPreFill: {}
    }
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  created () {
    this.dataPreFill.backupid = this.resource.id
    this.dataPreFill.computeofferingid = this.resource.vmdetails.serviceofferingid
    this.dataPreFill.templateid = this.resource.vmdetails.templateid
    this.dataPreFill.networkids = (this.resource.vmdetails.networkids || '').split(',').map(item => item.trim())
    this.diskofferingids = (this.resource.vmdetails.diskofferingids || '').split(',').map(item => item.trim())
    console.log(this.diskofferingids)
    const volumes = JSON.parse(this.resource.volumes).slice(1)
    const result = volumes.map((volume, index) => ({
      id: index,
      name: volume.uuid,
      size: volume.size / (1024 * 1024 * 1024), // Convert size from bytes to GB
      diskofferingid: this.diskofferingids[index]
    }))
    console.log(result)
    this.dataPreFill.diskofferingids = result
  },
  methods: {
    setConfigure () {
      this.configure = true
    },
    closeModal () {
      this.$emit('close-action')
    }
  }
}

</script>

<style lang="scss" scoped>
  .card-footer {
    text-align: right;
    margin-top: 2rem;

    button + button {
      margin-left: 8px;
    }
  }

.form {
  width: 80vw;

  @media (min-width: 1000px) {
    min-width: 1000px;
    width: 100%;
  }
}
</style>
