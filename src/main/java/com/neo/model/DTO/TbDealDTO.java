package com.neo.model.DTO;

import com.neo.util.DbUtil;
import com.neo.util.JDBCUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TbDealDTO {

    String tbName;
    String masterDataSource;
    String addColumns;

    public TbDealDTO(String tbName, String masterDataSource,String addColumns) {
        this.tbName = tbName;
        this.masterDataSource = masterDataSource;
        this.addColumns = addColumns;
    }

    public void dealWithTbProblem(){
        if (tbName.equals("EAF_DMM_METAATTR_L") || tbName.equals("EAF_DMM_METACLASS_L")){
            new JDBCUtil(masterDataSource).executeUpdate("delete from "+tbName+" where eaf_lid is null or eaf_lid !='6BEB598696F4116772AF9E03EFC7E962' ",new Object[][]{});
        }
        if (tbName.equals("EAF_ACM_ONLINE") ){
            new JDBCUtil(masterDataSource).executeUpdate("update eaf_acm_online l set l.eaf_contexlid='6BEB598696F4116772AF9E03EFC7E962' ",new Object[][]{});
            new JDBCUtil(masterDataSource).executeUpdate("alter table EAF_ACM_ONLINE modify eaf_contexlid default '6BEB598696F4116772AF9E03EFC7E962'",new Object[][]{});
            new JDBCUtil(masterDataSource).executeUpdate("  update EAF_ACM_ONLINE set eaf_session = null where eaf_session='EAFSYS_9CAA64E618B743A4AAC4D7198D70BF59' and  eaf_loginname ='sysadmin'  " ,new Object[][]{});
        }
        if (tbName.equals("EAF_DMM_METAATTR_M")){
            String[] arrColumns = addColumns.split(",");
            for (int i = 0; i <arrColumns.length ; i++) {
                new JDBCUtil(masterDataSource).executeUpdate(" alter TABLE  "+tbName+" drop column  "+arrColumns[i],new Object[][]{});
            }
        }
        //测试
        if (tbName.equals("EAF_ACM_USER")){
            new JDBCUtil(masterDataSource).executeUpdate("  update eaf_acm_user set eaf_phone = 15071228254  " ,new Object[][]{});
            new JDBCUtil(masterDataSource).executeUpdate("  update eaf_acm_user  set BIM_CATEGORY ='1 正式人员' where eaf_loginname ='sysadmin'  " ,new Object[][]{});
            new JDBCUtil(masterDataSource).executeUpdate("  delete from EAF_ACM_USER where  EAF_ID = '00000000000000000000000000000000'  and eaf_name is null  " ,new Object[][]{});
        }
    }

}
