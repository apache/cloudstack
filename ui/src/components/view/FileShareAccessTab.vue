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
  <a-card :title="$t('label.mount.fileshare')">
    <div class="title">{{ $t('label.nfs.mount') }}</div>
    <div class="content">{{ $t('message.ip.address') }} {{ resource.ipaddress }}</div>
    <div class="content">{{ $t('message.path') }} {{ resource.path }}</div>
  </a-card>
</template>

<script>
export default {
  name: 'FileShareAccessTab.vue',
  props: {
    resource: {
      type: Object,
      required: true
    }
  }
}
</script>

<style lang="css" scoped>
.title {
  font-weight: bold;
  margin-bottom: 14px;
  font-size: 16px; /* Increased font size */
}

.content {
  font-size: 16px; /* Increased font size */
}
</style>
