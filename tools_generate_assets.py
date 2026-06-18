from PIL import Image, ImageDraw, ImageFilter
import random, math, os
from pathlib import Path
base=Path('android/assets/textures')
(base/'character').mkdir(parents=True, exist_ok=True)
(base/'environment').mkdir(parents=True, exist_ok=True)

def save(img,path): img.save(path)
def noise_texture(size, base_color, variance=30, seed=0):
    random.seed(seed); img=Image.new('RGB',(size,size)); pix=img.load()
    for y in range(size):
        for x in range(size):
            n=sum(random.randint(-variance,variance) for _ in range(2))//2
            pix[x,y]=tuple(max(0,min(255,c+n)) for c in base_color)
    return img.filter(ImageFilter.GaussianBlur(0.45))
def normal_from_height(h, strength=3.0):
    g=h.convert('L'); w,hgt=g.size; pix=g.load(); out=Image.new('RGB',(w,hgt)); op=out.load()
    for y in range(hgt):
      for x in range(w):
        l=pix[(x-1)%w,y]/255; r=pix[(x+1)%w,y]/255; u=pix[x,(y-1)%hgt]/255; d=pix[x,(y+1)%hgt]/255
        dx=(l-r)*strength; dy=(u-d)*strength; dz=1.0; length=math.sqrt(dx*dx+dy*dy+dz*dz)
        op[x,y]=(int((dx/length*.5+.5)*255), int((dy/length*.5+.5)*255), int((dz/length*.5+.5)*255))
    return out
size=1024
# Grass with clover/leaf strokes
img=noise_texture(size,(62,122,47),35,1).convert('RGB'); d=ImageDraw.Draw(img,'RGBA')
for i in range(7000):
    x=random.randrange(size); y=random.randrange(size); length=random.randrange(3,16); col=random.choice([(84,160,65,95),(35,95,40,90),(130,180,72,60)])
    d.line((x,y,(x+random.randint(-3,3))%size,(y-length)%size),fill=col,width=random.choice([1,1,2]))
for i in range(260):
    x=random.randrange(size); y=random.randrange(size); r=random.randrange(2,5); d.ellipse((x-r,y-r,x+r,y+r),fill=(180,210,120,45))
save(img,base/'environment/grass_albedo.png'); save(normal_from_height(img,1.8),base/'environment/grass_normal.png')
# Stone pavers
img=noise_texture(size,(105,105,100),45,2).convert('RGB'); d=ImageDraw.Draw(img,'RGBA')
cell=128
for y in range(0,size,cell):
  for x in range(0,size,cell):
    off=random.randint(-12,12); poly=[(x+8,y+8),(x+cell-8+off,y+4),(x+cell-10,y+cell-12),(x+10-off,y+cell-8)]
    shade=random.randint(-25,25); fill=tuple(max(0,min(255,110+shade)) for _ in range(3))+(210,)
    d.polygon(poly,fill=fill); d.line(poly+[poly[0]],fill=(35,35,35,160),width=5)
for i in range(2500):
    x=random.randrange(size); y=random.randrange(size); r=random.randrange(1,4); c=random.randrange(70,155); d.ellipse((x-r,y-r,x+r,y+r),fill=(c,c,c,100))
save(img,base/'environment/stone_albedo.png'); save(normal_from_height(img,3.0),base/'environment/stone_normal.png')
# Wood bark/planks
img=noise_texture(size,(118,74,38),35,3).convert('RGB'); d=ImageDraw.Draw(img,'RGBA')
for x in range(0,size,128): d.rectangle((x,0,x+4,size),fill=(55,32,18,180))
for i in range(1000):
    y=random.randrange(size); x=random.randrange(size); d.arc((x-80,y-25,x+80,y+25),0,360,fill=(60,35,20,70),width=random.choice([1,2]))
for i in range(900):
    x=random.randrange(size); y=random.randrange(size); d.line((x,y,(x+random.randint(20,120))%size,y+random.randint(-6,6)),fill=(170,112,58,45),width=2)
save(img,base/'environment/wood_albedo.png'); save(normal_from_height(img,2.6),base/'environment/wood_normal.png')
# Character textures
cloth=noise_texture(size,(39,82,145),25,4).convert('RGB'); d=ImageDraw.Draw(cloth,'RGBA')
for i in range(0,size,32): d.line((0,i,size,i+random.randint(-4,4)),fill=(255,255,255,18),width=1)
for i in range(0,size,32): d.line((i,0,i+random.randint(-4,4),size),fill=(0,0,0,20),width=1)
for i in range(120):
    x=random.randrange(size); y=random.randrange(size); d.rectangle((x,y,x+random.randint(8,30),y+random.randint(2,8)),fill=(90,140,210,55))
save(cloth,base/'character/hero_cloth_albedo.png'); save(normal_from_height(cloth,1.5),base/'character/hero_cloth_normal.png')
skin=noise_texture(size,(196,132,92),18,5).convert('RGB'); d=ImageDraw.Draw(skin,'RGBA')
for i in range(500):
    x=random.randrange(size); y=random.randrange(size); r=random.choice([1,1,2]); d.ellipse((x-r,y-r,x+r,y+r),fill=(120,70,55,35))
save(skin,base/'character/hero_skin_albedo.png')
leather=noise_texture(size,(70,43,28),25,6).convert('RGB'); d=ImageDraw.Draw(leather,'RGBA')
for i in range(1200):
    x=random.randrange(size); y=random.randrange(size); d.line((x,y,x+random.randint(-10,10),y+random.randint(3,25)),fill=(145,98,54,40),width=1)
save(leather,base/'character/hero_leather_albedo.png'); save(normal_from_height(leather,2.2),base/'character/hero_leather_normal.png')
hair=noise_texture(size,(43,27,17),22,7).convert('RGB'); d=ImageDraw.Draw(hair,'RGBA')
for i in range(2200):
    x=random.randrange(size); y=random.randrange(size); d.line((x,y,x+random.randint(-8,8),y+random.randint(18,70)),fill=(120,75,40,45),width=1)
save(hair,base/'character/hero_hair_albedo.png')
# Sky gradient
sky=Image.new('RGB',(1024,512)); p=sky.load()
for y in range(512):
    t=y/511
    c=(int(35+95*t),int(105+90*t),int(190+50*t))
    for x in range(1024): p[x,y]=c
save(sky,base/'environment/sky_gradient.png')
