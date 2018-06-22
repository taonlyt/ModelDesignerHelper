/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luotao
 */
public class TableCol implements Cloneable {

    private int lineId;//模型结构中的行号
    private String dbType;//数据库类型
    private String tableId;//表唯一标识
    private String tableName;//表名
    private String columnId;//字段唯一标识
    private String columnSeq;//字段序号
    private String columnName;//字段名
    private String columnNameZh;//字段中文名
    private String dataType;//数据类型
    private String length;//字段长度
    private String precision;//字段精度
    private String partitionSeq;//分区字段序号
    private boolean isPrimaryKey;//是否主键
    private boolean isNullable;//是否可空
    private String remark;//备注
    private String openState;//开发状态
    private String filedType;//字段类型
    
    private String securityType1;//安全级别一级分类
    private String securityType2;//安全级别二级分类
    private String securityType3;//安全级别三级分类
    private String sensitivityLevel;//敏感级别
    private String outSensitivityId;//脱敏ID
    private String partKeyForPrimaryKey;
    private String dataExample;

    public String getDataExample() {
        return dataExample;
    }

    public void setDataExample(String dataExample) {
        this.dataExample = dataExample;
    }
    
    

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public String getPartKeyForPrimaryKey() {
        return partKeyForPrimaryKey;
    }

    public void setPartKeyForPrimaryKey(String partKeyForPrimaryKey) {
        this.partKeyForPrimaryKey = partKeyForPrimaryKey;
    }
    
    

    public String getSecurityType1() {
        return securityType1;
    }

    public void setSecurityType1(String securityType1) {
        this.securityType1 = securityType1;
    }

    public String getSecurityType2() {
        return securityType2;
    }

    public void setSecurityType2(String securityType2) {
        this.securityType2 = securityType2;
    }

    public String getSecurityType3() {
        return securityType3;
    }

    public void setSecurityType3(String securityType3) {
        this.securityType3 = securityType3;
    }

    public String getOutSensitivityId() {
        return outSensitivityId;
    }

    public void setOutSensitivityId(String outSensitivityId) {
        this.outSensitivityId = outSensitivityId;
    }

    
    
    public String getFiledType() {
        return filedType;
    }

    public void setFiledType(String filedType) {
        this.filedType = filedType;
    }


    @Override
    public TableCol clone() {
        TableCol tablecol = null;
        try {
            tablecol = (TableCol) super.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(TableCol.class.getName()).log(Level.SEVERE, null, ex);
        }
        return tablecol;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnId() {
        return columnId;
    }

    public void setColumnId(String columnId) {
        this.columnId = columnId;
    }

    public String getColumnSeq() {
        return columnSeq;
    }

    public void setColumnSeq(String columnSeq) {
        this.columnSeq = columnSeq;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnNameZh() {
        return columnNameZh;
    }

    public void setColumnNameZh(String columnNameZh) {
        this.columnNameZh = columnNameZh;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getPrecision() {
        return precision;
    }

    public void setPrecision(String precision) {
        this.precision = precision;
    }

    public String getPartitionSeq() {
        return partitionSeq;
    }

    public void setPartitionSeq(String partitionSeq) {
        this.partitionSeq = partitionSeq;
    }

    public boolean isIsPrimaryKey() {
        return isPrimaryKey;
    }

    public String getPrimaryKey() {
        return isPrimaryKey ? "1" : "";
    }

    public void setIsPrimaryKey(String isPrimaryKey) {
        this.isPrimaryKey = "1".equals(isPrimaryKey);
    }

    public boolean isIsNullable() {
        return isNullable;
    }

    public String getNullable() {
        return isNullable ? "Y" : "N";
    }

    public void setIsNullable(String isNullable) {
        this.isNullable = "Y".equals(isNullable);
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOpenState() {
        return openState;
    }

    public void setOpenState(String openState) {
        this.openState = openState;
    }

    @Override
    public String toString() {
        return "TableCol{" + "dbType=" + dbType + ", tableId=" + tableId + ", tableName=" + tableName + ", columnId=" + columnId + ", columnSeq=" + columnSeq + ", columnName=" + columnName + ", columnNameZh=" + columnNameZh + ", dataType=" + dataType + ", length=" + length + ", precision=" + precision + ", partitionSeq=" + partitionSeq + ", isPrimaryKey=" + isPrimaryKey + ", isNullable=" + isNullable + ", sensitivityLevel=" + sensitivityLevel + ", remark=" + remark + ", openState=" + openState + '}';
    }

}
