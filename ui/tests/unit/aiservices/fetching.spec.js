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
// CloudStack's ui/ has a real @/api; these specs need the recording
// double instead, and Jest wires that per file rather than by alias.
jest.mock('@/api', () => require('../../doubles/api.js'))

import { flushPromises, mount } from '@vue/test-utils'

import EvaluationsTab from '@/views/aiservices/EvaluationsTab.vue'
import FleetHealth from '@/views/aiservices/FleetHealth.vue'
import GpuCapacity from '@/views/aiservices/GpuCapacity.vue'
import HuggingFaceBrowser from '@/views/aiservices/HuggingFaceBrowser.vue'
import ApiKeysTab from '@/views/aiservices/ApiKeysTab.vue'
import GuardrailChecksTab from '@/views/aiservices/GuardrailChecksTab.vue'
import BudgetsTab from '@/views/aiservices/BudgetsTab.vue'
import EffectiveParameters from '@/views/aiservices/EffectiveParameters.vue'
import Playground from '@/views/aiservices/Playground.vue'
import RunActionsTab from '@/views/aiservices/RunActionsTab.vue'
import SetupWizard from '@/views/aiservices/SetupWizard.vue'
import { apiCalls, refusal, reset } from '../../doubles/api.js'

// CloudStack renders a section's `component` as the route view and
// passes it nothing. A screen that only accepts props therefore renders
// empty in the real UI however well it tests — so each one has to fetch
// for itself.

