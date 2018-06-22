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
import com.snal.model.core.DB2ModelSQLBuilder;
import com.snal.model.core.ModelExport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luo Tao
 */
public class DB2SQLHandler {

    public Map<String, StringBuilder> makeModelUpgradeSql(List<ModelChangeReq> reqList) {
        Map<String, StringBuilder> sqlMap = new HashMap<>();
        try {
            String[] branches = ContextData.getProperty("branch.code.value").split(",");//地市简称
            StringBuilder sqlbuffer = new StringBuilder();
            StringBuilder exportSqlBuffer = new StringBuilder();//导出模型SQL时使用。
            int successCount = 0;
            int failCount = 0;
            DB2ModelSQLBuilder sqlbuilder = new DB2ModelSQLBuilder();

            int exportCount = 0;//导出模型时用到，用于计算导出模型个数，超出个数则分文件输出。
            int exportFileNum = 0;
            for (ModelChangeReq changeReq : reqList) {
                Table table = ContextData.db2TableMap.get(changeReq.getTableName());
                if (table == null) {
                    System.out.println("Table not found:" + changeReq.getTableName());
                    throw new Exception("Table not found.");
                }
                table.setUpgradeNotice(changeReq.getNotice() == null ? "" : changeReq.getNotice());//升级时注意事项
                String sql = "";
                switch (changeReq.getChangeType()) {
                    //新建表
                    case Const.DB2_CREATE_TABLE: {
                        sql = sqlbuilder.makeCreateTable(table, branches);
                        sqlbuffer.append(sql);
                        break;
                    }
                    //追加字段
                    case Const.DB2_ADD_COLUMN: {
                        String[] newColumns = changeReq.getChangeContent().split(",");
                        sql = sqlbuilder.addColumns(table, newColumns);
                        sqlbuffer.append(sql);
                        break;
                    }
                    //修改字段
                    case Const.DB2_CHANGE_DATA_TYPE: {
                        String[] newColumns = changeReq.getChangeContent().split(",");
                        sql = sqlbuilder.changeDataType(table, newColumns);
                        sqlbuffer.append(sql);
                        break;
                    }
                    case Const.DB2_EXPORT_TABLE: {
                        sql = ModelExport.export(exportSqlBuffer,table);
                        exportSqlBuffer.append(sql);
                        exportCount++;
                        //全量导出时，每400个模型的SQL作为一个文件。
                        if (exportCount % 500 == 0 || exportCount == reqList.size()) {
                            exportFileNum++;
                            sqlMap.put("EXPORT_SQL_FLIE" + exportFileNum, exportSqlBuffer);
                            exportCount = 0;
                            exportSqlBuffer = new StringBuilder();
                        }
                        break;
                    }
                    default:
                        System.out.println("[Fail] unsupported  opr type:" + table.getTableName() + " -> " + changeReq.getChangeType());
                }
                if (sql != null && sql.trim().length() != 0) {
                    successCount++;
                    System.out.println("[OK] " + table.getTableName() + "  " + table.getTableNameZh());
                } else {
                    failCount++;
                    System.out.println("[Fail] " + table.getTableName() + "  " + table.getTableNameZh());
                }
            }
            sqlMap.put("GDDW", sqlbuffer);
            System.out.println("input:" + reqList.size() + "  success:" + successCount + "  fail:" + failCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlMap;
    }
}
