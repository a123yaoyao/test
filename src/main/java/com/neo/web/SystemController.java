package com.neo.web;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/system/table")
public class SystemController {

    @RequestMapping("/list")
    @CrossOrigin(origins = "*")
    @ResponseBody
    public String list() {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("total",305);
        data.put("pages",31);
        data.put("size",10);
        List<Map<String,Object>> record = new LinkedList<>();
        Map<String,Object> map = new HashMap<>();
        map.put("TB_NAME","t_hd_reord");
        map.put("id","111");

        record.add(map);
        data.put("current",1);
        data.put("records",record);
        String msg= "操作成功";
        Integer status = 1;
        Long timestamps = 1600693345605l;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("msg",msg);
        jsonObject.put("data",data);
        jsonObject.put("status",status);
        //jsonObject.put("timestamps",timestamps);




        return jsonObject.toJSONString();
    }

}
