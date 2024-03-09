package com.biyao.moses.tensorflowModel;

import com.alibaba.fastjson.JSONObject;
import com.biyao.moses.common.utils.CommonUtil;
import com.biyao.moses.config.AlgorithmRedisUtil;
import com.biyao.moses.match.MatchOnlineRequest;
import com.biyao.moses.param.UserPredictParam;
import com.biyao.moses.util.MatchUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tensorflow.Tensor;
import org.tensorflow.ndarray.*;
import org.tensorflow.types.TFloat32;
import org.tensorflow.types.TInt32;
import org.tensorflow.types.TInt64;
import org.tensorflow.types.TString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.biyao.moses.common.RediskeyCommonConstant.RECOMMOND_ES_REDISKEY_PREFIX;
import static com.biyao.moses.common.constant.EsIndexConstant.ICF_VECTOR;
import static com.biyao.moses.constant.MatchStrategyConst.PERSONAL_FM;
import static com.biyao.moses.constant.MatchStrategyConst.PERSONAL_ICF;
import static com.biyao.moses.constant.RedisKeyConstant.*;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @Date: 2021-12-06 17:31
 **/
@Component
@Slf4j
public class TensorflowModelService {

    @Autowired
    TensorflowModelCache tensorflowModelCache;
    @Autowired
    AlgorithmRedisUtil algorithmRedisUtil;

    private float[] u_vector_from_tensor(Tensor t){
        TFloat32 data = (TFloat32) t; // 具体的数据
        int vector_len = (int) data.shape().asArray()[1];  // 向量的长度
        float[] user_vector = new float[vector_len];  //转化成java数组
        StdArrays.copyFrom(data.get(0), user_vector);
        return user_vector;
    }
    /**
     * 用户向量预测
     * @param request
     * @return
     */
    public  Map<String,float[]> predict(MatchOnlineRequest request) {
        //结果初始化
        Map<String,float[]> preds = new HashMap<>();
        long start = System.currentTimeMillis();
        Tensor user_vector_fm_t = null;
        Tensor user_vector_i2v_t = null;
        UserPredictParam userPredictParam = buildUserPredictParam(request);
        try {
            Map<String, Tensor> call_res = tensorflowModelCache.getTensorflowModelfunction().call(new HashMap<String, Tensor>() {
                {
                    put("hour", userPredictParam.getT_hour());
                    put("week", userPredictParam.getT_week());
                    put("by_prov", userPredictParam.getT_by_prov());
                    put("site_id", userPredictParam.getT_siteId());
                    put("user_gender", userPredictParam.getT_sex());
                    put("device", userPredictParam.getT_device());
                    put("user_float", userPredictParam.getUser_float());
                    put("click_pid", userPredictParam.getT_click_pid_10());
                }
            });
             user_vector_fm_t = call_res.get("user_vector_fm");
             user_vector_i2v_t = call_res.get("user_vector_i2v");
            // 两个用户向量的长度不同
            float[] user_vector_fm = u_vector_from_tensor(user_vector_fm_t);
            float[] user_vector_i2v = u_vector_from_tensor(user_vector_i2v_t);
            preds.put(PERSONAL_FM,user_vector_fm);
            preds.put(PERSONAL_ICF,user_vector_i2v);
        } catch (Throwable e) {
            log.error("[严重异常]预测用户向量时发生异常,异常信息：",e);
        }finally {
            if(Objects.nonNull(user_vector_fm_t)){
                user_vector_fm_t.close();  // Tensor一定要记得close，或者放在Tensor Close
            }
            if(Objects.nonNull(user_vector_i2v_t)){
                user_vector_i2v_t.close();  // Tensor一定要记得close，或者放在Tensor Close
            }
        }
        log.info("[检查日志]计算用户向量结束，耗时：{}",System.currentTimeMillis()-start);
        return preds;
    }

