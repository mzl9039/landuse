from PIL import Image
import os
import glob

dir = r'C:\Users\mzl\workspace\landuse\opti\output\20161214_120521'
files = glob.glob(os.path.join(dir, '*.jpg'))

for image in files:
    img = Image.open(image)
    im = img.rotate(90)
    im.save(image)

