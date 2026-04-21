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

        for (int i = 1; i <= qtdJogadores; i++) {
            System.out.print("Digite o nome do Jogador " + i + ": ");
            String nome = scanner.nextLine();
            listaJogadores.add(new Jogador(nome));
        }

        Jogo jogo = new Jogo(listaJogadores, scanner);

        //jogo.limparConsole();

        jogo.iniciar();

        scanner.close();
    }
}