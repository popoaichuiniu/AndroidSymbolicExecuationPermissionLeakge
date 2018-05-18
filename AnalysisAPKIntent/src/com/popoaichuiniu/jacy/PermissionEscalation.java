package com.popoaichuiniu.jacy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.popoaichuiniu.util.Config;
import com.popoaichuiniu.util.Util;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.tagkit.Tag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Chain;

public class PermissionEscalation {

    private static BufferedWriter permissionEscalationOutput = null;// permissionEscalationOutput日志
    private static BufferedWriter hasSolvedAPPsFileWriter = null;

    private static Set<String> hasSolvedAPPsSet = null;

    public static String androidPlatformPath = "/home/zms/platforms";

    // private static String appDir = "./DroidBench/apk";
    // private static String appDir = "./MyApp";
    // private static String appDir =
    // "/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/apks_wandoujia/apks";
    //private static String appDir = "/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/down_fdroid_app_from_androzoo/f-droid-app";
    private static String appDir = "/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/InstrumentAPK/src/com/zhou/7B272D98ED01FC3AC0C2097552748860373EB724FF94767651C7B5062244971F.apk";
    // private static String appDir
    // ="/media/jacy/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/apks_wandoujia/apks/电话通讯/Messenger.apk";
    public static Logger appLogger = null;// 应用日志

    public static Logger errorRunLogger = null;// 总的运行日志

    public static BufferedWriter exceptionAPPWriter = null;// 异常app

    private static volatile int apkCount = 0;

