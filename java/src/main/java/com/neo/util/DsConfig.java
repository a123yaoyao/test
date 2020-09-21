package com.neo.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.test.JSONTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class DsConfig {



    private static final Logger logger = LoggerFactory.getLogger(DsConfig.class);
    static String path = "datasource.json";

    private static LinkedHashMap ds = new LinkedHashMap();

    static {
        loadJson();
    }

    DsConfig(){

    }


    synchronized static private void loadJson(){
        logger.info("开始加载datasource.json文件内容.......");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(JSONTest.class.getClassLoader().getResourceAsStream(path)));
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line + "\r\n");
                line = br.readLine();
            }

            ds = JSONObject.parseObject(sb.toString(),LinkedHashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("加载"+path+"n文件内容完成...........");
        logger.info(path+"文件内容：" + ds);
    }

    static JSONObject getJSONObject(){
        JSONObject resultJson = new JSONObject();
        Iterator it = ds.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            resultJson.put(key, ds.get(key));
        }
        return resultJson;
    }

    static Map<String,Object> getMap(){
        return ds;
    }


    static String  getFilePath(){
        return  DsConfig.class.getClassLoader().getResource(path).getPath();
    }


    public static void addProperty(Map<String,Object> convert) {
        Set<String> set =  convert.keySet();
        for (String key :set) {
            ds.put(key,convert.get(key));
        }
        createJsonFile(ds,getFilePath());
    }


    public static boolean createJsonFile(Object jsonData, String filePath) {
        String content = JSON.toJSONString(jsonData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat);
        // 标记文件生成是否成功
        boolean flag = true;
        // 生成json格式文件
        try {
            // 保证创建一个新文件
            File file = new File(filePath);
            if (!file.getParentFile().exists()) { // 如果父目录不存在，创建父目录
                file.getParentFile().mkdirs();
            }
            if (file.exists()) { // 如果已存在,删除旧文件
                file.delete();
            }
            file.createNewFile();
            // 将格式化后的字符串写入文件
            Writer write = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            write.write(content);
            write.flush();
            write.close();
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        return flag;
    }

    public static void main(String[] args) {
        DsConfig dsConfig = new DsConfig();
        String fileName = dsConfig.getClass().getClassLoader().getResource(path).getPath();
        System.out.println(fileName);
    }

}
