import  subprocess
import time
import os
import threading
def execuateCmd(cmd):
    status,output=subprocess.getstatusoutput(cmd);
    return status,output


def isADBWorkNormal():#ok
    adb_status="adb devices"
    status, output = execuateCmd(adb_status)
    print("*"+output+"*")
    if (status == 0):
        index1=output.find("emulator")
        index2= output.find("offline")
        if(index1!=-1 and index2==-1):
            return True
        else:
            return False
    else:
        return False


def isTestAPPAlive():#ok
    if(not isADBWorkNormal()):
        print ("adb work abnormal!")
        return False;
    else:
        app_status = "adb shell ps |grep jacy.popoaichuiniu.com.testpermissionleakge"
        status, output = execuateCmd(app_status)
        if(output.find("jacy.popoaichuiniu.com.testpermissionleakge")!=-1):
            print(output)
            return True
        else:
            return False
# def isGuakeAlive():#guake has not started completely ,but this method return true
#         app_status = "ps -a |grep guake"
#         status, output = execuateCmd(app_status)
#         index=-1
#         count=0
#         isFirstIn=True
#
#         while (isFirstIn or index!=-1):
#             isFirstIn=False
#             index=output.find("guake",index+1)
#             if(index!=-1):
#                 count=count+1
#
#         if count ==3:
#             print("guake ok")
#             return True
#         else:
#             return False

def installNewAPP(appPath):#ok
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False;
    else:
        install_app="adb install -r"+" "+appPath
        status,output=execuateCmd(install_app)
        if(status==0):
            index=output.find("Failure")
            if(index!=-1):
                info=appPath+"安装失败"+"eeeeeeeeeeee"
                print(info)
                return False,info
            else:
                info=appPath + "安装成功"
                print(info)
                return True,info
        else:
            info="安装失败"+"error:"+str(status)+","+output++"eeeeeeeeeeee"
            print(info)
            return False,info
def getPackageName(appPath):#
    get_package_cmd="aapt dump badging "+appPath
    status, output = execuateCmd(get_package_cmd)
    if (status==0):
        #print("*"+output+"*")
        tempStr=output.split("\n")[0]
        #print(tempStr)
        index1=tempStr.find("'")
        index2=tempStr.find("'",index1+1)
        packageName=tempStr[index1+1:index2]

        return packageName
    else:
        print("error:"+str(status)+","+output)
        return None

def uninstall_app_by_packageName(packageName):
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False
    else:
        install_app = "adb uninstall " + " " + packageName
        status, output = execuateCmd(install_app)
        if (status == 0):  # Success
            index = output.find("Success")
            if (index != -1):
                print(packageName + "卸载成功\n")
                return True
            else:
                print(packageName + "卸载失败\n")
                return False

        else:
            print("error:" + str(status) + "," + output)
            return False


def uninstall_app_by_path(appPath):#ok
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False
    else:
        killTestAPP()#虽然testapp已经进程没了但是还是要杀一下，不知道为啥
        packageName=getPackageName(appPath)
        if(packageName==None):
            return False
        return uninstall_app_by_packageName(packageName)
def pushTestFile(appPath_testFile):#ok
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False
    else:
        push_testFile="adb push " + appPath_testFile + " " + "/data/data/jacy.popoaichuiniu.com.testpermissionleakge/files/intentInfo.txt"
        status, output=execuateCmd(push_testFile)
        if (status == 0):
            info=appPath_testFile + "推送测试文件成功"
            print(info)
            return True,info
        else:
            info="推送测试文件失败"+"error:" + status + "," + output++"eeeeeeeeeeee"
            print(info)
            return False,info
def startTestAPP():#ok
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False
    start_app_cmd="adb shell am start -n jacy.popoaichuiniu.com.testpermissionleakge/jacy.popoaichuiniu.com.testpermissionleakge.MainActivity"
    status,output=execuateCmd(start_app_cmd)
    if(status==0):
        index=output.find("Error")
        if(index!=-1):
            print(output)
            return False,"启动APP失败"+output+"eeeeeeeeeeee"
        else:
            return True,"启动APP成功"+output

    else:
        print("启动测试APP失败")
        return False,"启动APP失败"+output+"eeeeeeeeeeee"
