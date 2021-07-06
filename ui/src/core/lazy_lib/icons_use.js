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
  SafetyOutlined,
  UserOutlined,
  LockOutlined,
  BlockOutlined,
  AuditOutlined,
  TranslationOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  LogoutOutlined,
  QuestionCircleOutlined,
  ClockCircleOutlined,
  ProjectOutlined,
  LoadingOutlined,
  BellOutlined,
  GithubOutlined,
  DashboardOutlined,
  TeamOutlined,
  ScheduleOutlined,
  FlagOutlined,
  DesktopOutlined,
  BankOutlined,
  ReadOutlined,
  FilterOutlined,
  HomeOutlined,
  UserAddOutlined,
  SyncOutlined,
  EditOutlined,
  PlusOutlined,
  MoreOutlined,
  SafetyCertificateOutlined,
  DeleteOutlined,
  PauseCircleOutlined,
  AppstoreOutlined,
  CalendarOutlined,
  RocketOutlined,
  BulbOutlined,
  IdcardOutlined,
  GlobalOutlined,
  ClusterOutlined,
  DeploymentUnitOutlined,
  GatewayOutlined,
  WifiOutlined,
  DatabaseOutlined,
  HddOutlined,
  BarcodeOutlined,
  CloudUploadOutlined,
  CloudOutlined,
  PictureOutlined,
  SwapOutlined,
  KeyOutlined,
  GoldOutlined,
  EnvironmentOutlined,
  ApiOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloseCircleTwoTone,
  CheckCircleTwoTone,
  DoubleLeftOutlined,
  DoubleRightOutlined,
  CaretUpOutlined,
  CaretDownOutlined,
  ShareAltOutlined,
  FilterTwoTone,
  SearchOutlined,
  StopOutlined,
  InfoCircleOutlined,
  FolderOutlined,
  LinkOutlined,
  InboxOutlined,
  DragOutlined,
  ArrowsAltOutlined,
  ScissorOutlined,
  CameraOutlined,
  PoweroffOutlined,
  PaperClipOutlined,
  CodeOutlined,
  FolderAddOutlined,
  CaretRightOutlined,
  CloseOutlined,
  NotificationOutlined
} from '@ant-design/icons-vue'

