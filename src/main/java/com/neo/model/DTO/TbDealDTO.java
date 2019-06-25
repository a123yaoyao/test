package com.neo.model.DTO;

import com.neo.util.DbUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TbDealDTO {

    String tbName;
    DbUtil masterDbUtil;
    String addColumns;

    public TbDealDTO(String tbName, DbUtil dbUtil,String addColumns) {
        this.tbName = tbName;
        this.masterDbUtil = dbUtil;
        this.addColumns = addColumns;
    }

    public void dealWithTbProblem(){
        if (tbName.equals("EAF_DMM_METAATTR_L") || tbName.equals("EAF_DMM_METACLASS_L")){
            masterDbUtil.executeUpdate("delete from "+tbName+" where eaf_lid is null or eaf_lid !='6BEB598696F4116772AF9E03EFC7E962' ",new Object[][]{});
        }
        if (tbName.equals("EAF_ACM_ONLINE") ){
            masterDbUtil.executeUpdate("update eaf_acm_online l set l.eaf_contexlid='6BEB598696F4116772AF9E03EFC7E962' ",new Object[][]{});
            masterDbUtil.executeUpdate("alter table EAF_ACM_ONLINE modify eaf_contexlid default '6BEB598696F4116772AF9E03EFC7E962'",new Object[][]{});
            masterDbUtil.executeUpdate("         update EAF_ACM_ONLINE set eaf_session = null where eaf_session='EAFSYS_9CAA64E618B743A4AAC4D7198D70BF59' and  eaf_loginname ='sysadmin'  " ,new Object[][]{});
        }
        if (tbName.equals("EAF_DMM_METAATTR_M")){
            String[] arrColumns = addColumns.split(",");
            for (int i = 0; i <arrColumns.length ; i++) {
                masterDbUtil.executeUpdate(" alter TABLE  "+tbName+" drop column  "+arrColumns[i],new Object[][]{});
            }
        }
        //测试
        if (tbName.equals("EAF_ACM_USER")){
            masterDbUtil.executeUpdate("  update eaf_acm_user set eaf_phone = 15071228254  " ,new Object[][]{});
            masterDbUtil.executeUpdate("  update eaf_acm_user  set BIM_CATEGORY ='1 正式人员' where eaf_loginname ='sysadmin'  " ,new Object[][]{});
            masterDbUtil.executeUpdate("  delete from EAF_ACM_USER where  EAF_ID = '00000000000000000000000000000000'  and eaf_name is null  " ,new Object[][]{});

        }
    }

}
