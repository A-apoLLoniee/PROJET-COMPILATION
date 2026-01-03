// analyseurLexical.java
import java.io.*;
import java.util.*;

enum TokenType {
    // Mots-Clés
    VAR_TOKEN, DEBUT_TOKEN, FIN_TOKEN, ALGORITHME_TOKEN,
    SI_TOKEN, ALORS_TOKEN, SINON_TOKEN, FINSI_TOKEN,
    POUR_TOKEN, JUSQUA_TOKEN, FAIRE_TOKEN, FINPOUR_TOKEN,
    REPETER_TOKEN, TANTQUE_TOKEN, FINTANTQUE_TOKEN,
    ECRIRE_TOKEN, LIRE_TOKEN,
    STRUCT_TOKEN, FINSTRUCT_TOKEN,
    FONCTION_TOKEN, FINFONCTION_TOKEN, RETOUR_TOKEN,
    ENTIER_TOKEN, REEL_TOKEN, CHAINE_TOKEN, BOOLEEN_TOKEN,
    VRAI_TOKEN, FAUX_TOKEN,
    ET_TOKEN, OU_TOKEN, NON_TOKEN,  // Opérateurs logiques

    // Symboles
    PV_TOKEN, PT_TOKEN, VIR_TOKEN, // ; . ,
    PLUS_TOKEN, MOINS_TOKEN, MULT_TOKEN, DIV_TOKEN,
    AFF_TOKEN, // <-
    INF_TOKEN, INFEG_TOKEN, SUP_TOKEN, SUPEG_TOKEN,
    EG_TOKEN, DIFF_TOKEN, // = <>
    PO_TOKEN, PF_TOKEN, // ( )

    // Tableaux
    CO_TOKEN, CF_TOKEN, // [ ]
    DP_TOKEN, // :

    // Règles lexicales
    ID_TOKEN, NUM_ENTIER_TOKEN, NUM_REEL_TOKEN, CHAINE_LIT_TOKEN,
    EOF_TOKEN, ERREUR_TOKEN,

    // Langage choisi
    LANGAGE_TOKEN // #PYTHON
}

enum Erreurs {
    ERR_CAR_INC, // Caractère inconnu
    ERR_ID_LONG, // Identificateur trop long (>20 caractères)
    ERR_NUM_LONG, // Nombre trop long (>11 chiffres)
    ERR_COMMENT, // Commentaire non fermé
    ERR_DIRECTIVE, // Directive de langage invalide
    ERR_CHAINE_NON_FERMEE // Chaîne de caractères non fermée
}

class SymboleCourant {
    TokenType code; // Type du token
    String nom; // Valeur textuelle (ex: "x", "123", "+")
    double valeurReelle; // Valeur numérique réelle
    int valeurEntiere; // Valeur numérique entière
    int ligne; // Numéro de ligne où le token a été trouvé
    boolean estReel; // Indique si c'est un nombre réel

    public SymboleCourant() {
        this.code = TokenType.ERREUR_TOKEN;
        this.nom = "";
        this.valeurEntiere = 0;
        this.valeurReelle = 0.0;
        this.ligne = 1;
        this.estReel = false;
    }

    @Override
    public String toString() {
        String info = String.format("Ligne %3d | %-25s | '%s'", ligne, code, nom);
        if (code == TokenType.NUM_ENTIER_TOKEN) {
            info += String.format(" | Valeur: %d", valeurEntiere);
        } else if (code == TokenType.NUM_REEL_TOKEN) {
            info += String.format(" | Valeur: %.2f", valeurReelle);
        }
        return info;
    }
}

public class analyseurLexical {
    private BufferedReader fichier; // Flux de lecture du fichier source
    private char carCourant; // Caractère actuellement lu
    private SymboleCourant symCourant; // Token courant
    private int numLigne; // Numéro de ligne actuel
    private String langageCible; // Langage cible (#JAVA, #PYTHON, etc.)

    // Tables de correspondance
    private Map<String, TokenType> tableMotsCles; // Mots-clés → TokenType
    private Map<Erreurs, String> tableErreurs; // Code erreur → Message

