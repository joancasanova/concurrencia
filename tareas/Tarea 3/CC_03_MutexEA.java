/**
 * Tercera tarea evaluable - Garantizar exclusión mutua con espera activa
 * 
 * Este ejercicio consiste en evitar la condición de carrera que se produjo en el ejercicio ante-
 * rior. Para ello supondremos la existencia de sólo dos procesos, que simultáneamente ejecutan
 * sendos bucles de N pasos incrementando y decrementando, respectivamente, en cada paso una
 * variable compartida (la operación de incremento y la de decremento sobre esa misma variable
 * compartida son secciones crı́ticas). El objetivo es evitar que mientras un proceso modifica la va-
 * riable el otro haga lo mismo (propiedad que se denomina exclusión mutua: no puede haber dos
 * procesos modificando simultáneamente esa variable) y el objetivo es hacerlo utilizando sólo nue-
 * vas variables y “espera activa” (en otras palabras, está prohibido utilizar métodos synchronized,
 * semáforos o cualquier otro mecanismo de concurrencia).
 * 
 * Para este ejercicio se proporciona un código auxiliar y solo modificaremos unas lineas.
 *
 * 
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */

class CC_03_MutexEA {
   static final int N_PASOS = 10000;

   // Generador de números aleatorios para simular tiempos de ejecución
   static final java.util.Random RNG = new java.util.Random(0);

   // Variable compartida
   volatile static int n = 0;

   // Variables para asegurar exclusión mutua
   volatile static boolean quiere_dec = false;
   volatile static boolean quiere_inc = false;
   volatile static boolean turno_inc = false;

   // Sección no crítica
   static void no_sc() {
      System.out.println("No SC");
      try {
        // No más de 2ms
        Thread.sleep(RNG.nextInt(3));
      }
      catch (Exception e) {
        e.printStackTrace();
      }
   }

   // Secciones críticas
   static void sc_inc() {
      System.out.println("Incrementando");
      n++;
   }

   static void sc_dec() {
      System.out.println("Decrementando");
      n--;
   }

   // La labor del proceso incrementador es ejecutar no_sc() y luego
   // sc_inc() durante N_PASOS asegurando exclusión mutua sobre sc_inc().
   static class Incrementador extends Thread {
      public void run () {
         for (int i = 0; i < N_PASOS; i++) {
            // Sección no crítica
            no_sc();

            // Protocolo de acceso a la sección crítica
            quiere_inc = true;
            turno_inc = false;
            while (quiere_dec && !turno_inc) {}

            // Sección crítica
            sc_inc();

            // Protocolo de salida de la sección crítica
            quiere_inc = false;
         }
      }
   }

   // La labor del proceso incrementador es ejecutar no_sc() y luego
   // sc_dec() durante N_PASOS asegurando exclusión mutua sobre sc_dec().
   static class Decrementador extends Thread {
      public void run () {
         for (int i = 0; i < N_PASOS; i++) {
            // Sección no crítica
            no_sc();

            // Protocolo de acceso a la sección crítica
            quiere_dec = true;
            turno_inc = true;
            while (quiere_inc && turno_inc) {}

            // Sección crítica
            sc_dec();

            // Protocolo de salida de la sección crítica
            quiere_dec = false;
         }
      }
   }

   public static final void main(final String[] args) throws InterruptedException {
      // Creamos las tareas
      Thread t1 = new Incrementador();
      Thread t2 = new Decrementador();

      // Las ponemos en marcha
      t1.start();
      t2.start();

      // Esperamos a que terminen
      t1.join();
      t2.join();

      // Simplemente se muestra el valor final de la variable:
      System.out.println(n);
   }
}
