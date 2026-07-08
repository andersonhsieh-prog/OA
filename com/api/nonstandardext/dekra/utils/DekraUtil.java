package com.api.nonstandardext.dekra.utils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import weaver.general.BaseBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class DekraUtil {

	public static BaseBean baseBean = new BaseBean();

	public final static String Quotation_TABLE = "formtable_main_157";
	public final static Integer Quotation_WF_ID = 149;

	public final static Integer ContractForm_WF_ID = 160;
	public final static String ContractForm_TABLE = "formtable_main_170";

	public final static Integer ServiceRecord_WF_ID = 159;
	public final static String ServiceRecord_TABLE = "formtable_main_169";

	public final static Integer Outsourcing_WF_ID = 242;
	public final static String Outsourcing_TABLE_V1 = "formtable_main_197";
	public final static String Outsourcing_TABLE_V2 = "formtable_main_197";

	public final static Integer Outsourcing_Payment_WF_ID = 250;
	public final static String Outsourcing_Payment_TABLE = "formtable_main_213";

	public final static SimpleDateFormat formatYYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public final static SimpleDateFormat formatYYYYMMDD = new SimpleDateFormat("yyyy-MM-dd");
	public final static SimpleDateFormat formatYYYYMMDD_SAP = new SimpleDateFormat("yyyyMMdd");
	public final static SimpleDateFormat formatYYYYMMDDHHMMSS_SAP = new SimpleDateFormat("yyyyMMddHHmmss");
	public final static SimpleDateFormat formatYYYYMM = new SimpleDateFormat("yyyy-MM");


	private static String CookieVal = null;

	public static Date parseToDate(String dateString, SimpleDateFormat formatInfo){
		try {
			return formatInfo.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String parseToDateString(Date date, SimpleDateFormat formatInfo){
		if (date == null){
			return "";
		}
		return formatInfo.format(date);
	}

	public static boolean isJsonArray(String content) {
		try {
			JSONArray.fromObject(content);;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isJsonObject(String content) {
		try {
			JSONObject.fromObject(content);;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String httpGet(String baseUrl){
		String url = baseUrl;
		try {
			URL u = new URL(url);
			HttpURLConnection huconn = (HttpURLConnection) u.openConnection();
			BufferedReader in = null;
			StringBuilder result = new StringBuilder();
			huconn.connect();
			in = new BufferedReader(new InputStreamReader(huconn.getInputStream(), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line);
			}
			if (in != null) {
				in.close();
			}
			// 打印返回结果
			return result.toString();
		} catch (Exception e) {
			baseBean.writeLog("错误信息");
			baseBean.writeLog(e.getMessage());
			return "";
		}
	}

	public static String httpPost(String httpUrl, Map<String, String> headParams, String bodyParams){
		String result = "";

		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(httpUrl);
		Set<String> keySet = headParams.keySet();
		for (String key : keySet) {
			postMethod.setRequestHeader(key, headParams.get(key));
		}
		postMethod.setRequestBody(bodyParams);
		postMethod.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
		postMethod.setRequestHeader("Cookie",CookieVal);
		try {
			int httpStatus = httpClient.executeMethod(postMethod);
			byte[] resultByte = postMethod.getResponseBody();
			result = new String(resultByte, "UTF-8");

		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static String getCurrencyShortName(String currencyId){
		if ("1".equals(currencyId)){
			return "CNY";
		} else if ("22".equals(currencyId)){
			return "HKD";
		} else if ("23".equals(currencyId)){
			return "USD";
		} else if ("24".equals(currencyId)){
			return "EUR";
		} else if ("25".equals(currencyId)){
			return "JPY";
		} else if ("30".equals(currencyId)){
			return "GBP";
		}
		return currencyId;
	}
}