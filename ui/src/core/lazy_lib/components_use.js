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

import { vueApp, vueProps } from '@/vue-app'
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
  notification,
  Affix,
  Timeline,
  Pagination,
  Comment,
  Tree,
  Calendar,
  Slider,
  AutoComplete,
  Collapse
} from 'ant-design-vue'

vueProps.$confirm = Modal.confirm
vueProps.$message = message
vueProps.$notification = notification
vueProps.$info = Modal.info
vueProps.$success = Modal.success
vueProps.$error = Modal.error
vueProps.$warning = Modal.warning

vueApp.use(ConfigProvider)
vueApp.use(Layout)
vueApp.use(Input)
vueApp.use(InputNumber)
vueApp.use(Button)
vueApp.use(Switch)
vueApp.use(Radio)
vueApp.use(Checkbox)
vueApp.use(Select)
vueApp.use(Card)
vueApp.use(Form)
vueApp.use(Row)
vueApp.use(Col)
vueApp.use(Modal)
vueApp.use(Table)
vueApp.use(Tabs)
vueApp.use(Badge)
vueApp.use(Popover)
vueApp.use(Dropdown)
vueApp.use(List)
vueApp.use(Avatar)
vueApp.use(Breadcrumb)
vueApp.use(Steps)
vueApp.use(Spin)
vueApp.use(Menu)
vueApp.use(Drawer)
vueApp.use(Tooltip)
vueApp.use(Alert)
vueApp.use(Tag)
vueApp.use(Divider)
vueApp.use(DatePicker)
vueApp.use(TimePicker)
vueApp.use(Upload)
vueApp.use(Progress)
vueApp.use(Skeleton)
vueApp.use(Popconfirm)
vueApp.use(Affix)
vueApp.use(Timeline)
vueApp.use(Pagination)
vueApp.use(Comment)
vueApp.use(Tree)
vueApp.use(Calendar)
vueApp.use(Slider)
vueApp.use(AutoComplete)
vueApp.use(Collapse)
vueApp.use(Descriptions)
