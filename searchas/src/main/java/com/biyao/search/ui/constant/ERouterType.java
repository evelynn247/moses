package com.biyao.search.ui.constant;

public enum ERouterType {

    DALIYPAGE("每日上新", 1),
    SUPPLIERPAGE("商家店铺", 2),
    TOPICLIST("专题列表", 3),
    TOPICDETAIL("专题详情", 4),
    SEARCH("搜索中间", 5),
    RECOMMEND("推荐中间",6);

    private String name;
    private Integer num;

    private ERouterType(String name, Integer num) {
        this.name = name;
        this.num = num;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNum() {
        return this.num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public static boolean containName(String name) {
        ERouterType[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ERouterType platform = var1[var3];
            if (platform.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public static Integer getValueByName(String name) {
        ERouterType[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ERouterType platform = var1[var3];
            if (platform.getName().equals(name)) {
                return platform.getNum();
            }
        }

        return null;
    }

    public static ERouterType getByName(String name) {
        ERouterType[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            ERouterType item = var1[var3];
            if (item.name.equals(name)) {
                return item;
            }
        }

        return null;
    }
    
    public static ERouterType getByNum(Integer num) {
        ERouterType[] enums = values();
        for (ERouterType eRouterType : enums) {
			if (eRouterType.num == num) {
				return eRouterType;
			}
		}
        return null;
    }
}
