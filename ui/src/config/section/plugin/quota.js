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
  title: 'Quota',
  icon: 'pie-chart',
  permission: ['quotaSummary'],
  children: [
    {
      name: 'quotasummary',
      title: 'Summary',
      icon: 'bars',
      permission: ['quotaSummary'],
      columns: ['account', 'domain', 'state', 'currency', 'balance', 'quota'],
      details: ['account', 'domain', 'state', 'currency', 'balance', 'quota', 'startdate', 'enddate']
    },
    {
      name: 'quotatariff',
      title: 'Tariff',
      icon: 'credit-card',
      permission: ['quotaTariffList'],
      columns: ['usageName', 'description', 'usageUnit', 'tariffValue'],
      details: ['usageName', 'description', 'usageUnit', 'tariffValue']
    },
    {
      name: 'quotaemailtemplate',
      title: 'Email Template',
      icon: 'mail',
      permission: ['quotaEmailTemplateList'],
      columns: ['templatetype', 'templatesubject', 'templatebody'],
      details: ['templatetype', 'templatesubject', 'templatebody']
    }
  ]
}
