package com.biyao.search.ui.home.dubbo.impl;

import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class Test {
    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        SimpleDateFormat sdf1 = new SimpleDateFormat("MMddHHmmssSS");

        String platfrom = "7";
        String sTime = sdf.format(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-","").substring(0,15);
        String extra = "0000000";
        uuid = platfrom+sTime+uuid+extra;
        System.out.println("uuid = " + uuid);
        String pvID = uuid.substring(0,7)+"-"+sdf1.format(System.currentTimeMillis())+"-"+UUID.randomUUID().toString().replace("-","").substring(0,9);
        System.out.println("pvID = " + pvID);
    }
}
