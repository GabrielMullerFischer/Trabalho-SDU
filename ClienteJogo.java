import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class ClienteJogo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n\n\t===PEDRA-PAPEL-TESOURA===\n");
        System.out.print("Digite o IP do servidor: ");
        String ip = scanner.next();
        System.out.print("Digite o seu nome: ");
        String nome = scanner.next();

        try {
            Registry registry = LocateRegistry.getRegistry(ip, 3099);
            ContratoJogo jogoRemoto = (ContratoJogo) registry.lookup("ServidorPPT");
            int JogadorId = jogoRemoto.registrarJogador(nome);
            System.out.println("Conectado! Seu ID de jogador é: " + JogadorId);
            System.out.println("Aguardando o início da partida...");

            FaseRodada ultimaFaseConhecida = FaseRodada.AGUARDANDO;
            boolean auto = true;

            while (true) {
                MensagemJogo msgStatus = jogoRemoto.aguardarSuaVez(JogadorId);
                FaseRodada faseAtual;
                if (msgStatus.getTipo().equals("STATUS_APOSTA")) {
                    faseAtual = FaseRodada.APOSTA;
                } else {
                    faseAtual = (FaseRodada) msgStatus.getConteudo();
                }
                if (faseAtual != ultimaFaseConhecida) {
                    ultimaFaseConhecida = faseAtual;
                    switch (faseAtual) {
                        case APOSTA:
                            int aposta = 0;
                            int saldoAtual = (int) msgStatus.getConteudo();
                            System.out.print("Saldo: " + saldoAtual + " => Sua aposta: ");
                            if (auto){
                                aposta = (saldoAtual > 0) ? ClienteJogo.gerarAleatorio(1, saldoAtual) : 0;
                            } else {
                                aposta = scanner.nextInt();
                            }
                            jogoRemoto.enviarAcao(JogadorId, new MensagemJogo("APOSTA", aposta));
                            break;
                        case JOGADA:
                            System.out.print("\nEscolha (1 - Pedra | 2 - Papel | 3 - Tesoura): ");
                            int escolha = 1;
                            if (auto){
                                escolha = ClienteJogo.gerarAleatorio(1, 3);
                            } else {
                                escolha = scanner.nextInt();
                            }
                            Jogada jogada = (escolha == 1) ? Jogada.PEDRA : (escolha == 2) ? Jogada.PAPEL : Jogada.TESOURA;
                            jogoRemoto.enviarAcao(JogadorId, new MensagemJogo("JOGADA", jogada));
                            break;
                        case DECISAO_DESEMPATE:
                            System.out.println("Empate na rodada! Iniciando congelamento e desempate...");
                            System.out.print("(1) Continuar e Apostar ou (2) Desistir e perder metade: ");
                            int decisao = 2;
                            if (auto){
                                decisao = ClienteJogo.gerarAleatorio(1, 2);
                            } else {
                                decisao = scanner.nextInt();
                            }
                            jogoRemoto.enviarAcao(JogadorId, new MensagemJogo("DECISAO_DESEMPATE", decisao));
                            break;
                        default:
                            break;
                    }
                }
                Thread.sleep(500);
            }

        } catch (java.rmi.RemoteException e) {
            System.out.println("\nA conexao com o servidor foi encerrada.");
        } catch (Exception e) {
            System.err.println("Erro no cliente: " + e.getMessage());
        }
        scanner.close();
    }

    public static int gerarAleatorio(int min, int max) {
        int temp = ThreadLocalRandom.current().nextInt(min, max + 1);
        System.out.println(temp);
        return temp;
    }

}