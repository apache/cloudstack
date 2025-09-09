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
    <a-form-item v-if="imageTypeSelectionAllowed" :label="$t('label.type')" name="imagetype" ref="imagetype">
      <a-radio-group
        v-model:value="localSelectedImageType"
        button-style="solid"
        :disabled="imagePreSelected"
        @change="emitChangeImageType()">
        <a-radio-button value="templateid">{{ $t('label.template') }}</a-radio-button>
        <a-radio-button value="isoid">{{ $t('label.iso') }}</a-radio-button>
        <a-radio-button value="volumeid">{{ $t('label.volume') }}</a-radio-button>
        <a-radio-button value="snapshotid">{{ $t('label.snapshot') }}</a-radio-button>
      </a-radio-group>
      <div style="margin-top: 5px; margin-bottom: 5px;">
        {{ $t('message.' + localSelectedImageType.replace('id', '') + '.desc') }}
      </div>
    </a-form-item>
    <a-form-item
      :label="$t('label.os')"
      name="guestoscategoryid"
      ref="guestoscategoryid"
      v-if="!guestOsCategoriesSelectionDisallowed">
      <block-radio-group-select
        :maxBlocks="16"
        :items="guestOsCategories"
        :selectedValue="localSelectedGuestOsCategoryId"
        :horizontalGutter="6"
        :verticalGutter="6"
        blockSize="square"
        @change="handleGuestOsCategoryChange">
        <template #radio-option="{ item }">
          <div class="radio-option">
            <div class="radio-opion__icon">
              <resource-icon v-if="item.icon && item.icon.base64image" :image="item.icon.base64image" size="os" style="margin-bottom: 2px; margin-left: 1px" />
              <font-awesome-icon v-else-if="['-1', '0'].includes(item.id)" :icon="['fas', item.id === '0' ? 'user' : 'images']" size="2x" :style="categoryFontAwesomeIconStyle" />
              <os-logo v-else size="2x" :os-name="item.name" />
            </div>
            <a-tooltip placement="top" :title="item.name">
              <div class="ellipsis">{{ item.name }}</div>
            </a-tooltip>
          </div>
        </template>
        <template #select-option="{ item }">
          <span>
            <resource-icon v-if="item.icon && item.icon.base64image" :image="item.icon.base64image" size="2x" style="margin-right: 5px"/>
            <font-awesome-icon
              v-else-if="item.id === '0'"
              :icon="['fas', 'user']"
              size="2x"
              :style="[$store.getters.darkMode ? { color: 'rgba(255, 255, 255, 0.65)' } : { color: '#666' }]"
            />
            <os-logo v-else :os-name="item.name" style="margin-right: 5px" />
            {{ item.name }}
          </span>
        </template>
      </block-radio-group-select>
    </a-form-item>
    <a-card>
      <os-based-image-selection-search-view
        v-if="!imagePreSelected"
        class="search-input"
        :filtersDisabled="searchFiltersDisabled"
        @search="handleImageSearch">
      </os-based-image-selection-search-view>
      <a-spin :spinning="imagesLoading">
        <os-based-image-radio-group
          :imagesList="imagesList"
          :itemCount="imagesCount"
          :input-decorator="localSelectedImageType"
          :selected="selectedImageId"
          :preFillContent="preFillContent"
          @emit-update-image="updateImage"
          @handle-search-filter="($event) => eventPagination($event)"
        />
      </a-spin>
      <div v-if="diskSizeSelectionAllowed">
        <div>
          {{ $t('label.override.rootdisk.size') }}
          <a-switch
            v-model:checked="localRootDiskOverrideChecked"
            :disabled="rootDiskOverrideDisabled"
            @change="handleRootDiskOverrideCheckedChange"
            style="margin-left: 10px;"/>
          <div v-if="diskSizeSelectionDeployAsIsMessageVisible">  {{ $t('message.deployasis') }} </div>
        </div>
        <disk-size-selection
          v-if="showRootDiskSizeChanger"
          :input-decorator="diskSizeSelectionInputDecorator"
          :preFillContent="preFillContent"
          :isCustomized="true"
          :minDiskSize="preFillContent.minrootdisksize"
          @update-disk-size="emitUpdateDiskSize"
          style="margin-top: 10px;"/>
      </div>
      <a-form-item :label="$t('label.hypervisor')" v-if="localSelectedImageType === 'isoid'">
        <a-select
          v-model:value="localSelectedIsoHypervisor"
          :preFillContent="preFillContent"
          :options="isoHypervisorItems"
          @change="handleIsoHypervisorChange"
          showSearch
          optionFilterProp="label"
          :filterOption="filterOption" />
      </a-form-item>
    </a-card>
  </div>
