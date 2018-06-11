package com.popoaichuiniu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReadFile {

    private String fileName=null;

    private BufferedReader bufferedReader=null;

    private List<String>  contentList=null;

    private Set<String > contentSet=null;




    public ReadFile(String fileName) {
        this.fileName = fileName;
        try {
            bufferedReader=new BufferedReader(new FileReader(fileName));
        }

        catch (IOException e)
        {
            e.printStackTrace();
        }

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
