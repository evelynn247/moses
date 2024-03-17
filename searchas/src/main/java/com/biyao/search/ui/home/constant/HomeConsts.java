package com.biyao.search.ui.home.constant;

public class HomeConsts {

    // 远程下载地址
    public final static String REMOTE_CONF_PATH = "http://conf.nova.biyao.com/search/ui/conf/";
    // 首页楼层配置文件
    public final static String HOME_FLOOR_CONF_URL=REMOTE_CONF_PATH + "home_floor.conf";
    // 首页楼层配置文件2 实验
    public final static String HOME_FLOOR_CONF_URL2=REMOTE_CONF_PATH + "home_floor2.conf";

    // 首页楼层配置文件3 推荐跳转的楼层配置
    public final static String HOME_FLOOR_CONF_REC_URL3=REMOTE_CONF_PATH + "home_floor3.conf";
    
    // 首页楼层配置文件4 推荐跳转的楼层配置
    public final static String HOME_FLOOR_CONF_REC_URL4=REMOTE_CONF_PATH + "home_floor4.conf";
    
    // 本地配置文件路径
    public final static String CONF_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "/conf/";
    // 首页楼层配置文件本地地址
    public final static String HOME_FLOOR_CONF_PATH=CONF_PATH + "home_floor.conf";
    // 首页楼层配置文件本地地址2 实验
    public final static String HOME_FLOOR_CONF_PATH2=CONF_PATH + "home_floor2.conf";
    
    // 首页楼层配置文件3 推荐跳转的本地楼层配置
    public final static String HOME_FLOOR_CONF_REC_PATH3=CONF_PATH + "home_floor3.conf";
    
    // 首页楼层配置文件4 推荐跳转的本地楼层配置
    public final static String HOME_FLOOR_CONF_PERSONAL_PATH4=CONF_PATH + "home_floor4.conf";
    
    // 实验配置exp远程地址
    public final static String EXP_CONF_URL=REMOTE_CONF_PATH + "exp.conf";
    // 实验配置layer远程地址
    public final static String LAYER_CONF_URL=REMOTE_CONF_PATH + "layer.conf";
    
    // search-ui实验exp
    public final static String EXPIREMENT_EXP_PATH=CONF_PATH + "exp.conf";
    // search-ui实验layer
    public final static String EXPIREMENT_LAYER_PATH=CONF_PATH + "layer.conf";

    /**
     * 副标题颜色，全部使用#BBBBBB
     */
    public final static String SUB_TITLE_COLOR = "#BBBBBB";

    /**
     * 标题默认颜色
     */
    public final static String DEFAULT_TITLE_COLOR = "#4A4A4A";

    public static class ModelTypeConst{
        public static final String TITLE_LINE = "titleline";
        public static final String SINGLE_LINE = "singleline";
        public static final String DOUBLE_DUP = "doubledup";
        public static final String DOUBLE_UNFILL = "doubleunfill";
        public static final String DOUBLE_FILL = "doublefill";
        public static final String DOUBLE_LEFT = "doubleleft";
        public static final String DOUBLE_RIGHT = "doubleright";
        public static final String TRIPLE = "triple";
        public static final String TRIPLE_GAP = "triplegap";
        public static final String FOUR_FOLD = "fourfold";
        public static final String BLOCK_LINE = "blockline";
        public static final String SPECIAL = "special";

        public static boolean validModelType(String modelType){
            if (TITLE_LINE.equals(modelType)) return true;
            if (SINGLE_LINE.equals(modelType)) return true;
            if (DOUBLE_DUP.equals(modelType)) return true;
            if (DOUBLE_UNFILL.equals(modelType)) return true;
            if (DOUBLE_FILL.equals(modelType)) return true;
            if (DOUBLE_LEFT.equals(modelType)) return true;
            if (DOUBLE_RIGHT.equals(modelType)) return true;
            if (TRIPLE.equals(modelType)) return true;
            if (TRIPLE_GAP.equals(modelType)) return true;
            if (FOUR_FOLD.equals(modelType)) return true;
            if (BLOCK_LINE.equals(modelType)) return true;
            if (SPECIAL.equals(modelType)) return true;
            return false;
        }
    }

    public final static int PARAMETER_ERROR_CODE = 20001;
    public final static String PARAMETER_ERROR_MESSAGE = "parameter error";
    public final static int SYSTEM_ERROR_CODE = 10001;
    public final static String SYSTEM_ERROR_MESSAGE = "system error";
    
    public final static int PARAMETER_ERROR_TO_OLD = 1;
    public final static String PARAMETER_ERROR_TO_OLD_MESSAGE = "parameter error ,redirect to old homepage";
    public final static String EXP_TO_OLD = "分流跳转旧页面";
}
