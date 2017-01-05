import os
import pandas as pd

dir = input('dir:')
lse = float(input('lse:'))
for root, dirs, files in os.walk(dir):
	for f in files:
		if (f.endswith('_tg.csv')):
			df = pd.read_csv(os.path.join(root, f))
			l = float(df['LSE'])
			if (lse - l < 0.1):
				print(f)
