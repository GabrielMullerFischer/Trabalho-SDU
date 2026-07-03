import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServidorRMI extends UnicastRemoteObject implements ContratoJogo {
    private static final long serialVersionUID = 1L;

    private FaseRodada faseRodada;
    private List<Jogador> jogadores;
    private Map<Integer, MensagemJogo> acoesRecebidas;
    private Mesa mesa;
    private int pote;
    private int limiteJogadores;
    private int numeroRodada;
    private final int LIMITE_EMPATES = 5;
    private int contadorEmpates;

    protected ServidorRMI(int limite) throws RemoteException {
        super();
        this.faseRodada = FaseRodada.AGUARDANDO;
        this.jogadores = new ArrayList<>();
        this.acoesRecebidas = new HashMap<>();
        this.pote = 0;
        this.limiteJogadores = limite;
        this.numeroRodada = 1;
        this.contadorEmpates = 1;
        this.mesa = new Mesa((limite * 10) / 2);
    }

    public Mesa getMesa() {
        return mesa;
    }

    public synchronized int getPote() {
        return this.pote;
    }

    public synchronized void zerarPote() {
        this.pote = 0;
    }

    @Override
    public synchronized int registrarJogador(String nome) throws RemoteException {
        if (faseRodada != FaseRodada.AGUARDANDO) {
            throw new RemoteException("Partida já em andamento.");
        }
        
        int id = jogadores.size();
        jogadores.add(new Jogador(id, nome)); 
        System.out.println("Jogador " + nome + " registrado com ID: " + id);

        if (jogadores.size() == limiteJogadores) {
            imprimirStatusMesa();
            System.out.print("-------------------------------- ");
            System.out.print("RODADA: " + numeroRodada);
            System.out.println(" -------------------------------");
            this.faseRodada = FaseRodada.APOSTA;
        }
        return id;
    }

    @Override
    public synchronized MensagemJogo aguardarSuaVez(int idJogador) throws RemoteException {
        if (faseRodada == FaseRodada.APOSTA) {
            Jogador j = buscarJogadorPorId(idJogador);
            int saldoDesteJogador = (j != null) ? j.getSaldo() : 0;
            return new MensagemJogo("STATUS_APOSTA", saldoDesteJogador);
        }
        return new MensagemJogo("STATUS", faseRodada);
    }

    @Override
    public synchronized void enviarAcao(int idJogador, MensagemJogo acao) throws RemoteException {
        acoesRecebidas.put(idJogador, acao);
        verificarFimDaFase();
    }

    private void verificarFimDaFase() {
        if (acoesRecebidas.size() == contarJogadoresAtivos()) {
            switch (faseRodada) {
                case APOSTA:
                    for (Map.Entry<Integer, MensagemJogo> entry : acoesRecebidas.entrySet()) {
                        Jogador j = buscarJogadorPorId(entry.getKey());
                        int aposta = (int) entry.getValue().getConteudo();
                        j.realizarAposta(aposta);
                        this.pote += aposta;
                        System.out.println(j.getNome() + " | Apostou: " + aposta + " fichas.");
                    }
                    faseRodada = FaseRodada.JOGADA;
                    break;

                case JOGADA:
                    List<Jogador> ativos = obterJogadoresAtivos();
                    for (Map.Entry<Integer, MensagemJogo> entry : acoesRecebidas.entrySet()) {
                        Jogador j = buscarJogadorPorId(entry.getKey());
                        j.setJogadaAtual((Jogada) entry.getValue().getConteudo());
                        System.out.println("O jogador " + j.getNome() + " escolheu: " + j.getJogadaAtual());
                    }

                    List<Jogador> vencedores = determinarVencedores(ativos);
                    if (vencedores == null || vencedores.isEmpty()) {
                        System.out.println("-----------------------------------------------------------------------------");
                        System.out.println("============================Empate na rodada!================================");
                        System.out.println("-----------------------------------------------------------------------------");
                        this.contadorEmpates = 1;
                        faseRodada = FaseRodada.DECISAO_DESEMPATE;
                    } else {
                        distribuirPremio(vencedores);
                        prepararProximaRodada();
                    }
                    break;

                case DECISAO_DESEMPATE:
                    for (Map.Entry<Integer, MensagemJogo> entry : acoesRecebidas.entrySet()) {
                        Jogador j = buscarJogadorPorId(entry.getKey());
                        if (j.getSaldo() < 1) {
                            System.out.println("Jogador " + j.getNome() + " esta sem fichas para apostar! ALL-IN");
                            System.out.println(j.getNome() + " continuou na rodada.");
                        } else {
                            int escolha = (int) entry.getValue().getConteudo();
                            if (escolha == 2) {
                                int totalApostado = j.getApostaRodadaAtual();
                                int recuperar = (int) Math.ceil(totalApostado / 2.0);
                                int paraMesa = totalApostado - recuperar;

                                j.adicionarSaldo(recuperar);
                                mesa.adicionarSaldo(paraMesa);
                                this.pote -= totalApostado;

                                System.out.println(j.getNome() + " desistiu e recuperou " + recuperar + " fichas.");
                                j.setAtivoNaRodada(false);
                            } else {
                                System.out.println(j.getNome() + " continuou na rodada.");
                            }
                        }
                    }
                    List<Jogador> continuam = obterJogadoresAtivos();
                    System.out.println("Continuam na disputa: " + continuam.size() + " jogadores.");
                    if (continuam.size() == 1) {
                        Jogador sobrevivente = continuam.get(0);
                        sobrevivente.adicionarSaldo(getPote());
                        System.out.println("########################################################################################");
                        System.out.println(sobrevivente.getNome() + " venceu o pote de " + getPote() + " fichas por desistencia dos outros!");
                        System.out.println("########################################################################################");
                        zerarPote();
                        prepararProximaRodada();
                    } else if (continuam.isEmpty()) {
                        System.out.println("########################################################################################");
                        System.out.println("Todos desistiram! O pote de " + getPote() + " fichas vai para a mesa.");
                        System.out.println("########################################################################################");
                        mesa.adicionarSaldo(getPote());
                        zerarPote();
                        prepararProximaRodada();
                    } else {
                        if (contadorEmpates < LIMITE_EMPATES) {
                            contadorEmpates++;
                            faseRodada = FaseRodada.APOSTA;
                        } else {
                            aplicarPenalidadeLimiteEmpates(continuam);
                            prepararProximaRodada();
                        }
                    }
                    break;

                default:
                    break;
            }
            acoesRecebidas.clear();
        }
    }

    private void prepararProximaRodada() {
        imprimirStatusMesa();
        if (contarTotalJogadoresComSaldo() < 2) {
            faseRodada = FaseRodada.AGUARDANDO;
            declararCampeaoFinal();
        } else {
            numeroRodada++;
            System.out.print("-------------------------------- ");
            System.out.print("RODADA: " + numeroRodada);
            System.out.println(" -------------------------------");
            for (Jogador j : jogadores) {
                j.resetarRodada();
                if (j.getSaldo() <= 0) {
                    j.setAtivoNaRodada(false);
                }
            }
            faseRodada = FaseRodada.APOSTA;
        }
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
        
        int valorPote = getPote();
        int cadaUmLeva = valorPote / vencedores.size();
        int restoParaMesa = valorPote % vencedores.size();

        for (Jogador j : vencedores) {
            j.adicionarSaldo(cadaUmLeva);
            System.out.println("########################################################################################");
            System.out.println("Jogador " + j.getNome() + " venceu e recebeu " + cadaUmLeva + " fichas do pote.");
            System.out.println("########################################################################################");
        }

        if (restoParaMesa > 0) {
            mesa.adicionarSaldo(restoParaMesa);
            System.out.println("A mesa recebeu " + restoParaMesa + " fichas de sobra de divisão.");
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

    private void imprimirStatusMesa() {
        System.out.println("-----------------------------------------------------------------------------");
        System.out.println("STATUS DA MESA");
        for (Jogador j : jogadores) {
            System.out.println("Jogador " + j.getNome() + " - Saldo: " + j.getSaldo() + " fichas");
        }
        System.out.println("Saldo da Mesa : " + mesa.getSaldo() + " fichas");
        System.out.println("-----------------------------------------------------------------------------");
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
        System.out.println("\nPote: " + getPote() + " fichas.");
        if (vencedor.getSaldo() <= 0) {
            System.out.println("Todos os jogadores ficaram sem fichas! Ninguém venceu.");
        } else {
            System.out.println("O vencedor foi: " + vencedor.getNome().toUpperCase());
            System.out.println("Saldo Final: " + vencedor.getSaldo() + " fichas.");
        }
        System.out.println("A mesa ficou com: " + mesa.getSaldo() + " fichas.");
        System.out.println("-----------------------------");
    }

    private int contarJogadoresAtivos() {
        int cont = 0;
        for (Jogador j : jogadores) {
            if (j.isAtivoNaRodada()) cont++;
        }
        return cont;
    }

    private int contarTotalJogadoresComSaldo() {
        int cont = 0;
        for (Jogador j : jogadores) {
            if (j.getSaldo() > 0) cont++;
        }
        return cont;
    }

    private List<Jogador> obterJogadoresAtivos() {
        List<Jogador> ativos = new ArrayList<>();
        for (Jogador j : jogadores) {
            if (j.isAtivoNaRodada()) ativos.add(j);
        }
        return ativos;
    }

    private Jogador buscarJogadorPorId(int id) {
        for (Jogador j : jogadores) {
            if (j.getId() == id) return j;
        }
        return null;
    }
}