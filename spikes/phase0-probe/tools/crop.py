"""crop.py in.png out.png x0 y0 w h zoom  -- dependency-free PNG crop + nearest-neighbour zoom."""
import sys, zlib, struct
def read_png(p):
    data=open(p,'rb').read(); pos=8; idat=b''
    while pos<len(data):
        ln=struct.unpack('>I',data[pos:pos+4])[0]; typ=data[pos+4:pos+8]; body=data[pos+8:pos+8+ln]; pos+=12+ln
        if typ==b'IHDR': w,h,bd,ct=struct.unpack('>IIBB',body[:10])
        if typ==b'IDAT': idat+=body
    raw=zlib.decompress(idat); bpp=4 if ct==6 else 3; stride=w*bpp; rows=[]; prev=bytearray(stride); i=0
    for y in range(h):
        f=raw[i]; i+=1; line=bytearray(raw[i:i+stride]); i+=stride
        if f:
            for x in range(stride):
                a=line[x-bpp] if x>=bpp else 0; b=prev[x]; c=prev[x-bpp] if x>=bpp else 0
                if f==1: line[x]=(line[x]+a)&255
                elif f==2: line[x]=(line[x]+b)&255
                elif f==3: line[x]=(line[x]+((a+b)>>1))&255
                elif f==4:
                    pa=abs(b-c); pb=abs(a-c); pc=abs(a+b-2*c); pr=a if pa<=pb and pa<=pc else (b if pb<=pc else c); line[x]=(line[x]+pr)&255
        rows.append(bytes(line)); prev=line
    return w,h,bpp,rows
def write_png(p,w,h,bpp,rows):
    raw=b''.join(b'\x00'+r for r in rows)
    def chunk(t,b): return struct.pack('>I',len(b))+t+b+struct.pack('>I',zlib.crc32(t+b)&0xffffffff)
    open(p,'wb').write(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6 if bpp==4 else 2,0,0,0))+chunk(b'IDAT',zlib.compress(raw,6))+chunk(b'IEND',b''))
src,dst,x0,y0,cw,ch,z=sys.argv[1],sys.argv[2],*map(int,sys.argv[3:8])
w,h,bpp,rows=read_png(src); out=[]
for y in range(ch):
    r=rows[y0+y]; line=b''.join(r[(x0+x)*bpp:(x0+x+1)*bpp]*z for x in range(cw)); out.extend([line]*z)
write_png(dst,cw*z,ch*z,bpp,out); print(f"{w}x{h} -> crop {cw}x{ch}@{x0},{y0} x{z}")
