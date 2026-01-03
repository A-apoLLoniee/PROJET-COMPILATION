// AnalyseurSemantique.java
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
    Object valeurInitiale; // Pour les constantes

    public EntreeTableSymboles(String nom, TypeDonnee type, int ligne) {
        this.nom = nom;
        this.type = type;
        this.ligneDeclaration = ligne;
        this.estConstante = false;
        this.estTableau = false;
        this.parametres = new ArrayList<>();
        this.valeurInitiale = null;
    }
}

class StructureInfo {
    String nom;
    Map<String, TypeDonnee> champs;
    Map<String, String> champsStructure;

    public StructureInfo(String nom) {
        this.nom = nom;
        this.champs = new HashMap<>();
        this.champsStructure = new HashMap<>();
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

        for (NoeudAST enfant : declaration.getEnfants()) {
            switch (enfant.getType()) {
                case LISTE_IDENTIFICATEURS:
                    for (NoeudAST id : enfant.getEnfants()) {
                        nomsVariables.add(id.getValeur());
                    }
                    break;

                case TYPE:
                    typeChamp = convertirType(enfant.getValeur());
                    if (typeChamp == TypeDonnee.STRUCTURE) {
                        nomStructureChamp = enfant.getValeur();
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

        // Analyser les paramètres et le type de retour
        for (int i = 0; i < fonction.getEnfants().size(); i++) {
            NoeudAST enfant = fonction.getEnfant(i);

            switch (enfant.getType()) {
                case LISTE_PARAMETRES:
                    analyserParametresFonction(enfant, parametres, nomsParametres);
                    break;

                case TYPE:
                    // Premier type est le type de retour
                    if (typeRetour == TypeDonnee.INCONNU) {
                        typeRetour = convertirType(enfant.getValeur());
                    }
                    break;

                case SECTION_VAR:
                    // Variables locales de la fonction
                    analyserSectionVariable(enfant, false);
                    break;
            }
        }

        FonctionInfo info = new FonctionInfo(nomFonction, typeRetour);
        info.parametres = parametres;
        fonctions.put(nomFonction, info);

        // Sauvegarder le contexte
        String fonctionPrecedente = fonctionCourante;
        TypeDonnee retourPrecedent = typeRetourAttendu;

        fonctionCourante = nomFonction;
        typeRetourAttendu = typeRetour;

        // Entrer dans une nouvelle portée pour la fonction
        entrerNouvellePortee();

        // Ajouter les paramètres à la table des symboles locale
        for (int i = 0; i < nomsParametres.size(); i++) {
            String nomParam = nomsParametres.get(i);
            TypeDonnee typeParam = parametres.get(i);

            EntreeTableSymboles entree = new EntreeTableSymboles(nomParam, typeParam,
                    fonction.getLigne());
            pilePortees.peek().put(nomParam, entree);
        }

        // Analyser le corps de la fonction
        // CORRECTION : Vérifier la présence d'une expression de retour
        boolean aRetour = false;

        // Analyser le corps de la fonction
        for (NoeudAST enfant : fonction.getEnfants()) {
            switch (enfant.getType()) {
                case BLOC_INSTRUCTIONS:
                    analyserBlocInstructions(enfant);
                    break;

                case EXPRESSION_BINAIRE:
                case NOMBRE:
                case VARIABLE:
                case APPEL_FONCTION:
                case VALEUR_BOOLEENNE:
                case CHAINE:
                case NEGATION:
                case ACCES_TABLEAU:
                    // C'est l'expression de retour - dernière expression non-bloc
                    aRetour = true;
                    TypeDonnee typeRetourTrouve = analyserExpression(enfant);
                    verifierCompatibiliteTypes(typeRetourAttendu, typeRetourTrouve,
                            "Type de retour incorrect", enfant.getLigne());
                    break;
            }
        }

        // Vérifier qu'il y a bien un retour si la fonction a un type de retour
        if (typeRetour != TypeDonnee.INCONNU && !aRetour) {
            erreurSemantique("La fonction doit contenir un RETOUR", fonction.getLigne());
        }

        // Restaurer le contexte
        sortirPortee();
        fonctionCourante = fonctionPrecedente;
        typeRetourAttendu = retourPrecedent;
    }

    private void analyserParametresFonction(NoeudAST parametresNode,
                                            List<TypeDonnee> types,
                                            List<String> noms) {
        for (NoeudAST param : parametresNode.getEnfants()) {
            if (param.getType() == NoeudAST.TypeNoeud.PARAMETRE) {
                String nomParam = param.getValeur();
                TypeDonnee typeParam = TypeDonnee.INCONNU;

                for (NoeudAST enfantParam : param.getEnfants()) {
                    if (enfantParam.getType() == NoeudAST.TypeNoeud.TYPE) {
                        typeParam = convertirType(enfantParam.getValeur());
                    }
                }

                if (typeParam != TypeDonnee.INCONNU) {
                    types.add(typeParam);
                    noms.add(nomParam);
                }
            }
        }
    }

    // Méthode utilitaire pour convertir un type simple (sans tableau)
    private TypeDonnee convertirTypeSimple(String typeStr) {
        switch (typeStr.toLowerCase()) {
            case "entier":
                return TypeDonnee.ENTIER;
            case "reel":
                return TypeDonnee.REEL;
            case "chainedecaractere":
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

    private void analyserDeclarationVariable(NoeudAST declaration, boolean globale) {
        List<String> nomsVariables = new ArrayList<>();
        TypeDonnee typeVariable = TypeDonnee.INCONNU;
        String nomStructure = null;
        boolean estTableau = false;  // AJOUT
        TypeDonnee typeElementTableau = TypeDonnee.INCONNU;  // AJOUT

        for (NoeudAST enfant : declaration.getEnfants()) {
            switch (enfant.getType()) {
                case LISTE_IDENTIFICATEURS:
                    for (NoeudAST id : enfant.getEnfants()) {
                        nomsVariables.add(id.getValeur());
                    }
                    break;

                case TYPE:
                    String typeStr = enfant.getValeur();

                    // CORRECTION : Gérer les tableaux (ex: "entier[10]")
                    if (typeStr.contains("[")) {
                        estTableau = true;
                        // Extraire le type de base avant le '['
                        String typeBase = typeStr.substring(0, typeStr.indexOf('[')).trim();
                        typeElementTableau = convertirTypeSimple(typeBase);
                        typeVariable = TypeDonnee.TABLEAU;
                    } else {
                        typeVariable = convertirType(typeStr);
                        if (typeVariable == TypeDonnee.STRUCTURE) {
                            nomStructure = typeStr;
                            if (!structures.containsKey(nomStructure)) {
                                erreurSemantique("Structure non définie: " + nomStructure,
                                        declaration.getLigne());
                            }
                        }
                    }
                    break;
            }
        }

        if (typeVariable == TypeDonnee.INCONNU) {
            erreurSemantique("Type de variable invalide", declaration.getLigne());
            return;
        }

        Map<String, EntreeTableSymboles> tableCourante =
                globale ? tableSymbolesGlobale : pilePortees.peek();

        for (String nomVariable : nomsVariables) {
            if (tableCourante.containsKey(nomVariable)) {
                erreurSemantique("Variable déjà déclarée: " + nomVariable,
                        declaration.getLigne());
            } else {
                EntreeTableSymboles entree = new EntreeTableSymboles(nomVariable,
                        typeVariable,
                        declaration.getLigne());
                if (nomStructure != null) {
                    entree.nomStructure = nomStructure;
                }
                if (estTableau) {
                    entree.estTableau = true;
                    entree.typeElement = typeElementTableau;
                }
                tableCourante.put(nomVariable, entree);

                // Ajouter aussi à la table globale pour recherche facile
                if (globale) {
                    tableSymbolesGlobale.put(nomVariable, entree);
                }
            }
        }
    }

    private void analyserBlocInstructions(NoeudAST bloc) {
        entrerNouvellePortee();
        for (NoeudAST instruction : bloc.getEnfants()) {
            analyserInstruction(instruction);
        }
        sortirPortee();
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

            default:
                erreurSemantique("Instruction non reconnue: " + instruction.getType(),
                        instruction.getLigne());
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

        // Cas spécial pour l'accès tableau
        if (affectation.getEnfants().get(0).getType() == NoeudAST.TypeNoeud.ACCES_TABLEAU) {
            NoeudAST acces = affectation.getEnfants().get(0);
            NoeudAST expression = affectation.getEnfants().get(1);

            // Vérifier l'indice
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
        } else {
            NoeudAST expression = affectation.getEnfants().get(0);
            TypeDonnee typeExpression = analyserExpression(expression);

            verifierCompatibiliteTypes(entree.type, typeExpression,
                    "Types incompatibles dans l'affectation",
                    affectation.getLigne());
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
            if (!typesCompatibles(info.parametres.get(i), typesArguments.get(i))) {
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

            default:
                erreurSemantique("Expression non reconnue: " + expression.getType(),
                        expression.getLigne());
                return TypeDonnee.INCONNU;
        }
    }

    private TypeDonnee analyserVariable(NoeudAST variable) {
        String nom = variable.getValeur();

        // Vérifier si c'est une constante booléenne
        if (nom.equalsIgnoreCase("vrai") || nom.equalsIgnoreCase("faux")) {
            return TypeDonnee.BOOLEEN;
        }

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
            erreurSemantique("Tableau non déclaré: " + nomTableau, acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        if (!entree.estTableau) {
            erreurSemantique("Variable n'est pas un tableau: " + nomTableau,
                    acces.getLigne());
            return TypeDonnee.INCONNU;
        }

        // Vérifier l'indice
        if (!acces.getEnfants().isEmpty()) {
            NoeudAST indice = acces.getEnfants().get(0);
            TypeDonnee typeIndice = analyserExpression(indice);

            if (typeIndice != TypeDonnee.ENTIER) {
                erreurSemantique("Indice de tableau doit être entier", indice.getLigne());
            }
        }

        return entree.typeElement;
    }

    private TypeDonnee analyserOperationBinaire(NoeudAST operation) {
        if (operation.getEnfants().size() < 2) {
            erreurSemantique("Opération binaire incomplète", operation.getLigne());
            return TypeDonnee.INCONNU;
        }

        String operateur = operation.getValeur();
        NoeudAST gauche = operation.getEnfants().get(0);
        NoeudAST droite = operation.getEnfants().get(1);

        TypeDonnee typeGauche = analyserExpression(gauche);
        TypeDonnee typeDroite = analyserExpression(droite);

        // Vérifier la compatibilité des types selon l'opérateur
        if (estOperateurArithmetique(operateur)) {
            if (!typesCompatiblesArithmetiques(typeGauche, typeDroite)) {
                erreurSemantique("Types incompatibles pour l'opération arithmétique " + operateur,
                        operation.getLigne());
                return TypeDonnee.INCONNU;
            }
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
        return typesCompatiblesArithmetiques(type1, type2) ||
                typesCompatiblesComparaison(type1, type2);
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

    private void verifierRetourFonction(NoeudAST fonction) {
        // Vérifie si la fonction a un retour explicite
        boolean aRetourExplicite = false;

        for (NoeudAST enfant : fonction.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.RETOUR) {
                aRetourExplicite = true;
                break;
            }

            if (enfant.getType() == NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS) {
                for (NoeudAST instruction : enfant.getEnfants()) {
                    if (instruction.getType() == NoeudAST.TypeNoeud.RETOUR) {
                        aRetourExplicite = true;
                        break;
                    }
                }
            }
        }

        if (!aRetourExplicite) {
            erreurSemantique("La fonction doit contenir un RETOUR",
                    fonction.getLigne());
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

    // Getters
    public List<String> getErreursSemantiques() {
        return erreursSemantiques;
    }

    public List<String> getAvertissements() {
        return avertissements;
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