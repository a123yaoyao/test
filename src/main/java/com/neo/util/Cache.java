package com.neo.util;

import java.util.HashMap;
import java.util.Map;

public class Cache {

    /**
     * 键值对集合
     */
    private static Map<String, Map<String,String>> datasource = new HashMap<>();

    static {
        datasource = PropertiesUtils.getMap();
    }

    public static Map<String, Map<String,String>> getDataSource(){
        return datasource;
    }

}