describe('route screens fetch their own data', () => {
  beforeEach(() => reset())

  it('the setup wizard asks for its checks', async () => {
    mount(SetupWizard)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiSetupChecks')
  })

  it('the playground asks which models it may use', async () => {
    // It was the one route screen that fetched nothing: it took a
    // `session` prop CloudStack never passes, so it rendered "Open a
    // session to try this endpoint." for ever and no request for
    // createAiPlaygroundSession was ever made.
    mount(Playground)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiModels')
  })

  it('the playground does not mint a session on load', async () => {
    // A session key is a credential. Minting one just because somebody
    // opened the screen issues a key nobody asked for, and the fifteen
    // minute expiry starts running against a tenant who may be reading
    // the page rather than using it.
    mount(Playground)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).not.toContain('createAiPlaygroundSession')
  })

  it('the playground opens a session for the model it was asked for', async () => {
    reset({
      listAiModels: { listaimodelsresponse: { aimodel: [{ name: 'llama-3.3-70b-instruct' }] } }
    })
    const wrapper = mount(Playground)
    await flushPromises()

    await wrapper.find('[data-test="open-session"]').trigger('click')
    await flushPromises()

    const opened = apiCalls().find((c) => c.command === 'createAiPlaygroundSession')
    expect(opened).toBeTruthy()
    expect(opened.params.model).toBe('llama-3.3-70b-instruct')
  })

  it('the heatmap asks for capacity', async () => {
    mount(GpuCapacity)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiGpuCapacity')
  })

  it('fleet health asks for capacity too', async () => {
    mount(FleetHealth)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiGpuCapacity')
  })

  it('the Hugging Face browser does not search until asked', async () => {
    // Searching on mount would hit the Hub for every operator who
    // opened the screen, and the results would be meaningless.
    mount(HuggingFaceBrowser)
    await flushPromises()

    expect(apiCalls()).toHaveLength(0)
  })

  it('the Hugging Face browser searches when asked', async () => {
    // The pair of the test above, and the one that was missing: a
    // screen that never searches also passes "does not search until
    // asked", so that assertion on its own documented a browser that
    // could not browse.
    const wrapper = mount(HuggingFaceBrowser)
    await wrapper.find('[data-test="query"]').setValue('llama')
    await wrapper.find('[data-test="search"]').trigger('click')
    await flushPromises()

    const search = apiCalls().find((c) => c.command === 'searchAiHfModels')
    expect(search).toBeTruthy()
    expect(search.params.query).toBe('llama')
  })

  it('effective parameters asks which models it may resolve for', async () => {
    mount(EffectiveParameters)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiModels')
  })

  it('effective parameters resolves for the model it was asked about', async () => {
    reset({
      listAiModels: { listaimodelsresponse: { aimodel: [{ name: 'llama-3.3-70b-instruct' }] } }
    })
    const wrapper = mount(EffectiveParameters)
    await flushPromises()

    await wrapper.find('[data-test="resolve"]').trigger('click')
    await flushPromises()

    const asked = apiCalls().find((c) => c.command === 'getAiEffectiveParameters')
    expect(asked).toBeTruthy()
    expect(asked.params.model).toBe('llama-3.3-70b-instruct')
  })

  it('the api keys tab asks for the keys', async () => {
    // A tab is handed `resource`, never a `keys` array, so one that
    // only accepts props shows an empty table whatever the account has.
    mount(ApiKeysTab)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiApiKeys')
  })

  it('the budgets tab asks for the budgets', async () => {
    mount(BudgetsTab)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).toContain('listAiBudgets')
  })

  it('the activity tab fetches nothing, because its run carries its actions', async () => {
    // This used to assert the opposite, and the opposite was the bug:
    // the tab fetched the whole run list and then read action fields —
    // kind, outcome, resourcetype — off each *run*, so every cell was
    // empty. A tab is handed the row the operator opened, and a run
    // carries its actions as a nested list.
    mount(RunActionsTab, { props: { resource: { id: 'run-1', actions: [] } } })
    await flushPromises()

    expect(apiCalls()).toHaveLength(0)
  })

  it('a screen given data does not fetch', async () => {
    // Which is how the rendering tests stay honest: they render what
    // they were given, not whatever a double answered.
    mount(SetupWizard, { props: { checks: [] } })
    await flushPromises()

    expect(apiCalls()).toHaveLength(0)
  })

  it('renders what came back', async () => {
    reset({
      // Keyed by command; the value is the envelope CloudStack returns —
      // and the list sits under the name CloudStack serialises the
      // response as, 'aisetupcheck'. This fixture used to say 'checks',
      // which is the key the *Java* reads from the engine, so it agreed
      // with the screen's bug instead of with the server.
      listAiSetupChecks: {
        listaisetupchecksresponse: {
          aisetupcheck: [
            { name: 'model registry', status: 'fail', detail: 'x', remedy: 'Fix it.' }
          ]
        }
      }
    })

    const wrapper = mount(SetupWizard)
    await flushPromises()

    expect(wrapper.text()).toContain('Fix it.')
  })

  it('an API failure leaves the screen empty rather than broken', async () => {
    reset({})
    const wrapper = mount(GpuCapacity)
    await flushPromises()

    expect(wrapper.findAll('[data-test="cell"]')).toHaveLength(0)
  })

  it('a screen that could not load says so', async () => {
    // Not the same as having nothing to show. listAiGpuCapacity
    // answers 534 until capacity has been observed, and the screen
    // drew a blank page — an operator could not tell a fresh install
    // from a broken one.
    reset({ listAiGpuCapacity: new Error('capacity has not been observed yet') })
    const wrapper = mount(GpuCapacity)
    await flushPromises()

    expect(wrapper.find('[data-test="load-failed"]').exists()).toBe(true)
  })

  it('a screen that could not load repeats the engine\'s own sentence', async () => {
    // Not "Request failed with status code 534", which tells an
    // operator nothing they can act on. CloudStack wraps a refusal in
    // a key named after the command, so the sentence sits at
    // data.listaigpucapacityresponse.errortext — one level deeper than
    // the screen used to look, which is why it always fell through to
    // axios's own message.
    reset({
      listAiGpuCapacity: refusal(
        'listAiGpuCapacity', 'capacity has not been observed yet', 534)
    })
    const wrapper = mount(GpuCapacity)
    await flushPromises()

    const said = wrapper.find('[data-test="load-failed"]').text()
    expect(said).toContain('capacity has not been observed yet')
    expect(said).not.toContain('status code')
  })

  it('a screen with nothing to show says that instead', async () => {
    reset({ listAiGpuCapacity: { listaigpucapacityresponse: { aigpucapacity: [] } } })
    const wrapper = mount(GpuCapacity)
    await flushPromises()

    expect(wrapper.find('[data-test="load-failed"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="empty"]').exists()).toBe(true)
  })
})

