package com.neo.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.neo.model.bo.TbDealBO;
import com.neo.util.*;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * @Auther: Administrator
 * @Date: 2019/2/19/019 10:42
 * @Description:
 */
@Service
public class TbService {

    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(TbService.class);
   

    @Value("${spring.master.datasource}")
    public String masterDataSource;

    @Value("${spring.dbs}")
    public String dbArray;

    @Value("${groupSize}")
    public String groupSize;

    @Value("${uniqueConstraint}")
    public String uniqueConstraint;

    @Value("${tbUserIds}")
    public String tbUserId;

    @Value("${tbOrgIds}")
    public String tbOrgId;

    @Value("${tbPrjIds}")
    public String tbPrjId;

    @Value("${threadNum}")
    public String threadNum;




    int getThreads(int dataCount){
        if (dataCount>1000 && dataCount<=10000){
            return dataCount%2000==0? dataCount/2000:dataCount/2000+1;
        }
        if (dataCount>10000 && dataCount<=100000){
            return dataCount%5000==0? dataCount/5000:dataCount/5000+1;
        }
        if (dataCount>100000 && dataCount<=1000000){
            return dataCount%10000==0? dataCount/10000:dataCount/10000+1;
        }
        if (dataCount>1000000 && dataCount<=10000000){
            return dataCount%10000==0? dataCount/10000:dataCount/10000+1;
        }
        return 1;
    }

    int getGroupSize(int dataCount){
        if (dataCount>1000 && dataCount<=10000){
            return 2000;
        }
        if (dataCount>10000 && dataCount<=100000){
            return 5000;
        }
        if (dataCount>100000 && dataCount<=1000000){
            return 10000;
        }
        if (dataCount>1000000 && dataCount<=10000000){
            return 10000;
        }
        return 1000;
    }

    /**
     *
     * @param dbName
     * @param page
     * @param rows
     * @param sort
     * @param order
     * @return
     */
    public Map<String,Object> getTableByDB(String dbName,String tbName, String page,
                                           String rows, String sort, String order, Connection connection) throws SQLException {
        List<Map<String,Object>>  tbCollection = null ;
        Map<String,Object> map =new HashMap<>();
        Map<String,Object> param =new HashMap<>();
        param.put("sort",sort);
        param.put("order",order);

        int  end= Integer.valueOf(page)*Integer.valueOf(rows)+1;
        int start = ( Integer.valueOf(page) -1 ) * Integer.valueOf(rows);
        DbUtil db = new DbUtil(connection);
        String sql =" select t.table_name, count_rows(t.table_name)  num_rows,\n" +
                "            ( select count(*) from user_tab_columns where table_name= t.table_name ) num_columns from user_tables t\n where 1=1 " ;

        if (null!=tbName && !"".equals(tbName.trim()) ) sql+=" and t.TABLE_NAME like '%"+tbName.toUpperCase()+"%'";

        String totalSql = "select count(*)  total from ("+sql +") t";
                if(sort!=null && !"".equals(sort)){
                    sql+= "  ORDER BY "+sort+" "+order;
                }

        int total =  db.getCount(totalSql,new Object[][]{});
        String newSql =" select * from ( select a.*,rownum rn from (" +sql +" )a where rownum < "+end+") where rn> "+start;


        tbCollection = db.excuteQuery(newSql,new Object[][]{});
        map.put("rows",tbCollection);
        map.put("total",total);
        return map;
    }


    private Callable<List<Map<String, Object>>> read2List(final int i, final int nums, final DbUtil salverDbUtil,final String dbName,
                                                          final String tbName,final DbUtil  masterDbUtil,CountDownLatch countDownLatch ) {
        Callable<List<Map<String, Object>>> callable = new Callable<List<Map<String, Object>>>() {
            public List<Map<String, Object>> call()  {
                try{
                int startIndex = i * nums;
                int maxIndex = startIndex + nums;

                List<Map<String, Object>> list = selectAllByDbAndTb(dbName, tbName, salverDbUtil,startIndex,maxIndex);
                //查询被导入数据库的表结构
                //List<Map<String, Object>> tb = selectTableStructureByDbAndTb(dbName, tbName,salverDbUtil);
                //判断该表是否使用批处理
               // boolean isUseBatch = checTableIsUseBatch(tbName);
               // masterDbUtil. insert(tbName,list,tb,isUseBatch);
                return list;
                }
                catch (Exception e){
                    logger.error(e.getMessage());
                    return new ArrayList<>();
                }finally {
                    countDownLatch.countDown();

                }
            }
        };
        return callable;

    }


