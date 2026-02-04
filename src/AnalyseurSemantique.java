// AnalyseurSemantique.java - VERSION CORRIGÉE
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

enum TypeDonnee {
    ENTIER, REEL, CHAINE, BOOLEEN, STRUCTURE, TABLEAU, FONCTION, INCONNU
}

class EntreeTableSymboles {
    String nom;
    TypeDonnee type;
    TypeDonnee typeElement; // Pour les tableaux
    String nomStructure; // Pour les variables de type structure
    List<TypeDonnee> parametres; // Pour les fonctions
    TypeDonnee typeRetour; // Pour les fonctions
    int ligneDeclaration;
    boolean estConstante;
    boolean estTableau;
    boolean estParametre; //  indique si c'est un paramètre
    Object valeurInitiale; // Pour les constantes

    public EntreeTableSymboles(String nom, TypeDonnee type, int ligne) {
        this.nom = nom;
        this.type = type;
        this.ligneDeclaration = ligne;
        this.estConstante = false;
        this.estTableau = false;
        this.estParametre = false;
        this.parametres = new ArrayList<>();
        this.valeurInitiale = null;
    }
}
class StructureInfo {
    String nom;
    Map<String, TypeDonnee> champs;
    Map<String, String> champsStructure; // nom du champ -> nom de la structure du champ
    Map<String, TypeDonnee> champsTableau; // nom du champ -> type d'élément si tableau

    public StructureInfo(String nom) {
        this.nom = nom;
        this.champs = new HashMap<>();
        this.champsStructure = new HashMap<>();
        this.champsTableau = new HashMap<>();
    }
}

class FonctionInfo {
    String nom;
    TypeDonnee typeRetour;
    List<TypeDonnee> parametres;
    Map<String, TypeDonnee> variablesLocales;

    public FonctionInfo(String nom, TypeDonnee typeRetour) {
        this.nom = nom;
        this.typeRetour = typeRetour;
        this.parametres = new ArrayList<>();
        this.variablesLocales = new HashMap<>();
    }
}

public class AnalyseurSemantique {
    private NoeudAST arbreSyntaxique;
    private List<String> erreursSemantiques;
    private List<String> avertissements;

    // Tables de symboles
    private Map<String, EntreeTableSymboles> tableSymbolesGlobale;
    private Map<String, StructureInfo> structures;
    private Map<String, FonctionInfo> fonctions;
    private Stack<Map<String, EntreeTableSymboles>> pilePortees;
    private String fonctionCourante;
    private TypeDonnee typeRetourAttendu;

    public AnalyseurSemantique(NoeudAST arbreSyntaxique) {
        this.arbreSyntaxique = arbreSyntaxique;
        this.erreursSemantiques = new ArrayList<>();
        this.avertissements = new ArrayList<>();
        this.tableSymbolesGlobale = new HashMap<>();
        this.structures = new HashMap<>();
        this.fonctions = new HashMap<>();
        this.pilePortees = new Stack<>();
        this.fonctionCourante = null;
        this.typeRetourAttendu = TypeDonnee.INCONNU;

        // Initialiser la pile avec une portée globale
        entrerNouvellePortee();
    }

    public void analyser() {
        analyserProgramme(arbreSyntaxique);
    }

    private void analyserProgramme(NoeudAST programme) {
        // Analyser tous les enfants du programme
        for (NoeudAST enfant : programme.getEnfants()) {
            switch (enfant.getType()) {
                case DIRECTIVE_LANGAGE:
                    // Pas de vérification sémantique nécessaire
                    break;

                case DECLARATION_STRUCTURE:
                    analyserStructure(enfant);
                    break;

                case DECLARATION_FONCTION:
                    analyserFonction(enfant);
                    break;

                case DECLARATION_PROCEDURE:  // AJOUTER CETTE LIGNE
                    analyserProcedure(enfant);
                    break;

                case SECTION_VAR:
                    analyserSectionVariable(enfant, true);
                    break;

                case BLOC_INSTRUCTIONS:
                    analyserBlocInstructions(enfant);
                    break;



                default:
                    erreurSemantique("Élément de programme inattendu: " +
                            enfant.getType(), enfant.getLigne());
            }
        }
    }


    private void analyserSectionVariable(NoeudAST sectionVar, boolean globale) {
        // Une section VAR contient plusieurs déclarations de variables
        for (NoeudAST declaration : sectionVar.getEnfants()) {
            if (declaration.getType() == NoeudAST.TypeNoeud.DECLARATION_VARIABLE) {
                analyserDeclarationVariable(declaration, globale);
            }
        }
    }

    private void analyserStructure(NoeudAST structure) {
        String nomStructure = structure.getValeur();
        if (nomStructure.isEmpty()) {
            erreurSemantique("Structure sans nom", structure.getLigne());
            return;
        }

        if (structures.containsKey(nomStructure)) {
            erreurSemantique("Structure déjà définie: " + nomStructure,
                    structure.getLigne());
            return;
        }

        StructureInfo info = new StructureInfo(nomStructure);

        // Analyser les champs
        for (NoeudAST champ : structure.getEnfants()) {
            if (champ.getType() == NoeudAST.TypeNoeud.CHAMP_STRUCTURE) {
                analyserChampStructure(champ, info, structure.getLigne());
            }
        }

        structures.put(nomStructure, info);
    }