    public static void main(String[] args) {


        try {
            exceptionAPPWriter = new BufferedWriter(new FileWriter(new File("appException.txt"), true));
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        errorRunLogger = Logger.getLogger(PermissionEscalation.class);
        File errorRunLog = new File("run.log");

        try {
            errorRunLogger.addAppender(new FileAppender(new PatternLayout("%d %p [%t] %C.%M(%L) | %m%n"), "run.log"));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        errorRunLogger.info("sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
        errorRunLogger.info("sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");
        errorRunLogger.info("sssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss");

        File hasSolvedAppsFile = new File("./AnalysisAPKIntent/hasSolvedApps.txt");
        if (!hasSolvedAppsFile.exists()) {
            try {
                hasSolvedAppsFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                errorRunLogger.error("hashasSolvedAppsFile创建失败", e);
            }
        }
        hasSolvedAPPsSet = new HashSet<>();
        try {
            BufferedReader bufferedReaderHasSolvedApp = new BufferedReader(new FileReader(hasSolvedAppsFile));
            String line = null;
            try {
                while ((line = bufferedReaderHasSolvedApp.readLine()) != null) {

                    hasSolvedAPPsSet.add(line);

                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                bufferedReaderHasSolvedApp.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            hasSolvedAPPsFileWriter = new BufferedWriter(new FileWriter(hasSolvedAppsFile, true));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<String> apkFiles = new ArrayList<String>();// apkFiles存储所有apk的名字
        findAllAPK(appDir, apkFiles);
        for (String appPath : apkFiles) {
            if (!hasSolvedAPPsSet.contains(appPath)) {

                // while(Thread.activeCount()>1)
                // {
                // //System.out.println("wait for new thread to solve task");
                // }
                Thread childThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub

                        Long startTime = System.nanoTime();
                        singleAppAnalysis(appPath);
                        Long stopTime = System.nanoTime();
                        System.out.println("运行时间:" + ((stopTime - startTime) / 1000 / 1000 / 1000 / 60) + "分钟");
                    }
                });
                childThread.start();

                try {
                    childThread.join();
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }

        }
        try {
            hasSolvedAPPsFileWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            exceptionAPPWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static void findAllAPK(String dir, List<String> arr) {
        File temp = new File(dir);
        if (temp.exists()) {
            if (temp.isDirectory()) {
                for (File file : temp.listFiles()) {
                    findAllAPK(file.getAbsolutePath(), arr);
                }
            } else {
                if (temp.getName().endsWith(".apk")) {
                    arr.add(temp.getAbsolutePath());
                }
            }
        }

    }

    public static void singleAppAnalysis(String appPath) {

        apkCount = apkCount + 1;
        errorRunLogger.info("***********************" + apkCount + "***************************");
        errorRunLogger.info(appPath + " start");
        appLogger = Logger.getLogger(appPath);
        String logfilePath = "./app_run_log/" + appPath.replaceAll("/|\\.", "_") + "_runlog.txt";
        File temp = new File(logfilePath);
        if (temp.exists()) {
            temp.delete();
        }

        try {
            appLogger.addAppender(new FileAppender(new PatternLayout("%d %p [%t] %C.%M(%L) | %m%n"), logfilePath));
            appLogger.info("***************************" + appPath + "*************************************");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("***************************************start********************************************");
        AndroidInfoHelper androidInfo = new AndroidInfoHelper(appPath);

        List<String> string_EAs = androidInfo.getString_EAs();

        Map<String, List<String>> permission_EAs = androidInfo.getEAProtctedPermission();

        if (string_EAs == null) {
            try {
                exceptionAPPWriter.write(appPath + "\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return;
        }

        AndroidCallGraphHelper androidCGHelper = new AndroidCallGraphHelper(appPath, androidPlatformPath);
        CallGraph cGraph = androidCGHelper.getCg();
        SootMethod entryPoint = androidCGHelper.getEntryPoint();

        System.out
                .println("***************************DummyMain method callee*****************************************");
        PrintAllDummyMainMethodCall(cGraph, entryPoint, appPath);

        System.out
                .println("***************************DummyMain method callee*****************************************");
        System.out.println("***************************分析正式开始*****************************************");

        File permission_escalation_path_app = new File(
                "./AnalysisAPKIntent/permission_escalation_path/" + appPath.replaceAll("/|\\.", "_") + "_PSPath.txt");
        if (!permission_escalation_path_app.exists()) {
            try {
                permission_escalation_path_app.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            permissionEscalationOutput = new BufferedWriter(new FileWriter(permission_escalation_path_app));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            appLogger.error("permissionEscalationOutput", e);
        }
        findEAToProtectedAPI(cGraph, string_EAs, entryPoint, permission_EAs,appPath);

        try {
            permissionEscalationOutput.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            appLogger.error("permissionEscalationOutput", e);
        }
        System.out.println("***************************分析正式开始*****************************************");

        System.out.println("**********************************end*****************************************");
        errorRunLogger.info(appPath + " end");
        try {
            hasSolvedAPPsFileWriter.write(appPath + "\n");
            hasSolvedAPPsFileWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void PrintAllDummyMainMethodCall(CallGraph cGraph, SootMethod entryPoint, String appPath) {
        String dummyMainMethodCallPath = "./AnalysisAPKIntent/dummyMainMethodCall/" + appPath.replaceAll("/|\\.", "_") + "dmc.txt";

        BufferedWriter appDummyMainMethodCall = null;
        try {
            appDummyMainMethodCall = new BufferedWriter(new FileWriter(new File(dummyMainMethodCallPath)));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            appLogger.error("dummyMainMethodCallFile", e);

        }
        for (Iterator<Edge> iterator = cGraph.edgesOutOf(entryPoint); iterator.hasNext(); )// DummyMain方法调用的所有方法（各个组件的回调方法和生命周期）
        {
            Edge edge = iterator.next();
            SootMethod method = edge.getTgt().method();
            System.out.println(method.getDeclaringClass().getName() + "	 " + method.getDeclaration());
            try {
                appDummyMainMethodCall
                        .write(method.getDeclaringClass().getName() + "	 " + method.getDeclaration() + "\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                appLogger.error("dummyMainMethodCallFile", e);
            }

        }
        if (appDummyMainMethodCall != null) {
            try {
                appDummyMainMethodCall.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                appLogger.error("dummyMainMethodCallFile", e);
            }
        } else {
            appLogger.error("dummyMainMethodCallFile为空");
        }
    }

    public static void findEAToProtectedAPI(CallGraph cGraph, List<String> string_EAs, SootMethod entryPoint,
                                            Map<String, List<String>> permission_EAs,String appPath) {

        List<SootMethod> ea_entryPoints = new ArrayList<>();
        for (Iterator<Edge> iterator = cGraph.edgesOutOf(entryPoint); iterator.hasNext(); )// DummyMain方法调用的所有方法（各个组件的回调方法和生命周期）
        {
            Edge edge = iterator.next();
            SootMethod method = edge.getTgt().method();

//            if (string_EAs.contains(method.getDeclaringClass().getName()))// 这个方法是不是属于EA的方法
//            {
//
//
//                // 区分回调方法还是UI方法还是生命周期等其他回调方法，（想UI方法占暂时不考虑但是不知道怎么区分，这里就一起弄)
//
//                System.out.println("111111111111111111111111111111111111111111" + method.getBytecodeSignature()
//                        + "1111111111111111111111111111111111111111111111111111111");
//
//                System.out.println(method.getActiveBody().toString());
//
//                Set<SootMethod> visited = new HashSet<>();// 不同的EA将遍历完所有可能到达的点，然后将到达的危险权限保护的API路径保存起来
//
//                Set<SootMethod> callPathCopy = new LinkedHashSet<>();
//
//                String EA_Permission = "";
//                if (permission_EAs.containsKey(method.getDeclaringClass().getName())) {
//                    EA_Permission = permission_EAs.get(method.getDeclaringClass().getName()).get(0);
//                }
//
//                //findPathToProtectedAPI(cGraph, visited, method, "", callPathCopy, EA_Permission);
//
//                wrapAllpath(cGraph, visited, method, "", callPathCopy, EA_Permission);
//
//
//                System.out.println("222222222222222222222222222222222222222222222222222222222222");
//            }


            if (string_EAs.contains(method.getDeclaringClass().getName()))// 这个方法是不是属于EA的方法
            {
                ea_entryPoints.add(method);
            }


        }

        generateUnitToAnalysis(ea_entryPoints, cGraph,appPath);
    }

    private static void generateUnitToAnalysis(List<SootMethod> ea_entryPoints, CallGraph cg,String appPath)  {
        //JimpleBasedInterproceduralCFG jcfg = new JimpleBasedInterproceduralCFG();
        ///////////////////////////////////////////////////////

//        Chain<SootClass> applicationClasses = Scene.v().getApplicationClasses();//不包括android.jar还有android.support. java的 org.的
//
//
//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++");
//
//        for (SootClass appClass : applicationClasses) {
//            System.out.println("applicationClass:"+appClass.getName());
//
//        }
//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++");
//
//
//        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
//
//        Chain<SootClass> allClasses = Scene.v().getClasses();
//
//        for (SootClass appClass : allClasses) {
//            System.out.println("allClasses:"+appClass.getName());
//
//        }
//        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++");



        ///////////////////////////////////////////////////////////


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
            System.out.println(sootMethod.getBytecodeSignature());
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
                    if (isDangerousOrSpecialProtectedAPI(calleeSootMethod)) {
                        unitsNeedToAnalysis.add(unit);
                        System.out.println("################"+unit.toString()+"################");
                    } else {

                        SootClass calleeSootClass=calleeSootMethod.getDeclaringClass();
                        if(calleeSootClass.isInterface())
                        {
                            continue;
                        }
                        List<SootClass> superClasses = h.getSuperclassesOfIncluding(calleeSootClass);
                        boolean flagisDangerousOrSpecialProtectedAPIClassSubClass = false;
                        for (SootClass sootClass : superClasses) {
                            if (isDangerousOrSpecialProtectedAPIClass(sootClass)) {
                                flagisDangerousOrSpecialProtectedAPIClassSubClass = true;
                                break;
                            }
                        }
                        if (flagisDangerousOrSpecialProtectedAPIClassSubClass) {
                            if (isDangerousOrSpecialProtectedAPIMethodName(calleeSootMethod.getName(),calleeSootMethod.getBytecodeParms())) {
                                unitsNeedToAnalysis.add(unit);
                                System.out.println("################"+unit.toString()+"################");
                                System.out.println("有子类覆盖啊");

                            }
                        }

                    }


                }

                for(Unit unit:unitsNeedToAnalysis)
                {
                    try {
                        bufferedWriterUnitsNeedAnalysis.write(sootMethod.getBytecodeSignature()+"#"+unit.getTag("BytecodeOffsetTag")+"#"+unit.toString()+"\n");
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

    private static void findPathToProtectedAPI(CallGraph cGraph, Set<SootMethod>
            visited, SootMethod sootMethod,
                                               String path, Set<SootMethod> callPath, String EA_permission) {

        Set<SootMethod> callPathCopy = new LinkedHashSet<>(callPath);// 引用类型

        path = path + "******>>>" + sootMethod.getBytecodeSignature();
        visited.add(sootMethod);//
        callPathCopy.add(sootMethod);
        // System.out.println(path);

        if (Util.isLibraryClass(sootMethod.getBytecodeSignature())) {

            System.out.println("LibraryClass()-methods:" +
                    sootMethod.getBytecodeSignature());

            if (isDangerousOrSpecialProtectedAPI(sootMethod)) {

                if (EA_permission != "" &&
                        AndroidInfoHelper.getPermissionAndroguardMethods()
                                .get(sootMethod.getBytecodeSignature()).contains(EA_permission)) {
                    return;// EA声明了保护的权限，而此时这个ProtectedAPI正好是这个权限 所以这个protectedAPI不能被执行
                }

                // System.out.println("DangerousOrSpecialProtectedAPI-methods:" +
                // sootMethod.getBytecodeSignature());

                try {
                    permissionEscalationOutput.write("ProtectedAPI" + path + "\n");// 写入路径
                    permissionEscalationOutput.flush();

                    System.out.println("ProtectedAPI" + path);


                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    appLogger.error("permissionEscalationOutput", e);
                }

            } else {

                System.out.println("Normal" + path);// 最后结束的点不是library节点
                // System.out.println("Non-DangerousOrSpecialProtectedAPI-methods:" +
                // sootMethod.getBytecodeSignature());

            }
        } else {

            System.out.println("Non-LibraryClass()-methods:" +
                    sootMethod.getBytecodeSignature());
            for (Iterator<Edge> iterator = cGraph.edgesOutOf(sootMethod);
                 iterator.hasNext(); ) {
                SootMethod target = iterator.next().getTgt().method();
                if (!visited.contains(target)) {
                    findPathToProtectedAPI(cGraph, visited, target, path, callPathCopy,
                            EA_permission);
                }

            }
        }

        // Non-LibraryClass()-methods:<android.telephony.SmsManager: sendTextMessage
        // 为什么系统的判断不准 因为这个函数在应用中出现了？所以算应用class 自己写一个

    }

    private static boolean isProtectedAPI(AndroidInfoHelper androidInfo,
                                          SootMethod sootMethod) {
        if
                (androidInfo.getPermissionMethod().containsKey(sootMethod.getBytecodeSignature())) {
            return true;
        } else {
            return false;
        }

    }


    private void getAllDangerousOrSpecialProtectedAPIMethod(Set<Unit> allSink, Set<SootMethod> callPath, SootMethod targetMethod) {

    }

    private static boolean isDangerousOrSpecialProtectedAPI(SootMethod sootMethod) {
        if (AndroidInfoHelper.getPermissionAndroguardMethods()
                .containsKey(sootMethod.getBytecodeSignature())) {
            return true;
        } else {
            return false;
        }

    }

    private static boolean isDangerousOrSpecialProtectedAPIClass(SootClass sootclass) {
        Set<String> sootMethodsStringSet = AndroidInfoHelper.getPermissionAndroguardMethods().keySet();

        for(String temp:sootMethodsStringSet)
        {
            if(temp.contains(sootclass.getName()))//这个Name是包含包名的
            {
                return true;
            }
        }
        return false;


    }

    private static boolean isDangerousOrSpecialProtectedAPIMethodName(String methodName,String param) {
        Set<String> sootMethodsStringSet = AndroidInfoHelper.getPermissionAndroguardMethods().keySet();

        for(String temp:sootMethodsStringSet)//<com.android.internal.telephony.SubscriptionController: clearDefaultsForInactiveSubIds()V>
        {
            int index0=temp.indexOf(":");
            int index1=temp.indexOf("(");
            int index2=temp.indexOf(")");
            String methodNameOfDSP=temp.substring(index0+2,index1);
            String methodParam=temp.substring(index1+1,index2);

            if (methodNameOfDSP.equals(methodName)&&methodParam.equals(param)) {
                return true;
            } else {
                return false;
            }
        }
        return  false;




    }


    private static Map<SootMethod, List<List<SootMethod>>> curToTargetPaths = new HashMap<SootMethod, List<List<SootMethod>>>();

    private static List<List<SootMethod>> allPathfindPathToProtectedAPI(CallGraph cGraph, Set<SootMethod> visited,
                                                                        SootMethod sootMethod, String path, Set<SootMethod> callPath, String EA_permission) {

        if (!sootMethod.isPhantom() && sootMethod.getActiveBody() != null) {
            List<UnitBox> units = sootMethod.getActiveBody().getAllUnitBoxes();
            for (UnitBox unitbox : units) {
                for (Tag tag : unitbox.getUnit().getTags()) {
                    System.out.println(tag.getName() + "zzzzzzzzzzz");
                }
            }
        }


        Set<SootMethod> callPathCopy = new LinkedHashSet<>(callPath);// 引用类型

        path = path + "******>>>" + sootMethod.getBytecodeSignature();
        visited.add(sootMethod);//
        callPathCopy.add(sootMethod);
        // System.out.println(path);

        if (Util.isLibraryClass(sootMethod.getBytecodeSignature())) {

            System.out.println("LibraryClass()-methods:" + sootMethod.getBytecodeSignature());

            if (isDangerousOrSpecialProtectedAPI(sootMethod)) {

                if (EA_permission != "" && AndroidInfoHelper.getPermissionAndroguardMethods()
                        .get(sootMethod.getBytecodeSignature()).contains(EA_permission)) {
                    return null;// EA声明了保护的权限，而此时这个ProtectedAPI正好是这个权限 所以这个protectedAPI不能被执行
                }

                // System.out.println("DangerousOrSpecialProtectedAPI-methods:" +
                // sootMethod.getBytecodeSignature());
                List<List<SootMethod>> toTarget_paths = null;

                toTarget_paths = new ArrayList<>();

                List<SootMethod> eachPath = new LinkedList<>();

                eachPath.add(sootMethod);

                toTarget_paths.add(eachPath);

                curToTargetPaths.put(sootMethod, toTarget_paths);

                return toTarget_paths;

            } else {

                System.out.println("Normal" + path);// 最后结束的点不是library节点
                // System.out.println("Non-DangerousOrSpecialProtectedAPI-methods:" +
                // sootMethod.getBytecodeSignature());

                return null;

            }

        } else {

            List<List<SootMethod>> curPaths = new ArrayList<>();

            System.out.println("Non-LibraryClass()-methods:" + sootMethod.getBytecodeSignature());
            for (Iterator<Edge> iterator = cGraph.edgesOutOf(sootMethod); iterator.hasNext(); ) {
                SootMethod target = iterator.next().getTgt().method();
                if (callPathCopy.contains(target)) {// 如果调用路径中存在这个了就不分析它了。 不然递归的方法的节点存储的路径会爆炸
                    continue;
                }

                List<List<SootMethod>> children = null;
                if (!visited.contains(target)) {
                    children = allPathfindPathToProtectedAPI(cGraph, visited, target, path, callPathCopy,
                            EA_permission);

                } else {//这个方法以前已经分析过了，现在的路径里还没有
                    children = curToTargetPaths.get(target);

                }
                if (children != null) {

                    for (List<SootMethod> eachPath : children) {
                        List<SootMethod> curNodeOnePath = new LinkedList<>(eachPath);
                        curNodeOnePath.add(0, sootMethod);
                        curPaths.add(curNodeOnePath);
                    }

                }

            }
            if (curPaths != null && curPaths.size() == 0) {
                curPaths = null;
            }
            if (curPaths != null) {
                curToTargetPaths.put(sootMethod, curPaths);
            }

            return curPaths;

        }

        // Non-LibraryClass()-methods:<android.telephony.SmsManager: sendTextMessage
        // 为什么系统的判断不准 因为这个函数在应用中出现了？所以算应用class 自己写一个

    }

    private static void wrapAllpath(CallGraph cGraph, Set<SootMethod> visited, SootMethod sootMethod, String path,
                                    Set<SootMethod> callPath, String EA_permission) {
        List<List<SootMethod>> toTargetPaths = allPathfindPathToProtectedAPI(cGraph, visited, sootMethod, path,
                callPath, EA_permission);
        System.out.println("************************************" + sootMethod.getDeclaration()
                + "resluts*********************************************");
        if (toTargetPaths == null) {
            System.out.println("没路径！");
            return;
        } else {
            if (toTargetPaths.size() == 0) {
                System.out.println("没路径！");
                return;
            }
        }

        for (List<SootMethod> eachPath : toTargetPaths) {
            for (SootMethod node : eachPath) {
                System.out.print(node.getDeclaration() + "---->");

                //judgeIsCheckPermission(callPath);没什么用

                try {
                    permissionEscalationOutput.write(node.getDeclaration() + "---->");
                    permissionEscalationOutput.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    appLogger.error("permissionEscalationOutput", e);
                } // 写入路径

            }
            try {
                permissionEscalationOutput.write("\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // ************************************数据流分析***************************************//

            // //Create transformer for analysis
            // FindDataSource analysisTransformer = new FindDataSource(callPathCopy);
            //
            // // Add transformer to appropriate Pack in PackManager. PackManager will run
            // all Packs when main function of Soot is called
            // PackManager.v().getPack("wjtp").add(new Transform("wjtp.FDS",
            // analysisTransformer));
            //
            // PackManager.v().getPack("wjtp").apply();

            // ************************************数据流分析***************************************//

            // ************************************intent条件啊***************************************//

            // Create transformer for analysis
            IntentConditionGenerate intentConditionGenerate = new IntentConditionGenerate(eachPath);

            intentConditionGenerate.directCall();

            // // Add transformer to appropriate Pack in PackManager. PackManager will run
            // all Packs when main function of Soot is called
            // PackManager.v().getPack("wjtp").add(new Transform("wjtp.ING",
            // intentConditionGenerate));
            //
            // PackManager.v().getPack("wjtp").apply();

            // ************************************intent条件啊***************************************/
            System.out.println();

        }
        System.out.println("************************************resluts*********************************************");
    }

    private static void judgeIsCheckPermission(Set<SootMethod> callPath) {//放到后面和intent的条件一起做，用来剪枝。

        List<SootMethod> callPathList = new ArrayList<>(callPath);

        /// *****************************************
        JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
        Map<SootMethod, List<Unit>> useMethodUnit = new HashMap<>();
        for (int i = callPathList.size() - 2; i >= 0; i--) {// 假如一个方法在某个方法中被多次调用，则callgraph只有一个。context-insensitive
            List<Unit> callUnitList = new ArrayList<>();
            System.out.println("cccccccccccccccccccccc" + callPathList.get(i) + "ccccccccccccccccccccccccc");
            Util.getAllUnitofCallPath(callPathList.get(i), callPathList.get(i + 1), callUnitList);
            System.out.println("cccccccccccccccccccccc" + callPathList.get(i) + "ccccccccccccccccccccccccc");

            useMethodUnit.put(callPathList.get(i), callUnitList);//会不会可能下面的unit会在上面的unit前执行（除了循环、递归）------------------？？？------------

        }

        BufferedWriter bufferedWriter_checkIPCPermissionUnits = null;
        try {
            bufferedWriter_checkIPCPermissionUnits = new BufferedWriter(
                    new FileWriter(new File("checkIPCPermissionUnits.txt")));
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        for (int i = 0; i < callPathList.size(); i++) {
            List<Unit> units = useMethodUnit.get(callPathList.get(i));
            for (Unit unit : units)// 如果unit调用了其他的方法，如果这个方法的返回值不是和if语句相关的，那么这个方法就不考虑
            {
                if (unit instanceof IfStmt) {
                    IfStmt ifStmt = (IfStmt) unit;
                    handleIfStmt(bufferedWriter_checkIPCPermissionUnits, ifStmt, callPathList.get(i), icfg);
                } else {

                    // Unit unit, BufferedWriter bufferedWriter_checkIPCPermissionUnits
                    isCheckIPCPermissionInvokeUnit(unit, bufferedWriter_checkIPCPermissionUnits);

                }
            }

        }
        try {
            bufferedWriter_checkIPCPermissionUnits.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void handleIfStmt(BufferedWriter bufferedWriter_checkIPCPermissionUnits, IfStmt ifStmt,
                                     SootMethod sootMethod, JimpleBasedInterproceduralCFG icfg) {
        SimpleLocalDefs defs = new SimpleLocalDefs(new BriefUnitGraph(sootMethod.getActiveBody()));

        ConditionExpr condition = (ConditionExpr) ifStmt.getCondition();

        Value opVal1 = condition.getOp1();
        Value opVal2 = condition.getOp2();
        System.out.println(opVal1.getType());
        System.out.println(opVal2.getType());
        if (opVal1.getType() instanceof BooleanType) {// -------------------------------!!!!

            if (opVal1 instanceof Local) {
                Local local = (Local) opVal1;
                List<Unit> units = defs.getDefsOfAt(local, ifStmt);// 找到对Local语句定义赋值的语句
                assert units.size() == 1;
                Unit unit = units.get(0);
                assert icfg.getCalleesOfCallAt(unit).size() == 1;
                Set<SootMethod> visited = new HashSet<>();
                for (SootMethod tempSootMethod : icfg.getCalleesOfCallAt(unit)) {// 找到此语句调用的方法
                    getAllMethodCalledBy(icfg, tempSootMethod, bufferedWriter_checkIPCPermissionUnits, visited);
                }

            }
        }

    }

    public static void getAllMethodCalledBy(JimpleBasedInterproceduralCFG icfg, SootMethod sootMethod,
                                            BufferedWriter bufferedWriter_checkIPCPermissionUnits, Set<SootMethod> visited) {
        visited.add(sootMethod);
        if (isTargetIPCPermissionCheckInvoke(sootMethod)) {
            try {
                assert icfg.getCallersOf(sootMethod).size() == 1;
                bufferedWriter_checkIPCPermissionUnits.write(icfg.getCallersOf(sootMethod) + "\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (!Util.isLibraryClass(sootMethod.getBytecodeSignature())) {
            Body body = sootMethod.getActiveBody();
            if (body != null) {
                for (Unit unit : body.getUnits()) {
                    for (SootMethod tempSootMethod : icfg.getCalleesOfCallAt(unit)) {
                        if (!visited.contains(tempSootMethod)) {
                            getAllMethodCalledBy(icfg, tempSootMethod, bufferedWriter_checkIPCPermissionUnits, visited);
                        }
                    }
                }

            }
        }

    }

    public static boolean isTargetIPCPermissionCheckInvoke(SootMethod sootMethod) {
        String patternString = "checkCallingOrSelfPermission|checkCallingOrSelfUriPermission|checkCallingPermission|checkCallingUriPermission|enforceCallingOrSelfPermission|enforceCallingOrSelfUriPermission|enforceCallingPermission|enforceCallingUriPermission";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(sootMethod.getBytecodeSignature());
        if (sootMethod.getBytecodeSignature().contains("<android.") && matcher.find()) {
            return true;
        }
        return false;
    }

    public static void isCheckIPCPermissionInvokeUnit(Unit unit,
                                                      BufferedWriter bufferedWriter_checkIPCPermissionUnits) {
        if (unit instanceof DefinitionStmt) {
            DefinitionStmt uStmt = (DefinitionStmt) unit;

            Value rValue = uStmt.getRightOp();

            if (rValue instanceof InvokeExpr) {

                InvokeExpr invokeExpr = (InvokeExpr) rValue;

                if (isTargetIPCPermissionCheckInvoke(invokeExpr.getMethod())) {
                    try {
                        bufferedWriter_checkIPCPermissionUnits.write(invokeExpr + "\n");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        } else if (unit instanceof InvokeStmt) {
            InvokeStmt invokeStmt = (InvokeStmt) unit;

            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (isTargetIPCPermissionCheckInvoke(invokeExpr.getMethod())) {
                try {
                    bufferedWriter_checkIPCPermissionUnits.write(invokeExpr + "\n");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
