# UI Development

The modern CloudStack UI is role-based progressive app that uses VueJS and Ant Design.

Javascript, VueJS references:
- https://www.w3schools.com/js/
- https://www.geeksforgeeks.org/javascript-tutorial/
- https://vuejs.org/v2/guide/
- https://www.youtube.com/watch?v=Wy9q22isx3U

All the source is in the `src` directory with its entry point at `main.js`.
The following tree shows the basic UI codebase filesystem:

```bash
    src
    ├── assests       # sprites, icons, images
    ├── components    # Shared vue files used to render various generic / widely used components
    ├── config        # Contains the layout details of the various routes / sections available in the UI
    ├── locales       # Custom translation keys for the various supported languages
    ├── store         # A key-value storage for all the application level state information such as user info, etc
    ├── utils         # Collection of custom libraries
    ├── views         # Custom vue files used to render specific components
    ├── ...
    └── main.js       # Main entry-point
```

## Development

Clone the repository:

```
git clone https://github.com/apache/cloudstack.git
cd cloudstack/ui
npm install
```
Override the default `CS_URL` to a running CloudStack management server:
```
cp .env.local.example .env.local
```
Change the `CS_URL` in the `.env.local` file
To configure https, you may use `.env.local.https.example`.
Build and run:
```
npm run serve
```

## Implementation

## Defining a new Section

### Section Config Definition

A new section may be added in `src/config/section` and in `src/config/router.js`,
import the new section's (newconfig.js as example) configuration file and rules to
`asyncRouterMap` as:

    import newconfig from '@/config/section/newconfig'

    [ ... snipped ... ]

      generateRouterMap(newSection),


### Section

An existing or new section's config/js file must export the following parameters:

- `name`: Unique path in URL
- `title`: The name to be displayed in navigation and breadcrumb
- `icon`: The icon to be displayed, from AntD's icon set
  https://vue.ant.design/components/icon/
- `docHelp`: Allows to provide a link to a document to provide details on the
  section
- `searchFilters`: List of parameters by which the resources can be filtered
  via the list API
- `children`: (optional) Array of resources sub-navigation under the parent
  group
- `permission`: When children are not defined, the array of APIs to check against
  allowed auto-discovered APIs
- `columns`: When children is not defined, list of column keys
- `component`: When children is not defined, the custom component for rendering
  the route view


See `src/config/section/compute.js` and `src/config/section/project.js` for example.

The children should have:

- `name`: Unique path in the URL
- `title`: The name to be displayed in navigation and breadcrumb
- `icon`: The icon to be displayed, from AntD's icon set
  https://vue.ant.design/components/icon/
- `permission`: The array of APIs to check against auto-discovered APIs
- `columns`: List of column keys for list view rendering
- `details`: List of keys for detail list rendering for a resource
- `tabs`: Array of custom components that will get rendered as tabs in the
  resource view
- `component`: The custom component for rendering the route view
- `related`: A list of associated entitiy types that can be listed via passing
  the current resource's id as a parameter in their respective list APIs
- `actions`: Array of actions that can be performed on the resource

## Custom Actions

The actions defined for children show up as group of buttons on the default
autogen view (that shows tables, actions etc.). Each action item should define:

- `api`: The CloudStack API for the action. The action button will be hidden if
  the user does not have permission to execute the API
- `icon`: The icon to be displayed, from AntD's icon set
  https://vue.ant.design/components/icon/
- `label`: The action button name label and modal header
- `message`: The action button confirmation message
- `docHelp`: Allows to provide a link to a document to provide details on the
  action
- `listView`: (boolean) Whether to show the action button in list view (table).
  Defaults to false
- `dataView`: (boolean) Whether to show the action button in resource/data view.
  Defaults to false
- `args`: List of API arguments to render/show on auto-generated action form.
  Can be a function which returns a list of arguments
- `show`: Function that takes in a records and returns a boolean to control if
  the action button needs to be shown or hidden. Defaults to true
- `groupShow`: Same as show but for group actions. Defaults to true
- `popup`: (boolean) When true, displays any custom component in a popup modal
  than in its separate route view. Defaults to false
- `groupAction`: Whether the button supports groupable actions when multiple
  items are selected in the table. Defaults to false
- `mapping`: The relation of an arg to an api and the associated parameters to
  be passed and filtered on the result (from which its id is used as a
  select-option) or a given hardcoded list of select-options
- `groupMap`: Function that maps the args and returns the list of parameters to
  be passed to the api
- `component`: The custom component to render the action (in a separate route
  view under src/views/<component>). Uses an autogenerated form by default.
  Examples of such views can be seen in the src/views/ directory

For Example:
```
{
  api: 'startVirtualMachine',
  icon: 'caret-right',
  label: 'label.action.start.instance',
  message: 'message.action.start.instance',
  docHelp: 'adminguide/virtual_machines.html#stopping-and-starting-vms',
  dataView: true,
  groupAction: true,
  groupMap: (selection) => { return selection.map(x => { return { id: x } }) },
  show: (record) => { return ['Stopped'].includes(record.state) },
  args: (record, store) => {
    var fields = []
    if (store.userInfo.roletype === 'Admin') {
      fields = ['podid', 'clusterid', 'hostid']
    }
    if (record.hypervisor === 'VMware') {
      if (store.apis.startVirtualMachine.params.filter(x => x.name === 'bootintosetup').length > 0) {
        fields.push('bootintosetup')
      }
    }
    return fields
  },
  response: (result) => { return result.virtualmachine && result.virtualmachine.password ? `Password of the VM is ${result.virtualmachine.password}` : null }
}
```

## Resource List View

After having, defined a section and the actions that can be performed in the
 particular section; on navigating to the section, we can have a list of
 resources available, for example, on navigating to **Compute > Instances**
 section, we see a list of all the VM instances (each instance referred to as a
 resource).

The columns that should be made available while displaying the list of
  resources can be defined in the section's configuration file under the
  columns attribute (as mentioned above). **columns** maybe defined as an array
  or a function in case we need to selectively (i.e., based on certain
  conditions) restrict the view of certain columns.

It also contains router-links to the resouce and other related data such as the
  account, domain, etc of the resource if present

For example:

```
    ...
    // columns defined as an array
    columns: ['name', 'state', 'displaytext', 'account', 'domain'],

    // columns can also be defined as a function, so as to conditionally restrict view of certain columns
    columns: () => {
        var fields = ['name', 'hypervisor', 'ostypename']
        if (['Admin', 'DomainAdmin'].includes(store.getters.userInfo.roletype)) {
          fields.push('account')
        }
        ...
    }
```

## Resource Detail View Customization

From the List View of the resources, on can navigate to the individual
  resource's detail view, which in CloudStack UI we refer to as the
  *Resource View* by click on the specific resource.
The Resource View has 2 sections:
- InfoCard to the left that has basic / minimal details of that resource along
  with the related entities
- DetailsTab to the right which provide the basic details about the resource.

Custom tabs to render custom details, addtional information of the resource
  The list of fields to be displayed maybe defined as an array
  or a function in case we need to selectively (i.e., based on certain
  conditions) restrict the view of certain columns. The names specified in the
  details array should correspond to the api parameters

For example,

```
    ...
    details: ['name', 'id', 'displaytext', 'projectaccountname', 'account', 'domain'],
    ...
    // To render the above mentioned details in the right section of the Resource View, we must import the DetailsTab
    tabs: [
    {
      name: 'details',
      component: () => import('@/components/view/DetailsTab.vue')
    },
    ...
    ]
```

Additional tabs can be defined by adding on to the tabs section.
