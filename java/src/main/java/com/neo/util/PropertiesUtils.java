package com.neo.util;

import java.io.*;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesUtils {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);
    private static Properties props;
    static{
        loadProps();
    }

    synchronized static private void loadProps(){
        logger.info("开始加载properties文件内容.......");
        props = new Properties();
        BufferedReader bufferedReader = null;

        try {
            // 要加载的属性文件
           // in = PropertiesUtils.class.getClassLoader().getResourceAsStream("D:\\project\\my\\test\\src\\main\\resources\\datasource.properties");
            bufferedReader = new BufferedReader(new FileReader("D:\\project\\my\\test\\src\\main\\resources\\datasource.properties"));

            props.load(bufferedReader);
        } catch (FileNotFoundException e) {
            logger.error("jdbc.properties文件未找到");
        } catch (IOException e) {
            logger.error("出现IOException");
        } finally {
            try {
                if(null != bufferedReader) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                logger.error("vas.properties文件流关闭出现异常");
            }
        }
        logger.info("加载properties文件内容完成...........");
        logger.info("properties文件内容：" + props);
    }

    public static String getProperty(String key){
        if(null == props) {
            loadProps();
        }
        return props.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        if(null == props) {
            loadProps();
        }
        return props.getProperty(key, defaultValue);
    }

    public static  Set<String> getSet() {
        return props.stringPropertyNames();
    }

    public static  Map<String,String> getPropertiesMap() {
        Set<String> set  = PropertiesUtils.getSet();
        Set<String> newSet = PropertiesUtils.sort(set);
        Map<String,String> result = new LinkedHashMap<>();
        for (String key :newSet ) {
            result.put(key,props.getProperty(key));
        }
        return result;
    }

    static Set<String> sort(Set<String> set){
        Set<String> sortSet = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);//降序排列
            }
        });
        sortSet.addAll(set);
        return sortSet;
    }


    public static Map<String,Map<String,String>> getMap() {
        Map<String,Map<String,String>> datasourceMap = new LinkedHashMap<>();
        Map<String,String> detailSource =null;
        Set<String> datasourceSeet = PropertiesUtils.getSet();
        Set<String> sortSet = new TreeSet<String>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);//降序排列
            }
        });
        sortSet.addAll(datasourceSeet);
        String key = null;
        int i= 0;
        for (String name:sortSet) {
            if (i ==0 )  detailSource = new LinkedHashMap<>();
            key = name.substring(0, name.indexOf("."));
            detailSource.put(name,props.getProperty(name));
            i++;
            if (i==4) {
                datasourceMap.put(key,detailSource);
                i=0;
            }
        }
        return datasourceMap;
    }

    /**
     * properties文件新增属性
     * @param data
     */
    public static void  addProperty(Map<String,String> data) {
        Map<String,String> result = PropertiesUtils.getPropertiesMap();
        for (String key :data.keySet()) {
            result.put(key,data.get(key));
        }
        PropertiesUtils.reWrite(result);
    }

    /**
     * properties文件重写属性
     * @param data
     */
    public static void reWrite(Map<String,String> data) {
        try {
            if (data != null) {
                Iterator<Map.Entry<String,String>> iter = data.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String,String> entry = iter.next();
                    props.setProperty(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            OutputStream out = new FileOutputStream("D:\\project\\my\\test\\src\\main\\resources\\datasource.properties");
            props.store(out, null);
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Map<String,String> result = PropertiesUtils.getPropertiesMap();

        result.put("test.url","jdbc:oracle:thin:@127.0.0.1:1521/orcl.168.3.5");
        result.put("test.username","root");
        result.put("test.password","root");
        result.put("test.driverClassName","oracle.jdbc.driver.OracleDriver");
        PropertiesUtils.reWrite(result);

        Map<String,String> result1 = PropertiesUtils.getPropertiesMap();
        System.out.println(result1);
        /*Map<String,String> data = new LinkedHashMap<>();
        data.put("hdt.username","1");
        PropertiesUtils.setProperty(data);
        System.out.println(PropertiesUtils.getSet());*/
    }
}
