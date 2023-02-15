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
  <div class="side-setting">
    <setting-item :title="$t('label.theme.page.style.setting')" view-type="item">
      <a-radio-group
        class="setting-group"
        name="themeGroup"
        v-model:value="layoutMode"
        @change="switchLayoutMode">
        <setting-item
          view-type="radio-group"
          :items="pageStyles"
          :checked="layoutMode"></setting-item>
      </a-radio-group>
    </setting-item>

    <setting-item :title="$t('label.theme.color')" view-type="item" style="display: block;">
      <a-radio-group
        class="setting-group"
        name="colorGroup"
        :default-value="colorPick"
        @change="switchColor">
        <setting-item
          view-type="radio-group"
          :items="colors"
          :checked="colorPick"></setting-item>
      </a-radio-group>

      <a-divider style="margin-top: 45px;" />
    </setting-item>

    <setting-item
      v-if="!projectView"
      :title="$t('label.theme.navigation.setting')"
      view-type="item">

      <a-list :split="false">
        <a-list-item>
          <div class="input-group">
            <label>{{ $t('label.theme.navigation.bgColor') }}</label>
            <div class="color-picker" :style="{ backgroundColor: navBgColorPick }">
              <a-input
                :disabled="layoutMode === 'dark'"
                type="color"
                v-model:value="navBgColorPick"
                @blur="(e) => updateSetting('@navigation-background-color', e.target.value)" />
            </div>
          </div>
        </a-list-item>
        <a-list-item>
          <div class="input-group">
            <label>{{ $t('label.theme.navigation.txtColor') }}</label>
            <div class="color-picker" :style="{ backgroundColor: navTextColorPick }">
              <a-input
                :disabled="layoutMode === 'dark'"
                type="color"
                v-model:value="navTextColorPick"
                @blur="(e) => updateSetting('@navigation-text-color', e.target.value)" />
            </div>
          </div>
        </a-list-item>
      </a-list>
    </setting-item>

    <setting-item
      v-if="projectView"
      :title="$t('label.theme.project.navigation.setting')"
      view-type="item">
      <a-list :split="false">
        <a-list-item>
          <div class="input-group">
            <label>{{ $t('label.theme.navigation.bgColor') }}</label>
            <div class="color-picker" :style="{ backgroundColor: projectNavBgColorPick }">
              <a-input
                type="color"
                v-model:value="projectNavBgColorPick"
                @blur="(e) => updateSetting('@project-nav-background-color', e.target.value)" />
            </div>
          </div>
        </a-list-item>
        <a-list-item>
          <div class="input-group">
            <label>{{ $t('label.theme.navigation.txtColor') }}</label>
            <div class="color-picker" :style="{ backgroundColor: projectNavTextColorPick }">
              <a-input
                type="color"
                v-model:value="projectNavTextColorPick"
                @blur="(e) => updateSetting('@project-nav-text-color', e.target.value)" />
            </div>
          </div>
        </a-list-item>
      </a-list>
    </setting-item>

    <div class="setting-action">
      <a-divider style="margin: 15px 0;" />
      <a-alert class="setting-action-alert" :message="$t('label.theme.alert')" type="warning" show-icon />
      <a-button
        class="setting-action-btn"
        @click="downloadSetting">
        <template #icon><download-outlined /></template>
        {{ $t('label.download.setting') }}
      </a-button>
      <a-button
        class="setting-action-btn"
        @click="resetSetting">
        <template #icon><undo-outlined /></template>
        {{ $t('label.reset.to.default') }}
      </a-button>
    </div>
  </div>
</template>

<script>
import SettingItem from '@/components/view/SettingItem'

