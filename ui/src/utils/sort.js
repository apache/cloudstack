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

function filterNumber (value) {
  if (/^[-+]?\d*\.?\d*$/.test(value)) {
    return Number(value)
  }
  return NaN
}

function stringComparator (a, b) {
  return a.localeCompare(b)
}

function numericComparator (a, b) {
  return filterNumber(a) < filterNumber(b) ? 1 : -1
}

function ipV4AddressCIDRComparator (a, b) {
  a = a.split(/[./]/gm)
  b = b.split(/[./]/gm)
  for (var i = 0; i < a.length; i++) {
    if ((a[i] = parseInt(a[i])) < (b[i] = parseInt(b[i]))) {
      return -1
    } else if (a[i] > b[i]) {
      return 1
    }
  }
  return 0
}

function ipV6AddressCIDRComparator (a, b) {
  a = a.split(/[:/]/gm)
  b = b.split(/[:/]/gm)
  for (var i = 0; i < a.length; i++) {
    if ((a[i] = parseInt('0x' + a[i], 16)) < (b[i] = parseInt('0x' + b[i], 16))) {
      return -1
    } else if (a[i] > b[i]) {
      return 1
    }
  }
  return 0
}

function isIpV4Address (obj) {
  return !Array.isArray(obj) && (/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$/gm).test(obj)
}

function isIpV6Address (obj) {
  return !Array.isArray(obj) && (/^[a-fA-F0-9:]+$/gm).test(obj)
}

function isIpV4CIDRAddress (obj) {
  return !Array.isArray(obj) && (/^([0-9]{1,3}\.){3}[0-9]{1,3}(\/([0-9]|[1-2][0-9]|3[0-2]))?$/gm).test(obj)
}

function isIpV6CIDRAddress (obj) {
  return !Array.isArray(obj) && (/^s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]d|1dd|[1-9]?d)(.(25[0-5]|2[0-4]d|1dd|[1-9]?d)){3}))|:)))(%.+)?s*(\/([0-9]|[1-9][0-9]|1[0-1][0-9]|12[0-8]))?$/gm).test(obj)
}

function isNumeric (obj) {
  return !Array.isArray(obj) && !isNaN(filterNumber(obj))
}

/**
 * Compare elements, attempting to determine type of element to get the best comparison
 *
 */
export function genericCompare (a, b) {
  // strict function for filtering numbers (e.g. "2.3", "-2" but not "8 CPUs")
  var comparator = stringComparator

  if (a === b) {
    // Short circuit out to avoid unnecessary effort
    return 0
  }
  if ((isIpV4CIDRAddress(a) || isIpV4Address(a)) && (isIpV4CIDRAddress(b) || isIpV4Address(b))) {
    comparator = ipV4AddressCIDRComparator
  }
  if ((isIpV6CIDRAddress(a) || isIpV6Address(a)) && (isIpV6CIDRAddress(b) || isIpV6Address(b))) {
    comparator = ipV6AddressCIDRComparator
  }
  if (isNumeric(a) && isNumeric(b)) {
    comparator = numericComparator
  }

  return comparator(a, b)
}
