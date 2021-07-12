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

  <div class="account-center-team" v-if="annotationType && 'listAnnotations' in $store.getters.apis">
    <a-divider :dashed="true"/>
    <a-spin :spinning="loadingAnnotations">
      <div class="title">
        {{ $t('label.comments') }} ({{ notes.length }})
      </div>
      <a-list
        v-if="notes.length"
        :dataSource="notes"
        itemLayout="horizontal"
        size="small" >
        <a-list-item slot="renderItem" slot-scope="item">
          <a-comment
            class="comment"
            :content="item.annotation"
            :datetime="$toLocaleDate(item.created)"
            :author="item.username" >
            <a-button
              v-if="'removeAnnotation' in $store.getters.apis && isAdminOrAnnotationOwner(item)"
              slot="avatar"
              type="danger"
              shape="circle"
              size="small"
              @click="deleteNote(item)">
              <a-icon type="delete"/>
            </a-button>
          </a-comment>
        </a-list-item>
      </a-list>

      <a-comment v-if="'addAnnotation' in $store.getters.apis">
        <a-avatar
          slot="avatar"
          icon="edit"
          @click="showNotesInput = true" />
        <div slot="content">
          <a-textarea
            rows="4"
            @change="handleNoteChange"
            :value="annotation"
            :placeholder="$t('label.add.note')" />
          <a-checkbox @change="toggleNoteVisibility" v-if="['Admin'].includes(this.$store.getters.userInfo.roletype)">
            {{ $t('label.annotation.admins.only') }}
          </a-checkbox>
          <a-button
            style="margin-top: 10px"
            @click="saveNote"
            type="primary"
          >
            {{ $t('label.save') }}
          </a-button>
        </div>
      </a-comment>
    </a-spin>
  </div>
</template>

<script>

import { api } from '@/api'

export default {
  name: 'AnnotationsTab',
  props: {
    resource: {
      type: Object,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    }
  },
  inject: ['parentFetchData'],
  data () {
    return {
      loadingAnnotations: false,
      notes: [],
      annotation: '',
      annotationType: '',
      annotationAdminsOnly: false,
      showNotesInput: false,

      annotationColumns: [
        {
          title: this.$t('label.created'),
          dataIndex: 'created'
        },
        {
          title: this.$t('label.username'),
          dataIndex: 'username'
        },
        {
          title: this.$t('label.annotation'),
          dataIndex: 'annotation'
        },
        {
          title: this.$t('label.annotation.entity.id'),
          dataIndex: 'entityid'
        },
        {
          title: this.$t('label.annotation.entity.type'),
          dataIndex: 'entitytype'
        },
        {
          title: this.$t('label.actions'),
          detaIndex: ''
        }
      ]
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
      this.resourceType = this.$route.meta.resourceType
      this.annotationType = ''

      switch (this.resourceType) {
        case 'UserVm':
          this.annotationType = 'VM'
          break
        case 'Domain':
          this.annotationType = 'DOMAIN'
          break
        case 'Host':
          this.annotationType = 'HOST'
          break
      }

      if (this.annotationType) {
        this.getAnnotations()
      }
    }
  },
  methods: {
    getAnnotations () {
      if (!('listAnnotations' in this.$store.getters.apis)) {
        return
      }
      this.loadingAnnotations = true
      this.notes = []
      api('listAnnotations', { entityid: this.resource.id, entitytype: this.annotationType, annotationfilter: 'self' }).then(json => {
        if (json.listannotationsresponse && json.listannotationsresponse.annotation) {
          this.notes = json.listannotationsresponse.annotation
        }
      }).finally(() => {
        this.loadingAnnotations = false
      })
    },
    handleNoteChange (e) {
      this.annotation = e.target.value
    },
    toggleNoteVisibility () {
      this.annotationAdminsOnly = !this.annotationAdminsOnly
    },
    isAdminOrAnnotationOwner (annotation) {
      return ['Admin'].includes(this.$store.getters.userInfo.roletype) || this.$store.getters.userInfo.id === annotation.userid
    },
    saveNote () {
      if (this.annotation.length < 1) {
        return
      }
      this.loadingAnnotations = true
      this.showNotesInput = false
      const args = {}
      args.entityid = this.resource.id
      args.entitytype = this.annotationType
      args.annotation = this.annotation
      args.adminsonly = this.annotationAdminsOnly
      api('addAnnotation', args).then(json => {
      }).finally(e => {
        this.getAnnotations()
      })
      this.annotation = ''
      this.annotationAdminsOnly = false
    },
    deleteNote (annotation) {
      this.loadingAnnotations = true
      const args = {}
      args.id = annotation.id
      api('removeAnnotation', args).then(json => {
      }).finally(e => {
        this.getAnnotations()
      })
    }
  }
}
</script>

<style lang="scss" scoped>

.account-center-team {
  .members {
    a {
      display: block;
      margin: 12px 0;
      line-height: 24px;
      height: 24px;
      .member {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.65);
        line-height: 24px;
        max-width: 100px;
        vertical-align: top;
        margin-left: 12px;
        transition: all 0.3s;
        display: inline-block;
      }
      &:hover {
        span {
          color: #1890ff;
        }
      }
    }
  }
}

.title {
  margin-bottom: 5px;
  font-weight: bold;
}

.comment {
  display: inline-block;
  text-overflow: ellipsis;
  width: calc(100%);
}
</style>
