package Controlador;

import Modelo.*;

import java.awt.event.KeyEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import static java.util.Collections.addAll;

public class PedidoDAOMySQL implements PedidoDAO, ProductoDAO {
    private static final Connection conexion = Conexion.getConexion();
    private static final PedidoDAOMySQL dao = new PedidoDAOMySQL();
    public static ArrayList<Producto> listadoProductos = new ArrayList<>();
    public static ArrayList<String> tipos = new ArrayList<>();

    /* SENTENCIAS PRODUCTO */
    private static final String OBTENER_LISTADO_PRODUCTOS_QUERY = """
            SELECT * FROM producto
            """;

    private static final String OBTENER_PRODUCTO_ID = """
            SELECT * FROM producto WHERE id_producto = (?)
            """;

    private static final String OBTENER_PRODUCTOS_DISPONIBLES = """
            SELECT * FROM producto WHERE disponibilidad = 1
            """;

    private static final String OBTENER_PRODUCTOS_NO_DISPONIBLES = """
            SELECT * FROM producto WHERE disponibilidad = 0
            """;

    private static final String INSERTAR_NUEVO_PRODUCTO = """
            INSERT INTO producto(nombre, tipo, precio, disponibilidad)
             VALUES (?, ?, ?, ?);
                """;

    private static final String CAMBIAR_DISPONIBILIDAD_PRODUCTO = """
            UPDATE producto SET disponibilidad = (?) WHERE id_producto = (?)
            """;

    private static final String TRAER_TIPOS_PRODUCTO = """
            SELECT DISTINCT tipo
            FROM producto;
            """;

    private static final String MODIFICAR_PRODUCTO = """
            UPDATE producto
            SET nombre = (?),
            tipo = (?),
            precio = (?),
            disponibilidad = (?)
            WHERE id_producto = (?); 
            """;

    private static final String PRODUCTOS_MAS_CAROS = """
            SELECT *
            FROM producto
            HAVING precio = (SELECT MAX(precio)
            				FROM producto);
            """;

    private static final String PRODUCTOS_MAS_BARATOS = """
            SELECT *
            FROM producto
            HAVING precio = (SELECT MIN(precio)
            				FROM producto);
            """;

    /* SENTENCIAS PEDIDO */
    private static final String MOSTRAR_TOTAL_PEDIDOS = """
            SELECT *
            FROM pedido
            """;

    private static final String TRAER_PRODUCTOS_PEDIDO_CONCRETO = """
            SELECT pro.*
            FROM pedido pe
            INNER JOIN entrada e
            ON pe.id_pedido = e.id_pedido
            INNER JOIN producto pro
            ON e.id_producto = pro.id_producto
            WHERE pe.id_pedido = (?);
            """;

    private static final String VER_PEDIDOS_HOY_QUERY = "SELECT * " +
            "FROM pedido " +
            "WHERE fecha =  CURDATE() " +
            "AND estado = 'Pendiente'";

    private static final String TRAER_PEDIDO_POR_ID = """
            SELECT *
            FROM pedido
            WHERE id_pedido = (?)
            """;

    private static final String TRAER_CLIENTES_QUERY = """
            SELECT DISTINCT cliente
            FROM pedido;
            """;

    private static final String TRAER_PEDIDOS_CLIENTE_CONCRETO = """
            SELECT *
            FROM pedido
            WHERE cliente = (?);
            """;

    private static final String CAMBIAR_ESTADO_A_RECOGIDO = """
            UPDATE pedido
            SET estado = 'Recogido'
            WHERE id_pedido = (?);
            """;

    private static final String TRAER_PEDIDOS_PENDIENTES = """
                SELECT *
                FROM pedido
                WHERE estado = "Pendiente";
            """;

