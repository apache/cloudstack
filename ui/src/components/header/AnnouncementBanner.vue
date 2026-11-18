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
  <a-affix v-if="showBanner" class="announcement-banner-container">
    <a-alert
      :type="bannerConfig.type || 'default'"
      :show-icon="bannerConfig.showIcon !== false"
      :closable="bannerConfig.closable !== false"
      :banner="true"
      @close="handleClose"
      :style="[ { border: borderColor }]"
    >
      <template #message>
        <div class="banner-content" v-html="sanitizedMessage" :style="[$store.getters.darkMode ? { color: 'rgba(255, 255, 255, 0.65)' } : { color: '#888' }]" />
      </template>
    </a-alert>
  </a-affix>
</template>

<script>
import DOMPurify from 'dompurify'

export default {
  name: 'AnnouncementBanner',
  data () {
    return {
      showBanner: false,
      bannerConfig: {},
      dismissed: false
    }
  },
  computed: {
    sanitizedMessage () {
      if (!this.bannerConfig.message) return ''
      const cleanHTML = DOMPurify.sanitize(this.bannerConfig.message, {
        ALLOWED_TAGS: [
          'p', 'div', 'span', 'br', 'strong', 'b', 'em', 'i', 'u',
          'a', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
          'small', 'mark', 'del', 'ins', 'sub', 'sup'
        ],
        ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'id', 'style'],
        ALLOWED_URI_REGEXP: /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|cid|xmpp|xxx):|[^a-z]|[a-z+.-]+(?:[^a-z+.\-:]|$))/i,
        FORBID_TAGS: ['script', 'object', 'embed', 'form', 'input', 'textarea', 'select', 'button'],
        FORBID_ATTR: ['onclick', 'onload', 'onerror', 'onmouseover', 'onfocus', 'onblur']
      })
      return cleanHTML
    },
    borderColor () {
      const colorMap = {
        error: '#ffa39e',
        warning: '#ffe58f',
        success: '#b7eb8f',
        info: '#b3cde3'
      }
      const color = colorMap[this.bannerConfig.type]
      return color ? `1px solid ${color}` : '0px'
    }
  },
  mounted () {
    this.loadBannerConfig()
  },
  methods: {
    loadBannerConfig () {
      const config = this.$config?.announcementBanner || {}
      if (config && config.enabled && config.message) {
        this.bannerConfig = config
        if (config.persistDismissal) {
          const dismissedKey = `cs-banner-dismissed-${this.getBannerHash()}`
          this.dismissed = this.$localStorage.get(dismissedKey) === 'true'
        }
        if (!this.dismissed && this.isWithinDisplayPeriod()) {
          this.showBanner = true
        }
      }
    },
    isWithinDisplayPeriod () {
      const config = this.bannerConfig
      const now = new Date()

      if (config.startDate) {
        const startDate = new Date(config.startDate)
        if (now < startDate) return false
      }

      if (config.endDate) {
        const endDate = new Date(config.endDate)
        if (now > endDate) return false
      }
      return true
    },
    handleClose () {
      this.showBanner = false
      if (this.bannerConfig.persistDismissal) {
        const dismissedKey = `cs-banner-dismissed-${this.getBannerHash()}`
        this.$localStorage.set(dismissedKey, 'true')
      }
      if (this.bannerConfig.onClose) {
        this.bannerConfig.onClose()
      }
    },
    getBannerHash () {
      // Create a simple hash of the message content for dismissal tracking
      let hash = 0
      const str = this.bannerConfig.message || ''
      for (let i = 0; i < str.length; i++) {
        const char = str.charCodeAt(i)
        hash = ((hash << 5) - hash) + char
        hash = hash & hash // Convert to 32bit integer
      }
      return Math.abs(hash).toString()
    }
  }
}
</script>

<style scoped>
.announcement-banner-container {
  z-index: 1000;
  top: 0;
  margin: 0;
  width: 100%;
  justify-content: center;
  align-items: center;
}

.banner-content {
  line-height: 1.7;
  text-align: center
}
</style>
