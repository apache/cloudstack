// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
// Stands in for CloudStack's '@/api' helper, which only exists inside
// their build. Records calls so tests can assert a screen fetches
// what it needs, and answers with whatever was queued.
const calls = []
let answers = {}

export function reset (responses = {}) {
  calls.length = 0
  answers = responses
}

export function apiCalls () {
  return calls
}

// Queue an Error for a command and the call rejects, which is what
// CloudStack's own helper does on a 4xx or 5xx. Without this every
// test saw a resolved promise, so a screen with no `.catch()` looked
// identical to one that handled failure — and the real screens showed
// a blank page with an unhandled rejection behind it.
function answer (command, params) {
  calls.push({ command, params })
  const queued = answers[command]
  return queued instanceof Error ? Promise.reject(queued) : Promise.resolve(queued ?? {})
}

export function getAPI (command, params) {
  return answer(command, params)
}

export function postAPI (command, params) {
  return answer(command, params)
}

// An axios error shaped the way CloudStack actually refuses a command.
// This matters more than it looks: a bare `new Error(reason)` carries
// the reason on `.message`, so a screen reading `.message` passed its
// test while showing "Request failed with status code 534" on a real
// server. CloudStack wraps every answer, refusals included, in a key
// named after the command, and the engine's sentence is the
// `errortext` inside it.
export function refusal (command, errortext, errorcode = 530) {
  const problem = new Error(`Request failed with status code ${errorcode}`)
  problem.response = {
    status: errorcode,
    data: {
      [`${command.toLowerCase()}response`]: { errorcode, cserrorcode: 9999, errortext }
    }
  }
  return problem
}
