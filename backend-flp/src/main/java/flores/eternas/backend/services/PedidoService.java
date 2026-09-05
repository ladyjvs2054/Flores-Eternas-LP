package flores.eternas.backend.services;

import flores.eternas.backend.dto.ComposicionRamoDTO;
import flores.eternas.backend.dto.CrearPedidoRequest;
import flores.eternas.backend.dto.PedidoRequestDTO;
import flores.eternas.backend.dto.PedidoResponseDTO;
import flores.eternas.backend.exception.ValidacionException;
import flores.eternas.backend.model.*;
import flores.eternas.backend.model.enums.Estado;
import flores.eternas.backend.repository.*;
import jakarta.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final TipoFlorRepository tipoFlorRepository;
    private final ColorFlorRepository colorFlorRepository;
    private final InventarioRepository inventarioRepository;
    private final RamoRepository ramoRepository;
    private final PedidoRepository pedidoRepository;
    private final CategoriaRamoRepository categoriaRamoRepository;
    private final DetalleAnadidoRepository detalleAnadidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PersonaRepository personaRepository;
    private final EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public PedidoService(
            TipoFlorRepository tipoFlorRepository,
            ColorFlorRepository colorFlorRepository,
            InventarioRepository inventarioRepository,
            RamoRepository ramoRepository,
            PedidoRepository pedidoRepository,
            CategoriaRamoRepository categoriaRamoRepository,
            DetalleAnadidoRepository detalleAnadidoRepository,
            DetallePedidoRepository detallePedidoRepository,
            PersonaRepository personaRepository,
            EmailService emailService) {
        this.tipoFlorRepository = tipoFlorRepository;
        this.colorFlorRepository = colorFlorRepository;
        this.inventarioRepository = inventarioRepository;
        this.ramoRepository = ramoRepository;
        this.pedidoRepository = pedidoRepository;
        this.categoriaRamoRepository = categoriaRamoRepository;
        this.detalleAnadidoRepository = detalleAnadidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.personaRepository = personaRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarPedidos() {
        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaCreacionDesc();
        return pedidos.stream().map(this::toResponseDTO).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Pedido crearPedido(CrearPedidoRequest request) {
        if (request.getFlores() == null || request.getFlores().isEmpty()) {
            throw new ValidacionException("Debe incluir al menos una flor en el pedido.");
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaMinima = sumarDiasHabiles(hoy, 5);
        if (request.getFechaEntrega() != null) {
            try {
                LocalDate fechaEntrega = LocalDate.parse(request.getFechaEntrega());
                if (fechaEntrega.isBefore(fechaMinima)) {
                    throw new ValidacionException("La fecha de entrega debe ser al menos 5 días hábiles después de hoy.");
                }
            } catch (Exception e) {
                if (e instanceof ValidacionException) throw e;
                throw new ValidacionException("Fecha de entrega inválida.");
            }
        } else {
            request.setFechaEntrega(fechaMinima.toString());
        }

        BigDecimal precioTotalFlores = BigDecimal.ZERO;
        BigDecimal precioAdiciones = BigDecimal.ZERO;

        StringBuilder adicionesJson = new StringBuilder();
        if (request.getAdiciones() != null && !request.getAdiciones().isEmpty()) {
            adicionesJson.append("[");
            for (int i = 0; i < request.getAdiciones().size(); i++) {
                var adicionReq = request.getAdiciones().get(i);
                Inventario inventario = inventarioRepository.findById(adicionReq.getInventarioId())
                        .orElseThrow(() -> new RuntimeException("Adicion no encontrada"));

                BigDecimal subtotal = inventario.getPrecioCosto()
                        .multiply(BigDecimal.valueOf(adicionReq.getCantidad()));
                precioAdiciones = precioAdiciones.add(subtotal);

                if (i > 0) adicionesJson.append(",");
                adicionesJson.append("{")
                        .append("\"nombre\":\"").append(escapeJson(inventario.getNombreInventario())).append("\",")
                        .append("\"cantidad\":").append(adicionReq.getCantidad()).append(",")
                        .append("\"precio\":").append(inventario.getPrecioCosto())
                        .append("}");
            }
            adicionesJson.append("]");
        }

        for (CrearPedidoRequest.ItemFlorRequest florReq : request.getFlores()) {
            TipoFlor tipoFlor = tipoFlorRepository.findById(florReq.getTipoFlorId())
                    .orElseThrow(() -> new RuntimeException("Tipo de flor no encontrado: " + florReq.getTipoFlorId()));

            BigDecimal subtotal = tipoFlor.getPrecioUnidad()
                    .multiply(BigDecimal.valueOf(florReq.getCantidad()));
            precioTotalFlores = precioTotalFlores.add(subtotal);
        }

        BigDecimal total = precioTotalFlores.add(precioAdiciones);

        Persona persona = null;
        if (request.getCedula() != null && !request.getCedula().isBlank()) {
            persona = personaRepository.findByCedula(request.getCedula()).orElse(null);
            if (persona == null) {
                persona = new Persona();
                persona.setCedula(request.getCedula());
                persona.setNombreCliente(request.getNombreCliente());
                persona.setTelefono(normalizarTelefono(request.getTelefono()));
                persona = personaRepository.save(persona);
            } else {
                if (request.getNombreCliente() != null) {
                    persona.setNombreCliente(request.getNombreCliente());
                }
                if (request.getTelefono() != null) {
                    persona.setTelefono(normalizarTelefono(request.getTelefono()));
                }
                persona = personaRepository.save(persona);
            }
        }

        Pedido pedido = new Pedido();
        pedido.setTotalPedido(total);
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        LocalDate fechaEntrega = request.getFechaEntrega() != null
            ? LocalDate.parse(request.getFechaEntrega())
            : LocalDate.now().plusDays(5);
        pedido.setFechaEntrega(fechaEntrega);
        pedido.setEstado(Estado.EN_PREPARACION);
        pedido.setFechaCreacion(LocalDateTime.now());
        if (persona != null) {
            pedido.setCliente(persona);
        }
        pedido = pedidoRepository.save(pedido);

        for (CrearPedidoRequest.ItemFlorRequest florReq : request.getFlores()) {
            TipoFlor tipoFlor = tipoFlorRepository.findById(florReq.getTipoFlorId())
                    .orElseThrow(() -> new RuntimeException("Tipo de flor no encontrado: " + florReq.getTipoFlorId()));

            ColorFlor colorFlor = null;
            if (florReq.getColorFlorId() != null) {
                colorFlor = colorFlorRepository.findById(florReq.getColorFlorId())
                        .orElseThrow(() -> new RuntimeException("Color de flor no encontrado: " + florReq.getColorFlorId()));
            }

            DetallePedido detallePedido = new DetallePedido();
            detallePedido.setPedido(pedido);
            detallePedido.setTipoFlor(tipoFlor.getDescripcionFlor());
            detallePedido.setColorFlor(colorFlor != null ? colorFlor.getDescripcionColor() : null);
            detallePedido.setCantidadFlores(florReq.getCantidad());
            detallePedido.setAdicionesJson(adicionesJson.length() > 0 ? adicionesJson.toString() : null);
            detallePedidoRepository.save(detallePedido);
        }

        return pedido;
    }

    @Transactional
    public Pedido crearPedidoPersonalizadoPendiente(CrearPedidoRequest request) {
        if (request.getFlores() == null || request.getFlores().isEmpty()) {
            throw new ValidacionException("Debe incluir al menos una flor en el pedido.");
        }

        BigDecimal precioTotalFlores = BigDecimal.ZERO;
        BigDecimal precioAdiciones = BigDecimal.ZERO;

        StringBuilder adicionesJson = new StringBuilder();
        if (request.getAdiciones() != null && !request.getAdiciones().isEmpty()) {
            adicionesJson.append("[");
            for (int i = 0; i < request.getAdiciones().size(); i++) {
                var adicionReq = request.getAdiciones().get(i);
                Inventario inventario = inventarioRepository.findById(adicionReq.getInventarioId())
                        .orElseThrow(() -> new RuntimeException("Adicion no encontrada"));

                BigDecimal subtotal = inventario.getPrecioCosto()
                        .multiply(BigDecimal.valueOf(adicionReq.getCantidad()));
                precioAdiciones = precioAdiciones.add(subtotal);

                if (i > 0) adicionesJson.append(",");
                adicionesJson.append("{")
                        .append("\"nombre\":\"").append(escapeJson(inventario.getNombreInventario())).append("\",")
                        .append("\"cantidad\":").append(adicionReq.getCantidad()).append(",")
                        .append("\"precio\":").append(inventario.getPrecioCosto())
                        .append("}");
            }
            adicionesJson.append("]");
        }

        for (CrearPedidoRequest.ItemFlorRequest florReq : request.getFlores()) {
            TipoFlor tipoFlor = tipoFlorRepository.findById(florReq.getTipoFlorId())
                    .orElseThrow(() -> new RuntimeException("Tipo de flor no encontrado: " + florReq.getTipoFlorId()));

            BigDecimal subtotal = tipoFlor.getPrecioUnidad()
                    .multiply(BigDecimal.valueOf(florReq.getCantidad()));
            precioTotalFlores = precioTotalFlores.add(subtotal);
        }

        BigDecimal total = precioTotalFlores.add(precioAdiciones);

        Persona persona = null;
        if (request.getNombreCliente() != null && !request.getNombreCliente().isBlank()) {
            if (request.getCedula() != null && !request.getCedula().isBlank()) {
                persona = personaRepository.findByCedula(request.getCedula()).orElse(null);
            }
            if (persona == null) {
                persona = new Persona();
                persona.setNombreCliente(request.getNombreCliente());
                if (request.getCedula() != null) persona.setCedula(request.getCedula());
                if (request.getTelefono() != null) persona.setTelefono(normalizarTelefono(request.getTelefono()));
                persona = personaRepository.save(persona);
            } else {
                persona.setNombreCliente(request.getNombreCliente());
                if (request.getTelefono() != null) {
                    persona.setTelefono(normalizarTelefono(request.getTelefono()));
                }
                persona = personaRepository.save(persona);
            }
        }

        LocalDate hoy = LocalDate.now();
        LocalDate fechaMinima = sumarDiasHabiles(hoy, 5);
        LocalDate fechaEntrega;
        if (request.getFechaEntrega() != null) {
            try {
                fechaEntrega = LocalDate.parse(request.getFechaEntrega());
                if (fechaEntrega.isBefore(fechaMinima)) {
                    throw new ValidacionException("La fecha de entrega debe ser al menos 5 días hábiles después de hoy.");
                }
            } catch (Exception e) {
                if (e instanceof ValidacionException) throw e;
                throw new ValidacionException("Fecha de entrega inválida.");
            }
        } else {
            fechaEntrega = fechaMinima;
        }

        Pedido pedido = new Pedido();
        pedido.setTotalPedido(total);
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setCiudad(request.getCiudad());
        pedido.setRegion(request.getRegion());
        pedido.setFechaEntrega(fechaEntrega);
        pedido.setEstado(Estado.EN_PROCESO);
        pedido.setMontoPagado(BigDecimal.ZERO);
        pedido.setEmailCliente(request.getEmailCliente());
        pedido.setPagoToken(UUID.randomUUID().toString());
        pedido.setTipoPedido("PERSONALIZADO");
        pedido.setFechaCreacion(LocalDateTime.now());
        if (persona != null) {
            pedido.setCliente(persona);
        }
        pedido = pedidoRepository.save(pedido);

        for (CrearPedidoRequest.ItemFlorRequest florReq : request.getFlores()) {
            TipoFlor tipoFlor = tipoFlorRepository.findById(florReq.getTipoFlorId())
                    .orElseThrow(() -> new RuntimeException("Tipo de flor no encontrado: " + florReq.getTipoFlorId()));

            ColorFlor colorFlor = null;
            if (florReq.getColorFlorId() != null) {
                colorFlor = colorFlorRepository.findById(florReq.getColorFlorId())
                        .orElseThrow(() -> new RuntimeException("Color de flor no encontrado: " + florReq.getColorFlorId()));
            }

            DetallePedido detallePedido = new DetallePedido();
            detallePedido.setPedido(pedido);
            detallePedido.setTipoFlor(tipoFlor.getDescripcionFlor());
            detallePedido.setColorFlor(colorFlor != null ? colorFlor.getDescripcionColor() : null);
            detallePedido.setCantidadFlores(florReq.getCantidad());
            detallePedido.setAdicionesJson(adicionesJson.length() > 0 ? adicionesJson.toString() : null);
            detallePedidoRepository.save(detallePedido);
        }

        return pedido;
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerPedidoPorToken(String token) {
        Pedido pedido = pedidoRepository.findByPagoToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado para el token: " + token));
        return toResponseDTO(pedido);
    }

    @Transactional
    public Pedido crearPedidoPendiente(PedidoRequestDTO request) {
        LocalDate hoy = LocalDate.now();
        boolean tienePersonalizados = request.getFloresPersonalizadas() != null && !request.getFloresPersonalizadas().isEmpty();

        if (tienePersonalizados) {
            LocalDate fechaMinima = sumarDiasHabiles(hoy, 5);
            if (request.getFechaEntrega() == null || request.getFechaEntrega().isBefore(fechaMinima)) {
                throw new ValidacionException("La fecha de entrega debe ser al menos 5 días hábiles después de hoy.");
            }
        } else {
            if (request.getFechaEntrega() == null || request.getFechaEntrega().isBefore(hoy)) {
                throw new ValidacionException("La fecha de entrega no puede ser anterior a hoy.");
            }
        }

        Persona persona;

        if (tienePersonalizados && request.getCedulaCliente() != null && !request.getCedulaCliente().isBlank()) {
            persona = personaRepository.findByCedula(request.getCedulaCliente()).orElse(null);
            if (persona == null) {
                persona = new Persona();
                persona.setCedula(request.getCedulaCliente());
                persona.setNombreCliente(request.getNombreCliente());
                persona.setTelefono(normalizarTelefono(request.getTelefonoCliente()));
                persona = personaRepository.save(persona);
            } else {
                persona.setNombreCliente(request.getNombreCliente());
                if (request.getTelefonoCliente() != null) {
                    persona.setTelefono(normalizarTelefono(request.getTelefonoCliente()));
                }
                persona = personaRepository.save(persona);
            }
        } else {
            persona = new Persona();
            persona.setNombreCliente(request.getNombreCliente());
            persona.setTelefono(null);
            persona.setCedula(null);
            persona.setFechaNacimiento(null);
            persona = personaRepository.save(persona);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<DetallePedido> detalles = new ArrayList<>();

        if (request.getItems() != null) {
            for (PedidoRequestDTO.ItemPedidoDTO itemReq : request.getItems()) {
                Ramo ramo = ramoRepository.findById(itemReq.getIdRamo())
                        .orElseThrow(() -> new EntityNotFoundException("Ramo no encontrado: " + itemReq.getIdRamo()));

                BigDecimal subtotal = ramo.getPrecioRamo().multiply(BigDecimal.valueOf(itemReq.getCantidad()));
                total = total.add(subtotal);

                DetallePedido detalle = new DetallePedido();
                detalle.setRamo(ramo);
                detalle.setCantidad(itemReq.getCantidad());
                detalle.setPedido(null);
                detalles.add(detalle);
            }
        }

        StringBuilder adicionesJson = new StringBuilder();
        if (tienePersonalizados) {
            if (request.getAdicionesPersonalizadas() != null && !request.getAdicionesPersonalizadas().isEmpty()) {
                adicionesJson.append("[");
                for (int i = 0; i < request.getAdicionesPersonalizadas().size(); i++) {
                    var adicionReq = request.getAdicionesPersonalizadas().get(i);
                    Inventario inventario = inventarioRepository.findById(adicionReq.getInventarioId())
                            .orElseThrow(() -> new RuntimeException("Adicion no encontrada"));

                    BigDecimal subtotal = inventario.getPrecioCosto()
                            .multiply(BigDecimal.valueOf(adicionReq.getCantidad()));
                    total = total.add(subtotal);

                    if (i > 0) adicionesJson.append(",");
                    adicionesJson.append("{")
                            .append("\"nombre\":\"").append(escapeJson(inventario.getNombreInventario())).append("\",")
                            .append("\"cantidad\":").append(adicionReq.getCantidad()).append(",")
                            .append("\"precio\":").append(inventario.getPrecioCosto())
                            .append("}");
                }
                adicionesJson.append("]");
            }

            String adicionesStr = adicionesJson.length() > 0 ? adicionesJson.toString() : null;
            for (CrearPedidoRequest.ItemFlorRequest florReq : request.getFloresPersonalizadas()) {
                TipoFlor tipoFlor = tipoFlorRepository.findById(florReq.getTipoFlorId())
                        .orElseThrow(() -> new RuntimeException("Tipo de flor no encontrado: " + florReq.getTipoFlorId()));

                BigDecimal subtotal = tipoFlor.getPrecioUnidad()
                        .multiply(BigDecimal.valueOf(florReq.getCantidad()));
                total = total.add(subtotal);

                ColorFlor colorFlor = null;
                if (florReq.getColorFlorId() != null) {
                    colorFlor = colorFlorRepository.findById(florReq.getColorFlorId())
                            .orElseThrow(() -> new RuntimeException("Color de flor no encontrado: " + florReq.getColorFlorId()));
                }

                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(null);
                detalle.setTipoFlor(tipoFlor.getDescripcionFlor());
                detalle.setColorFlor(colorFlor != null ? colorFlor.getDescripcionColor() : null);
                detalle.setCantidadFlores(florReq.getCantidad());
                detalle.setAdicionesJson(adicionesStr);
                detalles.add(detalle);
            }
        }

        Pedido pedido = new Pedido();
        pedido.setTotalPedido(total);
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setFechaEntrega(request.getFechaEntrega());
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setCliente(persona);
        pedido.setMetodoPago(null);
        pedido.setMontoPagado(BigDecimal.ZERO);
        pedido.setTipoPedido(request.getTipoPedido());
        pedido.setEmailCliente(request.getEmailCliente());
        pedido.setCiudad(request.getCiudad());
        pedido.setRegion(request.getRegion());
        pedido.setEstado(Estado.EN_PROCESO);
        pedido.setPagoToken(UUID.randomUUID().toString());
        pedido = pedidoRepository.save(pedido);

        for (DetallePedido detalle : detalles) {
            detalle.setPedido(pedido);
            detallePedidoRepository.save(detalle);
        }

        return pedido;
    }

    @Transactional
    public PedidoResponseDTO crearPedido(PedidoRequestDTO request) {
        LocalDate hoy = LocalDate.now();
        boolean tienePersonalizados = request.getFloresPersonalizadas() != null && !request.getFloresPersonalizadas().isEmpty();

        if (tienePersonalizados) {
            LocalDate fechaMinima = sumarDiasHabiles(hoy, 5);
            if (request.getFechaEntrega() == null || request.getFechaEntrega().isBefore(fechaMinima)) {
                throw new ValidacionException("La fecha de entrega debe ser al menos 5 días hábiles después de hoy.");
            }
        } else {
            if (request.getFechaEntrega() == null || request.getFechaEntrega().isBefore(hoy)) {
                throw new ValidacionException("La fecha de entrega no puede ser anterior a hoy.");
            }
        }

        Persona persona;

        if (tienePersonalizados && request.getCedulaCliente() != null && !request.getCedulaCliente().isBlank()) {
            persona = personaRepository.findByCedula(request.getCedulaCliente()).orElse(null);
            if (persona == null) {
                persona = new Persona();
                persona.setCedula(request.getCedulaCliente());
                persona.setNombreCliente(request.getNombreCliente());
                persona.setTelefono(normalizarTelefono(request.getTelefonoCliente()));
                persona = personaRepository.save(persona);
            } else {
                persona.setNombreCliente(request.getNombreCliente());
                if (request.getTelefonoCliente() != null) {
                    persona.setTelefono(normalizarTelefono(request.getTelefonoCliente()));
                }
                persona = personaRepository.save(persona);
            }
        } else {
            persona = new Persona();
            persona.setNombreCliente(request.getNombreCliente());
            persona.setTelefono(null);
            persona.setCedula(null);
            persona.setFechaNacimiento(null);
            persona = personaRepository.save(persona);
        }

        List<PedidoResponseDTO.ItemPedidoDTO> itemsResponse = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        List<DetallePedido> detalles = new ArrayList<>();

        // Procesar items predefinidos (ramos)
        if (request.getItems() != null) {
            for (PedidoRequestDTO.ItemPedidoDTO itemReq : request.getItems()) {
                Ramo ramo = ramoRepository.findById(itemReq.getIdRamo())
                        .orElseThrow(() -> new EntityNotFoundException("Ramo no encontrado: " + itemReq.getIdRamo()));

                BigDecimal subtotal = ramo.getPrecioRamo().multiply(BigDecimal.valueOf(itemReq.getCantidad()));
                total = total.add(subtotal);

                DetallePedido detalle = new DetallePedido();
                detalle.setRamo(ramo);
                detalle.setCantidad(itemReq.getCantidad());
                detalle.setPedido(null);
                detalles.add(detalle);

                PedidoResponseDTO.ItemPedidoDTO itemRes = new PedidoResponseDTO.ItemPedidoDTO();
                itemRes.setNombreRamo(ramo.getNombreRamo());
                itemRes.setCantidad(itemReq.getCantidad());
                itemRes.setPrecioUnitario(ramo.getPrecioRamo());
                itemsResponse.add(itemRes);
            }
        }

        // Procesar flores personalizadas
        StringBuilder adicionesJson = new StringBuilder();
        if (tienePersonalizados) {
            if (request.getAdicionesPersonalizadas() != null && !request.getAdicionesPersonalizadas().isEmpty()) {
                adicionesJson.append("[");
                for (int i = 0; i < request.getAdicionesPersonalizadas().size(); i++) {
                    var adicionReq = request.getAdicionesPersonalizadas().get(i);
                    Inventario inventario = inventarioRepository.findById(adicionReq.getInventarioId())
                            .orElseThrow(() -> new RuntimeException("Adicion no encontrada"));

                    BigDecimal subtotal = inventario.getPrecioCosto()
                            .multiply(BigDecimal.valueOf(adicionReq.getCantidad()));
                    total = total.add(subtotal);

                    if (i > 0) adicionesJson.append(",");
                    adicionesJson.append("{")
                            .append("\"nombre\":\"").append(escapeJson(inventario.getNombreInventario())).append("\",")
                            .append("\"cantidad\":").append(adicionReq.getCantidad()).append(",")
                            .append("\"precio\":").append(inventario.getPrecioCosto())
                            .append("}");
                }
                adicionesJson.append("]");
            }

            String adicionesStr = adicionesJson.length() > 0 ? adicionesJson.toString() : null;
            for (CrearPedidoRequest.ItemFlorRequest florReq : request.getFloresPersonalizadas()) {
                TipoFlor tipoFlor = tipoFlorRepository.findById(florReq.getTipoFlorId())
                        .orElseThrow(() -> new RuntimeException("Tipo de flor no encontrado: " + florReq.getTipoFlorId()));

                BigDecimal subtotal = tipoFlor.getPrecioUnidad()
                        .multiply(BigDecimal.valueOf(florReq.getCantidad()));
                total = total.add(subtotal);

                ColorFlor colorFlor = null;
                if (florReq.getColorFlorId() != null) {
                    colorFlor = colorFlorRepository.findById(florReq.getColorFlorId())
                            .orElseThrow(() -> new RuntimeException("Color de flor no encontrado: " + florReq.getColorFlorId()));
                }

                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(null);
                detalle.setTipoFlor(tipoFlor.getDescripcionFlor());
                detalle.setColorFlor(colorFlor != null ? colorFlor.getDescripcionColor() : null);
                detalle.setCantidadFlores(florReq.getCantidad());
                detalle.setAdicionesJson(adicionesStr);
                detalles.add(detalle);
            }
        }

        BigDecimal montoPagado;
        String tipoPago = request.getTipoPago() != null ? request.getTipoPago() : "COMPLETO";
        if ("PARCIAL".equalsIgnoreCase(tipoPago) && "PERSONALIZADO".equalsIgnoreCase(request.getTipoPedido())) {
            montoPagado = total.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
        } else {
            montoPagado = total;
        }

        Pedido pedido = new Pedido();
        pedido.setTotalPedido(total);
        pedido.setDireccionEntrega(request.getDireccionEntrega());
        pedido.setFechaEntrega(request.getFechaEntrega());
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setCliente(persona);
        pedido.setMetodoPago(null);
        pedido.setMontoPagado(montoPagado);
        pedido.setTipoPedido(request.getTipoPedido());
        pedido.setEmailCliente(request.getEmailCliente());
        pedido.setCiudad(request.getCiudad());
        pedido.setRegion(request.getRegion());
        pedido.setEstado(Estado.EN_PREPARACION);
        pedido = pedidoRepository.save(pedido);

        for (DetallePedido detalle : detalles) {
            detalle.setPedido(pedido);
            detallePedidoRepository.save(detalle);

            if (detalle.getRamo() != null && detalle.getCantidad() != null) {
                Ramo ramo = detalle.getRamo();
                if (ramo.getStock() != null) {
                    int nuevoStock = Math.max(0, ramo.getStock() - detalle.getCantidad());
                    ramo.setStock(nuevoStock);
                    if (nuevoStock <= 0) {
                        ramo.setDisponible(false);
                    }
                    ramoRepository.save(ramo);
                }
            }
        }

        BigDecimal montoPendiente = total.subtract(montoPagado);

        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId(pedido.getId());
        response.setTotal(total);
        response.setMontoPagado(montoPagado);
        response.setMontoPendiente(montoPendiente);
        response.setEstado(pedido.getEstado().name());
        response.setTipoPedido(request.getTipoPedido());
        response.setFechaEntrega(request.getFechaEntrega());
        response.setNombreCliente(persona.getNombreCliente());
        response.setEmailCliente(request.getEmailCliente());
        response.setCiudad(request.getCiudad());
        response.setRegion(request.getRegion());
        response.setItems(itemsResponse);

        if (montoPendiente.compareTo(BigDecimal.ZERO) > 0) {
            response.setMensaje("Pedido registrado. El 50% restante deberá pagarse antes de la entrega.");
        } else {
            response.setMensaje("Pedido confirmado. Pago completo recibido.");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO obtenerPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
        return toResponseDTO(pedido);
    }

    @Transactional
    public PedidoResponseDTO actualizarEstadoPedido(Long id, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));

        Estado estado;
        try {
            estado = Estado.valueOf(nuevoEstado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado invalido: " + nuevoEstado);
        }

        pedido.setEstado(estado);
        pedido = pedidoRepository.save(pedido);

        enviarNotificacionEstado(pedido, estado);

        return toResponseDTO(pedido);
    }

    private void enviarNotificacionEstado(Pedido pedido, Estado estado) {
        String email = pedido.getEmailCliente();
        if (email == null || email.isBlank()) return;

        String asunto;
        String cuerpo;

        switch (estado) {
            case EN_PROCESO:
                asunto = "Pedido #" + pedido.getId() + " en proceso";
                cuerpo = EmailTemplates.notificacionEnProceso(pedido.getId(), frontendUrl);
                break;
            case EN_PREPARACION:
                asunto = "Tu pedido #" + pedido.getId() + " está en preparación";
                cuerpo = EmailTemplates.notificacionEnPreparacion(pedido.getId(), frontendUrl);
                break;
            case PENDIENTE_DE_ENTREGA:
                asunto = "Pedido #" + pedido.getId() + " listo para entrega";
                BigDecimal pendiente = pedido.getTotalPedido().subtract(pedido.getMontoPagado());
                cuerpo = EmailTemplates.notificacionPendienteEntrega(pedido.getId(), pendiente, frontendUrl, pedido.getPagoToken());
                break;
            case ENTREGADO:
                asunto = "¡Pedido #" + pedido.getId() + " entregado con éxito!";
                cuerpo = EmailTemplates.notificacionEntregado(pedido.getId(), frontendUrl);
                break;
            case CANCELADO:
                asunto = "Tu pedido #" + pedido.getId() + " ha sido cancelado";
                cuerpo = EmailTemplates.notificacionCancelado(pedido.getId(), frontendUrl);
                break;
            default:
                return;
        }

        emailService.enviarEmail(email, asunto, cuerpo);
        log.info("Notificación enviada a {} por estado {}", email, estado);
    }

    private LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate resultado = desde;
        int agregados = 0;
        while (agregados < dias) {
            resultado = resultado.plusDays(1);
            if (resultado.getDayOfWeek() != DayOfWeek.SATURDAY && resultado.getDayOfWeek() != DayOfWeek.SUNDAY) {
                agregados++;
            }
        }
        return resultado;
    }

    private PedidoResponseDTO toResponseDTO(Pedido pedido) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(pedido.getId());
        dto.setTotal(pedido.getTotalPedido());
        dto.setMontoPagado(pedido.getMontoPagado());
        dto.setMontoPendiente(pedido.getTotalPedido() != null && pedido.getMontoPagado() != null
                ? pedido.getTotalPedido().subtract(pedido.getMontoPagado())
                : BigDecimal.ZERO);
        dto.setEstado(pedido.getEstado() != null ? pedido.getEstado().name() : null);
        dto.setTipoPedido(pedido.getTipoPedido());
        dto.setFechaEntrega(pedido.getFechaEntrega());
        dto.setFechaCreacion(pedido.getFechaCreacion());
        dto.setNombreCliente(pedido.getCliente() != null ? pedido.getCliente().getNombreCliente() : null);
        dto.setEmailCliente(pedido.getEmailCliente());
        dto.setDireccionEntrega(pedido.getDireccionEntrega());
        dto.setCiudad(pedido.getCiudad());
        dto.setRegion(pedido.getRegion());
        dto.setPagoToken(pedido.getPagoToken());
        dto.setMensaje(null);
        dto.setItems(populateItems(pedido.getId()));
        return dto;
    }

    private List<PedidoResponseDTO.ItemPedidoDTO> populateItems(Long pedidoId) {
        List<PedidoResponseDTO.ItemPedidoDTO> items = new ArrayList<>();
        List<DetallePedido> detallesPedido = detallePedidoRepository.findByPedidoId(pedidoId);
        for (DetallePedido detalle : detallesPedido) {
            PedidoResponseDTO.ItemPedidoDTO item = new PedidoResponseDTO.ItemPedidoDTO();
            if (detalle.getRamo() != null) {
                Ramo ramo = detalle.getRamo();
                item.setNombreRamo(ramo.getNombreRamo());
                item.setCantidad(detalle.getCantidad());
                item.setPrecioUnitario(ramo.getPrecioRamo());
                if (ramo.getDetallesRamo() != null) {
                    List<ComposicionRamoDTO> flores = ramo.getDetallesRamo().stream()
                            .filter(dr -> dr.getTipoFlor() != null)
                            .map(dr -> new ComposicionRamoDTO(
                                    dr.getTipoFlor().getDescripcionFlor(),
                                    dr.getColorFlor() != null ? dr.getColorFlor().getDescripcionColor() : null,
                                    dr.getCantidad() != null ? dr.getCantidad() : 0,
                                    null
                            ))
                            .collect(java.util.stream.Collectors.toList());
                    item.setFlores(flores);
                }
            } else {
                item.setNombreRamo("Ramo Personalizado");
                item.setCantidad(detalle.getCantidadFlores());
                item.setPrecioUnitario(calcularPrecioUnitario(detalle.getTipoFlor()));
            }
            item.setTipoFlor(detalle.getTipoFlor());
            item.setColorFlor(detalle.getColorFlor());
            item.setAdicionesJson(detalle.getAdicionesJson());
            items.add(item);
        }
        return items;
    }

    private BigDecimal calcularPrecioUnitario(String tipoFlor) {
        if (tipoFlor == null || tipoFlor.isBlank()) return BigDecimal.ZERO;
        return tipoFlorRepository.findFirstByDescripcionFlor(tipoFlor)
                .map(TipoFlor::getPrecioUnidad)
                .orElse(BigDecimal.ZERO);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Normaliza el número de teléfono agregando el prefijo nacional +57
     * (Colombia) por defecto cuando el número es local de 10 dígitos.
     * @param telefono número crudo ingresado por el cliente
     * @return número normalizado con prefijo +57, o el valor original
     * @author santiago
     */
    private String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) return telefono;
        String digitos = telefono.replaceAll("[^0-9]", "");
        if (digitos.startsWith("57") && digitos.length() == 12) {
            return "+57 " + digitos.substring(2);
        }
        if (digitos.length() == 10) {
            return "+57 " + digitos;
        }
        return telefono.trim();
    }
}