export default {
  name: 'Setting',
  components: {
    SettingItem
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      layoutMode: this.$config.theme['@layout-mode'] || 'light',
      colorPick: this.$config.theme['@primary-color'],
      navBgColorPick: this.$config.theme['@navigation-background-color'],
      navTextColorPick: this.$config.theme['@navigation-text-color'],
      projectNavBgColorPick: this.$config.theme['@project-nav-background-color'],
      projectNavTextColorPick: this.$config.theme['@project-nav-text-color'],
      uiSettings: {},
      originalSetting: {}
    }
  },
  computed: {
    projectView () {
      return Boolean(this.$store.getters.project && this.$store.getters.project.id)
    },
    pageStyles () {
      const arrStyle = [
        {
          name: 'light',
          type: 'image-checkbox',
          icon: 'light'
        },
        {
          name: 'dark',
          type: 'image-checkbox',
          icon: 'dark'
        }
      ]
      return arrStyle
    },
    colors () {
      return [
        {
          name: 'daybreak.blue',
          type: 'color-checkbox',
          color: '#1890ff'
        },
        {
          name: 'dust.red',
          type: 'color-checkbox',
          color: '#f5222d'
        },
        {
          name: 'volcano',
          type: 'color-checkbox',
          color: '#fa541c'
        },
        {
          name: 'sunset.orange',
          type: 'color-checkbox',
          color: '#faad14'
        },
        {
          name: 'cyan',
          type: 'color-checkbox',
          color: '#13c2c2'
        },
        {
          name: 'polar.green',
          type: 'color-checkbox',
          color: '#52c41a'
        },
        {
          name: 'geek.blue',
          type: 'color-checkbox',
          color: '#2f54eb'
        },
        {
          name: 'golden.purple',
          type: 'color-checkbox',
          color: '#722ed1'
        }
      ]
    }
  },
  inject: ['parentToggleSetting'],
  watch: {
    projectView () {
      this.fetchData()
    }
  },
  created () {
    this.fetchData()
  },
  methods: {
    fetchData () {
      this.originalSetting = Object.assign({}, this.$config.theme)
      this.layoutMode = this.$config.theme['@layout-mode'] || 'light'
      this.uiSettings = this.$config.theme
    },
    switchLayoutMode () {
      this.$store.dispatch('SetDarkMode', (this.layoutMode === 'dark'))
      this.updateSetting('@layout-mode', this.layoutMode)
    },
    switchColor (e) {
      this.colorPick = e.target.value
      this.updateSetting('@primary-color', this.colorPick)
    },
    updateSetting (name, value) {
      this.uiSettings[name] = value
      if (['@navigation-background-color'].includes(name)) {
        this.uiSettings['@logo-background-color'] = value
      }
      window.less.modifyVars(this.uiSettings)
    },
    onClose () {
      this.parentToggleSetting(false)
    },
    downloadSetting () {
      this.downloadObjectAsJson(this.uiSettings)
    },
    resetSetting () {
      this.uiSettings = {}
      this.layoutMode = 'light'
      this.colorPick = this.originalSetting['@primary-color']
      this.navBgColorPick = this.originalSetting['@navigation-background-color']
      this.navTextColorPick = this.originalSetting['@navigation-text-color']
      this.projectNavBgColorPick = this.originalSetting['@project-nav-background-color']
      this.projectNavTextColorPick = this.originalSetting['@project-nav-text-color']

      this.switchLayoutMode()

      this.$config.theme = this.originalSetting
      window.less.modifyVars(this.$config.theme)
      this.$message.success(this.$t('label.success'))
    },
    downloadObjectAsJson (exportObj) {
      const dataStr = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(exportObj, null, 2))
      const downloadAnchorNode = document.createElement('a')
      downloadAnchorNode.setAttribute('href', dataStr)
      downloadAnchorNode.setAttribute('download', 'theme.json')
      document.body.appendChild(downloadAnchorNode)
      downloadAnchorNode.click()
      downloadAnchorNode.remove()
    }
  }
}
</script>

<style lang="less" scoped>
.side-setting {
  min-height: 100%;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
  position: relative;
  padding: 20px 0;

  .flex{
    display: flex;
  }
  .select-item{
    width: 80px;
  }
}

.setting-group {
  width: 100%;
}

.input-group {
  display: inline-flex;
  width: 100%;
  position: relative;

  .color-picker {
    position: absolute;
    width: 20px;
    height: 20px;
    overflow: hidden;
    top: 0;
    right: 0;
    border: 1px solid;
    cursor: pointer;

    .ant-input {
      opacity: 0;
      height: 20px;
      width: 20px;
      position: absolute;
      top: 0;
      left: 0;
      cursor: pointer;
    }
  }
}

.setting-action {
  width: 100%;
  padding: 0 24px;

  &-alert {
    display: flex;
    flex-direction: row;
    align-items: baseline;
    margin: 20px 0 8px;
    word-break: break-word;
    position: relative;

    :deep(.ant-alert-icon) {
    }
  }

  &-btn {
    width: 100%;
    margin-bottom: 5px;
  }
}
</style>
