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

import {
  ConfigProvider,
  Layout,
  Input,
  InputNumber,
  Button,
  Switch,
  Radio,
  Checkbox,
  Select,
  Card,
  Form,
  Row,
  Col,
  Modal,
  Table,
  Tabs,
  Badge,
  Popover,
  Dropdown,
  List,
  Avatar,
  Breadcrumb,
  Steps,
  Spin,
  Menu,
  Drawer,
  Tooltip,
  Alert,
  Tag,
  Divider,
  DatePicker,
  TimePicker,
  Upload,
  Progress,
  Skeleton,
  Popconfirm,
  Descriptions,
  message,
  Affix,
  Timeline,
  Pagination,
  Comment,
  Tree,
  Calendar,
  Slider,
  AutoComplete,
  Collapse,
  Space
} from 'ant-design-vue'
import VueClipboard from 'vue3-clipboard'
import VueCropper from 'vue-cropper'

export default {
  install: (app) => {
    app.config.globalProperties.$confirm = Modal.confirm
    app.config.globalProperties.$message = message
    app.config.globalProperties.$info = Modal.info
    app.config.globalProperties.$success = Modal.success
    app.config.globalProperties.$error = Modal.error
    app.config.globalProperties.$warning = Modal.warning

    app.use(VueClipboard, { autoSetContainer: true })
    app.use(VueCropper)
    app.use(ConfigProvider)
    app.use(Layout)
    app.use(Input)
    app.use(InputNumber)
    app.use(Button)
    app.use(Switch)
    app.use(Radio)
    app.use(Checkbox)
    app.use(Select)
    app.use(Card)
    app.use(Form)
    app.use(Row)
    app.use(Col)
    app.use(Modal)
    app.use(Table)
    app.use(Tabs)
    app.use(Badge)
    app.use(Popover)
    app.use(Dropdown)
    app.use(List)
    app.use(Avatar)
    app.use(Breadcrumb)
    app.use(Steps)
    app.use(Spin)
    app.use(Menu)
    app.use(Drawer)
    app.use(Tooltip)
    app.use(Alert)
    app.use(Tag)
    app.use(Divider)
    app.use(DatePicker)
    app.use(TimePicker)
    app.use(Upload)
    app.use(Progress)
    app.use(Skeleton)
    app.use(Popconfirm)
    // app.use(Notification)
    app.use(Affix)
    app.use(Timeline)
    app.use(Pagination)
    app.use(Comment)
    app.use(Tree)
    app.use(Calendar)
    app.use(Slider)
    app.use(AutoComplete)
    app.use(Collapse)
    app.use(Descriptions)
    app.use(Space)
  }
}
