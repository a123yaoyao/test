package com.neo.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.neo.util.CollectionUtil;
import com.neo.util.DataSourceHelper;
import com.neo.util.DbUtil;
import com.sun.xml.internal.bind.v2.TODO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    @Value("${threadNum}")
    public String threadNum;


    int getThreads(int dataCount){
        if (dataCount>1000 && dataCount<=10000){
            return dataCount%1000==0? dataCount/1000:dataCount/1000+1;
        }
        if (dataCount>10000 && dataCount<=100000){
            return dataCount%2000==0? dataCount/2000:dataCount/2000+1;
        }
        if (dataCount>100000 && dataCount<=1000000){
            return dataCount%5000==0? dataCount/5000:dataCount/5000+1;
        }
        if (dataCount>1000000 && dataCount<=10000000){
            return dataCount%10000==0? dataCount/10000:dataCount/10000+1;
        }
        return 1;
    }

    int getGroupSize(int dataCount){
        if (dataCount>1000 && dataCount<=10000){
            return 1000;
        }
        if (dataCount>10000 && dataCount<=100000){
            return 2000;
        }
        if (dataCount>100000 && dataCount<=1000000){
            return 5000;
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
    public Integer mergeData(String dbName ,String tbName,String masterDataSource, List<Map<String,Object>> list,int groupSize,
    Connection masterConn,Connection slaverConn
    ) throws Exception {

        DbUtil masterDbUtil =new DbUtil(masterConn);
        DbUtil salverDbUtil =new DbUtil(slaverConn);

        int insertCount =0;
        int count =0;
        int threads = Integer.valueOf(threadNum) ;//多线程数量
        List<Map<String,Object>> tableStructure = null;
        Map<String,Object> param =new HashMap<>();
        //查询被导入数据库的表结构
        List<Map<String, Object>> tb = selectTableStructureByDbAndTb( tbName,salverDbUtil);
        //该主库是否存在此表
        count = checkTable(tbName,masterDbUtil,null);

        String addColumns = ""; //增加的列

        if (0 == count) {//若不存在则主数据源创建新表
            getCreateTableSql(tbName, tb, param);
            String sql = "select  to_char(dbms_metadata.get_ddl('TABLE','"+tbName.toUpperCase()+"')) TB_SQL from dual";
            List<Map<String, Object>> tbCreateList = salverDbUtil.excuteQuery(sql,new Object[][]{});
            sql  =tbCreateList.get(0).get("TB_SQL")+"";
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
                    if (masterTbMap.containsValue(cloumnName)){
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
                    logger.info("为"+dbName+"库添加"+cloumnName+"字段成功！");
                }
            }
        }

        // 查询所有数据
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("tbName", tbName);

        long queryStart =System.currentTimeMillis();
        //查询某个库下的某个表的所有数据
        List<Map<String, Object>> data =
                getDataByMulitThreads( dbName , tbName, masterDataSource,   groupSize, masterConn, slaverConn);
               // selectAllByDbAndTb(dbName, tbName,salverDbUtil,null,null);
        if (data.size()==0) return 0;
        if (data.size()<groupSize) threads =1; //如果同步的数据太少 小于切割的数据条数 则只用一个线程
        Map<String,Object> m =(Map<String,Object>)data.get(0);
        long queryEnd =System.currentTimeMillis();
        logger.info("查询"+dbName+"库 中表名为"+tbName+"的所有数据花费时间为"+(queryEnd-queryStart)/1000+"秒");
        //对数据进行切分
        int cutSize = data.size() /threads ;//每个线程处理的数据量


        //TODO
        if ("EAF_ACM_USER".equals(tbName)){
            saveUserMapper(masterDbUtil,tbName,data,dbName);
            //return 999999;
        }

        //


        List<List<Map<String, Object>>> newData = CollectionUtil.splitList(data, cutSize);

        //批量删除重复的数据
        int delCount = batchDelete(paramsMap, tbName, data, masterDbUtil,salverDbUtil);

        //判断该表是否使用批处理
      //  boolean isUseBatch = checTableIsUseBatch(tbName);

        if (threads == 1 ){ //不开启多线程
            for (List<Map<String, Object>> dat : newData) {
                insertCount += masterDbUtil.batchInsertJsonArry(tbName,dat,tb);
            }
        }else{
            final BlockingQueue<Future<Integer>> queue = new LinkedBlockingQueue<>();
            final CountDownLatch  endLock = new CountDownLatch(threads); //结束门
            List<Future<Integer>> results = new ArrayList<Future<Integer>>();
            final ExecutorService exec = Executors.newFixedThreadPool(threads);
            for (List<Map<String, Object>> dat : newData ) {
                Future<Integer> future= //(Future<Integer>) threadPoolUtils
                        exec.submit(new Callable<Integer>(){
                    @Override
                    public Integer call() {
                        try {

                            return //masterDbUtil. insert(tbName,dat,tb,isUseBatch);
                                    masterDbUtil. batchInsertJsonArry(tbName,dat,tb);
                        }catch(Exception e) {
                            logger.error("数据同步 exception!",e);
                            return 0;
                        }finally {
                            endLock.countDown(); //线程执行完毕，结束门计数器减1
                        }

                    }
                });
                queue.add(future);
            }
            endLock.await(); //主线程阻塞，直到所有线程执行完成
            for(Future<Integer> future : queue)  insertCount +=future.get();
            exec.shutdown(); //关闭线程池
        }

        if (tbName.equals("EAF_DMM_METAATTR_L") || tbName.equals("EAF_DMM_METACLASS_L")){
            masterDbUtil.executeUpdate("delete from "+tbName+" where eaf_lid is null or eaf_lid !='6BEB598696F4116772AF9E03EFC7E962' ",new Object[][]{});
        }
        if (tbName.equals("EAF_ACM_ONLINE") ){
            masterDbUtil.executeUpdate("update eaf_acm_online l set l.eaf_contexlid='6BEB598696F4116772AF9E03EFC7E962' ",new Object[][]{});
            masterDbUtil.executeUpdate("alter table EAF_ACM_ONLINE modify eaf_contexlid default '6BEB598696F4116772AF9E03EFC7E962'",new Object[][]{});
            masterDbUtil.executeUpdate("         update EAF_ACM_ONLINE set eaf_session = null where eaf_session='EAFSYS_9CAA64E618B743A4AAC4D7198D70BF59' and  eaf_loginname ='sysadmin'  " ,new Object[][]{});
        }
        if (tbName.equals("EAF_DMM_METAATTR_M")){
            String[] arrColumns = addColumns.split(",");
            for (int i = 0; i <arrColumns.length ; i++) {
                masterDbUtil.executeUpdate(" alter TABLE  "+tbName+" drop column  "+arrColumns[i],new Object[][]{});
            }
        }
        //测试
        if (tbName.equals("EAF_ACM_USER")){
            masterDbUtil.executeUpdate("  update eaf_acm_user set eaf_phone = 15071228254  " ,new Object[][]{});
            masterDbUtil.executeUpdate("  update eaf_acm_user  set BIM_CATEGORY ='1 正式人员' where eaf_loginname ='sysadmin'  " ,new Object[][]{});
            masterDbUtil.executeUpdate("  delete from EAF_ACM_USER where  EAF_ID = '00000000000000000000000000000000'  and eaf_name is null  " ,new Object[][]{});

        }
        return insertCount;
    }

    private void saveUserMapper(DbUtil masterDbUtil, String tbName, List<Map<String,Object>> data,String dbName) {
        if ("EAF_ACM_USER".equals(tbName)){
          String sql = " select * from eaf_acm_user where EAF_LOGINNAME in (";
          int k=0;
            String eafId = "";
            String loginName ="";
          for (Map m:data) {
                 eafId = m.get("EAF_ID")+"";
                 loginName = m.get("EAF_LOGINNAME")+"";
                 sql+="'"+loginName+"'";
                 if (k!= data.size()-1) sql+=",";
                 k++;
          }
          sql+=")";
            try {
             List<Map<String,Object>> list =    masterDbUtil.excuteQuery(sql,new Object[][]{});
             if (list.size()==0) return;
             boolean flag =false;
             List<Map<String,Object>> recordList =new ArrayList<>();
                Map<String,Object> map =new LinkedHashMap<String,Object>();
                for (Map m: list) {
                    for (Map mm:data) {
                        if (( m.get("EAF_LOGINNAME")+"").equals(mm.get("EAF_LOGINNAME")+"")  && !( m.get("EAF_ID")+"").equals(mm.get("EAF_ID")+"")    ){
                            map =new HashMap<>();
                            map.put("EAF_ID",UUID.randomUUID().toString());
                            map.put("EAF_DB_NAME",dbName);
                            map.put("EAF_TB_NAME",tbName);
                            map.put("EAF_DU_COL","EAF_ID");
                            map.put("EAF_DU_COLV",mm.get("EAF_ID"));
                            map.put("EAF_RE_COL","EAF_LOGINNAME");
                            map.put("EAF_RE_COLV",mm.get("EAF_LOGINNAME"));
                            recordList.add(map);
                            System.out.println("重复的数据为 登录名"+m.get("EAF_LOGINNAME")+"  "+masterDataSource+"库EAF_ID为: "+m.get("EAF_ID")+dbName+"库EAF_ID为： "+mm.get("EAF_ID"));
                            flag =true;
                            break;
                        }
                    }

                }
                if (flag) {
                    try {
                        masterDbUtil.insertTbRecord("EAF_TB_RECORD",recordList);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("插入失败----"+e.getMessage());
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
                logger.error("查询出错"+e.getSQLState());
            }
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
     * 通过库名和表名查询所有数据
     * @param dbName
     * @param tbName
     * @param paramsMap
     * @return
     */
    private List<Map<String,Object>> selectAllByDbAndTb(String dbName, String tbName,  DbUtil dbUtil,Integer startIndex,Integer maxIndex) throws SQLException {

        String sql = " select * from "+tbName;
         if (null !=startIndex && null !=maxIndex)   {
             sql =" SELECT * FROM  ( SELECT A.*, ROWNUM RN  FROM (SELECT * FROM "+tbName+") A  " +
                     "WHERE ROWNUM <= " + maxIndex+
                     ")  \n" +
                     "WHERE RN > "+startIndex;
         }


        System.out.println(sql);
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
}
