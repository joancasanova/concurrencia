/**
 * Primer programa concurrente
 * 
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */
package tareas.holamundos;

import java.util.Scanner; 
public class HolaMundos {

    /**
     * Clase interna que encapsula la tarea concurrente
     */
    private static class HolaMundo extends Thread {
        private String user_input;
        private int numero_proceso;

        public HolaMundo(String user_input, int numero_proceso) {
            this.user_input = user_input;
            this.numero_proceso = numero_proceso;
        }

        /**
         * El metodo run, heredado de la clase abstacta Thread,
         * implementa la tarea concurrente
         */
        public void run() {
            System.out.println("Hola mundo, has escrito: \"" + user_input + "\" con el proceso " + numero_proceso);
        }
    }
    
    public static void main(String[] args) {

        // Crear dos instancias de HolaMundo
        System.out.println("Introduce dos strings: ");
        Scanner scanner = new Scanner(System.in);
        HolaMundo hola1 = new HolaMundo(scanner.next(), 1);
        HolaMundo hola2 = new HolaMundo(scanner.next(), 2);
        scanner.close();

        // Ejecutar los metodos run de forma concurrente
        hola1.start();
        hola2.start();

        System.out.println("Hola mundo, esto lo ha escrito 'el main'");
    }
}