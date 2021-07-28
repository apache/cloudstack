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
  ApartmentOutlined,
  ApiOutlined,
  AppstoreOutlined,
  ArrowDownOutlined,
  ArrowUpOutlined,
  ArrowsAltOutlined,
  AuditOutlined,
  BankOutlined,
  BarcodeOutlined,
  BarsOutlined,
  BellOutlined,
  BlockOutlined,
  BuildOutlined,
  BulbOutlined,
  CalendarOutlined,
  CameraOutlined,
  CaretDownOutlined,
  CaretRightOutlined,
  CaretUpOutlined,
  CheckCircleOutlined,
  CheckCircleTwoTone,
  CheckOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  CloseCircleTwoTone,
  CloseOutlined,
  CloudDownloadOutlined,
  CloudOutlined,
  CloudUploadOutlined,
  ClusterOutlined,
  CodeOutlined,
  CopyOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  DeploymentUnitOutlined,
  DesktopOutlined,
  DoubleLeftOutlined,
  DoubleRightOutlined,
  DownloadOutlined,
  DragOutlined,
  EditOutlined,
  EnvironmentOutlined,
  FileProtectOutlined,
  FilterOutlined,
  FilterTwoTone,
  FireOutlined,
  FlagOutlined,
  FolderAddOutlined,
  FolderOutlined,
  FormOutlined,
  FullscreenOutlined,
  GatewayOutlined,
  GithubOutlined,
  GlobalOutlined,
  GoldOutlined,
  HddOutlined,
  HomeOutlined,
  IdcardOutlined,
  InboxOutlined,
  InfoCircleOutlined,
  KeyOutlined,
  LinkOutlined,
  LoadingOutlined,
  LockOutlined,
  LoginOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MinusCircleOutlined,
  MoreOutlined,
  NotificationOutlined,
  PaperClipOutlined,
  PauseCircleOutlined,
  PictureOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  PoweroffOutlined,
  ProjectOutlined,
  QuestionCircleOutlined,
  ReadOutlined,
  ReconciliationOutlined,
  ReloadOutlined,
  RocketOutlined,
  SafetyCertificateOutlined,
  SafetyOutlined,
  SaveOutlined,
  ScheduleOutlined,
  ScissorOutlined,
  SearchOutlined,
  ShareAltOutlined,
  StopOutlined,
  SwapOutlined,
  SyncOutlined,
  TeamOutlined,
  TranslationOutlined,
  UsbOutlined,
  UserAddOutlined,
  UserOutlined,
  WifiOutlined
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

    app.component('ApartmentOutlined', ApartmentOutlined)
    app.component('ApiOutlined', ApiOutlined)
    app.component('AppstoreOutlined', AppstoreOutlined)
    app.component('ArrowDownOutlined', ArrowDownOutlined)
    app.component('ArrowUpOutlined', ArrowUpOutlined)
    app.component('ArrowsAltOutlined', ArrowsAltOutlined)
    app.component('AuditOutlined', AuditOutlined)
    app.component('BankOutlined', BankOutlined)
    app.component('BarcodeOutlined', BarcodeOutlined)
    app.component('BarsOutlined', BarsOutlined)
    app.component('BellOutlined', BellOutlined)
    app.component('BlockOutlined', BlockOutlined)
    app.component('BuildOutlined', BuildOutlined)
    app.component('BulbOutlined', BulbOutlined)
    app.component('CalendarOutlined', CalendarOutlined)
    app.component('CameraOutlined', CameraOutlined)
    app.component('CaretDownOutlined', CaretDownOutlined)
    app.component('CaretRightOutlined', CaretRightOutlined)
    app.component('CaretUpOutlined', CaretUpOutlined)
    app.component('CheckCircleOutlined', CheckCircleOutlined)
    app.component('CheckCircleTwoTone', CheckCircleTwoTone)
    app.component('CheckOutlined', CheckOutlined)
    app.component('ClockCircleOutlined', ClockCircleOutlined)
    app.component('CloseCircleOutlined', CloseCircleOutlined)
    app.component('CloseCircleTwoTone', CloseCircleTwoTone)
    app.component('CloseOutlined', CloseOutlined)
    app.component('CloudDownloadOutlined', CloudDownloadOutlined)
    app.component('CloudOutlined', CloudOutlined)
    app.component('CloudUploadOutlined', CloudUploadOutlined)
    app.component('ClusterOutlined', ClusterOutlined)
    app.component('CodeOutlined', CodeOutlined)
    app.component('CopyOutlined', CopyOutlined)
    app.component('DashboardOutlined', DashboardOutlined)
    app.component('DatabaseOutlined', DatabaseOutlined)
    app.component('DeleteOutlined', DeleteOutlined)
    app.component('DeploymentUnitOutlined', DeploymentUnitOutlined)
    app.component('DesktopOutlined', DesktopOutlined)
    app.component('DoubleLeftOutlined', DoubleLeftOutlined)
    app.component('DoubleRightOutlined', DoubleRightOutlined)
    app.component('DownloadOutlined', DownloadOutlined)
    app.component('DragOutlined', DragOutlined)
    app.component('EditOutlined', EditOutlined)
    app.component('EnvironmentOutlined', EnvironmentOutlined)
    app.component('FileProtectOutlined', FileProtectOutlined)
    app.component('FilterOutlined', FilterOutlined)
    app.component('FilterTwoTone', FilterTwoTone)
    app.component('FireOutlined', FireOutlined)
    app.component('FlagOutlined', FlagOutlined)
    app.component('FolderAddOutlined', FolderAddOutlined)
    app.component('FolderOutlined', FolderOutlined)
    app.component('FormOutlined', FormOutlined)
    app.component('FullscreenOutlined', FullscreenOutlined)
    app.component('GatewayOutlined', GatewayOutlined)
    app.component('GithubOutlined', GithubOutlined)
    app.component('GlobalOutlined', GlobalOutlined)
    app.component('GoldOutlined', GoldOutlined)
    app.component('HddOutlined', HddOutlined)
    app.component('HomeOutlined', HomeOutlined)
    app.component('IdcardOutlined', IdcardOutlined)
    app.component('InboxOutlined', InboxOutlined)
    app.component('InfoCircleOutlined', InfoCircleOutlined)
    app.component('KeyOutlined', KeyOutlined)
    app.component('LinkOutlined', LinkOutlined)
    app.component('LoadingOutlined', LoadingOutlined)
    app.component('LockOutlined', LockOutlined)
    app.component('LoginOutlined', LoginOutlined)
    app.component('LogoutOutlined', LogoutOutlined)
    app.component('MedicineBoxOutlined', MedicineBoxOutlined)
    app.component('MenuFoldOutlined', MenuFoldOutlined)
    app.component('MenuUnfoldOutlined', MenuUnfoldOutlined)
    app.component('MinusCircleOutlined', MinusCircleOutlined)
    app.component('MoreOutlined', MoreOutlined)
    app.component('NotificationOutlined', NotificationOutlined)
    app.component('PaperClipOutlined', PaperClipOutlined)
    app.component('PauseCircleOutlined', PauseCircleOutlined)
    app.component('PictureOutlined', PictureOutlined)
    app.component('PlayCircleOutlined', PlayCircleOutlined)
    app.component('PlusOutlined', PlusOutlined)
    app.component('PoweroffOutlined', PoweroffOutlined)
    app.component('ProjectOutlined', ProjectOutlined)
    app.component('QuestionCircleOutlined', QuestionCircleOutlined)
    app.component('ReadOutlined', ReadOutlined)
    app.component('ReconciliationOutlined', ReconciliationOutlined)
    app.component('ReloadOutlined', ReloadOutlined)
    app.component('RocketOutlined', RocketOutlined)
    app.component('SafetyCertificateOutlined', SafetyCertificateOutlined)
    app.component('SafetyOutlined', SafetyOutlined)
    app.component('SaveOutlined', SaveOutlined)
    app.component('ScheduleOutlined', ScheduleOutlined)
    app.component('ScissorOutlined', ScissorOutlined)
    app.component('SearchOutlined', SearchOutlined)
    app.component('ShareAltOutlined', ShareAltOutlined)
    app.component('StopOutlined', StopOutlined)
    app.component('SwapOutlined', SwapOutlined)
    app.component('SyncOutlined', SyncOutlined)
    app.component('TeamOutlined', TeamOutlined)
    app.component('TranslationOutlined', TranslationOutlined)
    app.component('UsbOutlined', UsbOutlined)
    app.component('UserAddOutlined', UserAddOutlined)
    app.component('UserOutlined', UserOutlined)
    app.component('WifiOutlined', WifiOutlined)
  }
}
