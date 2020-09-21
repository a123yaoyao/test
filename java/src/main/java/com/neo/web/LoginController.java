package com.neo.web;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/account")
public class LoginController {

    @RequestMapping("/sign-in")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public String signIn() {
        String data =
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1aWQiOiIxIiwiZXhwIjoxNjAwOTk1NzQ1LCJ1c2VybmFtZSI6ImJpbXN1cGVydmlzb3IifQ.lggWR8Gc47A6rI1kEkUBy78NQ5FJ1A5QKoLNuO1FVck";
        String msg= "登录成功";
        Integer status = 2;
        Long timestamps = 1600693345605l;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg",msg);
        jsonObject.put("data",data);
        jsonObject.put("status",status);
        jsonObject.put("timestamps",timestamps);
        return jsonObject.toJSONString();
    }

    @RequestMapping("/all-permission-tag")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public String allPermissionTag() {
        List<String> data = new ArrayList<>();
        data.add("home");
        data.add("datasource");
        data.add("datasource:manager");
        data.add("system:role:add");
        data.add("system:user:remove");
        data.add( "system:user:remove");
        data.add( "system:resource:list");
        data.add( "system:role:list");
        data.add( "system:user:list");
        data.add( "system:user:resetPassword");
        data.add( "system:user:lock");
        data.add( "system:user:unlock");
        data.add( "system:log");
        data.add( "system:log:list");
        data.add( "system:user:update");
        data.add( "system:user:add");
        data.add( "system:role:update");
        data.add( "system");
        data.add( "system:user");
        data.add( "system:role");
        data.add( "system:resource");
        data.add( "system:resource:add");
        data.add( "system:resource:update");
        data.add("system:resource:remove");
        data.add( "system:role:remove");
        data.add("system:log:remove");
        String msg= "登录成功";
        Integer status = 2;
        Long timestamps = 1600693345605l;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg",msg);
        jsonObject.put("data",data);
        jsonObject.put("status",status);
        jsonObject.put("timestamps",timestamps);
        return jsonObject.toJSONString();
    }

}
