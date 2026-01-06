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
  <a-list
    size="small"
    :loading="loading"
    :dataSource="usageList" >
    <template #renderItem="{ item }">
      <a-list-item class="list-item" v-if="!($route.meta.name === 'project' && item === 'project')">
        <div class="list-item__container">
          <strong>
            {{ $t('label.' + item + 'limit') }}
          </strong>
          ({{ resource[item + 'available'] === '-1' ? $t('label.unlimited') : resource[item + 'available'] }} {{ $t('label.available') }})
          <div class="list-item__vals">
            <div class="list-item__data">
              {{ $t('label.used') }} / {{ $t('label.limit') }} : {{ resource[item + 'total'] }} / {{ resource[item + 'limit'] === '-1' ? $t('label.unlimited') : resource[item + 'limit'] }}
            </div>
            <a-progress
              status="normal"
              :percent="parseFloat(getPercentUsed(resource[item + 'total'], resource[item + 'limit']))"
              :format="p => resource[item + 'limit'] !== '-1' && resource[item + 'limit'] !== 'Unlimited' ? p.toFixed(2) + '%' : ''" />
          </div>
          <a-collapse
              v-if="taggedUsage[item]"
              class="list-item__collapse"
              @change="handleCollapseChange(item)">
            <a-collapse-panel key="1" :header="collpaseActive[item] ? $t('label.tagged.limits') : $t('label.tagged.limits') + ' - ' + this.tagData[item].tagsasstring">
              <a-list
                size="small"
                :loading="loading"
                :dataSource="taggedUsage[item]" >
                <template #renderItem="{ item }">
                  <a-list-item class="sub-list-item">
                    <div class="sub-list-item__container">
                      <strong>
                        {{ '#' + item.tag }}
                      </strong>
                      ({{ item.available === '-1' ? $t('label.unlimited') : item.available }} {{ $t('label.available') }})
                      <div class="sub-list-item__vals">
                        <div class="sub-list-item__data">
                          {{ $t('label.used') }} / {{ $t('label.limit') }} : {{ item.total }} / {{ item.limit === '-1' ? $t('label.unlimited') : item.limit }}
                        </div>
                        <a-progress
                          status="normal"
                          :percent="parseFloat(getPercentUsed(item.total, item.limit))"
                          :format="p => item.limit !== '-1' && item.limit !== 'Unlimited' ? p.toFixed(2) + '%' : ''" />
                      </div>
                    </div>
                  </a-list-item>
                </template>
              </a-list>
            </a-collapse-panel>
          </a-collapse>
        </div>
      </a-list-item>
    </template>
  </a-list>
</template>

<script>
import _ from 'lodash'

export default {
  name: 'ResourceCountUsageTab',
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
      usageList: [
        'vm', 'cpu', 'memory', 'primarystorage', 'volume', 'ip', 'network',
        'vpc', 'secondarystorage', 'snapshot', 'template', 'project'
      ],
      taggedUsage: {},
      tagData: {},
      collpaseActive: {}
    }
  },
  created () {
    this.updateTaggedUsage()
  },
  watch: {
    resource: {
      handler () {
        this.updateTaggedUsage()
      }
    }
  },
  computed: {
    resourceTypeToNameMap () {
      return {
        0: 'vm',
        8: 'cpu',
        9: 'memory',
        2: 'volume',
        10: 'primarystorage'
      }
    }
  },
  methods: {
    getPercentUsed (total, limit) {
      return (limit === 'Unlimited') ? 0 : (total / limit) * 100
    },
    addTaggedUsageToList (taggedResource) {
      var type = this.resourceTypeToNameMap['' + taggedResource.resourcetype]
      if (!type) {
        return
      }
      var typeResourceList = []
      if (this.taggedUsage[type]) {
        typeResourceList = this.taggedUsage[type]
      }
      if (taggedResource.limit === -1) {
        taggedResource.limit = '-1'
      }
      if (taggedResource.available === -1) {
        taggedResource.available = '-1'
      }
      if (['primarystorage'].includes(type)) {
        taggedResource.limit = taggedResource.limit === '-1' ? '-1' : this.$bytesToGiB(taggedResource.limit)
        taggedResource.total = this.$bytesToGiB(taggedResource.total)
        taggedResource.available = taggedResource.available === '-1' ? '-1' : this.$bytesToGiB(taggedResource.available)
      }
      typeResourceList.push(taggedResource)
      typeResourceList = typeResourceList.sort((a, b) => a.tag.localeCompare(b.tag))
      this.taggedUsage[type] = typeResourceList
    },
    updateTaggedUsage () {
      this.taggedUsage = {}
      this.tagData = {}
      if (!this.resource || !this.resource.taggedresources) {
        return
      }
      for (var taggedResource of this.resource.taggedresources) {
        this.addTaggedUsageToList(taggedResource)
      }
      for (var i in this.taggedUsage) {
        var tags = _.map(this.taggedUsage[i], 'tag')
        var tagsAsString = '#' + tags.join(', #')
        this.tagData[i] = {
          tags: tags,
          tagsasstring: tagsAsString
        }
      }
    },
    handleCollapseChange (type) {
      if (this.collpaseActive[type]) {
        this.collpaseActive[type] = null
        return
      }
      this.collpaseActive[type] = true
    }
  }
}
</script>

<style scoped lang="scss">
  .list-item {

    &__container {
      max-width: 90%;
      width: 100%;

      @media (min-width: 760px) {
        max-width: 95%;
      }
    }

    &__collapse {
      margin-top: 10px;
      margin-bottom: 10px;
    }

    &__title {
      font-weight: bold;
    }

    &__data {
      margin-right: 20px;
      white-space: nowrap;
    }

    &__vals {
      margin-top: 10px;
      @media (min-width: 760px) {
        display: flex;
      }
    }
  }
  .sub-list-item {

    &__container {
      max-width: 90%;
      width: 100%;

      @media (min-width: 760px) {
        max-width: 95%;
      }
    }

    &__title {
      font-weight: bold;
    }

    &__data {
      margin-right: 20px;
      white-space: nowrap;
    }

    &__vals {
      margin-top: 10px;
      @media (min-width: 760px) {
        display: flex;
      }
    }
  }
</style>