    private void analyserChampStructure(NoeudAST declaration, StructureInfo structure,
                                        int ligneStructure) {
        List<String> nomsVariables = new ArrayList<>();
        TypeDonnee typeChamp = TypeDonnee.INCONNU;
        String nomStructureChamp = null;
        TypeDonnee typeElementTableau = TypeDonnee.INCONNU;
        boolean estTableau = false;

        for (NoeudAST enfant : declaration.getEnfants()) {
            switch (enfant.getType()) {
                case LISTE_IDENTIFICATEURS:
                    for (NoeudAST id : enfant.getEnfants()) {
                        nomsVariables.add(id.getValeur());
                    }
                    break;

                case TYPE:
                    String typeStr = enfant.getValeur();

                    // CORRECTION: Gérer les tableaux dans les structures
                    if (typeStr.contains("[")) {
                        estTableau = true;
                        // Extraire le type de base (ex: "entier[3]" -> "entier")
                        String typeBase = typeStr.substring(0, typeStr.indexOf('[')).trim();
                        typeElementTableau = convertirType(typeBase);
                        typeChamp = TypeDonnee.TABLEAU;
                    } else {
                        typeChamp = convertirType(typeStr);
                        if (typeChamp == TypeDonnee.STRUCTURE) {
                            nomStructureChamp = typeStr;
                        }
                    }
                    break;
            }
        }

        if (typeChamp == TypeDonnee.INCONNU) {
            erreurSemantique("Type de champ invalide", declaration.getLigne());
            return;
        }

        for (String nomVariable : nomsVariables) {
            if (structure.champs.containsKey(nomVariable)) {
                erreurSemantique("Champ déjà défini dans la structure: " +
                        nomVariable, declaration.getLigne());
            } else {
                structure.champs.put(nomVariable, typeChamp);
                if (nomStructureChamp != null) {
                    structure.champsStructure.put(nomVariable, nomStructureChamp);
                }
                if (estTableau) {
                    structure.champsTableau.put(nomVariable, typeElementTableau);
                }
            }
        }
    }

    private void analyserProcedure(NoeudAST procedure) {
        String nomProcedure = procedure.getValeur();
        if (nomProcedure.isEmpty()) {
            erreurSemantique("Procédure sans nom", procedure.getLigne());
            return;
        }

        if (fonctions.containsKey(nomProcedure)) {
            erreurSemantique("Procédure déjà définie: " + nomProcedure,
                    procedure.getLigne());
            return;
        }

        List<TypeDonnee> parametres = new ArrayList<>();
        List<String> nomsParametres = new ArrayList<>();
        List<TypeDonnee> typesElementsParametres = new ArrayList<>(); // Pour les tableaux
        List<String> nomsStructuresParametres = new ArrayList<>(); // Pour les structures

        // Analyser les paramètres
        for (NoeudAST enfant : procedure.getEnfants()) {
            switch (enfant.getType()) {
                case LISTE_PARAMETRES:
                    analyserParametresFonction(enfant, parametres, nomsParametres,
                            typesElementsParametres, nomsStructuresParametres);
                    break;
            }
        }

        FonctionInfo info = new FonctionInfo(nomProcedure, TypeDonnee.INCONNU);
        info.parametres = parametres;
        fonctions.put(nomProcedure, info);

        // Entrer dans la portée de la procédure
        entrerNouvellePortee();
        fonctionCourante = nomProcedure;
        typeRetourAttendu = TypeDonnee.INCONNU;

        // Ajouter les paramètres
        for (int i = 0; i < nomsParametres.size(); i++) {
            EntreeTableSymboles entree = new EntreeTableSymboles(
                    nomsParametres.get(i), parametres.get(i), procedure.getLigne());
            entree.estParametre = true;

            // CORRECTION: Si c'est un tableau, ajouter le typeElement
            if (parametres.get(i) == TypeDonnee.TABLEAU) {
                entree.estTableau = true;
                entree.typeElement = typesElementsParametres.get(i);
            }

            // CORRECTION: Si c'est une structure, ajouter le nomStructure
            if (parametres.get(i) == TypeDonnee.STRUCTURE) {
                entree.nomStructure = nomsStructuresParametres.get(i);
            }

            pilePortees.peek().put(nomsParametres.get(i), entree);
        }

        // Analyser le corps
        for (NoeudAST enfant : procedure.getEnfants()) {
            switch (enfant.getType()) {
                case SECTION_VAR:
                    analyserSectionVariable(enfant, false);
                    break;
                case BLOC_INSTRUCTIONS:
                    analyserBlocInstructions(enfant);
                    break;
            }
        }

        sortirPortee();
        fonctionCourante = null;
        typeRetourAttendu = TypeDonnee.INCONNU;
    }

    private void analyserDeclarationVariable(NoeudAST declaration, boolean globale) {
        List<String> nomsVariables = new ArrayList<>();
        TypeDonnee type = TypeDonnee.INCONNU;
        TypeDonnee typeElement = TypeDonnee.INCONNU;
        String nomStructure = null;
        boolean estTableau = false;

        for (NoeudAST enfant : declaration.getEnfants()) {
            switch (enfant.getType()) {
                case LISTE_IDENTIFICATEURS:
                    for (NoeudAST id : enfant.getEnfants()) {
                        nomsVariables.add(id.getValeur());
                    }
                    break;

                case TYPE:
                    String typeStr = enfant.getValeur();

                    // Vérifier s'il s'agit d'un tableau
                    if (typeStr.contains("[")) {
                        estTableau = true;
                        // Extraire le type de base (ex: "entier[10]" -> "entier")
                        String typeBase = typeStr.substring(0, typeStr.indexOf('[')).trim();
                        typeElement = convertirType(typeBase);
                        type = TypeDonnee.TABLEAU;
                    } else {
                        type = convertirType(typeStr);
                        if (type == TypeDonnee.STRUCTURE) {
                            nomStructure = typeStr;
                            // Vérifier que la structure est définie
                            if (!structures.containsKey(nomStructure)) {
                                erreurSemantique("Structure non définie: " + nomStructure,
                                        enfant.getLigne());
                            }
                        }
                    }
                    break;
            }
        }

        if (type == TypeDonnee.INCONNU) {
            erreurSemantique("Type invalide dans la déclaration", declaration.getLigne());
            return;
        }

        // Ajouter chaque variable dans la table de symboles appropriée
        Map<String, EntreeTableSymboles> table = globale ?
                tableSymbolesGlobale : pilePortees.peek();

        for (String nomVariable : nomsVariables) {
            if (table.containsKey(nomVariable)) {
                erreurSemantique("Variable déjà déclarée: " + nomVariable,
                        declaration.getLigne());
            } else {
                EntreeTableSymboles entree = new EntreeTableSymboles(nomVariable, type,
                        declaration.getLigne());
                entree.estTableau = estTableau;
                entree.typeElement = typeElement;
                entree.nomStructure = nomStructure;
                table.put(nomVariable, entree);
            }
        }
    }

