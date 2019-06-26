package com.neo.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neo.service.LargeTbService;
//import com.neo.service.TableService;
import com.neo.service.TbService;
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

   /* @Autowired
    TableService tableService;*/
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
               //"user_list";
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
    public String getTableByDB(String dbName,String tbName, String page,
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
        return StringUtils.MapToString(map);
    }

    @RequestMapping("/mergeData")
    @ResponseBody
    public String mergeData(String dbName, String tbCollection) throws Exception {
        Connection masterConn = null ;//主库连接
        Connection slaverConn = null;//从库连接
        int groupSiz = 0; //每张表数据插入多次 一次插入的数据条数
        List<Map<String, Object>> list = null;
        List<Map<String, String>> result = new ArrayList<>();
        Map<String,Object> resu =new HashMap<>();
        String tbName = null;

        Map<String, String> resultMap = null;
        try{
            masterConn = DataSourceHelper.GetConnection(masterDataSource);
            slaverConn = DataSourceHelper.GetConnection(dbName);
            Long start =   System.currentTimeMillis();
            groupSiz= Integer.valueOf(   groupSize  );
            list = getParamList(tbCollection, "tbs");
            int k=0;
            int count=0;
            String insertCount ="";
            DbUtil salverDbUtil =new DbUtil(slaverConn);
            for (Map<String, Object> map : list) {
                tbName = map.get("TABLE_NAME") + "";
                count = salverDbUtil.getCount("select count(*) from "+tbName,new Object[][]{});
                resultMap = new HashMap();
                resultMap.put("TABLE_NAME", tbName);
                if (count<1){
                    insertCount =tbService.mergeData(dbName, tbName, masterDataSource, list, Integer.valueOf(groupSiz),masterConn,slaverConn)+"";
                }else{
                    insertCount = largeTbService. mergeData( count, tbName, dbName)+"";
                }
                resultMap.put("INSERT_COUNT",insertCount );
                result.add(resultMap);
                k++;
                if (k %50 == 0){//循环50次就关一次连接再重新获取、、
                    closeConn(masterConn,slaverConn);
                    masterConn = DataSourceHelper.GetConnection(masterDataSource);
                    slaverConn = DataSourceHelper.GetConnection(dbName);
                    salverDbUtil =new DbUtil(slaverConn);
                }
            }
            resu.put("list",result);
            Long end =   System.currentTimeMillis();
            masterConn.commit();
            logger.info("插入数据所花费的时间为"+ (end-start) /1000 +"s");
        }catch (Exception e){
            resu.put("err",true);
            resu.put("content",e.getMessage());
            logger.error(e.getMessage());
        }finally {

            closeConn(masterConn,slaverConn);

           /* if (null!=masterConn){
                masterConn.close();
            }
            if (null!=slaverConn){
                slaverConn.close();
            }*/
        }

        return StringUtils.MapToString(resu);


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
    public String updateUser(String dbName, String tbCollection) throws Exception {
        Connection masterConn = null ;//主库连接
        Connection slaverConn = null;//从库连接
        int groupSiz = 0; //每张表数据插入多次 一次插入的数据条数
        List<Map<String, Object>> list = null;
        List<Map<String, String>> result = new ArrayList<>();
        Map<String,Object> resu =new HashMap<>();
        String tbName = null;

        Map<String, String> resultMap = null;
        try{
            masterConn = DataSourceHelper.GetConnection(masterDataSource);
            slaverConn = DataSourceHelper.GetConnection(dbName);
            Long start =   System.currentTimeMillis();
            groupSiz= Integer.valueOf(   groupSize  );
            list = getParamList(tbCollection, "tbs");
            int k=0;
            int count=0;
            DbUtil salverDbUtil =new DbUtil(slaverConn);
            for (Map<String, Object> map : list) {
                tbName = map.get("TABLE_NAME") + "";
                resultMap = new HashMap();
                resultMap.put("TABLE_NAME", tbName);
                resultMap.put("INSERT_COUNT", tbService.updateUser(dbName, tbName, masterDataSource, Integer.valueOf(groupSiz),masterConn,slaverConn)+"");
                result.add(resultMap);
            }
            resu.put("list",result);
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
            if (null!=slaverConn){
                slaverConn.close();
            }
        }

        return StringUtils.MapToString(resu);


    }



    /**
     * 解析json
     *
     * @param tbCollection
     * @return
     */
    private List<Map<String, Object>> getParamList(String tbCollection, String key)throws Exception {
        JsonParser jsonParser = new JsonParser();
        JsonObject jo = (JsonObject) jsonParser.parse(tbCollection);
        JsonArray jsonArr = jo.getAsJsonArray(key);
        Gson googleJson = new Gson();
        return googleJson.fromJson(jsonArr, ArrayList.class);
    }


}