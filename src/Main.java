import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            String fichier = "algotest.txt";

            // Programme de test plus complet
            String algoTest = "#PYTHON\n" +
                    "Algorithme ExempleComplet\n" +
                    "\n" +
                    "Structure Personne\n" +
                    "    nom, prenom: chaine;\n" +
                    "    age: entier;\n" +
                    "FinStructure\n" +
                    "\n" +
                    "Fonction calculerCarre(x: entier): entier\n" +
                    "DEBUT\n" +
                    "    RETOUR x * x\n" +  // Pas de ; après RETOUR
                    "FinFonction\n" +  // Sans ; et avec capitalisation correcte
                    "\n" +
                    "VAR\n" +
                    "    age, limite, resultat, compteur, i: entier;\n" +
                    "    message: chaine;\n" +
                    "    estMajeur: booleen;\n" +
                    "    prix: reel;\n" +
                    "    tab: entier[10];\n" +
                    "DEBUT\n" +
                    "    limite <- 18;\n" +
                    "    age <- 20;\n" +
                    "    prix <- 12.5;\n" +
                    "    estMajeur <- age >= limite;\n" +
                    "    SI estMajeur ET age < 100 ALORS\n" +
                    "        message <- \"Personne majeure\";\n" +
                    "        ECRIRE message;\n" +
                    "    SINON\n" +
                    "        ECRIRE \"Personne mineure\";\n" +
                    "    FINSI\n" +
                    "    \n" +
                    "    resultat <- age + 5;\n" +
                    "    ECRIRE \"Age: \", age;\n" +
                    "    ECRIRE \"Prix: \", prix;\n" +
                    "    \n" +
                    "    // Test d'appel de fonction\n" +
                    "    resultat <- calculerCarre(age);\n" +
                    "    ECRIRE \"Carré de l'âge: \", resultat;\n" +
                    "    \n" +
                    "    // Test de boucle POUR\n" +
                    "    POUR i <- 1 JUSQUA 5 FAIRE\n" +
                    "        ECRIRE \"Itération: \", i;\n" +
                    "    FINPOUR\n" +
                    "    \n" +
                    "    // Test de boucle TANTQUE\n" +
                    "    compteur <- 0;\n" +
                    "    TANTQUE compteur < 3 FAIRE\n" +
                    "        ECRIRE \"Tantque: \", compteur;\n" +
                    "        compteur <- compteur + 1;\n" +
                    "    FINTANTQUE\n" +
                    "    \n" +
                    "    // Test de boucle REPETER\n" +
                    "    REPETER\n" +
                    "        ECRIRE \"Repeter\";\n" +
                    "    JUSQUA FAUX\n" +  // REPETER avec JUSQUA, pas TANTQUE
                    "FIN";

            FileWriter fw = new FileWriter(fichier);
            fw.write(algoTest);
            fw.close();

            System.out.println("==========================================");
            System.out.println("    COMPILATEUR POUR LANGAGE ALGORITHMIQUE");
            System.out.println("==========================================");

            System.out.println("\n=== PROGRAMME SOURCE ===");
            System.out.println(algoTest);

            System.out.println("\n=== ANALYSE LEXICALE ===");
            System.out.println("Tokens reconnus:");

            // Créer et exécuter l'analyseur lexical
            analyseurLexical analyseurLex = new analyseurLexical(fichier);

            // Analyser et afficher tous les tokens
            while (analyseurLex.getSymboleCourant().code != TokenType.EOF_TOKEN) {
                analyseurLex.symboleSuivant();
                analyseurLex.afficherToken();
            }

            System.out.println("\n=== ANALYSE LEXICALE TERMINÉE ===");

            // Fermer l'analyseur lexical
            analyseurLex.fermer();

            System.out.println("\n=== ANALYSE SYNTAXIQUE ===");

            // Recréer l'analyseur lexical pour l'analyse syntaxique
            analyseurLexical analyseurLex2 = new analyseurLexical(fichier);
            AnalyseurSyntaxique analyseurSyntaxique = new AnalyseurSyntaxique(analyseurLex2);

            try {
                // Exécuter l'analyse syntaxique
                analyseurSyntaxique.analyser();

                if (analyseurSyntaxique.aErreurs()) {
                    analyseurSyntaxique.afficherErreurs();
                    System.err.println("\n=== ANALYSE SYNTAXIQUE ÉCHOUÉE ===");
                    analyseurLex2.fermer();
                    return;
                }

                System.out.println("\n=== ANALYSE SYNTAXIQUE RÉUSSIE ===");

                // Récupérer l'arbre syntaxique
                NoeudAST arbreSyntaxique = analyseurSyntaxique.getArbreSyntaxique();

                System.out.println("\n=== ARBRE SYNTAXIQUE ABSTRAIT (AST) ===");
                if (arbreSyntaxique != null) {
                    System.out.println("\n--- Représentation arborescente ---");
                    System.out.println(arbreSyntaxique.toStringArbre());

                    System.out.println("\n--- Statistiques ---");
                    System.out.println("Nombre total de nœuds: " + arbreSyntaxique.compterNoeuds());
                    System.out.println("Hauteur de l'arbre: " + arbreSyntaxique.hauteur());

                    System.out.println("\n--- Résumé compact ---");
                    System.out.println(arbreSyntaxique.afficherResume());
                }

                System.out.println("\n=== ANALYSE SÉMANTIQUE ===");

                // Créer et exécuter l'analyseur sémantique
                AnalyseurSemantique analyseurSemantique = new AnalyseurSemantique(arbreSyntaxique);
                analyseurSemantique.analyser();

                // Afficher les résultats de l'analyse sémantique
                analyseurSemantique.afficherResultats();

                if (analyseurSemantique.aErreurs()) {
                    System.err.println("\n=== ANALYSE SÉMANTIQUE ÉCHOUÉE ===");
                } else {
                    System.out.println("\n=== ANALYSE SÉMANTIQUE RÉUSSIE ===");

                    // Si tout est bon, on pourrait passer à la génération de code
                    System.out.println("\n=== PRÊT POUR LA GÉNÉRATION DE CODE ===");
                    String langageCible = analyseurLex2.getLangageCible();
                    if (langageCible != null) {
                        System.out.println("Langage cible: " + langageCible);
                    }
                }

            } catch (IOException e) {
                System.err.println("Erreur lors de l'analyse syntaxique: " + e.getMessage());
            }

            // Fermer les analyseurs
            analyseurLex2.fermer();

        } catch (IOException e) {
            System.err.println("Erreur de lecture/écriture du fichier : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }
}