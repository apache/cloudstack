window.clientApiUrl = '/client/api';
window.g_sessionKey = '';

// Login
$.ajax({
    type: 'POST',
    url: clientApiUrl,
    dataType: 'json',
    async: false,
    success: function(json) {
        g_sessionKey = json.loginresponse.sessionkey;
    },
    data: {
        command: 'login',
        domain: '/',
        username: 'admin',
        password: 'password',
        response: 'json'
    }
});

// Dummy app structure
window.cloudStack = {
    sections: {
        instances: {
            listView: {
                fields: [
                    { id: 'name', label: 'Name' },
                    { id: 'zone', label: 'Zone' },
                    { id: 'state', label: 'State' }
                ],
                actions: [
                    {
                        id: 'remove', label: 'X',
                        action: function(args) {
                            args.response.success();
                        }
                    }
                ],
                dataProvider: function(args) {
                    $.ajax({
                        url: createURL('listVirtualMachines'),
                        dataType: 'json',
                        success: function(json) {
                            args.response.success({
                                data: json.listvirtualmachinesresponse.virtualmachine
                            });
                        }
                    })
                }
            }
        }
    }
};
