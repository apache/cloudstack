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
      <a-drawer
        class="resizable-drawer"
        :title="$t('Logs')"
        placement="bottom"
        :visible="visible"
        :height="drawerHeight"
        :maskClosable="false"
        :bodyStyle="{ overflow: 'hidden' }"
        @close="closeAction"
      >
      <template #extra>
        <a-button type="primary" @click="onDownload" v-if="tabsValid">{{ $t('label.download') }}</a-button>
      </template>
        <!-- Draggable handle at the top of the drawer content -->
        <div
          class="drag-handle"
          @mousedown="startDrag"
        ></div>
        <!-- Container that holds both the scrollable content and the fixed footer -->
        <div class="drawer-container">
          <div class="tabs-wrapper">
            <a-tabs v-model:activeKey="activeTabKey">
              <a-tab-pane
                v-for="tab in tabs"
                :key="tab.key"
                :tab="tab.title"
              >
              <div class="content" v-html="tab.webSocketData"></div>
              </a-tab-pane>
            </a-tabs>
          </div>
          <div class="footer">
            <div class="footer-left" v-if="filtersAsString">{{ $t('message.showing.logs').replace('%x', filtersAsString) }}</div>
            <div class="footer-right">
              <span>{{ $t('label.error') + ': ' + currentLogsErrorCount }}</span>
              <span class="separator">|</span>
              <span>{{ $t('label.warning') + ': ' + currentLogsWarningCount }}</span>
            </div>
          </div>
        </div>
      </a-drawer>
    </div>
  </template>

<script>
import { api } from '@/api'

export default {
  name: 'LogsConsole',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    filters: {
      type: Array,
      default: null
    }
  },
  data () {
    return {
      drawerHeight: window.innerHeight * 0.4, // start at 40% of viewport height
      dragging: false,
      startY: 0,
      startHeight: 0,
      loading: false,
      tabs: [],
      activeTabKey: null
    }
  },
  watch: {
    filters: {
      handler (newItem) {
        if (newItem && newItem.length > 0) {
          this.openLogsWebSession(newItem)
          return
        }
        this.disconnectWebSocketsAndClearTabs()
      }
    }
  },
  computed: {
    filtersAsString () {
      if (!this.filters) {
        return null
      }
      return this.filters.join(', ')
    },
    tabsValid () {
      return this.tabs && this.tabs.length > 0
    },
    currentTab () {
      if (!this.tabsValid || !this.activeTabKey) {
        return null
      }
      return this.tabs.filter(x => x.key === this.activeTabKey)[0]
    },
    currentLogsErrorCount () {
      var tab = this.currentTab
      if (!tab) {
        return 0
      }
      return tab.logsErrorCount
    },
    currentLogsWarningCount () {
      const tab = this.currentTab
      if (!tab) {
        return 0
      }
      return tab.logsWarningCount
    }
  },
  methods: {
    startDrag (event) {
      this.dragging = true
      this.startY = event.clientY
      this.startHeight = this.drawerHeight
      window.addEventListener('mousemove', this.onDrag)
      window.addEventListener('mouseup', this.stopDrag)
    },
    onDrag (event) {
      if (!this.dragging) return
      const dy = this.startY - event.clientY
      let newHeight = this.startHeight + dy
      // Ensure the drawer doesn't go below 40% of the viewport height
      newHeight = Math.max(newHeight, window.innerHeight * 0.4)
      // Also, ensure it doesn't exceed the viewport height
      this.drawerHeight = Math.min(newHeight, window.innerHeight)
    },
    stopDrag () {
      this.dragging = false
      window.removeEventListener('mousemove', this.onDrag)
      window.removeEventListener('mouseup', this.stopDrag)
    },
    handleResize () {
      // On resize, ensure the drawer height is within allowed bounds
      this.drawerHeight = Math.min(this.drawerHeight, window.innerHeight)
      this.drawerHeight = Math.max(this.drawerHeight, window.innerHeight * 0.4)
    },
    closeAction () {
      this.disconnectWebSocketsAndClearTabs()
      this.$emit('close')
    },
    disconnectWebSocketsAndClearTabs () {
      this.closeWebSockets()
      if (!this.tabsValid) {
        return
      }
      this.tabs = []
      this.activeTabKey = null
    },
    openLogsWebSession (filters) {
      this.disconnectWebSocketsAndClearTabs()
      api('createLogsWebSession', { filters: filters.join() }).then(json => {
        var session = json?.createlogswebsessionresponse?.logswebsession
        if (session) {
          this.prepareTabsAndOpenWebSockets(session.websocket)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.loading = false
        if (this.tabsValid) {
          this.activeTabKey = this.tabs[0].key
        }
      })
    },
    prepareTabsAndOpenWebSockets (webSocketsDetails) {
      var wsTabs = []
      for (var webSocketDetails of webSocketsDetails) {
        var tab = {
          key: webSocketDetails.managementserverid,
          title: webSocketDetails.managementservername,
          webSocketUrl: 'ws://' + webSocketDetails.host + ':' + webSocketDetails.port + webSocketDetails.path,
          webSocket: null,
          webSocketData: '',
          logsErrorCount: 0,
          logsWarningCount: 0
        }
        wsTabs.push(tab)
      }
      this.tabs = wsTabs.sort((a, b) => a.title.localeCompare(b.title))
      this.openWebSockets()
    },
    openWebSockets () {
      this.closeWebSockets()
      if (!this.tabsValid) {
        return
      }
      for (const tab of this.tabs) {
        tab.webSocket = new WebSocket(tab.webSocketUrl)

        tab.webSocket.addEventListener('message', (event) => {
          this.appendWebSocketData(tab, this.formatLogAndUpdateData(tab, event.data))
        })

        tab.webSocket.addEventListener('open', () => {
          this.appendWebSocketData(tab, '<span style="color: green; font-style: italic">Connection established.</span>')
        })

        tab.webSocket.addEventListener('error', (error) => {
          this.appendWebSocketData(tab, '<span style="color: red; font-style: italic">Error:' + error + '</span>')
        })
      }
    },
    closeWebSockets () {
      if (!this.tabsValid) {
        return
      }
      for (var tab of this.tabs) {
        if (tab.webSocket) {
          tab.webSocket.close()
          tab.webSocket = null
        }
      }
    },
    appendWebSocketData (tab, data) {
      if (!tab.webSocketData) {
        tab.webSocketData = data
        return
      }
      tab.webSocketData += '<br>' + data
    },
    formatLogAndUpdateData (tab, data) {
      if (data.startsWith('Connection idle')) {
        return '<span style="color: red; font-style: italic">' + data + '</span>'
      }
      // Escape HTML to avoid XSS issues if logs are uncontrolled
      let escaped = data
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      tab.logsErrorCount += escaped.split('ERROR').length - 1
      tab.logsWarningCount += escaped.split('WARN').length - 1
      // Highlight log levels
      escaped = escaped.replace(/(ERROR)/g, '<span style="color: red; font-weight: bold">$1</span>')
      escaped = escaped.replace(/(WARN)/g, '<span style="color: orange; font-weight: bold">$1</span>')
      escaped = escaped.replace(/(INFO)/g, '<span style="color: blue">$1</span>')
      return escaped
    },
    onDownload () {
      let htmlString = this.currentTab.webSocketData.replace(/<br\s*\/?>/gi, '\n')
      // Optionally, handle closing </p> tags for paragraphs
      htmlString = htmlString.replace(/<\/p>/gi, '\n')
      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = htmlString
      const plainTextData = tempDiv.textContent || tempDiv.innerText || ''
      var blob = new Blob([plainTextData], { type: 'text/plain' })
      var filename = this.currentTab.title + '-' + this.filters.join('-') + '.log'
      if (window.navigator.msSaveOrOpenBlob) {
        window.navigator.msSaveBlob(blob, filename)
      } else {
        var elem = window.document.createElement('a')
        elem.href = window.URL.createObjectURL(blob)
        elem.download = filename
        document.body.appendChild(elem)
        elem.click()
        document.body.removeChild(elem)
      }
    }
  },
  mounted () {
    window.addEventListener('resize', this.handleResize)
  },
  beforeUnmount () {
    window.removeEventListener('resize', this.handleResize)
    this.closeWebSockets()
  }
}
</script>

