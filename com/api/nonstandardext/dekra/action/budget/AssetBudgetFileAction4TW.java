package com.api.nonstandardext.dekra.action.budget;

import com.api.nonstandardext.dekra.service.DekraLogicService;
import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;
import weaver.workflow.workflow.WorkflowComInfo;
import java.math.BigDecimal;
import java.util.*;

/**
 * 流程歸檔時，更改預算使用狀態
 */
public class AssetBudgetFileAction4TW extends BaseBean implements Action {

	private final static String Action_Name = " AssetBudgetFileAction4TW ";

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

			String result = forzen(mainTablename, requestId, mainId, null);
			if (!"".equals(result)){
				return failureInfo(requestInfo, requestId, result);
			}
		} catch (Exception e) {
			this.writeLog(Action_Name + "  exception :" + e.getMessage());
			return failureInfo(requestInfo, requestId, "異常提示：" + Action_Name +"異常，請聯繫管理員");
		}
		return Action.SUCCESS;
	}

	public String forzen(String mainTablename, String requestId, String mainId, String wfCreateTime){
		String result = doBudgetDt1(requestId, mainTablename, mainId, wfCreateTime);
		if (!"".equals(result)){
			return result;
		}
		return "";
	}

	public String doBudgetDt1(String requestId, String mainTablename, String mainId, String wfCreateTime){
		RecordSet rs = new RecordSet();
		//匯總當前單據預算使用情況 dt1
		String budgetSql = "select * from " + mainTablename + "_dt6 where mainId = '" + mainId + "'";
		this.writeLog(Action_Name + "  budgetSql :" + budgetSql);
		rs.execute(budgetSql);
		Map<String, BigDecimal> currentUseBudgetMap = new HashMap<>();
		List<List> batchParamList = new ArrayList<List>();
		int line = 0;
		String uuid = "";
		while (rs.next()){
			if ("".equals(uuid)){
				uuid = Util.null2String(rs.getString("uuid"));
				if ("".equals(uuid)){
					uuid = UUID.randomUUID().toString();
				}
			}

			String id = Util.null2String(rs.getString("id"));
			List<Object> paramlist = new ArrayList<Object>();
			paramlist.add(uuid);
			paramlist.add(++line);
			paramlist.add(id);
			batchParamList.add(paramlist);

			String ysbm = Util.null2String(rs.getString("ysbm"));
			this.writeLog(Action_Name + "  ysbm :" + ysbm);
			if (!"".equals(ysbm)){
				String ygcgbhsjermb = Util.null2String(rs.getString("ygcgbhsjermb"));
				if ("".equals(ygcgbhsjermb)){
					return "預估採購不含稅金額不能為空";
				}

				this.writeLog(Action_Name + "  ygcgbhsjermb :" + ygcgbhsjermb);

				String bccgbhsjermb = Util.null2String(rs.getString("bccgbhsjermb"));
				if ("".equals(bccgbhsjermb)){
					return "本次採購不含稅金額不能為空";
				}
				this.writeLog(Action_Name + "  line :" + line + "  預估採購金額 :" + ygcgbhsjermb + "  實際採購金額 :" + bccgbhsjermb);
				BigDecimal currentUseBudget = currentUseBudgetMap.get(ysbm);
				if (currentUseBudget == null){
					currentUseBudget = new BigDecimal(bccgbhsjermb);
				} else {
					currentUseBudget = currentUseBudget.add(new BigDecimal(bccgbhsjermb));
				}
				this.writeLog(Action_Name + "   ysbm :" + ysbm + "   currentUseBudget :" + currentUseBudget);
				currentUseBudgetMap.put(ysbm, currentUseBudget);
			} else {
				return "預算編碼不能為空";
			}
		}

		//更新UUID和行號，UUID用於預算操作，行號用於單據關聯
		rs.executeBatchSql("update " + mainTablename + "_dt6 set uuid=?, line=? where id = ?", batchParamList);

		DekraLogicService logicService = new DekraLogicService();
		String modeId = logicService.getSystemConfigValue("Budget_Operation_Modeid", rs);
		if ("".equals(modeId)){
			modeId = "77";
		}

		this.writeLog(Action_Name + "  currentUseBudgetMap.size() :" + currentUseBudgetMap.size() + "  modeId:" + modeId);
		if (currentUseBudgetMap.size() > 0){
			DekraBudgetCalculationService4TW calculation = DekraBudgetCalculationService4TW.getInstance();
			JSONObject result = calculation.frozen(modeId, requestId, uuid, currentUseBudgetMap, true, wfCreateTime);
			if (result.getInt("code") < 0){
				return result.getString("message");
			}
			//賦權
			String ufSql = "select id from uf_TW_budget_operate where op_workflow=" + requestId + " and uuid='" + uuid + "'";
			rs.execute(ufSql);
			while (rs.next()){
				setModeRight(1, Integer.parseInt(modeId), rs.getInt("id"));
			}
		}
		return "";
	}

	public String failureInfo(RequestInfo requestInfo, String requestId, String msg){
		requestInfo.getRequestManager().setMessageid("111" + requestId + "222");
		requestInfo.getRequestManager().setMessagecontent(msg);
		return Action.FAILURE_AND_CONTINUE;
	}

	public void setModeRight(int creater,int modeid,int sourceid){
		ModeRightInfo moderightinfo = new ModeRightInfo();
		moderightinfo.setNewRight(true);
		moderightinfo.editModeDataShare(creater, modeid, sourceid);
	}
}
