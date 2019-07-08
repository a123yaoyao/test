package com.neo.util;

import java.util.List;
import java.util.Map;

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

    /**
     * 根据表名和list数据 返回插入sql
     * @param tbName
     * @param newData
     * @return
     */
    public static String getInsertSql(String tbName, List<Map<String, Object>> newData) {
        StringBuilder sql = new StringBuilder();
        Map<String,Object> m= newData.get(0);
        sql.append("insert into "+tbName+" (");
        for (Map.Entry<String, Object> mm:  m.entrySet()) {
            if ("RN".equals(mm.getKey())){
                continue;
            }
            sql .append(mm.getKey()+",") ;
        }
        sql.deleteCharAt(sql.length()-1);
        sql .append(" ) values (");
        for (int i=0;i<m.size();i++) {
            sql .append(" ?,");
        }
        sql.deleteCharAt(sql.length()-1);
        sql .append(" ) ");
        return sql.toString();
    }


}
