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
        <a-button type="primary" @click="onDownload" v-if="webSocketsValid">{{ $t('label.download') }}</a-button>
      </template>
        <!-- Draggable handle at the top of the drawer content -->
        <div
          class="drag-handle"
          @mousedown="startDrag"
        ></div>
        <!-- Container that holds both the scrollable content and the fixed footer -->
        <div class="drawer-container">
          <div class="header">
            <a-row :gutter="[16, 8]" style="width: 100%; margin-bottom: 16px">
              <a-col :xs="24" :sm="16">
                <tooltip-label :title="$t('label.source')" :tooltip="'Sources'" />
                <a-select
                  v-model:value="selectedWebSockets"
                  mode="multiple"
                  showSearch
                  optionFilterProp="label"
                  :filterOption="(input, option) => {
                    return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                  }"
                  :loading="webSocketsLoading"
                  :placeholder="'Sources'"
                  @change="handleSourceChange"
                  style="width: 100%">
                  <a-select-option
                    v-for="opt in webSockets"
                    :key="opt.id"
                    :label="opt.name">
                    {{ opt.name }}
                  </a-select-option>
                </a-select>
              </a-col>

              <a-col :xs="24" :sm="8">
                <tooltip-label :title="'Log Type'" :tooltip="'Log severity level'" />
                <a-select
                  mode="multiple"
                  v-model:value="selectedLogType"
                  :placeholder="'Log Type'"
                  style="width: 100%">
                  <a-select-option
                    v-for="level in logLevels"
                    :key="level"
                    :value="level">
                    {{ level }}
                  </a-select-option>
                </a-select>
              </a-col>
            </a-row>
          </div>
          <div class="content_wrapper">
            <div
              v-if="showRawLogs"
              class="content"
              v-html="webSocketData">
            </div>
            <a-table
              v-else
              :columns="logTableColumns"
              :data-source="filteredLogs"
              :rowKey="record => record.id"
              :pagination="false"
              :showHeader="false"
              :bordered="false"
              size="small"
              style="margin: 16px">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'message'">
                  <div
                    style="font-family: monospace; white-space: pre-wrap; font-size: 13px;"
                    v-html="highlightLogLevels(record.message)"
                  ></div>
                </template>
              </template>
            </a-table>
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
import { postAPI } from '@/api'
import TooltipLabel from '@/components/widgets/TooltipLabel'

