package com.test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTest {

    public static void main(String[] args) {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 200, TimeUnit.MILLISECONDS,

                new ArrayBlockingQueue<Runnable>(5));



        for(int i=0;i<15;i++){

            MyTask myTask = new MyTask(i);

            executor.execute(myTask);


            System.out.println("线程池中线程数目："+executor.getPoolSize()+"，队列中等待执行的任务数目："+

                    executor.getQueue().size()+"，已执行任务数目："+executor.getCompletedTaskCount());

        }

        executor.shutdown();

    }

}





class MyTask implements Runnable {

    private int taskNum;



    public MyTask(int num) {

        this.taskNum = num;

    }



    @Override

    public void run() {

        System.out.println("正在执行task "+taskNum);

        try {
            if( taskNum>0 && taskNum<3)
            Thread.currentThread().sleep(3000);
            if( taskNum>3 && taskNum<6)
                Thread.currentThread().sleep(6000);
            if( taskNum>6 && taskNum<9)
                Thread.currentThread().sleep(9000);

        } catch (InterruptedException e) {

            e.printStackTrace();

        }

        System.out.println("task "+taskNum+"执行完毕");

    }
}
