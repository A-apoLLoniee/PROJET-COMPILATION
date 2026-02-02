public abstract class GenerateurCode {
    protected int indent = 0;

    protected String indent() {
        return "    ".repeat(indent);
    }

    public abstract String generer(NoeudAST noeud);
}
