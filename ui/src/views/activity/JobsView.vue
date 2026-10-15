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
  <autogen-view v-if="$route.params.id" />
  <a-card v-else class="jobs-view">
    <async-jobs-tab />
  </a-card>
</template>

<script>
import AsyncJobsTab from '@/views/infra/AsyncJobsTab'
import AutogenView from '@/views/AutogenView'

export default {
  name: 'JobsView',
  components: {
    AsyncJobsTab,
    AutogenView
  }
}
</script>

<style scoped lang="less">
.jobs-view {
  margin: 12px 0;
}
</style>
