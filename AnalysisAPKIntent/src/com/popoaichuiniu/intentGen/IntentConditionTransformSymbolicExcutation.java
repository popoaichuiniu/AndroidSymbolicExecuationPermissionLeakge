package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Util;
import com.popoaichuiniu.util.Config;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class IntentConditionTransformSymbolicExcutation extends SceneTransformer {

    private String appPath=null;

    Set<Pair<Integer, String>> targets = new LinkedHashSet<Pair<Integer, String>>();
    private boolean pathLimitEnabled = false;

    private int finalPathsLimit = 100;

    private BufferedWriter ifWriter = null;


    /**
     * key: a symbol used to represent a Local, value: the Local represented by the symbol
     */
    private Map<String, Local> symbolLocalMap = new HashMap<String, Local>();


    /**
     * key: a symbol used to represent a Local, value: the Local represented by the symbol
     */
    private Map<Local, String> localSymbolMap = new HashMap<Local, String>();


    public IntentConditionTransformSymbolicExcutation(String apkFilePath) {

        appPath=apkFilePath;

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(new File(apkFilePath + "_" + "UnitsNeedAnalysis.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] strs = line.split("#");
                String sooMethodString = strs[0];
                String byteCode = strs[1];
                int intByteCode = Integer.parseInt(byteCode);
                String unitString = strs[2];
                targets.add(new Pair<>(intByteCode, sooMethodString));

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

        try {
            ifWriter = new BufferedWriter(new FileWriter("if_type.txt"));
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
        for (SootMethod sootMethod : roMethods) {
            analysisSootMethod(sootMethod, androidCallGraphHelper.getCg());
        }

        try {
            ifWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void analysisSootMethod(SootMethod sootMethod, CallGraph cg) {


        Body body = sootMethod.getActiveBody();
        if (body != null) {
            PatchingChain<Unit> units = body.getUnits();

            for (Unit unit : units) {
                boolean isNeedAnalysis = unitNeedAnalysis(unit, sootMethod);

                isNeedAnalysis = true;//---------为了方便测试----------------------------------------------------------------------
                if (isNeedAnalysis) {
                    doAnalysisOnUnit(unit, sootMethod, cg);
                }
            }
        }


    }

    private void doAnalysisOnUnit(Unit unit, SootMethod sootMethod, CallGraph cg) {

        //System.out.println(unit.toString());

        List<Set<Unit>> finalPaths = new ArrayList<>();


        if (!pathLimitEnabled) {
            finalPathsLimit = Integer.MAX_VALUE;
        }

        BriefUnitGraph ug = new BriefUnitGraph(sootMethod.getActiveBody());


        getAllPathInMethod(unit, ug, finalPaths, new LinkedHashSet<Unit>());


        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        for (Set<Unit> path : finalPaths) {
            analysisPathIntraMethod(path, sootMethod, cg);
        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");


    }

    private void analysisPathIntraMethod(Set<Unit> path, SootMethod sootMethod, CallGraph cg) {

        SimpleLocalDefs defs = new SimpleLocalDefs(new BriefUnitGraph(sootMethod.getActiveBody()));
        for (Unit unit : path) {
            Stmt stmt = (Stmt) unit;
            if (stmt instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) stmt;
                handleIfStmt(ifStmt, path, sootMethod, cg, defs);
            }

            if (stmt.containsInvokeExpr() && stmt instanceof DefinitionStmt) {
                //handleGetActionOfIntent(sootMethod, path, currPathCond, currDecls, defs, stmt);
                //handleGetExtraOfIntent(method, currPath, currPathCond, currDecls, defs, (DefinitionStmt) currStmtInPath);

                //---------------------------------
            }


        }


    }

    private void handleGetExtraOfIntent(SootMethod method, Set<Unit> currPath, Set<String> currPathCond, Set<String> currDecls, SimpleLocalDefs defs, DefinitionStmt currStmtInPath) {
        DefinitionStmt defStmt = currStmtInPath;
        if (defStmt.containsInvokeExpr() && defStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
            InstanceInvokeExpr ie = (InstanceInvokeExpr) defStmt.getInvokeExpr();
            if (Pattern.matches("get.*Extra", ie.getMethod().getName())) {
                if (ie.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    Pair<Set<String>, Set<String>> exprPair = buildGetExtraData(defStmt, defs, ie, method, currPath);
                    currDecls.addAll(exprPair.getValue0());
                    currPathCond.addAll(exprPair.getValue1());
                }
            }
            if (Pattern.matches("get.*", ie.getMethod().getName())) {
                if (ie.getMethod().getDeclaringClass().toString().equals("android.os.Bundle")) {
                    Pair<Set<String>, Set<String>> exprPair = buildGetBundleData(defStmt, defs, ie, method, currPath);
                    currDecls.addAll(exprPair.getValue0());
                    currPathCond.addAll(exprPair.getValue1());
                }
            }
        }
    }

    private Pair<Set<String>, Set<String>> buildGetBundleData(Unit currUnit, SimpleLocalDefs defs, InstanceInvokeExpr ie, SootMethod method, Set<Unit> currPath) {
        Set<String> newDecls = new LinkedHashSet<String>();
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
                    DefinitionStmt bundleDefStmt = (DefinitionStmt) bundleDef;
                    if (bundleDefStmt.containsInvokeExpr()) {
                        if (bundleDefStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                            InstanceInvokeExpr iie = (InstanceInvokeExpr) bundleDefStmt.getInvokeExpr();
                            if (iie.getBase().getType().toString().equals("android.content.Intent")) {
                                if (iie.getBase() instanceof Local) {
                                    Local intentLocal = (Local) iie.getBase();
                                    for (Unit intentDef : defs.getDefsOfAt(intentLocal, bundleDefStmt)) {
                                        if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, bundleDefStmt, defs)) {
                                            continue;
                                        }

                                        if (currUnit instanceof DefinitionStmt) {
                                            DefinitionStmt defStmt = (DefinitionStmt) currUnit;
                                            if (defStmt.getLeftOp() instanceof Local) {
                                                Local extraLocal = (Local) defStmt.getLeftOp();
                                                String extraLocalSymbol = createSymbol(extraLocal, method, defStmt);
                                                symbolLocalMap.put(extraLocalSymbol, extraLocal);
                                                String intentSymbol = createSymbol(intentLocal, method, intentDef);
                                                symbolLocalMap.put(intentSymbol, intentLocal);
                                                String newExtraType = getZ3Type(extraLocal.getType());
                                                String newIntentType = getZ3Type(intentLocal.getType());
                                                newDecls.add("(declare-const " + extraLocalSymbol + " " + newExtraType + " )");
                                                newDecls.add("(declare-const " + intentSymbol + " " + newIntentType + " )");
                                                newAsserts.add("(assert (= (containsKey " + extraLocalSymbol + " \"" + keyStrConst.value + "\") true))");
                                                newAsserts.add("(assert (= (fromIntent " + extraLocalSymbol + ") " + intentSymbol + "))");

                                                //addIntentExtraForPath(currPath, keyStrConst.value, newExtraType);

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

    private Pair<Set<String>, Set<String>> buildGetExtraData(Unit currUnit, SimpleLocalDefs defs, InstanceInvokeExpr ie, SootMethod method, Set<Unit> currPath) {
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
                for (Unit intentDef : defs.getDefsOfAt(intentLocal, currUnit)) {
					/*if (!currPath.contains(intentDef)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, intentDef, intentLocal, currUnit, defs)) {
                        continue;
                    }

                    if (currUnit instanceof DefinitionStmt) {
                        DefinitionStmt defStmt = (DefinitionStmt) currUnit;
                        if (defStmt.getLeftOp() instanceof Local) {
                            Local extraLocal = (Local) defStmt.getLeftOp();
                            String extraLocalSymbol = createSymbol(extraLocal, method, defStmt);
                            symbolLocalMap.put(extraLocalSymbol, extraLocal);
                            String intentSymbol = createSymbol(intentLocal, method, intentDef);
                            symbolLocalMap.put(intentSymbol, intentLocal);
                            String newExtraType = getZ3Type(extraLocal.getType());
                            String newIntentType = getZ3Type(intentLocal.getType());
                            newDecls.add("(declare-const " + extraLocalSymbol + " " + newExtraType + " )");
                            newDecls.add("(declare-const " + intentSymbol + " " + newIntentType + " )");
                            newAsserts.add("(assert (= (containsKey " + extraLocalSymbol + " \"" + keyStrConst.value + "\") true))");
                            newAsserts.add("(assert (= (fromIntent " + extraLocalSymbol + ") " + intentSymbol + "))");

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

                if (ie instanceof InstanceInvokeExpr) {//VirtualInvokeExpr也可以
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

                                //String getActionSymbol = createSymbol(leftLocal, method, intentDef);
                                //symbolLocalMap.put(getActionSymbol, intentLocal);
                                String intentSymbol = createSymbol(intentLocal, method, intentDef);
                                //symbolLocalMap.put(intentSymbol, intentLocal);

                                String intentDecl = "(declare-const " + intentSymbol + " Object )";
                                String actionRefDecl = "(declare-const " + actionRefSymbol + " String )";
                                //String getActionDecl = "(declare-const " + actionRefSymbol + " String )";
                                currDecls.add(intentDecl);
                                currDecls.add(actionRefDecl);
                                //currDecls.add(getActionDecl);
                                String getActionAssert = "(assert (= (getAction " + intentSymbol + ") " + actionRefSymbol + "))";
                                String newFromIntent = "(assert (= (fromIntent " + actionRefSymbol + ") " + intentSymbol + "))";
                                currPathCond.add(getActionAssert);
                                currPathCond.add(newFromIntent);

                                //--------------------------------------为啥没有关心getAction的值

                                //addIntentActionForPath(currPath, actionRefSymbol);

                                buildParamRefExpressions(method, currPath, currPathCond, currDecls, intentDef, intentSymbol);//intent的来源

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

    private boolean isDefInPathAndLatest(Set<Unit> path, Unit inDef, Local usedLocal, Unit usedUnit, SimpleLocalDefs defs) {
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
                int argDef2Pos = cipList.indexOf(otherDef);//这个是怕他的simpledef不正确吧
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

    private void handleIfStmt(IfStmt ifStmt, Set<Unit> path, SootMethod sootMethod, CallGraph cg, SimpleLocalDefs defs) {


        ConditionExpr condition = (ConditionExpr) ifStmt.getCondition();

        Value conditionLeft = condition.getOp1();
        Value conditionRight = condition.getOp2();

        String operator = condition.getSymbol();

        try {
            ifWriter.write(operator + "   $$$    " + conditionLeft.getType() + "   $$$    " + conditionRight.getType() + "   $$$   " + ifStmt + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (conditionLeft instanceof Local) {
            Local localConditionLeft = (Local) conditionLeft;
            List<Unit> defineLocalUnits = defs.getDefsOfAt(localConditionLeft, ifStmt);

            for (Unit defUnitConditionLeft : defineLocalUnits)//这个可能会有多个值，比如当前面有if语句,因为不确定不确定执行哪一个。
            {
                if (!isDefInPathAndLatest(path, defUnitConditionLeft, localConditionLeft, ifStmt, defs))//注意这个定义其的语句可能不在当前路径中
                {
                    continue;
                }

                if (defUnitConditionLeft instanceof DefinitionStmt) {//defUnitConditionLeft是定义if语句左边变量的语句
                    DefinitionStmt defDefintionStmtConditionLeft = (DefinitionStmt) defUnitConditionLeft;
                    if (defDefintionStmtConditionLeft.getRightOp() instanceof JVirtualInvokeExpr) {
                        JVirtualInvokeExpr jVInvokeExpr = (JVirtualInvokeExpr) defDefintionStmtConditionLeft.getRightOp();

                        //基本类型

                        if (conditionLeft.getType() instanceof PrimType) {

                            if (conditionLeft.getType().toString().equals("boolean")) {


                                Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> pair_quartet_left_and_right = findEqualsBoolean(ifStmt, path, sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr);
                                Quartet<Value, String, String, Unit> leftValue = pair_quartet_left_and_right.getValue0();
                                Quartet<Value, String, String, Unit> rightValue = pair_quartet_left_and_right.getValue1();//注意 equals fallthough 没有判定------------------------------

                                if (!((leftValue == null && rightValue == null) || (leftValue != null && rightValue != null)))//
                                {
                                    throw new RuntimeException("leftValue 和 rightValue值不正常");
                                }


                                findIntentBooleanValues(ifStmt, path, sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr);

                                pair_quartet_left_and_right = findBundleValues(sootMethod, defs, ifStmt, conditionLeft, path);//if match this case  leftValue！=null  rightValue=null

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


                            }//其他类型，比如float ，等
                            else {

                                if (jVInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {


                                    System.out.println(defDefintionStmtConditionLeft);//---------------------------------------------


                                }


                            }


                        }

                        //对象类型

                        else if (conditionLeft.getType() instanceof RefLikeType) {

                            if (conditionLeft.getType() instanceof ArrayType) {

                                System.out.println(conditionLeft);//-----------------------------------

                            }

                            if (conditionLeft.getType() instanceof RefType) {
                                if (conditionLeft.getType().toString().equals("java.lang.String")) {

                                }
                                if (conditionLeft.getType().toString().equals("android.os.Bundle")) {

                                }
                                if (conditionLeft.getType().toString().equals("android.content.Intent")) {

                                }

                            }

                        }


                    } else//不会有简单的值传递的，（比如：int i=3;int j=i;syso.(j)）,会不会有其他的语句，new 什么的，或者从在类属性那得值等
                    {
                        System.out.println(defDefintionStmtConditionLeft);
                    }

                }

            }

        }


    }

    Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findIntentBooleanValues(IfStmt ifStmt, Set<Unit> path, SootMethod sootMethod, SimpleLocalDefs defs, DefinitionStmt defDefintionStmtConditionLeft, JVirtualInvokeExpr jVInvokeExpr) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;

        Value origValue = null;
        Unit defOrigValueUnit = null;
        if (Pattern.matches("hasExtra", jVInvokeExpr.getMethod().getName())) {//intent hasExtra(是bundle的hasExtra is solved in findBundleValue)


            if (jVInvokeExpr.getMethod().getClass().getName().equals("android.content.Intent")) {

                leftVal = findIntentLocalDef(path, defs, defDefintionStmtConditionLeft, jVInvokeExpr, sootMethod);

                rightVal = findStringSource(jVInvokeExpr, ifStmt, localSymbolMap.get((Local) leftVal.getValue0()), sootMethod, path);


            }


        }
        //else if (Pattern.matches("hasCategory", jVInvokeExpr.getMethod().getName())) {//--------------------------------在其他地方考虑
//
//            String category = null;
//
//            Value arg0 = jVInvokeExpr.getArg(0);
//
//            if (arg0 instanceof StringConstant) {
//                category = ((StringConstant) arg0).value;///--------------------------------------------
//            }
//
//
//        } else if (Pattern.matches("hasFileDescriptors", jVInvokeExpr.getMethod().getName())) {
//
//            //------------------------------------------------------
//
//
//        } else if (Pattern.matches("getBooleanExtra", jVInvokeExpr.getMethod().getName())) {//这个有两个对应的方法
//
//
//            //get*EXtra系列一起处理
//
//
//        } else {
//
//            System.out.println(defDefintionStmtConditionLeft);//---------------------------------
//
//        }

        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }

    private Quartet<Value, String, String, Unit> findStringSource(JVirtualInvokeExpr jVInvokeExpr, IfStmt ifStmt, String intentLocalSymbol, SootMethod sootMethod, Set<Unit> path) {
        String keyString = null;

        Value origValue = null;
        Unit defOrigValueUnit = null;

        String newDecl = null;
        String newAssert = null;


        if (jVInvokeExpr.getArg(0) instanceof StringConstant) {
            keyString = ((StringConstant) (((StringConstant) jVInvokeExpr.getArg(0)))).value;


        } else if (jVInvokeExpr.getArg(0) instanceof Local) {
            Local keyLocal = (Local) (jVInvokeExpr.getArg(0));// -------------------------------------


        } else {
            throw new RuntimeException("unhandle case :" + jVInvokeExpr.toString());
        }

        if (keyString != null) {
            Body b = sootMethod.getActiveBody();
            UnitGraph ug = new BriefUnitGraph(b);
            List<Unit> currPathList = new ArrayList<Unit>(path);
            Unit succ = currPathList.get(currPathList.indexOf(ifStmt) - 1);

            boolean isFallThrough = isFallThrough(ifStmt, succ);
            if (isFallThrough) { // then intent contains the key
                newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") true))";//getExtras是将intent的extra data converted into bundle ,so assert is about intent
                //addIntentExtraForPath(currPath,keyString,keyVal.getType().toString());
            } else { // the intent does NOT contain the key
                newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") false))";
            }
        }

        return new Quartet<Value, String, String, Unit>(origValue, newDecl, newAssert, defOrigValueUnit);


    }

    private Quartet<Value, String, String, Unit> findIntentLocalDef(Set<Unit> path, SimpleLocalDefs defs, DefinitionStmt defDefintionStmtConditionLeft, JVirtualInvokeExpr jVInvokeExpr, SootMethod sootMethod) {
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
        localSymbolMap.put(intentLocal, intentLocalSymbol);
        newDecl = "(declare-const " + intentLocalSymbol + " Object )";
        newAssert = "(assert (= " + intentLocalSymbol + " NotNull))";
        return new Quartet<Value, String, String, Unit>(origValue, newDecl, newAssert, defOrigValueUnit);
    }

    private Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findBundleValues(SootMethod method, SimpleLocalDefs defs, Unit inUnit, Value value, Set<Unit> currPath) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;
        if (value instanceof Local) {
            Local local = (Local) value;
            if (local.getType() instanceof BooleanType) {
                for (Unit defUnit : defs.getDefsOfAt(local, inUnit)) {
					/*if (!currPath.contains(defUnit)) {
						continue;
					}*/
                    if (!isDefInPathAndLatest(currPath, defUnit, local, inUnit, defs)) {
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
                                                            if (bundleInvoke.getMethod().getName().equals("getExtras")) {//还有getBooleanExtra(String name)------------------------------
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
                                                                        symbolLocalMap.put(intentLocalSymbol, intentLocal);
                                                                        String newDecl = "(declare-const " + intentLocalSymbol + " Object )";
                                                                        String newAssert = "(assert (= " + intentLocalSymbol + " NotNull))";

                                                                        Body b = method.getActiveBody();
                                                                        UnitGraph ug = new BriefUnitGraph(b);
                                                                        List<Unit> currPathList = new ArrayList<Unit>(currPath);
                                                                        Unit succ = currPathList.get(currPathList.indexOf(inUnit) - 1);

                                                                        boolean isFallThrough = isFallThrough(inUnit, succ);
                                                                        if (isFallThrough) { // then intent contains the key
                                                                            newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") true))";//getExtras是将intent的extra data converted into bundle ,so assert is about intent
                                                                            //addIntentExtraForPath(currPath,keyString,keyVal.getType().toString());
                                                                        } else { // the intent does NOT contain the key
                                                                            newAssert += "\n(assert (= (containsKey " + intentLocalSymbol + " \"" + keyString + "\") false))";
                                                                        }

                                                                        leftVal = new Quartet<Value, String, String, Unit>(intentLocal, newDecl, newAssert, intentDef);
                                                                    }
                                                                }
                                                            } else if (bundleInvoke.getMethod().getName().equals("getBooleanExtra"))//-----------------------------
                                                            {

                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (keyVal instanceof Local)//------------------------------------------------
                                    {

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

    private Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>> findEqualsBoolean(IfStmt ifStmt, Set<Unit> path, SootMethod sootMethod, SimpleLocalDefs defs, DefinitionStmt defDefintionStmtConditionLeft, JVirtualInvokeExpr jVInvokeExpr) {
        Quartet<Value, String, String, Unit> leftVal = null;
        Quartet<Value, String, String, Unit> rightVal = null;

        if (jVInvokeExpr.getMethod().getName().equals("equals") && jVInvokeExpr.getMethod().getDeclaringClass().getName().equals("java.lang.String")) {

            leftVal = findEqualLeftAndRightVal(sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr.getBase(), path);
            rightVal = findEqualLeftAndRightVal(sootMethod, defs, defDefintionStmtConditionLeft, jVInvokeExpr.getArg(0), path);

        }

        return new Pair<Quartet<Value, String, String, Unit>, Quartet<Value, String, String, Unit>>(leftVal, rightVal);
    }


    private boolean isFallThrough(Unit inUnit, Unit succ) {
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

    private Quartet<Value, String, String, Unit> findEqualLeftAndRightVal(SootMethod sootMethod, SimpleLocalDefs defs, Unit equalsUnit, Value base, Set<Unit> path) {

        ////defUnitConditionLeft  equals语句

        Value origVal = null;
        String newDecl = null;
        String newAssert = null;
        Unit defOrigValUnit = null;


        if (base instanceof Local) {
            Local localBase = (Local) base;//如果这个equals其中一个是变量的话，去找这个变量的来源

            Quartet<Value, String, String, Unit> quartet = findLocalSource(localBase, equalsUnit, sootMethod, defs, path);

            origVal = quartet.getValue0();
            newDecl = quartet.getValue1();
            newAssert = quartet.getValue2();
            defOrigValUnit = quartet.getValue3();


        } else if (base instanceof StringConstant) {
            origVal = base;

        } else {


            throw new RuntimeException("不能处理equals的两个参数的类型情况" + equalsUnit);//---------------------------------------------------------
        }

        return new Quartet<Value, String, String, Unit>(origVal, newDecl, newAssert, defOrigValUnit);

    }

    private Quartet<Value, String, String, Unit> findLocalSource(Local localBase, Unit curUnit, SootMethod sootMethod, SimpleLocalDefs defs, Set<Unit> path) {

        String key = null;
        String type = null;
        String arg = null;

        String newDecl = null;
        String newAssert = null;

        Value origVal = null;
        Unit defUnitOfOrigVal = null;

        Quartet<String, String, String, String> quartet = null;//type,arg,newDecl,newAssert

        List<Unit> defineLocalUnits = defs.getDefsOfAt(localBase, curUnit);//curUnit是equals语句

        for (Unit defUnit : defineLocalUnits) {
            if (!isDefInPathAndLatest(path, defUnit, localBase, curUnit, defs)) {
                continue;
            }

            if (defUnit instanceof DefinitionStmt) {
                DefinitionStmt defDefintionStmt = (DefinitionStmt) defUnit;

                origVal = defDefintionStmt.getLeftOp();
                defUnitOfOrigVal = defDefintionStmt;

                if (defDefintionStmt.getRightOp() instanceof JVirtualInvokeExpr) {

                    JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) defDefintionStmt.getRightOp();


                    if (jVirtualInvokeExpr.getMethod().getName().equals("getAction")) {//-----------------------------------这几个约束没有产生,等到获取到equals的右边的值再说
                        if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {

                            type = "action";
                            arg = "#";



                        }
                    } else if (jVirtualInvokeExpr.getMethod().getName().equals("getDataString")) {
                        if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                            type = "dataType";
                            arg = "#";
                        }
                    } else if (jVirtualInvokeExpr.getMethod().getName().equals("getType")) {
                        if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                            type = "dataType";
                            arg = "#";
                        }
                    } else if (jVirtualInvokeExpr.getMethod().getName().equals("getScheme")) {
                        if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                            type = "dataScheme";
                            arg = "#";
                        }
                    } else {

                        Pair<String, String> pair = getExtra(defs, path, defDefintionStmt);//intent get Extra

                        type = pair.getValue0();
                        arg = pair.getValue1();
                    }

                    //generate constraints




                } else if (defDefintionStmt.getRightOp() instanceof JCastExpr) {

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

                                key = extractKeyFromIntentExtra(defLocalAssignFromCastStmt, defs, path);//--------------------------也可以是楼上的getAction啥的-------------
                            }
                        }
                    }

                    throw new RuntimeException("handle case for JCastExpr" + defDefintionStmt);////强制转换可能还蛮多的//--------------------------------

                } else if (defDefintionStmt.getRightOp() instanceof StringConstant) {//定义是一个常量定义
                    Local local = (Local) defDefintionStmt.getLeftOp();
                    String symbol = createSymbol(local, sootMethod, defDefintionStmt);
                    symbolLocalMap.put(symbol, local);
                    StringConstant stringConst = (StringConstant) defDefintionStmt.getRightOp();
                    newDecl = "(declare-const " + symbol + " String )";
                    newAssert = "(assert (= " + symbol + " " + stringConst + " ))";
                } else if (defDefintionStmt.getRightOp() instanceof ParameterRef) {//来源于参数
                    //logger.debug("Found parameter ref when searching for original value");
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
                }
                else {// from  field or other sources


                }

            }


        }


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

    private Pair<String, String> getExtra(SimpleLocalDefs defs, Set<Unit> path, DefinitionStmt defStmt) {//intent and bundle 的extra
        String key = null;

        String type = null;

        if (defStmt.getRightOp() instanceof JVirtualInvokeExpr) {

            JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) defStmt.getRightOp();
            boolean keyExtractionEnabled = false;

            if (Pattern.matches("get.*Extra", jVirtualInvokeExpr.getMethod().getName())) {//不需要判断返回值类型的，因为前面的equals已经限制了满足这个方法是getStringExtra等

                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    keyExtractionEnabled = true;
                    type = "get.*Extra";
                }
            }
            if (Pattern.matches("has.*Extra", jVirtualInvokeExpr.getMethod().getName())) {///equals语句已经限定了这个if为false
                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().toString().equals("android.content.Intent")) {
                    keyExtractionEnabled = true;
                    type = "has.*Extra";
                }
            }
            if (Globals.bundleExtraDataMethodsSet.contains(jVirtualInvokeExpr.getMethod().getName())) {//从bundle中取出来的,但是bundle从哪来，未生成bundle的约束----------------------------
                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.os.Bundle")) {
                    keyExtractionEnabled = true;
                    type = "Bundle";//equals语句已经限定了如果是bundle的调用的话，肯定是getString(xxx);
                }
                if (jVirtualInvokeExpr.getMethod().getDeclaringClass().getName().equals("android.os.BaseBundle")) {
                    keyExtractionEnabled = true;
                    type = "BaseBundle";
                }
            }

            if (keyExtractionEnabled) {
                if (!(jVirtualInvokeExpr.getArg(0) instanceof StringConstant)) { //参数不是一个字符串常量  //defUnit是equals一个参数的定义语句比如:  x1=intent.getStringExtra(x2)
                    if (jVirtualInvokeExpr.getArg(0) instanceof Local) {//参数是一个变量
                        Local keyLocal = (Local) jVirtualInvokeExpr.getArg(0);
                        List<Unit> defUnits = defs.getDefsOfAt(keyLocal, defStmt);
                        for (Unit defUnitOfKey : defUnits) {

                            if (!isDefInPathAndLatest(path, defUnitOfKey, keyLocal, defStmt, defs)) {
                                continue;
                            }
                            if (defUnitOfKey instanceof DefinitionStmt) {
                                DefinitionStmt keyLocalDefStmt = (DefinitionStmt) defUnitOfKey;
                                if (keyLocalDefStmt.getRightOp() instanceof VirtualInvokeExpr) {//key的值来源于Enum


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
                                    throw new RuntimeException("key的值来源于Enum");
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
                                    throw new RuntimeException("key的值来源于静态属性");
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


        return new Pair<String, String>(type, key);
    }

    private void getAllPathInMethod(Unit unit, BriefUnitGraph ug, List<Set<Unit>> finalPaths, Set<Unit> curPath) {
        Set<Unit> curPathCopy = new LinkedHashSet<>(curPath);
        curPathCopy.add(unit);
        if (curPathCopy.size() >= finalPathsLimit || ug.getPredsOf(unit).size() == 0) {
            if (curPathCopy.size() >= finalPathsLimit) {
                System.out.println("hitPathsLimit=true***************************************");
            }
            finalPaths.add(curPathCopy);
        } else {
            for (Unit parentUnit : ug.getPredsOf(unit)) {
                if (!curPathCopy.contains(parentUnit))//去除循环
                {
                    getAllPathInMethod(parentUnit, ug, finalPaths, curPathCopy);
                }
            }
        }

    }


    private boolean unitNeedAnalysis(Unit unit, SootMethod sootMethod) {
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

    public void run() {
        Config.setSootOptions(appPath);
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.intentGen", this));

        PackManager.v().getPack("wjtp").apply();
    }


    public static void main(String[] args) {

        IntentConditionTransformSymbolicExcutation intentConditionTransform = new IntentConditionTransformSymbolicExcutation(Config.defaultAppPath);
        intentConditionTransform.run();

    }


}
