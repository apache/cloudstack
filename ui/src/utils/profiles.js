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
// Showing resolved generation parameters (SPEC.md section 6).
//
// A resolved value on its own tells a tenant nothing: the useful
// question is always "why is it that, and can I change it". So every
// value is shown with the level it came from and whether it is open to
// them — which is exactly what getAiEffectiveParameters returns.

const LEVELS = {
  platform: 'Platform default',
  model: 'Model default',
  domain: 'Domain',
  account: 'Account',
  deployment: 'Endpoint',
  apikey: 'API key',
  request: 'Request'
}

export function sourceLabel (level) {
  // An unknown level is shown as-is rather than blanked: a new level
  // added to the engine should look odd here, not disappear.
  return LEVELS[level] ?? level
}

export function describeParameter (parameter) {
  const locked = parameter.policy === 'locked'
  return {
    name: parameter.name,
    value: parameter.value,
    source: sourceLabel(parameter.source),
    policy: parameter.policy,
    locked,
    // `clamp` still lets a tenant choose, within the bounds the level
    // above set; only `locked` takes the choice away.
    editable: !locked
  }
}
