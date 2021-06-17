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
  <a-modal
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
          <img :src="previews.url" :style="previews.img"/>
        </div>
      </a-col>
    </a-row>
    <br>
    <a-row>
      <a-col :lg="2" :md="2">
        <a-upload name="file" :beforeUpload="beforeUpload" :showUploadList="false">
          <a-button><a-icon type="upload" />{{ $t('label.upload.resource.icon') }} </a-button>
        </a-upload>
      </a-col>
      <a-col :lg="{span: 1, offset: 2}" :md="2">
        <a-button icon="plus" @click="changeScale(1)"/>
      </a-col>
      <a-col :lg="{span: 1, offset: 1}" :md="2">
        <a-button icon="minus" @click="changeScale(-1)"/>
      </a-col>
      <a-col :lg="{span: 2, offset: 9}" :md="2">
        <a-button type="primary" @click="uploadIcon('blob')"> {{ $t('label.upload') }} </a-button>
      </a-col>
      <a-col :lg="{span: 2, offset: 1}" :md="2">
        <a-button type="danger" @click="deleteIcon('blob')"> {{ $t('label.delete') }} </a-button>
      </a-col>
    </a-row>
  </a-modal>
</template>
<script>
import { api } from '@/api'
import eventBus from '../../config/eventBus'

export default {
  name: 'ResourceIcon',
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
  inject: ['parentFetchData'],
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
      previews: {}
    }
  },
  methods: {
    handleClose () {
      eventBus.$emit('handle-close')
    },
    realTime (data) {
      if (data.url) {
        this.previews = data
      } else {
        this.previews.url = 'data:image/png;charset=utf-8;base64, ' + this.resource?.icon?.base64image || ''
        this.previews.img = {
          height: '52',
          width: '52'
        }
      }
    },
    beforeUpload (file) {
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
    getResourceType () {
      const type = this.$route.path.split('/')[1]
      if (type !== 'vm') {
        return type
      }
      return 'UserVM'
    },
    uploadIcon () {
      var base64Canvas = ''
      const resourceType = this.getResourceType()
      const resourceid = this.resource.id
      if (this.options.img) {
        const width = 32
        const height = 32
        var newImage = document.createElement('img')
        newImage.src = this.options.img
        var canvas = document.createElement('canvas')
        var ctx = canvas.getContext('2d')
        ctx.imageSmoothingQuality = 'high'
        canvas.height = height
        canvas.width = width
        ctx.drawImage(newImage, 0, 0, width, height)
        base64Canvas = canvas.toDataURL('image/png', 1).split(';base64,')[1]
      }
      api('uploadResourceIcon', {
        resourceids: resourceid,
        resourcetype: resourceType,
        base64image: base64Canvas
      }).then(json => {
        if (json?.uploadresourceiconresponse?.success) {
          this.$notification.success({
            message: this.$t('label.upload.icon'),
            description: `${this.$t('message.success.upload.icon')} ${resourceType} :` + this.resource.name || resourceid
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
        this.parentFetchData()
      })
    },
    deleteIcon () {
      const resourceType = this.getResourceType()
      const resourceid = this.resource.id
      api('deleteResourceIcon', {
        resourcetype: resourceType,
        resourceids: resourceid
      }).then(json => {
        if (json?.deleteresourceiconresponse?.success) {
          this.$notification.success({
            message: this.$t('label.delete.icon'),
            description: `${this.$t('message.success.delete.icon')} ${resourceType} :` + this.resource.name || this.resourceid
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
        this.parentFetchData()
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
    width: 180px;
    height: 180px;
    border-radius: 50%;
    box-shadow: 0 0 4px #ccc;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
    }
  }
</style>
