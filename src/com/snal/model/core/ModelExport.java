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
public class ModelExport {

    public static String export(StringBuilder sqlbuffer,Table table) {
        String[] headnames1 = {"xmlid", "dbname", "dataname", "datacnname", "state", "cycletype", "topiccode", "extend_cfg", "rightlevel", "creater",
            "curdutyer", "eff_date", "state_date", "team_code", "open_state", "remark"};
        String[] headnames2 = {"xmlid", "col_xmlid", "dataname", "col_seq", "colname", "colcnname", "datatype", "length",
            "precision_val", "party_seq", "isprimarykey", "isnullable", "remark", "filed_type_child", "sensitive_level", "policyid"};
        try {
            String tablecolnam = (Arrays.toString(headnames1)).replaceAll("\\[", "").replaceAll("\\]", "");
            String fieldcolnam = (Arrays.toString(headnames2)).replaceAll("\\[", "").replaceAll("\\]", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            
            sqlbuffer.append("\n");
            String insertsql1 = "insert into tablefile (" + tablecolnam + ") values ("
                    + "'" + table.getTableId() + "',"
                    + "'" + table.getDbServName() + "',"
                    + "'" + table.getTableName() + "',"
                    + "'" + table.getTableNameZh() + "',"
                    + "'" + table.getState() + "',"
                    + "'" + table.getCycleType() + "',"
                    + "'" + table.getTopicCode() + "',"
                    + "'" + table.getExtendcfg() + "',"
                    + "'" + table.getRightlevel() + "',"
                    + "'" + table.getCreater() + "',"
                    + "'" + table.getCurdutyer() + "',"
                    + " TIMESTAMP '" + sdf1.format(sdf.parse(table.getEffDate())) + "',"
                    + " TIMESTAMP '" + sdf1.format(sdf.parse(table.getStateDate())) + "',"
                    + "'" + table.getTeamCode() + "',"
                    + "'" + table.getOpenState() + "',"
                    + "'" + table.getTopicName() + "');";//注意，PAAS平台模型REMARK字段已经被占用，该字段需要填写主题域。
            sqlbuffer.append(insertsql1).append("\n");
            List<TableCol> tablecols = table.getTablecols();
            if (tablecols != null) {
                for (TableCol tablecol : tablecols) {
                    if (tablecol.getDataType().startsWith("DECIMAL")) {
                        tablecol.setDataType("DECIMAL");//长度和精度在后面两列记录
                    }
                    String isIsPrimaryKey = tablecol.isIsPrimaryKey() ? "1" : "";
                    String isIsNullable = tablecol.isIsNullable() ? "Y" : "N";
                    String insertsql2 = "insert into column_val (" + fieldcolnam + ") values ("
                            + "'" + tablecol.getTableId() + "',"
                            + "'" + tablecol.getColumnId() + "',"
                            + "'" + tablecol.getTableName() + "',"
                            + "'" + tablecol.getColumnSeq() + "',"
                            + "'" + tablecol.getColumnName() + "',"
                            + "'" + tablecol.getColumnNameZh() + "',"
                            + "'" + tablecol.getDataType() + "',"
                            + "'" + tablecol.getLength() + "',"
                            + "'" + tablecol.getPrecision() + "',"
                            + "'" + tablecol.getPartitionSeq() + "',"
                            + "'" + isIsPrimaryKey + "',"
                            + "'" + isIsNullable + "',"
                            + "'" + tablecol.getRemark().replaceAll("'", "\"") + "',"
                            + "'" + tablecol.getSecurityType3() + "',"
                            + "'" + tablecol.getSensitivityLevel() + "',"
                            + "'" + tablecol.getOutSensitivityId() + "');";
                    sqlbuffer.append(insertsql2).append("\n");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlbuffer.toString();
    }
}
