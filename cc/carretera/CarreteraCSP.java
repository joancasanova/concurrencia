package cc.carretera;

import org.jcsp.lang.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Implementación del recurso compartido Carretera con paso de mensajes.
 *
 * @author Juan Francisco Casanova Ferrer
 * @author Ivan Carrion Lopez
 */
public class CarreteraCSP implements Carretera, CSProcess {
  // Configuración de la carretera
  private final int carriles;

  // Matriz que guarda los carriles ocupados
  private final boolean[][] carrilesOcupados;

  // Mapa que guarda el nombre del coche y su estado actual en la carretera (posición y ticks en ese instante)
  private final Map<String, EstadoCoche> coches;

  // Declaración de canales
  Any2OneChannel canalTick;
  Any2OneChannel canalSalir;
  Any2OneChannel canalEntrar;
  Any2OneChannel canalCircular;
  Any2OneChannel canalAvanzar;


  /**
   * Constructor.
   * Inicialización de los atributos de la carretera.
   *
   * @param segmentos numero de segmentos en los que se divide la carretera.
   * @param carriles numero de carriles en la carretera.
   */
  public CarreteraCSP(int segmentos, int carriles) {
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

    // Array que guarda los datos que enviamos en la peticion
    // 0 --> canal de la respuesta
    // 1 --> nombre del coche
    // 2 --> ticks del coche
    Object[] peticion = {canalRespuesta, car, tks};

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

    // Array que guarda los datos que enviamos en la peticion
    // 0 --> canal de la respuesta
    // 1 --> nombre del coche
    // 2 --> ticks del coche
    Object[] peticion = {canalRespuesta, car, tks};

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
    // Enviamos la peticion para salir de la carretera al servidor
    canalSalir.out().write(car);
  }

