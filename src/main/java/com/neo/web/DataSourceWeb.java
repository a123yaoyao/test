package com.neo.web;

import com.alibaba.fastjson.JSONObject;
import com.neo.service.DataSourceService;
import com.neo.service.LargeTbService;
import com.neo.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class DataSourceWeb {


    @Autowired
    DataSourceService dataSourceService;

    @RequestMapping("/getAllDataSource")
    @ResponseBody
    public Map<String, Object> getAllDataSource() {
        return dataSourceService.getAllDataSource();
    }


    @RequestMapping("/addDatasource")
    @ResponseBody
    public Map<String, Object> addDatasource(@RequestBody Map<String,Object> data) {
        return dataSourceService.addDatasource(data);
    }

}
