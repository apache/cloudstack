package org.apache.cloudstack.api.command.admin.iso;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.iso.CopyIsoCmd;
import org.apache.cloudstack.api.response.TemplateResponse;

@APICommand(name = "copyIso", description = "Copies an iso from one zone to another.", responseObject = TemplateResponse.class, responseView = ResponseView.Full,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CopyIsoCmdByAdmin extends CopyIsoCmd {

}
