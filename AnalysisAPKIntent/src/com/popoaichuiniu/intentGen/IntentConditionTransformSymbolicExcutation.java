package com.popoaichuiniu.intentGen;

import com.microsoft.z3.Z3Exception;
import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Util;
import com.popoaichuiniu.util.Config;
import org.javatuples.Pair;
import org.javatuples.Quartet;

import org.javatuples.Triplet;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.AbstractJimpleIntBinopExpr;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.internal.JimpleLocal;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentConditionTransformSymbolicExcutation extends SceneTransformer {

    private static boolean exeModelTest = false;

    private String appPath = null;

    Set<Triplet<Integer, String, String>> targets = new LinkedHashSet<Triplet<Integer, String, String>>();
    private boolean pathLimitEnabled = true;

    //private int finalPathsPreviousLimit = 1048576;//2^20

    private int finalPathsPreviousLimit = 50;//

    private boolean hasReachfinalPathSizeLimit = false;


    private static BufferedWriter ifWriter = null;


    private static BufferedWriter ifReducedWriter = null;


    private static BufferedWriter appReachFinalPathSizeLimitWriter = null;


    private static BufferedWriter reduceCFGAnalysisLimitWriter = null;

    private static BufferedWriter bufferWriterEAToProtectPath = null;


    /**
     * key: a Value corresponding to an Intent extra, value: the string representing the key of the extra data
     */
    private Map<Value, String> valueKeyMap = new LinkedHashMap<Value, String>();


    /**
     * key:extraDataSymbol,value:key  ///   xxx=ie.ge.*("ttt“),ie有可能是bundle或者intent
     */

    private Map<String, String> extraDataMapKey = new HashMap<>();

    /**
     * key: a symbol used to represent a Local, value: the Local represented by the symbol
     */
    private Map<String, Local> symbolLocalMap = new HashMap<String, Local>();


    /**
     * key: a symbol used to represent a Local, value: the defunit of the local
     */
    private Map<String, Unit> symbolDefUnitMap = new HashMap<String, Unit>();


    /**
     * key: a symbol used to represent a Local, value: the Local represented by the symbol
     */
    private Map<Local, String> localSymbolMap = new HashMap<Local, String>();


    private Map<String, String> extraFromMap = new HashMap<>();//key:extra   value:extra 从哪里得到, intent还是bundle


    Set<Pair<Unit, SootMethod>> infeasibleTargets = new LinkedHashSet<Pair<Unit, SootMethod>>();


    Map<SootMethod, Map<Unit, List<UnitPath>>> methodSummaries = new HashMap<SootMethod, Map<Unit, List<UnitPath>>>();


    Map<Set<Unit>, Intent> pathIntents = new LinkedHashMap<Set<Unit>, Intent>();


    public IntentConditionTransformSymbolicExcutation(String apkFilePath) {

        appPath = apkFilePath;


        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(apkFilePath + "_" + "UnitsNeedAnalysis.txt")));//----------------------------------------
        } catch (IOException e) {
            e.printStackTrace();
        }
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] strs = line.split("#");
                String sootMethodString = strs[0];
                String byteCode = strs[1];
                int intByteCode = Integer.parseInt(byteCode);
                String unitString = strs[2];
                targets.add(new Triplet<>(intByteCode, sootMethodString, unitString));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {


        AndroidCallGraphHelper androidCallGraphHelper = new AndroidCallGraphHelper(appPath, Config.androidJar);
        AndroidInfoHelper androidInfoHelper = new AndroidInfoHelper(appPath);


        List<SootMethod> ea_entryPoints = Util.getEA_EntryPoints(androidCallGraphHelper, androidInfoHelper);


        List<SootMethod> roMethods = Util.getMethodsInReverseTopologicalOrder(ea_entryPoints, androidCallGraphHelper.getCg());

        roMethods.add(androidCallGraphHelper.getEntryPoint());

        allSootMethodsAllUnitsTargetUnitInMethodInfo = new HashMap<>();


        for (SootMethod sootMethod : roMethods) {


            analysisSootMethod(sootMethod, androidCallGraphHelper.getCg(), ea_entryPoints, roMethods);


            try {
                ifWriter.flush();
                ifReducedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void analysisSootMethod(SootMethod sootMethod, CallGraph cg, List<SootMethod> ea_entryPoints, List<SootMethod> roMethods) {


        if (exeModelTest) {//----------------------------------------测试

            if (!sootMethod.getBytecodeSignature().contains("TestUnUsedIFBlockAlgorithm"))//-------------------------------
            {
                return;
            }

            //test
            //××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××
//            if (sootMethod.getBytecodeSignature().contains("onCreate")) {
//                HashSet<Unit> unitsSet = new HashSet<>(sootMethod.getActiveBody().getUnits());
//                assert unitsSet.size() == sootMethod.getActiveBody().getUnits().size();//
//
//                HashSet<Unit> xxx=new HashSet<>();
//                HashSet<Value> yyy=new HashSet<>();
//                HashSet<Value> zzz=new HashSet<>();
//                for(Unit unit:sootMethod.getActiveBody().getUnits())//每个unit都是唯一的,都是不同实例，即使unit.toString()相同
//                {
//                    if(unit.toString().contains("specialinvoke $r0.<com.example.lab418.testwebview2.TestUnUsedIFBlockAlgorithm: void test6(android.content.Intent)>($r2)"))
//                    {
//                        xxx.add(unit);
//                    }
//
//                    Stmt stmt= (Stmt) unit;
//                    if(stmt.containsInvokeExpr())
//                    {
//
//                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
//
//                        if(invokeExpr instanceof InstanceInvokeExpr)
//                        {
//                            InstanceInvokeExpr instanceInvokeExpr= (InstanceInvokeExpr) invokeExpr;
//                            yyy.add(instanceInvokeExpr.getBase());//每个变量在整个方法内是同一个一实例，
//                            // 但是表达式每一个都是唯一的。即使其toString()相同的。
//                        }
//
//                    }
//                    if(stmt instanceof DefinitionStmt)
//                    {
//                        zzz.add(((DefinitionStmt) stmt).getRightOp());
//                    }
//
//
//                }
//
//                System.out.println(xxx);
//                System.out.println(yyy);
//                System.out.println(zzz);
//
//
//
//            }

            //××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××××


        }


        System.out.println(sootMethod.getBytecodeSignature());

        Body body = sootMethod.getActiveBody();

        if (body != null) {
            PatchingChain<Unit> units = body.getUnits();


            if (!exeModelTest) {
                Map<Unit, List<Pair<SootMethod, Unit>>> targetUnitCallPathReverse = new HashMap<>();//key（unit）在value的sootMethod中的unit（sootMethod）调用


                for (Unit unit : units) {
                    boolean isNeedAnalysis = Util.unitNeedAnalysis(unit, sootMethod, targets);


                    if (isNeedAnalysis) {


                        findEAToTargetUnit(sootMethod, cg, unit, ea_entryPoints, roMethods);


                    }
                }
            }


            if (exeModelTest) {//----------------------------------------测试
                for (Unit unit : units) {
                    if (unit.toString().contains("sendTextMessage")) {

                        findEAToTargetUnit(sootMethod, cg, unit, ea_entryPoints, roMethods);

                    }

                }

            }


            try {
                Util.bufferedWriterCircleEntry.flush();
                appReachFinalPathSizeLimitWriter.flush();
                reduceCFGAnalysisLimitWriter.flush();
                bufferWriterEAToProtectPath.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


    }

    private void findEAToTargetUnit(SootMethod sootMethod, CallGraph cg, Unit unit, List<SootMethod> ea_entryPoints, List<SootMethod> roMethods) {

        Set<SootMethod> aboutUnitNeedMethods = new HashSet<SootMethod>();

        Util.getAllMethodsToThisSootMethod(sootMethod, cg, aboutUnitNeedMethods);


        HashSet<SootMethod> allSootMethodsInPathOfEAToTarget = new HashSet<>(roMethods);


        allSootMethodsInPathOfEAToTarget.retainAll(aboutUnitNeedMethods);

        MyCallGraph myCallGraph = new MyCallGraph(allSootMethodsInPathOfEAToTarget, cg, sootMethod, unit, this);

        myCallGraph.exportGexf(new File(appPath).getName());
        myCallGraph.reduced(allSootMethodsAllUnitsTargetUnitInMethodInfo);
//
//        myCallGraph.exportGexf("reduced_"+new File(appPath).getName());
//
//
//        Set<List<Pair<SootMethod, Unit>>> sootMethodCallFinalPaths = new HashSet<>();
//
//        getCallPathSootMethod(sootMethod, unit, new ArrayList<Pair<SootMethod, Unit>>(), myCallGraph, null, new HashSet<SootMethodEdge>(), sootMethodCallFinalPaths, ea_entryPoints);
//
//        System.out.println("sootMethodCallFinalPaths大小：" + sootMethodCallFinalPaths.size());
//
//
//        for (List<Pair<SootMethod, Unit>> callSootMethodPath : sootMethodCallFinalPaths) {
//
//
//            int sum = 1;
//            String writerStr = "";
//            for (Pair<SootMethod, Unit> sootMethodUnitPair : callSootMethodPath) {
//                SootMethod sootMethodTgt = sootMethodUnitPair.getValue0();
//                Unit unitTgt = sootMethodUnitPair.getValue1();
//
//                int pathCountInMethod = allSootMethodsAllUnitsTargetUnitInMethodInfo.get(sootMethodTgt).get(unitTgt).unitPaths.size();
//                writerStr = writerStr + " " + pathCountInMethod;
//
//                sum = sum * pathCountInMethod;
//
//
//            }
//
//            writerStr = sum + " " + writerStr + "\n";
//
//            try {
//                bufferWriterEAToProtectPath.write(writerStr);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//        }
    }


    public Map<SootMethod, Map<Unit, TargetUnitInMethodInfo>> allSootMethodsAllUnitsTargetUnitInMethodInfo = null;

    public int doAnalysisOnUnit(MyCallGraph.MyPairUnitToEdge myPairUnitToEdge, SootMethod sootMethod, IntentFlowAnalysis intentFlowAnalysis) {


        System.out.println(myPairUnitToEdge.srcUnit.toString());


        List<List<Unit>> finalPaths = new ArrayList<>();

        UnitGraph ug = new BriefUnitGraph(sootMethod.getActiveBody());


        if (!pathLimitEnabled) {
            finalPathsPreviousLimit = Integer.MAX_VALUE;
        }
        hasReachfinalPathSizeLimit = false;

        //getAllPathInMethod(unit, null, ug, finalPaths, new ArrayList<Unit>(), new HashSet<UnitEdge>());//欧拉路径

        if (hasReachfinalPathSizeLimit) {

            System.out.println("finalPaths数量达到限制");

            try {
                appReachFinalPathSizeLimitWriter.write(appPath + "#" + sootMethod + "#" + myPairUnitToEdge.srcUnit + "\n");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        System.out.println("finalPaths.size():" + finalPaths.size());


        List<List<Unit>> finalPathsReduced = new ArrayList<>();

        MyUnitGraph myUnitGraph = Util.getReducedCFG(sootMethod, ug, intentFlowAnalysis, myPairUnitToEdge.srcUnit);

        hasReachfinalPathSizeLimit = false;

        getAllPathInMethod(myPairUnitToEdge.srcUnit, null, myUnitGraph, finalPathsReduced, new ArrayList<Unit>(), new HashSet<UnitEdge>());


        if (hasReachfinalPathSizeLimit) {

            System.out.println("化简CFG的finalPathsReduced达到限制！");

            try {
                reduceCFGAnalysisLimitWriter.write(appPath + "#" + sootMethod + "#" + myPairUnitToEdge.srcUnit + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        System.out.println(finalPaths.size() + "*******************************" + finalPathsReduced.size() + "************************" + (finalPaths.size() - finalPathsReduced.size()) + " " + myPairUnitToEdge.srcUnit);


        try {
            ifReducedWriter.write(finalPaths.size() + "*******************************" + finalPathsReduced.size() + "************************" + (finalPaths.size() - finalPathsReduced.size()) + "&&&&&&&&&" + myUnitGraph.getAllUnit().size() + " " + sootMethod.getName() + " " + myPairUnitToEdge.srcUnit + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


        Map<Unit, TargetUnitInMethodInfo> allUnitsTargetUnitInMethodInfo = allSootMethodsAllUnitsTargetUnitInMethodInfo.get(sootMethod);

        if (allUnitsTargetUnitInMethodInfo == null) {
            allUnitsTargetUnitInMethodInfo = new HashMap<>();
        }

        HashSet<UnitPath> unitPaths = new HashSet<>();
        for (List<Unit> onePath : finalPathsReduced) {
            unitPaths.add(new UnitPath(new HashSet<>(), new HashSet<>(), onePath));
        }
        allUnitsTargetUnitInMethodInfo.put(myPairUnitToEdge.srcUnit, new TargetUnitInMethodInfo(myPairUnitToEdge, sootMethod, myUnitGraph, unitPaths));

        allSootMethodsAllUnitsTargetUnitInMethodInfo.put(sootMethod, allUnitsTargetUnitInMethodInfo);


        if (hasReachfinalPathSizeLimit) {
            return -1;
        } else {
            return myUnitGraph.getAllUnit().size();
        }


        //-------------------------------分析每条路径-------------------------------------------//

//        List<UnitPath> intraUnitPaths = new ArrayList<UnitPath>();
//        Map<Unit, List<UnitPath>> unitSum = null;
//        Map<Set<Unit>, UnitPath> pathMapUnitPathConds = new LinkedHashMap<Set<Unit>, UnitPath>();//key：path ----------------value：路径的条件
//        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%" + sootMethod.getBytecodeSignature() + "%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//        for (Set<Unit> path : finalPaths) {
//            Set<String> currPathCond = new LinkedHashSet<String>();
//            Set<String> currDecls = new LinkedHashSet<String>();
//            analysisPathIntraMethod(path, sootMethod, defs, cg, intentFlowAnalysis, currPathCond, currDecls);
//
//            System.out.println("33333333333333333333333333333333333");
//            currDecls.stream().forEach(System.out::println);
//
//            currPathCond.stream().forEach(System.out::println);
//
//            System.out.println("44444444444444444444444444444444444");
//
//            UnitPath up = new UnitPath(currPathCond, currDecls, path);
//            intraUnitPaths.add(up);
//
//            if (methodSummaries.containsKey(sootMethod)) {
//                unitSum = methodSummaries.get(sootMethod);//unitSum  key:startingUnit  value:method enrty to startingUnit  all path
//            } else {
//                unitSum = new HashMap<Unit, List<UnitPath>>();
//            }
//
//            List<UnitPath> unitPaths = null;
//            if (unitSum.containsKey(unit)) {
//                unitPaths = unitSum.get(unit);
//            } else {
//                unitPaths = new ArrayList<UnitPath>();
//            }
//            unitPaths.add(up);
//            unitSum.put(unit, unitPaths);
//
//            methodSummaries.put(sootMethod, unitSum);
//
//            pathMapUnitPathConds.put(path, up);
//
//
//        }
//
//        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
//        //--------------------------------
//
//
//        //对路径中的方法处理，其可能有约束(如果不解决的话，可能中间崩溃)
//
//
//        //--------------------------------
//
//        boolean isFeasibleUnit = false;
//
//
//        for (Map.Entry<Set<Unit>, UnitPath> pathMapPathCondEntry : pathMapUnitPathConds.entrySet()) {
//            Set<Unit> onePath = pathMapPathCondEntry.getKey();
//
//            UnitPath oneUnitPath = pathMapPathCondEntry.getValue();
//
//            boolean isFeasible = runSolvingPhase(sootMethod, onePath, oneUnitPath.getPathCond(), oneUnitPath.getDecl());
//
//            if (!isFeasible) {
//                infeasibleTargets.add(new Pair<Unit, SootMethod>(unit, sootMethod));
//            } else {
//                isFeasibleUnit = true;
//            }
//
//
//        }
//
//
//        return isFeasibleUnit;


    }


    class SootMethodEdge {
        SootMethod src;
        SootMethod tgt;


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SootMethodEdge that = (SootMethodEdge) o;
            return Objects.equals(src, that.src) &&
                    Objects.equals(tgt, that.tgt);
        }

        @Override
        public int hashCode() {

            return Objects.hash(src, tgt);
        }

        public SootMethodEdge(SootMethod src, SootMethod tgt) {
            this.src = src;
            this.tgt = tgt;
        }
    }

    private void getCallPathSootMethod(SootMethod sootMethod, Unit unit, List<Pair<SootMethod, Unit>> callSootMethodUnitPairPath, CallGraph cg, SootMethodEdge sootMethodEdge, Set<SootMethodEdge> sootMethodEdgeSet, Set<List<Pair<SootMethod, Unit>>> sootMethodCallFinalPaths, List<SootMethod> ea_entryPoints) {


        System.out.println("######" + sootMethod.getBytecodeSignature());
//
//        if(callSootMethodUnitPairPath.size()>=10)
//        {
//            return;
//        }


//        if(sootMethodCallFinalPaths.size()>20)
//        {
//            return;
//        }

        if (!cg.edgesInto(sootMethod).hasNext()) {
            if (sootMethod.getBytecodeSignature().equals("<dummyMainClass: dummyMainMethod([Ljava/lang/String;)V>")) {


                sootMethodCallFinalPaths.add(callSootMethodUnitPairPath);
                System.out.println("路径长度：" + callSootMethodUnitPairPath.size());


                return;
            } else {
                throw new RuntimeException("错误");
            }
        }


        List<Pair<SootMethod, Unit>> callSootMethodUnitPairPathCopy = new ArrayList<>(callSootMethodUnitPairPath);
        callSootMethodUnitPairPathCopy.add(new Pair<SootMethod, Unit>(sootMethod, unit));

        Set<SootMethodEdge> sootMethodEdgeSetCopy = new HashSet<>(sootMethodEdgeSet);

        sootMethodEdgeSetCopy.add(sootMethodEdge);


        for (Iterator<Edge> iteratorSootMethod = cg.edgesInto(sootMethod); iteratorSootMethod.hasNext(); ) {
            Edge edgeSootMethod = iteratorSootMethod.next();

            SootMethod sootMethodSrc = edgeSootMethod.getSrc().method();
            Unit srcUnit = edgeSootMethod.srcUnit();
//            System.out.println(edgeSootMethod.getSrc());
//            System.out.println(edgeSootMethod.srcUnit());


            SootMethodEdge sootMethodEdgeNew = new SootMethodEdge(sootMethod, sootMethodSrc);
            if ((!sootMethodEdgeSetCopy.contains(sootMethodEdgeNew))) {
                getCallPathSootMethod(sootMethodSrc, srcUnit, callSootMethodUnitPairPathCopy, cg, sootMethodEdgeNew, sootMethodEdgeSetCopy, sootMethodCallFinalPaths, ea_entryPoints);
            }


        }
        System.out.println("stopstopstop");
    }


    protected boolean runSolvingPhase(SootMethod method, Set<Unit> currPath, Set<String> interPathCond, Set<String> interDecls) {

        Pair<Intent, Boolean> soln = findSolutionForPath(interPathCond, method, interDecls, currPath);
        boolean feasible = soln.getValue1();
        Intent genIntent = soln.getValue0();

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!" + currPath.iterator().next() + "!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        if (feasible) {
            System.out.println(genIntent);
        } else {
            System.out.println("路径不可行");
        }
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        return feasible;
    }

    public Pair<Intent, Boolean> findSolutionForPath(Set<String> currPathCond,
                                                     SootMethod method,
                                                     Set<String> decls,
                                                     Set<Unit> currPath) {
        Set<Quartet<String, String, String, String>> extraData = new LinkedHashSet<Quartet<String, String, String, String>>();
        String action = null;
        Set<String> categories = new LinkedHashSet<String>();
        boolean isPathFeasible = false;

        try {
            Pair<Map<String, String>, Boolean> ret = returnSatisfyingModel(decls, currPathCond);
            Map<String, String> model = ret.getValue0();
            Boolean isSat = ret.getValue1();
            if (!isSat) {
                //logger.debug("path is infeasible");
                isPathFeasible = false;
            } else {
                //logger.debug("path is feasible---here is a solution");
                isPathFeasible = true;

                Map<String, String> intentActionSymbols = new LinkedHashMap<String, String>();
                for (String expr : currPathCond) {
                    Pattern p = Pattern.compile("\\(assert \\(= \\(getAction (.+)\\) (.+)\\)\\)");
                    Matcher m = p.matcher(expr);
                    while (m.find()) {
                        String intentSymbol = m.group(1);
                        //logger.info("intent symbol for action: " + intentSymbol);

                        String actionStrSymbol = m.group(2);
                        //logger.info("action symbol: " + actionStrSymbol);

                        intentActionSymbols.put(intentSymbol, actionStrSymbol);//
                    }
                }

                Map<String, List<String>> intentOrBundleLocalKeys = new LinkedHashMap<String, List<String>>();
                for (String expr : currPathCond) {
                    Pattern p = Pattern.compile("\\(assert \\(= \\(containsKey (.+) \\\"(.+)\\\"\\) true\\)\\)");
                    Matcher m = p.matcher(expr);
                    while (m.find()) {
                        String intentOrBundleLocalSymbol = m.group(1);

                        String key = m.group(2);

                        List<String> keys = intentOrBundleLocalKeys.get(intentOrBundleLocalSymbol);


                        if (keys == null) {
                            keys = new ArrayList<String>();
                        }

                        keys.add(key);
                        intentOrBundleLocalKeys.put(intentOrBundleLocalSymbol, keys);

                    }
                }

                Map<String, String> extraDataFromIntent = new LinkedHashMap<>();
                for (String expr : currPathCond) {
                    //"(assert (= (fromIntent " + extraLocalSymbol + ") " + intentSymbol + "))"
                    Pattern p = Pattern.compile("\\(assert\\s+\\(=\\s+\\(fromIntent\\s+(\\S+)\\)\\s+(\\S+)\\)\\)");
                    Matcher m = p.matcher(expr);
                    while (m.find()) {

                        String all = m.group(0);
                        //System.out.println(all);
                        String extraDataSymbol = m.group(1);

                        //System.out.println(dataSymbol);
//
                        String intentSymbol = m.group(2);

                        //System.out.println(intentSymbol);
                        extraDataFromIntent.put(extraDataSymbol, intentSymbol);

                    }

                }


                Set<Quartet<String, String, String, String>> tempIntentExtraData = new LinkedHashSet<Quartet<String, String, String, String>>();
                Map<String, Quartet<String, String, String, String>> tempBundleExtraDataMap = new HashMap<String, Quartet<String, String, String, String>>();
                for (Map.Entry<String, String> entry : model.entrySet()) {//model里的是一个变量和一个值 变量=值

                    String symbol = entry.getKey();
                    String generatedValue = entry.getValue();//  由z3推断出来的symbol=generatedValue

                    String key = extraDataMapKey.get(symbol);

                    if (key != null)//这里key！=null 则这个 symbol是extraData变量
                    {

                        String extraFrom = extraFromMap.get(symbol);

                        if (extraFrom != null) {
                            if (extraFrom.contains("Bundle"))//extraData from bundle
                            {
                                Local local = symbolLocalMap.get(symbol);
                                if (local != null) {
                                    Quartet<String, String, String, String> quartet = new Quartet<>("BundleKey", local.getType().toString(), key, generatedValue);
                                    tempBundleExtraDataMap.put(extraFrom, quartet);
                                }

                            } else if (extraFrom.contains("Intent"))//extraData from Intent
                            {
                                Local local = symbolLocalMap.get(symbol);
                                if (local != null) {
                                    if (local.getType().toString().equals("android.os.Bundle"))//这个intent 的extra是bundle
                                    {
                                        Quartet<String, String, String, String> quartet = new Quartet<>("IntentKey", local.getType().toString(), key, symbol);
                                        tempIntentExtraData.add(quartet);
                                    } else {
                                        Quartet<String, String, String, String> quartet = new Quartet<>("IntentKey", local.getType().toString(), key, generatedValue);
                                        tempIntentExtraData.add(quartet);
                                    }

                                }


                            } else {
                                throw new RuntimeException("extraData is not from intent or bundle");
                            }
                        }

                    }


                    for (String actionSymbol : intentActionSymbols.values()) {//-----------action的值求解---------
                        if (actionSymbol.equals(symbol)) {
                            action = generatedValue.replaceAll("^\"|\"$", "");// ^ Matches the beginning of the line.
                        }
                    }

                }
                for (Quartet<String, String, String, String> intentExtra : tempIntentExtraData) {
                    if (intentExtra.getValue1().equals("android.os.Bundle"))//得到bundle的具体值。而不是仅仅非空
                    {

                        String bundleExtraLocal = intentExtra.getValue3();
                        Quartet quartet = tempBundleExtraDataMap.get(bundleExtraLocal);
                        if (quartet != null) {
                            //intentExtra=intentExtra.setAt3(quartet.getValue0()+":"+quartet.getValue1()+":"+quartet.getValue2()+":"+quartet.getValue3());
                            intentExtra = intentExtra.setAt3(quartet.toString());
                        } else {
                            intentExtra = intentExtra.setAt3("NotNull");
                        }

                        extraData.add(intentExtra);

                    } else {
                        extraData.add(intentExtra);
                    }
                }


//                for (Map.Entry<String, String> entry : model.entrySet()) {//model里的是一个变量和一个值 变量=值
//                    String symbol = entry.getKey();
//                    String generatedValue = entry.getValue();
//                    //logger.debug(symbol + ": " + generatedValue);
//
//                    Quartet<String, String, String, String> genDatum = generateDatum(symbol, generatedValue, extraLocalKeys);//获取extra的值
//					/*if (genDatum == null) {
//						logger.warn("Skipping generation of extra datum for " + symbol);
//						continue;
//					}*/
//
//                    Quartet<String, String, String, String> extraDatum = genDatum;//这个key -value不一定是intent的可能是bundle的，而这个bundle是intent的
//                    if (extraDatum != null) {
//                        extraData.add(extraDatum);
//                    }
//
//                    for (String actionSymbol : intentActionSymbols.values()) {//-----------action的值求解---------
//                        if (actionSymbol.equals(symbol)) {
//                            action = generatedValue.replaceAll("^\"|\"$", "");// ^ Matches the beginning of the line.
//                        }
//                    }
//                }

                for (String expr : currPathCond) {
                    Pattern p = Pattern.compile("\\(assert \\(exists \\(\\(index Int\\)\\) \\(= \\(select cats index\\) \\\"(.+)\\\"\\)\\)\\)");
                    Matcher m = p.matcher(expr);
                    while (m.find()) {
                        String category = m.group(1);
                        //logger.info("Found category: " + category);
                        categories.add(category);
                    }
                }

                // I cannot generate an Intent for an extra datum if I don't know it's type
				/*for (String expr : currPathCond) {
					Pattern p = Pattern.compile("\\(assert \\(exists \\(\\(index Int\\)\\) \\(= \\(select keys index\\) \\\"(.+)\\\"\\)\\)\\)");
					Matcher m = p.matcher(expr);
					while (m.find()) {
						String key = m.group(1);
						logger.info(("Found extra key: " + key));
						Triplet<String,String,String> extraDatum = new Triplet(null,key,null);
						extraData.add(extraDatum);
					}
				}*/
                //logger.debug("");
            }
        } catch (Z3Exception e) {
            e.printStackTrace();
        }

        Intent genIntent = new Intent();
        genIntent.extras = new LinkedHashSet<>(extraData);
        genIntent.action = action;
        genIntent.categories = categories;
        genIntent.targetComponent = method.getDeclaringClass().getName();

        Intent modIntent = modifyGeneratedIntent(genIntent);

        if (pathIntents.containsKey(currPath)) {
            Intent prevIntent = pathIntents.get(currPath);
            //logger.debug("Replacing " + prevIntent + " with " + modIntent);
        }
        pathIntents.put(currPath, modIntent);

        return new Pair<Intent, Boolean>(modIntent, isPathFeasible);
    }

    protected Intent modifyGeneratedIntent(Intent genIntent) {
        return genIntent;
    }

    public Quartet<String, String, String, String> generateDatum(String symbol, String generatedValue, Map<String, String> extraLocalKeys) {

        Quartet<String, String, String, String> extraDatum = null;//


        Local local = symbolLocalMap.get(symbol);
        String key = extraLocalKeys.get(symbol);


        if (local != null && key != null) {


            String sourceOfExtra = extraFromMap.get(symbol);
            if (sourceOfExtra != null) {
                Local sourceOfExtraLocal = symbolLocalMap.get(sourceOfExtra);

                if (sourceOfExtraLocal != null) {
                    if (sourceOfExtraLocal.getType().toString().equals("android.content.Intent")) {
                        extraDatum = new Quartet<String, String, String, String>("intentKey", local.getType().toString(), key, generatedValue.toString().replaceAll("^\"|\"$", ""));
                    } else if (sourceOfExtraLocal.getType().toString().equals("android.os.Bundle")) {
                        extraDatum = new Quartet<String, String, String, String>("bundleKey_" + symbolDefUnitMap.get(sourceOfExtra), local.getType().toString(), key, generatedValue.toString().replaceAll("^\"|\"$", ""));

                    }
                }

            }


        } else {
            extraDatum = null;
        }
        return extraDatum;
    }

    Pair<Map<String, String>, Boolean> returnSatisfyingModel(Set<String> decls, Set<String> pathCond) throws Z3Exception {
        return returnSatisfyingModelForZ3(decls, pathCond);
    }

    Pair<Map<String, String>, Boolean> returnSatisfyingModelForZ3(Set<String> decls, Set<String> pathCond) throws Z3Exception {
        String pathCondFileName = null;
        try {
            pathCondFileName = Config.Z3_RUNTIME_SPECS_DIR + File.separator + "z3_path_cond";
            PrintWriter out = new PrintWriter(pathCondFileName);
            String outSpec = "";
            outSpec += "(declare-datatypes () ((Object Null NotNull)))\n" +
                    "(declare-fun containsKey (Object String) Bool)\n" +
                    "(declare-fun containsKey (String String) Bool)\n" +
                    "(declare-fun containsKey (Int String) Bool)\n" +
                    "(declare-fun containsKey (Real String) Bool)\n" +

                    "(declare-fun getAction (Object) String)\n" +

                    "(declare-fun fromIntent (Object) Object)\n" +//extraData  FromIntent
                    "(declare-fun fromIntent (String) Object)\n" +
                    "(declare-fun fromIntent (Int) Object)\n" +
                    "(declare-fun fromIntent (Real) Object)\n" +


                    "(declare-fun fromBundle (Object) Object)\n" +
                    "(declare-fun fromBundle (String) Object)\n" +
                    "(declare-fun fromBundle (Int) Object)\n" +
                    "(declare-fun fromBundle (Real) Object)\n" +

                    "(declare-datatypes () ((ParamRef (mk-paramref (index Int) (type String) (method String)))))\n" +
                    "(declare-fun hasParamRef (Object) ParamRef)\n" +
                    "(declare-fun hasParamRef (String) ParamRef)\n" +
                    "(declare-fun hasParamRef (Int) ParamRef)\n" +
                    "(declare-fun hasParamRef (Real) ParamRef)\n" +

                    "(declare-fun isNull (String) Bool)\n" +
                    "(declare-fun isNull (Object) Bool)\n" +
                    "(declare-fun oEquals (String Object) Bool)\n" +
                    "(declare-fun oEquals (Object String) Bool)\n" +
                    "(declare-const cats (Array Int String))\n" +
                    "(declare-const keys (Array Int String))\n";
            for (String d : decls) {
                outSpec += d + "\n";
            }
            for (String c : pathCond) {
                outSpec += c + "\n";
            }
            outSpec += "(check-sat-using (then qe smt))\n";
            outSpec += "(get-model)\n";
            //logger.debug("z3 specification sent to solver:");
            //logger.debug(outSpec);
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&finalSolve&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
            System.out.print(outSpec);
            System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
            out.print(outSpec);

            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        ProcessBuilder pb = new ProcessBuilder("/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/z3-4.6.0-x64-ubuntu-16.04/bin/z3", pathCondFileName);

        Process p = null;
        String returnedOutput = null;
        try {
            p = pb.start();
            int errCode = p.waitFor();

            returnedOutput = convertStreamToString(p.getInputStream());
            System.out.println(returnedOutput);

            if (errCode != 0) {
                String errorOut = convertStreamToString(p.getErrorStream());
                System.out.println("errCode:" + errCode + "*" + errorOut + "*");
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, String> model = new LinkedHashMap<String, String>();
        Pattern pat = Pattern.compile("\\s+\\(define-fun\\s+(\\S+)\\s+\\(\\)\\s+\\w+\\s+(.+)(?=\\))");//取出常数放入model
        //\\s+   多个空白（Matches the whitespace. Equivalent to [\t\n\r\f].）
        //(\S+) ：m.group(1)  函数名
        //  \\(\\)   （）
        //  \\w+  多个Matches the word characters. 返回类型
        //   (.+)  m.group(2)  .: Matches any single character except newline. Using m option allows it to match the newline as well.
        //   (?=\\)) 最后是一个括号,不占匹配的字符
        Matcher m = pat.matcher(returnedOutput);
        while (m.find()) {
            String symbol = m.group(1);
            String value = m.group(2);
            model.put(symbol, value);
        }

        String[] outLines = returnedOutput.split("\\n");
        Boolean isSat = false;
        for (String line : outLines) {
            if (line.trim().equals("sat"))
                isSat = true;
        }
        return new Pair<Map<String, String>, Boolean>(model, isSat);

    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");// \A表示字符串的开头 为了消除\的转义作用,多加一个\
        return s.hasNext() ? s.next() : "";
    }

    class TargetUnitInMethodInfo {

        Unit unit;

        Edge edge;

        SootMethod sootMethod;

        MyUnitGraph myUnitGraph;

        Set<UnitPath> unitPaths;

        public TargetUnitInMethodInfo(MyCallGraph.MyPairUnitToEdge myPairUnitToEdge, SootMethod sootMethod, MyUnitGraph myUnitGraph, Set<UnitPath> unitPaths) {
            this.unit = myPairUnitToEdge.srcUnit;
            this.edge=myPairUnitToEdge.outEdge;
            this.sootMethod = sootMethod;
            this.myUnitGraph = myUnitGraph;
            this.unitPaths = unitPaths;
        }
    }

    class UnitPath {
        Set<String> pathCond;
        Set<String> decl;
        List<Unit> path;


        public UnitPath(Set<String> currPathCond, Set<String> currDecls, List<Unit> currPath) {
            this.pathCond = currPathCond;
            this.decl = currDecls;
            this.path = currPath;

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UnitPath unitPath = (UnitPath) o;

            if (!pathCond.equals(unitPath.pathCond)) return false;
            if (!decl.equals(unitPath.decl)) return false;
            return path.equals(unitPath.path);

        }

        @Override
        public int hashCode() {
            int result = pathCond.hashCode();
            result = 31 * result + decl.hashCode();
            result = 31 * result + path.hashCode();
            return result;
        }

        public Set<String> getPathCond() {
            return pathCond;
        }

        public void setPathCond(Set<String> pathCond) {
            this.pathCond = pathCond;
        }

        public Set<String> getDecl() {
            return decl;
        }

        public void setDecl(Set<String> decl) {
            this.decl = decl;
        }

        public List<Unit> getPath() {
            return path;
        }

        public void setPath(List<Unit> path) {
            this.path = path;
        }
    }

    private void analysisPathIntraMethod(Set<Unit> path, SootMethod sootMethod, SimpleLocalDefs defs, CallGraph cg, IntentFlowAnalysis intentFlowAnalysis, Set<String> currPathCond, Set<String> currDecls) {


        for (Unit unit : path) {
            Stmt stmt = (Stmt) unit;
            if (stmt instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) stmt;
                ConditionExpr conditionExpr = (ConditionExpr) ifStmt.getCondition();
                if (intentFlowAnalysis.getFlowBefore(ifStmt).contains(conditionExpr.getOp1()) || intentFlowAnalysis.getFlowBefore(ifStmt).contains(conditionExpr.getOp2())) {
                    Unit succUnit = null;
                    List<Unit> currPathList = new ArrayList<Unit>(path);
                    int indexOfUnit = currPathList.indexOf(unit);
                    if (indexOfUnit == -1) {
                        throw new RuntimeException(unit + " is not in path");
                    }
                    Unit succ = currPathList.get(indexOfUnit - 1);

                    Set<String> newExprs = handleIfStmt(ifStmt, path, sootMethod, cg, defs, currDecls, succ);
                    currPathCond.addAll(newExprs);

                }

            }

            if (stmt.containsInvokeExpr() && stmt instanceof DefinitionStmt) {//-------------------------加一下IntentFlowAnalysis
                handleGetActionOfIntent(sootMethod, path, currPathCond, currDecls, defs, stmt);//对intent的action语句进行声明,例如：xxx=ie.getAction()  定义Intent ie ，约束 xxx来源于ie的getAction
                handleGetExtraOfIntent(sootMethod, path, currPathCond, currDecls, defs, stmt);//

                //category
                //data


            }


        }


    }


    private void handleGetExtraOfIntent(SootMethod method, Set<Unit> currPath, Set<String> currPathCond, Set<String> currDecls, SimpleLocalDefs defs, Stmt currStmtInPath) {
        DefinitionStmt defStmt = (DefinitionStmt) currStmtInPath;
        if (defStmt.containsInvokeExpr() && defStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
            InstanceInvokeExpr ie = (InstanceInvokeExpr) defStmt.getInvokeExpr();
            if (Pattern.matches("get.*Extra", ie.getMethod().getName())) {
                if (ie.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    Pair<Set<String>, Set<String>> exprPair = buildGetExtraData(defStmt, defs, ie, method, currPath);
                    currDecls.addAll(exprPair.getValue0());
                    currPathCond.addAll(exprPair.getValue1());
                }
            }
            if (Pattern.matches("get.*", ie.getMethod().getName())) {//defStmt:xxx=ie.get.*()  ie:android.os.Bundle
                if (ie.getMethod().getDeclaringClass().toString().equals("android.os.Bundle")) {
                    Pair<Set<String>, Set<String>> exprPair = buildGetBundleData(defStmt, defs, ie, method, currPath);
                    currDecls.addAll(exprPair.getValue0());
                    currPathCond.addAll(exprPair.getValue1());
                }
            }
        }
    }

    private Pair<Set<String>, Set<String>> buildGetBundleData(Unit currUnit, SimpleLocalDefs defs, InstanceInvokeExpr ie, SootMethod method, Set<Unit> currPath) {//ok
        Set<String> newDecls = new LinkedHashSet<String>();//currUnit:xxx=ie.get.*(arg1,arg2)   ie:android.os.Bundle
        Set<String> newAsserts = new LinkedHashSet<String>();
        Value arg1 = ie.getArg(0);
        Value arg2 = null;
        if (ie.getArgCount() > 1) {
            arg2 = ie.getArg(1);
        }

        String extraType = null;

        if (arg2 != null) {
            extraType = arg2.getType().toString();
        } else {
            extraType = ie.getMethod().getReturnType().toString();
        }

        String arg2Str = "unk";
        if (arg2 != null) {
            arg2Str = arg2.toString();
        }

        if (arg1 instanceof StringConstant) {
            StringConstant keyStrConst = (StringConstant) arg1;
            if (ie.getBase() instanceof Local) {
                Local bundleLocal = (Local) ie.getBase();
                for (Unit bundleDef : defs.getDefsOfAt(bundleLocal, currUnit)) {
					/*if (!currPath.contains(intentDef)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, bundleDef, bundleLocal, currUnit, defs)) {
                        continue;
                    }
                    DefinitionStmt bundleDefStmt = (DefinitionStmt) bundleDef;//bundleDefStmt:bundle对象的定义语句
                    if (bundleDefStmt.containsInvokeExpr()) {
                        if (bundleDefStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                            InstanceInvokeExpr iie = (InstanceInvokeExpr) bundleDefStmt.getInvokeExpr();
                            if (iie.getBase().getType().toString().equals("android.content.Intent")) {//iie:android.content.Intent
                                if (iie.getBase() instanceof Local) {
                                    Local intentLocal = (Local) iie.getBase();
                                    for (Unit intentDef : defs.getDefsOfAt(intentLocal, bundleDefStmt)) {
                                        if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, bundleDefStmt, defs)) {
                                            continue;
                                        }

                                        if (currUnit instanceof DefinitionStmt) {
                                            DefinitionStmt defStmt = (DefinitionStmt) currUnit;////currUnit:xxx=ie.get.*(arg1,arg2)   ie:android.os.Bundle
                                            if (defStmt.getLeftOp() instanceof Local) {//
                                                Local extraLocal = (Local) defStmt.getLeftOp();//extraLocal 是xxx,保存extra值的变量
                                                String extraLocalSymbol = createSymbol(extraLocal, method, defStmt);
                                                symbolLocalMap.put(extraLocalSymbol, extraLocal);
                                                symbolDefUnitMap.put(extraLocalSymbol, defStmt);
                                                String intentSymbol = createSymbol(intentLocal, method, intentDef);//包括intent在哪个位置定义的（什么类，什么方法，方法中哪个位置）
                                                symbolLocalMap.put(intentSymbol, intentLocal);
                                                symbolDefUnitMap.put(intentSymbol, intentDef);
                                                String bundleLocalSymbol = createSymbol(bundleLocal, method, bundleDef);
                                                symbolLocalMap.put(bundleLocalSymbol, bundleLocal);
                                                symbolDefUnitMap.put(bundleLocalSymbol, bundleDef);
                                                String newExtraType = getZ3Type(extraLocal.getType());
                                                String newIntentType = getZ3Type(intentLocal.getType());
                                                newDecls.add("(declare-const " + extraLocalSymbol + " " + newExtraType + " )");
                                                newDecls.add("(declare-const " + intentSymbol + " " + newIntentType + " )");

                                                if (iie.getMethod().getName().equals("getExtras")) {
                                                    newAsserts.add("(assert (= (containsKey " + intentSymbol + " \"" + keyStrConst.value + "\") true))");
                                                    newAsserts.add("(assert (= (fromIntent " + extraLocalSymbol + ") " + intentSymbol + "))");
                                                    extraFromMap.put(extraLocalSymbol, intentSymbol);
                                                    extraDataMapKey.put(extraLocalSymbol, keyStrConst.value);
                                                } else if (iie.getMethod().getName().equals("getBundleExtra")) {//已经在intent get*Extra做过了bundeldata=intent.getBundleExtra()

                                                    newAsserts.add("(assert (= (containsKey " + bundleLocalSymbol + " \"" + keyStrConst.value + "\") true))");
                                                    extraFromMap.put(extraLocalSymbol, bundleLocalSymbol);
                                                    extraDataMapKey.put(extraLocalSymbol, keyStrConst.value);
                                                    newAsserts.add("(assert (= (fromBundle " + extraLocalSymbol + ") " + bundleLocalSymbol + "))");

//                                                    extraFromMap.put(bundleLocalSymbol, intentSymbol);//已经被做过了
//                                                    Value getBundleExtraArg=iie.getArg(0);
//                                                    if(getBundleExtraArg instanceof StringConstant)
//                                                    {
//
//                                                        extraDataMapKey.put(bundleLocalSymbol,((StringConstant) getBundleExtraArg).value);
//                                                    }
//                                                    newAsserts.add("(assert (= (fromIntent " + bundleLocalSymbol + ") " + intentSymbol + "))");

                                                }


                                                buildParamRefExpressions(method, currPath, newAsserts, newDecls, intentDef, intentSymbol);
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        return new Pair<Set<String>, Set<String>>(newDecls, newAsserts);
    }

    private Pair<Set<String>, Set<String>> buildGetExtraData(Unit currUnit, SimpleLocalDefs defs, InstanceInvokeExpr ie, SootMethod method, Set<Unit> currPath) {//ok
        Set<String> newDecls = new LinkedHashSet<String>();
        Set<String> newAsserts = new LinkedHashSet<String>();
        Value arg1 = ie.getArg(0);
        Value arg2 = null;
        if (ie.getArgCount() > 1) {
            arg2 = ie.getArg(1);
        }

        String extraType = null;

        if (arg2 != null) {
            extraType = arg2.getType().toString();//默认值
        } else {
            extraType = ie.getMethod().getReturnType().toString();
        }

        String arg2Str = "unk";
        if (arg2 != null) {
            arg2Str = arg2.toString();
        }

        if (arg1 instanceof StringConstant) {
            StringConstant keyStrConst = (StringConstant) arg1;
            if (ie.getBase() instanceof Local) {
                Local intentLocal = (Local) ie.getBase();
                for (Unit intentDef : defs.getDefsOfAt(intentLocal, currUnit)) {//currUnit  例如：xxx=ie.getStringExtra("zz","t")
					/*if (!currPath.contains(intentDef)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, currUnit, defs)) {
                        continue;
                    }

                    if (currUnit instanceof DefinitionStmt) {
                        DefinitionStmt defStmt = (DefinitionStmt) currUnit;
                        if (defStmt.getLeftOp() instanceof Local) {//
                            Local extraLocal = (Local) defStmt.getLeftOp();
                            String extraLocalSymbol = createSymbol(extraLocal, method, defStmt);
                            symbolLocalMap.put(extraLocalSymbol, extraLocal);
                            symbolDefUnitMap.put(extraLocalSymbol, defStmt);
                            String intentSymbol = createSymbol(intentLocal, method, intentDef);
                            symbolLocalMap.put(intentSymbol, intentLocal);
                            symbolDefUnitMap.put(intentSymbol, intentDef);
                            String newExtraType = getZ3Type(extraLocal.getType());
                            String newIntentType = getZ3Type(intentLocal.getType());
                            newDecls.add("(declare-const " + extraLocalSymbol + " " + newExtraType + " )");
                            newDecls.add("(declare-const " + intentSymbol + " " + newIntentType + " )");

                            newAsserts.add("(assert (= (containsKey " + intentSymbol + " \"" + keyStrConst.value + "\") true))");
                            newAsserts.add("(assert (= (fromIntent " + extraLocalSymbol + ") " + intentSymbol + "))");
                            extraFromMap.put(extraLocalSymbol, intentSymbol);
                            extraDataMapKey.put(extraLocalSymbol, keyStrConst.value);
                            //addIntentExtraForPath(currPath, keyStrConst.value, newExtraType);


                            buildParamRefExpressions(method, currPath, newAsserts, newDecls, intentDef, intentSymbol);
                        }
                    }
                }
            }
        }
        return new Pair<Set<String>, Set<String>>(newDecls, newAsserts);
    }

    private String getZ3Type(Type type) {
        switch (type.toString()) {
            case "short":
                return "Int";
            case "int":
                return "Int";
            case "long":
                return "Int";
            case "float":
                return "Real";
            case "double":
                return "Real";
            case "boolean":
                return "Int";
            case "byte":
                return "Int";
            case "java.lang.String":
                return "String";
            default:
                return "Object";
        }
    }

    private void handleGetActionOfIntent(SootMethod method, Set<Unit> currPath, Set<String> currPathCond, Set<String> currDecls, SimpleLocalDefs defs, Stmt currStmtInPath) {
        DefinitionStmt currDefStmt = (DefinitionStmt) currStmtInPath;
        InvokeExpr ie = currStmtInPath.getInvokeExpr();
        if (ie.getMethod().getName().equals("getAction")) {
            if (ie.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {

                if (ie instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
                    String actionRefSymbol = null;
                    if (currDefStmt.getLeftOp() instanceof Local) {
                        Local leftLocal = (Local) currDefStmt.getLeftOp();
                        actionRefSymbol = createSymbol(currDefStmt.getLeftOp(), method, currStmtInPath);
                        //symbolLocalMap.put(actionRefSymbol,leftLocal);
                        if (iie.getBase() instanceof Local) {
                            Local intentLocal = (Local) iie.getBase();
                            for (Unit intentDef : defs.getDefsOfAt(intentLocal, currDefStmt)) {

                                if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, currDefStmt, defs)) {
                                    continue;
                                }

                                if (!currPath.contains(intentDef)) {
                                    throw new RuntimeException("isDefInPathAndLatest工作不正常");
                                }

                                String getActionSymbol = createSymbol(leftLocal, method, intentDef);
                                symbolLocalMap.put(getActionSymbol, intentLocal);
                                String intentSymbol = createSymbol(intentLocal, method, intentDef);
                                symbolLocalMap.put(intentSymbol, intentLocal);

                                String intentDecl = "(declare-const " + intentSymbol + " Object )";
                                String actionRefDecl = "(declare-const " + actionRefSymbol + " String )";

                                currDecls.add(intentDecl);
                                currDecls.add(actionRefDecl);

                                String getActionAssert = "(assert (= (getAction " + intentSymbol + ") " + actionRefSymbol + "))";

                                currPathCond.add(getActionAssert);


                                buildParamRefExpressions(method, currPath, currPathCond, currDecls, intentDef, intentSymbol);//intent的来源参数的话，再加一些decl,assert

                                //intent来源于getIntent呢？不用
                                //对Field暂时不考虑，不知道哪里被赋值
                                //强制转化而来，更不考虑。情况少
                                // 来源于参数的话，那么这个Intent在调用者方法中可能还有约束


                            }
                        }
                    }
                } else {
                    throw new RuntimeException("getAction竟然不是实例调用");
                }
            }
        }
    }

    private void buildParamRefExpressions(SootMethod method, Set<Unit> currPath, Set<String> currPathCond, Set<String> currDecls, Unit intentDef, String intentSymbol) {
        if (intentDef instanceof DefinitionStmt) {
            DefinitionStmt defStmt = (DefinitionStmt) intentDef;
            if (!currPath.contains(defStmt)) {
                return;
            }
            if (defStmt.getRightOp() instanceof ParameterRef) {//判断intent是否来源于参数
                ParameterRef pr = (ParameterRef) defStmt.getRightOp();

                String prSymbol = createParamRefSymbol(defStmt.getLeftOp(), pr.getIndex(), method, defStmt);

                currDecls.add("(declare-const " + prSymbol + " ParamRef)");
                currPathCond.add("(assert ( = (index " + prSymbol + ") " + pr.getIndex() + "))");
                currPathCond.add("(assert ( = (type " + prSymbol + ") \"" + pr.getType() + "\"))");
                currPathCond.add("(assert ( = (method " + prSymbol + ") \"" + method.getDeclaringClass().getName() + "." + method.getName() + "\"))");
                currPathCond.add("(assert (= (hasParamRef " + intentSymbol + ") " + prSymbol + "))");
            }
        }
    }

    private static String createParamRefSymbol(Value opVal, int index, SootMethod method, Unit unit) {
        String valNameNoDollar = opVal.toString();
        BytecodeOffsetTag bcoTag = null;
        for (Tag tag : unit.getTags()) {
            if (tag instanceof BytecodeOffsetTag) {
                bcoTag = (BytecodeOffsetTag) tag;
            }
        }
        String symbol = null;
        if (bcoTag != null)
            symbol = "pr" + index + "_" + convertTypeNameForZ3(opVal.getType()) + "_" + method.getName() + "_" + method.getDeclaringClass().getName() + "_" + bcoTag.toString();
        else
            symbol = "pr" + index + "_" + convertTypeNameForZ3(opVal.getType()) + "_" + method.getName() + "_" + method.getDeclaringClass().getName();
        return symbol;
    }

    private static String createSymbol(Value opVal, SootMethod method, Unit unit) {
        String valNameNoDollar = opVal.toString();
        BytecodeOffsetTag bcoTag = null;
        for (Tag tag : unit.getTags()) {
            if (tag instanceof BytecodeOffsetTag) {
                bcoTag = (BytecodeOffsetTag) tag;
            }
        }
        String symbol = null;
        if (bcoTag != null)
            symbol = valNameNoDollar + "_" + convertTypeNameForZ3(opVal.getType()) + "_" + method.getName() + "_" + method.getDeclaringClass().getName() + "_" + bcoTag.toString();
        else
            symbol = valNameNoDollar + "_" + convertTypeNameForZ3(opVal.getType()) + "_" + method.getName() + "_" + method.getDeclaringClass().getName();
        return symbol;
    }

    private static String convertTypeNameForZ3(Type type) {
        String returnStr = type.toString();
        returnStr = returnStr.replace("[]", "-Arr");
        return returnStr;
    }

    private boolean isDefInPathAndLatest(Set<Unit> path, Unit inDef, Local usedLocal, Unit usedUnit, SimpleLocalDefs defs) {//---------------------------------------
        if (path.contains(inDef)) { // does the path contain the definition
            for (Unit otherDef : defs.getDefsOfAt(usedLocal, usedUnit)) { // check other defs of usedLocal at usedUnit to determine if inDef is the latestDef in path
                if (inDef.equals(otherDef)) { // continue if inDef equals otherDef
                    continue;
                }
                if (!path.contains(otherDef)) { // if the otherDef is not in path, then continue
                    continue;
                }
                List<Unit> cipList = new ArrayList<Unit>(path);
                int inDefPos = cipList.indexOf(inDef);
                int argDef2Pos = cipList.indexOf(otherDef);//
                if (inDefPos > argDef2Pos) { // if inDef's position in the path is earlier then otherDef's position, then inDef is not the latest definition in the path, so return false
                    System.out.println("test");
                    //-------------是可能的，可能的for循环中的i
                    return false;
                }
            }
            return true; // inDef is in the path and is the latest definition along that path
        } else { // inDef is not in the path, so return false
            return false;
        }
    }

    private Set<String> handleIfStmt(IfStmt ifStmt, Set<Unit> path, SootMethod sootMethod, CallGraph cg, SimpleLocalDefs defs, Set<String> decls, Unit succUnit) {

        //只用生成关于if语句的表达式，intent的表达式都在intent方法里生成了

        String returnExpr = "";


        String opVal1Assert = null;
        String opVal2Assert = null;

        Unit opVal1DefUnit = null;
        Unit opVal2DefUnit = null;


        ConditionExpr condition = (ConditionExpr) ifStmt.getCondition();

        Value conditionLeft = condition.getOp1();
        Value conditionRight = condition.getOp2();


        Value opVal1 = conditionLeft;

        Value opVal2 = conditionRight;


        Value opVal1Org = conditionLeft;
        Value opVal2Org = conditionRight;

        boolean generateCondExpr = true;


        if (conditionLeft instanceof Local) {
            Local localConditionLeft = (Local) conditionLeft;
            List<Unit> defineLocalUnits = defs.getDefsOfAt(localConditionLeft, ifStmt);

            for (Unit defUnitConditionLeft : defineLocalUnits)//这个可能会有多个值，比如当前面有if语句,因为不确定不确定执行哪一个。
            {
                if (!isDefInPathAndLatest(path, defUnitConditionLeft, localConditionLeft, ifStmt, defs))//注意这个定义其的语句可能不在当前路径中
                {
                    continue;
                }

                if (defUnitConditionLeft instanceof DefinitionStmt) {//defUnitConditionLeft是定义if语句左边变量的语句  9484/9484
                    DefinitionStmt defDefintionStmtConditionLeft = (DefinitionStmt) defUnitConditionLeft;

                    if (opVal1.getType() instanceof ByteType) {//ByteType    $b0= 3>5   $b0就为ByteType

                        Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> condValuesPair = findLeftAndRightValuesOfByteVal(sootMethod, defs, ifStmt, opVal1, path);
                        Quartet<Value, String, String, Unit> left = condValuesPair.getValue0();
                        Quartet<Value, String, String, Unit> right = condValuesPair.getValue1();
                        opVal1 = left.getValue0();
                        opVal2 = right.getValue0();
                        if (left.getValue1() != null)
                            decls.add(left.getValue1());
                        if (right.getValue1() != null)
                            decls.add(right.getValue1());
                        if (left.getValue2() != null) {
                            opVal1Assert = left.getValue2();
                        }
                        if (right.getValue2() != null) {
                            opVal2Assert = right.getValue2();
                        }
                        opVal1DefUnit = left.getValue3();
                        opVal2DefUnit = right.getValue3();
                    } else

                        //基本类型
                        //if (conditionLeft.getType() instanceof PrimType) {
                        if (conditionLeft.getType().toString().equals("boolean")) {// 5196/7726

                            if (defDefintionStmtConditionLeft.getRightOp() instanceof JVirtualInvokeExpr) {//  7726/9484   只分析这个
                                JVirtualInvokeExpr jVInvokeExpr = (JVirtualInvokeExpr) defDefintionStmtConditionLeft.getRightOp();


                                Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> pair_quartet_left_and_right = findEqualsBoolean(ifStmt, path, sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr);
                                Quartet<Value, String, String, Unit> leftValue = pair_quartet_left_and_right.getValue0();
                                Quartet<Value, String, String, Unit> rightValue = pair_quartet_left_and_right.getValue1();//注意 equals fallthough 没有判定，ifStmt约束还没有生成，所以其generateCondExpr为true

                                if (leftValue == null && rightValue == null) {
                                    pair_quartet_left_and_right = findIntentBooleanValues(sootMethod, defs, ifStmt, conditionLeft, path, jVInvokeExpr, defDefintionStmtConditionLeft);


                                    if (pair_quartet_left_and_right == null) {
                                        pair_quartet_left_and_right = findBundleValues(sootMethod, defs, ifStmt, conditionLeft, path);//if match this case  leftValue！=null  rightValue=null
                                        leftValue = pair_quartet_left_and_right.getValue0();
                                        rightValue = pair_quartet_left_and_right.getValue1();
                                        if (leftValue == null) {//都不满足


                                            findKeysForLeftAndRightValues(ifStmt, opVal1, opVal2, defs, path);
                                            opVal1DefUnit = getDefOfValInPath(opVal1, ifStmt, path, defs);
                                            opVal2DefUnit = getDefOfValInPath(opVal2, ifStmt, path, defs);


                                        } else//BundleValue
                                        {
                                            generateCondExpr = false;

                                            if (leftValue != null) {
                                                if (leftValue.getValue1() != null) {
                                                    decls.add(leftValue.getValue1());
                                                }


                                                opVal1Assert = leftValue.getValue2();

                                                opVal1DefUnit = leftValue.getValue3();

                                            }

                                            if (rightValue != null) {
                                                if (rightValue.getValue1() != null) {
                                                    decls.add(rightValue.getValue1());
                                                }


                                                opVal2Assert = leftValue.getValue2();


                                                opVal2DefUnit = leftValue.getValue3();
                                            }

                                        }
                                    } else//IntentBooleanValues
                                    {
                                        generateCondExpr = false;

                                        leftValue = pair_quartet_left_and_right.getValue0();
                                        rightValue = pair_quartet_left_and_right.getValue1();

                                        if (leftValue != null) {
                                            if (leftValue.getValue1() != null) {
                                                decls.add(leftValue.getValue1());
                                            }


                                            opVal1Assert = leftValue.getValue2();

                                            opVal1DefUnit = leftValue.getValue3();

                                        }

                                        if (rightValue != null) {
                                            if (rightValue.getValue1() != null) {
                                                decls.add(rightValue.getValue1());
                                            }


                                            opVal2Assert = leftValue.getValue2();


                                            opVal2DefUnit = leftValue.getValue3();
                                        }

                                    }


                                } else //equals
                                {
                                    opVal1 = leftValue.getValue0();

                                    opVal2 = rightValue.getValue0();

                                    if (opVal1 == null && opVal2 == null) {

                                        throw new RuntimeException("equals异常");
                                        //findKeysForLeftAndRightValues(ifStmt, opVal1, opVal2, defs, path);//其实也没用
                                    } else {


                                        if (leftValue != null) {
                                            if (leftValue.getValue1() != null) {
                                                decls.add(leftValue.getValue1());
                                            }


                                            opVal1Assert = leftValue.getValue2();

                                            opVal1DefUnit = leftValue.getValue3();

                                        }

                                        if (rightValue != null) {
                                            if (rightValue.getValue1() != null) {
                                                decls.add(rightValue.getValue1());
                                            }


                                            opVal2Assert = leftValue.getValue2();


                                            opVal2DefUnit = leftValue.getValue3();
                                        }


                                    }


                                }


//                                if (leftValue == null) {
//                                    pair_quartet_left_and_right = findBundleValues(sootMethod, defs, ifStmt, conditionLeft, path);//if match this case  leftValue！=null  rightValue=null
//                                    leftValue = pair_quartet_left_and_right.getValue0();
//                                    rightValue = pair_quartet_left_and_right.getValue1();
//
//                                    if (leftValue != null || rightValue != null) {
//                                        generateCondExpr = false;
//                                    }
//
//                                    if (leftValue != null) {
//                                        opVal1 = leftValue.getValue0();
//                                    }
//                                    if (rightValue != null) {
//                                        opVal2 = rightValue.getValue0();
//                                    }
//                                    AssignOpVals assignOpVals = new AssignOpVals(decls, opVal1Assert, opVal2Assert, opVal1, opVal2, leftValue, rightValue).invoke();
//                                    opVal1DefUnit = assignOpVals.getOpVal1DefUnit();
//                                    opVal2DefUnit = assignOpVals.getOpVal2DefUnit();
//                                    opVal1Assert = assignOpVals.getOpVal1Assert();
//                                    opVal2Assert = assignOpVals.getOpVal2Assert();
//                                }


//


//                            }//其他类型，比如float ，等
//                            else {
//
//                                if (jVInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {
//
//
//                                    System.out.println(defDefintionStmtConditionLeft);//---------------------------------------------
//
//
//                                }
//
//
                            }

//
//
//                        }

                            //对象类型

//                        else if (conditionLeft.getType() instanceof RefLikeType) {
//
//                            if (conditionLeft.getType() instanceof ArrayType) {
//
//                                System.out.println(conditionLeft);//-----------------------------------
//
//                            }
//
//                            if (conditionLeft.getType() instanceof RefType) {
//                                if (conditionLeft.getType().toString().equals("java.lang.String")) {
//
//                                }
//                                if (conditionLeft.getType().toString().equals("android.os.Bundle")) {
//
//                                }
//                                if (conditionLeft.getType().toString().equals("android.content.Intent")) {
//
//                                }
//
//                            }
//
//                        }

                            else {

                                findKeysForLeftAndRightValues(ifStmt, opVal1, opVal2, defs, path);
                                opVal1DefUnit = getDefOfValInPath(opVal1, ifStmt, path, defs);
                                opVal2DefUnit = getDefOfValInPath(opVal2, ifStmt, path, defs);

                            }


                        } else {

                            findKeysForLeftAndRightValues(ifStmt, opVal1, opVal2, defs, path);
                            opVal1DefUnit = getDefOfValInPath(opVal1, ifStmt, path, defs);
                            opVal2DefUnit = getDefOfValInPath(opVal2, ifStmt, path, defs);

                        }


                }

            }

        }

        //生成if的表达式
        Set<String> returnExprs = new LinkedHashSet<String>();
        if (opVal1DefUnit == null && opVal2DefUnit == null && opVal1Assert == null && opVal2Assert == null) {
            //logger.debug("No new information from this if stmt, so returning empty set of expressions");
            return returnExprs;
        }


        // if the curr unit to convert is an ifStmt ensure the symbol is not negated //如果要转换的curr单位是ifStmt，则确保该符号不是否定的
        boolean isFallThrough = isFallThrough(ifStmt, succUnit);

        String branchSensitiveSymbol = null;
        if (isFallThrough) {
            if (opVal1Org.getType() instanceof BooleanType) {
                branchSensitiveSymbol = condition.getSymbol();
            } else {
                branchSensitiveSymbol = negateSymbol(condition.getSymbol());
            }
        } else {
            if (opVal1Org.getType() instanceof BooleanType) {
                branchSensitiveSymbol = negateSymbol(condition.getSymbol());
            } else {
                branchSensitiveSymbol = condition.getSymbol();
            }
        }


        if (generateCondExpr) {
            String opExpr1 = null;
            String opExpr2 = null;
            try {
                if (opVal1 == null) {
                    //logger.debug("Could not resolve opVal1, so setting it to true");
                    opExpr1 = "";
                } else {
                    opExpr1 = createZ3Expr(opVal1, ifStmt, opVal1DefUnit, sootMethod, decls);
                }

                if (opVal2 == null) {
                    //logger.debug("Could not resolve opVal2, so setting it to true");
                    opExpr2 = "";
                } else {
                    opExpr2 = createZ3Expr(opVal2, ifStmt, opVal2DefUnit, sootMethod, decls);
                }
            } catch (RuntimeException e) {
                //logger.warn("caught exception: ", e);
                return null;
            }

            if (opExpr1 == opExpr2 && opExpr1 == null) {//---------------------------------
                //logger.debug("op1 and op2 are both null, so just returning true expression");
                return Collections.singleton(returnExpr);
            }
            returnExpr = buildZ3CondExpr(opExpr1, opExpr2, branchSensitiveSymbol);
            returnExprs.add(returnExpr);
        }
        if (opVal1Assert != null) {
            returnExprs.add(opVal1Assert);
        }
        if (opVal2Assert != null) {
            returnExprs.add(opVal2Assert);
        }


        return returnExprs;


    }

    private Unit getDefOfValInPath(Value opVal, Unit currUnit, Set<Unit> currPath, SimpleLocalDefs defs) {
        Unit defUnit = null;
        if (opVal instanceof Local) {
            Local opLocal1 = (Local) opVal;
            for (Unit opLocalDefUnit : defs.getDefsOfAt(opLocal1, currUnit)) {
                if (currPath.contains(opLocalDefUnit)) {
                    defUnit = opLocalDefUnit;
                }
            }
        }
        return defUnit;
    }

    public void findKeysForLeftAndRightValues(Unit currUnit, Value opVal1, Value opVal2, SimpleLocalDefs defs, Set<Unit> currPath) {
        findKeyForVal(currUnit, opVal1, defs, currPath);
        findKeyForVal(currUnit, opVal2, defs, currPath);
    }

    public Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findLeftAndRightValuesOfByteVal(SootMethod method, SimpleLocalDefs defs, Unit inUnit, Value value, Set<Unit> currPath) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;
        if (value instanceof Local) {
            Local local = (Local) value;
            if (local.getType() instanceof ByteType) {
                List<Unit> potentialCmpUnits = defs.getDefsOfAt(local, inUnit);
                for (Unit potentialCmpUnit : potentialCmpUnits) {
					/*if (!currPath.contains(potentialCmpUnit)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, potentialCmpUnit, local, inUnit, defs)) {
                        continue;
                    }
                    if (potentialCmpUnit.toString().contains("cmp")) {
                        //logger.debug("Found potential cmp* statement: " + potentialCmpUnit);
                        if (potentialCmpUnit instanceof DefinitionStmt) {
                            DefinitionStmt defStmt = (DefinitionStmt) potentialCmpUnit;
                            Value rightOp = defStmt.getRightOp();
                            if (rightOp instanceof AbstractJimpleIntBinopExpr) {
                                AbstractJimpleIntBinopExpr cmpExpr = (AbstractJimpleIntBinopExpr) rightOp;
                                leftVal = findOriginalVal(method, defs, potentialCmpUnit, cmpExpr.getOp1(), currPath);
                                rightVal = findOriginalVal(method, defs, potentialCmpUnit, cmpExpr.getOp2(), currPath);
                            }
                        }
                    }
                }
            }
        }
        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }

    public void findKeyForVal(Unit currUnit, Value opVal, SimpleLocalDefs defs, Set<Unit> currPath) {
        if (opVal instanceof Local) {
            Local local = (Local) opVal;
            List<Unit> defUnits = defs.getDefsOfAt(local, currUnit);
            for (Unit defUnit : defUnits) {
				/*if (!currPath.contains(defUnit)) {
					continue;
				}*/
                if (!isDefInPathAndLatest(currPath, defUnit, local, currUnit, defs)) {
                    continue;
                }
                if (defUnit instanceof DefinitionStmt) {
                    DefinitionStmt defStmt = (DefinitionStmt) defUnit;
                    String key = extractKeyFromIntentExtra(defStmt, defs, currPath);
                    valueKeyMap.put(opVal, key);
                }
            }
        }
    }

    private String negateSymbol(String symbol) {
        switch (symbol.trim()) {
            case "==":
                return "!=";
            case "!=":
                return "==";
            case ">":
                return "<=";
            case "<":
                return ">=";
            case ">=":
                return "<";
            case "<=":
                return ">";
            default:
                throw new RuntimeException("invalid symbol passed to negateSymbol(): " + symbol);
        }
    }

    private String buildZ3CondExpr(String opExpr1, String opExpr2, String branchSensitiveSymbol) {
        String returnExpr;
        String condExpr = null;

        switch (branchSensitiveSymbol.trim()) {
            case "==":
                if (opExpr2.equals("Null"))
                    condExpr = "(assert (= (isNull " + opExpr1 + ") true))";
                else if (isObjectEquals(opExpr1, opExpr2))
                    condExpr = "(assert (= (oEquals " + opExpr1 + " " + opExpr2 + ") true))";
                else
                    condExpr = "(assert (= " + opExpr1 + " " + opExpr2 + "))";
                break;
            case "!=":
                if (opExpr2.equals("Null"))
                    condExpr = "(assert (= (isNull " + opExpr1 + ") false))";
                else if (isObjectEquals(opExpr1, opExpr2))
                    condExpr = "(assert (= (oEquals " + opExpr1 + " " + opExpr2 + ") false))";
                else
                    condExpr = "(assert (not (= " + opExpr1 + " " + opExpr2 + ")))";
                break;
            case ">":
                condExpr = "(assert (> " + opExpr1 + " " + opExpr2 + "))";
                break;
            case ">=":
                condExpr = "(assert (>= " + opExpr1 + " " + opExpr2 + "))";
                break;
            case "<":
                condExpr = "(assert (< " + opExpr1 + " " + opExpr2 + "))";
                break;
            case "<=":
                condExpr = "(assert (<= " + opExpr1 + " " + opExpr2 + "))";
                break;
        }
        //logger.debug(Utils.createTabsStr(tabs) + "z3 conditional expr: " + condExpr);

        if (condExpr == null) {
            //logger.error("currExpr should not be null");
            //logger.debug("opExpr1: " + opExpr1);
            //logger.debug("opExpr2: " + opExpr2);
            throw new RuntimeException("currExpr should not be null");
        }
        returnExpr = condExpr;
        return returnExpr;
    }

    private boolean isObjectEquals(String opExpr1, String opExpr2) {
        if (opExpr1.contains("_java.lang.String_") && !opExpr2.contains("_java.lang.String_") && !opExpr2.contains("\""))
            return true;
        else if (!opExpr1.contains("_java.lang.String_") && opExpr2.contains("_java.lang.String_") && !opExpr2.contains("\""))
            return true;
        else
            return false;
    }

    private String createZ3Expr(Value opVal, Unit currUnit, Unit defUnit, SootMethod method, Set<String> decls) {
        String opExpr = null;
        String newDecl = null;

        if (opVal instanceof IntConstant) {
            IntConstant intConst = (IntConstant) opVal;
            opExpr = Integer.toString(intConst.value);

        } else if (opVal instanceof LongConstant) {
            LongConstant longConst = (LongConstant) opVal;
            opExpr = Long.toString(longConst.value);
        } else if (opVal instanceof FloatConstant) {
            FloatConstant floatConst = (FloatConstant) opVal;
            opExpr = Float.toString(floatConst.value);
        } else if (opVal instanceof DoubleConstant) {
            DoubleConstant doubleConst = (DoubleConstant) opVal;
            opExpr = Double.toString(doubleConst.value);
        } else if (opVal instanceof NullConstant) {
            opExpr = "Null";
        } else if (opVal instanceof StringConstant) {
            StringConstant strConst = (StringConstant) opVal;
            opExpr = "\"" + strConst.value + "\"";
        } else if (opVal instanceof JimpleLocal) {
            JimpleLocal opLocal = (JimpleLocal) opVal;
            //logger.debug(Utils.createTabsStr(tabs + 1) + "opLocal type: " + opLocal.getType());

            String symbol = null;

            DefinitionStmt defStmt = (DefinitionStmt) defUnit;
            if (defStmt.getLeftOp() == opVal) {
                symbol = createSymbol(opVal, method, defStmt);
                symbolLocalMap.put(symbol, opLocal);
                symbolDefUnitMap.put(symbol, defStmt);
                localSymbolMap.put(opLocal, symbol);
            }

            symbol = localSymbolMap.get(opLocal);
            if (symbol == null) {
                symbol = createSymbol(opVal, method, defUnit);
                symbolLocalMap.put(symbol, opLocal);
                symbolDefUnitMap.put(symbol, defStmt);
                localSymbolMap.put(opLocal, symbol);
            }

            switch (opLocal.getType().toString().trim()) {
                case "short":
                    newDecl = "(declare-const " + symbol + " Int )";
                    opExpr = symbol;
                    break;
                case "int":
                    newDecl = "(declare-const " + symbol + " Int )";
                    opExpr = symbol;
                    break;
                case "long":
                    newDecl = "(declare-const " + symbol + " Int )";
                    opExpr = symbol;
                    break;
                case "float":
                    newDecl = "(declare-const " + symbol + " Real )";
                    opExpr = symbol;
                    break;
                case "double":
                    newDecl = "(declare-const " + symbol + " Real )";
                    opExpr = symbol;
                    break;
                case "boolean":
                    newDecl = "(declare-const " + symbol + " Int )";
                    opExpr = symbol;
                    break;
                case "byte":
                    newDecl = "(declare-const " + symbol + " Int )";
                    opExpr = symbol;
                    break;
                case "java.lang.String":
                    newDecl = "(declare-const " + symbol + " String )";
                    opExpr = symbol;
                    break;
                default:
                    // object is an arbitrary type so we'll mark it as null or not null
                    //logger.debug("Creating object with symbol: " + symbol + " for Local " + opLocal + " in " + method);
                    newDecl = "(declare-const " + symbol + " Object )";
                    opExpr = symbol;
            }
            decls.add(newDecl);
        } else {
            throw new RuntimeException("I don't know what to do with this Value's type: " + opVal.getType());
        }


        return opExpr;
    }

    private class AssignOpVals {
        private Set<String> decls;
        private String opVal1Assert;
        private String opVal2Assert;
        private Value opVal1;
        private Value opVal2;
        private Quartet<Value, String, String, Unit> left;
        private Quartet<Value, String, String, Unit> right;
        private Unit opVal1DefUnit;
        private Unit opVal2DefUnit;

        public AssignOpVals(Set<String> decls, String opVal1Assert, String opVal2Assert, Value opVal1, Value opVal2, Quartet<Value, String, String, Unit> left, Quartet<Value, String, String, Unit> right) {
            this.decls = decls;
            this.opVal1Assert = opVal1Assert;
            this.opVal2Assert = opVal2Assert;
            this.opVal1 = opVal1;
            this.opVal2 = opVal2;
            this.left = left;
            this.right = right;
        }

        public String getOpVal1Assert() {
            return opVal1Assert;
        }

        public String getOpVal2Assert() {
            return opVal2Assert;
        }

        public Unit getOpVal1DefUnit() {
            return opVal1DefUnit;
        }

        public Unit getOpVal2DefUnit() {
            return opVal2DefUnit;
        }

        public AssignOpVals invoke() {
            if (left != null) {
                if (left.getValue1() != null)
                    decls.add(left.getValue1());
                if (left.getValue2() != null)
                    opVal1Assert = left.getValue2();
                opVal1DefUnit = left.getValue3();
            }

            if (right != null) {
                if (right.getValue1() != null)
                    decls.add(right.getValue1());
                if (right.getValue2() != null)
                    opVal2Assert = right.getValue2();
                opVal2DefUnit = right.getValue3();
            }

            return this;
        }
    }

    private Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findCategories(SootMethod method, SimpleLocalDefs defs, Unit inUnit, Value value, Set<Unit> currPath) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;
        if (value instanceof Local) {
            Local local = (Local) value;
            if (local.getType() instanceof BooleanType) {
                List<Unit> potentialStringEqualsUnits = defs.getDefsOfAt(local, inUnit);
                for (Unit pseUnit : potentialStringEqualsUnits) {
					/*if (!currPath.contains(pseUnit)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, pseUnit, local, inUnit, defs)) {
                        continue;
                    }

                    if (pseUnit instanceof DefinitionStmt) {
                        DefinitionStmt defStmt = (DefinitionStmt) pseUnit;
                        if (defStmt.getRightOp() instanceof JVirtualInvokeExpr) {
                            JVirtualInvokeExpr jviExpr = (JVirtualInvokeExpr) defStmt.getRightOp();
                            if (jviExpr.getMethod().getName().equals("hasCategory")) {
                                if (jviExpr.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {
                                    if (jviExpr.getBase() instanceof Local) {
                                        Local intentLocal = (Local) jviExpr.getBase();
                                        for (Unit intentDef : defs.getDefsOfAt(intentLocal, defStmt)) {
											/*if (!currPath.contains(intentDef)) {
												continue;
											}*/
                                            if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, defStmt, defs)) {
                                                continue;
                                            }
                                            String intentSymbol = createSymbol(intentLocal, method, intentDef);
                                            symbolLocalMap.put(intentSymbol, intentLocal);
                                            localSymbolMap.put(intentLocal, intentSymbol);
                                            symbolDefUnitMap.put(intentSymbol, intentDef);

                                            String category = null;
                                            if (jviExpr.getArg(0) instanceof StringConstant) {
                                                StringConstant catStrConst = (StringConstant) jviExpr.getArg(0);
                                                category = catStrConst.value;
                                            }

                                            Body b = method.getActiveBody();
                                            UnitGraph ug = new BriefUnitGraph(b);

                                            List<Unit> currPathList = new ArrayList<Unit>(currPath);
                                            int indexOfUnit = currPathList.indexOf(inUnit);
                                            if (indexOfUnit == -1) {
                                                throw new RuntimeException(inUnit + " is not in path");
                                            }
                                            Unit succ = currPathList.get(indexOfUnit - 1);

                                            boolean isFallThrough = isFallThrough(inUnit, succ);
                                            String newAssert = null;
                                            if (isFallThrough) { // intent contains the category
                                                newAssert = "(assert (exists ((index Int)) (= (select cats index) \"" + category + "\")))";
                                                //addIntentCategoryForPath(currPath,category);
                                            } else { // intent does not contain the category
                                                newAssert = "(assert (forall ((index Int)) (not(= (select cats index) \"" + category + "\"))))";
                                            }
                                            leftVal = new Quartet<Value, String, String, Unit>(intentLocal, null, newAssert, intentDef);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }

    Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findIntentBooleanValues(SootMethod sootMethod, SimpleLocalDefs defs, IfStmt ifStmt, Value conditionLeft, Set<Unit> path, JVirtualInvokeExpr jVInvokeExpr, DefinitionStmt definitionStmtConditionLeft) {


        if (Pattern.matches("hasExtra", jVInvokeExpr.getMethod().getName())) {//ok

            return intentHasExtra(sootMethod, defs, ifStmt, conditionLeft, path, jVInvokeExpr, definitionStmtConditionLeft);

        } else if (Pattern.matches("hasCategory", jVInvokeExpr.getMethod().getName())) {//ok
            return findCategories(sootMethod, defs, ifStmt, conditionLeft, path);
        }


//     else if (Pattern.matches("hasFileDescriptors", jVInvokeExpr.getMethod().getName())) {
//
//            //------------------------------------------------------
//
//
//        } else if (Pattern.matches("getBooleanExtra", jVInvokeExpr.getMethod().getName())) {//这个有两个对应的方法
//
//
//
//
//
//        } else {
//
//            System.out.println(defDefintionStmtConditionLeft);//---------------------------------
//
//        }

        return null;

    }

    private Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> intentHasExtra(SootMethod sootMethod, SimpleLocalDefs defs, IfStmt ifStmt, Value conditionLeft, Set<Unit> path, JVirtualInvokeExpr jVInvokeExpr, DefinitionStmt defDefintionStmtConditionLeft) {

        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;

        if (Pattern.matches("hasExtra", jVInvokeExpr.getMethod().getName())) {

            if (jVInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {


                Value origValue = null;
                Unit defOrigValueUnit = null;

                String newDecl = null;
                String newAssert = null;

                Local intentLocal = (Local) jVInvokeExpr.getBase();
                int count = 0;

                for (Unit defIntentLocalUnit : defs.getDefsOfAt(intentLocal, defDefintionStmtConditionLeft)) {
                    if (!isDefInPathAndLatest(path, defIntentLocalUnit, intentLocal, defDefintionStmtConditionLeft, defs)) {
                        continue;
                    }
                    origValue = intentLocal;
                    defOrigValueUnit = defIntentLocalUnit;
                    count = count + 1;


                }
                assert count == 1;
                String intentLocalSymbol = createSymbol(intentLocal, sootMethod, defOrigValueUnit);//intent 的定义语句
                symbolLocalMap.put(intentLocalSymbol, intentLocal);
                symbolDefUnitMap.put(intentLocalSymbol, defOrigValueUnit);
                localSymbolMap.put(intentLocal, intentLocalSymbol);
                newDecl = "(declare-const " + intentLocalSymbol + " Object )";
                newAssert = "(assert (= " + intentLocalSymbol + " NotNull))";


                String keyString = null;
                if (jVInvokeExpr.getArg(0) instanceof StringConstant) {
                    keyString = ((StringConstant) (((StringConstant) jVInvokeExpr.getArg(0)))).value;


                } else if (jVInvokeExpr.getArg(0) instanceof Local) {
                    Local keyLocal = (Local) (jVInvokeExpr.getArg(0));


                } else {
                    throw new RuntimeException("unhandle case :" + jVInvokeExpr.toString());
                }

                if (keyString != null) {
                    Body b = sootMethod.getActiveBody();
                    UnitGraph ug = new BriefUnitGraph(b);
                    List<Unit> currPathList = new ArrayList<Unit>(path);
                    Unit succ = currPathList.get(currPathList.indexOf(ifStmt) - 1);

                    boolean isFallThrough = isFallThrough(ifStmt, succ);
                    ConditionExpr conditionExpr = (ConditionExpr) ifStmt.getCondition();

                    if (isFallThrough) { // then intent contains the key

                        if (conditionExpr.getSymbol().toString().equals(" == ")) {
                            newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") true))";
                        } else {
                            newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") false))";
                        }


                        //addIntentExtraForPath(currPath,keyString,keyVal.getType().toString());
                    } else { // the intent does NOT contain the key
                        if (conditionExpr.getSymbol().toString().equals(" == ")) {
                            newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") false))";
                        } else {
                            newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") true))";
                        }

                    }
//                    if (isFallThrough) { // intent contains the extra
//                        newAssert += "\n"+"(assert (exists ((index Int)) (= (select keys index) \"" + keyString + "\")))";
//
//                        //addIntentExtraForPath(currPath, rightVal.getValue0().toString(), rightVal.getValue0().getType().toString());
//                    } else { // intent does not contain the extra
//                        newAssert += "\n"+"(assert (forall ((index Int)) (not(= (select keys index) \"" + keyString + "\"))))";
//                    }
                }

                leftVal = new Quartet<Value, String, String, Unit>(origValue, newDecl, newAssert, defOrigValueUnit);


            }
        }
        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }


    private Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findBundleValues(SootMethod method, SimpleLocalDefs defs, IfStmt inUnit, Value value, Set<Unit> currPath) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;
        if (value instanceof Local) {
            Local local = (Local) value;
            if (local.getType() instanceof BooleanType) {
                for (Unit defUnit : defs.getDefsOfAt(local, inUnit)) {
					/*if (!currPath.contains(defUnit)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, defUnit, local, inUnit, defs)) {//ok
                        continue;
                    }

                    Stmt defStmt = (Stmt) defUnit;
                    if (defStmt.containsInvokeExpr()) {
                        if (defStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                            InstanceInvokeExpr ie = (InstanceInvokeExpr) defStmt.getInvokeExpr();
                            if (ie.getMethod().getDeclaringClass().getName().equals("android.os.Bundle")) {
                                if (ie.getMethod().getName().equals("containsKey")) {
                                    Value keyVal = ie.getArg(0);
                                    if (keyVal instanceof StringConstant) {//直接没有考虑key是否是个变量的情况--------------------------------------
                                        StringConstant keyStringConst = (StringConstant) keyVal;
                                        String keyString = keyStringConst.value;


                                        if (ie.getBase() instanceof Local) {
                                            Local bundleLocal = (Local) ie.getBase();
                                            for (Unit bundleDef : defs.getDefsOfAt(bundleLocal, defUnit)) {
												/*if (!currPath.contains(bundleDef)) {
													continue;
												}*/
                                                if (!isDefInPathAndLatest(currPath, bundleDef, bundleLocal, defUnit, defs)) {
                                                    continue;
                                                }
                                                Stmt bundleStmt = (Stmt) bundleDef;
                                                if (bundleStmt.containsInvokeExpr()) {
                                                    if (bundleStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                                                        InstanceInvokeExpr bundleInvoke = (InstanceInvokeExpr) bundleStmt.getInvokeExpr();
                                                        if (bundleInvoke.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {
                                                            if (bundleInvoke.getMethod().getName().equals("getExtras")) {//
                                                                if (bundleInvoke.getBase() instanceof Local) {
                                                                    Local intentLocal = (Local) bundleInvoke.getBase();
                                                                    for (Unit intentDef : defs.getDefsOfAt(intentLocal, bundleStmt)) {
																		/*if (!currPath.contains(intentDef)) {
																			continue;
																		}*/
                                                                        if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, bundleStmt, defs)) {
                                                                            continue;
                                                                        }
                                                                        String intentLocalSymbol = createSymbol(intentLocal, method, intentDef);//intent 的定义语句

//                                                                        symbolLocalMap.put(intentLocalSymbol, intentLocal);//已经做了
//                                                                        localSymbolMap.put(intentLocal,intentLocalSymbol);
//                                                                        symbolDefUnitMap.put(intentLocalSymbol, intentDef);

                                                                        String newDecl = "(declare-const " + intentLocalSymbol + " Object )";
                                                                        String newAssert = "(assert (= " + intentLocalSymbol + " NotNull))";


                                                                        List<Unit> currPathList = new ArrayList<Unit>(currPath);
                                                                        Unit succ = currPathList.get(currPathList.indexOf(inUnit) - 1);

                                                                        boolean isFallThrough = isFallThrough(inUnit, succ);
                                                                        ConditionExpr conditionExpr = (ConditionExpr) inUnit.getCondition();
                                                                        if (isFallThrough) { // then intent contains the key   //
                                                                            if (conditionExpr.getSymbol().equals(" == ")) {
                                                                                newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") true))";//getExtras是将intent的extra data converted into bundle ,so assert is about intent
                                                                            } else {
                                                                                newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") false))";
                                                                            }


                                                                        } else { // the intent does NOT contain the key
                                                                            if (conditionExpr.getSymbol().equals(" == ")) {
                                                                                newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") false))";

                                                                            } else {
                                                                                newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") true))";//getExtras是将intent的extra data converted into bundle ,so assert is about intent
                                                                            }
                                                                        }

                                                                        leftVal = new Quartet<Value, String, String, Unit>(null, newDecl, newAssert, null);
                                                                    }
                                                                }
                                                            } else if (bundleInvoke.getMethod().getName().equals("getBundleExtra"))//43个------------------------这里表明bundle是从intent的getBundleExtra取出来，这里intent的getBundleExtra约束已经再处理
                                                            {

                                                                String bundleLocalSymbol = createSymbol(bundleLocal, method, bundleDef);//

//                                                                symbolLocalMap.put(bundleLocalSymbol, bundleLocal);
//                                                                symbolDefUnitMap.put(bundleLocalSymbol, bundleDef);已经做了

                                                                String newDecl = "(declare-const " + bundleLocalSymbol + " Object )";
                                                                String newAssert = "(assert (= " + bundleLocalSymbol + " NotNull))";

                                                                List<Unit> currPathList = new ArrayList<Unit>(currPath);
                                                                Unit succ = currPathList.get(currPathList.indexOf(inUnit) - 1);

                                                                boolean isFallThrough = isFallThrough(inUnit, succ);
                                                                ConditionExpr conditionExpr = (ConditionExpr) inUnit.getCondition();
                                                                if (isFallThrough) { // then intent contains the key
                                                                    if (conditionExpr.getSymbol().equals(" == ")) {
                                                                        newAssert += "\n(assert (= (containsKey " + bundleLocalSymbol + " \"" + keyString + "\") true))";
                                                                    } else {
                                                                        newAssert += "\n(assert (= (containsKey " + bundleLocalSymbol + " \"" + keyString + "\") false))";
                                                                    }

                                                                    //addIntentExtraForPath(currPath,keyString,keyVal.getType().toString());
                                                                } else { // the intent does NOT contain the key

                                                                    if (conditionExpr.getSymbol().equals(" == ")) {
                                                                        newAssert += "\n(assert (= (containsKey " + bundleLocalSymbol + " \"" + keyString + "\") false))";
                                                                    } else {

                                                                        newAssert += "\n(assert (= (containsKey " + bundleLocalSymbol + " \"" + keyString + "\") true))";

                                                                    }


                                                                }


                                                                leftVal = new Quartet<Value, String, String, Unit>(null, newDecl, newAssert, null);


                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (keyVal instanceof Local)//------------------------------------------------写一个专门抽取函数
                                    {

                                    }
                                } else if (ie.getMethod().getName().equals("getBoolean")) {
                                    String booleanValueSymbol = createSymbol(value, method, defStmt);
                                    List<Unit> currPathList = new ArrayList<Unit>(currPath);
                                    Unit succ = currPathList.get(currPathList.indexOf(inUnit) - 1);
                                    String newDecl = "(declare-const " + booleanValueSymbol + " Bool )";
                                    String newAssert = null;
                                    boolean isFallThrough = isFallThrough(inUnit, succ);
                                    ConditionExpr conditionExpr = (ConditionExpr) inUnit.getCondition();
                                    if (isFallThrough) { // then intent contains the key

                                        if (conditionExpr.getSymbol().equals(" == ")) {
                                            newAssert = "(assert (=" + booleanValueSymbol + " true))";
                                        } else {
                                            newAssert = "(assert (=" + booleanValueSymbol + " false))";
                                        }


                                        //addIntentExtraForPath(currPath,keyString,keyVal.getType().toString());
                                    } else { // the intent does NOT contain the key
                                        if (conditionExpr.getSymbol().equals(" == ")) {
                                            newAssert = "(assert (=" + booleanValueSymbol + " false))";

                                        } else {
                                            newAssert = "(assert (=" + booleanValueSymbol + " true))";
                                        }
                                    }

                                    leftVal = new Quartet<Value, String, String, Unit>(null, newDecl, newAssert, null);

                                }
                            }
                        }
                    }
                }
            }
        }
        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }

    private Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findEqualsBoolean(IfStmt ifStmt, Set<Unit> path, SootMethod sootMethod, SimpleLocalDefs defs, DefinitionStmt defDefintionStmtConditionLeft, JVirtualInvokeExpr jVInvokeExpr) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;

        if (jVInvokeExpr.getMethod().getName().equals("equals") && jVInvokeExpr.getMethod().getDeclaringClass().getName().equals("java.lang.String")) {

            leftVal = findEqualLeftAndRightVal(sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr.getBase(), path);
            rightVal = findEqualLeftAndRightVal(sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr.getArg(0), path);


        }

        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }


    private boolean isFallThrough(Unit inUnit, Unit succ) {//-------------------------------------这个方法要改
        JimpleBasedInterproceduralCFG interproceduralCFG = new JimpleBasedInterproceduralCFG();
        return (succ == null && inUnit instanceof IfStmt) ? true : interproceduralCFG.isFallThroughSuccessor(inUnit, succ);
    }

    public Quartet<Value, String, String, Unit> findOriginalVal(SootMethod method, SimpleLocalDefs defs, Unit potentialCmpUnit, Value cmpOp, Set<Unit> currPath) {
        Value origVal = null;
        String newDecl = null;
        String newAssert = null;
        Unit defUnit = null;
        if (cmpOp instanceof Local) {
            Value cmpVal = cmpOp;
            Quartet<Value, String, String, Unit> r = findOriginalValFromCmpVal(method, defs, potentialCmpUnit, cmpVal, currPath);
            origVal = r.getValue0();
            newDecl = r.getValue1();
            newAssert = r.getValue2();
            defUnit = r.getValue3();
        } else if (cmpOp instanceof Constant) {
            origVal = cmpOp;

        } else {
            throw new RuntimeException("Unhandled cmpOp for: " + potentialCmpUnit);
        }
        return new Quartet<Value, String, String, Unit>(origVal, newDecl, newAssert, defUnit);
    }

    public Quartet<Value, String, String, Unit> findOriginalValFromCmpVal(SootMethod method, SimpleLocalDefs defs, Unit potentialCmpUnit, Value cmpVal, Set<Unit> currPath) {
        Value origVal = null;
        String key = null;
        Local cmpOp1 = (Local) cmpVal;
        List<Unit> castOrInvokeUnits = defs.getDefsOfAt(cmpOp1, potentialCmpUnit);
        String newDecl = null;
        String newAssert = null;
        Unit defUnit = null;
        for (Unit coiUnit : castOrInvokeUnits) {

            if (!isDefInPathAndLatest(currPath, coiUnit, cmpOp1, potentialCmpUnit, defs)) {
                continue;
            }

            if (coiUnit instanceof DefinitionStmt) {
                DefinitionStmt coiStmt = (DefinitionStmt) coiUnit;
                origVal = coiStmt.getLeftOp();
                defUnit = coiUnit;
                if (!currPath.contains(defUnit)) {
                    continue;
                }
                if (coiStmt.getRightOp() instanceof JCastExpr) {

                    JCastExpr expr = (JCastExpr) coiStmt.getRightOp();
                    if (expr.getOp() instanceof Local) {
                        Local localFromCast = (Local) expr.getOp();
                        List<Unit> defsOfLocalFromCast = defs.getDefsOfAt(localFromCast, coiUnit);
                        for (Unit defLocalAssignFromCastUnit : defsOfLocalFromCast) {
							/*if (!currPath.contains(defLocalAssignFromCastUnit)) {
								continue;
							}*/
                            if (!isDefInPathAndLatest(currPath, defLocalAssignFromCastUnit, localFromCast, coiUnit, defs)) {
                                continue;
                            }
                            if (defLocalAssignFromCastUnit instanceof DefinitionStmt) {
                                DefinitionStmt defLocalAssignFromCastStmt = (DefinitionStmt) defLocalAssignFromCastUnit;
                                origVal = defLocalAssignFromCastStmt.getLeftOp();
                                defUnit = defLocalAssignFromCastUnit;
                                key = extractKeyFromIntentExtra(defLocalAssignFromCastStmt, defs, currPath);
                            }
                        }
                    }
                } else {
                    key = extractKeyFromIntentExtra(coiStmt, defs, currPath);
                }

                if (coiStmt.getRightOp() instanceof StringConstant) {
                    Local local = (Local) coiStmt.getLeftOp();
                    String symbol = createSymbol(local, method, coiStmt);
                    symbolLocalMap.put(symbol, local);
                    symbolDefUnitMap.put(symbol, coiStmt);
                    StringConstant stringConst = (StringConstant) coiStmt.getRightOp();
                    newDecl = "(declare-const " + symbol + " String )";
                    newAssert = "(assert (= " + symbol + " " + stringConst + " ))";
                }

                if (coiStmt.getRightOp() instanceof ParameterRef) {

                    if (coiStmt.getLeftOp() instanceof Local) {
                        Local prLocal = (Local) coiStmt.getLeftOp();
                        String localSymbol = createSymbol(prLocal, method, coiStmt);

                        origVal = coiStmt.getLeftOp();
                        ParameterRef pr = (ParameterRef) coiStmt.getRightOp();
                        String prSymbol = createParamRefSymbol(prLocal, pr.getIndex(), method, coiStmt);

                        newDecl = "(declare-const " + prSymbol + " ParamRef)";
                        newAssert = "(assert ( = (index " + prSymbol + ") " + pr.getIndex() + "))\n";
                        newAssert += "(assert ( = (type " + prSymbol + ") \"" + pr.getType() + "\"))\n";
                        newAssert += "(assert ( = (method " + prSymbol + ") \"" + method.getDeclaringClass().getName() + "." + method.getName() + "\"))\n";
                        newAssert += "(assert (= (hasParamRef " + localSymbol + ") " + prSymbol + "))";
                        defUnit = coiStmt;
                    }
                }
            }
        }

        return new Quartet<Value, String, String, Unit>(origVal, newDecl, newAssert, defUnit);
    }

    private Quartet<Value, String, String, Unit> findEqualLeftAndRightVal(SootMethod sootMethod, SimpleLocalDefs defs, Unit equalsUnit, Value value, Set<Unit> path) {


        Value origVal = null;//左边变量
        String newDecl = null;
        String newAssert = null;
        Unit defOrigValUnit = null;


        if (value instanceof Local) {// value 是   x1 or x2   x1.euqals(x2)
            Local local = (Local) value;//如果这个equals其中一个是变量的话，去找这个变量的来源

            Quartet<Value, String, String, Unit> quartet = findLocalSource(local, equalsUnit, sootMethod, defs, path);

            origVal = quartet.getValue0();
            newDecl = quartet.getValue1();
            newAssert = quartet.getValue2();
            defOrigValUnit = quartet.getValue3();


        } else if (value instanceof StringConstant) {
            origVal = value;

        } else {


            throw new RuntimeException("不能处理equals的两个参数的类型情况" + equalsUnit);//---------------------------------------------------------
        }

        return new Quartet<Value, String, String, Unit>(origVal, newDecl, newAssert, defOrigValUnit);

    }

    private Quartet<Value, String, String, Unit> findLocalSource(Local localVar, Unit curUnit, SootMethod sootMethod, SimpleLocalDefs defs, Set<Unit> path) {

        String key = null;


        String newDecl = null;
        String newAssert = null;

        Value origVal = null;//x1.euqals(x2)     //这里的x1就是origVal   保存origVal,defUnitOfOrigVal用来生成约束
        Unit defUnitOfOrigVal = null;


        List<Unit> defineLocalUnits = defs.getDefsOfAt(localVar, curUnit);//curUnit是equals语句  localVar就是x1    x1.euals(x2)

        for (Unit defUnit : defineLocalUnits) {
            if (!isDefInPathAndLatest(path, defUnit, localVar, curUnit, defs)) {
                continue;
            }

            if (defUnit instanceof DefinitionStmt) {
                DefinitionStmt defDefintionStmt = (DefinitionStmt) defUnit;//x1的定义语句 例如：x1=intent.getStringExtra("xxx")

                origVal = defDefintionStmt.getLeftOp();
                defUnitOfOrigVal = defDefintionStmt;

                if (defDefintionStmt.getRightOp() instanceof InstanceInvokeExpr) {


                    //如果这个x1来源于intent方法, 这个约束已经在handleIntentExtra那里处理了。不用生成约束


                    key = getExtra(defs, path, defDefintionStmt);//但是得把getExtra的key取出保存方便后面使用


                } else if (defDefintionStmt.getRightOp() instanceof JCastExpr) {//x1=(String)3.5

                    JCastExpr jCastExpr = (JCastExpr) defDefintionStmt.getRightOp();
                    if (jCastExpr.getOp() instanceof Local) {
                        Local localFromCast = (Local) jCastExpr.getOp();
                        List<Unit> defsOfLocalFromCast = defs.getDefsOfAt(localFromCast, defUnit);
                        for (Unit defLocalAssignFromCastUnit : defsOfLocalFromCast) {
							/*if (!currPath.contains(defLocalAssignFromCastUnit)) {
								continue;
							}*/
                            if (!isDefInPathAndLatest(path, defLocalAssignFromCastUnit, localFromCast, defUnit, defs)) {
                                continue;
                            }
                            if (defLocalAssignFromCastUnit instanceof DefinitionStmt) {
                                DefinitionStmt defLocalAssignFromCastStmt = (DefinitionStmt) defLocalAssignFromCastUnit;

                                origVal = defLocalAssignFromCastStmt.getLeftOp();
                                defUnitOfOrigVal = defLocalAssignFromCastUnit;

                                key = extractKeyFromIntentExtra(defLocalAssignFromCastStmt, defs, path);
                            }
                        }
                    }


                } else if (defDefintionStmt.getRightOp() instanceof StringConstant) {//定义是一个常量定义 例如：x1="ttt"
                    Local local = (Local) defDefintionStmt.getLeftOp();
                    String symbol = createSymbol(local, sootMethod, defDefintionStmt);
                    symbolLocalMap.put(symbol, local);
                    symbolDefUnitMap.put(symbol, defDefintionStmt);
                    StringConstant stringConst = (StringConstant) defDefintionStmt.getRightOp();
                    newDecl = "(declare-const " + symbol + " String )";
                    newAssert = "(assert (= " + symbol + " " + stringConst + " ))";
                } else if (defDefintionStmt.getRightOp() instanceof ParameterRef) {//来源于参数   x1=parameter0

                    if (defDefintionStmt.getLeftOp() instanceof Local) {
                        Local prLocal = (Local) defDefintionStmt.getLeftOp();
                        String localSymbol = createSymbol(prLocal, sootMethod, defDefintionStmt);


                        ParameterRef pr = (ParameterRef) defDefintionStmt.getRightOp();
                        String prSymbol = createParamRefSymbol(prLocal, pr.getIndex(), sootMethod, defDefintionStmt);

                        newDecl = "(declare-const " + prSymbol + " ParamRef)";
                        newAssert = "(assert ( = (index " + prSymbol + ") " + pr.getIndex() + "))\n";
                        newAssert += "(assert ( = (type " + prSymbol + ") \"" + pr.getType() + "\"))\n";
                        newAssert += "(assert ( = (method " + prSymbol + ") \"" + sootMethod.getDeclaringClass().getName() + "." + sootMethod.getName() + "\"))\n";
                        newAssert += "(assert (= (hasParamRef " + localSymbol + ") " + prSymbol + "))";

                    }
                } else {// from  field or other sources


                }

            }


        }

        valueKeyMap.put(origVal, key);//等到之后使用


        return new Quartet<Value, String, String, Unit>(origVal, newDecl, newAssert, defUnitOfOrigVal);
    }

    public String extractKeyFromIntentExtra(DefinitionStmt defStmt, SimpleLocalDefs defs, Set<Unit> currPath) {

        String key = null;
        if (defStmt.getRightOp() instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr expr = (JVirtualInvokeExpr) defStmt.getRightOp();
            boolean keyExtractionEnabled = false;
            if (Pattern.matches("get.*Extra", expr.getMethod().getName())) {
                if (expr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    keyExtractionEnabled = true;
                }
            }
            if (Pattern.matches("has.*Extra", expr.getMethod().getName())) {
                if (expr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    keyExtractionEnabled = true;
                }
            }
            if (Globals.bundleExtraDataMethodsSet.contains(expr.getMethod().getName())) {
                if (expr.getMethod().getDeclaringClass().getName().equals("android.os.Bundle")) {
                    keyExtractionEnabled = true;
                }
                if (expr.getMethod().getDeclaringClass().getName().equals("android.os.BaseBundle")) {
                    keyExtractionEnabled = true;
                }
            }

            if (keyExtractionEnabled) {

                if (!(expr.getArg(0) instanceof StringConstant)) {
                    if (expr.getArg(0) instanceof Local) {
                        Local keyLocal = (Local) expr.getArg(0);
                        List<Unit> defUnits = defs.getDefsOfAt(keyLocal, defStmt);
                        for (Unit defUnit : defUnits) {
							/*if (!currPath.contains(defUnit)) {
								continue;
							}*/
                            if (!isDefInPathAndLatest(currPath, defUnit, keyLocal, defStmt, defs)) {
                                continue;
                            }
                            if (defUnit instanceof DefinitionStmt) {
                                DefinitionStmt keyLocalDefStmt = (DefinitionStmt) defUnit;
                                if (keyLocalDefStmt.getRightOp() instanceof VirtualInvokeExpr) {
                                    VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) keyLocalDefStmt.getRightOp();
                                    if (invokeExpr.getBase() instanceof Local) {
                                        if (invokeExpr.getMethod().getDeclaringClass().getType().toString().equals("java.lang.Enum")) {
                                            Local base = (Local) invokeExpr.getBase();
                                            List<Unit> baseDefs = defs.getDefsOfAt(base, keyLocalDefStmt);
                                            for (Unit baseDef : baseDefs) {
												/*if (!currPath.contains(baseDef)) {
													continue;
												}*/
                                                if (!isDefInPathAndLatest(currPath, baseDef, base, keyLocalDefStmt, defs)) {
                                                    continue;
                                                }
                                                if (baseDef instanceof DefinitionStmt) {
                                                    DefinitionStmt baseDefStmt = (DefinitionStmt) baseDef;
                                                    if (baseDefStmt.getRightOp() instanceof FieldRef) {
                                                        FieldRef fieldRef = (FieldRef) baseDefStmt.getRightOp();
                                                        if (fieldRef.getField().getDeclaringClass().toString().equals(invokeExpr.getBase().getType().toString())) {
                                                            key = fieldRef.getField().getName();
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }
                                    continue;
                                } else if (keyLocalDefStmt.getRightOp() instanceof StaticFieldRef) {
                                    SootField keyField = ((StaticFieldRef) keyLocalDefStmt.getRightOp()).getField();
                                    SootMethod clinitMethod = keyField.getDeclaringClass().getMethodByName("<clinit>");
                                    if (clinitMethod.hasActiveBody()) {
                                        Body clinitBody = clinitMethod.getActiveBody();
                                        for (Unit clinitUnit : clinitBody.getUnits()) {
                                            if (clinitUnit instanceof DefinitionStmt) {
                                                DefinitionStmt clinitDefStmt = (DefinitionStmt) clinitUnit;
                                                if (clinitDefStmt.getLeftOp() instanceof StaticFieldRef) {
                                                    SootField clinitField = ((StaticFieldRef) clinitDefStmt.getLeftOp()).getField();
                                                    if (clinitField.equals(keyField)) {
                                                        if (clinitDefStmt.getRightOp() instanceof StringConstant) {
                                                            StringConstant clinitStringConst = (StringConstant) clinitDefStmt.getRightOp();
                                                            key = clinitStringConst.value;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    throw new RuntimeException("Unhandled case for: " + keyLocalDefStmt.getRightOp());
                                }

                            }
                        }
                    }
                } else {
                    key = expr.getArg(0).toString();
                }
            }
        }
        return key;
    }

    private String getExtra(SimpleLocalDefs defs, Set<Unit> path, DefinitionStmt defStmt) {//intent and bundle 的extra
        String key = null;


        if (defStmt.getRightOp() instanceof JVirtualInvokeExpr) {

            JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) defStmt.getRightOp();
            boolean keyExtractionEnabled = false;

            if (Pattern.matches("get.*Extra", jVirtualInvokeExpr.getMethod().getName())) {//不需要判断返回值类型的，因为前面的equals已经限制了满足这个方法是getStringExtra等

                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    keyExtractionEnabled = true;

                }
            }
            if (Pattern.matches("has.*Extra", jVirtualInvokeExpr.getMethod().getName())) {
                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    keyExtractionEnabled = true;

                }
            }
            if (Globals.bundleExtraDataMethodsSet.contains(jVirtualInvokeExpr.getMethod().getName())) {
                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.os.Bundle")) {
                    keyExtractionEnabled = true;

                }
                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.os.BaseBundle")) {
                    keyExtractionEnabled = true;

                }
            }

            if (keyExtractionEnabled) {
                if (!(jVirtualInvokeExpr.getArg(0) instanceof StringConstant)) { //参数不是一个字符串常量
                    if (jVirtualInvokeExpr.getArg(0) instanceof Local) {//参数是一个变量
                        Local keyLocal = (Local) jVirtualInvokeExpr.getArg(0);
                        List<Unit> defUnits = defs.getDefsOfAt(keyLocal, defStmt);
                        for (Unit defUnitOfKey : defUnits) {

                            if (!isDefInPathAndLatest(path, defUnitOfKey, keyLocal, defStmt, defs)) {
                                continue;
                            }
                            if (defUnitOfKey instanceof DefinitionStmt) {
                                DefinitionStmt keyLocalDefStmt = (DefinitionStmt) defUnitOfKey;
                                if (keyLocalDefStmt.getRightOp() instanceof StringConstant) {
                                    key = ((StringConstant) keyLocalDefStmt.getRightOp()).value;
                                } else if (keyLocalDefStmt.getRightOp() instanceof VirtualInvokeExpr) {//key的值来源于Enum


                                    VirtualInvokeExpr invokeExpr = (VirtualInvokeExpr) keyLocalDefStmt.getRightOp();
                                    if (invokeExpr.getBase() instanceof Local) {
                                        if (invokeExpr.getMethod().getDeclaringClass().getType().toString().equals("java.lang.Enum")) {
                                            Local base = (Local) invokeExpr.getBase();
                                            List<Unit> baseDefs = defs.getDefsOfAt(base, keyLocalDefStmt);
                                            for (Unit baseDef : baseDefs) {

                                                if (!isDefInPathAndLatest(path, baseDef, base, keyLocalDefStmt, defs)) {
                                                    continue;
                                                }
                                                if (baseDef instanceof DefinitionStmt) {
                                                    DefinitionStmt baseDefStmt = (DefinitionStmt) baseDef;
                                                    if (baseDefStmt.getRightOp() instanceof FieldRef) {
                                                        FieldRef fieldRef = (FieldRef) baseDefStmt.getRightOp();
                                                        if (fieldRef.getField().getDeclaringClass().toString().equals(invokeExpr.getBase().getType().toString())) {
                                                            key = fieldRef.getField().getName();
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }
                                    //continue;
                                    //throw new RuntimeException("key的值来源于Enum");

                                } else if (keyLocalDefStmt.getRightOp() instanceof StaticFieldRef) {//key的值来源于静态属性
                                    //-----------------------------------------------------------------------
                                    SootField keyField = ((StaticFieldRef) keyLocalDefStmt.getRightOp()).getField();
                                    SootMethod clinitMethod = keyField.getDeclaringClass().getMethodByName("<clinit>");//静态初始化方法
                                    if (clinitMethod.hasActiveBody()) {
                                        Body clinitBody = clinitMethod.getActiveBody();
                                        for (Unit clinitUnit : clinitBody.getUnits()) {
                                            if (clinitUnit instanceof DefinitionStmt) {
                                                DefinitionStmt clinitDefStmt = (DefinitionStmt) clinitUnit;
                                                if (clinitDefStmt.getLeftOp() instanceof StaticFieldRef) {
                                                    SootField clinitField = ((StaticFieldRef) clinitDefStmt.getLeftOp()).getField();
                                                    if (clinitField.equals(keyField)) {
                                                        if (clinitDefStmt.getRightOp() instanceof StringConstant) {
                                                            StringConstant clinitStringConst = (StringConstant) clinitDefStmt.getRightOp();
                                                            key = clinitStringConst.value;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    }
                                    //throw new RuntimeException("key的值来源于静态属性");
                                } else {
                                    throw new RuntimeException("Unhandled case for key来源于其他类型语句" + keyLocalDefStmt.getRightOp());
                                }

                            }
                        }
                    }
                } else {//参数是一个字符串常量
                    key = jVirtualInvokeExpr.getArg(0).toString();
                }
            }

        }


        return key;
    }

    class UnitEdge {
        Unit src;
        Unit tgt;

        public UnitEdge(Unit src, Unit tgt) {
            this.src = src;
            this.tgt = tgt;
        }

        public Unit getSrc() {
            return src;
        }

        public Unit getTgt() {
            return tgt;
        }


        @Override
        public int hashCode() {
            return src.hashCode() * 2 + tgt.hashCode() + 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnitEdge unitEdge = (UnitEdge) o;
            return this.src.equals(unitEdge.src) && this.tgt.equals(unitEdge.tgt);
        }
    }

    private void getAllPathInMethod(Unit unit, UnitEdge unitEdge, UnitGraph ug, List<List<Unit>> finalPaths, List<Unit> curPath, Set<UnitEdge> visited) {


        if (finalPaths.size() >= finalPathsPreviousLimit) {
            hasReachfinalPathSizeLimit = true;
            return;
        }

        List<Unit> curPathCopy = new ArrayList<>(curPath);
        curPathCopy.add(unit);


        Set<UnitEdge> visitedCopy = new HashSet<>(visited);

        visitedCopy.add(unitEdge);


        if (ug.getPredsOf(unit).size() == 0) {

            finalPaths.add(curPathCopy);

        } else {
            for (Unit parentUnit : ug.getPredsOf(unit)) {
                UnitEdge unitEdgeNew = new UnitEdge(unit, parentUnit);

                if (!visitedCopy.contains(unitEdgeNew)) {//这条边是不是已经走过了


                    getAllPathInMethod(parentUnit, unitEdgeNew, ug, finalPaths, curPathCopy, visitedCopy);

                }

            }
        }

    }


    public void run() {
        Config.setSootOptions(appPath);
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.intentGen", this));

        PackManager.v().getPack("wjtp").apply();
    }


    public static void main(String[] args) {


        try {
            Util.bufferedWriterCircleEntry = new BufferedWriter(new FileWriter("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "circleEntry.txt"));
            ifWriter = new BufferedWriter(new FileWriter("if_type.txt"));

            ifReducedWriter = new BufferedWriter(new FileWriter("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "if_reduced.txt"));

            bufferWriterEAToProtectPath = new BufferedWriter(new FileWriter("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "EAToTargetAPIPPAthCount.txt"));
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }

        String appDir = null;

        if (exeModelTest) {
            appDir = Config.defaultAppPath;
        } else {

            appDir = "/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/sootOutput";
            //appDir=Config.wandoijiaAPP;
        }

        File appDirFile = new File(appDir);

        if (appDirFile.isDirectory()) {


            List<String> hasAnalysisAPP = null;

            try {

                BufferedReader hasAnalysisAPPBufferedReader = new BufferedReader(new FileReader("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "hasSatisticIfReducedAndPreviousIF.txt"));
                hasAnalysisAPP = new ArrayList<>();
                String line = null;
                while ((line = hasAnalysisAPPBufferedReader.readLine()) != null) {
                    hasAnalysisAPP.add(line);
                }

                hasAnalysisAPPBufferedReader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (hasAnalysisAPP == null) {
                return;
            }

            BufferedWriter hasAnalysisAPPBufferedWriter = null;
            try {
                hasAnalysisAPPBufferedWriter = new BufferedWriter(new FileWriter("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/" + "hasSatisticIfReducedAndPreviousIF.txt", true));
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : appDirFile.listFiles()) {
                if ((!file.getName().contains("_signed_zipalign")) && file.getName().endsWith(".apk")) {

                    if (hasAnalysisAPP.contains(file.getAbsolutePath())) {
                        continue;
                    }
                    System.out.println(file.getAbsolutePath());

//                    Thread childThread = new Thread(new Runnable() {
//                        @Override
//                        public void run() {


                    try {
                        appReachFinalPathSizeLimitWriter = new BufferedWriter(new FileWriter("app_analysis_results/reachFinalPathSizeLimitAPP/" + file.getAbsolutePath().replaceAll("/|\\.", "_") + ".txt"));
                        reduceCFGAnalysisLimitWriter = new BufferedWriter(new FileWriter("app_analysis_results/reduceCFGAnalysisLimit/" + file.getAbsolutePath().replaceAll("/|\\.", "_") + ".txt"));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    IntentConditionTransformSymbolicExcutation intentConditionTransform = new IntentConditionTransformSymbolicExcutation(file.getAbsolutePath());
                    intentConditionTransform.run();
                    try {
                        appReachFinalPathSizeLimitWriter.close();
                        reduceCFGAnalysisLimitWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


//                        }
//
//                    });
//
//                    childThread.start();
//
//                    try {
//                        childThread.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    try {
                        hasAnalysisAPPBufferedWriter.write(file.getAbsolutePath() + "\n");
                        hasAnalysisAPPBufferedWriter.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


//                    break;//------------------------------------


                }




            }
            try {
                hasAnalysisAPPBufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


        } else {

            try {
                appReachFinalPathSizeLimitWriter = new BufferedWriter(new FileWriter("app_analysis_results/reachFinalPathSizeLimitAPP/" + appDirFile.getAbsolutePath().replaceAll("/|\\.", "_") + ".txt"));
                reduceCFGAnalysisLimitWriter = new BufferedWriter(new FileWriter("app_analysis_results/reduceCFGAnalysisLimit/" + appDirFile.getAbsolutePath().replaceAll("/|\\.", "_") + ".txt"));

            } catch (IOException e) {
                e.printStackTrace();
            }

            IntentConditionTransformSymbolicExcutation intentConditionTransform = new IntentConditionTransformSymbolicExcutation(appDir);
            intentConditionTransform.run();

            try {
                appReachFinalPathSizeLimitWriter.close();
                reduceCFGAnalysisLimitWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {

            Util.bufferedWriterCircleEntry.close();
            ifWriter.close();
            ifReducedWriter.close();
            bufferWriterEAToProtectPath.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