    List<Map<String, Object>> getDataByMulitThreads(String dbName ,String tbName,String masterDataSource ,int groupSize,
                          Connection masterConn,Connection slaverConn) throws SQLException, InterruptedException, ExecutionException {
        String  sql =" select count(1) from "+tbName ;
        if ( null == slaverConn || slaverConn.isClosed()) slaverConn = DataSourceHelper.GetConnection(dbName);
        if ( null == masterConn||  masterConn.isClosed()) masterConn = DataSourceHelper.GetConnection(masterDataSource);
        DbUtil salverDbUtil =new DbUtil(slaverConn);
        DbUtil masterDbUtil =new DbUtil(masterConn);
        int count = salverDbUtil.getCount(sql,new Object[][]{});
        int thrednum =  getThreads(count);
        long queryStart =System.currentTimeMillis();

        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        ExecutorService service = Executors.newFixedThreadPool(thrednum);
        BlockingQueue<Future<List<Map<String, Object>>>> queue = new LinkedBlockingQueue<Future<List<Map<String, Object>>>>();

        groupSize = getGroupSize(count);
        final CountDownLatch  endLock = new CountDownLatch(thrednum); //结束门
        for (int i = 0; i < thrednum; i++) {
            if (slaverConn ==null || slaverConn.isClosed())slaverConn = DataSourceHelper.GetConnection(dbName);
            salverDbUtil = new DbUtil(slaverConn);
            Future<List<Map<String, Object>>> future = service.submit(read2List(i, groupSize, salverDbUtil,dbName,tbName,masterDbUtil,endLock));
            queue.add(future);
        }
        int queueSize = queue.size();
        for (int i = 0; i < queueSize; i++) {
            List<Map<String, Object>> list1= queue.take().get();
            data.addAll(list1);
        }
        service.shutdown();
        long queryEnd =System.currentTimeMillis();
        logger.info("查询"+dbName+"库 中表名为"+tbName+"的所有数据花费时间为"+(queryEnd-queryStart)/1000+"秒");
        return data;
    }



