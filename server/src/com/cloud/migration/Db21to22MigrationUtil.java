package com.cloud.migration;

import java.io.File;
import java.util.List;

import org.apache.log4j.xml.DOMConfigurator;

import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class Db21to22MigrationUtil {
    private AccountDao _accountDao;
    private DomainDao _domainDao;
    private ResourceCountDao _resourceCountDao;

    private void doMigration() {
        setupComponents();

        migrateResourceCounts();

        System.out.println("Migration done");
    }

    private void migrateResourceCounts() {
        System.out.println("migrating resource counts");
        SearchBuilder<ResourceCountVO> sb = _resourceCountDao.createSearchBuilder();
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);

        for (ResourceType type : ResourceType.values()) {
            SearchCriteria<ResourceCountVO> sc = sb.create();
            sc.setParameters("type", type);

            List<ResourceCountVO> resourceCounts = _resourceCountDao.search(sc, null);
            for (ResourceCountVO resourceCount : resourceCounts) {
                if (resourceCount.getAccountId() != null) {
                    Account acct = _accountDao.findById(resourceCount.getAccountId());
                    Long domainId = acct.getDomainId();
                    while (domainId != null) {
                        _resourceCountDao.updateDomainCount(domainId, type, true, resourceCount.getCount());
                        DomainVO domain = _domainDao.findById(domainId);
                        domainId = domain.getParent();
                    }
                }
            }
        }
        System.out.println("done migrating resource counts");
    }

    private void setupComponents() {
        ComponentLocator.getLocator("migration", "migration-components.xml", "log4j-cloud.xml");
        ComponentLocator locator = ComponentLocator.getCurrentLocator();

        _accountDao = locator.getDao(AccountDao.class);
        _domainDao = locator.getDao(DomainDao.class);
        _resourceCountDao = locator.getDao(ResourceCountDao.class);
    }

    public static void main(String[] args) {
        File file = PropertiesUtil.findConfigFile("log4j-cloud.xml");

        if (file != null) {
            System.out.println("Log4j configuration from : " + file.getAbsolutePath());
            DOMConfigurator.configureAndWatch(file.getAbsolutePath(), 10000);
        } else {
            System.out.println("Configure log4j with default properties");
        }

        new Db21to22MigrationUtil().doMigration();
        System.exit(0);
    }
}
