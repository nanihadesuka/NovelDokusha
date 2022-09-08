import os
import re
import shutil

mainDir = os.getcwd()
workDir = os.path.join(mainDir, "app", "build", "outputs", "apk")

extension = ".apk"


def setEnvValue(key, value):
    print(f"Setting env varaible: {key}={value}")
    os.system(f"echo \"{key}={value}\" >> $GITHUB_ENV ")


def getAPKs():
    list = []
    for root, dirs, files in os.walk(workDir):
        for file in files:
            if file.endswith(extension):
                list.append([root, file])
    return list


def processAPK(path, fileName):
    fileNamePath = os.path.join(path, fileName)
    name, version, flavour = re.match(
        "^(.+)_v(\d+\.\d+\.\d+)-(.+)-.*\.apk$", fileName).groups()
    newFileName = f"NovelDokusha_v{version}_{flavour}.apk"
    newFileNamePath = os.path.join(path, newFileName)

    shutil.move(fileNamePath, newFileNamePath)

    print(f"{name=} {version=} {newFileName=}")

    setEnvValue("APP_VERSION", version)
    setEnvValue(f"APK_FILE_PATH_{flavour}", newFileNamePath)


for [path, fileName] in getAPKs():
    processAPK(path, fileName)
