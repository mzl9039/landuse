#!/usr/bin/python3
# -*- coding: utf-8 -*-
import json
import numpy as np
import os
import os.path
import matplotlib.pyplot as plt


# 获得目标下所有的antTarget.json文件
def dir_walk(rootdir):
    for root, dirs, files in os.walk(rootdir):
        for dir in dirs:
            if dir.endswith("_time"):
                fls = []
                getTargFiles(os.path.join(root, dir), fls)
                tarstore(os.path.join(root, dir), fls)


def getTargFiles(d, fls):
    for root, dirs, files in os.walk(d):
        fls += [os.path.join(root, file) for file in files if file.endswith("antTarget.json")]
    return fls.sort(key=lambda f: int([s.split('_')[0] for s in f.split('/') if s.endswith("loop")][0].split('_')[0]))


# 存储数据到file
def store(data, file):
    if not os.path.exists(file):
        np.savetxt(file, data, delimiter=',')


# 从file中提取数据
def load(file):
    with open(file) as json_file:
        return json.load(json_file)


# 将data数据按目标类型提取保存
def tarstore(rootdir, files):
    data = [load(f) for f in files]
    tars = list(data[0].keys())
    for tar in tars:
        s = [d[tar] for d in data]
        store(s, os.path.join(rootdir, tar + ".csv"))
        x = list(range(len(s)))
        imgstore(tar, os.path.join(rootdir, tar + ".jpg"), x, s)

def imgstore(tar, file, x, y):
    fig = plt.figure()
    ax = fig.add_subplot(111)
    ax.plot(x, y, 'o-', label=tar)
    ax.legend(loc='upper left')
    plt.savefig(file)
    plt.close()


if __name__ == "__main__":
    rootdir = input()
    dir_walk(rootdir)
