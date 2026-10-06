package com.api.nonstandardext.dekra.action.budget;

import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;
import weaver.workflow.workflow.WorkflowComInfo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * 流程退回釋放預算
 */
public class AssetBudgetReleaseAction4TW extends BaseBean implements Action {

	private final static String Action_Name = " AssetBudgetReleaseAction4TW ";

	@Override
	public String execute(RequestInfo requestInfo) {

		WorkflowComInfo workflowComInfo = new WorkflowComInfo();
		String requestId = requestInfo.getRequestid();

		this.writeLog("*************** " + Action_Name + " start , requestId: " + requestId);

		try {
			String workflowId = requestInfo.getWorkflowid();
			int formId = Util.getIntValue(workflowComInfo.getFormId(workflowId), 0);
			if (formId == 0){
				return failureInfo(requestInfo, requestId, "表單不存在");
			}
			String mainTablename = "formtable_main_" + (formId * -1);

			RecordSet rs = new RecordSet();

			String sql = "select * from " + mainTablename + " where requestid = '" + requestId + "'";
			this.writeLog(Action_Name + "  main table sql :" + sql);

			rs.executeQuery(sql);
			String mainId = "";
			if (rs.next()){
				mainId = Util.null2String(rs.getString("id"));
			}
			String result = release(requestId, mainTablename, mainId);
			if (!"".equals(result)){
				return failureInfo(requestInfo, requestId, result);
			}
		} catch (Exception e) {
			this.writeLog(Action_Name + "  exception :" + e.getMessage());
			return failureInfo(requestInfo, requestId, "異常提示：" + Action_Name +"異常，請聯繫管理員");
		}
		return Action.SUCCESS;
	}

	public String release(String requestId, String mainTablename, String mainId){
		String result = doBudgetDt1(requestId, mainTablename, mainId);
		if (!"".equals(result)){
			return result;
		}
		return "";
	}

	public String doBudgetDt1(String requestId, String mainTablename, String mainId){
		RecordSet rs = new RecordSet();
		//匯總當前單據預算使用情況 dt1
		String budgetSql = "select * from " + mainTablename + "_dt6 where mainId = '" + mainId + "'";

		this.writeLog(Action_Name + "  budgetSql :" + budgetSql);
		rs.execute(budgetSql);
		Map<String, BigDecimal> currentUseBudgetMap = new HashMap<>();
		String uuid = "";
		while (rs.next()){
			uuid = Util.null2String(rs.getString("uuid"));
			String ysbm = Util.null2String(rs.getString("ysbm"));
			this.writeLog(Action_Name + "  ysbm :" + ysbm);
			if (!"".equals(ysbm)){
				String ygcgbhsjermb = Util.null2String(rs.getString("ygcgbhsjermb"));
				if ("".equals(ygcgbhsjermb)){
					return "金額不能為空";
				}

				this.writeLog(Action_Name + "  ygcgbhsjermb :" + ygcgbhsjermb);
				BigDecimal currentUseBudget = currentUseBudgetMap.get(ysbm);
				if (currentUseBudget == null){
					currentUseBudget = new BigDecimal(ygcgbhsjermb);
				} else {
					currentUseBudget = currentUseBudget.add(new BigDecimal(ygcgbhsjermb));
				}
				this.writeLog(Action_Name + "   ysbm :" + ysbm + "   currentUseBudget :" + currentUseBudget);
				currentUseBudgetMap.put(ysbm, currentUseBudget);
			} else {
				return "預算編碼不能為空";
			}
		}

		this.writeLog(Action_Name + "  currentUseBudgetMap.size() :" + currentUseBudgetMap.size());
		if (currentUseBudgetMap.size() > 0){
			DekraBudgetCalculationService4TW calculation = DekraBudgetCalculationService4TW.getInstance();
			JSONObject result = calculation.release(requestId, uuid, currentUseBudgetMap, true, null);
			this.writeLog(Action_Name + "  calculation release result :" + result);
			if (result.getInt("code") < 0){
				return result.getString("message");
			}
		}
		return "";
	}

	public String failureInfo(RequestInfo requestInfo, String requestId, String msg){
		requestInfo.getRequestManager().setMessageid("111" + requestId + "222");
		requestInfo.getRequestManager().setMessagecontent(msg);
		return Action.FAILURE_AND_CONTINUE;
	}
}
