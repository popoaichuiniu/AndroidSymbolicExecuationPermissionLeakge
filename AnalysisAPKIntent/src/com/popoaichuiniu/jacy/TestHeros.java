package com.popoaichuiniu.jacy;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import heros.solver.Pair;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.DefinitionStmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.exampleproblems.IFDSReachingDefinitions;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

public class TestHeros {
	
	
	public static String androidPlatformPath = "/home/zms/platforms";

	
	private static String appPath = "./testSoot5.apk";

	public static void main(String[] args) {
		
		
		AndroidCallGraphHelper androidCallGraphHelper=new AndroidCallGraphHelper(appPath, androidPlatformPath) ;
		
		JimpleBasedInterproceduralCFG jimpleBasedInterproceduralCFG=new JimpleBasedInterproceduralCFG() ;
		
		SootMethod entryPoint=androidCallGraphHelper.getEntryPoint();
		
		 Iterator<Edge> iterator=androidCallGraphHelper.getCg().edgesOutOf(entryPoint);
		
		System.out.println("****************************************************");
		
		for(;iterator.hasNext();)
		{
			SootMethod sootMethod=iterator.next().getTgt().method();
			if(!sootMethod.getDeclaringClass().getName().startsWith("android."))
			{
				
				
				
				Set<Unit> set=jimpleBasedInterproceduralCFG.getCallsFromWithin(sootMethod);//找到此方法中调用的语句
				
				
				
				for(Unit unit:set)
				{
					System.out.println("11111111111111111111111111111111");
					System.out.println(unit);				
					
					Collection<SootMethod> sootMethods=jimpleBasedInterproceduralCFG.getCalleesOfCallAt(unit);//找到此语句中被调用的方法
					System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
					for(SootMethod temp:sootMethods)
					{
						System.out.println(jimpleBasedInterproceduralCFG.getCallersOf(temp));//找到调用此方法的语句
					}
					System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
					System.out.println("222222222222222222222222222222222222");
				}
			}
			
		}
		
		
//		 IFDSTabulationProblem<Unit, Pair<Value,
//		 Set<DefinitionStmt>>, SootMethod,InterproceduralCFG<Unit,
//		 SootMethod>> problem = new IFDSReachingDefinitions(icfg);
//
//		IFDSSolver<Unit,Pair<Value,Set<DefinitionStmt>>,SootMethod,
//
//		  
//		 InterproceduralCFG<Unit,SootMethod>> solver = new IFDSSolver<Unit,Pair<Value, Set<DefinitionStmt>>,SootMethod,
//
//		  
//		 InterproceduralCFG<Unit,SootMethod>>(problem); 
//
//		  solver.solve();
//		
		
		// TODO Auto-generated method stub

	}

}
