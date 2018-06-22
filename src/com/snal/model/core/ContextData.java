/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.Table;
import com.snal.model.beans.TenantAttribute;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 存储频繁使用的数据，方便使用，避免过度参数传递。
 *
 * @author Luo Tao
 */
public class ContextData {

    public static Properties configration;//配置信息
    public static Map<String, TenantAttribute> tenantMap;//租户信息
    public static Map<String, Table> hiveTableMap;//hive模型信息
    public static Map<String, Table> db2TableMap;//db2模型信息
    public static List<String> oldTableIds;//最初一部分表ID。因为当时有一部分表的ID是自定义的，所以每次修改这部分表的时候仍然需要采用原来就的ID。最新的表ID是跟表名相同，

    public static String getProperty(String key) {
        return configration.getProperty(key);
    }

    /**
     * 加载HIVE元数据字典
     * @param tableNames
     */
    public static void loadHiveMetaData(List<String> tableNames) {
        MetaDataLoader loader = new MetaDataLoader();
        //两份元数据字典，hive一份，DB2一份，两份结构一致。
        String hiveMetaDataFile = ContextData.getProperty("hive.meta.data.file");//hive元数据文件路径
        //读取元数据字典excel时必须要用到的参数
        int startsheet = Integer.parseInt(ContextData.getProperty("start.sheet.index"));
        int endsheet = Integer.parseInt(ContextData.getProperty("end.sheet.index"));
        String[] sheetmincell = ContextData.getProperty("sheet.min.cell.load").split(",");//两份EXCEL 需要读取的sheet。
        int[] mincelltoread = {Integer.parseInt(sheetmincell[0]), Integer.parseInt(sheetmincell[1])};//两份excel至少读取的列数。
        try {
            hiveTableMap = loader.loadMetaData(hiveMetaDataFile, tableNames, startsheet, endsheet, mincelltoread);
        } catch (Exception ex) {
            Logger.getLogger(ContextData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * 加载DB2元数据字典
     * @param tableNames
     */
    public static void loadDB2MetaData(List<String> tableNames) {
        MetaDataLoader loader = new MetaDataLoader();
        //两份元数据字典，hive一份，DB2一份，两份结构一致。
        String db2MetaDataFile = ContextData.getProperty("db2.meta.data.file");//db2元数据文件路径
        //读取元数据字典excel时必须要用到的参数
        int startsheet = Integer.parseInt(ContextData.getProperty("start.sheet.index"));
        int endsheet = Integer.parseInt(ContextData.getProperty("end.sheet.index"));
        String[] sheetmincell = ContextData.getProperty("sheet.min.cell.load").split(",");//需要读取的sheet。
        int[] mincelltoread = {Integer.parseInt(sheetmincell[0]), Integer.parseInt(sheetmincell[1])};//excel至少读取的列数。
        try {
            db2TableMap = loader.loadMetaData(db2MetaDataFile,tableNames , startsheet, endsheet, mincelltoread);
        } catch (Exception ex) {
            Logger.getLogger(ContextData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
