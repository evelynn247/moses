package com.biyao.moses.exp.condition;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.moses.common.constant.ExpConditionConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.params.BaseRequest2;
import org.springframework.stereotype.Component;

/**
 * 新访客实验条件
 */
@Component(value = ExpConditionConstants.COND_NEW_VISITER)
public class NewVisitorCondition implements ExperimentCondition {

    @Override
    public Boolean satisfied(Object o) {
        BaseRequest2 request = (BaseRequest2) o;

        //非新访客
        if (request == null || request.getUpcUserType() == null
                || request.getUpcUserType() != UPCUserTypeConstants.NEW_VISITOR) {
            return false;
        }

        return true;
    }

}
