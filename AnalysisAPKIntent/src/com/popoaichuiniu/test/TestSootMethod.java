package com.popoaichuiniu.test;

import java.util.*;

import com.popoaichuiniu.util.Config;
import org.apache.log4j.Logger;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;


import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.*;

public class TestSootMethod {
	
	private static String androidPlatformPath = "/home/zms/platforms";
	
	public static Logger appLogger = null;// 应用日志
	
	public static void main(String[] args) {
		
		
		AndroidCallGraphHelper androidCallGraphHelper=new AndroidCallGraphHelper(Config.defaultAppPath, androidPlatformPath);
		
		CallGraph cGraph=androidCallGraphHelper.getCg();
		SootMethod entryPoint = androidCallGraphHelper.getEntryPoint();
		
		
		for (Iterator<Edge> iterator = cGraph.edgesOutOf(entryPoint); iterator.hasNext();)// DummyMain方法调用的所有方法（各个组件的回调方法和生命周期）
		{
			SootMethod sootMethod=iterator.next().tgt();




		
			
			
			if(sootMethod.getDeclaration().contains("onCreate")&&sootMethod.getDeclaringClass().getName().contains("TestIFBlock"))
			{


				for (Iterator<Edge> iterator1 = cGraph.edgesOutOf(sootMethod); iterator1.hasNext();)
				{

					SootMethod sootMethod1=iterator1.next().tgt();
					if(sootMethod1.getDeclaration().contains("test"))
					{
						System.out.println(sootMethod1);

						for(Unit unit:sootMethod1.getActiveBody().getUnits())
						{
							System.out.println(unit);
						}
					}
				}


//				//System.out.println(sootMethod.getActiveBody());
//
//				for(Unit unit:sootMethod.getActiveBody().getUnits())
//				{
//					System.out.println(unit);
//				}

//				ExceptionalUnitGraph exceptionalUnitGraph=new ExceptionalUnitGraph(sootMethod.getActiveBody());
//
//				ExceptionalBlockGraph exceptionalBlockGraph=new ExceptionalBlockGraph(exceptionalUnitGraph);
//
//
//				System.out.println(exceptionalUnitGraph.getHeads());
//
//				Block blockHead=exceptionalBlockGraph.getHeads().get(0);
//
//
//				List<Set<Block>> allBlockPath=new ArrayList<>();
//				for(Block block:blockHead.getSuccs())
//				{
//					LinkedHashSet<Block>  callBlockPath=new LinkedHashSet();
//					getAllBlockPath(block,callBlockPath,allBlockPath);
//
//
//				}
//
//				for(Set<Block> blockPath:allBlockPath)
//				{
//					System.out.println("11111111111111111111111111");
//					for(Block block:blockPath)
//					{
//						System.out.println(block);
//					}
//					System.out.println("22222222222222222222222222");
//				}







			}
		}
		
		
	}

	public static void getAllBlockPath(Block block,Set<Block> callBlockPath,List<Set<Block>> allBlockPath)
	{

		Set <Block> callBlockPathCopy=new LinkedHashSet<>(callBlockPath);

		callBlockPathCopy.add(block);


		if(block.getSuccs().size()==0)
		{
			allBlockPath.add(callBlockPathCopy);
			return;
		}
		else
		{
			for(Block childBlock:block.getSuccs())
			{
				if(!callBlockPathCopy.contains(childBlock))
				{
					getAllBlockPath(childBlock,callBlockPathCopy,allBlockPath);
				}
			}
		}

	}





}
