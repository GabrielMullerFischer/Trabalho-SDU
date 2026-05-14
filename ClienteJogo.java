import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class ClienteJogo {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Socket socket = new Socket("localhost", 5000)) {

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Conectado ao servidor e aguardando jogadores, aguarde...");

            boolean auto = true; // Modo automatico, tá na gambiarra mas na fase 4 arrumo ksks

            while (true) {
                MensagemJogo msg = (MensagemJogo) in.readObject();
                if (msg.getTipo().equals("APOSTA")) {
                    int saldoAtual = (int) msg.getConteudo();
                    System.out.print("Saldo: " + msg.getConteudo() + ". Sua aposta: ");
                    int aposta = 1;
                    if (auto){
                        aposta = (saldoAtual > 0) ? ClienteJogo.gerarAleatorio(1, (int) msg.getConteudo()) : 0;
                    } else {
                        aposta = scanner.nextInt();
                    }
                    out.writeObject(new MensagemJogo("APOSTA", aposta));
                } else if (msg.getTipo().equals("JOGADA")) {
                    System.out.print("\nEscolha (1 - Pedra | 2 - Papel | 3 - Tesoura): ");
                    int jogada = 1;
                    if (auto){ 
                        jogada = ClienteJogo.gerarAleatorio(1, 3);
                    } else {
                        jogada = scanner.nextInt();
                    }
                    out.writeObject(new MensagemJogo("JOGADA", Jogada.fromInt(jogada)));
                } else if (msg.getTipo().equals("DECISAO_DESEMPATE")) {
                    System.out.println("Empate na rodada! Iniciando congelamento e desempate...");
                    System.out.print("(1) Continuar e Apostar ou (2) Desistir e perder metade: ");
                    int decisao = 2;
                    if (auto){
                        decisao = ClienteJogo.gerarAleatorio(1, 2);
                    } else {
                        decisao = scanner.nextInt();
                    }
                    out.writeObject(new MensagemJogo("DECISAO_DESEMPATE", decisao));
                }
                out.flush();
            }
        } catch (EOFException | SocketException e) {
            System.out.println("\nA conexao com o servidor foi encerrada.");
        } catch (Exception e) {
            System.err.println("\nErro no cliente: " + e.getMessage());
        }
        scanner.close();

    }

    public static int gerarAleatorio(int min, int max) {
        int temp = ThreadLocalRandom.current().nextInt(min, max + 1);
        System.out.println(temp);
        return temp;
    }

}