package com.neo.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neo.util.JDBCUtil;
import com.neo.util.SqlTools;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @Auther: Administrator
 * @Date: 2019/6/26/026 17:21
 * @Description:
 */
public class TaskTbMerge  implements Callable<Map<String,String>> {
    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(TaskTbMerge.class);

    int i; //线程序号
    int nums ;//一次同步数据量
    String dbName ;//从库名
    String tbName;//表名
    CountDownLatch endLock;
    final int startIndex;
    final int maxIndex;
    String masterDataSource;
    String uniqueConstraint;

    public TaskTbMerge(int i, int nums,String masterDataSource, String dbName, String tbName, CountDownLatch endLock,
                    int startIndex, int maxIndex,String uniqueConstraint){
        this.i =i;
        this.nums=nums;
        this.masterDataSource=masterDataSource;
        this.dbName=dbName;
        this.tbName =tbName;
        this.endLock =endLock;
        this.startIndex =startIndex;
        this.maxIndex = maxIndex;
        this.uniqueConstraint =uniqueConstraint;
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    @Override
    public Map<String,String> call() throws Exception {
        int len = 0;
        Map<String,String> returnMap = null;
        try{
            //JDBCUtil salver  = new JDBCUtil(dbName);
            //查询从库的数据
            if (nums <5000){
                String  querySql = SqlTools.queryDataPager(tbName,startIndex,maxIndex);
                List<Map<String,Object>> list = new JDBCUtil(dbName).excuteQuery(querySql,new Object[][]{});
                //删除重复的数据
                int i= batchDelete(list);
                //获取当前主库表结构
                List<Map<String, Object>> masterTbStruct = selectTableStructureByDbAndTb();
                //插入数据
                returnMap = new JDBCUtil(masterDataSource).batchInsertJsonArry(tbName,list,masterTbStruct);
            }else{
                int newSize =5000;
                int cycleLenth =nums % newSize ==0 ?nums/newSize:(nums/newSize)+1;
                for (int i = 0; i <newSize ; i++) {
                    int start1 = startIndex+i*newSize;
                    int end1 =0;
                    if (i!=len-1){
                        end1 =  start1+newSize;
                    }else{
                        end1 =  maxIndex;
                    }

                    System.out.println("起始："+start1+" end："+end1);
                }
                returnMap = new HashMap<>();
                returnMap.put("INSERT_COUNT","0");
                returnMap.put("MESSAGE","测试新分页");
            }
            long end = System.currentTimeMillis();
            return returnMap;
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
        return new JDBCUtil(masterDataSource).delete(data, uniqueList, tbName);
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
