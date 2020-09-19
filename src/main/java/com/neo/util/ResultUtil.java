package com.neo.util;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResultUtil {


    public Map<String,Object> getResErr(Exception e) {
        Map<String,Object> reuslt = new LinkedHashMap<>();
        reuslt.put("code","500");
        reuslt.put("msg",e.getMessage());
        return reuslt;
    }


    public Map<String,Object> getReult(String code, String msg) {
        Map<String,Object> reuslt = new LinkedHashMap<>();
        reuslt.put("code",code);
        reuslt.put("msg",msg);
        return reuslt;
    }

    public Map<String,Object> getResMap(Map<String, Object> source) {
        Map<String,Object> reuslt = new LinkedHashMap<>();//返回结果集
        List<Map<String,String> > list = new LinkedList<>();
        Map<String,String> newMap =  null;
        for (String key:source.keySet()) {
            newMap = (Map<String, String>) source.get(key);
            newMap.put("url_name",key);
            list.add(newMap);
        }
        reuslt.put("rows",list);
        reuslt.put("total",source.size());
        return reuslt;
    }



}
