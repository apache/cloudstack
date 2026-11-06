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
    <a-button
      v-if="'createKubernetesClusterNodeGroup' in $store.getters.apis"
      type="dashed"
      style="width: 100%"
      :disabled="!canManage"
      @click="openModal('create')">
      <template #icon><plus-outlined /></template>
      {{ $t('label.kubernetes.node.group.create') }}
    </a-button>
    <a-table
      class="table"
      size="small"
      :columns="columns"
      :dataSource="nodeGroups"
      :loading="loading || fetchLoading"
      :rowKey="item => item.id"
      :pagination="false">
      <template #expandedRowRender="{ record: nodeGroup }">
        <a-table
          size="small"
          tableLayout="fixed"
          :columns="nodeColumns"
          :dataSource="nodeGroup.nodes || []"
          :rowKey="item => item.id"
          :pagination="false">
          <template #bodyCell="{ column, text, record: node }">
            <template v-if="column.key === 'name'">
              <router-link :to="{ path: '/vm/' + node.id }">{{ text }}</router-link>
            </template>
            <template v-if="column.key === 'state'">
              <status :text="text ? text : ''" displayText />
            </template>
            <template v-if="column.key === 'nodelabels'">
              <a-tag v-for="label in nodeLabels(nodeGroup, node)" :key="label.text" :color="label.own ? 'blue' : undefined" class="pair-tag">
                {{ label.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'nodetaints'">
              <a-tag v-for="taint in nodeTaints(nodeGroup, node)" :key="taint.text" :color="taint.own ? 'blue' : undefined" class="pair-tag">
                {{ taint.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'actions'">
              <a-tooltip placement="bottom" v-if="'updateKubernetesClusterNode' in $store.getters.apis">
                <template #title>{{ $t('label.kubernetes.node.update') }}</template>
                <a-button shape="circle" size="small" :disabled="!canManageNodeGroup(nodeGroup)" @click="openNodeModal(nodeGroup, node)">
                  <template #icon><edit-outlined /></template>
                </a-button>
              </a-tooltip>
            </template>
          </template>
        </a-table>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'name'">
          {{ text }}
          <a-tag v-if="record.isdefault" color="blue" class="default-tag">{{ $t('label.default') }}</a-tag>
        </template>
        <template v-if="column.key === 'state'">
          <status :text="text ? text : ''" displayText />
        </template>
        <template v-if="column.key === 'autoscalingenabled'">
          <span v-if="record.autoscalingenabled">{{ record.minsize }} – {{ record.maxsize }}</span>
          <span v-else>{{ $t('label.disabled') }}</span>
        </template>
        <template v-if="column.key === 'nodelabels'">
          <a-tag v-for="(value, key) in (record.nodelabels || {})" :key="key" class="pair-tag">{{ key }}={{ value }}</a-tag>
        </template>
        <template v-if="column.key === 'nodetaints'">
          <a-tag v-for="taint in (record.nodetaints || [])" :key="taint" class="pair-tag">{{ taint }}</a-tag>
        </template>
        <template v-if="column.key === 'templatename'">
          {{ text || $t('label.default') }}
        </template>
        <template v-if="column.key === 'created'">
          {{ $toLocaleDate(text) }}
        </template>
        <template v-if="column.key === 'actions'">
          <a-space>
            <a-tooltip placement="bottom" v-if="'scaleKubernetesClusterNodeGroup' in $store.getters.apis">
              <template #title>{{ $t('label.kubernetes.node.group.scale') }}</template>
              <a-button shape="circle" :disabled="!canManageNodeGroup(record)" @click="openModal('scale', record)">
                <template #icon><arrows-alt-outlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip placement="bottom" v-if="'updateKubernetesClusterNodeGroup' in $store.getters.apis">
              <template #title>{{ $t('label.kubernetes.node.group.update') }}</template>
              <a-button shape="circle" :disabled="!canManageNodeGroup(record)" @click="openModal('update', record)">
                <template #icon><edit-outlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip placement="bottom" v-if="'deleteKubernetesClusterNodeGroup' in $store.getters.apis && !record.isdefault">
              <template #title>{{ $t('label.kubernetes.node.group.delete') }}</template>
              <a-popconfirm
                :title="$t('message.kubernetes.node.group.delete')"
                :okText="$t('label.yes')"
                :cancelText="$t('label.no')"
                :disabled="!canManageNodeGroup(record)"
                @confirm="deleteNodeGroup(record)">
                <a-button type="primary" danger shape="circle" :disabled="!canManageNodeGroup(record)">
                  <template #icon><delete-outlined /></template>
                </a-button>
              </a-popconfirm>
            </a-tooltip>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-if="modalVisible"
      :visible="modalVisible"
      :title="modalTitle"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      :width="600"
      @cancel="closeModal">
      <kubernetes-node-labels-form
        v-if="modalMode === 'node'"
        :nodeGroup="selectedNodeGroup"
        :node="selectedNode"
        @close-action="closeModal"
        @refresh-data="parentFetchData" />
      <kubernetes-node-group-form
        v-else
        :mode="modalMode"
        :resource="resource"
        :nodeGroup="selectedNodeGroup"
        @close-action="closeModal"
        @refresh-data="parentFetchData" />
    </a-modal>
  </div>
</template>

<script>
import { getAPI, postAPI } from '@/api'
import KubernetesNodeGroupForm from '@/views/compute/KubernetesNodeGroupForm'
import KubernetesNodeLabelsForm from '@/views/compute/KubernetesNodeLabelsForm'
import Status from '@/components/widgets/Status'

export default {
  name: 'KubernetesNodeGroupsTab',
  components: {
    KubernetesNodeGroupForm,
    KubernetesNodeLabelsForm,
    Status
  },
  inject: ['parentFetchData'],
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      nodeGroups: [],
      fetchLoading: false,
      modalVisible: false,
      modalMode: 'create',
      selectedNodeGroup: null,
      selectedNode: null
    }
  },
  computed: {
    nodeGroupOperationsInProgress () {
      return this.nodeGroups.some(nodeGroup => this.isNodeGroupBusy(nodeGroup))
    },
    canManage () {
      // The cluster is Scaling while operations on its node groups are in progress, its other node groups can be managed
      return this.resource.clustertype === 'CloudManaged' && (this.resource.state === 'Running' ||
        (this.resource.state === 'Scaling' && this.nodeGroupOperationsInProgress))
    },
    columns () {
      const columns = [
        { key: 'name', title: this.$t('label.name'), dataIndex: 'name' },
        { key: 'state', title: this.$t('label.state'), dataIndex: 'state' },
        { key: 'size', title: this.$t('label.size'), dataIndex: 'size' },
        { key: 'autoscalingenabled', title: this.$t('label.kubernetes.node.group.autoscaling'), dataIndex: 'autoscalingenabled' },
        { key: 'serviceofferingname', title: this.$t('label.serviceofferingid'), dataIndex: 'serviceofferingname' },
        { key: 'templatename', title: this.$t('label.template'), dataIndex: 'templatename' },
        { key: 'nodelabels', title: this.$t('label.kubernetes.node.labels'), dataIndex: 'nodelabels' },
        { key: 'nodetaints', title: this.$t('label.kubernetes.node.taints'), dataIndex: 'nodetaints' },
        { key: 'created', title: this.$t('label.created'), dataIndex: 'created' }
      ]
      if (this.resource.clustertype === 'CloudManaged') {
        columns.push({ key: 'actions', title: this.$t('label.actions'), dataIndex: 'actions' })
      }
      return columns
    },
    nodeColumns () {
      const columns = [
        // Fixed widths keep the columns of the nodes of all the node groups aligned
        { key: 'name', title: this.$t('label.name'), dataIndex: 'name', ellipsis: true },
        { key: 'state', title: this.$t('label.state'), dataIndex: 'state', width: 120 },
        { key: 'ipaddress', title: this.$t('label.ipaddress'), dataIndex: 'ipaddress', width: 130 },
        { key: 'nodelabels', title: this.$t('label.kubernetes.node.labels'), dataIndex: 'nodelabels', width: '25%' },
        { key: 'nodetaints', title: this.$t('label.kubernetes.node.taints'), dataIndex: 'nodetaints', width: '25%' }
      ]
      if (this.resource.clustertype === 'CloudManaged') {
        columns.push({ key: 'actions', title: this.$t('label.actions'), dataIndex: 'actions', width: 80 })
      }
      return columns
    },
    modalTitle () {
      if (this.modalMode === 'node') {
        return `${this.$t('label.kubernetes.node.update')} ${this.selectedNode?.name || ''}`
      }
      return this.$t({
        create: 'label.kubernetes.node.group.create',
        scale: 'label.kubernetes.node.group.scale',
        update: 'label.kubernetes.node.group.update'
      }[this.modalMode])
    }
  },
  watch: {
    resource: {
      deep: true,
      handler (newData, oldData) {
        if (newData && newData !== oldData) {
          this.fetchNodeGroups()
        }
      }
    }
  },
  created () {
    this.fetchNodeGroups()
  },
  methods: {
    fetchNodeGroups () {
      if (!this.resource?.id) {
        return
      }
      if (!('listKubernetesClusterNodeGroups' in this.$store.getters.apis)) {
        this.nodeGroups = this.resource.nodegroups || []
        return
      }
      this.fetchLoading = true
      getAPI('listKubernetesClusterNodeGroups', { clusterid: this.resource.id }).then(json => {
        this.nodeGroups = json?.listkubernetesclusternodegroupsresponse?.kubernetesclusternodegroup || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.fetchLoading = false
      })
    },
    isNodeGroupBusy (nodeGroup) {
      return ['Scaling', 'Updating', 'Deleting'].includes(nodeGroup.state)
    },
    canManageNodeGroup (nodeGroup) {
      return this.canManage && !this.isNodeGroupBusy(nodeGroup)
    },
    openModal (mode, nodeGroup = null) {
      this.modalMode = mode
      this.selectedNodeGroup = nodeGroup
      this.modalVisible = true
    },
    openNodeModal (nodeGroup, node) {
      this.modalMode = 'node'
      this.selectedNodeGroup = nodeGroup
      this.selectedNode = node
      this.modalVisible = true
    },
    closeModal () {
      this.modalVisible = false
      this.selectedNodeGroup = null
      this.selectedNode = null
    },
    // The labels of a node: the labels of its node group, overridden by the own labels of the node which are highlighted
    nodeLabels (nodeGroup, node) {
      const own = node.nodelabels || {}
      const labels = Object.entries(nodeGroup.nodelabels || {})
        .filter(([key]) => !(key in own))
        .map(([key, value]) => ({ text: `${key}=${value}`, own: false }))
      return labels.concat(Object.entries(own).map(([key, value]) => ({ text: `${key}=${value}`, own: true })))
    },
    // The taints of a node: the taints of its node group, overridden by the own taints of the node with the same key and effect
    nodeTaints (nodeGroup, node) {
      const taintId = taint => {
        const effectIndex = taint.lastIndexOf(':')
        return `${taint.substring(0, effectIndex).split('=')[0]}:${taint.substring(effectIndex + 1)}`
      }
      const own = node.nodetaints || []
      const ownIds = own.map(taintId)
      const taints = (nodeGroup.nodetaints || [])
        .filter(taint => !ownIds.includes(taintId(taint)))
        .map(taint => ({ text: taint, own: false }))
      return taints.concat(own.map(taint => ({ text: taint, own: true })))
    },
    deleteNodeGroup (nodeGroup) {
      const title = this.$t('label.kubernetes.node.group.delete')
      postAPI('deleteKubernetesClusterNodeGroup', { id: nodeGroup.id }).then(json => {
        const jobId = json.deletekubernetesclusternodegroupresponse.jobid
        this.$pollJob({
          jobId,
          title,
          description: nodeGroup.name,
          loadingMessage: `${title} ${nodeGroup.name} ${this.$t('label.in.progress')}`,
          catchMessage: this.$t('error.fetching.async.job.result'),
          successMessage: `${this.$t('message.success.delete.kubernetes.node.group')} ${nodeGroup.name}`,
          successMethod: () => {
            this.parentFetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .table {
    margin-top: 20px;
    overflow-y: auto;
  }

  .pair-tag {
    margin-bottom: 4px;
  }

  .default-tag {
    margin-left: 6px;
  }
</style>
