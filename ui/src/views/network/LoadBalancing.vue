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
    <div @keyup.ctrl.enter="handleOpenAddVMModal">
      <div class="form">
        <div class="form__item" ref="newRuleName">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.name') }}</div>
          <a-input v-focus="true" v-model:value="newRule.name"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newRulePublicPort">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.publicport') }}</div>
          <a-input v-model:value="newRule.publicport"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
        <div class="form__item" ref="newRulePrivatePort">
          <div class="form__label"><span class="form__required">*</span>{{ $t('label.privateport') }}</div>
          <a-input v-model:value="newRule.privateport"></a-input>
          <span class="error-text">{{ $t('label.required') }}</span>
        </div>
      </div>
      <div class="form">
        <div class="form__item" ref="newCidrList">
          <tooltip-label :title="$t('label.sourcecidrlist')" bold :tooltip="createLoadBalancerRuleParams.cidrlist.description" :tooltip-placement="'right'"/>
          <a-input v-model:value="newRule.cidrlist"></a-input>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.algorithm') }}</div>
          <a-select
            v-model:value="newRule.algorithm"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="roundrobin" :label="$t('label.lb.algorithm.roundrobin')">{{ $t('label.lb.algorithm.roundrobin') }}</a-select-option>
            <a-select-option value="leastconn" :label="$t('label.lb.algorithm.leastconn')">{{ $t('label.lb.algorithm.leastconn') }}</a-select-option>
            <a-select-option value="source" :label="$t('label.lb.algorithm.source')">{{ $t('label.lb.algorithm.source') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.protocol') }}</div>
          <a-select
            v-model:value="newRule.protocol"
            style="min-width: 100px"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="tcp-proxy" :label="$t('label.tcp.proxy')">{{ $t('label.tcp.proxy') }}</a-select-option>
            <a-select-option value="tcp" :label="$t('label.tcp')">{{ $t('label.tcp') }}</a-select-option>
            <a-select-option value="udp" :label="$t('label.udp')">{{ $t('label.udp') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item">
          <div class="form__label">{{ $t('label.autoscale') }}</div>
          <a-select
            v-model:value="newRule.autoscale"
            defaultValue="no"
            style="min-width: 100px"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="yes">{{ $t('label.yes') }}</a-select-option>
            <a-select-option value="no">{{ $t('label.no') }}</a-select-option>
          </a-select>
        </div>
        <div class="form__item" v-if="!newRule.autoscale || newRule.autoscale === 'no'">
          <div class="form__label" style="white-space: nowrap;">{{ $t('label.add.vms') }}</div>
          <a-button :disabled="!('createLoadBalancerRule' in $store.getters.apis)" type="primary" @click="handleOpenAddVMModal">
            {{ $t('label.add') }}
          </a-button>
        </div>
        <div class="form__item" v-else-if="newRule.autoscale === 'yes' && ('vpcid' in this.resource && !this.associatednetworkid)">
          <div class="form__label" style="white-space: nowrap;">{{ $t('label.select.tier') }}</div>
          <a-button :disabled="!('createLoadBalancerRule' in $store.getters.apis)" type="primary" @click="handleOpenAddNetworkModal">
            {{ $t('label.add') }}
          </a-button>
        </div>
        <div class="form__item" v-else-if="newRule.autoscale === 'yes'">
          <div class="form__label" style="white-space: nowrap;">{{ $t('label.add') }}</div>
          <a-button :disabled="!('createLoadBalancerRule' in $store.getters.apis)" type="primary" @click="handleAddNewRule">
            {{ $t('label.add') }}
          </a-button>
        </div>
      </div>
    </div>

    <a-divider />
    <a-button
      v-if="(('deleteLoadBalancerRule' in $store.getters.apis) && this.selectedItems.length > 0)"
      type="primary"
      danger
      style="width: 100%; margin-bottom: 15px"
      @click="bulkActionConfirmation()">
      <template #icon><delete-outlined /></template>
      {{ $t('label.action.bulk.delete.load.balancer.rules') }}
    </a-button>
    <a-table
      size="small"
      class="list-view"
      :loading="loading"
      :columns="columns"
      :dataSource="lbRules"
      :pagination="false"
      :rowSelection="{selectedRowKeys: selectedRowKeys, onChange: onSelectChange}"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cidrlist'">
          <span style="white-space: pre-line"> {{ record.cidrlist?.replaceAll(",", "\n") }}</span>
        </template>
        <template v-if="column.key === 'algorithm'">
          {{ returnAlgorithmName(record.algorithm) }}
        </template>
        <template v-if="column.key === 'protocol'">
          {{ getCapitalise(record.protocol) }}
        </template>
        <template v-if="column.key === 'stickiness'">
          <a-button @click="() => openStickinessModal(record.id)">
            {{ returnStickinessLabel(record.id) }}
          </a-button>
        </template>
        <template v-if="column.key === 'autoscale'">
          <div>
            <router-link :to="{ path: '/autoscalevmgroup/' + record.autoscalevmgroup.id }" v-if='record.autoscalevmgroup'>
              <a-button>{{ $t('label.view') }}</a-button>
            </router-link>
            <router-link :to="{ path: '/action/createAutoScaleVmGroup', query: { networkid: record.networkid, lbruleid : record.id } }" v-else-if='!record.ruleInstances'>
              <a-button>{{ $t('label.new') }}</a-button>
            </router-link>
          </div>
        </template>
        <template v-if="column.key === 'healthmonitor'">
          <a-button @click="() => openHealthMonitorModal(record.id)">
            {{ returnHealthMonitorLabel(record.id) }}
          </a-button>
        </template>
        <template v-if="column.key === 'add'">
          <a-button v-if="!record.autoscalevmgroup" type="primary" @click="() => { selectedRule = record; handleOpenAddVMModal() }">
            <template #icon><plus-outlined /></template>
              {{ $t('label.add') }}
          </a-button>
        </template>
        <template v-if="column.key === 'actions'">
          <div class="actions">
            <tooltip-button :tooltip="$t('label.edit')" icon="edit-outlined" @onClick="() => openEditRuleModal(record)" />
            <tooltip-button :tooltip="$t('label.edit.tags')" :disabled="!('updateLoadBalancerRule' in $store.getters.apis)" icon="tag-outlined" @onClick="() => openTagsModal(record.id)" />
            <a-popconfirm
              :title="$t('label.delete') + '?'"
              @confirm="handleDeleteRule(record)"
              :okText="$t('label.yes')"
              :cancelText="$t('label.no')"
            >
              <tooltip-button
                :tooltip="$t('label.delete')"
                :disabled="!('deleteLoadBalancerRule' in $store.getters.apis)"
                type="primary"
                :danger="true"
                icon="delete-outlined" />
            </a-popconfirm>
          </div>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <div class="rule-instance-list">
          <div v-for="instance in record.ruleInstances" :key="instance.loadbalancerruleinstance.id">
            <div v-for="ip in instance.lbvmipaddresses" :key="ip" class="rule-instance-list__item">
              <div>
                <status :text="instance.loadbalancerruleinstance.state" />
                <desktop-outlined />
                <router-link :to="{ path: '/vm/' + instance.loadbalancerruleinstance.id }">
                  {{ instance.loadbalancerruleinstance.displayname }}
                </router-link>
              </div>
              <div>{{ ip }}</div>
              <tooltip-button
                :disabled='record.autoscalevmgroup'
                :tooltip="$t('label.remove.vm.from.lb')"
                type="primary"
                :danger="true"
                icon="delete-outlined"
                @onClick="() => handleDeleteInstanceFromRule(instance, record, ip)" />
            </div>
          </div>
        </div>
      </template>
    </a-table>
    <a-pagination
      class="pagination"
      size="small"
      :current="page"
      :pageSize="pageSize"
      :total="totalCount"
      :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
      :pageSizeOptions="['10', '20', '40', '80', '100']"
      @change="handleChangePage"
      @showSizeChange="handleChangePageSize"
      showSizeChanger>
      <template #buildOptionText="props">
        <span>{{ props.value }} / {{ $t('label.page') }}</span>
      </template>
    </a-pagination>

    <a-modal
      v-if="tagsModalVisible"
      :title="$t('label.edit.tags')"
      :visible="tagsModalVisible"
      :footer="null"
      :closable="true"
      :afterClose="closeModal"
      :maskClosable="false"
      class="tags-modal"
      @cancel="tagsModalVisible = false">
      <span v-show="tagsModalLoading" class="modal-loading">
        <loading-outlined />
      </span>

      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        class="add-tags"
        @finish="handleAddTag"
        v-ctrl-enter="handleAddTag"
       >
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.key') }}</p>
          <a-form-item ref="key" name="key">
            <a-input
              v-focus="true"
              v-model:value="form.key" />
          </a-form-item>
        </div>
        <div class="add-tags__input">
          <p class="add-tags__label">{{ $t('label.value') }}</p>
          <a-form-item ref="value" name="value">
            <a-input v-model:value="form.value" />
          </a-form-item>
        </div>
        <a-button :disabled="!('createTags' in $store.getters.apis)" type="primary" ref="submit" @click="handleAddTag">{{ $t('label.add') }}</a-button>
      </a-form>

      <a-divider />

      <div v-show="!tagsModalLoading" class="tags-container">
        <div class="tags" v-for="(tag, index) in tags" :key="index">
          <a-tag :key="index" :closable="'deleteTags' in $store.getters.apis" @close="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </div>
      </div>

      <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.done') }}</a-button>
    </a-modal>

    <a-modal
      :visible="stickinessModalVisible"
      :footer="null"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :okButtonProps="{ props: {htmlType: 'submit'}}"
      @cancel="stickinessModalVisible = false">

      <template #title>
        <span>{{ $t('label.configure.sticky.policy') }}</span>
        <a
          style="margin-left: 5px"
          :href="$config.docBase + '/adminguide/networking/external_firewalls_and_load_balancers.html#sticky-session-policies-for-load-balancer-rules'"
          target="_blank">
          <question-circle-outlined />
        </a>
      </template>

      <span v-show="stickinessModalLoading" class="modal-loading">
        <loading-outlined />
      </span>

      <a-form
        :ref="formRef"
        :model="form"
        :rules="rules"
        @finish="handleSubmitStickinessForm"
        v-ctrl-enter="handleSubmitStickinessForm"
        class="custom-ant-form"
       >
        <a-form-item name="methodname" ref="methodname">
          <template #label>
            <tooltip-label :title="$t('label.stickiness.method')" :tooltip="createLoadBalancerStickinessPolicyParams.methodname.description" :tooltip-placement="'right'"/>
          </template>
          <a-select
            v-focus="true"
            v-model:value="form.methodname"
            @change="handleStickinessMethodSelectChange"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="LbCookie" :label="$t('label.lb.cookie')">{{ $t('label.lb.cookie') }}</a-select-option>
            <a-select-option value="AppCookie" :label="$t('label.app.cookie')">{{ $t('label.app.cookie') }}</a-select-option>
            <a-select-option value="SourceBased" :label="$t('label.source.based')">{{ $t('label.source.based') }}</a-select-option>
            <a-select-option value="none" :label="$t('label.none')">{{ $t('label.none') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="name"
          ref="name"
          v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
            'AppCookie' || stickinessPolicyMethod === 'SourceBased'">
          <a-input v-model:value="form.name" />
          <template #label>
            <tooltip-label :title="$t('label.sticky.name')" :tooltip="createLoadBalancerStickinessPolicyParams.name.description" :tooltip-placement="'right'"/>
          </template>
        </a-form-item>
        <div v-if="stickinessPolicyMethod !== 'none'">
          <br/>
          {{ $t('message.loadbalancer.stickypolicy.configuration') }}
          <br/>
          <a-card>
            <a-form-item
              name="cookieName"
              ref="cookieName"
              :label="$t('label.sticky.cookie-name')"
              v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
                'AppCookie'">
              <a-input v-model:value="form.cookieName" />
            </a-form-item>
            <a-form-item
              name="mode"
              ref="mode"
              :label="$t('label.sticky.mode')"
              v-show="stickinessPolicyMethod === 'LbCookie' || stickinessPolicyMethod ===
                'AppCookie'">
              <a-input v-model:value="form.mode" />
            </a-form-item>
            <a-form-item name="nocache" ref="nocache" :label="$t('label.sticky.nocache')" v-show="stickinessPolicyMethod === 'LbCookie'">
              <a-checkbox v-model:checked="form.nocache"></a-checkbox>
            </a-form-item>
            <a-form-item name="indirect" ref="indirect" :label="$t('label.sticky.indirect')" v-show="stickinessPolicyMethod === 'LbCookie'">
              <a-checkbox v-model:checked="form.indirect"></a-checkbox>
            </a-form-item>
            <a-form-item name="postonly" ref="postonly" :label="$t('label.sticky.postonly')" v-show="stickinessPolicyMethod === 'LbCookie'">
              <a-checkbox v-model:checked="form.postonly"></a-checkbox>
            </a-form-item>
            <a-form-item name="domain" ref="domain" :label="$t('label.domain')" v-show="stickinessPolicyMethod === 'LbCookie'">
              <a-input v-model:value="form.domain" />
            </a-form-item>
            <a-form-item name="length" ref="length" :label="$t('label.sticky.length')" v-show="stickinessPolicyMethod === 'AppCookie'">
              <a-input v-model:value="form.length" type="number" />
            </a-form-item>
            <a-form-item name="holdtime" ref="holdtime" :label="$t('label.sticky.holdtime')" v-show="stickinessPolicyMethod === 'AppCookie'">
              <a-input v-model:value="form.holdtime" type="number" />
            </a-form-item>
            <a-form-item name="requestLearn" ref="requestLearn" :label="$t('label.sticky.request-learn')" v-show="stickinessPolicyMethod === 'AppCookie'">
              <a-checkbox v-model:checked="form.requestLearn"></a-checkbox>
            </a-form-item>
            <a-form-item name="prefix" ref="prefix" :label="$t('label.sticky.prefix')" v-show="stickinessPolicyMethod === 'AppCookie'">
              <a-checkbox v-model:checked="form.prefix"></a-checkbox>
            </a-form-item>
            <a-form-item name="tablesize" ref="tablesize" :label="$t('label.sticky.tablesize')" v-show="stickinessPolicyMethod === 'SourceBased'">
              <a-input v-model:value="form.tablesize" />
            </a-form-item>
            <a-form-item name="expire" ref="expire" :label="$t('label.sticky.expire')" v-show="stickinessPolicyMethod === 'SourceBased'">
              <a-input v-model:value="form.expire" />
            </a-form-item>
          </a-card>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="stickinessModalVisible = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" ref="submit" @click="handleSubmitStickinessForm">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <a-modal
      :title="$t('label.edit.rule')"
      :visible="editRuleModalVisible"
      :afterClose="closeModal"
      :maskClosable="false"
      :closable="true"
      :footer="null"
      @cancel="editRuleModalVisible = false">
      <span v-show="editRuleModalLoading" class="modal-loading">
        <loading-outlined />
      </span>

      <div class="edit-rule" v-if="selectedRule" v-ctrl-enter="handleSubmitEditForm">
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.name') }}</p>
          <a-input v-focus="true" v-model:value="editRuleDetails.name" />
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.algorithm') }}</p>
          <a-select
            v-model:value="editRuleDetails.algorithm"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="roundrobin" :label="$t('label.lb.algorithm.roundrobin')">{{ $t('label.lb.algorithm.roundrobin') }}</a-select-option>
            <a-select-option value="leastconn" :label="$t('label.lb.algorithm.leastconn')">{{ $t('label.lb.algorithm.leastconn') }}</a-select-option>
            <a-select-option value="source" :label="$t('label.lb.algorithm.source')">{{ $t('label.lb.algorithm.source') }}</a-select-option>
          </a-select>
        </div>
        <div class="edit-rule__item">
          <p class="edit-rule__label">{{ $t('label.protocol') }}</p>
          <a-select
            v-model:value="editRuleDetails.protocol"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option value="tcp-proxy" :label="$t('label.tcp.proxy')">{{ $t('label.tcp.proxy') }}</a-select-option>
            <a-select-option value="tcp" :label="$t('label.tcp')">{{ $t('label.tcp') }}</a-select-option>
            <a-select-option value="udp" :label="$t('label.udp')">{{ $t('label.udp') }}</a-select-option>
          </a-select>
        </div>
        <div :span="24" class="action-button">
          <a-button @click="() => editRuleModalVisible = false">{{ $t('label.cancel') }}</a-button>
          <a-button type="primary" @click="handleSubmitEditForm">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal
      :title="$t('label.add.vms')"
      :maskClosable="false"
      :closable="true"
      v-if="addVmModalVisible"
      :visible="addVmModalVisible"
      class="vm-modal"
      width="60vw"
      :footer="null"
      @cancel="closeModal"
    >
      <div @keyup.ctrl.enter="handleAddNewRule">
        <span
          v-if="'vpcid' in resource && !('associatednetworkid' in resource)">
          <strong>{{ $t('label.select.tier') }} </strong>
          <a-select
            v-focus="'vpcid' in resource && !('associatednetworkid' in resource)"
            v-model:value="selectedTier"
            @change="fetchVirtualMachines()"
            :placeholder="$t('label.select.tier')"
            showSearch
            optionFilterProp="label"
            :filterOption="(input, option) => {
              return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }" >
            <a-select-option
              v-for="tier in tiers.data"
              :loading="tiers.loading"
              :key="tier.id"
              :label="tier.displaytext">
              {{ tier.displaytext }}
            </a-select-option>
          </a-select>
        </span>
        <a-input-search
          v-focus="!('vpcid' in resource && !('associatednetworkid' in resource))"
          class="input-search"
          :placeholder="$t('label.search')"
          v-model:value="searchQuery"
          allowClear
          @search="onSearch" />
        <a-table
          size="small"
          class="list-view"
          :loading="addVmModalLoading"
          :columns="vmColumns"
          :dataSource="vms"
          :pagination="false"
          :rowKey="record => record.id"
          :scroll="{ y: 300 }">
          <template #bodyCell="{ column, text, record, index }">
            <template v-if="column.key === 'name'">
              <span>
                {{ text }}
              </span>
              <loading-outlined v-if="addVmModalNicLoading" />
              <a-select
                style="display: block"
                v-else-if="!addVmModalNicLoading && newRule.virtualmachineid[index] === record.id"
                mode="multiple"
                v-model:value="newRule.vmguestip[index]"
                showSearch
                optionFilterProp="label"
                :filterOption="(input, option) => {
                  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }" >
                <a-select-option
                  v-for="(nic, nicIndex) in nics[index]"
                  :key="nic"
                  :value="nic"
                  :label="nic + nicIndex === 0 ? ` (${$t('label.primary')})` : null">
                  {{ nic }}{{ nicIndex === 0 ? ` (${$t('label.primary')})` : null }}
                </a-select-option>
              </a-select>
            </template>

            <template v-if="column.key === 'state'">
              <status :text="text ? text : ''" displayText></status>
            </template>

            <template v-if="column.key === 'actions'" style="text-align: center" :text="text">
              <a-checkbox v-model:value="record.id" @change="e => fetchNics(e, index)" />
            </template>
          </template>
        </a-table>
        <a-pagination
          class="pagination"
          size="small"
          :current="vmPage"
          :pageSize="vmPageSize"
          :total="vmCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="handleChangeVmPage"
          @showSizeChange="handleChangeVmPageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>

        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button :disabled="newRule.virtualmachineid === []" type="primary" ref="submit" @click="handleAddNewRule">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal
      :title="$t('label.select.tier')"
      :maskClosable="false"
      :closable="true"
      v-if="addNetworkModalVisible"
      :visible="addNetworkModalVisible"
      class="network-modal"
      width="60vw"
      :footer="null"
      @cancel="closeModal"
    >
      <div @keyup.ctrl.enter="handleAddNewRule">
        <a-input-search
          v-focus="!('vpcid' in resource && !('associatednetworkid' in resource))"
          class="input-search"
          :placeholder="$t('label.search')"
          v-model:value="searchQuery"
          allowClear
          @search="onNetworkSearch" />
        <a-table
          size="small"
          class="list-view"
          :loading="addNetworkModalLoading"
          :columns="networkColumns"
          :dataSource="networks"
          :pagination="false"
          :rowKey="record => record.id"
          :scroll="{ y: 300 }">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'actions'">
              <div style="text-align: center">
                <a-radio-group
                  class="radio-group"
                  :key="record.id"
                  v-model:value="this.selectedTierForAutoScaling"
                  @change="($event) => this.selectedTierForAutoScaling = $event.target.value">
                  <a-radio :value="record.id" />
                </a-radio-group>
              </div>
            </template>
          </template>
        </a-table>
        <a-pagination
          class="pagination"
          size="small"
          :current="networkPage"
          :pageSize="networkPageSize"
          :total="networkCount"
          :showTotal="total => `${$t('label.total')} ${total} ${$t('label.items')}`"
          :pageSizeOptions="['10', '20', '40', '80', '100']"
          @change="handleChangeNetworkPage"
          @showSizeChange="handleChangeNetworkPageSize"
          showSizeChanger>
          <template #buildOptionText="props">
            <span>{{ props.value }} / {{ $t('label.page') }}</span>
          </template>
        </a-pagination>

        <div :span="24" class="action-button">
          <a-button @click="closeModal">{{ $t('label.cancel') }}</a-button>
          <a-button :disabled="this.selectedTierForAutoScaling === null" type="primary" ref="submit" @click="handleAddNewRule">{{ $t('label.ok') }}</a-button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-if="healthMonitorModal"
      :title="$t('label.configure.health.monitor')"
      :visible="healthMonitorModal"
      :footer="null"
      :maskClosable="false"
      :closable="true"
      @cancel="closeMonitorModal">
      <a-form
        :ref="monitorRef"
        :model="monitorForm"
        :rules="monitorRules"
        layout="vertical"
        @finish="handleConfigHealthMonitor"
        v-ctrl-enter="handleConfigHealthMonitor">
        <a-form-item name="type" ref="type" :label="$t('label.monitor.type')">
          <a-select
            v-focus="true"
            v-model:value="monitorForm.type"
            @change="(value) => { healthMonitorParams.type = value }"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option value="PING">PING</a-select-option>
            <a-select-option value="TCP">TCP</a-select-option>
            <a-select-option value="HTTP">HTTP</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="retry" ref="retry" :label="$t('label.monitor.retry')">
          <a-input v-model:value="monitorForm.retry" />
        </a-form-item>
        <a-form-item name="timeout" ref="timeout" :label="$t('label.monitor.timeout')">
          <a-input v-model:value="monitorForm.timeout" />
        </a-form-item>
        <a-form-item name="interval" ref="interval" :label="$t('label.monitor.interval')">
          <a-input v-model:value="monitorForm.interval" />
        </a-form-item>
        <a-form-item
          name="httpmethodtype"
          ref="httpmethodtype"
          :label="$t('label.monitor.http.method')"
          v-if="healthMonitorParams.type === 'HTTP'">
          <a-select
            v-focus="true"
            v-model:value="monitorForm.httpmethodtype"
            showSearch
            optionFilterProp="value"
            :filterOption="(input, option) => {
              return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
            }">
            <a-select-option value="GET">GET</a-select-option>
            <a-select-option value="HEAD">HEAD</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          name="expectedcode"
          ref="expectedcode"
          :label="$t('label.monitor.expected.code')"
          v-if="healthMonitorParams.type === 'HTTP'">
          <a-input v-model:value="monitorForm.expectedcode" />
        </a-form-item>
        <a-form-item
          name="urlpath"
          ref="urlpath"
          :label="$t('label.monitor.url')"
          v-if="healthMonitorParams.type === 'HTTP'">
          <a-input v-model:value="monitorForm.urlpath" />
        </a-form-item>

        <div :span="24" class="action-button">
          <a-button :loading="healthMonitorLoading" @click="closeMonitorModal">{{ $t('label.cancel') }}</a-button>
          <a-button :loading="healthMonitorLoading" type="primary" @click="handleConfigHealthMonitor">{{ $t('label.ok') }}</a-button>
        </div>
      </a-form>
    </a-modal>

    <bulk-action-view
      v-if="showConfirmationAction || showGroupActionModal"
      :showConfirmationAction="showConfirmationAction"
      :showGroupActionModal="showGroupActionModal"
      :items="lbRules"
      :selectedRowKeys="selectedRowKeys"
      :selectedItems="selectedItems"
      :columns="columns"
      :selectedColumns="selectedColumns"
      :filterColumns="filterColumns"
      action="deleteLoadBalancerRule"
      :loading="loading"
      :message="message"
      @group-action="deleteRules"
      @handle-cancel="handleCancel"
      @close-modal="closeModal" />
  </div>
</template>

<script>
import { ref, reactive, toRaw, nextTick } from 'vue'
import { api } from '@/api'
import { mixinForm } from '@/utils/mixin'
import Status from '@/components/widgets/Status'
import TooltipButton from '@/components/widgets/TooltipButton'
import BulkActionView from '@/components/view/BulkActionView'
import eventBus from '@/config/eventBus'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'LoadBalancing',
  mixins: [mixinForm],
  components: {
    Status,
    TooltipButton,
    BulkActionView,
    TooltipLabel
  },
  props: {
    resource: {
      type: Object,
      required: true
    }
  },
  inject: ['parentFetchData', 'parentToggleLoading'],
  data () {
    return {
      selectedRowKeys: [],
      showGroupActionModal: false,
      selectedItems: [],
      selectedColumns: [],
      filterColumns: ['State', 'Actions', 'Add VMs', 'Stickiness'],
      showConfirmationAction: false,
      message: {
        title: this.$t('label.action.bulk.delete.load.balancer.rules'),
        confirmMessage: this.$t('label.confirm.delete.loadbalancer.rules')
      },
      loading: true,
      lbRules: [],
      tagsModalVisible: false,
      tagsModalLoading: false,
      tags: [],
      selectedRule: null,
      selectedTier: null,
      stickinessModalVisible: false,
      stickinessPolicies: [],
      stickinessModalLoading: false,
      selectedStickinessPolicy: null,
      stickinessPolicyMethod: 'LbCookie',
      editRuleModalVisible: false,
      editRuleModalLoading: false,
      editRuleDetails: {
        name: '',
        algorithm: '',
        protocol: ''
      },
      newRule: {
        algorithm: 'roundrobin',
        name: '',
        privateport: '',
        publicport: '',
        protocol: 'tcp',
        virtualmachineid: [],
        vmguestip: [],
        cidrlist: ''
      },
      addVmModalVisible: false,
      addVmModalLoading: false,
      addVmModalNicLoading: false,
      vms: [],
      nics: [],
      totalCount: 0,
      page: 1,
      pageSize: 10,
      columns: [
        {
          title: this.$t('label.name'),
          dataIndex: 'name'
        },
        {
          title: this.$t('label.publicport'),
          dataIndex: 'publicport'
        },
        {
          title: this.$t('label.privateport'),
          dataIndex: 'privateport'
        },
        {
          key: 'algorithm',
          title: this.$t('label.algorithm')
        },
        {
          key: 'cidrlist',
          title: this.$t('label.sourcecidrlist')
        },
        {
          key: 'protocol',
          title: this.$t('label.protocol')
        },
        {
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          key: 'stickiness',
          title: this.$t('label.action.configure.stickiness')
        },
        {
          key: 'add',
          title: this.$t('label.add.vms')
        },
        {
          key: 'autoscale',
          title: this.$t('label.autoscale')
        },
        {
          key: 'actions',
          title: this.$t('label.actions')
        }
      ],
      tiers: {
        loading: false,
        data: []
      },
      vmColumns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name',
          width: 220
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.displayname'),
          dataIndex: 'displayname'
        },
        {
          title: this.$t('label.account'),
          dataIndex: 'account'
        },
        {
          title: this.$t('label.zonename'),
          dataIndex: 'zonename'
        },
        {
          key: 'actions',
          title: this.$t('label.select'),
          dataIndex: 'actions',
          width: 80
        }
      ],
      vmPage: 1,
      vmPageSize: 10,
      vmCount: 0,
      addNetworkModalVisible: false,
      addNetworkModalLoading: false,
      networks: [],
      associatednetworkid: null,
      selectedTierForAutoScaling: null,
      networkColumns: [
        {
          key: 'name',
          title: this.$t('label.name'),
          dataIndex: 'name',
          width: 220
        },
        {
          key: 'state',
          title: this.$t('label.state'),
          dataIndex: 'state'
        },
        {
          title: this.$t('label.gateway'),
          dataIndex: 'gateway'
        },
        {
          title: this.$t('label.netmask'),
          dataIndex: 'netmask'
        },
        {
          key: 'actions',
          title: this.$t('label.select'),
          dataIndex: 'actions',
          width: 80
        }
      ],
      networkPage: 1,
      networkPageSize: 10,
      networkCount: 0,
      searchQuery: null,
      tungstenHealthMonitors: [],
      healthMonitorModal: false,
      healthMonitorParams: {
        type: 'PING',
        retry: 3,
        timeout: 5,
        interval: 5,
        httpmethodtype: 'GET',
        expectedcode: undefined,
        urlpath: '/'
      },
      healthMonitorLoading: false
    }
  },
  computed: {
    hasSelected () {
      return this.selectedRowKeys.length > 0
    }
  },
  beforeCreate () {
    this.createLoadBalancerRuleParams = this.$getApiParams('createLoadBalancerRule')
    this.createLoadBalancerStickinessPolicyParams = this.$getApiParams('createLBStickinessPolicy')
    if ('associatednetworkid' in this.resource) {
      this.associatednetworkid = this.resource.associatednetworkid
    }
  },
  created () {
    this.initForm()
    this.initMonitorForm()
    this.fetchData()
  },
  watch: {
    resource: {
      deep: true,
      handler (newItem) {
        if (!newItem || !newItem.id) {
          return
        }
        this.fetchData()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({})
      this.rules = reactive({})
    },
    initMonitorForm () {
      this.monitorRef = ref()
      this.monitorForm = reactive({
        type: this.healthMonitorParams.type,
        retry: this.healthMonitorParams.retry,
        timeout: this.healthMonitorParams.timeout,
        interval: this.healthMonitorParams.interval,
        httpmethodtype: this.healthMonitorParams.httpmethodtype,
        expectedcode: this.healthMonitorParams.expectedcode,
        urlpath: this.healthMonitorParams.urlpath
      })
      this.monitorRules = reactive({
        retry: [{ required: true, message: this.$t('message.error.required.input') }],
        timeout: [{ required: true, message: this.$t('message.error.required.input') }],
        interval: [{ required: true, message: this.$t('message.error.required.input') }],
        expectedcode: [{ required: true, message: this.$t('message.error.required.input') }],
        urlpath: [{ required: true, message: this.$t('message.error.required.input') }]
      })
    },
    fetchData () {
      this.fetchListTiers()
      this.fetchLBRules()
    },
    fetchListTiers () {
      this.tiers.loading = true

      api('listNetworks', {
        supportedservices: 'Lb',
        isrecursive: true,
        vpcid: this.resource.vpcid
      }).then(json => {
        this.tiers.data = json.listnetworksresponse.network || []
        this.selectedTier = this.tiers.data?.[0]?.id ? this.tiers.data[0].id : null
        if (this.tiers.data?.[0]?.broadcasturi === 'tf://tf') {
          this.columns.splice(8, 0, {
            title: this.$t('label.action.health.monitor'),
            key: 'healthmonitor'
          })
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => { this.tiers.loading = false })
    },
    fetchLBRules () {
      this.loading = true
      this.lbRules = []
      this.stickinessPolicies = []

      api('listLoadBalancerRules', {
        listAll: true,
        publicipid: this.resource.id,
        page: this.page,
        pageSize: this.pageSize
      }).then(response => {
        this.lbRules = response.listloadbalancerrulesresponse.loadbalancerrule || []
        this.totalCount = response.listloadbalancerrulesresponse.count || 0
      }).then(() => {
        if (this.lbRules.length > 0) {
          setTimeout(() => {
            this.fetchLBRuleInstances()
          }, 100)
          this.fetchLBStickinessPolicies()
          this.fetchLBTungstenFabricHealthMonitor()
          this.fetchAutoScaleVMgroups()
          return
        }
        this.loading = false
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    },
    fetchLBRuleInstances () {
      for (const rule of this.lbRules) {
        this.loading = true
        api('listLoadBalancerRuleInstances', {
          listAll: true,
          lbvmips: true,
          id: rule.id
        }).then(response => {
          rule.ruleInstances = response.listloadbalancerruleinstancesresponse.lbrulevmidip
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      }
    },
    fetchLBStickinessPolicies () {
      this.loading = true
      this.lbRules.forEach(rule => {
        api('listLBStickinessPolicies', {
          listAll: true,
          lbruleid: rule.id
        }).then(response => {
          this.stickinessPolicies.push(...response.listlbstickinesspoliciesresponse.stickinesspolicies)
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    fetchAutoScaleVMgroups () {
      this.loading = true
      this.lbRules.forEach(rule => {
        api('listAutoScaleVmGroups', {
          listAll: true,
          lbruleid: rule.id
        }).then(response => {
          rule.autoscalevmgroup = response.listautoscalevmgroupsresponse?.autoscalevmgroup?.[0]
        }).finally(() => {
          this.loading = false
        })
      })
    },
    returnAlgorithmName (name) {
      switch (name) {
        case 'leastconn':
          return 'Least connections'
        case 'roundrobin' :
          return 'Round-robin'
        case 'source':
          return 'Source'
        default :
          return ''
      }
    },
    returnStickinessLabel (id) {
      const match = this.stickinessPolicies.filter(policy => policy.lbruleid === id)
      if (match.length > 0 && match[0].stickinesspolicy.length > 0) {
        return match[0].stickinesspolicy[0].methodname
      }
      return 'Configure'
    },
    getCapitalise (val) {
      if (!val) {
        return
      }
      if (val === 'all') return this.$t('label.all')
      return val.toUpperCase()
    },
    openTagsModal (id) {
      this.initForm()
      this.rules = {
        key: [{ required: true, message: this.$t('message.specify.tag.key') }],
        value: [{ required: true, message: this.$t('message.specify.tag.value') }]
      }
      this.tagsModalLoading = true
      this.tagsModalVisible = true
      this.tags = []
      this.selectedRule = id
      api('listTags', {
        resourceId: id,
        resourceType: 'LoadBalancer',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag
        this.tagsModalLoading = false
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    handleAddTag (e) {
      if (this.tagsModalLoading) return
      this.tagsModalLoading = true

      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)

        api('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedRule,
          resourceType: 'LoadBalancer'
        }).then(response => {
          this.$pollJob({
            jobId: response.createtagsresponse.jobid,
            successMessage: this.$t('message.success.add.tag'),
            successMethod: () => {
              this.parentToggleLoading()
              this.openTagsModal(this.selectedRule)
            },
            errorMessage: this.$t('message.add.tag.failed'),
            errorMethod: () => {
              this.parentToggleLoading()
              this.closeModal()
            },
            loadingMessage: this.$t('message.add.tag.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.parentFetchData()
              this.parentToggleLoading()
              this.closeModal()
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.tagsModalLoading = false
      })
    },
    handleDeleteTag (tag) {
      this.tagsModalLoading = true
      api('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: tag.resourceid,
        resourceType: 'LoadBalancer'
      }).then(response => {
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: this.$t('message.success.delete.tag'),
          successMethod: () => {
            this.parentToggleLoading()
            this.openTagsModal(this.selectedRule)
          },
          errorMessage: this.$t('message.delete.tag.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.closeModal()
          },
          loadingMessage: this.$t('message.delete.tag.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    openStickinessModal (id) {
      this.initForm()
      this.rules = {
        methodname: [{ required: true, message: this.$t('message.error.specify.stickiness.method') }]
      }
      this.stickinessModalVisible = true
      this.selectedRule = id
      const match = this.stickinessPolicies.find(policy => policy.lbruleid === id)

      if (match && match.stickinesspolicy.length > 0) {
        this.selectedStickinessPolicy = match.stickinesspolicy[0]
        this.stickinessPolicyMethod = this.selectedStickinessPolicy.methodname
        nextTick().then(() => {
          this.form.methodname = this.selectedStickinessPolicy.methodname
          this.form.name = this.selectedStickinessPolicy.name
          this.form.cookieName = this.selectedStickinessPolicy.params['cookie-name']
          this.form.mode = this.selectedStickinessPolicy.params.mode
          this.form.domain = this.selectedStickinessPolicy.params.domain
          this.form.length = this.selectedStickinessPolicy.params.length
          this.form.holdtime = this.selectedStickinessPolicy.params.holdtime
          this.form.nocache = !!this.selectedStickinessPolicy.params.nocache
          this.form.indirect = !!this.selectedStickinessPolicy.params.indirect
          this.form.postonly = !!this.selectedStickinessPolicy.params.postonly
          this.form.requestLearn = !!this.selectedStickinessPolicy.params['request-learn']
          this.form.prefix = !!this.selectedStickinessPolicy.params.prefix
        })
      }
    },
    handleAddStickinessPolicy (data, values) {
      api('createLBStickinessPolicy', {
        ...data,
        lbruleid: this.selectedRule,
        name: values.name,
        methodname: values.methodname
      }).then(response => {
        this.$pollJob({
          jobId: response.createLBStickinessPolicy.jobid,
          successMessage: this.$t('message.success.config.sticky.policy'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.config.sticky.policy.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.config.sticky.policy.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.closeModal()
      })
    },
    handleDeleteStickinessPolicy () {
      this.stickinessModalLoading = true
      api('deleteLBStickinessPolicy', { id: this.selectedStickinessPolicy.id }).then(response => {
        this.$pollJob({
          jobId: response.deleteLBstickinessrruleresponse.jobid,
          successMessage: this.$t('message.success.remove.sticky.policy'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.remove.sticky.policy.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.remove.sticky.policy.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    handleSubmitStickinessForm (e) {
      if (this.stickinessModalLoading) return
      this.stickinessModalLoading = true
      e.preventDefault()
      this.formRef.value.validate().then(() => {
        const formRaw = toRaw(this.form)
        const values = this.handleRemoveFields(formRaw)
        if (values.methodname === 'none') {
          this.handleDeleteStickinessPolicy()
          return
        }

        if (values.name === null || values.name === undefined || values.name === '') {
          this.$notification.error({
            message: this.$t('label.error'),
            description: this.$t('message.error.specify.sticky.name')
          })
          return
        }

        values.nocache = this.form.nocache
        values.indirect = this.form.indirect
        values.postonly = this.form.postonly
        values.requestLearn = this.form.requestLearn
        values.prefix = this.form.prefix

        let data = {}
        let count = 0
        Object.entries(values).forEach(([key, val]) => {
          if (val && key !== 'name' && key !== 'methodname') {
            if (key === 'cookieName') {
              data = { ...data, ...{ [`param[${count}].name`]: 'cookie-name' } }
            } else if (key === 'requestLearn') {
              data = { ...data, ...{ [`param[${count}].name`]: 'request-learn' } }
            } else {
              data = { ...data, ...{ [`param[${count}].name`]: key } }
            }
            data = { ...data, ...{ [`param[${count}].value`]: val } }
            count++
          }
        })

        this.handleAddStickinessPolicy(data, values)
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.stickinessModalLoading = false
      })
    },
    handleStickinessMethodSelectChange (e) {
      if (this.formRef.value) this.formRef.value.resetFields()
      this.stickinessPolicyMethod = e
      this.form.methodname = e
    },
    handleDeleteInstanceFromRule (instance, rule, ip) {
      this.loading = true
      api('removeFromLoadBalancerRule', {
        id: rule.id,
        'vmidipmap[0].vmid': instance.loadbalancerruleinstance.id,
        'vmidipmap[0].vmip': ip
      }).then(response => {
        this.$pollJob({
          jobId: response.removefromloadbalancerruleresponse.jobid,
          successMessage: this.$t('message.success.remove.instance.rule'),
          successMethod: () => {
            this.fetchData()
          },
          errorMessage: this.$t('message.remove.instance.failed'),
          errorMethod: () => {
            this.fetchData()
          },
          loadingMessage: this.$t('message.remove.instance.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
      })
    },
    openEditRuleModal (rule) {
      this.selectedRule = rule
      this.editRuleModalVisible = true
      this.editRuleDetails.name = this.selectedRule.name
      this.editRuleDetails.algorithm = this.selectedRule.algorithm
      this.editRuleDetails.protocol = this.selectedRule.protocol
    },
    handleSubmitEditForm () {
      if (this.editRuleModalLoading) return
      this.loading = true
      this.editRuleModalLoading = true
      api('updateLoadBalancerRule', {
        ...this.editRuleDetails,
        id: this.selectedRule.id
      }).then(response => {
        this.$pollJob({
          jobId: response.updateloadbalancerruleresponse.jobid,
          successMessage: this.$t('message.success.edit.rule'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.edit.rule.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.edit.rule.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    },
    setSelection (selection) {
      this.selectedRowKeys = selection
      this.$emit('selection-change', this.selectedRowKeys)
      this.selectedItems = (this.lbRules.filter(function (item) {
        return selection.indexOf(item.id) !== -1
      }))
    },
    resetSelection () {
      this.setSelection([])
    },
    onSelectChange (selectedRowKeys, selectedRows) {
      this.setSelection(selectedRowKeys)
    },
    bulkActionConfirmation () {
      this.showConfirmationAction = true
      this.selectedColumns = this.columns.filter(column => {
        return !this.filterColumns.includes(column.title)
      })
      this.selectedItems = this.selectedItems.map(v => ({ ...v, status: 'InProgress' }))
    },
    handleCancel () {
      eventBus.emit('update-bulk-job-status', { items: this.selectedItems, action: false })
      this.showGroupActionModal = false
      this.selectedItems = []
      this.selectedColumns = []
      this.selectedRowKeys = []
      this.parentFetchData()
    },
    deleteRules (e) {
      this.showConfirmationAction = false
      this.selectedColumns.splice(0, 0, {
        key: 'status',
        dataIndex: 'status',
        title: this.$t('label.operation.status'),
        filters: [
          { text: 'In Progress', value: 'InProgress' },
          { text: 'Success', value: 'success' },
          { text: 'Failed', value: 'failed' }
        ]
      })
      if (this.selectedRowKeys.length > 0) {
        this.showGroupActionModal = true
      }
      for (const rule of this.selectedItems) {
        this.handleDeleteRule(rule)
      }
    },
    handleDeleteRule (rule) {
      this.loading = true
      api('deleteLoadBalancerRule', {
        id: rule.id
      }).then(response => {
        const jobId = response.deleteloadbalancerruleresponse.jobid
        eventBus.emit('update-job-details', { jobId, resourceId: null })
        this.$pollJob({
          title: this.$t('label.action.delete.load.balancer'),
          description: rule.id,
          jobId: jobId,
          successMessage: this.$t('message.success.remove.rule'),
          successMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: rule.id, state: 'success' })
            }
            if (this.selectedRowKeys.length === 0) {
              this.parentToggleLoading()
              this.fetchData()
            }
            this.closeModal()
          },
          errorMessage: this.$t('message.remove.rule.failed'),
          errorMethod: () => {
            if (this.selectedItems.length > 0) {
              eventBus.emit('update-resource-state', { selectedItems: this.selectedItems, resource: rule.id, state: 'failed' })
            }
            if (this.selectedRowKeys.length === 0) {
              this.parentToggleLoading()
              this.fetchData()
            }
            this.closeModal()
          },
          loadingMessage: this.$t('message.delete.rule.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            if (this.selectedRowKeys.length === 0) {
              this.parentToggleLoading()
              this.parentFetchData()
            }
            this.closeModal()
          },
          bulkAction: `${this.selectedItems.length > 0}` && this.showGroupActionModal
        })
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })
    },
    checkNewRule () {
      if (!this.selectedRule) {
        if (!this.newRule.name) {
          this.$refs.newRuleName.classList.add('error')
        } else {
          this.$refs.newRuleName.classList.remove('error')
        }
        if (!this.newRule.publicport) {
          this.$refs.newRulePublicPort.classList.add('error')
        } else {
          this.$refs.newRulePublicPort.classList.remove('error')
        }
        if (!this.newRule.privateport) {
          this.$refs.newRulePrivatePort.classList.add('error')
        } else {
          this.$refs.newRulePrivatePort.classList.remove('error')
        }
        if (!this.newRule.name || !this.newRule.publicport || !this.newRule.privateport) return false
      }
      return true
    },
    handleOpenAddVMModal () {
      if (this.addVmModalLoading) return
      if (!this.checkNewRule()) {
        return
      }
      this.addVmModalVisible = true
      this.fetchVirtualMachines()
    },
    fetchNics (e, index) {
      if (!e.target.checked) {
        this.newRule.virtualmachineid[index] = null
        this.nics[index] = null
        this.newRule.vmguestip[index] = null
        return
      }
      this.newRule.virtualmachineid[index] = e.target.value
      this.addVmModalNicLoading = true

      api('listNics', {
        virtualmachineid: e.target.value,
        networkid: ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.selectedTier : this.resource.associatednetworkid
      }).then(response => {
        if (!response || !response.listnicsresponse || !response.listnicsresponse.nic[0]) return
        const newItem = []
        newItem.push(response.listnicsresponse.nic[0].ipaddress)
        if (response.listnicsresponse.nic[0].secondaryip) {
          newItem.push(...response.listnicsresponse.nic[0].secondaryip.map(ip => ip.ipaddress))
        }
        this.nics[index] = newItem
        this.newRule.vmguestip[index] = [this.nics[index][0]]
        this.addVmModalNicLoading = false
      }).catch(error => {
        this.$notifyError(error)
        this.closeModal()
      })
    },
    fetchVirtualMachines () {
      this.vmCount = 0
      this.vms = []
      this.addVmModalLoading = true
      const networkId = ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.selectedTier : this.resource.associatednetworkid
      if (!networkId) {
        this.addVmModalLoading = false
        return
      }
      api('listVirtualMachines', {
        listAll: true,
        keyword: this.searchQuery,
        page: this.vmPage,
        pagesize: this.vmPageSize,
        networkid: networkId
      }).then(response => {
        this.vmCount = response.listvirtualmachinesresponse.count || 0
        this.vms = response.listvirtualmachinesresponse.virtualmachine || []
        this.vms.forEach((vm, index) => {
          this.newRule.virtualmachineid[index] = null
          this.nics[index] = null
          this.newRule.vmguestip[index] = null
        })
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addVmModalLoading = false
      })
    },
    handleOpenAddNetworkModal () {
      if (this.addNetworkModalLoading) return
      if (!this.checkNewRule()) {
        return
      }
      this.addNetworkModalVisible = true
      this.fetchNetworks()
    },
    fetchNetworks () {
      this.networkCount = 0
      this.networks = []
      this.addNetworkModalLoading = true
      const vpcid = this.resource.vpcid
      if (!vpcid) {
        this.addNetworkModalLoading = false
        return
      }
      api('listNetworks', {
        listAll: true,
        keyword: this.searchQuery,
        page: this.networkPage,
        pagesize: this.networkPageSize,
        supportedservices: 'Lb',
        isrecursive: true,
        vpcid: vpcid
      }).then(response => {
        this.networkCount = response.listnetworksresponse.count || 0
        this.networks = response.listnetworksresponse.network || []
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.addNetworkModalLoading = false
      })
      this.selectedTierForAutoScaling = null
    },
    onNetworkSearch (value) {
      this.searchQuery = value
      this.fetchNetworks()
    },
    handleChangeNetworkPage (page, pageSize) {
      this.networkPage = page
      this.networkPageSize = pageSize
      this.fetchNetworks()
    },
    handleChangeNetworkPageSize (currentPage, pageSize) {
      this.networkPage = currentPage
      this.networkPageSize = pageSize
      this.fetchNetworks()
    },
    handleAssignToLBRule (data) {
      const vmIDIpMap = {}

      let selectedVmCount = 0
      let count = 0
      let innerCount = 0
      this.newRule.vmguestip.forEach(ip => {
        if (Array.isArray(ip)) {
          ip.forEach(i => {
            vmIDIpMap[`vmidipmap[${innerCount}].vmid`] = this.newRule.virtualmachineid[count]
            vmIDIpMap[`vmidipmap[${innerCount}].vmip`] = i
            innerCount++
          })
        } else {
          vmIDIpMap[`vmidipmap[${innerCount}].vmid`] = this.newRule.virtualmachineid[count]
          vmIDIpMap[`vmidipmap[${innerCount}].vmip`] = ip
          innerCount++
        }
        if (this.newRule.virtualmachineid[count]) {
          selectedVmCount++
        }
        count++
      })

      if (selectedVmCount === 0) {
        this.fetchData()
        return
      }

      this.loading = true
      api('assignToLoadBalancerRule', {
        id: data,
        ...vmIDIpMap
      }).then(response => {
        this.$pollJob({
          jobId: response.assigntoloadbalancerruleresponse.jobid,
          successMessage: this.$t('message.success.assign.vm'),
          successMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          errorMessage: this.$t('message.assign.vm.failed'),
          errorMethod: () => {
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          },
          loadingMessage: this.$t('message.assign.vm.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.parentFetchData()
            this.parentToggleLoading()
            this.fetchData()
            this.closeModal()
          }
        })
      })
    },
    handleAddNewRule () {
      if (this.loading) return
      this.loading = true

      if (this.selectedRule) {
        this.handleAssignToLBRule(this.selectedRule.id)
        return
      } else if (!this.checkNewRule()) {
        this.loading = false
        return
      }

      const networkId = this.selectedTierForAutoScaling != null ? this.selectedTierForAutoScaling
        : ('vpcid' in this.resource && !('associatednetworkid' in this.resource)) ? this.selectedTier : this.resource.associatednetworkid
      api('createLoadBalancerRule', {
        openfirewall: false,
        networkid: networkId,
        publicipid: this.resource.id,
        algorithm: this.newRule.algorithm,
        name: this.newRule.name,
        privateport: this.newRule.privateport,
        protocol: this.newRule.protocol,
        publicport: this.newRule.publicport,
        cidrlist: this.newRule.cidrlist
      }).then(response => {
        this.addVmModalVisible = false
        this.addNetworkModalVisible = false
        this.handleAssignToLBRule(response.createloadbalancerruleresponse.id)
        this.associatednetworkid = networkId
      }).catch(error => {
        this.$notifyError(error)
        this.loading = false
      })

      // assigntoloadbalancerruleresponse.jobid
    },
    closeModal () {
      this.selectedRule = null
      this.tagsModalVisible = false
      this.stickinessModalVisible = false
      this.stickinessModalLoading = false
      this.selectedStickinessPolicy = null
      this.stickinessPolicyMethod = 'LbCookie'
      this.editRuleModalVisible = false
      this.editRuleModalLoading = false
      this.addVmModalLoading = false
      this.addVmModalNicLoading = false
      this.showConfirmationAction = false
      this.vms = []
      this.nics = []
      this.addVmModalVisible = false
      this.newRule.virtualmachineid = []
      this.addNetworkModalLoading = false
      this.addNetworkModalVisible = false
      this.selectedTierForAutoScaling = null
    },
    handleChangePage (page, pageSize) {
      this.page = page
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangePageSize (currentPage, pageSize) {
      this.page = currentPage
      this.pageSize = pageSize
      this.fetchData()
    },
    handleChangeVmPage (page, pageSize) {
      this.vmPage = page
      this.vmPageSize = pageSize
      this.fetchVirtualMachines()
    },
    handleChangeVmPageSize (currentPage, pageSize) {
      this.vmPage = currentPage
      this.vmPageSize = pageSize
      this.fetchVirtualMachines()
    },
    onSearch (value) {
      this.searchQuery = value
      this.fetchVirtualMachines()
    },
    fetchLBTungstenFabricHealthMonitor () {
      if (!('listTungstenFabricLBHealthMonitor' in this.$store.getters.apis)) {
        return
      }
      this.tungstenHealthMonitors = []
      this.loading = true
      this.lbRules.forEach(rule => {
        api('listTungstenFabricLBHealthMonitor', {
          listAll: true,
          lbruleid: rule.id
        }).then(response => {
          const healthmonitor = response?.listtungstenfabriclbhealthmonitorresponse?.healthmonitor || []
          if (healthmonitor.length > 0) {
            healthmonitor[0].lbruleid = rule.id
            this.tungstenHealthMonitors.push(...healthmonitor)
          }
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.loading = false
        })
      })
    },
    returnHealthMonitorLabel (id) {
      const match = this.tungstenHealthMonitors.filter(item => item.lbruleid === id)
      if (match.length > 0) {
        return match[0].type
      }
      return this.$t('label.configure')
    },
    openHealthMonitorModal (id) {
      const match = this.tungstenHealthMonitors.filter(item => item.lbruleid === id)
      this.healthMonitorParams.lbruleid = id
      if (match.length > 0) {
        this.healthMonitorParams.type = match[0].type
        this.healthMonitorParams.retry = match[0].retry
        this.healthMonitorParams.timeout = match[0].timeout
        this.healthMonitorParams.interval = match[0].interval
        this.healthMonitorParams.httpmethodtype = match[0].httpmethod
        this.healthMonitorParams.expectedcode = match[0].expectedcode
        this.healthMonitorParams.urlpath = match[0].urlpath
      }
      this.initMonitorForm()
      this.healthMonitorModal = true
    },
    closeMonitorModal () {
      this.healthMonitorModal = false
      this.healthMonitorParams = {
        type: 'PING',
        retry: 3,
        timeout: 5,
        interval: 5,
        httpmethodtype: 'GET',
        expectedcode: undefined,
        urlpath: '/'
      }
    },
    handleConfigHealthMonitor () {
      if (this.healthMonitorLoading) return

      this.monitorRef.value.validate().then(() => {
        const values = toRaw(this.monitorForm)

        this.healthMonitorParams.type = values.type
        this.healthMonitorParams.retry = values.retry
        this.healthMonitorParams.timeout = values.timeout
        this.healthMonitorParams.interval = values.interval
        if (values.type === 'HTTP') {
          this.healthMonitorParams.httpmethodtype = values.httpmethodtype
          this.healthMonitorParams.expectedcode = values.expectedcode
          this.healthMonitorParams.urlpath = values.urlpath
        }

        this.healthMonitorLoading = true
        api('updateTungstenFabricLBHealthMonitor', this.healthMonitorParams).then(json => {
          const jobId = json?.updatetungstenfabriclbhealthmonitorresponse?.jobid
          this.$pollJob({
            jobId: jobId,
            successMessage: this.$t('message.success.config.health.monitor'),
            successMethod: () => {
              this.parentToggleLoading()
              this.fetchData()
              this.closeMonitorModal()
              this.healthMonitorLoading = false
            },
            errorMessage: this.$t('message.config.health.monitor.failed'),
            errorMethod: () => {
              this.parentToggleLoading()
              this.fetchData()
              this.closeMonitorModal()
              this.healthMonitorLoading = false
            },
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.parentToggleLoading()
              this.fetchData()
              this.closeMonitorModal()
              this.healthMonitorLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
        }).finally(() => {
          this.healthMonitorLoading = false
        })
      }).catch((error) => {
        this.monitorRef.value.scrollToField(error.errorFields[0].name)
      }).finally(() => {
        this.healthMonitorLoading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
  .rule {

    &-container {
      display: flex;
      flex-direction: column;
      width: 100%;

      @media (min-width: 760px) {
        margin-right: -20px;
        margin-bottom: -10px;
      }

    }

    &__row {
      display: flex;
      flex-wrap: wrap;
    }

    &__item {
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        flex: 1;
      }

    }

    &__title {
      font-weight: bold;
    }

  }

  .add-btn {
    width: 100%;
    padding-top: 15px;
    padding-bottom: 15px;
    height: auto;
  }

  .add-actions {
    display: flex;
    justify-content: flex-end;
    margin-right: -20px;
    margin-bottom: 20px;

    @media (min-width: 760px) {
      margin-top: 20px;
    }

    button {
      margin-right: 20px;
    }

  }

  .form {
    display: flex;
    margin-right: -20px;
    flex-direction: column;
    align-items: flex-start;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__required {
      margin-right: 5px;
      color: red;
    }

    .error-text {
      display: none;
      color: red;
      font-size: 0.8rem;
    }

    .error {

      input {
        border-color: red;
      }

      .error-text {
        display: block;
      }

    }

    &--column {
      flex-direction: column;
      margin-right: 0;
      align-items: flex-end;

      .form__item {
        width: 100%;
        padding-right: 0;
      }

    }

    &__item {
      display: flex;
      flex-direction: column;
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 1200px) {
        margin-bottom: 0;
        flex: 1;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

      &__input-container {
        display: flex;

        input {

          &:not(:last-child) {
            margin-right: 10px;
          }

        }

      }

    }

    &__label {
      font-weight: bold;
    }

  }

  .rule-action {
    margin-bottom: 10px;
  }

  .tags-modal {

    .ant-divider {
      margin-top: 0;
    }

  }

  .tags {
    margin-bottom: 10px;
  }

  .add-tags {
    display: flex;
    align-items: center;
    justify-content: space-between;

    &__input {
      margin-right: 10px;
    }

    &__label {
      margin-bottom: 5px;
      font-weight: bold;
    }

  }

  .tags-container {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 10px;
  }

  .add-tags-done {
    display: block;
    margin-left: auto;
  }

  .modal-loading {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: rgba(0,0,0,0.5);
    z-index: 1;
    color: #1890ff;
    font-size: 2rem;
  }

  .ant-list-item {
    display: flex;
    flex-direction: column;
    align-items: flex-start;

    @media (min-width: 760px) {
      flex-direction: row;
      align-items: center;
    }

  }

  .rule-instance-collapse {
    width: 100%;
    margin-left: -15px;

    .ant-collapse-item {
      border: 0;
    }

  }

  .rule-instance-list {
    display: flex;
    flex-direction: column;

    &__item {
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      div {
        margin-left: 25px;
        margin-bottom: 10px;
      }
    }
  }

  .edit-rule {

    .ant-select {
      width: 100%;
    }

    &__item {
      margin-bottom: 10px;
    }

    &__label {
      margin-bottom: 5px;
      font-weight: bold;
    }

  }

  .vm-modal {

    &__header {
      display: flex;

      span {
        flex: 1;
        font-weight: bold;
        margin-right: 10px;
      }

    }

    &__item {
      display: flex;
      margin-top: 10px;

      span,
      label {
        display: block;
        flex: 1;
        margin-right: 10px;
      }

    }

  }

  .custom-ant-form {
    .ant-form-item-label {
      font-weight: bold;
      line-height: 1;
    }
    .ant-form-item {
      margin-bottom: 10px;
    }
  }

  .custom-ant-list {
    .ant-list-item-action {
      margin-top: 10px;
      margin-left: 0;

      @media (min-width: 760px) {
        margin-top: 0;
        margin-left: 24px;
      }

    }
  }

  .rule-instance-collapse {
    .ant-collapse-header,
    .ant-collapse-content {
      margin-left: -12px;
    }
  }

  .rule {
    .ant-list-item-content-single {
      width: 100%;

      @media (min-width: 760px) {
        width: auto;
      }

    }
  }

  .pagination {
    margin-top: 20px;
    text-align: right;
  }

  .actions {
    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }
  }

  .list-view {
    overflow-y: auto;
    display: block;
    width: 100%;
  }

  .filter {
    display: block;
    width: 240px;
    margin-bottom: 10px;
  }

  .input-search {
    margin-bottom: 10px;
    width: 50%;
    float: right;
  }
</style>
