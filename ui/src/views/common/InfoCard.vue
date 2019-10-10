<template>
  <a-card :bordered="true">
    <div class="resource-details">
      <div class="avatar">
        <slot name="avatar">
          <a-icon style="font-size: 36px" :type="$route.meta.icon" />
        </slot>
      </div>
      <div class="name">
        <slot name="name">
          <h4>
            <status :text="resource.state" v-if="resource.state"/>
            {{ resource.name }}
          </h4>
          <a-tag v-if="resource.state || resource.status">
            {{ resource.state || resource.status }}
          </a-tag>
          <a-tag v-if="resource.type">
            {{ resource.type }}
          </a-tag>
          <a-tag v-if="resource.hypervisor">
            {{ resource.hypervisor }}
          </a-tag>
        </slot>
      </div>
    </div>
    <div class="resource-center-detail">
      <div class="resource-detail-item" v-if="resource.state || resource.status">
        <status :text="resource.state || resource.status" class="resource-detail-item" />
        <span style="margin-left: -5px">{{ resource.state || resource.status }}</span>
      </div>
      <div class="resource-detail-item" v-if="resource.id">
        <a-icon type="barcode" class="resource-detail-item"/>{{ resource.id }}
        <a-tooltip placement="right" >
          <template slot="title">
            <span>Copy</span>
          </template>
          <a-button shape="circle" type="dashed" size="small" v-clipboard:copy="resource.id">
            <a-icon type="copy"/>
          </a-button>
        </a-tooltip>
      </div>
      <div class="resource-detail-item">
        <slot name="details">
        </slot>
      </div>
      <div class="resource-detail-item" v-if="resource.vmname && resource.virtualmachineid">
        <a-icon type="desktop" class="resource-detail-item"/>
        <router-link :to="{ path: '/vm/' + resource.virtualmachineid }">{{ resource.vmname }} </router-link>
        <status :text="resource.vmstate" v-if="resource.vmstate"/>
      </div>
      <div class="resource-detail-item" v-if="resource.serviceofferingname && resource.serviceofferingid">
        <a-icon type="cloud" class="resource-detail-item"/>
        <router-link :to="{ path: '/computeoffering/' + resource.serviceofferingid }">{{ resource.serviceofferingname }} </router-link>
      </div>
      <div class="resource-detail-item" v-if="resource.templatename && resource.templateid">
        <a-icon type="picture" class="resource-detail-item"/>
        <router-link :to="{ path: '/template/' + resource.templateid }">{{ resource.templatename }} </router-link>
      </div>
      <div class="resource-detail-item" v-if="resource.storage && resource.storageid">
        <a-icon type="database" class="resource-detail-item"/>
        <router-link :to="{ path: '/storagepool/' + resource.storageid }">{{ resource.storage }} </router-link>
        <a-tag v-if="resource.storagetype">
          {{ resource.storagetype }}
        </a-tag>
      </div>
      <div class="resource-detail-item" v-if="resource.hostname && resource.hostid">
        <a-icon type="desktop" class="resource-detail-item"/>
        <router-link :to="{ path: '/host/' + resource.hostid }">{{ resource.hostname }} </router-link>
        <a-tag v-if="resource.hypervisor">
          {{ resource.hypervisor }}
        </a-tag>
      </div>
      <div class="resource-detail-item" v-if="resource.zonename && resource.zoneid">
        <a-icon type="global" class="resource-detail-item"/>
        <router-link :to="{ path: '/zone/' + resource.zoneid }">{{ resource.zonename }}</router-link>
      </div>
      <div class="resource-detail-item" v-if="resource.account">
        <a-icon type="user" class="resource-detail-item"/>
        <router-link :to="{ path: '/account?name=' + resource.account }">{{ resource.account }}</router-link>
      </div>
      <div class="resource-detail-item" v-if="resource.domain && resource.domainid">
        <a-icon type="block" class="resource-detail-item"/>
        <router-link :to="{ path: '/domain/' + resource.domainid }">{{ resource.domain }}</router-link>
      </div>
      <div class="resource-detail-item" v-if="resource.created">
        <a-icon type="calendar" class="resource-detail-item"/>{{ resource.created }}
      </div>
    </div>

    <div class="account-center-tags" v-if="resourceType">
      <a-divider/>
      <div class="tagsTitle">Tags</div>
      <div>
        <template v-for="(tag, index) in tags">
          <a-tag :key="index" :closable="true" :afterClose="() => handleDeleteTag(tag)">
            {{ tag.key }} = {{ tag.value }}
          </a-tag>
        </template>

        <div v-if="inputVisible">
          <a-input-group
            type="text"
            size="small"
            @blur="handleInputConfirm"
            @keyup.enter="handleInputConfirm"
            compact>
            <a-input ref="input" :value="inputKey" @change="handleKeyChange" style="width: 100px; text-align: center" placeholder="Key" />
            <a-input style=" width: 30px; border-left: 0; pointer-events: none; backgroundColor: #fff" placeholder="=" disabled />
            <a-input :value="inputValue" @change="handleValueChange" style="width: 100px; text-align: center; border-left: 0" placeholder="Value" />
            <a-button shape="circle" size="small" @click="handleInputConfirm">
              <a-icon type="check"/>
            </a-button>
            <a-button shape="circle" size="small" @click="inputVisible=false">
              <a-icon type="close"/>
            </a-button>
          </a-input-group>
        </div>
        <a-tag v-else @click="showInput" style="background: #fff; borderStyle: dashed;">
          <a-icon type="plus" /> New Tag
        </a-tag>
      </div>
    </div>

    <div class="account-center-team" v-if="showNotes">
      <a-divider :dashed="true"/>
      <div class="teamTitle">
        Comments ({{ notes.length }})
      </div>

      <a-list
        v-if="notes.length"
        :dataSource="notes"
        itemLayout="horizontal"
        size="small"
      >
        <a-list-item slot="renderItem" slot-scope="item">
          <a-comment
            :content="item.annotation"
            :datetime="item.created"
          >
            <a-button slot="avatar" shape="circle" size="small" @click="deleteNote(item)">
              <a-icon type="close"/>
            </a-button>
          </a-comment>
        </a-list-item>
      </a-list>

      <a-comment>
        <a-avatar
          slot="avatar"
          icon="edit"
          @click="showNotesInput = true"
        />
        <div slot="content">
          <a-textarea
            rows="4"
            @change="handleNoteChange"
            :value="annotation"
            placeholder="Add Note" />
          <a-button
            @click="saveNote"
            type="primary"
          >
            Save
          </a-button>
        </div>
      </a-comment>
    </div>
  </a-card>