    public analyseurLexical(String nomFichier) throws IOException {
        this.fichier = new BufferedReader(new FileReader(nomFichier));
        this.symCourant = new SymboleCourant();
        this.numLigne = 1;
        this.langageCible = null;

        initialiserMotsCles();
        initialiserTableErreurs();

        lireCaractere();
        // Initialiser le premier token
        symboleSuivant();
    }

    private void initialiserMotsCles() {
        tableMotsCles = new HashMap<>();

        // Mots-clés de structure
        tableMotsCles.put("var", TokenType.VAR_TOKEN);
        tableMotsCles.put("debut", TokenType.DEBUT_TOKEN);
        tableMotsCles.put("fin", TokenType.FIN_TOKEN);
        tableMotsCles.put("algorithme", TokenType.ALGORITHME_TOKEN);

        // Mots-clés conditionnels
        tableMotsCles.put("si", TokenType.SI_TOKEN);
        tableMotsCles.put("alors", TokenType.ALORS_TOKEN);
        tableMotsCles.put("sinon", TokenType.SINON_TOKEN);
        tableMotsCles.put("finsi", TokenType.FINSI_TOKEN);

        // Mots-clés de boucles
        tableMotsCles.put("pour", TokenType.POUR_TOKEN);
        tableMotsCles.put("jusqua", TokenType.JUSQUA_TOKEN);
        tableMotsCles.put("faire", TokenType.FAIRE_TOKEN);
        tableMotsCles.put("finpour", TokenType.FINPOUR_TOKEN);
        tableMotsCles.put("repeter", TokenType.REPETER_TOKEN);
        tableMotsCles.put("tantque", TokenType.TANTQUE_TOKEN);
        tableMotsCles.put("fintantque", TokenType.FINTANTQUE_TOKEN);

        // Entrées/Sorties
        tableMotsCles.put("ecrire", TokenType.ECRIRE_TOKEN);
        tableMotsCles.put("lire", TokenType.LIRE_TOKEN);

        // Structures et fonctions
        tableMotsCles.put("structure", TokenType.STRUCT_TOKEN);
        tableMotsCles.put("finstructure", TokenType.FINSTRUCT_TOKEN);
        tableMotsCles.put("fonction", TokenType.FONCTION_TOKEN);
        tableMotsCles.put("finfonction", TokenType.FINFONCTION_TOKEN);
        tableMotsCles.put("retour", TokenType.RETOUR_TOKEN);

        // Types de données
        tableMotsCles.put("entier", TokenType.ENTIER_TOKEN);
        tableMotsCles.put("reel", TokenType.REEL_TOKEN);
        tableMotsCles.put("chainedecharactere", TokenType.CHAINE_TOKEN);
        tableMotsCles.put("chaine", TokenType.CHAINE_TOKEN); // Alias
        tableMotsCles.put("booleen", TokenType.BOOLEEN_TOKEN);

        // Valeurs booléennes
        tableMotsCles.put("vrai", TokenType.VRAI_TOKEN);
        tableMotsCles.put("faux", TokenType.FAUX_TOKEN);

        // Opérateurs logiques
        tableMotsCles.put("et", TokenType.ET_TOKEN);
        tableMotsCles.put("ou", TokenType.OU_TOKEN);
        tableMotsCles.put("non", TokenType.NON_TOKEN);
    }

    private void initialiserTableErreurs() {
        tableErreurs = new HashMap<>();
        tableErreurs.put(Erreurs.ERR_CAR_INC, "Caractère inconnu ou invalide");
        tableErreurs.put(Erreurs.ERR_ID_LONG, "Identificateur trop long (maximum 20 caractères)");
        tableErreurs.put(Erreurs.ERR_NUM_LONG, "Nombre trop long (maximum 11 chiffres)");
        tableErreurs.put(Erreurs.ERR_COMMENT, "Commentaire non fermé (manque '*/')");
        tableErreurs.put(Erreurs.ERR_DIRECTIVE, "Directive de langage invalide (ex: #JAVA, #PYTHON)");
        tableErreurs.put(Erreurs.ERR_CHAINE_NON_FERMEE, "Chaîne de caractères non fermée");
    }

