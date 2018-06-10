package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.jacy.CGExporter;
import com.popoaichuiniu.util.Util;
import soot.Kind;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MyCallGraph extends CallGraph {

    Set<SootMethod> allMethods = new HashSet<>();

    Set<Edge> allEdges = new HashSet<>();


    Map<SootMethod, Set<MyPairUnitToEdge>> targetUnitInSootMethod = new HashMap<SootMethod, Set<MyPairUnitToEdge>>();



    Map<SootMethod, Set<List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>>> identicalMyUnitGraphSetOfMethodMap = new HashMap<>();


    Map<SootMethod, Set<Edge>> inEdgesOfThisMethod = new HashMap<>();


    Map<SootMethod, Set<Edge>> outEdgesOfThisMethod = new HashMap<>();

    Set<SootMethod> sootMethodsNeedToRemove = new HashSet<>();

    BufferedWriter bufferedWriterRepeatEdge = null;

    SootMethod targetSootMethod = null;

    Unit targetUnit = null;


    static {

        File file = new File("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "RepeatEdgeSituation.txt");
        if (file.exists()) {
            file.delete();
        }


    }

    class MyPairUnitToEdge{//edge 相同保证：srcUnit,src,tgt,kind都相同 //srcUnit可能为空。一个方法里可能有多个edge的srcUnit为空空，因此使用edge作为key。

        Edge outEdge;//key  对于方法里有多个多个edge:srcUnit是null ,src,tgt,kind都相同，认为只有一条边(由于edge的equals定义)
        Unit srcUnit;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MyPairUnitToEdge that = (MyPairUnitToEdge) o;
            return Objects.equals(outEdge, that.outEdge);
        }

        @Override
        public int hashCode() {

            return Objects.hash(outEdge);
        }

        public MyPairUnitToEdge(Edge outEdge, Unit srcUnit) {
            this.outEdge = outEdge;
            this.srcUnit = srcUnit;
        }
    }

    public MyCallGraph(Set<SootMethod> allMethodsInPathOfTarget, CallGraph cg, SootMethod targetSootMethod, Unit targetUnit, IntentConditionTransformSymbolicExcutation intentConditionTransformSymbolicExcutation) {


        this.targetSootMethod = targetSootMethod;
        this.targetUnit = targetUnit;


        try {
            bufferedWriterRepeatEdge = new BufferedWriter(new FileWriter("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "RepeatEdgeSituation.txt", true));
        } catch (IOException e) {
            e.printStackTrace();
        }





        for (SootMethod sootMethod : allMethodsInPathOfTarget) {


            inEdgesOfThisMethod.put(sootMethod, new HashSet<>());
            outEdgesOfThisMethod.put(sootMethod, new HashSet<>());


        }



        MyPairUnitToEdge myPairUnitToEdge=new MyPairUnitToEdge(null,targetUnit);
        HashSet<MyPairUnitToEdge> hashSetTargetSootMethod=new HashSet<>();
        hashSetTargetSootMethod.add(myPairUnitToEdge);
        targetUnitInSootMethod.put(targetSootMethod,hashSetTargetSootMethod);

        constructMyCallGraph(allMethodsInPathOfTarget, cg, targetSootMethod, new HashSet<>());


        validateCallGraph(targetSootMethod);


//        HashSet<SootMethod> xxx = new HashSet<>();
//
//        HashSet<SootMethod> yyy = new HashSet<>();
//
//
//
//
//
//
//        getTargetUnitInSootMethod(targetSootMethod, targetUnit, new HashSet<MyPairSootMethodUnit>(), xxx);//得到callgraph每一个方法的分析点
//
//
//
//
//
//
//        Util.getAllMethodsToThisSootMethod(targetSootMethod, this, yyy);
//        if (!xxx.containsAll(yyy)) {
//            throw new RuntimeException();
//        }
//
//
//        if (!xxx.containsAll(allMethodsInPathOfTarget)) {
//            throw new RuntimeException();
//        }

        analyseEverySootMethodToGetMyUnitGraph(targetSootMethod, intentConditionTransformSymbolicExcutation);


        System.out.println("MyCallGraph初始化完成！");


        try {
            bufferedWriterRepeatEdge.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void constructMyCallGraph(Set<SootMethod> allMethodsInPathOfTarget, CallGraph cg, SootMethod targetSootMethod, HashSet<SootMethod> visited) {

        visited.add(targetSootMethod);
        for (Iterator<Edge> iteratorSootMethod = cg.edgesInto(targetSootMethod); iteratorSootMethod.hasNext(); ) {

            Edge edge = iteratorSootMethod.next();

            SootMethod sootMethodSrc = edge.getSrc().method();

            if (allMethodsInPathOfTarget.contains(sootMethodSrc)) {

                addEdge(edge);//允许环
                Set<MyPairUnitToEdge> targetMyPairUnitToEdges = targetUnitInSootMethod.get(sootMethodSrc);

                if (targetMyPairUnitToEdges == null) {
                    targetMyPairUnitToEdges = new HashSet<>();
                }
                //FINALIZE edge: null in <org.jivesoftware.smackx.muc.MultiUserChat: void <init>(org.jivesoftware.smack.Connection,java.lang.String)> ==> <org.jivesoftware.smackx.muc.MultiUserChat: void finalize()>


                targetMyPairUnitToEdges.add(new MyPairUnitToEdge(edge,edge.srcUnit()));//注意其srcUnit是可以为空的  这个条边是虚拟的


                targetUnitInSootMethod.put(sootMethodSrc, targetMyPairUnitToEdges);

                if (!visited.contains(sootMethodSrc)) {
                    constructMyCallGraph(allMethodsInPathOfTarget, cg, sootMethodSrc, visited);
                }

            }


        }


    }

    private void analyseEverySootMethodToGetMyUnitGraph(SootMethod targetSootMethod, IntentConditionTransformSymbolicExcutation intentConditionTransformSymbolicExcutation) {


        for (Map.Entry<SootMethod, Set<MyPairUnitToEdge>> oneEntry : this.targetUnitInSootMethod.entrySet()) {

            SootMethod oneSootMethod = oneEntry.getKey();


            UnitGraph ug = new BriefUnitGraph(oneSootMethod.getActiveBody());


            IntentFlowAnalysis intentFlowAnalysis = new IntentFlowAnalysis(ug);

            Set<MyPairUnitToEdge> oneMyPairUnitToEdgeSet = oneEntry.getValue();

            boolean flagNeedToRemove = true;
            for (MyPairUnitToEdge onePair : oneMyPairUnitToEdgeSet) {

                if(onePair.srcUnit==null)//如果有的边的srcUnit就不分析它.
                {
                    continue;
                }

                int count=-1;
                Map<Unit, IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> map=intentConditionTransformSymbolicExcutation.allSootMethodsAllUnitsTargetUnitInMethodInfo.get(oneSootMethod);
                if(map!=null)
                {
                    IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo targetUnitInMethodInfo=map.get(onePair.srcUnit);
                    if(targetUnitInMethodInfo!=null)
                    {
                        count=targetUnitInMethodInfo.myUnitGraph.getAllUnit().size();
                    }
                }

                if(count==-1)
                {
                    count = intentConditionTransformSymbolicExcutation.doAnalysisOnUnit(onePair, oneSootMethod, intentFlowAnalysis);
                }


                if (count != 2) {
                    flagNeedToRemove = false;
                }
            }

            if (flagNeedToRemove) {//不删除起始节点和终点
                if (oneSootMethod != targetSootMethod && (!oneSootMethod.getBytecodeSignature().equals("<dummyMainClass: dummyMainMethod([Ljava/lang/String;)V>"))) {
                    sootMethodsNeedToRemove.add(oneSootMethod);
                }

            }


        }
        System.out.println("call grah节点数:" + this.allMethods.size() + " 需要删除的节点数:" + sootMethodsNeedToRemove.size());

        try {
            bufferedWriterRepeatEdge.write("call grah节点数:" + this.allMethods.size() + " 需要删除的节点数:" + sootMethodsNeedToRemove.size() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (Map.Entry<SootMethod, Set<MyPairUnitToEdge>> oneEntry : this.targetUnitInSootMethod.entrySet()) {

            if (oneEntry.getKey() == targetSootMethod)//不分析targetSootMethod
            {
                continue;
            }


            getIdenticalTargetUnitInMethod(oneEntry.getKey(), oneEntry.getValue(), intentConditionTransformSymbolicExcutation.allSootMethodsAllUnitsTargetUnitInMethodInfo);
        }


        for (Map.Entry<SootMethod, Set<List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>>> entry : identicalMyUnitGraphSetOfMethodMap.entrySet()) {
            int all = targetUnitInSootMethod.get(entry.getKey()).size();
            int canRemoveMax = 0;
            for (List list : entry.getValue()) {
                canRemoveMax = canRemoveMax + list.size() - 1;
            }
            int remain = all - canRemoveMax;
            try {
                bufferedWriterRepeatEdge.write(all + " " + remain + " " + canRemoveMax + entry.getKey().getBytecodeSignature() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void getIdenticalTargetUnitInMethod(SootMethod sootMethod, Set<MyPairUnitToEdge> myPairUnitToEdgeSet, Map<SootMethod, Map<Unit, IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> allSootMethodsAllUnitsTargetUnitInMethodInfo) {


        identicalMyUnitGraphSetOfMethodMap.put(sootMethod, new HashSet<>());

        Map<Integer, List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> targetUnitInMethodInfoDivideByMyUnitGraphSize = new HashMap<>();//将这些unit按照unitgraph图的大小划分
        for (MyPairUnitToEdge myPairUnitToEdge:myPairUnitToEdgeSet) {

            if(myPairUnitToEdge.srcUnit==null)
            {
                continue;
            }
            IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo targetUnitInMethodInfo = allSootMethodsAllUnitsTargetUnitInMethodInfo.get(sootMethod).get(myPairUnitToEdge.srcUnit);
            int count = targetUnitInMethodInfo.myUnitGraph.getAllUnit().size();
            List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> arr = targetUnitInMethodInfoDivideByMyUnitGraphSize.get(count);
            if (arr == null) {
                arr = new ArrayList<>();
            }
            arr.add(targetUnitInMethodInfo);

            targetUnitInMethodInfoDivideByMyUnitGraphSize.put(count, arr);

        }


        for (Map.Entry<Integer, List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> countTargetUnitInMethodInfoMyGraphSize : targetUnitInMethodInfoDivideByMyUnitGraphSize.entrySet()) {


            List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> targetUnitInMethodInfoArrWithIdenticalMyGraphSize = countTargetUnitInMethodInfoMyGraphSize.getValue();

            if (targetUnitInMethodInfoArrWithIdenticalMyGraphSize.size() >= 2) {//其他myGraphSize相同且大于2的 targetUnitInMethodInfo集合

                Map<Integer, List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> targetUnitInMethodInfoDivideByUnitPathsSize = new HashMap<>();
                for (IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo targetUnitInMethodInfo : targetUnitInMethodInfoArrWithIdenticalMyGraphSize) {
                    int count = targetUnitInMethodInfo.unitPaths.size();

                    List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> targetUnitInMethodInfoArrWithIdenticalUnitPathsSize = targetUnitInMethodInfoDivideByUnitPathsSize.get(count);
                    if (targetUnitInMethodInfoArrWithIdenticalUnitPathsSize == null) {
                        targetUnitInMethodInfoArrWithIdenticalUnitPathsSize = new ArrayList<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>();
                    }

                    targetUnitInMethodInfoArrWithIdenticalUnitPathsSize.add(targetUnitInMethodInfo);

                    targetUnitInMethodInfoDivideByUnitPathsSize.put(count, targetUnitInMethodInfoArrWithIdenticalUnitPathsSize);

                }

                for (Map.Entry<Integer, List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> countTargetUnitInMethodInfoFinalPaths : targetUnitInMethodInfoDivideByUnitPathsSize.entrySet()) {
                    if (countTargetUnitInMethodInfoFinalPaths.getValue().size() >= 2)//unitPaths数量相同。
                    {

                        List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> targetUnitInMethodInfoFinalPaths = countTargetUnitInMethodInfoFinalPaths.getValue();

                        Set<List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> unitInfoWithIdenticalMyUnitGraph = getIdenticalMyUnitGraphTargetUnitSet(targetUnitInMethodInfoFinalPaths);


                        identicalMyUnitGraphSetOfMethodMap.put(sootMethod, unitInfoWithIdenticalMyUnitGraph);


                    }
                }

            }


        }


    }

    private void validateCallGraph(SootMethod targetSootMethod) {
        for (SootMethod sootMethod : allMethods) {
            Set<Edge> inEdges = inEdgesOfThisMethod.get(sootMethod);
            if (inEdges.size() == 0 && (!sootMethod.getBytecodeSignature().equals("<dummyMainClass: dummyMainMethod([Ljava/lang/String;)V>"))) {
                System.out.println(sootMethod.getBytecodeSignature() + "xxxxxxxx入度为0");
                throw new RuntimeException();
            }
            Set<Edge> outEdges = outEdgesOfThisMethod.get(sootMethod);

            if (outEdges.size() == 0 && sootMethod != targetSootMethod) {
                System.out.println(sootMethod.getBytecodeSignature() + "xxxxxxxx出度为0");
                throw new RuntimeException();
            }
        }
    }

    public void reduced(Map<SootMethod, Map<Unit, IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> allSootMethodsAllUnitsTargetUnitInMethodInfo) {

        for (Map.Entry<SootMethod, Set<MyPairUnitToEdge>> entry : targetUnitInSootMethod.entrySet()) {
            unitRemainInSootMethodMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        deleteSootMethod(allSootMethodsAllUnitsTargetUnitInMethodInfo);
    }


//    class MyPairSootMethodUnit {
//
//        SootMethod sootMethod;
//
//        Unit unit;
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            MyPairSootMethodUnit myPairSootMethodUnit = (MyPairSootMethodUnit) o;
//            return Objects.equals(sootMethod, myPairSootMethodUnit.sootMethod) &&
//                    Objects.equals(unit, myPairSootMethodUnit.unit);
//        }
//
//        @Override
//        public int hashCode() {
//
//            return Objects.hash(sootMethod, unit);
//        }
//
//        public MyPairSootMethodUnit(SootMethod sootMethod, Unit unit) {
//            this.sootMethod = sootMethod;
//            this.unit = unit;
//        }
//    }

//    private Set<Edge> allEdgesAnotherWay=new HashSet<>();
//    private void getTargetUnitInSootMethod(SootMethod sootMethod, Unit unit, HashSet<MyPairSootMethodUnit> visited, HashSet<SootMethod> xxx) {
//
//        xxx.add(sootMethod);
//
//        System.out.println(sootMethod.getBytecodeSignature());
//
//        Set<Unit> targetUnits = targetUnitInSootMethod.get(sootMethod);
//
//        if (targetUnits == null) {
//            targetUnits = new HashSet<>();
//        }
//
//
//        targetUnits.add(unit);
//
//        targetUnitInSootMethod.put(sootMethod, targetUnits);
//
//
//        if(sootMethod!=targetSootMethod)
//        {
//            Edge edge=unitEdgeMap.get(unit);
//
//            if(edge==null)
//            {
//                throw  new RuntimeException();
//            }
//        }
//
//
//        visited.add(new MyPairSootMethodUnit(sootMethod, unit));
//
//        for (Iterator<Edge> iteratorSootMethod = this.edgesInto(sootMethod); iteratorSootMethod.hasNext(); ) {
//
//            Edge edgeSootMethod = iteratorSootMethod.next();
//
//            SootMethod sootMethodSrc = edgeSootMethod.getSrc().method();
//            Unit srcUnit = edgeSootMethod.srcUnit();
//
//            MyPairSootMethodUnit myPairSootMethodUnit = new MyPairSootMethodUnit(sootMethodSrc, srcUnit);
//
//            if (!visited.contains(myPairSootMethodUnit)) {
//                allEdgesAnotherWay.add(edgeSootMethod);
//                getTargetUnitInSootMethod(sootMethodSrc, srcUnit, visited, xxx);
//            }
//
//
//        }
//
//
//    }


    private Map<SootMethod, Set<MyPairUnitToEdge>> unitRemainInSootMethodMap = new HashMap<>();

    public void deleteSootMethod(Map<SootMethod, Map<Unit, IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> allSootMethodsAllUnitsTargetUnitInMethodInfo) {

        for (SootMethod sootMethod : allMethods) {
            if (sootMethod == targetSootMethod)//targetSootMethod没有出去的边，虽然有targetUnit
            {
                continue;
            }

            for (MyPairUnitToEdge myPairUnitToEdge : targetUnitInSootMethod.get(sootMethod)) {

                Edge edge = myPairUnitToEdge.outEdge;
                if (edge == null) {
                    throw new RuntimeException();
                }
                deleteRepeatEdge(sootMethod, edge, allSootMethodsAllUnitsTargetUnitInMethodInfo);
            }

        }
        validateCallGraph(targetSootMethod);


        int count = 0;
        for (SootMethod sootMethod : sootMethodsNeedToRemove) {

            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^" + count++);

            Set<Edge> inEdges = new HashSet<>();
            for (Iterator<Edge> inEdgeIterator = this.edgesInto(sootMethod); inEdgeIterator.hasNext(); ) {
                Edge inEdge = inEdgeIterator.next();
                inEdges.add(inEdge);

            }
            for (Edge inEdge : inEdges) {
                removeEdge(inEdge);

            }

            Set<Edge> outEdges = new HashSet<>();
            for (Iterator<Edge> outEdgeIterator = this.edgesOutOf(sootMethod); outEdgeIterator.hasNext(); ) {
                Edge outEdge = outEdgeIterator.next();
                outEdges.add(outEdge);

            }

            for (Edge outEdge : outEdges) {
                removeEdge(outEdge);

            }


            for (Edge inEdge : inEdges) {
                SootMethod sootMethodSrc = inEdge.getSrc().method();

                for (Edge outEdge : outEdges) {
                    SootMethod sootMethodTgt = outEdge.getTgt().method();

                    if ((!sootMethodTgt.equals(sootMethod)) && (!sootMethodSrc.equals(sootMethod))) {
                        Edge newEdge = new Edge(sootMethodSrc, inEdge.srcStmt(), sootMethodTgt, Kind.ZMS_Set);
                        System.out.println("ttttttttttttttttttt" + count);
                        addEdge(newEdge);
                        deleteRepeatEdge(sootMethodSrc, newEdge, allSootMethodsAllUnitsTargetUnitInMethodInfo);


                    }


                }


            }


            removeNode(sootMethod);
            System.out.println(sootMethod);
            validateCallGraph(targetSootMethod);


        }

        for (SootMethod sootMethod : allMethods) {
            if (sootMethod == targetSootMethod) {
                continue;
            }
            for (MyPairUnitToEdge myPairUnitToEdge : targetUnitInSootMethod.get(sootMethod)) {
                Edge edge = myPairUnitToEdge.outEdge;
                if (edge == null) {
                    throw new RuntimeException();
                }
                deleteRepeatEdge(sootMethod, edge, allSootMethodsAllUnitsTargetUnitInMethodInfo);
            }

        }


        validateCallGraph(targetSootMethod);

    }

    private void removeNode(SootMethod sootMethod) {
        allMethods.remove(sootMethod);
        inEdgesOfThisMethod.remove(sootMethod);
        outEdgesOfThisMethod.remove(sootMethod);
    }


    @Override
    public boolean addEdge(Edge e) {


        SootMethod src = e.src();
        SootMethod tgt = e.tgt();

        Set<Edge> inEdgesOfSootMethod = inEdgesOfThisMethod.get(tgt);
        if (inEdgesOfSootMethod == null) {
            inEdgesOfSootMethod = new HashSet<>();
        }


        boolean flag1 = inEdgesOfSootMethod.add(e);

        inEdgesOfThisMethod.put(tgt, inEdgesOfSootMethod);

        Set<Edge> outEdgesOfSootMethod = outEdgesOfThisMethod.get(src);
        if (outEdgesOfSootMethod == null) {
            outEdgesOfSootMethod = new HashSet<>();
        }

        boolean flag2 = outEdgesOfSootMethod.add(e);

        outEdgesOfThisMethod.put(src, outEdgesOfSootMethod);

        boolean flag = flag1 && flag2;
        allEdges.add(e);
        allMethods.add(src);
        allMethods.add(tgt);




        return flag;


    }

    @Override
    public boolean removeEdge(Edge e) {

        SootMethod src = e.src();
        SootMethod tgt = e.tgt();
        Set<Edge> outEdges = outEdgesOfThisMethod.get(src);
        boolean flag1 = true;
        if (outEdges != null) {
            flag1 = outEdges.remove(e);
        } else {
            flag1 = false;
        }

        Set<Edge> inEdges = inEdgesOfThisMethod.get(tgt);
        boolean flag2 = true;
        if (inEdges != null) {
            flag2 = inEdges.remove(e);
        } else {

            flag2 = false;
        }

        boolean flag = flag1 && flag2;


        allEdges.remove(e);




        return flag;


    }

    private void removeSrcUnitInSootMethod(Edge edge, SootMethod src) {
        Set<MyPairUnitToEdge> myPairUnitToEdgeSet=unitRemainInSootMethodMap.get(src);
        for(Iterator<MyPairUnitToEdge> iterator = myPairUnitToEdgeSet.iterator(); iterator.hasNext();)
        {
            MyPairUnitToEdge myPairUnitToEdge=iterator.next();
            if(myPairUnitToEdge.outEdge.equals(edge))
            {
                iterator.remove();
                break;
            }

        }
    }

    @Override
    public Iterator<Edge> edgesOutOf(MethodOrMethodContext m) {

        Set<Edge> outEdges = outEdgesOfThisMethod.get(m.method());
        if (outEdges == null) {
            return null;
        }

        return outEdges.iterator();

    }

    @Override
    public Iterator<Edge> edgesInto(MethodOrMethodContext m) {
        Set<Edge> inEdges = inEdgesOfThisMethod.get(m.method());
        if (inEdges == null) {
            return null;
        } else {
            return inEdges.iterator();
        }
    }

    public void deleteRepeatEdge(SootMethod sootMethod, Edge edge, Map<SootMethod, Map<Unit, IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> allSootMethodsAllUnitsTargetUnitInMethodInfo) {//判断这个边是不是和其他边重复的。重复的就删除其他边

        boolean flagEdgeIsRemain = isEdgeRemain(sootMethod, edge);
        if(!flagEdgeIsRemain)
        {
            return;
        }

        Set<List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> unitInfoWithIdenticalMyUnitGraph = identicalMyUnitGraphSetOfMethodMap.get(sootMethod);

        int count = 0;//edge 出现在几个集团中
        for (List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> list : unitInfoWithIdenticalMyUnitGraph) {
            boolean flag = false;
            for (IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo targetUnitInMethodInfo : list) {
                if (targetUnitInMethodInfo.unit == edge.srcUnit())//这条边的起点在一个相同myUnitGraph的集合中
                {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                count = count + 1;
                for (IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo targetUnitInMethodInfo : list) {
                    Edge edgeTemp = targetUnitInMethodInfo.edge;
                    if (edgeTemp == null) {
                        throw new RuntimeException();
                    }
                    if ((!edgeTemp.equals(edge)) && edge.tgt() == edgeTemp.tgt())//这两条边有相同的tgt，但是 不是相同的
                    {



                        if(isEdgeRemain(sootMethod,edgeTemp))
                        {
                            removeEdge(edgeTemp);
                            removeSrcUnitInSootMethod(edgeTemp,sootMethod);

                        }


                    }
                }
            }
        }
        if (count > 1) {
            throw new RuntimeException();
        }

    }

    private boolean isEdgeRemain(SootMethod sootMethod, Edge edge) {
        boolean flagEdgeIsRemain=false;
        for(MyPairUnitToEdge myPairUnitToEdgeSet:unitRemainInSootMethodMap.get(sootMethod))
        {
            if(myPairUnitToEdgeSet.outEdge.equals(edge))
            {
                flagEdgeIsRemain=true;
                break;
            }
        }
        return flagEdgeIsRemain;
    }


    private Set<List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> getIdenticalMyUnitGraphTargetUnitSet(List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> targetUnitInMethodInfoFinalPaths) {

        List<Integer>[] arr = new ArrayList[targetUnitInMethodInfoFinalPaths.size()];

        for (int i = 0; i < arr.length; i++) {
            arr[i] = new ArrayList<>();
            arr[i].add(i);
        }
        for (int i = 0; i < targetUnitInMethodInfoFinalPaths.size(); i++) {//------------------可以再优化，如果之前比过和他同一集团的其他的，就不用再比了，直接取结果

            for (int j = 0; j < targetUnitInMethodInfoFinalPaths.size(); j++) {
                if (j == i) {
                    continue;
                }


                if (targetUnitInMethodInfoFinalPaths.get(i).myUnitGraph.equivTo(targetUnitInMethodInfoFinalPaths.get(j).myUnitGraph)) {
                    arr[i].add(j);
                }
            }
        }


        for (int i = 0; i < arr.length; i++) {//将最小的编号作为这个集合的标号，放在arr第一个元素
            int min = Integer.MAX_VALUE;
            int minIndex = -1;
            for (int j = 0; j < arr[i].size(); j++) {
                if (arr[i].get(j) < min) {
                    min = arr[i].get(j);
                    minIndex = j;
                }
            }
            arr[i].remove(minIndex);
            arr[i].add(0, min);

        }

        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < arr.length; i++) {

            if (arr[i].size() >= 2) {
                map.put(arr[i].get(0), arr[i]);
            }

        }

        Set<List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo>> identicalEdgeSets = new HashSet<>();

        for (Map.Entry<Integer, List<Integer>> entry : map.entrySet())

        {
            List<IntentConditionTransformSymbolicExcutation.TargetUnitInMethodInfo> oneIdenticalEdgeList = new ArrayList<>();
            List<Integer> list = entry.getValue();
            for (int temp : list) {
                oneIdenticalEdgeList.add(targetUnitInMethodInfoFinalPaths.get(temp));
            }

            identicalEdgeSets.add(oneIdenticalEdgeList);


        }
        return identicalEdgeSets;


    }

    public void exportGexf(String fileName) {

        CGExporter cgExporter = new CGExporter();

        for (SootMethod sootMethod : allMethods) {
            cgExporter.createNode(sootMethod.getBytecodeSignature());
        }

        for (Edge edge : allEdges) {
            it.uniroma1.dis.wsngroup.gexf4j.core.Node node1 = cgExporter.createNode(edge.src().getBytecodeSignature());
            it.uniroma1.dis.wsngroup.gexf4j.core.Node node2 = cgExporter.createNode(edge.tgt().getBytecodeSignature());
            cgExporter.linkNodeByID(edge.src().getBytecodeSignature(), edge.tgt().getBytecodeSignature());

        }

        cgExporter.exportMIG(fileName, "AnalysisAPKIntent/intentConditionSymbolicExcutationResults/");
    }

}
