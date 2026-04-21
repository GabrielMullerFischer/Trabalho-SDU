public class Mesa {
    
    private int saldo;

    public Mesa(int saldoInicial) {
        this.saldo = saldoInicial;
    }

    public int getSaldo() {
        return saldo;
    }

    public void adicionarSaldo(int valor) {
        this.saldo += valor;
    }

    public void deduzirSaldo(int valor) {
        this.saldo = Math.max(0, this.saldo - valor);
    }
}
