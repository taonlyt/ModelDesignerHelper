/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.Table;
import com.snal.model.beans.TableCol;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luo Tao
 */
public class HiveModelHQLBuilder {

    /**
     * 新建表
     *
     * @param table
     * @return
     */
    public String createTable(Table table) {
        boolean isUseProxy = false;
        StringBuilder sqlbuffer = new StringBuilder();
        String tablecols = makeTableCreateScript(table, isUseProxy);
        sqlbuffer.append(tablecols);//表字段语句

        String tableproperties = makeTablePropertiesScript(table, isUseProxy);
        sqlbuffer.append(tableproperties);//表属性语句

        if (table.isConstantParam()) {
            sqlbuffer.append("\nALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName()).append(" ADD PARTITION (branch='GMCC');\n");
        }
        sqlbuffer.append("\n");
        return sqlbuffer.toString();
    }

    /**
     * 追加字段
     *
     * @param table
     * @param newColumns
     * @return
     */
    public String addColumns(Table table, String[] newColumns) {
        StringBuilder sqlbuffer = new StringBuilder("");
        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                .append(table.getTableName()).append(" ADD COLUMNS (");
        for (String newColumn : newColumns) {
            TableCol col = table.getTableCol(newColumn);
            sqlbuffer.append(col.getColumnName()).append(" ").append(col.getDataType()).append(",");
        }
        sqlbuffer.deleteCharAt(sqlbuffer.length() - 1);
        sqlbuffer.append(")");
        if (table.isPartitionTable()) {
            sqlbuffer.append(" CASCADE;\n");
        }else{
            sqlbuffer.append(";");
        }
        sqlbuffer.append("\n");
        return sqlbuffer.toString();
    }

    /**
     * 修改字段，包括字段名，数据类型。
     *
     * @param table
     * @param columnPairs
     * @return
     */
    public String modifyColumns(Table table, String[] columnPairs) {
        StringBuilder sqlbuffer = new StringBuilder("");
        for (String columnPair : columnPairs) {
            String[] columns = columnPair.split("-");
            String oldname = columns[0];
            String newname = columns[1];
            TableCol newCol = table.getTableCol(newname);
            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                    .append(table.getTableName()).append(" ")
                    .append(" CHANGE ").append(oldname).append(" ").append(newname).append(" ").append(newCol.getDataType());
            if (table.isPartitionTable()) {
                sqlbuffer.append(" CASCADE");
            }
            sqlbuffer.append(";\n");
        }
        return sqlbuffer.toString();
    }

    /**
     * 修改模型字段分隔符
     *
     * @param table
     * @return
     */
    public String changeTableColDelimiters(Table table) {
        StringBuilder sqlbuffer = new StringBuilder();
        sqlbuffer.append("ALTER TABLE ").append(table.getDbName())
                .append(".").append(table.getTableName()).append(" SET SERDEPROPERTIES ('field.delim'='")
                .append(table.getColDelimiter()).append("');\n\n");
        return sqlbuffer.toString();
    }

    /**
     * 修改模型存储格式
     *
     * @param table
     * @return
     */
    public String changeTableFileFormat(Table table) {
        StringBuilder sqlbuffer = new StringBuilder();
        sqlbuffer.append("ALTER TABLE ").append(table.getDbName())
                .append(".").append(table.getTableName()).append(" SET FILEFORMAT ").append(table.getStoredFormat()).append(";\n\n");
        return sqlbuffer.toString();
    }

    /**
     * 修改模型字符集
     *
     * @param table
     * @return
     */
    public String changeEncodingCode(Table table) {
        StringBuilder sqlbuffer = new StringBuilder();
        sqlbuffer.append("ALTER TABLE ").append(table.getDbName())
                .append(".").append(table.getTableName()).append(" SET SERDEPROPERTIES ('serialization.encoding'='")
                .append(table.getCharacterSet()).append("');\n\n");
        return sqlbuffer.toString();
    }

    /**
     * 重建模型的历史分区，只支持三种分区键[branch,month,day;branch,month;branch]
     *
     * @param table
     * @param startTime
     * @param endTime
     * @param partionBranch
     * @return
     */
    public String reAddPartitions(Table table, String startTime, String endTime, String partionBranch) {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");
        StringBuilder sqlbuffer = new StringBuilder();
        String[] branches = ContextData.getProperty("branch.code.value").split(",");//地市简称
        try {
            Calendar oldday = Calendar.getInstance();
            oldday.setTime(sdfDay.parse(startTime));

            Calendar nowDay = Calendar.getInstance();
            nowDay.setTime(sdfDay.parse(endTime));

            Calendar nowMon = Calendar.getInstance();
            nowMon.setTime(sdfDay.parse(endTime));

            List parititons = Arrays.asList(table.getPartitionCols());

            if (parititons.contains("month") && parititons.contains("day")) {
                while (nowDay.after(oldday) || nowDay.equals(oldday)) {
                    String nowDayStr = sdfDay.format(nowDay.getTime());
                    String nowMonStr = sdfMon.format(nowDay.getTime());
                    if (table.isMainTable() && table.isPartitionTable()) {
                        if ("GMCC".equals(partionBranch)) {
                            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                    .append(" DROP IF EXISTS PARTITION(branch='GMCC',")
                                    .append("month=").append(nowMonStr).append(",day=").append(nowDayStr).append(");");
                            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                    .append(" ADD PARTITION(branch='GMCC',")
                                    .append("month=").append(nowMonStr).append(",day=").append(nowDayStr).append(");\n\n");
                        } else {
                            for (String branch : branches) {
                                sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                                        .append(table.getTableName()).append(" DROP IF EXISTS PARTITION(branch='").append(branch)
                                        .append("',")
                                        .append("month=").append(nowMonStr).append(",day=").append(nowDayStr).append(");");
                                sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                                        .append(table.getTableName()).append(" ADD PARTITION(branch='").append(branch)
                                        .append("',")
                                        .append("month=").append(nowMonStr).append(",day=").append(nowDayStr).append(");\n\n");
                            }
                        }
                    } else if (!table.isMainTable() && table.isPartitionTable()) {
                        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                .append(" DROP IF EXISTS PARTITION(")
                                .append("month=").append(nowMonStr).append(",day=").append(nowDayStr).append(");");
                        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                .append(" ADD PARTITION(")
                                .append("month=").append(nowMonStr).append(",day=").append(nowDayStr).append(");\n\n");
                    }
                    nowDay.add(Calendar.DAY_OF_MONTH, -1);
                }
            } else if (parititons.contains("month") && !parititons.contains("day")) {
                while (nowMon.after(oldday) || nowMon.equals(oldday)) {
                    String nowMonStr = sdfMon.format(nowMon.getTime());
                    if (table.isMainTable() && table.isPartitionTable()) {
                        if ("GMCC".equals(partionBranch)) {
                            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                    .append(" DROP IF EXISTS PARTITION(branch='GMCC',")
                                    .append("month=").append(nowMonStr).append(");");
                            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                    .append(" ADD PARTITION(branch='GMCC',")
                                    .append("month=").append(nowMonStr).append(");\n\n");
                        } else {
                            for (String branch : branches) {
                                sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                                        .append(table.getTableName()).append(" DROP IF EXISTS PARTITION(branch='").append(branch)
                                        .append("',")
                                        .append("month=").append(nowMonStr).append(");");
                                sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                                        .append(table.getTableName()).append(" ADD PARTITION(branch='").append(branch)
                                        .append("',")
                                        .append("month=").append(nowMonStr).append(");\n\n");
                            }
                        }
                    } else if (!table.isMainTable() && table.isPartitionTable()) {
                        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                .append(" DROP IF EXISTS PARTITION(")
                                .append("month=").append(nowMonStr).append(");");
                        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                .append(" ADD PARTITION(")
                                .append("month=").append(nowMonStr).append(");\n\n");
                    }
                    nowMon.add(Calendar.MONTH, -1);
                }
            } else {
                if (table.isMainTable() && table.isPartitionTable()) {
                    if ("GMCC".equals(partionBranch)) {
                        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                .append(" DROP IF EXISTS PARTITION(branch='GMCC'").append(");");
                        sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName())
                                .append(" ADD PARTITION(branch='GMCC'").append(");\n\n");
                    } else {
                        for (String branch : branches) {
                            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                                    .append(table.getTableName()).append(" DROP IF EXISTS PARTITION(branch='").append(branch).append("');");
                            sqlbuffer.append("ALTER TABLE ").append(table.getDbName()).append(".")
                                    .append(table.getTableName()).append(" ADD PARTITION(branch='").append(branch).append("');\n\n");
                        }
                    }
                }
            }
        } catch (ParseException ex) {
            Logger.getLogger(HiveModelHQLBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sqlbuffer.toString();
    }

    /**
     * 生成主键检查语句
     *
     * @param table
     * @return
     */
    public String createPKCheckRule(Table table) {
        String rule = "";
        if (table.getPrimaryKeys() != null && table.getPrimaryKeys().trim().length() > 0) {
            rule = "\"" + table.getDbName() + "." + table.getTableName() + "\",\"PK_CHK\",1,\"PK\",\"" + table.getPrimaryKeys() + "\",\""
                    + table.getPartitionColsStr() + "\",1.00," + table.getMonitorThreshold() + ",\"N\"";
        }
        return rule;
    }

    /**
     * 生成字段建表语句
     *
     * @param table
     * @param isUseProxy
     * @return
     */
    private String makeTableCreateScript(Table table, boolean isUseProxy) {
        StringBuilder sqlbuff = new StringBuilder();
        if (isUseProxy) {
            sqlbuff.append("DROP TABLE IF EXISTS ").append(table.getDbName()).append(".").append(table.getTableName()).append(";");
            sqlbuff.append("CREATE EXTERNAL TABLE ").append(table.getDbName()).append(".").append(table.getTableName()).append(" (").append("");
            table.getTablecols().stream().filter((tablecol) -> (tablecol.getPartitionSeq() == null
                    || tablecol.getPartitionSeq().trim().length() == 0)).forEach((tablecol) -> {
                sqlbuff.append("   ").append(tablecol.getColumnName()).append("    ").append(tablecol.getDataType()).append(",");
            });
            sqlbuff.deleteCharAt(sqlbuff.length() - 1);//删除末尾多余的逗号。
            sqlbuff.append(")");
        } else {
            sqlbuff.append("DROP TABLE IF EXISTS ").append(table.getDbName()).append(".").append(table.getTableName()).append(";\n");
            sqlbuff.append("CREATE EXTERNAL TABLE ").append(table.getDbName()).append(".").append(table.getTableName()).append(" (").append("\n");
            table.getTablecols().stream().filter((tablecol) -> (tablecol.getPartitionSeq() == null
                    || tablecol.getPartitionSeq().trim().length() == 0)).forEach((tablecol) -> {
                sqlbuff.append("   ").append(tablecol.getColumnName()).append("    ").append(tablecol.getDataType()).append(",\n");
            });
            sqlbuff.deleteCharAt(sqlbuff.length() - 2);//删除末尾多余的逗号。
            sqlbuff.append(")\n");
        }
        return sqlbuff.toString();
    }

    /**
     * 生成设置表属性语句
     *
     * @param table
     * @param isUseProxy
     * @return
     */
    public static String makeTablePropertiesScript(Table table, boolean isUseProxy) {
        List<String> partitioncols = new ArrayList();
        StringBuilder strbuffer = new StringBuilder();
        for (String partitionCol : table.getPartitionCols()) {
            //地市共享模型不需要建branch分区
            if (!table.isMainTable() && partitionCol.equals("branch")) {
                continue;
            }
            partitioncols.add(partitionCol);
        }
        String partitionstr = "";
        if (!partitioncols.isEmpty()) {
            for (int i = 0; i < partitioncols.size(); i++) {
                String datatype = partitioncols.get(i).equals("branch") ? " STRING" : " INT";
                if (partitionstr == null || partitionstr.trim().length() == 0) {
                    partitionstr = partitioncols.get(i) + datatype;
                } else {
                    partitionstr += "," + partitioncols.get(i) + datatype;
                }
            }
        }
        if (partitionstr != null && partitionstr.trim().length() > 0) {
            strbuffer.append("PARTITIONED BY (").append(partitionstr).append(")").append("");
            if (!isUseProxy) {
                strbuffer.append("\n");
            }
        }
        if (table.getStoredFormat().equalsIgnoreCase("PARQUET")
                || table.getStoredFormat().equalsIgnoreCase("ORC")) {
            if (isUseProxy) {
                strbuffer.append(" STORED AS ").append(table.getStoredFormat()).append("")
                        .append(" LOCATION '").append(table.getLocation()).append("'")
                        .append(" TBLPROPERTIES ('serialization.null.format' ='NVL'").append(",")
                        .append("'serialization.encoding' ='").append(table.getCharacterSet()).append("');");
            } else {
                strbuffer.append("STORED AS ").append(table.getStoredFormat()).append("\n")
                        .append("LOCATION '").append(table.getLocation()).append("'\n")
                        .append("TBLPROPERTIES ('serialization.null.format' ='NVL'").append(",")
                        .append("'serialization.encoding' ='").append(table.getCharacterSet()).append("');\n");
            }
        } else {
            if (isUseProxy) {
                strbuffer.append(" ROW FORMAT SERDE ").append("'").append(table.getSerdeClass()).append("'").append("")
                        .append(" WITH SERDEPROPERTIES ('field.delim'='").append(table.getColDelimiter()).append("'").append(",")
                        .append("'serialization.null.format' ='NVL'").append(",")
                        .append("'serialization.encoding' ='").append(table.getCharacterSet()).append("')")
                        .append(" STORED AS ").append(table.getStoredFormat()).append("")
                        .append(" LOCATION '").append(table.getLocation()).append("';");
            } else {
                strbuffer.append("ROW FORMAT SERDE ").append("'").append(table.getSerdeClass()).append("'").append("\n")
                        .append("WITH SERDEPROPERTIES ('field.delim'='").append(table.getColDelimiter()).append("'").append(",")
                        .append("'serialization.null.format' ='NVL'").append(",")
                        .append("'serialization.encoding' ='").append(table.getCharacterSet()).append("')\n")
                        .append("STORED AS ").append(table.getStoredFormat()).append("\n")
                        .append("LOCATION '").append(table.getLocation()).append("';\n");
            }
        }
        return strbuffer.toString();
    }

    /**
     * 在PAAS平台模型上线
     *
     * @param table
     * @return
     */
    public String publishTableOnPaas(Table table) {

        StringBuilder sqlbuffer = new StringBuilder();
        sqlbuffer.append("update tablefile set state='PUBLISHED',open_state='开放'  where xmlid='").append(table.getTableId()).append("';\n");
//        sqlbuffer.append("update mds.tablefile set state='PUBLISHED',open_state='开放' where xmlid='").append(table.getTableId()).append("';\n");

        sqlbuffer.append("update metaobj set state='PUBLISHED' where xmlid='").append(table.getTableId()).append("';\n");
//        sqlbuffer.append("update mds.metaobj set state='PUBLISHED' where xmlid='").append(table.getTableId()).append("';\n");

        sqlbuffer.append("update tableall set state='PUBLISHED' where xmlid='").append(table.getTableId()).append("';\n");
//        sqlbuffer.append("update mds.tableall set state='PUBLISHED' where xmlid='").append(table.getTableId()).append("';\n");

        sqlbuffer.append("insert into mds.meta_team_role_table (xmlid,team_code,objname,id,target_team_code,open_status) values('")
                .append(table.getTableId()).append("','")
                .append(table.getTeamCode()).append("','")
                .append(table.getTableName()).append("','").append(table.getTableId())
                .append(System.currentTimeMillis())
                .append("','all','all');\n\n");
        try {
            Thread.sleep(1);//延时1ms，确保meta_team_role_table.id不重复。
        } catch (InterruptedException ex) {
            Logger.getLogger(HiveModelHQLBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sqlbuffer.toString();
    }

    /**
     * 将模型从PAAS平台下线语句
     * @param table
     * @return 
     */
    public String changeTableOfflineInPaas(Table table) {

        StringBuilder sqlbuffer = new StringBuilder();
        sqlbuffer.append("update tablefile set state='INVALID' where xmlid='").append(table.getTableId()).append("';\n");
        sqlbuffer.append("update metaobj set state='INVALID' where xmlid='").append(table.getTableId()).append("';\n\n");
        return sqlbuffer.toString();
    }
}