    /**
     * 合并数据
     * @param dbName
     * @param tbName
     * @param masterDataSource
     * @param list
     * @param groupSize
     * @return
     * @throws IOException
     * @throws SQLException
     */
    public Map<String,Object> mergeData(String dbName ,String tbName,String masterDataSource, List<Map<String,Object>> list,int groupSize,
    Connection masterConn,Connection slaverConn
    ) throws Exception {
        Map<String,Object> returnMap =new HashMap<>();
        returnMap.put("INSERT_COUNT","0");
        returnMap.put("MESSAGE","执行成功");



        DbUtil masterDbUtil =new DbUtil(masterConn);
        DbUtil salverDbUtil =new DbUtil(slaverConn);
        int insertCount =0;
        int count =0;
        int listCount = salverDbUtil.getCount(" select count(1) from "+tbName,new Object[][]{});
        //多线程数量
        int threads = (listCount %  groupSize ==0)? listCount / groupSize: (listCount /groupSize)+1 ;
        //表结构
        List<Map<String,Object>> tableStructure = null;
        Map<String,Object> param =new HashMap<>();
        //查询被导入数据库的表结构
        List<Map<String, Object>> tb = selectTableStructureByDbAndTb( tbName,salverDbUtil);
        //该主库是否存在此表
        count = checkTable(tbName,masterDbUtil,null);

        String addColumns = ""; //增加的列

        if (0 == count) {//若不存在则主数据源创建新表
            getCreateTableSql(tbName, tb, param);
            String sql = "select  dbms_metadata.get_ddl('TABLE','"+tbName.toUpperCase()+"') TB_SQL from dual";
            sql = salverDbUtil.getCreateTableSql(sql,new Object[][]{});
            String  createSalver =  "CREATE TABLE \""+dbName.toUpperCase()+"\"" +".\""+tbName.toUpperCase() +"\"";
            String createMaster  =  "CREATE TABLE \""+masterDataSource.toUpperCase()+"\""+".\""+tbName.toUpperCase() +"\"";
            sql = sql.replace(createSalver,createMaster);
            try{
                masterDbUtil.executeUpdate(sql,new Object[][]{});
            }catch (Exception e){
                logger.error("根据ddl语句创建表失败， 采取第二种创建表方式");
                createNewTable(param,masterDbUtil);
            }

            logger.info("*****************创建"+tbName+"成功！***************** ");
        }else{//若存在则比较
            //主库表结构
            List<Map<String, Object>> masterTb = selectTableStructureByDbAndTb( tbName,masterDbUtil);
            String cloumnName ;
            String dataType ;
            Object dataLength;
            for (Map<String, Object> tbMap:tb ) {
                boolean flag =false;
                cloumnName =tbMap.get("COLUMN_NAME")+"";
                dataType = tbMap.get("DATA_TYPE")+"" ;
                dataLength = tbMap.get("DATA_LENGTH");
                for (Map<String, Object> masterTbMap:masterTb) {
                    if (masterTbMap.containsValue(cloumnName)){//若主库表中字段和从库字段一样 判断长度 取最大的
                        if ("VARCHAR".equals(dataType) || "VARCHAR2".equals(dataType) || "CHAR".equals(dataType)){
                            int len1 =(Integer.valueOf(dataLength+"")) ;
                            int len2 =(Integer.valueOf(masterTbMap.get("DATA_LENGTH")+"")) ;
                            if (len2>len1){
                                String sql = " alter table "+tbName+" modify ("+cloumnName+" "+dataType+"("+len2+"))";
                                masterDbUtil.executeUpdate(sql,new Object[][]{});
                            }
                            if (len1>len2){
                                String sql = " alter table "+tbName+" modify ("+cloumnName+" "+dataType+"("+len1+"))";
                                masterDbUtil.executeUpdate(sql,new Object[][]{});
                            }
                        }
                        flag = true;
                        break;
                    }
                }
                if (!flag){
                    addColumns +=cloumnName+",";
                    String sql = " alter table "+tbName+" add ("+cloumnName+" "+dataType;
                    if (null!= dataLength && !"CLOB".equals(dataType) && !"BLOB".equals(dataType)){
                        sql+="("+dataLength+")";
                    }
                    sql+=" ) ";
                    masterDbUtil.executeUpdate(sql,new Object[][]{});
                    logger.info("为"+masterDataSource+"库添加"+cloumnName+"字段成功！");
                }
            }
        }

        // 查询所有数据
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("tbName", tbName);

        long queryStart =System.currentTimeMillis();
        //查询某个库下的某个表的所有数据
        List<Map<String, Object>> data =  getDataByMulitThreads( dbName , tbName, masterDataSource,   groupSize, masterConn, slaverConn);
        if (data.size()==0){
          return returnMap;
        }
        if (data.size()<groupSize) threads =1; //如果同步的数据太少 小于切割的数据条数 则只用一个线程
        Map<String,Object> m =(Map<String,Object>)data.get(0);
        long queryEnd =System.currentTimeMillis();
        logger.info("查询"+dbName+"库 中表名为"+tbName+"的所有数据花费时间为"+(queryEnd-queryStart)/1000+"秒");
        //对数据进行切分
        int cutSize = groupSize ;//每个线程处理的数据量
        List<List<Map<String, Object>>> newData = CollectionUtil.splitList(data, cutSize);
        //批量删除重复的数据
        int delCount = batchDelete(paramsMap, tbName, data, masterDbUtil,salverDbUtil);
        //判断该表是否使用批处理
      //  boolean isUseBatch = checTableIsUseBatch(tbName);
        if (threads == 1 ){ //不开启多线程
            for (List<Map<String, Object>> dat : newData) {
                insertRecord(masterDbUtil,tbName,dat,dbName);//插入记录
                returnMap = masterDbUtil.batchInsertJsonArry(tbName,dat,tb);
            }
        }else{
            final BlockingQueue<Future<Map<String ,Object>>> queue = new LinkedBlockingQueue<>();
            final CountDownLatch  endLock = new CountDownLatch(threads); //结束门
            //List<Future<Integer>> results = new ArrayList<Future<Integer>>();
            final ExecutorService exec = Executors.newFixedThreadPool(threads);
            for (List<Map<String, Object>> dat : newData ) {
                Future<Map<String ,Object>> future=   exec.submit(new Callable<Map<String ,Object>>(){
                    @Override
                    public Map<String ,Object> call() {
                        try {

                            insertRecord(masterDbUtil,tbName,dat,dbName);//插入记录
                            Map<String ,Object>  returnMap =  masterDbUtil. batchInsertJsonArry(tbName,dat,tb);
                            return  returnMap ;
                        }catch(Exception e) {
                            logger.error("数据同步 exception!",e);
                            Map<String ,Object> returnMap =new HashMap<>();
                            returnMap.put("INSERT_COUNT","0");
                            returnMap.put("MESSAGE",e.getMessage());
                            return returnMap;
                        }finally {
                            endLock.countDown(); //线程执行完毕，结束门计数器减1
                        }

                    }
                });
                queue.add(future);
            }
            endLock.await(); //主线程阻塞，直到所有线程执行完成
            for(Future<Map<String ,Object>> future : queue){
                returnMap  =   future.get();
                insertCount += Integer.valueOf(returnMap.get("INSERT_COUNT")+"");
            }
            returnMap.put("INSERT_COUNT",insertCount+"");
            exec.shutdown(); //关闭线程池
        }

        // 对表数据进行特殊业务处理
        TbDealBO tbDealBO =new TbDealBO( tbName,masterDataSource, addColumns);
        tbDealBO.dealWithTbProblem();
        //将从库
        return returnMap;
    }




