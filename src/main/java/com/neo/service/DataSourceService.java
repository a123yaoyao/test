package com.neo.service;

import com.neo.util.Cache;
import com.neo.util.DsConfig;
import com.neo.util.ResultUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DataSourceService extends ResultUtil{

    /**
     * 查询所有数据源
     * @return
     */
    public   Map<String,Object> getAllDataSource() {
        Map<String,Object> source = Cache.getDataSource();
       return  getResMap(source);
    }

    /**
     * 添加数据源
     * @param data
     * @return
     */
    public Map<String,Object> addDatasource(Map<String,Object> data) {
        try {
            Cache.validateDataSource(data);//校验数据合法性
        } catch (Exception e) {
            return new ResultUtil().getResErr(e);
        }
        Map<String,Object> convert = new LinkedHashMap<>();
        convert.put(data.get("url_name")+"",data);
        DsConfig.addProperty(convert);
        return getReult("200","新增成功！");
    }


    public Map<String,Object> getDataSourceByName(Map<String,Object> data) {
        Map<String,Object> source = Cache.getDataSource();
        if (source.containsKey(data.get("url_name"))){
            return (Map<String, Object>) source.get(data.get("url_name")+"");
        }
        return null;
    }
}