  /**
   * El coche espera en un segmento hasta que tiene 0 ticks.
   *
   * @param car identificador del coche
   */
  public void circulando(String car) {
    // Creamos un canal para que el servidor nos envie la respuesta
    One2OneChannel canalRespuesta = Channel.one2one();


    // Array que guarda los datos que enviamos en la peticion
    // 0 --> canal de la respuesta
    // 1 --> nombre del coche
    Object[] peticion = {canalRespuesta, car};

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
   * @return El numero del primer carril libre.
   *         Si no hay carril libre, devuelve 0.
   */
  private Integer CarrilLibre(Integer segmento) {
    int carrilLibre = 0;
    for (int carril = 1; carril <= carriles; carril++) {
      if (!carrilesOcupados[segmento][carril]) {
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
   * Codigo del servidor.
   */
  public void run() {

    // Cola de peticiones aplazadas para entrar, avanzar y circular
    Queue<Object[]> peticionesEntrar = new LinkedList<>();
    Queue<Object[]> peticionesAvanzar = new LinkedList<>();
    Queue<Object[]> peticionesCircular = new LinkedList<>();

    // Nombres simbolicos a las posiciones de las entradas alternativas
    final int TICK = 0;
    final int SALIR = 1;
    final int ENTRAR = 2;
    final int CIRCULAR = 3;
    final int AVANZAR = 4;

    // Calculo de las guardas
    Guard[] entradas = new Guard[5];
    entradas[TICK] = canalTick.in();
    entradas[SALIR] = canalSalir.in();
    entradas[ENTRAR] = canalEntrar.in();
    entradas[CIRCULAR] = canalCircular.in();
    entradas[AVANZAR] = canalAvanzar.in();

    // Servicios alternativos
    Alternative servicios = new Alternative(entradas);

    // Bucle principal del servidor
    while (true) {
      switch (servicios.fairSelect()) {

        case TICK:

          // Para cada coche en la carretera
          for (EstadoCoche coche : coches.values()) {

            // Si el coche tiene mas de 0 ticks
            if (coche.getTks() > 0) {

              // Actualizamos el numero de ticks (uno menos)
              coche.setTks(coche.getTks() - 1);

            }
          }

          canalTick.in().read();
          break;

        case SALIR:

          // Obtener el id del coche que quiere salir
          String id = (String) canalSalir.in().read();

          // Eliminamos al coche de la carretera
          int segmentoActual = coches.get(id).getPosicion().getSegmento();
          int carrilActual = coches.get(id).getPosicion().getCarril();
          carrilesOcupados[segmentoActual][carrilActual] = false;
          coches.remove(id);
          break;

        case ENTRAR:

          // Aplazamos la peticion de entrada
          peticionesEntrar.add((Object[]) canalEntrar.in().read());
          break;

        case CIRCULAR:

          // Aplazamos la peticion de circular
          peticionesCircular.add((Object[]) canalCircular.in().read());
          break;

        case AVANZAR:

          // Aplazamos la peticion de avanzar
          peticionesAvanzar.add((Object[]) canalAvanzar.in().read());
          break;
      }




      // Variables auxiliares: peticion, id del coche, canal de respuesta, y tamaño de la cola
      Object[] peticion;
      String id;
      One2OneChannel canalRespuesta;

      // Ejecutamos, si se puede, alguna de las peticiones aplazadas de entrar
      int sizeCola = peticionesEntrar.size();
      for (int i = 0; i < sizeCola; i++) {

        // Obtenemos la primera peticion de la cola
        peticion = peticionesEntrar.poll();

        // Obtenemos el id del coche
        id = (String) peticion[1];

        // Si hay hueco en el primer segmento, entramos
        int carrilLibre = CarrilLibre(1);
        if (carrilLibre != 0) {

          // Asignamos la nueva posicion al coche
          Pos posicion = new Pos(1, carrilLibre);

          // Obtenemos los ticks del coche
          Integer ticks = (Integer) peticion[2];

          // Actualizamos el estado del coche en la carretera
          EstadoCoche estado = new EstadoCoche(posicion, ticks);
          coches.put(id, estado);
          carrilesOcupados[1][carrilLibre] = true;

          // Obtenemos el canal de respuesta y desbloqueamos al coche
          canalRespuesta = (One2OneChannel) peticion[0];
          canalRespuesta.out().write(posicion);
        }

        // Sino, reintroducimos al coche en la cola de peticiones
        else {
          peticionesEntrar.add(peticion);
        }
      }

      // Ejecutamos, si se puede, alguna de las peticiones aplazadas de avanzar
      sizeCola = peticionesAvanzar.size();
      for (int i = 0; i < sizeCola; i++) {

        // Obtenemos la primera peticion de la cola
        peticion = peticionesAvanzar.poll();

        // Obtenemos el id del coche
        id = (String) peticion[1];

        // Obtenemos el siguiente segmento al que quiere avanzar el coche
        int siguienteSegmento = coches.get(id).getPosicion().getSegmento() + 1;

        // Si hay hueco en el siguiente segmento, entramos
        int carrilLibre = CarrilLibre(siguienteSegmento);
        if (carrilLibre != 0) {

          // Asignamos la nueva posicion al coche
          Pos posicion = new Pos(siguienteSegmento, carrilLibre);

          // Obtenemos los ticks del coche
          Integer ticks = (Integer) peticion[2];

          // Actualizamos el estado del coche en la carretera
          coches.get(id).setPosicion(posicion);
          coches.get(id).setTks(ticks);
          carrilesOcupados[siguienteSegmento][carrilLibre] = true;
          carrilesOcupados[siguienteSegmento - 1][carrilLibre] = false;


          // Obtenemos el canal de respuesta y desbloqueamos al coche
          canalRespuesta = (One2OneChannel) peticion[0];
          canalRespuesta.out().write(posicion);
        }

        // Sino, reintroducimos al coche en la cola de peticiones
        else {
          peticionesAvanzar.add(peticion);
        }
      }

      // Comprobamos si alguno de los coches ya puede circular
      sizeCola = peticionesCircular.size();
      for (int i = 0; i < sizeCola; i++) {

        // Obtenemos la primera peticion de la cola
        peticion = peticionesCircular.poll();

        // Obtenemos el id del coche
        id = (String) peticion[1];

        // Si el numero de ticks del coche es igual a cero, lo liberamos
        if (coches.get(id).getTks() == 0) {
          canalRespuesta = (One2OneChannel) peticion[0];
          canalRespuesta.out().write(null);
        }

        // Sino, reintroducimos al coche en la cola de peticiones
        else {
          peticionesCircular.add(peticion);
        }
      }
    }
  }
}
