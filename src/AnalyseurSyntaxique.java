// AnalyseurSyntaxique.java
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalyseurSyntaxique {
    private analyseurLexical analyseur;
    private SymboleCourant tokenCourant;
    private NoeudAST arbreSyntaxique;
    private List<String> erreursSyntaxiques;

    public AnalyseurSyntaxique(analyseurLexical analyseur) throws IOException {
        this.analyseur = analyseur;
        // Lire le premier token
        analyseur.symboleSuivant();
        this.tokenCourant = analyseur.getSymboleCourant();
        this.arbreSyntaxique = new NoeudAST(NoeudAST.TypeNoeud.PROGRAMME);
        this.erreursSyntaxiques = new ArrayList<>();
    }

    public void analyser() throws IOException {
        programme();
        if (tokenCourant.code != TokenType.EOF_TOKEN) {
            erreurSyntaxique("Fin de fichier attendue");
        }
    }

    private void avancer() throws IOException {
        analyseur.symboleSuivant();
        tokenCourant = analyseur.getSymboleCourant();
    }

    private boolean consommer(TokenType typeAttendu) throws IOException {
        if (tokenCourant.code == typeAttendu) {
            avancer();
            return true;
        }
        return false;
    }

    private boolean verifier(TokenType type) {
        return tokenCourant.code == type;
    }

    private void erreurSyntaxique(String message) {
        String erreur = String.format("Erreur syntaxique ligne %d: %s (Token: '%s')",
                tokenCourant.ligne, message, tokenCourant.nom);
        erreursSyntaxiques.add(erreur);
        System.err.println(" ERREUR SYNTAXIQUE");
        System.err.println("   Ligne " + tokenCourant.ligne + ": " + message);
        System.err.println("   Token actuel: " + tokenCourant.nom);
    }

    // Règle: PROGRAMME ::= DIRECTIVE_LANGAGE ALGORITHME DECLARATIONS DEBUT INSTRUCTIONS FIN
    private void programme() throws IOException {
        // Directive de langage OBLIGATOIRE
        if (verifier(TokenType.LANGAGE_TOKEN)) {
            NoeudAST directive = new NoeudAST(NoeudAST.TypeNoeud.DIRECTIVE_LANGAGE,
                    tokenCourant.nom, tokenCourant.ligne);
            arbreSyntaxique.ajouterEnfant(directive);
            avancer();
        } else {
            erreurSyntaxique("Directive de langage OBLIGATOIRE manquante (ex: #PYTHON, #JAVA, #C)");
            System.err.println("ERREUR CRITIQUE : DIRECTIVE DE LANGAGE MANQUANTE");
            System.err.println("\nVotre algorithme DOIT commencer par une directive de langage.");
            System.err.println("\nExemples de directives valides :");
            System.err.println("  • #PYTHON  → Pour générer du code Python");
            System.err.println("  • #JAVA    → Pour générer du code Java");
            System.err.println("  • #C       → Pour générer du code C");
            System.err.println("\nFormat attendu de votre fichier :");
            System.err.println("┌─────────────────────────────────");
            System.err.println("│ #PYTHON");
            System.err.println("│ algorithme MonAlgorithme");
            System.err.println("│ var");
            System.err.println("│   x : entier;");
            System.err.println("│ debut");
            System.err.println("│   ...");
            System.err.println("│ fin");
            System.err.println("└─────────────────────────────────");
            System.err.println("\n⚠️  Ajoutez la directive au début de votre fichier et réessayez.\n");
            return;
        }

        // ALGORITHME [nom]
        if (verifier(TokenType.ALGORITHME_TOKEN)) {
            avancer(); // ALGORITHME

            if (!verifier(TokenType.ID_TOKEN)) {
                erreurSyntaxique("Nom d'algorithme attendu");
            } else {
                arbreSyntaxique.setValeur(tokenCourant.nom); // Stocker le nom de l'algorithme
                avancer(); // nom de l'algorithme
            }
        } else {
            erreurSyntaxique("'ALGORITHME' attendu");
        }

        // Déclarations
        declarations();

        // DEBUT
        if (!consommer(TokenType.DEBUT_TOKEN)) {
            erreurSyntaxique("'DEBUT' attendu");
        }

        // Instructions
        NoeudAST instructions = blocInstructions();
        if (instructions != null) {
            arbreSyntaxique.ajouterEnfant(instructions);
        }

        // FIN
        if (!consommer(TokenType.FIN_TOKEN)) {
            erreurSyntaxique("'FIN' attendu");
        }
    }

    // Règle: DECLARATIONS ::= (DECL_STRUCTURE | DECL_FONCTION | SECTION_VAR)*
    private void declarations() throws IOException {
        while (verifier(TokenType.STRUCT_TOKEN) ||
                verifier(TokenType.FONCTION_TOKEN) ||
                verifier(TokenType.VAR_TOKEN) ||
                verifier(TokenType.PROCEDURE_TOKEN)) {

            if (verifier(TokenType.STRUCT_TOKEN)) {
                declarationStructure();
            } else if (verifier(TokenType.FONCTION_TOKEN)) {
                declarationFonction();
            } else if (verifier(TokenType.VAR_TOKEN)) {
                sectionVar();
            }
            else if (verifier(TokenType.PROCEDURE_TOKEN)) {
                declarationProcedure();
            }
        }
    }

    // Règle: SECTION_VAR ::= VAR (DECL_VARIABLE)+
    private void sectionVar() throws IOException {
        NoeudAST sectionVar = new NoeudAST(NoeudAST.TypeNoeud.SECTION_VAR,
                "", tokenCourant.ligne);
        avancer(); // VAR

        // Lire au moins une déclaration de variable
        boolean auMoinsUneDecl = false;

        do {
            if (verifier(TokenType.ID_TOKEN)) {
                declarationVariableSansVar(sectionVar);
                auMoinsUneDecl = true;

                // Vérifier s'il y a une autre déclaration
                if (!verifier(TokenType.ID_TOKEN)) {
                    break;
                }
            } else {
                if (!auMoinsUneDecl) {
                    erreurSyntaxique("Déclaration de variable attendue après VAR");
                }
                break;
            }
        } while (true);

        arbreSyntaxique.ajouterEnfant(sectionVar);
    }

    // Règle: DECL_VARIABLE ::= LISTE_IDENTIFICATEURS : TYPE ;
    private void declarationVariableSansVar(NoeudAST parent) throws IOException {
        NoeudAST decl = new NoeudAST(NoeudAST.TypeNoeud.DECLARATION_VARIABLE,
                "", tokenCourant.ligne);

        // Liste d'identificateurs
        NoeudAST listeId = listeIdentificateurs();
        decl.ajouterEnfant(listeId);

        if (!consommer(TokenType.DP_TOKEN)) {
            erreurSyntaxique("':' attendu");
        }

        // Type
        NoeudAST type = typeDeclaration();
        if (type != null) {
            decl.ajouterEnfant(type);
        }

        if (!consommer(TokenType.PV_TOKEN)) {
            erreurSyntaxique("';' attendu");
        }

        parent.ajouterEnfant(decl);
    }

    // Règle: DECL_STRUCTURE ::= STRUCTURE IDENTIFICATEUR CHAMPS_STRUCTURE FINSTRUCTURE
    private void declarationStructure() throws IOException {
        NoeudAST structure = new NoeudAST(NoeudAST.TypeNoeud.DECLARATION_STRUCTURE,
                "", tokenCourant.ligne);
        avancer(); // STRUCTURE

        if (!verifier(TokenType.ID_TOKEN)) {
            erreurSyntaxique("Nom de structure attendu");
            return;
        }

        structure.setValeur(tokenCourant.nom);
        avancer();

        // Champs de la structure (sans VAR)
        while (verifier(TokenType.ID_TOKEN)) {
            declarationVariableStructure(structure);
        }

        if (!consommer(TokenType.FINSTRUCT_TOKEN)) {
            erreurSyntaxique("'FINSTRUCTURE' attendu");
        }

        arbreSyntaxique.ajouterEnfant(structure);
    }

    private void declarationVariableStructure(NoeudAST parent) throws IOException {
        NoeudAST decl = new NoeudAST(NoeudAST.TypeNoeud.CHAMP_STRUCTURE,
                "", tokenCourant.ligne);

        // Liste d'identificateurs
        NoeudAST listeId = listeIdentificateurs();
        decl.ajouterEnfant(listeId);

        if (!consommer(TokenType.DP_TOKEN)) {
            erreurSyntaxique("':' attendu");
        }

        // Type
        NoeudAST type = typeDeclaration();
        if (type != null) {
            decl.ajouterEnfant(type);
        }

        if (!consommer(TokenType.PV_TOKEN)) {
            erreurSyntaxique("';' attendu");
        }

        parent.ajouterEnfant(decl);
    }

    // Règle: DECL_FONCTION ::= FONCTION IDENTIFICATEUR ( [PARAMETRES] ) : TYPE
//                          [SECTION_VAR] DEBUT INSTRUCTIONS RETOUR [EXPRESSION] FINFONCTION
    private void declarationFonction() throws IOException {
        NoeudAST fonction = new NoeudAST(NoeudAST.TypeNoeud.DECLARATION_FONCTION,
                "", tokenCourant.ligne);
        avancer(); // FONCTION

        if (!verifier(TokenType.ID_TOKEN)) {
            erreurSyntaxique("Nom de fonction attendu");
            return;
        }

        fonction.setValeur(tokenCourant.nom);
        avancer();

        // Paramètres
        if (!consommer(TokenType.PO_TOKEN)) {
            erreurSyntaxique("'(' attendu");
        }

        if (!verifier(TokenType.PF_TOKEN)) {
            NoeudAST parametres = parametresDeclaration();
            fonction.ajouterEnfant(parametres);
        }

        if (!consommer(TokenType.PF_TOKEN)) {
            erreurSyntaxique("')' attendu");
        }

        if (!consommer(TokenType.DP_TOKEN)) {
            erreurSyntaxique("':' attendu");
        }

        // Type de retour
        NoeudAST typeRetour = typeDeclaration();
        if (typeRetour != null) {
            fonction.ajouterEnfant(typeRetour);
        }

        // Section VAR locale (optionnelle) - CORRECTION ICI
        if (verifier(TokenType.VAR_TOKEN)) {
            sectionVarLocale(fonction);
        }

        if (!consommer(TokenType.DEBUT_TOKEN)) {
            erreurSyntaxique("'DEBUT' attendu");
        }

        // Instructions jusqu'à RETOUR
        NoeudAST corps = blocInstructionsFonction();
        if (corps != null) {
            fonction.ajouterEnfant(corps);
        }

        if (!consommer(TokenType.RETOUR_TOKEN)) {
            erreurSyntaxique("'RETOUR' attendu dans la fonction");
        } else {
            // Expression de retour OBLIGATOIRE pour une fonction
            NoeudAST exprRetour = expression();
            if (exprRetour == null) {
                erreurSyntaxique("Expression de retour attendue après RETOUR");
            } else {
                // Créer un nœud RETOUR qui contient l'expression
                NoeudAST retourNode = new NoeudAST(NoeudAST.TypeNoeud.RETOUR, "", tokenCourant.ligne);
                retourNode.ajouterEnfant(exprRetour);
                fonction.ajouterEnfant(retourNode);
            }
        }

        if (!consommer(TokenType.FINFONCTION_TOKEN)) {
            erreurSyntaxique("'FinFonction' attendu");
        }

        arbreSyntaxique.ajouterEnfant(fonction);
    }

    private NoeudAST blocInstructionsFonction() throws IOException {
        NoeudAST bloc = new NoeudAST(NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS,
                "", tokenCourant.ligne);

        // Lire les instructions jusqu'à RETOUR
        while (!verifier(TokenType.RETOUR_TOKEN) &&
                !verifier(TokenType.FINFONCTION_TOKEN) &&
                !verifier(TokenType.EOF_TOKEN)) {

            NoeudAST instr = instruction();
            if (instr != null) {
                bloc.ajouterEnfant(instr);
            }

            // Consommer le point-virgule si présent
            if (verifier(TokenType.PV_TOKEN)) {
                avancer();
            }

            // Si on tombe sur RETOUR, sortir
            if (verifier(TokenType.RETOUR_TOKEN)) {
                break;
            }
        }

        return bloc.getEnfants().isEmpty() ? null : bloc;
    }

    // SECTION_VAR dans une fonction
    private void sectionVarLocale(NoeudAST parent) throws IOException {
        NoeudAST sectionVar = new NoeudAST(NoeudAST.TypeNoeud.SECTION_VAR,
                "", tokenCourant.ligne);
        avancer(); // VAR

        // Lire au moins une déclaration de variable
        boolean auMoinsUneDecl = false;

        do {
            if (verifier(TokenType.ID_TOKEN)) {
                declarationVariableSansVar(sectionVar);
                auMoinsUneDecl = true;

                // Vérifier s'il y a une autre déclaration
                if (!verifier(TokenType.ID_TOKEN)) {
                    break;
                }
            } else {
                if (!auMoinsUneDecl) {
                    erreurSyntaxique("Déclaration de variable attendue après VAR");
                }
                break;
            }
        } while (true);

        parent.ajouterEnfant(sectionVar);
    }

    // Règle: PARAMETRES ::= PARAMETRE ( , PARAMETRE )*
    private NoeudAST parametresDeclaration() throws IOException {
        NoeudAST listeParams = new NoeudAST(NoeudAST.TypeNoeud.LISTE_PARAMETRES,
                "", tokenCourant.ligne);

        do {
            if (!verifier(TokenType.ID_TOKEN)) {
                erreurSyntaxique("Identificateur de paramètre attendu");
                return listeParams;
            }

            NoeudAST param = new NoeudAST(NoeudAST.TypeNoeud.PARAMETRE,
                    tokenCourant.nom, tokenCourant.ligne);
            avancer();

            if (!consommer(TokenType.DP_TOKEN)) {
                erreurSyntaxique("':' attendu");
            }

            NoeudAST type = typeDeclaration();
            if (type != null) {
                param.ajouterEnfant(type);
            }

            listeParams.ajouterEnfant(param);

            if (!consommer(TokenType.VIR_TOKEN)) {
                break;
            }
        } while (true);

        return listeParams;
    }

    private void declarationProcedure() throws IOException {
        NoeudAST procedure = new NoeudAST(NoeudAST.TypeNoeud.DECLARATION_PROCEDURE,
                "", tokenCourant.ligne);
        avancer(); // PROCEDURE

        if (!verifier(TokenType.ID_TOKEN)) {
            erreurSyntaxique("Nom de procédure attendu");
            return;
        }

        procedure.setValeur(tokenCourant.nom);
        avancer();

        // Paramètres
        if (!consommer(TokenType.PO_TOKEN)) {
            erreurSyntaxique("'(' attendu");
        }

        if (!verifier(TokenType.PF_TOKEN)) {
            NoeudAST parametres = parametresDeclaration();
            procedure.ajouterEnfant(parametres);
        }

        if (!consommer(TokenType.PF_TOKEN)) {
            erreurSyntaxique("')' attendu");
        }

        // PAS de type de retour pour une procédure!

        // Section VAR locale (optionnelle)
        if (verifier(TokenType.VAR_TOKEN)) {
            sectionVarLocale(procedure);
        }

        if (!consommer(TokenType.DEBUT_TOKEN)) {
            erreurSyntaxique("'DEBUT' attendu");
        }

        // Instructions (PAS de RETOUR)
        NoeudAST corps = blocInstructionsProcedure();  // NOUVELLE MÉTHODE
        if (corps != null) {
            procedure.ajouterEnfant(corps);
        }

        if (!consommer(TokenType.FINPROCEDURE_TOKEN)) {
            erreurSyntaxique("'FinProcedure' attendu");
        }

        arbreSyntaxique.ajouterEnfant(procedure);
    }

    private NoeudAST blocInstructionsProcedure() throws IOException {
        NoeudAST bloc = new NoeudAST(NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS,
                "", tokenCourant.ligne);

        // Lire les instructions jusqu'à FINPROCEDURE
        while (!verifier(TokenType.FINPROCEDURE_TOKEN) &&
                !verifier(TokenType.EOF_TOKEN)) {

            NoeudAST instr = instruction();
            if (instr != null) {
                bloc.ajouterEnfant(instr);
            }

            if (verifier(TokenType.PV_TOKEN)) {
                avancer();
            }

            if (verifier(TokenType.FINPROCEDURE_TOKEN)) {
                break;
            }
        }

        return bloc.getEnfants().isEmpty() ? null : bloc;
    }

    // Règle: LISTE_IDENTIFICATEURS ::= IDENTIFICATEUR ( , IDENTIFICATEUR )*
    private NoeudAST listeIdentificateurs() throws IOException {
        NoeudAST liste = new NoeudAST(NoeudAST.TypeNoeud.LISTE_IDENTIFICATEURS,
                "", tokenCourant.ligne);

        if (!verifier(TokenType.ID_TOKEN)) {
            erreurSyntaxique("Identificateur attendu");
            return liste;
        }

        liste.ajouterEnfant(new NoeudAST(NoeudAST.TypeNoeud.VARIABLE,
                tokenCourant.nom, tokenCourant.ligne));
        avancer();

        while (consommer(TokenType.VIR_TOKEN)) {
            if (!verifier(TokenType.ID_TOKEN)) {
                erreurSyntaxique("Identificateur attendu après ','");
                break;
            }

            liste.ajouterEnfant(new NoeudAST(NoeudAST.TypeNoeud.VARIABLE,
                    tokenCourant.nom, tokenCourant.ligne));
            avancer();
        }

        return liste;
    }

    // Règle: TYPE ::= ENTIER | REEL | CHAINE | BOOLEEN | IDENTIFICATEUR
    private NoeudAST typeDeclaration() throws IOException {
        TokenType typeToken = tokenCourant.code;

        if (typeToken == TokenType.ENTIER_TOKEN ||
                typeToken == TokenType.REEL_TOKEN ||
                typeToken == TokenType.CHAINE_TOKEN ||
                typeToken == TokenType.BOOLEEN_TOKEN ||
                typeToken == TokenType.ID_TOKEN) {

            NoeudAST type = new NoeudAST(NoeudAST.TypeNoeud.TYPE,
                    tokenCourant.nom, tokenCourant.ligne);
            avancer();

            // Vérifier si c'est un tableau (suivi de [nombre])
            if (verifier(TokenType.CO_TOKEN)) {
                avancer(); // [

                // Lire la taille du tableau
                if (verifier(TokenType.NUM_ENTIER_TOKEN)) {
                    type.setValeur(type.getValeur() + "[" + tokenCourant.nom + "]");
                    avancer();
                }

                if (!consommer(TokenType.CF_TOKEN)) {
                    erreurSyntaxique("']' attendu");
                }
            }

            return type;
        }

        erreurSyntaxique("Type attendu (ENTIER, REEL, CHAINE, BOOLEEN ou nom de structure)");
        return null;
    }

    // Règle: INSTRUCTIONS ::= INSTRUCTION ( ; INSTRUCTION )*
    private NoeudAST blocInstructions() throws IOException {
        NoeudAST bloc = new NoeudAST(NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS,
                "", tokenCourant.ligne);

        // Lire les instructions jusqu'à atteindre une fin de bloc
        while (!verifier(TokenType.FIN_TOKEN) &&
                !verifier(TokenType.FINSI_TOKEN) &&
                !verifier(TokenType.FINPOUR_TOKEN) &&
                !verifier(TokenType.FINTANTQUE_TOKEN) &&
                !verifier(TokenType.FINFONCTION_TOKEN) &&
                !verifier(TokenType.SINON_TOKEN) &&
                !verifier(TokenType.JUSQUA_TOKEN) &&
                !verifier(TokenType.EOF_TOKEN)) {

            NoeudAST instr = instruction();
            if (instr != null) {
                bloc.ajouterEnfant(instr);
            }

            // Consommer le point-virgule s'il est présent (optionnel après certaines structures)
            consommer(TokenType.PV_TOKEN);
        }

        return bloc;
    }

    // Modifier la méthode instruction() pour gérer RETOUR
    private NoeudAST instruction() throws IOException {
        // Vérifier d'abord si on est à la fin d'un bloc
        if (tokenCourant.code == TokenType.FIN_TOKEN ||
                tokenCourant.code == TokenType.FINSI_TOKEN ||
                tokenCourant.code == TokenType.FINPOUR_TOKEN ||
                tokenCourant.code == TokenType.FINTANTQUE_TOKEN ||
                tokenCourant.code == TokenType.FINFONCTION_TOKEN ||
                tokenCourant.code == TokenType.FINPROCEDURE_TOKEN ||
                tokenCourant.code == TokenType.SINON_TOKEN ||
                tokenCourant.code == TokenType.JUSQUA_TOKEN) {
            return null;
        }

        switch (tokenCourant.code) {
            case ID_TOKEN:
                // Peut être affectation, appel de fonction, ou accès à un champ
                String nom = tokenCourant.nom;
                int ligne = tokenCourant.ligne;
                avancer();

                // Vérifier si c'est un accès à un champ (avec point)
                if (verifier(TokenType.PT_TOKEN)) {
                    // C'est une affectation à un champ de structure
                    return affectationChamp(nom, ligne);
                } else if (verifier(TokenType.AFF_TOKEN)) {
                    return affectation(nom, ligne);
                } else if (verifier(TokenType.CO_TOKEN)) {
                    return affectationTableau(nom, ligne);
                } else if (verifier(TokenType.PO_TOKEN)) {
                    return appelFonctionInstruction(nom, ligne);
                } else {
                    erreurSyntaxique("'<-', '.', '(' ou '[' attendu après identificateur");
                    return null;
                }

            case SI_TOKEN:
                return condition();

            case POUR_TOKEN:
                return bouclePour();

            case TANTQUE_TOKEN:
                return boucleTantque();

            case REPETER_TOKEN:
                return boucleRepeter();

            case ECRIRE_TOKEN:
                return ecrire();

            case LIRE_TOKEN:
                return lire();

            default:
                erreurSyntaxique("Instruction attendue");
                return null;
        }
    }

    private NoeudAST affectationChamp(String nomStructure, int ligne) throws IOException {
        NoeudAST affect = new NoeudAST(NoeudAST.TypeNoeud.AFFECTATION, nomStructure, ligne);

        // Lire l'accès au champ
        NoeudAST acces = accesChamp(nomStructure, ligne);
        if (acces != null) {
            affect.ajouterEnfant(acces);
        }

        if (!consommer(TokenType.AFF_TOKEN)) {
            erreurSyntaxique("'<-' attendu après l'accès au champ");
            return null;
        }

        // Lire l'expression de droite
        NoeudAST expr = expression();
        if (expr != null) {
            affect.ajouterEnfant(expr);
        }

        return affect;
    }

    // Règle: AFFECTATION ::= VARIABLE <- EXPRESSION ;
    private NoeudAST affectation(String nomVariable, int ligne) throws IOException {
        NoeudAST affect = new NoeudAST(NoeudAST.TypeNoeud.AFFECTATION,
                nomVariable, ligne);
        avancer(); // <-

        NoeudAST expr = expression();
        if (expr != null) {
            affect.ajouterEnfant(expr);
        }

        return affect;
    }

    private NoeudAST affectationTableau(String nomTableau, int ligne) throws IOException {
        NoeudAST affect = new NoeudAST(NoeudAST.TypeNoeud.AFFECTATION,
                nomTableau, ligne);

        // Accès tableau
        NoeudAST acces = new NoeudAST(NoeudAST.TypeNoeud.ACCES_TABLEAU,
                nomTableau, ligne);
        avancer(); // [

        NoeudAST indice = expression();
        if (indice != null) {
            acces.ajouterEnfant(indice);
        }

        if (!consommer(TokenType.CF_TOKEN)) {
            erreurSyntaxique("']' attendu");
        }

        affect.ajouterEnfant(acces);

        if (!consommer(TokenType.AFF_TOKEN)) {
            erreurSyntaxique("'<-' attendu");
        }

        NoeudAST expr = expression();
        if (expr != null) {
            affect.ajouterEnfant(expr);
        }

        return affect;
    }

    // Règle: CONDITION ::= SI CONDITION_EXPRESSION ALORS INSTRUCTIONS [ SINON INSTRUCTIONS ] FINSI
    private NoeudAST condition() throws IOException {
        NoeudAST cond = new NoeudAST(NoeudAST.TypeNoeud.CONDITION,
                "", tokenCourant.ligne);
        avancer(); // SI

        // Condition expression
        NoeudAST conditionExpr = expression();
        if (conditionExpr != null) {
            cond.ajouterEnfant(conditionExpr);
        }

        if (!consommer(TokenType.ALORS_TOKEN)) {
            erreurSyntaxique("'ALORS' attendu");
        }

        // Instructions du bloc ALORS
        NoeudAST blocAlors = blocInstructions();
        if (blocAlors != null) {
            cond.ajouterEnfant(blocAlors);
        }

        // Bloc SINON optionnel
        if (verifier(TokenType.SINON_TOKEN)) {
            avancer(); // SINON
            NoeudAST blocSinon = blocInstructions();
            if (blocSinon != null) {
                cond.ajouterEnfant(blocSinon);
            }
        }

        if (!consommer(TokenType.FINSI_TOKEN)) {
            erreurSyntaxique("'FINSI' attendu");
        }

        return cond;
    }

    private boolean estOperateurComparaison(TokenType type) {
        return type == TokenType.INF_TOKEN ||
                type == TokenType.INFEG_TOKEN ||
                type == TokenType.SUP_TOKEN ||
                type == TokenType.SUPEG_TOKEN ||
                type == TokenType.EG_TOKEN ||
                type == TokenType.DIFF_TOKEN;
    }

    private boolean estOperateurLogique(TokenType type) {
        return type == TokenType.ET_TOKEN ||
                type == TokenType.OU_TOKEN;
    }

    // Règle: BOUCLE_POUR ::= POUR IDENTIFICATEUR <- EXPRESSION JUSQUA EXPRESSION
    //                        FAIRE INSTRUCTIONS FINPOUR
    private NoeudAST bouclePour() throws IOException {
        NoeudAST boucle = new NoeudAST(NoeudAST.TypeNoeud.BOUCLE_POUR,
                "", tokenCourant.ligne);
        avancer(); // POUR

        if (!verifier(TokenType.ID_TOKEN)) {
            erreurSyntaxique("Variable de boucle attendue");
            return null;
        }

        boucle.setValeur(tokenCourant.nom);
        avancer();

        if (!consommer(TokenType.AFF_TOKEN)) {
            erreurSyntaxique("'<-' attendu");
        }

        // Expression de début
        NoeudAST debut = expression();
        if (debut != null) {
            boucle.ajouterEnfant(debut);
        }

        if (!consommer(TokenType.JUSQUA_TOKEN)) {
            erreurSyntaxique("'JUSQUA' attendu");
        }

        // Expression de fin
        NoeudAST fin = expression();
        if (fin != null) {
            boucle.ajouterEnfant(fin);
        }

        if (!consommer(TokenType.FAIRE_TOKEN)) {
            erreurSyntaxique("'FAIRE' attendu");
        }

        // Instructions
        NoeudAST corps = blocInstructions();
        if (corps != null) {
            boucle.ajouterEnfant(corps);
        }

        if (!consommer(TokenType.FINPOUR_TOKEN)) {
            erreurSyntaxique("'FINPOUR' attendu");
        }

        return boucle;
    }

    // Règle: BOUCLE_TANTQUE ::= TANTQUE CONDITION_EXPRESSION FAIRE INSTRUCTIONS FINTANTQUE
    private NoeudAST boucleTantque() throws IOException {
        NoeudAST boucle = new NoeudAST(NoeudAST.TypeNoeud.BOUCLE_TANTQUE,
                "", tokenCourant.ligne);
        avancer(); // TANTQUE

        // Condition expression
        NoeudAST condition = expression();
        if (condition != null) {
            boucle.ajouterEnfant(condition);
        }

        if (!consommer(TokenType.FAIRE_TOKEN)) {
            erreurSyntaxique("'FAIRE' attendu");
        }

        // Instructions
        NoeudAST corps = blocInstructions();
        if (corps != null) {
            boucle.ajouterEnfant(corps);
        }

        if (!consommer(TokenType.FINTANTQUE_TOKEN)) {
            erreurSyntaxique("'FINTANTQUE' attendu");
        }

        return boucle;
    }

    // Règle: BOUCLE_REPETER ::= REPETER INSTRUCTIONS JUSQUA EXPRESSION
    private NoeudAST boucleRepeter() throws IOException {
        NoeudAST boucle = new NoeudAST(NoeudAST.TypeNoeud.BOUCLE_REPETER,
                "", tokenCourant.ligne);
        avancer(); // REPETER

        // Instructions
        NoeudAST corps = blocInstructions();
        if (corps != null) {
            boucle.ajouterEnfant(corps);
        }

        if (!consommer(TokenType.JUSQUA_TOKEN)) {
            erreurSyntaxique("'JUSQUA' attendu");
        }

        // Condition expression
        NoeudAST condition = expression();
        if (condition != null) {
            boucle.ajouterEnfant(condition);
        }

        return boucle;
    }

    // Règle: ECRIRE ::= ECRIRE EXPRESSION ( , EXPRESSION )* ;
    private NoeudAST ecrire() throws IOException {
        NoeudAST ecrire = new NoeudAST(NoeudAST.TypeNoeud.ECRIRE,
                "", tokenCourant.ligne);
        avancer(); // ECRIRE

        // Expression
        NoeudAST expr = expression();
        if (expr != null) {
            ecrire.ajouterEnfant(expr);
        }

        while (consommer(TokenType.VIR_TOKEN)) {
            expr = expression();
            if (expr != null) {
                ecrire.ajouterEnfant(expr);
            }
        }

        return ecrire;
    }

    // Règle: LIRE ::= LIRE IDENTIFICATEUR ( , IDENTIFICATEUR )* ;
    private NoeudAST lire() throws IOException {
        NoeudAST lire = new NoeudAST(NoeudAST.TypeNoeud.LIRE,
                "", tokenCourant.ligne);
        avancer(); // LIRE

        if (!verifier(TokenType.ID_TOKEN)) {
            erreurSyntaxique("Variable attendue");
            return null;
        }

        lire.ajouterEnfant(new NoeudAST(NoeudAST.TypeNoeud.VARIABLE,
                tokenCourant.nom, tokenCourant.ligne));
        avancer();

        while (consommer(TokenType.VIR_TOKEN)) {
            if (!verifier(TokenType.ID_TOKEN)) {
                erreurSyntaxique("Variable attendue");
                break;
            }

            lire.ajouterEnfant(new NoeudAST(NoeudAST.TypeNoeud.VARIABLE,
                    tokenCourant.nom, tokenCourant.ligne));
            avancer();
        }

        return lire;
    }

    private NoeudAST appelFonctionInstruction(String nomFonction, int ligne) throws IOException {
        NoeudAST appel = new NoeudAST(NoeudAST.TypeNoeud.APPEL_FONCTION,
                nomFonction, ligne);

        avancer(); // (

        // Arguments optionnels
        if (!verifier(TokenType.PF_TOKEN)) {
            NoeudAST arguments = argumentsAppel();
            if (arguments != null) {
                appel.ajouterEnfant(arguments);
            }
        }

        if (!consommer(TokenType.PF_TOKEN)) {
            erreurSyntaxique("')' attendu");
        }

        return appel;
    }

    private NoeudAST argumentsAppel() throws IOException {
        NoeudAST listeArgs = new NoeudAST(NoeudAST.TypeNoeud.LISTE_ARGUMENTS,
                "", tokenCourant.ligne);

        NoeudAST expr = expression();
        if (expr != null) {
            listeArgs.ajouterEnfant(expr);
        }

        while (consommer(TokenType.VIR_TOKEN)) {
            expr = expression();
            if (expr != null) {
                listeArgs.ajouterEnfant(expr);
            }
        }

        return listeArgs;
    }

    // NOUVELLES MÉTHODES D'EXPRESSION
    // EXPRESSION ::= EXPRESSION_BOOL
    private NoeudAST expression() throws IOException {
        return expressionBool();
    }

    // EXPRESSION_BOOL ::= EXPRESSION_COMP ( ( ET | OU ) EXPRESSION_COMP )*
    private NoeudAST expressionBool() throws IOException {
        NoeudAST gauche = expressionComp();

        while (verifier(TokenType.ET_TOKEN) || verifier(TokenType.OU_TOKEN)) {
            String operateur = tokenCourant.nom;
            avancer();

            NoeudAST droite = expressionComp();

            NoeudAST operation = new NoeudAST(NoeudAST.TypeNoeud.EXPRESSION_BINAIRE,
                    operateur, tokenCourant.ligne);
            operation.ajouterEnfant(gauche);
            operation.ajouterEnfant(droite);
            gauche = operation;
        }

        return gauche;
    }

    // EXPRESSION_COMP ::= EXPRESSION_ARITH ( OP_COMPARAISON EXPRESSION_ARITH )*
    private NoeudAST expressionComp() throws IOException {
        NoeudAST gauche = expressionArith();

        while (estOperateurComparaison(tokenCourant.code)) {
            String operateur = tokenCourant.nom;
            avancer();

            NoeudAST droite = expressionArith();

            NoeudAST operation = new NoeudAST(NoeudAST.TypeNoeud.EXPRESSION_BINAIRE,
                    operateur, tokenCourant.ligne);
            operation.ajouterEnfant(gauche);
            operation.ajouterEnfant(droite);
            gauche = operation;
        }

        return gauche;
    }

    // EXPRESSION_ARITH ::= TERME ( ( + | - ) TERME )*
    private NoeudAST expressionArith() throws IOException {
        NoeudAST gauche = terme();

        while (verifier(TokenType.PLUS_TOKEN) || verifier(TokenType.MOINS_TOKEN)) {
            String operateur = tokenCourant.nom;
            avancer();

            NoeudAST droite = terme();

            NoeudAST operation = new NoeudAST(NoeudAST.TypeNoeud.EXPRESSION_BINAIRE,
                    operateur, tokenCourant.ligne);
            operation.ajouterEnfant(gauche);
            operation.ajouterEnfant(droite);
            gauche = operation;
        }

        return gauche;
    }

    // TERME ::= FACTEUR ( ( * | / ) FACTEUR )*
    private NoeudAST terme() throws IOException {
        NoeudAST gauche = facteur();

        while (verifier(TokenType.MULT_TOKEN) || verifier(TokenType.DIV_TOKEN)) {
            String operateur = tokenCourant.nom;
            avancer();

            NoeudAST droite = facteur();

            NoeudAST operation = new NoeudAST(NoeudAST.TypeNoeud.EXPRESSION_BINAIRE,
                    operateur, tokenCourant.ligne);
            operation.ajouterEnfant(gauche);
            operation.ajouterEnfant(droite);
            gauche = operation;
        }

        return gauche;
    }

    // FACTEUR ::= NOMBRE | VARIABLE | ( EXPRESSION ) | APPEL_FONCTION | - FACTEUR | CHAINE | VRAI | FAUX | NON FACTEUR
    private NoeudAST facteur() throws IOException {
        switch (tokenCourant.code) {
            case NUM_ENTIER_TOKEN:
            case NUM_REEL_TOKEN:
                NoeudAST nombre = new NoeudAST(NoeudAST.TypeNoeud.NOMBRE,
                        tokenCourant.nom, tokenCourant.ligne);
                avancer();
                return nombre;

            case ID_TOKEN:
                String nom = tokenCourant.nom;
                int ligne = tokenCourant.ligne;
                avancer();

                if (verifier(TokenType.PT_TOKEN)) {
                    return accesChamp(nom, ligne);
                } else if (verifier(TokenType.PO_TOKEN)) {
                    return appelFonctionExpression(nom, ligne);
                } else if (verifier(TokenType.CO_TOKEN)) {
                    return accesTableau(nom, ligne);
                } else {
                    return new NoeudAST(NoeudAST.TypeNoeud.VARIABLE, nom, ligne);
                }

            case CHAINE_LIT_TOKEN:
                NoeudAST chaine = new NoeudAST(NoeudAST.TypeNoeud.CHAINE,
                        tokenCourant.nom, tokenCourant.ligne);
                avancer();
                return chaine;

            case VRAI_TOKEN:
            case FAUX_TOKEN:
                NoeudAST bool = new NoeudAST(NoeudAST.TypeNoeud.VALEUR_BOOLEENNE,
                        tokenCourant.nom, tokenCourant.ligne);
                avancer();
                return bool;

            case PO_TOKEN:
                avancer(); // (
                NoeudAST expr = expression();
                if (!consommer(TokenType.PF_TOKEN)) {
                    erreurSyntaxique("')' attendu");
                }
                return expr;

            case MOINS_TOKEN:
                avancer(); // -
                NoeudAST fact = facteur();
                NoeudAST neg = new NoeudAST(NoeudAST.TypeNoeud.NEGATION,
                        "-", tokenCourant.ligne);
                neg.ajouterEnfant(fact);
                return neg;

            case NON_TOKEN:
                avancer(); // NON
                NoeudAST factNon = facteur();
                NoeudAST non = new NoeudAST(NoeudAST.TypeNoeud.NEGATION,
                        "NON", tokenCourant.ligne);
                non.ajouterEnfant(factNon);
                return non;

            default:
                erreurSyntaxique("Facteur attendu (nombre, variable, chaîne, booléen, '(' ou '-')");
                return null;
        }
    }

    private NoeudAST accesChamp(String nomStructure, int ligne) throws IOException {
        NoeudAST acces = new NoeudAST(NoeudAST.TypeNoeud.ACCES_CHAMP, nomStructure, ligne);

        while (verifier(TokenType.PT_TOKEN)) {
            avancer(); // consommer le point

            if (!verifier(TokenType.ID_TOKEN)) {
                erreurSyntaxique("Identificateur de champ attendu après le point");
                return null;
            }

            // Ajouter le champ comme enfant
            NoeudAST champ = new NoeudAST(NoeudAST.TypeNoeud.VARIABLE,
                    tokenCourant.nom, tokenCourant.ligne);
            acces.ajouterEnfant(champ);
            avancer();

            // Vérifier si on a un autre point (accès en chaîne comme rect.coinSupGauche.x)
            // ou un crochet pour un tableau (comme etud.notes[0])
            if (!verifier(TokenType.PT_TOKEN) && !verifier(TokenType.CO_TOKEN)) {
                break;
            }

            // Si c'est un crochet, gérer l'accès tableau
            if (verifier(TokenType.CO_TOKEN)) {
                avancer(); // [
                NoeudAST indice = expression();
                if (indice != null) {
                    // CORRECTION: Utiliser le nom du champ au lieu de tokenCourant.nom
                    NoeudAST accesTableau = new NoeudAST(NoeudAST.TypeNoeud.ACCES_TABLEAU,
                            champ.getValeur(), ligne);
                    accesTableau.ajouterEnfant(indice);

                    // Remplacer le champ par l'accès tableau
                    acces.getEnfants().remove(acces.getEnfants().size() - 1);
                    acces.ajouterEnfant(accesTableau);
                }

                if (!consommer(TokenType.CF_TOKEN)) {
                    erreurSyntaxique("']' attendu");
                }
                break;
            }
        }

        return acces;
    }

    private NoeudAST appelFonctionExpression(String nomFonction, int ligne) throws IOException {
        NoeudAST appel = new NoeudAST(NoeudAST.TypeNoeud.APPEL_FONCTION,
                nomFonction, ligne);
        avancer(); // (

        // Arguments optionnels
        if (!verifier(TokenType.PF_TOKEN)) {
            NoeudAST arguments = argumentsAppel();
            if (arguments != null) {
                appel.ajouterEnfant(arguments);
            }
        }

        if (!consommer(TokenType.PF_TOKEN)) {
            erreurSyntaxique("')' attendu");
        }

        return appel;
    }

    private NoeudAST accesTableau(String nomTableau, int ligne) throws IOException {
        NoeudAST acces = new NoeudAST(NoeudAST.TypeNoeud.ACCES_TABLEAU,
                nomTableau, ligne);
        avancer(); // [

        NoeudAST indice = expression();
        if (indice != null) {
            acces.ajouterEnfant(indice);
        }

        if (!consommer(TokenType.CF_TOKEN)) {
            erreurSyntaxique("']' attendu");
        }

        return acces;
    }

    // Getters
    public NoeudAST getArbreSyntaxique() {
        return arbreSyntaxique;
    }

    public List<String> getErreursSyntaxiques() {
        return erreursSyntaxiques;
    }

    public boolean aErreurs() {
        return !erreursSyntaxiques.isEmpty();
    }

    public void afficherErreurs() {
        if (aErreurs()) {
            System.err.println("\n=== ERREURS SYNTAXIQUES ===");
            for (String erreur : erreursSyntaxiques) {
                System.err.println(erreur);
            }
        }
    }
}