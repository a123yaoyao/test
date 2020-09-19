package com.neo.util;

import com.alibaba.fastjson.JSONObject;
import org.codehaus.groovy.tools.StringHelper;
import org.codehaus.groovy.util.StringUtil;
import org.junit.platform.commons.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class Cache {

    /**
     * 键值对集合
     */
    private static Map<String, Map<String,String>> datasource = new HashMap<>();

    private static JSONObject datasourceJson = new JSONObject();

    static {
//        datasource = PropertiesUtils.getMap();
          datasourceJson = DsConfig.getJSONObject();
    }

    public static JSONObject getDataSource(){
        return datasourceJson;
    }

    public static boolean validateAddDataSource(Map<String,Object> data) throws Exception {
       Map<String,Object> dataMap = (Map) datasourceJson;
       if (data.containsKey(data.get("url_name"))){
           throw new Exception("连接名不可重复");
       }
       return true;
    }

    public static void validateUpdateDataSource(Map<String,Object> data) throws Exception {
        for (String key:data.keySet() ) {
            if (com.neo.util.StringUtils.isBlank(data.get("url_name")+""))  throw new Exception("连接名称不能为空");
            if (com.neo.util.StringUtils.isBlank(data.get("url")+""))  throw new Exception("连接不能为空");
            if (com.neo.util.StringUtils.isBlank(data.get("username")+""))  throw new Exception("连接不能为空");
            if (com.neo.util.StringUtils.isBlank(data.get("password")+""))  throw new Exception("密码不能为空");
            if (com.neo.util.StringUtils.isBlank(data.get("driverCLass")+""))  throw new Exception("驱动不能为空");


        }
    }
}
