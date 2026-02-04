import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            String nomFichierSource = "test1_base.txt";
            // Options disponibles :
            // - test1_base.txt
            // - test2_boucles.txt
            // - test3_controles.txt
            // - test4_fonctions.txt
            // - test5_procedures.txt
            // - test6_tableaux.txt
            // - test7_structures.txt
            // - testGlobal.txt

            // 2. Dossier de sortie (généré automatiquement)
            String dossierSortie = "src/code_genere/";

            // Vérifier si le fichier source existe
            Path cheminSource = Paths.get("src/tests", nomFichierSource);
            if (!Files.exists(cheminSource)) {
                System.err.println("ERREUR : Fichier source introuvable : " + cheminSource);
                System.err.println("Vérifiez que le fichier existe dans src/tests/");
                return;
            }

            // Lire le contenu du fichier
            String algorithmeSource = new String(Files.readAllBytes(cheminSource), "UTF-8");

            System.out.println("    COMPILATEUR POUR LANGAGE ALGORITHMIQUE");

            System.out.println("\n=== FICHIER SOURCE : " + nomFichierSource + " ===");
            System.out.println(algorithmeSource);

            System.out.println("\n=== ANALYSE LEXICALE ===");
            System.out.println("Tokens reconnus:");

            // Créer et exécuter l'analyseur lexical
            analyseurLexical analyseurLex = new analyseurLexical(cheminSource.toString());

            // Analyser et afficher tous les tokens
            while (analyseurLex.getSymboleCourant().code != TokenType.EOF_TOKEN) {
                analyseurLex.symboleSuivant();
                analyseurLex.afficherToken();
            }

            System.out.println("\n=== ANALYSE LEXICALE TERMINÉE ===");

            // NOUVEAU: Récupérer le langage cible
            String langageCible = analyseurLex.getLangageCible();

            // Fermer l'analyseur lexical
            analyseurLex.fermer();

            System.out.println("\n=== ANALYSE SYNTAXIQUE ===");

            // Recréer l'analyseur lexical pour l'analyse syntaxique
            analyseurLexical analyseurLex2 = new analyseurLexical(cheminSource.toString());
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
                    analyseurLex2.fermer();
                    return;
                } else {
                    System.out.println("\n=== ANALYSE SÉMANTIQUE RÉUSSIE ===");

                    // VÉRIFICATION DE LA DIRECTIVE DE LANGAGE
                    if (langageCible == null || !langageCible.equalsIgnoreCase("PYTHON")) {
                        System.err.println("\n========================================");
                        System.err.println("ATTENTION : Génération de code impossible !");
                        System.err.println("========================================");
                        if (langageCible == null) {
                            System.err.println("Raison : Aucune directive de langage trouvée (#PYTHON attendu)");
                        } else {
                            System.err.println("Raison : Langage cible non supporté : " + langageCible);
                        }
                        System.err.println("\nLes langages supportés actuellement :");
                        System.err.println("  - PYTHON (#PYTHON)");
                        System.err.println("\nAjoutez la directive #PYTHON en début de fichier pour générer du code Python.");
                        System.err.println("========================================");
                        analyseurLex2.fermer();
                        return;
                    }

                    // GÉNÉRATION DE CODE PYTHON
                    System.out.println("\n=== GÉNÉRATION DE CODE (PYTHON) ===");

                    // Créer le générateur Python
                    GenerateurCode generateur = new GenerateurPython();
                    String codeGenere = generateur.generer(arbreSyntaxique);

                    // Créer le dossier de sortie s'il n'existe pas
                    Path cheminDossierSortie = Paths.get(dossierSortie);
                    if (!Files.exists(cheminDossierSortie)) {
                        Files.createDirectories(cheminDossierSortie);
                        System.out.println("Dossier créé : " + dossierSortie);
                    }

                    // Nom du fichier de sortie (même nom que le source avec .py)
                    String nomFichierSortie = nomFichierSource.replace(".txt", ".py");
                    Path cheminCompletSortie = Paths.get(dossierSortie, nomFichierSortie);

                    // Écrire le code généré dans le fichier
                    FileWriter fwSortie = new FileWriter(cheminCompletSortie.toString());
                    fwSortie.write(codeGenere);
                    fwSortie.close();

                    // Affichage console
                    System.out.println("\n--- CODE PYTHON GÉNÉRÉ ---");
                    System.out.println(codeGenere);
                    System.out.println("========================================");
                    System.out.println("Fichier généré avec succès : " + cheminCompletSortie);
                    System.out.println("========================================");
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