def killTestAPP():#ok
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False;
    kill_app_cmd="adb shell am force-stop jacy.popoaichuiniu.com.testpermissionleakge"
    status, output = execuateCmd(kill_app_cmd)
    if (status == 0):
        return True
    else:
        print("杀死app失败,"+output)
        return False

def  analysisAPKDir(apkDir):#

    if(not os.path.isdir(apkDir)):
        if(apkDir.endswith("_signed_zipalign.apk")):
            parent_path = os.path.dirname(apkDir)
            apk_name=os.path.basename(apkDir)
            intent_file=parent_path+"/"+apk_name.replace("_signed_zipalign","")+"_"+"intentInfo.txt"
            if (os.path.exists(intent_file)):
                yield apkDir, intent_file
            else:
                print("没有找到指定的intent测试文件")


    else:
        failure_log = open("apk_file_no_test_info.txt", "a+")
        for file in os.listdir(apkDir):
            #print(type(file))
            path=apkDir+"/"+file
            if(str(path).endswith("_signed_zipalign.apk")):
                #intent_file=path+"_"+"intent_info.txt"#-------------------------------
                intent_file=apkDir+"/../"+file.replace("_signed_zipalign","")+"_"+"intentInfo.txt"#---------------------------
                if(os.path.exists(intent_file)):
                    yield path,intent_file
                else:
                    print(path+"没有找到指定的intent测试文件")
                    failure_log.write(path+"\n")
                    failure_log.flush()

        failure_log.close()



def waitForTestStop():
    count = 0
    flagADB=isADBWorkNormal()
    flagTestAPPLive=isTestAPPAlive()
    while ((not flagADB) or flagTestAPPLive):
        if (not flagADB):
            print("adb工作不正常")
        else:
            print("等待当前APP测试结束！"+str(count))
            count = count + 1
            if (count > 10):
                killTestAPP()
        time.sleep(5)
        flagADB = isADBWorkNormal()
        flagTestAPPLive = isTestAPPAlive()

def rebootPhone():
    status, output = execuateCmd("killall guake")
    if(status==0):
        print("killall guake ok!")
        cmd = "adb shell reboot -p"
        status, output = execuateCmd(cmd)
        if (status == 0):
            print("emulator has closed")
            initialLogger()
            return True
        else:
            print("关闭手机失败," + output)
            return False
    else:
        print("killall guake fail!")
        return False



intent_test_count=0
def test(apkPath,intent_file):#
    global  intent_test_count
    flag_test=False;
    app_test_status=open("app_test_status",'a+')
    print(apkPath + "11111111111111111111111111111111111" + "\n")
    app_test_status.write(apkPath+"11111111111111111111111111111111111"+"\n")
    waitForTestStop()
    print("开始测试新app")
    intent=open(intent_file,"r")
    lines_intent=intent.readlines()
    intent.close()
    if(len(lines_intent)>100):
        too_many_test_app=open("too_many_test_app",'a+')
        too_many_test_app.write(apkPath+"\n")
        too_many_test_app.close()
    index=0
    for line in lines_intent:
        if(index >100):
            continue
        print("ssssssssssssssssss"+line)
        oneIntentFile=open("temp_intent","w")
        oneIntentFile.write(line)
        oneIntentFile.close()
        flag_install = installNewAPP("/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestPermissionleakge/app/build/outputs/apk/debug/app-debug.apk")
        if (not flag_install):
            print("install error")
            raise RuntimeError
        flag_install, install_info = installNewAPP(apkPath)
        if (flag_install):
            flag_pushTestFile,push_info = pushTestFile("./temp_intent")
            if (flag_pushTestFile):
                flag_test_APP,test_info=startTestAPP()
                if (flag_test_APP):
                    flag_test=True
                    print(str(index)+":oneIntent启动成功!"+"\n")
                    app_test_status.write(str(index)+":oneIntent启动成功!"+"\n")

                else:
                    print(str(index)+":启动TestPermissionAPP失败")
                    app_test_status.write(str(index)+test_info+"\n")

            else:
                print(str(index) + ":推送测试文件失败")
                app_test_status.write(str(index)+push_info+"\n")


        else:
            print(str(index) + ":待测apk安装失败")
            app_test_status.write(str(index)+install_info+"\n")

        waitForTestStop()
        uninstall_app_by_path(apkPath)
        uninstall_app_by_packageName("jacy.popoaichuiniu.com.testpermissionleakge")
        index=index+1
        intent_test_count=intent_test_count+1
        if(intent_test_count%20==0):
            if(rebootPhone()):
                while(not isADBWorkNormal()):
                    print("等待手机启动！")
                    time.sleep(1)
            else:
                raise RuntimeError

    print(apkPath + "222222222222222222222222222222222222222" + "\n")
    app_test_status.write(apkPath + "222222222222222222222222222222222222222" + "\n")
    app_test_status.close()

    return flag_test




