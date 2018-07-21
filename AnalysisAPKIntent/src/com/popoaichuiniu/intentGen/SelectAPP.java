package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.util.Config;
import com.popoaichuiniu.util.ReadFileOrInputStream;
import com.popoaichuiniu.util.WriteFile;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SelectAPP {

    private static String appDir = Config.big_app_set;

    public static void main(String[] args) {
        ReadFileOrInputStream readFileOrInputStream = new ReadFileOrInputStream("AnalysisAPKIntent/unitNeedAnalysisGenerate/dangerousPermission.txt");
        Set<String> dangerousPermissions= readFileOrInputStream.getAllContentLinSet();
        for(Iterator<String> dangerousPermissionsIterator=dangerousPermissions.iterator();((Iterator) dangerousPermissionsIterator).hasNext();)
        {
            String dangerousPermission=dangerousPermissionsIterator.next();
            if(dangerousPermission.startsWith("#"))
            {
                dangerousPermissionsIterator.remove();
            }
        }
        WriteFile writeFile = new WriteFile("AnalysisAPKIntent/unitNeedAnalysisGenerate/dangerousAPP.txt",false);
        File xmlFile = new File("AnalysisAPKIntent/unitNeedAnalysisGenerate/" + new File(appDir).getName() + "_DIR_permissionUse.xml");
        Set<String> apps=new HashSet<>();


        try {
            Document document = new SAXReader().read(xmlFile).getDocument();
            Element rootElement = document.getRootElement();
            for(Element apkElement:rootElement.elements("APK"))
            {
                for(Element permissionElement:apkElement.elements("permission"))
                {
                    String permissionValue=permissionElement.getStringValue();
                    if(dangerousPermissions.contains(permissionValue))
                    {

                        apps.add(apkElement.attributeValue("name"));
                    }
                }
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        for(String app:apps)
        {
            System.out.println(app);
            try {
                FileOutputStream fileOutputStream=new FileOutputStream("selectAPP/"+new File(app).getName());
                FileInputStream fileInputStream=new FileInputStream(new File(app));
                byte [] buffer= new byte[1024*512];
                int length=-1;
                while ((length=fileInputStream.read(buffer))!=-1)
                {
                    fileOutputStream.write(buffer,0,length);


                }
                writeFile.writeStr(app+"\n");

                fileInputStream.close();
                fileOutputStream.close();

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
        writeFile.close();
        System.out.println("over!");


    }
}