    /**
     * 根据主商品获取商品向量
     */
    public  Map<String,float[]> getIcfVectorByMainPid(Long pid){
        Map<String,float[]> vector = new HashMap<>();
        // 获取该商品的icf向量
        String icfVector = algorithmRedisUtil.hget(RECOMMOND_ES_REDISKEY_PREFIX + pid, ICF_VECTOR);
        if (StringUtils.isEmpty(icfVector)) {
            return vector;
        }
        String[] icfArr = icfVector.split(",");
        if (icfArr.length == 16) {
            vector.put(PERSONAL_ICF,CommonUtil.StringArrToFloat(icfArr));
        } else {
            log.error("[严重异常]icfVector向量维度异常,icfVector:{}", JSONObject.toJSONString(icfVector));
            return vector;
        }
        // 结果返回量
        return vector;
    }
    /**
     * 构建用户向量预测方法参数
     * @param request
     * @return
     */
    private UserPredictParam buildUserPredictParam(MatchOnlineRequest request){
        UserPredictParam userPredictParam =new UserPredictParam();
        /**
         * ************ 实时获取&传参 ***************
         * 当前小时
         * 当前周
         * 是否为注册用户
         * 设备
         * 平台
         ************ UC ***************
         * 地区：UC  com.uc.domain.constant.UserFieldConstants#LOCATION
         * 10个最近点击的商品  viewPids
         * 性别
         ************ redis：hash ***************
         * 7天、15天内的点击率
         * 购买单数
         * 总金额
         * 商品均价
         * 商品价格方差
         * 注册时间
         * 首单时间
         */
        try {
            Map<String, String> userInfoMap = algorithmRedisUtil.hgetAll(user_info + request.getUuid());
            // 当前小时 取值范围 0~23
            IntNdArray hour_nd = NdArrays.ofInts(Shape.of(1, 1));
            hour_nd.setInt(MatchUtil.getHour(), 0, 0);
            userPredictParam.setT_hour(TInt32.tensorOf(hour_nd));

            // 当前周  取值范围 1~7
            IntNdArray week_nd = NdArrays.ofInts(Shape.of(1, 1));
            week_nd.setInt(MatchUtil.getWeek(), 0, 0);
            userPredictParam.setT_week(TInt32.tensorOf(week_nd));

            // 性别  取值范围0,1,-1 分别表示 男、女、未知
            IntNdArray sex_nd = NdArrays.ofInts(Shape.of(1, 1));
            sex_nd.setInt(request.getUserSex().intValue(), 0, 0);
            userPredictParam.setT_sex(TInt32.tensorOf(sex_nd));

            // 季节 没有使用
            //int[][] season_arr = new int[1][1];
            //sex_arr[0][0] = request.getUserSeason();
            //userPredictParam.setT_season(Tensor.create(season_arr, Integer.class));

            // 用户端id
            IntNdArray siteId_nd = NdArrays.ofInts(Shape.of(1, 1));
            siteId_nd.setInt(request.getSiteId(), 0, 0);
            userPredictParam.setT_siteId(TInt32.tensorOf(siteId_nd));

            // 浮点型数据 从redis取
            FloatNdArray user_float_nd = NdArrays.ofFloats(Shape.of(1, 15));
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(is_register),0f), 0, 0);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(regist_date),6000f), 0, 1);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(first_order_date),6000f), 0, 2);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_ctr_7day),0f), 0, 3);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_cvr_7day),0f), 0, 4);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_paid_num_7day),0f), 0, 5);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_gmv_7day),0f), 0, 6);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_paid_price_avg_7day),0f), 0, 7);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_paid_price_var_7day),0f), 0, 8);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_ctr_15day),0f), 0, 9);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_cvr_15day),0f), 0, 10);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_paid_num_15day),0f), 0, 11);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_gmv_15day),0f), 0, 12);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_paid_price_avg_15day),0f), 0, 13);  // redis
            user_float_nd.setFloat(MatchUtil.stringToFloat(userInfoMap.get(user_paid_price_var_15day),0f), 0, 14);  // redis
            userPredictParam.setUser_float(TFloat32.tensorOf(user_float_nd));

            // 设备
            NdArray<String> device_nd = NdArrays.ofObjects(String.class, Shape.of(1, 1));
            device_nd.set(NdArrays.vectorOfObjects(request.getDevice()), 0); // "iphone X"  默认值是""
            userPredictParam.setT_device(TString.tensorOf(device_nd));

            // 地区  当前省份
            NdArray<String> by_prov_nd = NdArrays.ofObjects(String.class, Shape.of(1, 1));
            by_prov_nd.set(NdArrays.vectorOfObjects(request.getLocation()), 0); // "河南省"  默认值是""
            userPredictParam.setT_by_prov(TString.tensorOf(by_prov_nd));

            //最近点击的商品
            List<String> viewPids = request.getViewPids();
            LongNdArray click_pid_nd = NdArrays.ofLongs(Shape.of(1, viewPids.size())); // 0 初始化
            for (int i = 0; i < viewPids.size(); i++) {
                click_pid_nd.setLong(Long.valueOf(viewPids.get(i)), 0, i);
            }
            userPredictParam.setT_click_pid_10(TInt64.tensorOf(click_pid_nd));
        }catch (Exception e){
            log.error("[严重异常][告警邮件]构建预测用户向量需要的参数异常，异常原因：",e);
        }
        return  userPredictParam;
    }



}