<style scoped>
/* Make the drawer body a flex container filling all space */
.resizable-drawer .ant-drawer-body {
  display: flex;          /* flex container */
  flex-direction: column;
  height: 100%;           /* matches bodyStyle height */
  margin: 0;
  padding: 0;
  overflow: hidden;       /* no scrolling on the drawer body itself */
}

/* The drag handle is a small, fixed-height bar at the top */
.resizable-drawer .drag-handle {
  flex-shrink: 0;
  height: 10px;
  background-color: #f0f0f0;
  cursor: ns-resize;
  width: 100%;
}

/* The container below the drag handle uses flex to position tabs + footer */
.resizable-drawer .drawer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;        /* fills remaining space under handle */
  overflow: hidden;
}

/* Tabs wrapper is also flex so it can grow/shrink properly */
.resizable-drawer .tabs-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Ensure the content holder fills available space */
::v-deep .ant-tabs-content-holder {
  display: flex;
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

/* Force the ant-tabs-content container to fill its parent */
::v-deep .ant-tabs-content {
  display: flex !important;
  flex: 1 !important;
  height: 100% !important;
  overflow: hidden;
  min-height: 0;
}

/* Force each tab pane to match the container’s height */
::v-deep .ant-tabs-tabpane {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow-y: auto; /* This pane’s content scrolls if needed */
}

/* Finally, only the log content area should scroll */
.resizable-drawer .content {
  padding: 16px;
  font-family: 'Courier New', Courier, monospace;
  white-space: pre-wrap;
  background-color: #f7f7f7;
  border: 1px solid #ddd;
  line-height: 1.5;
}

/* Footer is pinned at the bottom with a fixed height */
.resizable-drawer .footer {
  flex: 0 0 50px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  display: flex;
  align-items: center;
  padding: 0 8px;
  justify-content: space-between;
}

.resizable-drawer .footer-right {
  display: flex;
  align-items: center;
}

.resizable-drawer .separator {
  margin: 0 8px;
}

/* Log level highlighting */
.log-error {
  color: red;
  font-weight: bold;
}
.log-warn {
  color: orange;
  font-weight: bold;
}
.log-info {
  color: blue;
}
</style>
