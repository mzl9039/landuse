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
            if (file=="tours.json"):
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

if __name__ == "__main__":
    files = []
    rootdir = input()
    dir_walk(rootdir, files)
    for f in files:
        data = load(f)
        filedir = os.path.dirname(f)
        store(data, os.path.join(filedir, "grids.csv"))
