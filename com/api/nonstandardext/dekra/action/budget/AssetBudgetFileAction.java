package com.api.nonstandardext.dekra.action.budget;

import com.api.nonstandardext.dekra.service.DekraBudgetCalculationService;
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
 * 流程归档时，更改预算使用状态
 */
public class AssetBudgetFileAction extends BaseBean implements Action {

	private final static String Action_Name = " AssetBudgetFileAction ";

	@Override
	public String execute(RequestInfo requestInfo) {

		WorkflowComInfo workflowComInfo = new WorkflowComInfo();
		String requestId = requestInfo.getRequestid();

		this.writeLog("*************** " + Action_Name + " start , requestId: " + requestId);

		try {
			String workflowId = requestInfo.getWorkflowid();
			int formId = Util.getIntValue(workflowComInfo.getFormId(workflowId), 0);
			if (formId == 0){
				return failureInfo(requestInfo, requestId, "表单不存在");
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
			return failureInfo(requestInfo, requestId, "异常提示：" + Action_Name +"异常，请联系管理员");
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
		//汇总当前单据预算使用情况 dt1
		String budgetSql = "select * from " + mainTablename + "_dt1 where mainId = '" + mainId + "'";
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
					return "预估采购不含税金额不能为空";
				}

				this.writeLog(Action_Name + "  ygcgbhsjermb :" + ygcgbhsjermb);

				String bccgbhsjermb = Util.null2String(rs.getString("bccgbhsjermb"));
				if ("".equals(bccgbhsjermb)){
					return "本次采购不含税金额不能为空";
				}
				this.writeLog(Action_Name + "  line :" + line + "  预估采购金额 :" + ygcgbhsjermb + "  实际采购金额 :" + bccgbhsjermb);
				BigDecimal currentUseBudget = currentUseBudgetMap.get(ysbm);
				if (currentUseBudget == null){
					currentUseBudget = new BigDecimal(bccgbhsjermb);
				} else {
					currentUseBudget = currentUseBudget.add(new BigDecimal(bccgbhsjermb));
				}
				this.writeLog(Action_Name + "   ysbm :" + ysbm + "   currentUseBudget :" + currentUseBudget);
				currentUseBudgetMap.put(ysbm, currentUseBudget);
			} else {
				return "预算编码不能为空";
			}
		}

		//更新UUID和行号，UUID用于预算操作，行号用于单据关联
		rs.executeBatchSql("update " + mainTablename + "_dt1 set uuid=?, line=? where id = ?", batchParamList);

		DekraLogicService logicService = new DekraLogicService();
		String modeId = logicService.getSystemConfigValue("Budget_Operation_Modeid", rs);
		if ("".equals(modeId)){
			modeId = "77";
		}

		this.writeLog(Action_Name + "  currentUseBudgetMap.size() :" + currentUseBudgetMap.size() + "  modeId:" + modeId);
		if (currentUseBudgetMap.size() > 0){
			DekraBudgetCalculationService calculation = DekraBudgetCalculationService.getInstance();
			JSONObject result = calculation.frozen(modeId, requestId, uuid, currentUseBudgetMap, true, wfCreateTime);
			if (result.getInt("code") < 0){
				return result.getString("message");
			}
			//赋权
			String ufSql = "select id from uf_budget_operat where op_workflow=" + requestId + " and uuid='" + uuid + "'";
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
