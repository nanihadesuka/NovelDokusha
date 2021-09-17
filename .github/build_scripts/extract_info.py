import os
import shutil

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

print(f"{name=}")
print(f"{version=}")
print(f"{newFileName=}")

shutil.move(os.path.join(workDir,apkFile), os.path.join(workDir,newFileName))

def setEnvValue(key, value):
    print(f"Setting env varaible: {key}={value}")
    os.system(f"echo \"{key}={value}\" >> $GITHUB_ENV ")


setEnvValue("APP_RELEASE_VERSION", version)
setEnvValue("APP_RELEASE_FILE", newFileName)
