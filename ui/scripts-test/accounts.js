(function(cloudStack) {
  cloudStack.sections.accounts = {    
    title: 'Accounts',
    id: 'accounts',
    sectionSelect: {
      label: 'Select view'
    },
    sections: {
      accounts: {
        type: 'select',
        title: 'Accounts',
        listView: {
          id: 'accounts',
          fields: {
            name: { label: 'Name', editable: true },
            domain: { label: 'Domain' },
            state: { label: 'State' }
          },
          filters: {
            mine: { label: 'My Accounts' },
            all: { label: 'All Accounts' }
          },
          dataProvider: testData.dataProvider.listView('accounts'),
          
          detailView: {
            name: 'Account details',
            viewAll: { path: 'accounts.users', label: 'Users' },
            
            tabs: {
              details: {
                title: 'Details',
                fields: [
                  {
                    name: { label: 'Name' }
                  },
                  {
                    domain: { label: 'Domain' },
                    vmlimit: { label: 'VM Limit' },
                    vmtotal: { label: 'Total VMs' },
                    iplimit: { label: 'IP Limit' },
                    iptotal: { label: 'Total IPs' }
                  }
                ],
                dataProvider: testData.dataProvider.detailView('accounts')
              }
            }
          }
        }        
      },
      users: {
        type: 'select',
        title: 'Users',
        listView: {
          id: 'users',
          fields: {
            username: { label: 'Username', editable: true },
            email: { label: 'E-mail' },
            state: { label: 'State' }
          },
          filters: {
            mine: { label: 'My Accounts' },
            all: { label: 'All Accounts' }
          },
          dataProvider: testData.dataProvider.listView('users')
        }
      }
    }
  };  
})(cloudStack);
