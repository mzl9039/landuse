#!/usr/bin/python3
# -*- coding: utf-8 -*-
import json
import numpy as np
import time
import os
import os.path

# 获得目标下所有的targets.json文件
def dir_walk(rootdir, fls):
    for root, dirs, files in os.walk(rootdir):
        for file in files:
            if (file=="targets.json"):
                fls.append(os.path.join(root, file))


# 存储数据到file
def store(data, file):
    np.savetxt(file, data, delimiter=',')

# 从file中提取数据
def load(file):
    with open(file) as json_file:
        data = json.load(json_file)
        # load进来的data是字符串，要转为json对象，需要再次loads
        return json.loads(data)

def tarlist(data):
    if (len(data)>0 and len(data[0])>0):
        t = data[0][0]
        return list(t.keys())

#将data数据按目标类型提取保存
def tarstore(file):
    data = load(file)
    tars = tarlist(data)
    for i in range(len(tars)):
        first, five = [], []
        for j in range(len(data)):
            t = []
            for k in range(len(data[j])):
                t.append(data[j][k][tars[i]])
                if (k == 0):
                    first.append(data[j][k][tars[i]])
            five.append(t)

        filedir = os.path.dirname(file)
        store(first, os.path.join(filedir, tars[i] + "1.csv"))
        store(five, os.path.join(filedir, tars[i] + "5.csv"))

if __name__ == "__main__":

    files = []
    rootdir = input()
    dir_walk(rootdir, files)
    for f in files:
        tarstore(f)