import { library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'

import { faCentos, faUbuntu, faSuse, faRedhat, faFedora, faLinux, faFreebsd, faApple, faWindows, faJava } from '@fortawesome/free-brands-svg-icons'
import { faLanguage, faCompactDisc, faCameraRetro } from '@fortawesome/free-solid-svg-icons'

library.add(faCentos, faUbuntu, faSuse, faRedhat, faFedora, faLinux, faFreebsd, faApple, faWindows, faJava)
library.add(faLanguage, faCompactDisc, faCameraRetro)

export default {
  install: (app) => {
    app.component('font-awesome-icon', FontAwesomeIcon)

    app.component('SafetyOutlined', SafetyOutlined)
    app.component('UserOutlined', UserOutlined)
    app.component('LockOutlined', LockOutlined)
    app.component('BlockOutlined', BlockOutlined)
    app.component('AuditOutlined', AuditOutlined)
    app.component('TranslationOutlined', TranslationOutlined)
    app.component('MenuFoldOutlined', MenuFoldOutlined)
    app.component('MenuUnfoldOutlined', MenuUnfoldOutlined)
    app.component('LogoutOutlined', LogoutOutlined)
    app.component('QuestionCircleOutlined', QuestionCircleOutlined)
    app.component('ClockCircleOutlined', ClockCircleOutlined)
    app.component('ProjectOutlined', ProjectOutlined)
    app.component('LoadingOutlined', LoadingOutlined)
    app.component('BellOutlined', BellOutlined)
    app.component('GithubOutlined', GithubOutlined)
    app.component('DashboardOutlined', DashboardOutlined)
    app.component('TeamOutlined', TeamOutlined)
    app.component('ScheduleOutlined', ScheduleOutlined)
    app.component('FlagOutlined', FlagOutlined)
    app.component('DesktopOutlined', DesktopOutlined)
    app.component('BankOutlined', BankOutlined)
    app.component('ReadOutlined', ReadOutlined)
    app.component('FilterOutlined', FilterOutlined)
    app.component('HomeOutlined', HomeOutlined)
    app.component('UserAddOutlined', UserAddOutlined)
    app.component('SyncOutlined', SyncOutlined)
    app.component('EditOutlined', EditOutlined)
    app.component('PlusOutlined', PlusOutlined)
    app.component('MoreOutlined', MoreOutlined)
    app.component('SafetyCertificateOutlined', SafetyCertificateOutlined)
    app.component('DeleteOutlined', DeleteOutlined)
    app.component('PauseCircleOutlined', PauseCircleOutlined)
    app.component('AppstoreOutlined', AppstoreOutlined)
    app.component('BulbOutlined', BulbOutlined)
    app.component('CalendarOutlined', CalendarOutlined)
    app.component('RocketOutlined', RocketOutlined)
    app.component('IdcardOutlined', IdcardOutlined)
    app.component('GlobalOutlined', GlobalOutlined)
    app.component('ClusterOutlined', ClusterOutlined)
    app.component('DeploymentUnitOutlined', DeploymentUnitOutlined)
    app.component('GatewayOutlined', GatewayOutlined)
    app.component('WifiOutlined', WifiOutlined)
    app.component('DatabaseOutlined', DatabaseOutlined)
    app.component('HddOutlined', HddOutlined)
    app.component('BarcodeOutlined', BarcodeOutlined)
    app.component('CloudUploadOutlined', CloudUploadOutlined)
    app.component('CloudOutlined', CloudOutlined)
    app.component('PictureOutlined', PictureOutlined)
    app.component('SwapOutlined', SwapOutlined)
    app.component('KeyOutlined', KeyOutlined)
    app.component('GoldOutlined', GoldOutlined)
    app.component('EnvironmentOutlined', EnvironmentOutlined)
    app.component('ApiOutlined', ApiOutlined)
    app.component('ArrowUpOutlined', ArrowUpOutlined)
    app.component('ArrowDownOutlined', ArrowDownOutlined)
    app.component('ReloadOutlined', ReloadOutlined)
    app.component('CheckCircleOutlined', CheckCircleOutlined)
    app.component('CloseCircleOutlined', CloseCircleOutlined)
    app.component('CloseCircleTwoTone', CloseCircleTwoTone)
    app.component('CheckCircleTwoTone', CheckCircleTwoTone)
    app.component('DoubleLeftOutlined', DoubleLeftOutlined)
    app.component('DoubleRightOutlined', DoubleRightOutlined)
    app.component('CaretUpOutlined', CaretUpOutlined)
    app.component('CaretDownOutlined', CaretDownOutlined)
    app.component('ShareAltOutlined', ShareAltOutlined)
    app.component('FilterTwoTone', FilterTwoTone)
    app.component('SearchOutlined', SearchOutlined)
    app.component('StopOutlined', StopOutlined)
    app.component('InfoCircleOutlined', InfoCircleOutlined)
    app.component('FolderOutlined', FolderOutlined)
    app.component('LinkOutlined', LinkOutlined)
    app.component('InboxOutlined', InboxOutlined)
    app.component('DragOutlined', DragOutlined)
    app.component('ArrowsAltOutlined', ArrowsAltOutlined)
    app.component('ScissorOutlined', ScissorOutlined)
    app.component('CameraOutlined', CameraOutlined)
    app.component('PoweroffOutlined', PoweroffOutlined)
    app.component('PaperClipOutlined', PaperClipOutlined)
    app.component('CodeOutlined', CodeOutlined)
    app.component('FolderAddOutlined', FolderAddOutlined)
    app.component('CaretRightOutlined', CaretRightOutlined)
    app.component('CloseOutlined', CloseOutlined)
    app.component('NotificationOutlined', NotificationOutlined)
  }
}
