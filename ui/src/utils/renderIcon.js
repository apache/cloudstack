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

import { h, resolveComponent } from 'vue'

export default {
  name: 'RenderIcon',
  props: {
    icon: {
      type: String,
      default: ''
    },
    svgIcon: {
      type: Object,
      default: {}
    },
    props: {
      type: Object,
      default: {}
    },
    event: {
      type: Object,
      default: {}
    }
  },
  methods: {
    renderIcon () {
      return h(resolveComponent(this.icon), this.props, this.event)
    },
    renderSvgIcon () {
      const props = Object.assign({}, this.props)
      props.width = '1em'
      props.height = '1em'
      props.class = 'custom-icon'

      return h('span', { role: 'img', class: 'anticon' }, [
        h(this.svgIcon, { ...props }, this.event)
      ])
    }
  },
  render () {
    if (this.icon) {
      return this.renderIcon()
    }

    return this.renderSvgIcon()
  }
}
