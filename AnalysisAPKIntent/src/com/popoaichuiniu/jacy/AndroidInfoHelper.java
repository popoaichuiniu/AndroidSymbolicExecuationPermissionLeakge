package com.popoaichuiniu.jacy;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.axml.AXmlAttribute;
import soot.jimple.infoflow.android.axml.AXmlNode;

public class AndroidInfoHelper {
	
	
	//private static String permissionMappingFilePath="./AnalysisAPKIntent/mapping_5.1.1.csv";//API=22  这个csv有问题,还有一个是别人修正的
	
	//private static String jellybean_allmappings="jellybean_allmappings.txt";//API=16,这个太老
	
	private static String permission_25="./AnalysisAPKIntent/permissions_25.json";//API=25 androguard提供
	private static String dangerousOrSpecailPermissionFilePath="./AnalysisAPKIntent/dangerousOrSpecialPermission_lose.txt";//存在API>=26


	private static Map<String, List<String>> permissionMethods= null;
	private static Map<String, List<String>> permissionAndroguardMethods= null;

	public static Map<String, List<String>> getPermissionAndroguardMethods() {
		return permissionAndroguardMethods;
	}

	//private static Map<String, String> permissionDangerousAndSpecialMethods1= null;//因为permissionMappingFilePath="mapping_5.1.1.csv"，这个不对，所有这个被丢弃

	private static Map<String, List<String>> permissionDangerousAndSpecialMethodsUltimulateString = null;//被危险或者特殊权限保护起来的API
	private String appPath = null;
	private Map<String, AXmlNode> EAs = null;
	private Map<String, List<String>> EAProtctedPermission = null;//EA的permission


	private BufferedWriter androidAPKException=null;

	private List<String> exceptionAndroidInfoList =null;
	
	public Map<String, List<String>> getEAProtctedPermission() {
		if(EAProtctedPermission==null)
		{
			caculatePermissionProtectedEAs();
		}
		return EAProtctedPermission;
	}

	private List<String> string_EAs = null;
	private Map<String, AXmlNode> components = null;
	private Map<String, Map<AXmlNode, String>> permissionProtectedEAs = null;
	private Map<String, Map<AXmlNode, String>> permissionProtectedComponents = null;
	
	static {
		processJsonPermissionmapping();
	}
	public Map<String, AXmlNode> getComponents() {
		return components;
	}




	public  Map<String, List<String>> getPermissionMethod() {
		return permissionMethods;
	}

	public String getAppPath() {
		return appPath;
	}

