public enum Jogada {
    PEDRA, PAPEL, TESOURA;

    public static Jogada fromInt(int valor) {
        switch (valor) {
            case 1: return PEDRA;
            case 2: return PAPEL;
            case 3: return TESOURA;
            default: 
                throw new IllegalArgumentException("Valor invalido!");
        }
    }
}
