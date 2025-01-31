package com.cloud.upgrade;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationSubGroupVO;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationGroupsAggregatorTest {
    @InjectMocks
    private ConfigurationGroupsAggregator configurationGroupsAggregator = new ConfigurationGroupsAggregator();

    @Mock
    private ConfigurationDao configDao;

    @Mock
    private ConfigurationGroupDao configGroupDao;

    @Mock
    private ConfigurationSubGroupDao configSubGroupDao;

    @Mock
    private Logger logger;

    @Test
    public void testUpdateConfigurationGroups() {
        ConfigurationVO config = new ConfigurationVO("Advanced", "DEFAULT", "management-server",
                "test.config.name", null, "description");
        config.setGroupId(1L);
        config.setSubGroupId(1L);

        SearchBuilder<ConfigurationVO> sb = Mockito.mock(SearchBuilder.class);
        when(configDao.createSearchBuilder()).thenReturn(sb);
        Mockito.when(sb.select("name", SearchCriteria.Func.NATIVE, "test.config.name")).thenReturn(sb);
        Mockito.when(sb.select("groupId", SearchCriteria.Func.NATIVE, 1L)).thenReturn(sb);
        Mockito.when(sb.select("subGroupId", SearchCriteria.Func.NATIVE, 1L)).thenReturn(sb);
        Mockito.when(sb.entity()).thenReturn(config);
        when(configDao.searchIncludingRemoved(any(), isNull(), isNull(), eq(false))).thenReturn(Collections.singletonList(config));

        ConfigurationSubGroupVO configSubGroup = Mockito.mock(ConfigurationSubGroupVO.class);
        when(configSubGroupDao.findByName("name")).thenReturn(configSubGroup);
        Mockito.when(configSubGroup.getId()).thenReturn(10L);
        Mockito.when(configSubGroup.getGroupId()).thenReturn(5L);

        configurationGroupsAggregator.updateConfigurationGroups();

        Assert.assertEquals(Long.valueOf(5), config.getGroupId());
        Assert.assertEquals(Long.valueOf(10), config.getSubGroupId());
        Mockito.verify(configDao, Mockito.times(1)).persist(config);
        Mockito.verify(logger, Mockito.times(1)).debug("Updating configuration groups");
        Mockito.verify(logger, Mockito.times(1)).debug("Successfully updated configuration groups.");
    }
}
