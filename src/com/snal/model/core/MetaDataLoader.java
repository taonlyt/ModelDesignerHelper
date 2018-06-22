/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.util.excel.BigExcelUtil;
import com.snal.model.beans.Table;
import com.snal.model.beans.TenantAttribute;
import com.snal.model.beans.TableCol;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author luotao
 */
public class MetaDataLoader {

    /**
     * 加载元数据 key：表名 value：表信息
     *
     * @param metaDataFile
     * @param tenantMap
     * @param startsheet
     * @param endsheet
     * @param mincolumns
     * @param tableNames
     * @return
     * @throws java.lang.Exception
     */
    public Map<String, Table> loadMetaData(String metaDataFile, List<String> tableNames,
            int startsheet, int endsheet, int[] mincolumns) throws Exception {
        BigExcelUtil bigExlUtil = new BigExcelUtil();
        Map<String, TenantAttribute> tenantMap = ContextData.tenantMap;
        Map<String, List<List<String>>> metadata = bigExlUtil.readExcelData(metaDataFile, startsheet, endsheet, mincolumns);
        List<List<String>> tableIndxList = metadata.get("0");//第一个工作表格，模型索引
        List<List<String>> tableStructList = metadata.get("1");//第二个工作表格，模型结构
        Map<String, Table> metaDataMap = new HashMap();
        for (int i = 1; i < tableIndxList.size(); i++) {
            List<String> row = tableIndxList.get(i);
            String tableName = row.get(3);
            if (row == null || !tableNames.contains(tableName)) {
                continue;
            }
            Table table = createTable(row, tenantMap);
            table.setLineId(i);
            table.setMainTable(true);
            for (String tableIdStr : ContextData.oldTableIds) {
                String[] tableIdArray = tableIdStr.split(",");
                if (tableIdArray[0].equalsIgnoreCase(table.getTableName())
                        && tableIdArray[1] != null
                        && tableIdArray[1].trim().length() > 0) {
                    table.setTableId(tableIdArray[1]);//从PAAS平台导出的模型唯一标识XMLID
                    break;
                }
            }
            metaDataMap.put(table.getTableName(), table);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        String currentMonth = sdf.format(new Date());
        for (int j = 1; j < tableStructList.size(); j++) {
            List<String> row = tableStructList.get(j);
            String tableName = row.get(1);
            if (row == null || !tableNames.contains(tableName)) {
                continue;
            }
            TableCol tableCol = createTableCol(row, currentMonth);
            tableCol.setLineId(j);
            Table mainTable = metaDataMap.get(tableCol.getTableName());
            if (mainTable != null) {
                mainTable.addTableCol(tableCol);
                tableCol.setTableId(mainTable.getTableId());
            } else {
                System.out.println("【模型索引】中缺少[模型名：" + tableCol.getTableName() + "][行号:" + tableCol.getLineId() + "]");
            }
        }
        /**
         * 将模型分区字段添加到模型字段列表中。
         */
        for (String tablename : metaDataMap.keySet()) {
            Table table = metaDataMap.get(tablename);
            if (table.getTablecols() == null) {
                System.out.println("[警告] " + tablename + " 模型未定义字段.");
                continue;
            }
            TableCol tablecol0 = table.getTablecols().get(0);
            int colcount = table.getTablecols().size();
            String[] partitioncosl = table.getPartitionCols();
            int partitionseq = 0;
            for (String colname : partitioncosl) {
                if (colname == null || colname.isEmpty()) {
                    continue;
                }
                TableCol tablecol = tablecol0.clone();
                tablecol.setColumnName(colname);
                tablecol.setColumnNameZh(transPartitionName(colname));
                tablecol.setColumnSeq(String.valueOf(++colcount));
                if (colname.equalsIgnoreCase("branch") && table.isMainTable()) {
                    tablecol.setDataType("STRING");
                } else if (colname.equalsIgnoreCase("month") || colname.equalsIgnoreCase("day")
                        || colname.equalsIgnoreCase("year") || colname.equalsIgnoreCase("week")
                        || colname.equalsIgnoreCase("ds") || colname.equals("hour")) {
                    tablecol.setDataType("INT");
                }
                tablecol.setColumnId(tablecol.getTableName() + "_201610" + tablecol.getColumnSeq());
                tablecol.setPartitionSeq(String.valueOf(++partitionseq));
                tablecol.setIsPrimaryKey("");
                tablecol.setIsNullable("N");
                tablecol.setSecurityType3("");
                tablecol.setSensitivityLevel("");
                tablecol.setOutSensitivityId("");
                tablecol.setRemark("");
                tablecol.setOpenState("");
                table.addTableCol(tablecol);
            }
        }
        return metaDataMap;
    }

    private String transPartitionName(String colname) {
        String name = colname;
        if (colname.equalsIgnoreCase("branch")) {
            name = "地市";
        } else if (colname.equalsIgnoreCase("month")) {
            name = "月份";
        } else if (colname.equalsIgnoreCase("day")) {
            name = "日期";
        } else if (colname.equalsIgnoreCase("year")) {
            name = "年份";
        } else if (colname.equalsIgnoreCase("week")) {
            name = "周";
        } else if (colname.equalsIgnoreCase("ds")) {
            name = "数据源";
        } else if (colname.equalsIgnoreCase("hour")) {
            name = "小时";
        }
        return name;
    }

    private TableCol createTableCol(List<String> tablecolumn, String currentMonth) {
        TableCol tablecol = new TableCol();
        int i = 0;
        tablecol.setDbType(tablecolumn.get(i++));
        tablecol.setTableName(tablecolumn.get(i++));
        tablecol.setTableId(tablecol.getTableName());//模型标识
        tablecol.setColumnSeq(tablecolumn.get(i++));
        tablecol.setColumnId(tablecol.getTableName() + "_" + currentMonth + tablecol.getColumnSeq());//字段标识
        tablecol.setColumnName(tablecolumn.get(i++));
        tablecol.setColumnNameZh(tablecolumn.get(i++));
        tablecol.setDataType(tablecolumn.get(i++));
        tablecol.setLength(tablecolumn.get(i++));
        tablecol.setPrecision(tablecolumn.get(i++));
        tablecol.setPartitionSeq(tablecolumn.get(i++));
        tablecol.setIsPrimaryKey(tablecolumn.get(i++));
        tablecol.setPartKeyForPrimaryKey(tablecolumn.get(i++));
        tablecol.setIsNullable(tablecolumn.get(i++));
        tablecol.setRemark(tablecolumn.get(i++));
        if (tablecol.getRemark() != null && tablecol.getRemark().length() > 4000) {
            tablecol.setRemark(tablecol.getRemark().substring(0, 4000));//单引号转移，避免破会SQL语法结构。
        }
        //替换连续换行符为一个换行符
        Pattern p = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
        Matcher m = p.matcher(tablecol.getRemark().replaceAll("'", "''"));
        tablecol.setRemark(m.replaceAll("\r\n"));
        
        tablecol.setOpenState(tablecolumn.get(i++));
        tablecol.setSecurityType3(tablecolumn.get(i++));
        tablecol.setSensitivityLevel(tablecolumn.get(i++));
        
        switch (tablecol.getSecurityType3()) {
            case "C1-1":
            case "C1-5":
            case "D1-7":
            case "D2-3":
            case "D2-4":
            case "D3-5": {
                tablecol.setSensitivityLevel("1");
                break;
            }
            case "C1-3":
            case "C2-1":
            case "C2-2":
            case "D1-3":
            case "D1-6":
            case "D2-2":
            case "D3-4":
            case "D3-6": {
                tablecol.setSensitivityLevel("2");
                break;
            }
            case "A1-1":
            case "A1-2":
            case "A1-3":
            case "B1-1":
            case "B1-2":
            case "C1-2":
            case "C1-4":
            case "D1-2":
            case "D1-5":
            case "D1-8":
            case "D2-1":
            case "D3-3":
            case "D4-1":
            case "D4-2": {
                tablecol.setSensitivityLevel("3");
                break;
            }
            case "A1-4":
            case "A1-5":
            case "A2-1":
            case "D1-1":
            case "D1-4":
            case "D3-1":
            case "D3-2": {
                tablecol.setSensitivityLevel("4");
                break;
            }
            default: {
                break;
            }
        }
        tablecol.setOutSensitivityId(tablecolumn.get(i++));
        tablecol.setDataExample(tablecolumn.get(i++));
        if (tablecol.getDataExample() != null) {
            tablecol.setDataExample(tablecol.getDataExample().replaceAll("'", "''"));//单引号转移，避免破会SQL语法结构。
            //替换连续换行符为一个换行符
            Pattern p1 = Pattern.compile("(\r?\n(\\s*\r?\n)+)");
            Matcher m1 = p1.matcher(tablecol.getDataExample());
            tablecol.setDataExample(m1.replaceAll("\r\n"));
        }
        return tablecol;
    }

    public boolean checkTableColumn(Table table, List<String> hiveKeywords) {
        boolean isValid = true;
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_]*$");
        List<TableCol> tablecolumns = table.getTablecols();
        if (tablecolumns != null) {
            List<String> colunnames = new ArrayList(tablecolumns.size());
            for (TableCol column : tablecolumns) {
                Matcher matcher = pattern.matcher(column.getColumnName());
                if (!matcher.find()) {
                    System.out.println("[错误]" + column.getTableName() + "." + column.getColumnName() + " 字段名不正确！");
                    isValid = false;
                }
//                if (!isValidColumnName(column.getColumnName())) {
//                    System.out.println("[错误]" + column.getTableName() + "." + column.getColumnName() + " 字段名不正确！");
//                    isValid = false;
//                }
                if (!colunnames.contains(column.getColumnName())) {
                    colunnames.add(column.getColumnName());
                } else {
                    System.out.println("[错误]" + table.getTableName() + "." + column.getColumnName() + " 字段重复！");
                    isValid = false;
                }
                if (hiveKeywords.contains(column.getColumnName())) {
                    System.out.println("[错误]" + table.getTableName() + "." + column.getColumnName() + " 属于语法关键字！");
                    //isValid = false;
                }
            }
        }
        return isValid;
    }

