package com.neo.service;

import com.neo.model.DTO.TbDealDTO;
import com.neo.task.TaskTbMerge;
import com.neo.util.JDBCUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Auther: Administrator
 * @Date: 2019/6/26/026 16:16
 * @Description:
 */
@Service
public class LargeTbService{

    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(LargeTbService.class);


    @Value("${spring.master.datasource}")
    public String masterDataSource;

    @Value("${uniqueConstraint}")
    public String uniqueConstraint;


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
     * 数据合并
     * @param dataNums 从库表数据条数
     * @param tbName   表名
     * @param dbName   库名
     * @return
     * @throws Exception
     */
    public  Map<String ,String> mergeData(int dataNums,String tbName,String dbName) throws Exception {
        long startTime =System.currentTimeMillis();//合并数据开始时间
        Map<String,String> returnMap =new HashMap<>();
        returnMap.put("INSERT_COUNT","0");
        returnMap.put("MESSAGE","执行成功");
        //获得线程数量
        int threads = getThreads(dataNums);
        //对比主库和从库创建表或者增加修改列
        String addColumns = createTable(tbName,dbName);
        if (dataNums ==0 ) return returnMap;//如果数据查询为0条直接返回
        returnMap = insertTbData(threads,dbName,tbName,dataNums);
        // 对表数据进行特殊业务处理
        TbDealDTO tbDealDTO =new TbDealDTO( tbName,masterDataSource, addColumns);
        tbDealDTO.dealWithTbProblem();
        return  returnMap;
    }

    /**
     * 插入数据
     * @param threads 线程数
     * @param dbName  库名
     * @param tbName  表名
     * @param dataCount 从库数据条数
     * @return
     * @throws Exception
     */
    private Map<String,String>  insertTbData(int threads,String dbName,String tbName,int dataCount) throws Exception {
        Map<String,String> returnMap =new HashMap<>();
        returnMap.put("INSERT_COUNT","0");
        returnMap.put("MESSAGE","执行成功");
        int insertCount =0 ;
        //threads=1;//测试
        if (threads==1){
            String sql ="select * from "+tbName;// +" where xml_clob is not null " ;
            List<Map<String,Object>> list = new JDBCUtil(dbName).excuteQuery(sql,new Object[][]{});
            List<Map<String,Object>> masterTbStructor = selectTableStructureByDbAndTb(tbName,  dbName);
            returnMap = //new JDBCUtil(masterDataSource).batchInsertJsonArry(tbName,list,masterTbStructor);
                    new JDBCUtil(masterDataSource).batchInsertJsonArry1(tbName,list,masterTbStructor);
        }else{
            int groupSize =getGroupSize(dataCount);
            final BlockingQueue<Future<Map<String,String>>> queue = new LinkedBlockingQueue<>();
            final CountDownLatch  endLock = new CountDownLatch(threads); //结束门
            final ExecutorService exec = Executors.newFixedThreadPool(threads);
            for (int i = 0; i <threads ; i++) {
                int startIndex = i * groupSize;
                int maxIndex = startIndex + groupSize;
                Future<Map<String,String>> future = exec.submit(new TaskTbMerge(i, groupSize,masterDataSource,dbName,tbName,endLock,startIndex,maxIndex,uniqueConstraint));
                queue.add(future);
            }
            endLock.await(); //主线程阻塞，直到所有线程执行完成
            for(Future<Map<String,String>> future : queue){
                insertCount +=  Integer.valueOf(future.get().get("INSERT_COUNT"));
            } ;
            exec.shutdown(); //关闭线程池
            returnMap.put("INSERT_COUNT",insertCount+"");
        }
        return returnMap;
    }