    private void analyserFonction(NoeudAST fonction) {
        String nomFonction = fonction.getValeur();
        if (nomFonction.isEmpty()) {
            erreurSemantique("Fonction sans nom", fonction.getLigne());
            return;
        }

        if (fonctions.containsKey(nomFonction)) {
            erreurSemantique("Fonction déjà définie: " + nomFonction,
                    fonction.getLigne());
            return;
        }

        TypeDonnee typeRetour = TypeDonnee.INCONNU;
        List<TypeDonnee> parametres = new ArrayList<>();
        List<String> nomsParametres = new ArrayList<>();
        List<TypeDonnee> typesElementsParametres = new ArrayList<>(); // Pour les tableaux
        List<String> nomsStructuresParametres = new ArrayList<>(); // Pour les structures

        // Analyser les enfants pour trouver les paramètres et le type de retour
        for (NoeudAST enfant : fonction.getEnfants()) {
            switch (enfant.getType()) {
                case LISTE_PARAMETRES:
                    analyserParametresFonction(enfant, parametres, nomsParametres,
                            typesElementsParametres, nomsStructuresParametres);
                    break;

                case TYPE:
                    String typeStr = enfant.getValeur();

                    // CORRECTION: Gérer les types de structure pour le retour
                    if (structures.containsKey(typeStr)) {
                        typeRetour = TypeDonnee.STRUCTURE;
                    } else {
                        typeRetour = convertirType(typeStr);
                    }
                    break;
            }
        }

        FonctionInfo info = new FonctionInfo(nomFonction, typeRetour);
        info.parametres = parametres;
        fonctions.put(nomFonction, info);

        // Entrer dans la portée de la fonction
        entrerNouvellePortee();
        fonctionCourante = nomFonction;
        typeRetourAttendu = typeRetour;

        // Ajouter les paramètres à la portée locale
        for (int i = 0; i < nomsParametres.size(); i++) {
            EntreeTableSymboles entree = new EntreeTableSymboles(
                    nomsParametres.get(i), parametres.get(i), fonction.getLigne());
            entree.estParametre = true;

            // CORRECTION: Si c'est un tableau, ajouter le typeElement
            if (parametres.get(i) == TypeDonnee.TABLEAU) {
                entree.estTableau = true;
                entree.typeElement = typesElementsParametres.get(i);
            }

            // CORRECTION: Si c'est une structure, ajouter le nomStructure
            if (parametres.get(i) == TypeDonnee.STRUCTURE) {
                entree.nomStructure = nomsStructuresParametres.get(i);
            }

            pilePortees.peek().put(nomsParametres.get(i), entree);
        }

        // Analyser le corps de la fonction
        for (NoeudAST enfant : fonction.getEnfants()) {
            switch (enfant.getType()) {
                case SECTION_VAR:
                    analyserSectionVariable(enfant, false);
                    break;

                case BLOC_INSTRUCTIONS:
                    analyserBlocInstructions(enfant);
                    break;

                case RETOUR:
                    analyserRetour(enfant);
                    break;
            }
        }

        sortirPortee();
        fonctionCourante = null;
        typeRetourAttendu = TypeDonnee.INCONNU;
    }

    private void analyserParametresFonction(NoeudAST listeParametres,
                                            List<TypeDonnee> parametres,
                                            List<String> nomsParametres,
                                            List<TypeDonnee> typesElements,
                                            List<String> nomsStructures) {
        for (NoeudAST param : listeParametres.getEnfants()) {
            if (param.getType() == NoeudAST.TypeNoeud.PARAMETRE) {
                String nomParam = param.getValeur();

                for (NoeudAST enfant : param.getEnfants()) {
                    if (enfant.getType() == NoeudAST.TypeNoeud.TYPE) {
                        String typeStr = enfant.getValeur();
                        TypeDonnee type;
                        TypeDonnee typeElement = TypeDonnee.INCONNU;
                        String nomStructure = null;

                        // CORRECTION: Gérer les tableaux et structures dans les paramètres
                        if (typeStr.contains("[")) {
                            type = TypeDonnee.TABLEAU;
                            // Extraire le type de base (ex: "entier[5]" -> "entier")
                            String typeBase = typeStr.substring(0, typeStr.indexOf('[')).trim();
                            typeElement = convertirType(typeBase);
                        } else if (structures.containsKey(typeStr)) {
                            type = TypeDonnee.STRUCTURE;
                            nomStructure = typeStr;
                        } else {
                            type = convertirType(typeStr);
                        }

                        parametres.add(type);
                        nomsParametres.add(nomParam);
                        typesElements.add(typeElement);
                        nomsStructures.add(nomStructure);
                    }
                }
            }
        }
    }

    private void analyserBlocInstructions(NoeudAST bloc) {
        for (NoeudAST instruction : bloc.getEnfants()) {
            analyserInstruction(instruction);
        }
    }