</template>

<script>

import { api } from '@/api'
import Status from '@/components/widgets/Status'

export default {
  name: 'InfoCard',
  components: {
    Status
  },
  props: {
    resource: {
      type: Object,
      required: true
    },
    resourceType: {
      type: String,
      required: true
    },
    showNotes: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      inputVisible: false,
      inputKey: '',
      inputValue: '',
      tags: [],
      notes: [],
      annotation: '',
      showNotesInput: false
    }
  },
  watch: {
    resource: function (newItem, oldItem) {
      this.resource = newItem
      this.getTags()
      this.getNotes()
    }
  },
  methods: {
    getTags () {
      this.tags = []
      api('listTags', { 'listall': true, 'resourceid': this.resource.id, 'resourcetype': this.resourceType }).then(json => {
        if (json.listtagsresponse && json.listtagsresponse.tag) {
          this.tags = json.listtagsresponse.tag
        }
      })
    },
    getNotes () {
      // TODO: support for HOST, DOMAIN and VM entities
      this.notes = []
      api('listAnnotations', { 'entityid': this.resource.id, 'entitytype': 'VM' }).then(json => {
        if (json.listannotationsresponse && json.listannotationsresponse.annotation) {
          this.notes = json.listannotationsresponse.annotation
        }
      })
    },
    showInput () {
      this.inputVisible = true
      this.$nextTick(function () {
        this.$refs.input.focus()
      })
    },
    handleKeyChange (e) {
      this.inputKey = e.target.value
    },
    handleValueChange (e) {
      this.inputValue = e.target.value
    },
    handleInputConfirm () {
      const args = {}
      args['resourceids'] = this.resource.id
      args['resourcetype'] = this.resourceType
      args['tags[0].key'] = this.inputKey
      args['tags[0].value'] = this.inputValue
      api('createTags', args).then(json => {
      }).finally(e => {
        this.getTags()
      })

      this.inputVisible = false
      this.inputKey = ''
      this.inputValue = ''
    },
    handleDeleteTag (tag) {
      const args = {}
      args['resourceids'] = tag.resourceid
      args['resourcetype'] = tag.resourcetype
      args['tags[0].key'] = tag.key
      args['tags[0].value'] = tag.value
      api('deleteTags', args).then(json => {
      }).finally(e => {
        this.getTags()
      })
    },
    handleNoteChange (e) {
      this.annotation = e.target.value
    },
    saveNote () {
      if (this.annotation.length < 1) {
        return
      }
      this.showNotesInput = false
      const entityType = 'VM' // TODO: support HOST and DOMAIN

      const args = {}
      args['annotation'] = this.annotation
      args['entityid'] = this.resource.id
      args['entitytype'] = entityType
      api('addAnnotation', args).then(json => {
      }).finally(e => {
        this.getNotes()
      })
      this.annotation = ''
    },
    deleteNote (annotation) {
      const args = {}
      args['id'] = annotation.id
      api('removeAnnotation', args).then(json => {
      }).finally(e => {
        this.getNotes()
      })
    }
  }
}
</script>

<style lang="less" scoped>

.resource-details {
  text-align: center;
  margin-bottom: 24px;
  & > .avatar {
    margin: 0 auto;
    padding-top: 20px;
    width: 104px;
    //height: 104px;
    margin-bottom: 20px;
    border-radius: 50%;
    overflow: hidden;
    img {
      height: 100%;
      width: 100%;
    }
  }
  .name {
    color: rgba(0, 0, 0, 0.85);
    font-size: 20px;
    line-height: 28px;
    font-weight: 500;
    margin-bottom: 4px;
  }
}
.resource-center-detail {
  p {
    margin-bottom: 8px;
    padding-left: 12px;
    padding-right: 12px;
    position: relative;

    font-awesome-icon, .svg-inline--fa {
      width: 30px;
    }
  }
  .title {
    background-position: 0 0;
  }
  .group {
    background-position: 0 -22px;
  }
  .address {
    background-position: 0 -44px;
  }
}
.resource-detail-item {
  margin-bottom: 8px;
  margin-left: 10px;
  margin-right: 10px;
}
.account-center-tags {
  .ant-tag {
    margin-bottom: 8px;
  }
}
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
.tagsTitle,
.teamTitle {
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}
</style>
