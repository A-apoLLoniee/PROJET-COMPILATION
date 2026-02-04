def afficherMessage(msg):
    print("Message reçu : ", str(msg), sep="")

def afficherTableMulti(n):
    i = 0
    print("Table de multiplication de ", str(n), sep="")
    for i in range(1, 10 + 1):
        print(str(n), " x ", str(i), " = ", str((n * i)), sep="")

nombre = 0
message = ""
message = "Test des procédures"
afficherMessage(message)
nombre = 7
afficherTableMulti(nombre)
