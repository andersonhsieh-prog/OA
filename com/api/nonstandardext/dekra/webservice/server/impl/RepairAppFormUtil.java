package com.api.nonstandardext.dekra.webservice.server.impl;

import com.api.nonstandardext.dekra.service.DekraCreateDocumentService;
import com.api.nonstandardext.dekra.service.DekraLogicService;
import com.api.nonstandardext.dekra.service.Dekra_Interface_Service;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.xfire.util.Base64;
import weaver.conn.RecordSet;
import weaver.general.GCONST;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.hrm.User;
import weaver.workflow.webservices.*;

import java.io.*;
import java.util.*;

/**
 * 维修申请单流程
 * @author: caojy
 * @since: 2025/10/09 11:44
 * @description:
 */

public class RepairAppFormUtil {


    private String className = "RepairAppFormUtil";
    private String logName = "dkCus";

    private static String WF_CREATE_USER_ID = "1";//创建人 默认的管理员
    private static final String REQUEST_LEVEL = "0";//紧急程度
    private static final String IS_NEXT_FLOW = "1";//0创建节点，1下一节点

    /**
     * 整理创建流程信息
     * @param bill_type "bill type"
     * @param workflowId "workflow id"
     * @param gxbs
     * @param tableName "table name"
     * @param dataJson "json data"
     * @param logicService "logic service"
     * @return "JSONObject"
     * @throws Exception "Exception"
     */
    public JSONObject createWf(String bill_type, String workflowId, String gxbs, String tableName, JSONObject dataJson, DekraLogicService logicService) throws Exception{

        writeCusLog(className,"--------"+className+" Begin--------");

        JSONObject result = new JSONObject();
        result.put("code", "1");
        writeCusLog(className,"[dataJson:"+dataJson+"]");
        writeCusLog(className,"[bill_type:"+bill_type+"][workflowId:"+workflowId+"][tableName:"+tableName+"]");
        try {
            String QuotationNo = getJsonVal(dataJson,"QuotationNo");

            Dekra_Interface_Service interfaceService = new Dekra_Interface_Service();
            List<Map<String, String>> mainConfigList = interfaceService.getInterfaceConfigList(gxbs, "10");//10主表
            writeCusLog(className,"[mainConfigList:"+mainConfigList+"]");

            List<Map<String, String>> dt1_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "0");//明细1
            writeCusLog(className,"[dt1_ConfigList:"+dt1_ConfigList+"]");

            List<Map<String, String>> dt2_ConfigList = interfaceService.getInterfaceConfigList(gxbs, "1");//明细2
            writeCusLog(className,"[dt2_ConfigList:"+dt2_ConfigList+"]");

            String workcode4Apply = "";
            if ("CalibrateMaintainPayment".equals(bill_type)) {
                workcode4Apply = getJsonVal(dataJson, "UserId4Payment"); //用户工号
                writeCusLog(className, "[workcode4Apply:" + workcode4Apply + "]");
                if ("".equals(workcode4Apply)) {
                    result.put("code", "-1");
                    result.put("message", "UserId4Payment 不能为空");
                    return result;
                }
              } else  {
                workcode4Apply = getJsonVal(dataJson, "UserId4Apply"); // 用户工号
                writeCusLog(className, "[workcode4Apply:" + workcode4Apply + "]");
                if ("".equals(workcode4Apply)) {
                  result.put("code", "-1");
                  result.put("message", "UserId4Apply 不能为空");
                  return result;
                }
            }
            dataJson = getHrmInfo(dataJson, workcode4Apply, "4apply");
            writeCusLog(className, "[dataJsonNew:" + dataJson + "]");
            if ("".equals(getJsonVal(dataJson, "hrid4apply"))) {
                result.put("code", "-1");
                result.put("message", "【UserId4Apply:" + workcode4Apply + "】未匹配上正确的人员信息");
                return result;
            }

            String workcode = getJsonVal(dataJson,"UserId");//用户工号
            writeCusLog(className,"[workcode:"+workcode+"]");
            if ("".equals(workcode)) {
                result.put("code", "-1");
                result.put("message", "UserId 不能为空");
                return result;
            }
            dataJson = getHrmInfo(dataJson,workcode, "");
            writeCusLog(className,"[dataJsonNew1:"+dataJson+"]");
            if("".equals(getJsonVal(dataJson,"hrid"))){
                result.put("code", "-1");
                result.put("message", "【UserId:"+workcode+"】未匹配上正确的人员信息");
                return result;
            }

            WF_CREATE_USER_ID = getJsonVal(dataJson, "hrid");//流程创建人

            int requestid = createWorkflow(bill_type,dataJson,workflowId,mainConfigList,dt1_ConfigList,dt2_ConfigList);

            if (requestid <= 0) {
                result.put("code", "-1");
                result.put("message", "流程创建失败【requestid:"+requestid+"】");
                return result;
            }
            //附件处理
            if(dataJson.containsKey("Files")) {
                doFileInfo(tableName, requestid, Integer.parseInt(WF_CREATE_USER_ID), dataJson, "OutsourcingPayment_FILE_CATEGORY_ID", logicService);
            }
            result.put("code", "1");
            result.put("message", "操作成功");
            JSONObject dataObj = new JSONObject();
            dataObj.put("requestId", requestid);
            dataObj.put("txt_QuotationNo", QuotationNo);
            result.put("data", dataObj);

        }catch (Exception e){
            writeCusLog(className,"Err:"+getExceptionStr(e));
        }
        writeCusLog(className,"--------"+className+" End--------");
        return result;
    }


    /**
     * 创建流程
     * @param dataJson 主表基本信息
     * @param workflowid 流程ID
     * @param mainConfigList 主表配置
     * @param dt1_ConfigList 明细表1配置
     * @param dt2_ConfigList 明细表2配置
     * @return 流程请求ID
     */
    public int createWorkflow(String bill_type, JSONObject dataJson, String workflowid,
                              List<Map<String, String>> mainConfigList,
                              List<Map<String, String>> dt1_ConfigList,
                              List<Map<String, String>> dt2_ConfigList) {
        int reqid = 0;
        try {
            // 1. 构建基础参数
            String lastname = getJsonVal(dataJson, "lastname");
            String wfName = getWfName(workflowid);
            String nowDate = TimeUtil.getCurrentDateString();
            String requestname = String.format("%s-%s-%s", wfName, lastname, nowDate);

            // 2. 初始化主流程信息
            WorkflowRequestInfo requestInfo = buildBaseWorkflowRequestInfo(requestname, wfName, workflowid);

            // 3. 处理主表信息
            WorkflowMainTableInfo mainTableInfo = buildMainTableInfo(mainConfigList, dataJson);
            requestInfo.setWorkflowMainTableInfo(mainTableInfo);

            if (!"StandardApply".equals(bill_type)) {
                 // 4. 处理明细表信息
                handleDetailTables(bill_type,requestInfo, dataJson, dt1_ConfigList, dt2_ConfigList);
            }

            // 5. 提交流程请求
            WorkflowServiceImpl workflowService = new WorkflowServiceImpl();
            reqid = Integer.parseInt(workflowService.doCreateWorkflowRequest(
                    requestInfo, Integer.parseInt(WF_CREATE_USER_ID)));
            writeCusLog(className, String.format("[%s][reqid:%d]", requestname, reqid));

        } catch (Exception e) {
            // 完善异常日志，包含堆栈信息便于排查
            writeCusLog(className, String.format("创建流程异常: %s",getExceptionStr(e)));
            e.printStackTrace();
        }
        return reqid;
    }

    /**
     * 构建流程基础信息
     */
    private WorkflowRequestInfo buildBaseWorkflowRequestInfo(String requestname, String wfName, String workflowid) {
        WorkflowRequestInfo requestInfo = new WorkflowRequestInfo();
        requestInfo.setCanView(true);
        requestInfo.setCanEdit(true);
        requestInfo.setRequestName(requestname);
        requestInfo.setRequestLevel(REQUEST_LEVEL);
        requestInfo.setCreatorId(WF_CREATE_USER_ID);
        requestInfo.setIsnextflow(IS_NEXT_FLOW);

        WorkflowBaseInfo baseInfo = new WorkflowBaseInfo();
        baseInfo.setWorkflowId(workflowid);
        baseInfo.setWorkflowName(wfName);
        baseInfo.setWorkflowTypeName(wfName);
        requestInfo.setWorkflowBaseInfo(baseInfo);

        return requestInfo;
    }

    /**
     * 构建主表信息
     */
    private WorkflowMainTableInfo buildMainTableInfo(List<Map<String, String>> mainConfigList, JSONObject dataJson) {
        WorkflowMainTableInfo mainTableInfo = new WorkflowMainTableInfo();
        // 主表只有一条记录
        WorkflowRequestTableRecord mainRecord = new WorkflowRequestTableRecord();
        // 构建主表字段
        WorkflowRequestTableField[] mainFields = buildTableFields(mainConfigList, dataJson);
        mainRecord.setWorkflowRequestTableFields(mainFields);
        mainTableInfo.setRequestRecords(new WorkflowRequestTableRecord[]{mainRecord});
        return mainTableInfo;
    }

    /**
     * 处理明细表信息（支持明细1和明细2）
     */
    private void handleDetailTables(String bill_type, WorkflowRequestInfo requestInfo, JSONObject dataJson,
                                    List<Map<String, String>> dt1Config, List<Map<String, String>> dt2Config) {
        // 获取明细表JSON数组
        JSONArray dt1Array = new JSONArray();//仪器校正 Details4Calibrate
        JSONArray dt2Array = new JSONArray();//仪器维修 Details4Maintain
        int dt1Size = 0;
        int dt2Size = 0;
        int detailTableCount = 0;
        if(dataJson.containsKey("Details")) {
            dt1Array = getSafeJsonArray(dataJson, "Details");//付款 Details
        }else{
            if ("StandardApply".equals(bill_type)) {

            }else {
                String ApllyType = getJsonVal(dataJson,"ApllyType");
                if("0".equals(ApllyType)){
                    dt1Array = getSafeJsonArray(dataJson, "Details4Calibrate");//仪器校正 Details4Calibrate
                }else if ("1".equals(ApllyType)){
                    dt2Array = getSafeJsonArray(dataJson, "Details4Maintain");//仪器维修 Details4Maintain
                }
            }
        }

        dt1Size = dt1Array.size();
        dt2Size = dt2Array.size();
        // 计算实际需要处理的明细表数量
        if (dt1Size > 0) detailTableCount = 1;
        if (dt2Size > 0) detailTableCount = 2;

        writeCusLog(className, String.format("[明细表数量:%d]", detailTableCount));
        if (detailTableCount == 0) {
            return; // 无明细表，直接返回
        }
        // 构建明细表数组
        WorkflowDetailTableInfo[] detailTables = new WorkflowDetailTableInfo[detailTableCount];

        // 处理明细1
        if (dt1Size > 0) {
            WorkflowRequestTableRecord[] dt1Records = buildTableRecords(dt1Array, dt1Config);
            detailTables[0] = createDetailTableInfo(dt1Records);
        }

        // 处理明细2
        if (dt2Size > 0) {
            WorkflowRequestTableRecord[] dt2Records = buildTableRecords(dt2Array, dt2Config);
            detailTables[1] = createDetailTableInfo(dt2Records);
        }

        requestInfo.setWorkflowDetailTableInfos(detailTables);
    }

    /**
     * 构建表记录（通用方法：支持主表和明细表）
     * @param jsonArray 数据源JSON数组
     * @param configList 字段配置列表
     * @return 表记录数组
     */
    private WorkflowRequestTableRecord[] buildTableRecords(JSONArray jsonArray, List<Map<String, String>> configList) {
        int recordCount = jsonArray.size();
        WorkflowRequestTableRecord[] records = new WorkflowRequestTableRecord[recordCount];

        for (int i = 0; i < recordCount; i++) {
            JSONObject recordJson = jsonArray.getJSONObject(i);
            WorkflowRequestTableField[] fields = buildTableFields(configList, recordJson);

            WorkflowRequestTableRecord record = new WorkflowRequestTableRecord();
            record.setWorkflowRequestTableFields(fields);
            records[i] = record;
        }
        return records;
    }

    /**
     * 构建表字段（通用方法：支持所有表的字段构建）
     * @param configList 字段配置（oa字段与接口字段映射）
     * @param dataJson 字段值所在的JSON对象
     * @return 表字段数组
     */
    private WorkflowRequestTableField[] buildTableFields(List<Map<String, String>> configList, JSONObject dataJson) {
        int fieldCount = configList.size();//字段数量
        WorkflowRequestTableField[] fields = new WorkflowRequestTableField[fieldCount];

        for (int i = 0; i < fieldCount; i++) {
            Map<String, String> config = configList.get(i);
            String oaField = config.get("oa_field");
            String interfaceField = config.get("interface_field");
            String fieldValue = getJsonVal(dataJson, interfaceField);

            WorkflowRequestTableField field = new WorkflowRequestTableField();
            field.setFieldName(oaField);
            field.setFieldValue(fieldValue);
            field.setEdit(true);
            field.setView(true);
            fields[i] = field;
        }
        return fields;
    }

    /**
     * 创建明细表信息对象
     */
    private WorkflowDetailTableInfo createDetailTableInfo(WorkflowRequestTableRecord[] records) {
        WorkflowDetailTableInfo detailTable = new WorkflowDetailTableInfo();
        detailTable.setWorkflowRequestTableRecords(records);
        return detailTable;
    }

    /**
     * 生成附件
     * @param tableName
     * @param requestid
     * @param fileCategoryIdKey
     * @param userId
     * @param dataJson
     * @param logicService
     * @throws Exception
     */
    private void doFileInfo(String tableName, Integer requestid,Integer userId, JSONObject dataJson,String fileCategoryIdKey,DekraLogicService logicService) throws Exception{
        JSONArray fileArray = JSONArray.fromObject(dataJson.getString("Files"));
        FileOutputStream fileOut = null;
        File file = null;
        List<Integer> docIds = new ArrayList<>();
        DekraCreateDocumentService createDocumentService = new DekraCreateDocumentService();
        RecordSet rs = new RecordSet();
        User user = new User(userId);
        String fileCategoryId = logicService.getSystemConfigValue(fileCategoryIdKey, rs);
        for (int i = 0; i < fileArray.size(); i++){
            JSONObject jsonObject = (JSONObject) fileArray.get(i);
            byte[] buff= Base64.decode(jsonObject.getString("Datas"));

            String[] fileNameInfo = jsonObject.getString("Filename").split("\\.");

            String filePath = "D:\\WEAVER\\temp_files\\tw\\";
            File dir = new File(filePath);
            if (!dir.exists()) dir.mkdirs();

            String fileUrl = filePath +  fileNameInfo;
            try {
                file = new File(fileUrl);
                fileOut = new FileOutputStream(file);
                fileOut.write(buff);
                fileOut.close();
            } catch (IOException e) {
                if (fileOut != null){
                    fileOut.close();
                }
                writeCusLog(className,"IOException........fileUrl:" + fileUrl);
                e.printStackTrace();
            }
            int textDocId = createDocumentService.creatDoc(user, fileNameInfo[0], Integer.parseInt(fileCategoryId), fileNameInfo[0], fileNameInfo[1], fileUrl);

            docIds.add(textDocId);
            writeCusLog(className, "[docIds " + docIds+"]");
            if(file != null && file.exists()){
                file.delete();
            }
        }

        if (docIds.size() > 0){
            String updateSql = "update " + tableName + " set fj='" + StringUtils.join(docIds,",") + "' where requestid=" + requestid;
            rs.execute(updateSql);
        }
    }



    /**
     * 安全获取JSON数组
     */
    private JSONArray getSafeJsonArray(JSONObject json, String key) {
        // 1. 先判断json对象是否为null（避免传入null导致空指针）
        if (json == null) {
            return new JSONArray();
        }
        // 2. 判断key是否存在 + 对应值是否为null
        if (!json.containsKey(key) || json.get(key) == null) {
            return new JSONArray();
        }
        // 3. 判断值是否为JSONArray类型（避免值存在但类型不符，比如是字符串/JSONObject）
        Object value = json.get(key);
        if (!(value instanceof JSONArray)) {
            return new JSONArray();
        }
        // 所有校验通过，返回原JSONArray
        return (JSONArray) value;
    }


    /**
     * 获取流程名称
     * @param wfid
     * @return
     */
    public String getWfName(String wfid){
        String wfName = "";
        String sql = "select workflowname from  workflow_base where id = ?";
        RecordSet rs = new RecordSet();
        rs.executeQuery(sql,wfid);
        if(rs.next()){
            wfName = Util.null2String(rs.getString(1));
        }
        return wfName;
    }

    /**
     * 整理人员信息,并将人员信息整理到报文里面
     * @param json
     * @param workcode
     * @param  keyname
     * @return
     */
    public JSONObject getHrmInfo(JSONObject json,String workcode, String keyname){
        try{
            String hrid = "";
            String lastname = "";
            String subid = "";
            String deptid = "";
            String name4chineese="";
            String departmentcode = "";
            RecordSet rs = new RecordSet();
            String sel = "select * from HrmResource where workcode=?";
            rs.executeQuery(sel,workcode);
            if(rs.next()){
                hrid = Util.null2String(rs.getString("id"),"");
                lastname = Util.null2String(rs.getString("lastname"));
                subid = Util.null2String(rs.getString("subcompanyid1"));
                deptid = Util.null2String(rs.getString("departmentid"));
            }
            rs.execute("select * from cus_fielddata where id='" + hrid + "'");
            if (rs.next()) {
                name4chineese = Util.null2String(rs.getString("field4"));
            }
            rs.execute("select * from hrmdepartment where id='" + deptid + "'");
            if (rs.next()) {
                departmentcode = Util.null2String(rs.getString("departmentcode"));
            }

            //json.put("hrid",hrid);
            json.put("hrid"+keyname, hrid);//員工ID
            json.put("lastname"+keyname, lastname);//英文姓名
            json.put("subid"+keyname, subid);//公司ID
            json.put("deptid"+keyname, deptid);//部門ID
            json.put("name4chineese"+keyname, name4chineese);//中文姓名
            json.put("departmentcode"+keyname, departmentcode);//部門代碼
            //json.put("departmentname"+keyname, departmentname);//部門名稱
            //json.put("organizationname"+keyname, organizationname);//公司名稱

            return json;

        }catch(Exception e){
            writeCusLog(className,"getHrmInfo error, errormessage:" + e.getMessage());
            return json;
        }
    }




    public boolean isJsonArr(JSONObject strjson,String nodeName){
        try{
            JSONArray jsonArr = strjson.getJSONArray(nodeName);
            return true;
        }catch(Exception e){
            return false;
        }
    }

    public JSONArray getJsonArr(JSONObject strjson,String nodeName){
        JSONArray jsonArr = new JSONArray();
        try{
            jsonArr = strjson.getJSONArray(nodeName);
            return jsonArr;
        }catch(Exception e){
            return jsonArr;
        }
    }


    public String getJsonVal(JSONObject json,String field){
        String res = "";
        if(json.containsKey(field)){
            res = json.getString(field);
            if("null".equals(res)){
                res = "";
            }
        }
        return res;
    }



    /**
     * 获取堆栈中的异常信息
     * @param throwable 异常
     * @return
     */
    public static String getExceptionStr(Throwable throwable){
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter,true));
        String s = stringWriter.getBuffer().toString();
        try{
            stringWriter.close();
        }catch (Exception ignored){
            ignored.printStackTrace();
        }
        return s;
    }


    private void writeCusLog(String o, String s){
        try{
            String filename = logName + TimeUtil.getCurrentDateString() + ".log";
            String folder = GCONST.getRootPath() + "log" + File.separatorChar + logName;
            File f = new File(folder);
            if (!f.exists()) {
                f.mkdirs();
            }
            f = new File(folder + File.separatorChar + filename);
            if (!f.exists()) {
                f.createNewFile();
            }
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f, true),"UTF-8"));
            out.write("[" + o+ "][" + TimeUtil.getCurrentTimeString() + "]:" + s);//换行符 windows: \r\n  Linux：\r
            out.newLine();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
