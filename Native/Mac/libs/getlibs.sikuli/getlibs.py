f = open("/Users/rhocke/Sikuli12.11/Resources/natives/Mac/libs/done.txt")
names = []
for line in f.readlines(): 
    line = line.strip()
    if line.startswith("+"): 
        print line
        name = line.split(" ")[1]
    if line.startswith("@"):
        print line
        name = line.split("/")[1].split("(")[0].strip()
    if name not in names:
        names.append(name)
print "--------"
names.sort()
for name in names: print name