package com.cloud.bridge.persist.dao;

import com.cloud.bridge.model.OfferingBundleVO;
import com.cloud.utils.db.GenericDao;

public interface OfferingDao extends GenericDao<OfferingBundleVO, Long> {

    int getOfferingCount();

    String getCloudOffering(String amazonEC2Offering);

    String getAmazonOffering(String cloudStackOffering);

    void setOfferMapping(String amazonEC2Offering, String cloudStackOffering);

    void deleteOfferMapping(String amazonEC2Offering);

}
