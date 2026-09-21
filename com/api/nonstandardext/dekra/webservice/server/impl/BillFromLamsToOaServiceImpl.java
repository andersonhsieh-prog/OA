package com.api.nonstandardext.dekra.webservice.server.impl;
import com.api.nonstandardext.dekra.service.DekraCreateDocumentService;
import com.api.nonstandardext.dekra.service.DekraLogicService;
import com.api.nonstandardext.dekra.service.Dekra_Interface_Service;
import com.api.nonstandardext.dekra.utils.DekraUtil;
import com.api.nonstandardext.dekra.webservice.server.BillFromLamsToOaService;
import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sun.misc.BASE64Decoder;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.webservices.*;
/**
 * Lams单据接收接口：报价单
 */
public class BillFromLamsToOaServiceImpl extends BaseBean implements BillFromLamsToOaService {
    private final static String SERVICE_NAME = "BillFromLamsToOaServiceImpl V5 : ";
    @Override
    public String syncBillFromLamsToOa(String paramsJsonString) {
        writeLog(SERVICE_NAME + " paramsJsonString：" + paramsJsonString);
        JSONObject result = new JSONObject();
        result.put("code", "1");
        result.put("message", "操作成功");
        try {
            BASE64Decoder decoder = new BASE64Decoder();
            paramsJsonString = new String(decoder.decodeBuffer(paramsJsonString), "UTF-8");
            //java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
            //paramsJsonString = new String(decoder.decode(paramsJsonString), StandardCharsets.UTF_8);
            writeLog(SERVICE_NAME + " paramsJsonString：" + paramsJsonString);
            JSONObject jsonObject = JSONObject.fromObject(paramsJsonString);
            String secret = Util.null2String(jsonObject.get("secret"));
            if ("".equals(secret) || !"170f3e1cd12a4a5081ad2a551cb28494".equals(secret)) {
                result.put("code", "-1");
                result.put("message", "参数secret无效");
                return result.toString();
            }
            RecordSet rs = new RecordSet();
            DekraLogicService logicService = new DekraLogicService();
            String bill_type = Util.null2String(jsonObject.get("billType"));
            JSONObject dataJson = JSONObject.fromObject(Util.null2String(jsonObject.get("data")));
            writeLog(SERVICE_NAME + " bill_type：" + bill_type);
            if ("Quotation".equals(bill_type)) {
                String workflowId = logicService.getSystemConfigValue("Quotation_WF_ID", rs);
                result = createWfForQuotation(workflowId, "Quotation", dataJson);
            } else if ("ContractForm".equals(bill_type)) {
                String workflowId = logicService.getSystemConfigValue("ContractForm_WF_ID", rs);
                result = createWfForContractForm(workflowId, "ContractForm", dataJson);
            } else if ("ServiceRecord".equals(bill_type)) {
                String workflowId = logicService.getSystemConfigValue("ServiceRecord_WF_ID", rs);
                result = createWfForServiceRecord(workflowId, "ServiceRecord", dataJson);
            } else if ("Outsourcing".equals(bill_type)) {
                String workflowId = logicService.getSystemConfigValue("Outsourcing_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("Outsourcing_TABLE", rs);
                result = createWfForOutsourcing(workflowId, tableName, "Outsourcing", dataJson);
            } else if ("OutsourcingPayment".equals(bill_type)) {
                String workflowId = logicService.getSystemConfigValue("Outsourcing_Payment_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("Outsourcing_Payment_TABLE", rs);
                result = createWfForOutsourcingPayment(workflowId, "Outsourcing_Payment", tableName, dataJson, logicService);
            } else if ("CalibrateMaintainApply".equals(bill_type)) { //维修申请单流程
                String workflowId = logicService.getSystemConfigValue("RepairAppForm_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("RepairAppForm_TABLE", rs);
                RepairAppFormUtil rafuApply = new RepairAppFormUtil();
                result = rafuApply.createWf(bill_type, workflowId, "CalibrateMaintainApply", tableName, dataJson, logicService);
            } else if ("CalibrateMaintainPayment".equals(bill_type)) { //维修付款申请单流程
                String workflowId = logicService.getSystemConfigValue("CalibrateMaintainPayment_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("CalibrateMaintainPayment_TABLE", rs);
                RepairAppFormUtil rafuPayment = new RepairAppFormUtil();
                result = rafuPayment.createWf(bill_type, workflowId, "CalibrateMaintainPayment", tableName, dataJson, logicService);
            } else if ("StandardApply".equals(bill_type)) { // 新增法規標準
                String workflowId = logicService.getSystemConfigValue("StandardApply_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("StandardApply_TABLE", rs);
                RepairAppFormUtil rafuStandardNewApply = new RepairAppFormUtil();
                result = rafuStandardNewApply.createWf(bill_type, workflowId, "StandardApply", tableName, dataJson, logicService);
            } else if ("ExchangeRate".equals(bill_type)) { // ExchangeRate
                String workflowId = logicService.getSystemConfigValue("ExchangeRate_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("ExchangeRate_TABLE", rs);
                RepairAppFormUtil rafuExchangeRate = new RepairAppFormUtil();
                result = rafuExchangeRate.createWf(bill_type, workflowId, "ExchangeRate", tableName, dataJson, logicService);
            } else if ("FuelAllowance".equals(bill_type)) { // FuelAllowance
                String workflowId = logicService.getSystemConfigValue("FuelAllowance_WF_ID", rs);
                String tableName = logicService.getSystemConfigValue("FuelAllowance_TABLE", rs);
                RepairAppFormUtil rafuFuelAllowance = new RepairAppFormUtil();
                result = rafuFuelAllowance.createWf(bill_type, workflowId, "FuelAllowance", tableName, dataJson, logicService);
            } else {
                result.put("code", "-1");
                result.put("message", "单据类型有误:"+bill_type);
            }
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
     * 报价单
     *
     * @param workflowId
     * @param gxbs
     * @param dataJson
     * @return
     * @throws Exception
     */
    private JSONObject createWfForQuotation(String workflowId, String gxbs, JSONObject dataJson) throws Exception {
        JSONObject result = new JSONObject();
        result.put("code", "1");
        RecordSet rs = new RecordSet();
        try {
            //获取数据库接口主表配置
            Dekra_Interface_Service interfaceService = new Dekra_Interface_Service();
            List < Map < String, String >> mainConfigList = interfaceService.getInterfaceConfigList(gxbs, "10");
            //            this.writeLog("---" + SERVICE_NAME + " mainConfigList size:" + mainConfigList.size());
            //获取数据库接口明细表配置
            List < Map < String, String >> dt1_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "0");
            //            this.writeLog("---" + SERVICE_NAME + " dt1_ConfigList size:" + dt1_ConfigList.size());
            //设置主表字段
            Integer _userid = 0;
            String lastname = "";
            String subcompanyid1 = "";
            String departmentid = "";
            if (dataJson.get("txt_UserId") != null) {
                String userCode = dataJson.getString("txt_UserId");
                rs.execute("select * from HrmResource where workcode='" + userCode + "'");
                if (rs.next()) {
                    _userid = Integer.parseInt(Util.null2String(rs.getString("id")));
                    lastname = Util.null2String(rs.getString("lastname"));
                    subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
                    departmentid = Util.null2String(rs.getString("departmentid"));
                }
            }
            this.writeLog("---" + SERVICE_NAME + " lastname:" + lastname);
            this.writeLog("---" + SERVICE_NAME + " subcompanyid1:" + subcompanyid1);
            this.writeLog("---" + SERVICE_NAME + " departmentid:" + departmentid);
            if (dataJson.get("txt_QuotationNo") == null) {
                result.put("code", "-1");
                result.put("message", "txt_QuotationNo 不能为空");
                return result;
            }
            if (_userid == 0) {
                result.put("code", "-1");
                result.put("message", "txt_UserId 无效");
                return result;
            }
            //查询人力资源信息
            String userName_CN = "";
            rs.execute("select * from cus_fielddata where id='" + _userid + "'");
            if (rs.next()) {
                userName_CN = Util.null2String(rs.getString("field4"));
            }
            //            String companyid = "";
            //            rs.execute("select * from hrmsubcompany where id='" + subcompanyid1 + "'");
            //            if (rs.next()) {
            //                companyid = Util.null2String(rs.getString("companyid"));
            //            }
            String departmentcode = "";
            rs.execute("select * from hrmdepartment where id='" + departmentid + "'");
            if (rs.next()) {
                departmentcode = Util.null2String(rs.getString("departmentcode"));
            }
            this.writeLog("---" + SERVICE_NAME + " _userid:" + _userid);
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[mainConfigList.size()]; //主表字段
            for (int i = 0; i < mainConfigList.size(); i++) {
                Map < String, String > paramMap = mainConfigList.get(i);
                wrti[i] = new WorkflowRequestTableField();
                for (Map.Entry < String, String > m: paramMap.entrySet()) {
                    writeLog("main table key:" + m.getKey() + " value:" + m.getValue());
                    if ("oa_field".equals(m.getKey())) {
                        wrti[i].setFieldName(m.getValue());
                    }
                    if ("interface_field".equals(m.getKey())) {
                        if ("txt_UserNameE".equals(m.getValue())) {
                            wrti[i].setFieldValue(_userid + "");
                        } else if ("txt_UserName".equals(m.getValue())) {
                            wrti[i].setFieldValue(userName_CN);
                        } else if ("txt_OrganizationName".equals(m.getValue())) {
                            wrti[i].setFieldValue(subcompanyid1);
                        } else if ("txt_DepartmentNo".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentcode);
                        } else if ("txt_DepartmentName".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentid);
                        } else if ("billStatus".equals(m.getValue())) {
                            wrti[i].setFieldValue("0");
                        } else {
                            wrti[i].setFieldValue(dataJson.get(m.getValue()) != null ? dataJson.getString(m.getValue()) : "");
                        }
                    }
                }
                wrti[i].setEdit(true);
                wrti[i].setView(true);
            }
            WorkflowRequestTableRecord[] mainRecordArray = new WorkflowRequestTableRecord[1]; //主字段只有一行数据
            mainRecordArray[0] = new WorkflowRequestTableRecord();
            mainRecordArray[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo maininfo = new WorkflowMainTableInfo(); //主表
            maininfo.setRequestRecords(mainRecordArray);
            //添加明细数据
            String detail_1 = dataJson.getString("detail_1");
            this.writeLog("---" + SERVICE_NAME + " detail_1:" + detail_1);
            JSONArray dt1Array = JSONArray.fromObject(detail_1);
            int detailrows = dt1Array.size();
            this.writeLog("---" + SERVICE_NAME + " detailrows:" + detailrows);
            WorkflowRequestTableRecord[] detaiTableArray = new WorkflowRequestTableRecord[detailrows];
            for (int i = 0; i < detailrows; i++) {
                JSONObject item = (JSONObject) dt1Array.get(i);
                wrti = new WorkflowRequestTableField[dt1_ConfigList.size()]; //字段信息
                for (int j = 0; j < dt1_ConfigList.size(); j++) {
                    Map < String, String > paramMap = dt1_ConfigList.get(j);
                    wrti[j] = new WorkflowRequestTableField();
                    for (Map.Entry < String, String > m: paramMap.entrySet()) {
                        writeLog("dt1 table key:" + m.getKey() + " value:" + m.getValue());
                        if ("oa_field".equals(m.getKey())) {
                            wrti[j].setFieldName(m.getValue());
                        }
                        if ("interface_field".equals(m.getKey())) {
                            wrti[j].setFieldValue(item.get(m.getValue()) != null ? item.getString(m.getValue()) : "");
                        }
                    }
                    wrti[j].setView(true);
                    wrti[j].setEdit(true);
                }
                detaiTableArray[i] = new WorkflowRequestTableRecord();
                detaiTableArray[i].setWorkflowRequestTableFields(wrti);
            }
            //指定明细表的个数，多个明细表指定多个，顺序按照明细的顺序
            WorkflowDetailTableInfo[] WorkflowDetailTableInfo = new WorkflowDetailTableInfo[1];
            WorkflowDetailTableInfo[0] = new WorkflowDetailTableInfo();
            WorkflowDetailTableInfo[0].setWorkflowRequestTableRecords(detaiTableArray);
            WorkflowRequestInfo requestInfo = new WorkflowRequestInfo();
            //流程标题
            StringBuffer titleSb = new StringBuffer();
            if ("24".equals(subcompanyid1)) {
                titleSb.append("報價單");
            } else {
                titleSb.append("报价单");
            }
            if (dataJson.get("txt_QuotationNo") != null) {
                titleSb.append("-");
                titleSb.append(dataJson.get("txt_QuotationNo"));
            }
            if (!"".equals(lastname)) {
                titleSb.append("(");
                titleSb.append(lastname);
                titleSb.append(")");
            }
            requestInfo.setRequestName(titleSb.toString());
            this.writeLog("---" + SERVICE_NAME + " createWf() 标题:" + requestInfo.getRequestName());
            requestInfo.setCreatorId(_userid + ""); //创建人
            requestInfo.setRequestLevel("0"); //0 正常，1重要，2紧急
            WorkflowBaseInfo base = new WorkflowBaseInfo();
            base.setWorkflowId(workflowId); //流程id
            requestInfo.setWorkflowBaseInfo(base);
            requestInfo.setIsnextflow("1"); //流转到下一个节点
            requestInfo.setWorkflowMainTableInfo(maininfo); //添加主字段数据
            requestInfo.setWorkflowDetailTableInfos(WorkflowDetailTableInfo); //添加明细数据
            requestInfo.setCanEdit(true);
            WorkflowServiceImpl wsi = new WorkflowServiceImpl();
            int requestid = Integer.parseInt(wsi.doCreateWorkflowRequest(requestInfo, _userid));
            this.writeLog(SERVICE_NAME + " requestid:" + requestid);
            if (requestid <= 0) {
                result.put("code", "-1");
                result.put("message", "流程创建失败");
                return result;
            }
            this.writeLog(SERVICE_NAME + " 流程创建成功,requestid:" + requestid);
            result.put("code", "1");
            result.put("message", "操作成功");
            JSONObject dataObj = new JSONObject();
            dataObj.put("requestId", requestid);
            dataObj.put("txt_QuotationNo", dataJson.getString("txt_QuotationNo"));
            result.put("data", dataObj);
        } catch (Exception ex) {
            ex.printStackTrace();
            this.writeLog(SERVICE_NAME + " createWf() error:" + ex.getMessage());
            result.put("code", "-1");
            result.put("message", "流程创建失败");
        }
        //修改，审批打回后，申请人什么都不做
        return result;
    }
    /**
     * 合约单
     *
     * @param workflowId
     * @param gxbs
     * @param dataJson
     * @return
     * @throws Exception
     */
    private JSONObject createWfForContractForm(String workflowId, String gxbs, JSONObject dataJson) throws Exception {
        JSONObject result = new JSONObject();
        result.put("code", "1");
        RecordSet rs = new RecordSet();
        try {
            //获取数据库接口主表配置
            Dekra_Interface_Service interfaceService = new Dekra_Interface_Service();
            List < Map < String, String >> mainConfigList = interfaceService.getInterfaceConfigList(gxbs, "10");
            this.writeLog("---" + SERVICE_NAME + " mainConfigList size:" + mainConfigList.size());
            //获取数据库接口明细表配置
            List < Map < String, String >> dt1_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "0");
            this.writeLog("---" + SERVICE_NAME + " dt1_ConfigList size:" + dt1_ConfigList.size());
            //设置主表字段
            Integer _userid = 0;
            String lastname = "";
            String subcompanyid1 = "";
            String departmentid = "";
            if (dataJson.get("txt_UserId") != null) {
                String userCode = dataJson.getString("txt_UserId");
                rs.execute("select * from HrmResource where workcode='" + userCode + "'");
                if (rs.next()) {
                    _userid = Integer.parseInt(Util.null2String(rs.getString("id")));
                    lastname = Util.null2String(rs.getString("lastname"));
                    subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
                    departmentid = Util.null2String(rs.getString("departmentid"));
                }
            }
            this.writeLog("---" + SERVICE_NAME + " lastname:" + lastname);
            this.writeLog("---" + SERVICE_NAME + " subcompanyid1:" + subcompanyid1);
            this.writeLog("---" + SERVICE_NAME + " departmentid:" + departmentid);
            if (dataJson.get("txt_UniformNo") == null) {
                result.put("code", "-1");
                result.put("message", "txt_UniformNo 不能为空");
                return result;
            }
            if (_userid == 0) {
                result.put("code", "-1");
                result.put("message", "txt_UserId 无效");
                return result;
            }
            //查询人力资源信息
            String userName_CN = "";
            rs.execute("select * from cus_fielddata where id='" + _userid + "'");
            if (rs.next()) {
                userName_CN = Util.null2String(rs.getString("field4"));
            }
            String departmentcode = "";
            rs.execute("select * from hrmdepartment where id='" + departmentid + "'");
            if (rs.next()) {
                departmentcode = Util.null2String(rs.getString("departmentcode"));
            }
            this.writeLog("---" + SERVICE_NAME + " _userid:" + _userid);
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[mainConfigList.size()]; //主表字段
            for (int i = 0; i < mainConfigList.size(); i++) {
                Map < String, String > paramMap = mainConfigList.get(i);
                wrti[i] = new WorkflowRequestTableField();
                for (Map.Entry < String, String > m: paramMap.entrySet()) {
                    writeLog("main table key:" + m.getKey() + " value:" + m.getValue());
                    if ("oa_field".equals(m.getKey())) {
                        wrti[i].setFieldName(m.getValue());
                    }
                    if ("interface_field".equals(m.getKey())) {
                        if ("txt_UserNameE".equals(m.getValue())) {
                            wrti[i].setFieldValue(_userid + "");
                        } else if ("txt_UserName".equals(m.getValue())) {
                            wrti[i].setFieldValue(userName_CN);
                        } else if ("txt_OrganizationName".equals(m.getValue())) {
                            wrti[i].setFieldValue(subcompanyid1);
                        } else if ("txt_DepartmentNo".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentcode);
                        } else if ("txt_DepartmentName".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentid);
                        } else if ("billStatus".equals(m.getValue())) {
                            wrti[i].setFieldValue("0");
                        } else {
                            wrti[i].setFieldValue(dataJson.get(m.getValue()) != null ? dataJson.getString(m.getValue()) : "");
                        }
                    }
                }
                wrti[i].setEdit(true);
                wrti[i].setView(true);
            }
            WorkflowRequestTableRecord[] mainRecordArray = new WorkflowRequestTableRecord[1]; //主字段只有一行数据
            mainRecordArray[0] = new WorkflowRequestTableRecord();
            mainRecordArray[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo maininfo = new WorkflowMainTableInfo(); //主表
            maininfo.setRequestRecords(mainRecordArray);
            //添加明细数据
            String detail_1 = dataJson.getString("detail_1");
            this.writeLog("---" + SERVICE_NAME + " detail_1:" + detail_1);
            JSONArray dt1Array = JSONArray.fromObject(detail_1);
            int detailrows = dt1Array.size();
            this.writeLog("---" + SERVICE_NAME + " detailrows:" + detailrows);
            WorkflowRequestTableRecord[] detaiTableArray = new WorkflowRequestTableRecord[detailrows];
            for (int i = 0; i < detailrows; i++) {
                JSONObject item = (JSONObject) dt1Array.get(i);
                wrti = new WorkflowRequestTableField[dt1_ConfigList.size()]; //字段信息
                for (int j = 0; j < dt1_ConfigList.size(); j++) {
                    Map < String, String > paramMap = dt1_ConfigList.get(j);
                    wrti[j] = new WorkflowRequestTableField();
                    for (Map.Entry < String, String > m: paramMap.entrySet()) {
                        //                        writeLog("dt1 table key:" + m.getKey() + " value:" + m.getValue());
                        if ("oa_field".equals(m.getKey())) {
                            wrti[j].setFieldName(m.getValue());
                        }
                        if ("interface_field".equals(m.getKey())) {
                            wrti[j].setFieldValue(item.get(m.getValue()) != null ? item.getString(m.getValue()) : "");
                        }
                    }
                    wrti[j].setView(true);
                    wrti[j].setEdit(true);
                }
                detaiTableArray[i] = new WorkflowRequestTableRecord();
                detaiTableArray[i].setWorkflowRequestTableFields(wrti);
            }
            //指定明细表的个数，多个明细表指定多个，顺序按照明细的顺序
            WorkflowDetailTableInfo[] WorkflowDetailTableInfo = new WorkflowDetailTableInfo[2];
            WorkflowDetailTableInfo[0] = new WorkflowDetailTableInfo();
            WorkflowDetailTableInfo[0].setWorkflowRequestTableRecords(detaiTableArray);
            WorkflowRequestInfo requestInfo = new WorkflowRequestInfo();
            //流程标题
            StringBuffer titleSb = new StringBuffer();
            if ("24".equals(subcompanyid1)) {
                titleSb.append("合約價申請");
            } else {
                titleSb.append("合约价申请");
            }
            if (dataJson.get("txt_CustomerName") != null) {
                titleSb.append("-");
                titleSb.append(dataJson.get("txt_CustomerName"));
            }
            if (!"".equals(lastname)) {
                titleSb.append("(");
                titleSb.append(lastname);
                titleSb.append(")");
            }
            requestInfo.setRequestName(titleSb.toString());
            this.writeLog("---" + SERVICE_NAME + " createWf() 标题:" + requestInfo.getRequestName());
            requestInfo.setCreatorId(_userid + ""); //创建人
            requestInfo.setRequestLevel("0"); //0 正常，1重要，2紧急
            WorkflowBaseInfo base = new WorkflowBaseInfo();
            base.setWorkflowId(workflowId); //流程id
            requestInfo.setWorkflowBaseInfo(base);
            requestInfo.setIsnextflow("1"); //流转到下一个节点
            requestInfo.setWorkflowMainTableInfo(maininfo); //添加主字段数据
            requestInfo.setWorkflowDetailTableInfos(WorkflowDetailTableInfo); //添加明细数据
            requestInfo.setCanEdit(true);
            WorkflowServiceImpl wsi = new WorkflowServiceImpl();
            int requestid = Integer.parseInt(wsi.doCreateWorkflowRequest(requestInfo, _userid));
            this.writeLog(SERVICE_NAME + " requestid:" + requestid);
            if (requestid <= 0) {
                result.put("code", "-1");
                result.put("message", "流程创建失败");
                return result;
            }
            this.writeLog(SERVICE_NAME + " 流程创建成功,requestid:" + requestid);
            result.put("code", "1");
            result.put("message", "操作成功");
            JSONObject dataObj = new JSONObject();
            dataObj.put("requestId", requestid);
            dataObj.put("txt_UniformNo", dataJson.getString("txt_UniformNo"));
            result.put("data", dataObj);
        } catch (Exception ex) {
            ex.printStackTrace();
            this.writeLog(SERVICE_NAME + " createWf() error:" + ex.getMessage());
            result.put("code", "-1");
            result.put("message", "流程创建失败");
        }
        //修改，审批打回后，申请人什么都不做
        return result;
    }
    /**
     * 服务记录单
     *
     * @param workflowId
     * @param gxbs
     * @param dataJson
     * @return
     * @throws Exception
     */
    private JSONObject createWfForServiceRecord(String workflowId, String gxbs, JSONObject dataJson) throws Exception {
        JSONObject result = new JSONObject();
        result.put("code", "1");
        RecordSet rs = new RecordSet();
        try {
            //获取数据库接口主表配置
            Dekra_Interface_Service interfaceService = new Dekra_Interface_Service();
            List < Map < String, String >> mainConfigList = interfaceService.getInterfaceConfigList(gxbs, "10");
            this.writeLog("---" + SERVICE_NAME + " mainConfigList size:" + mainConfigList.size());
            //获取数据库接口明细表配置
            List < Map < String, String >> dt1_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "0");
            this.writeLog("---" + SERVICE_NAME + " dt1_ConfigList size:" + dt1_ConfigList.size());
            //设置主表字段
            Integer _userid = 0;
            String lastname = "";
            String subcompanyid1 = "";
            String departmentid = "";
            if (dataJson.get("txt_UserId") != null) {
                String userCode = dataJson.getString("txt_UserId");
                rs.execute("select * from HrmResource where workcode='" + userCode + "'");
                if (rs.next()) {
                    _userid = Integer.parseInt(Util.null2String(rs.getString("id")));
                    lastname = Util.null2String(rs.getString("lastname"));
                    subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
                    departmentid = Util.null2String(rs.getString("departmentid"));
                }
            }
            this.writeLog("---" + SERVICE_NAME + " lastname:" + lastname);
            this.writeLog("---" + SERVICE_NAME + " subcompanyid1:" + subcompanyid1);
            this.writeLog("---" + SERVICE_NAME + " departmentid:" + departmentid);
            if (dataJson.get("txt_ServiceRecordNo") == null) {
                result.put("code", "-1");
                result.put("message", "txt_ServiceRecordNo 不能为空");
                return result;
            }
            if (_userid == 0) {
                result.put("code", "-1");
                result.put("message", "txt_UserId 无效");
                return result;
            }
            //查询人力资源信息
            String userName_CN = "";
            rs.execute("select * from cus_fielddata where id='" + _userid + "'");
            if (rs.next()) {
                userName_CN = Util.null2String(rs.getString("field4"));
            }
            String departmentcode = "";
            rs.execute("select * from hrmdepartment where id='" + departmentid + "'");
            if (rs.next()) {
                departmentcode = Util.null2String(rs.getString("departmentcode"));
            }
            this.writeLog("---" + SERVICE_NAME + " _userid:" + _userid);
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[mainConfigList.size()]; //主表字段
            for (int i = 0; i < mainConfigList.size(); i++) {
                Map < String, String > paramMap = mainConfigList.get(i);
                wrti[i] = new WorkflowRequestTableField();
                for (Map.Entry < String, String > m: paramMap.entrySet()) {
                    writeLog("main table key:" + m.getKey() + " value:" + m.getValue());
                    if ("oa_field".equals(m.getKey())) {
                        wrti[i].setFieldName(m.getValue());
                    }
                    if ("interface_field".equals(m.getKey())) {
                        if ("txt_UserNameE".equals(m.getValue())) {
                            wrti[i].setFieldValue(_userid + "");
                        } else if ("txt_UserName".equals(m.getValue())) {
                            wrti[i].setFieldValue(userName_CN);
                        } else if ("txt_OrganizationName".equals(m.getValue())) {
                            wrti[i].setFieldValue(subcompanyid1);
                        } else if ("txt_DepartmentNo".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentcode);
                        } else if ("txt_DepartmentName".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentid);
                        } else if ("billStatus".equals(m.getValue())) {
                            wrti[i].setFieldValue("0");
                        } else {
                            wrti[i].setFieldValue(dataJson.get(m.getValue()) != null ? dataJson.getString(m.getValue()) : "");
                        }
                    }
                }
                wrti[i].setEdit(true);
                wrti[i].setView(true);
            }
            WorkflowRequestTableRecord[] mainRecordArray = new WorkflowRequestTableRecord[1]; //主字段只有一行数据
            mainRecordArray[0] = new WorkflowRequestTableRecord();
            mainRecordArray[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo maininfo = new WorkflowMainTableInfo(); //主表
            maininfo.setRequestRecords(mainRecordArray);
            //添加明细数据
            String detail_1 = dataJson.getString("detail_1");
            this.writeLog("---" + SERVICE_NAME + " detail_1:" + detail_1);
            JSONArray dt1Array = JSONArray.fromObject(detail_1);
            int detailrows = dt1Array.size();
            this.writeLog("---" + SERVICE_NAME + " detailrows:" + detailrows);
            WorkflowRequestTableRecord[] detaiTableArray = new WorkflowRequestTableRecord[detailrows];
            for (int i = 0; i < detailrows; i++) {
                JSONObject item = (JSONObject) dt1Array.get(i);
                wrti = new WorkflowRequestTableField[dt1_ConfigList.size()]; //字段信息
                for (int j = 0; j < dt1_ConfigList.size(); j++) {
                    Map < String, String > paramMap = dt1_ConfigList.get(j);
                    wrti[j] = new WorkflowRequestTableField();
                    for (Map.Entry < String, String > m: paramMap.entrySet()) {
                        writeLog("dt1 table key:" + m.getKey() + " value:" + m.getValue());
                        if ("oa_field".equals(m.getKey())) {
                            wrti[j].setFieldName(m.getValue());
                        }
                        if ("interface_field".equals(m.getKey())) {
                            wrti[j].setFieldValue(item.get(m.getValue()) != null ? item.getString(m.getValue()) : "");
                        }
                    }
                    wrti[j].setView(true);
                    wrti[j].setEdit(true);
                }
                detaiTableArray[i] = new WorkflowRequestTableRecord();
                detaiTableArray[i].setWorkflowRequestTableFields(wrti);
            }
            //指定明细表的个数，多个明细表指定多个，顺序按照明细的顺序
            WorkflowDetailTableInfo[] WorkflowDetailTableInfo = new WorkflowDetailTableInfo[2];
            WorkflowDetailTableInfo[0] = new WorkflowDetailTableInfo();
            WorkflowDetailTableInfo[0].setWorkflowRequestTableRecords(detaiTableArray);
            WorkflowRequestInfo requestInfo = new WorkflowRequestInfo();
            //流程标题
            StringBuffer titleSb = new StringBuffer();
            if ("24".equals(subcompanyid1)) {
                titleSb.append("服務紀錄單免收費申請");
            } else {
                titleSb.append("服务纪录单免收费申请");
            }
            if (dataJson.get("txt_ServiceRecordNo") != null) {
                titleSb.append("-");
                titleSb.append(dataJson.get("txt_ServiceRecordNo"));
            }
            if (!"".equals(lastname)) {
                titleSb.append("(");
                titleSb.append(lastname);
                titleSb.append(")");
            }
            requestInfo.setRequestName(titleSb.toString());
            this.writeLog("---" + SERVICE_NAME + " createWf() 标题:" + requestInfo.getRequestName());
            requestInfo.setCreatorId(_userid + ""); //创建人
            requestInfo.setRequestLevel("0"); //0 正常，1重要，2紧急
            WorkflowBaseInfo base = new WorkflowBaseInfo();
            base.setWorkflowId(workflowId); //流程id
            requestInfo.setWorkflowBaseInfo(base);
            requestInfo.setIsnextflow("1"); //流转到下一个节点
            requestInfo.setWorkflowMainTableInfo(maininfo); //添加主字段数据
            requestInfo.setWorkflowDetailTableInfos(WorkflowDetailTableInfo); //添加明细数据
            requestInfo.setCanEdit(true);
            WorkflowServiceImpl wsi = new WorkflowServiceImpl();
            int requestid = Integer.parseInt(wsi.doCreateWorkflowRequest(requestInfo, _userid));
            this.writeLog(SERVICE_NAME + " requestid:" + requestid);
            if (requestid <= 0) {
                result.put("code", "-1");
                result.put("message", "流程创建失败");
                return result;
            }
            this.writeLog(SERVICE_NAME + " 流程创建成功,requestid:" + requestid);
            result.put("code", "1");
            result.put("message", "操作成功");
            JSONObject dataObj = new JSONObject();
            dataObj.put("requestId", requestid);
            dataObj.put("txt_ServiceRecordNo", dataJson.getString("txt_ServiceRecordNo"));
            result.put("data", dataObj);
        } catch (Exception ex) {
            ex.printStackTrace();
            this.writeLog(SERVICE_NAME + " createWf() error:" + ex.getMessage());
            result.put("code", "-1");
            result.put("message", "流程创建失败");
        }
        //修改，审批打回后，申请人什么都不做
        return result;
    }
    /**
     * 外包申请流程数据对接
     *
     * @param workflowId
     * @param gxbs
     * @param dataJson
     * @return
     * @throws Exception
     */
    private JSONObject createWfForOutsourcing(String workflowId, String tableName, String gxbs, JSONObject dataJson) throws Exception {
        JSONObject result = new JSONObject();
        result.put("code", "1");
        RecordSet rs = new RecordSet();
        try {
            //获取数据库接口主表配置
            Dekra_Interface_Service interfaceService = new Dekra_Interface_Service();
            List < Map < String, String >> mainConfigList = interfaceService.getInterfaceConfigList(gxbs, "10");
            this.writeLog("---" + SERVICE_NAME + " mainConfigList size:" + mainConfigList.size());
            //获取数据库接口明细表配置
            List < Map < String, String >> dt1_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "0");
            this.writeLog("---" + SERVICE_NAME + " dt1_ConfigList size:" + dt1_ConfigList.size());
            //设置主表字段
            Integer _userid = 0;
            String lastname = "";
            String subcompanyid1 = "";
            String departmentid = "";
            String jobtitle = "";
            if (dataJson.get("UserId") != null) {
                String userCode = dataJson.getString("UserId");
                rs.execute("select * from HrmResource where workcode='" + userCode + "'");
                if (rs.next()) {
                    _userid = Integer.parseInt(Util.null2String(rs.getString("id")));
                    lastname = Util.null2String(rs.getString("lastname"));
                    subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
                    departmentid = Util.null2String(rs.getString("departmentid"));
                    jobtitle = Util.null2String(rs.getString("jobtitle"));
                }
            }
            this.writeLog("---" + SERVICE_NAME + " lastname:" + lastname);
            this.writeLog("---" + SERVICE_NAME + " subcompanyid1:" + subcompanyid1);
            this.writeLog("---" + SERVICE_NAME + " departmentid:" + departmentid);
            if (dataJson.get("ProjectItemNo") == null) {
                result.put("code", "-1");
                result.put("message", "ProjectItemNo 不能为空");
                return result;
            }
            if (_userid == 0) {
                result.put("code", "-1");
                result.put("message", "UserId 无效");
                return result;
            }
            //查询人力资源信息
            String userName_CN = "";
            rs.execute("select * from cus_fielddata where id='" + _userid + "'");
            if (rs.next()) {
                userName_CN = Util.null2String(rs.getString("field4"));
            }
            String departmentcode = "";
            rs.execute("select * from hrmdepartment where id='" + departmentid + "'");
            if (rs.next()) {
                departmentcode = Util.null2String(rs.getString("departmentcode"));
            }
            this.writeLog("---" + SERVICE_NAME + " _userid:" + _userid);
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[mainConfigList.size()]; //主表字段
            for (int i = 0; i < mainConfigList.size(); i++) {
                Map < String, String > paramMap = mainConfigList.get(i);
                wrti[i] = new WorkflowRequestTableField();
                for (Map.Entry < String, String > m: paramMap.entrySet()) {
                    writeLog("main table key:" + m.getKey() + " value:" + m.getValue());
                    if ("oa_field".equals(m.getKey())) {
                        wrti[i].setFieldName(m.getValue());
                    }
                    if ("interface_field".equals(m.getKey())) {
                        if ("txt_UserNameE".equals(m.getValue())) {
                            wrti[i].setFieldValue(_userid + "");
                        } else if ("txt_UserName".equals(m.getValue())) {
                            wrti[i].setFieldValue(userName_CN);
                        } else if ("txt_OrganizationName".equals(m.getValue())) {
                            wrti[i].setFieldValue(subcompanyid1);
                        } else if ("txt_DepartmentNo".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentcode);
                        } else if ("txt_DepartmentName".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentid);
                        } else if ("jobtitle".equals(m.getValue())) {
                            wrti[i].setFieldValue(jobtitle);
                        } else if ("sqrq".equals(m.getValue())) {
                            wrti[i].setFieldValue(DekraUtil.parseToDateString(Calendar.getInstance().getTime(), DekraUtil.formatYYYYMMDD));
                        } else if ("billStatus".equals(m.getValue())) {
                            wrti[i].setFieldValue("0");
                        } else {
                            wrti[i].setFieldValue(dataJson.get(m.getValue()) != null ? dataJson.getString(m.getValue()) : "");
                        }
                    }
                }
                wrti[i].setEdit(true);
                wrti[i].setView(true);
            }
            WorkflowRequestTableRecord[] mainRecordArray = new WorkflowRequestTableRecord[1]; //主字段只有一行数据
            mainRecordArray[0] = new WorkflowRequestTableRecord();
            mainRecordArray[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo maininfo = new WorkflowMainTableInfo(); //主表
            maininfo.setRequestRecords(mainRecordArray);
            WorkflowRequestInfo requestInfo = new WorkflowRequestInfo();
            //流程标题
            StringBuffer titleSb = new StringBuffer();
            if ("24".equals(subcompanyid1)) {
                titleSb.append("外包申請單");
            } else {
                titleSb.append("外包申请单");
            }
            if (dataJson.get("ProjectItemNo") != null) {
                titleSb.append("-");
                titleSb.append(dataJson.get("ProjectItemNo"));
            }
            if (!"".equals(lastname)) {
                titleSb.append("(");
                titleSb.append(lastname);
                titleSb.append(")");
            }
            requestInfo.setRequestName(titleSb.toString());
            this.writeLog("---" + SERVICE_NAME + " createWf() 标题:" + requestInfo.getRequestName());
            requestInfo.setCreatorId(_userid + ""); //创建人
            requestInfo.setRequestLevel("0"); //0 正常，1重要，2紧急
            WorkflowBaseInfo base = new WorkflowBaseInfo();
            base.setWorkflowId(workflowId); //流程id
            requestInfo.setWorkflowBaseInfo(base);
            requestInfo.setIsnextflow("1"); //流转到下一个节点
            requestInfo.setWorkflowMainTableInfo(maininfo); //添加主字段数据
            //            requestInfo.setWorkflowDetailTableInfos(WorkflowDetailTableInfo);//添加明细数据
            requestInfo.setCanEdit(true);
            WorkflowServiceImpl wsi = new WorkflowServiceImpl();
            int requestid = Integer.parseInt(wsi.doCreateWorkflowRequest(requestInfo, _userid));
            this.writeLog(SERVICE_NAME + " requestid:" + requestid);
            if (requestid <= 0) {
                result.put("code", "-1");
                result.put("message", "流程创建失败");
                return result;
            }
            this.writeLog(SERVICE_NAME + " 流程创建成功,requestid:" + requestid);
            result.put("code", "1");
            result.put("message", "操作成功");
            JSONObject dataObj = new JSONObject();
            dataObj.put("requestId", requestid);
            dataObj.put("ProjectItemNo", dataJson.getString("ProjectItemNo"));
            result.put("data", dataObj);
        } catch (Exception ex) {
            ex.printStackTrace();
            this.writeLog(SERVICE_NAME + " createWf() error:" + ex.getMessage());
            result.put("code", "-1");
            result.put("message", "流程创建失败");
        }
        return result;
    }
    /**
     * 外包付款流程
     *
     * @param workflowId
     * @param gxbs
     * @param dataJson
     * @return
     * @throws Exception
     */
    private JSONObject createWfForOutsourcingPayment(String workflowId, String gxbs, String tableName, JSONObject dataJson, DekraLogicService logicService) throws Exception {
        JSONObject result = new JSONObject();
        result.put("code", "1");
        RecordSet rs = new RecordSet();
        try {
            //获取数据库接口主表配置
            Dekra_Interface_Service interfaceService = new Dekra_Interface_Service();
            List < Map < String, String >> mainConfigList = interfaceService.getInterfaceConfigList(gxbs, "10");
            this.writeLog("---" + SERVICE_NAME + " mainConfigList size:" + mainConfigList.size());
            //获取数据库接口明细表配置
            List < Map < String, String >> dt1_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "1");
            this.writeLog("---" + SERVICE_NAME + " dt1_ConfigList size:" + dt1_ConfigList.size());
            //设置主表字段
            Integer creator = 0;
            Integer applyId = 0;
            String lastname = "";
            String subcompanyid1 = "";
            String departmentid = "";
            String jobtitle = "";
            Integer inputUserId = 0;
            String inputLastname = "";
            String inputJobTitle = "";
            if (dataJson.get("UserId") != null) {
                String applyUserCode = dataJson.getString("UserId4Payment");
                rs.execute("select * from HrmResource where workcode='" + applyUserCode + "'");
                if (rs.next()) {
                    applyId = Integer.parseInt(Util.null2String(rs.getString("id")));
                    lastname = Util.null2String(rs.getString("lastname"));
                    subcompanyid1 = Util.null2String(rs.getString("subcompanyid1"));
                    departmentid = Util.null2String(rs.getString("departmentid"));
                    jobtitle = Util.null2String(rs.getString("jobtitle"));
                }
                String userCode = dataJson.getString("UserId"); //填单人
                String tdrSql = "select * from HrmResource where workcode='" + userCode + "'";
                this.writeLog("---" + SERVICE_NAME + " inputLastname:" + tdrSql);
                rs.execute(tdrSql);
                if (rs.next()) {
                    inputUserId = Integer.parseInt(Util.null2String(rs.getString("id")));
                    creator = inputUserId;
                    inputLastname = Util.null2String(rs.getString("lastname"));
                    inputJobTitle = Util.null2String(rs.getString("jobtitle"));
                }
            }
            this.writeLog("---" + SERVICE_NAME + " lastname:" + lastname);
            this.writeLog("---" + SERVICE_NAME + " subcompanyid1:" + subcompanyid1);
            this.writeLog("---" + SERVICE_NAME + " departmentid:" + departmentid);
            if (dataJson.get("ProjectItemNo") == null) {
                result.put("code", "-1");
                result.put("message", "ProjectItemNo 不能为空");
                return result;
            }
            if (applyId == 0) {
                result.put("code", "-1");
                result.put("message", "UserId 无效");
                return result;
            }
            //查询人力资源信息
            String userName_CN = "";
            rs.execute("select * from cus_fielddata where id='" + applyId + "'");
            if (rs.next()) {
                userName_CN = Util.null2String(rs.getString("field4"));
            }
            String departmentcode = "";
            rs.execute("select * from hrmdepartment where id='" + departmentid + "'");
            if (rs.next()) {
                departmentcode = Util.null2String(rs.getString("departmentcode"));
            }
            //填单人信息
            String input_UserName_CN = "";
            rs.execute("select * from cus_fielddata where id='" + inputUserId + "'");
            if (rs.next()) {
                input_UserName_CN = Util.null2String(rs.getString("field4"));
            }
            this.writeLog("---" + SERVICE_NAME + " applyId:" + applyId);
            this.writeLog("---" + SERVICE_NAME + " inputLastname:" + inputLastname);
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[mainConfigList.size()]; //主表字段
            for (int i = 0; i < mainConfigList.size(); i++) {
                Map < String, String > paramMap = mainConfigList.get(i);
                wrti[i] = new WorkflowRequestTableField();
                for (Map.Entry < String, String > m: paramMap.entrySet()) {
                    writeLog("main table key:" + m.getKey() + " value:" + m.getValue());
                    if ("oa_field".equals(m.getKey())) {
                        wrti[i].setFieldName(m.getValue());
                    }
                    if ("interface_field".equals(m.getKey())) {
                        if ("txt_UserNameE".equals(m.getValue())) {
                            wrti[i].setFieldValue(applyId + "");
                        } else if ("txt_UserName".equals(m.getValue())) {
                            wrti[i].setFieldValue(userName_CN);
                        } else if ("txt_OrganizationName".equals(m.getValue())) {
                            wrti[i].setFieldValue(subcompanyid1);
                        } else if ("txt_DepartmentNo".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentcode);
                        } else if ("txt_DepartmentName".equals(m.getValue())) {
                            wrti[i].setFieldValue(departmentid);
                        } else if ("jobtitle".equals(m.getValue())) {
                            wrti[i].setFieldValue(jobtitle);
                        } else if ("sqrq".equals(m.getValue())) {
                            wrti[i].setFieldValue(DekraUtil.parseToDateString(Calendar.getInstance().getTime(), DekraUtil.formatYYYYMMDD));
                        } else if ("billStatus".equals(m.getValue())) {
                            wrti[i].setFieldValue("0");
                        } else if ("txt_UserNameE_Input".equals(m.getValue())) {
                            wrti[i].setFieldValue(inputUserId + "");
                        } else if ("txt_UserName_Input".equals(m.getValue())) {
                            wrti[i].setFieldValue(input_UserName_CN);
                        } else if ("jobtitle_Input".equals(m.getValue())) {
                            wrti[i].setFieldValue(inputJobTitle);
                        } else {
                            wrti[i].setFieldValue(dataJson.get(m.getValue()) != null ? dataJson.getString(m.getValue()) : "");
                        }
                    }
                }
                wrti[i].setEdit(true);
                wrti[i].setView(true);
            }
            WorkflowRequestTableRecord[] mainRecordArray = new WorkflowRequestTableRecord[1]; //主字段只有一行数据
            mainRecordArray[0] = new WorkflowRequestTableRecord();
            mainRecordArray[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo maininfo = new WorkflowMainTableInfo(); //主表
            maininfo.setRequestRecords(mainRecordArray);
            //添加明细数据
            String detail_1 = dataJson.getString("Details");
            this.writeLog("---" + SERVICE_NAME + " Details:" + detail_1);
            JSONArray dt1Array = JSONArray.fromObject(detail_1);
            int detailrows = dt1Array.size();
            this.writeLog("---" + SERVICE_NAME + " detailrows:" + detailrows);
            WorkflowRequestTableRecord[] detaiTableArray = new WorkflowRequestTableRecord[detailrows];
            for (int i = 0; i < detailrows; i++) {
                JSONObject item = (JSONObject) dt1Array.get(i);
                wrti = new WorkflowRequestTableField[dt1_ConfigList.size()]; //字段信息
                for (int j = 0; j < dt1_ConfigList.size(); j++) {
                    Map < String, String > paramMap = dt1_ConfigList.get(j);
                    wrti[j] = new WorkflowRequestTableField();
                    for (Map.Entry < String, String > m: paramMap.entrySet()) {
                        writeLog("dt1 table key:" + m.getKey() + " value:" + m.getValue());
                        if ("oa_field".equals(m.getKey())) {
                            wrti[j].setFieldName(m.getValue());
                        }
                        if ("interface_field".equals(m.getKey())) {
                            wrti[j].setFieldValue(item.get(m.getValue()) != null ? item.getString(m.getValue()) : "");
                        }
                    }
                    wrti[j].setView(true);
                    wrti[j].setEdit(true);
                }
                detaiTableArray[i] = new WorkflowRequestTableRecord();
                detaiTableArray[i].setWorkflowRequestTableFields(wrti);
            }
            //            指定明细表的个数，多个明细表指定多个，顺序按照明细的顺序
            WorkflowDetailTableInfo[] WorkflowDetailTableInfo = new WorkflowDetailTableInfo[2];
            WorkflowDetailTableInfo[1] = new WorkflowDetailTableInfo();
            WorkflowDetailTableInfo[1].setWorkflowRequestTableRecords(detaiTableArray);
            WorkflowRequestInfo requestInfo = new WorkflowRequestInfo();
            //流程标题
            StringBuffer titleSb = new StringBuffer();
            if ("24".equals(subcompanyid1)) {
                titleSb.append("外包付款申请");
            } else {
                titleSb.append("外包付款申请");
            }
            if (dataJson.get("ProjectItemNo") != null) {
                titleSb.append("-");
                titleSb.append(dataJson.get("ProjectItemNo"));
            }
            if (!"".equals(lastname)) {
                titleSb.append("(");
                titleSb.append(lastname);
                titleSb.append(")");
            }
            requestInfo.setRequestName(titleSb.toString());
            this.writeLog("---" + SERVICE_NAME + " createWf() 标题:" + requestInfo.getRequestName());
            requestInfo.setCreatorId(creator + ""); //创建人
            requestInfo.setRequestLevel("0"); //0 正常，1重要，2紧急
            WorkflowBaseInfo base = new WorkflowBaseInfo();
            base.setWorkflowId(workflowId); //流程id
            requestInfo.setWorkflowBaseInfo(base);
            requestInfo.setIsnextflow("1"); //流转到下一个节点
            requestInfo.setWorkflowMainTableInfo(maininfo); //添加主字段数据
            requestInfo.setWorkflowDetailTableInfos(WorkflowDetailTableInfo); //添加明细数据
            requestInfo.setCanEdit(true);
            WorkflowServiceImpl wsi = new WorkflowServiceImpl();
            int requestid = Integer.parseInt(wsi.doCreateWorkflowRequest(requestInfo, creator));
            this.writeLog(SERVICE_NAME + " requestid:" + requestid);
            if (requestid <= 0) {
                result.put("code", "-1");
                result.put("message", "流程创建失败");
                return result;
            }
            this.writeLog(SERVICE_NAME + " 流程创建成功,requestid:" + requestid);
            doFileInfo(tableName, requestid, creator, dataJson, logicService);
            result.put("code", "1");
            result.put("message", "操作成功");
            JSONObject dataObj = new JSONObject();
            dataObj.put("requestId", requestid);
            dataObj.put("ProjectItemNo", dataJson.getString("ProjectItemNo"));
            result.put("data", dataObj);
        } catch (Exception ex) {
            ex.printStackTrace();
            this.writeLog(SERVICE_NAME + " createWf() error:" + ex.getMessage());
            result.put("code", "-1");
            result.put("message", "流程创建失败");
        }
        //修改，审批打回后，申请人什么都不做
        return result;
    }
    private void doFileInfo(String tableName, Integer requestid, Integer userId, JSONObject dataJson, DekraLogicService logicService) throws Exception {
        JSONArray fileArray = JSONArray.fromObject(dataJson.getString("Files"));
        FileOutputStream fileOut = null;
        File file = null;
        StringBuffer docIds = new StringBuffer();
        DekraCreateDocumentService createDocumentService = new DekraCreateDocumentService();
        RecordSet rs = new RecordSet();
        User user = new User(userId);
        String fileCategoryId = logicService.getSystemConfigValue("OutsourcingPayment_FILE_CATEGORY_ID", rs);
        for (int i = 0; i < fileArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) fileArray.get(i);
            byte[] buff = java.util.Base64.getDecoder().decode(jsonObject.getString("Datas"));
            String[] fileNameInfo = jsonObject.getString("Filename").split("\\.");
            String filePath = "D:\\WEAVER\\temp_files\\tw\\";
            File dir = new File(filePath);
            if (!dir.exists()) dir.mkdirs();
            String fileUrl = filePath + fileNameInfo;
            try {
                file = new File(fileUrl);
                fileOut = new FileOutputStream(file);
                fileOut.write(buff);
                fileOut.close();
            } catch (IOException e) {
                if (fileOut != null) {
                    fileOut.close();
                }
                this.writeLog("IOException........fileUrl:" + fileUrl);
                e.printStackTrace();
            }
            int textDocId = createDocumentService.creatDoc(user, fileNameInfo[0], Integer.parseInt(fileCategoryId), fileNameInfo[0], fileNameInfo[1], fileUrl);
            if (docIds.toString().length() == 0) {
                docIds.append(textDocId);
            } else {
                docIds.append(",");
                docIds.append(textDocId);
            }
            this.writeLog(SERVICE_NAME + " docIds " + docIds);
            if (file != null && file.exists()) {
                file.delete();
            }
        }
        if (docIds.toString().length() != 0) {
            String updateSql = "update " + tableName + " set fj='" + docIds + "' where requestid=" + requestid;
            rs.execute(updateSql);
        }
    }
}