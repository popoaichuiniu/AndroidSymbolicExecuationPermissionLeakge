package com.popoaichuiniu.intentGen;

import soot.Unit;
import soot.toolkits.graph.Block;

import java.util.List;

public class MyIntentBlock {

    String assertCon=null;

    String declareCon=null;
    List<Unit>  units;

    List<MyIntentBlock> parents=null;

    List<MyIntentBlock> children=null;

    public MyIntentBlock(Block block)
    {



    }

}