export default {
  name: 'LogsConsole',
  components: {
    TooltipLabel
  },
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
      webSocketsLoading: false,
      webSockets: null,
      selectedWebSockets: undefined,
      webSocketData: '',
      selectedLogType: undefined,
      dataSource: [],
      logLevels: ['CRITICAL', 'ERROR', 'WARN', 'INFO', 'DEBUG', 'TRACE']
    }
  },
  watch: {
    filters: {
      handler (newItem) {
        if (newItem && newItem.length > 0) {
          this.openLogsWebSession(newItem)
          return
        }
        this.disconnectWebSocketsAndClearData()
      }
    }
  },
  computed: {
    showRawLogs () {
      return false
    },
    showSourceOnlyWhenMultiple () {
      return true
    },
    filtersAsString () {
      if (!this.filters) {
        return null
      }
      return this.filters.join(', ')
    },
    logTableColumns () {
      const columns = []

      if (!this.showSourceOnlyWhenMultiple || (this.selectedWebSockets && this.selectedWebSockets.length > 1)) {
        columns.push({
          title: 'Source',
          dataIndex: 'sourceName',
          key: 'sourceName',
          width: 150
        })
      }

      columns.push({
        title: 'Message',
        dataIndex: 'message',
        key: 'message'
      })

      return columns
    },
    webSocketsValid () {
      return this.webSockets && this.webSockets.length > 0
    },
    currentLogsErrorCount () {
      return 0
    },
    currentLogsWarningCount () {
      return 0
    },
    filteredLogs () {
      if (!this.dataSource || this.dataSource.length === 0) return []

      return this.dataSource.filter(log => {
        const sourceMatch = !this.selectedWebSockets || this.selectedWebSockets.length === 0 || this.selectedWebSockets.includes(log.sourceId)
        const levelMatch = !this.selectedLogType || this.selectedLogType.length === 0 || this.selectedLogType.includes(log.level)
        return sourceMatch && levelMatch
      })
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
      this.disconnectWebSocketsAndClearData()
      this.$emit('close')
    },
    disconnectWebSocketsAndClearData () {
      this.closeWebSockets()
      this.webSockets = null
      this.selectedWebSockets = undefined
      this.dataSource = []
      this.webSocketData = ''
      this.webSocketsLoading = false
    },
    openLogsWebSession (filters) {
      this.webSocketsLoading = true
      this.disconnectWebSocketsAndClearData()
      postAPI('createLogsWebSession', { filters: filters.join() }).then(json => {
        var session = json?.createlogswebsessionresponse?.logswebsession
        if (session) {
          this.prepareAndOpenWebSockets(session.websocket)
        }
      }).catch(error => {
        this.$notifyError(error)
      }).finally(() => {
        this.webSocketsLoading = false
      })
    },
    handleSourceChange (value) {
      console.log('Selected source:', value)
    },
    prepareAndOpenWebSockets (webSocketsDetails) {
      var opts = []
      for (var ws of webSocketsDetails) {
        var opt = {
          key: ws.managementserverid,
          id: ws.managementserverid,
          name: ws.managementservername,
          title: ws.managementservername,
          webSocketUrl: (ws.ssl ? 'wss://' : 'ws://') + ws.host + ':' + ws.port + ws.path,
          webSocket: null,
          logsErrorCount: 0,
          logsWarningCount: 0
        }
        opts.push(opt)
      }
      this.webSockets = opts.sort((a, b) => a.title.localeCompare(b.title))
      console.log('WebSockets prepared:', this.webSockets)
      this.openWebSockets()
    },
    openWebSockets () {
      this.closeWebSockets()
      if (!this.webSocketsValid) {
        return
      }
      for (const opt of this.webSockets) {
        opt.webSocket = new WebSocket(opt.webSocketUrl)

        opt.webSocket.addEventListener('message', (event) => {
          this.appendWebSocketData(opt, event.data)
        })

        opt.webSocket.addEventListener('open', () => {
          this.appendWebSocketData(opt, '<span style="color: green; font-style: italic">Connection established.</span>')
        })

        opt.webSocket.addEventListener('error', (error) => {
          this.appendWebSocketData(opt, '<span style="color: red; font-style: italic">Error:' + error + '</span>')
        })
      }
    },
    closeWebSockets () {
      if (!this.webSocketsValid) {
        return
      }
      for (var opt of this.webSockets) {
        if (opt.webSocket) {
          opt.webSocket.close()
          opt.webSocket = null
        }
      }
    },
    parseLogLine (line, sourceId, sourceName) {
      const logRegex = /^([\d-]+\s[\d:,]+)\s+([A-Z]+)\s+\[.*?\]\s+\(.*?\)\s+(?:\(logid:[^)]*\)\s+)?(.*)$/
      const match = line.match(logRegex)

      var level = null
      var timestamp = new Date().toISOString()
      if (match) {
        timestamp = match[1]
        level = match[2]
      }

      return {
        id: sourceId + '-' + timestamp,
        timestamp: timestamp,
        level: level,
        message: line,
        sourceId,
        sourceName
      }
    },
    appendWebSocketData (opt, data) {
      if (!this.webSocketData) {
        this.webSocketData = data
        return
      }
      this.dataSource.push(this.parseLogLine(data, opt.id, opt.name))
      this.webSocketData += '<br>' + data
    },
    formatLogAndUpdateData (opt, data) {
      if (data.startsWith('Connection idle')) {
        return '<span style="color: red; font-style: italic">' + data + '</span>'
      }
      // Escape HTML to avoid XSS issues if logs are uncontrolled
      let escaped = data
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      opt.logsErrorCount += escaped.split('ERROR').length - 1
      opt.logsWarningCount += escaped.split('WARN').length - 1
      // Highlight log levels
      escaped = escaped.replace(/(ERROR)/g, '<span style="color: red; font-weight: bold">$1</span>')
      escaped = escaped.replace(/(WARN)/g, '<span style="color: orange; font-weight: bold">$1</span>')
      escaped = escaped.replace(/(INFO)/g, '<span style="color: blue">$1</span>')
      return escaped
    },
    highlightLogLevels (message) {
      if (!message) return ''
      return message
        .replace(/CRITICAL/g, '<span style="color: red; font-weight: bold;">CRITICAL</span>')
        .replace(/ERROR/g, '<span style="color: red; font-weight: bold;">ERROR</span>')
        .replace(/WARN/g, '<span style="color: orange; font-weight: bold;">WARN</span>')
        .replace(/INFO/g, '<span style="color: blue; font-weight: bold;">INFO</span>')
        .replace(/DEBUG/g, '<span style="color: green; font-weight: bold;">DEBUG</span>')
        .replace(/TRACE/g, '<span style="color: gray; font-weight: bold;">TRACE</span>')
    },
    onDownload () {
      let htmlString = this.webSocketData.replace(/<br\s*\/?>/gi, '\n')
      // Optionally, handle closing </p> tags for paragraphs
      htmlString = htmlString.replace(/<\/p>/gi, '\n')
      const tempDiv = document.createElement('div')
      tempDiv.innerHTML = htmlString
      const plainTextData = tempDiv.textContent || tempDiv.innerText || ''
      var blob = new Blob([plainTextData], { type: 'text/plain' })
      var filename = 'logs-' + this.filters.join('-') + '.log'
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

/* The container below the drag handle uses flex to position content + footer */
.resizable-drawer .drawer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;        /* fills remaining space under handle */
  overflow: hidden;
}

.resizable-drawer .header {
  display: flex;
  flex-direction: row;  /* <-- make fields align horizontally */
  gap: 16px;            /* optional spacing between the fields */
  margin: 8px 16px;     /* optional margin */
}

/* Content wrapper is also flex so it can grow/shrink properly */
.resizable-drawer .content_wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto; /* This pane’s content scrolls if needed */
}

.resizable-drawer .header .field {
  display: flex;
  flex-direction: row;  /* horizontal */
  align-items: center;
  gap: 8px;
  flex: 1;
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