describe('the evaluations tab', () => {
  // Evaluations cannot be a screen of their own: listAiEvaluations is
  // scoped to a model (GET /v1/models/{model_name}/evaluations), so
  // there is no model-less list to draw. It is a tab on the registry,
  // where the model is the row you are already looking at.
  beforeEach(() => reset())

  it('asks for the evaluations of the model it was given', async () => {
    mount(EvaluationsTab, { props: { resource: { name: 'llama-3.3-70b-instruct' } } })
    await flushPromises()

    expect(apiCalls()).toEqual([
      { command: 'listAiEvaluations', params: { model: 'llama-3.3-70b-instruct' } }
    ])
  })

  it('asks for nothing until it has a model', async () => {
    // CloudStack mounts a tab before the detail view has loaded its
    // row; firing then would ask for the evaluations of undefined.
    mount(EvaluationsTab, { props: { resource: {} } })
    await flushPromises()

    expect(apiCalls()).toHaveLength(0)
  })

  it('refetches when the row changes', async () => {
    const wrapper = mount(EvaluationsTab, { props: { resource: {} } })
    await wrapper.setProps({ resource: { name: 'qwen-2.5-72b' } })
    await flushPromises()

    expect(apiCalls().map((c) => c.params.model)).toEqual(['qwen-2.5-72b'])
  })

  it('shows the gate result, not just the score', async () => {
    // The number alone does not say whether publishing was blocked,
    // which is the only thing the gate is for (SPEC.md section 8).
    reset({
      listAiEvaluations: {
        listaievaluationsresponse: {
          aievaluation: [
            { id: 'e1', model: 'm', revision: 'abc123', passed: false, overridden: false }
          ]
        }
      }
    })

    const wrapper = mount(EvaluationsTab, { props: { resource: { name: 'm' } } })
    await flushPromises()

    expect(wrapper.text()).toMatch(/blocked/i)
  })

  it('says when a root admin published past the gate', async () => {
    reset({
      listAiEvaluations: {
        listaievaluationsresponse: {
          aievaluation: [
            { id: 'e1', model: 'm', revision: 'abc', passed: false, overridden: true }
          ]
        }
      }
    })

    const wrapper = mount(EvaluationsTab, { props: { resource: { name: 'm' } } })
    await flushPromises()

    expect(wrapper.text()).toMatch(/overridden/i)
  })
})

describe('a screen survives the API failing', () => {
  // Every one of these called the API with no .catch(), so a 4xx or
  // 5xx became an unhandled promise rejection and the screen rendered
  // blank with the reason only in the browser console. listAiGpuCapacity
  // answers 534 until capacity is observed, which made that the normal
  // case on a fresh install rather than an edge one.
  const screens = [
    ['the heatmap', GpuCapacity, 'listAiGpuCapacity'],
    ['fleet health', FleetHealth, 'listAiGpuCapacity'],
    ['the setup wizard', SetupWizard, 'listAiSetupChecks'],
    ['the playground', Playground, 'listAiModels'],
    ['effective parameters', EffectiveParameters, 'listAiModels'],
    ['the api keys tab', ApiKeysTab, 'listAiApiKeys'],
    ['the budgets tab', BudgetsTab, 'listAiBudgets']
  ]

  screens.forEach(([what, component, command]) => {
    it(`${what} handles a refusal`, async () => {
      const rejections = []
      const record = (e) => rejections.push(e)
      process.on('unhandledRejection', record)

      reset({ [command]: new Error('refused') })
      const wrapper = mount(component)
      await flushPromises()
      await new Promise((resolve) => setTimeout(resolve, 0))

      process.off('unhandledRejection', record)
      expect(rejections).toHaveLength(0)
      expect(wrapper.exists()).toBe(true)
    })
  })
})

