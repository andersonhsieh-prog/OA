package com.api.nonstandardext.dekra.service;

import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Dekra_Interface_Service extends BaseBean {

	public List<Map<String, String>> getInterfaceConfigList(String gxbs, String o_fieldtype) throws Exception{
		List<Map<String, String>> cusList = new ArrayList<Map<String, String>>();
		RecordSet rs1 = new RecordSet();
		String mainSql = "select * from uf_bookexport where gxbs = '" + gxbs + "'";
		rs1.execute(mainSql);
		if (rs1.next()){
			String sql = "select * from uf_bookexport_dt1 where mainId = '" + Util.null2String(rs1.getString("id")) + "' and o_fieldtype=" +o_fieldtype;
			if(rs1.executeQuery(sql)){
				while(rs1.next()){
					String oa_fieldid = Util.null2String(rs1.getString("o_fieldname"));//OA字段
					String interface_field = Util.null2String(rs1.getString("interface_field"));//接口字段
					String convert_type = Util.null2String(rs1.getString("changerule"));//转换类型
					String convert_value = Util.null2String(rs1.getString("cussql")).replaceAll("<br>", " ").replaceAll("&nbsp;", " ");//转换值

					if("".equals(interface_field)){
						continue;
					}

					String oa_field = getOaFiledName(oa_fieldid);
					Map<String, String> cusmap = new HashMap<String, String>();
					cusmap.put("oa_field", oa_field);
					cusmap.put("interface_field", interface_field);
					cusmap.put("convert_type", convert_type);
					cusmap.put("convert_value", convert_value);
					cusList.add(cusmap);
				}
			}
		}
		return cusList;
	}

	public JSONObject getDataByJson(List<Map<String, String>> cusList, String requestid,String currUser,String currDateStr) throws Exception{
		RecordSet rs = new RecordSet();
		JSONObject json = new JSONObject();
		for(Map<String, String> cusMap : cusList){
			String oa_field = cusMap.get("oa_field");//OA字段
			String interface_field = cusMap.get("interface_field");//接口字段
			String convert_type = cusMap.get("convert_type");//转换类型
			String convert_value = cusMap.get("convert_value");//转换值
			String oa_timefield = cusMap.get("oa_timefield");//时间字段

			String values = "";
			if("".equals(convert_type) || "0".equals(convert_type)){//不转换
				values = Util.null2String(rs.getString(oa_field));
			}else if("1".equals(convert_type)){//固定值

				values = convert_value;
			}else if("2".equals(convert_type)){//sql转换

				values = Util.null2String(rs.getString(oa_field));
				values = "'" + values + "'";
				values = getValueByChangeRule(convert_value, values, requestid);
			}else if("3".equals(convert_type)){//流程ID

				values = requestid;
			}else if("4".equals(convert_type)){//日期+时间

				values = Util.null2String(rs.getString(oa_field)) + " " + Util.null2String(rs.getString(oa_timefield));
			}else if("5".equals(convert_type)){//日期格式

				values = Util.null2String(rs.getString(oa_field));
				values = cusDateRule(values, convert_value);
			}else if("6".equals(convert_type)){//当前操作者

				values = currUser;
			}else if("7".equals(convert_type)){//当前时间

				values = currDateStr;
			}
			json.put(interface_field, values);
		}
		return json;
	}

	public Map<String, String> getDataByMap(List<Map<String, String>> cusList, RecordSet rs, String requestid,String currUser,String currDateStr) throws Exception{
		Map<String, String> dataMap = new HashMap();
		for(Map<String, String> cusMap : cusList){
			String oa_field = cusMap.get("oa_field");//OA字段
			String interface_field = cusMap.get("interface_field");//接口字段
			String convert_type = cusMap.get("convert_type");//转换类型
			String convert_value = cusMap.get("convert_value");//转换值
			String oa_timefield = cusMap.get("oa_timefield");//时间字段

			String values = "";
			if("".equals(convert_type) || "0".equals(convert_type)){//不转换
				values = Util.null2String(rs.getString(oa_field));
			}else if("1".equals(convert_type)){//固定值

				values = convert_value;
			}else if("2".equals(convert_type)){//sql转换

				values = Util.null2String(rs.getString(oa_field));
				values = "'" + values + "'";
				values = getValueByChangeRule(convert_value, values, requestid);
			}else if("3".equals(convert_type)){//流程ID

				values = requestid;
			}else if("4".equals(convert_type)){//日期+时间

				values = Util.null2String(rs.getString(oa_field)) + " " + Util.null2String(rs.getString(oa_timefield));
			}else if("5".equals(convert_type)){//日期格式

				values = Util.null2String(rs.getString(oa_field));
				values = cusDateRule(values, convert_value);
			}else if("6".equals(convert_type)){//当前操作者

				values = currUser;
			}else if("7".equals(convert_type)){//当前时间

				values = currDateStr;
			}
			this.writeLog("  interface_field:" + interface_field + ", values:" + values + ",convert_type:" + convert_type + ",convert_value:" + convert_value);
			dataMap.put(interface_field, values);
		}
		return dataMap;
	}

	public String getWorkcode(int userid){
		String lastname = "";
		RecordSet rs = new RecordSet();
		if(userid == 1){
			lastname = "sysadmin";
		}else{
			if(rs.executeQuery("select * from hrmresource where id = " + userid)){
				rs.next();
				lastname = Util.null2String(rs.getString("workcode"));
			}
		}
		return lastname;
	}

	public static String cusDateRule(String datastr, String pattern) throws Exception{

		if("".equals(datastr) || "".equals(pattern)){
			return datastr;
		}
		if(!"".equals(datastr)) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			Date date = null;
			try {
				date = format.parse(datastr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			SimpleDateFormat sdf = new SimpleDateFormat(pattern);
			datastr = sdf.format(date);
		}

		return datastr;
	}

	/**
	 * 用数据库值，根据规则转换，获取其最终结果
	 * @param cus_sql 自定义转换的SQL
	 * @param value 参数值
	 * @param requestid 流程请求ID
	 * @return
	 */
	public static String getValueByChangeRule(String cus_sql,String value,String requestid){
		String endValue = "";

		cus_sql = cus_sql.replace("&nbsp;", " ");

		//参数进行替换
		String sqlString = cus_sql.replace("{?requestid}", requestid);

		sqlString = sqlString.replace("?", value);

		RecordSet rs = new RecordSet();

		if(rs.executeQuery(sqlString)){
			rs.next();

			endValue = Util.null2String(rs.getString(1));
		}

		return endValue;
	}

	public static String getOaFiledName(String oa_fieldid){

		String FiledName = "";

		if(!"".equals(oa_fieldid)){

			RecordSet rs = new RecordSet();

			String select_sql = "select fieldname from Workflow_Field_View where fieldid = " + oa_fieldid;
			if(rs.executeQuery(select_sql)){
				rs.next();

				FiledName = Util.null2String(rs.getString(1));
			}
		}
		return FiledName;

	}
}