    private void analyserInstruction(NoeudAST instruction) {
        switch (instruction.getType()) {
            case AFFECTATION:
                analyserAffectation(instruction);
                break;

            case CONDITION:
                analyserCondition(instruction);
                break;

            case BOUCLE_POUR:
                analyserBouclePour(instruction);
                break;

            case BOUCLE_TANTQUE:
                analyserBoucleTantque(instruction);
                break;

            case BOUCLE_REPETER:
                analyserBoucleRepeter(instruction);
                break;

            case ECRIRE:
                analyserEcrire(instruction);
                break;

            case LIRE:
                analyserLire(instruction);
                break;

            case APPEL_FONCTION:
                analyserAppelFonction(instruction, true);
                break;

            case RETOUR:
                analyserRetour(instruction);
                break;

            default:
                erreurSemantique("Instruction non reconnue: " + instruction.getType(),
                        instruction.getLigne());
        }
    }

    private void analyserRetour(NoeudAST retour) {
        // Vérifier que le type de retour correspond
        if (retour.getEnfants().size() > 0) {
            NoeudAST expression = retour.getEnfant(0);
            TypeDonnee typeRetourTrouve = analyserExpression(expression);
            verifierCompatibiliteTypes(typeRetourAttendu, typeRetourTrouve,
                    "Type de retour incorrect", retour.getLigne());
        } else {
            // RETOUR sans expression
            if (typeRetourAttendu != TypeDonnee.INCONNU) {
                erreurSemantique("RETOUR doit avoir une expression (type attendu: " +
                        typeRetourAttendu + ")", retour.getLigne());
            }
        }
    }

    private void analyserAffectation(NoeudAST affectation) {
        String nomVariable = affectation.getValeur();
        EntreeTableSymboles entree = chercherVariable(nomVariable);

        if (entree == null) {
            erreurSemantique("Variable non déclarée: " + nomVariable,
                    affectation.getLigne());
            return;
        }

        if (entree.estConstante) {
            erreurSemantique("Impossible de modifier une constante: " + nomVariable,
                    affectation.getLigne());
            return;
        }

        if (affectation.getEnfants().size() < 1) {
            erreurSemantique("Expression manquante dans l'affectation",
                    affectation.getLigne());
            return;
        }

        // NOUVELLE LOGIQUE : distinguer les cas selon le nombre d'enfants ET le type du premier enfant
        if (affectation.getEnfants().size() == 2) {
            // 2 enfants : peut être ACCES_TABLEAU ou ACCES_CHAMP
            NoeudAST acces = affectation.getEnfants().get(0);
            NoeudAST expression = affectation.getEnfants().get(1);

            if (acces.getType() == NoeudAST.TypeNoeud.ACCES_TABLEAU) {
                // CAS 1 : Affectation à un élément de tableau (ex: notes[0] <- 15)
                // Vérifier l'indice du tableau
                if (acces.getEnfants().size() > 0) {
                    TypeDonnee typeIndice = analyserExpression(acces.getEnfants().get(0));
                    if (typeIndice != TypeDonnee.ENTIER) {
                        erreurSemantique("Indice de tableau doit être entier",
                                acces.getLigne());
                    }
                }

                TypeDonnee typeExpression = analyserExpression(expression);
                verifierCompatibiliteTypes(entree.typeElement, typeExpression,
                        "Types incompatibles dans l'affectation de tableau",
                        affectation.getLigne());
            } else if (acces.getType() == NoeudAST.TypeNoeud.ACCES_CHAMP) {
                // CAS 2 : Affectation à un champ de structure (ex: p1.x <- 5.0)
                TypeDonnee typeChamp = analyserAccesChampExpression(acces);
                TypeDonnee typeExpression = analyserExpression(expression);

                verifierCompatibiliteTypes(typeChamp, typeExpression,
                        "Types incompatibles dans l'affectation au champ",
                        affectation.getLigne());
            } else {
                erreurSemantique("Structure d'affectation invalide",
                        affectation.getLigne());
            }
        } else {
            // CAS 3 : Affectation normale (ex: max <- tab[0] ou x <- 5)
            // Structure: 1 seul enfant = expression (qui peut être un ACCES_TABLEAU)
            NoeudAST expression = affectation.getEnfants().get(0);
            TypeDonnee typeExpression = analyserExpression(expression);

            // Si l'expression est un accès tableau, vérifier que le type correspond
            if (expression.getType() == NoeudAST.TypeNoeud.ACCES_TABLEAU) {
                // Pour un accès tableau, le type de l'expression est typeElement du tableau
                // On doit donc vérifier la compatibilité avec le type de la variable
                verifierCompatibiliteTypes(entree.type, typeExpression,
                        "Types incompatibles dans l'affectation",
                        affectation.getLigne());
            } else {
                // Pour les autres types d'expressions
                verifierCompatibiliteTypes(entree.type, typeExpression,
                        "Types incompatibles dans l'affectation",
                        affectation.getLigne());
            }
        }
    }

    private void analyserCondition(NoeudAST condition) {
        if (condition.getEnfants().isEmpty()) {
            erreurSemantique("Condition vide", condition.getLigne());
            return;
        }

        // Vérifier l'expression de condition
        NoeudAST conditionExpr = condition.getEnfants().get(0);
        TypeDonnee typeCondition = analyserConditionExpression(conditionExpr);

        if (typeCondition != TypeDonnee.BOOLEEN) {
            erreurSemantique("Condition doit être de type booléen",
                    conditionExpr.getLigne());
        }

        // Analyser le bloc ALORS
        if (condition.getEnfants().size() > 1) {
            entrerNouvellePortee();
            analyserBlocInstructions(condition.getEnfants().get(1));
            sortirPortee();
        }

        // Analyser le bloc SINON s'il existe
        if (condition.getEnfants().size() > 2) {
            entrerNouvellePortee();
            analyserBlocInstructions(condition.getEnfants().get(2));
            sortirPortee();
        }
    }

