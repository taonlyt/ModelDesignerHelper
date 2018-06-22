/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.core;

import com.snal.model.beans.Table;
import com.snal.model.beans.TableCol;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Luo Tao
 */
public class ModelSyncToPaas {

    public StringBuilder syncModelToPaas(StringBuilder sqlbuffer,String srId, List<Table> mTableList, String[] branches) {
        long operseq = System.currentTimeMillis();
        //生成写入 paas 平台的 tablefile_imp_temp,column_val_imp_temp 临时表的语句，这两个表用于存储当前最新模型信息。
        String[] headnames1 = {"oprseq", "xmlid", "dbname", "dataname", "datacnname", "state", "cycletype", "topiccode", "extend_cfg", "rightlevel", "creater",
            "curdutyer", "eff_date", "state_date", "team_code", "open_state", "remark", "sr_id", "table_explain"};
        String[] headnames2 = {"oprseq", "xmlid", "col_xmlid", "dataname", "col_seq", "colname", "colcnname", "datatype", "length",
            "precision_val", "party_seq", "isprimarykey", "isnullable", "remark", "filed_type_child", "sensitive_level", "data_example", "sr_id", "policyid"};
        //为了避免过多开销内存，将 sqlbuffer 当做参数传入。
        updateTableToTemp(sqlbuffer,operseq, srId, mTableList, headnames1, headnames2);
        sqlbuffer.append("\n\n");

        //将要同步到 paas 平台的模型信息从 paas 正式表 tablefile,column_val 删除。
        sqlbuffer.append("delete from tablefile a where xmlid in (select xmlid from tablefile_imp_temp where oprseq='").append(operseq).append("' and sr_id='").append(srId).append("');\n");
        sqlbuffer.append("delete from column_val a where xmlid in (select xmlid from tablefile_imp_temp where oprseq='").append(operseq).append("' and sr_id='").append(srId).append("');\n");
        sqlbuffer.append("delete from metaobj where xmlid in (select xmlid from tablefile_imp_temp where oprseq='").append(operseq).append("' and sr_id='").append(srId).append("');\n");
        sqlbuffer.append("delete from tableall where xmlid in(select xmlid from tablefile_imp_temp where oprseq='").append(operseq).append("' and sr_id='").append(srId).append("');\n\n");

        //写入最新模型信息到 PAAS 平台TABLEFILE和COLUMN_VAL两个表。
        String sqlToTableFile = "insert into tablefile( xmlid, dbname, dataname, datacnname, state, cycletype, topiccode, extend_cfg, rightlevel, creater, curdutyer, eff_date, state_date, team_code, open_state, remark ,table_explain)select  xmlid, dbname, dataname, datacnname, state, cycletype, topiccode, extend_cfg, rightlevel, creater, curdutyer, eff_date, state_date, team_code, open_state, remark,table_explain from tablefile_imp_temp where oprseq='" + operseq + "' and sr_id='" + srId + "';\n";
        String sqlToColumVal = "insert into column_val( xmlid, col_xmlid, dataname, col_seq, colname, colcnname, datatype, length, precision_val, party_seq, isprimarykey, isnullable, remark, filed_type_child, sensitive_level, data_example,policyid )select  xmlid, col_xmlid, dataname, col_seq, colname, colcnname, datatype, length, precision_val, party_seq, isprimarykey, isnullable, remark, filed_type_child, sensitive_level, data_example,policyid from column_val_imp_temp where oprseq='" + operseq + "' and  sr_id='" + srId + "';\n";
        sqlbuffer.append("\n")
                .append(sqlToTableFile)
                .append(sqlToColumVal);
        //写入最新元数据对象信息至PAAS平台的METAOBJ和TABLEALL两个表。
        String sqlToMetaObj = "insert into metaobj (xmlid, dbname, objname, objcnname, objtype, team_code, cycletype, topiccode, eff_date, creater, state, state_date, remark) select  xmlid, dbname, dataname, datacnname, 'tab', team_code, cycletype, topiccode, eff_date, creater, state, state_date, remark from tablefile where xmlid  in (select xmlid from tablefile_imp_temp  where oprseq='" + operseq + "' and sr_id='" + srId + "');\n";
        String sqlToTableAll = "insert into tableall (dbname,dataname,eff_date,xmlid,modeltab,creator,taskid,dropdate) select  dbname,  dataname,  eff_date,  xmlid,  dataname,  '谢英俊',  '20161101',  '9999/12/31' from tablefile  where xmlid  in (select xmlid from tablefile_imp_temp  where oprseq='" + operseq + "' and sr_id='" + srId + "');\n\n";
        sqlbuffer.append(sqlToMetaObj)
                .append(sqlToTableAll);
        //将已经开放了的模型开放状态改为“开放”（mds库）
        sqlbuffer.append("update tablefile tf set tf.open_state='开放' where exists (select 1 from mds.meta_team_role_table mtrt where mtrt.xmlid=tf.xmlid) and tf.xmlid in(select xmlid from tablefile_imp_temp where  oprseq='").append(operseq).append("' and sr_id='").append(srId).append("')\n");
        sqlbuffer.append("\nexit;");//调用脚本要求增加exit 才能正常退出。

        return sqlbuffer;
    }

