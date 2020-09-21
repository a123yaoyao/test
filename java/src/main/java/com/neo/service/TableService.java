package com.neo.service;

import com.neo.util.CollectionUtil;
import com.neo.util.DataSourceHelper;
import com.neo.util.DbUtil;
import com.neo.util.JDBCUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


@Service
public class TableService {




    private Logger logger = Logger.getLogger(TbService.class);

    @Value("${spring.master.datasource}")
    public String masterDataSource;

    @Value("${spring.dbs}")
    public String dbArray;

    @Value("${groupSize}")
    public String groupSize;

    @Value("${uniqueConstraint}")
    public String uniqueConstraint;

    @Value("${threadNum}")
    public String threadNum;



      public int  sepcialDealWith(String dbName ,String tbName){

          return 0;

      }

















}
