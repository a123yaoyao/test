package com.neo.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neo.service.LargeTbService;
import com.neo.service.TableService;
import com.neo.service.TbService;
import com.neo.util.CollectionUtil;
import com.neo.util.DataSourceHelper;
import com.neo.util.DbUtil;
import com.neo.util.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/")
public class TbController  {


    @Autowired
    TbService tbService;

    @Autowired
    TableService tabService;


    @Autowired
    LargeTbService largeTbService;

    @Value("${spring.master.datasource}")
    public String masterDataSource;

    @Value("${spring.dbs}")
    public String dbArray;

    @Value("${groupSize}")
    public String groupSize;


    private Logger logger = Logger.getLogger(TbController.class);



    @RequestMapping("/")
    public String index() {
       return "index";
    }

    @RequestMapping("/getAllByDB")
    @ResponseBody
    public String getAllByDB() {

        List<Map<String, Object>> list = null ;
        Map<String,Object> result =new HashMap<>();
        try{
            list= tbService.getAllByDB(dbArray, masterDataSource);
            result.put("list",list);
        }catch (Exception e){
            result.put("err",true);
            result.put("content",e.getMessage());
            e.printStackTrace();
        }
        return StringUtils.MapToString(result);
    }


    @RequestMapping("/getTableByDB")
    @ResponseBody
    public  Map<String, Object> getTableByDB(String dbName,String tbName, String page,
                                 String rows, String sort, String order) throws SQLException {
        Map<String, Object> map = null;
        Connection conn = null ;
        try {
            if (null== dbName || dbName.trim().equals("")) dbName =masterDataSource;
            conn = DataSourceHelper.GetConnection(dbName);
            map = tbService.getTableByDB(dbName,tbName, page, rows, sort, order,conn);
        } catch (Exception e) {
            map.put("err", true);
            map.put("content", e.getMessage());
        }finally {
            if (conn != null) {
                conn.close();
            }
        }
       return map;
    }

    @RequestMapping("/mergeData")
    @ResponseBody
    public Map<String,Object> mergeData(String dbName, String tbCollection) throws Exception {
        Connection masterConn = null ;//主库连接
        Connection slaverConn = null;//从库连接
        List<Map<String, Object>> list = null;
        List<Map<String, String>> result = new ArrayList<>();
        Map<String,Object> resu =new HashMap<>();
        String tbName = null;
        Map<String, String> resultMap = null;
        Map<String ,Object> insertMessageMap = null;
        try{
            masterConn = DataSourceHelper.GetConnection(masterDataSource);
            slaverConn = DataSourceHelper.GetConnection(dbName);
            Long start =   System.currentTimeMillis();

            list = CollectionUtil.getParamList(tbCollection, "tbs");
            int insertCountRecord =0 ;
            int k=0;
            int count=0;
            String insertCount ="";
            DbUtil salverDbUtil =new DbUtil(slaverConn);
            for (Map<String, Object> map : list) {

                tbName = map.get("TABLE_NAME") + "";

                count = salverDbUtil.getCount("select count(*) from "+tbName,new Object[][]{});
                insertCountRecord += count;//记录 每张表的数据条数的和
                resultMap = new HashMap();
                resultMap.put("TABLE_NAME", tbName);
                insertMessageMap =tbService.mergeData(dbName, tbName, masterDataSource, list, Integer.valueOf(groupSize),masterConn,slaverConn);

               /* if (count<8000){
                    insertMessageMap =tbService.mergeData(dbName, tbName, masterDataSource, list, Integer.valueOf(groupSize),masterConn,slaverConn);

                }else{
                    insertMessageMap = largeTbService. mergeData( count, tbName, dbName);
                }*/
                if (tbName.equals("EAF_DMM_METAATTR_M")){ //如果是模型属性类添加数据 同时要给资源表添加数据
                    tbService.mergeData(dbName, "EAF_DMM_RESOURCE", masterDataSource, list, Integer.valueOf(groupSize),masterConn,slaverConn);

                }
                resultMap.put("INSERT_COUNT",insertMessageMap.get("INSERT_COUNT")+"" );
                resultMap.put("MESSAGE",insertMessageMap.get("MESSAGE")+"" );
                result.add(resultMap);
                k++;
                if (k %50 == 0 || insertCountRecord >18000 ){//循环50次就关一次连接再重新获取、、
                    closeConn(masterConn,slaverConn);
                    masterConn = DataSourceHelper.GetConnection(masterDataSource);
                    slaverConn = DataSourceHelper.GetConnection(dbName);
                    salverDbUtil =new DbUtil(slaverConn);
                    insertCountRecord =0 ;//重置表条数记录
                }
            }
            resu.put("list",result);
            Long end =   System.currentTimeMillis();
            masterConn.commit();
            logger.info("插入数据所花费的时间为"+ (end-start) /1000 +"s");
        }catch (Exception e){
            resu.put("err",true);
            resu.put("content",e.getMessage());
            if (e.getMessage().contains("java.lang.OutOfMemoryError")){
                resu.put("content","内存不足");
            }

            logger.error(e.getMessage());
        }finally {
            closeConn(masterConn,slaverConn);//关闭所有数据源
        }

        return resu;
    }

