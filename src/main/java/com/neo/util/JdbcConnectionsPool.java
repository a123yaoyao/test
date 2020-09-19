package com.neo.util;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.*;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class JdbcConnectionsPool implements DataSource {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JdbcConnectionsPool.class);

    /*
     * 使用静态块代码，初始化连接池，创建连接池的中最小链接数量连接，
     * 创建linkedlist集合，将这些连接放入集合中
     */
    //创建linkedlist集合
    //private static LinkedList<Connection> linkedlist1=new LinkedList<Connection>();
    private static Map<String,LinkedList<Connection>> connections=new LinkedHashMap<>();
    private static Map<String,String> urlNameMapperDriver=new LinkedHashMap<>();
    private static Map<String,Integer> driverMax=new LinkedHashMap<>();//每个连接名对应的连接数

    private static String currentConn;
    private static String driver;//
    private static String url;//
    private static String username;//数据库登陆名
    private static String password;//数据库的登陆密码
    private static int jdbcConnectionInitSize =3;//最小连接数量
    private static int max=5; //当前最大连接数量=max*jdbcConnectionInitSize
    static{
        //通过反射机制获取访问db.properties文件
        //InputStream is=JdbcConnectionsPool.class.getResourceAsStream("/db.properties");
        //Properties prop=new Properties();
        try {
            //加载db.properties文件
            //prop.load(is);
            //获取db.properties文件中的数据库连接信息
            //driver=prop.getProperty("driver");
            //url=prop.getProperty("url");
           // username=prop.getProperty("username");
           // password=prop.getProperty("password");
           // jdbcConnectionInitSize=Integer.parseInt(prop.getProperty("jdbcConnectionInitSize"));
            Set<String> driverClassNames = Cache.getDriverClassName();
            for (String driver :driverClassNames ) {
                Class.forName(driver);//加载数据源配置的驱动
            }
            
            Map<String,Object> dsMap  = Cache.getDataSourceMap();
            urlNameMapperDriver = Cache.getUrlNameMapperDriver();
            for (String key : dsMap.keySet()) driverMax.put(key,5);
            Map temp = null;
            LinkedList<Connection> linkedlist= null ;
            for (String urlName: dsMap.keySet()) {
                temp = (Map)dsMap.get(urlName);
                url = temp.get("url")+"";
                username= temp.get("username")+"";
                password=temp.get("password")+"";
                linkedlist = new LinkedList<Connection>();
                //创建最小连接数个数据库连接对象以备使用
                for(int i=0;i<jdbcConnectionInitSize;i++){
                    Connection conn=DriverManager.getConnection(url, username, password);
                    System.out.println(urlName+"获取到了链接" + conn);
                    //将创建好的数据库连接对象添加到Linkedlist集合中
                    linkedlist.add(conn);
                }
                connections.put(urlName,linkedlist);
            }



        }  catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void setCurrentConn(String currentConn) {
        JdbcConnectionsPool.currentConn = currentConn;
    }


    @Override
    public PrintWriter getLogWriter() throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * 实现数据库连接的获取和新创建
     */
    @Override
    public Connection getConnection( ) throws SQLException {
        //如果集合中没有数据库连接对象了，且创建的数据库连接对象没有达到最大连接数量，可以再创建一组数据库连接对象以备使用
        Set<String> hasLoadedDriver = new LinkedHashSet<>();
        LinkedList<Connection> linkedlist= null;
        String driver = null;
        int connNums = 0 ;
        for (String urlName:connections.keySet() ) {
            if (!urlName.equals(currentConn)) continue; //只取当前连接名所属的连接
            linkedlist = connections.get(urlName); //拿出属于这个连接名的连接池
            connNums = driverMax.get(urlName);

            /**************************************************************/
            if(linkedlist.size()==0&&connNums<=5){
                try {
                    driver = urlNameMapperDriver.get(urlName);//加载驱动
                    if (!hasLoadedDriver.contains(driver)) Class.forName(urlNameMapperDriver.get(urlName));
                    hasLoadedDriver.add(urlNameMapperDriver.get(urlName));
                } catch (ClassNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                //
                for(int i=0;i<jdbcConnectionInitSize;i++){
                    Connection conn=DriverManager.getConnection(url, username, password);
                    logger.info(urlName+"获取到了链接" + conn);
                    //将创建好的数据库连接对象添加到Linkedlist集合中
                    linkedlist.add(conn);
                }
                connNums++;
            }

            /**************************************************************/
            if(linkedlist.size()>0) {
                Connection conn1=linkedlist.removeFirst();//从linkedlist集合中取出一个数据库链接对象Connection使用
                logger.info("连接名为："+urlName+" 的数据库连接池大小是" + linkedlist.size());
                /*返回一个Connection对象，并且设置Connection对象方法调用的限制，
                 *当调用connection类对象的close()方法时会将Connection对象重新收集放入linkedlist集合中。
                 */
                LinkedList<Connection> finalLinkedlist = linkedlist;
                LinkedList<Connection> finalLinkedlist1 = linkedlist;
               /* if (conn1.isClosed()){ //连接已关闭
                    finalLinkedlist.add(conn1);
                    System.out.println("连接名为："+urlName+"的 连接 ["+conn1+"]对象被释放，重新放回linkedlist集合中！");
                    System.out.println("此时"+urlName+"所属的Linkedlist集合中有"+ finalLinkedlist.size()+"个数据库连接对象！");
                    return null;
                }*/
                return (Connection) Proxy.newProxyInstance(conn1.getClass().getClassLoader(),//这里换成JdbcConnectionsPool.class.getClassLoader();也行
                        new Class[] { Connection.class }//conn1.getClass().getInterfaces()
                        , new InvocationHandler() {

                            @Override
                            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                                if(!method.getName().equalsIgnoreCase("close")){
                                    return method.invoke(conn1, args);
                                }else{
                                    finalLinkedlist.add(conn1);
                                    System.out.println("连接名为："+urlName+"的 连接 ["+conn1+"]对象被释放，重新放回linkedlist集合中！");
                                    System.out.println("此时"+urlName+"所属的Linkedlist集合中有"+ finalLinkedlist.size()+"个数据库连接对象！");
                                    return null;
                                }
                            }
                        });
            }else{
                logger.info("连接名为："+urlName+"连接数据库失败！");
            }

        }

        return null;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {

        return null;
    }





}