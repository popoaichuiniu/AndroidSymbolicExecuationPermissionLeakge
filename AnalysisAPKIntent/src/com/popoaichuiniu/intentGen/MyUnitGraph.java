package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.base.ExitJStmt;
import soot.Body;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.*;

public class MyUnitGraph extends BriefUnitGraph {

    private Unit targetUnit=null;

    public MyUnitGraph(Body body ,Unit targetUnit) {
        super(body);

        this.targetUnit=targetUnit;

        for (Unit unit : body.getUnits()) {
            if (unitToPreds.get(unit) == null) {
                unitToPreds.put(unit, new ArrayList<>());
            }
            if (unitToSuccs.get(unit) == null) {
                unitToSuccs.put(unit, new ArrayList<>());
            }
        }

//        for(Unit unit:body.getUnits())
//        {
//            for(Unit s:unitToSuccs.get(unit))
//            {
//                assert unitToPreds.get(s).contains(unit);
//            }
//
//            for(Unit p:unitToPreds.get(unit))
//            {
//                assert  unitToSuccs.get(p).contains(unit);
//            }
//        }

    }

    public Map<Unit, List<Unit>> getUnitToPreds() {
        return unitToPreds;
    }

    public Map<Unit, List<Unit>> getUnitToSuccs() {
        return unitToSuccs;
    }

    public Set<Unit> getAllUnit() {
//        if (!unitToPreds.keySet().containsAll(unitToSuccs.keySet()) && unitToSuccs.keySet().containsAll(unitToPreds.keySet())) {
//            throw new RuntimeException("冲突");
//        }

        return unitToPreds.keySet();
    }

    public void deleteUnit(Unit unit) {

        List<Unit> parents = unitToPreds.get(unit);

        List<Unit> successors = unitToSuccs.get(unit);


        if (parents != null && successors != null) {


            for (Unit p : parents) {

                HashSet<Unit> pSuccessorsSet=new HashSet<>(unitToSuccs.get(p));//本来的后继

                pSuccessorsSet.addAll(successors);

                pSuccessorsSet.remove(unit);

                unitToSuccs.put(p,new ArrayList<>(pSuccessorsSet));
            }


            for (Unit s : successors) {

                HashSet<Unit> sPredecessors=new HashSet<>(unitToPreds.get(s));//本来的前继

                sPredecessors.addAll(parents);
                sPredecessors.remove(unit);
                unitToPreds.put(s,new ArrayList<>(sPredecessors));

            }

            unitToSuccs.remove(unit);
            unitToPreds.remove(unit);


        } else {
            throw new RuntimeException("算法异常！");
        }


    }

    public void addEndReturnNode(Unit exitUnit) {

        for (Unit targetUnit : this.getBody().getUnits()) {
            if ((targetUnit instanceof ReturnVoidStmt || targetUnit instanceof ReturnStmt) && (exitUnit instanceof ExitJStmt)) {
                List<Unit> successorsOfReturn = unitToSuccs.get(targetUnit);
                assert successorsOfReturn == null;

                successorsOfReturn = new ArrayList<>();

                successorsOfReturn.add(exitUnit);

                unitToSuccs.put(targetUnit, successorsOfReturn);


                List<Unit> parentsOfExit = unitToPreds.get(exitUnit);

                if (parentsOfExit == null) {
                    parentsOfExit = new ArrayList<>();
                }

                parentsOfExit.add(targetUnit);

                unitToPreds.put(exitUnit, parentsOfExit);


            }

        }


    }

    public void changeInherit(Unit ifUnit, Unit joinUnit)//(去除If语句块)
    {

        List<Unit> ifParents = unitToPreds.get(ifUnit);

        List<Unit> joinParents = new ArrayList<>();


        if (ifParents != null) {
            for (Unit ifFather : ifParents) {
                List<Unit> ifFatherChilds = unitToSuccs.get(ifFather);
                if (ifFatherChilds != null) {
                    ifFatherChilds.remove(ifUnit);//去除原来的if的后继
                    ifFatherChilds.add(joinUnit);//直接加入joinUnit作为后继
                }

                joinParents.add(ifFather);


            }

        }

        if (joinParents != null) {
            unitToPreds.put(joinUnit, joinParents);//join的前驱为ifUnit的前驱
        }

        unitToPreds.remove(ifUnit);
        unitToSuccs.remove(ifUnit);


    }

    public void removeExitJstmt(Unit exitUnit) {
        unitToSuccs.remove(exitUnit);
        unitToPreds.remove(exitUnit);
        for (Unit targetUnit : this.getBody().getUnits()) {
            if ((targetUnit instanceof ReturnVoidStmt || targetUnit instanceof ReturnStmt) && (exitUnit instanceof ExitJStmt)) {
                unitToSuccs.remove(targetUnit);


            }

        }
    }

    public boolean equivTo(MyUnitGraph otherUnitGraph)
    {
        if(otherUnitGraph==null)
        {
            return false;
        }
        if(this.getAllUnit().size()!=otherUnitGraph.getAllUnit().size())
        {
            return false;
        }

        for(Unit unit:this.getAllUnit())//targetUnit不在考察范围之内
        {
            Unit unit1=null;
            Unit unit2=null;
            if(unit==targetUnit)
            {
                unit1=targetUnit;
                unit2=otherUnitGraph.targetUnit;
            }
            else
            {
                unit1=unit;
                unit2=unit;
            }

            Set<Unit> parents=new HashSet<>(getPredsOf(unit1));
            parents.remove(targetUnit);
            Set<Unit> otherParents=new HashSet<>(otherUnitGraph.getPredsOf(unit2));
            otherParents.remove(otherUnitGraph.targetUnit);
            if(parents.size()!=otherParents.size())
            {
                return false;
            }

            if(!parents.containsAll(otherParents))
            {
                return false;
            }

            Set<Unit> children=new HashSet<>(getSuccsOf(unit1));
            children.remove(targetUnit);
            Set<Unit> otherChildren=new HashSet<>(otherUnitGraph.getSuccsOf(unit2));
            otherChildren.remove(otherUnitGraph.targetUnit);
            if(children.size()!=otherChildren.size())
            {
                return false;
            }

            if(!children.containsAll(otherChildren))
            {
                return  false;
            }
        }


        return true;



    }
}