    private int  specialDealTble(String dbName,String tbName) {
        if(tbName.equals("EAF_ACM_USER")){
           return tabService.sepcialDealWith(dbName,tbName);
        }
        return 1;
    }

    private void closeConn(Connection masterConn, Connection slaverConn) {

        try {
            if (null!=masterConn && !masterConn.isClosed()){
                try {
                    masterConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            if (null!=slaverConn && !slaverConn.isClosed()){
                try {
                    slaverConn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/updateUser")
    @ResponseBody
    public Map<String,Object> updateUser() throws Exception {
        Map<String,Object> resu =new HashMap<>();
        try{
            tbService.updateUser1( );
        }catch (Exception e){
            resu.put("err",true);
            resu.put("content",e.getMessage());
            logger.error(e.getMessage());
        }
        return resu;


    }

    @RequestMapping("/updateOrg")
    @ResponseBody
    public Map<String,Object> updateOrg(String dbName, String tbCollection) throws Exception {
        Connection masterConn = null ;//主库连接
        int groupSiz = 0; //每张表数据插入多次 一次插入的数据条数
        List<Map<String, Object>> list = null;
        List<Map<String, String>> result = new ArrayList<>();
        Map<String,Object> resu =new HashMap<>();
        String tbName = null;
        Map<String, String> resultMap = null;
        try{
            masterConn = DataSourceHelper.GetConnection(masterDataSource);
            Long start =   System.currentTimeMillis();
            tbService.updateOrg1(masterConn);
            Long end =   System.currentTimeMillis();
            masterConn.commit();
            logger.info("插入数据所花费的时间为"+ (end-start) /1000 +"s");
        }catch (Exception e){
            resu.put("err",true);
            resu.put("content",e.getMessage());
            logger.error(e.getMessage());
        }finally {
            if (null!=masterConn){
                masterConn.close();
            }

        }

        return resu;


    }

    @RequestMapping("/updateProj")
    @ResponseBody
    public Map<String,Object> updateProj(String dbName, String tbCollection) throws Exception {
        Map<String,Object> resu =new HashMap<>();
        try{
            tbService.updateProj();
        }catch (Exception e){
            resu.put("err",true);
            resu.put("content",e.getMessage());
            logger.error(e.getMessage());
        }
        return resu;

    }

    @RequestMapping("/getTableStruct")
    @ResponseBody
    public Map<String,Object>  getTableStruct(String dbName,String tbName) throws SQLException {
        List<Map<String, Object>> list= null;
        Map<String,Object> map =new HashMap<>();
        Connection conn = null ;
        try {
            if (null== dbName || dbName.trim().equals("")) dbName =masterDataSource;
            conn = DataSourceHelper.GetConnection(dbName);
            list = tbService.getTableStruct(dbName,tbName,conn);
            map.put("list",list);
        } catch (Exception e) {
            map.put("err", true);
            map.put("content", e.getMessage());
        }finally {
            if (conn != null) {
                conn.close();
            }
        }
        return map;
    }

    @RequestMapping("/editTableStruct")
    @ResponseBody
    public Map<String,Object> editTableStruct(String dbName,String tbName,String content) throws SQLException {
        List<Map<String, Object>> list= null;
        Map<String,Object> map =new HashMap<>();
        Connection conn = null ;
        try {
            if (null== dbName || dbName.trim().equals("")) dbName =masterDataSource;
            JsonParser jsonParser = new JsonParser();
            JsonObject jo = (JsonObject) jsonParser.parse(content);
            list = CollectionUtil.getParamList(content, "content");
            conn = DataSourceHelper.GetConnection(dbName);

            map = tbService.editTableStruct(dbName,tbName, jo,conn);
            map.put("code",200);
        } catch (Exception e) {
            map.put("err", true);
            map.put("content", e.getMessage());
        }finally {
            if (conn != null) {
                conn.close();
            }
        }
        return map;
    }






}