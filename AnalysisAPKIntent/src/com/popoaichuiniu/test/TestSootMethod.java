package com.popoaichuiniu.test;

import java.util.*;

import javax.management.InstanceAlreadyExistsException;

import beaver.Parser;
import com.popoaichuiniu.intentGen.IFBlock;
import com.popoaichuiniu.util.Config;
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
import soot.toolkits.graph.*;
import soot.toolkits.scalar.SimpleLocalDefs;

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




		
			
			
			if(sootMethod.getDeclaration().contains("onCreate")&&sootMethod.getDeclaringClass().getName().contains("Main3Activity"))
			{



				//System.out.println(sootMethod.getActiveBody());

				for(Unit unit:sootMethod.getActiveBody().getUnits())
				{
					System.out.println(unit);
				}

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
