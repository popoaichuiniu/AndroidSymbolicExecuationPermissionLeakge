package com.popoaichuiniu.intentGen;

import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.*;

import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;
import soot.toolkits.scalar.ForwardFlowAnalysis;

import java.util.*;

public class IntentFlowAnalysis extends ForwardFlowAnalysis<Unit, FlowSet<Value>> {


    private Map<Value, Unit> valuMapDef = null;

    private Set<Unit> visited = null;


    public IntentFlowAnalysis(DirectedGraph<Unit> graph) {

        super(graph);
        valuMapDef = new HashMap<>();
        visited = new HashSet<>();
        doAnalysis();
    }

    @Override
    protected void flowThrough(FlowSet<Value> in, Unit d, FlowSet<Value> out) {

        if (visited.contains(d)) {
            return;
        }
        visited.add(d);//去除循环
        in.copy(out);

        //System.out.println("%%%%%%%"+d);

        if (d instanceof DefinitionStmt) {
            DefinitionStmt definitionStmt = (DefinitionStmt) d;


            if (definitionStmt.getRightOp().getType().toString().equals("android.content.Intent")) {//get Intent
                if (definitionStmt.getRightOp() instanceof Local || definitionStmt.getRightOp() instanceof ParameterRef || definitionStmt.getRightOp() instanceof FieldRef) {


                    out.add(definitionStmt.getRightOp());


                    out.add(definitionStmt.getLeftOp());

                    //valuMapDef.put(definitionStmt.getRightOp(),definitionStmt);


                } else if (definitionStmt.getRightOp() instanceof JVirtualInvokeExpr) {
                    JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) definitionStmt.getRightOp();
                    if (jVirtualInvokeExpr.getMethod().getName().equals("getIntent")) {

                        out.add(definitionStmt.getLeftOp());


                    }
                    //new Intent()不要
                }
                else if(definitionStmt.getRightOp() instanceof CastExpr)
                {
                    out.add(definitionStmt.getLeftOp());
                }

            } else if (definitionStmt.containsInvokeExpr()) {// intent attribute
                if (definitionStmt.getInvokeExpr() instanceof JVirtualInvokeExpr) {
                    JVirtualInvokeExpr invokeExpr = (JVirtualInvokeExpr) definitionStmt.getInvokeExpr();
                    if (invokeExpr.getBase().getType().toString().equals("android.content.Intent")) {

                        //if (invokeExpr.getMethod().getName().startsWith("get") || invokeExpr.getMethod().getName().startsWith("has")) {

                        if (in.contains(invokeExpr))//intent from in
                        {
                            out.add(definitionStmt.getLeftOp());
                        }


                        //}


                    }

                }
            }

            //in value dataflow to others

            if (definitionStmt.getRightOp() instanceof Local) {

                if (in.contains(definitionStmt.getRightOp())) {

                    out.add(definitionStmt.getLeftOp());


                }


            } else if (definitionStmt.containsInvokeExpr()) {
                InvokeExpr invokeExpr = definitionStmt.getInvokeExpr();

                List<Value> args = invokeExpr.getArgs();
                for (Value arg : args) {
                    if (in.contains(arg)) {

                        out.add(definitionStmt.getLeftOp());


                        break;
                    }
                }
                if (invokeExpr instanceof InstanceInvokeExpr) {
                    InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                    if (in.contains(instanceInvokeExpr.getBase())) {
                        out.add(definitionStmt.getLeftOp());

                    }
                }
            }

            //kill

            if (in.contains(definitionStmt.getLeftOp())) {
                if (definitionStmt.getRightOp().getType().toString().equals("android.content.Intent")) {
                    if (definitionStmt.getRightOp() instanceof Local || definitionStmt.getRightOp() instanceof ParameterRef || definitionStmt.getRightOp() instanceof FieldRef) {

                        return;


                    } else if (definitionStmt.getRightOp() instanceof JVirtualInvokeExpr) {//new Intent()不要
                        JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) definitionStmt.getRightOp();
                        if (jVirtualInvokeExpr.getMethod().getName().equals("getIntent")) {

                            return;


                        }
                    }
                    else if(definitionStmt.getRightOp() instanceof CastExpr)
                    {
                        return;
                    }
                }

                if (definitionStmt.containsInvokeExpr()) {
                    InvokeExpr invokeExpr = definitionStmt.getInvokeExpr();

                    for (Value arg : invokeExpr.getArgs()) {
                        if (in.contains(arg)) {
                            return;
                        }
                    }

                    if (invokeExpr instanceof InstanceInvokeExpr) {
                        InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;
                        if (in.contains(instanceInvokeExpr.getBase())) {
                            return;
                        }
                    }


                }

                //this unit is not about intent,so kill

                out.remove(definitionStmt.getLeftOp());
            }


        }


    }

    @Override
    protected void merge(FlowSet<Value> in1, FlowSet<Value> in2, FlowSet<Value> out) {

        in1.union(in2, out);

    }

    @Override
    protected void copy(FlowSet<Value> source, FlowSet<Value> dest) {

        source.copy(dest);

    }

    @Override
    protected FlowSet<Value> newInitialFlow() {
        return new MyArraySparseSet<>();
    }

    @Override
    protected FlowSet<Value> entryInitialFlow() {
        return new MyArraySparseSet<>();
    }
}


