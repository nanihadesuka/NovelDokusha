import os
import re
import shutil

mainDir = os.getcwd()
workDir = os.path.join(mainDir, "app", "build", "outputs", "apk")

extension = ".apk"


def setEnvValue(key, value):
    print(f"Setting env variable: {key}={value}")
    os.system(f'echo "{key}={value}" >> $GITHUB_ENV ')


def getAPKs():
    apk_list = []
    for root, dirs, files in os.walk(workDir):
        for file in files:
            if file.endswith(extension):
                apk_list.append([root, file])
    return apk_list


def processAPK(path, fileName):
    fileNamePath = os.path.join(path, fileName)
    # The APK filename pattern is:
    #   ParasDokusha_v3.0.1-full-release-unsigned.apk
    #   ParasDokusha_v3.0.1-foss-release-unsigned.apk
    # We need to extract: version, flavour (full or foss)
    #
    # Regex breakdown:
    #   ^.+_v            → prefix + "_v" (don't capture the prefix)
    #   (\d+\.\d+\.\d+)  → version (e.g. 3.0.1)
    #   -(full|foss)     → flavour (only "full" or "foss", not greedy)
    #   -.*              → rest (-release-unsigned)
    #   \.apk$           → extension
    m = re.match(r'^.+_v(\d+\.\d+\.\d+)-(full|foss)-.*\.apk$', fileName)
    if not m:
        print(f"WARNING: APK filename does not match expected pattern: {fileName}")
        print(f"  Expected: <name>_v<X.Y.Z>-<full|foss>-<rest>.apk")
        return

    version = m.group(1)
    flavour = m.group(2)

    newFileName = f"ParasDokusha_v{version}_{flavour}-release.apk"
    newFileNamePath = os.path.join(path, newFileName)

    shutil.move(fileNamePath, newFileNamePath)

    print(f"  version={version}  flavour={flavour}  newFileName={newFileName}")

    setEnvValue("APP_VERSION", version)
    setEnvValue(f"APK_FILE_PATH_{flavour}", newFileNamePath)


for [path, fileName] in getAPKs():
    print(f"Processing: {fileName}")
    processAPK(path, fileName)
