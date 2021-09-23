import os
import shutil
import re

mainDir = os.getcwd()
workDir = os.path.join(mainDir, "github_release")

extension = ".apk"
apkFile = None
for file in os.listdir(workDir):
    if os.path.isfile(os.path.join(workDir, file)):
        if file.endswith(extension):
            apkFile = file

name, version = re.match("^(.+)_v(\d+\.\d+\.\d+).*\.apk$",apkFile).groups() 

newFileName = f"NovelDokusha_v{version}.apk"

currentPath = os.path.join(workDir,apkFile)
destinationPath = os.path.join(mainDir,newFileName)

print(f"{name=}")
print(f"{version=}")
print(f"{newFileName=}")

print("Moving apk")
print(f"{currentPath=}")
print(f"{destinationPath=}")

shutil.move(currentPath, destinationPath)

def setEnvValue(key, value):
    print(f"Setting env varaible: {key}={value}")
    os.system(f"echo \"{key}={value}\" >> $GITHUB_ENV ")


setEnvValue("APP_RELEASE_VERSION", version)
setEnvValue("APP_RELEASE_FILE", newFileName)
