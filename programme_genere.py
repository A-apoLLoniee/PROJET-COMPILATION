def calculerCarre(x):
    return x * x

limite = 18
age = 20
prix = 12.5
estMajeur = age >= limite
if estMajeur and age < 100:
    message = "Personne majeure"
    print(message)
else:
    print("Personne mineure")
resultat = age + 5
print("Age: ", age)
print("Prix: ", prix)
resultat = calculerCarre(age)
print("Carré de l'âge: ", resultat)
for i in range(1, 5 + 1):
    print("Itération: ", i)
compteur = 0
while compteur < 3:
    print("Tantque: ", compteur)
    compteur = compteur + 1
while True:
    print("Repeter")
    if False:
        break
