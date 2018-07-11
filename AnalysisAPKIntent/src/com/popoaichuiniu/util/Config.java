package com.popoaichuiniu.util;

import soot.Main;
import soot.Scene;
import soot.options.Options;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Config {


    public  static  String androidJar="/home/zms/platforms";

    public  static  String defaultAppPath ="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestWebView2/app/build/outputs/apk/debug/app-debug.apk";

    public static String fDroidAPPDir="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/sootOutput";

    public static String wandoijiaAPP="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/apks_wandoujia/apks/all_app";
    public static String selectAPP="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/selectAPP";


    public static  String Z3_RUNTIME_SPECS_DIR="Z3_RUNTIME_SPECS_DIR";

    public static boolean isTest=false;

    // public  static  String defaultAppPath="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/AnalysisAPKIntent/万花筒之旅一宝宝巴士.apk";
    public static  void setSootOptions(String appPath)
    {
        soot.G.reset();// 标准的soot操作，清空soot之前所有操作遗留下的缓存值

        Options.v().set_src_prec(Options.src_prec_apk);// 设置优先处理的文件格式

        //Options.v().set_process_dir(Collections.singletonList(defaultAppPath));// 处理文件夹中所有的class
        // singletonList(T) 方法用于返回一个只包含指定对象的不可变列表

        //Options.v().set_android_jars(androidPlatformPath);// 在该路径下找android.jar

        //////////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////

        Options.v().set_app(false);





//		Options.v().set_app(true);//
//
//		List<String> excludeList = new LinkedList<String>();
//		excludeList.add("java.*");
//		excludeList.add("sun.misc.*");//app false也是可以设置这个的
//		excludeList.add("android.*");
//		excludeList.add("org.apache.*");
//		excludeList.add("soot.*");
//		excludeList.add("javax.servlet.*");
//		Options.v().set_exclude(excludeList);//这个底下也有，那个sootConfig.setSootOptions(Options.v());
//
//		List<String> includeList = new LinkedList<String>();
//		includeList.add("android.support.*");//不添加的话会有android.support.* 某类的getActiveBody为空的错误，
//		//为什么android.*不会呢？是应为classpath有android.jar吗？测试了，好像也不行。
//		Options.v().set_include(includeList);




        ////////////////////////////////////////////////////////////////////////////////





        Options.v().set_whole_program(true);// 以全局模式运行，这个默认是关闭的，否则不能构建cg(cg是全局的pack)

        Options.v().set_allow_phantom_refs(true);// 允许未被解析的类，可能导致错误




        //Phantom classes are classes that are neither in the process directory nor on
//		the Soot classpath, but that are referenced by some class / method body that
//		Soot loads. If phantom classes are enabled, Soot will not just abort and
//		fail on such an unresolvable reference, but create an empty stub called a
//		phantom class which in turn contains phanom methods to make up for the
//		missing bits and pieces.


        //设置cg pack的选项

        Options.v().setPhaseOption("cg.cha", "on");//不用设置的默认就为true

        Options.v().setPhaseOption("cg.cha", "verbose:true");

        Options.v().setPhaseOption("cg.cha", "apponly:true");

        //Options.v().setPhaseOption("cg.spark", "off");// 默认为fasle 构建cg的选项，spark是一个指向性分析框架 这个打开的会可能会消除一些 节点

        Options.v().set_output_format(Options.output_format_none);

        Options.v().set_process_dir(Collections.singletonList(appPath));

        Options.v().set_android_jars(androidJar);


        // Set the Soot configuration options. Note that this will needs to be
        // done before we compute the classpath.

        List<String> excludeList = new LinkedList<String>();
        excludeList.add("java.*");
        excludeList.add("sun.misc.*");
        excludeList.add("android.*");
        excludeList.add("org.apache.*");
        excludeList.add("soot.*");
        excludeList.add("javax.servlet.*");
        Options.v().set_exclude(excludeList);
        Options.v().set_no_bodies_for_excluded(true);


        Options.v().set_keep_line_number(true);

        Options.v().set_coffi(true);

        Options.v().set_keep_offset(true);


        //Options.v().set_soot_classpath(apkFileLocation+ File.pathSeparator+"/home/zms/platforms/android-27/android.jar");
        Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(androidJar, appPath));//+":/media/softdata/AndroidSDKdirectory/extras/android/support"
        Main.v().autoSetOptions();

        // Load whetever we need
        Scene.v().loadNecessaryClasses();

    }
}
