package com.neo.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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

    public static Map<String,Object> getConditionSql(String tbName,String uniqueConstraint,String masterDataSource) throws SQLException {
        Map<String,Object> result =new HashMap<>();

        List<String> list = null;
        String sql =" where NOT EXISTS  ( select 1 from  "+tbName+" WHERE 1=1 ";
        Map<Integer,String> indexCloumnMapper =new LinkedHashMap<>();
        JSONArray constraint = JSONArray.parseArray(uniqueConstraint);
        JSONObject jsonObject =null;

        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);
            if (tbName.equals(jsonObject.get("table")+"")){
                list = (List<String>)jsonObject.get("column");
                int mk=1;
                for (String columnName: list) {
                    indexCloumnMapper.put(mk,columnName);
                    mk++;
                    sql += " and " + columnName+" is not null and "+columnName + " =  ? ";
                }
                sql +=" )";
                result.put("tempSql",sql);
                result.put("mapper",indexCloumnMapper);
            }
        }

        if (null!= list)return result;
        if (null == list){//判断该表是否存在eaf_Id
            int i = hasEAFId(tbName,masterDataSource);
            list =new ArrayList<>();
            Map<String,Object> map =new HashMap<>();
            if (i==1) {
                List<String> columns = new ArrayList<>();
                sql += " and EAF_ID is not null and EAF_ID =  ? )";
                indexCloumnMapper.put(1,"EAF_ID");
            }
        }
        result.put("tempSql",sql);
        result.put("mapper",indexCloumnMapper);
        return result;
    }

    public static int hasEAFId(String tbName,String masterDataSource) throws SQLException {
        String  sql="select count(0) as TABLE_NUMS  from user_tab_columns   \n" +
                "where UPPER(column_name)='EAF_ID' AND TABLE_NAME = '"+tbName+"'";
        List<Map<String,Object>> list =new JDBCUtil(masterDataSource).excuteQuery(sql,new Object[][]{});
        String count =list.get(0).get("TABLE_NUMS")+"";
        if ("1".equals(count) ) return 1;
        else return 0;
    }



}
