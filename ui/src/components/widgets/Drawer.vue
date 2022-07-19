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
    <div :class="['mask', visible ? 'open' : 'close']" @click="close"></div>
    <div :class="['drawer', placement, visible ? 'open' : 'close']">
      <div ref="drawer" class="content">
        <slot name="drawer"></slot>
      </div>

      <div
        :class="['handler-container', placement, visible ? 'open' : 'close']"
        ref="handler"
        @click="toggle">
        <slot v-if="$slots.handler" name="handler"></slot>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Drawer',
  data () {
    return {
    }
  },
  model: {
    prop: 'visible',
    event: 'change'
  },
  props: {
    visible: {
      type: Boolean,
      required: false,
      default: false
    },
    placement: {
      type: String,
      required: false,
      default: 'left'
    },
    showHandler: {
      type: Boolean,
      required: false,
      default: true
    }
  },
  inject: ['parentToggleSetting'],
  methods: {
    open () {
      this.parentToggleSetting(true)
    },
    close () {
      this.parentToggleSetting(false)
    },
    toggle () {
      this.parentToggleSetting(!this.visible)
    }
  }
}
</script>

<style lang="less" scoped>
.mask {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  top: 0;
  transition: all 0.5s;
  z-index: 100;
  background-color: #000000;
  opacity: 0.2;

  &.open{
    display: inline-block;
  }

  &.close{
    display: none;
  }
}

.drawer{
  position: fixed;
  transition: all 0.5s;
  height: 100vh;
  z-index: 100;

  &.left{
    left: 0;

    &.close{
      transform: translateX(-100%);
    }
  }

  &.right{
    right: 0;

    &.close{
      transform: translateX(100%);
    }
  }
}

.content {
  display: inline-block;
  height: 100vh;
  overflow-y: auto;
  width: 300px;
  background-color: #FFFFFF;
}

.handler-container {
  position: absolute;
  display: inline-block;
  text-align: center;
  transition: all 0.5s;
  cursor: pointer;
  top: calc(50% - 45px);
  z-index: 100;

  &.left{
    right: -40px;

    .handler{
      border-radius: 0 5px 5px 0;
    }

    :deep(button) {
      border-top-left-radius: 0;
      border-bottom-left-radius: 0;
      padding-left: 10px;
      padding-right: 12px;
    }
  }

  &.right{
    left: -40px;

    .handler {
      border-radius: 5px 0 0 5px;
    }

    :deep(button) {
      border-top-right-radius: 0;
      border-bottom-right-radius: 0;
      padding-left: 12px;
      padding-right: 10px;
    }
  }
}
</style>
