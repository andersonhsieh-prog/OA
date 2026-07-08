package com.api.nonstandardext.dekra.service;

import weaver.conn.RecordSet;
import weaver.docs.docs.DocComInfo;
import weaver.docs.docs.DocImageManager;
import weaver.docs.docs.DocManager;
import weaver.docs.docs.DocViewer;
import weaver.file.ImageFileManager;
import weaver.general.BaseBean;
import weaver.hrm.User;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DekraCreateDocumentService extends BaseBean {

	private final static String SERVICE_NAME = "Create Document Service ";

	/**
	 * @param user : 文档创建人用户对象
	 * @param docsubject:文档标题(不带扩展名的)
	 * @param categoryid	文档目录ID
	 * @param filename	附件名称
	 * @param filepath	附件在OA应用服务器绝对路径
	 * @return 正数为成功id,-2:用户为空,-1:文档获取异常
	 * */
	public int creatDoc(User user, String docsubject, int categoryid, String filename, String fileType, String filepath){
//		writeLog(SERVICE_NAME + "-----------creatDoc----start---------userid=" + (user!=null?user.getUID():"") + ";categoryid="+categoryid+";filename="+filename+";filepath="+filepath);
		int docId =-1;
		int newimagefileid=0;

		try{
			if(user == null){
				return -2;
			}
			RecordSet rs = new RecordSet();
			InputStream input=new FileInputStream(filepath);
			if(input!=null){
				newimagefileid = saveImageFile(input, filename + "." + fileType,"processcreate");
			}
			if(newimagefileid>0){
				DocManager dm = new DocManager();
				DocComInfo dc = new DocComInfo();
				docId = dm.getNextDocId(rs);
				for (int i =0;i < 5;i++) {
					String sql="select id from DocDetail where id="+docId;
					rs.executeSql(sql);
					if(rs.next()){
						docId = dm.getNextDocId(rs);
					}else{
						break;
					}
				}
				if(docId < 0){
					return -1;
				}
				dm.resetParameter();
				dm.setId(docId);
				dm.setSeccategory(categoryid);
				dm.setDocsubject(docsubject);
				dm.setDocextendname("html");
				dm.setDocstatus("1");
				dm.setDocType(1);
				Date dateTime = new Date();
				String formatdate = new SimpleDateFormat("yyyy-MM-dd").format(dateTime);
				String formattime = new SimpleDateFormat("HH:mm:ss").format(dateTime);

				dm.setDocValidUserId(user.getUID());
				dm.setDocValidUserType(user.getType() + "");
				dm.setDocValidDate(formatdate);
				dm.setDocValidTime(formattime);

				dm.setAccessorycount(1);
				dm.setDoccreaterid(user.getUID());

				dm.setDoccreatedate(formatdate);
				dm.setDoccreatetime(formattime);
				dm.setDoclastmoddate(formatdate);
				dm.setDoclastmodtime(formattime);
				dm.setDoclastmoduserid(user.getUID());
				dm.setUserid(user.getUID());
				dm.setOwnerid(user.getUID());

				dm.setDoclangurage(user.getLanguage());
				dm.setUsertype(""+user.getLogintype());
				dm.setOwnerType("" + user.getLogintype());
				dm.setDocLastModUserType("" + user.getLogintype());
				dm.AddDocInfo();
				dm.AddShareInfo();
				dc.addDocInfoCache("" + docId);

				addDocImages(docId, newimagefileid,docsubject + "." + fileType);
				DocViewer DocViewer = new DocViewer();
				DocViewer.setDocShareByDoc("" + docId);
				rs.executeSql(" update docdetail set accessorycount = (select count(distinct id) from DocImageFile where isextfile = '1' and docid = " + docId + " and docfiletype <> '1'   and docfiletype <> '11') where id = " + docId);
			}
		}catch(Exception e){
			docId=-3;
			writeLog(SERVICE_NAME + "-----------creatDoc----Exception  ::: " + e.getMessage());
		}
		return docId;
	}

	/**
	 * 绑定附件信息
	 * @param docid
	 */
	public void addDocImages(int docid,int imagefileid, String imagefilename){
//		writeLog("addDocImages-------start---------docid=" + docid + ";imagefileid = "+imagefileid+";imagefilename = " + imagefilename);
		DocImageManager imgManger = new DocImageManager();
		imgManger.resetParameter();
		imgManger.setImagefilename(imagefilename);
		String ext = getFileExt(imagefilename);
		if (ext.equalsIgnoreCase("doc")) {
			imgManger.setDocfiletype("3");
		} else if (ext.equalsIgnoreCase("xls")) {
			imgManger.setDocfiletype("4");
		} else if (ext.equalsIgnoreCase("ppt")) {
			imgManger.setDocfiletype("5");
		} else if (ext.equalsIgnoreCase("wps")) {
			imgManger.setDocfiletype("6");
		} else if (ext.equalsIgnoreCase("docx")) {
			imgManger.setDocfiletype("7");
		} else if (ext.equalsIgnoreCase("xlsx")) {
			imgManger.setDocfiletype("8");
		} else if (ext.equalsIgnoreCase("pptx")) {
			imgManger.setDocfiletype("9");
		} else if (ext.equalsIgnoreCase("et")) {
			imgManger.setDocfiletype("10");
		} else {
			imgManger.setDocfiletype("2");
		}
		imgManger.setDocid(docid);
		imgManger.setImagefileid(imagefileid);
		imgManger.setIsextfile("1");
		imgManger.AddDocImageInfo();

		RecordSet updateRs = new RecordSet();
		updateRs.execute("update imagefile set mainimagefile=1 where imagefileid =" + imagefileid);
	}

	/**
	 * 保存文件
	 * @param fis
	 * @param fileName
	 * @param comefrom
	 * @return
	 */
	public int saveImageFile(InputStream fis,String fileName, String comefrom){

		ByteArrayOutputStream bos = null;
		try
		{
			bos = new ByteArrayOutputStream();
			byte[] b = new byte[1024];
			int n;
			while ((n = fis.read(b)) != -1)
			{
				bos.write(b, 0, n);
			}
			ImageFileManager imageFileManager=new ImageFileManager();
			imageFileManager.resetParameter();
			imageFileManager.setImagFileName(fileName);
			imageFileManager.setComefrom(comefrom);
			imageFileManager.setData(bos.toByteArray());
			return imageFileManager.saveImageFile();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(fis != null)
			{
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(bos != null)
			{
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return -1;
	}


	public void writeToLocal(String destination, InputStream input)
			throws IOException {
		int index;
		byte[] bytes = new byte[1024];
		FileOutputStream downloadFile = new FileOutputStream(destination);
		while ((index = input.read(bytes)) != -1) {
			downloadFile.write(bytes, 0, index);
			downloadFile.flush();
		}
		downloadFile.close();
		input.close();
	}

	/**
	 * 得到文档的扩展名
	 * @param file 文档全名
	 * @return 文档的扩展名
	 */
	public String getFileExt(String file) {
		if (file == null || file.trim().equals("")) {
			return "";
		} else {
			int idx = file.lastIndexOf(".");
			if (idx == -1) {
				return "";
			} else {
				if (idx + 1 >= file.length()) {
					return "";
				} else {
					return file.substring(idx + 1);
				}
			}
		}
	}


	public boolean isOfficeToDocument(String extName){
		boolean isOfficeForToDocument=false;
		if("xls".equalsIgnoreCase(extName) || "doc".equalsIgnoreCase(extName)||"wps".equalsIgnoreCase(extName)||"ppt".equalsIgnoreCase(extName)||"docx".equalsIgnoreCase(extName)||"xlsx".equalsIgnoreCase(extName)||"pptx".equalsIgnoreCase(extName)){
			isOfficeForToDocument=true;
		}
		return isOfficeForToDocument;
	}

	public InputStream getInputStreamByImagefileId(int  imagefileid){
		InputStream inputStream=null;
		if(imagefileid>0){
			ImageFileManager imageFileManager=new ImageFileManager();
			imageFileManager.getImageFileInfoById(imagefileid);
			inputStream = imageFileManager.getInputStream();
		}
		return inputStream;
	}
}
