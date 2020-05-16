package cc.carretera;

import es.upm.babel.cclib.Monitor;
import java.util.Map;
import java.util.HashMap;

/**
 * Implementación del recurso compartido Carretera con Monitores.
 *
 * @author Juan Francisco Casanova Ferrer
 * @author Ivan Carrion Lopez
 */
public class CarreteraMonitor implements Carretera {
  private final int carriles;

  // Matriz que guarda los carriles ocupados
  private final boolean[][] carriles_ocupados;

  // Mapa que guarda el nombre del coche y su estado actual en la carretera (posición, ticks, y si está bloqueado mientras circula)
  private final Map<String, EstadoCoche> automoviles;

  // Monitor para la exclusión mutua
  private final Monitor mutex;

  // Condiciones para avanzar de segmento
  private final Monitor.Cond[] condiciones_avanzar;

  // Condicion para que un coche circule inmediatamente despues de alcanzar los 0 ticks
  private final Monitor.Cond sincronizar_circular_tick;

  /**
   * Constructor.
   * Inicialización de los atributos de la carretera.
   *
   * @param segmentos numero de segmentos en los que se divide la carretera.
   * @param carriles numero de carriles en la carretera.
   */
  public CarreteraMonitor(int segmentos, int carriles) {
    this.carriles = carriles;

    // En la matriz de carriles ocupados ignoramos la posicion 0
    // Esto es poque los carriles y segmentos empiezan a contar por el numero 1
    this.carriles_ocupados = new boolean[segmentos + 1][carriles + 1];

    // Inicializar mapa de automoviles
    this.automoviles = new HashMap<>();

    // Inicializar monitor
    this.mutex = new Monitor();

    // Necesitamos una cola de condicion por cada segmento de la carretera
    this.condiciones_avanzar = new Monitor.Cond[segmentos + 1];
    for (int i = 0; i < segmentos + 1; i++) {
      condiciones_avanzar[i] = mutex.newCond();
    }

    // Inicializar condicion para sincronizar el metodo tick y circulando
    sincronizar_circular_tick = mutex.newCond();
  }

  /**
   * El coche entra en la carretera si hay hueco.
   *
   * @param id identificador del coche
   * @param tks número de ticks necearios para atravesar un segmento (velocidad)
   *
   * @return La posicion en la que entra el coche.
   */
  public Pos entrar(String id, int tks) {
    // Entrada en la zona de exclusion mutua
    mutex.enter();

    // Si no hay hueco en el primer carril, esperamos
    if (CarrilLibre(1) == 0) {
      condiciones_avanzar[1].await();
    }

    // Comprobamos cual es el carril libre y asignamos la nueva posicion al coche
    int carril_libre = CarrilLibre(1);
    Pos posicion = new Pos(1, carril_libre);

    // Asignamos un monitor al coche para bloquearlo cuando este circulando
    Monitor.Cond bloqueado = mutex.newCond();

    // Introducimos el coche en la carretera
    EstadoCoche estado = new EstadoCoche(posicion, tks, bloqueado);
    automoviles.put(id, estado);
    carriles_ocupados[1][carril_libre] = true;

    // Salida de la zona de exclusion mutua
    mutex.leave();

    return posicion;
  }

  /**
   * El coche avanza al siguiente segmento si hay hueco.
   *
   * @param id identificador del coche
   * @param tks número de ticks necearios para atravesar un segmento (velocidad)
   *
   * @return La siguiente posicion del coche.
   */
  public Pos avanzar(String id, int tks) {
    // Entrada en la zona de exclusion mutua
    mutex.enter();

    // Si no hay hueco en el siguiente carril, esperamos
    int siguiente_segmento = automoviles.get(id).getPosicion().getSegmento() + 1;
    if (CarrilLibre(siguiente_segmento) == 0) {
      condiciones_avanzar[siguiente_segmento].await();
    }

    // Comprobamos cual es el carril libre y asignamos la nueva posicion al coche
    int carril_libre = CarrilLibre(siguiente_segmento);
    Pos posicion = new Pos(siguiente_segmento, carril_libre);

    // Actualizamos el estado del coche en la carretera
    automoviles.get(id).setPosicion(posicion);
    automoviles.get(id).setTks(tks);
    carriles_ocupados[siguiente_segmento][carril_libre] = true;
    carriles_ocupados[siguiente_segmento - 1][carril_libre] = false;

    // Señalizamos que queda un huevo libre en el segmento actual
    condiciones_avanzar[siguiente_segmento - 1].signal();

    // Salida de la zona de exclusion mutua
    mutex.leave();

    return posicion;
  }

