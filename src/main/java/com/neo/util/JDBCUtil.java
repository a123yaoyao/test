package com.neo.util;


import oracle.sql.BLOB;
import oracle.sql.CLOB;
import org.apache.log4j.Logger;

import javax.sql.rowset.serial.SerialClob;
import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * @Auther: Administrator
 * @Date: 2019/3/21/021 16:01
 * @Description:
 */
public class JDBCUtil {

    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(JDBCUtil.class);


    Connection conn ;

    private PreparedStatement pst = null;

    private Statement st = null;

    private ResultSet rst = null;
    /**
     * 构造方法
     *
     * @param connection
     * 			数据库连接
     */
    public JDBCUtil(Connection connection) {
        conn = connection ;
    }

    public JDBCUtil(String  dbName) {
        conn = DataSourceHelper.GetConnection(dbName) ;
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
            logger.error(e.getMessage());
        }
        finally{
            closeAll();
        }
        return rowCount;
    }


    public List<Map<String, Object>> excuteQuery(String sql, Object[] params) throws SQLException {
        long start =System.currentTimeMillis();
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
                    if (rsmd.getColumnTypeName(i).equals("BLOB")){
                        BLOB v = (BLOB) rs.getBlob(i);
                        if (v==null){
                            map.put(rsmd.getColumnLabel(i),null);
                        }else{
                            map.put(rsmd.getColumnLabel(i),StringUtils.BlobToString(v));
                        }
                    }
                    if (rsmd.getColumnTypeName(i).equals("CLOB")){
                        CLOB v = (CLOB) rs.getClob(i);
                        if (v==null){
                            map.put(rsmd.getColumnLabel(i),"");
                        }else {
                            map.put(rsmd.getColumnLabel(i),StringUtils.ClobToString(v));
                        }

                    } else if(rsmd.getColumnTypeName(i).equals("DATE")){
                         Date v =  rs.getDate(i);
                        if (v==null){
                            map.put(rsmd.getColumnLabel(i),null);
                        }else {
                            map.put(rsmd.getColumnLabel(i),DateUtil.DateToStr(v));
                        }
                    }
                    else {
                        map.put(rsmd.getColumnLabel(i), rs.getObject(i));
                    }

                }
                list.add(map);//每一个map代表一条记录，把所有记录存在list中
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            logger.error(sql);
        }  finally{
            closeAll();
        }
        long end =System.currentTimeMillis();
        //logger.info("线程"+Thread.currentThread().getName()+"查询sql:"+sql+"花费时间"+((end-start)/1000)+"s");
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
            // 执行
            affectedLine = pst.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.fillInStackTrace();
        } finally{
            closeAll();
        }
        return affectedLine;
    }

    /**
     * insert update delete SQL语句的执行的统一方法 执行完关闭连接
     * @param sql SQL语句
     * @return 受影响的行数
     */
    public int executeUpdate(String sql) throws Exception {
        // 受影响的行数
        int affectedLine = 0;
        try {
            // 获得连接
            //  conn = this.getConnection();
            // 调用SQL
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            // 参数赋值
            // 执行
            affectedLine = pst.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.fillInStackTrace();
            throw new Exception(e.getMessage());
        } finally{
            closeAll();
        }
        return affectedLine;
    }

    /**
     *  关闭数据连接
     *  /
     */
    private void closeAll(){

        try {
            if(rst !=null && !rst.isClosed())rst.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        try {
            if(pst !=null && !pst.isClosed())pst.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            if(st !=null && !st.isClosed())st.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        try {
            if(conn !=null && !conn.isClosed() )conn.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }





    public  Map<String,Object>   batchInsertJsonArry(String tbName, List<Map<String, Object>> newData, List<Map<String, Object>> tbstruct,  Map<String,Object> conditionMap) throws Exception{
        long start = System.currentTimeMillis();
        Map<String,Object> returnMap =new HashMap<>();
        String  sql= null;
        try {
            sql =  getInsertSql( tbName,  newData) + conditionMap.get("tempSql");

            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            returnMap =    insertBatch(tbName , newData, pst,tbstruct,conditionMap);
            long end = System.currentTimeMillis();
            logger.info(tbName+"表批量插入了:"+returnMap.get("INSERT_COUNT")+"条数据 需要时间:"+(end - start)/1000+"s"); //批量插入需要时间:
            newData =null;
            return returnMap;
        } catch (Exception e) {
            logger.error(sql.toString()+e.getMessage());
             }finally {
            closeAll();
        }
        return returnMap;

    }




    public int insertTbRecord(String tbName, List<Map<String, Object>> dat,Map<String,Integer> mapper) throws Exception{
        long start = System.currentTimeMillis();
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
        }finally {
            closeAll();
        }
        return 0;

    }



    private  String getInsertSql(String tbName, List<Map<String, Object>> newData) {
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


    private  Map<String,Object> insertBatch(String tbName, List<Map<String, Object>> dat,PreparedStatement pst,List<Map<String, Object>> tbstruct,Map<String, Object> conditionMap) throws SQLException, IOException {
        /*******************************变量说明*********************************************************************/
        Map<String,Object> returnMap =new HashMap<>();//返回信息
        Map<Integer,String > conditionMapper = (Map<Integer, String>) conditionMap.get("mapper");
        String errMsg = "" ;//错误信息
        int affectRows =0 ; //数据插入条数
        conn.setAutoCommit(false);
        Map<String, Object> ma = null;
        String dataType = null;//字段类型
        java.sql.Timestamp timestampValue = null;
        Map<String, String> structureMap = new LinkedHashMap<>();
        /******************************* 字段名和字段类型映射*********************************************************************/
        for (Map<String, Object> structure : tbstruct) {
            structureMap.put(structure.get("COLUMN_NAME") + "", structure.get("DATA_TYPE") + "");
        }
        /******************************* 字段值和参数位置的映射*********************************************************************/
        Map<String, Integer> nameIndexMapper = new LinkedHashMap<>();
        Map<String, Object> insertMap = (Map<String, Object>) dat.get(0);
        int mapperFlag =0;
        for (String  col : insertMap.keySet()) { //表 字段名和字段类型映射
            mapperFlag++;
            nameIndexMapper.put(col,mapperFlag);
        }
        /******************************* 数据插入*********************************************************************/
        try {

            for (int i = 0; i < dat.size(); i++) {
                ma = (Map<String, Object>) dat.get(i);
                int j = 0;
                for (String k : ma.keySet()) {
                    dataType   = structureMap.get(k);
                    mapperFlag =  nameIndexMapper.get(k);
                    if ("DATE".equals(dataType)){
                         if (ma.get(k) == null){
                             pst.setTimestamp(mapperFlag, null);
                         }else {
                             timestampValue= DateUtil.strToTimeStamp( (ma.get(k)+"") );
                             pst.setTimestamp(mapperFlag, timestampValue);
                         }
                    }
                   else if (("CLOB".equals(dataType))){
                        Clob clob = conn.createClob();
                        pst.setClob(mapperFlag, clob);
                    }
                    else if (("BLOB".equals(dataType))){
                        Blob blob= conn.createBlob();
                        pst.setBlob(mapperFlag, blob);
                    }
                    else{
                        if (ma.get(k) == null){
                            pst.setObject(mapperFlag, null);
                        }else {
                            String v = ma.get(k)+"";
                            pst.setObject(mapperFlag, v);
                        }
                    }
                }
                for (Integer k : conditionMapper.keySet()) {
                    pst.setObject(k+ma.size(), ma.get(conditionMapper.get(k)));
                }


                pst.addBatch();

                if (i > 0 && i % 1000 == 0) {
                    affectRows  += pst.executeBatch().length;
                    conn.commit();
                    //清除批处理命令
                    pst.clearBatch();
                    //如果不想出错后，完全没保留数据，则可以每执行一次提交一次，但得保证数据不会重复
                }
            }
            affectRows  += pst.executeBatch().length;
            conn.commit();
            pst.clearBatch();
            returnMap.put("INSERT_COUNT",affectRows);
            returnMap.put("MESSAGE","执行成功");
            dat = null ;
            return returnMap;
        }catch (Exception e){
            logger.error("JDBCUtil 432行："+e.getMessage()+" ");
            logger.info("************************单条插入************************");
            conn.rollback();
            conn.setAutoCommit(false);
            String errMessage ="";
            /*******************循环列表 一条一条插入数据 ********************************/
            /*for (int i = 0; i < dat.size(); i++) {
                ma = (Map<String, Object>) dat.get(i);
                int j = 0;

                for (String k : ma.keySet()) {
                    dataType   =   structureMap.get(k);
                    mapperFlag =  nameIndexMapper.get(k);
                    if ("DATE".equals(dataType)){
                        if (ma.get(k) == null){
                            pst.setTimestamp(mapperFlag, null);
                        }else {
                            timestampValue= DateUtil.strToTimeStamp( (ma.get(k)+"") );
                            pst.setTimestamp(mapperFlag, timestampValue);
                        }
                    }
                    if (("CLOB".equals(dataType))){
                        Clob clob = conn.createClob();
                        clob.setString(1, ma.get(k)+"");
                        pst.setClob(mapperFlag, clob);
                    }
                    else if (("BLOB".equals(dataType))){
                        if (ma.get(k) == null){
                            pst.setObject(mapperFlag, null);
                        }else {
                            BLOB v = (BLOB)ma.get(k);
                            pst.setBlob(mapperFlag, v);
                        }
                    }else{
                        if (ma.get(k) == null){
                            pst.setObject(mapperFlag, null);
                        }else {
                            String v = ma.get(k)+"";
                            pst.setObject(mapperFlag, v);
                        }
                    }
                }

                try{
                    affectRows += pst.executeUpdate();
                    conn.commit();
                }catch (Exception e1){
                    if (structureMap.containsKey("EAF_ID")){
                        logger.info("affectRows:"+affectRows +" EAF_ID "+ma.get("EAF_ID") );
                    }else{
                        for (String colm:structureMap.keySet()) {
                            errMsg += colm +" 值为："+ma.get(colm)+" ";
                        }
                    }
                    errMessage += errMsg+e1.getMessage()+"@_@";
                    returnMap.put("MESSAGE",errMessage);
                    returnMap.put("INSERT_COUNT",affectRows);
                    conn.rollback();
                    continue;//执行下一次更新
                }
            }
            dat = null;*/
           return returnMap;

        }

    }




    private int specialDelete(List<Map<String,Object>> data, List<Map<String,Object>> uniqueList, String tbName) throws SQLException {
        String sql=null;
        sql =" delete from "+tbName+" where eaf_id = ? ";
        pst = conn.prepareStatement(sql);

        int i =0;
        for (Map<String, Object> map : data) {
            i++;
            pst.setObject(1, map.get("EAF_ID") + "");
            pst.addBatch();
            if(i>0 && i%1000==0){
                pst.executeBatch();
                conn.commit();
                //清除批处理命令
                pst.clearBatch();
                //如果不想出错后，完全没保留数据，则可以每执行一次提交一次，但得保证数据不会重复
            }
        }
        int[] result = pst.executeBatch();
        conn.commit();
        pst.clearBatch();
        return result.length;
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
        long start =System.currentTimeMillis();
        if (tbName.equals("EAF_ACM_R_USERORG") || tbName.equals("EAF_ACM_ORG")){
           return specialDelete( data,  uniqueList,  tbName);
        }
        int len =0;
        try{
            String sql = " DELETE  FROM   " + tbName + " where 1=1 ";
            int[] result = null;//批量插入返回的数组
            if (uniqueList.size() ==0) return 0;
            List<String> columnNames = null ;
            Map<Integer,String> indexCloumnMapper =new LinkedHashMap<>();
            int k = 0;
            List<String> cloumnList =null;
            for (Map<String, Object> uniqueMap : uniqueList) {
                k++;
                cloumnList= (List<String>)uniqueMap.get("COLUMN_NAME") ;
                for (String columnName: cloumnList) {
                   if (k==1){
                       int i =1;
                       indexCloumnMapper.put(i,columnName);
                       i++;
                    }
                sql += " and " + columnName+" is not null and "+columnName + " =  ? ";
                }
                }
                pst = conn.prepareStatement(sql);
                //批量处理
                int m =0;
                for (Map<String, Object> map : data) {
                    m++;
                    for (Integer index :indexCloumnMapper.keySet() ) {
                        pst.setObject(index, map.get(indexCloumnMapper.get(index)));
                    }
                    pst.addBatch();
                    if(m>0 && m%1000==0){
                        pst.executeBatch();
                        conn.commit();
                        //清除批处理命令
                        pst.clearBatch();
                        //如果不想出错后，完全没保留数据，则可以每执行一次提交一次，但得保证数据不会重复
                    }
                }
                result = pst.executeBatch();
           // }
            conn.commit();
            len =  result.length;
        }catch (Exception e){
            logger.error("删除失败"+e.getMessage());
        }finally {
            closeAll();
            long end =System.currentTimeMillis();
            logger.info("删除"+tbName +"表 "+len+"条数据成功 耗时："+((end-start)/1000)+" 秒") ;
            return len;
        }
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
        }finally {
            closeAll();
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
        } finally {
           closeAll();
        }
        return returnSql;

    }

    //删除部分数据获得影响的行数 不使用addBatch
    public int delete (List<Map<String, Object>> data, List<Map<String, Object>> uniqueList, String tbName)throws Exception {
        try{
            long startTime = System.currentTimeMillis();
        int len = uniqueList.size(); //删除条件字段数量
        if (len == 0 || null==data || data.size() ==0 ) return 0;
        //根据数据条数切割
        List<List<Map<String, Object>>> newData = CollectionUtil.splitList(data, data.size() / uniqueList.size());

            //获得sql
        int affectLines = 0;
        for (List<Map<String, Object>> list : newData) { //
            String sql = " DELETE  FROM   " + tbName + " where 1=1 and ( ";
            int i = 0;
            String column = "";
            for (Map<String, Object> param : list) {
                if (len != 1) sql += "    ( ";
                int j = 0;
                for (Map<String, Object> uniqueMap : uniqueList) {

                    column = (uniqueMap.get("COLUMN_NAME") + "").replace("[", "").replace("]", "");
                    if ("null".equals((uniqueMap.get("COLUMN_NAME") + ""))) {//如果删除条件的字段数量为1 且这个字段值为空 则
                        if (len != 1) sql += "" + column + " is null ";
                    } else {
                        sql += "" + column + " = '" + (param.get(column)) + "'";
                    }
                    j++;
                }
                if (len != 1) sql += " ) ";
                if (i != list.size() - 1) {
                    sql += " or ";
                }
                i++;
            }
            sql += " ) ";
            pst = conn.prepareStatement(sql);
            affectLines += pst.executeUpdate();
        }
        long endTime = System.currentTimeMillis();
        logger.info("" + tbName + "表 " + affectLines + "行被删除, 耗时 " + DateUtil.getRocord(startTime, endTime));
        return affectLines;
    }catch (Exception e){e.printStackTrace();}
        finally {
            closeAll();
        }
        return 0;
    }

}
