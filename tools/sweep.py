import numpy as np, pickle, itertools
O=pickle.load(open('/private/tmp/claude-504/-Users-denisrosa-Documents-projects-whereintheworld/5d98da09-c497-495c-a32e-6ee7f3e6c357/scratchpad/oracles.pkl','rb'))
sm=O['San Marino']; comp=sm['comp']; idx=sm['idx']; H,W=sm['H'],sm['W']

def lzss(src, off_bits, len_bits, threshold, lit_bit, msb_first, lohi, fill, out_limit=25000):
    N=1<<off_bits; F=(1<<len_bits)+threshold-1
    out=bytearray(); ring=bytearray([fill]*N); r=N-F; i=0; L=len(src)
    off_mask=N-1; len_mask=(1<<len_bits)-1
    while i<L and len(out)<out_limit:
        flags=src[i]; i+=1
        for b in range(8):
            if i>=L: break
            bit=(flags>>(7-b))&1 if msb_first else (flags>>b)&1
            islit=(bit==lit_bit)
            if islit:
                c=src[i]; i+=1; out.append(c); ring[r]=c; r=(r+1)&(N-1)
            else:
                if i+1>=L: return bytes(out),i
                b0=src[i]; b1=src[i+1]; i+=2
                v=(b0|(b1<<8)) if lohi else (b1|(b0<<8))
                if off_bits+len_bits<=16:
                    # low off_bits = offset, high len_bits = length  (one common layout)
                    pos=v & off_mask
                    length=((v>>off_bits)&len_mask)+threshold
                else:
                    pos=0;length=0
                for k in range(length):
                    c=ring[(pos+k)&(N-1)]; out.append(c); ring[r]=c; r=(r+1)&(N-1)
    return bytes(out),i

def score(out):
    best=0;bestdesc=None
    n=len(out)
    for mode in ('1bpp','4bpp_hi','4bpp_lo'):
        if mode=='1bpp':
            px=np.frombuffer(out,dtype=np.uint8).astype(np.int16)
        elif mode=='4bpp_hi':
            a=np.frombuffer(out,dtype=np.uint8)
            px=np.empty(len(a)*2,dtype=np.int16); px[0::2]=a>>4; px[1::2]=a&0xf
        else:
            a=np.frombuffer(out,dtype=np.uint8)
            px=np.empty(len(a)*2,dtype=np.int16); px[0::2]=a&0xf; px[1::2]=a>>4
        if len(px)<W*H: continue
        for orient in ('td','bu'):
            im=px[:W*H].reshape(H,W)
            if orient=='bu': im=im[::-1]
            m=(im==idx).mean()
            if m>best: best=m;bestdesc=(mode,orient)
    return best,bestdesc

results=[]
for off_bits in (10,11,12,13):
    for len_bits in (3,4,5,6):
        for threshold in (1,2,3):
            for lit_bit in (0,1):
                for msb in (False,True):
                    for lohi in (True,False):
                        for fill in (0,0x20):
                            out,ci=lzss(comp,off_bits,len_bits,threshold,lit_bit,msb,lohi,fill)
                            if abs(len(out)-20160)<600 or abs(len(out)-10080)<400:
                                s,d=score(out)
                                results.append((s,len(out),ci,off_bits,len_bits,threshold,lit_bit,msb,lohi,fill,d))
results.sort(reverse=True)
print("top candidates (score, outlen, consumed, offb,lenb,thr,litbit,msb,lohi,fill,mode):")
for r in results[:15]:
    print(f"  score={r[0]:.3f} out={r[1]} used={r[2]}/{len(comp)} ob={r[3]} lb={r[4]} thr={r[5]} lit={r[6]} msb={r[7]} lohi={r[8]} fill={r[9]} {r[10]}")
print("baseline random match ~",1/15)