    private void lireCaractere() throws IOException {
        int c = fichier.read();
        if (c == -1) {
            carCourant = '\0';
        } else {
            carCourant = (char) c;
        }
    }

    private char peekCaractere() throws IOException {
        fichier.mark(1);
        int c = fichier.read();
        fichier.reset();
        if (c == -1) {
            return '\0';
        }
        return (char) c;
    }

    private void passerSeparateurs() throws IOException {
        while (estSeparateur(carCourant) || carCourant == '/') {
            if (carCourant == '/') {
                lireCaractere();

                // Commentaire multi-lignes: /* ... */
                if (carCourant == '*') {
                    lireCaractere();
                    while (true) {
                        if (carCourant == '\0') {
                            erreur(Erreurs.ERR_COMMENT);
                            return;
                        }
                        if (carCourant == '*') {
                            lireCaractere();
                            if (carCourant == '/') {
                                lireCaractere();
                                break;
                            }
                        } else {
                            lireCaractere();
                        }
                    }
                }
                // Commentaire sur une ligne: // ...
                else if (carCourant == '/') {
                    while (carCourant != '\n' && carCourant != '\0') {
                        lireCaractere();
                    }
                    if (carCourant == '\n') {
                        lireCaractere();
                    }
                }
                // Ce n'est pas un commentaire, c'était juste '/'
                else {
                    // Revenir au caractère '/'
                    carCourant = '/';
                    return;
                }
            } else {
                if (carCourant == '\n') {
                    numLigne++;
                }
                lireCaractere();
            }
        }
    }

