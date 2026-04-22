public class Mesa {
    
    private int saldo;

    public Mesa(int saldoInicial) {
        this.saldo = saldoInicial;
    }

    public synchronized int getSaldo() {
        return saldo;
    }

    public synchronized void adicionarSaldo(int valor) {
        this.saldo += valor;
    }

    public synchronized void deduzirSaldo(int valor) {
        this.saldo = Math.max(0, this.saldo - valor);
    }
}
