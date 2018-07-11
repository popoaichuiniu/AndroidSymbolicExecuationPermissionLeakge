package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Config;
import com.popoaichuiniu.util.MyLogger;
import com.popoaichuiniu.util.ReadFile;
import com.popoaichuiniu.util.Util;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.*;
import java.util.*;

public class GenerateUnitNeedToAnalysis {


    private static boolean isTest=false;


    private static  BufferedWriter bufferedWriterOverridePermissionMethod=null;
    static Set<String> dangerousPermissions=null;
    static Map<String,Set<String>> apiPermissionMap= AndroidInfoHelper.getPermissionAndroguardMethods();
    private static void generateUnitToAnalysis(List<SootMethod> ea_entryPoints, CallGraph cg, String appPath)  {


        Hierarchy h = Scene.v().getActiveHierarchy();


        List<SootMethod> roMethods = Util.getMethodsInReverseTopologicalOrder(ea_entryPoints, cg);


        BufferedWriter bufferedWriterUnitsNeedAnalysis =null;

        try {
            bufferedWriterUnitsNeedAnalysis = new BufferedWriter(new FileWriter(appPath + "_" + "UnitsNeedAnalysis.txt"));
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }


        for (SootMethod sootMethod : roMethods) {
            MyLogger.getOverallLogger(GenerateUnitNeedToAnalysis.class).info(sootMethod.getBytecodeSignature());
            List<Unit> unitsNeedToAnalysis = new ArrayList<>();

            Body body = sootMethod.getActiveBody();
            if (body != null) {
                PatchingChain<Unit> units = body.getUnits();
                for (Unit unit : units) {

                    SootMethod calleeSootMethod=Util.getCalleeSootMethodat(unit);
                    if(calleeSootMethod==null)
                    {
                        continue;
                    }
                    Set<String> permissionSet=apiPermissionMap.get(calleeSootMethod.getBytecodeSignature());
                    if (permissionSet!=null&&isExistSimilarItem(permissionSet,dangerousPermissions)) {
                        unitsNeedToAnalysis.add(unit);
                        MyLogger.getOverallLogger(GenerateUnitNeedToAnalysis.class).info("################"+unit.toString()+"################");
                    } else {

                        SootClass calleeSootClass=calleeSootMethod.getDeclaringClass();
                        if(calleeSootClass.isInterface())//是接口就不可能调用是权限保护的API方法。因为接口是需要用户自己实现的
                        {
                            continue;
                        }
                        List<SootClass> superClasses = h.getSuperclassesOfIncluding(calleeSootClass);
                        boolean flagisDangerousOrSpecialProtectedAPIClassSubClass = false;
                        for (SootClass sootClass : superClasses) {
                            if (Util.isPermissionProtectedAPIClass(sootClass)) {
                                flagisDangerousOrSpecialProtectedAPIClassSubClass = true;
                                break;
                            }
                        }
                        if (flagisDangerousOrSpecialProtectedAPIClassSubClass) {
                            if (Util.isPermissionProtectedAPIMethodName(calleeSootMethod.getName(),calleeSootMethod.getBytecodeParms())) {
                                unitsNeedToAnalysis.add(unit);
                                MyLogger.getOverallLogger(GenerateUnitNeedToAnalysis.class).info("################"+unit.toString()+"################");
                                //MyLogger.getOverallLogger(GenerateUnitNeedToAnalysis.class).info("有子类覆盖啊");
                                //throw new RuntimeException("有子类覆盖啊");
                                try {
                                    bufferedWriterOverridePermissionMethod.write(appPath+"#"+sootMethod.getBytecodeSignature()+"#"+unit.toString()+"\n");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                    }


                }

                for(Unit unit:unitsNeedToAnalysis)
                {
                    try {
                        Stmt stmt= (Stmt) unit;
                        InvokeExpr invokeExpr=stmt.getInvokeExpr();
                        if(invokeExpr==null)
                        {
                            throw new RuntimeException("illegal unitNeedAnalysis");
                        }
                        else
                        {
                            bufferedWriterUnitsNeedAnalysis.write(sootMethod.getBytecodeSignature()+"#"+unit.getTag("BytecodeOffsetTag")+"#"+unit.toString()+"#"+invokeExpr.getMethod().getBytecodeSignature()+"\n");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        try {
            bufferedWriterUnitsNeedAnalysis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private static boolean isExistSimilarItem(Set<String> permissionSet, Set<String> dangerousPermissions) {
        for(String permission:permissionSet)
        {
            if(dangerousPermissions.contains(permission))
            {
                return true;
            }
        }
        return false;
    }

    public static  void  main(String [] args)
    {

        dangerousPermissions=new ReadFile("AnalysisAPKIntent/unitNeedAnalysisGenerate/dangerousPermission.txt").getAllContentLinSet();
        for(Iterator<String> dangerousPermissionsIterator=dangerousPermissions.iterator();((Iterator) dangerousPermissionsIterator).hasNext();)
        {
            String dangerousPermission=dangerousPermissionsIterator.next();
            if(dangerousPermission.startsWith("#"))
            {
                dangerousPermissionsIterator.remove();
            }
        }
        //String appDirPath="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/sootOutput";

        //String appDirPath=Config.wandoijiaAPP;
        String appDirPath=null;
        if(isTest)
        {
            appDirPath=Config.defaultAppPath;
        }
        else
        {
            appDirPath=Config.selectAPP;
        }


        File appDir=new File(appDirPath);
        try {

            bufferedWriterOverridePermissionMethod=new BufferedWriter(new FileWriter("AnalysisAPKIntent/unitNeedAnalysisGenerate/"+appDir.getName()+"_overridePermissionMethodSituation.txt"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        if(appDir.isDirectory())
        {




            List<String> hasGenerateAppList=null;
            try {
                hasGenerateAppList=new ArrayList<>();
                BufferedReader bufferedReaderHasGenerateUnitNeedAnalysis=new BufferedReader(new FileReader("AnalysisAPKIntent/unitNeedAnalysisGenerate/"+appDir.getName()+"_hasGeneratedAPP.txt"));

                String line=null;

                while ((line=bufferedReaderHasGenerateUnitNeedAnalysis.readLine())!=null)
                {
                    hasGenerateAppList.add(line);
                }

                bufferedReaderHasGenerateUnitNeedAnalysis.close();

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


            if (hasGenerateAppList==null)
            {
                return;
            }

            BufferedWriter bufferedWriterHasGenerateUnitNeedAnalysis=null;

            try {

                bufferedWriterHasGenerateUnitNeedAnalysis =new BufferedWriter(new FileWriter("AnalysisAPKIntent/unitNeedAnalysisGenerate/"+appDir.getName()+"_hasGeneratedAPP.txt"));

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


            for(File apkFile:appDir.listFiles())
            {
                if(apkFile.getName().endsWith(".apk")&&(!apkFile.getName().contains("_signed_zipalign")))

                {
                    if(hasGenerateAppList.contains(apkFile.getAbsolutePath()))
                    {
                        continue;
                    }

                    Thread childThread=new Thread(new Runnable() {
                        @Override
                        public void run() {

                            singleAPPAnalysis(apkFile.getAbsolutePath());
                        }
                    });

                    childThread.start();

                    try {
                        childThread.join();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        bufferedWriterHasGenerateUnitNeedAnalysis.write(apkFile.getAbsolutePath()+"\n");
                        bufferedWriterHasGenerateUnitNeedAnalysis.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
            try {
                bufferedWriterHasGenerateUnitNeedAnalysis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
        {
            singleAPPAnalysis(appDirPath);
        }


        try {
            bufferedWriterOverridePermissionMethod.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public  static  void  singleAPPAnalysis(String appPath)
    {




        AndroidInfoHelper androidInfo = new AndroidInfoHelper(appPath);
        List<String> string_EAs = androidInfo.getString_EAs();


        AndroidCallGraphHelper androidCallGraphHelper=new AndroidCallGraphHelper(appPath,Config.androidJar);


        CallGraph cGraph=androidCallGraphHelper.getCg();

        SootMethod entryPoint=androidCallGraphHelper.getEntryPoint();




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

        generateUnitToAnalysis(ea_entryPoints, cGraph,appPath);
    }
}
