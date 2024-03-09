package com.biyao.moses.exp.condition;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.moses.common.constant.ExpConditionConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.params.BaseRequest2;
import org.springframework.stereotype.Component;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/10/12
 **/
@Component(value = ExpConditionConstants.COND_OLD_BUYER_LAST_VISIT)
public class OldBuyerCondition implements ExperimentCondition {
    private static final long LAST_VIEW_TIME_LIMIT = 2592000000L; //60*60*24*30*1000 30天
    @Override
    public Boolean satisfied(Object o) {
        BaseRequest2 request = (BaseRequest2) o;
        //老客 最近30天有浏览记录
        if(request == null || request.getUpcUserType() == null){
            return false;
        }

        if (UPCUserTypeConstants.CUSTOMER == request.getUpcUserType()){
            long currentTime = System.currentTimeMillis();
            Long latestViewTime = request.getLatestViewTime();
            if(latestViewTime != null && latestViewTime != 0) {
                if (currentTime - request.getLatestViewTime().longValue() < LAST_VIEW_TIME_LIMIT) {
                    return true;
                }
            }
        }
        return false;
    }
}
