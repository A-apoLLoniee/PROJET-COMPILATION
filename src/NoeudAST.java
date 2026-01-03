// NoeudAST.java
import java.util.ArrayList;
import java.util.List;

public class NoeudAST {
    public enum TypeNoeud {
        // Structure du programme
        PROGRAMME("Programme"),
        DIRECTIVE_LANGAGE("DirectiveLangage"),
        SECTION_VAR("SectionVar"),
        DECLARATION_VARIABLE("DeclarationVariable"),
        DECLARATION_STRUCTURE("DeclarationStructure"),
        CHAMP_STRUCTURE("ChampStructure"),
        DECLARATION_FONCTION("DeclarationFonction"),

        // Instructions
        BLOC_INSTRUCTIONS("BlocInstructions"),
        AFFECTATION("Affectation"),
        CONDITION("Condition"),
        BOUCLE_POUR("BouclePour"),
        BOUCLE_TANTQUE("BoucleTantque"),
        BOUCLE_REPETER("BoucleRepeter"),
        ECRIRE("Ecrire"),
        LIRE("Lire"),
        APPEL_FONCTION("AppelFonction"),
        RETOUR("Retour"),


        // Expressions
        EXPRESSION_BINAIRE("ExpressionBinaire"),
        NEGATION("Negation"),
        NOMBRE("Nombre"),
        VARIABLE("Variable"),
        VALEUR_BOOLEENNE("ValeurBooleenne"),
        ACCES_TABLEAU("AccesTableau"),
        CHAINE("Chaine"),

        // Types et paramètres
        TYPE("Type"),
        PARAMETRE("Parametre"),
        LISTE_PARAMETRES("ListeParametres"),
        LISTE_ARGUMENTS("ListeArguments"),

        // Spécial
        LISTE_IDENTIFICATEURS("ListeIdentificateurs"),
        CONDITION_EXPRESSION("ConditionExpression");

        private final String nom;

        TypeNoeud(String nom) {
            this.nom = nom;
        }

        @Override
        public String toString() {
            return nom;
        }
    }

    private TypeNoeud type;
    private String valeur;
    private int ligne;
    private List<NoeudAST> enfants;

    public NoeudAST(TypeNoeud type) {
        this.type = type;
        this.valeur = "";
        this.ligne = 1;
        this.enfants = new ArrayList<>();
    }

    public NoeudAST(TypeNoeud type, String valeur) {
        this(type);
        this.valeur = valeur;
    }

    public NoeudAST(TypeNoeud type, String valeur, int ligne) {
        this(type, valeur);
        this.ligne = ligne;
    }

    public void ajouterEnfant(NoeudAST enfant) {
        if (enfant != null) {
            enfants.add(enfant);
        }
    }

    public void ajouterEnfants(List<NoeudAST> enfants) {
        if (enfants != null) {
            this.enfants.addAll(enfants);
        }
    }

    public NoeudAST getEnfant(int index) {
        if (index >= 0 && index < enfants.size()) {
            return enfants.get(index);
        }
        return null;
    }

    public TypeNoeud getType() { return type; }
    public String getValeur() { return valeur; }
    public int getLigne() { return ligne; }
    public List<NoeudAST> getEnfants() { return enfants; }