    private TypeDonnee analyserConditionExpression(NoeudAST condition) {
        if (condition.getEnfants().size() < 2) {
            // C'est peut-être une variable booléenne simple
            if (condition.getType() == NoeudAST.TypeNoeud.VARIABLE) {
                TypeDonnee type = analyserVariable(condition);
                if (type == TypeDonnee.BOOLEEN) {
                    return TypeDonnee.BOOLEEN;
                } else {
                    erreurSemantique("Variable booléenne attendue dans la condition",
                            condition.getLigne());
                    return TypeDonnee.INCONNU;
                }
            } else if (condition.getType() == NoeudAST.TypeNoeud.VALEUR_BOOLEENNE) {
                return TypeDonnee.BOOLEEN;
            }
            erreurSemantique("Condition incomplète", condition.getLigne());
            return TypeDonnee.INCONNU;
        }

        NoeudAST gauche = condition.getEnfants().get(0);
        NoeudAST droite = condition.getEnfants().get(1);
        String operateur = condition.getValeur();

        TypeDonnee typeGauche = analyserExpression(gauche);
        TypeDonnee typeDroite = analyserExpression(droite);

        // Vérifier la compatibilité des types selon l'opérateur
        if (estOperateurComparaison(operateur)) {
            if (!typesCompatiblesComparaison(typeGauche, typeDroite)) {
                erreurSemantique("Types incompatibles dans la comparaison " + operateur,
                        condition.getLigne());
                return TypeDonnee.INCONNU;
            }
        } else if (estOperateurLogique(operateur)) {
            if (typeGauche != TypeDonnee.BOOLEEN || typeDroite != TypeDonnee.BOOLEEN) {
                erreurSemantique("Opérateur logique " + operateur + " nécessite des booléens",
                        condition.getLigne());
                return TypeDonnee.INCONNU;
            }
        }

        return TypeDonnee.BOOLEEN;
    }

    private boolean estOperateurComparaison(String operateur) {
        return operateur.equals("<") || operateur.equals("<=") ||
                operateur.equals(">") || operateur.equals(">=") ||
                operateur.equals("=") || operateur.equals("<>");
    }

    private boolean estOperateurLogique(String operateur) {
        return operateur.equals("ET") || operateur.equals("OU");
    }

    private void analyserBouclePour(NoeudAST boucle) {
        if (boucle.getEnfants().size() < 3) {
            erreurSemantique("Boucle POUR incomplète", boucle.getLigne());
            return;
        }

        String nomVariable = boucle.getValeur();
        NoeudAST debut = boucle.getEnfants().get(0);
        NoeudAST fin = boucle.getEnfants().get(1);
        NoeudAST corps = boucle.getEnfants().get(2);

        // Vérifier les expressions de début et fin
        TypeDonnee typeDebut = analyserExpression(debut);
        TypeDonnee typeFin = analyserExpression(fin);

        if (typeDebut != TypeDonnee.ENTIER && typeDebut != TypeDonnee.REEL) {
            erreurSemantique("Valeur de début doit être numérique", debut.getLigne());
        }

        if (typeFin != TypeDonnee.ENTIER && typeFin != TypeDonnee.REEL) {
            erreurSemantique("Valeur de fin doit être numérique", fin.getLigne());
        }

        // Vérifier la compatibilité des types
        if (typeDebut != typeFin) {
            avertissement("Types différents pour début et fin de boucle",
                    boucle.getLigne());
        }

        // Analyser le corps de la boucle dans une nouvelle portée
        entrerNouvellePortee();

        // Ajouter la variable de boucle à la portée locale
        EntreeTableSymboles entree = new EntreeTableSymboles(nomVariable, typeDebut,
                boucle.getLigne());
        entree.estConstante = true; // La variable de boucle ne doit pas être modifiée
        pilePortees.peek().put(nomVariable, entree);

        analyserBlocInstructions(corps);

        sortirPortee();
    }

    private void analyserBoucleTantque(NoeudAST boucle) {
        if (boucle.getEnfants().size() < 2) {
            erreurSemantique("Boucle TANTQUE incomplète", boucle.getLigne());
            return;
        }

        NoeudAST condition = boucle.getEnfants().get(0);
        NoeudAST corps = boucle.getEnfants().get(1);

        // Vérifier la condition
        TypeDonnee typeCondition = analyserConditionExpression(condition);
        if (typeCondition != TypeDonnee.BOOLEEN) {
            erreurSemantique("Condition doit être de type booléen", condition.getLigne());
        }

        // Analyser le corps
        entrerNouvellePortee();
        analyserBlocInstructions(corps);
        sortirPortee();
    }

    private void analyserBoucleRepeter(NoeudAST boucle) {
        if (boucle.getEnfants().size() < 2) {
            erreurSemantique("Boucle REPETER incomplète", boucle.getLigne());
            return;
        }

        NoeudAST corps = boucle.getEnfants().get(0);
        NoeudAST condition = boucle.getEnfants().get(1);

        // Analyser le corps
        entrerNouvellePortee();
        analyserBlocInstructions(corps);
        sortirPortee();

        // Vérifier la condition
        TypeDonnee typeCondition = analyserConditionExpression(condition);
        if (typeCondition != TypeDonnee.BOOLEEN) {
            erreurSemantique("Condition doit être de type booléen", condition.getLigne());
        }
    }

    private void analyserEcrire(NoeudAST ecrire) {
        if (ecrire.getEnfants().isEmpty()) {
            erreurSemantique("Expression manquante pour ECRIRE", ecrire.getLigne());
            return;
        }

        for (NoeudAST expression : ecrire.getEnfants()) {
            TypeDonnee type = analyserExpression(expression);

            // Vérifier que le type peut être affiché
            if (type == TypeDonnee.INCONNU) {
                erreurSemantique("Type non valide pour ECRIRE", expression.getLigne());
            }
        }
    }

