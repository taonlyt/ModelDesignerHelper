/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.snal.model.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 模型基本信息
 *
 * @author luotao
 */
public class Table implements Cloneable {

    private int lineId;//模型索引中的行号
    private boolean mainTable;
    private String dataDomain;//数据域：B域，O域，M域
    private String dbType;//数据库类型
    private String dbName;//数据库名
    private String dbServName;//数据库服务名
    private String tableId;//模型唯一标识
    private String tableName;//模型英文名
    private String tableNameZh;//模型中文名
    private String tableModel;//表模式
    private String state;//状态
    private String cycleType;//数据周期
    private String topicCode;//模型所属主题编码
    private String topicName;//模型所属主题名称
    private String tenantUser;//模型所属租户名
    private String location;//模型存储路径
    private String colDelimiter;//列分隔符
    private String storedFormat;//数据文件存储格式
    private String serdeClass;//序列与反序列引用的类
    private String characterSet;//模型文件字符集
    private String[] partitionCols;//分区字段
    private String extendcfg;//模型扩展信息
    private String rightlevel;//敏感级别
    private String creater;//模型创建人
    private String curdutyer;//当前负责人
    private String effDate;//生效日期
    private String stateDate;//状态日期
    private String teamCode;//项目/团队编号
    private String openState;//开放状态
    private String remark;//备注
    private boolean isVIP218;//是否218重点模型
    private boolean shared;//是否地市共享模型
    private boolean constantParam;//是否固定参数
    private boolean hasJobConf;//是否有调度
    private boolean interfaceTable;//是否接口模型
    private boolean perlTable;//是否perl模型
    private boolean shareAllDataToCity;//地市模型是否共享整表数据
    private String tableExplain;//模型描述字段
    
    private String upgradeNotice;//升级注意事项

    public String getUpgradeNotice() {
        return upgradeNotice;
    }

    public void setUpgradeNotice(String upgradeNotice) {
        this.upgradeNotice = upgradeNotice;
    }
    
    

    public String getTableExplain() {
        return tableExplain;
    }

    public void setTableExplain(String tableExplain) {
        this.tableExplain = tableExplain;
    }
    private List<TableCol> tablecols;//模型字段集

    /**
     * 是否分区模型
     *
     * @return
     */
    public boolean isPartitionTable() {
        boolean isPrtTable = false;
        if (!isMainTable()) {
            if (Arrays.asList(partitionCols).contains("day")
                    || Arrays.asList(partitionCols).contains("month")) {
                isPrtTable = true;//包含有时间分区字段的地市模型才是分区模型
            }
        } else {
            isPrtTable = true;//主模型都是分区模型
        }
        return isPrtTable;
    }

    public TableCol getTableCol(String colName) {
        TableCol retCol = null;
        if (tablecols != null && !tablecols.isEmpty()) {
            for (TableCol tablecol : tablecols) {
                if (tablecol.getColumnName().equalsIgnoreCase(colName)) {
                    retCol = tablecol;
                }
            }
        }
        return retCol;
    }

    public int getLineId() {
        return lineId;
    }

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public void setTablecols(List<TableCol> tablecols) {
        this.tablecols = tablecols;
    }

