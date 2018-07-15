package com.popoaichuiniu.util;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReadFileOrInputStream {

    private String fileName=null;

    private BufferedReader bufferedReader=null;

    private List<String>  contentList=null;

    private Set<String > contentSet=null;




    public ReadFileOrInputStream(String fileName) {
        this.fileName = fileName;
        try {
            bufferedReader=new BufferedReader(new FileReader(fileName));
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public ReadFileOrInputStream(InputStream inputStream) {
        this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public String getContent()
    {
        String line = null;
        String content = "";

        try {
            while ((line = bufferedReader.readLine()) != null) {
                content=content+line+"\n";

            }
            bufferedReader.close();
        }
        catch (IOException e)
        {

            e.printStackTrace();
        }

        return content;
    }


    public List<String> getAllContentList()
    {
        contentList=new ArrayList<>();
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
                contentList.add(line);

            }
            bufferedReader.close();
        }
        catch (IOException e)
        {
            contentList=null;
            e.printStackTrace();
        }

        return contentList;
    }

    public Set<String> getAllContentLinSet()
    {
        contentSet=new LinkedHashSet<>();
        String line = null;

        try {
            while ((line = bufferedReader.readLine()) != null) {
               contentSet.add(line);

            }
            bufferedReader.close();
        }
        catch (IOException e)
        {
            contentSet=null;
            e.printStackTrace();
        }

        return contentSet;
    }
}
