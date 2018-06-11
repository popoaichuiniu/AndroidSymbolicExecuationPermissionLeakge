package com.popoaichuiniu.util;



import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class WriteFile {

    private String fileName=null;

    private BufferedWriter bufferedWriter=null;

    public WriteFile(String fileName,boolean append) {
        this.fileName = fileName;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(fileName, append));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    public void writeStr(String str)
    {
        try {
            bufferedWriter.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public  void  flush()
    {
        try {
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close()
    {
        try {
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