    public boolean isMainTable() {
        return mainTable;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public void setMainTable(boolean mainTable) {
        this.mainTable = mainTable;
    }

    public String getStoredFormat() {
        return storedFormat;
    }

    public void setStoredFormat(String storedFormat) {
        this.storedFormat = storedFormat;
    }

    public String getSerdeClass() {
        return serdeClass;
    }

    public void setSerdeClass(String serdeClass) {
        this.serdeClass = serdeClass;
    }

    @Override
    public Table clone() {
        Table table = null;
        try {
            table = (Table) super.clone();
            List<TableCol> tmplist = new ArrayList();
            if (table.getTablecols() == null) {
                System.out.println(table.getTableName() + " has no table columns !");
            } else {
                for (TableCol tablecol : table.getTablecols()) {
                    tmplist.add((TableCol) tablecol.clone());
                }
                table.setTablecols(tmplist);
            }
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
        }
        return table;
    }

    public List<TableCol> getTablecols() {
        return tablecols;
    }

    public void addTableCol(TableCol tablecol) {
        if (this.tablecols == null) {
            this.tablecols = new ArrayList();
        }
        tablecols.add(tablecol);
    }

    public String getDataDomain() {
        return dataDomain;
    }

    public void setDataDomain(String dataDomain) {
        this.dataDomain = dataDomain;
    }

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbServName() {
        return dbServName;
    }

    public void setDbServName(String dbServName) {
        this.dbServName = dbServName;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableNameZh() {
        return tableNameZh;
    }

    public void setTableNameZh(String tableNameZh) {
        this.tableNameZh = tableNameZh;
    }

    public String getTableModel() {
        return tableModel;
    }

    public void setTableModel(String tableModel) {
        this.tableModel = tableModel;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public String getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(String topicCode) {
        this.topicCode = topicCode;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getTenantUser() {
        return tenantUser;
    }

    public void setTenantUser(String tenantUser) {
        this.tenantUser = tenantUser;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getColDelimiter() {
        return colDelimiter;
    }

    public void setColDelimiter(String colDelimiter) {
        if (colDelimiter.equals("€")) {
            colDelimiter = "\\-128";
        }
        this.colDelimiter = colDelimiter;
    }

    public String[] getPartitionCols() {
        return partitionCols;
    }

    public String getPartitionColsStr() {
        String retstr = "";
        for (String a : partitionCols) {
            retstr += a + ",";
        }
        if (retstr != null) {
            retstr = retstr.substring(0, retstr.length() - 1);
        }
        return retstr;
    }

    public String getMonitorThreshold() {
        return interfaceTable ? "0.001" : "0.1";
    }

    public void setPartitionCols(String partitionCols) {
        this.partitionCols = partitionCols.split(",");
    }

    public void setPartitionCols(String[] partitionCols) {
        this.partitionCols = partitionCols;
    }

    public String getExtendcfg() {
        return extendcfg;
    }

    public void setExtendcfg(String extendcfg) {
        this.extendcfg = extendcfg;
    }

    public String getRightlevel() {
        return rightlevel;
    }

    public void setRightlevel(String rightlevel) {
        this.rightlevel = rightlevel;
    }

    public String getCreater() {
        return creater;
    }

    public void setCreater(String creater) {
        this.creater = creater;
    }

    public String getCurdutyer() {
        return curdutyer;
    }

    public void setCurdutyer(String curdutyer) {
        this.curdutyer = curdutyer;
    }

    public String getEffDate() {
        return effDate;
    }

    public void setEffDate(String effDate) {
        this.effDate = effDate;
    }

    public String getStateDate() {
        return stateDate;
    }

    public void setStateDate(String stateDate) {
        this.stateDate = stateDate;
    }

    public String getTeamCode() {
        return teamCode;
    }

    public void setTeamCode(String teamCode) {
        this.teamCode = teamCode;
    }

    public String getOpenState() {
        return openState;
    }

    public void setOpenState(String openState) {
        this.openState = openState;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isIsVIP218() {
        return isVIP218;
    }

    public void setIsVIP218(String isVIP218) {
        this.isVIP218 = "是".equals(isVIP218);
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(String shared) {
        this.shared = "是".equals(shared);
    }

    public boolean isConstantParam() {
        return constantParam;
    }

    public void setConstantParam(String constantParam) {
        this.constantParam = "是".equals(constantParam);
    }

    public boolean isHasJobConf() {
        return hasJobConf;
    }

    public void setHasJobConf(String hasJobConf) {
        this.hasJobConf = "是".equals(hasJobConf);
    }

    public boolean isInterfaceTable() {
        return interfaceTable;
    }

    public void setInterfaceTable(String isInterfaceTable) {

        this.interfaceTable = "是".equals(isInterfaceTable);
    }

    public boolean isPerlTable() {
        return perlTable;
    }

    public void setPerlTable(String isPerlTable) {
        this.perlTable = "是".equals(isPerlTable);
    }

    public boolean isShareAllDataToCity() {
        return shareAllDataToCity;
    }

    public void setShareAllDataToCity(String isShareAllDataToCity) {
        this.shareAllDataToCity = "是".equals(isShareAllDataToCity);
    }

    public String getPrimaryKeys() {
        String keystr = "";
        for (TableCol col : tablecols) {
            if (col.isIsPrimaryKey()) {
                keystr += col.getColumnName() + ",";
            }
        }
        if (keystr.trim().length() > 0) {
            keystr = keystr.substring(0, keystr.length() - 1);
        }
        return keystr;
    }

    @Override
    public String toString() {
        return "Table{" + "dataDomain=" + dataDomain + ", dbType=" + dbType + ", dbName=" + dbName + ", dbServName=" + dbServName + ", tableId=" + tableId + ", tableName=" + tableName + ", tableNameZh=" + tableNameZh + ", tableModel=" + tableModel + ", state=" + state + ", cycleType=" + cycleType + ", topicCode=" + topicCode + ", topicName=" + topicName + ", tenantUser=" + tenantUser + ", location=" + location + ", colDelimiter=" + colDelimiter + ", partitionCols=" + Arrays.toString(partitionCols) + ", extendcfg=" + extendcfg + ", rightlevel=" + rightlevel + ", creater=" + creater + ", curdutyer=" + curdutyer + ", effDate=" + effDate + ", stateDate=" + stateDate + ", teamCode=" + teamCode + ", openState=" + openState + ", remark=" + remark + ", isVIP218=" + isVIP218 + ", shared=" + shared + ", constantParam=" + constantParam + ", hasJobConf=" + hasJobConf + ", tablecols=" + tablecols + '}';
    }

}
