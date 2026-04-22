import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class Jogo {
    private List<Jogador> jogadores;
    private Mesa mesa;
    private Scanner scanner;
    private int pote;
    private boolean jogoEncerrado;
    private int LIMITE_EMPATES;
    private boolean auto;
    private FaseRodada faseAtual;
    private int threadsProntas;
    private int threadsEsperadas;
    private int rodadaAtualBarreira;

    public Jogo(List<Jogador> jogadores, Scanner scanner) {
        this.jogadores = jogadores;
        this.mesa = new Mesa((jogadores.size() * 10) / 2);
        this.scanner = scanner;
        this.rodadaAtualBarreira = 0;
        this.pote = 0;
        this.jogoEncerrado = false;
        this.LIMITE_EMPATES = 5;
        this.auto = false;
        this.faseAtual = FaseRodada.AGUARDANDO;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public void iniciar() {
        int numeroRodada = 1;
        imprimirStatusMesa();
        System.out.print("Jogar automaticamente? (s/n): ");
        String resposta = scanner.next();
        if (resposta.equalsIgnoreCase("s")) {
            this.auto = true;
        }

        for (Jogador j : jogadores) {
            j.setJogo(this);
            j.start();
        }

        while (!this.jogoEncerrado) {
            this.rodadaAtualBarreira++;
            executarRodada(numeroRodada);
            numeroRodada++;
        }
        synchronized(this) {
            notifyAll(); 
        }

        for (Jogador j : jogadores) {
            try {
                j.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        declararCampeaoFinal();
    }

    private void executarRodada(int numeroRodada) {
        if (contarJogadoresComSaldo() < 2) {
            this.jogoEncerrado = true;
            return;
        }

        List<Jogador> ativos = new ArrayList<>();

        for (Jogador j : this.jogadores) {
            j.resetarRodada();
            if (j.getSaldo() > 0) {
                ativos.add(j);
            } else {
                j.setAtivoNaRodada(false);
            }
        }

        if (ativos.size() < 2) {
            this.jogoEncerrado = true;
            return;
        }

        System.out.print("-------------------------------- ");
        System.out.print("RODADA: " + numeroRodada);
        System.out.println(" -------------------------------");

        abrirBarreira(FaseRodada.APOSTA, ativos.size());

        abrirBarreira(FaseRodada.JOGADA, ativos.size());

        List<Jogador> vencedores = determinarVencedores(ativos);
        if (vencedores == null || vencedores.isEmpty()) {
            System.out.println("Empate na rodada! Iniciando congelamento e desempate...");
            executarSubRodadaDesempate(ativos);
        } else {
            distribuirPremio(vencedores);
        }
        abrirBarreira(FaseRodada.AGUARDANDO, ativos.size());
        imprimirStatusMesa();
    }

    private void imprimirStatusMesa() {
        System.out.println("-----------------------------------------------------------------------------");
        System.out.println("STATUS DA MESA");
        for (Jogador j : jogadores) {
            System.out.println("Jogador " + j.getNome() + " - Saldo: " + j.getSaldo() + " fichas");
        }
        System.out.println("Saldo da Mesa : " + mesa.getSaldo() + " fichas");
        System.out.println("-----------------------------------------------------------------------------");
    }

    private int executarSubRodadaDesempate(List<Jogador> ativos) {
        int contadorEmpates = 1;
        while (contadorEmpates < this.LIMITE_EMPATES) {
            abrirBarreira(FaseRodada.DECISAO_DESEMPATE, ativos.size());

            List<Jogador> continuam = new ArrayList<>();
            for (Jogador j : ativos) {
                if (j.isAtivoNaRodada()) {
                    continuam.add(j);
                }
            }
            System.out.println("Continumam na disputa: " + continuam.size() + " jogadores.");

            if (continuam.size() == 1) {
                continuam.get(0).adicionarSaldo(getPote());
                System.out.println("########################################################################################");
                System.out.println(continuam.get(0).getNome() + " venceu o pote de " + getPote() + " fichas por desistencia dos outros!");
                System.out.println("########################################################################################");
                zerarPote();
                declararCampeaoFinal();
                return 0;
            }

            if (continuam.isEmpty()) {
                System.out.println("########################################################################################");
                System.out.println("Todos desistiram! O pote de " + getPote() + " fichas vai para a mesa.");
                System.out.println("########################################################################################");
                mesa.adicionarSaldo(getPote());
                zerarPote();
                return 0;
            }

            abrirBarreira(FaseRodada.APOSTA, continuam.size());

            abrirBarreira(FaseRodada.JOGADA, continuam.size());

            List<Jogador> vencedores = determinarVencedores(continuam);

            if (vencedores != null && !vencedores.isEmpty()) {
                distribuirPremio(vencedores);
                return 0;
            }

            ativos = continuam;
            contadorEmpates++;
        }

        aplicarPenalidadeLimiteEmpates(ativos);
        return 0;
    }

    private List<Jogador> determinarVencedores(List<Jogador> ativos) {
        boolean temPedra = false, temPapel = false, temTesoura = false;
        for (Jogador j : ativos) {
            if (j.getJogadaAtual() == Jogada.PEDRA) temPedra = true;
            if (j.getJogadaAtual() == Jogada.PAPEL) temPapel = true;
            if (j.getJogadaAtual() == Jogada.TESOURA) temTesoura = true;
        }

        int tiposDiferentes = (temPedra ? 1 : 0) + (temPapel ? 1 : 0) + (temTesoura ? 1 : 0);
        if (tiposDiferentes != 2) return null;

        Jogada vencedor;
        if (temPedra && temTesoura) vencedor = Jogada.PEDRA;
        else if (temTesoura && temPapel) vencedor = Jogada.TESOURA;
        else vencedor = Jogada.PAPEL;

        List<Jogador> vencedores = new ArrayList<>();
        for (Jogador j : ativos) {
            if (j.getJogadaAtual() == vencedor) vencedores.add(j);
        }
        return vencedores;
    }

    private void distribuirPremio(List<Jogador> vencedores) {
        if (vencedores.isEmpty() || vencedores == null) return;
        System.out.println("Pote : " + getPote() + " fichas.");
        for(Jogador j : vencedores) {
            int recebe = j.getApostaRodadaAtual() * 2;
            if(getPote() >= recebe){
                subtrairDoPote(recebe);
            } else {
                if(mesa.getSaldo() + getPote() < recebe) {
                    System.out.println("A MESA QUEBROU!!!");
                    int recebeMenos = getPote() + mesa.getSaldo();
                    j.adicionarSaldo(recebeMenos);
                    System.out.println("########################################################################################");
                    System.out.println("Jogador " + j.getNome() + " venceu e recebeu " + recebeMenos + " fichas.");
                    System.out.println("########################################################################################");
                    mesa.deduzirSaldo(mesa.getSaldo());
                    this.jogoEncerrado = true;
                    zerarPote();
                    return;
                }
                int valorMesa = recebe - getPote();
                System.out.println("########################################################################################");
                System.out.println("A mesa perdeu " + valorMesa + " fichas para completar o pagamento ao jogador!");
                System.out.println("########################################################################################");
                mesa.deduzirSaldo(valorMesa);
                zerarPote();
            }
            j.adicionarSaldo(recebe);
            System.out.println("########################################################################################");
            System.out.println("Jogador " + j.getNome() + " venceu e recebeu " + recebe + " fichas.");
            System.out.println("########################################################################################");
        }

        if (getPote() > 0) {
            mesa.adicionarSaldo(getPote());
            System.out.println("A mesa recebeu " + getPote() + " fichas excedentes.");
            System.out.println("########################################################################################");
        }
        zerarPote();
    }

    private void aplicarPenalidadeLimiteEmpates(List<Jogador> ativos) {
        System.out.println("Limite de empates atingido!");
        for (Jogador j : ativos) {
            j.adicionarSaldo(j.getApostaRodadaAtual());

            if (j.getSaldo() > 0) {
                j.deduzirSaldo(1);
                mesa.adicionarSaldo(1);
            }
        }
        zerarPote();
        System.out.println("Apostas devolvidas e 1 ficha de penalidade retirada de cada jogador.");
    }

    private int contarJogadoresComSaldo() {
        int cont = 0;
        for (Jogador j : jogadores) {
            if (j.getSaldo() > 0) {
                cont++;
            }
        }
        return cont;
    }

    private void declararCampeaoFinal() {
        System.out.println("\nFIM DE JOGO");
        imprimirStatusMesa();
        Jogador vencedor = jogadores.get(0);

        for (Jogador j : jogadores) {
            if (j.getSaldo() > vencedor.getSaldo()) {
                vencedor = j;
            }
        }

        System.out.println("Pote: " + getPote() + " fichas.");

        if(vencedor.getSaldo() <= 0) {
            System.out.println("Todos os jogadores ficaram sem fichas! Ninguém venceu.");
        } else {
            System.out.println("O vencedor foi: " + vencedor.getNome().toUpperCase());
            System.out.println("Saldo Final: " + vencedor.getSaldo() + " fichas.");
        }
        System.out.println("A mesa ficou com: " + mesa.getSaldo() + " fichas.");
        System.out.println("-----------------------------");
    }

    public int gerarAleatorio(int min, int max) {
        int temp = ThreadLocalRandom.current().nextInt(min, max + 1);
        System.out.println(temp);
        return temp;
    }

    public synchronized void setPote(int valor) {
        this.pote = valor;
    }

    public synchronized void adicionarAoPote(int valor) {
        this.pote += valor;
    }

    public synchronized void subtrairDoPote(int valor) {
        this.pote -= valor;
    }

    public synchronized void zerarPote() {
        this.pote = 0;
    }

    public synchronized int getPote() {
        return this.pote;
    }

    public synchronized void sinalizarPronto() {
        threadsProntas++;
        notifyAll(); 
    }

    public synchronized int lerInteiro(int min, int max) {
        int valor = -1;
        while (true) {
            try {
                if (scanner.hasNextInt()) {
                    valor = scanner.nextInt();
                    if (valor >= min && valor <= max) {
                        return valor;
                    }
                } else {
                    scanner.next();
                }
                System.out.print("Entrada invalida. Digite entre " + min + " e " + max + ": ");
            } catch (Exception e) {
                System.out.println("Erro tente novamente: ");
            }
        }
    }

    public boolean isRodando() {
        return !this.jogoEncerrado;
    }


    public boolean isAuto() {
        return this.auto;
    }

    public synchronized FaseRodada aguardarComando() {
        try {
            wait(); 
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return this.faseAtual;
    }

    private void abrirBarreira(FaseRodada novaFase, int ativos) {
        synchronized(this) {
            this.faseAtual = novaFase;
            this.threadsProntas = 0;
            this.threadsEsperadas = ativos;
            notifyAll();
        }

        synchronized(this) {
            while (threadsProntas < threadsEsperadas) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public synchronized int getRodadaAtualBarreira() {
        return rodadaAtualBarreira;
    }
}