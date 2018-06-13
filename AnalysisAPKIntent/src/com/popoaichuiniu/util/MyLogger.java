package com.popoaichuiniu.util;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.IOException;

public class MyLogger {




    private  Logger logger=null;




    public static Logger getOverallLogger(Class logger)
    {
        Logger runLogger=Logger.getLogger(logger);
        if(runLogger.getAllAppenders().hasMoreElements())
        {
            return runLogger;
        }
        String logfilePathRun="AnalysisAPKIntent/logger_file/"+logger.getName()+".log";
        try {
        runLogger.addAppender(new FileAppender(new PatternLayout("%d %p [%t] %C.%M(%L) | %m%n"), logfilePathRun));
        runLogger.addAppender(new ConsoleAppender(new PatternLayout("%d %p [%t] %C.%M(%L) | %m%n")));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return runLogger;
    }


    public MyLogger(String loggerName) {
        this.logger=Logger.getLogger(loggerName);
        String logfilePath="AnalysisAPKIntent/logger_file/"+loggerName+".log";
        try {
            this.logger.addAppender(new FileAppender(new PatternLayout("%d %p [%t] %C.%M(%L) | %m%n"), logfilePath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }



    }

    public Logger getLogger() {
        return logger;
    }
}
