package com.neo.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.math.IntMath;
import com.neo.util.JDBCUtil;
import com.neo.util.SqlTools;
import org.apache.log4j.Logger;

import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Auther: Administrator
 * @Date: 2019/6/26/026 17:21
 * @Description:
 */
public class TaskTbMerge  implements Callable<Map<String,Object>> {
    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(TaskTbMerge.class);

    int i; //线程序号
    private AtomicInteger i1;// = new AtomicInteger(i);

    int nums ;//一次同步数据量
    private AtomicInteger nums1 ;//= new AtomicInteger(nums);

    String dbName ;//从库名
    String tbName;//表名
    CountDownLatch endLock;
    int startIndex;
    private AtomicInteger startIndex1; //= new AtomicInteger(startIndex);
    int maxIndex;
    private AtomicInteger maxIndex1 ;//= new AtomicInteger(maxIndex);
    String masterDataSource;
    String uniqueConstraint;

    List<Map<String,Object>> list ;
    List<Map<String,Object>> masterTbStruct;

    public TaskTbMerge(int i, int nums,String masterDataSource, String dbName, String tbName, CountDownLatch endLock,
                    int startIndex, int maxIndex,String uniqueConstraint, List<Map<String,Object>> list ,List<Map<String,Object>> masterTbStruct){
        this.i =i;
        this.nums=nums;
        this.masterDataSource=masterDataSource;
        this.dbName=dbName;
        this.tbName =tbName;
        this.endLock =endLock;
        this.startIndex =startIndex;
        this.maxIndex = maxIndex;
        this.uniqueConstraint =uniqueConstraint;
        nums1 = new AtomicInteger(nums);
        startIndex1 = new AtomicInteger(startIndex);
        maxIndex1 = new AtomicInteger(maxIndex);
        i1 = new AtomicInteger(i);
        this.list =list ;
        this.masterTbStruct = masterTbStruct ;

    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public   Map<String,Object> call() throws Exception {
        int len = 0;
        Map<String,Object> resultMap = new HashMap<>();
        Map<String,Object> returnMap = null;
        try{
          /*  String  querySql = SqlTools.queryDataPager(tbName,startIndex,maxIndex);
            List<Map<String,Object>> list = new JDBCUtil(dbName).excuteQuery(querySql,new Object[][]{});
            returnMap =new JDBCUtil(masterDataSource).batchInsertJsonArry(tbName,list,masterTbStruct);
            list =null;
            System.gc();
            return returnMap;*/


            //JDBCUtil salver  = new JDBCUtil(dbName);
            //查询从库的数据
            if (nums <=3000){
                String  querySql = SqlTools.queryDataPager(tbName,startIndex,maxIndex);
                List<Map<String,Object>> list = new JDBCUtil(dbName).excuteQuery(querySql,new Object[][]{});
                //删除重复的数据

                     batchDelete(list);

                //获取当前主库表结构
                List<Map<String, Object>> masterTbStruct = selectTableStructureByDbAndTb();
                //插入数据
                return new JDBCUtil(masterDataSource).batchInsertJsonArry(tbName,list,masterTbStruct);
            }else{
                int nums2 =nums1.get();
                int newSize =5000;
                int startIndex2= startIndex1.get();
                int maxIndex2 =maxIndex1.get();
                int cycleLenth =  IntMath.mod(nums,newSize) ==0 ? IntMath.divide(nums, newSize, RoundingMode.DOWN)
                        : IntMath.checkedAdd(IntMath.divide(nums, newSize, RoundingMode.DOWN), 1);
                String count1= "";
                String count2= "";
                for (int i = 0; i <cycleLenth ; i++) {
                    int start1 = IntMath.checkedAdd(startIndex, IntMath.checkedMultiply(i,newSize));
                    int end1 =0;
                    if (i!=cycleLenth-1){
                        end1 =  IntMath.checkedAdd(start1,newSize);
                    }else{
                        end1 = maxIndex1.get() ;
                        IntMath.checkedAdd(start1,newSize);
                    }

                    logger.info("起始："+start1+" end："+end1);
                    String  querySql = SqlTools.queryDataPager(tbName,start1,end1);
                    logger.info("当前线程名称："+Thread.currentThread().getName()+" 执行sql:"+querySql);
                    List<Map<String,Object>> list = new JDBCUtil(dbName).excuteQuery(querySql,new Object[][]{});
                    //删除重复的数据

                        batchDelete(list);

                    //获取当前主库表结构
                    List<Map<String, Object>> masterTbStruct = selectTableStructureByDbAndTb();
                    //插入数据
                    returnMap = new JDBCUtil(masterDataSource).batchInsertJsonArry(tbName,list,masterTbStruct);
                     count1= resultMap.get("INSERT_COUNT")==null?"0":resultMap.get("INSERT_COUNT")+"";
                     count2= returnMap.get("INSERT_COUNT")==null?"0":returnMap.get("INSERT_COUNT")+"";
                     resultMap.put("INSERT_COUNT",IntMath.checkedAdd(Integer.valueOf(count1), Integer.valueOf(count2) ));
                }

            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }finally {
            endLock.countDown();//计时器减1

        }
      return returnMap;

    }

    private List<Map<String,Object>> selectTableStructureByDbAndTb() throws SQLException {
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
        return new JDBCUtil(masterDataSource).excuteQuery(sql,new Object[][]{});
    }

    /**
     * 批量删除重复的数据
     * @param data
     * @return
     * @throws Exception
     */
    private int batchDelete(List<Map<String, Object>> data) throws Exception {
        JDBCUtil masterDbUtil =new JDBCUtil(masterDataSource);
        //获得列表中的唯一键
        List<Map<String, Object>> uniqueList = getUniqueConstriant();
        //批量删除重复数据
        return new JDBCUtil(masterDataSource).batchDelete(data, uniqueList, tbName);
    }

    /**
     * 获得唯一约束
     */
    private List<Map<String, Object>> getUniqueConstriant() throws Exception {
        List<Map<String, Object>> list = null;
        JSONArray constraint = JSONArray.parseArray(uniqueConstraint);
        JSONObject jsonObject =null;
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
            int i = hasEAFId();
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

    private int hasEAFId() throws SQLException {
        String  sql="select count(0) as TABLE_NUMS  from user_tab_columns   \n" +
                "where UPPER(column_name)='EAF_ID' AND TABLE_NAME = '"+tbName+"'";
        List<Map<String,Object>> list =new JDBCUtil(masterDataSource).excuteQuery(sql,new Object[][]{});
        String count =list.get(0).get("TABLE_NUMS")+"";
        if ("1".equals(count) ) return 1;
        else return 0;
    }



}
