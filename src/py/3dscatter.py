import os
import numpy as np
import pandas as pd
from matplotlib import pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

fig = plt.figure(figsize=(15,8))
ax=plt.subplot(221,projection='3d')
ax2 = plt.subplot(222)
ax3 = plt.subplot(223)
ax4 = plt.subplot(224)

x, y, z= [], [], []
dir = r'C:\Users\mzl\workspace\landuse\opti\output'
dirs = os.listdir(dir)
for d in dirs:
	d2 = os.path.join(dir, d, 'pareto_tgs.csv')
	df = pd.read_csv(d2)
	x.extend(df['MPC'])
	y.extend(df['LU'])
	z.extend(df['LSE'])

print(len(x))
xmax = max(x)
ymax = max(y)
zmax = max(z)
xmin = min(x)
ymin = min(y)
zmin = min(z)

x = [(v-xmin)/(xmax-xmin) for v in x]
y = [(v-ymin)/(ymax-ymin) for v in y]
z = [(v-zmin)/(zmax-zmin) for v in z]

ax.scatter(x, y, z)
ax.set_xlabel('mpc') #坐标轴
ax.set_ylabel('lu')
ax.set_zlabel('lse')
ax2.scatter(x, y)
ax2.set_xlabel('mpc') #坐标轴
ax2.set_ylabel('lu')
ax3.scatter(x, z)
ax3.set_xlabel('mpc')
ax3.set_ylabel('lse')
ax4.scatter(y, z)
ax4.set_xlabel('lu')
ax4.set_ylabel('lse')
plt.show()