import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            String fichier = "algotest.txt";

            // On suppose que le fichier existe (éditeur : écris directement dans algotest.txt)
            String algoTest;
            java.io.File f = new java.io.File(fichier);
            if (!f.exists()) {
                System.err.println("Fichier '" + fichier + "' introuvable. Crée et remplis le fichier avant d'exécuter le programme.");
                return;
            }

            byte[] bytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fichier));
            algoTest = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

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
                    System.out.println("\n=== GÉNÉRATION DE CODE ===");

                    String langageCible = analyseurLex2.getLangageCible();
                    if (langageCible == null) {
                        langageCible = "PYTHON"; // langage par défaut
                    }

                    GenerateurCode generateur = null;
                    String extension = "";

                    switch (langageCible) {
                        case "PYTHON":
                            generateur = new GenerateurPython();
                            extension = ".py";
                            break;

                        default:
                            System.err.println("Langage cible non supporté : " + langageCible);
                            return;
                    }

                    String codeGenere = generateur.generer(arbreSyntaxique);

                    // Affichage console
                    System.out.println("\n--- CODE GÉNÉRÉ (" + langageCible + ") ---");
                    System.out.println(codeGenere);

                    // Écriture dans un fichier
                    String fichierSortie = "programme_genere" + extension;
                    FileWriter fwSortie = new FileWriter(fichierSortie);
                    fwSortie.write(codeGenere);
                    fwSortie.close();

                    System.out.println("\nCode généré avec succès dans le fichier : " + fichierSortie);

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