package com.cloud.ucs.manager;

import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.utils.component.Manager;

public interface UcsManager extends Manager {
    AddUcsManagerResponse addUcsManager(AddUcsManagerCmd cmd);
    
    ListResponse<ListUcsProfileResponse> listUcsProfiles(ListUcsProfileCmd cmd);
    
    ListResponse<ListUcsManagerResponse> listUcsManager(ListUcsManagerCmd cmd);

    void associateProfileToBlade(AssociateUcsProfileToBladeCmd cmd);
}
