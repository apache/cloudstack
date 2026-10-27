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
  <a-spin :spinning="componentLoading">
    <div class="form" v-ctrl-enter="handleAdd">
      <div class="form__label">
        <a-input v-model:value="newRoute" :placeholder="$t('label.cidr.destination.network')" v-focus="true"></a-input>
      </div>
      <div class="form__label" v-if="this.$route.fullPath.startsWith('/vpc')">
        <div :span="24" class="form__label">via</div>
      </div>
      <div class="form__label" v-if="this.$route.fullPath.startsWith('/vpc')">
        <a-input v-model:value="nexthop" :placeholder="$t('label.nexthop')"></a-input>
      </div>
      <a-button type="primary" :disabled="!('createStaticRoute' in $store.getters.apis)" @click="handleAdd">{{ $t('label.add.route') }}</a-button>
    </div>

    <a-divider/>
    <a-table
      size="small"
      style="overflow-y: auto"
      :loading="loading"
      :columns="columns"
      :dataSource="routes"
      :pagination="false"
      :rowKey="record => record.id">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.key === 'vpcgatewayip'">
          <router-link :to="{ path: '/privategw/' + record.vpcgatewayid }" >{{ text }}</router-link>
        </template>
        <template v-if="column.key === 'actions'">
          <tooltip-button :tooltip="$t('label.edit.tags')" icon="tag-outlined" @onClick="() => openTagsModal(record)" />
          <tooltip-button
            :tooltip="$t('label.delete')"
            :disabled="!('deleteStaticRoute' in $store.getters.apis)"
            icon="delete-outlined"
            type="primary"
            :danger="true"
            @onClick="() => handleDelete(record)" />
        </template>
      </template>
    </a-table>

    <a-modal
      :title="$t('label.edit.tags')"
      :visible="tagsModalVisible"
      :footer="null"
      :closable="true"
      :maskClosable="false"
      @cancel="tagsModalVisible = false">
      <a-spin v-if="tagsLoading"></a-spin>

      <div v-else v-ctrl-enter="handleAddTag">
        <a-form :ref="formRef" :model="form" :rules="rules" class="add-tags">
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.key') }}</p>
            <a-form-item name="key" ref="key">
              <a-input
                v-focus="true"
                v-model:value="form.key" />
            </a-form-item>
          </div>
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.value') }}</p>
            <a-form-item name="value" ref="value">
              <a-input v-model:value="form.value" />
            </a-form-item>
          </div>
          <a-button type="primary" :disabled="!('createTags' in $store.getters.apis)" @click="handleAddTag">{{ $t('label.add') }}</a-button>
        </a-form>

        <a-divider style="margin-top: 0;" />

        <div class="tags-container">
          <div class="tags" v-for="(tag, index) in tags" :key="index">
            <a-tag :key="index" :closable="'deleteTags' in $store.getters.apis" @close="() => handleDeleteTag(tag)">
              {{ tag.key }} = {{ tag.value }}
            </a-tag>
          </div>
        </div>

        <a-button class="add-tags-done" @click="tagsModalVisible = false" type="primary">{{ $t('label.ok') }}</a-button>
      </div>

    </a-modal>
  </a-spin>
</template>

<script>
import { ref, reactive, toRaw } from 'vue'
import { getAPI, postAPI } from '@/api'
import TooltipButton from '@/components/widgets/TooltipButton'
import TooltipLabel from '@/components/widgets/TooltipLabel.vue'