</template>

<script>
import BlockRadioGroupSelect from '@/components/widgets/BlockRadioGroupSelect.vue'
import ResourceIcon from '@/components/view/ResourceIcon'
import OsLogo from '@/components/widgets/OsLogo'
import OsBasedImageSelectionSearchView from '@views/compute/wizard/OsBasedImageSelectionSearchView'
import OsBasedImageRadioGroup from '@views/compute/wizard/OsBasedImageRadioGroup'
import DiskSizeSelection from '@views/compute/wizard/DiskSizeSelection'

export default {
  name: 'OsBasedImageSelection',
  components: {
    BlockRadioGroupSelect,
    ResourceIcon,
    OsLogo,
    OsBasedImageSelectionSearchView,
    OsBasedImageRadioGroup,
    DiskSizeSelection
  },
  props: {
    imageTypeSelectionAllowed: {
      type: Boolean,
      default: true
    },
    selectedImageType: {
      type: String,
      default: 'templateid'
    },
    imagePreSelected: {
      type: Boolean,
      default: false
    },
    guestOsCategoriesSelectionDisallowed: {
      type: Boolean,
      default: false
    },
    guestOsCategories: {
      type: Array,
      default: () => []
    },
    guestOsCategoriesLoading: {
      type: Boolean,
      default: false
    },
    selectedGuestOsCategoryId: {
      type: String,
      default: undefined
    },
    imageItems: {
      type: Object,
      default: () => {}
    },
    imagesLoading: {
      type: Boolean,
      default: false
    },
    diskSizeSelectionAllowed: {
      type: Boolean,
      default: true
    },
    diskSizeSelectionDeployAsIsMessageVisible: {
      type: Boolean,
      default: false
    },
    diskSizeSelectionInputDecorator: {
      type: String,
      default: 'rootdisksize'
    },
    rootDiskOverrideDisabled: {
      type: Boolean,
      default: false
    },
    rootDiskOverrideChecked: {
      type: Boolean,
      default: false
    },
    isoHypervisorItems: {
      type: Array,
      default: () => []
    },
    selectedIsoHypervisor: {
      type: String,
      default: undefined
    },
    filterOption: {
      type: Function,
      required: true
    },
    preFillContent: {
      type: Object,
      default: () => {}
    }
  },
  data () {
    return {
      filterType: 'executable',
      selectedImageId: '',
      imageSearchFilters: {},
      showRootDiskSizeChanger: false,
      // Local data properties to mirror props
      localSelectedImageType: this.selectedImageType,
      localSelectedGuestOsCategoryId: this.selectedGuestOsCategoryId,
      localRootDiskOverrideChecked: this.rootDiskOverrideChecked,
      localSelectedIsoHypervisor: this.selectedIsoHypervisor
    }
  },
  mounted () {
    this.filterType = this.defaultImageFilter
  },
  watch: {
    selectedImageType (newValue) {
      this.localSelectedImageType = newValue
    },
    guestOsCategories (newValue) {
      this.imageSearchFilters = {}
    },
    selectedGuestOsCategoryId (newValue) {
      this.localSelectedGuestOsCategoryId = newValue
      this.updateImageFilterType()
    },
    rootDiskOverrideChecked (newValue) {
      this.localRootDiskOverrideChecked = newValue
    },
    selectedIsoHypervisor (newValue) {
      this.localSelectedIsoHypervisor = newValue
    }
  },
  computed: {
    defaultImageFilter () {
      return ['DomainAdmin', 'User'].includes(this.$store.getters.userInfo.roletype) ? 'executable' : 'all'
    },
    imagesList () {
      if (!this.localSelectedImageType || !this.imageItems || !this.imageItems[this.filterType]) {
        return []
      }
      const imageTypeKey = this.localSelectedImageType.slice(0, -2)
      return this.imageItems[this.filterType][imageTypeKey] || []
    },
    imagesCount () {
      return this.imageItems[this.filterType] ? this.imageItems[this.filterType].count || 0 : 0
    },
    selectedCategory () {
      if (this.localSelectedGuestOsCategoryId && this.guestOsCategories) {
        return this.guestOsCategories.find(option => option.id === this.localSelectedGuestOsCategoryId)
      }
      return null
    },
    categoryFontAwesomeIconStyle () {
      return [this.$store.getters.darkMode ? { color: 'rgba(255, 255, 255, 0.65)' } : { color: '#666' }]
    },
    searchFiltersDisabled () {
      return this.selectedCategory?.disableimagefilters
    }
  },
  emits: ['change-image-type', 'change-guest-os-category', 'update-image', 'handle-image-search-filter', 'change-root-disk-override-checked', 'update-disk-size', 'change-iso-hypervisor'],
  methods: {
    emitChangeImageType () {
      this.$emit('change-image-type', this.localSelectedImageType)
    },
    handleGuestOsCategoryChange (value) {
      this.localSelectedGuestOsCategoryId = value
      this.$emit('change-guest-os-category', this.localSelectedGuestOsCategoryId)
    },
    updateImage (decorator, id) {
      this.selectedImageId = id
      this.$emit('update-image', decorator, id)
    },
    handleImageSearch (searchFilters) {
      this.imageSearchFilters = {
        page: 1,
        pageSize: 10
      }
      Object.assign(this.imageSearchFilters, searchFilters)
      this.updateImageFilterType()
      this.emitSearchFilter()
    },
    updateImageFilterType () {
      this.filterType = this.defaultImageFilter
      if (this.localSelectedGuestOsCategoryId === '0') {
        this.filterType = 'self'
      } else {
        if (this.imageSearchFilters?.featured) {
          this.filterType = 'featured'
        } else if (this.imageSearchFilters?.public) {
          this.filterType = 'community'
        }
      }
    },
    eventPagination (pagination) {
      Object.assign(this.imageSearchFilters, pagination)
      this.emitSearchFilter()
    },
    emitSearchFilter () {
      this.$emit('handle-image-search-filter', this.imageSearchFilters)
    },
    changeFilterType (value) {
      this.filterType = value
    },
    handleRootDiskOverrideCheckedChange (value) {
      this.showRootDiskSizeChanger = value
      this.$emit('change-root-disk-override-checked', value)
    },
    emitUpdateDiskSize (decorator, value) {
      this.$emit('update-disk-size', decorator, value)
    },
    handleIsoHypervisorChange (hypervisor) {
      this.$emit('change-iso-hypervisor', hypervisor)
    }
  }
}
</script>

<style lang="less" scoped>
  .search-input {
    z-index: 8;

    @media (max-width: 600px) {
      position: relative;
      width: 100%;
      top: 0;
      right: 0;
    }
  }

  :deep(.ant-tabs-nav-scroll) {
    min-height: 45px;
  }

  .radio-option {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
    padding-top: 8px;
  }

  .ellipsis {
    max-width: 80px;
    flex-grow: 1;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .radio-opion__icon {
    width: 30px;
    height: 30px;
    object-fit: contain;
  }
</style>
