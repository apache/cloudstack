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
    <a-button
      v-shortkey="['f6']"
      @shortkey="showKeyboardModal(true)"
      type="primary"
      @click="() => showKeyboardModal(true)"
    >
      Keyboard Shortcuts [F6]
    </a-button>
    <a-modal v-model="modalVisible" title="Keyboard Shortcuts" centered footer>
      <a-table :columns="columns" :data-source="data" :pagination="false" >
        <a slot="name" slot-scope="text">{{ text }}</a>
      </a-table>
    </a-modal>
  </div>
</template>
<script>
const columns = [
  {
    title: 'Shortcut Key(s)',
    dataIndex: 'keyboardShortcut',
    key: 'keyboardShortcut'
  },
  {
    title: 'Action',
    dataIndex: 'action',
    key: 'action'
  }
]

const data = [
  {
    key: '1',
    keyboardShortcut: 't',
    action: 'Toggle Side Menu View'
  },
  {
    key: '2',
    keyboardShortcut: 'c',
    action: 'Clear Notification'
  }
]

export default {
  data () {
    return {
      data,
      columns,
      modalVisible: false
    }
  },
  methods: {
    showKeyboardModal (modalVisible) {
      this.modalVisible = !this.modalVisible
    }
  }
}
</script>
