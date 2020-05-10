/**
 * Novena tarea evaluable - Multibuffer con métodos monitores
 *
 * En esta versión del ejercicio, la tarea consiste en resolver el problema anterior usando las
 * clases es.upm.babel.cclib.Monitor y es.upm.babel.cclib.Monitor.Cond .
 *
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */
package tareas.tarea9;

import es.upm.babel.cclib.MultiAlmacen;
import es.upm.babel.cclib.Producto;
import es.upm.babel.cclib.Monitor;

class MultiAlmacenMon implements MultiAlmacen {
    private int capacidad = 0;
    private Producto[] almacenado = null;
    private int aExtraer = 0;
    private int aInsertar = 0;
    private int nDatos = 0;

   // Declaración de atributos extras necesarios
   // para exclusión mutua y sincronización por condición
   private Monitor mutex;
   private Monitor.Cond[] cAlm;
   private Monitor.Cond[] cExt;

   // Para evitar la construcción de almacenes sin inicializar la capacidad
   private MultiAlmacenMon() {
   }

   public MultiAlmacenMon(int n) {
      almacenado = new Producto[n];
      aExtraer = 0;
      aInsertar = 0;
      capacidad = n;
      nDatos = 0;

      // Inicialización de otros atributos
      mutex = new Monitor();
      cAlm = new Monitor.Cond[capacidad/2 + 1];
      cExt = new Monitor.Cond[capacidad/2 + 1];

      for (int i = 0; i < cAlm.length; i++) {
         cAlm[i] = mutex.newCond();
         cExt[i] = mutex.newCond();
      }
   }

   private int nDatos() {
         return nDatos;
   }
   
   private int nHuecos() {
      return capacidad - nDatos;
   }

   public void almacenar(Producto[] productos) {
      mutex.enter();

      // Evaluación de la PRE
      if (productos.length > this.capacidad / 2) {
         mutex.leave();
         throw new IllegalArgumentException("No se puede almacenar tantos datos");
      }

      // Implementación de código de bloqueo para
      // exclusión mutua y sincronización condicional
      if (nHuecos() < productos.length) {
         cAlm[productos.length].await();
      }

      // Sección crítica
      for (int i = 0; i < productos.length; i++) {
         almacenado[aInsertar] = productos[i];
         nDatos++;
         aInsertar++;
         aInsertar %= capacidad;
      }

      // Implementación de código de desbloqueo para
      // sincronización condicional y liberación de la exclusión mutua
      desbloqueo();
      mutex.leave();
   }

   public Producto[] extraer(int n) {
      mutex.enter();

      // Evaluación de la PRE
      if (n > this.capacidad / 2) {
         mutex.leave();
         throw new IllegalArgumentException("No se puede extraer tantos datos");
      }

      // Implementación de código de bloqueo para exclusión
      // mutua y sincronización condicional
      if (nDatos() < n) {
         cAlm[n].await();
      }

      Producto[] result = new Producto[n];

      // Sección crítica
      for (int i = 0; i < result.length; i++) {
         result[i] = almacenado[aExtraer];
         almacenado[aExtraer] = null;
         nDatos--;
         aExtraer++;
         aExtraer %= capacidad;
      }

      // Implementación de código de desbloqueo para
      // sincronización condicional y liberación de la exclusión mutua
      desbloqueo();
      mutex.leave();

      return result;
   }

   private void desbloqueo() {
      boolean signaled = false;
      for (int i = 1; i < cAlm.length && !signaled; i++) {
         if (nHuecos() >= i && cAlm[i].waiting() > 0) {
            signaled = true;
            cAlm[i].signal();
         }
      }
      for (int i = 1; i < cExt.length && !signaled; i++) {
         if (nDatos() >= i && cExt[i].waiting() > 0) {
            signaled = true;
            cExt[i].signal();
         }
      }
   }
}
