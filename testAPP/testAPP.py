import  subprocess
import time
import os
import threading
flag_work_well=True
def execuateCmd(cmd):
    status,output=subprocess.getstatusoutput(cmd);
    return status,output


def isADBWorkNormal():#
    adb_status="adb shell ps"
    status, output = execuateCmd(adb_status)
    if (status == 0):
        flag_work_well=True;
        return True
    else:
        flag_work_well = False;
        return False


def isTestAPPAlive():#
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

def installNewAPP(appPath):#
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False;
    else:
        install_app="adb install -r"+" "+appPath
        status,output=execuateCmd(install_app)
        if(status==0):
            print(appPath+"安装成功")
            return True
        else:
            print("error:"+str(status)+","+output)
            return False
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

def uninstall_app(appPath):#
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False;
    else:
        killTestAPP()#虽然testapp已经进程没了但是还是要杀一下，不知道为啥
        packageName=getPackageName(appPath)
        if(packageName==None):
            return False
        install_app="adb uninstall "+" "+packageName
        status,output=execuateCmd(install_app)
        if(status==0):
            print(appPath+"卸载成功\n")
            return True
        else:
            print("error:"+str(status)+","+output)
            return False
def pushTestFile(appPath_testFile):#
    if (not isADBWorkNormal()):
        print("adb work abnormal!")
        return False;
    else:
        push_testFile="adb push " + appPath_testFile + " " + "/data/data/jacy.popoaichuiniu.com.testpermissionleakge/files/intentInfo.txt"
        status, output=execuateCmd(push_testFile)
        if (status == 0):
            print(appPath_testFile + "推送测试文件成功")
            return True
        else:
            print("error:" + status + "," + output)
            return False
def startTestAPP():#
    start_app_cmd="adb shell am start -n jacy.popoaichuiniu.com.testpermissionleakge/jacy.popoaichuiniu.com.testpermissionleakge.MainActivity"
    status,output=execuateCmd(start_app_cmd)
    if(status==0):
        return True
    else:
        print("启动测试APP失败")
        return False
def killTestAPP():#
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
        failure_log = open("failure_apk_file", "a+")
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

        failure_log.close()



def waitForTestStop():
    count = 0

    while ((not flag_work_well) or isTestAPPAlive()):
        if (not flag_work_well):
            print("adb工作不正常")
        else:
            print("等待当前APP测试结束！"+str(count))
            count = count + 1
            if (count > 10):
                killTestAPP()
        time.sleep(5)


def test(apkPath,intent_file):#
    waitForTestStop()
    print("开始测试新app")
    flag_install = installNewAPP(apkPath)
    if (flag_install):
        flag_pushTestFile=pushTestFile(intent_file)
        if(flag_pushTestFile):
            if(startTestAPP()):
                return True

            else:
                print(apkPath+"启动TestPermissionAPP失败")
                return False

        else:
            print(apkPath+"推送测试文件失败")
            return False
    else:
        print(apkPath+"待测apk安装失败")
        return False

class MyThread (threading.Thread):
    isRun=True
    def __init__(self, cmd):
        threading.Thread.__init__(self)

        self.cmd=cmd
        self.isRun = True

    def run(self):
       status,output=execuateCmd(self.cmd)
       print(output)
       while(status == 0 and MyThread.isRun):
           pass



def initialLogger():
    log1='guake -e  "adb logcat | grep ZMSInstrument | tee -a /home/zms/logger_file/testlog/ZMSInstrument.log"'
    log2='guake -e  "adb logcat | grep ZMSStart | tee -a /home/zms/logger_file/testlog/ZMSStart.log"'
    log3='guake -e  "adb logcat *:E|tee -a /home/zms/logger_file/testlog/error.log"'

    #创建3个线程

    #execuateCmd('guake -e "adb logcat | grep ZMSInstrument | tee ZMSInstrument.log"')

    #log1="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/testAPP/testLog/startLogger.sh"
    thread1=MyThread(log1)
    thread2=MyThread(log2)
    thread3=MyThread(log3)
    thread1.start()
    thread2.start()
    thread3.start()
    time.sleep(5)





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

    initialLogger()
    fail_apk_list=open("fail_apk_list","w")
    success_apk_list=open("success_apk_list","w")
    while(not isADBWorkNormal()):
        print("等待adb工作正常")
        time.sleep(3)

    #apkDir="/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/sootOutput"
    apkDir='/media/lab418/4579cb84-2b61-4be5-a222-bdee682af51b/myExperiment/idea_ApkIntentAnalysis/android_project/Camera/TestWebView2/app/build/outputs/apk/debug/instrumented'
    for apkPath,intent_file in analysisAPKDir(apkDir):
        flag_test=test(apkPath,intent_file)
        if(flag_test):
            waitForTestStop()
            success_apk_list.write(apkPath+"\n")
        else:
            print(apkPath+"测试失败！")
            fail_apk_list.write(apkPath+"\n")

        #uninstall_app(apkPath)
    success_apk_list.close()
    fail_apk_list.close()
MyThread.isRun=False#doesn't work  thread is died but subProcess to execuate command other run
print("over")


