class Point:
    def __init__(self):
        self.x = 0.0
        self.y = 0.0

class Rectangle:
    def __init__(self):
        self.coinSupGauche = Point()
        self.largeur = 0.0
        self.hauteur = 0.0

class Etudiant:
    def __init__(self):
        self.nom = ""
        self.age = 0
        self.notes = [0] * 3
        self.actif = False

def calculerPerimetre(rect):
    return (2 * (rect.largeur + rect.hauteur))

p1 = Point()
p2 = Point()
rect = Rectangle()
etud = Etudiant()
perimetre = 0.0
i = 0
p1.x = 0.0
p1.y = 0.0
p2.x = 5.0
p2.y = 3.0
print("Point 1 : (", str(p1.x), ", ", str(p1.y), ")", sep="")
print("Point 2 : (", str(p2.x), ", ", str(p2.y), ")", sep="")
rect.coinSupGauche = p1
rect.largeur = 10.5
rect.hauteur = 5.2
print("Rectangle : largeur=", str(rect.largeur), ", hauteur=", str(rect.hauteur), sep="")
perimetre = calculerPerimetre(rect)
print("Périmètre = ", str(perimetre), sep="")
etud.nom = "Alice"
etud.age = 20
etud.actif = True
etud.notes[0] = 15
etud.notes[1] = 12
etud.notes[2] = 18
print("Étudiant : ", str(etud.nom), sep="")
print("Âge : ", str(etud.age), sep="")
print("Actif : ", str(etud.actif), sep="")
print("Notes : ", sep="")
for i in range(0, 2 + 1):
    print("  Note ", str(i), " = ", str(etud.notes[i]), sep="")
