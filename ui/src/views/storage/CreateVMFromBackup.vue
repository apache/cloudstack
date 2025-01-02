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
  <div v-if="openModal">
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
      openModal: true,
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
    this.dataPreFill.computeofferingid = this.resource.vmdetails.serviceofferingid
    this.dataPreFill.templateid = this.resource.vmdetails.templateid
    this.dataPreFill.backupid = this.resource.id
  },
  methods: {
    closeModal () {
      this.openModal = false
      this.$emit('close-action')
    }
  }
}

</script>

<style lang="scss" scoped>
.form {
  width: 80vw;

  @media (min-width: 500px) {
    min-width: 400px;
    width: 100%;
  }
}
</style>
