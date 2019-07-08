package com.neo.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neo.task.TaskTbMerge;
import org.apache.log4j.Logger;

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
 * @Date: 2019/7/1/001 18:40
 * @Description:
 */
public class TaskTbDelete implements Callable<Integer> {

    /**
     * 日志对象
     */
    private Logger logger = Logger.getLogger(TaskTbDelete.class);

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

    public TaskTbDelete(int i, int nums,String masterDataSource, String dbName, String tbName, CountDownLatch endLock,
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
    public Integer call() throws Exception {
        try{
            String  querySql = SqlTools.queryDataPager(tbName,startIndex,maxIndex);
            List<Map<String,Object>> list = new JDBCUtil(dbName).excuteQuery(querySql,new Object[][]{});
            int i= batchDelete(list);
            list =null;
            System.gc();
            return i;
        }
        catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }finally {
            endLock.countDown();//计时器减1

        }
        return 0;

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