    /**
     * 检查字段是否为有效字段名
     *
     * @param columName
     * @return
     */
    private boolean isValidColumnName(String columName) {
        if (columName.contains(" ")) {
            return false;
        }
        for (int i = 0; i < columName.length(); i++) {
            if (isContainChineseChar(columName.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否包含中文
     *
     * @param c
     * @return
     */
    private boolean isContainChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private Table createTable(List<String> tableindx, Map<String, TenantAttribute> tenantMap) throws Exception {
        Table table = new Table();
        int i = 0;
        table.setMainTable(true);
        table.setDataDomain(tableindx.get(i++));
        table.setDbType(tableindx.get(i++));
        table.setDbName(tableindx.get(i++));
        table.setTableName(tableindx.get(i++));
        /**
         * 模型 XMLID 不直接从EXCEL中读取，代码固话，减少excel填写工作。
         */
        table.setTableId(table.getTableName());
        table.setTableNameZh(tableindx.get(i++));
        table.setTableModel(tableindx.get(i++));
        if (table.getTableModel() == null || table.getTableModel().trim().length() == 0) {
            throw new Exception(table.getTableName() + " 表模式不能为空");
        }
        if (!table.getTableNameZh().startsWith(table.getTableModel() + "_")) {
            table.setTableNameZh(table.getTableModel() + "_" + table.getTableNameZh());
        }
        table.setState(tableindx.get(i++));
        table.setCycleType(tableindx.get(i++));
        table.setTopicName(tableindx.get(i++));
        table.setTopicCode(tableindx.get(i++));
        if (table.getTopicName().startsWith("基础层")) {
            table.setInterfaceTable("是");//是否接口模型，用于设置数据质量监控阀值。
        }
        table.setTenantUser(tableindx.get(i++));
        table.setColDelimiter(tableindx.get(i++));
        String partcols = tableindx.get(i++);
        if (partcols.equalsIgnoreCase("NA")) {
            partcols = "";
        }
        table.setPartitionCols(partcols);
        table.setStoredFormat(tableindx.get(i++));
        if (table.getStoredFormat().equalsIgnoreCase("TEXTFILE")) {
            if (table.getColDelimiter().equals("\\001")
                    || table.getColDelimiter().equals("\t")
                    || table.getColDelimiter().length() == 1
                    || table.getColDelimiter().startsWith("0x")) {
                table.setSerdeClass("org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe");
            } else {
                table.setSerdeClass("org.apache.hadoop.hive.contrib.serde2.MultiDelimitSerDe");
            }
        } else if (table.getStoredFormat().equalsIgnoreCase("ORC")) {
            table.setSerdeClass("org.apache.hadoop.hive.ql.io.orc.OrcSerde");
        } else if (table.getStoredFormat().equals("PARQUET")) {
            table.setSerdeClass("org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe");
        } else {
            table.setSerdeClass("org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe");
        }
        table.setCharacterSet(tableindx.get(i++));
//        table.setSerdeClass(tableindx.get(i++));//取消从excel读取，因为excel经常有人填错。
//        table.setRightlevel(tableindx.get(i++));//取消该字段，敏感级别通过安全等级自动计算。
        table.setCreater(tableindx.get(i++));
//        table.setCurdutyer(tableindx.get(i++));//PAAS 平台填写固定值。
        table.setEffDate(tableindx.get(i++));//创建时间
        if (table.getEffDate() == null || table.getEffDate().trim().length() == 0) {
            table.setEffDate("2016/10/01");
        }
        table.setStateDate(tableindx.get(i++));

        if (table.getStateDate() == null || table.getStateDate().trim().length() == 0) {
            table.setStateDate("2016/10/01");
        }
//        table.setOpenState(tableindx.get(i++));//改字段已经失效
        table.setRemark(tableindx.get(i++));
        if ("HIVE".equals(table.getDbType())) {
            //table.setIsVIP218(tableindx.get(i++));已经无用处
            table.setShared(tableindx.get(i++));
            table.setConstantParam(tableindx.get(i++));
            //table.setHasJobConf(tableindx.get(i++));已经无用处
            //table.setInterfaceTable(tableindx.get(i++));已经无用处
            table.setShareAllDataToCity(tableindx.get(i++));
            /**
             * 设置模型其他属性
             */
            TenantAttribute tenantAttr = tenantMap.get(table.getTenantUser());
            table.setLocation(tenantAttr.getLocation() + "/" + table.getTableModel() + "/" + table.getTableName());
            table.setDbServName(tenantAttr.getDbserver());
            table.setTeamCode(tenantAttr.getTeamcode());
            /**
             * 格式：
             * {"EXTERNAL":"y","LOCATION":"/tenant/BIGDATA/JCFW/JRCL/BD_B/STAGE/TS_BASS_NBRGRP","FILEFORMAT":"TEXTFILE","DELIMITER":"@#$"}
             * {"EXTERNAL":"y",,"LOCATION":"/tenant/BIGDATA/JCFW/JRCL/BD_B/STAGE/TS_O_HSH_ECCOUPON_ALLOT_D","FILEFORMAT":"TEXTFILE","DELIMITER":"|"}
             */
            StringBuilder sbdbuff = new StringBuilder();
            sbdbuff.append("{\"EXTERNAL\":\"y\",")
                    .append("\"LOCATION\":\"")
                    .append(table.getLocation()).append("\",")
                    .append("\"FILEFORMAT\":\"")
                    .append(table.getStoredFormat()).append("\",")
                    .append("\"DELIMITER\":\"")
                    .append(table.getColDelimiter()).append("\"}");
            table.setExtendcfg(sbdbuff.toString());
        }
        table.setState("PUBLISHED");
        table.setOpenState("");
        return table;
    }
}