    @Override
    public ArrayList<Producto> obtenerProductosCarta() {

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(OBTENER_LISTADO_PRODUCTOS_QUERY);

            while (rs.next()) {
                var producto = new Producto();

                setearValoresProducto(rs, producto);

                listadoProductos.add(producto);

            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return listadoProductos;
    }

    @Override
    public Producto obtenerProductoPorId(Integer id) {
        var producto = new Producto();

        try (var ps = conexion.prepareStatement(OBTENER_PRODUCTO_ID)) {
            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                setearValoresProducto(rs, producto);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }

        return producto;
    }

    @Override
    public ArrayList<Producto> obtenerProductosDisponibles() {
        var listadoProductosDisponibles = new ArrayList<Producto>();

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(OBTENER_PRODUCTOS_DISPONIBLES);

            while (rs.next()) {
                var producto = new Producto();

                setearValoresProducto(rs, producto);

                listadoProductosDisponibles.add(producto);

            }
        } catch (SQLException e) {
            System.out.println(e);
        }

        return listadoProductosDisponibles;
    }

    @Override
    public ArrayList<Producto> obtenerProductosNoDisponible() {
        var listadoProductosNoDisponibles = new ArrayList<Producto>();

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(OBTENER_PRODUCTOS_NO_DISPONIBLES);

            while (rs.next()) {
                var producto = new Producto();

                setearValoresProducto(rs, producto);

                listadoProductosNoDisponibles.add(producto);

            }
        } catch (SQLException e) {
            System.out.println(e);
        }

