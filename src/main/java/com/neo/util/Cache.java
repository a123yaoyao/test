package com.neo.util;

import com.alibaba.fastjson.JSONObject;

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

}
