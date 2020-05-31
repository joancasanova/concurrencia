package cc.carretera;

import org.jcsp.lang.*;
import java.util.*;

/**
 * Implementación del recurso compartido Carretera con paso de mensajes.
 *
 * @author Juan Francisco Casanova Ferrer
 * @author Ivan Carrion Lopez
 */
public class CarreteraCSP implements Carretera, CSProcess {
  // Configuración de la carretera
  private final int segmentos;
  private final int carriles;

  // Matriz que guarda los carriles ocupados
  private final boolean[][] carrilesOcupados;

  // Mapa que guarda el nombre del coche y su estado actual en la carretera (posición y ticks en ese instante)
  private final Map<String, EstadoCoche> coches;

  // Declaración de canales
  Any2OneChannel canalTick;
  Any2OneChannel canalCircular;
  Any2OneChannel canalEntrar;
  Any2OneChannel canalAvanzar;
  Any2OneChannel canalSalir;

  /**
   * Constructor.
   * Inicialización de los atributos de la carretera.
   *
   * @param segmentos numero de segmentos en los que se divide la carretera.
   * @param carriles numero de carriles en la carretera.
   */
  public CarreteraCSP(int segmentos, int carriles) {
    this.segmentos = segmentos;
    this.carriles = carriles;

    // En la matriz de carriles ocupados ignoramos la posicion 0
    // Esto es poque los carriles y segmentos empiezan a contar por el numero 1
    this.carrilesOcupados = new boolean[segmentos + 1][carriles + 1];

    // Inicializar mapa de coches
    this.coches = new HashMap<>();

    // Creación de canales para comunicación con el servidor
    canalTick = Channel.any2one();
    canalSalir = Channel.any2one();
    canalEntrar = Channel.any2one();
    canalCircular = Channel.any2one();
    canalAvanzar = Channel.any2one();

    // Puesta en marcha del servidor: alternativa sucia (desde el
    // punto de vista de CSP) a Parallel que nos ofrece JCSP para
    // poner en marcha un CSProcess
    new ProcessManager(this).start();
  }

  /**
   * El coche entra en la carretera si hay hueco.
   *
   * @param car nombre del coche
   * @param tks número de ticks necesarios para atravesar un segmento (velocidad)
   *
   * @return La posicion en la que entra el coche.
   */
  public Pos entrar(String car, int tks) {
    // Creamos un canal para que el servidor nos envie la respuesta
    One2OneChannel canalRespuesta = Channel.one2one();

    // Guardamos los datos que enviamos en la peticion
    Peticion peticion = new Peticion(canalRespuesta, car, tks);

    // Enviamos la peticion al servidor para entrar
    canalEntrar.out().write(peticion);

    // Esperamos la respuesta del servidor una vez haya un carril libre en el primer segmento
    return (Pos) canalRespuesta.in().read();
  }

  /**
   * El coche avanza al siguiente segmento si hay hueco.
   *
   * @param car identificador del coche
   * @param tks número de ticks necesarios para atravesar un segmento (velocidad)
   *
   * @return La siguiente posicion del coche.
   */
  public Pos avanzar(String car, int tks) {
    // Creamos un canal para que el servidor nos envie la respuesta
    One2OneChannel canalRespuesta = Channel.one2one();

    // Guardamos los datos que enviamos en la peticion
    Peticion peticion = new Peticion(canalRespuesta, car, tks);

    // Enviamos la peticion al servidor para avanzar
    canalAvanzar.out().write(peticion);

    // Esperamos la respuesta del servidor una vez haya un carril libre en el siguiente segmento
    return (Pos) canalRespuesta.in().read();
  }

  /**
   * El coche sale de la carretera.
   *
   * @param car identificador del coche
   */
  public void salir(String car) {

    // Guardamos los datos que enviamos en la peticion
    Peticion peticion = new Peticion(car);

    // Enviamos la peticion para salir de la carretera al servidor
    canalSalir.out().write(peticion);
  }

  /**
   * El coche espera en un segmento hasta que tiene 0 ticks.
   *
   * @param car identificador del coche
   */
  public void circulando(String car) {
    // Creamos un canal para que el servidor nos envie la respuesta
    One2OneChannel canalRespuesta = Channel.one2one();

    // Guardamos los datos que enviamos en la peticion
    Peticion peticion = new Peticion(canalRespuesta, car);

    // Enviamos la peticion al servidor para que circule
    canalCircular.out().write(peticion);

    // Esperamos la respuesta del servidor una vez el coche tenga 0 ticks
    canalRespuesta.in().read();
  }

  /**
   * Disminuye un tick para todos los coches en circulacion.
   * Si un coche ya tiene cero ticks, continua con cero ticks.
   */
  public void tick() {
    // Enviamos una peticion al servidor para que se ejecute un tick del reloj
    canalTick.out().write(null);
  }

