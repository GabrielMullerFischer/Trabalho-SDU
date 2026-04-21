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
    private int threadsProntas;
    private int threadsEsperadas;

    public Jogo(List<Jogador> jogadores, Scanner scanner) {
        this.jogadores = jogadores;
        this.mesa = new Mesa((jogadores.size() * 10) / 2);
        this.scanner = scanner;
        this.pote = 0;
        this.jogoEncerrado = false;
        this.LIMITE_EMPATES = 5;
        this.auto = false;
    }

    public void iniciar() {
        int numeroRodada = 1;
        imprimirStatusMesa();
        System.out.print("Jogar automaticamente? (s/n): ");
        String resposta = scanner.next();
        if (resposta.equalsIgnoreCase("s")) {
            this.auto = true;
        }
        while (!this.jogoEncerrado) {
            System.out.print("----------------------------- ");
            System.out.print("RODADA: " + numeroRodada);
            System.out.print(" -----------------------------");
            executarRodada(numeroRodada);
            imprimirStatusMesa();
            numeroRodada++;
            if(numeroRodada > 20) scanner.nextLine(); // Limpa o buffer para evitar problemas de entrada após 20 rodadas
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

        for (Jogador j : ativos) {
            this.pote += solicitarAposta(j, 1);
        }

        coletarJogadasOcultas(ativos);

        List<Jogador> vencedores = determinarVencedores(ativos);

        if (vencedores == null || vencedores.isEmpty()) {
            System.out.println("Empate na rodada! Iniciando congelamento e desempate...");
            this.pote = executarSubRodadaDesempate(ativos);
        } else {
            distribuirPremio(ativos, vencedores);
        }
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

    private int solicitarAposta(Jogador j, int apostaMinima) {
        int aposta = 0;
        while (true) {
            System.out.print("\n - " + j.getNome() + " sua aposta: ");
            if(this.auto){
                aposta = gerarAleatorio(apostaMinima, j.getSaldo());
                break;
            } else {
                if (scanner.hasNextInt()) {
                    aposta = scanner.nextInt();
                    if (aposta >= apostaMinima && aposta <= j.getSaldo()) {
                        break;
                    } else {
                        System.out.println("Aposta invalida! Sua aposta deve ser no minimo " + apostaMinima + " e maximo " + j.getSaldo() + " fichas.");
                    }
                } else {
                    System.out.println("Entrada invalida. Por favor, insira um numero inteiro.");
                    scanner.next();
                }
            }
        }

        j.realizarAposta(aposta);
        return aposta;
    }

    private void coletarJogadasOcultas(List<Jogador> ativos) {
        for (Jogador j : ativos) {
            //limparConsole();
            int escolha = 0;
            while (true) {
                System.out.println("-----------------------------------------------------------------------------");
                System.out.print("\nVez de " + j.getNome() + " | Escolha sua jogada (1 - Pedra | 2 - Papel | 3 - Tesoura): ");

                if(this.auto){
                    escolha = gerarAleatorio(1, 3);
                    break;
                } else {
                    if (scanner.hasNextInt()) {
                        escolha = scanner.nextInt();
                        if (escolha >= 1 && escolha <= 3) {
                            j.setJogadaAtual(Jogada.fromInt(escolha));
                            break;
                        } else {
                            System.out.println("Escolha invalida! Digite 1, 2 ou 3.");
                        }
                    } else {
                        System.out.println("Entrada invalida. Por favor, insira um numero inteiro.");
                        scanner.next();
                    }
                }
            }
            //limparConsole();
        }
    }

    public void limparConsole() {
        try {
            final String os = System.getProperty("os.name");

            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                Runtime.getRuntime().exec("clear");
            }
        } catch (final Exception e) {
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }

    private int executarSubRodadaDesempate(List<Jogador> ativos) {
        int contadorEmpates = 1;
        while (contadorEmpates < this.LIMITE_EMPATES) {
            List<Jogador> continuam = new ArrayList<>();
            List<Jogador> desistentes = new ArrayList<>();

            System.out.println("\nSUB-RODADA DE DESEMPATE - Empate " + contadorEmpates + "/" + this.LIMITE_EMPATES + "!");
            for (Jogador j : ativos) {
                if (j.getSaldo() < 1) {
                    int allIn = j.getSaldo();
                    System.out.println("Jogador " + j.getNome() + " esta sem fichas para apostar! ALL-IN");
                    j.realizarAposta(allIn);
                    continuam.add(j);
                }else {
                    System.out.println(j.getNome() + ", você deseja (1) Continuar ou (2) Desistir?");
                    int escolha = 0;
                    if (this.auto){
                        escolha = gerarAleatorio(1, 2);
                    } else {
                        escolha = scanner.nextInt();
                    }
                    if (escolha == 1) {
                        this.pote += solicitarAposta(j, 1);
                        continuam.add(j);
                    } else {
                        int totalApostado = j.getApostaRodadaAtual();
                        int recuperar = (int) Math.ceil(totalApostado / 2.0);
                        int paraMesa = totalApostado - recuperar;

                        j.adicionarSaldo(recuperar);
                        mesa.adicionarSaldo(paraMesa);
                        this.pote -= totalApostado;

                        System.out.println(j.getNome() + " desistiu e recuperou " + recuperar + " fichas.");
                        desistentes.add(j);
                    }
                }
            }

            if (continuam.size() == 1) {
                continuam.get(0).adicionarSaldo(this.pote);
                System.out.println(continuam.get(0).getNome() + " venceu o pote de " + this.pote + " fichas por desistencia dos outros!");
                this.pote = 0;
                return 0;
            }

            if (continuam.isEmpty()) return 0;

            coletarJogadasOcultas(continuam);
            List<Jogador> vencedores = determinarVencedores(continuam);

            if (vencedores != null && !vencedores.isEmpty()) {
                distribuirPremio(continuam, vencedores);
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

    private void distribuirPremio(List<Jogador> ativos, List<Jogador> vencedores) {
        for(Jogador j : vencedores) {
            int recebe = j.getApostaRodadaAtual() * 2;
            if(this.pote >= recebe){
                this.pote -= recebe;
            } else {
                if(mesa.getSaldo() + this.pote < recebe) {
                    System.out.println("A MESA QUEBROU!!!");
                    int recebeMenos = this.pote + mesa.getSaldo();
                    j.adicionarSaldo(recebeMenos);
                    System.out.println("########################################################################################");
                    System.out.println("Jogador " + j.getNome() + " venceu e recebeu " + recebeMenos + " fichas.");
                    System.out.println("########################################################################################");
                    mesa.deduzirSaldo(mesa.getSaldo());
                    this.jogoEncerrado = true;
                    this.pote = 0;
                    return;
                }
                mesa.deduzirSaldo(recebe - this.pote);
                this.pote = 0;
            }
            j.adicionarSaldo(recebe);
            System.out.println("########################################################################################");
            System.out.println("Jogador " + j.getNome() + " venceu e recebeu " + recebe + " fichas.");
            System.out.println("########################################################################################");
        }

        if (this.pote > 0) {
            mesa.adicionarSaldo(this.pote);
            System.out.println("A mesa recebeu " + this.pote + " fichas excedentes.");
        }
        this.pote = 0;
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
        this.pote = 0;
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
        System.out.println("-----------------------------");
        System.out.println("\nFIM DE JOGO");
        Jogador vencedor = jogadores.get(0);


        for (Jogador j : jogadores) {
            if (j.getSaldo() > vencedor.getSaldo()) {
                vencedor = j;
            }
        }

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

    /* 

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

        if (threadsProntas >= threadsEsperadas) {
            notifyAll(); 
        } else {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
        */
}