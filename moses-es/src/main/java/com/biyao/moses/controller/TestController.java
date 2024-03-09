package com.biyao.moses.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: moses-parent-online
 * @description:
 * @author: changxiaowei
 * @create: 2021-09-17 10:46
 **/
@RestController
@Slf4j
public class TestController {
    @GetMapping("/testonline")
    @ResponseBody
    public Map<String,String> testonline(){
        Map<String,String> result = new HashMap<>();
        result.put("state","yes");
        log.error("ceshi");
        return result;
    }
}
