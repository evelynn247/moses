package com.biyao.search.ui.model;

/**
 * @Auther: sunbaokui
 * @Date: 2019/5/22 14:45
 * @Description: 用户状态
 */
public class NewGuyAccessStatus {

    //是否是新客
    private Boolean isNewUser;
    //是否能访问新手专享
    private Boolean canAccessNewGuyBenefit;

    public NewGuyAccessStatus() {
    }

    public NewGuyAccessStatus(Boolean isNewUser, Boolean canAccessNewGuyBenefit) {
        this.isNewUser = isNewUser;
        this.canAccessNewGuyBenefit = canAccessNewGuyBenefit;
    }

    public Boolean getNewUser() {
        return isNewUser;
    }

    public void setNewUser(Boolean newUser) {
        isNewUser = newUser;
    }

    public Boolean getCanAccessNewGuyBenefit() {
        return canAccessNewGuyBenefit;
    }

    public void setCanAccessNewGuyBenefit(Boolean canAccessNewGuyBenefit) {
        this.canAccessNewGuyBenefit = canAccessNewGuyBenefit;
    }
}
