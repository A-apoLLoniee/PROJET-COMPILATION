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

        default:
            return "";
        }
    }

    private String genererProgramme(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        for (NoeudAST enfant : n.getEnfants()) {
            sb.append(generer(enfant));
        }
        return sb.toString();
    }


    private String genererBloc(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        for (NoeudAST instr : n.getEnfants()) {
            sb.append(generer(instr));
        }
        return sb.toString();
    }


    private String genererAffectation(NoeudAST n) {
        String left;
        // Si c'est un accès tableau, l'enfant 0 est l'accès (ex: tab[indice])
        if (n.getEnfants().size() > 0 && n.getEnfant(0).getType() == NoeudAST.TypeNoeud.ACCES_TABLEAU) {
            left = generer(n.getEnfant(0));
        } else {
            left = n.getValeur();
        }

        String right = "";
        if (n.getEnfants().size() == 2) {
            right = generer(n.getEnfant(1));
        } else if (n.getEnfants().size() == 1) {
            right = generer(n.getEnfant(0));
        }

        return indent() + left + " = " + right + "\n";
    }


    private String genererCondition(NoeudAST n) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent()).append("if ")
        .append(generer(n.getEnfant(0)))
        .append(":\n");

        indent++;
        sb.append(generer(n.getEnfant(1)));
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
            sb.append(generer(n.getEnfant(i)));
        }
        sb.append(")\n");
        return sb.toString();
    }


    private String genererLire(NoeudAST n) {
        // utiliser generer pour supporter variables ou accès tableau
        return indent() + generer(n.getEnfant(0)) + " = input()\n";
    }

    private String genererAppelFonction(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        sb.append(n.getValeur()).append("(");
        if (n.getEnfants().size() > 0) {
            NoeudAST args = n.getEnfant(0);
            for (int i = 0; i < args.getEnfants().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(generer(args.getEnfant(i)));
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String genererExpression(NoeudAST n) {
        String op = n.getValeur();
        if ("ET".equals(op)) op = "and";
        else if ("OU".equals(op)) op = "or";
        return generer(n.getEnfant(0))
            + " " + op + " "
            + generer(n.getEnfant(1));
    }

    private String genererDeclarationFonction(NoeudAST n) {
        StringBuilder sb = new StringBuilder();
        // Nom et paramètres
        sb.append("def ").append(n.getValeur()).append("(");
        if (n.getEnfants().size() > 0 && n.getEnfant(0).getType() == NoeudAST.TypeNoeud.LISTE_PARAMETRES) {
            NoeudAST params = n.getEnfant(0);
            for (int i = 0; i < params.getEnfants().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(params.getEnfant(i).getValeur());
            }
        }
        sb.append("):\n");

        // Corps et retour
        indent++;
        boolean hasBody = false;
        for (NoeudAST enfant : n.getEnfants()) {
            if (enfant.getType() == NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS) {
                sb.append(generer(enfant));
                hasBody = true;
            }
        }
        // Expression de retour (dernier enfant après le bloc)
        NoeudAST retourExpr = null;
        for (int i = n.getEnfants().size() - 1; i >= 0; i--) {
            NoeudAST enfant = n.getEnfant(i);
            if (enfant.getType() != NoeudAST.TypeNoeud.TYPE && enfant.getType() != NoeudAST.TypeNoeud.LISTE_PARAMETRES) {
                // Si ce n'est pas le type ou la liste de params, on le considère comme retour/expression
                retourExpr = enfant;
                break;
            }
        }
        if (retourExpr != null && retourExpr.getType() != NoeudAST.TypeNoeud.BLOC_INSTRUCTIONS) {
            sb.append(indent()).append("return ").append(generer(retourExpr)).append("\n");
            hasBody = true;
        }
        if (!hasBody) {
            sb.append(indent()).append("pass\n");
        }
        indent--;

        sb.append("\n");
        return sb.toString();
    }

    private String genererRetour(NoeudAST n) {
        if (n.getEnfants().size() > 0) {
            return indent() + "return " + generer(n.getEnfant(0)) + "\n";
        }
        return indent() + "return\n";
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
        String cond = generer(n.getEnfant(0));
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("while ").append(cond).append(":\n");
        indent++;
        if (n.getEnfants().size() > 1) {
            sb.append(generer(n.getEnfant(1)));
        }
        indent--;
        return sb.toString();
    }

    private String genererBoucleRepeter(NoeudAST n) {
        // REPETER bloc JUSQUA expr  -> translate as do-while: while True: bloc; if expr: break
        NoeudAST bloc = n.getEnfant(0);
        NoeudAST cond = n.getEnfant(1);
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("while True:\n");
        indent++;
        if (bloc != null) sb.append(generer(bloc));
        sb.append(indent()).append("if ").append(generer(cond)).append(":\n");
        indent++;
        sb.append(indent()).append("break\n");
        indent -= 2;
        return sb.toString();
    }

    private String genererAccesTableau(NoeudAST n) {
        String name = n.getValeur();
        String idx = n.getEnfants().isEmpty() ? "" : generer(n.getEnfant(0));
        return name + "[" + idx + "]";
    }

    private String genererValeurBooleenne(NoeudAST n) {
        String v = n.getValeur();
        if ("VRAI".equalsIgnoreCase(v) || "true".equalsIgnoreCase(v)) return "True";
        if ("FAUX".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) return "False";
        return v;
    }

    private String genererNegation(NoeudAST n) {
        return "not " + generer(n.getEnfant(0));
    }

}
