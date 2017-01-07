#!/usr/bin/python3
# -*- coding: utf-8 -*-
import json
import time
import os
import os.path
import csv

# 获得目标下所有的targets.json文件
def dir_walk(rootdir):
    fls = []
    for root, dirs, files in os.walk(rootdir):
        for file in files:
            if (file=="tours.json"):
                fls.append(os.path.join(root, file))
                return fls
        for dir in dirs:
            fls = fls + dir_walk(os.path.join(rootdir, dir))


# 存储数据到file
def store(data, file):
    with open(file, 'w', newline='') as datacsv:
    	csvwriter = csv.writer(datacsv, dialect=('excel'))
    	csvwriter.writerows(data)

# 从file中提取数据
def load(file):
    with open(file) as json_file:
        data = json.load(json_file)
        # load进来的data是字符串，要转为json对象，需要再次loads
        return json.loads(data)

def trangrid(data):
	dt = []
	for i in range(len(data)):
		for j in range(len(data[i])):
			if(data[i][j] != None):
				d = []
				t = data[i][j]
				d.append(t['objectid'])
				d.append(t['dlbm8'])
				dt.append(d)

	return dt


if __name__ == "__main__":

    rootdir = input()
    files = dir_walk(rootdir)
    for f in files:
        data = load(f)
        data = trangrid(data)
        print(len(data))
        filedir = os.path.dirname(f)
        store(data, os.path.join(filedir, "grids.csv"))
