package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.base.ExitJStmt;
import soot.Body;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.*;

public class MyUnitGraph extends BriefUnitGraph {

    public MyUnitGraph(Body body) {
        super(body);

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
        if (!unitToPreds.keySet().containsAll(unitToSuccs.keySet()) && unitToSuccs.keySet().containsAll(unitToPreds.keySet())) {
            throw new RuntimeException("冲突");
        }

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
}