    private void analyserLire(NoeudAST lire) {
        if (lire.getEnfants().isEmpty()) {
            erreurSemantique("Variable manquante pour LIRE", lire.getLigne());
            return;
        }

        for (NoeudAST variable : lire.getEnfants()) {
            String nomVariable = variable.getValeur();
            EntreeTableSymboles entree = chercherVariable(nomVariable);

            if (entree == null) {
                erreurSemantique("Variable non déclarée: " + nomVariable,
                        variable.getLigne());
                continue;
            }

            if (entree.estConstante) {
                erreurSemantique("Impossible de lire dans une constante: " +
                        nomVariable, variable.getLigne());
            }

            // Vérifier que le type est lisible
            if (entree.type == TypeDonnee.STRUCTURE || entree.type == TypeDonnee.TABLEAU) {
                erreurSemantique("Impossible de lire une structure ou un tableau directement: " +
                        nomVariable, variable.getLigne());
            }
        }
    }

    private void analyserAppelFonction(NoeudAST appel, boolean estInstruction) {
        String nomFonction = appel.getValeur();
        FonctionInfo info = fonctions.get(nomFonction);

        if (info == null) {
            erreurSemantique("Fonction non définie: " + nomFonction, appel.getLigne());
            return;
        }

        // Vérifier les arguments
        List<TypeDonnee> typesArguments = new ArrayList<>();

        if (!appel.getEnfants().isEmpty()) {
            NoeudAST argumentsNode = appel.getEnfants().get(0);
            for (NoeudAST arg : argumentsNode.getEnfants()) {
                TypeDonnee typeArg = analyserExpression(arg);
                typesArguments.add(typeArg);
            }
        }

        // Vérifier le nombre d'arguments
        if (typesArguments.size() != info.parametres.size()) {
            erreurSemantique("Nombre incorrect d'arguments pour " + nomFonction +
                    " (attendu: " + info.parametres.size() +
                    ", trouvé: " + typesArguments.size() + ")", appel.getLigne());
            return;
        }

        // Vérifier la compatibilité des types d'arguments
        for (int i = 0; i < typesArguments.size(); i++) {
            TypeDonnee typeParam = info.parametres.get(i);
            TypeDonnee typeArg = typesArguments.get(i);

            if (typeParam == TypeDonnee.TABLEAU) {
                // Pour un tableau, vérifier que l'argument est aussi un tableau
                if (typeArg != TypeDonnee.TABLEAU) {
                    erreurSemantique("Argument " + (i+1) + " doit être un tableau pour " +
                            nomFonction, appel.getLigne());
                }
            } else if (!typesCompatibles(typeParam, typeArg)) {
                erreurSemantique("Type d'argument incompatible pour le paramètre " + (i+1) +
                        " de " + nomFonction, appel.getLigne());
            }
        }
    }

    private TypeDonnee analyserExpression(NoeudAST expression) {
        switch (expression.getType()) {
            case NOMBRE:
                return determinerTypeNombre(expression.getValeur());

            case VARIABLE:
                return analyserVariable(expression);

            case VALEUR_BOOLEENNE:
                return TypeDonnee.BOOLEEN;

            case ACCES_TABLEAU:
                return analyserAccesTableau(expression);

            case EXPRESSION_BINAIRE:
                return analyserOperationBinaire(expression);

            case NEGATION:
                return analyserNegation(expression);

            case APPEL_FONCTION:
                return analyserAppelFonctionExpression(expression);

            case CONDITION_EXPRESSION:
                return analyserConditionExpression(expression);

            case CHAINE:
                return TypeDonnee.CHAINE;

            case ACCES_CHAMP:  // CORRECTION: Ajouter ce cas
                return analyserAccesChampExpression(expression);

            default:
                erreurSemantique("Expression non reconnue: " + expression.getType(),
                        expression.getLigne());
                return TypeDonnee.INCONNU;
        }
    }

