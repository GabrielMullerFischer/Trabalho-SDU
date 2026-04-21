public class Jogador {
    private String nome;
    private int saldo;
    private int apostaRodadaAtual;
    private Jogada jogadaAtual;
    private boolean ativoNaRodada;

    public Jogador(String nome) {
        this.nome = nome;
        this.saldo = 10;
        this.apostaRodadaAtual = 0;
        this.ativoNaRodada = true;
    }

    public String getNome() {
        return nome;
    }

    public int getSaldo() {
        return saldo;
    }

    public int getApostaRodadaAtual() {
        return apostaRodadaAtual;
    }

    public Jogada getJogadaAtual() {
        return jogadaAtual;
    }

    public boolean isAtivoNaRodada() {
        return ativoNaRodada;
    }

    public void setAtivoNaRodada(boolean ativo) {
        this.ativoNaRodada = ativo;
    }

    public void setJogadaAtual(Jogada jogada) {
        this.jogadaAtual = jogada;
    }

    public void adicionarSaldo(int valor) {
        this.saldo += valor;
    }

    public void deduzirSaldo(int valor) {
        this.saldo -= valor;
    }

    public void realizarAposta(int valor) {
        this.apostaRodadaAtual += valor;
        deduzirSaldo(valor);
    }

    public void resetarRodada() {
        this.apostaRodadaAtual = 0;
        this.jogadaAtual = null;
        this.ativoNaRodada = true;
    }
}
