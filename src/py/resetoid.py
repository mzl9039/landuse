import os
import pandas as pd
import csv

minx, miny = 40507003.233,3303246.637
sfile = r'C:\Users\mzl\Desktop\data\Result_200_200_2.csv'
tfile = input('reset file:')
sdf = pd.read_csv(sfile)
tdf = pd.read_csv(tfile)
tdf['X'] = tdf['X'] * 200 + minx
tdf['Y'] = tdf['Y'] * 200 + miny
df = pd.merge(sdf, tdf)
filename = input('filename:')
df.to_csv(filename)