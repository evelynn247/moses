package com.biyao.moses.exp.condition;

import com.biyao.experiment.ExperimentCondition;
import com.biyao.moses.common.constant.ExpConditionConstants;
import com.biyao.moses.common.constant.UPCUserTypeConstants;
import com.biyao.moses.params.BaseRequest2;
import org.springframework.stereotype.Component;

/**
 * 老客实验条件
 */
@Component(value = ExpConditionConstants.COND_CUSTOMER)
public class CustomerCondition implements ExperimentCondition {

    @Override
    public Boolean satisfied(Object o) {
        BaseRequest2 request = (BaseRequest2) o;

        //非老客
        if (request == null || request.getUpcUserType() == null
                || request.getUpcUserType() != UPCUserTypeConstants.CUSTOMER) {
            return false;
        }

        return true;
    }

}
