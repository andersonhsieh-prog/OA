package com.api.nonstandardext.dekra.webservice.server.impl;
import com.api.nonstandardext.dekra.service.DekraLogicService;
import com.api.nonstandardext.dekra.webservice.server.BillForceOverService;
import net.sf.json.JSONObject;
import sun.misc.BASE64Decoder;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.workflow.webservices.WorkflowService;
import weaver.workflow.webservices.WorkflowServiceImpl;
import java.util.Base64;
/**
 * Lams单据强制归档
 */
public class BillForceOverServiceImpl extends BaseBean implements BillForceOverService {
    private final static String SERVICE_NAME = " 强制归档，";
    @Override
    public String billForceOverService(String paramsJsonString) {
        JSONObject result = new JSONObject();
        result.put("code", "1");
        result.put("message", "操作成功");
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            paramsJsonString = new String(decoder.decodeBuffer(paramsJsonString), "UTF-8");
            //Base64.Decoder decoder=Base64.getDecoder();
            //paramsJsonString = new String(decoder.decode(paramsJsonString), "UTF-8");
            writeLog(SERVICE_NAME + " paramsJsonString：" + paramsJsonString);
            JSONObject jsonObject = JSONObject.fromObject(paramsJsonString);
            String secret = Util.null2String(jsonObject.get("secret"));
            if ("".equals(secret) || !"170f3e1cd12a4a5081ad2a551cb28494".equals(secret)) {
                result.put("code", "-1");
                result.put("message", "参数secret无效");
                return result.toString();
            }
            RecordSet rs = new RecordSet();
            String billType = Util.null2String(jsonObject.get("billType"));
            String requestId = Util.null2String(jsonObject.get("requestId"));
            String operator = Util.null2String(jsonObject.get("operatorCode"));
            this.writeLog("---" + SERVICE_NAME + " 参数 billType:" + billType);
            this.writeLog("---" + SERVICE_NAME + " 参数 requestId:" + requestId);
            this.writeLog("---" + SERVICE_NAME + " 参数 operator1:" + operator);
            if (("Quotation".equals(billType) || "ContractForm".equals(billType) || "ServiceRecord".equals(billType) || "Outsourcing".equals(billType) || "OutsourcingPayment".equals(billType)) && !"".equals(requestId)) {} else {
                result.put("code", "-1");
                result.put("message", "单据类型或者参数无效");
                return result.toString();
            }
            if (!"".equals(operator)) {
                rs.execute("select id from HrmResource where workcode='" + operator + "'");
                if (rs.next()) {
                    operator = Util.null2String(rs.getString("id"));
                }
            }
            if ("".equals(operator)) {
                String sql = "select userid,isremark,nodeid from workflow_currentoperator where nodeid =(select currentnodeid from workflow_requestbase where requestid=" + requestId + ") and isremark='0' and requestid= " + requestId;
                rs.executeQuery(sql);
                if (rs.next()) {
                    operator = Util.null2String(rs.getString("userid"));
                }
            }
            DekraLogicService logicService = new DekraLogicService();
            this.writeLog("---" + SERVICE_NAME + " 参数 operator:" + operator);
            this.writeLog("---" + SERVICE_NAME + " 参数 requestId:" + requestId);
            WorkflowService workflowService = new WorkflowServiceImpl();
            String res = workflowService.doForceOver(Util.getIntValue(requestId), Util.getIntValue(operator));
            this.writeLog("---" + SERVICE_NAME + " 归档结果 res:" + res);
            if ("error".equals(res)) {
                result.put("code", "-1");
                result.put("message", "操作失败");
                return result.toString();
            }
            result.put("message", res);
            String tableName = "";
            if ("Quotation".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("Quotation_TABLE", rs);
            } else if ("ContractForm".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("ContractForm_TABLE", rs);
            } else if ("ServiceRecord".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("ServiceRecord_TABLE", rs);
            } else if ("Outsourcing".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("Outsourcing_TABLE", rs);
            } else if ("OutsourcingPayment".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("Outsourcing_Payment_TABLE", rs);
            } else if ("CalibrateMaintainApply".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("RepairAppForm_TABLE", rs);
            } else if ("CalibrateMaintainPayment".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("CalibrateMaintainPayment_TABLE", rs);
            } else if ("StandardApply".equals(billType) && !"".equals(requestId)) {
                tableName = logicService.getSystemConfigValue("StandardApply_TABLE", rs);
            }
            if ("".equals(tableName)) {
                result.put("code", "-1");
                result.put("message", "流程表名获取失败");
                return result.toString();
            }
            String updateSql = "update " + tableName + " set billStatus=4 where requestId in(" + requestId + ")";
            this.writeLog("---" + SERVICE_NAME + "  updateSql:" + updateSql);
            rs.executeUpdate(updateSql);
        } catch (Exception e) {
            result.put("code", "-1");
            result.put("message", "操作失败");
            writeLog(SERVICE_NAME + " exception : " + e.getMessage());
            e.printStackTrace();
        } finally {
            writeLog(SERVICE_NAME + " end result : " + result.toString());
        }
        return result.toString();
    }
}