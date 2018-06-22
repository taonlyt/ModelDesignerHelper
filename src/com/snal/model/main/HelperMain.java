/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.main;

import com.snal.model.beans.ChangeRequest;
import com.snal.model.beans.ModelChangeReq;
import com.snal.model.beans.Table;
import com.snal.model.beans.TableCol;
import com.snal.model.constant.Const;
import com.snal.model.core.ContextData;
import com.snal.model.core.PropertiesFileLoader;
import com.snal.model.handler.DB2SQLHandler;
import com.snal.model.handler.HiveHQLHandler;
import com.snal.model.util.excel.ExcelUtil;
import com.snal.model.util.text.TextUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 *
 * @author Luo Tao
 */
public class HelperMain {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            ContextData.configration = PropertiesFileLoader.loadConfData();
            ContextData.tenantMap = PropertiesFileLoader.initTenantAttribute(ContextData.configration);
            System.out.println("loading tableid.txt ...");
            ContextData.oldTableIds = TextUtil.readTxtFileToList("tableid.txt", true);
            /**
             * 读取模型变更申请EXCEL表，将HIVE 和 DB2 的变更分别保存到两个集合中。然后分别调用对应的方法生成SQL语句。
             */
            ChangeRequest chgRequest = readChangeRequest();
            if (!chgRequest.getHiveChgRequest().isEmpty()) {
                List<String> tableNames = new ArrayList<>();
                chgRequest.getHiveChgRequest().stream().filter((chgModel) -> 
                        (!tableNames.contains(chgModel.getTableName()))).forEachOrdered((chgModel) -> {
                    tableNames.add(chgModel.getTableName());
                });
                System.out.println("loading hive tables ...");
                
                ContextData.loadHiveMetaData(tableNames);
                System.out.println("load completed...");
                if (check(chgRequest.getHiveChgRequest())) {
                    HiveHQLHandler hiveHander = new HiveHQLHandler();
                    Map<String, StringBuilder> hiveSqlMap = hiveHander.makeModelUpgradeSql(chgRequest.getHiveChgRequest());
                    writeToFile(hiveSqlMap, chgRequest, "HIVE");
                }
            }
            if (!chgRequest.getDb2ChgRequest().isEmpty()) {
                System.out.println("loading db2 tables ...");
                List<String> tableNames = new ArrayList<>();
                chgRequest.getDb2ChgRequest().stream().filter((chgModel) -> 
                        (!tableNames.contains(chgModel.getTableName()))).forEachOrdered((chgModel) -> {
                    tableNames.add(chgModel.getTableName());
                });
                ContextData.loadDB2MetaData(tableNames);
                System.out.println("load completed...");
                DB2SQLHandler db2Hander = new DB2SQLHandler();
                Map<String, StringBuilder> db2SqlMap = db2Hander.makeModelUpgradeSql(chgRequest.getDb2ChgRequest());
                writeToFile(db2SqlMap, chgRequest, "DB2");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取模型变更Excel表格
     *
     * @return
     */
    public static ChangeRequest readChangeRequest() {
        List<ModelChangeReq> hiveChangeReqList = new ArrayList<>();
        List<ModelChangeReq> db2ChangeReqList = new ArrayList<>();
        String changeReqFile = "upgrade.xlsx";
        XSSFSheet reqSheet = ExcelUtil.readHighExcelSheet(changeReqFile, 0);
        int rowcnt = reqSheet.getLastRowNum() + 1;
        String requestId = null;
        ModelChangeReq req = null;
        for (int i = 2; i < rowcnt; i++) {
            XSSFRow row = reqSheet.getRow(i);
            req = new ModelChangeReq();

            req.setReqId(ExcelUtil.getXCellValueString(row.getCell(0)));
            requestId = requestId == null ? req.getReqId() : requestId;
            req.setTableName(ExcelUtil.getXCellValueString(row.getCell(1)));
            req.setDbType(ExcelUtil.getXCellValueString(row.getCell(2)));
            req.setChangeType(ExcelUtil.getXCellValueString(row.getCell(3)));
            req.setChangeContent(ExcelUtil.getXCellValueString(row.getCell(4)));
            req.setNotice(ExcelUtil.getXCellValueString(row.getCell(5)));
            if (req.getReqId() == null || req.getReqId().trim().length() == 0) {
                System.out.println("[警告] 需求编号不能为空");
                continue;
            }
            if (req.getDbType() == null || req.getDbType().trim().length() == 0) {
                System.out.println("[警告] 数据类型不能为空");
                continue;
            }
            if (req.getTableName() == null || req.getTableName().trim().length() == 0) {
                System.out.println("[警告] 表名不能为空");
                continue;
            }
            if (req.getChangeType() == null || req.getChangeType().trim().length() == 0) {
                System.out.println("[警告] 变更类型不能为空");
                continue;
            }
            if (req.getChangeType().equals("H2") || req.getChangeType().equals("H3") || req.getChangeType().equals("H4")
                    || req.getChangeType().equals("D2") || req.getChangeType().equals("D3")) {
                if (req.getChangeContent() == null || req.getChangeContent().trim().length() == 0) {
                    System.out.println("[警告] " + req.getTableName() + " -> " + req.getChangeType() + " 变更内容不能为空");
                    continue;
                }
            }
            if (req.getDbType().equalsIgnoreCase("HIVE")) {
                List<String> hiveSupportedOpt = Arrays.asList(Const.HIVE_SUPPORT_OPRTYP.split(","));
                if (!hiveSupportedOpt.contains(req.getChangeType())) {
                    System.out.println("[警告] 不支持的HIVE操作类型，不处理 " + req.getTableName() + " -> " + req.getChangeType());
                } else {
                    hiveChangeReqList.add(req);
                }
            } else if (req.getDbType().equalsIgnoreCase("DB2")) {
                List<String> db2SupportedOpt = Arrays.asList(Const.DB2_SUPPORT_OPRTYP.split(","));
                if (!db2SupportedOpt.contains(req.getChangeType())) {
                    System.out.println("[警告] 不支持的DB2操作类型，不处理 " + req.getTableName() + " -> " + req.getChangeType());
                } else {
                    db2ChangeReqList.add(req);
                }
            }
        }
        ChangeRequest chgReq = new ChangeRequest(requestId, hiveChangeReqList, db2ChangeReqList);
        return chgReq;
    }

    public static void writeToFile(Map<String, StringBuilder> dataBufferMap, ChangeRequest changeReq, String dbType) throws IOException {
        BufferedWriter bufwriter = null;
        try {
            if (!dataBufferMap.isEmpty()) {
                File file = new File(ContextData.getProperty("output.dir") + "\\" + changeReq.getRequestId());
                if (!file.exists()) {
                    file.mkdir();
                }
                for (String key : dataBufferMap.keySet()) {
                    String sql = dataBufferMap.get(key).toString();
                    if (sql == null || sql.trim().length() == 0) {
                        continue;
                    }
                    String outFileName = changeReq.getRequestId();
                    if (key.equalsIgnoreCase("CTL.HIVE_CHECK_RULE")) {
                        outFileName = file.getAbsolutePath() + "\\" + changeReq.getRequestId() + "_" + key.toUpperCase() + ".txt";
                    } else if (key.trim().length() > 0) {
                        outFileName = file.getAbsolutePath() + "\\" + changeReq.getRequestId() + "_" + dbType + "_" + key.toUpperCase() + ".sql";
                    }
                    OutputStreamWriter writerStream = new OutputStreamWriter(new FileOutputStream(outFileName), "UTF-8");
                    bufwriter = new BufferedWriter(writerStream);
                    bufwriter.write(sql);
                    bufwriter.newLine();
                    bufwriter.close();
                    System.out.println("输出结果：" + outFileName);
                }
            }
        } catch (IOException e) {
            System.out.println("[FAIL]输出建表语句失败！\n");
            Logger.getLogger(HelperMain.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (bufwriter != null) {
                bufwriter.close();
            }
        }
    }

    /**
     * 只检查HIVE模型
     *
     * @param chgRequestList
     * @return
     */
    public static boolean check(List<ModelChangeReq> chgRequestList) {
        boolean isValid = true;
        try (Scanner scanner = new Scanner(System.in)) {
            for (ModelChangeReq changeReq : chgRequestList) {
                Table table = ContextData.hiveTableMap.get(changeReq.getTableName());
                if (table == null) {
                    System.out.println("模型不存在：" + changeReq.getTableName());
                    continue;
                }
                //新增的模型如果不是PARQUET格式则给出提示，固定参数模型除外。
                if (changeReq.getChangeType().equals(Const.HIVE_CREATE_TABLE)) {
                    if (!table.getStoredFormat().equalsIgnoreCase("PARQUET") && !table.isConstantParam()) {
                        System.out.println(table.getTableName() + " 存储格式不是PARQUET，可能影响性能，是否继续(Y/N)？");
                        String optional = scanner.nextLine();
                        if (!optional.equalsIgnoreCase("Y")) {
                            isValid = false;
                            break;
                        }
                    }
                    /**
                     * 主键检查
                     */
                    int pkCount = 0;
                    for (TableCol col : table.getTablecols()) {
                        if (col.isIsPrimaryKey()) {
                            pkCount++;
                        }
                    }
                    if (pkCount == 0) {
                        System.out.println(table.getTableName() + " 缺少主键，是否继续(Y/N)？");
                        String optional = scanner.nextLine();
                        if (!optional.equalsIgnoreCase("Y")) {
                            isValid = false;
                            break;
                        }
                    }
                    List<String> hivekeywords = Arrays.asList(ContextData.getProperty("hive.keywords").split(","));
                    isValid = checkTableColumn(table, hivekeywords);
                    if (!isValid) {
                        break;
                    }
                }
            }
        }
        return isValid;
    }

    private static boolean checkTableColumn(Table table, List<String> hiveKeywords) {
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
                if (!isValidColumnName(column.getColumnName())) {
                    System.out.println("[错误]" + column.getTableName() + "." + column.getColumnName() + " 字段名不正确！");
                    isValid = false;
                }
                if (!colunnames.contains(column.getColumnName())) {
                    colunnames.add(column.getColumnName());
                } else {
                    System.out.println("[错误]" + table.getTableName() + "." + column.getColumnName() + " 字段重复！");
                    isValid = false;
                }
                if (hiveKeywords.contains(column.getColumnName())) {
                    System.out.println("[警告]" + table.getTableName() + "." + column.getColumnName() + " 属于预留语法关键字！");
                    isValid = false;
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
    private static boolean isValidColumnName(String columName) {
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
    private static boolean isContainChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
