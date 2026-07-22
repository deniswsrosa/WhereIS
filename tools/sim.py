d=open('/Users/denisrosa/Documents/projects/whereintheworld/work/extracted/wwcse/CITIES.DAT','rb').read()
# Athens compressed image starts at 0x4d
SRC_START=0x4d
src=d
def decompress(src, si, out_count):
    N=1024
    ring=bytearray(N)          # filled 0
    bx=N-0x42                  # 958 initial write pos
    F=0                        # [bp-4], 16-bit flag state
    out=bytearray()
    cx=out_count
    def rd():
        nonlocal si
        b=src[si]; si+=1; return b
    while cx>0:
        # getbit
        ax=F
        ax=(ax>>1)&0xffff      # shift#1
        if (ax>>8)&0xff==0:    # test ah
            al=rd()
            ax=(ax&0xff00)|al  # al=new byte
            ax=ax|0xff00       # not ah (ah was 0 -> 0xff)
        F=ax
        ax=(ax>>1)&0xffff      # shift#2
        cf=(F>>0)&1            # CF from shift#2 = bit0 of F(before shift2)
        # Actually CF of (F>>1) = bit0 of F
        if cf==0:
            # MATCH
            B1=rd(); B2=rd()
            val=((B1<<8)|B2)&0xffff
            off=val&0x3ff
            length=(B1>>2)+3
            rpos=off
            for _ in range(length):
                al=ring[rpos]
                rpos+=1
                if rpos>=N: rpos=0
                out.append(al)
                ring[bx]=al
                bx+=1
                if bx>=N: bx=0
                cx-=1
                if cx==0: break
        else:
            # LITERAL
            al=rd()
            out.append(al)
            ring[bx]=al
            bx+=1
            if bx>=N: bx=0
            cx-=1
    return out,si

for oc in [10080,20160]:
    out,si=decompress(src,SRC_START,oc)
    hi=[b for b in out if b>15]
    print(f'out_count={oc} produced={len(out)} src_used={si-SRC_START} bytes_over_15={len(hi)} first16={out[:16].hex()}')

print("--- sweep to find natural out_count where src ~5048 (Athens img len) ---")
for oc in range(18000,21200,140):
    out,si=decompress(src,SRC_START,oc)
    used=si-SRC_START
    if 5040<=used<=5060:
        print('out_count',oc,'src_used',used)