    public void setLigne(int ligne) { this.ligne = ligne; }
    public void setValeur(String valeur) { this.valeur = valeur; }

    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int niveau) {
        StringBuilder sb = new StringBuilder();

        // Ajouter l'indentation
        for (int i = 0; i < niveau; i++) {
            if (i == niveau - 1) {
                sb.append("├── ");
            } else {
                sb.append("│   ");
            }
        }

        // Ajouter le type et la valeur
        sb.append(type);
        if (!valeur.isEmpty()) {
            sb.append(" [").append(valeur).append("]");
        }
        sb.append(" (ligne ").append(ligne).append(")");

        // Ajouter les enfants
        for (int i = 0; i < enfants.size(); i++) {
            sb.append("\n").append(enfants.get(i).toString(niveau + 1));
        }

        return sb.toString();
    }

    // Méthode pour afficher l'arbre sous forme graphique ASCII
    public String toStringArbre() {
        return toStringArbre(0, new ArrayList<>());
    }

    private String toStringArbre(int niveau, List<Boolean> derniers) {
        StringBuilder sb = new StringBuilder();

        // Ajouter les branches verticales
        for (int i = 0; i < niveau - 1; i++) {
            if (i < derniers.size() && derniers.get(i)) {
                sb.append("    ");
            } else {
                sb.append("│   ");
            }
        }

        // Ajouter la branche finale
        if (niveau > 0) {
            if (derniers.size() > niveau - 1 && derniers.get(niveau - 1)) {
                sb.append("└── ");
            } else {
                sb.append("├── ");
            }
        }

        // Ajouter le type et la valeur
        sb.append(type);
        if (!valeur.isEmpty()) {
            sb.append(": ").append(valeur);
        }
        sb.append(" (l.").append(ligne).append(")");

        // Ajouter les enfants
        for (int i = 0; i < enfants.size(); i++) {
            sb.append("\n");
            if (niveau > 0) {
                if (derniers.size() > niveau) {
                    derniers.set(niveau, i == enfants.size() - 1);
                } else {
                    derniers.add(niveau, i == enfants.size() - 1);
                }
            } else {
                if (derniers.isEmpty()) {
                    derniers.add(i == enfants.size() - 1);
                } else {
                    derniers.set(0, i == enfants.size() - 1);
                }
            }
            sb.append(enfants.get(i).toStringArbre(niveau + 1, derniers));
        }

        return sb.toString();
    }

    // Méthode pour générer une représentation JSON de l'arbre (utile pour des outils de visualisation)
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        toJson(sb, 0);
        return sb.toString();
    }

    private void toJson(StringBuilder sb, int niveau) {
        String indentation = "  ".repeat(niveau);

        sb.append(indentation).append("{\n");
        sb.append(indentation).append("  \"type\": \"").append(type).append("\",\n");
        sb.append(indentation).append("  \"valeur\": \"").append(valeur.replace("\"", "\\\"")).append("\",\n");
        sb.append(indentation).append("  \"ligne\": ").append(ligne).append(",\n");

        if (!enfants.isEmpty()) {
            sb.append(indentation).append("  \"enfants\": [\n");
            for (int i = 0; i < enfants.size(); i++) {
                enfants.get(i).toJson(sb, niveau + 2);
                if (i < enfants.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append(indentation).append("  ]\n");
        } else {
            sb.append(indentation).append("  \"enfants\": []\n");
        }

        sb.append(indentation).append("}");
    }

    // Méthode utilitaire pour afficher un résumé compact de l'arbre
    public String afficherResume() {
        return afficherResume(0);
    }

    private String afficherResume(int niveau) {
        StringBuilder sb = new StringBuilder();

        // Indentation
        for (int i = 0; i < niveau; i++) {
            sb.append("  ");
        }

        // Type et valeur
        sb.append(type);
        if (!valeur.isEmpty()) {
            sb.append(": ").append(valeur);
        }

        // Afficher les enfants
        for (NoeudAST enfant : enfants) {
            sb.append("\n").append(enfant.afficherResume(niveau + 1));
        }

        return sb.toString();
    }

    // Compteur de nœuds
    public int compterNoeuds() {
        int count = 1;
        for (NoeudAST enfant : enfants) {
            count += enfant.compterNoeuds();
        }
        return count;
    }

    // Hauteur de l'arbre
    public int hauteur() {
        if (enfants.isEmpty()) {
            return 1;
        }

        int maxHauteur = 0;
        for (NoeudAST enfant : enfants) {
            int hauteurEnfant = enfant.hauteur();
            if (hauteurEnfant > maxHauteur) {
                maxHauteur = hauteurEnfant;
            }
        }

        return maxHauteur + 1;
    }

    // Rechercher un nœud par type
    public List<NoeudAST> rechercherParType(TypeNoeud typeRecherche) {
        List<NoeudAST> resultats = new ArrayList<>();

        if (this.type == typeRecherche) {
            resultats.add(this);
        }

        for (NoeudAST enfant : enfants) {
            resultats.addAll(enfant.rechercherParType(typeRecherche));
        }

        return resultats;
    }
}