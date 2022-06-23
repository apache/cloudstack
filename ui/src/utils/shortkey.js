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

import 'element-matches'
import 'custom-event-polyfill'
import { vueApp } from '@/vue-app'

const ShortKey = {}
const mapFunctions = {}
const elementAvoided = ['input', 'textarea']
const definedCharKeys = []
let objAvoided = []
let keyPressed = false
let charKeyPressed = false

const parseValue = (value) => {
  value = typeof value === 'string' ? JSON.parse(value.replace(/\'/gi, '"')) : value // eslint-disable-line
  if (value instanceof Array) {
    return { '': value }
  }
  return value
}

const bindValue = (value, el, binding, vnode) => {
  const push = binding.modifiers.push === true
  const avoid = binding.modifiers.avoid === true
  const focus = !binding.modifiers.focus === true
  const once = binding.modifiers.once === true
  const propagte = binding.modifiers.propagte === true
  if (avoid) {
    objAvoided = objAvoided.filter((itm) => {
      return !itm === el
    })
    objAvoided.push(el)
  } else {
    mappingFunctions({ b: value, push, once, focus, propagte, el: vnode.elm })
  }
}

const unbindValue = (value, el) => {
  for (const key in value) {
    const k = ShortKey.encodeKey(value[key])
    const idxElm = mapFunctions[k].el.indexOf(el)
    if (mapFunctions[k].el.length > 1 && idxElm > -1) {
      mapFunctions[k].el.splice(idxElm, 1)
    } else {
      delete mapFunctions[k]
    }
  }
}
vueApp.directive('shortkey', {
  bind: (el, binding, vnode) => {
    // Mapping the commands
    const value = parseValue(binding.value)
    bindValue(value, el, binding, vnode)
  },
  update: (el, binding, vnode) => {
    const oldValue = parseValue(binding.oldValue)
    unbindValue(oldValue, el)

    const newValue = parseValue(binding.value)
    bindValue(newValue, el, binding, vnode)
  },
  unbind: (el, binding) => {
    const value = parseValue(binding.value)
    unbindValue(value, el)
  }
})

ShortKey.decodeKey = (pKey) => createShortcutIndex(pKey)
ShortKey.encodeKey = (pKey) => {
  const shortKey = {}
  shortKey.shiftKey = pKey.includes('shift')
  shortKey.ctrlKey = pKey.includes('ctrl')
  shortKey.metaKey = pKey.includes('meta')
  shortKey.altKey = pKey.includes('alt')
  shortKey.charKey = (String(pKey[0]) + String(pKey[1])).match(/^[a-zA-Z0-9]{2}$/)
  if (shortKey.charKey) {
    const chars = {}
    chars.base = shortKey.charKey.input[0]
    chars.action = shortKey.charKey.input[1]
    definedCharKeys.push(chars)
  }
  let indexedKeys = createShortcutIndex(shortKey)
  const vKey = pKey.filter((item) => !['shift', 'ctrl', 'meta', 'alt'].includes(item))
  indexedKeys += vKey.join('')
  return indexedKeys
}

const createShortcutIndex = (pKey) => {
  let k = ''
  if (definedCharKeys.length) {
    definedCharKeys.forEach((keySequence) => {
      if (keySequence.base === pKey.key && !pKey.shiftKey && !pKey.ctrlKey && !pKey.metaKey && !pKey.altKey) {
        charKeyPressed = pKey.key
      }
    })
  }
  if (pKey.key === 'Shift' || pKey.shiftKey) { k += 'shift' }
  if (pKey.key === 'Control' || pKey.ctrlKey) { k += 'ctrl' }
  if (pKey.key === 'Meta' || pKey.metaKey) { k += 'meta' }
  if (pKey.key === 'Alt' || pKey.altKey) { k += 'alt' }
  if (pKey.key === 'ArrowUp') { k += 'arrowup' }
  if (pKey.key === 'ArrowLeft') { k += 'arrowleft' }
  if (pKey.key === 'ArrowRight') { k += 'arrowright' }
  if (pKey.key === 'ArrowDown') { k += 'arrowdown' }
  if (pKey.key === 'AltGraph') { k += 'altgraph' }
  if (pKey.key === 'Escape') { k += 'esc' }
  if (pKey.key === 'Enter') { k += 'enter' }
  if (pKey.key === 'Tab') { k += 'tab' }
  if (pKey.key === ' ') { k += 'space' }
  if (pKey.key === 'PageUp') { k += 'pageup' }
  if (pKey.key === 'PageDown') { k += 'pagedown' }
  if (pKey.key === 'Home') { k += 'home' }
  if (pKey.key === 'End') { k += 'end' }
  if (pKey.key === 'Delete') { k += 'del' }
  if (pKey.key === 'Backspace') { k += 'backspace' }
  if (pKey.key === 'Insert') { k += 'insert' }
  if (pKey.key === 'NumLock') { k += 'numlock' }
  if (pKey.key === 'CapsLock') { k += 'capslock' }
  if (pKey.key === 'Pause') { k += 'pause' }
  if (pKey.key === 'ContextMenu') { k += 'contextmenu' }
  if (pKey.key === 'ScrollLock') { k += 'scrolllock' }
  if (pKey.key === 'BrowserHome') { k += 'browserhome' }
  if (pKey.key === 'MediaSelect') { k += 'mediaselect' }
  if (charKeyPressed) {
    definedCharKeys.forEach((keySequence) => {
      if ((keySequence.base + keySequence.action) === (charKeyPressed + pKey.key)) {
        k = keySequence.base + keySequence.action
      }
    })
  } else if ((!charKeyPressed && pKey.key && pKey.key !== ' ' && pKey.key.length === 1) || /F\d{1,2}|\//g.test(pKey.key)) {
    k += pKey.key.toLowerCase()
  }
  return k
}

const dispatchShortkeyEvent = (pKey) => {
  const e = new CustomEvent('shortkey', { bubbles: false })
  if (mapFunctions[pKey].key) e.srcKey = mapFunctions[pKey].key
  const elm = mapFunctions[pKey].el
  if (!mapFunctions[pKey].propagte) {
    elm[elm.length - 1].dispatchEvent(e)
  } else {
    elm.forEach(elmItem => elmItem.dispatchEvent(e))
  }
}

ShortKey.keyDown = (pKey) => {
  if ((!mapFunctions[pKey].once && !mapFunctions[pKey].push) || (mapFunctions[pKey].push && !keyPressed)) {
    dispatchShortkeyEvent(pKey)
  }
}

if (process && process.env && process.env.NODE_ENV !== 'test') {
  (function () {
    document.addEventListener('keydown', (pKey) => {
      const decodedKey = ShortKey.decodeKey(pKey)
      // Check avoidable elements
      if (availableElement(decodedKey)) {
        if (!mapFunctions[decodedKey].propagte) {
          pKey.preventDefault()
          pKey.stopPropagation()
        }
        if (mapFunctions[decodedKey].focus) {
          ShortKey.keyDown(decodedKey)
          keyPressed = true
        } else if (!keyPressed) {
          const elm = mapFunctions[decodedKey].el
          elm[elm.length - 1].focus()
          keyPressed = true
        }
      }
    }, true)

    document.addEventListener('keyup', (pKey) => {
      const decodedKey = ShortKey.decodeKey(pKey)
      if (availableElement(decodedKey)) {
        if (!mapFunctions[decodedKey].propagte) {
          pKey.preventDefault()
          pKey.stopPropagation()
        }
        if (mapFunctions[decodedKey].once || mapFunctions[decodedKey].push) {
          dispatchShortkeyEvent(decodedKey)
        }
      }
      if (definedCharKeys.length) {
        definedCharKeys.forEach((keySequence) => {
          if (keySequence.base === pKey.key) {
            charKeyPressed = false
          }
        })
      }
      keyPressed = false
    }, true)
  })()
}

const mappingFunctions = ({ b, push, once, focus, propagte, el }) => {
  for (const key in b) {
    const k = ShortKey.encodeKey(b[key])
    const elm = mapFunctions[k] && mapFunctions[k].el ? mapFunctions[k].el : []
    const propagated = mapFunctions[k] && mapFunctions[k].propagte
    elm.push(el)
    mapFunctions[k] = {
      push,
      once,
      focus,
      key,
      propagte: propagated || propagte,
      el: elm
    }
  }
}

const availableElement = (decodedKey) => {
  const objectIsAvoided = !!objAvoided.find(r => r === document.activeElement)
  const filterAvoided = !!(elementAvoided.find(selector => document.activeElement && document.activeElement.matches(selector)))
  return !!mapFunctions[decodedKey] && !(objectIsAvoided || filterAvoided)
}
