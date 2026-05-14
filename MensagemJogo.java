import java.io.Serializable;

// Esta é a "encomenda" que viajará pela rede
public class MensagemJogo implements Serializable {
    // Um ID de versão é recomendado para evitar conflitos de serialização
    // Sempre que mudar algo na estrutura do objeto, incremente a versão (ex.: novoAtributo)
    private static final long serialVersionUID = 1L;

    private String tipo; // Ex: "JOGADA", "APOSTA", "DESISTENCIA"
    private Object conteudo; // Pode ser um Enum de Pedra/Papel/Tesoura, ou valor de aposta

    public MensagemJogo(String tipo, Object conteudo) {
        this.tipo = tipo;
        this.conteudo = conteudo;
    }

    public String getTipo() { return tipo; }
    public Object getConteudo() { return conteudo; }
}