    private TypeDonnee analyserAccesChampExpression(NoeudAST acces) {
        String nomStructure = acces.getValeur();
        EntreeTableSymboles entreeStructure = chercherVariable(nomStructure);

        if (entreeStructure == null) {
            erreurSemantique("Variable non déclarée: " + nomStructure, acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        if (entreeStructure.type != TypeDonnee.STRUCTURE) {
            erreurSemantique("Variable n'est pas une structure: " + nomStructure, acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        // Obtenir les informations de la structure
        StructureInfo info = structures.get(entreeStructure.nomStructure);
        if (info == null) {
            erreurSemantique("Structure non définie: " + entreeStructure.nomStructure,
                    acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        // Parcourir la chaîne d'accès aux champs
        String currentStructure = entreeStructure.nomStructure;
        TypeDonnee finalType = TypeDonnee.INCONNU;

        for (int i = 0; i < acces.getEnfants().size(); i++) {
            NoeudAST champNode = acces.getEnfants().get(i);
            String nomChamp = champNode.getValeur();

            StructureInfo currentInfo = structures.get(currentStructure);
            if (currentInfo == null) {
                erreurSemantique("Structure non définie: " + currentStructure,
                        champNode.getLigne());
                return TypeDonnee.INCONNU;
            }

            // Vérifier que le champ existe
            if (!currentInfo.champs.containsKey(nomChamp)) {
                erreurSemantique("Champ '" + nomChamp + "' non défini dans la structure '" +
                        currentStructure + "'", champNode.getLigne());
                return TypeDonnee.INCONNU;
            }

            TypeDonnee typeChamp = currentInfo.champs.get(nomChamp);

            // Si c'est le dernier champ de la chaîne
            if (i == acces.getEnfants().size() - 1) {
                finalType = typeChamp;

                // Si c'est un accès à un tableau
                if (champNode.getType() == NoeudAST.TypeNoeud.ACCES_TABLEAU) {
                    if (typeChamp != TypeDonnee.TABLEAU) {
                        erreurSemantique("Le champ '" + nomChamp + "' n'est pas un tableau",
                                champNode.getLigne());
                        return TypeDonnee.INCONNU;
                    }

                    // Vérifier l'indice
                    if (champNode.getEnfants().size() > 0) {
                        TypeDonnee typeIndice = analyserExpression(champNode.getEnfants().get(0));
                        if (typeIndice != TypeDonnee.ENTIER) {
                            erreurSemantique("Indice de tableau doit être entier",
                                    champNode.getLigne());
                        }
                    }

                    // Retourner le type d'élément du tableau
                    if (currentInfo.champsTableau.containsKey(nomChamp)) {
                        finalType = currentInfo.champsTableau.get(nomChamp);
                    }
                }
            } else {
                // Continuer vers la structure suivante dans la chaîne
                if (typeChamp != TypeDonnee.STRUCTURE) {
                    erreurSemantique("Le champ '" + nomChamp + "' n'est pas une structure",
                            champNode.getLigne());
                    return TypeDonnee.INCONNU;
                }

                String nextStructure = currentInfo.champsStructure.get(nomChamp);
                if (nextStructure == null) {
                    erreurSemantique("Structure non définie pour le champ '" + nomChamp + "'",
                            champNode.getLigne());
                    return TypeDonnee.INCONNU;
                }

                currentStructure = nextStructure;
            }
        }

        return finalType;
    }

    private TypeDonnee analyserVariable(NoeudAST variable) {
        String nom = variable.getValeur();
        EntreeTableSymboles entree = chercherVariable(nom);

        if (entree == null) {
            erreurSemantique("Variable non déclarée: " + nom, variable.getLigne());
            return TypeDonnee.INCONNU;
        }

        return entree.type;
    }

    private TypeDonnee analyserAccesTableau(NoeudAST acces) {
        String nomTableau = acces.getValeur();
        EntreeTableSymboles entree = chercherVariable(nomTableau);

        if (entree == null) {
            erreurSemantique("Variable non déclarée: " + nomTableau, acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        if (entree.type != TypeDonnee.TABLEAU) {
            erreurSemantique("Variable n'est pas un tableau: " + nomTableau,
                    acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        // Vérifier l'indice
        if (acces.getEnfants().size() > 0) {
            TypeDonnee typeIndice = analyserExpression(acces.getEnfants().get(0));
            if (typeIndice != TypeDonnee.ENTIER) {
                erreurSemantique("Indice de tableau doit être entier", acces.getLigne());
            }
        }

        // Retourner le type de l'élément du tableau
        return entree.typeElement;
    }

    private TypeDonnee analyserOperationBinaire(NoeudAST operation) {
        if (operation.getEnfants().size() < 2) {
            erreurSemantique("Opération binaire incomplète", operation.getLigne());
            return TypeDonnee.INCONNU;
        }

        NoeudAST gauche = operation.getEnfants().get(0);
        NoeudAST droite = operation.getEnfants().get(1);
        String operateur = operation.getValeur();

        TypeDonnee typeGauche = analyserExpression(gauche);
        TypeDonnee typeDroite = analyserExpression(droite);

        // Vérifier la compatibilité des types selon l'opérateur
        if (estOperateurArithmetique(operateur)) {
            if (!typesCompatiblesArithmetiques(typeGauche, typeDroite)) {
                erreurSemantique("Types incompatibles pour l'opération arithmétique " +
                        operateur, operation.getLigne());
                return TypeDonnee.INCONNU;
            }
            return determinerTypeResultat(operateur, typeGauche, typeDroite);
        } else if (estOperateurComparaison(operateur)) {
            if (!typesCompatiblesComparaison(typeGauche, typeDroite)) {
                erreurSemantique("Types incompatibles pour la comparaison " + operateur,
                        operation.getLigne());
                return TypeDonnee.INCONNU;
            }
            return TypeDonnee.BOOLEEN;
        } else if (estOperateurLogique(operateur)) {
            if (typeGauche != TypeDonnee.BOOLEEN || typeDroite != TypeDonnee.BOOLEEN) {
                erreurSemantique("Opérateur logique " + operateur + " nécessite des booléens",
                        operation.getLigne());
                return TypeDonnee.INCONNU;
            }
            return TypeDonnee.BOOLEEN;
        }

        // Déterminer le type du résultat
        return determinerTypeResultat(operateur, typeGauche, typeDroite);
    }

    private boolean estOperateurArithmetique(String operateur) {
        return operateur.equals("+") || operateur.equals("-") ||
                operateur.equals("*") || operateur.equals("/");
    }

    private TypeDonnee analyserNegation(NoeudAST negation) {
        if (negation.getEnfants().isEmpty()) {
            erreurSemantique("Expression manquante pour la négation", negation.getLigne());
            return TypeDonnee.INCONNU;
        }

        NoeudAST expression = negation.getEnfants().get(0);
        TypeDonnee type = analyserExpression(expression);

        if (negation.getValeur().equals("NON")) {
            // Négation logique
            if (type != TypeDonnee.BOOLEEN) {
                erreurSemantique("Négation logique applicable seulement aux booléens",
                        negation.getLigne());
                return TypeDonnee.INCONNU;
            }
            return TypeDonnee.BOOLEEN;
        } else {
            // Négation numérique
            if (type != TypeDonnee.ENTIER && type != TypeDonnee.REEL) {
                erreurSemantique("Négation numérique applicable seulement aux types numériques",
                        negation.getLigne());
                return TypeDonnee.INCONNU;
            }
            return type;
        }
    }

    private TypeDonnee analyserAppelFonctionExpression(NoeudAST appel) {
        String nomFonction = appel.getValeur();
        FonctionInfo info = fonctions.get(nomFonction);

        if (info == null) {
            erreurSemantique("Fonction non définie: " + nomFonction, appel.getLigne());
            return TypeDonnee.INCONNU;
        }

        analyserAppelFonction(appel, false);

        return info.typeRetour;
    }

    // Méthodes utilitaires
    private TypeDonnee convertirType(String typeStr) {
        switch (typeStr.toLowerCase()) {
            case "entier":
                return TypeDonnee.ENTIER;
            case "reel":
                return TypeDonnee.REEL;
            case "chainedecharactere":
            case "chaine":
                return TypeDonnee.CHAINE;
            case "booleen":
                return TypeDonnee.BOOLEEN;
            default:
                if (structures.containsKey(typeStr)) {
                    return TypeDonnee.STRUCTURE;
                }
                return TypeDonnee.INCONNU;
        }
    }

    private TypeDonnee determinerTypeNombre(String valeur) {
        // Supprimer les espaces et vérifier le signe
        String val = valeur.trim();
        if (val.startsWith("-") || val.startsWith("+")) {
            val = val.substring(1);
        }

        if (val.contains(".") || val.contains(",")) {
            return TypeDonnee.REEL;
        } else {
            return TypeDonnee.ENTIER;
        }
    }

    private boolean typesCompatibles(TypeDonnee type1, TypeDonnee type2) {
        if (type1 == type2) {
            return true;
        }

        // Tableaux sont compatibles entre eux
        if (type1 == TypeDonnee.TABLEAU && type2 == TypeDonnee.TABLEAU) {
            return true;
        }

        // Conversions implicites entre numériques
        if ((type1 == TypeDonnee.ENTIER && type2 == TypeDonnee.REEL) ||
                (type1 == TypeDonnee.REEL && type2 == TypeDonnee.ENTIER)) {
            return true;
        }

        return false;
    }

    private boolean typesCompatiblesArithmetiques(TypeDonnee type1, TypeDonnee type2) {
        if (type1 == type2 &&
                (type1 == TypeDonnee.ENTIER || type1 == TypeDonnee.REEL ||
                        type1 == TypeDonnee.CHAINE)) return true;

        // Conversions implicites entre numérique
        if ((type1 == TypeDonnee.ENTIER && type2 == TypeDonnee.REEL) ||
                (type1 == TypeDonnee.REEL && type2 == TypeDonnee.ENTIER)) return true;

        return false;
    }

    private boolean typesCompatiblesComparaison(TypeDonnee type1, TypeDonnee type2) {
        if (type1 == type2) return true;

        // Comparaisons numériques
        if ((type1 == TypeDonnee.ENTIER || type1 == TypeDonnee.REEL) &&
                (type2 == TypeDonnee.ENTIER || type2 == TypeDonnee.REEL)) return true;

        return false;
    }

    private TypeDonnee determinerTypeResultat(String operateur, TypeDonnee type1, TypeDonnee type2) {
        // Pour les opérations arithmétiques
        if (operateur.equals("+") || operateur.equals("-") ||
                operateur.equals("*") || operateur.equals("/")) {
            if (type1 == TypeDonnee.CHAINE && operateur.equals("+")) {
                return TypeDonnee.CHAINE; // Concaténation
            }
            if (type1 == TypeDonnee.REEL || type2 == TypeDonnee.REEL) {
                return TypeDonnee.REEL;
            }
            return TypeDonnee.ENTIER;
        }

        // Pour les comparaisons et opérateurs logiques
        return TypeDonnee.BOOLEEN;
    }

    private EntreeTableSymboles chercherVariable(String nom) {
        // Chercher d'abord dans les portées locales (de la plus récente à la plus ancienne)
        for (int i = pilePortees.size() - 1; i >= 0; i--) {
            Map<String, EntreeTableSymboles> portee = pilePortees.get(i);
            if (portee.containsKey(nom)) {
                return portee.get(nom);
            }
        }

        // Chercher dans les variables globales
        if (tableSymbolesGlobale.containsKey(nom)) {
            return tableSymbolesGlobale.get(nom);
        }

        return null;
    }

    private void entrerNouvellePortee() {
        pilePortees.push(new HashMap<>());
    }

    private void sortirPortee() {
        if (!pilePortees.isEmpty()) {
            pilePortees.pop();
        }
    }

    private void verifierCompatibiliteTypes(TypeDonnee attendu, TypeDonnee trouve,
                                            String message, int ligne) {
        if (!typesCompatibles(attendu, trouve)) {
            erreurSemantique(message + " (attendu: " + attendu +
                    ", trouvé: " + trouve + ")", ligne);
        }
    }


    private void erreurSemantique(String message, int ligne) {
        String erreur = String.format("Erreur sémantique ligne %d: %s", ligne, message);
        erreursSemantiques.add(erreur);
        System.err.println(" ERREUR SÉMANTIQUE");
        System.err.println("   Ligne " + ligne + ": " + message);
    }

    private void avertissement(String message, int ligne) {
        String avert = String.format("Avertissement ligne %d: %s", ligne, message);
        avertissements.add(avert);
        System.out.println(" AVERTISSEMENT");
        System.out.println("   Ligne " + ligne + ": " + message);
    }


    public boolean aErreurs() {
        return !erreursSemantiques.isEmpty();
    }

    public void afficherResultats() {
        if (aErreurs()) {
            System.err.println("\n=== ERREURS SÉMANTIQUES ===");
            for (String erreur : erreursSemantiques) {
                System.err.println(erreur);
            }
        } else {
            System.out.println("\n=== ANALYSE SÉMANTIQUE TERMINÉE AVEC SUCCÈS ===");
        }

        if (!avertissements.isEmpty()) {
            System.out.println("\n=== AVERTISSEMENTS ===");
            for (String avert : avertissements) {
                System.out.println(avert);
            }
        }

        // Afficher la table des symboles
        System.out.println("\n=== TABLE DES SYMBOLES GLOBAUX ===");
        for (EntreeTableSymboles entree : tableSymbolesGlobale.values()) {
            System.out.println("  " + entree.nom + " : " + entree.type +
                    " (ligne " + entree.ligneDeclaration + ")");
        }
    }
}