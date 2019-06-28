package com.neo.util;

import ch.qos.logback.core.db.dialect.DBUtil;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: Administrator
 * @Date: 2019/3/21/021 15:43
 * @Description:
 */
public class DataSourceHelper {
    /**
     * 日志对象
     */
    private static Logger logger = Logger.getLogger(JDBCUtil.class);

    private static Connection conn;


    static ApplicationContext context = SpringContextUtil.getApplicationContext();


    public static Connection GetConnection(String dbName) {
        Connection connection = null;

        try {
            String driver = context.getEnvironment().getProperty("spring.datasource."+dbName+".driverClassName");
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                logger.error("加载驱动错误");
                logger.error(e.getMessage());
                e.printStackTrace();
            }

            String url = context.getEnvironment().getProperty("spring.datasource."+dbName+".url");
            String username = context.getEnvironment().getProperty("spring.datasource."+dbName+".username");
            String password = context.getEnvironment().getProperty("spring.datasource."+dbName+".password");
            connection = DriverManager.getConnection(url, username, password);

            return connection;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

   /* public static synchronized  Connection GetConnection(String dbName)throws Exception
    {
        if(conn==null || conn.isClosed())
        {
            synchronized (DBUtil.class){
                if(conn==null || conn.isClosed()){
                    String url = context.getEnvironment().getProperty("spring.datasource."+dbName+".url");
                    String username = context.getEnvironment().getProperty("spring.datasource."+dbName+".username");
                    String password = context.getEnvironment().getProperty("spring.datasource."+dbName+".password");
                    conn = DriverManager.getConnection(url, username, password);
                }
            }
        }

        return conn;
    }
*/




}
