/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

import java.util.List;

/**
 *
 * @author luotao
 */
public class Partition {

    private String tableName;
    private List<PartitionCol> partitions;

    public void removePartitionCol(String colName) {
        if (this.partitions != null) {
            for (PartitionCol partitionCol : partitions) {
                if (partitionCol.getColName().equals(colName)) {
                    partitions.remove(partitionCol);
                    break;
                }
            }
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<PartitionCol> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<PartitionCol> partitions) {
        this.partitions = partitions;
    }

}
