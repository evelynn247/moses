package com.biyao.search.ui.enums;
import java.util.Arrays;

/**
 * @Auther: sunbaokui
 * @Date: 2019/5/22 14:09
 * @Description: 展示开关
 */
public enum DisplaySwitchEnum {

    ALL_NEW_USER("1", "全部新客") {
        @Override
        public boolean isCanAccess(VisitorTypeEnum visitorTypeEnum) {
            if (visitorTypeEnum == VisitorTypeEnum.NEW_VISITOR || visitorTypeEnum == VisitorTypeEnum.OLD_VISITOR) {
                return true;
            }
            return false;
        }
    }, NEW_VISITOR("2", "新访客") {
        @Override
        public boolean isCanAccess(VisitorTypeEnum visitorTypeEnum) {
            if (visitorTypeEnum == VisitorTypeEnum.NEW_VISITOR) {
                return true;
            }
            return false;
        }
    }, OLD_VISITOR("3", "老访客") {
        @Override
        public boolean isCanAccess(VisitorTypeEnum visitorTypeEnum) {
            if (visitorTypeEnum == VisitorTypeEnum.OLD_VISITOR) {
                return true;
            }
            return false;
        }
    };

    private String code;
    private String desc;

    DisplaySwitchEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public abstract boolean isCanAccess(VisitorTypeEnum visitorTypeEnum);

    public static DisplaySwitchEnum getByCode(String code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst().orElse(null);
    }

}
