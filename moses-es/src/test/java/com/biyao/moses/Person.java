package com.biyao.moses;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-15 16:56
 **/
@Setter
@Getter

public class Person {
    private  Integer id;
    private String name;
    private Integer age;
    private Date birthday;

    public Person(Integer id,String name,Integer age,Date birthday){
        this.id =id;
        this.age=age;
        this.name =name;
        this.birthday =birthday;
    }

}
