package com.popoaichuiniu.test;

import java.util.*;

import com.popoaichuiniu.util.Config;
import org.apache.log4j.Logger;

import com.popoaichuiniu.jacy.AndroidCallGraphHelper;


import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.*;

public class TestSootMethod {

    private static String androidPlatformPath = "/home/zms/platforms";

    public static Logger appLogger = null;// 应用日志

    public static void main(String[] args) {


        AndroidCallGraphHelper androidCallGraphHelper = new AndroidCallGraphHelper(Config.defaultAppPath, androidPlatformPath);

        CallGraph cGraph = androidCallGraphHelper.getCg();
        SootMethod entryPoint = androidCallGraphHelper.getEntryPoint();


        for (Iterator<Edge> iterator = cGraph.edgesOutOf(entryPoint); iterator.hasNext(); )// DummyMain方法调用的所有方法（各个组件的回调方法和生命周期）
        {
            SootMethod sootMethod = iterator.next().tgt();


            if (sootMethod.getDeclaration().contains("onCreate") && sootMethod.getDeclaringClass().getName().contains("MainActivity")) {
                System.out.println(sootMethod.getActiveBody().getUnits());


                System.out.println("**********************************************");
                for (Iterator<Edge> iteratorOnCreate = cGraph.edgesOutOf(sootMethod); iteratorOnCreate.hasNext(); ) {
                    SootMethod sootMethodTgt = iteratorOnCreate.next().tgt();
                    System.out.println(sootMethodTgt.getBytecodeSignature());
                    //System.out.println(sootMethodTgt.getActiveBody());
                }
                System.out.println("**********************************************");
//
////                       结果没有addAll <com.google.common.collect.HashMultiset: create()Lcom/google/common/collect/HashMultiset;>
////<java.util.HashSet: <init>()V>
////<com.example.lab418.testwebview2.MainActivity: sendSMS(Ljava/lang/String;Ljava/lang/String;)V>
////<android.support.v7.app.AppCompatActivity: setContentView(I)V>
//                    }

            }
        }


    }

    public static void getAllBlockPath(Block block, Set<Block> callBlockPath, List<Set<Block>> allBlockPath) {

        Set<Block> callBlockPathCopy = new LinkedHashSet<>(callBlockPath);

        callBlockPathCopy.add(block);


        if (block.getSuccs().size() == 0) {
            allBlockPath.add(callBlockPathCopy);
            return;
        } else {
            for (Block childBlock : block.getSuccs()) {
                if (!callBlockPathCopy.contains(childBlock)) {
                    getAllBlockPath(childBlock, callBlockPathCopy, allBlockPath);
                }
            }
        }

    }


}
