package com.popoaichuiniu.test;

import java.util.Iterator;

import javax.management.InstanceAlreadyExistsException;

import org.apache.log4j.Logger;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;


import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.asm.backend.targets.InstanceOfCasts;
import soot.jimple.*;
import soot.jimple.infoflow.android.data.AndroidMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

public class TestSootMethod {
	
	private static String androidPlatformPath = "/home/zms/platforms";
	
	public static Logger appLogger = null;// 应用日志
	
	public static void main(String[] args) {
		
		
		AndroidCallGraphHelper androidCallGraphHelper=new AndroidCallGraphHelper("/home/zms/android_project/Camera/TestWebView2/app/build/outputs/apk/debug/app-debug.apk", androidPlatformPath);
		
		CallGraph cGraph=androidCallGraphHelper.getCg();
		SootMethod entryPoint = androidCallGraphHelper.getEntryPoint();
		
		
		for (Iterator<Edge> iterator = cGraph.edgesOutOf(entryPoint); iterator.hasNext();)// DummyMain方法调用的所有方法（各个组件的回调方法和生命周期）
		{
			SootMethod sootMethod=iterator.next().tgt();
			//sootMethod.getDeclaration();		
		
			
			
			if(sootMethod.getDeclaration().contains("onCreate")&&sootMethod.getDeclaringClass().getName().contains("Main2Activity"))
			{
				System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
				
				System.out.println(sootMethod.getActiveBody().toString());
				System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
				
				UnitGraph unitGraph=new BriefUnitGraph(sootMethod.getActiveBody());
				SimpleLocalDefs defs=new SimpleLocalDefs(unitGraph);
				
				System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
				
				for(Iterator<Unit> iterator2=unitGraph.iterator();iterator2.hasNext();)
				{
					Unit unit=iterator2.next();

					if(unit instanceof IfStmt)
					{
						System.out.println(unit.toString());
						Value con1=((ConditionExpr)(((IfStmt)unit).getCondition())).getOp1();

						System.out.println(defs.getDefsOfAt((Local)con1,unit));
					}









//					if(unit instanceof InvokeStmt)
//					{
//						//System.out.println(unit.toString());
//
//						InvokeExpr invokeExpr=((InvokeStmt)unit).getInvokeExpr();
//						if(invokeExpr.getArgCount()<=0)
//							continue;
//
//						Value value=invokeExpr.getArg(0);
//						if(value instanceof Local)
//						{
//							System.out.println("1111111111111111111111111");
//							System.out.println(defs.getDefsOfAt((Local) value, unit));
//							System.out.println("1111111111111111111111111");
//
//
//						}
//
//
//
//					}
//					System.out.println(unit.getUseBoxes());
					
//					for(ValueBox valueBox:unit.getDefBoxes())
//					{
//						if (valueBox.getValue() instanceof Local)
//						{
//							System.out.println(((Local)valueBox.getValue()).getName());
//							System.out.println(((Local)valueBox.getValue()).getNumber());
//						}
//						else {
//							System.out.println("uuuuuuuuuuuuuuuuuuuuuu");
//						}
//					}
				//	System.out.println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
				}
				
				System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
					
			}
		}
		
		
	}

}