    /**
     * 将要导入的模型从临时表中删除，然后写入最新的模型信息到临时表。
     *
     * @param tables
     * @return
     */
    private StringBuilder updateTableToTemp(StringBuilder sqlbuffer,long oprseq, String srId, List<Table> tables, 
            String[] colNames, String[] fieldsNames) {
        for (Table table : tables) {
            if (table.getTableName().endsWith("_OTH")) {
                continue;
            }
            //删除该需求已有记录
            sqlbuffer.append("delete from tablefile_imp_temp where oprseq='").append(oprseq).append("' and sr_id ='").append(srId).append("' and xmlid ='").append(table.getTableId()).append("';\n");
            sqlbuffer.append("delete from column_val_imp_temp where oprseq='").append(oprseq).append("' and sr_id ='").append(srId).append("' and xmlid ='").append(table.getTableId()).append("';\n\n");
            //写入该需求最新记录，为了避免过多开销内存，将 sqlbuffer 当做参数传入。
            addTableToTemp(sqlbuffer, oprseq, srId, table, colNames, fieldsNames);
            sqlbuffer.append("\n");
        }
        return sqlbuffer;
    }

    /**
     * 将一个模型索引及其字段写入到临时表。
     *
     * @param sqlbuffer
     * @param table
     * @param colNames
     * @param fieldsNames
     * @return
     */
    private StringBuilder addTableToTemp(StringBuilder sqlbuffer, long oprseq, String srId, Table table, String[] colNames, String[] fieldsNames) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            String tablecolnam = (Arrays.toString(colNames)).replaceAll("\\[", "").replaceAll("\\]", "");
            String fieldcolnam = (Arrays.toString(fieldsNames)).replaceAll("\\[", "").replaceAll("\\]", "");
            //生成写入 md.tablefile_imp_temp 的语句。
            sqlbuffer.append("insert into  tablefile_imp_temp (").append(tablecolnam).append(") values (")
                    .append("'").append(oprseq).append("',")
                    .append("'").append(table.getTableId()).append("',")
                    .append("'").append(table.getDbServName()).append("',")
                    .append("'").append(table.getTableName()).append("',")
                    .append("'").append(table.getTableNameZh()).append("',")
                    .append("'").append(table.getState()).append("',")
                    .append("'").append(table.getCycleType()).append("',")
                    .append("'").append(table.getTopicCode()).append("',")
                    .append("'").append(table.getExtendcfg()).append("',")
                    .append("'").append(table.getRightlevel()).append("',")
                    .append("'").append(table.getCreater()).append("',")
                    .append("'").append(table.getCurdutyer()).append("',")
                    .append(" TIMESTAMP '").append(sdf1.format(sdf.parse(table.getEffDate()))).append("',")
                    .append(" TIMESTAMP '").append(sdf1.format(sdf.parse(table.getStateDate()))).append("',")
                    .append("'").append(table.getTeamCode()).append("',")
                    .append("'").append(table.getOpenState()).append("',")
                    .append("'").append(table.getTopicName()).append("',")//注意，PAAS平台模型REMARK字段已经被占用，该字段需要填写主题域。
                    .append("'").append(srId).append("',")
                    .append("'").append(table.getRemark()).append("');\n");
            //生成写入 md.column_val_imp_temp 的语句。
            List<TableCol> tableColumns = table.getTablecols();
            if (tableColumns != null) {
                for (TableCol tablecol : tableColumns) {
                    if (tablecol.getDataType().startsWith("DECIMAL")) {
                        tablecol.setDataType("DECIMAL");//长度和精度在后面两列记录
                    }
                    String isIsPrimaryKey = tablecol.isIsPrimaryKey() ? "1" : "";
                    String isIsNullable = tablecol.isIsNullable() ? "Y" : "N";
                    sqlbuffer.append("insert into column_val_imp_temp (").append(fieldcolnam).append(") values (")
                            .append("'").append(oprseq).append("',")
                            .append("'").append(tablecol.getTableId()).append("',")
                            .append("'").append(tablecol.getColumnId()).append("',")
                            .append("'").append(tablecol.getTableName()).append("',")
                            .append("'").append(tablecol.getColumnSeq()).append("',")
                            .append("'").append(tablecol.getColumnName()).append("',")
                            .append("'").append(tablecol.getColumnNameZh()).append("',")
                            .append("'").append(tablecol.getDataType()).append("',")
                            .append("'").append(tablecol.getLength()).append("',")
                            .append("'").append(tablecol.getPrecision()).append("',")
                            .append("'").append(tablecol.getPartitionSeq()).append("',")
                            .append("'").append(isIsPrimaryKey).append("',")
                            .append("'").append(isIsNullable).append("',")
                            .append("'").append(tablecol.getRemark().replaceAll("'", "\"")).append("',")
                            .append("'").append(tablecol.getSecurityType3()).append("',")
                            .append("'").append(tablecol.getSensitivityLevel()).append("',")
                            .append("'").append(tablecol.getDataExample()).append("',")
                            .append("'").append(srId).append("',")
                            .append("'").append(tablecol.getOutSensitivityId()).append("');\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlbuffer;
    }
}