    private void insertRecord(DbUtil masterDbUtil, String tbName, List<Map<String, Object>> data, String dbName) {
        if ("EAF_ACM_USER".equals(tbName)){
            List<Map<String,Object>> record =new ArrayList<>();
            for (Map m:data) {
                Map<String,Object> recordMap =new LinkedHashMap<>();
                recordMap.put("EAF_ID",m.get("EAF_ID"));
                recordMap.put("EAF_DB_NAME",dbName);
                recordMap.put("EAF_NAME",m.get("EAF_NAME"));
                recordMap.put("EAF_LOGINNAME",m.get("EAF_LOGINNAME"));
                record.add(recordMap);
            }
            Map<String,Integer> mapper =new HashMap<>();
            Map<String,Object> map =record.get(0);
            int k =0;
            for (String key :map.keySet()) {
                k++;
                mapper.put(key,k);
            }

            try {
                masterDbUtil.executeUpdate("delete from EAF_USER_RECORD where eaf_db_name ='"+dbName+"'",new Object[][]{});
                masterDbUtil.insertTbRecord("EAF_USER_RECORD",record,mapper);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("插入用户记录表出错，原因"+e.getMessage());
            }
        }
        if ("EAF_ACM_ORG".equals(tbName)|| "BIM_PRJ_PROJ".equals(tbName)){
            List<Map<String,Object>> record =new ArrayList<>();
            for (Map m:data) {
                Map<String,Object> recordMap =new LinkedHashMap<>();
                recordMap.put("EAF_ID",m.get("EAF_ID"));
                recordMap.put("EAF_DB_NAME",dbName);
                String name1 = m.get("EAF_NAME")+"" ;
                recordMap.put("EAF_NAME",m.get("EAF_NAME"));
                recordMap.put("BIM_NUM",m.get("BIM_NUM"));
                record.add(recordMap);
            }
            Map<String,Integer> mapper =new HashMap<>();
            Map<String,Object> map =record.get(0);
            int k =0;
            for (String key :map.keySet()) {
                k++;
                mapper.put(key,k);
            }

            try {
                if ("EAF_ACM_ORG".equals(tbName)){
                    masterDbUtil.executeUpdate("delete from EAF_ORG_RECORD where eaf_db_name ='"+dbName+"'",new Object[][]{});
                    masterDbUtil.insertTbRecord("EAF_ORG_RECORD",record,mapper);
                }
                if("BIM_PRJ_PROJ".equals(tbName)){
                    masterDbUtil.executeUpdate("delete from EAF_PRJ_RECORD where eaf_db_name ='"+dbName+"'",new Object[][]{});
                    masterDbUtil.insertTbRecord("EAF_PRJ_RECORD",record,mapper);
                }

            } catch (Exception e) {
                e.printStackTrace();
                logger.error("插入记录表出错，原因"+e.getMessage());
            }
            logger.info("----------------------插入记录表 success-------------------------");
        }



    }


    /**
     * 批量删除重复的数据
     * @param paramsMap
     * @param tbName
     * @param data
     * @param masterDbUtil
     * @return
     * @throws Exception
     */
    private int batchDelete(Map<String, Object> paramsMap, String tbName, List<Map<String, Object>> data, DbUtil masterDbUtil,DbUtil salverDbUtil) throws Exception {

        //获得列表中的唯一键
        List<Map<String, Object>> uniqueList = getUniqueConstriant(paramsMap, masterDbUtil,salverDbUtil);
        //批量删除重复数据
        //return masterDbUtil.delete(data, uniqueList, tbName);
        return masterDbUtil.batchDelete(data, uniqueList, tbName);

    }

    /**
     * 获得参数值
     * @param tbName
     * @param dat
     * @return
     */
    private Object[][] getParams(String tbName, List<Map<String, Object>> dat) {
        Map<String,Object> m =(Map<String,Object>)dat.get(0);
        Object[][] params =new Object[dat.size()][m.size()];
        for (int i = 0; i <dat.size() ; i++) {
            Map<String,Object> ma =(Map<String,Object>)dat.get(i);
            int j=0;
            for (String k:ma.keySet()) {
                params[i][j]= String.valueOf(ma.get(k));
                j++;
            }

        }
        return  params;
    }




