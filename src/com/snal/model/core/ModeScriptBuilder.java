/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.Table;
import com.snal.model.beans.TableCol;
import com.snal.model.util.text.TextUtil;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luotao
 */
public class ModeScriptBuilder {

    public static String getAnnotationStart(int charcount, String annotationChar) {
        StringBuilder retbuff = new StringBuilder();
        for (int i = 0; i < charcount; i++) {
            retbuff.append(annotationChar);
        }
        return retbuff.toString();
    }

    /**
     * 生成更新模型分隔符脚本
     *
     * @param tablenames
     * @param tablemap
     * @param branches
     * @param useProxy
     * @return
     */
    public static StringBuilder modifyTableColDelimiters(List<String> tablenames, Map<String, Table> tablemap,
            String[] branches, boolean useProxy) {
        Map<String, StringBuilder> usermap = new HashMap();
        tablenames.stream().forEach((tablename) -> {
            Table table = tablemap.get(tablename);
            String username = table.getTenantUser();
            StringBuilder hqlbuffer = usermap.get(username);
            if (hqlbuffer == null) {
                hqlbuffer = new StringBuilder();
                usermap.put(username, hqlbuffer);
            }
            String modifyhql = "";
            modifyhql = "ALTER TABLE " + table.getDbName() + "." + table.getTableName() + " SET SERDE '"
                    + table.getSerdeClass()
                    + "' WITH SERDEPROPERTIES ('field.delim'='"
                    + table.getColDelimiter()
                    + "','serialization.encoding'='"
                    + table.getCharacterSet()
                    + "','serialization.null.format'='NVL');";
            if (useProxy) {
                modifyhql = "remote_cli.pl " + table.getTenantUser() + " beeline -e \"USE " + table.getDbName() + ";" + modifyhql + "\"";
            }
            hqlbuffer.append(modifyhql).append("\n");
            if (table.isShared()) {
                for (String branch : branches) {
                    String branchTablename = table.getTableName() + "_" + branch;
                    hqlbuffer.append(modifyhql.replaceAll(table.getTableName(), branchTablename)).append("\n");
                }
            }
        });
        StringBuilder retbuffer = new StringBuilder();
        usermap.keySet().stream().forEach((key) -> {
            StringBuilder sbdf1 = usermap.get(key);
            retbuffer.append("---").append(key).append("租户脚本---\n");
            retbuffer.append(sbdf1.toString());
            System.out.println(retbuffer.toString());
        });
        System.out.println("处理完成");
        return retbuffer;
    }

    /**
     * 批量生成多个主表的建表语句
     *
     * @param tablenames
     * @param metadata
     * @param branches
     * @param outputSharedTable
     * @return
     */
    public static Map<String, StringBuilder> makeHiveMainTableScript(List<String> tablenames, Map<String, Table> metadata,
            String[] branches, boolean outputSharedTable, boolean isUseProxy) {
        Map<String, StringBuilder> hqlmap = new HashMap();
        List<String> failtables = new ArrayList();//存放生成建表语句失败的模型
        List<String> successtables = new ArrayList();//存放生成建表语句成功的模型
        StringBuilder ruleBuffer = new StringBuilder();//用来存放数据质量检查规则（主键检查规则）
        if (tablenames != null) {
            tablenames.stream().map((tablename) -> metadata.get(tablename)).forEach((table) -> {
                if (table != null) {
                    String hqlstr = createHiveMainTableScript(branches, table, failtables, successtables, outputSharedTable, isUseProxy);//生成建表语句
                    StringBuilder hqlbuffer = hqlmap.get(table.getTenantUser());
                    if (hqlbuffer == null) {
                        hqlbuffer = new StringBuilder("----").append(table.getTenantUser()).append("租户脚本----\r\n");
                        hqlmap.put(table.getTenantUser(), hqlbuffer);
                    }
                    hqlbuffer.append(hqlstr);
                    /**
                     * 主键规则
                     */
                    String rule = "";
                    if (table.getPrimaryKeys() != null && table.getPrimaryKeys().trim().length() > 0) {
                        rule = "\"" + table.getDbName() + "." + table.getTableName() + "\",\"PK_CHK\",1,\"PK\",\"" + table.getPrimaryKeys() + "\",\""
                                + table.getPartitionColsStr() + "\",1.00," + table.getMonitorThreshold() + ",\"N\"";
                    }
                    ruleBuffer.append(rule).append("\n");
                }
            });
            hqlmap.put("CTL.HIVE_CHECK_RULE", ruleBuffer);
        }
        System.out.println("输入（" + tablenames.size() + "）  输出（" + successtables.size() + "）  失败（" + failtables.size() + ")");
        if (!failtables.isEmpty()) {
            System.out.print("处理失败模型：");
            System.out.println(Arrays.toString(failtables.toArray()));
        }
        return hqlmap;
    }

    public static Map<String, StringBuilder> changeColumnSecurityType(List<String> tablenames, Map<String, Table> metadata, String[] branches) {
        StringBuilder buffer = new StringBuilder();
        for (String tablename : tablenames) {
            Table table = metadata.get(tablename);
            for (TableCol tablecol : table.getTablecols()) {
                String sql = " update column_val set file_type_child ='" + tablecol.getSecurityType3()
                        + "' where colname= '" + tablecol.getColumnName() + "' and dataname in ('" + table.getTableName() + "'";
                buffer.append(sql);
                if (table.isShared()) {
                    String tnames = "";
                    for (String branch : branches) {
                        tnames += ",'" + tablename + "_" + branch + "'";
                    }
                    buffer.append(tnames);
                }
                buffer.append(");\n");
            }
            buffer.append("\n");
        }
        System.out.println(buffer.toString());
        return null;
    }

