def addition(x, y):
    return (x + y)

def factorielle(n):
    i = 0
    f = 0
    f = 1
    for i in range(1, n + 1):
        f = (f * i)
    return f

def concatener(s1, s2):
    return (s1 + s2)

resultat = 0
a = 0
b = 0
chaineResultat = ""
a = 7
b = 3
resultat = addition(a, b)
print(str(a), " + ", str(b), " = ", str(resultat), sep="")
print("Factorielle de 4 = ", str(factorielle(4)), sep="")
chaineResultat = concatener("Bonjour ", "Monde")
print(str(chaineResultat), sep="")
