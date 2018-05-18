package com.zhou;

import java.io.*;
import java.util.*;

import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Util;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.javatuples.Pair;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.StringConstant;
import soot.options.Options;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;


class InstrumentUnit
{
	Body body=null;
	Unit point=null;
	String message=null;
	public InstrumentUnit(Body body, Unit unit, String appPath) {
		super();
		this.body = body;
		this.point = unit;
		this.message = appPath;
	}
	
	
}

public class InstrumentAPPBeforePermissionInvoke extends BodyTransformer {

	@Override
	protected void internalTransform(Body b, String phaseName, Map<String, String> options) {

		if(Util.isLibraryClass(b.getMethod().getBytecodeSignature()))
		{
			return;
		}

		// TODO Auto-generated method stub
//		System.out.println("************************************************");
//		System.out.println("phaseName=" + phaseName);
//
//
//		options.forEach((key, value)->{
//			System.out.println("key="+key+"--value="+value);
//		});


		PatchingChain<Unit> units = b.getUnits();


		List<InstrumentUnit> instrumentUnits=new ArrayList<InstrumentUnit>();
		for(Unit unit:units)
		{


//			for(Tag tag:unit.getTags())
//			{
//				System.out.println(tag.getName()+"zzzzzzzzzzz");
//			}


			if(unitNeedAnalysis(unit,b.getMethod()))
			{
	              instrumentUnits.add(new InstrumentUnit(b,unit,appPath));
			}


		}

		for(InstrumentUnit instrumentUnit: instrumentUnits)
		{
			addInstrumentBeforeStatement(instrumentUnit.body, instrumentUnit.point, instrumentUnit.message);
		}

	}

	/**
	 * appPath是应用的绝对路径
	 * platforms是SDK中platforms的路径	 
	 */
	//static String defaultAppPath = "/home/lab418/AndroidStudioProjects/TestIntrument3/app/build/outputs/apk/debug/app-debug.apk";
	private String appPath="./InstrumentAPK/sms2.apk";
	//private String platforms = "/home/zms/platforms";//设置最低版本为android5.0，app就插桩失败  签名的jarsigner不行了？soot太老了？

	private Set<Pair<Integer, String>> targets = null;

	private static BufferedWriter bufferedWriter=null;
	private static BufferedWriter bufferedWriter_app_has_Instrumented=null;

	private static volatile int inStrumentCount=0;

	private static int targetCount=0;


