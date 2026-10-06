package com.api.nonstandardext.dekra.action.budget;

import com.api.nonstandardext.dekra.utils.DekraUtil;
import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * select id,cocode,cocode from uf_TW_yskz where oaentry1 = '$oaentry1$' and oaentry2 = '$oaentry2$' and oaentry3 = '$oaentry3$'
 */
public class DekraBudgetCalculationService4TW extends BaseBean {

    private final static String SERVICE_NAME = "";

    //volatitle 保證修飾變量在內存中的可見性，與java內存模型配合
    //volatile關鍵字能禁止指令重排序，所以volatile能在一定程度上保證有序性
    private volatile static DekraBudgetCalculationService4TW instance = null;

    private DekraBudgetCalculationService4TW() {}

    public static DekraBudgetCalculationService4TW getInstance(){
        if(instance == null){
            //保證原子性操作
            synchronized (DekraBudgetCalculationService4TW.class){
                if(instance == null) {
                    instance = new DekraBudgetCalculationService4TW();
                }
            }
        }
        return instance;
    }

    public JSONObject frozen(String modeId, String requestId, String uuid, Map<String, BigDecimal> currentUseBudgetMap, boolean isFrozen, String wfCreateTime) {

        JSONObject result = new JSONObject();
        result.put("code", 1);
        result.put("message", "success");

        RecordSet rs = new RecordSet();

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        try {
            if (wfCreateTime != null){
                Date date = DekraUtil.parseToDate(wfCreateTime, DekraUtil.formatYYYYMMDDHHMMSS);
                cal.setTime(date);
                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH) + 1;
                this.writeLog(SERVICE_NAME + " operatedate is null, " + year + ",month:" + month + ",requestId:" + requestId);
            }

            //插入預算操作表; 預算凍結:1.更新預算凍結金額，2.更新剩餘金額
            List<List> operationBatchList = new ArrayList<List>();
            List<List> budgetBatchList = new ArrayList<List>();
            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.writeLog(SERVICE_NAME + "  currentDateTime:" + currentDateTime + ", currentUseBudgetMap size:" + currentUseBudgetMap);

            //刪除之前凍結的預算
            String opDelSql = "delete from uf_TW_budget_operate where op_workflow='" + requestId + "' and uuid='" + uuid + "'";
            this.writeLog(SERVICE_NAME + " opDelSql:" + opDelSql);
            rs.execute(opDelSql);

            for (Map.Entry<String, BigDecimal> m : currentUseBudgetMap.entrySet()) {
                List<Object> paramlist = new ArrayList<Object>();

                paramlist.add(modeId);
                paramlist.add(1);
                paramlist.add(0);
                paramlist.add(currentDateTime.substring(0, 10));
                paramlist.add(currentDateTime.substring(11));

                paramlist.add(m.getKey()); //預算

                //更新前的剩餘金額
                String budgetSql = "select * from uf_TW_yskz where zt=0 and id=" + m.getKey();
                this.writeLog(SERVICE_NAME + "  frozen key:" + m.getKey() + " value:" + m.getValue() + " budgetSql:" + budgetSql);
                rs.execute(budgetSql);
                String amountinlc = "0";
                String kyje = "0";
                if (rs.next()){
                    amountinlc = Util.null2String(rs.getString("amountinlc"));
                    if ("".equals(amountinlc)){
                        amountinlc = "0";
                    }
                    kyje = Util.null2String(rs.getString("kyje"));
                    if ("".equals(kyje)){
                        kyje = "0";
                    }
                } else {
                    result.put("code", -100);
                    result.put("message", "Amount in LC未維護，請聯繫管理員");
                    return result;
                }
                this.writeLog(SERVICE_NAME + " year : " + year + " , month : " + month + ",budgetSql:" + budgetSql);
                //operation表記錄剩餘金額

                paramlist.add(year);
                paramlist.add(requestId);
                paramlist.add(requestId);

                paramlist.add(kyje);//凍結前可用金額
                paramlist.add(m.getValue().toString());//凍結金額

                if (wfCreateTime  == null){
                    paramlist.add(DekraUtil.parseToDateString(Calendar.getInstance().getTime(), DekraUtil.formatYYYYMMDDHHMMSS));
                } else {
                    paramlist.add(wfCreateTime);
                }
                paramlist.add(0);

                paramlist.add(uuid);

                this.writeLog(SERVICE_NAME + "  paramlist :" + paramlist.toString());
                operationBatchList.add(paramlist);

                List<Object> budgetParamlist = new ArrayList<Object>();

                //佔用金額
                BigDecimal forzenFee = getForzenByYsbm(rs, m.getKey(), m.getValue(), year, "0,1", isFrozen);
                budgetParamlist.add(forzenFee.toString());

                //可用金額
                BigDecimal canUse = new BigDecimal(amountinlc).subtract(forzenFee);
                budgetParamlist.add(canUse.toString());
                if (canUse.compareTo(BigDecimal.ZERO) < 0){
                    result.put("code", -105);
                    result.put("message", "預算可用餘額不足，請檢查");
                    return result;
                }
                budgetParamlist.add(m.getKey());
                budgetParamlist.add(year);
                this.writeLog(SERVICE_NAME + "  budgetParamlist :" + budgetParamlist.toString());
                budgetBatchList.add(budgetParamlist);

            }
            this.writeLog(SERVICE_NAME + "  operationBatchList size :" + operationBatchList.size());
            this.writeLog(SERVICE_NAME + "  budgetBatchList size :" + budgetBatchList.size());
//            RecordSetTrans rsTrans = new RecordSetTrans();
//            this.writeLog(SERVICE_NAME + "  rsTrans :" + rsTrans);
//            rsTrans.setAutoCommit(true);
            this.writeLog(SERVICE_NAME + "  result :" + result);

            //14個參數
            String opInsertSql = "insert into uf_TW_budget_operate (formmodeid, modedatacreater, modedatacreatertype, modedatacreatedate, modedatacreatetime, ysbm, nd, op_workflow, op_requestid, before_frozen, frozen_fee, frozen_time, zt, uuid) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            this.writeLog(SERVICE_NAME + " v2 opInsertSql :" + opInsertSql);
            boolean iR = rs.executeBatchSql(opInsertSql, operationBatchList);
            this.writeLog(SERVICE_NAME + " v2 iR :" + iR);

            String updateForzenSql = "update uf_TW_yskz set zyje=?,kyje=? where id=? and nd=?";

            this.writeLog(SERVICE_NAME + " v2 updateForzenSql :" + updateForzenSql);

            boolean uR = rs.executeBatchSql(updateForzenSql, budgetBatchList);

            this.writeLog(SERVICE_NAME + " v2 uR :" + uR);
        } catch (Exception e) {
            this.writeLog(SERVICE_NAME + " forzen exception message :" + e.getMessage());
            e.printStackTrace();
            result.put("code", -5);
            result.put("message", "預算扣減異常，請聯繫管理員 " + e.getMessage());
            return result;
        }
        return result;
    }

    /**
     * 按預算編碼統計凍結預算金額
     * @param rs
     * @param ysx
     * @param accountIncurred
     * @param year
     * @param status
     * @param isFrozen
     * @return
     */
    public BigDecimal getForzenByYsbm(RecordSet rs, String ysx, BigDecimal accountIncurred, Integer year, String status, boolean isFrozen) {
        String opSql = "select sum(frozen_fee) as forzen from uf_TW_budget_operate where zt in (" + status + ") and ysbm=" + ysx + " and nd=" + year;
        rs.execute(opSql);
        String forzen = "";
        if (rs.next()){
            forzen = Util.null2String(rs.getString("forzen"));
        }
        if ("".equals(forzen)){
            forzen = "0";
        }
        this.writeLog(SERVICE_NAME + "  getForzenByMonth forzen:" + forzen);

        if (isFrozen){
            return new BigDecimal(forzen).add(accountIncurred); //加人凍結金額
        } else {
            return new BigDecimal(forzen); //不加入到預提凍結
        }
    }

    public JSONObject release(String requestId, String uuid, Map<String, BigDecimal> currentUseBudgetMap, boolean isFrozen, String wfCreateTime) {

        RecordSet rs = new RecordSet();

        JSONObject result = new JSONObject();
        result.put("code", 1);
        result.put("message", "success");

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        try {
            if (!"".equals(wfCreateTime) && wfCreateTime != null){
                Date date = DekraUtil.parseToDate(wfCreateTime, DekraUtil.formatYYYYMMDDHHMMSS);
                cal.setTime(date);
                year = cal.get(Calendar.YEAR);
                month = cal.get(Calendar.MONTH) + 1;
                this.writeLog(SERVICE_NAME + " operatedate is null, " + year + ",month:" + month + ",requestId:" + requestId);
            }

            //刪除操作記錄; 預算釋放:1.更新預算凍結金額，2.更新剩餘金額
            List<List> operationBatchList = new ArrayList<List>();
            List<List> budgetBatchList = new ArrayList<List>();
            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.writeLog(SERVICE_NAME + "  currentDateTime:" + currentDateTime + ", uuid:" + uuid + ", currentUseBudgetMap:" + currentUseBudgetMap.size() + ", isFrozen:" + isFrozen);
            for (Map.Entry<String, BigDecimal> m : currentUseBudgetMap.entrySet()) {
                String checkSql = "select * from uf_TW_budget_operate where op_workflow=" + requestId + " and uuid='" + uuid + "'";
                this.writeLog(SERVICE_NAME + "  release budget:" + m.getKey() + ", value:" + m.getValue() + ",  checkSql:" + checkSql);
                rs.execute(checkSql);
                BigDecimal realseFee = m.getValue();
                if (!rs.next()){
                    realseFee = BigDecimal.ZERO;
                }

                //刪除參數
                List<Object> paramlist = new ArrayList<Object>();
                paramlist.add(requestId);
                paramlist.add(uuid);
                operationBatchList.add(paramlist);

                //更新前的剩餘金額
                String budgetSql = "select * from uf_TW_yskz where zt=0 and id=" + m.getKey();
                this.writeLog(SERVICE_NAME + "  frozen key:" + m.getKey() + " value:" + m.getValue() + " budgetSql:" + budgetSql);
                rs.execute(budgetSql);
                String amountinlc = "0";
                if (rs.next()){
                    amountinlc = Util.null2String(rs.getString("amountinlc"));
                    if ("".equals(amountinlc)){
                        amountinlc = "0";
                    }
                }

                this.writeLog(SERVICE_NAME + "  release budget:" + m.getKey() + ", requestId:" + requestId + ", amountinlc:" + amountinlc + " , year : " + year + " , month : " + month);

                List<Object> budgetParamlist = new ArrayList<Object>();

                //凍結金額
                BigDecimal forzenFee = getForzenByYsbm(rs, m.getKey(), realseFee.multiply(new BigDecimal("-1")), year, "0,1", true);
                budgetParamlist.add(forzenFee.toString());

                //可用金額
                budgetParamlist.add(new BigDecimal(amountinlc).subtract(forzenFee).toString());

                budgetParamlist.add(m.getKey());
                budgetParamlist.add(year);

                budgetBatchList.add(budgetParamlist);

            }
            this.writeLog(SERVICE_NAME + "  operationBatchList size :" + operationBatchList.size());
            this.writeLog(SERVICE_NAME + "  budgetBatchList size :" + budgetBatchList.size());

//            RecordSetTrans rsTrans = new RecordSetTrans();
//            this.writeLog(SERVICE_NAME + "  rsTrans :" + rsTrans);
//            rsTrans.setAutoCommit(true);
//            this.writeLog(SERVICE_NAME + "  result :" + result);

            String opDelSql = "delete from uf_TW_budget_operate where op_workflow=? and uuid=?";
            this.writeLog(SERVICE_NAME + "  opDelSql :" + opDelSql);
            boolean dR = rs.executeBatchSql(opDelSql, operationBatchList);
            this.writeLog(SERVICE_NAME + " v2 dR :" + dR);

            String updateForzenSql = "update uf_TW_yskz set zyje=?,kyje=? where id=? and nd=?";
            boolean uR = rs.executeBatchSql(updateForzenSql, budgetBatchList);
            this.writeLog(SERVICE_NAME + " v2 uR :" + uR);
        } catch (Exception e) {
            this.writeLog(SERVICE_NAME + " release exception message :" + e.getMessage());
            e.printStackTrace();
            result.put("code", -105);
            result.put("message", "預算扣減異常，請聯繫管理員,Message:" + e.getMessage());
            return result;
        }
        return result;
    }
}