export default {
  name: 'StaticRoutesTab',
  components: {
    TooltipLabel,
    TooltipButton
  },
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
      routes: [],
      componentLoading: false,
      selectedRule: null,
      tagsModalVisible: false,
      tags: [],
      tagsLoading: false,
      newRoute: null,
      nexthop: null,
      columns: [
        {
          title: this.$t('label.cidr.destination.network'),
          dataIndex: 'cidr'
        },
        {
          title: this.$t('label.vpc.gateway.ip'),
          key: 'vpcgatewayip',
          dataIndex: 'vpcgatewayip'
        },
        {
          title: this.$t('label.nexthop'),
          dataIndex: 'nexthop'
        },
        {
          title: this.$t('label.actions'),
          key: 'actions'
        }
      ]
    }
  },
  created () {
    this.initForm()
    this.fetchData()
  },
  watch: {
    loading (newData, oldData) {
      if (!newData && this.resource.id) {
        this.fetchData()
      }
    }
  },
  methods: {
    initForm () {
      this.formRef = ref()
      this.form = reactive({
        start: true
      })
      this.rules = reactive({
        key: [{ required: true, message: this.$t('message.specify.tag.key') }],
        value: [{ required: true, message: this.$t('message.specify.tag.value') }]
      })
    },
    fetchData () {
      this.componentLoading = true
      var params = {
        listAll: true
      }
      if (this.$route.fullPath.startsWith('/vpc')) {
        params.vpcid = this.resource.id
      } else {
        params.gatewayid = this.resource.id
      }
      getAPI('listStaticRoutes', params).then(json => {
        this.routes = json.liststaticroutesresponse.staticroute
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleAdd () {
      if (this.componentLoading) return
      if (!this.newRoute) return

      this.componentLoading = true
      var params = {
        cidr: this.newRoute
      }
      if (this.$route.fullPath.startsWith('/vpc')) {
        params.vpcid = this.resource.id
        if (this.nexthop) {
          params.nexthop = this.nexthop
        }
      } else {
        params.gatewayid = this.resource.id
      }
      postAPI('createStaticRoute', params).then(response => {
        this.$pollJob({
          jobId: response.createstaticrouteresponse.jobid,
          title: this.$t('message.success.add.static.route'),
          description: this.newRoute,
          successMethod: () => {
            this.fetchData()
            this.componentLoading = false
            this.newRoute = null
          },
          errorMessage: this.$t('message.add.static.route.failed'),
          errorMethod: () => {
            this.fetchData()
            this.componentLoading = false
          },
          loadingMessage: this.$t('message.add.static.route.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.componentLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.componentLoading = false
      })
    },
    handleDelete (route) {
      this.componentLoading = true
      postAPI('deleteStaticRoute', {
        id: route.id
      }).then(response => {
        this.$pollJob({
          jobId: response.deletestaticrouteresponse.jobid,
          title: this.$t('message.success.delete.static.route'),
          description: route.id,
          successMethod: () => {
            this.fetchData()
            this.componentLoading = false
          },
          errorMessage: this.$t('message.delete.static.route.failed'),
          errorMethod: () => {
            this.fetchData()
            this.componentLoading = false
          },
          loadingMessage: this.$t('message.delete.static.route.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchData()
            this.componentLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.fetchData()
        this.componentLoading = false
      })
    },
    fetchTags (route) {
      getAPI('listTags', {
        resourceId: route.id,
        resourceType: 'StaticRoute',
        listAll: true
      }).then(response => {
        this.tags = response.listtagsresponse.tag
      }).catch(error => {
        this.$notifyError(error)
      })
    },
    handleDeleteTag (tag) {
      this.tagsLoading = true
      postAPI('deleteTags', {
        'tags[0].key': tag.key,
        'tags[0].value': tag.value,
        resourceIds: this.selectedRule.id,
        resourceType: 'StaticRoute'
      }).then(response => {
        this.$pollJob({
          jobId: response.deletetagsresponse.jobid,
          successMessage: this.$t('message.success.delete.tag'),
          successMethod: () => {
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          },
          errorMessage: this.$t('message.delete.tag.failed'),
          errorMethod: () => {
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          },
          loadingMessage: this.$t('message.delete.tag.processing'),
          catchMessage: this.$t('error.fetching.async.job.result'),
          catchMethod: () => {
            this.fetchTags(this.selectedRule)
            this.tagsLoading = false
          }
        })
      }).catch(error => {
        this.$notifyError(error)
        this.tagsLoading = false
      })
    },
    handleAddTag (e) {
      if (this.tagsLoading) return
      this.tagsLoading = true

      this.formRef.value.validate().then(() => {
        const values = toRaw(this.form)

        postAPI('createTags', {
          'tags[0].key': values.key,
          'tags[0].value': values.value,
          resourceIds: this.selectedRule.id,
          resourceType: 'StaticRoute'
        }).then(response => {
          this.$pollJob({
            jobId: response.createtagsresponse.jobid,
            successMessage: this.$t('message.success.add.tag'),
            successMethod: () => {
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            },
            errorMessage: this.$t('message.add.tag.failed'),
            errorMethod: () => {
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            },
            loadingMessage: this.$t('message.add.tag.processing'),
            catchMessage: this.$t('error.fetching.async.job.result'),
            catchMethod: () => {
              this.fetchTags(this.selectedRule)
              this.tagsLoading = false
            }
          })
        }).catch(error => {
          this.$notifyError(error)
          this.tagsLoading = false
        }).finally(() => { this.tagsLoading = false })
      }).catch(error => {
        this.formRef.value.scrollToField(error.errorFields[0].name)
      })
    },
    openTagsModal (route) {
      this.selectedRule = route
      this.fetchTags(this.selectedRule)
      this.tagsModalVisible = true
    }
  }
}
</script>

<style lang="scss" scoped>

  .list {
    padding-top: 20px;

    &__item {
      display: flex;
      justify-content: space-between;

      &:not(:last-child) {
        margin-bottom: 20px;
      }
    }

    &__label {
      font-weight: bold;
    }

  }

  .actions {
    display: flex;

    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
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

  .form {
    display: flex;
    margin-right: -20px;
    margin-bottom: 20px;
    flex-direction: column;
    align-items: flex-start;

    @media (min-width: 760px) {
      flex-direction: row;
    }

    &__item {
      display: flex;
      flex-direction: column;
      flex: 1;
      padding-right: 20px;
      margin-bottom: 20px;

      @media (min-width: 760px) {
        margin-bottom: 0;
      }

      input,
      .ant-select {
        margin-top: auto;
      }

    }

    &__label {
      font-size: 18px;
      font-weight: bold;
    }
  }
</style>
