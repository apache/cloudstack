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

// Why a screen could not load, in the engine's own words.
//
// The engine answers in RFC 9457 problem JSON so its caller can explain
// itself, the plugin appends that detail to the status, and CloudStack
// carries it to the browser as `errortext`. All of that is wasted if the
// screen reads the wrong field: axios's own `message` is "Request failed
// with status code 534", which names no remedy at all.
//
// CloudStack wraps every answer in a key named after the command —
// {"listaigpucapacityresponse": {"errorcode": 534, "errortext": "..."}} —
// refusals included, so the sentence is one level deeper than a reader
// expects. The wrapper key is not worth reconstructing from the command
// name: a refusal before dispatch comes back under "errorresponse"
// instead, so the only reliable rule is to look inside whatever single
// wrapper is there.
export function engineReason (problem) {
  const data = problem?.response?.data

  if (data && typeof data === 'object') {
    if (typeof data.errortext === 'string' && data.errortext) return data.errortext

    for (const wrapped of Object.values(data)) {
      const text = wrapped?.errortext
      if (typeof text === 'string' && text) return text
    }
  }

  return problem?.message ?? ''
}
