package com.biyao.moses.exp.condition;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.moses.common.constant.ExpConditionConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.params.BaseRequest2;
import org.springframework.stereotype.Component;

/**
 * 老访客实验条件
 */
@Component(value = ExpConditionConstants.COND_OLD_VISITER)
public class OldVisitorCondition implements ExperimentCondition {

    @Override
    public Boolean satisfied(Object o) {
        BaseRequest2 request = (BaseRequest2) o;

        //非老访客
        if (request == null || request.getUpcUserType() == null
                || request.getUpcUserType() != UPCUserTypeConstants.OLD_VISITOR) {
            return false;
        }

        return true;
    }

}
