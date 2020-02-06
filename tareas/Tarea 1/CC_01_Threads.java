
/**
 * Primera tarea evaluable - Creación de threads en Java
 * 
 * Escribir un programa concurrente en Java que arranque N threads y termine cuando los N threads terminen.
 * Todos los threads deben realizar el mismo trabajo: imprimir una lı́nea que los identifique y dis-
 * tinga (no se permite el uso de Thread.currentThread() ni los métodos getId() o toString() o
 * getName() de la clase Thread ), dormir durante T milisegundos y terminar imprimiendo una lı́nea
 * que los identifique y distinga. El thread principal, además de poner en marcha todos los procesos,
 * debe imprimir una lı́nea avisando de que todos los threads han terminado una vez que lo hayan
 * hecho.
 * Es un ejercicio muy sencillo que debe servir para jugar con el concepto de proceso intentando
 * distinguir lo que cada proceso hace y el momento en el que lo hace. Además, se podrá observar
 * cómo cada ejecución lleva a resultados diferentes. Se sugiere jugar con los valores N y T e
 * incluso hacer que T sea distinto para cada proceso.
 * 
 * 
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */

import java.util.concurrent.ThreadLocalRandom;

public class CC_01_Threads {

    /**
     * Clase interna que encapsula la tarea concurrente
     */
    private static class Esclavo extends Thread {
        private int numero_proceso;
        private int tiempo_dormir;

        public Esclavo(int numero_proceso, int tiempo_dormir) {
            this.numero_proceso = numero_proceso;
            this.tiempo_dormir = tiempo_dormir;
        }

        /**
         * El metodo run, heredado de la clase abstacta Thread, implementa la tarea concurrente
         */
        public void run() {
            System.out.println("Hola, soy el esclavo " + numero_proceso + ". Esta es mi primera linea");
            try {
                Thread.sleep(tiempo_dormir);
            } catch (InterruptedException e) {
                e.printStackTrace(); // Ha ocurrido una interrupcion
            }
            System.out.println("Ya he dormido, soy el esclavo " + numero_proceso + ". Esta es mi segunda linea");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {

        // Obtener el numero de Threads de forma aleatoria
        int numero_esclavos = ThreadLocalRandom.current().nextInt(15);

        // Crear array para guardar los Threads a ejecutar
        Esclavo[] esclavos = new Esclavo[numero_esclavos];

        // Obtener el tiempo a dormir para cada Thread
        // Crear los Threads y guardarlos en el array
        for (int i = 0; i < numero_esclavos; i++) {
            int tiempo_dormir = ThreadLocalRandom.current().nextInt(15000);
            esclavos[i] = new Esclavo(i+1, tiempo_dormir);
        }

        // Ejecutar los metodos run de forma concurrente
        for (Esclavo esclavo : esclavos) {
            esclavo.start();
        }

        // Esperar a que se han terminado de ejecutar todos los Threads
        for (Esclavo esclavo : esclavos) {
            esclavo.join();
        }

        System.out.println("\nTodos los esclavos han terminado su tarea");
    }
}