## Custom Actions

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
- `args`: list of API arguments to render/show on auto-generated action form
- `show`: function that takes in a records and returns a boolean to control if
  the action button needs to be shown or hidden
- `popup`: (boolean) when true, displays any custom component in a popup modal
  than in its separate route view
- `component`: the custom component to render the action (in a separate route view)