    private boolean estLettre(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean estChiffre(char c) {
        return Character.isDigit(c);
    }

    private boolean estSeparateur(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private void erreur(Erreurs codeErreur) {
        System.err.println("\n ERREUR LEXICALE");
        System.err.println("   Ligne " + numLigne + ": " + tableErreurs.get(codeErreur));
        System.err.println("   Contexte: caractère courant = '" + carCourant + "'");
        System.exit(1);
    }

    /*
     * Format: #<NOM_LANGAGE>
     */
    private void lireDirective() throws IOException {
        StringBuilder directive = new StringBuilder();
        directive.append(carCourant); // '#'
        lireCaractere();

        // Lire le nom du langage (lettres uniquement)
        while (estLettre(carCourant)) {
            directive.append(carCourant);
            lireCaractere();
        }

        String directiveStr = directive.toString();

        // Valider la directive
        if (directiveStr.length() <= 1) {
            erreur(Erreurs.ERR_DIRECTIVE);
            return;
        }

        // Extraire et sauvegarder le langage cible
        langageCible = directiveStr.substring(1).toUpperCase();

        // Vérifier que le langage est supporté
        Set<String> langagesSuportes = new HashSet<>(Arrays.asList("PYTHON", "JAVA", "C"));
        if (!langagesSuportes.contains(langageCible)) {
            System.err.println("Avertissement: Langage '" + langageCible + "' non supporté par défaut.");
        }

        symCourant.code = TokenType.LANGAGE_TOKEN;
        symCourant.nom = directiveStr;
        symCourant.ligne = numLigne;
    }

    /**
     * Lit un mot: soit un mot-clé, soit un identificateur
     * Format: lettre (lettre | chiffre | _)*
     * Longueur max: 20 caractères
     */
    private void lireMot() throws IOException {
        StringBuilder mot = new StringBuilder();
        int longueur = 0;

        // Lire tant qu'on a des lettres, chiffres ou underscore
        while ((estLettre(carCourant) || estChiffre(carCourant)) && longueur < 20) {
            mot.append(carCourant);
            longueur++;
            lireCaractere();
        }

        // Vérifier si le mot est trop long
        if (longueur >= 20 && (estLettre(carCourant) || estChiffre(carCourant))) {
            erreur(Erreurs.ERR_ID_LONG);
            return;
        }

        String motStr = mot.toString();
        String motLower = motStr.toLowerCase();

        symCourant.nom = motStr;
        symCourant.ligne = numLigne;

        // Vérifier si c'est un mot-clé
        if (tableMotsCles.containsKey(motLower)) {
            symCourant.code = tableMotsCles.get(motLower);
        } else {
            // Sinon c'est un identificateur
            symCourant.code = TokenType.ID_TOKEN;
        }
    }

    /**
     * Lit un nombre entier ou réel
     * Format: [-+]?chiffre+ [.chiffre+]
     * Longueur max: 11 chiffres
     */
    private void lireNombre() throws IOException {
        StringBuilder nombre = new StringBuilder();
        int longueur = 0;
        boolean negatif = false;
        boolean estReel = false;

        // Vérifier le signe négatif
        if (carCourant == '-') {
            negatif = true;
            nombre.append(carCourant);
            lireCaractere();
        }
        // Gérer le signe + optionnel - on le consomme mais on ne l'ajoute pas
        else if (carCourant == '+') {
            lireCaractere(); // Consommer le + mais ne pas l'ajouter au nombre
        }

        // Lire la partie entière
        while (estChiffre(carCourant) && longueur < 11) {
            nombre.append(carCourant);
            longueur++;
            lireCaractere();
        }

        // Vérifier si le nombre est trop long
        if (longueur >= 11 && estChiffre(carCourant)) {
            erreur(Erreurs.ERR_NUM_LONG);
            return;
        }

        // Vérifier si c'est un nombre réel (avec point décimal)
        if (carCourant == '.') {
            estReel = true;
            nombre.append(carCourant);
            lireCaractere();

            // Lire la partie décimale
            while (estChiffre(carCourant) && longueur < 11) {
                nombre.append(carCourant);
                longueur++;
                lireCaractere();
            }
        }

        // Si on n'a lu aucun chiffre, ce n'est pas un nombre valide
        if (nombre.length() == 0 || (negatif && nombre.length() == 1)) {
            erreur(Erreurs.ERR_NUM_LONG);
            return;
        }

        symCourant.nom = nombre.toString();
        symCourant.ligne = numLigne;
        symCourant.estReel = estReel;

        // Convertir en valeur numérique
        try {
            if (estReel) {
                symCourant.code = TokenType.NUM_REEL_TOKEN;
                symCourant.valeurReelle = Double.parseDouble(nombre.toString());
            } else {
                symCourant.code = TokenType.NUM_ENTIER_TOKEN;
                symCourant.valeurEntiere = Integer.parseInt(nombre.toString());
            }
        } catch (NumberFormatException e) {
            if (estReel) {
                symCourant.valeurReelle = 0.0;
            } else {
                symCourant.valeurEntiere = 0;
            }
        }
    }

    /**
     * Lit une chaîne de caractères entre guillemets
     * Format: "caractères"
     */
    private void lireChaine() throws IOException {
        StringBuilder chaine = new StringBuilder();

        // Passer le guillemet ouvrant
        lireCaractere();

        // Lire jusqu'au guillemet fermant
        while (carCourant != '"' && carCourant != '\0' && carCourant != '\n') {
            chaine.append(carCourant);
            lireCaractere();
        }

        // Vérifier si la chaîne est bien fermée
        if (carCourant != '"') {
            erreur(Erreurs.ERR_CHAINE_NON_FERMEE);
            return;
        }

        // Passer le guillemet fermant
        lireCaractere();

        symCourant.code = TokenType.CHAINE_LIT_TOKEN;
        symCourant.nom = chaine.toString();
        symCourant.ligne = numLigne;
    }

    /**
     * Lit un symbole spécial: opérateurs, délimiteurs, etc.
     */
    private void lireSpecial() throws IOException {
        symCourant.ligne = numLigne;

        switch (carCourant) {
            // Délimiteurs
            case ';':
                symCourant.code = TokenType.PV_TOKEN;
                symCourant.nom = ";";
                lireCaractere();
                break;
            case '.':
                symCourant.code = TokenType.PT_TOKEN;
                symCourant.nom = ".";
                lireCaractere();
                break;
            case ',':
                symCourant.code = TokenType.VIR_TOKEN;
                symCourant.nom = ",";
                lireCaractere();
                break;
            case ':':
                symCourant.code = TokenType.DP_TOKEN;
                symCourant.nom = ":";
                lireCaractere();
                break;

            // Parenthèses
            case '(':
                symCourant.code = TokenType.PO_TOKEN;
                symCourant.nom = "(";
                lireCaractere();
                break;
            case ')':
                symCourant.code = TokenType.PF_TOKEN;
                symCourant.nom = ")";
                lireCaractere();
                break;

            // Crochets (tableaux)
            case '[':
                symCourant.code = TokenType.CO_TOKEN;
                symCourant.nom = "[";
                lireCaractere();
                break;
            case ']':
                symCourant.code = TokenType.CF_TOKEN;
                symCourant.nom = "]";
                lireCaractere();
                break;

            // Opérateurs arithmétiques
            case '+':
                symCourant.code = TokenType.PLUS_TOKEN;
                symCourant.nom = "+";
                lireCaractere();
                break;
            case '-':
                symCourant.code = TokenType.MOINS_TOKEN;
                symCourant.nom = "-";
                lireCaractere();
                break;
            case '*':
                symCourant.code = TokenType.MULT_TOKEN;
                symCourant.nom = "*";
                lireCaractere();
                break;
            case '/':
                symCourant.code = TokenType.DIV_TOKEN;
                symCourant.nom = "/";
                lireCaractere();
                break;

            // Affectation: <-
            case '<':
                lireCaractere();
                if (carCourant == '-') {
                    symCourant.code = TokenType.AFF_TOKEN;
                    symCourant.nom = "<-";
                    lireCaractere();
                } else if (carCourant == '=') {
                    symCourant.code = TokenType.INFEG_TOKEN;
                    symCourant.nom = "<=";
                    lireCaractere();
                } else if (carCourant == '>') {
                    symCourant.code = TokenType.DIFF_TOKEN;
                    symCourant.nom = "<>";
                    lireCaractere();
                } else {
                    symCourant.code = TokenType.INF_TOKEN;
                    symCourant.nom = "<";
                }
                break;

            // Opérateurs de comparaison
            case '>':
                lireCaractere();
                if (carCourant == '=') {
                    symCourant.code = TokenType.SUPEG_TOKEN;
                    symCourant.nom = ">=";
                    lireCaractere();
                } else {
                    symCourant.code = TokenType.SUP_TOKEN;
                    symCourant.nom = ">";
                }
                break;
            case '=':
                symCourant.code = TokenType.EG_TOKEN;
                symCourant.nom = "=";
                lireCaractere();
                break;

            // Guillemet pour chaîne
            case '"':
                lireChaine();
                break;

            // Fin de fichier
            case '\0':
                symCourant.code = TokenType.EOF_TOKEN;
                symCourant.nom = "EOF";
                break;

            // Caractère non reconnu
            default:
                symCourant.code = TokenType.ERREUR_TOKEN;
                symCourant.nom = String.valueOf(carCourant);
                erreur(Erreurs.ERR_CAR_INC);
                break;
        }
    }

    public void symboleSuivant() throws IOException {
        passerSeparateurs();

        if (carCourant == '#') {
            lireDirective();
        } else if (estLettre(carCourant)) {
            lireMot();
        } else if (estChiffre(carCourant)) {
            lireNombre();
        } else if (carCourant == '-' || carCourant == '+') {
            // Peut être un opérateur ou le début d'un nombre
            char prochain = peekCaractere();
            if (estChiffre(prochain)) {
                // C'est un nombre (signé)
                lireNombre();
            } else {
                // C'est un opérateur
                lireSpecial();
            }
        } else {
            lireSpecial();
        }
    }

    public SymboleCourant getSymboleCourant() {
        return symCourant;
    }

    public String getLangageCible() {
        return langageCible;
    }

    public int getNumLigne() {
        return numLigne;
    }

    public void afficherToken() {
        System.out.println(symCourant.toString());
    }

    public void fermer() throws IOException {
        if (fichier != null) {
            fichier.close();
        }
    }
}