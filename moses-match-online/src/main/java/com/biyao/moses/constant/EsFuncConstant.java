package com.biyao.moses.constant;

import com.biyao.moses.Enum.SceneEnum;

import static com.biyao.moses.Enum.SceneEnum.NEWV_VIDEO;
import static com.biyao.moses.constant.MatchStrategyConst.PERSONAL_FM;
import static com.biyao.moses.constant.MatchStrategyConst.PERSONAL_ICF;

/**
 * @program: moses-parent-online
 * @description: es 打分函数
 * @author: changxiaowei
 * @Date: 2022-02-28 15:30
 **/
public class EsFuncConstant {

    // 热门召回函数
    public static final String HOT_FUNC =
            "if (doc['hotscore'].size() == 0 || doc['hotscore'].value == 0) {\n" +
                    "    double[] seed = new double[]{0.2, 0.23, 0.25, 0.27, 0.3, 0.33, 0.36, 0.385, 0.41,\n" +
                    "        0.43, 0.45, 0.48, 0.5, 0.53, 0.56, 0.59, 0.62, 0.65, 0.68, 0.71, 0.74, 0.77};\n" +
                    "    double ran = Math.random();\n" +
                    "    if(ran > 0.99 && ran < 1){\n" +
                    "        int index = (int) (Math.random() * seed.length);\n" +
                    "        return ran - seed[index] + 1.0;\n" +
                    "    }else{\n" +
                    "        return ran - 0.8 + 1.0;\n" +
                    "    }\n" +
                    "}else{\n" +
                    "    return doc['hotscore'].value + 1.0;\n" +
                    "}";
    /**
     * fmVector向量召回 打分函数
     */
    public static final String PER_FM_FUNC = "double cos = cosineSimilarity(params.query_vector, 'fmVector') + 1.0; return Math.abs(cos-0.2)";
    /**
     * icfVector向量召回 打分函数
     */
    public static final String PER_ICF_FUNC = "cosineSimilarity(params.query_vector, 'icfVector') + 1.0";

    /**
     * 新访客新手专享icfVector向量召回 打分函数
     */
    public static final String PER_ICF_FUNC_NEWV =
            "   if(doc['isNewVProduct'].value == 1 && doc['isToggroupProduct'].value == 1 ){\n" +
                    "      return cosineSimilarity(params.query_vector, 'icfVector') + 7.0;\n" +
                    "   }else if(doc['isNewVProduct'].value == 1 && doc['isToggroupProduct'].value == 0){\n" +
                    "       return cosineSimilarity(params.query_vector, 'icfVector') + 5.0;\n" +
                    "   }else if(doc['isNewVProduct'].value == 0 && doc['isToggroupProduct'].value == 1){\n" +
                    "       return cosineSimilarity(params.query_vector, 'icfVector') + 3.0;\n" +
                    "   }else{\n" +
                    "       return cosineSimilarity(params.query_vector, 'icfVector') + 1.0;\n" +
                    "   }";
    /**
     * 新访客新手专享FMVector向量召回 打分函数
     */
    public static final String PER_FM_FUNC_NEWV =
            "   if(doc['isNewVProduct'].value == 1 && doc['isToggroupProduct'].value == 1 ){\n" +
                    "      double cos = cosineSimilarity(params.query_vector, 'fmVector') + 7.0;  return Math.abs(cos-0.2);\n" +
                    "   }else if(doc['isNewVProduct'].value == 1 && doc['isToggroupProduct'].value == 0){\n" +
                    "       double cos = cosineSimilarity(params.query_vector, 'fmVector') + 5.0;  return Math.abs(cos-0.2);\n" +
                    "   }else if(doc['isNewVProduct'].value == 0 && doc['isToggroupProduct'].value == 1){\n" +
                    "       double cos = cosineSimilarity(params.query_vector, 'fmVector') + 3.0;  return Math.abs(cos-0.2);\n" +
                    "   }else{\n" +
                    "       double cos = cosineSimilarity(params.query_vector, 'fmVector') + 1.0;  return Math.abs(cos-0.2); \n" +
                    "   } ";


    /**
     * 老访客新手专享icfVector向量召回 打分函数
     */
    /**
     * fmVector向量召回 打分函数
     */
    public static final String PER_ICF_FUNC_OLDV =
            "if(doc['isToggroupProduct'].value == 1){\n" +
                    "   return cosineSimilarity(params.query_vector, 'icfVector') + 3.0;\n" +
                    "}else{\n" +
                    "   return cosineSimilarity(params.query_vector, 'icfVector') + 1.0;\n" +
                    "}";
    public static final String PER_FM_FUNC_OLDV =
            "if(doc['isToggroupProduct'].value == 1){\n" +
                    "   double cos = cosineSimilarity(params.query_vector, 'fmVector') + 3.0; return Math.abs(cos-0.2); \n" +
                    "}else{\n" +
                    "   double cos = cosineSimilarity(params.query_vector, 'fmVector') + 1.0;  return Math.abs(cos-0.2);\n" +
                    "} ";
    /**
     * 特权金icfVector向量召回 打分函数
     */
    public static final String PER_ICF_FUNC_TQJ =
            "if(doc['newPrivilege'].value == 1){\n" +
                    "   return cosineSimilarity(params.query_vector, 'icfVector') + 3.0;\n" +
                    "}else{\n" +
                    "   return cosineSimilarity(params.query_vector, 'icfVector') + 1.0;\n" +
                    "}";
    /**
     * 特权金icfVector向量召回 打分函数
     */
    public static final String PER_FM_FUNC_TQJ =
            "if(doc['newPrivilege'].value == 1){\n" +
                    "   double cos = cosineSimilarity(params.query_vector, 'fmVector') + 3.0;  return Math.abs(cos-0.2);\n" +
                    "}else{\n" +
                    "   double cos = cosineSimilarity(params.query_vector, 'fmVector') + 1.0;  return Math.abs(cos-0.2);\n" +
                    "}";

    public static final String  PAINLESS  =  "painless";



    /**
     * 根据场景id 和召回源 获取对应的打分函数
     * @param source
     * @param sceneId
     * @return
     */
    public static   String getFuncBySceneAndoSource(String source,int sceneId){
        switch (source){
            case PERSONAL_FM :
                if(NEWV_VIDEO.getSceneId()== sceneId){
                    return PER_FM_FUNC_NEWV;
                } else if(SceneEnum.OLDV_VIDEO.getSceneId()== sceneId){
                    return PER_FM_FUNC_OLDV;
                } else if(SceneEnum.TQJXF_VIDEO.getSceneId()== sceneId){
                    return PER_FM_FUNC_TQJ;
                }else {
                    return PER_FM_FUNC;
                }
            case PERSONAL_ICF :
                if(NEWV_VIDEO.getSceneId()== sceneId){
                    return PER_ICF_FUNC_NEWV;
                } else if(SceneEnum.OLDV_VIDEO.getSceneId()== sceneId){
                    return PER_ICF_FUNC_OLDV;
                } else if(SceneEnum.TQJXF_VIDEO.getSceneId()== sceneId){
                    return PER_ICF_FUNC_TQJ;
                }else {
                    return PER_ICF_FUNC;
                }
        }
        return "";
    }
}
