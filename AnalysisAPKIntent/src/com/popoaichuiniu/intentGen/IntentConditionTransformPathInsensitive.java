package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Config;
import com.popoaichuiniu.util.Util;
import soot.*;
import soot.jimple.ConditionExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.FlowAnalysis;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IntentConditionTransformPathInsensitive extends SceneTransformer {


    static BufferedWriter intentDefineUnitFile = null;

    static BufferedWriter intentDefineUnitFileError11111111 = null;

    static BufferedWriter intentDefineUnitFileError22222222 = null;

    static BufferedWriter ifUnitFile = null;


    private String appPath = null;

    public IntentConditionTransformPathInsensitive(String appPath) {
        this.appPath = appPath;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {

        AndroidCallGraphHelper androidCallGraphHelper = new AndroidCallGraphHelper(appPath, Config.androidJar);
        AndroidInfoHelper androidInfoHelper = new AndroidInfoHelper(appPath);


        List<SootMethod> ea_entryPoints = Util.getEA_EntryPoints(androidCallGraphHelper, androidInfoHelper);

        List<SootMethod> roMethods = Util.getMethodsInReverseTopologicalOrder(ea_entryPoints, androidCallGraphHelper.getCg());

        for (SootMethod sootMethod : roMethods) {


            Body body = sootMethod.getActiveBody();

            BriefUnitGraph briefUnitGraph = new BriefUnitGraph(body);

            SimpleLocalDefs defs = new SimpleLocalDefs(briefUnitGraph);


            IntentFlowAnalysis intentFlowAnalysis = new IntentFlowAnalysis(briefUnitGraph);


            System.out.println("#############################"+sootMethod.getBytecodeSignature()+"################################");


            for (Unit unit : body.getUnits()) {

                if (unit instanceof DefinitionStmt) {
                    DefinitionStmt definitionStmt = (DefinitionStmt) unit;

                    if (definitionStmt.containsInvokeExpr()) {
                        InvokeExpr invokeExpr = definitionStmt.getInvokeExpr();


                        if (invokeExpr.getMethod().getDeclaringClass().getName().equals("android.content.Intent")) {
                            if (invokeExpr instanceof JVirtualInvokeExpr) {
                                JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) invokeExpr;

                                Value value = jVirtualInvokeExpr.getBase();

                                if (value instanceof Local) {
                                    Local intentLocal = (Local) value;

                                    for (Unit unitDefIntentLocal : defs.getDefsOfAt(intentLocal, definitionStmt)) {

                                        try {
                                            intentDefineUnitFile.write(unitDefIntentLocal + "##########" + definitionStmt + "\n");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }


                                    }


                                } else {
                                    //throw new RuntimeException("222222222222222222222222222222");
                                    try {
                                        intentDefineUnitFileError22222222.write(definitionStmt + "\n");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } else {
                                //throw new RuntimeException("11111111111111111111111111111");

                                try {
                                    intentDefineUnitFileError11111111.write(definitionStmt + "\n");//不是JvirtualExptr
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                }

                if (unit instanceof JIfStmt) {
                    FlowSet<Value> flowSet = intentFlowAnalysis.getFlowBefore(unit);
                    System.out.println("111111111111111111111111111111111111");


                    for (Value value : flowSet) {
                        System.out.println(value);
                    }
                    System.out.println("2222222222222222222222222222222222222");
                }
                System.out.println(unit);

                if (unit instanceof JIfStmt) {//数据流分析，判断intent值是否到达Ifstmt

                    IfStmt ifStmt = (IfStmt) unit;
                    ConditionExpr condition = (ConditionExpr) ifStmt.getCondition();
                    Value op1 = condition.getOp1();

                    Value op2 = condition.getOp2();

                    FlowSet<Value> flowSet = intentFlowAnalysis.getFlowBefore(ifStmt);

                    if (flowSet.contains(op1)) {
                        if (op1 instanceof Local) {
                            for (Unit defCon1Unit : defs.getDefsOfAt((Local) op1, ifStmt)) {
                                if (defCon1Unit instanceof DefinitionStmt) {
                                    try {
                                        ifUnitFile.write(defCon1Unit + "###########" + ifStmt + "\n");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }

                    if (flowSet.contains(op2)) {
                        if (op2 instanceof Local) {
                            for (Unit defCon1Unit : defs.getDefsOfAt((Local) op2, ifStmt)) {
                                if (defCon1Unit instanceof DefinitionStmt) {
                                    try {
                                        ifUnitFile.write(defCon1Unit + "###########" + ifStmt + "\n");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }


                }
            }


        }

    }


    public void run() {

        Config.setSootOptions(appPath);
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.intentPathInsensitive", this));

        PackManager.v().getPack("wjtp").apply();
    }


    public static void main(String[] args) {


        try {
            intentDefineUnitFile = new BufferedWriter(new FileWriter("intentDefUnit.txt"));

            intentDefineUnitFileError11111111 = new BufferedWriter(new FileWriter("intentDefUnitError1111111.txt"));
            intentDefineUnitFileError22222222 = new BufferedWriter(new FileWriter("intentDefUnitError2222222.txt"));

            ifUnitFile = new BufferedWriter(new FileWriter("ifStmtDefAboutIntent.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String appDir = "/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/sootOutput";
        //String appDir = "/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/AnalysisAPKIntent/src/com/popoaichuiniu/intentGen/15B08211CDC4FD079F3B1297CD279347958C7F53FEE51EC204B217FE01D0472F_signed_zipalign.apk";
        //String appDir=Config.defaultAppPath;
        File appDirFile = new File(appDir);


        if (appDirFile.isDirectory()) {


            List<String> hasAnalysisAPP = null;

            try {

                BufferedReader hasAnalysisAPPBufferedReader = new BufferedReader(new FileReader("hasSatisticIfDefIntentDefAPP.txt"));
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
                hasAnalysisAPPBufferedWriter = new BufferedWriter(new FileWriter("hasSatisticIfDefIntentDefAPP.txt", true));
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (File file : appDirFile.listFiles()) {
                if (file.getName().endsWith("_signed_zipalign.apk")) {

                    if (hasAnalysisAPP.contains(file.getAbsolutePath())) {
                        continue;
                    }

                    Thread childThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            IntentConditionTransformPathInsensitive intentConditionTransform = new IntentConditionTransformPathInsensitive(file.getAbsolutePath());
                            intentConditionTransform.run();
                            try {
                                intentDefineUnitFile.flush();
                                intentDefineUnitFileError11111111.flush();
                                intentDefineUnitFileError22222222.flush();
                                ifUnitFile.flush();


                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    childThread.start();

                    try {
                        childThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        hasAnalysisAPPBufferedWriter.write(file.getAbsolutePath() + "\n");
                        hasAnalysisAPPBufferedWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                }
            }
            try {
                hasAnalysisAPPBufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            IntentConditionTransformPathInsensitive intentConditionTransform = new IntentConditionTransformPathInsensitive(appDirFile.getAbsolutePath());
            intentConditionTransform.run();
            try {
                intentDefineUnitFile.flush();
                intentDefineUnitFileError11111111.flush();
                intentDefineUnitFileError22222222.flush();
                ifUnitFile.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            intentDefineUnitFile.close();
            intentDefineUnitFileError11111111.close();
            intentDefineUnitFileError22222222.close();

            ifUnitFile.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}