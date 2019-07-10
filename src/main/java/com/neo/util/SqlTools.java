package com.neo.util;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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

    /**
     * 获取创建表的sql
     * @param tbName
     * @param tableStructure
     */
    public static String getCreateTableSql(String tbName, List<Map<String, Object>> tableStructure) throws IOException, SQLException {
        String primarySql ="";
        String dataType = null; //数据类型
        String columnName =null ;//列名
        List<String> primaryList =new ArrayList<>();
        String sql =" CREATE TABLE "+tbName+" (\n";
        int k=0;
        for (Map<String,Object> map :tableStructure ) {
            dataType = map.get("DATA_TYPE")+"" ;
            columnName  = map.get("COLUMN_NAME")+"" ;
            k++;
            if ("P".equals(map.get("IS_PRIMARY")+"")){
                primaryList.add(columnName);
            }
            sql+= " "+map.get("COLUMN_NAME") +" "+dataType ;
            if (null!= map.get("DATA_LENGTH") ){
                if ("DATE".equals(dataType)||"CLOB".equals(dataType)
                        ||"BLOB".equals(dataType)  ||"LONG RAW".equals(dataType)
                        ) sql+= " " ;
                else sql+= " (" + map.get("DATA_LENGTH")+")";
            }
            if ("N".equals(map.get("NULLABLE"))){
                sql+=" NOT NULL "  ;
            }
            if ("U".equals(map.get("IS_UNIQUE")+"")){
                sql+=" UNIQUE "  ;
            }
            if (k==tableStructure.size() && primaryList.size() == 0 ){
                sql +=" \n";
            }else  sql +=" ,\n";
        }
        if (primaryList.size() > 0 ){
            primarySql = " PRIMARY KEY("+CollectionUtil.ListToString(primaryList)+")";
        }
        sql += primarySql +"   ) ";
        return sql ;
    }



}
