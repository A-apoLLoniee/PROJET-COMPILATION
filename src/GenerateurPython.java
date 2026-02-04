public class GenerateurPython extends GenerateurCode {

    @Override
    public String generer(NoeudAST n) {
        if (n == null) {
            System.out.println("Objet is null!");
            return "";
        }

        switch (n.getType()) {
            case PROGRAMME:
                return genererProgramme(n);

            case BLOC_INSTRUCTIONS:
                return genererBloc(n);

            case AFFECTATION:
                return genererAffectation(n);

            case CONDITION:
                return genererCondition(n);

            case ECRIRE:
                return genererEcrire(n);

            case LIRE:
                return genererLire(n);

            case DECLARATION_FONCTION:
                return genererDeclarationFonction(n);

            case DECLARATION_PROCEDURE:
                return genererDeclarationProcedure(n);

            case RETOUR:
                return genererRetour(n);

            case BOUCLE_POUR:
                return genererBouclePour(n);

            case BOUCLE_TANTQUE:
                return genererBoucleTantque(n);

            case BOUCLE_REPETER:
                return genererBoucleRepeter(n);

            case APPEL_FONCTION:
                return genererAppelFonction(n);

            case EXPRESSION_BINAIRE:
                return genererExpression(n);

            case VARIABLE:
            case NOMBRE:
                return n.getValeur();

            case CHAINE:
                return "\"" + n.getValeur() + "\"";

            case ACCES_TABLEAU:
                return genererAccesTableau(n);

            case VALEUR_BOOLEENNE:
                return genererValeurBooleenne(n);

            case NEGATION:
                return genererNegation(n);

            case SECTION_VAR:
                return genererSectionVar(n);

            case DECLARATION_STRUCTURE:
                return genererDeclarationStructure(n);

            case ACCES_CHAMP:
                return genererAccesChamp(n);

            default:
                return "";
        }
    }

    private String genererProgramme(NoeudAST n) {
        StringBuilder sb = new StringBuilder();

        // Générer d'abord les structures (classes Python)
        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.DECLARATION_STRUCTURE) {
                sb.append(generer(enfant));
            }
        }

        // Puis les fonctions
        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.DECLARATION_FONCTION ||
                    enfant.getType() == NoeudAST.TypeNoeud.DECLARATION_PROCEDURE) {
                sb.append(generer(enfant));
            }
        }

        // Puis les variables globales
        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.SECTION_VAR) {
                sb.append(generer(enfant));
            }
        }

        // Enfin les instructions principales
        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS) {
                sb.append(generer(enfant));
            }
        }

        return sb.toString();
    }

    private String genererDeclarationStructure(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        String nomStructure = n.getValeur();

        sb.append("class ").append(nomStructure).append(":\n");
        indent++;
        sb.append(indent()).append("def __init__(self):\n");
        indent++;

        // Initialiser tous les champs
        boolean hasFields = false;
        for (NoeudAST champ : n.getEnfants()) {
            if (champ.getType() == NoeudAST.TypeNoeud.CHAMP_STRUCTURE) {
                hasFields = true;
                NoeudAST listeId = null;
                NoeudAST typeNode = null;

                for (NoeudAST enfant : champ.getEnfants()) {
                    if (enfant.getType() == NoeudAST.TypeNoeud.LISTE_IDENTIFICATEURS) {
                        listeId = enfant;
                    } else if (enfant.getType() == NoeudAST.TypeNoeud.TYPE) {
                        typeNode = enfant;
                    }
                }

                if (listeId != null && typeNode != null) {
                    String type = typeNode.getValeur();

                    for (NoeudAST var : listeId.getEnfants()) {
                        String nomChamp = var.getValeur();

                        if (type.contains("[")) {
                            int debut = type.indexOf('[');
                            int fin = type.indexOf(']');
                            String tailleStr = type.substring(debut + 1, fin);
                            sb.append(indent()).append("self.").append(nomChamp)
                                    .append(" = [0] * ").append(tailleStr).append("\n");
                        } else {
                            switch (type.toLowerCase()) {
                                case "entier":
                                    sb.append(indent()).append("self.").append(nomChamp)
                                            .append(" = 0\n");
                                    break;
                                case "reel":
                                    sb.append(indent()).append("self.").append(nomChamp)
                                            .append(" = 0.0\n");
                                    break;
                                case "chaine":
                                case "chainedecharactere":
                                    sb.append(indent()).append("self.").append(nomChamp)
                                            .append(" = \"\"\n");
                                    break;
                                case "booleen":
                                    sb.append(indent()).append("self.").append(nomChamp)
                                            .append(" = False\n");
                                    break;
                                default:
                                    sb.append(indent()).append("self.").append(nomChamp)
                                            .append(" = ").append(type).append("()\n");
                            }
                        }
                    }
                }
            }
        }

        if (!hasFields) {
            sb.append(indent()).append("pass\n");
        }

        indent--;
        indent--;
        sb.append("\n");
        return sb.toString();
    }

    private String genererAccesChamp(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        String nomStructure = n.getValeur();

        sb.append(nomStructure);

        for (NoeudAST champ : n.getEnfants()) {
            if (champ.getType() == NoeudAST.TypeNoeud.VARIABLE) {
                sb.append(".").append(champ.getValeur());
            } else if (champ.getType() == NoeudAST.TypeNoeud.ACCES_TABLEAU) {
                sb.append(".").append(champ.getValeur());
                if (!champ.getEnfants().isEmpty()) {
                    sb.append("[").append(generer(champ.getEnfant(0))).append("]");
                }
            }
        }

        return sb.toString();
    }

    private String genererBloc(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        for (NoeudAST instr : n.getEnfants()) {
            // CORRECTION: Ajouter \n après les appels de procédures qui sont des instructions
            String code = generer(instr);
            sb.append(code);

            // Si c'est un appel de fonction qui est une instruction (pas dans une expression)
            // et qu'il ne se termine pas déjà par \n, ajouter \n
            if (instr.getType() == NoeudAST.TypeNoeud.APPEL_FONCTION && !code.endsWith("\n")) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String genererAffectation(NoeudAST n) {
        if (n.getEnfants().size() == 2) {
            String left = generer(n.getEnfant(0));
            String right = generer(n.getEnfant(1));
            return indent() + left + " = " + right + "\n";
        } else if (n.getEnfants().size() == 1) {
            String left = n.getValeur();
            String right = generer(n.getEnfant(0));
            return indent() + left + " = " + right + "\n";
        } else {
            return indent() + "# ERREUR: Affectation invalide\n";
        }
    }

    private String genererCondition(NoeudAST n) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent()).append("if ")
                .append(generer(n.getEnfant(0)))
                .append(":\n");

        indent++;
        if (n.getEnfants().size() > 1) {
            sb.append(generer(n.getEnfant(1)));
        } else {
            sb.append(indent()).append("pass\n");
        }
        indent--;

        if (n.getEnfants().size() > 2) {
            sb.append(indent()).append("else:\n");
            indent++;
            sb.append(generer(n.getEnfant(2)));
            indent--;
        }

        return sb.toString();
    }

    private String genererEcrire(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("print(");

        for (int i = 0; i < n.getEnfants().size(); i++) {
            if (i > 0) sb.append(", ");
            String arg = generer(n.getEnfant(i));

            if (n.getEnfant(i).getType() == NoeudAST.TypeNoeud.CHAINE) {
                sb.append(arg);
            } else {
                sb.append("str(").append(arg).append(")");
            }
        }

        sb.append(", sep=\"\")\n");
        return sb.toString();
    }

    private String genererLire(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        for (NoeudAST var : n.getEnfants()) {
            sb.append(indent()).append(generer(var)).append(" = input()\n");
        }
        return sb.toString();
    }

    private String genererSectionVar(NoeudAST n) {
        StringBuilder sb = new StringBuilder();

        for (NoeudAST decl : n.getEnfants()) {
            if (decl.getType() == NoeudAST.TypeNoeud.DECLARATION_VARIABLE) {
                NoeudAST listeId = null;
                NoeudAST typeNode = null;

                for (NoeudAST enfant : decl.getEnfants()) {
                    if (enfant.getType() == NoeudAST.TypeNoeud.LISTE_IDENTIFICATEURS) {
                        listeId = enfant;
                    } else if (enfant.getType() == NoeudAST.TypeNoeud.TYPE) {
                        typeNode = enfant;
                    }
                }

                if (listeId != null && typeNode != null) {
                    String type = typeNode.getValeur();

                    for (NoeudAST var : listeId.getEnfants()) {
                        String nomVar = var.getValeur();

                        if (type.contains("[")) {
                            int debut = type.indexOf('[');
                            int fin = type.indexOf(']');
                            String tailleStr = type.substring(debut + 1, fin);
                            sb.append(indent()).append(nomVar).append(" = [0] * ").append(tailleStr).append("\n");
                        } else {
                            switch (type.toLowerCase()) {
                                case "entier":
                                    sb.append(indent()).append(nomVar).append(" = 0\n");
                                    break;
                                case "reel":
                                    sb.append(indent()).append(nomVar).append(" = 0.0\n");
                                    break;
                                case "chaine":
                                case "chainedecharactere":
                                    sb.append(indent()).append(nomVar).append(" = \"\"\n");
                                    break;
                                case "booleen":
                                    sb.append(indent()).append(nomVar).append(" = False\n");
                                    break;
                                default:
                                    sb.append(indent()).append(nomVar).append(" = ").append(type).append("()\n");
                            }
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    private String genererDeclarationFonction(NoeudAST n) {
        StringBuilder sb = new StringBuilder();

        sb.append("def ").append(n.getValeur()).append("(");

        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.LISTE_PARAMETRES) {
                NoeudAST params = enfant;
                for (int i = 0; i < params.getEnfants().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(params.getEnfant(i).getValeur());
                }
                break;
            }
        }

        sb.append("):\n");

        indent++;

        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.SECTION_VAR) {
                sb.append(generer(enfant));
            }
        }

        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS ||
                    enfant.getType() == NoeudAST.TypeNoeud.RETOUR) {
                sb.append(generer(enfant));
            }
        }

        String content = sb.toString();
        if (content.endsWith("):\n")) {
            sb.append(indent()).append("pass\n");
        }

        indent--;
        sb.append("\n");
        return sb.toString();
    }

    private String genererDeclarationProcedure(NoeudAST n) {
        return genererDeclarationFonction(n);
    }

    private String genererRetour(NoeudAST n) {
        if (n.getEnfants().isEmpty()) {
            return indent() + "return\n";
        } else {
            // CORRECTION: Préserver les parenthèses dans les expressions
            return indent() + "return " + generer(n.getEnfant(0)) + "\n";
        }
    }

    private String genererBouclePour(NoeudAST n) {
        String var = n.getValeur();
        String start = generer(n.getEnfant(0));
        String end = generer(n.getEnfant(1));

        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("for ").append(var).append(" in range(")
                .append(start).append(", ").append(end).append(" + 1):\n");

        indent++;
        if (n.getEnfants().size() > 2) {
            sb.append(generer(n.getEnfant(2)));
        }
        indent--;

        return sb.toString();
    }

    private String genererBoucleTantque(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("while ").append(generer(n.getEnfant(0))).append(":\n");

        indent++;
        if (n.getEnfants().size() > 1) {
            sb.append(generer(n.getEnfant(1)));
        }
        indent--;

        return sb.toString();
    }

    private String genererBoucleRepeter(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("while True:\n");

        indent++;
        if (n.getEnfants().size() > 0) {
            sb.append(generer(n.getEnfant(0)));
        }

        if (n.getEnfants().size() > 1) {
            sb.append(indent()).append("if ").append(generer(n.getEnfant(1))).append(":\n");
            indent++;
            sb.append(indent()).append("break\n");
            indent--;
        }

        indent--;
        return sb.toString();
    }

    private String genererAppelFonction(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getValeur()).append("(");

        if (!n.getEnfants().isEmpty()) {
            NoeudAST argsNode = n.getEnfant(0);
            for (int i = 0; i < argsNode.getEnfants().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(generer(argsNode.getEnfant(i)));
            }
        }

        sb.append(")");
        return sb.toString();
    }

    private String genererExpression(NoeudAST n) {
        String op = n.getValeur();

        if ("ET".equals(op)) op = "and";
        else if ("OU".equals(op)) op = "or";
        else if ("=".equals(op)) op = "==";
        else if ("<>".equals(op)) op = "!=";

        // CORRECTION: Entourer de parenthèses pour préserver la priorité
        String gauche = generer(n.getEnfant(0));
        String droite = generer(n.getEnfant(1));

        // Ajouter des parenthèses si nécessaire
        return "(" + gauche + " " + op + " " + droite + ")";
    }

    private String genererAccesTableau(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getValeur()).append("[");

        if (!n.getEnfants().isEmpty()) {
            sb.append(generer(n.getEnfant(0)));
        }

        sb.append("]");
        return sb.toString();
    }

    private String genererValeurBooleenne(NoeudAST n) {
        String val = n.getValeur().toLowerCase();
        if ("vrai".equals(val)) return "True";
        if ("faux".equals(val)) return "False";
        return n.getValeur();
    }

    private String genererNegation(NoeudAST n) {
        if ("NON".equals(n.getValeur())) {
            return "not " + generer(n.getEnfant(0));
        } else {
            return "-" + generer(n.getEnfant(0));
        }
    }
}