    public static Map<String, StringBuilder> makeHiveBranchTableScript(List<String> tablenames, Map<String, Table> metadata,
            String[] branches, boolean isUseProxy) {

        Map<String, StringBuilder> hqlmap = new HashMap();
        List<String> failtables = new ArrayList();//存放生成建表语句失败的模型
        List<String> successtables = new ArrayList();//存放生成建表语句成功的模型
        if (tablenames != null) {
            tablenames.stream().map((tablename) -> metadata.get(tablename)).forEach((table) -> {
                if (table != null) {
                    String hqlstr = createHiveBranchTableScript(branches, table, failtables, successtables, isUseProxy);//生成建表语句
                    StringBuilder hqlbuffer = hqlmap.get(table.getTenantUser());
                    if (hqlbuffer == null) {
                        hqlbuffer = new StringBuilder("----").append(table.getTenantUser()).append("租户脚本----\r\n");
                        hqlmap.put(table.getTenantUser(), hqlbuffer);
                    }
                    hqlbuffer.append(hqlstr);
                }
            });
        }
        System.out.println("输入（" + tablenames.size() + "）  输出（" + successtables.size() + "）  失败（" + failtables.size() + ")");
        if (!failtables.isEmpty()) {
            System.out.print("处理失败模型：");
            System.out.println(Arrays.toString(failtables.toArray()));
        }
        return hqlmap;
    }

    /**
     * 生成主模型建表语句，如果主表是地市共享模型，则同时生成地市共享模型建表语句。
     *
     * @param branches
     * @param table
     * @param failtables
     * @param successtables
     * @param outputSharedTable
     * @return
     */
    public static String createHiveMainTableScript(String[] branches, Table table, List<String> failtables, List<String> successtables,
            boolean outputSharedTable, boolean isUseProxy) {

        StringBuilder sqlbuffer = new StringBuilder();
        if (isUseProxy) {
            sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"use jcfw;");
        }
        String tablecols = makeTableCreateScript(table, isUseProxy);
        sqlbuffer.append(tablecols);//表字段语句

        String tableproperties = makeTablePropertiesScript(table, isUseProxy);
        sqlbuffer.append(tableproperties);//表属性语句

