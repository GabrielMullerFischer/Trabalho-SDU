import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ContratoJogo extends Remote {
    int registrarJogador(String nome) throws RemoteException;
    MensagemJogo aguardarSuaVez(int idJogador) throws RemoteException;
    void enviarAcao(int idJogador, MensagemJogo acao) throws RemoteException;
}
