package cc.carretera;

import org.jcsp.lang.*;
import org.jcsp.net.Link;

import java.nio.file.attribute.UserPrincipalLookupService;
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

    // Array que guarda los datos que enviamos en la peticion
    // 0 --> null, porque aqui deberia ir el canal de respuesta y no es necesario para salir
    // 1 --> nombre del coche
    // 2 --> null, porque aquí deberían ir los ticks y no es necesario para salir
    Object[] peticion = {null, car, null};

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
   * Metodo recursivo que hace avanzar a los coches que se encuentran en el segmento anterior.
   *
   * @param siguienteSegmento Segmento al que puede avanzar el coche (o entrar, si es el primero).
   * @param peticionesAvanzar Array que almacena colas de cochees esperando a entrar en el siguiente segmento.
   * @return Las colas actualizadas
   */
  private Queue<Object[]>[] actualizacion(int siguienteSegmento, Queue<Object[]>[] peticionesAvanzar) {

    // Variables auxiliares
    Object[] peticion;
    int carrilLibre;
    Pos posicion;
    String id;
    Integer ticks;
    One2OneChannel canalRespuesta;

    // Si no hay coches esperando a entrar al segmento, terminamos
    if (peticionesAvanzar[siguienteSegmento].size() == 0) {
      return peticionesAvanzar;
    }










    // Si estamos en el segmento 1, entonces simplemente se deja entrar a los que quieran entrar
    if (siguienteSegmento == 1) {

      peticion = peticionesAvanzar[siguienteSegmento].poll();

      // Comprobamos que carril ha quedado libre
      carrilLibre = CarrilLibre(1);

      // Asignamos la nueva posicion al coche
      posicion = new Pos(1, carrilLibre);

      // Obtenemos el id del coche
      id = (String) peticion[1];

      // Obtenemos los ticks del coche
      ticks = (Integer) peticion[2];

      // Actualizamos el estado del coche en la carretera
      EstadoCoche estado = new EstadoCoche(posicion, ticks);
      coches.put(id, estado);
      carrilesOcupados[1][carrilLibre] = true;

      // Obtenemos el canal de respuesta y desbloqueamos al coche
      canalRespuesta = (One2OneChannel) peticion[0];
      canalRespuesta.out().write(posicion);

      return peticionesAvanzar;
    }





    // Si estamos en el segmento 1, entonces simplemente se deja entrar a los que quieran entrar
    if (siguienteSegmento == 1) {



      // Obtener el id del coche que quiere salir
      id = (String) canalSalir.in().read();

      // Eliminamos al coche de la carretera
      int segmentoActual = coches.get(id).getPosicion().getSegmento();
      int carrilActual = coches.get(id).getPosicion().getCarril();
      carrilesOcupados[segmentoActual][carrilActual] = false;
      coches.remove(id);


      peticion = peticionesAvanzar[siguienteSegmento].poll();

      // Comprobamos que carril ha quedado libre
      carrilLibre = CarrilLibre(1);

      // Asignamos la nueva posicion al coche
      posicion = new Pos(1, carrilLibre);

      // Obtenemos el id del coche
      id = (String) peticion[1];

      // Obtenemos los ticks del coche
      ticks = (Integer) peticion[2];

      // Actualizamos el estado del coche en la carretera
      EstadoCoche estado = new EstadoCoche(posicion, ticks);
      coches.put(id, estado);
      carrilesOcupados[1][carrilLibre] = true;

      // Obtenemos el canal de respuesta y desbloqueamos al coche
      canalRespuesta = (One2OneChannel) peticion[0];
      canalRespuesta.out().write(posicion);

      return peticionesAvanzar;
    }










    // Caso recursivo: avazar

    // Obtenemos la peticion de avanzar
    peticion = peticionesAvanzar[siguienteSegmento].poll();

    // Comprobamos que carril ha quedado libre
    carrilLibre = CarrilLibre(siguienteSegmento);

    // Asignamos la nueva posicion al coche
    posicion = new Pos(siguienteSegmento, carrilLibre);

    // Obtenemos el id del coche
    id = (String) peticion[1];

    // Obtenemos los ticks del coche
    ticks = (Integer) peticion[2];

    // Actualizamos el estado del coche en la carretera
    coches.get(id).setPosicion(posicion);
    coches.get(id).setTks(ticks);
    carrilesOcupados[siguienteSegmento][carrilLibre] = true;
    carrilesOcupados[siguienteSegmento - 1][carrilLibre] = false;

    // Obtenemos el canal de respuesta y desbloqueamos al coche
    canalRespuesta = (One2OneChannel) peticion[0];
    canalRespuesta.out().write(posicion);

    return actualizacion(siguienteSegmento - 1, peticionesAvanzar);
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
    Queue<Object[]>[] peticionesAvanzar = new LinkedList[segmentos];
    for (int i = 0; i < peticionesAvanzar.length; i++) {
      peticionesAvanzar[i] = new LinkedList<>();
    }

    // Peticiones aplazadas para circular
    HashMap<String, Object[]> peticionesCircular = new HashMap<>();

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

    // Variables auxiliares: peticion, id del coche, canal de respuesta
    Object[] peticion;
    String id;
    int segmentoActual;
    One2OneChannel canalRespuesta;

    // Bucle principal del servidor
    while (true) {
      switch (servicios.fairSelect()) {

        case TICK:
          canalTick.in().read();

          // Para cada coche en la carretera
          for (Map.Entry<String, EstadoCoche> coche : coches.entrySet()) {

            // Si el coche tiene mas de 1 tick
            if (coche.getValue().getTks() > 1) {

              // Actualizamos el numero de ticks (uno menos)
              coche.getValue().setTks(coche.getValue().getTks() - 1);
            }

            // Si el coche tiene 1 tick, desbloqueamos de circulando
            if (coche.getValue().getTks() == 1) {
              canalRespuesta = (One2OneChannel) peticionesCircular.get(coche.getKey())[0];
              canalRespuesta.out().write(null);
            }
          }

          break;

        case CIRCULAR:

          // Obtenemos la peticion
          peticion = (Object[]) canalEntrar.in().read();
          id = (String) peticion[1];

          // Aplazamos la peticion de circular
          peticionesCircular.put(id, peticion);
          break;

        case ENTRAR:

          // Obtenemos la peticion
          peticion = (Object[]) canalEntrar.in().read();

          // Aplazamos la peticion de entrada
          peticionesAvanzar[0].add(peticion);
          break;

        case AVANZAR:
          // Obtenemos la peticion
          peticion = (Object[]) canalAvanzar.in().read();

          // Obtenemos el id del coche
          id = (String) peticion[1];

          segmentoActual = coches.get(id).getPosicion().getSegmento();

          // Aplazamos la peticion de entrada
          peticionesAvanzar[segmentoActual].add(peticion);
          break;

        case SALIR:

          // Obtenemos el id del coche que quiere salir
          peticion = (Object[]) canalSalir.in().read();

          // Obtenemos el id del coche
          id = (String) peticion[1];

          segmentoActual = coches.get(id).getPosicion().getSegmento();

          // Aplazamos la peticion de entrada
          peticionesAvanzar[segmentoActual].add(peticion);
          break;
      }
    }
  }

  /**
   * Clase que guarda el estado de un coche dentro de la carretera:
   * - Posicion: la posicion del coche en la carretera
   * - Tks: numero de ticks que tiene el coche actualmente
   * - canalRespuesta: canal de respuesta de la peticion
   */
  private static class EstadoCoche {
    private Pos posicion;
    private Integer tks;
    private One2OneChannel canalRespuesta;

    public EstadoCoche(Pos posicion, Integer tks, One2OneChannel canalRespuesta) {
      this.posicion = posicion;
      this.tks = tks;
      this.canalRespuesta = canalRespuesta;
    }

    public Pos getPosicion() {
      return posicion;
    }

    public Integer getTks() {
      return tks;
    }

    public One2OneChannel getCanalRespuesta() {
      return canalRespuesta;
    }

    public void setPosicion(Pos posicion) {
      this.posicion = posicion;
    }

    public void setTks(Integer tks) {
      this.tks = tks;
    }
  }

}
