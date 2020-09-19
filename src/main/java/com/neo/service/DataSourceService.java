package com.neo.service;

import com.alibaba.fastjson.JSONObject;
import com.neo.util.Cache;
import com.neo.util.DsConfig;
import com.neo.util.PropertiesUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class DataSourceService {

    public   Map<String,Object> getAllDataSource() {
        Map<String,Object> source = Cache.getDataSource();
       return  getResMap(source);
    }

    private Map<String,Object> getResMap(Map<String,Object> source) {
        Map<String,Object> reuslt = new LinkedHashMap<>();//返回结果集
        List<Map<String,String> >  list = new LinkedList<>();
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


    private   Map<String,Object> getReult(Map<String,Map<String,String>> source) {
        Map<String,Object> reuslt = new LinkedHashMap<>();
        List<Map<String,String> >  list = new LinkedList<>();
         for (String key:source.keySet()) {
            Map<String,String> old =  source.get(key);
           Map<String,String> newMap =  new LinkedHashMap<>();
             for (String oldKey:old.keySet() ) {
                 newMap.put(oldKey.substring(oldKey.lastIndexOf(".")+1),old.get(oldKey));
             }
             newMap.put("url_name",key);
            list.add(newMap);
        }
        reuslt.put("rows",list);
        reuslt.put("total",source.size());
        return reuslt;
    }


    public static void main(String[] args) {
       new DataSourceService(). getAllDataSource();
    }

    public Map<String,Object> addDatasource(Map<String,Object> data) {
        Map<String,Object> convert = new LinkedHashMap<>();
        convert.put(data.get("url_name")+"",data);
        DsConfig.addProperty(convert);
        return getReult("200","新增成功！");
    }


    private   Map<String,Object> getReult(String code,String msg) {
        Map<String,Object> reuslt = new LinkedHashMap<>();
        reuslt.put("code",code);
        reuslt.put("msg",msg);
        return reuslt;
    }

}