	public InstrumentAPPBeforePermissionInvoke(String appPath,String targetsFile) {
		this.appPath=appPath;

		targets=new LinkedHashSet<Pair<Integer, String>>();
		try{
			BufferedReader bufferedReader=new BufferedReader(new FileReader(targetsFile));

			String content=null;
			while ((content=bufferedReader.readLine())!=null)
			{

				String []str=content.split("#");
				String methodString=str[0];
				String byteTag=str[1];

				targets.add(new Pair<>(Integer.valueOf(byteTag),methodString));

			}

			targetCount=targets.size();
			inStrumentCount=0;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}

	private   boolean unitNeedAnalysis(Unit unit, SootMethod sootMethod) {
		BytecodeOffsetTag tag = Util.extractByteCodeOffset(unit);
		if (tag == null) {
			return false;
		}

		for (Pair<Integer, String> item : targets) {
			if (item.getValue0() == tag.getBytecodeOffset() && item.getValue1().equals(sootMethod.getBytecodeSignature())) {
				return true;
			}

		}
		return false;
	}
	public static void main(String[] args) {


		try
		{bufferedWriter_app_has_Instrumented = new BufferedWriter(new FileWriter("app_has_Instrumented.txt" ));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}


		File dir=new File("/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/down_fdroid_app_from_androzoo/f-droid-app");
		for(File file:dir.listFiles()) {
			if (file.getName().endsWith(".apk")) {
				File unitedAnalysis = new File(file.getAbsolutePath() + "_UnitsNeedAnalysis.txt");
				if (unitedAnalysis.exists()) {

					Thread childThread = new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub

							Long startTime = System.nanoTime();
							args[0]=file.getAbsolutePath();
							args[1]="/home/zms/platforms";
							args[2]=unitedAnalysis.getAbsolutePath();

							soot.G.reset();
							SingleAPPAnalysis(args);

							Long stopTime = System.nanoTime();
							System.out.println("运行时间:" + ((stopTime - startTime) / 1000 / 1000 / 1000 / 60) + "分钟");
						}
					});
					childThread.start();

					try {
						childThread.join();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}



				}

			}
		}

		try {
			bufferedWriter_app_has_Instrumented.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private static void SingleAPPAnalysis(String[] args) {
		String sootArgs[] = {
				"-process-dir",args[0],
				"-android-jars",args[1],
				"-allow-phantom-refs",


		};
		//prefer Android APK files// -src-prec apk
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_keep_line_number(true);
		Options.v().set_coffi(true);
		Options.v().set_keep_offset(true);

		//output as APK, too//-f J
		Options.v().set_output_format(Options.output_format_dex);
		Options.v().setPhaseOption("jb", "use-original-names:true");
		Options.v().set_force_overwrite(true);

		Scene.v().addBasicClass("android.util.Log",SootClass.SIGNATURES);


		try {
			bufferedWriter=new BufferedWriter(new FileWriter(args[0]+"_instrument.log"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		InstrumentAPPBeforePermissionInvoke instrumentAPPBeforePermissionInvoke=new InstrumentAPPBeforePermissionInvoke(args[0],args[2]);

		PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", instrumentAPPBeforePermissionInvoke));
		//PackManager.v().getPack("jtp").apply();


		soot.Main.main(sootArgs);
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		if(targetCount!=inStrumentCount)
		{
			throw  new RuntimeException("插桩数量和需要插桩的数量不匹配！");
		}
		else
		{

			try {
				bufferedWriter_app_has_Instrumented.write(args[0]+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public  void addInstrumentAfterStatement(Body b, Unit point,String message){
			

	        //insert a log.i instrument statement
			
			Scene.v().forceResolve("android.util.Log",SootClass.SIGNATURES);
			
			SootMethod log = Scene.v().getMethod("<android.util.Log: int i(java.lang.String,java.lang.String)>");
			Value logMessage = StringConstant.v("#Instrument#"+ message+
					 "#method#"+b.getMethod().getSignature()+"#unitPoint_after#"+point.toString()+"#lineNumber#"+point.getJavaSourceStartLineNumber());
			Value logType = StringConstant.v("ZMSInstrument");
			Value logMsg = logMessage;
	         //make new static invokement
			StaticInvokeExpr newInvokeExpr = Jimple.v().newStaticInvokeExpr(log.makeRef(), logType, logMsg);
	        // turn it into an invoke statement
	      
			InvokeStmt invokeStmt=Jimple.v().newInvokeStmt(newInvokeExpr);
	        
	        b.getUnits().insertAfter(invokeStmt,point );
	        //check that we did not mess up the Jimple
	        b.validate();
	        inStrumentCount =inStrumentCount+1;
	        try {
				bufferedWriter.write(logMessage.toString()+"\n");
				bufferedWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	       
		}
public  void addInstrumentBeforeStatement(Body b, Unit point,String message){
			

	        //insert a log.i instrument statement
			
			Scene.v().forceResolve("android.util.Log",SootClass.SIGNATURES);
			
			SootMethod log = Scene.v().getMethod("<android.util.Log: int i(java.lang.String,java.lang.String)>");
			Value logMessage = StringConstant.v("#Instrument#"+ message+
					 "#method#"+b.getMethod().getSignature()+"#unitPoint_before#"+point.toString()+"#lineNumber#"+point.getJavaSourceStartLineNumber());
			Value logType = StringConstant.v("ZMSInstrument");
			Value logMsg = logMessage;
	         //make new static invokement
			StaticInvokeExpr newInvokeExpr = Jimple.v().newStaticInvokeExpr(log.makeRef(), logType, logMsg);
	        // turn it into an invoke statement
	      
			InvokeStmt invokeStmt=Jimple.v().newInvokeStmt(newInvokeExpr);
	        
	        b.getUnits().insertBefore(invokeStmt,point );
	        //check that we did not mess up the Jimple
	        b.validate();
			inStrumentCount =inStrumentCount+1;
	        try {
				bufferedWriter.write(logMessage.toString()+"\n");
				bufferedWriter.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	       
		}

}
