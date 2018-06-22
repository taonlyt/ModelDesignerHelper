/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.Table;
import com.snal.model.beans.TableCol;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Luo Tao
 */
public class DB2ModelSQLBuilder {

    /**
     * 生成DB2建表语句
     *
     * @param table
     * @param branchList
     * @return
     */
    public String makeCreateTable(Table table, String[] branchList) {
        String sqlComment = makeSqlComment(table);
        StringBuilder basebuff = new StringBuilder(sqlComment);//建表基本语句
        StringBuilder commentbuff = new StringBuilder();//备注
        List<String> partitionkeys = new ArrayList<>();//分区键
        List<String> primarykeys = new ArrayList<>();//主键
        basebuff.append("CREATE TABLE ").append(table.getTableModel()).append(".").append(table.getTableName()).append(" (").append("\n");
        commentbuff.append("COMMENT ON TABLE ").append(table.getTableModel()).append(".")
                .append(table.getTableName()).append(" IS '")
                .append(table.getTableNameZh()).append("';\n");
        int colcount = 0;
        if (table.getTablecols().isEmpty()) {
            return null;
        }
        for (TableCol tablecol : table.getTablecols()) {
            /**
             * 表字段
             */
            basebuff.append("    ");
            basebuff.append(tablecol.getColumnName()).append(" ").append(tablecol.getDataType());
            if (tablecol.isIsPrimaryKey() || !tablecol.isIsNullable()) {
                basebuff.append(" NOT NULL ");
            }
            if (colcount < table.getTablecols().size() - 1) {
                basebuff.append(",");
            }
            basebuff.append("\n");
            /**
             * 表和表字段备注信息
             */
            commentbuff.append("COMMENT ON COLUMN ").append(table.getTableModel()).append(".")
                    .append(table.getTableName()).append(".").append(tablecol.getColumnName())
                    .append(" IS '").append(tablecol.getColumnNameZh()).append("';\n");
            /**
             * 表分区字段
             */
            if (tablecol.getPartitionSeq() != null && !tablecol.getPartitionSeq().isEmpty()) {
                partitionkeys.add(tablecol.getColumnName());
            }
            /**
             * 主键字段
             */
            if (tablecol.isIsPrimaryKey()) {
                primarykeys.add(tablecol.getColumnName());
            }
            colcount++;
        }
        basebuff.append(")");
        if (!partitionkeys.isEmpty()) {
            basebuff.append("\n PARTITIONING KEY (");
            int count = 0;
            for (String partitionkey : partitionkeys) {
                if (count == 0) {
                    basebuff.append(partitionkey);
                } else {
                    basebuff.append(",").append(partitionkey);
                }
                count++;
            }
            basebuff.append(")");
        }
        basebuff.append(";\n");
        basebuff.append(commentbuff.toString()).append("\n");
        if (!primarykeys.isEmpty()) {
            basebuff.append("ALTER TABLE ").append(table.getTableModel()).append(".").append(table.getTableName())
                    .append(" ADD PRIMARY KEY (");
            int count = 0;
            for (String primarykey : primarykeys) {
                if (count != 0) {
                    basebuff.append(",");
                }
                basebuff.append(primarykey);
                count++;
            }
            basebuff.append(");\n");
        }
        basebuff.append("\n");
        return basebuff.toString();
    }

    /**
     * 追加字段
     *
     * @param table
     * @param columns
     * @return
     */
    public String addColumns(Table table, String[] columns) {
        String sqlComment = makeSqlComment(table);
        StringBuilder sqlbuffer = new StringBuilder(sqlComment);
        for (String column : columns) {
            TableCol tableCol = table.getTableCol(column);
            if(tableCol==null){
                System.out.println("[ERROR] Can not found column "+table.getTableName()+"."+column);
            }
            sqlbuffer.append("ALTER TABLE ").append(table.getTableModel()).append(".").append(table.getTableName())
                    .append(" ADD COLUMN ").append(column).append(" ").append(tableCol.getDataType()).append(";\n");

            sqlbuffer.append("COMMENT ON COLUMN ").append(table.getTableModel()).append(".")
                    .append(table.getTableName()).append(".").append(tableCol.getColumnName())
                    .append(" IS '").append(tableCol.getColumnNameZh()).append("';\n\n");
        }
        return sqlbuffer.toString();
    }

    /**
     * 修改数据类型
     *
     * @param table
     * @param columns
     * @return
     */
    public String changeDataType(Table table, String[] columns) {
        String sqlComment = makeSqlComment(table);
        StringBuilder sqlbuffer = new StringBuilder(sqlComment);
        for (String column : columns) {
            TableCol tableCol = table.getTableCol(column);
            sqlbuffer.append("ALTER TABLE ").append(table.getTableModel()).append(".").append(table.getTableName())
                    .append(" ALTER COLUMN ").append(column).append(" SET DATA TYPE ").append(tableCol.getDataType()).append(";\n\n");
        }
        return sqlbuffer.toString();
    }

    private String makeSqlComment(Table table) {
        StringBuilder sqlbuffer = new StringBuilder();
     
        sqlbuffer.append("-------------------------------------------------------\n");
        sqlbuffer.append("-- 模型名：").append(table.getTableNameZh()).append("(").append(table.getTableName()).append(")").append("\n");
        sqlbuffer.append("-- 表模式：").append(table.getTableModel()).append("\n");
        sqlbuffer.append("-- 分表方式：").append(table.getCycleType()).append("\n");
        sqlbuffer.append("-- 数据库：").append(table.getDbName()).append("\n");
        sqlbuffer.append("-- 创建日期：").append(table.getEffDate()).append("\n");
        sqlbuffer.append("-- 注意事项：").append(table.getUpgradeNotice()).append("\n");
        sqlbuffer.append("-------------------------------------------------------\n");
        return sqlbuffer.toString();
    }
}
