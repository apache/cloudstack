/* eslint-disable no-mixed-spaces-and-tabs */
// Run this in browser with old UI Running and dump the data to olderLayout.json.
// Then run filterTranslations.py to populate the translations files.
// This is hacky but perhaps more effort isn't needed. Migrate away to the new UI!

var loadLabel = function (allFields, fieldDict, prefix) {
  var cols = ''
  $.each(Object.keys(fieldDict), function (idx1, fieldId) {
    if (fieldDict[fieldId].label) {
      if (allFields[fieldId]) {
        console.log('[WARN] Found multiple labels for API Key: ' + fieldId)
        allFields[fieldId].labels.push(fieldDict[fieldId].label)
        allFields[fieldId].components.push(prefix)
      } else {
        allFields[fieldId] = {
          'labels': [fieldDict[fieldId].label],
          'components': [prefix]
        }
      }
      cols = cols + "'" + fieldId + "', "
      if (fieldDict[fieldId].columns && $.type(fieldDict[fieldId].columns) === 'object') {
        prefix = prefix + '_columns'
        var columns = fieldDict[fieldId].columns
        $.each(Object.keys(columns), function (idx, colId) {
          if (allFields[colId]) {
            console.log('[WARN] Found multiple labels for API Key: ' + colId)
            allFields[colId].labels.push(columns[colId].label)
            allFields[colId].components.push(prefix)
          } else {
            allFields[colId] = {
              'labels': [columns[colId].label],
              'components': [prefix]
            }
          }
        })
      }
    }
  })
  return cols
}

var countActions = 0

var loadFields = function (data, prefix) {
  if ($.type(data) !== 'object') return {}
  var allFields = {}
  var columnsOrder = {}
  var actions = {}
  $.each(Object.keys(data), function (idx, key) {
    if (key === 'fields' || key === 'bottomFields' || key === 'topFields') {
      var fields = data[key]
	    var cols = ''
      if ($.type(fields) === 'object') {
        cols = loadLabel(allFields, fields, prefix)
      } else if ($.type(fields) === 'array') {
        $.each(fields, function (idx, fieldDict) {
		      cols = cols + "'" + loadLabel(allFields, fieldDict, prefix) + "', "
        })
      }
      columnsOrder[prefix] = cols.substring(0, cols.length - 2)
    } else if (key === 'actions') {
      var acVal = data[key]
      var curActions = []
      $.each(Object.keys(acVal), function (idx, acKey) {
        if (acVal[acKey].createForm) {
          curActions.push({ 'action': acKey, 'label': acVal[acKey].label, 'keys': acVal[acKey].createForm.fields })
        } else {
          curActions.push({ 'action': acKey, 'label': acVal[acKey].label })
        }
      })
      countActions = countActions + curActions.length
      actions[prefix] = curActions
    } else if ($.type(data[key]) === 'object' && ($.type(key) !== 'string' || key.indexOf('$') === -1)) {
      var recRes = loadFields(data[key], prefix + '.' + key)
      $.extend(allFields, recRes.allFields)
      $.extend(columnsOrder, recRes.columnsOrder)
      $.extend(actions, recRes.actions)
    }
  })
  return { 'allFields': allFields, 'columnsOrder': columnsOrder, 'actions': actions }
}
