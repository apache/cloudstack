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
    <div class="new-route">
      <a-input v-model="newRoute" icon="plus" :placeholder="$t('label.cidr.destination.network')" autoFocus></a-input>
      <a-button type="primary" :disabled="!('createStaticRoute' in $store.getters.apis)" @click="handleAdd">{{ $t('label.add.route') }}</a-button>
    </div>

    <div class="list">
      <div v-for="(route, index) in routes" :key="index" class="list__item">
        <div class="list__col">
          <div class="list__label">{{ $t('label.cidr.destination.network') }}</div>
          <div>{{ route.cidr }}</div>
        </div>
        <div class="actions">
          <tooltip-button :tooltip="$t('label.edit.tags')" icon="tag" @click="() => openTagsModal(route)" />
          <tooltip-button :tooltip="$t('label.delete')" :disabled="!('deleteStaticRoute' in $store.getters.apis)" icon="delete" type="danger" @click="() => handleDelete(route)" />
        </div>
      </div>
    </div>

    <a-modal title="Edit Tags" v-model="tagsModalVisible" :footer="null" :maskClosable="false">
      <a-spin v-if="tagsLoading"></a-spin>

      <div v-else>
        <a-form :form="newTagsForm" class="add-tags" @submit="handleAddTag">
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.key') }}</p>
            <a-form-item>
              <a-input
                autoFocus
                v-decorator="['key', { rules: [{ required: true, message: this.$t('message.specifiy.tag.key')}] }]" />
            </a-form-item>
          </div>
          <div class="add-tags__input">
            <p class="add-tags__label">{{ $t('label.value') }}</p>
            <a-form-item>
              <a-input v-decorator="['value', { rules: [{ required: true, message: this.$t('message.specifiy.tag.value')}] }]" />
            </a-form-item>
          </div>
          <a-button type="primary" :disabled="!('createTags' in $store.getters.apis)" html-type="submit">{{ $t('label.add') }}</a-button>
        </a-form>

        <a-divider style="margin-top: 0;"></a-divider>

        <div class="tags-container">
          <div class="tags" v-for="(tag, index) in tags" :key="index">
            <a-tag :key="index" :closable="'deleteTags' in $store.getters.apis" :afterClose="() => handleDeleteTag(tag)">
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
import { api } from '@/api'
import TooltipButton from '@/components/view/TooltipButton'

export default {
  name: 'StaticRoutesTab',
  components: {
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
      newTagsForm: this.$form.createForm(this),
      tags: [],
      tagsLoading: false,
      newRoute: null
    }
  },
  created () {
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
    fetchData () {
      this.componentLoading = true
      api('listStaticRoutes', { gatewayid: this.resource.id }).then(json => {
        this.routes = json.liststaticroutesresponse.staticroute
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.componentLoading = false
      })
    },
    handleAdd () {
      if (!this.newRoute) return

      this.componentLoading = true
      api('createStaticRoute', {
        cidr: this.newRoute,
        gatewayid: this.resource.id
      }).then(response => {
        this.$pollJob({
          jobId: response.createstaticrouteresponse.jobid,
          successMethod: () => {
            this.fetchData()
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('message.success.add.static.route'),
              jobid: response.createstaticrouteresponse.jobid,
              status: 'progress'
            })
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
      api('deleteStaticRoute', {
        id: route.id
      }).then(response => {
        this.$pollJob({
          jobId: response.deletestaticrouteresponse.jobid,
          successMethod: () => {
            this.fetchData()
            this.$store.dispatch('AddAsyncJob', {
              title: this.$t('message.success.delete.static.route'),
              jobid: response.deletestaticrouteresponse.jobid,
              status: 'progress'
            })
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
      api('listTags', {
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
      api('deleteTags', {
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
      this.tagsLoading = true

      e.preventDefault()
      this.newTagsForm.validateFields((err, values) => {
        if (err) {
          this.tagsLoading = false
          return
        }

        api('createTags', {
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
        })
      })
    },
    openTagsModal (route) {
      this.selectedRule = route
      this.newTagsForm.resetFields()
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

  .new-route {
    display: flex;
    padding-top: 10px;

    input {
      margin-right: 10px;
    }

    button {
      &:not(:last-child) {
        margin-right: 10px;
      }
    }

  }

</style>
