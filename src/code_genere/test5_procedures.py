def afficherMessage(msg):
    print("Message : ", str(msg), sep="")

def afficherTableMulti(n):
    i = 0
    print("Table de multiplication de ", str(n), sep="")
    for i in range(1, 10 + 1):
        print(str(n), " x ", str(i), " = ", str((n * i)), sep="")

def analyserNombre(n):
    if (n > 0):
        print(str(n), " est positif", sep="")
    else:
        if (n < 0):
            print(str(n), " est négatif", sep="")
        else:
            print("Le nombre est zéro", sep="")

nombre = 0
message = ""
message = "Bienvenue"
afficherMessage(message)
nombre = 7
afficherTableMulti(nombre)
nombre = -5
afficherTableMulti(nombre)
analyserNombre(15)
analyserNombre(-8)
analyserNombre(0)