describe('every screen says why it could not load', () => {
  // A screen that empties its list on a refusal renders exactly like a
  // screen with nothing to show. Only the heatmap told them apart; the
  // other eight set their list to [] in the catch and said nothing, so
  // "could not reach the engine" and "you have no keys yet" were the
  // same blank panel. The reason has to be the engine's own sentence,
  // which is the thing that names a remedy.
  const screens = [
    ['the heatmap', GpuCapacity, 'listAiGpuCapacity'],
    ['fleet health', FleetHealth, 'listAiGpuCapacity'],
    ['the setup wizard', SetupWizard, 'listAiSetupChecks'],
    ['the playground', Playground, 'listAiModels'],
    ['effective parameters', EffectiveParameters, 'listAiModels'],
    ['the api keys tab', ApiKeysTab, 'listAiApiKeys'],
    ['the budgets tab', BudgetsTab, 'listAiBudgets']
  ]

  screens.forEach(([what, component, command]) => {
    it(`${what} repeats the engine's sentence`, async () => {
      reset({ [command]: refusal(command, 'the engine is not configured', 530) })
      const wrapper = mount(component)
      await flushPromises()

      const failed = wrapper.find('[data-test="load-failed"]')
      expect(failed.exists()).toBe(true)
      expect(failed.text()).toContain('the engine is not configured')
      expect(failed.text()).not.toContain('status code')
    })

    it(`${what} does not claim failure when it simply has nothing`, async () => {
      reset({ [command]: {} })
      const wrapper = mount(component)
      await flushPromises()

      expect(wrapper.find('[data-test="load-failed"]').exists()).toBe(false)
    })
  })

  it('the setup wizard does not let you continue past checks it could not read', async () => {
    // canProceed([]) is true, which is right for a zone whose checks
    // all passed and wrong for a wizard that never got any: the button
    // was enabled, so a refused listAiSetupChecks read as a clean
    // preflight. Checks that could not be read are not checks that
    // passed.
    reset({ listAiSetupChecks: refusal('listAiSetupChecks', 'the engine is not configured', 530) })
    const wrapper = mount(SetupWizard)
    await flushPromises()

    expect(wrapper.find('[data-test="proceed"]').attributes('disabled')).toBeDefined()
  })

  it('the hugging face browser repeats the hub refusal', async () => {
    // Its fetch happens on search rather than on mount, so it needs
    // asking before there is anything to fail.
    reset({ searchAiHfModels: refusal('searchAiHfModels', 'the hub could not be reached', 530) })
    const wrapper = mount(HuggingFaceBrowser)
    await wrapper.find('[data-test="query"]').setValue('llama')
    await wrapper.find('[data-test="search"]').trigger('click')
    await flushPromises()

    const failed = wrapper.find('[data-test="load-failed"]')
    expect(failed.exists()).toBe(true)
    expect(failed.text()).toContain('the hub could not be reached')
  })
})

