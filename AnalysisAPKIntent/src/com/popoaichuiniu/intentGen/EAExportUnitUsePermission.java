package com.popoaichuiniu.intentGen;

import com.popoaichuiniu.jacy.AndroidInfoHelper;
import com.popoaichuiniu.util.Config;
import com.popoaichuiniu.util.ReadFileOrInputStream;
import com.popoaichuiniu.util.Util;
import com.popoaichuiniu.util.WriteFile;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EAExportUnitUsePermission {

    private static String appDir = Config.wandoijiaAPP;


    public static void main(String[] args) {

        Map<String, Set<String>> sootMethodPermissionMap = AndroidInfoHelper.getPermissionAndroguardMethods();

        Map<String, Set<String>> permissionAPIMap = Util.getPermissionAPIMap(sootMethodPermissionMap);
        Set<String> allPermissionsSet = new HashSet<>();

        File xmlFile = new File("AnalysisAPKIntent/unitNeedAnalysisGenerate/" + new File(appDir).getName() + "_DIR_permissionUse.xml");

        Document document = null;
        Element rootElement = null;
        if (xmlFile.exists()) {
            try {
                document = new SAXReader().read(xmlFile).getDocument();
                rootElement = document.getRootElement();
            } catch (DocumentException e) {
                e.printStackTrace();
            }

        } else {
            document = DocumentHelper.createDocument();

            rootElement = document.addElement("APKs");

        }


        for (File file : new File(appDir).listFiles()) {
            if (file.getName().endsWith(".txt"))//"1元乐购.apk_UnitsNeedAnalysis.txt"
            {
                ReadFileOrInputStream readFileOrInputStream = new ReadFileOrInputStream(file.getAbsolutePath());
                String apkName = appDir+"/"+file.getName().substring(0, file.getName().length() - 22);
                System.out.println(apkName);
                Set<String> appPermissionSet = new HashSet<>();
                for (String str : readFileOrInputStream.getAllContentLinSet()) {
                    String[] contentArray = str.split("#");
                    String sootMethodSignature = contentArray[3];
                    for (String permission : sootMethodPermissionMap.get(sootMethodSignature)) {

                        appPermissionSet.add(permission);
                        allPermissionsSet.add(permission);
                    }


                }

                addElementNode(document, rootElement, apkName, appPermissionSet);


            }
        }


        try {

            XMLWriter xmlWriter = new XMLWriter(new FileWriter("AnalysisAPKIntent/unitNeedAnalysisGenerate/" + new File(appDir).getName() + "_DIR_permissionUse.xml"));
            xmlWriter.write(document);
            xmlWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


        WriteFile writeFileAllPermission = new WriteFile("AnalysisAPKIntent/unitNeedAnalysisGenerate/" + new File(appDir).getName() + "_DIR_AllPermission.txt", false);
        for (String permissionString : allPermissionsSet) {

            writeFileAllPermission.writeStr(permissionString + "\n");//all permission


        }
        writeFileAllPermission.writeStr("\n\n\n");


        for (String permissionString : allPermissionsSet) {

            writeFileAllPermission.writeStr(permissionString + ":" + "\n");//permission
            for (String api : permissionAPIMap.get(permissionString)) {
                writeFileAllPermission.writeStr(api + "\n");//api
            }
            writeFileAllPermission.writeStr("\n");
        }
        writeFileAllPermission.close();
    }

    private static void addElementNode(Document document, Element rootElement, String apkName, Set<String> appPermissionSet) {


        Element apkElement = rootElement.addElement("APK");
        apkElement.addAttribute("name", apkName);

        for (String permission : appPermissionSet) {
            Element permissionElement = apkElement.addElement("permission");

            permissionElement.addText(permission);
        }


    }
}
