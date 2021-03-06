package com.neo.util;


import oracle.sql.BLOB;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * @Auther: Administrator
 * @Date: 2019/3/21/021 16:01
 * @Description:
 */
public class DbUtil {

    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(JDBCUtil.class);


    Connection conn ;

    private PreparedStatement pst = null;

    private ResultSet rst = null;
    /**
     * 构造方法
     *
     * @param connection
     * 			数据库连接
     */
    public DbUtil(Connection connection) {
        conn = connection ;
    }

    public Connection getConn() {
        return conn;
    }

    /**
     * 获取结果集，并将结果放在List中
     *
     * @param sql  SQL语句
     *         params  参数，没有则为null
     * @return List
     *                       结果集
     */


    public  int  getCount(String sql, Object[] params) throws SQLException {
        // 执行SQL获得结果集
        ResultSet rs = executeQueryRS(sql, params);

        int rowCount = 0;
        try {
            if(rs.next())
            {
                rowCount=rs.getInt(1);
            }
        }catch (Exception e){
           // e.printStackTrace();
            logger.error(e.getMessage());
        }
        finally{
            pst.close();
            rs.close();
        }

        return rowCount;
    }


    public List<Map<String, Object>> excuteQuery(String sql, Object[] params) throws SQLException {
        // 执行SQL获得结果集
        ResultSet rs = executeQueryRS(sql, params);

        // 创建ResultSetMetaData对象
        ResultSetMetaData rsmd = null;

        // 结果集列数
        int columnCount = 0;
        try {
            rsmd = rs.getMetaData();

            // 获得结果集列数
            columnCount = rsmd.getColumnCount();
        } catch (SQLException e1) {
            logger.error(e1.getMessage());
        }

        // 创建List
        List<Map<String, Object>> list = new ArrayList<>();

        try {
            // 将ResultSet的结果保存到List中
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                for (int i = 1; i <= columnCount; i++) {
                    if ("RN".equals(rsmd.getColumnLabel(i))) continue;
                    map.put(rsmd.getColumnLabel(i), rs.getObject(i));
                }
                list.add(map);//每一个map代表一条记录，把所有记录存在list中
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(sql);
        } /*finally {
            if (null!= rs) rs.close();
            if (null!= pst) pst.close();
            if (null!= conn) conn.close();
        }*/

        return list;
    }

    /**
     * SQL 查询将查询结果直接放入ResultSet中
     * @param sql SQL语句
     * @param params 参数数组，若没有参数则为null
     * @return 结果集
     */
    private ResultSet executeQueryRS(String sql, Object[] params) throws SQLException {
        try {
            // 获得连接

            // 调用SQL
            pst = conn.prepareStatement(sql);

            // 参数赋值
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pst.setObject(i + 1, params[i]);
                }
            }

