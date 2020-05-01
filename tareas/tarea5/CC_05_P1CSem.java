/**
 * Quinta tarea evaluable - Almacén de un dato con semáforos
 * 
 * En este caso nos enfrentamos a un tı́pico programa de concurrencia: productores-consumidores.
 * Existen procesos de dos tipos diferentes:
 *  - Productores: su hilo de ejecución consiste, repetidamente, en crear un producto (ver la
 *    clase es.upm.babel.cclib.Producto ) y hacerlo llegar a uno de los consumidores.
 *  - Consumidores: su hilo de ejecución consiste, repetidamente, recoger productos producidos
 *    por los productores y consumirlos.
 * 
 * Las clases que implementan ambos threads forman parte de la librerı́a CCLib:
 * es.upm.babel.cclib.Productor y es.upm.babel.cclib.Consumidor.
 * 
 * La comunicación entre productores y consumidores se realizará a través de un “almacén”
 * compartido por todos los procesos. Dicho objeto respetará la interfaz es.upm.babel.cclib.Almacen.
 * 
 * Se pide implementar sólo con semáforos una clase que siga dicha interfaz. Sólo puede haber
 * almacenado como máximo un producto, si un proceso quiere almacenar debe esperar hasta que
 * no haya un producto y si un proceso quiera extraer espere hasta que haya un producto. Téngase
 * en cuenta además los posible problemas de no asegurar la exclusión mutua en el acceso a los
 * atributos compartidos.
 * 
 * Para valorar si el problema está bien resuelto, os recordamos que el objetivo es asegurar
 *  1. que todos los productos producidos acaban por ser consumidos,
 *  2. que no se consume un producto dos veces y
 *  3. que no se consume ningún producto no válido (null, por ejemplo).
 * 
 * Para este ejercicio se proporciona un código auxiliar y solo modificaremos unas lineas.
 *
 * 
 * Universidad Politecnica de Madrid
 * Concurrencia
 * Autor: Juan Francisco Casanova Ferrer
 * github: https://github.com/joancasanova
 */
package tareas.tarea5;

import es.upm.babel.cclib.Almacen;
import es.upm.babel.cclib.Productor;
import es.upm.babel.cclib.Consumidor;
import es.upm.babel.cclib.Consumo;
import es.upm.babel.cclib.Fabrica;

/**
 * Programa concurrente para productor-buffer-consumidor con almacen
 * de tamaño 1 implementado con semáforos (Almacen1).
 */
class CC_05_P1CSem {
    public static final void main(final String[] args)
       throws InterruptedException
    {
        // Número de productores y consumidores
        final int N_PRODS = 2;
        final int N_CONSS = 2;
        
        Consumo.establecerTiempoMedioCons(100);
        Fabrica.establecerTiempoMedioProd(100);

        // Almacen compartido
        Almacen almac = new Almacen1();

        // Declaración de los arrays de productores y consumidores
        Productor[] productores;
        Consumidor[] consumidores;

        // Creación de los arrays
        productores = new Productor[N_PRODS];
        consumidores = new Consumidor[N_CONSS];

        // Creación de los productores
        for (int i = 0; i < N_PRODS; i++) {
            productores[i] = new Productor(almac);
        }

        // Creación de los consumidores
        for (int i = 0; i < N_CONSS; i++) {
            consumidores[i] = new Consumidor(almac);
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