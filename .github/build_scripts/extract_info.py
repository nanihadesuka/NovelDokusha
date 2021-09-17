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


fileName = apkFile[:-len(extension)]
version = fileName.split("_v")[-1]

print(apkFile)
print(fileName)
print(version)

shutil.move(os.path.join(workDir,apkFile), mainDir)

def setEnvValue(key, value):
    print(f"Setting env varaible: {key}={value}")
    os.system(f"echo \"{key}={value}\" >> $GITHUB_ENV ")


setEnvValue("APP_RELEASE_VERSION", version)
setEnvValue("APP_RELEASE_FILE", apkFile)
