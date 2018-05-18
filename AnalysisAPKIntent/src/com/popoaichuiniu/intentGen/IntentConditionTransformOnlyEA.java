package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;
import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Util;
import com.popoaichuiniu.util.Config;
import org.javatuples.Pair;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.tagkit.BytecodeOffsetTag;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

import java.io.*;
import java.util.*;

public class IntentConditionTransformOnlyEA
        extends SceneTransformer {




    private String appPath=null;

    private String platforms=null;

    public IntentConditionTransformOnlyEA
            (String apkFilePath,String platforms) {
        this.appPath = apkFilePath;
        this.platforms = platforms;


    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {


        //AndroidCallGraphHelper androidCallGraphHelper = new AndroidCallGraphHelper(defaultAppPath,platforms);
        AndroidInfoHelper androidInfoHelper = new AndroidInfoHelper(appPath);
        String packageName=androidInfoHelper.getPackageName(appPath);

        Map<String,AXmlNode> eas=androidInfoHelper.getEAs();


        System.out.println("(((((((((((((((((((((((((((((");
        System.out.println(appPath);


        List<IntentInfo> intentInfoList=new ArrayList<>();
        for(Map.Entry<String,AXmlNode> eaComponent:eas.entrySet())
        {
            System.out.println(eaComponent.getKey());
            IntentInfo intentInfo=new IntentInfo();
            intentInfo.appPath=appPath;
            intentInfo.appPackageName=packageName;
            intentInfo.comPonentName=eaComponent.getKey();
            intentInfo.comPonentType=eaComponent.getValue().getTag();
            System.out.println(intentInfo.comPonentType);
            intentInfoList.add(intentInfo);

        }

        IntentInfoFileGenerate.generateIntentInfoFile(appPath,intentInfoList);
        System.out.println("))))))))))))))))))))))))))))))");





    }



    public void run() {
        Config.setSootOptions(appPath);
        PackManager.v().getPack("wjtp")
                .add(new Transform("wjtp.intentGenOnlyEA", this));

        PackManager.v().getPack("wjtp").apply();
    }


    public static void main(String[] args) {
        String APKDir="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/sootOutput";

        for(File apkFile:new File(APKDir).listFiles())
        {

            if(apkFile.getName().endsWith("_signed_zipalign.apk"))
            {
                IntentConditionTransformOnlyEA
                        intentConditionTransform = new IntentConditionTransformOnlyEA
                        (apkFile.getAbsolutePath(),Config.androidJar);
                intentConditionTransform.run();
            }

        }




    }


}
