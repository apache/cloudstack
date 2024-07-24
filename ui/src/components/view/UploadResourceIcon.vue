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
    <a-modal
      v-if="visible"
      :title="$t('label.upload.resource.icon')"
      :visible="visible"
      :maskClosable="true"
      :confirmLoading="confirmLoading"
      :width="800"
      :footer="null"
      @cancel="handleClose">
      <a-row>
        <a-col :xs="24" :md="12" :style="{height: '350px'}">
          <vue-cropper
            ref="cropper"
            :img="options.img"
            :outputSize="1"
            outputType="png"
            :info="true"
            :autoCrop="options.autoCrop"
            :autoCropWidth="options.autoCropWidth"
            :autoCropHeight="options.autoCropHeight"
            :fixedBox="options.fixedBox"
            @realTime="realTime"
          >
          </vue-cropper>
        </a-col>
        <a-col :xs="24" :md="12" :style="{height: '350px'}">
          <div class="avatar-upload-preview">
            <span v-if="previews.url">
              <img :src="previews.url" :style="previews.img"/>
            </span>
            <span v-else-if="defaultImage">
              <img :src="'data:image/png;charset=utf-8;base64, ' + defaultImage" style="height: 52px;width: 52px;marginLeft: 65px;marginTop: 55px"/>
            </span>
          </div>
        </a-col>
      </a-row>
      <br>
      <a-row>
        <a-col :xs="2" :md="2">
          <a-upload name="file" :beforeUpload="beforeUpload" :showUploadList="false">
            <a-button><upload-outlined />{{ $t('label.choose.resource.icon') }} </a-button>
          </a-upload>
        </a-col>
        <a-col :xs="{span: 2, offset: 4}" :md="1">
          <a-button @click="changeScale(5)">
            <template #icon><plus-outlined /></template>
          </a-button>
        </a-col>
        <a-col :xs="{span: 1, offset: 0}" :md="2">
          <a-button @click="changeScale(-5)">
            <template #icon><minus-outlined /></template>
          </a-button>
        </a-col>
        <a-col :lg="{span: 1, offset: 0}" :md="2">
          <a-button @click="rotateLeft">
            <template #icon><undo-outlined /></template>
          </a-button>
        </a-col>
        <a-col :lg="{span: 1, offset: 0}" :md="2">
          <a-button @click="rotateRight">
            <template #icon><redo-outlined /></template>
          </a-button>
        </a-col>
        <a-col :xs="{span: 1, offset: 3}" :md="1">
          <a-button :disabled="options.img === ''" type="primary" @click="uploadIcon('blob')"> {{ $t('label.upload') }} </a-button>
        </a-col>
        <a-col :xs="{span: 2, offset: 5}" :md="2">
          <a-button
            v-if="resource.icon && resource.icon.resourcetype.toLowerCase() === $getResourceType().toLowerCase()"
            type="primary"
            danger
            @click="deleteIcon('blob')"> {{ $t('label.delete') }} </a-button>
        </a-col>
      </a-row>
    </a-modal>
    <a-modal
      v-if="showAlert"
      :visible="showAlert"
      :footer="null"
      style="top: 20px;"
      centered
      width="auto"
      :maskClosable="false">
      <template #title>
        {{ $t('label.warning') }}
      </template>
      <a-alert type="warning">
        <template #message>
          <span v-html="$t('message.warn.filetype')" />
        </template>
      </a-alert>
      <a-divider style="margin-top: 0;"></a-divider>
      <div :span="24" class="action-button">
        <a-button key="submit" type="primary" @click="handleOk" style="textAlign: right">
          {{ $t('label.ok') }}
        </a-button>
      </div>
    </a-modal>
  </div>
</template>
<script>
import { api } from '@/api'
import eventBus from '@/config/eventBus'