            // 执行
            rst = pst.executeQuery();

        } catch (SQLException e) {
            logger.error(e.getMessage());

        }
        return rst;
    }


    /**
     * insert update delete SQL语句的执行的统一方法 执行完关闭连接
     * @param sql SQL语句
     * @param params 参数数组，若没有参数则为null
     * @return 受影响的行数
     */
    public int executeUpdate(String sql, Object[] params) {
        // 受影响的行数
        int affectedLine = 0;

        try {
            // 获得连接
          //  conn = this.getConnection();
            // 调用SQL
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);

            // 参数赋值
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    pst.setObject(i + 1, params[i]);
                }
            }
            /*在此 PreparedStatement 对象中执行 SQL 语句，
                                          该语句必须是一个 SQL 数据操作语言（Data Manipulation Language，DML）语句，比如 INSERT、UPDATE 或 DELETE
                                          语句；或者是无返回内容的 SQL 语句，比如 DDL 语句。    */
            // 执行
            affectedLine = pst.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.fillInStackTrace();

        }/* finally {
                closeAll();
        }*/
        return affectedLine;
    }

    /**
     *  关闭数据连接
     *  /
     */
    private void closeAll(){

        try {
            if(rst !=null)rst.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        try {
            if(pst !=null)pst.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        try {
            if(conn !=null)conn.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }


/*
    public int insert(String tbName, List<Map<String, Object>> newData,List<Map<String, Object>> tbstruct,boolean isUseBatch) throws Exception {
        int len =0;
        try{
            if (isUseBatch){
                len =  batchInsertJsonArry(tbName,newData,tbstruct);
            }else len =  odinaryInsert(tbName,newData,tbstruct);
        }catch (Exception e){
           if(isUseBatch) len =  odinaryInsert(tbName,newData,tbstruct);
        }
       return len ;


    }
*/

    private Map<String,Object> odinaryInsert(String tbName, List<Map<String,Object>> dat, List<Map<String,Object>> tbstruct) {
        long start = System.currentTimeMillis();
        boolean failureFlag =false;
        Map<String,Object> retrunMap =new HashMap<>();
        String  sql= sql =  getInsertSql( tbName,  dat);;
        int[] result= null;
        PreparedStatement pst = null;
        Map<String,Object> ma;
        String value ;
        boolean flag;
        String cloumnName;
        String dataType;
        java.sql.Date dateValue = null;
        int rows=0;
        try {
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            //result =  insertBatch(tbName , newData, pst,tbstruct);

            for (int i = 0; i <dat.size() ; i++) {
                ma = (Map<String,Object>)dat.get(i);
                int j=0;
                for (String k:ma.keySet()) {
                    value =ma.get(k)+"";

                    if ("null".equals(value.trim())) value =null;
                    flag =false;
                    for (Map<String, Object> structure:tbstruct) {
                        cloumnName =structure.get("COLUMN_NAME")+"";
                        dataType =structure.get("DATA_TYPE")+"";
                        if ( k.equals(cloumnName) && ("DATE".equals(dataType)  && value !=null )){
                            value =   value.substring(0,value.indexOf("."));
                            dateValue = DateUtil.strToDate(value);
                            flag =true;
                            break;
                        }
                        if ( k.equals(cloumnName) && ("CLOB".equals(dataType) ||"BLOB".equals(dataType)) && value !=null){
                            value =getValueByType(ma,k,dataType);
                            break;
                        }

                    }

                    if (flag)pst.setObject(j+1,dateValue);
                    else pst.setObject(j+1,value);
                    j++;
                }
                try {
                    rows +=  pst.executeUpdate();
                    conn.commit();
                    logger.info("插入"+tbName+"第"+(i+1)+"条数据成功");
                }catch (Exception e){
                    failureFlag =true ;
                    retrunMap.put("MESSAGE","EAF_ID为 "+ma.get("EAF_ID")+" 原因："+e.getMessage());
                    logger.info(""+tbName+"表插入第"+(i+1)+"条数据失败原因为 ："+e.getMessage()+" EAF_ID为"+ma.get("EAF_ID"));
                    if (!e.getMessage().contains("ORA-00001: 违反唯一约束条件")){
                        logger.error(sql.toString()+e.getMessage());
                    }

                }

            }


            long end = System.currentTimeMillis();
            logger.info(tbName+"插入了:"+rows+"条数据需要时间:"+(end - start)/1000+"s"); //批量插入需要时间:
            retrunMap.put("INSERT_COUNT",rows+"");
            if(!failureFlag) retrunMap.put("MESSAGE","执行成功");
            return retrunMap;
        } catch (Exception e) {
            if (!e.getMessage().contains("ORA-00001")){
                logger.error(sql.toString()+e.getMessage());
            }



           // e.printStackTrace();
        }/*finally {
            closeAll();
        }*/

        return retrunMap;

    }

    public Map<String,Object> batchInsertJsonArry(String tbName, List<Map<String, Object>> newData,List<Map<String, Object>> tbstruct, Map<String,Object> conditionMap) throws Exception{
        Map<String,Object> returnMap =new HashMap<>();
        long start = System.currentTimeMillis();
        String  sql= null;
        int[] result= null;
        PreparedStatement pst = null;
        try {
            sql =  getInsertSql( tbName,  newData)+ conditionMap.get("tempSql");;
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            result =  insertBatch(tbName , newData, pst,tbstruct,conditionMap);
            long end = System.currentTimeMillis();
            logger.info("批量插入了:"+newData.size()+"条数据 需要时间:"+(end - start)/1000+"s"); //批量插入需要时间:
            int len= newData.size() ;
            returnMap.put("INSERT_COUNT",len+"");
            returnMap.put("MESSAGE","执行成功");
            newData =null;
            return returnMap;
        } catch (Exception e) {
            returnMap.put("INSERT_COUNT","0");
            returnMap.put("MESSAGE",e.getMessage());
           logger.error(sql.toString()+e.getMessage());

        }
        return returnMap;
    }

    public int insertTbRecord(String tbName, List<Map<String, Object>> dat,Map<String,Integer> mapper) throws Exception{
        long start = System.currentTimeMillis();
       /* Map<String,Integer> mapper =new HashMap<>();
        Map<String,Object> map =dat.get(0);
        int k =0;
        for (String key :map.keySet()) {
            k++;
            mapper.put(key,k);
        }*/
        String  sql= null;
        int[] result= null;
        PreparedStatement pst = null;
        try {
            sql =  getInsertSql( tbName,  dat);
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            int i =0 ;
            int ik[] =null;
            for (Map m :  dat) {
                i++;
                for (String column:mapper.keySet()) {
                    pst.setString(mapper.get(column),m.get(column)==null ? null : m.get(column)+"");
                }
                pst.addBatch();
                if(i>0 && i%1000==0){
                    ik = pst.executeBatch();
                    conn.commit();
                    //清除批处理命令
                    pst.clearBatch();
                    //如果不想出错后，完全没保留数据，则可以每执行一次提交一次，但得保证数据不会重复
                }
            }
            ik = pst.executeBatch();
            conn.commit();
            pst.clearBatch();
            long end = System.currentTimeMillis();
            logger.info("记录表"+tbName+"批量插入了:"+dat.size()+"条数据 需要时间:"+(end - start)/1000+"s"); //批量插入需要时间:
            int len= dat.size() ;
            return len;
        } catch (Exception e) {
            logger.error(sql.toString()+e.getMessage());
        }
        return 0;

    }



    private String getInsertSql(String tbName, List<Map<String, Object>> newData) {
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
        sql .append(" ) ");
        //sql .append(" values (");
        sql.append(" SELECT ");

        for (int i=0;i<m.size();i++) {
            sql .append(" ?,");
        }
        sql.deleteCharAt(sql.length()-1);
        sql.append("   FROM DUAL ");
        // sql .append(" ) ");
        return sql.toString();
    }

    private int[] insertBatch(String tbName, List<Map<String, Object>> dat,PreparedStatement pst,List<Map<String, Object>> tbstruct,Map<String, Object> conditionMap) throws SQLException, IOException {
        conn.setAutoCommit(false);
        Map<String,Object> m =(Map<String,Object>)dat.get(0);
        int[] ik =null;
        String value = null;
        Map<String,Object> ma = null;
        String cloumnName = null;
        String dataType = null;
        java.sql.Date dateValue =null;
        java.sql.Timestamp timestampValue = null;
        boolean flag ;
        Map<String, String> structureMap =new LinkedHashMap<>();
        Map<Integer,String > conditionMapper = (Map<Integer, String>) conditionMap.get("mapper");

        for (Map<String, Object> structure:tbstruct) {
            cloumnName =structure.get("COLUMN_NAME")+"";
            dataType =structure.get("DATA_TYPE")+"";
            structureMap.put(cloumnName,dataType);
        }


        for (int i = 0; i <dat.size() ; i++) {
            ma = (Map<String,Object>)dat.get(i);
            int j=0;
            for (String k:ma.keySet()) {
                value =ma.get(k)+"";

                if ("null".equals(value.trim())) value =null;
                flag =false;
                dataType = structureMap.get(k);
                if ( ("DATE".equals(dataType)  && value !=null )){
                    value =   value.substring(0,value.indexOf("."));
                    //dateValue = DateUtil.strToDate(value);
                    timestampValue = DateUtil.strToTimeStamp(value);
                    flag =true;
                }
                if ( ("CLOB".equals(dataType) ||"BLOB".equals(dataType)) && value !=null){
                    value =getValueByType(ma,k,dataType);
                }
                if (flag) {
                    pst.setTimestamp(j+1,timestampValue);
                }
                else pst.setString(j+1,value);
                j++;
            }

            for (Integer k : conditionMapper.keySet()) {
                pst.setObject(k+ma.size(), ma.get(conditionMapper.get(k)));
            }

            pst.addBatch();

            if(i>0 && i%1000==0){
                ik = pst.executeBatch();
                conn.commit();
                //清除批处理命令
                  pst.clearBatch();
                //如果不想出错后，完全没保留数据，则可以每执行一次提交一次，但得保证数据不会重复
            }


        }
        ik = pst.executeBatch();
        conn.commit();
        pst.clearBatch();
        return  ik;
    }



    /**
     * 根据数据类型获得值
     * @param map
     * @param key
     * @param data_type
     * @return
     * @throws IOException
     * @throws SQLException
     */
    private String getValueByType(Map<String, Object> map, String key, String data_type) throws IOException, SQLException {
        if ("CLOB".equals(data_type)){
            Clob columnClob = (Clob) map.get(key);
            return StringUtils.ClobToString(columnClob);
        }
        if ("BLOB".equals(data_type)){
            BLOB columnClob = (BLOB) map.get(key);
            return StringUtils.BlobToString(columnClob);
        }
        return null;
    }

    /**
     * 批量删除
     * @param data
     * @param uniqueList
     * @param tbName
     * @return
     * @throws Exception
     */
    public int batchDelete(List<Map<String, Object>> data, List<Map<String, Object>> uniqueList, String tbName) throws Exception {

              specialDelete( data,  uniqueList,  tbName);

        int len =0;

        try{
        String sql = " DELETE  FROM   " + tbName + " where 1=1 ";

        int[] result = null;//批量插入返回的数组

        PreparedStatement pst = null;
        boolean isNeedDel = true;
        if (uniqueList.size() ==0) return 0;

        List<String> columnNames ;
        if (uniqueList.size() == 1) {
            boolean flag =null != ((uniqueList.get(0).get("IS_NEED_DEL")) );

            columnNames =  (List<String>)uniqueList.get(0).get("COLUMN_NAME");
            if (flag ){
                isNeedDel = (Boolean) uniqueList.get(0).get("IS_NEED_DEL") ;
            }
            if (!isNeedDel) return 0;
            for (String columnName:  columnNames) {
                sql += " and " + columnName+" is not null and "+ columnName + " =  ? ";
            }

            pst = conn.prepareStatement(sql);
            int m ;
            for (Map<String, Object> map : data) {
                m=0;
                for (String columnName:  columnNames) {
                    m++;
                    pst.setObject(m, map.get(columnName) + "");
                }

                pst.addBatch();
            }
            result = pst.executeBatch();
        } else if ((uniqueList.size() > 1)) {
            //sql 预编译
            String columnName = null;//列名
            int k = 0;
            String[] arr = new String[uniqueList.size()];
            for (Map<String, Object> uniqueMap : uniqueList) {
                columnName = uniqueMap.get("COLUMN_NAME") + "";
                sql += " and " + columnName+" is not null and "+ columnName + " =  ? ";
                arr[k] = columnName;
                k++;
            }
            pst = conn.prepareStatement(sql);
            //批量处理
            for (Map<String, Object> map : data) {
                for (int i = 0; i < arr.length; i++) {
                    pst.setObject(i + 1, map.get(arr[i]) + "");
                }
                pst.addBatch();
            }
            result = pst.executeBatch();
        }
            conn.commit();
            len =  result.length;
        }catch (Exception e){

            logger.error("删除失败"+e.getMessage());
        }finally {
           logger.info("删除"+tbName +"表 "+len+"条数据成功！");
            return len;
        }
    }

    private int specialDelete(List<Map<String,Object>> data, List<Map<String,Object>> uniqueList, String tbName) throws SQLException {
        String sql=null;
        if (tbName.equals("EAF_ACM_R_USERORG")||tbName.equals("EAF_ACM_ORG")|| tbName.equals("EAF_DMM_METACLASS_M")
        || tbName.equals("EAF_DMM_METAOPER_M") || tbName.equals("BIM_PRJ_PROJ")
                ){
             sql =" delete from "+tbName+" where eaf_id = ? ";
        }
        if(tbName.equals("EAF_DMM_METAATTR_M")){
            sql = "delete from EAF_DMM_RESOURCE where EAF_RELATEATTRID =?";
            pst = conn.prepareStatement(sql);
            List<String> columnNames;
            for (Map<String, Object> map : data) {
                pst.setObject(1, map.get("EAF_ID") + "");
                pst.addBatch();
            }
            int[] result = pst.executeBatch();
            conn.commit();
            pst.clearBatch();
            sql = "delete from EAF_DMM_METAATTR_M where EAF_ID =?";
        }
        if ( null == sql) return 0;
        pst = conn.prepareStatement(sql);
        List<String> columnNames;
        for (Map<String, Object> map : data) {
            pst.setObject(1, map.get("EAF_ID") + "");
            pst.addBatch();
        }
        int[] result = pst.executeBatch();
        conn.commit();
        pst.clearBatch();
        return result.length;
    }

    //删除部分数据获得影响的行数 不使用addBatch
    public int delete (List<Map<String, Object>> data, List<Map<String, Object>> uniqueList, String tbName)throws Exception {
        long startTime =System.currentTimeMillis();
        int len =uniqueList.size(); //删除条件字段数量
        if (len ==0) return 0;
        //根据数据条数切割
        List<List<Map<String, Object>>> newData = CollectionUtil.splitList(data, data.size()/uniqueList.size());
        //获得sql
        int affectLines =0 ;
        for (List<Map<String, Object>> list: newData) { //
            String sql = " DELETE  FROM   " + tbName + " where 1=1 and ( ";
            int i=0;
            String column ="";
            for (Map<String, Object> param: list) {
                if (len !=1)  sql +="    ( ";
                int j =0;
                for (Map<String,Object> uniqueMap : uniqueList) {

                    column = (uniqueMap.get("COLUMN_NAME") +"").replace("[","").replace("]","");
                    if ( "null".equals((uniqueMap.get("COLUMN_NAME") +""))){//如果删除条件的字段数量为1 且这个字段值为空 则
                        if (len !=1) sql += "" + column+ " is null ";
                    }else {
                        sql += "" + column+ " = '"+ ( param.get(column))+"'";
                    }
                    j++;
                }
                if (len !=1)   sql +=" ) ";
                if (i!=list.size()-1){sql+=" or ";}
                i++;
            }
            sql += " ) ";
            pst = conn.prepareStatement(sql);
            affectLines +=   pst.executeUpdate();
        }
        long endTime =System.currentTimeMillis();
        logger.info(""+tbName +"表 "+affectLines+"行被删除, 耗时 "+DateUtil.getRocord(startTime,endTime));
        return affectLines;
    }


    public int updateTbCreator(List<Map<String, Object>> list ,String tbName,String column) {
        String sql = " update   " + tbName + " set "+column+" =? where "+column+" =? ";
        PreparedStatement pst = null;
        int[] dataArr ;
        try {
             pst = conn.prepareStatement(sql);
            for (Map map:list) {
                pst.setString(1,map.get("EAF_ID")+"");
                pst.setString(2,map.get("T_ID")+"");
                pst.addBatch();
            }
            dataArr =  pst.executeBatch();

            conn.commit();
            return dataArr.length;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getCreateTableSql(String sql, Object[][] params) {
        // 执行SQL获得结果集
        ResultSet rs = null;
        String returnSql ="" ;

        // 创建ResultSetMetaData对象
        ResultSetMetaData rsmd = null;

        // 结果集列数
        int columnCount = 0;
        try {
            rs =   executeQueryRS(sql, params);
            rsmd = rs.getMetaData();

            // 获得结果集列数
            columnCount = rsmd.getColumnCount();
        } catch (SQLException e1) {
            logger.error(e1.getMessage());
        }


        try {
            // 将ResultSet的结果保存到List中
            String type = "";
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                for (int i = 1; i <= columnCount; i++) {
                    type = rsmd.getColumnTypeName(i);
                    Clob createSql =    rs.getClob(i) ;
                    returnSql = StringUtils.ClobToString(createSql);
                }

            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(sql);
        } /*finally {
            if (null!= rs) rs.close();
            if (null!= pst) pst.close();
            if (null!= conn) conn.close();
        }*/

        return returnSql;

    }


}
