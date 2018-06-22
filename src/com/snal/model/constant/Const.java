/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.constant;

/**
 *
 * @author Luo Tao
 */
public class Const {

    /**
     * HIVE 模型操作类型编码
     */
    public static final String HIVE_CREATE_TABLE = "H1";//创建表
    public static final String HIVE_ADD_COLUMN = "H2";//追加字段
    public static final String HIVE_CHANGE_COLUMN = "H3";//修改字段
    public static final String HIVE_READD_PARTITION = "H4";//重建分区
    public static final String HIVE_CHANGE_CHARSET = "H5";//修改字符集
    public static final String HIVE_CHANGE_FILEFORMAT = "H6";//修改存储格式
    public static final String HIVE_CHANGE_DELIMITERS = "H7";//修改字段分隔符
    public static final String HIVE_EXPORT_TABLE = "H11";//导出模型
    /**
     * PASS 模型操作类型编码
     */
    public static final String PAAS_PUBLISH_TABLE = "H8";//PAAS发布模型
    public static final String PAAS_OFFLINE_TABLE = "H9";//PAAS下线模型
    public static final String PAAS_SYNC_TABLE="H10";//PAAS模型同步

    /**
     * DB2 模型操作类型编码
     */
    public static final String DB2_CREATE_TABLE = "D1";//新建表
    public static final String DB2_ADD_COLUMN = "D2";//追加字段
    public static final String DB2_CHANGE_DATA_TYPE = "D3";//修改数据类型
    public static final String DB2_EXPORT_TABLE = "D4";//导出模型
    
    
    public static final String HIVE_SUPPORT_OPRTYP="H1,H2,H3,H4,H5,H6,H7,H8,H9,H10,H11";
    public static final String DB2_SUPPORT_OPRTYP="D1,D2,D3,D4";
}