        if (table.isConstantParam()) {
            sqlbuffer.append("\nALTER TABLE ").append(table.getDbName()).append(".").append(table.getTableName()).append(" ADD PARTITION (branch='GMCC');\n");
        }
        if (isUseProxy) {
            sqlbuffer.append("\"");
        }
        sqlbuffer.append("\n");
        if (tablecols != null && tablecols.length() > 0 && tableproperties != null && tableproperties.length() > 0) {
            System.out.println("[OK] " + table.getTableName() + " [" + table.getTenantUser() + "]");
            successtables.add(table.getTableName());
        } else {
            System.out.println("[Fail] " + table.getTableName());
            failtables.add(table.getTableName());
        }
        /**
         * 如果是地市共享模型，则生成创建地市共享模型脚本。
         */
        if (table.isShared() && outputSharedTable) {
            String branchTableHql = createHiveBranchTableScript(branches, table, failtables, successtables, isUseProxy);
            if (branchTableHql != null && branchTableHql.trim().length() > 0) {
                sqlbuffer.append(branchTableHql).append("\r\n");
            }
        }
        return sqlbuffer.toString();
    }

    /**
     * 根据主表生成建地市共享模型语句
     *
     * @param branches
     * @param mainTable
     * @param database
     * @return
     */
    public static String createBranchTableLikeMainTable(String[] branches, Table mainTable, String database) {
        StringBuilder sqlbuffer = new StringBuilder();
        for (String branch : branches) {
            String branchTableName = mainTable.getTableName() + "_" + branch;
            sqlbuffer.append("DROP TABLE IF EXISTS ").append(database).append(".").append(branchTableName).append(";\r\n");
            sqlbuffer.append("CREATE EXTERNAL TABLE ").append(database).append(".").append(branchTableName)
                    .append(" LIKE ").append(database).append(".").append(mainTable.getTableName())
                    .append(" LOCATION '").append(mainTable.getLocation()).append("';\r\n");
        }
        return sqlbuffer.toString();
    }

    /**
     * 根据主表生成地市共享模型建表语句，地市共享模型分区是在主表分区的基础上去掉地市分区。 如果主表只按地市分区，则共享模型不分区。
     *
     * @param branches
     * @param table
     * @param failtables
     * @param successtables
     * @return
     */
    public static String createHiveBranchTableScript(String[] branches, Table table, List<String> failtables,
            List<String> successtables, boolean isUseProxy) {
        StringBuilder sqlbuff = new StringBuilder();
        for (String branch : branches) {
            Table branchTable = (Table) table.clone();
            branchTable.setMainTable(false);
            branchTable.setTableName(table.getTableName() + "_" + branch);
            if (branchTable.isShareAllDataToCity()) {
                branchTable.setLocation(branchTable.getLocation() + "/branch=GMCC");
            } else {
                branchTable.setLocation(branchTable.getLocation() + "/branch=" + branch);
            }
            if (isUseProxy) {
                sqlbuff.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(branchTable.getTenantUser()).append(" beeline -e \"use jcfw;");
            }
            String tablecols = makeTableCreateScript(branchTable, isUseProxy);
            sqlbuff.append(tablecols);//表字段语句
            String tablepropertes = makeTablePropertiesScript(branchTable, isUseProxy);
            sqlbuff.append(tablepropertes);//表属性语句
            if (isUseProxy) {
                sqlbuff.append("\"");
            }
            sqlbuff.append("\n");
//            List parititons = Arrays.asList(table.getPartitionCols());
//            if (parititons.contains("month") || parititons.contains("day")) {
//                sqlbuff.append("MSCK REPAIR TABLE ").append(branchTable.getDbName()).append(".").append(branchTable.getTableName()).append(";\n\n");
//            }
            if (tablecols != null && tablecols.length() > 0 && tablepropertes != null && tablepropertes.length() > 0) {
                System.out.println("[OK] " + branchTable.getTableName() + " [" + branchTable.getTenantUser() + "]");
                successtables.add(branchTable.getTableName());
            } else {
                System.out.println("[Fail] " + branchTable.getTableName());
                failtables.add(branchTable.getTableName());
            }
        }
        return sqlbuff.toString();
    }

    /**
     * 生成创建脚本字段语句
     *
     * @param table
     * @param isUseProxy
     * @return
     */
    public static String makeTableCreateScript(Table table, boolean isUseProxy) {
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
     * 生成DB2建表语句
     *
     * @param table
     * @param failtables
     * @param successtables
     * @return
     */
    public static String makeDB2TableInstCreateScript(Table table, List<String> failtables, List<String> successtables, String[] branchList) {
        StringBuilder sqlbuffer = new StringBuilder();
        for (String branch : branchList) {

            StringBuilder basebuff = new StringBuilder();//建表基本语句
            StringBuilder commentbuff = new StringBuilder();//备注
            List<String> partitionkeys = new ArrayList<>();//分区键
            List<String> primarykeys = new ArrayList<>();//主键
            String tableName = table.getTableName() + "_" + branch + "201708";
            basebuff.append("-------------------------------------------------------\n");
            basebuff.append("-- 模型名：").append(table.getTableNameZh()).append("(").append(table.getTableName()).append(")").append("\n");
            basebuff.append("-- 表模式：").append(table.getTableModel()).append("\n");
            basebuff.append("-- 分表方式：").append(table.getCycleType()).append("\n");
            basebuff.append("-- 数据库：").append(table.getDbName()).append("\n");
            basebuff.append("-- 创建日期：").append(table.getEffDate()).append("\n");
            basebuff.append("-- 需求编号及说明：").append(table.getRemark()).append("\n");
            basebuff.append("-------------------------------------------------------\n");

            basebuff.append("CREATE TABLE ").append(table.getTableModel()).append(".").append(tableName).append(" (").append("\n");
            commentbuff.append("COMMENT ON TABLE ").append(table.getTableModel()).append(".")
                    .append(tableName).append(" IS '")
                    .append(table.getTableNameZh()).append("';\n");
            int colcount = 0;
            if (table.getTablecols().isEmpty()) {
                failtables.add(table.getTableName());
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
                        .append(tableName).append(".").append(tablecol.getColumnName())
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
                basebuff.append("ALTER TABLE ").append(table.getTableModel()).append(".").append(tableName)
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
            sqlbuffer.append(basebuff.toString());
        }
        successtables.add(table.getTableName());
        return sqlbuffer.toString();
    }

    /**
     * 生成DB2建表语句
     *
     * @param table
     * @param failtables
     * @param successtables
     * @param branchList
     * @return
     */
    public static String makeDB2TableCreateScript(Table table, List<String> failtables, List<String> successtables, String[] branchList) {
        StringBuilder basebuff = new StringBuilder();//建表基本语句
        StringBuilder commentbuff = new StringBuilder();//备注
        List<String> partitionkeys = new ArrayList<>();//分区键
        List<String> primarykeys = new ArrayList<>();//主键
        basebuff.append("-------------------------------------------------------\n");
        basebuff.append("-- 模型名：").append(table.getTableNameZh()).append("(").append(table.getTableName()).append(")").append("\n");
        basebuff.append("-- 表模式：").append(table.getTableModel()).append("\n");
        basebuff.append("-- 分表方式：").append(table.getCycleType()).append("\n");
        basebuff.append("-- 数据库：").append(table.getDbName()).append("\n");
        basebuff.append("-- 创建日期：").append(table.getEffDate()).append("\n");
        basebuff.append("-- 需求编号及说明：").append(table.getRemark()).append("\n");
        basebuff.append("-------------------------------------------------------\n");
        basebuff.append("CREATE TABLE ").append(table.getTableModel()).append(".").append(table.getTableName()).append(" (").append("\n");
        commentbuff.append("COMMENT ON TABLE ").append(table.getTableModel()).append(".")
                .append(table.getTableName()).append(" IS '")
                .append(table.getTableNameZh()).append("';\n");
        int colcount = 0;
        if (table.getTablecols().isEmpty()) {
            failtables.add(table.getTableName());
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
        successtables.add(table.getTableName());
        return basebuff.toString();
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
     * 给地市模型添加分区
     *
     * @param tableNames
     * @param tableMap
     * @param startDate
     * @param endDate
     * @param branches
     * @return
     * @throws ParseException
     */
    public static Map<String, StringBuilder> addBranchTablePartition(List<String> tableNames, Map<String, Table> tableMap,
            String startDate, String endDate, String[] branches) throws ParseException {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        int count = 0;
        int havesqlcnt = 0;
        int nosqlcnt = 0;
        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
            StringBuilder hqlBuffer = hqlmap.get(table.getTenantUser());
            if (hqlBuffer == null) {
                hqlBuffer = new StringBuilder("----" + table.getTenantUser() + "租户脚本----\n");
                hqlmap.put(table.getTenantUser(), hqlBuffer);
            }
            String hql1 = makeMainTablePartition(table, startDate, endDate, branches);
            if (hql1.length() != 0) {
                count++;
            }
            hqlBuffer.append(hql1).append("\n");
            if (table.isShared()) {
                String hql = makeBranchTablePartition(table, startDate, endDate, branches);
                if (hql.length() != 0) {
                    havesqlcnt++;
                    System.out.println("have sql:" + tableName);
                } else {
                    nosqlcnt++;
                    System.out.println("no sql:" + tableName);
                }
                hqlBuffer.append(hql).append("\n");
            } else {
                System.out.println("no share:" + tableName);
            }

        }
//        StringBuilder hqlBuffer = new StringBuilder();
//        hqlmap.keySet().stream().map((username) -> hqlmap.get(username).toString()).forEachOrdered((hql) -> {
//            hqlBuffer.append(hql);
//        });
//        System.out.println(hqlBuffer.toString());
        System.out.println("count:" + count + " have sql:" + havesqlcnt + "\n no sql:" + nosqlcnt);
        return hqlmap;
    }

    public static Map<String, StringBuilder> overRideData(List<String> tableNames, Map<String, Table> tableMap,
            String startDate, String endDate, String[] branches) throws ParseException {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        int count = 0;
        int havesqlcnt = 0;
        int nosqlcnt = 0;

        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
            StringBuilder hqlBuffer = hqlmap.get(table.getTenantUser());
            if (hqlBuffer == null) {
                hqlBuffer = new StringBuilder("----" + table.getTenantUser() + "租户脚本----\n");
                hqlmap.put(table.getTenantUser(), hqlBuffer);
            }
            String hql1 = recoeryData(table, startDate, endDate, branches);
            if (hql1.length() != 0) {
                count++;
            }
            hqlBuffer.append(hql1).append("\n");
        }
//        StringBuilder hqlBuffer = new StringBuilder();
//        hqlmap.keySet().stream().map((username) -> hqlmap.get(username).toString()).forEachOrdered((hql) -> {
//            hqlBuffer.append(hql);
//        });
//        System.out.println(hqlBuffer.toString());
        System.out.println("count:" + count + " have sql:" + havesqlcnt + "\n no sql:" + nosqlcnt);
        return hqlmap;
    }

    public static Map<String, StringBuilder> offlineTables(List<String> tableNames, Map<String, Table> tableMap,
            String[] branches, boolean isUseProxy) {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        StringBuilder hqlBuffer = new StringBuilder();
        hqlmap.put("OFFLINE", hqlBuffer);
        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
            String sql = "update tablefile set state ='INVALID' where dataname ='" + table.getTableName() + "';\n";
            hqlBuffer.append(sql);
            hqlBuffer.append("update metaobj set state ='INVALID' where objname ='").append(table.getTableName()).append("';\n");
            if (table.isShared()) {
                for (String branch : branches) {
                    String branchTableName = table.getTableName() + "_" + branch;
                    String sql1 = "update tablefile set state ='INVALID' where dataname ='" + branchTableName + "';\n";
                    hqlBuffer.append(sql1);
                    hqlBuffer.append("update metaobj set state ='INVALID' where objname ='").append(branchTableName).append("';\n");
                }
            }
        }
        return hqlmap;
    }

    public static Map<String, StringBuilder> changeExtendFlag(List<String> tableNames, Map<String, Table> tableMap,
            String startDate, String endDate, String[] branches, boolean isUseProxy) {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        StringBuilder hqlBuffer = new StringBuilder();
        hqlmap.put("AAA", hqlBuffer);
        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
            String sql = "update tablefile set extend_cfg ='" + table.getExtendcfg() + "' where dataname ='" + table.getTableName() + "';\n";
            hqlBuffer.append(sql);
            if (table.isShared()) {
                for (String branch : branches) {
                    String branchTableName = table.getTableName() + "_" + branch;
                    String sql1 = "update tablefile set extend_cfg ='" + table.getExtendcfg() + "' where dataname ='" + branchTableName + "';\n";
                    hqlBuffer.append(sql1);
                }
            }
        }
        return hqlmap;
    }

    public static Map<String, StringBuilder> changeColumnDelimiter(List<String> tableNames, Map<String, Table> tableMap,
            String startDate, String endDate, String[] branches, boolean isUseProxy) throws ParseException {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        int count = 0;
        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
            if (!table.isShared()) {
                continue;
            }
            StringBuilder hqlBuffer = hqlmap.get(table.getTenantUser());
            if (hqlBuffer == null) {
                hqlBuffer = new StringBuilder("----" + table.getTenantUser() + "租户脚本----\n");
                hqlmap.put(table.getTenantUser(), hqlBuffer);
            }
            String hql = changeTableColumnDelimiter(table, startDate, endDate, branches);
            if (hql.length() != 0) {
                count++;
            } else {
                System.out.println(tableName);
            }
            //hql = "perl ~schadm/dssprog/bin/remote_cli.pl " + table.getTenantUser() + " beeline -e \"USE JCFW;" + hql + "\"";
            hqlBuffer.append(hql).append("\n");
        }

