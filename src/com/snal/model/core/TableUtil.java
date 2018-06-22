/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.Table;
import com.snal.model.beans.TableCol;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luo Tao
 */
public class TableUtil {

    public static List<Table> cloneTableForBranch(Table table, String[] branches) {
        List<Table> branchTables = new ArrayList();
        LocalDate today = LocalDate.now();
        String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyyMM"));

        table.setCreater("谢英俊");
        table.setCurdutyer("谢英俊");
        for (String tableIdStr : ContextData.oldTableIds) {
            String[] tableIdArray = tableIdStr.split(",");
            if (tableIdArray[0].equalsIgnoreCase(table.getTableName())
                    && tableIdArray[1] != null
                    && tableIdArray[1].trim().length() > 0) {
                table.setTableId(tableIdArray[1]);//从PAAS平台导出的模型唯一标识XMLID
                table.getTablecols().forEach((tablecol) -> {
                    tablecol.setTableId(table.getTableId());
                });
                break;
            }
        }
        if (table.isShared()) {
            for (String branch : branches) {

                Table branchTable = (Table) table.clone();
                branchTable.setMainTable(false);
                branchTable.setTableName(table.getTableName() + "_" + branch);
                branchTable.setTableId(branchTable.getTableName());
                for (String tableIdStr : ContextData.oldTableIds) {
                    String[] tableIdArray = tableIdStr.split(",");
                    if (tableIdArray[0].equalsIgnoreCase(branchTable.getTableName())
                            && tableIdArray[1] != null
                            && tableIdArray[1].trim().length() > 0) {
                        branchTable.setTableId(tableIdArray[1]);//从PAAS平台导出的模型唯一标识XMLID
                        branchTable.getTablecols().forEach((tablecol) -> {
                            tablecol.setTableId(branchTable.getTableId());
                        });
                        break;
                    }
                }
                /**
                 * 如果是共享全省数据，则地市模型分区指向主模型的GMCC分区。否则指向主表的地市分区。
                 */
                if (branchTable.isShareAllDataToCity()) {
                    branchTable.setLocation(branchTable.getLocation() + "/branch=GMCC");
                } else {
                    branchTable.setLocation(branchTable.getLocation() + "/branch=" + branch);
                }
                /**
                 * 地市模型需要去掉地市分区字段branch
                 */
                String[] partycols = branchTable.getPartitionCols();
                String[] newpartycols = new String[partycols.length - 1];
                int i = 0;
                for (String partycol : partycols) {
                    if (!partycol.equalsIgnoreCase("branch")) {
                        newpartycols[i++] = partycol;
                    }
                }
                branchTable.setPartitionCols(newpartycols);
                List<TableCol> tablecols = new ArrayList<>();
                branchTable.getTablecols().stream().filter((branchTablecol) -> (!branchTablecol.getColumnName().equalsIgnoreCase("branch"))).map((branchTablecol) -> {
                    if (branchTablecol.getColumnName().equalsIgnoreCase("month")
                            || branchTablecol.getColumnName().equalsIgnoreCase("day")) {
                        int colseq = Integer.parseInt(branchTablecol.getColumnSeq()) - 1;
                        int partyseq = Integer.parseInt(branchTablecol.getPartitionSeq()) - 1;
                        branchTablecol.setColumnSeq(String.valueOf(colseq));
                        branchTablecol.setPartitionSeq(String.valueOf(partyseq));
                    }
                    return branchTablecol;
                }).forEachOrdered((branchTablecol) -> {
                    tablecols.add(branchTablecol);
                });
                branchTable.setTablecols(tablecols);
                String extendcfg = "{\"EXTERNAL\":\"y\",\"LOCATION\":\"" + branchTable.getLocation() + "\",\"FILEFORMAT\":\"" + branchTable.getStoredFormat() + "\",\"DELIMITER\":\"" + branchTable.getColDelimiter() + "\"}";
                branchTable.setExtendcfg(extendcfg);
                if (branchTable.getTablecols() != null) {
                    branchTable.getTablecols().stream().map((tablecol) -> {
                        /**
                         * 地市模型的列是从主表复制过来的，所以复制过来后需要将列对象中的表名改为地市模型，模型ID也需要改成地市模型的ID。
                         */
                        tablecol.setTableName(branchTable.getTableName());
                        return tablecol;
                    }).map((tablecol) -> {
                        tablecol.setTableId(branchTable.getTableId());
                        return tablecol;
                    }).forEach((tablecol) -> {
                        tablecol.setColumnId(tablecol.getTableName() + "_" + currentMonth + tablecol.getColumnSeq());
                    });
                } else {
                    System.out.println(table.getTableName() + " has no table columns!");
                }
                branchTables.add(branchTable);
            }
        }
        return branchTables;
    }
}
