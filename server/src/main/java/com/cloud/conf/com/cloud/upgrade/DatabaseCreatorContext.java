
package com.cloud.conf.com.cloud.upgrade;

import java.util.ArrayList;
import com.cloud.upgrade.DatabaseUpgradeChecker;
import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentInstantiationPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class DatabaseCreatorContext {


    @Bean("instantiatePostProcessor")
    public ComponentInstantiationPostProcessor instantiatePostProcessor(
        @Qualifier("transactionContextBuilder")
        com.cloud.utils.db.TransactionContextBuilder transactionContextBuilder) {
        ComponentInstantiationPostProcessor bean = new ComponentInstantiationPostProcessor();
        ArrayList list0 = new ArrayList();
        list0 .add(transactionContextBuilder);
        bean.setInterceptors(list0);
        return bean;
    }

    @Bean("databaseUpgradeChecker")
    public DatabaseUpgradeChecker databaseUpgradeChecker() {
        return new DatabaseUpgradeChecker();
    }

    @Bean("componentContext")
    public ComponentContext componentContext() {
        return new ComponentContext();
    }

    @Bean("transactionContextBuilder")
    public com.cloud.utils.db.TransactionContextBuilder transactionContextBuilder() {
        return new com.cloud.utils.db.TransactionContextBuilder();
    }

    @Bean("versionDaoImpl")
    public VersionDaoImpl versionDaoImpl() {
        return new VersionDaoImpl();
    }

}
