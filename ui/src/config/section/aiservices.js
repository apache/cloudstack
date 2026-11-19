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

// The AI Services section (SPEC.md section 9).
//
// Declarative on purpose. CloudStack builds list views, detail views,
// search, pagination, async job notifications and role-based action
// visibility from this config, so a screen described here inherits all
// of it and behaves like every other section — which is the whole
// reason for not hand-rolling the screens.
//
// Tenant screens come first: milestone 6 is "tenant first", and a
// tenant landing on an admin list they cannot read is a bad first
// impression of the product.
//
// Every `api` below must exist in aisvc/api/commands.py; a unit test
// holds the two together, because a renamed command would otherwise
// leave a button that fails with no sign of why.

import { defineAsyncComponent, shallowRef } from 'vue'
import store from '@/store'

const role = () => store.getters.userInfo.roletype
const isAdmin = () => role() === 'Admin'
const isAdminOrDomainAdmin = () => ['Admin', 'DomainAdmin'].includes(role())

const view = (name) =>
  shallowRef(defineAsyncComponent(() => import(`@/views/aiservices/${name}.vue`)))

const detailsTab = {
  name: 'details',
  component: shallowRef(
    defineAsyncComponent(() => import('@/components/view/DetailsTab.vue'))
  )
}

export default {
  name: 'aiservices',
  title: 'label.aiservices',
  icon: 'experiment-outlined',
  // Visible to everyone with a role: a tenant sees their catalog and
  // endpoints, an admin sees those plus the operator screens.
  show: () => ['Admin', 'DomainAdmin', 'User'].includes(role()),
  children: [
    // -- tenant ---------------------------------------------------------
    {
      name: 'aimodelcatalog',
      title: 'label.ai.model.catalog',
      icon: 'appstore-outlined',
      permission: ['listAiModels'],
      resourceType: 'AiModel',
      columns: () => [
        'name', { field: 'task', customTitle: 'ai.task' }, 'size',
        { field: 'license', customTitle: 'ai.license' },
        { field: 'pricetier', customTitle: 'ai.pricetier' }
      ],
      details: () => [
        'id', 'name', { field: 'task', customTitle: 'ai.task' }, 'size',
        { field: 'license', customTitle: 'ai.license' },
        { field: 'pricetier', customTitle: 'ai.pricetier' },
        { field: 'contextlength', customTitle: 'ai.contextlength' },
        'description'
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'deployAiModel',
          icon: 'rocket-outlined',
          label: 'label.ai.deploy.model',
          dataView: true,
          popup: true,
          args: ['zoneid', 'name', 'replicas', 'servingprofile']
        }
      ]
    },
    {
      name: 'aiendpoint',
      title: 'label.ai.endpoints',
      icon: 'api-outlined',
      permission: ['listAiEndpoints'],
      resourceType: 'AiEndpoint',
      columns: () => {
        const fields = [
          // Keyed `name` and rendered from the model, because
          // AiEndpointResponse has no `name` of its own: a bare
          // 'name' column drew blank, and ListView links only a
          // column called `name`, so the detail view — with its API
          // keys tab, its snippets tab and three actions — had no
          // route to it at all.
          { field: 'name', customTitle: 'ai.model', name: (record) => record.model },
          'state',
          { field: 'replicas', customTitle: 'ai.replicas' },
          { field: 'baseurl', customTitle: 'ai.baseurl' }
        ]
        // Only an operator benefits from the owner column; for a tenant
        // every row is theirs and it is a wasted column.
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => [
        'id', 'name', { field: 'model', customTitle: 'ai.model' }, 'state',
        { field: 'replicas', customTitle: 'ai.replicas' },
        { field: 'baseurl', customTitle: 'ai.baseurl' },
        { field: 'pricetier', customTitle: 'ai.pricetier' },
        'account', 'domainpath', 'created'
      ],
      tabs: [
        detailsTab,
        {
          // A tab's name is the key *without* `label.`: CloudStack
          // renders it as $t('label.' + tab.name), so a name carrying
          // the prefix came out as "label.label.ai.api.keys" on the
          // screen. The shared `detailsTab` is named plain `details`,
          // which is why that one tab always read correctly.
          name: 'ai.api.keys',
          component: view('ApiKeysTab')
        },
        {
          name: 'ai.snippets',
          component: view('SnippetsTab')
        }
      ],
      actions: [
        {
          api: 'updateAiModelDeployment',
          icon: 'edit-outlined',
          label: 'label.ai.update.deployment',
          dataView: true,
          popup: true,
          args: ['replicas', 'servingprofile']
        },
        {
          api: 'deleteAiModelDeployment',
          icon: 'delete-outlined',
          label: 'label.ai.delete.deployment',
          dataView: true,
          // CloudStack asks for confirmation; this is not reversible.
          message: 'message.ai.delete.deployment'
        }
      ]
    },
    {
      name: 'aiplayground',
      title: 'label.ai.playground',
      icon: 'message-outlined',
      permission: ['createAiPlaygroundSession'],
      component: view('Playground'),
      columns: () => [{ field: 'model', customTitle: 'ai.model' }],
      details: () => [{ field: 'model', customTitle: 'ai.model' }],
      actions: []
    },
    {
      name: 'aigenerationprofile',
      title: 'label.ai.generation.profiles',
      icon: 'sliders-outlined',
      permission: ['listAiGenerationProfiles'],
      resourceType: 'AiGenerationProfile',
      columns: () => {
        const fields = ['name', 'scope', 'policy']
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => ['id', 'name', 'description', 'scope', 'policy', 'account'],
      tabs: [detailsTab],
      actions: [
        {
          api: 'createAiGenerationProfile',
          icon: 'plus-outlined',
          label: 'label.ai.create.generation.profile',
          listView: true,
          popup: true,
          args: ['name', 'params']
        },
        {
          api: 'updateAiGenerationProfile',
          icon: 'edit-outlined',
          label: 'label.ai.update.generation.profile',
          dataView: true,
          popup: true,
          args: ['params']
        },
        {
          api: 'assignAiTuningProfile',
          icon: 'link-outlined',
          label: 'label.ai.assign.profile',
          dataView: true,
          popup: true,
          args: ['profile', 'scope', 'target']
        },
        {
          api: 'deleteAiGenerationProfile',
          icon: 'delete-outlined',
          label: 'label.ai.delete.generation.profile',
          dataView: true,
          message: 'message.ai.delete.generation.profile'
        }
      ]
    },
    {
      name: 'aieffectiveparameters',
      title: 'label.ai.effective.parameters',
      icon: 'function-outlined',
      permission: ['getAiEffectiveParameters'],
      // A resolved value is meaningless without the level it came from
      // (SPEC.md section 6), and a plain detail view cannot show that
      // pairing, so this screen is its own component.
      component: view('EffectiveParameters'),
      columns: () => [{ field: 'parameter', customTitle: 'ai.parameter' }, 'value', 'source'],
      details: () => [
        { field: 'parameter', customTitle: 'ai.parameter' }, 'value', 'source', 'policy'
      ],
      actions: []
    },
    {
      name: 'aiusage',
      title: 'label.ai.usage.budgets',
      icon: 'dollar-outlined',
      permission: ['listAiUsage'],
      resourceType: 'AiUsage',
      columns: () => {
        const fields = [
          // Keyed `name` so CloudStack links the row: ListView
          // renders the way into a detail view only on a column
          // called `name`, and `updateAiBudget` lives there.
          { field: 'name', customTitle: 'ai.model', name: (record) => record.model },
          { field: 'inputtokens', customTitle: 'ai.inputtokens' },
          { field: 'outputtokens', customTitle: 'ai.outputtokens' },
          { field: 'gpuhours', customTitle: 'ai.gpuhours' },
          { field: 'cost', customTitle: 'ai.cost' }
        ]
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => [
        'account', { field: 'model', customTitle: 'ai.model' },
        { field: 'inputtokens', customTitle: 'ai.inputtokens' },
        { field: 'outputtokens', customTitle: 'ai.outputtokens' },
        { field: 'gpuhours', customTitle: 'ai.gpuhours' },
        { field: 'cost', customTitle: 'ai.cost' },
        'currency', { field: 'recorded', customTitle: 'ai.recorded' }
      ],
      tabs: [
        detailsTab,
        {
          name: 'ai.budgets',
          component: view('BudgetsTab')
        }
      ],
      actions: [
        {
          api: 'createAiBudget',
          icon: 'plus-outlined',
          label: 'label.ai.create.budget',
          listView: true,
          popup: true,
          // Admin only: a tenant able to set its own budget could lift
          // the limit that is blocking it.
          show: isAdminOrDomainAdmin,
          args: ['account', 'domain', 'monthly', 'currency', 'onexceed']
        },
        {
          api: 'updateAiBudget',
          icon: 'edit-outlined',
          label: 'label.ai.update.budget',
          dataView: true,
          popup: true,
          show: isAdminOrDomainAdmin,
          args: ['monthly', 'currency', 'onexceed']
        }
      ]
    },

    // -- tenant, v2 -----------------------------------------------------
    //
    // These need no `show` of their own. CloudStack drops a route whose
    // `permission` is missing from the account's listApis answer, and
    // the plugin only registers the v2 commands when
    // ai.services.v2.enabled is on — so on a v1 install the commands
    // are absent, the routes are dropped, and these screens are not
    // there. A second switch here could only disagree with that one.
    {
      name: 'aiadapter',
      title: 'label.ai.adapters',
      icon: 'deployment-unit-outlined',
      permission: ['listAiAdapters'],
      resourceType: 'AiAdapter',
      columns: () => {
        // `servedmodelname` is what a tenant actually puts in a
        // request — `<base>:<adapter>` — so it earns a column over the
        // adapter's own name, which addresses nothing.
        const fields = [
          'name', { field: 'basemodel', customTitle: 'ai.basemodel' },
          { field: 'servedmodelname', customTitle: 'ai.servedmodelname' },
          { field: 'rank', customTitle: 'ai.rank' }
        ]
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => [
        'id', 'name', { field: 'basemodel', customTitle: 'ai.basemodel' },
        { field: 'servedmodelname', customTitle: 'ai.servedmodelname' },
        { field: 'rank', customTitle: 'ai.rank' },
        { field: 'sizebytes', customTitle: 'ai.sizebytes' },
        'shared', 'account', 'created'
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'uploadAiAdapter',
          icon: 'upload-outlined',
          label: 'label.ai.upload.adapter',
          listView: true,
          popup: true,
          args: ['name', 'basemodel', 'rank', 'sizebytes', 'files', 'uri']
        },
        {
          api: 'deleteAiAdapter',
          icon: 'delete-outlined',
          label: 'label.ai.delete.adapter',
          dataView: true,
          message: 'message.ai.delete.adapter'
        }
      ]
    },
    {
      name: 'aibatch',
      title: 'label.ai.batches',
      icon: 'unordered-list-outlined',
      permission: ['listAiBatches'],
      resourceType: 'AiBatch',
      columns: () => {
        const fields = [
          { field: 'name', customTitle: 'ai.batch', name: (record) => record.id },
          { field: 'model', customTitle: 'ai.model' }, 'status',
          { field: 'requestcount', customTitle: 'ai.requestcount' }, 'created'
        ]
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => [
        'id', { field: 'model', customTitle: 'ai.model' }, 'status',
        { field: 'requestcount', customTitle: 'ai.requestcount' }, 'account', 'created',
        { field: 'finished', customTitle: 'ai.finished' },
        { field: 'inputuri', customTitle: 'ai.inputuri' },
        { field: 'outputuri', customTitle: 'ai.outputuri' }
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'createAiBatch',
          icon: 'plus-outlined',
          label: 'label.ai.create.batch',
          listView: true,
          popup: true,
          args: ['model', 'input', 'inputuri']
        },
        {
          api: 'getAiBatchResults',
          icon: 'download-outlined',
          label: 'label.ai.batch.results',
          dataView: true
        },
        {
          api: 'cancelAiBatch',
          icon: 'stop-outlined',
          label: 'label.ai.cancel.batch',
          dataView: true,
          message: 'message.ai.cancel.batch'
        }
      ]
    },
    {
      name: 'aicertificate',
      title: 'label.ai.certificates',
      icon: 'safety-certificate-outlined',
      permission: ['listAiCertificates'],
      resourceType: 'AiCertificate',
      // Metadata only: the engine never returns the certificate body
      // and never the key, so there is nothing here to put on a screen
      // that should not be on one.
      columns: () => {
        const fields = [
          {
            field: 'name',
            customTitle: 'ai.customdomain',
            name: (record) => record.customdomain
          },
          { field: 'expires', customTitle: 'ai.expires' }, 'fingerprint'
        ]
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => [
        'id', { field: 'customdomain', customTitle: 'ai.customdomain' }, 'fingerprint',
        { field: 'serial', customTitle: 'ai.serial' },
        { field: 'expires', customTitle: 'ai.expires' }, 'account',
        'created'
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'uploadAiCertificate',
          icon: 'upload-outlined',
          label: 'label.ai.upload.certificate',
          listView: true,
          popup: true,
          args: ['customdomain', 'certificate', 'privatekey']
        },
        {
          api: 'replaceAiCertificate',
          icon: 'reload-outlined',
          label: 'label.ai.replace.certificate',
          dataView: true,
          popup: true,
          // Replacing rebuilds the proxy, which is downtime: a proxy
          // fetches its material once, at boot (PROGRESS.md, m9).
          message: 'message.ai.replace.certificate',
          args: ['certificate', 'privatekey']
        },
        {
          api: 'deleteAiCertificate',
          icon: 'delete-outlined',
          label: 'label.ai.delete.certificate',
          dataView: true,
          message: 'message.ai.delete.certificate'
        }
      ]
    },
    {
      name: 'aiprivateendpoint',
      title: 'label.ai.private.endpoints',
      icon: 'lock-outlined',
      permission: ['listAiPrivateEndpoints'],
      resourceType: 'AiPrivateEndpoint',
      columns: () => {
        // `exposure` is the phrase the engine builds — "private on
        // net-1 as ai.acme.example". It is the one column a buyer
        // checking that an endpoint really is private will read.
        const fields = [
          {
            field: 'name',
            customTitle: 'ai.deployment',
            name: (record) => record.deployment
          },
          { field: 'exposure', customTitle: 'ai.exposure' },
          { field: 'customdomain', customTitle: 'ai.customdomain' },
          { field: 'tls', customTitle: 'ai.tls' }
        ]
        if (isAdminOrDomainAdmin()) fields.push('account')
        return fields
      },
      details: () => [
        'id', { field: 'deployment', customTitle: 'ai.deployment' },
        { field: 'exposure', customTitle: 'ai.exposure' }, 'network',
        { field: 'publicenabled', customTitle: 'ai.publicenabled' },
        { field: 'customdomain', customTitle: 'ai.customdomain' },
        { field: 'tls', customTitle: 'ai.tls' }, 'certificate', 'account', 'created'
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'createAiPrivateEndpoint',
          icon: 'plus-outlined',
          label: 'label.ai.create.private.endpoint',
          listView: true,
          popup: true,
          args: [
            'deployment', 'networkid', 'publicenabled', 'customdomain',
            'tls', 'certificateid'
          ]
        },
        {
          api: 'deleteAiPrivateEndpoint',
          icon: 'delete-outlined',
          label: 'label.ai.delete.private.endpoint',
          dataView: true,
          message: 'message.ai.delete.private.endpoint'
        }
      ]
    },
    {
      name: 'aiguardrail',
      title: 'label.ai.guardrails',
      icon: 'safety-outlined',
      permission: ['listAiGuardrailProfiles'],
      // No `show`: the engine scopes these now, so a tenant sees the
      // platform profile screening them and may tighten their own
      // account. What they may not do is written where it belongs —
      // in aisvc/guardrails/authority.py — and refused there, with a
      // message saying what they can do instead.
      resourceType: 'AiGuardrailProfile',
      columns: () => ['name', 'scope', { field: 'target', customTitle: 'ai.target' }],
      details: () => [
        'id', 'name', 'scope', { field: 'target', customTitle: 'ai.target' }
      ],
      // The checks are a tab, not a detail row: they are a named list,
      // and DetailsTab prints a value rather than rendering one, so a
      // list there reads "[object Object]".
      tabs: [
        detailsTab,
        {
          name: 'ai.checks',
          component: view('GuardrailChecksTab')
        }
      ],
      actions: [
        {
          api: 'createAiGuardrailProfile',
          icon: 'plus-outlined',
          label: 'label.ai.create.guardrail.profile',
          listView: true,
          popup: true,
          args: ['name', 'scope', 'target', 'preset']
        },
        {
          api: 'updateAiGuardrailProfile',
          icon: 'edit-outlined',
          label: 'label.ai.update.guardrail.profile',
          dataView: true,
          popup: true,
          args: ['target']
        },
        {
          api: 'deleteAiGuardrailProfile',
          icon: 'delete-outlined',
          label: 'label.ai.delete.guardrail.profile',
          dataView: true,
          message: 'message.ai.delete.guardrail.profile'
        }
      ]
    },

    // -- admin ----------------------------------------------------------
    {
      name: 'aisetup',
      title: 'label.ai.setup',
      icon: 'tool-outlined',
      permission: ['listAiSetupChecks'],
      show: isAdmin,
      // A wizard is a sequence, not a list of rows.
      component: view('SetupWizard'),
      columns: () => ['name', 'status', { field: 'detail', customTitle: 'ai.detail' }],
      details: () => [
        'name', 'status', { field: 'detail', customTitle: 'ai.detail' },
        { field: 'remedy', customTitle: 'ai.remedy' }
      ],
      actions: [
        {
          api: 'runAiSetupSelfTest',
          icon: 'experiment-outlined',
          label: 'label.ai.run.self.test',
          listView: true,
          popup: true,
          args: ['zoneid']
        },
        {
          api: 'discoverAiGpuClasses',
          icon: 'search-outlined',
          label: 'label.ai.discover.gpu.classes',
          listView: true
        }
      ]
    },
    {
      name: 'aihuggingface',
      title: 'label.ai.huggingface',
      icon: 'cloud-download-outlined',
      permission: ['searchAiHfModels'],
      show: isAdminOrDomainAdmin,
      component: view('HuggingFaceBrowser'),
      columns: () => [
        { field: 'name', customTitle: 'ai.repo', name: (record) => record.repo },
        { field: 'task', customTitle: 'ai.task' },
        { field: 'license', customTitle: 'ai.license' },
        { field: 'downloads', customTitle: 'ai.downloads' },
        { field: 'compatible', customTitle: 'ai.compatible' }
      ],
      details: () => [
        { field: 'repo', customTitle: 'ai.repo' },
        { field: 'revision', customTitle: 'ai.revision' },
        { field: 'task', customTitle: 'ai.task' },
        { field: 'license', customTitle: 'ai.license' }, 'params',
        { field: 'problems', customTitle: 'ai.problems' }
      ],
      actions: [
        {
          api: 'checkAiHfModel',
          icon: 'safety-certificate-outlined',
          label: 'label.ai.check.compatibility',
          dataView: true,
          popup: true,
          args: ['repoid', 'revision']
        },
        {
          api: 'mirrorAiModel',
          icon: 'cloud-download-outlined',
          label: 'label.ai.mirror.model',
          dataView: true,
          popup: true,
          args: ['repoid', 'revision']
        },
        {
          api: 'approveAiModel',
          icon: 'check-outlined',
          label: 'label.ai.approve.model',
          dataView: true,
          show: isAdmin,
          message: 'message.ai.approve.model'
        }
      ]
    },
    {
      name: 'airegistry',
      title: 'label.ai.registry',
      icon: 'database-outlined',
      permission: ['listAiModels'],
      show: isAdminOrDomainAdmin,
      resourceType: 'AiModel',
      columns: () => [
        'name', { field: 'repo', customTitle: 'ai.repo' },
        { field: 'revision', customTitle: 'ai.revision' },
        { field: 'license', customTitle: 'ai.license' }, 'sizegb',
        { field: 'deployments', customTitle: 'ai.deployments' }
      ],
      details: () => [
        'id', 'name', { field: 'repo', customTitle: 'ai.repo' },
        { field: 'revision', customTitle: 'ai.revision' },
        { field: 'sha256', customTitle: 'ai.sha256' },
        { field: 'license', customTitle: 'ai.license' }, 'sizegb',
        { field: 'task', customTitle: 'ai.task' },
        { field: 'published', customTitle: 'ai.published' },
        { field: 'deployments', customTitle: 'ai.deployments' }
      ],
      tabs: [
        detailsTab,
        {
          // v2. The tab is always declared; `runAiModelEvaluation` and
          // `listAiEvaluations` are not registered on a v1 install, so
          // the action is hidden by CloudStack and the tab reports
          // nothing to show.
          name: 'ai.evaluations',
          component: view('EvaluationsTab')
        }
      ],
      actions: [
        {
          api: 'publishAiModel',
          icon: 'global-outlined',
          label: 'label.ai.publish.model',
          dataView: true,
          popup: true,
          args: ['pricetier', 'visibility']
        },
        {
          api: 'runAiModelEvaluation',
          icon: 'experiment-outlined',
          label: 'label.ai.run.evaluation',
          dataView: true,
          popup: true,
          // Root admin: the gate exists to stop a bad revision being
          // published, so the person who runs it is the one who can
          // override it (SPEC.md section 8).
          show: isAdmin,
          args: ['model', 'revision']
        }
      ]
    },
    {
      name: 'aicapacity',
      title: 'label.ai.gpu.capacity',
      icon: 'hdd-outlined',
      permission: ['listAiGpuCapacity'],
      show: isAdmin,
      // SPEC.md section 9 asks for a heatmap by zone and class, which
      // is not a table.
      component: view('GpuCapacity'),
      columns: () => [
        'zone', { field: 'gpuclass', customTitle: 'ai.gpuclass' }, 'total', 'allocated', 'free',
        { field: 'unhealthy', customTitle: 'ai.unhealthy' }
      ],
      details: () => [
        'zone', { field: 'gpuclass', customTitle: 'ai.gpuclass' },
        { field: 'hosttag', customTitle: 'ai.hosttag' }, 'total', 'allocated', 'free',
        { field: 'unhealthy', customTitle: 'ai.unhealthy' },
        { field: 'nodes', customTitle: 'ai.nodes' }
      ],
      actions: []
    },
    {
      name: 'aifleet',
      title: 'label.ai.fleet.health',
      icon: 'cluster-outlined',
      permission: ['listAiGpuCapacity'],
      show: isAdmin,
      component: view('FleetHealth'),
      columns: () => [
        { field: 'name', customTitle: 'ai.host', name: (record) => record.host },
        'template', 'driver', 'state', { field: 'drift', customTitle: 'ai.drift' }
      ],
      details: () => [
        'host', 'template', 'driver', 'state', { field: 'drift', customTitle: 'ai.drift' },
        { field: 'nodes', customTitle: 'ai.nodes' }
      ],
      actions: [
        {
          api: 'drainAiNode',
          icon: 'logout-outlined',
          label: 'label.ai.drain.node',
          dataView: true,
          popup: true,
          // Destroy, not stop: a stopped node keeps its GPU allocated
          // (ADR-0003), so the confirmation has to say so.
          message: 'message.ai.drain.node',
          args: ['force']
        }
      ]
    },
    {
      name: 'airollout',
      title: 'label.ai.rollouts',
      icon: 'swap-outlined',
      permission: ['listAiModelDeployments'],
      show: isAdminOrDomainAdmin,
      resourceType: 'AiDeployment',
      columns: () => [
        // Same as endpoints: AiDeploymentResponse carries no `name`,
        // so the row had nothing to link and promote/rollback were
        // unreachable.
        { field: 'name', customTitle: 'ai.model', name: (record) => record.model },
        { field: 'livecolour', customTitle: 'ai.livecolour' },
        { field: 'candidate', customTitle: 'ai.candidate' },
        { field: 'weight', customTitle: 'ai.weight' }, 'state'
      ],
      details: () => [
        'id', 'name', { field: 'model', customTitle: 'ai.model' },
        { field: 'livecolour', customTitle: 'ai.livecolour' },
        { field: 'candidate', customTitle: 'ai.candidate' },
        { field: 'weight', customTitle: 'ai.weight' }, 'state',
        { field: 'spechash', customTitle: 'ai.spechash' }
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'promoteAiRollout',
          icon: 'check-circle-outlined',
          label: 'label.ai.promote.rollout',
          dataView: true,
          message: 'message.ai.promote.rollout'
        },
        {
          api: 'rollbackAiRollout',
          icon: 'rollback-outlined',
          label: 'label.ai.rollback.rollout',
          dataView: true,
          message: 'message.ai.rollback.rollout'
        }
      ]
    },
    {
      name: 'aiservingprofile',
      title: 'label.ai.serving.profiles',
      icon: 'control-outlined',
      permission: ['listAiServingProfiles'],
      // Serving profiles size vLLM against the hardware; a tenant
      // changing one could take a GPU host down (SPEC.md section 7).
      show: isAdmin,
      resourceType: 'AiServingProfile',
      columns: () => [
        'name', { field: 'maxnumseqs', customTitle: 'ai.maxnumseqs' },
        { field: 'gpumemoryutilization', customTitle: 'ai.gpumemoryutilization' },
        { field: 'maxmodellen', customTitle: 'ai.maxmodellen' }
      ],
      details: () => [
        'id', 'name', 'description', { field: 'maxnumseqs', customTitle: 'ai.maxnumseqs' },
        { field: 'gpumemoryutilization', customTitle: 'ai.gpumemoryutilization' },
        { field: 'maxmodellen', customTitle: 'ai.maxmodellen' },
        { field: 'tensorparallel', customTitle: 'ai.tensorparallel' }
      ],
      tabs: [detailsTab],
      actions: [
        {
          api: 'createAiServingProfile',
          icon: 'plus-outlined',
          label: 'label.ai.create.serving.profile',
          listView: true,
          popup: true,
          args: ['name', 'params']
        },
        {
          api: 'updateAiServingProfile',
          icon: 'edit-outlined',
          label: 'label.ai.update.serving.profile',
          dataView: true,
          popup: true,
          args: ['params']
        },
        {
          api: 'deleteAiServingProfile',
          icon: 'delete-outlined',
          label: 'label.ai.delete.serving.profile',
          dataView: true,
          message: 'message.ai.delete.serving.profile'
        }
      ]
    },
    {
      name: 'aiactivity',
      title: 'label.ai.activity',
      icon: 'history-outlined',
      permission: ['listAiReconcilerRuns'],
      show: isAdminOrDomainAdmin,
      resourceType: 'AiReconcilerRun',
      columns: () => [
        'id', 'zone', 'status', 'actions', 'started',
        { field: 'finished', customTitle: 'ai.finished' }
      ],
      details: () => [
        'id', 'zone', 'status', 'actions', 'started',
        { field: 'finished', customTitle: 'ai.finished' }, 'error'
      ],
      tabs: [
        detailsTab,
        {
          name: 'ai.run.actions',
          component: view('RunActionsTab')
        }
      ],
      actions: []
    }
  ]
}
