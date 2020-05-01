/**
 * Segunda tarea evaluable - Provocar una condición de carrera
 * 
 * Escribir un programa concurrente en el que múltiples threads compartan y modifiquen una 
 * variable de tipo int de forma que el resultado final de la variable una vez que los 
 * threads terminan no sea el valor esperado. Seamos más concretos. Tendremos dos tipos de 
 * procesos, decrementadores e incrementadores que realizan N decrementos e incrementos, 
 * respectivamente, sobre una misma variable ( n ) de tipo int inicializada a 0. El programa
 * concurrente pondrá en marcha M procesos de cada tipo y una vez que todos los threads han
 * terminado imprimirá el valor de la variable compartida.
 * El valor final de la variable deberı́a ser 0 ya que se habrán producido M × N decrementos
 * ( n-- ) y M × N incrementos ( n++ ), sin embargo, si dos operaciones (tanto de decremento como
 * de incremento) se realizan a la vez el resultado puede no ser el esperado (por ejemplo, dos
 * incrementos podrı́an terminar por no incrementar la variable en 2).
 * El alumno no deberı́a realizar la entrega hasta que no vea que el valor final de la variable
 * puede ser distinto de 0 (aunque esto no garantiza que haya una condición de carrera).
 * 
 * 
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */
package tareas.tarea2;

import java.util.concurrent.ThreadLocalRandom;

public class CC_02_Carrera {
    private static int n = 0;

    /**
     * Decrementador
     */
    private static class Dec extends Thread {
        private int iteraciones;
        public Dec (int iteraciones) {
            this.iteraciones = iteraciones;
        }
        public void run() {
            for(int i = 0; i < iteraciones; i++) {
                n--;
            }
        }
    }

    /**
     * Incrementador
     */
    private static class Inc extends Thread {
        private int iteraciones;
        public Inc (int iteraciones) {
            this.iteraciones = iteraciones;
        }
        public void run() {
            for(int i = 0; i < iteraciones; i++) {
                n++;
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {

        // Obtener el numero de Incrementadores y Decrementadores
        int numero_threads = ThreadLocalRandom.current().nextInt(10000);
        int numero_iteraciones = ThreadLocalRandom.current().nextInt(1000);

        System.out.format("\nSe realizarán %d incrementos y decrementos\n", numero_threads);

        Dec[] decrementos = new Dec[numero_threads];
        Inc[] incrementos = new Inc[numero_threads];

        // Ejecutar los metodos run de forma concurrente
        for (int i = 0; i < numero_threads; i++) {
            decrementos[i] = new Dec(numero_iteraciones);
            incrementos[i] = new Inc(numero_iteraciones);
            decrementos[i].start();
            incrementos[i].start();
        }

        // Esperar a que se han terminado de ejecutar todos los Threads
        for (int i = 0; i < numero_threads; i++) {
            decrementos[i].join();
            incrementos[i].join();
        }

        System.out.println("La variable 'n' ahora vale: " + n);
    }
}