//        StringBuilder hqlBuffer = new StringBuilder();
//        hqlmap.keySet().stream().map((username) -> hqlmap.get(username).toString()).forEachOrdered((hql) -> {
//            hqlBuffer.append(hql);
//        });
//        System.out.println(hqlBuffer.toString());
        System.out.println("count:" + count);
        return hqlmap;
    }

    public static Map<String, StringBuilder> updateTableExplainInfo(List<String> tableNames, Map<String, Table> tableMap, String[] branches) {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        StringBuilder hqlBuffer = new StringBuilder();
        tableNames.stream().map((tableName) -> tableMap.get(tableName)).map((table) -> {
            List<TableCol> cols = table.getTablecols();
            String tablenames = "'" + table.getTableName() + "'";
            if (table.isShared()) {
                for (String branch : branches) {
                    tablenames += ",'" + table.getTableName() + "_" + branch + "'";
                }
            }
            String sql1 = "update tablefile set table_explain='" + table.getRemark().replaceAll("'", "''") + "' where dataname in ("+tablenames+");\n";
            hqlBuffer.append(sql1);
            for (TableCol tableCol : cols) {
                if(tableCol.getPartitionSeq()==null||tableCol.getPartitionSeq().trim().length()==0){
                    String sql = "update column_val set colcnname='" + tableCol.getColumnNameZh() + "',remark='" + tableCol.getRemark().replaceAll("'", "''")
                            + "',data_example='" + tableCol.getDataExample().replaceAll("'", "''") + "' where dataname in (" + tablenames + ") and colname='" + tableCol.getColumnName() + "';\n";
                    hqlBuffer.append(sql);
                }
            }
            hqlBuffer.append("\n");
            return table;
        }).forEachOrdered((_item) -> {
            hqlmap.put("UPDATE_PAAS_TABLE_COLUMN", hqlBuffer);
        });
        return hqlmap;
    }

    public static Map<String, StringBuilder> changeColumnType(List<String> tableNames, Map<String, Table> tableMap,
            String startDate, String endDate, String[] branches, boolean isUseProxy) throws ParseException {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        int count = 0;
        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
//            if (!table.isShared()) {
//                continue;
//            }
            StringBuilder hqlBuffer = hqlmap.get(table.getTenantUser());
            if (hqlBuffer == null) {
                hqlBuffer = new StringBuilder("----" + table.getTenantUser() + "租户脚本----\n");
                hqlmap.put(table.getTenantUser(), hqlBuffer);
            }
            String hql = changeTableColumnType(table, startDate, endDate, branches);
            if (hql.length() != 0) {
                count++;
            } else {
                System.out.println(tableName);
            }
            //hql = "perl ~schadm/dssprog/bin/remote_cli.pl " + table.getTenantUser() + " beeline -e \"USE JCFW;" + hql + "\"";
            hqlBuffer.append(hql).append("\n");
        }

