package com.neo.util;

/**
 * @Auther: Administrator
 * @Date: 2019/6/26/026 16:27
 * @Description:
 */
public  class SqlTools {


    /**
     * 分页查询
     * @param tbName
     * @param startIndex
     * @param maxIndex
     * @return
     */
    public static String queryDataPager(String tbName,int startIndex, int maxIndex){
      String   querySql =" SELECT * FROM  ( SELECT A.*, ROWNUM RN  FROM (SELECT * FROM ("+tbName+")t ) A  " +
              "WHERE ROWNUM <= " + maxIndex+
              ")  \n" +
              "WHERE RN > "+startIndex;
       return querySql;
  }

}
