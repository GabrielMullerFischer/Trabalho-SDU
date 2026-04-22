import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Jogador extends java.lang.Thread {
    private String nome;
    private int saldo;
    private int apostaRodadaAtual;
    private Jogada jogadaAtual;
    private boolean ativoNaRodada;
    private Jogo jogo;

    public Jogador(String nome) {
        this.nome = nome;
        this.saldo = 10;
        this.apostaRodadaAtual = 0;
        this.ativoNaRodada = true;
    }

    public String getNome() {
        return nome;
    }

    public void setJogo(Jogo jogo) {
        this.jogo = jogo;
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

    public synchronized int gerarAleatorio(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

public void run() {
    FaseRodada ultimaFaseProcessada = FaseRodada.AGUARDANDO;
    while (jogo.isRodando()) {
        FaseRodada fase = jogo.aguardarComando();
        if (!jogo.isRodando()) {
            break;
        }
        if (fase == ultimaFaseProcessada) {
            Thread.yield();
            continue;
        }
        if (!ativoNaRodada) {
            jogo.sinalizarPronto();
            ultimaFaseProcessada = fase;
            continue;
        } else {
            if (fase == FaseRodada.APOSTA) {
                processarAposta();
                ultimaFaseProcessada = fase;
                jogo.sinalizarPronto();
            } 
            else if (fase == FaseRodada.JOGADA) {
                processarJogada();
                ultimaFaseProcessada = fase;
                jogo.sinalizarPronto();
            } 
            else if (fase == FaseRodada.DECISAO_DESEMPATE) {
                processarDesempate();
                ultimaFaseProcessada = fase;
                jogo.sinalizarPronto();
            } 
            else if (fase == FaseRodada.AGUARDANDO) {
                ultimaFaseProcessada = fase;
                jogo.sinalizarPronto();
            }
        }
    }
}

    private void processarAposta() {
        if (this.getSaldo() < 1) {
            // ALL-IN
        } else {
            int aposta;
            if (jogo.isAuto()) {
                aposta = gerarAleatorio(1, this.getSaldo());
                System.out.println(getNome() + " | Apostou: " + aposta + " fichas.");
            } else {
                synchronized (Scanner.class) {
                    System.out.print("\n - " + this.getNome() + ", sua aposta (Saldo: " + this.getSaldo() + "): ");
                    aposta = jogo.lerInteiro(1, this.getSaldo());
                }
            }
            this.realizarAposta(aposta);
            jogo.adicionarAoPote(aposta);
        }
    }

    private void processarJogada() {
        int escolha;
        if (jogo.isAuto()) {
            escolha = gerarAleatorio(1, 3);
            System.out.println(getNome() + " | Escolheu (1 - Pedra | 2 - Papel | 3 - Tesoura): " + escolha);
        } else {
            synchronized (Scanner.class) {
                System.out.println("-----------------------------------------------------------------------------");
                System.out.print("\nVez de " + getNome() + " | Escolha (1 - Pedra | 2 - Papel | 3 - Tesoura): ");
                escolha = jogo.lerInteiro(1, 3);
            }
        }
        this.setJogadaAtual(Jogada.fromInt(escolha));
    }

    private void processarDesempate() {
    int escolha;
    if (this.getSaldo() < 1) {
        System.out.println("Jogador " + getNome() + " esta sem fichas para apostar! ALL-IN");
        return; 
    }

    if (jogo.isAuto()) {
        escolha = gerarAleatorio(1, 2);
        System.out.println(getNome() + " | Escolheu (1 - Continuar e Apostar | 2 - Desistir e perder metade): " + escolha);
    } else {
        synchronized (Scanner.class) {
            System.out.println("\n--- DESEMPATE: " + getNome() + " ---");
            System.out.print("(1) Continuar e Apostar ou (2) Desistir e perder metade: ");
            escolha = jogo.lerInteiro(1, 2);
        }
    }

    if (escolha == 2) {
        int totalApostado = this.getApostaRodadaAtual();
        int recuperar = (int) Math.ceil(totalApostado / 2.0);
        int paraMesa = totalApostado - recuperar;

        this.adicionarSaldo(recuperar);
        jogo.getMesa().adicionarSaldo(paraMesa);
        jogo.subtrairDoPote(totalApostado);

        System.out.println(getNome() + " desistiu e recuperou " + recuperar + " fichas.");
        this.setAtivoNaRodada(false);
    } else {
        System.out.println(getNome() + " continuou na rodada.");
    }
    }
}