    /**
     * 创建表或修改添加列
     * @param tbName
     * @param dbName
     * @return
     * @throws SQLException
     */
    private String createTable(String tbName,String dbName) throws SQLException {
       String addColumns ="";
       int  dataNums =  checkTable(tbName);
       if (dataNums==0){//若不存在则主数据源创建新表
           String sql = "select  dbms_metadata.get_ddl('TABLE','"+tbName.toUpperCase()+"') TB_SQL from dual";
           //从库查询创建表的sql语句
           sql = new JDBCUtil(dbName).getCreateTableSql(sql,new Object[][]{});
           String  createSalver =  "CREATE TABLE \""+dbName.toUpperCase()+"\"" +".\""+tbName.toUpperCase() +"\"";
           String createMaster  =  "CREATE TABLE \""+masterDataSource.toUpperCase()+"\""+".\""+tbName.toUpperCase() +"\"";
           sql = sql.replace(createSalver,createMaster);
           new JDBCUtil(masterDataSource).executeUpdate(sql,new Object[][]{});
       }else{//判断表是否需要新增列或者修改列
           List<Map<String, Object>> masterTbStruct = selectTableStructureByDbAndTb(tbName, masterDataSource);
           //查询表结构
           List<Map<String, Object>> tb = selectTableStructureByDbAndTb(tbName,dbName);
           String cloumnName ;
           String dataType ;
           Object dataLength;
           for (Map<String, Object> tbMap:tb ) {
               boolean flag =false;

               cloumnName =tbMap.get("COLUMN_NAME")+"";
               dataType = tbMap.get("DATA_TYPE")+"" ;
               dataLength = tbMap.get("DATA_LENGTH");
               for (Map<String, Object> masterTbMap:masterTbStruct) {
                   if (masterTbMap.containsValue(cloumnName)){//若主库表中字段和从库字段一样 判断长度 取最大的
                       if ("VARCHAR".equals(dataType) || "VARCHAR2".equals(dataType) || "CHAR".equals(dataType)){
                           int len1 =(Integer.valueOf(dataLength+"")) ;
                           int len2 =(Integer.valueOf(masterTbMap.get("DATA_LENGTH")+"")) ;
                           if (len2>len1){
                               String sql = " alter table "+tbName+" modify ("+cloumnName+" "+dataType+"("+len2+"))";
                               new JDBCUtil(masterDataSource).executeUpdate(sql,new Object[][]{});
                           }
                           if (len1>len2){
                               String sql = " alter table "+tbName+" modify ("+cloumnName+" "+dataType+"("+len1+"))";
                               new JDBCUtil(masterDataSource).executeUpdate(sql,new Object[][]{});
                           }
                       }
                       flag = true;
                       break;
                   }
               }
               if (!flag){//增加列
                   addColumns +=cloumnName+",";
                   String sql = " alter table "+tbName+" add ("+cloumnName+" "+dataType;
                   if (null!= dataLength && !"CLOB".equals(dataType) && !"BLOB".equals(dataType)){
                       sql+="("+dataLength+")";
                   }
                   sql+=" ) ";
                   new JDBCUtil(masterDataSource).executeUpdate(sql,new Object[][]{});
                   logger.info("为"+masterDataSource+"库 "+tbName+"添加"+cloumnName+"字段成功！");
               }
           }
       }
       return addColumns;
    }

    private int checkTable(String tbName) throws SQLException {
      String  sql =" SELECT COUNT(*) TABLE_NUMS FROM User_Tables WHERE table_name = '"+tbName+"' " ;
      List<Map<String,Object>> list = new JDBCUtil(masterDataSource).excuteQuery(sql,new Object[][]{});
      String count =list.get(0).get("TABLE_NUMS")+"";
      if ("1".equals(count) ) return 1;
      else return 0;
    }

    /**
     * 通过库名和表名查询数据库表结构
     *
     * @param tbName
     * @return
     */
    private List<Map<String,Object>> selectTableStructureByDbAndTb( String tbName, String dbName) throws SQLException {
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
        return new JDBCUtil(dbName).excuteQuery(sql,new Object[][]{});
    }



}
