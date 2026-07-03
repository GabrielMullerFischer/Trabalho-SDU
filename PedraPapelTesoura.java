import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
        System.out.println("Aguardando os " + qtdJogadores + " se conectarem...");

        try {
            Registry registry = LocateRegistry.createRegistry(3099);
            ServidorRMI servidor = new ServidorRMI(qtdJogadores);
            registry.rebind("ServidorPPT", servidor);
        } catch (Exception e) {
            System.err.println("Erro ao inicializar: " + e.getMessage());
            e.printStackTrace();
        }
        scanner.close();
    }
}