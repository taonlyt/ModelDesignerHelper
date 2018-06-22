/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

/**
 *
 * @author luotao
 */
public class PartitionCol {

    private String colName;
    private String colValue;

    public PartitionCol(String colName, String colValue) {
        this.colName = colName;
        this.colValue = colValue;
    }

    public String getColName() {
        return colName;
    }

    public void setColName(String colName) {
        this.colName = colName;
    }

    public String getColValue() {
        return colValue;
    }

    public void setColValue(String colValue) {
        this.colValue = colValue;
    }

}
