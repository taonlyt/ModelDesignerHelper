-------------------------------------------------------
-- 模型名：EDS_每天4G终端销售日表(TW_TERM_SELL_SP_D)
-- 表模式：EDS
-- 分表方式：按月分表
-- 数据库：GDDW
-- 创建日期：2017/5/22
-- 需求编号及说明：SR201804080011001_关于CCOM系统终端数据统计逻辑变更需求
-------------------------------------------------------
ALTER TABLE EDS.TW_TERM_SELL_SP_D_201805 ADD COLUMN TRML_PRDCT_TYP CHAR(2);

COMMENT ON COLUMN EDS.TW_TERM_SELL_SP_D.TRML_PRDCT_TYP IS '终端类型';


