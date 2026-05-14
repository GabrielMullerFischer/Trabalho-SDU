import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ThreadLocalRandom;

public class Jogador extends java.lang.Thread {
    private String nome;
    private int saldo;
    private int apostaRodadaAtual;
    private Jogada jogadaAtual;
    private boolean ativoNaRodada;
    private Jogo jogo;
    private Socket socketCliente;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Jogador(String nome, Socket socketCliente, ObjectOutputStream out, ObjectInputStream in) {
        this.nome = nome;
        this.socketCliente = socketCliente;
        this.out = out;
        this.in = in;
        this.saldo = 10;
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

    @Override
    public void run() {
        try {
            socketCliente.setSoTimeout(30000);
            FaseRodada ultimaFase = FaseRodada.AGUARDANDO;
            while (jogo.isRodando()) {
                FaseRodada fase = jogo.aguardarComando(ultimaFase);
                if (!ativoNaRodada) {
                    ultimaFase = fase;
                    jogo.sinalizarPronto();
                    continue;
                }
                if (fase == FaseRodada.APOSTA) {
                    mensagemRede("APOSTA", this.saldo);
                } else if (fase == FaseRodada.JOGADA) {
                    mensagemRede("JOGADA", null);
                } else if (fase == FaseRodada.DECISAO_DESEMPATE) {
                    mensagemRede("DECISAO_DESEMPATE", null);
                }
                ultimaFase = fase;
                jogo.sinalizarPronto();
            }
        } catch (SocketTimeoutException e) {
            System.err.println("O jogador demorou demais! Timeout atingido.");
        } catch (EOFException | SocketException e) {
            System.err.println("Jogador desconectou abruptamente.");
        } catch (Exception e) {
            System.err.println("Erro na comunicação: " + e.getMessage());
        } finally {
            if (jogo.isRodando()){
                jogo.resetarRodada();
                finalizaConexao();
                jogo.sinalizarPronto();
            }
        }
    }

    private void mensagemRede(String tipo, Object dado) throws Exception {
        out.writeObject(new MensagemJogo(tipo, dado));
        out.flush();
        MensagemJogo resposta = (MensagemJogo) in.readObject();
        if (tipo.equals("APOSTA")) {
            int aposta = (int) resposta.getConteudo();
            this.realizarAposta(aposta);
            jogo.adicionarAoPote(aposta);
            System.out.println(getNome() + " | Apostou: " + aposta + " fichas.");
        } else if (tipo.equals("JOGADA")) {
            this.setJogadaAtual((Jogada) resposta.getConteudo());
            System.out.println("\nO jogador " + getNome() + " escolheu: " + this.getJogadaAtual());
        } else if (tipo.equals("DECISAO_DESEMPATE")) {
            if (this.getSaldo() < 1) {
                System.out.println("Jogador " + getNome() + " esta sem fichas para apostar! ALL-IN");
                System.out.println(getNome() + " continuou na rodada.");
            } else {
                int escolha = (int) resposta.getConteudo();
                if (escolha == 2){
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
    }

    private synchronized void finalizaConexao() {
        try {
            if (socketCliente != null && !socketCliente.isClosed()) {
            this.ativoNaRodada = false;
            if (jogo.isRodando()) {
                jogo.getMesa().adicionarSaldo(this.saldo);
                this.saldo = 0;
            }
            if (out != null) out.close();
            if (in != null) in.close();
            socketCliente.close();
            }
        } catch (IOException e) { 
        }
    }
}
