package com.cloud.api.query;

import com.cloud.agent.api.storage.OVFPropertyTO;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.storage.ImageStore;
import com.cloud.storage.VMTemplateDetailVO;
import com.cloud.storage.dao.VMTemplateDetailsDao;
import com.cloud.utils.db.SearchCriteria;
import com.google.gson.Gson;
import org.apache.cloudstack.api.command.user.template.ListTemplateOVFProperties;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TemplateOVFPropertyResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class QueryManagerImplTest {
    public static final String TYPE = "type";

    @Mock SearchCriteria<VMTemplateDetailVO> sc;
    @Mock VMTemplateDetailsDao vmTemplateDetailsDao;
    @Mock TemplateJoinDao _templateJoinDao;
    @Mock ListTemplateOVFProperties cmd;

    @InjectMocks
    private QueryManagerImpl mgr = new QueryManagerImpl();

    @Before
    public void setup() {
    }

    @Test public void listTemplateOVFProperties() {
        when(vmTemplateDetailsDao.createSearchCriteria()).thenReturn(sc);
        when(cmd.getTemplateId()).thenReturn(1L);
        VMTemplateDetailVO detailsVO = createDetailVO("naam", TYPE, "value", "", "concise label", "very elaborate description");
        List<VMTemplateDetailVO> list = createDetails(detailsVO);
        when(vmTemplateDetailsDao.search(sc,null)).thenReturn(list);
        when(_templateJoinDao.createTemplateOVFPropertyResponse(any())).thenReturn(new TemplateOVFPropertyResponse());

        ListResponse<TemplateOVFPropertyResponse> result = mgr.listTemplateOVFProperties(cmd);
        assertEquals("expecting 1 object returned",result.getCount().longValue(), 1l);
    }

    List<VMTemplateDetailVO> createDetails(VMTemplateDetailVO ... vos) {
        List<VMTemplateDetailVO> list = new ArrayList<>();
        for (VMTemplateDetailVO vo : vos) {
            list.add(vo);
        }
        return list;
    }

    private VMTemplateDetailVO createDetailVO(String name, String type, String value, String qualifiers, String label, String description) {
        VMTemplateDetailVO vo = new VMTemplateDetailVO();
        vo.setName(ImageStore.OVF_PROPERTY_PREFIX + name);
        OVFPropertyTO propertyTO = new OVFPropertyTO(name, type, value, qualifiers, true, label, description, false);
        Gson gson = new Gson();
        vo.setValue(gson.toJson(propertyTO));
        return vo;
    }
}