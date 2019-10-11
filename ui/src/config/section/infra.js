import zones from '@/config/section/infra/zones'
import pods from '@/config/section/infra/pods'
import clusters from '@/config/section/infra/clusters'
import hosts from '@/config/section/infra/hosts'
import primaryStorages from '@/config/section/infra/primaryStorages'
import secondaryStorages from '@/config/section/infra/secondaryStorages'
import systemVms from '@/config/section/infra/systemVms'
import routers from '@/config/section/infra/routers'

export default {
  name: 'infra',
  title: 'Infrastructure',
  icon: 'bank',
  permission: [ 'listInfrastructure' ],
  children: [
    zones,
    pods,
    clusters,
    hosts,
    primaryStorages,
    secondaryStorages,
    systemVms,
    routers,
    {
      name: 'cpusocket',
      title: 'CPU Sockets',
      icon: 'api',
      permission: [ 'listHosts' ],
      params: { 'type': 'routing' },
      columns: [ 'hypervisor', 'hosts', 'cpusockets' ]
    },
    {
      name: 'managementserver',
      title: 'Management Servers',
      icon: 'rocket',
      permission: [ 'listManagementServers' ],
      columns: [ 'name', 'state', 'version' ]
    }
  ]
}
