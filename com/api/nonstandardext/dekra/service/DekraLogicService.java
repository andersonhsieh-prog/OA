package com.api.nonstandardext.dekra.service;

import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

public class DekraLogicService extends BaseBean {

	private final static String SERVICE_NAME = "DekraLogicService ";

	public String getSystemConfigValue(String configKey, RecordSet rs) {
		String configValue = "";
		String corpSecretSql = "select config_value from uf_system_config where config_key = '" + configKey + "'";
		rs.execute(corpSecretSql);
		if (rs.next()) {
			configValue = Util.null2String(rs.getString("config_value"));
		}
		return configValue;
	}
}
