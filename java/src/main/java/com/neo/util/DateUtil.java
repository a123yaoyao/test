package com.neo.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Auther: Administrator
 * @Date: 2019/2/18/018 16:52
 * @Description:
 */
public class DateUtil {

    public static void main(String[] args) {
        System.out.println( 1005%1000 == 0 ? (1005/1000) : (1005/1000)+1);
    }
    public static String splitDate(String orcDate ){

        return  orcDate.substring(0,orcDate.indexOf("."));
    }

    /**
     *
     * Description: 日期格式
     * @param date 日期
     * @param pattern 格式
     * @return 日期字符串
     * @see
     */
    public static String formatDate(Date date, String pattern) {
        if (date==null) {return null;}
        return new SimpleDateFormat(pattern).format(date);
    }

    /**
     * @param
     *
     * */
    public static java.sql.Date strToDate(String strDate) {
        String str = strDate;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date d = null;
        try {
            d = format.parse(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        java.sql.Date date = new java.sql.Date(d.getTime());
        return date;
    }



    public static java.sql.Timestamp strToTimeStamp(String strDate) {
        String str = strDate;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        java.util.Date d = null;
        try {
            d = format.parse(str);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new java.sql.Timestamp(d.getTime());

    }

    public static  String getRocord(long start ,long end ) {
        String cost ="";
        long time =end - start ;
        if (time >=0l && time<1000l){
            cost += time+"毫秒";
        }else if(time>=1000 && time<60000){
            cost += time/1000 +"秒";
        }else {
            cost += time/60000 +"分钟";
        }
       return cost ;
    }


    public static Object DateToStr(Date v) {
      SimpleDateFormat sdf =new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
      return sdf.format(v);
    }
}
