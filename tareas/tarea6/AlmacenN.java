package tareas.tarea6;

import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.Almacen;
import es.upm.babel.cclib.Semaphore;

/**
 * Implementación de la clase Almacen que permite el almacenamiento
 * FIFO de hasta un determinado número de productos y el uso
 * simultáneo del almacén por varios threads.
 */
class AlmacenN implements Almacen {
   private int capacidad = 0;
   private Producto[] almacenado = null;
   private int nDatos = 0;
   private int aExtraer = 0;
   private int aInsertar = 0;

   private Semaphore sem_productores;
   private Semaphore sem_consumidores;
   private Semaphore mutex;

   public AlmacenN(int n) {
      capacidad = n;
      almacenado = new Producto[capacidad];
      nDatos = 0;
      aExtraer = 0;
      aInsertar = 0;

      sem_productores = new Semaphore(n);
      sem_consumidores = new Semaphore(0);
      mutex = new Semaphore(1);
   }

   public void almacenar(Producto producto) {
      // Protocolo de acceso a la sección crítica y código de
      // sincronización para poder almacenar.
      sem_productores.await();
      mutex.await();

      // Sección crítica
      almacenado[aInsertar] = producto;
      nDatos++;
      aInsertar++;
      aInsertar %= capacidad;

      // Protocolo de salida de la sección crítica y código de
      // sincronización para poder extraer.
      mutex.signal();
      sem_consumidores.signal();
   }

   public Producto extraer() {
      Producto result;

      // Protocolo de acceso a la sección crítica y código de
      // sincronización para poder extraer.
      sem_consumidores.await();
      mutex.await();

      // Sección crítica
      result = almacenado[aExtraer];
      almacenado[aExtraer] = null;
      nDatos--;
      aExtraer++;
      aExtraer %= capacidad;

      // Protocolo de salida de la sección crítica y código de
      // sincronización para poder almacenar.
      mutex.signal();
      sem_productores.signal();

      return result;
   }
}