	public AndroidInfoHelper(String appPath) {
		super();
		this.appPath = appPath;
		exceptionAndroidInfoList =new ArrayList<>();
		calcaulateEAs();
		caculatePermissionProtectedEAs();
		caculatePermissionProtectedComponents();

		if(exceptionAndroidInfoList.size()>0)
		{


			try {

				androidAPKException = new BufferedWriter(new FileWriter("app-exception/androidAPKInfo/"+appPath.replaceAll("/|\\.", "_")+"_androidAPKInfo.txt"));


				for(String exception: exceptionAndroidInfoList)
				{
					androidAPKException.write(exception);
				}


				androidAPKException.close();

			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}






	}

	public String getPackageName(String appPath)
	{

		try {
			MyProcessManifest processMan = new MyProcessManifest(appPath);
			return processMan.getPackageName();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;

	}

//	public static Map<String, String> getPermissionDangerousAndSpecialMethods1() {
//		return permissionDangerousAndSpecialMethods1;
//	}

	private void calcaulateEAs() {
		
		try {
			MyProcessManifest processMan = new MyProcessManifest(appPath);
			EAs = new HashMap<String, AXmlNode>();
			string_EAs=new ArrayList<String>();

			components = processMan.getComponentClasses();

			for (Iterator<Entry<String, AXmlNode>> iterator = components.entrySet().iterator(); iterator.hasNext();) {
				String componentName = iterator.next().getKey();
				AXmlNode node = components.get(componentName);
				if (node == null) {
					throw new RuntimeException("AndroidMainifest文件节点为空！");
				}
				if (judgeEA(node)) {
					// System.out.println(componentName);
					// System.out.println(node.getTag());
					EAs.put(componentName, node);
					string_EAs.add(componentName);

				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			PermissionEscalation.appLogger.error(appPath, e);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			PermissionEscalation.appLogger.error(appPath, e);
		}

		

	}

	public Map<String, AXmlNode> getEAs() {
		return EAs;
	}
	
	public List<String> getString_EAs() {
		return string_EAs;
	}

	public Map<String, Map<AXmlNode, String>> getPermissionProtectedEAs() {
		return permissionProtectedEAs;
	}

	public Map<String, Map<AXmlNode, String>> getPermissionProtectedComponents() {
		return permissionProtectedComponents;
	}

	private boolean judgeEA(AXmlNode node) {

		AXmlAttribute<Boolean> exported = (AXmlAttribute<Boolean>) node.getAttribute("exported");

		if (exported != null) {
			
			if(exported.getValue() instanceof Boolean)
			{
				if (exported.getValue()) {
					return true;

				} else {
					return false;
				}
			}
			else
			{
				//存在异常


				exceptionAndroidInfoList.add(node+"exported属性异常！:"+node.getAttribute("exported")+"\n");



			}
			
		}

		if (node.getChildrenWithTag("intent-filter").size() > 0) {
			return true;
		} else {
			return false;
		}

	}
	
	

	public static void processJellyBean() {
//		RandomAccessFile randomAccessFile=null;
//		try {
//			randomAccessFile=new RandomAccessFile(new File(jellybean_allmappings), "r");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
	}
//	public static void processPermissionCSVMapFile() {
//		permissionMethods=new HashMap<String,List<String>>();
//		permissionDangerousAndSpecialMethods1=new HashMap<String,String>();
//		List<String> dangerousAndSpecialPermissions=getDangerousOrSpecailPermission();
//		CsvReader reader = null;
//		try {
//			reader = new CsvReader(permissionMappingFilePath);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			PermissionEscalation.errorRunLogger.error("processPermissionCSVMapFile()",e);
//		}
//
//		try {
//			reader.readHeaders();
//			while (reader.readRecord()) {
//
//				//csvList.add(reader.getValues()); // 按行读取，并把每一行的数据添加到list集合
//
//				String methodSignature="<"+reader.get("CallerClass").replaceAll("/", ".")+": "+reader.get("CallerMethod")+reader.get("CallerMethodDesc")+">";
//				String methodPermission=reader.get("Permission");
//				//System.out.println("*"+methodSignature+"*");
//				//System.out.println("*"+methodPermission+"*");
//
//				List<String> arr=permissionMethods.get(methodSignature);
//				if(arr==null)
//				{
//					arr=new ArrayList<String>();
//					arr.add(methodPermission);
//					permissionMethods.put(methodSignature,arr);
//
//				}
//				else
//				{
//
//					arr.add(methodPermission);
//
//
//
//				}
//
//				///**************************************************
//				if(dangerousAndSpecialPermissions.contains(methodPermission))
//				{
//					permissionDangerousAndSpecialMethods1.put(methodSignature, methodPermission);
//				}
//
//
//			}
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			PermissionEscalation.errorRunLogger.error("processPermissionCSVMapFile()",e);
//		}
//		reader.close();
////		System.out.println("*************************PermissionMap****************************");
////		for (Iterator<Map.Entry<String,List<String>>> iterator=permissionMethods.entrySet().iterator();iterator.hasNext();)
////		{
////			Map.Entry<String,List<String>> permissionMap=iterator.next();
////			if(permissionMap.getValue().size()>1)
////			{
////				System.out.println(permissionMap.getValue().size()+permissionMap.getKey());
////				System.out.println(permissionMap.getValue());
////			}
////		}
////		System.out.println("*************************PermissionMap****************************");
//
//
//	}
	private static String convertAndroGuardMethodSignatureToSoot(String methodSignature) {
		//"Landroid/telephony/SmsManager;-sendTextMessage-(Ljava/lang/String; Ljava/lang/String; Ljava/lang/String; Landroid/app/PendingIntent; Landroid/app/PendingIntent;)V"
		//<android.telephony.SmsManager: sendTextMessage(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Landroid/app/PendingIntent;Landroid/app/PendingIntent;)V>
		if(methodSignature==null)
		{
			return null;
		}
		else
		{
			String temp=methodSignature.replaceAll("-", "");
			String packageStr=temp.substring(0, temp.indexOf(";")).substring(1).replaceAll("/", ".")+": ";
			
			return "<"+packageStr+temp.substring(temp.indexOf(";")+1).replace(" ", "")+">";
			
		}
		
		
		
	}
	public static void processJsonPermissionmapping() {
		permissionAndroguardMethods=new HashMap<>();
		permissionDangerousAndSpecialMethodsUltimulateString =new HashMap<String,List<String>>();
		System.out.println(new File(".").getAbsolutePath());
		List<String> dangerousAndSpecialPermissions=getDangerousOrSpecailPermission();
		File jsonFile=new File(permission_25);
        String content = null;
		try {
			content = FileUtils.readFileToString(jsonFile,"UTF-8");
			JSONObject jsonObject=new JSONObject(content);
			System.out.println("**********************JsonPermissionmapping********************************");
			for(Iterator<String> iterator=jsonObject.keys();iterator.hasNext();)
			{
				String key=iterator.next();
				//System.out.println(key);
				//System.out.println(jsonObject.getJSONArray(key));
				JSONArray permissionArray= jsonObject.getJSONArray(key);
				List<String> permissionList=new ArrayList<>();
				if(permissionArray!=null)
				{
					
					for(Iterator<Object> permissionIterator= permissionArray.iterator();permissionIterator.hasNext();)
					{
						permissionList.add((String)permissionIterator.next());
					}
				}				
				String modifiedKey=convertAndroGuardMethodSignatureToSoot(key);
				System.out.println(modifiedKey);
				System.out.println(permissionList);
				permissionAndroguardMethods.put(modifiedKey, permissionList);
				
				//*********************permissionDangerousAndSpecialMethods2*************************
				for(String permission:permissionList)
				{
					if(dangerousAndSpecialPermissions.contains(permission))
					{
						System.out.println("***************dangerousAndSpecialPermissions******************");
						System.out.println(modifiedKey);
						System.out.println("****************dangerousAndSpecialPermissions*****************");
						List<String> arrPermission= permissionDangerousAndSpecialMethodsUltimulateString.get(modifiedKey);
						if(arrPermission==null)
						{
							arrPermission=new ArrayList<>();
							arrPermission.add(permission);
							permissionDangerousAndSpecialMethodsUltimulateString.put(modifiedKey, arrPermission);
						}
						else
						{
							arrPermission.add(permission);
						}
						
					}
				}
				
				
				
			}
			System.out.println("**********************JsonPermissionmapping********************************");
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			PermissionEscalation.errorRunLogger.error("JsonPermissionmapping()",e);
		}
        
		
	}
	
	public static Map<String, List<String>> getPermissionDangerousAndSpecialMethods2() {
		if(permissionDangerousAndSpecialMethodsUltimulateString ==null)
		{
			processJsonPermissionmapping();
		}
		return permissionDangerousAndSpecialMethodsUltimulateString;
	}

	private static List<String> getDangerousOrSpecailPermission() {
		
		
		List<String>  dangerousOrSpecialPermissions=new ArrayList<>();
		BufferedReader bufferedReader=null;
		try {
			bufferedReader=new BufferedReader(new FileReader(dangerousOrSpecailPermissionFilePath));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			PermissionEscalation.errorRunLogger.error("getDangerousOrSpecailPermission()",e);
			
		}
		
		String line=null;
		try {
			while ((line=bufferedReader.readLine())!=null)
			{ String permission=line.split(" ")[0];
			
			System.out.println("dangerousOrSpecialPermission :"+"****"+permission);
			
			 dangerousOrSpecialPermissions.add(permission);
			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			PermissionEscalation.errorRunLogger.error("getDangerousOrSpecailPermission()",e);
		}
	
		return dangerousOrSpecialPermissions;
	}
	

	public void caculatePermissionProtectedEAs() {
		
		if(EAs==null)
		{
			calcaulateEAs();
		}
		permissionProtectedEAs = new HashMap<String, Map<AXmlNode, String>>();
		
		EAProtctedPermission=new HashMap<>();
		for (Iterator<Map.Entry<String, AXmlNode>> iterator = EAs.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, AXmlNode> eaNode = iterator.next();
			String componentName = eaNode.getKey();

			AXmlNode eaNodeAXMlValue = eaNode.getValue();
			// System.out.println(eaNode.getAttribute("permission"));
			AXmlAttribute<?> permissionAttribute = eaNodeAXMlValue.getAttribute("permission");//android:permission只会有一个
			if (permissionAttribute != null) {
				String value = (String) permissionAttribute.getValue();
				System.out.println(value);
				Map<AXmlNode, String> temp = new HashMap<AXmlNode, String>();
				temp.put(eaNodeAXMlValue, value);
				permissionProtectedEAs.put(componentName, temp);
				
				List<String>  permissionList=new ArrayList<>();
				permissionList.add(value);
				EAProtctedPermission.put(componentName,permissionList);//保护EA的权限
			}

		}

	}

	public void caculatePermissionProtectedComponents() {//allComponents
		
		if(components==null)
			return;
		permissionProtectedComponents = new HashMap<String, Map<AXmlNode, String>>();
		for (Iterator<Map.Entry<String, AXmlNode>> iterator = components.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, AXmlNode> compoNode = iterator.next();
			String componentName = compoNode.getKey();

			AXmlNode componentNodeAXMlValue = compoNode.getValue();
			// System.out.println(eaNode.getAttribute("permission"));
			AXmlAttribute<?> permissionAttribute = componentNodeAXMlValue.getAttribute("permission");
			if (permissionAttribute != null) {
				String value = (String) permissionAttribute.getValue();
				System.out.println(value);
				Map<AXmlNode, String> temp = new HashMap<AXmlNode, String>();
				temp.put(componentNodeAXMlValue, value);
				permissionProtectedComponents.put(componentName, temp);
			}

		}

	}

}
