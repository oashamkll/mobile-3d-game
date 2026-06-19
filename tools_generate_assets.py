from PIL import Image, ImageDraw, ImageFilter, ImageChops
import random, math, wave, struct, os
from pathlib import Path
base=Path('android/assets')
tex=base/'textures'
for p in [tex/'character', tex/'environment', base/'audio']:
    p.mkdir(parents=True, exist_ok=True)

def fbm(size, colors, seed=0, octaves=5):
    random.seed(seed)
    acc=Image.new('L',(size,size),0)
    amp=1.0; total=0
    for o in range(octaves):
        small=max(4,size//(2**(octaves-o)))
        im=Image.effect_noise((small,small), random.uniform(20,80)).resize((size,size), Image.Resampling.BICUBIC).convert('L')
        acc=ImageChops.add(acc, im.point(lambda v:int(v*amp)))
        total += amp*255; amp*=0.55
    if total:
        acc=acc.point(lambda v: int(max(0,min(255,v*255/total))))
    out=Image.new('RGB',(size,size)); pix=out.load(); np=acc.load()
    for y in range(size):
      for x in range(size):
        t=np[x,y]/255*(len(colors)-1); i=min(len(colors)-2,int(t)); f=t-i
        c=tuple(int(colors[i][k]*(1-f)+colors[i+1][k]*f) for k in range(3))
        pix[x,y]=c
    return out

def normal_from_height(img, strength=3.0):
    g=img.convert('L'); w,h=g.size; pix=g.load(); out=Image.new('RGB',(w,h)); op=out.load()
    for y in range(h):
      for x in range(w):
        l=pix[(x-1)%w,y]/255; r=pix[(x+1)%w,y]/255; u=pix[x,(y-1)%h]/255; d=pix[x,(y+1)%h]/255
        dx=(l-r)*strength; dy=(u-d)*strength; dz=1.0; ln=math.sqrt(dx*dx+dy*dy+dz*dz)
        op[x,y]=(int((dx/ln*.5+.5)*255), int((dy/ln*.5+.5)*255), int((dz/ln*.5+.5)*255))
    return out

def save(img,path): img.save(path,optimize=True)
S=1024
# Premium grass meadow
random.seed(10); img=fbm(S,[(25,70,31),(48,120,45),(82,158,55),(145,185,78)],10,6); d=ImageDraw.Draw(img,'RGBA')
for i in range(14000):
    x=random.randrange(S); y=random.randrange(S); ln=random.randrange(5,22); angle=random.uniform(-0.55,0.55)-math.pi/2
    col=random.choice([(95,190,78,95),(35,110,45,100),(160,205,88,55),(75,150,62,80)])
    d.line((x,y,x+math.cos(angle)*ln,y+math.sin(angle)*ln),fill=col,width=random.choice([1,1,1,2]))
for i in range(900):
    x=random.randrange(S); y=random.randrange(S); r=random.randrange(1,4); col=random.choice([(240,230,120,90),(245,245,245,85),(180,110,220,75)])
    d.ellipse((x-r,y-r,x+r,y+r),fill=col)
save(img,tex/'environment/grass_albedo.png'); save(normal_from_height(img,2.2),tex/'environment/grass_normal.png')
# Stone detailed
random.seed(11); img=fbm(S,[(65,65,66),(115,114,109),(170,168,158)],11,6); d=ImageDraw.Draw(img,'RGBA')
for y in range(0,S,128):
 for x in range(0,S,128):
    jitter=random.randint(-16,16); poly=[(x+8,y+8),(x+124+jitter,y+random.randint(3,14)),(x+120,y+122),(x+random.randint(5,16),y+118)]
    d.line(poly+[poly[0]],fill=(26,27,29,200),width=5)
    d.line([(px+2,py+2) for px,py in poly]+[(poly[0][0]+2,poly[0][1]+2)],fill=(230,225,205,45),width=2)
for i in range(4500):
    x=random.randrange(S); y=random.randrange(S); r=random.randrange(1,5); c=random.randrange(55,210); d.ellipse((x-r,y-r,x+r,y+r),fill=(c,c,c,random.randrange(35,110)))
save(img,tex/'environment/stone_albedo.png'); save(normal_from_height(img,4.0),tex/'environment/stone_normal.png')
# Wood premium
random.seed(12); img=fbm(S,[(58,31,14),(105,62,28),(162,101,46),(88,48,19)],12,5); d=ImageDraw.Draw(img,'RGBA')
for x in range(0,S,128): d.rectangle((x,0,x+5,S),fill=(42,22,11,190)); d.line((x+12,0,x+12,S),fill=(200,140,78,45),width=2)
for i in range(1600):
    x=random.randrange(S); y=random.randrange(S); w=random.randrange(40,180); h=random.randrange(10,45)
    d.arc((x-w//2,y-h//2,x+w//2,y+h//2),0,360,fill=(44,24,11,80),width=random.choice([1,2,3]))
save(img,tex/'environment/wood_albedo.png'); save(normal_from_height(img,3.5),tex/'environment/wood_normal.png')
# Leaves
random.seed(13); img=fbm(S,[(15,74,30),(31,125,46),(72,168,55),(130,190,68)],13,6); d=ImageDraw.Draw(img,'RGBA')
for i in range(4200):
    x=random.randrange(S); y=random.randrange(S); r=random.randrange(3,10); col=random.choice([(25,120,39,80),(120,190,62,70),(10,80,30,70)])
    d.ellipse((x-r,y-r//2,x+r,y+r//2),fill=col)
save(img,tex/'environment/leaves_albedo.png'); save(normal_from_height(img,2.0),tex/'environment/leaves_normal.png')
# Character cloth with seams
random.seed(20); img=fbm(S,[(18,41,92),(37,88,178),(62,134,224),(25,58,130)],20,6); d=ImageDraw.Draw(img,'RGBA')
for x in range(0,S,64): d.line((x,0,x,S),fill=(255,255,255,22),width=1)
for y in range(0,S,64): d.line((0,y,S,y),fill=(0,0,0,28),width=1)
for i in range(100):
    x=random.randrange(S); y=random.randrange(S); d.rounded_rectangle((x,y,x+random.randrange(60,190),y+random.randrange(16,42)),radius=8,outline=(190,220,255,65),width=3)
save(img,tex/'character/hero_cloth_albedo.png'); save(normal_from_height(img,2.1),tex/'character/hero_cloth_normal.png')
# Skin detailed
random.seed(21); img=fbm(S,[(155,92,62),(201,135,92),(229,169,124)],21,5); d=ImageDraw.Draw(img,'RGBA')
for i in range(1300):
    x=random.randrange(S); y=random.randrange(S); r=random.choice([1,1,2]); d.ellipse((x-r,y-r,x+r,y+r),fill=(120,55,45,32))
for i in range(180):
    x=random.randrange(S); y=random.randrange(S); d.line((x,y,x+random.randrange(20,80),y+random.randrange(-8,8)),fill=(255,220,190,35),width=2)
save(img,tex/'character/hero_skin_albedo.png')
# Leather
random.seed(22); img=fbm(S,[(42,25,16),(76,44,26),(128,76,40),(55,32,20)],22,6); d=ImageDraw.Draw(img,'RGBA')
for i in range(2300):
    x=random.randrange(S); y=random.randrange(S); d.line((x,y,x+random.randint(-15,15),y+random.randint(5,40)),fill=(185,120,64,42),width=1)
for i in range(320):
    x=random.randrange(S); y=random.randrange(S); d.ellipse((x-3,y-2,x+3,y+2),fill=(25,12,8,65))
save(img,tex/'character/hero_leather_albedo.png'); save(normal_from_height(img,3.2),tex/'character/hero_leather_normal.png')
# Hair
random.seed(23); img=fbm(S,[(18,10,6),(45,27,16),(92,54,28),(25,15,9)],23,5); d=ImageDraw.Draw(img,'RGBA')
for i in range(5200):
    x=random.randrange(S); y=random.randrange(S); d.line((x,y,x+random.randint(-12,12),y+random.randint(20,90)),fill=(140,82,42,45),width=1)
save(img,tex/'character/hero_hair_albedo.png')
# Metal/eyes
img=fbm(S,[(85,89,96),(150,158,168),(225,230,235)],24,5); save(img,tex/'character/hero_metal_albedo.png'); save(normal_from_height(img,4),tex/'character/hero_metal_normal.png')
# Audio wav generation
def wav(path, samples):
    with wave.open(str(path),'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(44100)
        frames=b''.join(struct.pack('<h', max(-32767,min(32767,int(s*32767)))) for s in samples)
        w.writeframes(frames)

def footstep():
    sr=44100; n=int(sr*0.18); arr=[]
    for i in range(n):
        t=i/sr; env=math.exp(-t*18); noise=random.uniform(-1,1)*env
        tone=math.sin(2*math.pi*95*t)*env*0.35
        arr.append((noise*0.55+tone)*0.55)
    return arr
def chime():
    sr=44100; n=int(sr*0.75); arr=[]
    for i in range(n):
        t=i/sr; env=math.exp(-t*3.2); arr.append((math.sin(2*math.pi*660*t)+0.5*math.sin(2*math.pi*990*t))*env*0.28)
    return arr
def wind():
    sr=44100; n=int(sr*2.0); arr=[]
    last=0
    for i in range(n):
        t=i/sr; last=last*0.96+random.uniform(-1,1)*0.04; env=0.45+0.2*math.sin(2*math.pi*0.37*t)
        arr.append(last*env*0.38)
    return arr
wav(base/'audio/footstep.wav', footstep())
wav(base/'audio/chime.wav', chime())
wav(base/'audio/wind_loop.wav', wind())
