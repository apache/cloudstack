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

<script>
import { defineComponent, ref, h, provide, inject, watch } from 'vue'

const INDENT = 18 // px per depth level

const C = {
  key: '#0451a5',
  string: '#a31515',
  number: '#098658',
  boolean: '#0000ff',
  null: '#767676',
  punct: '#333333',
  arrow: '#888888',
  summary: '#888888'
}

const JsonNode = defineComponent({
  name: 'JsonNode',
  props: {
    data: {},
    keyName: { type: String, default: null },
    depth: { type: Number, default: 0 },
    isLast: { type: Boolean, default: true }
  },
  setup (props) {
    // jeExpandMode tracks the last toolbar action so newly-mounted children
    // (revealed when their parent expands) pick up the correct initial state.
    const expandMode = inject('jeExpandMode', ref(null)) // null | 'expanded' | 'collapsed'
    const collapsed = ref(expandMode.value === 'expanded' ? false : props.depth >= 2)
    const toggle = () => { collapsed.value = !collapsed.value }

    const expandSignal = inject('jeExpand', ref(0))
    const collapseSignal = inject('jeCollapse', ref(0))
    watch(expandSignal, () => { collapsed.value = false })
    watch(collapseSignal, () => { collapsed.value = props.depth >= 2 })

    return () => {
      const { data, keyName, depth, isLast } = props
      const pl = `${depth * INDENT}px`

      // Key fragment: "name":·
      const keyFrag = keyName !== null
        ? [
          h('span', { style: { color: C.key } }, `"${keyName}"`),
          h('span', { style: { color: C.punct } }, ': ')
        ]
        : []

      const comma = !isLast ? h('span', { style: { color: C.punct } }, ',') : null

      // null
      if (data === null) {
        return h('div', { style: { paddingLeft: pl } }, [
          ...keyFrag,
          h('span', { style: { color: C.null } }, 'null'),
          comma
        ])
      }

      const type = Array.isArray(data) ? 'array' : typeof data

      // Object / Array
      if (type === 'object' || type === 'array') {
        const entries = type === 'array'
          ? data.map((v, i) => [i, v])
          : Object.entries(data)
        const count = entries.length
        const open = type === 'array' ? '[' : '{'
        const close = type === 'array' ? ']' : '}'

        if (count === 0) {
          return h('div', { style: { paddingLeft: pl } }, [
            ...keyFrag,
            h('span', { style: { color: C.punct } }, `${open}${close}`),
            comma
          ])
        }

        const arrowSt = {
          display: 'inline-block',
          width: '14px',
          color: C.arrow,
          cursor: 'pointer',
          userSelect: 'none',
          fontSize: '9px',
          verticalAlign: 'middle'
        }
        const summary = type === 'array'
          ? `${count} item${count !== 1 ? 's' : ''}`
          : `${count} key${count !== 1 ? 's' : ''}`

        if (collapsed.value) {
          return h('div', {
            style: { paddingLeft: pl, cursor: 'pointer', whiteSpace: 'nowrap' },
            onClick: toggle
          }, [
            h('span', { style: arrowSt }, '▶'),
            ...keyFrag,
            h('span', { style: { color: C.punct } }, `${open} `),
            h('span', { style: { color: C.summary, fontStyle: 'italic' } }, summary),
            h('span', { style: { color: C.punct } }, ` ${close}`),
            comma
          ])
        }

        return h('div', {}, [
          // Opening line with toggle
          h('div', { style: { paddingLeft: pl, cursor: 'pointer' }, onClick: toggle }, [
            h('span', { style: arrowSt }, '▼'),
            ...keyFrag,
            h('span', { style: { color: C.punct } }, open)
          ]),
          // Children — each child provides its own paddingLeft via (depth+1)*INDENT
          ...entries.map(([k, v], i) =>
            h(JsonNode, {
              key: String(k),
              data: v,
              keyName: type === 'array' ? null : String(k),
              depth: depth + 1,
              isLast: i === count - 1
            })
          ),
          // Closing bracket at same indentation as the opening line
          h('div', { style: { paddingLeft: pl } }, [
            h('span', { style: { color: C.punct } }, close),
            comma
          ])
        ])
      }

      // Primitives
      let valueEl
      if (type === 'string') {
        valueEl = h('span', { style: { color: C.string } }, `"${data}"`)
      } else if (type === 'number') {
        valueEl = h('span', { style: { color: C.number } }, String(data))
      } else {
        // boolean
        valueEl = h('span', { style: { color: C.boolean } }, String(data))
      }

      return h('div', { style: { paddingLeft: pl } }, [
        ...keyFrag,
        valueEl,
        comma
      ])
    }
  }
})

export default defineComponent({
  name: 'JsonViewer',
  components: { JsonNode },
  props: {
    data: { required: true }
  },
  setup (props) {
    const expandSignal = ref(0)
    const collapseSignal = ref(0)
    const expandMode = ref(null) // tracks last toolbar action for newly-mounted nodes
    provide('jeExpand', expandSignal)
    provide('jeCollapse', collapseSignal)
    provide('jeExpandMode', expandMode)

    const copyDone = ref(false)

    const copyJson = () => {
      const text = JSON.stringify(props.data, null, 2)
      if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
          copyDone.value = true
          setTimeout(() => { copyDone.value = false }, 1500)
        })
      }
    }

    return () => h('div', {
      style: {
        position: 'relative',
        fontFamily: "'Consolas', 'Courier New', monospace",
        fontSize: '12px',
        lineHeight: '1.7',
        backgroundColor: '#fafafa',
        border: '1px solid #e0e0e0',
        borderRadius: '4px',
        maxHeight: '60vh',
        overflow: 'auto',
        padding: '10px 10px 10px 6px'
      }
    }, [
      // Toolbar (top-right, sticky): Expand All · Collapse All · Copy
      h('div', {
        style: {
          position: 'sticky',
          top: 0,
          float: 'right',
          zIndex: 1,
          marginBottom: '-20px',
          display: 'flex',
          gap: '4px'
        }
      }, [
        h('span', {
          onClick: () => { expandMode.value = 'expanded'; expandSignal.value++ },
          title: 'Expand all nodes',
          style: {
            cursor: 'pointer',
            fontSize: '11px',
            padding: '2px 8px',
            backgroundColor: '#e8e8e8',
            border: '1px solid #ccc',
            borderRadius: '3px',
            color: '#555',
            userSelect: 'none'
          }
        }, '⊞ Expand all'),
        h('span', {
          onClick: () => { expandMode.value = 'collapsed'; collapseSignal.value++ },
          title: 'Collapse to default view (2 levels)',
          style: {
            cursor: 'pointer',
            fontSize: '11px',
            padding: '2px 8px',
            backgroundColor: '#e8e8e8',
            border: '1px solid #ccc',
            borderRadius: '3px',
            color: '#555',
            userSelect: 'none'
          }
        }, '⊟ Collapse all'),
        h('span', {
          onClick: copyJson,
          title: 'Copy JSON',
          style: {
            cursor: 'pointer',
            fontSize: '11px',
            padding: '2px 8px',
            backgroundColor: copyDone.value ? '#d4edda' : '#e8e8e8',
            border: '1px solid #ccc',
            borderRadius: '3px',
            color: copyDone.value ? '#155724' : '#555',
            userSelect: 'none'
          }
        }, copyDone.value ? '✓ Copied' : 'Copy')
      ]),
      // Tree root
      h(JsonNode, { data: props.data, depth: 0, keyName: null, isLast: true })
    ])
  }
})
</script>
