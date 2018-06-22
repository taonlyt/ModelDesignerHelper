/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

import java.util.List;

/**
 * 模型操作信息
 *
 * @author tao.luo
 */
public class TableOP {

    private String tableName;//模型名
    /**
     * 1：新建模型 2：重建模型（模型字段顺序发生变化） 3：追加字段（在模型末尾新加字段）
     */
    private String optype;
    private String requireNo;//需求编号
    private String requireName;//需求名称
    private String requireDesc;//需求描述
    private String dataBase;//操作数据库类型
    private String[] columns;//操作字段
    private int startTime;
    private int endTime;

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }
    
    

    public String[] getColumns() {
        return columns;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }

    public String getDataBase() {
        return dataBase;
    }

    public void setDataBase(String dataBase) {
        this.dataBase = dataBase;
    }

    public String getRequireNo() {
        return requireNo;
    }

    public void setRequireNo(String requireNo) {
        this.requireNo = requireNo;
    }

    public String getRequireName() {
        return requireName;
    }

    public void setRequireName(String requireName) {
        this.requireName = requireName;
    }

    public String getRequireDesc() {
        return requireDesc;
    }

    public void setRequireDesc(String requireDesc) {
        this.requireDesc = requireDesc;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getOptype() {
        return optype;
    }

    public void setOptype(String optype) {
        this.optype = optype;
    }

}
