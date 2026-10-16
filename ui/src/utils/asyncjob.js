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

// Mirrors org.apache.cloudstack.jobs.JobInfo.Status ordinals.
export const JOB_STATUS = {
  IN_PROGRESS: 0,
  SUCCEEDED: 1,
  FAILED: 2,
  CANCELLED: 3,
  // display only: an in-progress job whose cancellation was requested (API: jobstatus 0 + cancelrequested)
  CANCELLING: 'cancelling'
}

export function jobStatusLabel (status) {
  switch (status) {
    case JOB_STATUS.IN_PROGRESS: return 'label.pending'
    case JOB_STATUS.SUCCEEDED: return 'label.success'
    case JOB_STATUS.FAILED: return 'label.failed'
    case JOB_STATUS.CANCELLED: return 'label.cancelled'
    case JOB_STATUS.CANCELLING: return 'label.cancelling'
    default: return String(status)
  }
}

export function jobStatusColor (status) {
  switch (status) {
    case JOB_STATUS.IN_PROGRESS: return 'blue'
    case JOB_STATUS.SUCCEEDED: return 'green'
    case JOB_STATUS.FAILED: return 'red'
    case JOB_STATUS.CANCELLED: return 'orange'
    case JOB_STATUS.CANCELLING: return 'gold'
    default: return 'default'
  }
}

// A job stays in progress while its cancellation runs and carries a flag; the UI shows that as its own state.
export function jobDisplayStatus (job) {
  if (job && job.cancelrequested && job.jobstatus === JOB_STATUS.IN_PROGRESS) {
    return JOB_STATUS.CANCELLING
  }
  return job ? job.jobstatus : undefined
}

export function jobResultText (result) {
  if (!result) {
    return '-'
  }
  if (typeof result === 'object') {
    return result.errortext || JSON.stringify(result)
  }
  return String(result)
}

export function jobResultPretty (result) {
  if (result && typeof result === 'object' && !result.errortext) {
    return JSON.stringify(result, null, 2)
  }
  return jobResultText(result)
}

export function jobResourcePath (resourceType, resourceId) {
  if (!resourceId) {
    return null
  }
  switch (resourceType) {
    case 'VirtualMachine': return '/vm/' + resourceId
    case 'Volume': return '/volume/' + resourceId
    case 'Snapshot': return '/snapshot/' + resourceId
    case 'VmSnapshot': return '/vmsnapshot/' + resourceId
    case 'Template': return '/template/' + resourceId
    case 'Iso': return '/iso/' + resourceId
    case 'Network': return '/guestnetwork/' + resourceId
    case 'Host': return '/host/' + resourceId
    default: return null
  }
}

// The Cancel action as the list rows and the detail page show it: always present for admins, disabled once the job is done.
export const cancelJobAction = {
  api: 'cancelAsyncJob',
  icon: 'close-circle-outlined',
  danger: true,
  label: 'label.cancel.job',
  message: 'message.confirm.cancel.job',
  dataView: true,
  args: ['jobid'],
  mapping: {
    jobid: {
      value: (record) => { return record.jobid }
    }
  },
  show: (record, store) => {
    return ['Admin'].includes(store.userInfo.roletype)
  },
  disabled: (record) => {
    return record.jobstatus !== JOB_STATUS.IN_PROGRESS || record.cancelrequested === true
  }
}
