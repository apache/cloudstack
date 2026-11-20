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
// The setup wizard's checks (SPEC.md sections 7 and 14).
//
// Section 14's first criterion is that a new zone reaches a passing
// self-test using only the wizard, so the screen has to be usable on a
// zone where most things are not configured yet.

const STATUSES = ['pass', 'warn', 'fail', 'skipped']

export function groupChecks (checks) {
  // Every group exists even when empty, so the template can render them
  // unconditionally instead of guarding each one.
  const grouped = Object.fromEntries(STATUSES.map((status) => [status, []]))
  for (const check of checks) {
    if (grouped[check.status]) grouped[check.status].push(check)
  }
  return grouped
}

/**
 * Whether the operator may move on.
 *
 * Only a failure blocks. A warning is worth saying and not worth
 * stopping for, and `skipped` is the normal state of a zone that has
 * not been observed yet — which is every zone on its first run.
 */
export function canProceed (checks) {
  return !checks.some((check) => check.status === 'fail')
}