  /**
   * Codigo del servidor.
   */
  public void run() {

    // Cola de peticiones aplazadas para entrar, avanzar, y salir (una por cada segmento)
    // 0 --> Los que desean entrar al segmento 1
    // 1 --> Los que desean avanzar al segmento 2
    // 2 --> Los que desean avanzar al segmento 3
    // ...
    // N --> Los que desean salir
    Queue<Peticion>[] colasPeticiones = new Queue[segmentos + 1];
    for (int i = 0; i < colasPeticiones.length; i++) {
      colasPeticiones[i] = new LinkedList<>();
    }

    // Peticiones aplazadas para circular
    HashMap<String, Peticion> peticionesCircular = new HashMap<>();

    // Nombres simbolicos a las posiciones de las entradas alternativas
    final int TICK = 0;
    final int CIRCULAR = 1;
    final int ENTRAR = 2;
    final int AVANZAR = 3;
    final int SALIR = 4;

    // Calculo de las guardas
    Guard[] entradas = new Guard[5];
    entradas[TICK] = canalTick.in();
    entradas[CIRCULAR] = canalCircular.in();
    entradas[ENTRAR] = canalEntrar.in();
    entradas[AVANZAR] = canalAvanzar.in();
    entradas[SALIR] = canalSalir.in();

    // Servicios alternativos
    Alternative servicios = new Alternative(entradas);

    // Bucle principal del servidor
    while (true) {
      switch (servicios.fairSelect()) {

        case TICK:
          canalTick.in().read();

          // Para cada coche en la carretera
          for (Map.Entry<String, EstadoCoche> coche : coches.entrySet()) {

            // Si el coche tiene mas de 1 tick
            if (coche.getValue().getTks() > 0) {

              // Actualizamos el numero de ticks (uno menos)
              coche.getValue().setTks(coche.getValue().getTks() - 1);

              // Si el coche tiene 0 ticks, desbloqueamos de circulando
              if (coche.getValue().getTks() == 0) {
                One2OneChannel canalRespuesta = peticionesCircular.get(coche.getKey()).getCanalRespuesta();
                canalRespuesta.out().write(null);
              }
            }
          }
          break;

        case CIRCULAR:
          // Obtenemos la peticion
          Peticion peticion = (Peticion) canalCircular.in().read();
          String id = peticion.getId();

          // Aplazamos la peticion de circular
          peticionesCircular.put(id, peticion);
          break;

        case ENTRAR:
          // Actualizamos el estado de la carretera y las colas de peticiones
          colasPeticiones = procesarPeticion((Peticion) canalEntrar.in().read(), colasPeticiones);
          break;

        case AVANZAR:
          // Actualizamos el estado de la carretera y las colas de peticiones
          colasPeticiones = procesarPeticion((Peticion) canalAvanzar.in().read(), colasPeticiones);
          break;

        case SALIR:
          // Actualizamos el estado de la carretera y las colas de peticiones
          colasPeticiones = procesarPeticion((Peticion) canalSalir.in().read(), colasPeticiones);
          break;
      }
    }
  }

  /**
   * Metodo que procesa una peticion de entrar, avanzar o salir.
   *
   * Este método no incumple la regla de cambiar el estado del recurso exclusivamente desde dentro del servidor,
   * ya que solo se invoca desde dentro del servidor.
   */
  private Queue<Peticion>[] procesarPeticion(Peticion peticion, Queue<Peticion>[] colasPeticiones) {

    // Obtenemos el segmento actual del coche
    int segmentoActual = 0;
    if (coches.containsKey(peticion.getId())) {
      segmentoActual = coches.get(peticion.getId()).getPosicion().getSegmento();
    }

    // Aplazamos la peticion de entrada
    colasPeticiones[segmentoActual].add(peticion);

    // Actualizamos el estado de la carretera y las colas de peticiones
    return actualizacion(segmentoActual, colasPeticiones);
  }