export default {
  name: 'UploadResourceIcon',
  props: {
    visible: {
      type: Boolean,
      required: false,
      default: false
    },
    resource: {
      type: Object,
      required: true
    }
  },
  watch: {
    resource: {
      deep: true,
      handler () {
        this.defaultImage = this.resource?.icon?.base64image || ''
      }
    },
    preview: {
      deep: true,
      handler () {
        this.realTime(this.preview)
      }
    }
  },
  data () {
    return {
      id: null,
      confirmLoading: false,
      fileList: [],
      uploading: false,
      options: {
        img: '',
        autoCrop: true,
        autoCropWidth: 200,
        autoCropHeight: 200,
        fixedBox: true
      },
      previews: {},
      defaultImage: '',
      showAlert: false,
      croppedImage: ''
    }
  },
  methods: {
    handleClose () {
      this.options.img = ''
      this.previews = {}
      eventBus.emit('handle-close')
    },
    realTime (data) {
      if (data && data.url) {
        this.previews = data
      } else {
        if (this.resource?.icon?.base64image) {
          this.previews.url = 'data:image/png;charset=utf-8;base64, ' + (this.defaultImage || this.resource.icon.base64image || '')
          this.previews.img = {
            height: '52px',
            width: '52px',
            marginLeft: '65px',
            marginTop: '55px'
          }
        } else {
          this.previews = {}
        }
      }
    },
    handleOk () {
      this.showAlert = false
      this.options.img = ''
    },
    beforeUpload (file) {
      if (!/\.(bmp|jpeg|jpg|png|svg)$/i.test(file.name)) {
        this.showAlert = true
      }
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = () => {
        this.options.img = reader.result
      }
      return false
    },
    changeScale (num) {
      num = num || 1
      this.$refs.cropper.changeScale(num)
    },
    rotateLeft () {
      this.$refs.cropper.rotateLeft()
    },
    rotateRight () {
      this.$refs.cropper.rotateRight()
    },
    getResourceIcon () {
      return new Promise((resolve, reject) => {
        this.$refs.cropper.getCropData((data) => {
          resolve(data)
          return data
        })
      })
    },
    getNewImage (img) {
      return new Promise((resolve, reject) => {
        img.onload = function () {
          var canvas = document.createElement('canvas')
          var ctx = canvas.getContext('2d')
          ctx.imageSmoothingQuality = 'high'
          canvas.height = 52
          canvas.width = 52
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
          var base64Canvas = canvas.toDataURL('image/png', 1).split(';base64,')[1]
          resolve(base64Canvas)
          return base64Canvas
        }
      })
    },
    async uploadIcon () {
      var base64Canvas = ''
      const resourceType = this.$getResourceType()
      const resourceid = this.resource.id
      if (this.options.img) {
        var newImage = new Image()
        newImage.src = await this.getResourceIcon()
        base64Canvas = await this.getNewImage(newImage)
      }
      api('uploadResourceIcon', {}, 'POST', {
        resourceids: resourceid,
        resourcetype: resourceType,
        base64image: base64Canvas
      }).then(json => {
        console.log(this.resource)
        if (json?.uploadresourceiconresponse?.success) {
          this.$notification.success({
            message: this.$t('label.upload.icon'),
            description: `${this.$t('message.success.upload.icon')} ${resourceType}: ` + (this.resource.name || this.resource.username || resourceid)
          })
        }
      }).catch((error) => {
        this.$notification.error({
          message: this.$t('label.upload.icon'),
          description: error?.response?.data?.uploadresourceiconresponse?.errortext || '',
          duration: 0
        })
      }).finally(() => {
        this.handleClose()
        eventBus.emit('refresh-icon')
        if (['user', 'account'].includes(resourceType.toLowerCase())) {
          eventBus.emit('refresh-header')
        }
        if (['domain'].includes(this.$route.path.split('/')[1])) {
          eventBus.emit('refresh-domain-icon')
        }
      })
    },
    deleteIcon () {
      const resourceType = this.$getResourceType()
      const resourceid = this.resource.id
      api('deleteResourceIcon', {
        resourcetype: resourceType,
        resourceids: resourceid
      }).then(json => {
        if (json?.deleteresourceiconresponse?.success) {
          this.$notification.success({
            message: this.$t('label.delete.icon'),
            description: `${this.$t('message.success.delete.icon')} ${resourceType}: ` + (this.resource.name || this.resource.username || resourceid)
          })
        }
      }).catch((error) => {
        this.$notification.error({
          message: this.$t('label.delete.icon'),
          description: error?.response?.data?.deleteresourceiconresponse?.errortext || '',
          duration: 0
        })
      }).finally(() => {
        this.handleClose()
        eventBus.emit('refresh-icon')
        if (['user', 'account'].includes(resourceType.toLowerCase())) {
          eventBus.emit('refresh-header')
        }
        if (['domain'].includes(this.$route.path.split('/')[1])) {
          eventBus.emit('refresh-domain-icon')
        }
      })
    }
  }
}
</script>
<style lang="less" scoped>
  .avatar-upload-preview {
    position: absolute;
    top: 50%;
    transform: translate(50%, -50%);
    width: 200px;
    height: 200px;
    box-shadow: 0 0 4px #ccc;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
    }
  }
  .action-button {
    text-align: right;
    margin-top: 20px;

    button {
      margin-right: 5px;
    }
  }
</style>
