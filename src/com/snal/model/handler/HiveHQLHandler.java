/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.handler;

import com.snal.model.beans.ModelChangeReq;
import com.snal.model.beans.Table;
import com.snal.model.constant.Const;
import com.snal.model.core.ContextData;
import com.snal.model.core.HiveModelHQLBuilder;
import com.snal.model.core.ModelExport;
import com.snal.model.core.ModelSyncToPaas;
import com.snal.model.core.TableUtil;
import com.snal.model.util.text.TextUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luo Tao
 */
public class HiveHQLHandler {

    public Map<String, StringBuilder> makeModelUpgradeSql(List<ModelChangeReq> reqList) {
        Map<String, StringBuilder> sqlMap = new HashMap<>();//key：租户名
        try {
            ModelSyncToPaas syncpaas = new ModelSyncToPaas();
            String[] branches = ContextData.getProperty("branch.code.value").split(",");//地市简称
            boolean outMainTable = Boolean.parseBoolean(ContextData.getProperty("script.output.main.table"));//是否输出主模型脚本
            boolean outBranchTable = Boolean.parseBoolean(ContextData.getProperty("script.output.share.table"));//是否输出地市模型脚本

            HiveModelHQLBuilder hiveBuilder = new HiveModelHQLBuilder();

            //用于保存主键检查配置脚本
            StringBuilder chekPKRuleBuffer = new StringBuilder();
            sqlMap.put("CTL.HIVE_CHECK_RULE", chekPKRuleBuffer);
            //用于保存其他不需要区分租户的脚本。
            StringBuilder otherBuffer = new StringBuilder();
            sqlMap.put("CHANGE_STS_ON_PAAS", otherBuffer);

            StringBuilder exportSqlBuffer = new StringBuilder();//导出模型SQL时使用。
            //用于存储需要同步到PAAS的模型名称
            List<Table> syncTableList = new ArrayList();

            List<Table> syncTempList = new ArrayList();//用于大量模型一次性同步PAAS
            StringBuilder syncTempBuffer = new StringBuilder();//用于大量模型一次性同步PAAS
            int successCount = 0;
            int failCount = 0;

            int exportCount = 0;//导出模型时用到，用于计算导出模型个数，超出个数则分文件输出。
            int exportFileNum = 0;
            int syncFileNum = 0;
            int synchCount = 0;
            int tableTotal = 0;
            /**
             * 计算模型总数量，只用于模型批量同步PAAS和模型导出，用于分文件输出。
             */
            for (ModelChangeReq changeReq : reqList) {
                Table mainTable = ContextData.hiveTableMap.get(changeReq.getTableName());
                if (mainTable != null) {
                    tableTotal++;
                    if (mainTable.isShared()) {
                        tableTotal += 22;//21个地市表 +OTH 表
                    }
                }
            }
            for (ModelChangeReq changeReq : reqList) {
                Table mainTable = ContextData.hiveTableMap.get(changeReq.getTableName());
                if (mainTable == null) {
                    System.out.println("Table not found:" + changeReq.getTableName());
                    throw new Exception("Table not found.");
                }
                List<Table> processTableList = new ArrayList<>();//需要处理的模型对象
                if (outMainTable) {
                    processTableList.add(mainTable);
                }
                if (outBranchTable) {
                    //如果有不需要处理地市模型的场景，则增加限制条件。
                    List<Table> branchTables = TableUtil.cloneTableForBranch(mainTable, branches);
                    processTableList.addAll(branchTables);
                }
                StringBuilder sqlbuffer = sqlMap.get(mainTable.getTenantUser());
                if (sqlbuffer == null) {
                    sqlbuffer = new StringBuilder();
                    sqlMap.put(mainTable.getTenantUser(), sqlbuffer);
                }
                for (Table table : processTableList) {
                    String sqlstr = "";
                    switch (changeReq.getChangeType()) {
                        case Const.HIVE_CREATE_TABLE://新建表
                            sqlstr = hiveBuilder.createTable(table);
                            sqlbuffer.append(sqlstr).append("\n");
                            syncTableList.add(table);
                            break;
                        case Const.HIVE_ADD_COLUMN:  //追加字段
                            String[] newColumns = changeReq.getChangeContent().split(",");
                            sqlstr = hiveBuilder.addColumns(table, newColumns);
                            sqlbuffer.append(sqlstr).append("\n");
                            syncTableList.add(table);
                            break;
                        case Const.HIVE_CHANGE_COLUMN://修改字段
                            String[] columnPairs = changeReq.getChangeContent().split(",");
                            sqlstr = hiveBuilder.modifyColumns(mainTable, columnPairs);
                            sqlbuffer.append(sqlstr).append("\n");
                            syncTableList.add(table);
                            break;
                        case Const.PAAS_PUBLISH_TABLE://PAAS平台发布模型
                            if (table.getTableName().endsWith("_OTH")) {
                                continue;
                            }
                            sqlstr = hiveBuilder.publishTableOnPaas(table);
                            otherBuffer.append(sqlstr).append("\n");
                            break;
                        case Const.HIVE_CHANGE_DELIMITERS://修改分隔符
                            sqlstr = hiveBuilder.changeTableColDelimiters(table);
                            sqlbuffer.append(sqlstr).append("\n");
                            syncTableList.add(table);
                            break;
                        case Const.HIVE_CHANGE_CHARSET://修改字符集
                            syncTableList.add(table);
                            sqlstr = hiveBuilder.changeEncodingCode(table);
                            sqlbuffer.append(sqlstr).append("\n");
                            break;

                        case Const.HIVE_CHANGE_FILEFORMAT://修改存储格式
                            syncTableList.add(mainTable);
                            sqlstr = hiveBuilder.changeTableFileFormat(table);
                            sqlbuffer.append(sqlstr).append("\n");
                            break;
                        case Const.HIVE_READD_PARTITION://重建分区
                            String[] changeContent = changeReq.getChangeContent().split("\\|");
                            String partitionBranch = changeContent[0].substring(changeContent[0].length() - 4, changeContent[0].length());
                            String[] timeInfo = changeContent[1].split("-");
                            sqlstr = hiveBuilder.reAddPartitions(table, timeInfo[0], timeInfo[1], partitionBranch);
                            sqlbuffer.append(sqlstr).append("\n");
                            break;
                        case Const.PAAS_OFFLINE_TABLE://模型下线
                            if (table.getTableName().endsWith("_OTH")) {
                                continue;
                            }
                            sqlstr = hiveBuilder.changeTableOfflineInPaas(mainTable);
                            otherBuffer.append(sqlstr).append("\n");
                            break;
                        case Const.PAAS_SYNC_TABLE://同步PAAS平台
                            syncTempList.add(table);
                            synchCount++;
                            successCount++;
                            if (synchCount % 500 == 0 || synchCount == tableTotal) {
                                syncFileNum++;
                                syncpaas.syncModelToPaas(syncTempBuffer, changeReq.getReqId(), syncTempList, branches);
                                syncTempList.clear();
                                String filename = ContextData.getProperty("output.dir") + "\\" + changeReq.getReqId() + "\\" + changeReq.getReqId() + "_"
                                        + changeReq.getDbType() + "_" + "SYNC_TO_PAAS" + syncFileNum + ".sql";
                                File file = new File(ContextData.getProperty("output.dir") + "\\" + changeReq.getReqId());
                                if (!file.exists()) {
                                    file.mkdir();
                                }
                                TextUtil.writeToFile(syncTempBuffer.toString(), filename);

                                synchCount = 0;
                                syncTempBuffer.delete(0, syncTempBuffer.length());
                            }
                            break;
                        case Const.HIVE_EXPORT_TABLE:
                            sqlstr = ModelExport.export(exportSqlBuffer, table);
                            exportCount++;
                            if (sqlstr != null && sqlstr.trim().length() > 0) {
                                successCount++;
                            } else {
                                failCount++;
                            }

                            //每500个模型的SQL作为一个文件。
                            if (exportCount % 500 == 0 || exportCount == tableTotal) {
                                exportFileNum++;
                                String filename = ContextData.getProperty("output.dir") + "\\" + changeReq.getReqId() + "\\" + changeReq.getReqId() + "_"
                                        + changeReq.getDbType() + "_" + "EXPORT_SQL_FLIE" + exportFileNum + ".sql";
                                File file = new File(ContextData.getProperty("output.dir") + "\\" + changeReq.getReqId());
                                if (!file.exists()) {
                                    file.mkdir();
                                }
                                TextUtil.writeToFile(exportSqlBuffer.toString(), filename);
                                exportCount = 0;
                                exportSqlBuffer.delete(0, exportSqlBuffer.length());
                            }
                            break;
                        default:
                            System.out.println("[Fail] unsupported  opr type:" + changeReq.getTableName() + " -> " + changeReq.getChangeType());
                    }
                    if (!changeReq.getChangeType().equals(Const.PAAS_SYNC_TABLE)
                            && !changeReq.getChangeType().equals(Const.HIVE_EXPORT_TABLE)) {
                        if (sqlstr != null && sqlstr.trim().length() > 0) {
                            successCount++;
                            System.out.println("[OK] " + table.getTableName() + "  " + table.getTableNameZh());
                        } else {
                            System.out.println("[Fail] " + table.getTableName() + "  " + table.getTableNameZh());
                            failCount++;
                        }
                    }
                }
                /**
                 * 部分修改是不需要输出主键检查配置
                 */
                if (changeReq.getChangeType().equals(Const.HIVE_CREATE_TABLE)) {
                    String checkPKRule = hiveBuilder.createPKCheckRule(mainTable);
                    if (checkPKRule != null && checkPKRule.trim().length() != 0) {
                        chekPKRuleBuffer.append(checkPKRule).append("\n");
                    }
                }
            }
            /**
             * 生成同步PAAS脚本
             */
            if (!syncTableList.isEmpty()) {
                if (reqList != null && !reqList.isEmpty()) {
                    String srId = reqList.get(0).getReqId();
                    StringBuilder syncbuffer = new StringBuilder();
                    syncpaas.syncModelToPaas(syncbuffer, srId, syncTableList, branches);
                    sqlMap.put("SYNC_TO_PAAS", syncbuffer);
                }
            }
            System.out.println("input:" + reqList.size() + "   success:" + successCount + "  fail:" + failCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlMap;
    }
}