describe('the guardrail checks tab', () => {
  // A profile's checks are the profile. They used to be nowhere: the
  // engine sent them, no Java response declared the field, and
  // CloudStack dropped it — so a domain admin who had bought a locked
  // education-safe profile could open it and see a name, a scope and
  // nothing about what it screens.
  //
  // The engine keys them by name, so the plugin turns that object into
  // a named list and this reads it off `resource`, the way the
  // snippets tab does. A list cannot go in `details`: CloudStack's
  // DetailsTab takes a field and a title and prints the value, which
  // for a list of objects is "[object Object]".
  beforeEach(() => reset())

  it('shows each check with what it does', async () => {
    const wrapper = mount(GuardrailChecksTab, {
      props: {
        resource: {
          name: 'education-safe',
          checks: [
            { name: 'pii', action: 'block', policy: 'locked' },
            { name: 'topics', action: 'block', policy: 'locked' }
          ]
        }
      }
    })

    expect(wrapper.findAll('[data-test="check"]')).toHaveLength(2)
    expect(wrapper.text()).toContain('pii')
    expect(wrapper.text()).toContain('block')
  })

  it('says which checks cannot be relaxed', async () => {
    // The whole reason a school buys the education-safe profile is that
    // an account beneath them cannot loosen it. A tenant looking at the
    // profile screening them needs to see that, not infer it.
    const wrapper = mount(GuardrailChecksTab, {
      props: {
        resource: {
          checks: [
            { name: 'pii', action: 'block', policy: 'locked' },
            { name: 'input_size', action: 'warn', policy: 'default' }
          ]
        }
      }
    })

    const locked = wrapper.findAll('[data-test="check"]').filter((c) => c.text().match(/locked/i))
    expect(locked).toHaveLength(1)
    expect(locked[0].text()).toContain('pii')
  })

  it('says so when a profile screens nothing', async () => {
    const wrapper = mount(GuardrailChecksTab, { props: { resource: { checks: [] } } })

    expect(wrapper.find('[data-test="empty"]').exists()).toBe(true)
  })
})

describe('a tab reads the names CloudStack sends, not the engine\'s', () => {
  // CloudStack serialises a response field under its `api` name, which
  // is lower-cased and has no underscores: the engine's `percent_used`
  // arrives as `percentused`. Reading the engine's spelling in a
  // template is silent — Vue renders an undefined as nothing — so the
  // budgets tab showed an empty percentage, never marked a blocked
  // account, and always said a budget came from platform.yaml.
  beforeEach(() => reset())

  it('the budgets tab shows how much of a budget is used', async () => {
    reset({
      listAiBudgets: {
        listaibudgetsresponse: {
          aibudget: [{
            account: 'acme', monthly: 100, currency: 'USD', spent: 90,
            percentused: 90, blocked: true, onexceed: 'block'
          }]
        }
      }
    })
    const wrapper = mount(BudgetsTab)
    await flushPromises()

    const row = wrapper.find('[data-test="budget"]')
    expect(row.text()).toContain('90')
    expect(row.text()).toContain('stop serving')
  })

  it('the budgets tab marks an account whose spend is being refused', async () => {
    // The one thing a budget does. It was never shown: `blocked` was
    // dropped by the response class entirely.
    reset({
      listAiBudgets: {
        listaibudgetsresponse: {
          aibudget: [{ account: 'acme', blocked: true, percentused: 120, onexceed: 'block' }]
        }
      }
    })
    const wrapper = mount(BudgetsTab)
    await flushPromises()

    expect(wrapper.find('[data-test="budget"]').classes()).toContain('is-blocked')
  })

  it('the activity tab reads the actions of the run it was given', async () => {
    // It iterated the *runs* and read action fields off them, so every
    // cell was empty. A run carries its actions as a nested list, and a
    // tab is handed the row the user opened — that run — not the list.
    const wrapper = mount(RunActionsTab, {
      props: {
        resource: {
          id: 'run-1',
          actions: [
            { kind: 'create', resourcetype: 'deployment', name: 'llama', outcome: 'succeeded' }
          ]
        }
      }
    })
    await flushPromises()

    const rows = wrapper.findAll('[data-test="action"]')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('deployment')
    expect(rows[0].text()).toContain('succeeded')
  })
})

