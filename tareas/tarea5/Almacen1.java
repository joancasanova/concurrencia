package tareas.tarea5;

import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.Almacen;
import es.upm.babel.cclib.Semaphore;

/**
 * Implementación de la clase Almacen que permite el almacenamiento
 * de producto y el uso simultáneo del almacen por varios threads.
 */
class Almacen1 implements Almacen {
   // Producto a almacenar: null representa que no hay producto
   private Producto almacenado = null;

   // Semáforo que controla si se puede extraer, es decir, si el almacén está lleno
   private Semaphore sem_almacenar = new Semaphore(1); 
   
   // Semáforo que controla si se puede almacenar, es decir, si el almacén está vacío
   private Semaphore sem_extraer = new Semaphore(0);
   
   public void almacenar(Producto producto) {
      // Protocolo de acceso a la sección crítica y código de
      // sincronización para poder almacenar.
      sem_almacenar.await();

      // Sección crítica
      almacenado = producto;

      // Protocolo de salida de la sección crítica y código de
      // sincronización para poder extraer.
      sem_extraer.signal();
   }

   public Producto extraer() {
      Producto result;

      // Protocolo de acceso a la sección crítica y código de
      // sincronización para poder extraer.
      sem_extraer.await();
  
      // Sección crítica
      result = almacenado;
  
      // Protocolo de salida de la sección crítica y código de
      // sincronización para poder almacenar.
      sem_almacenar.signal();

      return result;
   }
}