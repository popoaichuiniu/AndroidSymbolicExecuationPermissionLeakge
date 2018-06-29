package com.popoaichuiniu.util;

public class UnHandleWriter {


   private static WriteFile writeFile=new WriteFile("AnalysisAPKIntent/intentConditionSymbolicExcutationResults/"+"unhandleSituation.txt",true);
    public static void write(String message) {
        writeFile.writeStr(message);
        writeFile.flush();
    }


}
