/**
 * Octava tarea evaluable - Multibuffer con métodos synchronized
 *
 * El MultiBuffer es una variación del problema del búffer compartido en el que productores
 * y consumidores pueden insertar o extraer secuencias de elementos de longitud arbitraria, lo
 * cual lo convierte en un ejemplo más realista. A diferencia de versiones más sencillas, este es
 * un ejercicio de programación difícil si sólo se dispone de mecanismos de sincronización de bajo
 * nivel (p.ej. semáforos).
 *
 * Por ello, os pedimos que lo implementéis en Java traduciendo la siguiente especificación de
 * recurso a una clase usando métodos synchronized y el mecanismo wait()/notifyAll().
 *
 * C-TAD MultiBuffer
 *  OPERACIONES
 *      ACCIÓN Poner: Tipo_Secuencia[e]
 *      ACCIÓN Tomar: N[e] × Tipo_Secuencia[s]
 *  SEMÁNTICA
 *  DOMINIO:
 *      TIPO: MultiBuffer = Secuencia(Tipo_Dato)
 *            Tipo_Secuencia = MultiBuffer
 *      INVARIANTE: Longitud(self) ≤ MAX
 *                  DONDE: MAX = ...
 *      INICIAL: self = <>
 *
 *      PRE: n ≤ [MAX/2]
 *      CPRE: Hay suficientes elementos en el multibuffer
 *      CPRE: Longitud(self) ≥ n
 *          Tomar(n, s)
 *      POST: Retiramos elementos
 *      POST: n = Longitud(s) ∧ self-pre = s + self
 *
 *      PRE: Longitud(s) ≤ [MAX/2]
 *      CPRE: Hay sitio en el buffer para dejar la secuencia
 *      CPRE: Longitud(self + s) ≤ MAX
 *          Poner(s)
 *      POST: Añadimos una secuencia al buffer
 *      POST: self = self-pre + s-pre
 *
 * Para este ejercicio se proporciona un código auxiliar y solo modificaremos unas lineas.
 *
 *
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */
package tareas.tarea8;

import es.upm.babel.cclib.MultiAlmacen;
import es.upm.babel.cclib.Producto;

class MultiAlmacenSync implements MultiAlmacen {
    private int capacidad = 0;
    private Producto[] almacenado = null;
    private int aExtraer = 0;
    private int aInsertar = 0;
    private int nDatos = 0;

   // Para evitar la construcción de almacenes sin inicializar la capacidad
   private MultiAlmacenSync() {
   }

   public MultiAlmacenSync(int n) {
      almacenado = new Producto[n];
      aExtraer = 0;
      aInsertar = 0;
      capacidad = n;
      nDatos = 0;
   }

   private int nDatos() {
         return nDatos;
   }
   
   private int nHuecos() {
      return capacidad - nDatos;
   }

   synchronized public void almacenar(Producto[] productos) {
      // Evaluación de la PRE
      if (productos.length > this.capacidad / 2) {
         throw new IllegalArgumentException("No se puede almacenar tantos datos");
      }

      // Implementación de código de bloqueo para sincronización condicional
      while (nHuecos() < productos.length) {
         try {
            wait();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }

      // Sección crítica
      for (int i = 0; i < productos.length; i++) {
         almacenado[aInsertar] = productos[i];
         nDatos++;
         aInsertar++;
         aInsertar %= capacidad;
      }

      // Implementación de código de desbloqueo para sincronización condicional
      notifyAll();
   }

   synchronized public Producto[] extraer(int n) {
      // Evaluación de la PRE
      if (n > this.capacidad / 2) {
         throw new IllegalArgumentException("No se puede extraer tantos datos");
      }

      // Implementación de código de bloqueo para sincronización condicional
      while (nDatos() < n) {
         try {
            wait();
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
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

      // Implementación de código de desbloqueo para sincronización condicional
      notifyAll();

      return result;
   }
}
