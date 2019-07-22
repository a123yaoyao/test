package com.neo.util;

/**
 * @Auther: Administrator
 * @Date: 2019/2/18/018 11:18
 * @Description:
 */

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public  class CollectionUtil {

    public static List<List<Map<String,Object>>> splitList(List<Map<String,Object>> list , int groupSize){
        return  Lists.partition(list, groupSize);
    }

    // list类型 转String
    public static String ListToString(List<?> list) throws SQLException, IOException {
       return  Joiner.on(",").join(list);
    }

    // String类型 转 list
    public static List <String>  stringToList(String input) {
        return  Splitter.on(",").trimResults().splitToList(input.replace("[","").replace("]",""));
    }

    /**
     * 解析json
     *
     * @param tbCollection
     * @return
     */
    public static List<Map<String, Object>> getParamList(String tbCollection, String key)throws Exception {
        JsonParser jsonParser = new JsonParser();
        JsonObject jo = (JsonObject) jsonParser.parse(tbCollection);
        JsonArray jsonArr = jo.getAsJsonArray(key);
        Gson googleJson = new Gson();
        return googleJson.fromJson(jsonArr, ArrayList.class);
    }



}
