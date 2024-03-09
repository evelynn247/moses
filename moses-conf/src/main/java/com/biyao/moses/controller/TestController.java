package com.biyao.moses.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Api("测试接口")
public class TestController {

    @GetMapping("/testonline")
    @ResponseBody
    public Map<String,String> testonline(){

        Map<String,String> result = new HashMap<>();
        result.put("state","yes");
        return result;

    }

}