    /**
     * 获得唯一约束
     * @param paramsMap
     * @param masterDbUtil
     * @return
     */
    private List<Map<String, Object>> getUniqueConstriant(Map<String, Object> paramsMap, DbUtil masterDbUtil,DbUtil salverDbUtil) throws Exception {
        List<Map<String, Object>> list = null;
        JSONArray constraint = JSONArray.parseArray(uniqueConstraint);
        JSONObject jsonObject =null;
        String tbName = paramsMap.get("tbName")+"";
        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);
            if (tbName.equals(jsonObject.get("table")+"")){
                list = new ArrayList<>();
                Map<String,Object> map =new HashMap<>();
                map.put("COLUMN_NAME",(List<String>)jsonObject.get("column"));
                map.put("IS_NEED_DEL",jsonObject.get("isNeedDel"));
                list.add(map);
                break;
            }
        }
        if (null!= list)return list;
        if (null == list){//判断该表是否存在eaf_Id
          int i =  checkTable(  tbName, salverDbUtil,"EAF_ID");
            list =new ArrayList<>();
            Map<String,Object> map =new HashMap<>();
          if (i==1) {
              List<String> columns = new ArrayList<>();
              columns.add("EAF_ID");
              map.put("COLUMN_NAME",columns);
              list.add(map);
          }
        }

        for (Map<String,Object> map:list) {
            map.put("IS_NEED_DEL",true);
        }
        return list;
    }

    /**
     *
     * @param param
     * @return
     */
    private int createNewTable(Map<String,Object> param,DbUtil dbUtil ) {
        String sql = param.get("sql")+"";
        return dbUtil.executeUpdate(sql,new Object[][]{});
    }

    /**
     * 判断表是否存在
     * @param tbName
     * @return
     */
    private int checkTable( String tbName,DbUtil dbUtil,String Column) throws Exception{
        String sql = null ;
        if(Column ==null )
         sql =" SELECT COUNT(*) TABLE_NUMS FROM User_Tables WHERE table_name = '"+tbName+"' " ;
        else sql="select count(0) as TABLE_NUMS  from user_tab_columns   \n" +
                "where UPPER(column_name)='"+Column+"' AND TABLE_NAME = '"+tbName+"'";

        List<Map<String,Object>> list =dbUtil.excuteQuery(sql,new Object[][]{});
        String count =list.get(0).get("TABLE_NUMS")+"";
        if ("1".equals(count) ) return 1;
        else return 0;
    }

    /**
     *
     * @param dbName
     * @param tbName
     * @param dbUtil
     * @param startIndex
     * @param maxIndex
     * @return
     * @throws SQLException
     */
    private List<Map<String,Object>> selectAllByDbAndTb(String dbName, String tbName,  DbUtil dbUtil,Integer startIndex,Integer maxIndex) throws SQLException {

        String sql = " select * from "+tbName;
         if (null !=startIndex && null !=maxIndex)   {
             sql =" SELECT * FROM  ( SELECT A.*, ROWNUM RN  FROM (SELECT * FROM "+tbName+") A  " +
                     "WHERE ROWNUM <= " + maxIndex+
                     ")  \n" +
                     "WHERE RN > "+startIndex;
         }

        logger.info(sql);
        return dbUtil.excuteQuery(sql,new Object[][]{});
    }

    /**
     * 通过库名和表名查询数据库表结构
     *
     * @param tbName
     * @return
     */
    private List<Map<String,Object>> selectTableStructureByDbAndTb( String tbName, DbUtil dbUtil) throws SQLException {
        String sql ="select t.COLUMN_NAME,  t.DATA_TYPE, t.DATA_LENGTH,\n" +
                "        t.DATA_PRECISION, t.NULLABLE, t.COLUMN_ID, c.COMMENTS,\n" +
                "                (\n" +
                "        select a.CONSTRAINT_TYPE\n" +
                "        from user_constraints  a,USER_CONS_COLUMNS b\n" +
                "        where   a.CONSTRAINT_TYPE ='P'\n" +
                "              and a. constraint_name=b.constraint_name\n" +
                "              and a.table_name =  '"+tbName +"' and b.column_name = t.COLUMN_NAME\n" +
                "        ) IS_PRIMARY,\n" +
                "           (\n" +
                "        select a.CONSTRAINT_TYPE\n" +
                "        from user_constraints  a,USER_CONS_COLUMNS b\n" +
                "        where   a.CONSTRAINT_TYPE ='P'\n" +
                "              and a. constraint_name=b.constraint_name\n" +
                "              and a.table_name =  '"+tbName+"'  and b.column_name = t.COLUMN_NAME\n" +
                "        ) IS_UNIQUE\n" +
                "\n" +
                "        from user_tab_columns t, user_col_comments c\n" +
                "\n" +
                "        where t.table_name = c.table_name  and t.column_name = c.column_name\n" +
                "\n" +
                "        and t.table_name =  '"+tbName+"'";
        return dbUtil.excuteQuery(sql,new Object[][]{});
    }


    /**
     * 获取创建表的sql
     * @param tbName
     * @param tableStructure
     * @param param
     */
    private void getCreateTableSql(String tbName, List<Map<String, Object>> tableStructure, Map<String, Object> param) throws IOException, SQLException {
        String sql = SqlTools.getCreateTableSql(tbName, tableStructure);
        param.put("sql",sql);
    }

    /**
     * 获得所有数据库
     * @param dbArray
     * @param masterDataSource
     * @return
     */
    public List<Map<String,Object>> getAllByDB(String dbArray, String masterDataSource) {
        List<Map<String,Object>> list =new ArrayList<>();
        JSONArray jsonArray = JSONArray.parseArray(dbArray);
        JSONObject jsonObject =null;
        for (int i = 0; i <jsonArray.size() ; i++) {
            jsonObject = (JSONObject) jsonArray.get(i);
            Map<String,Object> map =new HashMap<>();
            map.put("id",jsonObject.get("value")+"");
            map.put("text",jsonObject.get("text")+"");
            if (masterDataSource.equals(jsonObject.get("value")+"")){
                map.put("IS_MASTER",1);
                map.put("selected",true);
            }else map.put("IS_MASTER",0);
            list.add(map);
        }

        return  list;
    }

    public String updateUser(String dbName, String tbName, String masterDataSource,  Integer integer, Connection masterConn, Connection slaverConn) {
        JSONArray constraint = JSONArray.parseArray(tbUserId);
        List<String> columnList = null ;
        JSONObject jsonObject =null;
        String sql = "" ;
        DbUtil masterDbUtil = new DbUtil(masterConn);
        Boolean flag =false;
        int len =0;
        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);
            if (tbName.equals(jsonObject.get("table")+"")){
                flag = true ;
                columnList = (List<String>)jsonObject.get("column");
                for (String column: columnList) {
                    sql =  getQueryUserIdMapperSql(column,tbName);
                    try {
                        List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                       //更新业务表关联人员为空的人员id
                        len += masterDbUtil.updateTbCreator(list,tbName,column);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logger.info("查询或插入业务表人员报错"+e.getSQLState());
                    }
                }
                break;
            }
        }
        if (!flag){ //如果没有配置 默认更新创建人 修改人
            try {
                sql =  getQueryUserIdMapperSql("EAF_CREATOR",tbName);
                List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                len += masterDbUtil.updateTbCreator(list,tbName,"EAF_CREATOR");
                sql =  getQueryUserIdMapperSql("EAF_MODIFIER",tbName);
                list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                len += masterDbUtil.updateTbCreator(list,tbName,"EAF_MODIFIER");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
       return len+"";
    }

    public String updateUser1( ) throws SQLException {
        JSONArray constraint = JSONArray.parseArray(tbUserId);
        List<String> columnList = null ;
        JSONObject jsonObject =null;
        String sql = "" ;
        Connection masterConn = DataSourceHelper.GetConnection(masterDataSource);
        DbUtil masterDbUtil = new DbUtil(masterConn);
        int len =0;
        String tbName = "";
        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);
                columnList = (List<String>)jsonObject.get("column");
                tbName = jsonObject.getString("table");
                for (String column: columnList) {
                    sql =  getQueryUserIdMapperSql(column,tbName);
                    try {
                        List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                        //更新业务表关联人员为空的人员id
                        len += masterDbUtil.updateTbCreator(list,tbName,column);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logger.info("查询或插入业务表人员报错"+e.getSQLState());
                    }
                }
        }

        try {
            List<Map<String,Object>> list = masterDbUtil.excuteQuery("SELECT table_name FROM USER_TABLES",new Object[][]{});
            String tb =null;
            List<Map<String,Object>> newList =null;
            int i =0;
            for (Map<String,Object> map : list) {
                try{
                    i++;
                    tb = map.get("TABLE_NAME")+"";
                    sql =  getQueryUserIdMapperSql("EAF_CREATOR",tbName);
                    newList = masterDbUtil.excuteQuery(sql,new Object[][]{});
                    len += masterDbUtil.updateTbCreator(newList,tbName,"EAF_CREATOR");
                    sql =  getQueryUserIdMapperSql("EAF_MODIFIER",tbName);
                    newList = masterDbUtil.excuteQuery(sql,new Object[][]{});
                    len += masterDbUtil.updateTbCreator(newList,tbName,"EAF_MODIFIER");

                    if(i>0 && i%50==0){
                        logger.info("序号:"+i+"更新"+tb+"表成功");
                        masterConn.commit();
                        masterConn.close();
                        masterConn = DataSourceHelper.GetConnection(masterDataSource);
                        masterDbUtil =new DbUtil(masterConn);
                    }
                }catch (Exception e){
                    logger.error(e.getMessage());
                    continue;
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (null!=masterConn && !masterConn.isClosed()){
                masterConn.close();
            }
        }

        return len+"";
    }


    private String getQueryUserIdMapperSql(String column,String tbName) {
        String querySql ="-- 查询当前业务表中人员关联结果为空的eaf_id 也就是现在的eaf_id\n" +
                "with tmp as (" +
                "select distinct t."+column+" from "+tbName+" t where t."+column+" not in (select distinct eaf_id from eaf_acm_user)\n" +
                "),\n" +
                "\n" +
                "--找出这些eaf_id 在记录表中对应的 人员登录名\n" +
                "tmp_user as (\n" +
                "select m.* from eaf_user_record m where m.eaf_id in (select "+column+" from tmp)\n" +
                "),\n" +
                "finall_user as (\n" +
                "select  t.eaf_id ,tm.eaf_id t_id ,t.eaf_name  from eaf_acm_user  t inner join tmp_user  tm on t.eaf_loginname =tm.eaf_loginname \n" +
                ")\n" +
                "\n" +
                "select * from finall_user";
        return querySql;
    }

    private String getQueryOrgIdMapperSql(String column,String tbName,String compareKey,String compareTb) {
        String recordTb ="EAF_ORG_RECORD";
        String querySql ="-- 查询当前业务表中人员关联结果为空的eaf_id 也就是现在的eaf_id\n" +
                "with tmp as (" +
                "select distinct t."+column+" from "+tbName+" t where t."+column+" not in (select distinct eaf_id from "+compareTb+")\n" +
                "),\n" +
                "\n" +
                "--找出这些eaf_id 在记录表中对应的 人员登录名\n" +
                "tmp_user as (\n" +
                "select m.* from "+recordTb+" m where m.eaf_id in (select "+column+" from tmp)\n" +
                "),\n" +
                "finall_user as (\n" +
                "select  t.eaf_id ,tm.eaf_id t_id ,t.eaf_name  from "+compareTb+"  t inner join tmp_user  tm on t."+compareKey+" =tm."+compareKey+" \n" +
                ")\n" +
                "\n" +
                "select * from finall_user";
        return querySql;
    }

    private String getQueryProjMapperSql(String column,String tbName,String compareKey,String compareTb) {
        String recordTb ="EAF_PRJ_RECORD";
        String querySql ="-- 查询当前业务表中人员关联结果为空的eaf_id 也就是现在的eaf_id\n" +
                "with tmp as (" +
                "select distinct t."+column+" from "+tbName+" t where t."+column+" not in (select distinct eaf_id from "+compareTb+")\n" +
                "),\n" +
                "\n" +
                "--找出这些eaf_id 在记录表中对应的 人员登录名\n" +
                "tmp_user as (\n" +
                "select m.* from "+recordTb+" m where m.eaf_id in (select "+column+" from tmp)\n" +
                "),\n" +
                "finall_user as (\n" +
                "select  t.eaf_id ,tm.eaf_id t_id ,t.eaf_name  from "+compareTb+"  t inner join tmp_user  tm on t."+compareKey+" =tm."+compareKey+" \n" +
                ")\n" +
                "\n" +
                "select * from finall_user";
        return querySql;
    }

    public static void main(String[] args) {
        String recordTb ="EAF_PRJ_RECORD";
        String column ="BIM_PROJ";
        String tbName ="BIM_QUALITY_PROBLEM";
        String compareTb ="BIM_PRJ_PROJ";
        String compareKey ="BIM_NUM";
        String querySql ="" +
                "with tmp as (" +
                "select distinct t."+column+" from "+tbName+" t where t."+column+" not in (select distinct eaf_id from "+compareTb+")\n" +
                "),\n" +
                "\n" +
                "--找出这些eaf_id 在记录表中对应的 人员登录名\n" +
                "tmp_user as (\n" +
                "select m.* from "+recordTb+" m where m.eaf_id in (select "+column+" from tmp)\n" +
                "),\n" +
                "finall_user as (\n" +
                "select  t.eaf_id ,tm.eaf_id t_id ,t.eaf_name  from "+compareTb+"  t inner join tmp_user  tm on t."+compareKey+" =tm."+compareKey+" \n" +
                ")\n" +
                "\n" +
                "select * from finall_user";
        System.out.println(querySql);

    }

    public List<Map<String, Object>> getTableStruct(String dbName, String tbName, Connection conn) throws SQLException {
        DbUtil salverDbUtil =new DbUtil(conn);
        List<Map<String, Object>> tb = selectTableStructureByDbAndTb( tbName,salverDbUtil);
        return tb;
    }

    public Map<String,Object> editTableStruct(String dbName, String tbName, JsonObject jo , Connection conn) {
       Map<String,Object> returnMap = new HashMap<>();

        Gson googleJson = new Gson();
        Map<String,Object> map =  googleJson.fromJson(jo, HashMap.class);
        String sql = "alter table "+tbName+" modify "+map.get("COLUMN_NAME")+" "+map.get("DATA_TYPE")+"("+map.get("DATA_LENGTH")+")";
        try{
            int i =new JDBCUtil(dbName).executeUpdate(sql) ;
            returnMap.put("code","200");
        }catch (Exception e){
            System.out.println(e.getMessage());
            returnMap.put("err",true);
            returnMap.put("content",e.getMessage());
        }

         return returnMap;
    }

    public String updateOrg(String dbName, String tbName, String masterDataSource, Integer integer, Connection masterConn, Connection slaverConn) {
        JSONArray constraint = JSONArray.parseArray(tbOrgId);
        List<String> columnList = null ;
        JSONObject jsonObject =null;
        String sql = "" ;
        DbUtil masterDbUtil = new DbUtil(masterConn);
        Boolean flag =false;
        int len =0;
        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);
            if (tbName.equals(jsonObject.get("table")+"")){
                flag = true ;
                columnList = (List<String>)jsonObject.get("column");
                for (String column: columnList) {

                    sql =  getQueryOrgIdMapperSql(column,tbName,"BIM_NUM","eaf_acm_org");
                    try {
                        List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                        //更新业务表关联人员为空的人员id
                        len += masterDbUtil.updateTbCreator(list,tbName,column);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logger.info("查询或插入业务表人员报错"+e.getSQLState());
                    }
                }
                break;
            }
        }
      /*  if (!flag){ //如果没有配置 默认更新创建人 修改人
            try {
                sql =  getQueryUserIdMapperSql("EAF_CREATOR",tbName);
                List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                len += masterDbUtil.updateTbCreator(list,tbName,"EAF_CREATOR");
                sql =  getQueryUserIdMapperSql("EAF_MODIFIER",tbName);
                list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                len += masterDbUtil.updateTbCreator(list,tbName,"EAF_MODIFIER");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }*/
        return len+"";
    }

    public void updateOrg1(Connection masterConn) {
        JSONArray constraint = JSONArray.parseArray(tbOrgId);
        List<String> columnList = null ;
        JSONObject jsonObject =null;
        String sql = "" ;
        DbUtil masterDbUtil = new DbUtil(masterConn);
        Boolean flag =false;
        int len =0;
        String tbName =null;
        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);

                columnList = (List<String>)jsonObject.get("column");
                tbName = jsonObject.getString("table");
                for (String column: columnList) {
                    sql =  getQueryOrgIdMapperSql(column,tbName,"BIM_NUM","eaf_acm_org");
                    try {
                        List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                        //更新业务表关联人员为空的人员id
                        len += masterDbUtil.updateTbCreator(list,tbName,column);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        logger.info("查询或插入业务表人员报错"+e.getSQLState());
                    }
                }

        }
    }

    public void updateProj() throws SQLException {
        JSONArray constraint = JSONArray.parseArray(tbPrjId);
        List<String> columnList = null ;
        JSONObject jsonObject =null;
        String sql = "" ;
        Connection masterConn = DataSourceHelper.GetConnection(masterDataSource);
        DbUtil masterDbUtil = new DbUtil(masterConn);
        int len =0;
        String tbName = "";
        for (int i = 0; i <constraint.size() ; i++) {
            jsonObject = (JSONObject) constraint.get(i);
            columnList = (List<String>)jsonObject.get("column");
            tbName = jsonObject.getString("table");
            for (String column: columnList) {
                 sql =  getQueryProjMapperSql(column,tbName,"BIM_NUM","BIM_PRJ_PROJ");
                try {
                    List<Map<String,Object>> list = masterDbUtil.excuteQuery(sql,new Object[][]{});
                    //更新业务表关联人员为空的人员id
                    len += masterDbUtil.updateTbCreator(list,tbName,column);
                } catch (SQLException e) {
                    e.printStackTrace();
                    logger.info("查询或插入业务表人员报错"+e.getSQLState());
                }
            }
        }

        try {
            List<Map<String,Object>> list = masterDbUtil.excuteQuery("SELECT table_name FROM USER_TABLES",new Object[][]{});
            String tb =null;
            List<Map<String,Object>> newList =null;
            int i =0;
            for (Map<String,Object> map : list) {
                try{
                    i++;
                    tb = map.get("TABLE_NAME")+"";
                    sql  =  getQueryProjMapperSql("BIM_PROJ",tbName,"BIM_NUM","BIM_PRJ_PROJ");

                    newList = masterDbUtil.excuteQuery(sql,new Object[][]{});
                    len += masterDbUtil.updateTbCreator(newList,tbName,"BIM_PROJ");

                    if(i>0 && i%50==0){
                        logger.info("序号:"+i+"更新"+tb+"表成功");
                        masterConn.commit();
                        masterConn.close();
                        masterConn = DataSourceHelper.GetConnection(masterDataSource);
                        masterDbUtil =new DbUtil(masterConn);
                    }
                }catch (Exception e){
                    logger.error(e.getMessage());
                    continue;
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (null!=masterConn && !masterConn.isClosed()){
                masterConn.close();
            }
        }


    }
}
