package com.neo.service;

import com.neo.util.Cache;
import com.neo.util.PropertiesUtils;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class DataSourceService {

    public   Map<String,Object> getAllDataSource() {
        Map<String, Map<String,String>> source = Cache.getDataSource();
       return  getReult(source);
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

            list.add(newMap);
        }
        reuslt.put("rows",list);
        reuslt.put("total",source.size());
        return reuslt;
    }


    public static void main(String[] args) {
       new DataSourceService(). getAllDataSource();
    }

    public Map<String,Object> addDatasource(Map<String,Object> data,String url_name) {
        Map<String,String> convert = new LinkedHashMap<>();
        data.remove("url_name");
        for (String key:data.keySet() ) {
            convert.put(url_name+"."+key,String.valueOf(data.get(key)));
        }
        PropertiesUtils.addProperty(convert);
        return getReult("200","新增成功！");
    }


    private   Map<String,Object> getReult(String code,String msg) {
        Map<String,Object> reuslt = new LinkedHashMap<>();
        reuslt.put("code",code);
        reuslt.put("msg",msg);
        return reuslt;
    }

}
