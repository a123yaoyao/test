/*
package com.neo.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neo.service.TbService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

*/
/**
 * @Auther: Administrator
 * @Date: 2019/5/13/013 18:46
 * @Description:
 *//*

public class SyncTask implements Callable<Integer>{

    */
/**
     * 日志对象
     *//*

    private Logger logger = Logger.getLogger(SyncTask.class);



    int i; //线程序号
    int nums ;//一次同步数据量
    String dbName ;//从库名
    String tbName;//表名
    CountDownLatch endLock;
    int startIndex;
    int maxIndex;
    String masterDataSource;
    String uniqueConstraint;

    public SyncTask(int i, int nums,String masterDataSource, String dbName, String tbName, CountDownLatch endLock,
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

    */
/**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     *//*

    @Override
    public Integer call() throws Exception {
        JDBCUtil salver  = new JDBCUtil(dbName);
        int len = 0;
        String   querySql =" SELECT * FROM  ( SELECT A.*, ROWNUM RN  FROM (SELECT * FROM ("+tbName+")t ) A  " +
                    "WHERE ROWNUM <= " + maxIndex+
                    ")  \n" +
                    "WHERE RN > "+startIndex;


         long start = System.currentTimeMillis();


         Map<String,Object> result = salver.excuteQueryWithMuliResult(tbName,querySql,new Object[][]{});

         JDBCUtil master =new JDBCUtil(masterDataSource);
         int i= batchDelete((List<Map<String, Object>>)result.get("list") );
         master.batchInsert(tbName,result);
         long end = System.currentTimeMillis();
         printMessage(start,end);
         endLock.countDown();//计时器减1
        return  len ;

    }

    */
/**
     * 批量删除重复的数据
     * @param data
     * @return
     * @throws Exception
     *//*

    private int batchDelete(List<Map<String, Object>> data) throws Exception {
        JDBCUtil masterDbUtil =new JDBCUtil(masterDataSource);
        //获得列表中的唯一键
        List<Map<String, Object>> uniqueList = getUniqueConstriant( masterDbUtil);
        //批量删除重复数据
        return masterDbUtil.batchDelete(data, uniqueList, tbName);

    }


    */
/**
     * 获得唯一约束
     * @param dbUtil
     * @return
     *//*

    private List<Map<String, Object>> getUniqueConstriant( JDBCUtil dbUtil) throws Exception {
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
            int i =  checkTable(  tbName, dbUtil,"EAF_ID");
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

    private int checkTable(String tbName, JDBCUtil dbUtil, String Column) {
        String sql = null ;
        if(Column ==null )
            sql =" SELECT COUNT(*) TABLE_NUMS FROM User_Tables WHERE table_name = '"+tbName+"' " ;
        else sql="select count(0) as TABLE_NUMS  from user_tab_columns   \n" +
                "where UPPER(column_name)='"+Column+"' AND TABLE_NAME = '"+tbName+"'";

        List<Map<String,Object>> list = null;
        list = dbUtil.excuteQuery(sql,new Object[][]{});
        String count =list.get(0).get("TABLE_NUMS")+"";
        if ("1".equals(count) ) return 1;
        else return 0;
    }


    private void printMessage(long start ,long end ) {
        long time =end - start ;
        String message = "当前线程 : "+Thread.currentThread().getName()+"从"+dbName+"库同步到"+masterDataSource+"库需要";
        if (time >=0l && time<1000l){
            message += time+"毫秒";
        }else if(time>=1000 && time<60000){
            message += time/1000 +"秒";
        }else {
            message += time/60000 +"分钟";
        }
        logger.info(message);
    }


}
*/
