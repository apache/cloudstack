package org.apache.cloudstack.engine.cloud.entity.api.db.dao;


import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;


import org.apache.cloudstack.engine.cloud.entity.api.db.VMComputeTagVO;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value = { VMComputeTagDao.class })
public class VMComputeTagDaoImpl extends GenericDaoBase<VMComputeTagVO, Long> implements VMComputeTagDao {

    protected SearchBuilder<VMComputeTagVO> VmIdSearch;
    
    public VMComputeTagDaoImpl() {
    }
    
    @PostConstruct
    public void init() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmIdSearch.done();
        
    }
    
    @Override
    public void persist(long vmId, List<String> computeTags) {
        Transaction txn = Transaction.currentTxn();

        txn.start();
        SearchCriteria<VMComputeTagVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);
        expunge(sc);
        
        for (String tag : computeTags) {
            if(tag != null){
                tag = tag.trim();
                if(tag.length() > 0) {
                    VMComputeTagVO vo = new VMComputeTagVO(vmId, tag);
                    persist(vo);
                }
            }
        }
        txn.commit();
    }

    @Override
    public List<String> getComputeTags(long vmId) {
        
        SearchCriteria<VMComputeTagVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);
        
        List<VMComputeTagVO> results = search(sc, null);
        List<String> computeTags = new ArrayList<String>(results.size());
        for (VMComputeTagVO result : results) {
            computeTags.add(result.getComputeTag());
        }

        return computeTags;
    }
    
}