  /**
   * El coche sale de la carretera.
   *
   * @param id identificador del coche
   */
  public void salir(String id) {
    // Entrada en la zona de exclusion mutua
    mutex.enter();

    // Eliminamos al coche de la carretera
    int segmento_actual = automoviles.get(id).getPosicion().getSegmento();
    int carril_actual = automoviles.get(id).getPosicion().getCarril();
    carriles_ocupados[segmento_actual][carril_actual] = false;
    automoviles.remove(id);

    // Señalizamos que queda un huevo libre en el ultimo segmento
    condiciones_avanzar[segmento_actual].signal();

    // Salida de la zona de exclusion mutua
    mutex.leave();
  }

  /**
   * El coche espera en un segmento hasta que tiene 0 ticks.
   *
   * @param id identificador del coche
   */
  public void circulando(String id) {
    // Entrada en la zona de exclusion mutua
    mutex.enter();

    // Si el numero de ticks del coche es mayor que cero, lo bloqueamos
    if (automoviles.get(id).getTks() != 0) {
      automoviles.get(id).getBloqueo().await();
    }

    // Señalamos que se ha completado la circulacion del coche
    sincronizar_circular_tick.signal();

    // Salida de la zona de exclusion mutua
    mutex.leave();
  }

  /**
   * Disminuye un tick para todos los coches en circulacion.
   * Si un coche ya tiene cero ticks, continua con cero ticks.
   */
  public void tick() {
    // Entrada en la zona de exclusion mutua
    mutex.enter();

    // Para cada coche en la carretera
    for (EstadoCoche coche : automoviles.values()) {

      // Si el coche tiene mas de 0 ticks
      if (coche.getTks() > 0) {

        // Actualizamos el numero de ticks (uno menos)
        coche.setTks(coche.getTks() - 1);

        // Si se ha quedado con 0 ticks
        if (coche.getTks() == 0) {

          // Desbloqueamos el coche para que circule
          coche.getBloqueo().signal();

          // Esperamos a que complete la circulacion
          sincronizar_circular_tick.await();
        }
      }
    }

    // Salida de la zona de exclusion mutua
    mutex.leave();
  }

  /**
   * @return El numero del primer carril libre.
   *         Si no hay carril libre, devuelve 0.
   */
  private Integer CarrilLibre(Integer segmento) {
    int carril_libre = 0;
    for (int carril = 1; carril <= carriles; carril++) {
      if (!carriles_ocupados[segmento][carril]) {
        carril_libre = carril;
        break;
      }
    }
    return carril_libre;
  }


  /**
   * Clase que guarda el estado de un coche dentro de la carretera:
   * - Posicion: la posicion del coche en la carretera
   * - Tks: numero de ticks que tiene el coche actualmente
   * - Bloqueo: condicion de bloqueo para circular
   */
  private static class EstadoCoche {
    private Pos posicion;
    private Integer tks;
    private final Monitor.Cond bloqueo;

    public EstadoCoche(Pos posicion, Integer tks, Monitor.Cond bloqueo) {
      this.posicion = posicion;
      this.tks = tks;
      this.bloqueo = bloqueo;
    }

    public Pos getPosicion() {
      return posicion;
    }

    public Integer getTks() {
      return tks;
    }

    public Monitor.Cond getBloqueo() {
      return bloqueo;
    }

    public void setPosicion(Pos posicion) {
      this.posicion = posicion;
    }

    public void setTks(Integer tks) {
      this.tks = tks;
    }
  }
}
