package com.biyao.moses.rpc;


import com.biyao.upc.dubbo.client.business.toc.IBusinessTocDubboService;
import com.biyao.upc.dubbo.dto.VisitorInfoDTO;
import com.biyao.upc.dubbo.param.business.VisitorInfoParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class UpcRpcService {

    @Resource
    private IBusinessTocDubboService businessTocDubboService;

    /**
     * 查询用户类型返回(访客 0、老客 1)，默认0
     * @param uuid
     * @return
     */
    public Integer queryUserType(String uuid) {
        int rtNum  = 0;
        try {
            VisitorInfoParam vi = new VisitorInfoParam();
            vi.setCallSysName("mosesmatch.biyao.com");
            vi.setUuid(uuid);
            com.biyao.bsrd.common.client.model.result.Result<VisitorInfoDTO> visitorInfo =
                    businessTocDubboService.getVisitorInfo(vi);
            if(visitorInfo==null || visitorInfo.getObj() == null || visitorInfo.getObj().isMatch()){
                rtNum = 0;
            }else {
                rtNum = 1;
            }
        } catch (Exception e) {
            log.error("[严重异常]调用upc接口查询用户身份出错， uuid {} ",uuid, e);
        }
        return rtNum;

    }
}