class MyThread (threading.Thread):
    def __init__(self, cmd):
        threading.Thread.__init__(self)

        self.cmd=cmd


    def run(self):
       print(self.cmd+"start...")
       status,output=execuateCmd(self.cmd)#wait
       print(self.cmd+"over")









def initialLogger():
    log1='guake -e  "adb logcat | grep ZMSInstrument | tee -a /home/zms/logger_file/testlog/ZMSInstrument.log "'
    log2='guake -e  "adb logcat | grep ZMSStart | tee -a /home/zms/logger_file/testlog/ZMSStart.log"'
    log3='guake -e  "adb logcat *:E|tee -a /home/zms/logger_file/testlog/error.log"'

    #创建3个线程


    threadStart = MyThread("/home/lab418/Android/Sdk/emulator/emulator -avd Nexus_5X_API_19 -wipe-data")
    threadStart.start()
    while(not isADBWorkNormal()):#adb devices work  normal and install app inmmediately and install successfully and app is not installed
        print("等待adb...")
        time.sleep(1)
    # flag_install = installNewAPP("/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestPermissionleakge/app/build/outputs/apk/debug/app-debug.apk")
    # if (not flag_install):
    #     print("install error")
    thread1=MyThread(log1)
    thread2=MyThread(log2)
    thread3=MyThread(log3)
    thread1.start()
    thread2.start()
    thread3.start()
    time.sleep(10)
def getFileContent(path):
    str_file=open(path,'r')
    return str_file.readlines();





if __name__ == '__main__':

    #print(isADBWorkNormal())
    #print(isTestAPPAlive())
    #print(installNewAPP("/home/lab418/PycharmProjects/testAPP/sms2.apk"))
    #print(getPackageName("/home/lab418/PycharmProjects/testAPP/sms2.apk"))
    #print(uninstall_app("/home/lab418/PycharmProjects/testAPP/sms2.apk"))
    #print(pushTestFile("/home/lab418/PycharmProjects/testAPP/intentInfo1.txt"))
    #print(startTestAPP())
    #print(killTestAPP())

    # for apk,intent_file in analysisAPKDir("."):
    #     print(apk+","+intent_file)

    #test("sms2.apk","intentInfo1.txt")

    # initialLogger()
    # rebootPhone()
    # initialLogger()
    # startTestAPP()


    #killTestAPP()
    #print(pushTestFile("/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestWebView2/app/build/outputs/apk/debug/instrumented/app-debug_signed_zipalign.ap"))
    #print(uninstall_app("/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestWebView2/app/build/outputs/apk/debug/instrumented/app-debug_signed_zipalign.apk"))

    #test************************************************************before
    initialLogger()
    fail_apk_list=open("failTest_apk_list","w")
    success_apk_list=open("successTest_apk_list","w")
    has_process = getFileContent("has_process_app_list")
    has_process_app_list = open("has_process_app_list", "a+")
    while(not isADBWorkNormal()):
        print("等待adb工作正常")
        time.sleep(1)
    apkDir="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/selectAPP/instrumented"
    #apkDir='/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestWebView2/app/build/outputs/apk/debug/instrumented'

    for apkPath,intent_file in analysisAPKDir(apkDir):
        if apkPath in has_process:
            continue
        has_process_app_list.write(apkPath + "\n")
        has_process_app_list.flush()
        flag_test=test(apkPath,intent_file)
        if(flag_test):
            success_apk_list.write(apkPath+"\n")
            success_apk_list.flush()
        else:
            print(apkPath+"测试失败！")
            fail_apk_list.write(apkPath+"\n")
            fail_apk_list.flush()
    success_apk_list.close()
    fail_apk_list.close()
    has_process_app_list.close()
print("over")