//        StringBuilder hqlBuffer = new StringBuilder();
//        hqlmap.keySet().stream().map((username) -> hqlmap.get(username).toString()).forEachOrdered((hql) -> {
//            hqlBuffer.append(hql);
//        });
//        System.out.println(hqlBuffer.toString());
        System.out.println("count:" + count);
        return hqlmap;
    }

    public static String changeTableColumnDelimiter(Table table, String startDate, String endDate, String[] branches) throws ParseException {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");

        Calendar oldday = Calendar.getInstance();
        oldday.setTime(sdfDay.parse(startDate));

        Calendar nowDay = Calendar.getInstance();
        nowDay.setTime(sdfDay.parse(endDate));

        Calendar nowMon = Calendar.getInstance();
        nowMon.setTime(sdfDay.parse(endDate));

        StringBuilder sqlbuffer = new StringBuilder();
        List parititons = Arrays.asList(table.getPartitionCols());
        /**
         * 1.修改模型属性中的分隔符。
         */
        sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
        for (String branch : branches) {
            String tableName = table.getTableName() + "_" + branch;
            String sql = "ALTER TABLE JCFW." + tableName + " SET SERDEPROPERTIES('field.delim'='" + table.getColDelimiter() + "');";
            sqlbuffer.append(sql);
        }
        sqlbuffer.append("\"\n");
        /**
         * 2.删除历史分区（按月快速删除分区）
         */
        if (parititons.contains("month")) {
            while (nowMon.after(oldday) || nowMon.equals(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                for (String branch : branches) {
                    String branchTable = table.getTableName() + "_" + branch;
                    String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " DROP IF EXISTS PARTITION (";
                    String pStr = "month=" + sdfMon.format(nowMon.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
            nowMon.setTime(sdfDay.parse(endDate));//前面时间有修改，需重新设置时间，以便后续继续使用
        }
        /**
         * 3.重建历史分区
         */
        if (parititons.contains("month") && parititons.contains("day")) {
            while (nowDay.after(oldday) || nowDay.equals(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                for (String branch : branches) {
                    String branchTable = table.getTableName();
                    String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " ADD PARTITION (";
                    String pStr = "branch='" + branch + "',month=" + sdfMon.format(nowDay.getTime()) + ",day=" + sdfDay.format(nowDay.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowDay.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else if (parititons.contains("month") && !parititons.contains("day")) {
            while (nowMon.after(oldday) || nowMon.equals(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                for (String branch : branches) {
                    String branchTable = table.getTableName() + "_" + branch;
                    String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " ADD PARTITION (";
                    String pStr = "month=" + sdfMon.format(nowMon.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
        }
//        sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
//        for (String branch : branches) {
//            String branchTable = table.getTableName() + "_" + branch;
//            // sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET FILEFORMAT PARQUET;");
//            //sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET SERDEPROPERTIES('field.delim'='").append(table.getColDelimiter()).append("');");
//            sqlbuffer.append("MSCK REPAIR TABLE JCFW.").append(branchTable).append(";");
//        }
//        sqlbuffer.append("\"\n");
        return sqlbuffer.toString();
    }

    public static String changeTableColumnType(Table table, String startDate, String endDate, String[] branches) throws ParseException {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");

        Calendar oldday = Calendar.getInstance();
        oldday.setTime(sdfDay.parse(startDate));

        Calendar nowDay = Calendar.getInstance();
        nowDay.setTime(sdfDay.parse(endDate));

        Calendar nowMon = Calendar.getInstance();
        nowMon.setTime(sdfDay.parse(endDate));

        StringBuilder sqlbuffer = new StringBuilder();
        List parititons = Arrays.asList(table.getPartitionCols());
        /**
         * 1.修改模型属性中的分隔符。
         */
//        sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
//        if (table.isShared()) {
//            for (String branch : branches) {
//                String tableName = table.getTableName() + "_" + branch;
//                String sql = "ALTER TABLE JCFW." + tableName + " SET SERDEPROPERTIES('field.delim'='" + table.getColDelimiter() + "');";
//                sqlbuffer.append(sql);
//            }
//        }
//        sqlbuffer.append("\"\n");
        /**
         * 2.删除历史分区（按月快速删除分区）
         */
//        if (parititons.contains("month")) {
//            while (nowMon.after(oldday) || nowMon.equals(oldday)) {
//                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
//                for (String branch : branches) {
//                    String branchTable = table.getTableName() + "_" + branch;
//                    String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " DROP IF EXISTS PARTITION (";
//                    String pStr = "month=" + sdfMon.format(nowMon.getTime());
//                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
//                }
//                sqlbuffer.append("\"\n");
//                nowMon.add(Calendar.MONTH, -1);
//            }
//            nowMon.setTime(sdfDay.parse(endDate));//前面时间有修改，需重新设置时间，以便后续继续使用
//        }
        /**
         * 3.重建历史分区
         */
        if (parititons.contains("month") && parititons.contains("day")) {
            while (nowDay.after(oldday) || nowDay.equals(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
//                for (String branch : branches) {
                String branchTable = table.getTableName();
                String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " if exist PARTITION (";
                String pStr = "branch='GMCC',month=" + sdfMon.format(nowDay.getTime()) + ",day=" + sdfDay.format(nowDay.getTime());
                sqlbuffer.append(alterTabelStr).append(pStr).append(") CHANGE ADDR7_ID ADDR7_ID INT;");
//                }
                sqlbuffer.append("\"\n");
                nowDay.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else if (parititons.contains("month") && !parititons.contains("day")) {
            while (nowMon.after(oldday) || nowMon.equals(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
//                for (String branch : branches) {
                String branchTable = table.getTableName();
                String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " PARTITION (";
                String pStr = "month=" + sdfMon.format(nowMon.getTime());
                sqlbuffer.append(alterTabelStr).append(pStr).append(") CHANGE ADDR7_ID ADDR7_ID INT;");
//                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
        }
//        sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
//        for (String branch : branches) {
//            String branchTable = table.getTableName() + "_" + branch;
//            // sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET FILEFORMAT PARQUET;");
//            //sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET SERDEPROPERTIES('field.delim'='").append(table.getColDelimiter()).append("');");
//            sqlbuffer.append("MSCK REPAIR TABLE JCFW.").append(branchTable).append(";");
//        }
//        sqlbuffer.append("\"\n");
        return sqlbuffer.toString();
    }

    public static Map<String, StringBuilder> changeBranchTableFileFormat(List<String> tableNames, Map<String, Table> tableMap,
            String startDate, String endDate, String[] branches, boolean isUseProxy) throws ParseException {
        Map<String, StringBuilder> hqlmap = new HashMap<>();
        int count = 0;
        for (String tableName : tableNames) {
            Table table = tableMap.get(tableName);
            if (!table.isShared()) {
                continue;
            }
            StringBuilder hqlBuffer = hqlmap.get(table.getTenantUser());
            if (hqlBuffer == null) {
                hqlBuffer = new StringBuilder("----" + table.getTenantUser() + "租户脚本----\n");
                hqlmap.put(table.getTenantUser(), hqlBuffer);
            }
            String hql = changeTableFileFormat(table, startDate, endDate, branches);
            if (hql.length() != 0) {
                count++;
            } else {
                System.out.println(tableName);
            }
            //hql = "perl ~schadm/dssprog/bin/remote_cli.pl " + table.getTenantUser() + " beeline -e \"USE JCFW;" + hql + "\"";
            hqlBuffer.append(hql).append("\n");
        }
//        StringBuilder hqlBuffer = new StringBuilder();
//        hqlmap.keySet().stream().map((username) -> hqlmap.get(username).toString()).forEachOrdered((hql) -> {
//            hqlBuffer.append(hql);
//        });
//        System.out.println(hqlBuffer.toString());
        System.out.println("count:" + count);
        return hqlmap;
    }

    public static String changeTableFileFormat(Table table, String startDate, String endDate, String[] branches) throws ParseException {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");

        Calendar oldday = Calendar.getInstance();
        oldday.setTime(sdfDay.parse(startDate));

        Calendar nowDay = Calendar.getInstance();
        nowDay.setTime(sdfDay.parse(endDate));

        Calendar nowMon = Calendar.getInstance();
        nowMon.setTime(sdfDay.parse(endDate));

        StringBuilder sqlbuffer = new StringBuilder();
        List parititons = Arrays.asList(table.getPartitionCols());
        /**
         * 先删除历史分区
         */

        /*if (parititons.contains("month") && parititons.contains("day")) {
            while (nowDay.after(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                for (String branch : branches) {
                    String branchTable = table.getTableName() + "_" + branch;
                    String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " DROP IF EXISTS PARTITION (";
                    String pStr = "month=" + sdfMon.format(nowDay.getTime()) + ",day=" + sdfDay.format(nowDay.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowDay.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else */
        if (parititons.contains("month")) {
            while (nowMon.after(oldday)) {
                sqlbuffer.append("perl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                for (String branch : branches) {
                    String branchTable = table.getTableName() + "_" + branch;
                    String alterTabelStr = "ALTER TABLE JCFW." + branchTable + " DROP IF EXISTS PARTITION (";
                    String pStr = "month=" + sdfMon.format(nowMon.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
        }
        for (String branch : branches) {
            sqlbuffer.append("\nperl ~schadm/dssprog/bin/remote_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
            String branchTable = table.getTableName() + "_" + branch;
            //sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET FILEFORMAT ").append(table.getStoredFormat()).append(";");
//            sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET SERDEPROPERTIES('field.delim'='").append(table.getColDelimiter()).append("');");
            // sqlbuffer.append("ALTER TABLE JCFW.").append(branchTable).append(" SET SERDE '").append(table.getSerdeClass()).append("';");
            sqlbuffer.append("MSCK REPAIR TABLE JCFW.").append(branchTable).append(";\"");
        }
        sqlbuffer.append("\n");
        return sqlbuffer.toString();
    }

    public static String recoeryData(Table table, String startDate, String endDate, String[] branches) throws ParseException {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");

        Calendar oldday = Calendar.getInstance();
        oldday.setTime(sdfDay.parse(startDate));

        Calendar nowDay = Calendar.getInstance();
        nowDay.setTime(sdfDay.parse(endDate));
        Calendar nowMon = Calendar.getInstance();
        nowMon.setTime(sdfDay.parse(endDate));

        StringBuilder sqlbuffer = new StringBuilder();
        List parititons = Arrays.asList(table.getPartitionCols());

        String sqltem = "INSERT OVERWRITE TABLE JCFW.TM_PERS_NEW_USR_D PARTITION(branch='AA',month=201803,day=20180301) SELECT STAT_DT,CMCC_BRANCH_CD,DIST_ID,MICRODIST_ID,BUSI_USR_NBR,CHNL_STAR_LVL3_IND,CHNL_CD,CHNL_STS_CD,BRND_CD,BASS_PRDCT_CD,BASS_PRDCT_NAM,MAIN_PRDCT_CD,MAIN_PRDCT_NAM,'-1',DSSB_NEW_USR_CNT,SMDJ_NEW_USR_CNT,TKGX_NEW_USR_CNT  FROM JCFW.TM_PERS_NEW_USR_D_BAK WHERE branch='AA' AND month=201803 and day=20180301;";

        if (parititons.contains("month") && parititons.contains("day")) {
            while (nowDay.after(oldday)) {
                for (String branch : branches) {
                    sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                    String sql11 = sqltem.replaceAll("AA", branch)
                            .replaceAll("20180301", sdfDay.format(nowDay.getTime()))
                            .replaceAll("201803", sdfMon.format(nowDay.getTime()));
                    sqlbuffer.append(sql11);
                    sqlbuffer.append("\"\n");
                }
                nowDay.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else if (parititons.contains("month") && !parititons.contains("day")) {
            while (nowMon.after(oldday)) {
                sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"");
                for (String branch : branches) {
                    String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + " ADD PARTITION (";
                    String pStr = "branch='" + branch + "',month=" + sdfMon.format(nowMon.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
        } else if (!parititons.contains("month") && !parititons.contains("day")) {
            sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"");
            for (String branch : branches) {
                String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + " ADD PARTITION (";
                String pStr = "branch='" + branch + "'";
                sqlbuffer.append(alterTabelStr).append(pStr).append(");");
            }
            sqlbuffer.append("\"\n");
        }
        return sqlbuffer.toString();

    }

    /**
     * 主模型加分区
     *
     * @param table
     * @param startDate
     * @param endDate
     * @param branches
     * @return
     * @throws ParseException
     */
    public static String makeMainTablePartition(Table table, String startDate, String endDate, String[] branches) throws ParseException {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");

        Calendar oldday = Calendar.getInstance();
        oldday.setTime(sdfDay.parse(startDate));

        Calendar nowDay = Calendar.getInstance();
        nowDay.setTime(sdfDay.parse(endDate));
        Calendar nowMon = Calendar.getInstance();
        nowMon.setTime(sdfDay.parse(endDate));

        StringBuilder sqlbuffer = new StringBuilder();
        List parititons = Arrays.asList(table.getPartitionCols());
        if (parititons.contains("month") && parititons.contains("day")) {
            while (nowDay.after(oldday)) {
                sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"USE JCFW;");
                for (String branch : branches) {
                    String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + " ADD PARTITION (";
                    String pStr = "branch='" + branch + "', month=" + sdfMon.format(nowDay.getTime()) + ",day=" + sdfDay.format(nowDay.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowDay.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else if (parititons.contains("month") && !parititons.contains("day")) {
            while (nowMon.after(oldday)) {
                sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"");
                for (String branch : branches) {
                    String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + " ADD PARTITION (";
                    String pStr = "branch='" + branch + "',month=" + sdfMon.format(nowMon.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
        } else if (!parititons.contains("month") && !parititons.contains("day")) {
            sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"");
            for (String branch : branches) {
                String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + " ADD PARTITION (";
                String pStr = "branch='" + branch + "'";
                sqlbuffer.append(alterTabelStr).append(pStr).append(");");
            }
            sqlbuffer.append("\"\n");
        }
        return sqlbuffer.toString();
    }

    /**
     * 给地市模型建分区
     *
     * @param table
     * @param startDate
     * @param endDate
     * @param branches
     * @return
     * @throws ParseException
     */
    public static String makeBranchTablePartition(Table table, String startDate, String endDate, String[] branches) throws ParseException {
        SimpleDateFormat sdfDay = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfMon = new SimpleDateFormat("yyyyMM");

        Calendar oldday = Calendar.getInstance();
        oldday.setTime(sdfDay.parse(startDate));

        Calendar nowDay = Calendar.getInstance();
        nowDay.setTime(sdfDay.parse(endDate));
        Calendar nowMon = Calendar.getInstance();
        nowMon.setTime(sdfDay.parse(endDate));

        StringBuilder sqlbuffer = new StringBuilder();
        List parititons = Arrays.asList(table.getPartitionCols());
        if (parititons.contains("month") && parititons.contains("day")) {
            while (nowDay.after(oldday)) {
                sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"");
                for (String branch : branches) {
                    String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + "_" + branch + " ADD PARTITION (";
                    String pStr = "month=" + sdfMon.format(nowDay.getTime()) + ",day=" + sdfDay.format(nowDay.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowDay.add(Calendar.DAY_OF_MONTH, -1);
            }
        } else if (parititons.contains("month") && !parititons.contains("day")) {
            while (nowMon.after(oldday)) {
                sqlbuffer.append("perl ~etl/dssprog/bin/hadoop_cli.pl ").append(table.getTenantUser()).append(" beeline -e \"");
                for (String branch : branches) {
                    String alterTabelStr = "ALTER TABLE JCFW." + table.getTableName() + "_" + branch + " ADD PARTITION (";
                    String pStr = "month=" + sdfMon.format(nowMon.getTime());
                    sqlbuffer.append(alterTabelStr).append(pStr).append(");");
                }
                sqlbuffer.append("\"\n");
                nowMon.add(Calendar.MONTH, -1);
            }
        }
        return sqlbuffer.toString();
    }

    /**
     * 生成重建模型分区语句
     *
     * @param showPartitionsLogFile
     * @param tableMap
     * @param branches
     * @param useProxyProgram
     * @return
     */
    public static StringBuilder reAddTableParition(String showPartitionsLogFile, Map<String, Table> tableMap,
            String[] branches, boolean useProxyProgram) {
        List<String> loglines = TextUtil.readTxtFileToList(showPartitionsLogFile, false);
        Map<String, StringBuilder> usermap = new HashMap();
        String tablename = null;
        String username = null;
        for (String linestr : loglines) {
            if (linestr.contains("show partitions ")) {
                int idx = linestr.indexOf("show partitions");
                tablename = linestr.substring(idx).replaceAll("show partitions", "").replaceAll(";", "").trim();
                Table tab = tableMap.get(tablename);
                if (tab == null) {
                    int indx = tablename.lastIndexOf("_");
                    String tmptabname = tablename.substring(0, indx);
                    tab = tableMap.get(tmptabname);
                }
                if (tab != null) {
                    username = tab.getTenantUser();
                }
                StringBuilder sbdf = usermap.get(username);
                if (sbdf == null) {
                    sbdf = new StringBuilder();
                    usermap.put(username, sbdf);
                }
                continue;
            }
            if (!linestr.contains("branch=") && !linestr.contains("month=")
                    && !linestr.contains("day=") && !linestr.contains("year=")
                    && !linestr.contains("ds=") && !linestr.contains("hour=")) {
                continue;
            }
            String[] partitions = linestr.replaceAll("\\|", "").trim().split("/");
            /**
             * 先删掉旧分区，再加新分区。
             */
            String droparrtsql = "ALTER TABLE JCFW." + tablename + " DROP IF EXISTS PARTITION (";

            for (int i = 0; i < partitions.length; i++) {
                String[] paritioncells = partitions[i].split("=");
                if (i == partitions.length - 1) {
                    droparrtsql += paritioncells[0] + "='" + paritioncells[1] + "');";
                } else {
                    droparrtsql += paritioncells[0] + "='" + paritioncells[1] + "',";
                }
            }
            String addpartsql = "ALTER TABLE JCFW." + tablename + " ADD PARTITION (";
            for (int i = 0; i < partitions.length; i++) {
                String[] paritioncells = partitions[i].split("=");
                if (i == partitions.length - 1) {
                    addpartsql += paritioncells[0] + "='" + paritioncells[1] + "');";
                } else {
                    addpartsql += paritioncells[0] + "='" + paritioncells[1] + "',";
                }
            }
            if (useProxyProgram) {
                droparrtsql = "remote_cli.pl " + username + " beeline -e \"USE JCFW;" + droparrtsql + "\"";
                addpartsql = "remote_cli.pl " + username + " beeline -e \"USE JCFW;" + addpartsql + "\"";
            }
            usermap.get(username).append(droparrtsql).append("\n");
            usermap.get(username).append(addpartsql).append("\n");
        }
        StringBuilder retbuffer = new StringBuilder();
        usermap.keySet().stream().forEach((key) -> {
            StringBuilder sbdf1 = usermap.get(key);
            retbuffer.append("---").append(key).append("租户脚本---\n");
            retbuffer.append(sbdf1.toString());
            System.out.println(retbuffer.toString());
        });
        System.out.println("处理完成");
        return retbuffer;
    }

    /**
     * 创建DB2建表语句
     *
     * @param tablenames
     * @param metadata
     * @return
     */
    public static Map<String, StringBuilder> makeTableCreateScriptForDB2(List<String> tablenames, Map<String, Table> metadata, String[] branchList) {
        Map<String, StringBuilder> hqlmap = new HashMap();
        List<String> failtables = new ArrayList();//存放生成建表语句失败的模型
        List<String> successtables = new ArrayList();//存放生成建表语句成功的模型
        if (tablenames != null) {
            tablenames.stream().map((tablename) -> metadata.get(tablename)).forEach((table) -> {
                String hqlstr = makeDB2TableCreateScript(table, failtables, successtables, branchList);//生成建表语句
                StringBuilder hqlbuffer = hqlmap.get(table.getTenantUser());
                if (hqlbuffer == null) {
                    hqlbuffer = new StringBuilder();
                    hqlmap.put(table.getTenantUser(), hqlbuffer);
                }
                hqlbuffer.append(hqlstr);
            });
        }
        System.out.println("输入（" + tablenames.size() + " ） 输出（" + successtables.size() + "）  失败（" + failtables.size() + "）");
        if (!failtables.isEmpty()) {
            System.out.print("处理失败模型：");
            System.out.println(Arrays.toString(failtables.toArray()));
        }
        return hqlmap;
    }

//    public static String createDB2TableScript(String[] branches, Table table, List<String> failtables, List<String> successtables, boolean outputSharedTable) {
//        StringBuilder sqlbuffer = new StringBuilder();
//        String tablecols = makeTableCreateScript(table);
//        sqlbuffer.append(tablecols);//表字段语句
//
//        String tableproperties = makeTablePropertiesScript(table);
//        sqlbuffer.append(tableproperties);//表属性语句
//        if (tablecols != null && tablecols.length() > 0 && tableproperties != null && tableproperties.length() > 0) {
//            System.out.println("[OK][Main Table]" + table.getTableName());
//            successtables.add(table.getTableName());
//        } else {
//            System.out.println("[Fail]" + table.getTableName());
//            failtables.add(table.getTableName());
//        }
//        /**
//         * 如果是地市共享模型，则生成创建地市共享模型脚本。
//         */
//        if (table.isShared() && outputSharedTable) {
//            String branchTableHql = createHiveBranchTableScript(branches, table, failtables, successtables);
//            if (branchTableHql != null && branchTableHql.trim().length() > 0) {
//                sqlbuffer.append(branchTableHql).append("\r\n");
//            }
//        }
//        return sqlbuffer.toString();
//    }
    public static void writeToFile(Map<String, StringBuilder> dataBufferMap, String filename) throws IOException {
        BufferedWriter bufwriter = null;
        try {
            if (!dataBufferMap.isEmpty()) {
                for (String key : dataBufferMap.keySet()) {
                    String usrRentSql = dataBufferMap.get(key).toString();
                    String outFileName = filename;
                    if (key.equalsIgnoreCase("CTL.HIVE_CHECK_RULE")) {
                        outFileName = System.getProperty("user.dir") + "\\" + key.toUpperCase() + ".txt";
                    } else if (key != null && key.trim().length() > 0) {
                        outFileName = System.getProperty("user.dir") + "\\" + key.toUpperCase() + ".sql";
                    }
                    OutputStreamWriter writerStream = new OutputStreamWriter(new FileOutputStream(outFileName), "UTF-8");
                    bufwriter = new BufferedWriter(writerStream);
                    bufwriter.write(usrRentSql);
                    bufwriter.newLine();
                    bufwriter.close();
                    System.out.println("输出结果：" + outFileName);
                }
            }
        } catch (IOException e) {
            System.out.println("[FAIL]输出建表语句失败！\n");
            Logger.getLogger(ModeScriptBuilder.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (bufwriter != null) {
                bufwriter.close();
            }
        }
    }
}
