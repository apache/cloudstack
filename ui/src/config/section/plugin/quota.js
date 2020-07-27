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

export default {
  name: 'quota',
  title: 'label.quota',
  icon: 'pie-chart',
  docHelp: 'plugins/quota.html',
  permission: ['quotaSummary'],
  children: [
    {
      name: 'quotasummary',
      title: 'label.summary',
      icon: 'bars',
      permission: ['quotaSummary'],
      columns: ['account', 'domain', 'state', 'currency', 'balance', 'quota'],
      details: ['account', 'domain', 'state', 'currency', 'balance', 'quota', 'startdate', 'enddate'],
      component: () => import('@/views/plugins/quota/QuotaSummary.vue'),
      tabs: [
        {
          name: 'details',
          component: () => import('@/components/view/DetailsTab.vue')
        },
        {
          name: 'quota.statement.quota',
          component: () => import('@/views/plugins/quota/QuotaUsage.vue')
        },
        {
          name: 'quota.statement.balance',
          component: () => import('@/views/plugins/quota/QuotaBalance.vue')
        }
      ],
      actions: [
        {
          api: 'quotaCredits',
          icon: 'plus',
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
      icon: 'credit-card',
      docHelp: 'plugins/quota.html#quota-tariff',
      permission: ['quotaTariffList'],
      columns: ['usageName', 'description', 'usageUnit', 'tariffValue', 'tariffActions'],
      details: ['usageName', 'description', 'usageUnit', 'tariffValue'],
      component: () => import('@/views/plugins/quota/QuotaTariff.vue')
    },
    {
      name: 'quotaemailtemplate',
      title: 'label.templatetype',
      icon: 'mail',
      permission: ['quotaEmailTemplateList'],
      columns: ['templatetype', 'templatesubject', 'templatebody'],
      details: ['templatetype', 'templatesubject', 'templatebody'],
      tabs: [{
        name: 'details',
        component: () => import('@/views/plugins/quota/EmailTemplateDetails.vue')
      }]
    }
  ]
}
