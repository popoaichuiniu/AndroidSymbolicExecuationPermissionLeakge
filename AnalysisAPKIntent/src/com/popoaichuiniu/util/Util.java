package com.popoaichuiniu.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import com.google.common.collect.Lists;
import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

public class Util {
	public static void getAllUnitofCallPath(SootMethod target, SootMethod methodCall, List<Unit> callUnitList)// target中找methodCall的调用
	{

		UnitGraph unitGraph = new BriefUnitGraph(target.getActiveBody());
		for (Iterator<Unit> iterator = unitGraph.iterator(); iterator.hasNext();) {
			Unit unit = iterator.next();
			
			callUnitList.add(unit);
			System.out.println(unit);
			if (unit instanceof DefinitionStmt) {
				Value value = ((DefinitionStmt) unit).getRightOp();
				if (value instanceof InvokeExpr) {
					InvokeExpr invokeExpr = (InvokeExpr) value;
					if (isTargetAPIInvoke(methodCall, invokeExpr)) {

						return;
					}
				}

			} else if (unit instanceof InvokeStmt) {
				InvokeExpr invokeExpr = ((InvokeStmt) unit).getInvokeExpr();
				if (isTargetAPIInvoke(methodCall, invokeExpr)) {

					return;
				}
			}

		}

	}
	public  static SootMethod getCalleeSootMethodat(Unit unit)
	{
		if (unit instanceof DefinitionStmt) {
			DefinitionStmt uStmt = (DefinitionStmt) unit;

			Value rValue = uStmt.getRightOp();

			if (rValue instanceof InvokeExpr) {

				InvokeExpr invokeExpr = (InvokeExpr) rValue;

				return  invokeExpr.getMethod();



			}
		} else if (unit instanceof InvokeStmt) {
			InvokeStmt invokeStmt = (InvokeStmt) unit;

			InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

			return  invokeExpr.getMethod();

		}
		else {

			Stmt stmt= (Stmt) unit;
			if(stmt.containsInvokeExpr())
			{
				throw  new RuntimeException();
			}

		}
		return  null;
	}

	public static List<SootMethod> getMethodsInReverseTopologicalOrder(List<SootMethod> entryPoints,CallGraph cg) {


		List<SootMethod> topologicalOrderMethods = new ArrayList<SootMethod>();

		Stack<SootMethod> methodsToAnalyze = new Stack<SootMethod>();

		for (SootMethod entryPoint : entryPoints) {//层次遍历
			if (isApplicationMethod(entryPoint)) {
				methodsToAnalyze.push(entryPoint);
				while (!methodsToAnalyze.isEmpty()) {
					SootMethod method = methodsToAnalyze.pop();
					if (!topologicalOrderMethods.contains(method)) {//已经在里面的方法就不在加进去了
						if (method.hasActiveBody()){
							topologicalOrderMethods.add(method);
							for (Edge edge : getOutgoingEdges(method, cg)) {
								methodsToAnalyze.push(edge.tgt());
							}
						}
					}
				}
			}
		}

		List<SootMethod> rtoMethods = Lists.reverse(topologicalOrderMethods);
		return rtoMethods;
	}
	public static boolean isApplicationMethod(SootMethod method) {
		Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();

		for (SootClass appClass : applicationClasses) {
			//System.out.println("applicationClass:"+appClass.getName());
			if (appClass.getMethods().contains(method)) {
				return true;
			}
		}
		return false;
	}
	public static List<Edge> getOutgoingEdges(SootMethod method, CallGraph cg) {
		Iterator<Edge> edgeIterator = cg.edgesOutOf(method);
		List<Edge> outgoingEdges = Lists.newArrayList(edgeIterator);
		return outgoingEdges;
	}
	public static boolean isTargetAPIInvoke(SootMethod target, InvokeExpr invokeExpr) {
		if (invokeExpr.getMethod().getBytecodeSignature().equals(target.getBytecodeSignature())) {
			return true;
		}
		return false;
	}
	public static boolean isLibraryClass(String methodDesc) {

		if (methodDesc.contains("<android.")) {
			return true;
		}
		if (methodDesc.contains("<java.")) {
			return true;
		}
		if (methodDesc.contains("<org.apache.")) {
			return true;
		}
		if (methodDesc.contains("<org.hamcrest.")) {
			return true;
		}
		if (methodDesc.contains("<org.junit.")) {
			return true;
		}
		if (methodDesc.contains("<org.xml.")) {
			return true;
		}
		if (methodDesc.contains("<org.json.")) {
			return true;
		}
		if (methodDesc.contains("<org.w3c.")) {
			return true;
		}
		if (methodDesc.contains("<soot.")) {
			return true;
		}
		if (methodDesc.contains("<sun.misc.")) {
			return true;
		}
		if (methodDesc.contains("<javax.servlet.")) {
			return true;
		}
		if (methodDesc.contains("<javax.annotation.")) {
			return true;
		}
		if (methodDesc.contains("<com.squareup.javawriter.")) {
			return true;
		}
		return false;

	}
	
	public static InvokeExpr getInvokeOfUnit(Unit unit)
	{
		if (unit instanceof DefinitionStmt) {
			DefinitionStmt uStmt = (DefinitionStmt) unit;

			Value rValue = uStmt.getRightOp();

			if (rValue instanceof InvokeExpr) {

				InvokeExpr invokeExpr = (InvokeExpr) rValue;

				return invokeExpr;

			}
		} else if (unit instanceof InvokeStmt) {
			InvokeStmt invokeStmt = (InvokeStmt) unit;

			InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

			return invokeExpr;
		}
		return null;
	}

	public static List<SootMethod> getEA_EntryPoints(AndroidCallGraphHelper androidCallGraphHelper, AndroidInfoHelper androidInfoHelper)
    {



        CallGraph cGraph=androidCallGraphHelper.getCg();

        SootMethod entryPoint=androidCallGraphHelper.getEntryPoint();

        List<String> string_EAs=androidInfoHelper.getString_EAs();



        List<SootMethod> ea_entryPoints = new ArrayList<>();

        for (Iterator<Edge> iterator = cGraph.edgesOutOf(entryPoint); iterator.hasNext(); )// DummyMain方法调用的所有方法（各个组件的回调方法和生命周期）
        {
            Edge edge = iterator.next();
            SootMethod method = edge.getTgt().method();

            if (string_EAs.contains(method.getDeclaringClass().getName()))// 这个方法是不是属于EA的方法
            {
                ea_entryPoints.add(method);
            }


        }

        return  ea_entryPoints;
    }

	public static BytecodeOffsetTag extractByteCodeOffset(Unit unit) {
		for (Tag tag : unit.getTags()) {
			//System.out.println(tag.getName()+"zzz"+tag.getValue());
			if (tag instanceof BytecodeOffsetTag) {
				BytecodeOffsetTag bcoTag = (BytecodeOffsetTag) tag;
				return bcoTag;
			}
		}
		return null;
	}

}
