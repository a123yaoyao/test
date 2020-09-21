package com.neo.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: Administrator
 * @Date: 2019/2/19/019 17:33
 * @Description:
 */
public class StringUtils {

    public static void main(String[] args) {
        String aaa ="作业 \"EAFBIM\".\"SYS_EXPORT_SCHEMA_01\" 因致命错误于 18:26:29 停止";
        if (aaa.contains("\"")){
            System.out.println(111111);
        }
        String bbb =aaa.replace("\"","");
        System.out.println(bbb);
    }


    // Clob类型 转String
    public static String ClobToString(Clob clob)  {
        String ret = "";
        Reader read= null;
        BufferedReader br =null;
        try {
            read = clob.getCharacterStream();
             br = new BufferedReader(read);
            String s = br.readLine();
            StringBuffer sb = new StringBuffer();
            while (s != null) {
                sb.append(s);
                s = br.readLine();
            }
            ret = sb.toString();
        } catch (SQLException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        finally {

            if(br != null ){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(read != null){
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        return ret;
    }

    /**
     * Blob字段的通用转换
     * 注意可能出现乱码
     * @return 转好的字符串，
     * **/
    public static String BlobToString(Blob blob ) throws SQLException {
        StringBuffer str=new StringBuffer();
        //使用StringBuffer进行拼接
        InputStream in=null;//输入字节流
        try {
            in = blob.getBinaryStream();
            //一般接下来是把in的字节流写入一个文件中,但这里直接放进字符串
            byte[] buff=new byte[(int) blob.length()];
            //      byte[] buff=new byte[1024];
            //    byte[] b = new byte[blob.getBufferSize()];
            for(int i=0;(i=in.read(buff))>0;){
                str=str.append(new String(buff));
            }
            return str.toString();


        }catch (Exception e) {
            e.printStackTrace();
        } finally{
            try{
                in.close();
            }catch(Exception e){
                System.out.println("转换异常");
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *
     * @param map
     * @return
     * @throws SQLException
     */
    public static String MapToString(Map<String, Object> map )  {
        return  new Gson().toJson(map);
    }

    /**
     * 解析json
     *
     * @param tbCollection
     * @return
     */
    private List<Map<String, Object>> getParamList(String tbCollection, String key) {
        JsonParser jsonParser = new JsonParser();
        JsonObject jo = (JsonObject) jsonParser.parse(tbCollection);
        JsonArray jsonArr = jo.getAsJsonArray(key);
        Gson googleJson = new Gson();
        return googleJson.fromJson(jsonArr, ArrayList.class);
    }

    //转2进制
    public static String toBinary(String str){
        //把字符串转成字符数组
        char[] strChar=str.toCharArray();
        String result="";
        for(int i=0;i<strChar.length;i++){
            result +=Integer.toBinaryString(strChar[i])+ " ";
        }
        return result;
    }

    //转16进制
    public static String toOctal(String str){
        //把字符串转成字符数组
        char[] strChar=str.toCharArray();
        String result="";
        for(int i=0;i<strChar.length;i++){
            result +=Integer.toOctalString(strChar[i])+ "";
        }
        return result;
    }

    //转8进制
    public static String toHex(String str){
        //把字符串转成字符数组
        char[] strChar=str.toCharArray();
        String result="";
        for(int i=0;i<strChar.length;i++){
            result +=Integer.toHexString(strChar[i])+ "";
        }
        return result;
    }


    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean containsWhitespace(String str) {
        return str != null && str.codePoints().anyMatch(Character::isWhitespace);
    }

}




