package com.cloud.migration;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import com.cloud.utils.db.Transaction;
import com.cloud.vm.InstanceGroupVMMapVO;
import com.cloud.vm.InstanceGroupVO;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.InstanceGroupVMMapDao;

public class Db21to22MigrationUtil {
    private AccountDao _accountDao;
    private DomainDao _domainDao;
    private ResourceCountDao _resourceCountDao;
    private InstanceGroupDao _vmGroupDao;
    private InstanceGroupVMMapDao _groupVMMapDao;

    private void doMigration() {
        setupComponents();

        migrateResourceCounts();
        
        setupInstanceGroups();

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
        _vmGroupDao = locator.getDao(InstanceGroupDao.class);
        _groupVMMapDao = locator.getDao(InstanceGroupVMMapDao.class);
    }
    
    private void setupInstanceGroups() {
    	System.out.println("setting up vm instance groups");
    	
    	//Search for all the vms that have not null groups
    	long vmId = 0;
    	long accountId = 0;
    	String groupName;
    	Transaction txn = Transaction.open(Transaction.CLOUD_DB);
    	txn.start();
		try {
	    	String request = "SELECT vm.id, uservm.account_id, vm.group from vm_instance vm, user_vm uservm where vm.group is not null and vm.removed is null and vm.id=uservm.id order by id";
	    	PreparedStatement statement = txn.prepareAutoCloseStatement(request);
	    	ResultSet result = statement.executeQuery();
	    	while (result.next()) {
	    		vmId = result.getLong(1);
	    		accountId = result.getLong(2);
	    		groupName = result.getString(3);
		        InstanceGroupVO group = _vmGroupDao.findByAccountAndName(vmId, groupName);
		    	//Create vm group if the group doesn't exist for this account
		        if (group == null) {
					group = new InstanceGroupVO(groupName, accountId);
					group =  _vmGroupDao.persist(group);
		        }
				
				if (group != null) {
					InstanceGroupVMMapVO groupVmMapVO = new InstanceGroupVMMapVO(group.getId(), vmId);
					_groupVMMapDao.persist(groupVmMapVO);
				}
	    	}
	    	statement.close();
			txn.commit();
		} catch (Exception e) {
			System.out.println("Unhandled exception: " + e);
		} finally {
			txn.close();
		}
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
