package com.neo.util;

import com.alibaba.fastjson.JSONObject;
import org.codehaus.groovy.tools.StringHelper;
import org.codehaus.groovy.util.StringUtil;
import org.junit.platform.commons.util.StringUtils;

import java.util.*;

public class Cache {

    /**
     * 键值对集合
     */
    private static Map<String, Map<String,String>> datasource = new HashMap<>();

    private static JSONObject datasourceJson = new JSONObject();

    private static Map<String,String> drivers = new LinkedHashMap<String,String>(){
        private static final long serialVersionUID = 1L;
        {
            put("oracle", "oracle.jdbc.driver.OracleDriver");
            put("mysql", "com.mysql.jdbc.Driver");
            put("SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver");//（2005版本及以后）
            put("SQLServerOld", "com.microsoft.jdbc.sqlserver.SQLServerDriver");// （2000版本）
            put("PostgreSQL", "org.postgresql.Driver");// （2000版本）
            put("Sybase", "com.sybase.jdbc3.jdbc.SybDriver");// （2000版本）
        }
    };

    static {
//        datasource = PropertiesUtils.getMap();
          datasourceJson = DsConfig.getJSONObject();
    }

    public static JSONObject getDataSource(){
        return datasourceJson;
    }

    public static Set<String>  getDriverClassName(){
        Set<String> set = new LinkedHashSet<>();
        Map<String,Object> temp = (Map)datasourceJson;
        Map<String,Object> tempMap = null;
        for (String key: temp.keySet()) {
            tempMap = (Map<String, Object>) temp.get(key);
            set.add(tempMap.get("driverClassName")+"");
        }
        return set;
    }

    public static Map  getUrlNameMapperDriver(){
        Map<String,String> result = new LinkedHashMap<>();
        Map<String,Object> temp = (Map)datasourceJson;
        Map tempMap = null;
        for (String key: temp.keySet()) {
            tempMap = (Map)temp.get(key);
            result.put(key,tempMap.get("driverClass")+"");
        }
        return result;
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
            if (!drivers.containsValue(data.get("driverCLass")+"")) throw new Exception("驱动错误");
        }
    }

    public static Map getDataSourceMap() {
        return (Map)datasourceJson;
    }
}
