package com.test;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import sun.misc.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class JSONTest {



    @Test
    public void testMethod() {
        BufferedReader br = null;
        String path = "datasource.json";
        try {
            br = new BufferedReader(new InputStreamReader(JSONTest.class.getClassLoader().getResourceAsStream(path)));
            String line = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line + "\r\n");
                line = br.readLine();
            }

            JSONObject json = JSONObject.parseObject(sb.toString());
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
