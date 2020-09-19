package com.neo.util;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 *
 */
public class ThreadPoolUtils
{

    private static Logger logger = Logger.getLogger(ThreadPoolUtils.class);

    //private ThreadPoolTaskExecutor pool;

    //池大小，即也是线程池的最大事务数  并数据源连接池最大值应远超过此值
    private static int POOL_SIZE = 4;

    private static ThreadPoolUtils instance = null;

    private ExecutorService pool;

    private static int MAX_WAIT_QUEUE = 50;

    /**
     * 异步线程工具类构造器
     */
    private ThreadPoolUtils()
    {
        /*		
        pool = new ThreadPoolTaskExecutor();
        // 线程池所使用的缓冲队列
        //poolTaskExecutor.setQueueCapacity(200);
        // 线程池维护线程的最少数量(核心线程数)
        pool.setCorePoolSize(5);
        // 线程池维护线程的最大数量
        pool.setMaxPoolSize(POOL_SIZE);
        // 线程池维护线程所允许的空闲时间
        pool.setKeepAliveSeconds(60000);
        pool.initialize();
        */
        pool = Executors.newFixedThreadPool(POOL_SIZE);
    }

    /**
     * 
     * Description: 关闭线程池
     * @param now 
     * @see
     */
    public void shutdown(boolean now)
    {
        if (now)
        {
            //等待队列不执行
            pool.shutdownNow();
        }
        else
        {
            //等待所有任务运行完毕后关闭
            pool.shutdown();
        }
    }

    /**
     * 
     * Description: 异步线程工具类单例方法
     * @return 
     * @see
     */
    public synchronized static ThreadPoolUtils getInstance()
    {
        if (instance == null)
        {
            instance = new ThreadPoolUtils();
        }
        return instance;
    }

    /**
     * 
     * Description: 线程方式执行runnale实例
     * @param runnale runnale实例
     * @see
     */
    public void execute(Runnable runnale)
    {
        waitwait();
        pool.execute(runnale);
    }

    /**
     * 
     * Description: 线程方式执行Callable实例
     * @param c Callable实例
     * @return  Future实例
     * @see
     */
    public Future<?> submit(Callable<?> c)
    {
        waitwait();
        return pool.submit(c);
    }

    public int getTaskNum()
    {
        int threadCount = ((ThreadPoolExecutor)pool).getActiveCount();
        int waitCount = ((ThreadPoolExecutor)pool).getQueue().size();
        System.out.println("=====thread count:" + threadCount + "=====" + waitCount);
        return threadCount + waitCount;
    }

    /**
     * 
     * Description:  活动线程满时，延缓排队
     * @see
     */
    public void waitwait()
    {
        while (true)
        {
            int threadCount = ((ThreadPoolExecutor)pool).getActiveCount();
            int waitCount = ((ThreadPoolExecutor)pool).getQueue().size();
            System.out.println("=====thread count:" + threadCount + "=====" + waitCount);

            if (threadCount + waitCount >= MAX_WAIT_QUEUE + POOL_SIZE)
            {
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
            else
            {
                break;
            }
        }
    }

    /*public static void main(String[] args)
    {
        final Random random = new Random();
        AtomicInteger c = new AtomicInteger();
        for (int i = 0; i < 100; i++ )
        {
            ThreadPoolUtils.getInstance().execute(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        int m = random.nextInt(50) + 10;
                        Thread.sleep(1000L * m);
                        System.out.println("=========" + c.incrementAndGet());
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        while (c.get() < 100)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("task num:" + ThreadPoolUtils.getInstance().getTaskNum());
        System.out.println("end");
        ThreadPoolUtils.getInstance().shutdown(false);
    }*/


    public static void main(String[] args) throws ClassNotFoundException, SQLException {
       /* Class.forName("com.mysql.jdbc.Driver");
        Class.forName("oracle.jdbc.driver.OracleDriver");
        String url = "jdbc:mysql://localhost:3306/test";
        String username ="root" ;
        String password = "123";
        Connection connection1 = DriverManager.getConnection(url,username, password);
        List<Connection> list = new ArrayList<>();
        list.add(connection1);


        String url1 = "jdbc:oracle:thin:@127.0.0.1:1521/orcl.168.3.5";
        String usename1 ="hdt" ;
        String password1 = "hdt";
        Connection connection2 = DriverManager.getConnection(url1,usename1, password1);
        list.add(connection2);
        connection1.prepareStatement("select * from user ");

        connection1.close();
        connection2.close();*/
        // connection1.close();

/*        Map<String,Map<String,String>> map = new HashMap();
        Map<String,String> map1 = new HashMap<>();
        map.put("1",map1);
        map1.put("2","3");
        System.out.println(map);*/

        List<Map<String, Object>> list =   new JDBCUtil("hdt").excuteQuery(" select * from t_hd_contract ",null);
        System.out.println(list);
    }

}
