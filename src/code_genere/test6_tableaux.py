def trouverMaximum(tab):
    i = 0
    max = 0
    max = tab[0]
    for i in range(1, 4 + 1):
        if (tab[i] > max):
            max = tab[i]
    return max

notes = [0] * 5
i = 0
somme = 0
max = 0
moyenne = 0.0
notes[0] = 15
notes[1] = 12
notes[2] = 18
notes[3] = 10
notes[4] = 14
print("Notes : ", sep="")
for i in range(0, 4 + 1):
    print("notes[", str(i), "] = ", str(notes[i]), sep="")
somme = 0
for i in range(0, 4 + 1):
    somme = (somme + notes[i])
print("Somme = ", str(somme), sep="")
moyenne = (somme / 5)
print("Moyenne = ", str(moyenne), sep="")
max = trouverMaximum(notes)
print("Note maximale = ", str(max), sep="")
