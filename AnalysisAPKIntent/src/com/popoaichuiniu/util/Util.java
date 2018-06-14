package com.popoaichuiniu.util;

import java.io.File;
import java.util.*;

import com.google.common.collect.Lists;
import com.popoaichuiniu.base.Graph;
import com.popoaichuiniu.base.Node;
import com.popoaichuiniu.intentGen.*;
import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import org.javatuples.Triplet;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Chain;


public class Util {

    private static boolean hasReachReducedCFGAnalysisLimit = false;

    private static int ifpathIFcountlimit = 20;


    public static void getAllUnitofCallPath(SootMethod target, SootMethod methodCall, List<Unit> callUnitList)// target中找methodCall的调用
    {

        UnitGraph unitGraph = new BriefUnitGraph(target.getActiveBody());
        for (Iterator<Unit> iterator = unitGraph.iterator(); iterator.hasNext(); ) {
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

    public static SootMethod getCalleeSootMethodat(Unit unit) {
        if (unit instanceof DefinitionStmt) {
            DefinitionStmt uStmt = (DefinitionStmt) unit;

            Value rValue = uStmt.getRightOp();

            if (rValue instanceof InvokeExpr) {

                InvokeExpr invokeExpr = (InvokeExpr) rValue;

                return invokeExpr.getMethod();


            }
        } else if (unit instanceof InvokeStmt) {
            InvokeStmt invokeStmt = (InvokeStmt) unit;

            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            return invokeExpr.getMethod();

        } else {

            Stmt stmt = (Stmt) unit;
            if (stmt.containsInvokeExpr()) {
                throw new RuntimeException();
            }

        }
        return null;
    }

    public static void getAllMethodsToThisSootMethod(SootMethod sootMethod, CallGraph cg, Set<SootMethod> visited) {

        visited.add(sootMethod);
        for (Iterator<Edge> iteratorSootMethod = cg.edgesInto(sootMethod); iteratorSootMethod.hasNext(); ) {
            SootMethod sootMethodSrc = iteratorSootMethod.next().getSrc().method();

            if ((!visited.contains(sootMethodSrc)) && Util.isApplicationMethod(sootMethodSrc)) {
                getAllMethodsToThisSootMethod(sootMethodSrc, cg, visited);
            }


        }


    }


    public static List<SootMethod> getMethodsInReverseTopologicalOrder(List<SootMethod> entryPoints, CallGraph cg) {


        List<SootMethod> topologicalOrderMethods = new ArrayList<SootMethod>();

        Stack<SootMethod> methodsToAnalyze = new Stack<SootMethod>();

        for (SootMethod entryPoint : entryPoints) {//层次遍历
            if (isApplicationMethod(entryPoint)) {
                methodsToAnalyze.push(entryPoint);
                while (!methodsToAnalyze.isEmpty()) {
                    SootMethod method = methodsToAnalyze.pop();
                    if (!topologicalOrderMethods.contains(method)) {//已经在里面的方法就不在加进去了
                        if (method.hasActiveBody()) {
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

    public static boolean isThirdtyPartyMethod(SootMethod method) {
        Set<String> thirtyLibraryList = AndroidInfoHelper.getThirdLibraryPackageNameSet();

        for (String temp : thirtyLibraryList) {
            if (method.getDeclaringClass().getPackageName().startsWith(temp)) {
                System.out.println(temp + "第三方库方法" + method.getBytecodeSignature());
                return true;
            }
        }

        if (thirtyLibraryList.contains(method.getDeclaringClass().getPackageName())) {
            return true;
        }
        return false;
    }


    public static boolean isApplicationMethod(SootMethod method) {


        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();

        for (SootClass appClass : applicationClasses) {

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

    public static InvokeExpr getInvokeOfUnit(Unit unit) {
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

    public static List<SootMethod> getEA_EntryPoints(AndroidCallGraphHelper androidCallGraphHelper, AndroidInfoHelper androidInfoHelper) {


        CallGraph cGraph = androidCallGraphHelper.getCg();

        SootMethod entryPoint = androidCallGraphHelper.getEntryPoint();

        List<String> string_EAs = androidInfoHelper.getString_EAs();


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

        return ea_entryPoints;
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


    public static MyUnitGraph getReducedCFG(SootMethod sootMethod, UnitGraph ug, IntentFlowAnalysis intentFlowAnalysis, Unit targetUnit, Map<JimpleBody, Body> bodyMap, SimpleLocalDefs defs) {


        JimpleBody jimpleBody = (JimpleBody) sootMethod.getActiveBody();
        Body modifiedBody = (Body) jimpleBody.clone();


        Graph allUnitInPathGraphPrevious = new Graph(sootMethod, targetUnit);

        getAllBranchUnit(ug,  new HashSet<>(jimpleBody.getUnits()), allUnitInPathGraphPrevious);

       // allUnitInPathGraphPrevious.exportGexf(sootMethod.getName() + "_previous");


        Set<Unit> needToRemove = new HashSet<>();//需要删除的语句

        Set<Unit> unitIntentRelativeAndStartStop = new HashSet<>();//和intent相关语句还有起点和终点

        findUnRelativeNode(targetUnit, sootMethod, needToRemove, intentFlowAnalysis, ug,unitIntentRelativeAndStartStop);


        Set<Unit> relativeUnitDefUnit = new HashSet<>();

        for (Unit relativeUnit : unitIntentRelativeAndStartStop) {//relativeUnit中使用的变量的定义语句不删除
            for (ValueBox valueBox : relativeUnit.getUseBoxes()) {
                Value value = valueBox.getValue();
                if (value instanceof Local) {
                    Local local = (Local) value;
                    for (Unit defUnit : defs.getDefsOfAt(local, relativeUnit)) {

                        needToRemove.remove(defUnit);

                        relativeUnitDefUnit.add(defUnit);

                        removeUseLocalDefUnit(defs, needToRemove, defUnit, relativeUnitDefUnit);
                    }
                }

            }
        }

        MyUnitGraph removeAllUnRelativeNodeInTargetUnitPathGraph = new MyUnitGraph(jimpleBody, targetUnit);
        Set<Unit> allUnitsOfGraph = new HashSet<>(removeAllUnRelativeNodeInTargetUnitPathGraph.getAllUnit());
        deleteUnitAndSimilarEdge(removeAllUnRelativeNodeInTargetUnitPathGraph, needToRemove, allUnitsOfGraph, allUnitInPathGraphPrevious.allBranchUnitSet);//删除不相关的点




        for (Unit unit : needToRemove) {
            modifiedBody.getUnits().remove(jimpleBody.bindings.get(unit));
        }

        bodyMap.put(jimpleBody, modifiedBody);


        Graph allUnitInPathGraphReduced = new Graph(sootMethod, targetUnit);

        getAllBranchUnit(removeAllUnRelativeNodeInTargetUnitPathGraph, removeAllUnRelativeNodeInTargetUnitPathGraph.getAllUnit(), allUnitInPathGraphReduced);

        //allUnitInPathGraphReduced.exportGexf(sootMethod.getName() + "_reduced");


        return removeAllUnRelativeNodeInTargetUnitPathGraph;

    }

    private static void getAllBranchUnit(UnitGraph ug, Set<Unit> allUnits, Graph allUnitInPathGraph) {

        for(Unit unit:allUnits)
        {
            allUnitInPathGraph.addNode(new Node(unit));

            if(ug.getSuccsOf(unit).size()>=2)
            {
                allUnitInPathGraph.allBranchUnitSet.add(unit);
            }

            for(Unit p:ug.getPredsOf(unit))
            {
                allUnitInPathGraph.addEdge(new Node(p),new Node(unit));
            }

            for(Unit s:ug.getSuccsOf(unit))
            {
                allUnitInPathGraph.addEdge(new Node(unit),new Node(s));
            }
        }


    }

    private static void findUnRelativeNode(Unit targetUnit, SootMethod sootMethod, Set<Unit> unitNeedToRemove, IntentFlowAnalysis intentFlowAnalysis, UnitGraph ug,Set<Unit> unitIntentRelativeAndStartStop) {

        for (Unit oneUnit : sootMethod.getActiveBody().getUnits())

        {
            if ((!isAboutIntentOrTargetAPI(oneUnit, intentFlowAnalysis, targetUnit)) && ug.getPredsOf(oneUnit).size() != 0 && ug.getSuccsOf(oneUnit).size() != 0) {

                unitNeedToRemove.add(oneUnit);


            }
            else
            {
                unitIntentRelativeAndStartStop.add(oneUnit);
            }

        }
    }


    private static void removeUseLocalDefUnit(SimpleLocalDefs defs, Set<Unit> needToRemove, Unit defUnit, Set<Unit> relativeUnitDefUnit) {
        if (defUnit instanceof DefinitionStmt) {
            DefinitionStmt definitionStmt = (DefinitionStmt) defUnit;
            if (definitionStmt.getRightOp() instanceof Local) {
                Local localTemp = (Local) definitionStmt.getRightOp();
                for (Unit defUnitOfLocalTemp : defs.getDefsOfAt(localTemp, defUnit)) {
                    needToRemove.remove(defUnitOfLocalTemp);
                    relativeUnitDefUnit.add(defUnitOfLocalTemp);
                    removeUseLocalDefUnit(defs, needToRemove, defUnitOfLocalTemp, relativeUnitDefUnit);
                }

            }
        }
    }

    private static void findUnRelativeNode(Unit targetUnit, SootMethod sootMethod, Set<Unit> allUnitUnRelativeIntentAndTargetUnit, IntentFlowAnalysis intentFlowAnalysis, UnitGraph ug, SimpleLocalDefs defs) {

        HashSet<Unit> relativeUnitSet = new HashSet<>();
        for (Unit oneUnit : sootMethod.getActiveBody().getUnits())

        {
            if ((!isAboutIntentOrTargetAPI(oneUnit, intentFlowAnalysis, targetUnit)) && ug.getPredsOf(oneUnit).size() != 0 && ug.getSuccsOf(oneUnit).size() != 0) {

                allUnitUnRelativeIntentAndTargetUnit.add(oneUnit);


            } else {
                relativeUnitSet.add(oneUnit);
            }

        }


        for (Unit oneUnit : relativeUnitSet) {
            for (ValueBox valueBox : oneUnit.getUseBoxes()) {
                Value value = valueBox.getValue();
                if (value instanceof Local) {
                    Local local = (Local) value;
                    for (Unit defUnit : defs.getDefsOfAt(local, oneUnit)) {
                        allUnitUnRelativeIntentAndTargetUnit.remove(defUnit);
                    }
                }

            }
        }


    }

    private static void deleteUnitAndSimilarEdge(MyUnitGraph myUnitGraph, Set<Unit> needToRemove, Set<Unit> allUnit, Set<Unit> branchUnitSet) {


        HashSet<Unit> removeSet = new HashSet<>(needToRemove);

        while (!removeSet.isEmpty()) {

            for (Unit oneUnit : allUnit) {

                deleteRepeatEdge(myUnitGraph, removeSet, oneUnit, needToRemove, branchUnitSet);


            }

            while (!removeSet.isEmpty()) {

                Iterator<Unit> iterator = removeSet.iterator();

                Unit toBeRemovedUnit = iterator.next();//第一个元素
                myUnitGraph.deleteUnit(toBeRemovedUnit);
                allUnit.remove(toBeRemovedUnit);
                iterator.remove();

                HashSet<Unit> parents = new HashSet<>(myUnitGraph.getPredsOf(toBeRemovedUnit));

                for (Unit p : parents) {
                    deleteRepeatEdge(myUnitGraph, removeSet, p, needToRemove, branchUnitSet);
                }

                HashSet<Unit> successors = new HashSet<>(myUnitGraph.getSuccsOf(toBeRemovedUnit));

                for (Unit s : successors) {
                    deleteRepeatEdge(myUnitGraph, removeSet, s, needToRemove, branchUnitSet);
                }


            }

            for (Unit oneUnit : allUnit) {


                deleteRepeatEdge(myUnitGraph, removeSet, oneUnit, needToRemove, branchUnitSet);


            }
        }


    }

    private static void deleteRepeatEdge(MyUnitGraph myUnitGraph, HashSet<Unit> removeSet, Unit oneUnit, Set<Unit> needToRemove, Set<Unit> branchUnitSet) {

        List<Unit> allParentsList = myUnitGraph.getPredsOf(oneUnit);

        HashSet<Unit> allParentsSet = new HashSet<>(allParentsList);//去除重复的父亲

        allParentsSet.remove(oneUnit);//去除自循环 约减循环

        List<Unit> newParentsList = new ArrayList<>(allParentsSet);

        myUnitGraph.getUnitToPreds().put(oneUnit, newParentsList);


        List<Unit> allChildList = myUnitGraph.getSuccsOf(oneUnit);

        HashSet<Unit> allChildListSet = new HashSet<>(allChildList);//去除重复的孩子

        allChildListSet.remove(oneUnit);//去除自循环  约减循环

        if (allChildListSet.size() == 1 && (branchUnitSet.contains(oneUnit))) {
            removeSet.add(oneUnit);
            needToRemove.add(oneUnit);
        }

        List<Unit> newChildList = new ArrayList<>(allChildListSet);

        myUnitGraph.getUnitToSuccs().put(oneUnit, newChildList);
    }

    private static void findAllUnitInToTargetUnitPath( Unit oneUnit, UnitGraph unitGraph, Set<Unit> visited) {

        visited.add(oneUnit);

        for (Unit unit : unitGraph.getPredsOf(oneUnit)) {
            if (!visited.contains(unit)) {
                findAllUnitInToTargetUnitPath(unit, unitGraph, visited);
            }

        }

    }


    private static void getIfFlowInfo(UnitGraph ug, Unit targetUnit, Map<Unit, Set<String>> unitHasInfo, Set<Unit> startUnits, Set<Unit> allUnitOfTargetUnit, Map<Unit, Integer> unitInDegree) {


        List<Unit> queue = new ArrayList<>(startUnits);

        Set<Unit> unResolveUnitsSet = new HashSet<>(allUnitOfTargetUnit);

        unResolveUnitsSet.removeAll(startUnits);

        while (queue.size() != 0) {//
            Unit oneStartUnit = queue.remove(0);//取队首
            if (oneStartUnit == targetUnit) {
                assert queue.size() == 0;
                return;
            }
            for (int i = 0; i < ug.getSuccsOf(oneStartUnit).size(); i++)//给每个子节点传承自己的信息
            {
                Unit childUnit = ug.getSuccsOf(oneStartUnit).get(i);

                if (allUnitOfTargetUnit.contains(childUnit)) {
                    Set<String> childUnitUnitInfo = unitHasInfo.get(childUnit);
                    Set<String> parentUnitInfo = unitHasInfo.get(oneStartUnit);
                    childUnitUnitInfo.addAll(parentUnitInfo);//将自己继承的信息传承给字节点
                    if (ug.getSuccsOf(oneStartUnit).size() >= 2)//如果自己是分支语句的话，给子节点额外加入自己的分支语句的信息。
                    {
                        childUnitUnitInfo.add(oneStartUnit.toString() + "b:" + i);
                    }

                    unitInDegree.put(childUnit, unitInDegree.get(childUnit) - 1);//将自己的所有子节点的入度减一
                }


            }

            Set<Unit> newInDegreeZero = new HashSet<>();
            for (Unit unResolveUnit : unResolveUnitsSet) {
                int inDegree = unitInDegree.get(unResolveUnit);
                if (inDegree == 0) {
                    newInDegreeZero.add(unResolveUnit);
                }

            }

            queue.addAll(newInDegreeZero);
            unResolveUnitsSet.removeAll(newInDegreeZero);


        }

    }



//    private static Pair<Boolean, Unit> analyseIFBlock(Unit unit, int branchLayer, UnitGraph ug, Map<Unit, Set<String>> unitHasInfo, Stack<Pair<Unit, List<String>>> ifStack, IntentFlowAnalysis intentFlowAnalysis, Unit targetUnit, Set<Unit> path, Set<Unit> allUnitOfTargetUnit) {
//        //分析这个unit 设为xx （if）是否可以约简
//
//        if (branchLayer > ifpathIFcountlimit) {
//            hasReachReducedCFGAnalysisLimit = true;
//            return null;
//        }
//
//
//        Set<Unit> pathCopy = new LinkedHashSet<>(path);
//        pathCopy.add(unit);
//
//        if (ug.getPredsOf(unit).size() >= 2) {
//
//            Set<String> unitInfo = new HashSet<>(unitHasInfo.get(unit));
//            while (!unitInfo.isEmpty()) {
//
//
//                if (ifStack.size() > 0) {
//                    Pair<Unit, List<String>> ifInfo = ifStack.peek();
//
//                    if (unitInfo.containsAll(ifInfo.getValue1())) {
//                        System.out.println("pop: " + ifInfo.getValue0());
//                        ifStack.pop();
//                        unitInfo.removeAll(ifInfo.getValue1());
//                    } else {
//                        break;
//                    }
//                } else {
//                    break;
//                }
//
//
//            }
//
//
//            if (ifStack.empty()) {//xx的汇入结点
//                return new Pair<>(false, unit);//到达终结点还没有找到intent相关所以返回false
//            }
//
//
//        }
//
//        List<Unit> successorsOfCurUnit = ug.getSuccsOf(unit);
//
//        if (successorsOfCurUnit.size() >= 2)//branch
//        {
//
//            branchLayer++;//如果有一分支到达不了targetUnit就直接返回了。所以当branchLayer到达为20,路径中的所有if的branch是都到达到target的。
//
//            List<String> ifInfoString = new ArrayList<>();
//            for (int i = 0; i < successorsOfCurUnit.size(); i++) {
//                ifInfoString.add(unit.toString() + "b:" + i);
//            }
//
//            ifStack.push(new Pair<Unit, List<String>>(unit, ifInfoString));
//            System.out.println("push: " + unit);
//
//
//            Set<Pair<Boolean, Unit>> branchStopPoints = new HashSet<>();
//            Pair<Boolean, Unit> flagChildIF = null;
//            for (Unit branchUnit : ug.getSuccsOf(unit)) {
//
//                if (!allUnitOfTargetUnit.contains(branchUnit))//这个分支到不了targetUnit,那我就不需要走这个分支，因为之后约简的cfg是逆向找路径，所以这个if去不去无所谓，因为这个if的汇入点一定在这个targetUnit之后
//                //逆向找路径数关键是汇合点的个数，而不是if,但是为了好写算法，这个约简算法是从if开始顺序约简的
//                {
//                    return new Pair<>(true, null);
//
//                }
//                if (pathCopy.contains(branchUnit)) {//检测到有循环,不约简这个xx了，因为这个xx找不到汇入点了
//
//                    return new Pair<>(true, null);
//
//                }
//                flagChildIF = analyseIFBlock(branchUnit, branchLayer, ug, unitHasInfo, ifStack, intentFlowAnalysis, targetUnit, pathCopy, allUnitOfTargetUnit);//if 需不需要分析
//
//
//                if (flagChildIF == null) {//达到限制
//
//                    return null;
//
//                }
//
//                if (flagChildIF.getValue0())//这个分支不约简
//                {
//                    return new Pair<>(true, null);
//                } else {
//                    branchStopPoints.add(flagChildIF);
//                }
//            }
//
//            //当没一个branch都和intent和targetUnit不相关的话
//
//            Set<Unit> stopPoints = new HashSet<>();
//            for (Pair<Boolean, Unit> branchStop : branchStopPoints) {
//                stopPoints.add(branchStop.getValue1());
//            }
//
//            if (stopPoints.size() == 1) {
//                return new Pair<>(false, flagChildIF.getValue1());
//            } else {
//
//                throw new RuntimeException("if汇入点不一样");
//            }
//
//
//        }
//
//
//        if (isAboutIntentOrTargetAPI(unit, intentFlowAnalysis, targetUnit)) {
//            return new Pair<>(true, null);
//        }
//
//        if (successorsOfCurUnit.size() <= 0) {//<0是不可能的，然后=0是最后一个ExitJStmt
//            assert unit instanceof ExitJStmt;
//            return new Pair<>(false, unit);
//        } else {//=1
//            if (!pathCopy.contains(successorsOfCurUnit.get(0))) {
//                return analyseIFBlock(successorsOfCurUnit.get(0), branchLayer, ug, unitHasInfo, ifStack, intentFlowAnalysis, targetUnit, pathCopy, allUnitOfTargetUnit);
//            } else//处理到了循环
//            {
//
//                return new Pair<>(true, null);//不约简这个xx了，因为这个xx找不到汇入点了
//            }
//
//        }
//
//
//    }

//    private static Pair<Boolean, Unit> analyseIFBlock(Unit unit, int branchLayer, UnitGraph ug, Map<Unit, Set<String>> unitHasInfo, Stack<Pair<Unit, List<String>>> ifStack, IntentFlowAnalysis intentFlowAnalysis, Unit targetUnit, Set<Unit> path, Set<Unit> allUnitOfTargetUnit) {
//        //分析这个unit 设为xx （if）是否可以约简
//
//        Set<Unit> pathCopy = new LinkedHashSet<>(path);
//        pathCopy.add(unit);
//
//        if (ug.getSuccsOf(unit).size() == 1 && isAboutIntentOrTargetAPI(unit, intentFlowAnalysis, targetUnit)) {
//            return new Pair<>(true, null);
//        }
//        if (ug.getPredsOf(unit).size() >= 2) {
//            if (ifStack.size() > 0) {
//                Pair<Unit, List<String>> ifInfo = ifStack.peek();
//                if (unitHasInfo.get(unit).containsAll(ifInfo.getValue1()))//unit是xx汇入点
//                {
//                    ifStack.pop();
//                    return new Pair<>(false, unit);
//                }
//            }
//        }
//
//
//        if (allUnitOfTargetUnit.containsAll(ug.getSuccsOf(unit))) {//存在不能到达targtUnit的分支
//            return new Pair<>(true, null);
//        } else {
//
//            Set<Unit> branchJoinSet = new HashSet<>();
//            for (Unit branchUnit : ug.getSuccsOf(unit)) {
//
//                if (pathCopy.contains(branchUnit)) {//遇到循环
//                    return new Pair<>(true, null);
//                } else {
//
//                    Pair<Boolean, Unit> flagBranch = analyseIFBlock(branchUnit, branchLayer, ug, unitHasInfo, ifStack, intentFlowAnalysis, targetUnit, pathCopy, allUnitOfTargetUnit);
//
//                    if (flagBranch == null) {
//                        return null;
//                    }
//                    if (flagBranch.getValue0()) {
//                        return new Pair<>(true, null);
//                    } else {
//                        branchJoinSet.add(flagBranch.getValue1());
//                    }
//                }
//
//
//            }
//            if (branchJoinSet.size() == 1) {
//                return new Pair<>(false, branchJoinSet.iterator().next());
//            } else {
//                throw new RuntimeException("branch入口点不一样");
//            }
//        }
//
//
//    }

    private static boolean isAboutIntentOrTargetAPI(Unit unit, IntentFlowAnalysis intentFlowAnalysis, Unit targetUnit) {

        if (unit == targetUnit) {
            return true;
        }
        FlowSet<Value> intentRelativeValue = intentFlowAnalysis.getFlowBefore(unit);

        FlowSet<Value> intentAfter = intentFlowAnalysis.getFlowAfter(unit);


        for (Value value : intentAfter) {
            for (ValueBox valueBox : unit.getDefBoxes()) {
                if (value.equivTo(valueBox.getValue())) {
                    return true;
                }
            }
        }

        for (Value value : intentRelativeValue) {
            for (ValueBox valueBox : unit.getUseBoxes()) {
                if (value.equivTo(valueBox.getValue())) {
                    return true;
                }

            }
        }


        return false;
    }

    public static boolean unitNeedAnalysis(Unit unit, SootMethod sootMethod, Set<Triplet<Integer, String, String>> targets) {
        BytecodeOffsetTag tag = Util.extractByteCodeOffset(unit);
        if (tag == null) {
            return false;
        }

        for (Triplet<Integer, String, String> item : targets) {
            if (item.getValue0() == tag.getBytecodeOffset() && item.getValue1().equals(sootMethod.getBytecodeSignature())) {
                assert unit.toString().equals(item.getValue2());
                return true;
            }

        }
        return false;
    }


    public static boolean isPermissionProtectedAPIClass(SootClass sootclass) {
        Set<String> sootMethodsStringSet = AndroidInfoHelper.getPermissionAndroguardMethods().keySet();

        for (String temp : sootMethodsStringSet) {
            if (temp.contains(sootclass.getName()))//这个Name是包含包名的
            {
                return true;
            }
        }
        return false;


    }

    public static boolean isPermissionProtectedAPIMethodName(String methodName, String param) {
        Set<String> sootMethodsStringSet = AndroidInfoHelper.getPermissionAndroguardMethods().keySet();

        for (String temp : sootMethodsStringSet)//<com.android.internal.telephony.SubscriptionController: clearDefaultsForInactiveSubIds()V>
        {
            int index0 = temp.indexOf(":");
            int index1 = temp.indexOf("(");
            int index2 = temp.indexOf(")");
            String methodNameOfDSP = temp.substring(index0 + 2, index1);
            String methodParam = temp.substring(index1 + 1, index2);

            if (methodNameOfDSP.equals(methodName) && methodParam.equals(param)) {
                return true;
            } else {
                return false;
            }
        }
        return false;


    }

    public static boolean isPermissionProtectedAPI(SootMethod sootMethod) {
        if (AndroidInfoHelper.getPermissionAndroguardMethods()
                .containsKey(sootMethod.getBytecodeSignature())) {
            return true;
        } else {
            return false;
        }

    }

    public static void testInitial(List<SootMethod> ea_entryPoints, List<SootMethod> roMethods, Chain<SootClass> applicationClasses, String appPath) {
        WriteFile writeFile = new WriteFile("AnalysisAPKIntent/testInitial/" + new File(appPath).getName() + ".txt", false);

        writeFile.writeStr("all application class:" + "\n\n\n\n");
        for (SootClass sootClass : applicationClasses) {
            writeFile.writeStr(sootClass.getName() + "\n");
        }
        writeFile.writeStr("\n\n\n\n");

        writeFile.writeStr("ea_tryPoints:" + "\n\n\n\n");


        for (SootMethod sootMethod : ea_entryPoints) {
            writeFile.writeStr(sootMethod.getBytecodeSignature() + "\n");
        }
        writeFile.writeStr("\n\n\n\n");


        writeFile.writeStr("roMethods:" + "\n\n\n\n");

        for (SootMethod sootMethod : roMethods) {
            writeFile.writeStr(sootMethod.getBytecodeSignature() + "\n");
        }


        writeFile.close();


    }


}