describe('minting an API key', () => {
  // The secret exists for one moment. createAiApiKey returns it, keys are
  // stored as salted hashes, and CloudStack's autogen dialog closed without
  // showing it — so a key made in the UI could never be used. The two hooks
  // that would have shown it live inside $pollJob and fire for an async
  // command only. So the tab mints the key itself, the way the playground
  // mints a session.
  beforeEach(() => reset())

  const created = {
    createaiapikeyresponse: {
      aiapikey: { id: 'k1', name: 'ci', apikey: 'aisvc-0123456789abcdef-secret' }
    }
  }

  it('does not mint a key just because the tab was opened', async () => {
    // A key is a credential. The playground learned this the same way.
    mount(ApiKeysTab)
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).not.toContain('createAiApiKey')
  })

  it('sends the parameter names the command actually takes', async () => {
    // `allowedmodels`, `cidrs` and `expiresat` are what the Terraform
    // provider sent and what the autogen action sent, and CloudStack drops
    // an unknown parameter in silence — so a key restricted to two models
    // and a CIDR was created with neither.
    reset({ createAiApiKey: created })
    const wrapper = mount(ApiKeysTab)
    await flushPromises()

    await wrapper.find('[data-test="new-name"]').setValue('ci')
    await wrapper.find('[data-test="new-models"]').setValue('llama-3.3-70b-instruct')
    await wrapper.find('[data-test="new-cidrs"]').setValue('10.0.0.0/8')
    await wrapper.find('[data-test="create"]').trigger('click')
    await flushPromises()

    const call = apiCalls().find((c) => c.command === 'createAiApiKey')
    expect(call).toBeTruthy()
    expect(Object.keys(call.params).sort()).toEqual(['cidrlist', 'models', 'name'])
    expect(call.params.name).toBe('ci')
  })

  it('shows the secret, and says it is shown once', async () => {
    reset({ createAiApiKey: created })
    const wrapper = mount(ApiKeysTab)
    await flushPromises()

    await wrapper.find('[data-test="new-name"]').setValue('ci')
    await wrapper.find('[data-test="create"]').trigger('click')
    await flushPromises()

    const shown = wrapper.find('[data-test="new-secret"]')
    expect(shown.exists()).toBe(true)
    expect(shown.text()).toContain('aisvc-0123456789abcdef-secret')
    expect(wrapper.text()).toMatch(/shown once/i)
  })

  it('does not keep the secret on screen once it is dismissed', async () => {
    // It gets demonstrated and screenshotted, as the playground does.
    reset({ createAiApiKey: created })
    const wrapper = mount(ApiKeysTab)
    await flushPromises()

    await wrapper.find('[data-test="new-name"]').setValue('ci')
    await wrapper.find('[data-test="create"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-test="dismiss-secret"]').trigger('click')

    expect(wrapper.find('[data-test="new-secret"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('aisvc-0123456789abcdef-secret')
  })

  it('says why a key could not be made', async () => {
    reset({ createAiApiKey: refusal('createAiApiKey', 'that name is already taken', 530) })
    const wrapper = mount(ApiKeysTab)
    await flushPromises()

    await wrapper.find('[data-test="new-name"]').setValue('ci')
    await wrapper.find('[data-test="create"]').trigger('click')
    await flushPromises()

    const failed = wrapper.find('[data-test="create-failed"]')
    expect(failed.exists()).toBe(true)
    expect(failed.text()).toContain('that name is already taken')
    expect(failed.text()).not.toContain('status code')
  })

  it('will not mint a key with no name', async () => {
    reset({ createAiApiKey: created })
    const wrapper = mount(ApiKeysTab)
    await flushPromises()

    await wrapper.find('[data-test="create"]').trigger('click')
    await flushPromises()

    expect(apiCalls().map((c) => c.command)).not.toContain('createAiApiKey')
  })

  it('lists the new key once it exists', async () => {
    reset({ createAiApiKey: created })
    const wrapper = mount(ApiKeysTab)
    await flushPromises()

    await wrapper.find('[data-test="new-name"]').setValue('ci')
    await wrapper.find('[data-test="create"]').trigger('click')
    await flushPromises()

    const reads = apiCalls().filter((c) => c.command === 'listAiApiKeys')
    expect(reads.length).toBeGreaterThan(1)
  })
})
