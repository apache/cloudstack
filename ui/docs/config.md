## Configuration

### Section Config Definition

A new section may be added in `src/config/section` and in `src/config/router.js`
import the new section (newconfig.js as example) configuration file and rules to
`asyncRouterMap` as:

    import newconfig from '@/config/section/newconfig'

    [ ... snipped ... ]

      generateRouterMap(newSection),


### API

An existing or new section config/js file must export the following parameters:

- `name`: unique path in URL
- `title`: the name to be displayed in navigation and breadcrumb
- `icon`: the icon to be displayed, from AntD's icon set https://vue.ant.design/components/icon/
- `children`: (optional) array of resources sub-navigation under the parent group
- `permission`: when children are not defined, the array of API to check against
  allowed auto-discovered APIs
- `columns`: when children is not defined, list of column keys
- `component`: when children is not defined, the custom component for rendering
  the route view

See `src/config/section/compute.js` and `src/config/section/project.js` for example.

The children should have:

- `name`: unique path in the URL
- `title`: the name to be displayed in navigation and breadcrumb
- `icon`: the icon to be displayed, from AntD's icon set https://vue.ant.design/components/icon/
- `permission`: the array of API to check against auto-discovered APIs
- `columns`: list of column keys for list view rendering
- `details`: list of keys for detail list rendering for a resource
- `tabs`: array of custom components that will get rendered as tabs in the
  resource view
- `component`: the custom component for rendering the route view
  default list view (table)
- `actions`: arrays of actions/buttons

### Action API

The actions defined for a children show up as group of buttons on the default
autogen view (that shows tables, actions etc.). Each action item should define:

- `api`: The CloudStack API for the action
- `icon`: the icon to be displayed, from AntD's icon set https://vue.ant.design/components/icon/
- `label`: The action button name label
- `listView`: (boolean) whether to show the action button in list view (table)
- `dataView`: (boolean) whether to show the action button in resource/data view
- `groupAction`: Whether the button supports groupable actions when multiple
  items are selected in the table
- `options`: list of API arguments to render/show on auto-generated action form
- `hidden`: function that takes in a records and returns a boolean to control if
  the action button needs to be disabled/hidden
- `component`: the custom component to render the action (in a separate route view)
- `popup`: (boolean) when true, displays any custom component in a popup modal
  than in its separate route view
