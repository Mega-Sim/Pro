from glob import glob 
import re

files = glob("*.cpp") + glob("*.c")
pat = re.compile(r'mylog[\w]*')
def replace_mylog(line):
    global pat
    if 'void' in line : return line # function declaration or definition line 
    m = pat.search(line)
    if m :
        return line[:m.start()] + 'MYLOG' + line[ m.end():]
    else:
        return line 
for file in files:
    if file == 'log.cpp' : continue # except log.cpp (real implementation of mylog)
    with open(file) as rfile:
        wlines = []
        for line in rfile.readlines():
            wlines.append( line if not 'mylog' in line else replace_mylog(line))
    print 'Updating', file 
    with open(file, "w") as wfile:
        wfile.writelines(wlines)

            
                