import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            String fichier = "algotest.txt";

            String algoTest = "#PYTHON\n" +
                    "VAR age, limite\n" +
                    "DEBUT\n" +
                    "    limite <- 18;\n" +
                    "    age <- 0;\n" +
                    "    SI age >= limite ALORS\n" +
                    "        ECRIRE age\n" +
                    "    FINSI\n" +
                    "FIN";
                    
            FileWriter fw = new FileWriter(fichier);
            fw.write(algoTest);
            fw.close();

            System.out.println("=== ANALYSE LEXICALE ===\n");
            System.out.println("Programme source:");
            System.out.println(algoTest);
            System.out.println("\n=== TOKENS RECONNUS ===\n");

            // String fichier = "programme.txt";

            // Créer l'analyseur lexical
            analyseurLexical analyseur = new analyseurLexical(fichier);

            // Analyser le fichier
            while (analyseur.getSymboleCourant().code != TokenType.EOF_TOKEN) {
                analyseur.symboleSuivant();
                analyseur.afficherToken();
            }

            System.out.println("\n=== ANALYSE TERMINÉE AVEC SUCCÈS ===");

            // Fermer l'analyseur
            analyseur.fermer();

        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier : " + e.getMessage());
        }
    }
}