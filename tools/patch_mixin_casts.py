import os,re
from pathlib import Path

root = Path(__file__).resolve().parents[1] / 'src' / 'main' / 'java' / 'meteordevelopment' / 'meteorclient' / 'mixin'
for dp,_,fs in os.walk(root):
    for fn in fs:
        if not fn.endswith('.java'): continue
        p=os.path.join(dp,fn)
        s=open(p,encoding='utf-8').read()
        s=re.sub(r'\(\(([A-Za-z_][\w.$]*)\)this\)', r'((\1)(Object)this)', s)
        s=re.sub(r'\(([A-Za-z_][\w.$]*)\)this\b', r'(\1)(Object)this', s)
        s=re.sub(r'\bthis\s+instanceof\s+([A-Za-z_][\w.$]*)', r'(Object)this instanceof \1', s)
        s=re.sub(r'\bthis\s*(==|!=)\s*', r'(Object)this \1 ', s)
        open(p,'w',encoding='utf-8').write(s)
print('patched')
