package com.api.nonstandardext.dekra.webservice.server.impl;

import com.api.nonstandardext.dekra.service.DekraLogicService;
import com.api.nonstandardext.dekra.webservice.server.BillStatusService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sun.misc.BASE64Decoder;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;

/**
 * Lams单据状态查询
 */
public class BillStatusServiceImpl extends BaseBean implements BillStatusService {

    private final static String SERVICE_NAME = "OA单据状态查询，";

    @Override
    public String getBillStatusFromOa(String paramsJsonString) {
        //        writeLog(SERVICE_NAME + " paramsJsonString：" + paramsJsonString);
        JSONObject result = new JSONObject();
        result.put("code", "1");
        result.put("message", "操作成功");
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            paramsJsonString = new String(decoder.decodeBuffer(paramsJsonString), "UTF-8");
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
            this.writeLog("---" + SERVICE_NAME + " 参数 billType:" + billType);
            this.writeLog("---" + SERVICE_NAME + " 参数 requestId:" + requestId);

            DekraLogicService logicService = new DekraLogicService();

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

            }  else if ("StandardApply".equals(billType) && !"".equals(requestId)) {// 新增法規標準

                tableName = logicService.getSystemConfigValue("StandardApply_TABLE", rs);

            }  else if ("ExchangeRate".equals(billType) && !"".equals(requestId)) {// ExchangeRate

                tableName = logicService.getSystemConfigValue("ExchangeRate_TABLE", rs);

            }  else if ("FuelAllowance".equals(billType) && !"".equals(requestId)) {// ExchangeRate

                tableName = logicService.getSystemConfigValue("FuelAllowance_TABLE", rs);

            } else {
                result.put("code", "-1");
                result.put("message", "单据类型无效");
                return result.toString();
            }

            String querySql = "select * from " + tableName + " where requestid in(" + requestId + ") ";
            //String querySql = "select * from " + tableName + " where requestid = " + requestId;
            this.writeLog("---" + SERVICE_NAME + " 参数 querySql:" + querySql);
            rs.execute(querySql);

            /*20250915 修改成批量
            String billStatus = "-1";
            String lcbh = "";
            String servicearea = "";
            if (rs.next()){
                billStatus = Util.null2String(rs.getString("billStatus"));
                lcbh = Util.null2String(rs.getString("lcbh"));
                servicearea = Util.null2String(rs.getString("servicearea"));
            }

            this.writeLog("---" + SERVICE_NAME + " 参数 billStatus:" + billStatus);
            JSONObject obj = new JSONObject();
            obj.put("requestId", requestId);
            obj.put("billStatus", billStatus);
            obj.put("lcbh", lcbh);

            if ("Outsourcing".equals(billType)){
                obj.put("servicearea", servicearea);
            }
            result.put("data", obj.toString());
            */
            result.put("data", getQueryArray(rs, billType));

        } catch (Exception e) {
            result.put("code", "-1");
            result.put("message", "操作失败");
            writeLog(SERVICE_NAME + " exception：" + e.getMessage());
            e.printStackTrace();
        }
        writeLog(SERVICE_NAME + " result：" + result.toString());
        return result.toString();
    }

    /**
     * 批量查询，返回数组
     * @param rs
     * @param billType
     * @return
     */
    public JSONArray getQueryArray(RecordSet rs, String billType) {
        JSONArray array = new JSONArray();
        String billStatus = "-1";
        String lcbh = "";
        String servicearea = "";
        String reqid = "";
        while (rs.next()) {
            billStatus = Util.null2String(rs.getString("billStatus"));
            reqid = Util.null2String(rs.getString("requestid"));
            lcbh = Util.null2String(rs.getString("lcbh"));

            JSONObject obj = new JSONObject();
            obj.put("requestId", reqid);
            obj.put("billStatus", billStatus);
            obj.put("lcbh", lcbh);
            if ("Outsourcing".equals(billType)) {
                servicearea = Util.null2String(rs.getString("servicearea"));
                obj.put("servicearea", servicearea);
            }
            array.add(obj);
        }
        return array;
    }

}