        return listadoProductosNoDisponibles;
    }

    @Override
    public Boolean insertarNuevoProducto(Producto producto) {
        Boolean resultado = false;

        try (var ps = conexion.prepareStatement(INSERTAR_NUEVO_PRODUCTO)) {
            ps.setString(1, producto.getNombreProducto());
            ps.setString(2, producto.getTipoProducto());
            ps.setFloat(3, producto.getPrecioProducto());
            ps.setBoolean(4, producto.getDisponibilidadProducto());

            if (ps.executeUpdate() == 1) {
                resultado = true;
            }
        } catch (SQLException e) {
            System.out.println(e);
        }

        return resultado;
    }

    @Override
    public Boolean cambiarDisponibilidadProducto(Integer idProducto, Boolean disponibilidad) {
        Boolean resultado = false;
        System.out.println(idProducto);
        System.out.println(disponibilidad);

        if (disponibilidad) {
            disponibilidad = false;
        } else {
            disponibilidad = true;
        }

        try (var ps = conexion.prepareStatement(CAMBIAR_DISPONIBILIDAD_PRODUCTO)) {
            ps.setBoolean(1, disponibilidad);
            ps.setInt(2, idProducto);

            if (ps.executeUpdate() == 1) {
                resultado = true;
            }

        } catch (SQLException e) {
            System.out.println(e);
        }

        return resultado;
    }

    @Override
    public ArrayList<Producto> obtenerProductosPedido(Integer id_pedido) {
        var listaProductos = new ArrayList<Producto>();

        try (var ps = conexion.prepareStatement(TRAER_PRODUCTOS_PEDIDO_CONCRETO)) {
            ps.setInt(1, id_pedido);

            var rs = ps.executeQuery();

            while (rs.next()) {
                var pro = new Producto();

                dao.setearValoresProducto(rs, pro);

                listaProductos.add(pro);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return listaProductos;
    }

    @Override
    public void traerTipoProductos() {

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(TRAER_TIPOS_PRODUCTO);

            while (rs.next()) {
                String tipo = "";
                tipo = rs.getString(1);

                tipos.add(tipo);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < tipos.size(); i++) {
            System.out.println((i + 1) + ".- " + tipos.get(i));
        }
    }

    @Override
    public void traerProductosCaros() {
        var listadoProductosCaros = new ArrayList<Producto>();
        Float precioMasCaro = 0f;

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(PRODUCTOS_MAS_CAROS);

            while (rs.next()) {
                var producto = new Producto();
                setearValoresProducto(rs, producto);
                listadoProductosCaros.add(producto);
                precioMasCaro = producto.getPrecioProducto();
            }

            System.out.println("El precio más caro de la carta es de: " + precioMasCaro + " euros.");
            System.out.println("Los productos con ese precio son: ");
            listadoProductosCaros.forEach(
                    producto -> System.out.println(producto)
            );

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void traerProductosBaratos() {
        var listadoProductosBaratos = new ArrayList<Producto>();
        Float precioMasBarato = 0f;

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(PRODUCTOS_MAS_BARATOS);

            while (rs.next()) {
                var producto = new Producto();
                setearValoresProducto(rs, producto);
                listadoProductosBaratos.add(producto);
                precioMasBarato = producto.getPrecioProducto();
            }

            System.out.println("El precio más barato de la carta es de: " + precioMasBarato + " euros.");
            System.out.println("Los productos con ese precio son: ");
            listadoProductosBaratos.forEach(
                    producto -> System.out.println(producto)
            );

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setearValoresProducto(ResultSet rs, Producto producto) throws SQLException {
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombreProducto(rs.getString("nombre"));
        producto.setTipoProducto(rs.getString("tipo"));
        producto.setPrecioProducto(rs.getFloat("precio"));
        producto.setDisponibilidadProducto(rs.getBoolean("disponibilidad"));
    }

    @Override
    public Boolean insertarNuevoPedido(Pedido nuevoPedido) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean cambiarEstadoARecogido(Integer id) {
        Boolean resultado = false;

        try (var ps = conexion.prepareStatement(CAMBIAR_ESTADO_A_RECOGIDO)) {
            ps.setInt(1, id);

            if (ps.executeUpdate() == 1) {
                resultado = true;
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return resultado;
    }

    @Override
    public void eliminarPedido(Pedido pedido) {

    }

    @Override
    public ArrayList<Pedido> verPedidosPendientesHoy() {
        var pedidosPendientesHoy = new ArrayList<Pedido>();

        try (var st = conexion.createStatement()) {
            var rs = st.executeQuery(VER_PEDIDOS_HOY_QUERY);

            while (rs.next()) {
                var pedido = new Pedido();

                setearValoresPedido(rs, pedido);

                pedidosPendientesHoy.add(pedido);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return pedidosPendientesHoy;
    }

    @Override
    public ArrayList<Pedido> verPedidosUsuarioConcreto(String nombre) {
        var listadoPedidos = new ArrayList<Pedido>();

        try (var ps = conexion.prepareStatement(TRAER_PEDIDOS_CLIENTE_CONCRETO)) {
            ps.setString(1, nombre);

            var rs = ps.executeQuery();

            while (rs.next()) {
                var pedido = new Pedido();
                setearValoresPedido(rs, pedido);

                var listaProducto = new ArrayList<Producto>(dao.obtenerProductosPedido(pedido.getIdPedido()));
                pedido.setListaProductos(listaProducto);

                listadoPedidos.add(pedido);

            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return listadoPedidos;
    }

    @Override
    public ArrayList<String> verClientes() {
        var listadoClientes = new ArrayList<String>();

        try (var st = conexion.createStatement()) {
            var rs = st.executeQuery(TRAER_CLIENTES_QUERY);

            while (rs.next()) {
                listadoClientes.add(rs.getString("cliente"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return listadoClientes;
    }

    @Override
    public void mostrarListadoPedidos() {
        var listadoPedidos = new ArrayList<Pedido>();

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(MOSTRAR_TOTAL_PEDIDOS);

            while (rs.next()) {
                var ped = new Pedido();
                setearValoresPedido(rs, ped);

                var productos = new ArrayList<Producto>(dao.obtenerProductosPedido(ped.getIdPedido()));
                ped.setListaProductos(productos);

                listadoPedidos.add(ped);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        listadoPedidos.forEach(
                pedido -> System.out.println("Pedido con ID " + pedido.getIdPedido() + ": " +
                        "\n\t- Cliente: " + pedido.getNombreCliente() +
                        "\n\t- Fecha del pedido: " + pedido.getFechaPedido() +
                        "\n\t- Estado: " + pedido.getEstadoPedido())
        );
    }

    private static void setearValoresPedido(ResultSet rs, Pedido ped) throws SQLException {
        ped.setIdPedido(rs.getInt("id_pedido"));
        ped.setFechaPedido(rs.getDate("fecha"));
        ped.setNombreCliente(rs.getString("cliente"));
        ped.setEstadoPedido(rs.getString("estado"));
    }

    @Override
    public Pedido traerPedidoPorId(Integer id_pedido) {
        var pedido = new Pedido();

        try (var ps = conexion.prepareStatement(TRAER_PEDIDO_POR_ID)) {
            var rs = ps.executeQuery();

            pedido.setIdPedido(rs.getInt("id_pedido"));
            pedido.setFechaPedido(rs.getDate("fecha"));
            pedido.setNombreCliente(rs.getString("cliente"));

            var listaProductos = new ArrayList<Producto>(dao.obtenerProductosPedido(pedido.getIdPedido()));
            pedido.setListaProductos(listaProductos);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return pedido;
    }

    @Override
    public ArrayList<Pedido> traerPedidosPendientes() {
        var pedidosPendientes = new ArrayList<Pedido>();

        try (var st = conexion.createStatement()) {
            ResultSet rs = st.executeQuery(TRAER_PEDIDOS_PENDIENTES);

            while (rs.next()) {
                var pedido = new Pedido();

                setearValoresPedido(rs, pedido);

                pedidosPendientes.add(pedido);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return pedidosPendientes;
    }


    public void switchMenuProducto() {
        var sc = new Scanner(System.in);

        Integer opcion = 1;

        while (opcion != 0) {
            menuProducto();
            opcion = sc.nextInt();

            switch (opcion) {
                case 1:
                    listarProductos();
                    break;
                case 2:
                    mostrarInformacionProductoConcreto(sc);
                    break;
                case 3:
                    listarProductosDisponibles();

                    break;
                case 4:
                    listarProductosNoDisponibles();

                    break;
                case 5:
                    insertarNuevoProducto(sc);

                    break;
                case 6:
                    actualizarProducto();

                    break;
                case 7:
                    cambiarEstadoProducto();

                    break;

                case 8:
                    menuEstadisticasProducto();

                    break;
                case 0:
                    limpiarPantalla();
                    dao.mostrarMenuPrincipal();
                    break;

                default:
                    System.out.println("Elección incorrecta.");
                    pulsarTeclaContinuar();
            }

        }
    }

    public void switchMenuPedido() {
        var sc = new Scanner(System.in);

        var opcion = 6;

        while (opcion != 0) {
            menuPedido();
            opcion = sc.nextInt();

            switch (opcion) {
                case 0:
                    limpiarPantalla();
                    mostrarMenuPrincipal();
                    break;
                case 1:
                    mostrarListadoPedidos();
                    pulsarTeclaContinuar();
                    limpiarPantalla();
                    opcion = 6;
                    break;
                case 2:
                    opcion = opcionMostrarPedidoCliente(sc, opcion);
                    pulsarTeclaContinuar();
                    limpiarPantalla();
                    opcion = 6;
                    break;
                case 3:
                    opcionVerPedidosPendientesHoy();
                    pulsarTeclaContinuar();
                    limpiarPantalla();
                    opcion = 6;
                    break;
                case 4:
                    opcion = opcionCambiarEstadoARecogido(sc, opcion);
                    pulsarTeclaContinuar();
                    limpiarPantalla();
                    opcion = 6;
                    break;
                case 5:
                    pulsarTeclaContinuar();
                    limpiarPantalla();
                    break;
            }

        }
    }

    private static boolean insertarNuevoPedido() {
        Boolean comprobadorPedidoCorrecto = false;
        Boolean comprobadorValoresCorrectos = false;
        var escaner = new Scanner(System.in);

        /* Variables que añadir a la consulta */
        String cliente = "";


        limpiarPantalla();
        while (!comprobadorPedidoCorrecto) {
            System.out.println("Menú para insertar un nuevo producto.");
            System.out.print("Introduzca su nombre: ");
            cliente = escaner.next();
            System.out.println();



        }



        return comprobadorPedidoCorrecto;
    }
    private static void opcionVerPedidosPendientesHoy() {
        var pedidosPendientes = new ArrayList<Pedido>(dao.verPedidosPendientesHoy());

        if (pedidosPendientes.size() > 0) {
            pedidosPendientes.forEach(
                    pedido -> System.out.println("Pedido con ID " + pedido.getIdPedido() + ": " +
                            "\n\t- Cliente: " + pedido.getNombreCliente() +
                            "\n\t- Fecha del pedido: " + pedido.getFechaPedido() +
                            "\n\t- Estado: " + pedido.getEstadoPedido())
            );
        } else {
            System.out.println("Actualmente no contamos con pedidos pendientes.");
        }
    }

    private int opcionCambiarEstadoARecogido(Scanner sc, int opcion) {

        limpiarPantalla();
        var pedidosPendientes = new ArrayList<Pedido>(dao.traerPedidosPendientes());

        if (pedidosPendientes.size() > 0) {
            pedidosPendientes.forEach(
                    pedido -> System.out.println("Pedido con ID " + pedido.getIdPedido() + ": " +
                            "\n\t- Cliente: " + pedido.getNombreCliente() +
                            "\n\t- Fecha del pedido: " + pedido.getFechaPedido() +
                            "\n\t- Estado: " + pedido.getEstadoPedido())
            );
            System.out.println("Seleccione el ID del pedido: ");
            opcion = sc.nextInt();

            if (cambiarEstadoARecogido(opcion)) {
                System.out.println("El estado el pedido ha sido modificado con éxito");
            } else {
                System.out.println("No se ha podido modificar el estado del pedido.");
            }
        } else {
            System.out.println("Actualmente no existen pedidos pendientes.");
        }

        return opcion;
    }

    private int opcionMostrarPedidoCliente(Scanner sc, int opcion) {
        limpiarPantalla();
        var listadoClientes = new ArrayList<String>(verClientes());

        var comprobador = false;

        while (!comprobador) {
            System.out.println("Listado de clientes: ");

            for (int i = 0; i < listadoClientes.size(); i++) {
                System.out.println((i + 1) + ".- " + listadoClientes.get(i));
            }
            System.out.print("Seleccione una opción: ");
            opcion = sc.nextInt();
            opcion -= 1;

            if (opcion >= 0 && opcion < listadoClientes.size()) {
                comprobador = true;
            } else {
                System.out.println("Ha elegido una opción incorrecta.");
            }
        }

        limpiarPantalla();
        var listadoPedidos = new ArrayList<Pedido>(verPedidosUsuarioConcreto(listadoClientes.get(opcion)));
        System.out.println("El usuario " + listadoClientes.get(opcion) + " cuenta con " + listadoPedidos.size() + " pedidos en nuestra base de datos.");
        for (Pedido pedido : listadoPedidos) {
            System.out.println("\nPedido con ID " + pedido.getIdPedido() + ": " +
                    "\n\t- Fecha del pedido: " + pedido.getFechaPedido() +
                    "\n\t- Estado del pedido: " + pedido.getEstadoPedido() +
                    "\n\t- Productos: ");
            for (Producto producto : pedido.getListaProductos()) {
                System.out.println("\t\t- " + producto.getNombreProducto() + " - Precio: " + producto.getPrecioProducto());
            }
        }
        return opcion;
    }

    private static void cambiarEstadoProducto() {
        listarProductos();

        Scanner sc3 = new Scanner(System.in);
        Boolean validador = false;
        Integer eleccion = 0;
        Boolean disponibilidad;

        while (!validador) {
            System.out.println("¿A qué producto quieres cambiarle su estado?: ");
            eleccion = sc3.nextInt();

            if (eleccion > 0 && eleccion < listadoProductos.size()) {
                validador = true;
            } else {
                System.out.println("No ha elegido un producto correcto");
            }
        }
        Producto producto = new Producto();
        producto = listadoProductos.get(eleccion - 1);
        System.out.println(producto.getNombreProducto());


        if (dao.cambiarDisponibilidadProducto(eleccion, producto.getDisponibilidadProducto())) {
            System.out.println("Producto cambiado.");


        } else {
            System.out.println("No se ha podido modificar el estado del producto");
        }
    }

    private void actualizarProducto() {
        Scanner escaner = new Scanner(System.in);
        Integer lectorEnteros = 0;
        String lectorTextos = "1";
        Float lectorFloat = 0f;
        listarProductos();

        Boolean comprobador = false;

        while (comprobador != true) {
            System.out.print("¿Qué producto quiere modificar?: ");
            lectorEnteros = escaner.nextInt();

            if (lectorEnteros > 0 && lectorEnteros < listadoProductos.size()) {
                comprobador = true;
            } else {
                System.out.println("No ha elegido un producto correcto.");
            }
        }

        comprobador = false;
        var productoElegido = new Producto();
        productoElegido = listadoProductos.get(lectorEnteros - 1);
        lectorEnteros = 0;

        while (comprobador != true) {
            System.out.println("\nEl nombre actual del producto es: " + productoElegido.getNombreProducto());
            System.out.print("¿Cuál será su nombre?: ");
            lectorTextos = escaner.next();

            try {
                if (lectorTextos == "") {
                    comprobador = true;
                } else if (lectorTextos != "") {
                    productoElegido.setNombreProducto(lectorTextos);
                    comprobador = true;
                }
            } catch (InputMismatchException e) {
                System.out.println(e);
            }

        }

        comprobador = false;
        lectorTextos = "";

        while (!comprobador) {
            System.out.println("\nEl tipo actual del producto es: " + productoElegido.getTipoProducto());
            traerTipoProductos();
            System.out.print("Elija su nuevo tipo: ");
            lectorEnteros = escaner.nextInt();

            if (lectorEnteros == KeyEvent.VK_ENTER) {
                comprobador = true;
            } else if (lectorEnteros > 0 && lectorEnteros <= tipos.size()) {

                comprobador = true;
                productoElegido.setTipoProducto(tipos.get(lectorEnteros - 1));
            }
            tipos.removeAll(tipos);
        }

        comprobador = false;
        lectorEnteros = 0;

        while (!comprobador) {
            System.out.println("\nEl precio actual del producto es: " + productoElegido.getPrecioProducto());
            System.out.print("Introduzca su nuevo precio: ");

            try {
                lectorFloat = escaner.nextFloat();

                if (lectorFloat == 0f) {
                    comprobador = true;
                } else if (lectorFloat != 0f) {
                    productoElegido.setPrecioProducto(lectorFloat);
                    comprobador = true;
                }
            } catch (InputMismatchException e) {
                System.out.println(e);
            }
        }

        lectorFloat = 0f;
        comprobador = false;
        String dispo = "";

        if (productoElegido.getDisponibilidadProducto()) {
            dispo = "Disponible";
        } else {
            dispo = "No disponible";
        }

        while (!comprobador) {
            System.out.println("El producto está: " + dispo);
            System.out.print("¿Quiere cambiar la disponiblidad?: ");
            System.out.println("\n1.- Sí.");
            System.out.println("2.- No.");
            System.out.println("Elija su opción: ");

            try {
                lectorEnteros = escaner.nextInt();

                if (lectorEnteros == 1 || lectorEnteros == 2) {
                    if (lectorEnteros == 1) {
                        if (productoElegido.getDisponibilidadProducto()) {
                            productoElegido.setDisponibilidadProducto(false);
                        } else {
                            productoElegido.setDisponibilidadProducto(true);
                        }
                    } else {

                    }
                    comprobador = true;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try (var ps = conexion.prepareStatement(MODIFICAR_PRODUCTO)) {
            ps.setString(1, productoElegido.getNombreProducto());
            ps.setString(2, productoElegido.getTipoProducto());
            ps.setFloat(3, productoElegido.getPrecioProducto());
            ps.setBoolean(4, productoElegido.getDisponibilidadProducto());
            ps.setInt(5, productoElegido.getIdProducto());

            if (ps.executeUpdate() == 1) {
                System.out.println("\nProducto actualizado.");
                System.out.println(productoElegido);
            } else {
                System.out.println("El producto no ha podido ser actualizado.");
                dao.switchMenuProducto();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void insertarNuevoProducto(Scanner sc) {
        var listaProductosNombre = new ArrayList<Producto>(dao.obtenerProductosCarta());
        Scanner sc2 = new Scanner(System.in);

        var productoNuevo = new Producto();

        Boolean productoValido = false;
        String nombreProducto = "";
        Integer contador = 0;

        while (!productoValido) {
            System.out.println();
            System.out.println("\nInsertar nuevo producto:");
            System.out.print("Indique el nombre de su producto: ");
            nombreProducto = sc2.nextLine();

            for (Producto producto : listaProductosNombre) {
                if (producto.getNombreProducto().toLowerCase().equals(nombreProducto.toLowerCase())) {
                    contador++;
                }

            }

            if (contador == 0) {
                productoValido = true;
            } else {
                System.out.println("El producto ya se encuentra en la carta.");
                contador = 0;
            }

        }

        Integer opcionTipo = 5;

        while (opcionTipo < 0 || opcionTipo > 4) {
            System.out.println();
            System.out.println("Insertar tipo de producto: ");
            System.out.println("1.- Refresco.");
            System.out.println("2.- Bebida.");
            System.out.println("3.- Bocadillo");
            System.out.println("4.- Bollería.");
            System.out.print("Elija la opción ");
            opcionTipo = sc.nextInt();

        }

        System.out.println();
        System.out.print("Introduzca el precio del producto: ");
        Float precioProducto = sc.nextFloat();

        Boolean comprobarResultado = false;
        Integer opcionDisponibilidad = 2;
        Boolean opcionBoolean = false;

        while (!comprobarResultado) {
            System.out.println();
            System.out.println("Introduzca disponibilidad del producto: ");
            System.out.println("0.- No disponible. ");
            System.out.println("1.- Disponible. ");
            System.out.print("Escriba: ");
            opcionDisponibilidad = sc.nextInt();

            if (opcionDisponibilidad != 0 && opcionDisponibilidad != 1) {
                System.out.println("No ha elegido una opción correcta.");
            } else {
                comprobarResultado = true;

                opcionBoolean = opcionDisponibilidad != 0;
            }
        }

        String tipoProducto = switch (opcionTipo) {
            case 1 -> "Refresco";
            case 2 -> "Bebida";
            case 3 -> "Bocadillo";
            case 4 -> "Bolleria";
            default -> "";
        };

        productoNuevo.setNombreProducto(nombreProducto);
        productoNuevo.setTipoProducto(tipoProducto);
        productoNuevo.setPrecioProducto(precioProducto);
        productoNuevo.setDisponibilidadProducto(opcionBoolean);


        if (dao.insertarNuevoProducto(productoNuevo)) {
            System.out.println();
            System.out.println("Producto insertado con éxito.");
        }
    }

    private static void listarProductosNoDisponibles() {
        var listaProductosNoDisponibles = new ArrayList<Producto>(dao.obtenerProductosNoDisponible());

        System.out.println();
        System.out.println("Listado de productos no disponibles: ");

        if (listaProductosNoDisponibles.size() > 0) {
            listaProductosNoDisponibles.forEach(
                    producto -> System.out.println("\t- Nombre: " + producto.getNombreProducto() +
                            " - Precio: " + producto.getPrecioProducto()));
        } else {
            System.out.println("Actualmente todos los productos están disponibles.");
        }
    }

    private static void listarProductosDisponibles() {
        var listaProductosDisponibles = new ArrayList<Producto>(dao.obtenerProductosDisponibles());

        System.out.println();
        System.out.println("Listado de productos disponibles: ");

        if (listaProductosDisponibles.size() > 0) {
            listaProductosDisponibles.forEach(
                    producto -> System.out.println("\t- Nombre: " + producto.getNombreProducto() +
                            " - Precio: " + producto.getPrecioProducto()));
        } else {
            System.out.println("Actualmente ningún producto está disponible.");
        }
    }

    private static void mostrarInformacionProductoConcreto(Scanner sc) {
        System.out.println();
        listarProductos();

        System.out.print("¿De qué producto quiere ver información?: ");
        Integer eleccionProducto = sc.nextInt();
        System.out.println(eleccionProducto);
        if (eleccionProducto > 0 && eleccionProducto <= listadoProductos.size()) {
            var producto = dao.obtenerProductoPorId(eleccionProducto);

            System.out.println(producto);
        } else {
            System.out.println("No ha introducido un número correcto.");
        }
    }

    private static void listarProductos() {
        var listadoProductos = new ArrayList<Producto>(dao.obtenerProductosCarta());
        System.out.println("Listado de producto: ");
        listadoProductos.forEach(
                producto -> System.out
                        .println("\t" + producto.getIdProducto() + ".- " + producto.getNombreProducto()));
    }

    static public void pulsarTeclaContinuar() {
        String seguir;
        Scanner teclado = new Scanner(System.in);
        System.out.println("\nPulsa una tecla para continuar...");
        try {
            seguir = teclado.nextLine();
        } catch (Exception e) {
        }
    }

    static public void limpiarPantalla() {
        for (int i = 0; i < 20; i++) {
            System.out.println();
        }
    }

    static public void menuPedido() {
        System.out.println();
        System.out.println("MENÚ DE PEDIDOS");
        System.out.println("1.- Mostrar total de pedidos. ");
        System.out.println("2.- Mostrar pedido de un cliente concreto. ");
        System.out.println("3.- Ver pedidos pendientes de hoy.");
        System.out.println("4.- Cambiar estado a recogido.");
        System.out.println("0.- Volver al menú principal.");
        System.out.print("Seleccione opción: ");
    }

    private static void menuProducto() {
        System.out.println();
        System.out.println("MENÚ DE PRODUCTOS");
        System.out.println("1.- Mostrar carta de productos. ");
        System.out.println("2.- Mostrar información de un producto. ");
        System.out.println("3.- Mostrar productos disponibles.");
        System.out.println("4.- Mostrar productos no disponibles.");
        System.out.println("5.- Insertar un nuevo producto.");
        System.out.println("6.- Modificar un producto.");
        System.out.println("7.- Cambiar estado de un producto.");
        System.out.println("8.- Ver estadísticas de la carta de productos.");
        System.out.println("0.- Volver al menú principal.");
        System.out.print("Seleccione opción: ");
    }

    private static void menuEstadisticasProducto() {
        Scanner sc = new Scanner(System.in);
        Boolean comprobador = false;
        Integer opcion = 0;

        while (!comprobador) {
            System.out.println("MENÚ ESTADÍSTICAS PRODUCTO");
            System.out.println("1.- Ver producto más caro.");
            System.out.println("2.- Ver producto más barato.");
            System.out.println("3.- Volver al menú de producto.");
            System.out.print("Elija una opción: ");
            opcion = sc.nextInt();

            if (opcion >= 1 && opcion <= 3) {
                comprobador = true;
            } else {
                System.out.println("No ha elegido una opción válida. ");
                pulsarTeclaContinuar();
            }
        }

        switch (opcion) {
            case 1:
                limpiarPantalla();
                dao.traerProductosCaros();
                pulsarTeclaContinuar();
                limpiarPantalla();
                menuEstadisticasProducto();

                break;
            case 2:
                dao.traerProductosBaratos();
                break;
            case 3:
                System.out.println("Volviendo al menú de producto. ");
                pulsarTeclaContinuar();
                limpiarPantalla();
                dao.switchMenuProducto();
                break;
            default:
                System.out.println("Ha elegido una opción incorrecta.");
        }


    }

    static public void mostrarMenuPrincipal() {
        var comprobador = false;
        var sc = new Scanner(System.in);
        var opcion = 5;

        while (!comprobador) {
            System.out.println("¡Bienvenido al menú principal!");
            System.out.println("Seleccione la opción que desea ver: ");
            System.out.println("1.- Pedidos.");
            System.out.println("2.- Productos.");
            System.out.println("0.- Salir del programa.");
            System.out.print("Seleccione la opción deseada: ");
            opcion = sc.nextInt();

            if (opcion >= 0 & opcion < 3) {
                comprobador = true;
            } else {
                System.out.println("Ha elegido una opción incorrecta.");
            }
        }

        if (opcion == 0) {
            System.out.println("Saliendo del programa...");
            System.exit(0);
        } else if (opcion == 1) {
            limpiarPantalla();
            dao.switchMenuPedido();
        } else if (opcion == 2) {
            limpiarPantalla();
            dao.switchMenuProducto();
        }

    }
}
