import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PedraPapelTesoura {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n\n\t===PEDRA-PAPEL-TESOURA===\n");
        int qtdJogadores = 0;
        while (true) {
            System.out.print("Digite o numero de jogadores (2 a 6): ");
            if (scanner.hasNextInt()) {
                qtdJogadores = scanner.nextInt();
                if (qtdJogadores >= 2 && qtdJogadores <= 6) {
                    break;
                } else {
                    System.out.println("Quantidade invalida! O jogo permite de 2 a 6 jogadores.");
                }
            } else {
                System.out.println("Por favor, digite um numero valido.");
                scanner.next();
            }
        }
        scanner.nextLine();
        List<Jogador> listaJogadores = new ArrayList<>();
        try {
            ServerSocket servidor = new ServerSocket(5000);
            System.out.println("Aguardando os " + qtdJogadores + " se conectarem...");
            for (int i = 1; i <= qtdJogadores; i++) {
                Socket socket = servidor.accept();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                Jogador j = new Jogador("Player " + i, socket, out, in);
                listaJogadores.add(j);
                System.out.println("Jogador " + i + " conectado!");
            }
            Jogo jogo = new Jogo(listaJogadores, scanner);
            jogo.iniciar();
            servidor.close();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
        scanner.close();
    }
}