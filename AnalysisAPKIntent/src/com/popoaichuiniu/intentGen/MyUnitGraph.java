package com.popoaichuiniu.intentGen;

import soot.Body;
import soot.Unit;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.ArrayList;
import java.util.List;

public class MyUnitGraph extends BriefUnitGraph {

    public MyUnitGraph(Body body) {
        super(body);
    }

    public boolean addEndReturnNode(Unit targetUnit,Unit exitUnit)
    {
        if((targetUnit instanceof ReturnVoidStmt|| targetUnit instanceof ReturnStmt)&& (exitUnit instanceof ExitJStmt))
        {
            List<Unit> successorsOfReturn=unitToSuccs.get(targetUnit);
            assert successorsOfReturn==null;

            successorsOfReturn=new ArrayList<>();

            successorsOfReturn.add(exitUnit);


            List<Unit> parentsOfExit=unitToPreds.get(exitUnit);

            if(parentsOfExit==null)
            {
                parentsOfExit=new ArrayList<>();
            }

            parentsOfExit.add(targetUnit);

            unitToPreds.put(exitUnit,parentsOfExit);


            return true;


        }
        else
        {
            return false;
        }

    }

    public void changeInherit(Unit head,List<Unit> successorsNeedToRemoveOfHead,Unit tail,List<Unit> parentsNeedToRemoveOfHead)
    {



    }
}
