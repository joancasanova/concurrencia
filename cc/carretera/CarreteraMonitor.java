// Nunca cambia la declaracion del package!
package cc.carretera;

import es.upm.babel.cclib.Monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del recurso compartido Carretera con Monitores
 */
public class CarreteraMonitor implements Carretera {
  // Añadir atributos para representar el estado del recurso y
  // la gestión de la concurrencia (monitor y conditions)

  private boolean[][] carriles_ocupados;
  private Map<String, List> automoviles;

  private Monitor mutex;        //
  private Monitor.Cond cond_entrar;
  private Monitor.Cond cond_avanzar;

  public CarreteraMonitor(int segmentos, int carriles) {
    // Inicializar estado, monitor y conditions

    this.mutex = new Monitor();
    this.cond_entrar = mutex.newCond();
    this.cond_avanzar = mutex.newCond();

    this.carriles_ocupados = new boolean[segmentos][carriles];
    this.automoviles = new HashMap<>();
  }

  /**
   * Un coche pide permiso para entrar en el primer segmento de la
   * carretera con una determinada velocidad
   *
   * @param id identificador del coche
   * @param tks número de ticks necearios para atravesar un segmento (velocidad)
   *
   * @return posición (segmento/carril) que ocupa el coche, ver clase Pos
   *
   * ESPECIFICACIÓN DEL MÉTODO ENTRAR:
   * PRE: id ∉ dom self
   * CPRE: |CarrilesLibres(self, 1)| > 0
   *   Entrar( id , tks , pos)
   * POST: pos.s  = 1 ∧ pos.c ∈ CarrilesLibres(self pre , 1)
   *       ∧ self = self-pre ∪ {id → (pos, tks)}
   */
  public Pos entrar(String id, int tks) {
    // Protocolo de entrada
    mutex.enter();
    List<Integer> carriles_libres = CarrilesLibres(1); // cambiar a segmento 1
    if (carriles_libres.size() == 0) {
      cond_entrar.await();
    }

    // Sección crítica
    Pos posicion = new Pos(1, carriles_libres.get(0));
    List posicion_ticks = new ArrayList();
    posicion_ticks.add(posicion);
    posicion_ticks.add(tks);
    automoviles.put(id, posicion_ticks);

    carriles_ocupados[0][carriles_libres.get(0) - 1] = true;

    // Protocolo de salida
    carriles_libres = CarrilesLibres(1);
    if (carriles_libres.size() > 0) {
      cond_entrar.signal();
    }
    mutex.leave();

    return posicion;
  }

  public Pos avanzar(String id, int tks) {
    // protocolo entrada
    mutex.enter();
    Pos posicion_actual = (Pos) automoviles.get(id).get(0);
    Integer siguiente_segmento = posicion_actual.getSegmento() + 1; //es el siguiente segmento
    List<Integer> carriles_libres = CarrilesLibres(siguiente_segmento);
    if(carriles_libres.size() == 0) {
      cond_avanzar.await();
    }
    // sección crítica
    Pos posicion = new Pos(siguiente_segmento, carriles_libres.get(0));
    List posicion_ticks = new ArrayList<>();
    posicion_ticks.add(posicion);
    posicion_ticks.add(tks);
    automoviles.put(id,posicion_ticks);

    carriles_ocupados[siguiente_segmento - 1][carriles_libres.get(0) - 1] = true;

    // protocolo salida
    carriles_libres = CarrilesLibres(siguiente_segmento);
    if(carriles_libres.size() != 0) {
      cond_avanzar.signal();
    }
    mutex.leave();
    return posicion;
  }

  public void circulando(String id) {
    // TODO: implementa r circulando
  }

  public void salir(String id) {
    // TODO: implementar salir
  }

  public void tick() {
    // TODO: implementar tick
  }

  /**
   * La definición auxiliar CarrilesLibres al final de la especificación, devuelve el conjunto de carriles
   * libres en un segmento ( seg ) de una carretera ( crt ).
   */
  private List<Integer> CarrilesLibres(Integer segmento) {
    List<Integer> libres = new ArrayList<>();

    int carril = 1;
    for (boolean ocupado : carriles_ocupados[segmento - 1]) {
      if (!ocupado) {
        libres.add(carril);
      }
      carril++;
    }

    return libres;
  }
}
