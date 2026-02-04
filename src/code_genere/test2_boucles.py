i = 0
j = 0
somme = 0
produit = 0
somme = 0
for i in range(1, 5 + 1):
    somme = (somme + i)
print("Somme 1-5 = ", str(somme), sep="")
i = 1
produit = 1
while (i <= 5):
    produit = (produit * i)
    i = (i + 1)
print("Factorielle 5 = ", str(produit), sep="")
j = 0
while True:
    print("Répétition ", str(j), sep="")
    j = (j + 1)
    if (j == 3):
        break
