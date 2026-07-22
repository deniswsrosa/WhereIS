import capstone, sys
data=open('/Users/denisrosa/Documents/projects/whereintheworld/work/exe_decompressed/CARMEN.000.exe','rb').read()
LOAD=512
md=capstone.Cs(capstone.CS_ARCH_X86, capstone.CS_MODE_16)
lin=int(sys.argv[1],0); n=int(sys.argv[2]) if len(sys.argv)>2 else 60
code=data[LOAD+lin:LOAD+lin+n*6]
cnt=0
for ins in md.disasm(code, lin):
    print(f"{ins.address:#07x} {ins.bytes.hex():<14} {ins.mnemonic} {ins.op_str}")
    cnt+=1
    if cnt>=n: break
