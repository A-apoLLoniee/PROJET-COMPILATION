class Personne:
    def __init__(self):
        self.nom = ""
        self.age = 0
        self.salaire = 0.0

def calculerSomme(a, b):
    return (a + b)

def calculerMoyenne(tab):
    i = 0
    somme = 0.0
    somme = 0
    for i in range(0, 4 + 1):
        somme = (somme + tab[i])
    return (somme / 5)

def afficherBonjour():
    print("Bonjour tout le monde !", sep="")

p = Personne()
notes = [0] * 5
nombres = [0] * 5
i = 0
somme = 0
resultat = 0
moyenne = 0.0
afficherBonjour()
p.nom = "Alice"
p.age = 25
p.salaire = 3500.50
print("Nom : ", str(p.nom), sep="")
print("Age : ", str(p.age), sep="")
print("Salaire : ", str(p.salaire), sep="")
notes[0] = 15.5
notes[1] = 12.0
notes[2] = 18.5
notes[3] = 10.0
notes[4] = 14.0
moyenne = calculerMoyenne(notes)
print("Moyenne : ", str(moyenne), sep="")
nombres[0] = 5
nombres[1] = 10
nombres[2] = 15
nombres[3] = 20
nombres[4] = 25
somme = 0
for i in range(0, 4 + 1):
    somme = (somme + nombres[i])
print("Somme des nombres : ", str(somme), sep="")
resultat = calculerSomme(10, 20)
print("10 + 20 = ", str(resultat), sep="")
if (moyenne > 12.0):
    print("Bonne moyenne !", sep="")
else:
    print("Peut mieux faire", sep="")