  /**
   * Metodo recursivo que hace entrar, avanzar, y salir a los coches.
   * Cuando un coche avanza o sale, avisa a los anteriores que ha dejado un hueco libre.
   *
   * Este método no incumple la regla de cambiar el estado del recurso exclusivamente desde dentro del servidor,
   * ya que solo se invoca desde dentro del servidor.
   *
   * @param colasPeticiones Array que almacena colas de cochees esperando a ir al siguiente segmento.
   * @param segmentoActual Segmento emn el que está el coche:
   *                       - Si segmento == 0, quiere entrar.
   *                       - Si segmento < num segmentos, quiere avanzar.
   *                       - Si segmento == ultimo segmento, quiere salir.
   *
   * @return Las colas actualizadas
   */
  private Queue<Peticion>[] actualizacion(int segmentoActual, Queue<Peticion>[] colasPeticiones) {

    // Variable para return
    Queue<Peticion>[] colasPeticionesActualizadas = null;

    // Caso base para parar la recursividad
    // Si el segmento actual es el correspondiente para entrar
    if (segmentoActual == 0) {

      // Por cada coche que quiera entrar y mientras haya hueco en el siguiente carril
      int sizeCola = colasPeticiones[segmentoActual].size();
      for (int i = 0; i < sizeCola && carrilLibre(segmentoActual + 1) != 0; i++) {

        // Obtenemos y eliminamos al primer coche de la cola para entrar
        Peticion peticion = colasPeticiones[segmentoActual].poll();

        // Comprobamos cual es el carril libre y asignamos la nueva posicion al coche
        int carrilLibre = carrilLibre(1);
        Pos posicion = new Pos(1, carrilLibre);

        // Introducimos el coche en la carretera
        EstadoCoche estado = new EstadoCoche(posicion, peticion.getTicks());
        coches.put(peticion.getId(), estado);
        carrilesOcupados[1][carrilLibre] = true;

        // Liberamos al coche del bloqueo
        peticion.getCanalRespuesta().out().write(posicion);
      }

      // Se devuelve las listas de peticiones y se rompe la recursividad
      colasPeticionesActualizadas = colasPeticiones;
    }

    // Caso recursivo particular, para salir
    else if (segmentoActual == segmentos) {

      // Por cada coche que quiera salir
      int sizeCola = colasPeticiones[segmentoActual].size();
      for (int i = 0; i < sizeCola; i++) {

        // Obtenemos y eliminamos al primer coche de la cola para salir
        Peticion peticion = colasPeticiones[segmentoActual].poll();

        // Eliminamos al coche de la carretera
        int carrilActual = coches.get(peticion.getId()).getPosicion().getCarril();
        carrilesOcupados[segmentoActual][carrilActual] = false;
        coches.remove(peticion.getId());
      }

      // Llamada recursiva a los coches del anterior carril
      colasPeticionesActualizadas = actualizacion(segmentoActual - 1, colasPeticiones);
    }

    // Caso recursivo general, para avanzar
    // Si el segmento actual es el correspondiente para avanzar
    else if (0 < segmentoActual  && segmentoActual < segmentos) {

      // Por cada coche que quiera avanzar y mientras haya hueco en el siguiente carril
      int sizeCola = colasPeticiones[segmentoActual].size();
      for (int i = 0; i < sizeCola && carrilLibre(segmentoActual + 1) != 0; i++) {

        // Obtenemos y eliminamos al primer coche de la cola para avanzar
        Peticion peticion = colasPeticiones[segmentoActual].poll();

        // Comprobamos cual es el carril libre y asignamos la nueva posicion al coche
        int carrilLibre = carrilLibre(segmentoActual + 1);
        Pos posicion = new Pos(segmentoActual + 1, carrilLibre);

        // Actualizamos el estado del coche en la carretera
        coches.get(peticion.getId()).setPosicion(posicion);
        coches.get(peticion.getId()).setTks(peticion.getTicks());
        carrilesOcupados[segmentoActual + 1][carrilLibre] = true;
        carrilesOcupados[segmentoActual][carrilLibre] = false;

        // Liberamos al coche del bloqueo
        peticion.getCanalRespuesta().out().write(posicion);
      }

      // Llamada recursiva a los coches del anterior carril
      colasPeticionesActualizadas = actualizacion(segmentoActual - 1, colasPeticiones);
    }

    return colasPeticionesActualizadas;
  }

  /**
   * @return El numero del primer carril libre.
   *         Si no hay carril libre, devuelve 0.
   */
  private Integer carrilLibre(Integer siguienteSegmento) {
    int carrilLibre = 0;
    for (int carril = 1; carril <= carriles; carril++) {
      if (!carrilesOcupados[siguienteSegmento][carril]) {
        carrilLibre = carril;
        break;
      }
    }
    return carrilLibre;
  }

  /**
   * Clase que guarda el estado de un coche dentro de la carretera:
   * - Posicion: la posicion del coche en la carretera
   * - Tks: numero de ticks que tiene el coche actualmente
   */
  private static class EstadoCoche {
    private Pos posicion;
    private Integer tks;

    public EstadoCoche(Pos posicion, Integer tks) {
      this.posicion = posicion;
      this.tks = tks;
    }

    public Pos getPosicion() {
      return posicion;
    }

    public Integer getTks() {
      return tks;
    }

    public void setPosicion(Pos posicion) {
      this.posicion = posicion;
    }

    public void setTks(Integer tks) {
      this.tks = tks;
    }
  }


  /**
   * Clase que guarda una peticion:
   * - CanalRespuesta: canal por el que se da respuesta a la peticion.
   * - Id: nombre del coche
   * - Tks: numero maximo de ticks del coche
   */
  private static class Peticion {
    private One2OneChannel canalRespuesta;
    private final String id;
    private Integer ticks;

    // Constructor para entrar y avanzar
    public Peticion(One2OneChannel canalRespuesta, String id, Integer ticks) {
      this.canalRespuesta = canalRespuesta;
      this.id = id;
      this.ticks = ticks;
    }

    // Constructor para circulando
    public Peticion(One2OneChannel canalRespuesta, String id) {
      this.canalRespuesta = canalRespuesta;
      this.id = id;
    }

    // Constructor para salir
    public Peticion(String id) {
      this.id = id;
    }

    public One2OneChannel getCanalRespuesta() {
      return canalRespuesta;
    }

    public String getId() {
      return id;
    }

    public Integer getTicks() {
      return ticks;
    }
  }
}
