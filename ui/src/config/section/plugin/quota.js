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

import { shallowRef, defineAsyncComponent } from 'vue'
export default {
  name: 'quota',
  title: 'label.quota',
  icon: 'pie-chart-outlined',
  docHelp: 'plugins/quota.html',
  permission: ['quotaSummary'],
  children: [
    {
      name: 'quotasummary',
      title: 'label.quota.summary',
      icon: 'bars-outlined',
      permission: ['quotaSummary'],
      columns: ['account',
        {
          state: (record) => record.state.toLowerCase()
        },
        {
          quotastate: (record) => record.quotaenabled ? 'Enabled' : 'Disabled'
        }, 'domain', 'currency', 'balance'
      ],
      columnNames: ['account', 'accountstate', 'quotastate', 'domain', 'currency', 'currentbalance'],
      details: ['account', 'domain', 'state', 'currency', 'balance', 'quota', 'startdate', 'enddate'],
      component: shallowRef(() => import('@/views/plugins/quota/QuotaSummary.vue')),
      tabs: [
        {
          name: 'details',
          component: shallowRef(defineAsyncComponent(() => import('@/components/view/DetailsTab.vue')))
        },
        {
          name: 'quota.statement.quota',
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/QuotaUsage.vue')))
        },
        {
          name: 'quota.statement.balance',
          component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/QuotaBalance.vue')))
        }
      ],
      actions: [
        {
          api: 'quotaCredits',
          icon: 'plus-outlined',
          docHelp: 'plugins/quota.html#quota-credits',
          label: 'label.quota.add.credits',
          dataView: true,
          args: ['value', 'min_balance', 'quota_enforce'],
          mapping: {
            account: {
              value: (record) => { return record.account }
            },
            domainid: {
              value: (record) => { return record.domainid }
            }
          }
        }
      ]
    },
    {
      name: 'quotatariff',
      title: 'label.quota.tariff',
      icon: 'credit-card-outlined',
      docHelp: 'plugins/quota.html#quota-tariff',
      permission: ['quotaTariffList'],
      columns: ['usageName', 'usageTypeDescription', 'usageUnit', 'tariffValue', 'tariffActions'],
      details: ['usageName', 'usageTypeDescription', 'usageUnit', 'tariffValue'],
      component: shallowRef(() => import('@/views/plugins/quota/QuotaTariff.vue'))
    },
    {
      name: 'quotaemailtemplate',
      title: 'label.templatetype',
      icon: 'mail-outlined',
      permission: ['quotaEmailTemplateList'],
      columns: ['templatetype', 'templatesubject', 'templatebody'],
      details: ['templatetype', 'templatesubject', 'templatebody'],
      tabs: [{
        name: 'details',
        component: shallowRef(defineAsyncComponent(() => import('@/views/plugins/quota/EmailTemplateDetails.vue')))
      }]
    }
  ]
}
