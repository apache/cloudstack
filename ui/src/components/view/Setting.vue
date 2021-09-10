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
        v-model="layoutMode"
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
                v-model="navBgColorPick"
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
                v-model="navBgColorPick"
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
                v-model="projectNavBgColorPick"
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
                v-model="projectNavTextColorPick"
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
        icon="copy"
        @click="saveSetting">{{ $t('label.save.setting') }}</a-button>
      <a-button
        class="setting-action-btn"
        icon="undo"
        @click="resetSetting">{{ $t('label.reset.to.default') }}</a-button>
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
      layoutMode: 'light',
      colorPick: this.$store.getters.themeSetting['@primary-color'] || this.$config.theme['@primary-color'],
      navBgColorPick: this.$store.getters.themeSetting['@navigation-background-color'] || this.$config.theme['@navigation-background-color'],
      navTextColorPick: this.$store.getters.themeSetting['@navigation-text-color'] || this.$config.theme['@navigation-text-color'],
      projectNavBgColorPick: this.$store.getters.themeSetting['@project-nav-background-color'] || this.$config.theme['@project-nav-background-color'],
      projectNavTextColorPick: this.$store.getters.themeSetting['@project-nav-text-color'] || this.$config.theme['@project-nav-text-color'],
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
          component: () => import('@/assets/icons/light.svg?inline')
        },
        {
          name: 'dark',
          type: 'image-checkbox',
          component: () => import('@/assets/icons/dark.svg?inline')
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
      this.layoutMode = 'light'
      if (this.$store.getters.darkMode) {
        this.layoutMode = 'dark'
      }
      this.uiSettings = this.$config.theme
    },
    switchLayoutMode () {
      this.$store.dispatch('SetDarkMode', (this.layoutMode === 'dark'))
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
    saveSetting () {
      const loading = this.$message.loading(this.$t('label.save.setting'), 0)
      this.$store.dispatch('SetThemeSetting', this.uiSettings)
      setTimeout(() => {
        loading()
        this.$message.success(this.$t('label.success'))
      }, 1000)
    },
    resetSetting () {
      this.layoutMode = 'light'
      this.colorPick = this.originalSetting['@primary-color']
      this.navBgColorPick = this.originalSetting['@navigation-background-color']
      this.navTextColorPick = this.originalSetting['@navigation-text-color']
      this.projectNavBgColorPick = this.originalSetting['@project-nav-background-color']
      this.projectNavTextColorPick = this.originalSetting['@project-nav-text-color']

      this.$store.dispatch('SetThemeSetting', {})
      this.switchLayoutMode()

      this.$config.theme = this.originalSetting
      window.less.modifyVars(this.$config.theme)
      this.$message.success(this.$t('label.success'))
    },
    formatConfig (obj, dep) {
      dep = dep || 1
      const LN = '\n'
      const TAB = '  '
      let indent = ''
      for (let i = 0; i < dep; i++) {
        indent += TAB
      }
      let isArray = false
      let arrayLastIsObj = false
      let str = ''
      let prefix = '{'
      let subfix = '}'

      if (Array.isArray(obj)) {
        isArray = true
        prefix = '['
        subfix = ']'
        str = obj.map((item, index) => {
          let format = ''
          if (typeof item === 'function') {
            //
          } else if (typeof item === 'object') {
            arrayLastIsObj = true
            format = `${LN}${indent}${this.formatConfig(item, dep + 1)},`
          } else if ((typeof item === 'number' && !isNaN(item)) || typeof item === 'boolean') {
            format = `${item},`
          } else if (typeof item === 'string') {
            format = `'${item}',`
          }
          if (index === obj.length - 1) {
            format = format.substring(0, format.length - 1)
          } else {
            arrayLastIsObj = false
          }
          return format
        }).join('')
      } else if (typeof obj !== 'function' && typeof obj === 'object') {
        str = Object.keys(obj).map((key, index, keys) => {
          const val = obj[key]
          let format = ''
          if (typeof val === 'function') {
            //
          } else if (typeof val === 'object') {
            format = `${LN}${indent}${key}: ${this.formatConfig(val, dep + 1)},`
          } else if ((typeof val === 'number' && !isNaN(val)) || typeof val === 'boolean') {
            format = `${LN}${indent}${key}: ${val},`
          } else if (typeof val === 'string') {
            format = `${LN}${indent}${key}: '${val}',`
          }
          if (index === keys.length - 1) {
            format = format.substring(0, format.length - 1)
          }
          return format
        }).join('')
      }
      const len = TAB.length
      if (indent.length >= len) {
        indent = indent.substring(0, indent.length - len)
      }
      if (!isArray || arrayLastIsObj) {
        subfix = LN + indent + subfix
      }

      return `${prefix}${str}${subfix}`
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
    margin: 20px 0 8px;
    word-break: break-word;
  }

  &-btn {
    width: 100%;
    margin-bottom: 5px;
  }
}
</style>
