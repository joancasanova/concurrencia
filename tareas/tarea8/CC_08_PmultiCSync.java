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
import es.upm.babel.cclib.MultiConsumidor;
import es.upm.babel.cclib.MultiProductor;

/**
 * Programa concurrente para productor-buffer-consumidor con multialmacen
 * de capacidad N implementado con métodos synchronized (MultiAlmacenSync).
 */
class CC_08_PmultiCSync {
    public static final void main(final String[] args)
        throws InterruptedException
    {
        // Capacidad del multialmacen
        final int N = 10;

        // Número de productores y consumidores
        final int N_PRODS = 2;
        final int N_CONSS = 2;

        // Máxima cantidad de productos por paquete para producir y
        // consumir
        final int MAX_PROD = N / 2;
        final int MAX_CONS = N / 2;

        // Almacen compartido
        MultiAlmacen almac = new MultiAlmacenSync(N);

        // Declaración de los arrays de productores y consumidores
        MultiProductor[] productores;
        MultiConsumidor[] consumidores;

        // Creación de los arrays
        productores = new MultiProductor[N_PRODS];
        consumidores = new MultiConsumidor[N_CONSS];

        // Creación de los productores
        for (int i = 0; i < N_PRODS; i++) {
            productores[i] = new MultiProductor(almac,MAX_PROD);
        }

        // Creación de los consumidores
        for (int i = 0; i < N_CONSS; i++) {
            consumidores[i] = new MultiConsumidor(almac,MAX_CONS);
        }

        // Lanzamiento de los productores
        for (int i = 0; i < N_PRODS; i++) {
            productores[i].start();
        }

        // Lanzamiento de los consumidores
        for (int i = 0; i < N_CONSS; i++) {
            consumidores[i].start();
        }

        // Espera hasta la terminación de los procesos
        try {
            for (int i = 0; i < N_PRODS; i++) {
                productores[i].join();
            }
            for (int i = 0; i < N_CONSS; i++) {
                consumidores[i].join();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit (-1);
        }
    }
}
