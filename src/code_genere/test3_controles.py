x = 0
y = 0
nombre = 0
estVrai = False
condition = False
x = 15
y = 25
if (x < y):
    print("x est plus petit", sep="")
if (x > y):
    print("x est plus grand", sep="")
else:
    print("x n'est pas plus grand", sep="")
estVrai = True
condition = ((x > 10) and estVrai)
if condition:
    print("Condition complexe vraie", sep="")
nombre = -15
if (nombre < 0):
    print(str(nombre), " est négatif", sep="")
else:
    print(str(nombre), " est positif ou nul", sep="")
x = -10
y = -20
if (x > y):
    print(str(x), " est plus grand que ", str(y), sep="")
