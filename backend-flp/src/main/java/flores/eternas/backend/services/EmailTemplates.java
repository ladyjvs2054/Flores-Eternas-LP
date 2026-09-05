package flores.eternas.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author esteban
 * Helper con plantillas HTML para los correos transaccionales del sistema
 * de recuperacion de contrasena y notificaciones de pedidos.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static final String ASUNTO_RECUPERACION = "Recuperación de contraseña - Flores Eternas LP";

    private static String getBaseTemplate(String asunto, String tituloHeader, String contenido, String frontendUrl) {
        String estilos = "margin:0; padding:0; background-color:#FCF9F6; font-family:'Helvetica Neue', Arial, sans-serif; color:#5E3A1F;";
        String estilosTabla = "max-width:600px; background-color:#FFFFFF; border-radius:16px; border:1px solid #E8D5C8; margin: 0 auto;";
        String estilosHeader = "padding:32px 32px 16px 32px; text-align:center; border-bottom:1px solid #FCEEE3;";
        String estilosCuerpo = "padding:32px;";
        String estilosTitulo = "margin:0; font-family:Georgia, serif; font-size:26px; color:#8C5A3C; font-weight:normal;";
        String estilosFooter = "padding:16px 32px 32px 32px; text-align:center; border-top:1px solid #FCEEE3;";
        String estilosFooterTexto = "margin:0; font-size:12px; color:#8C5A3C;";
        
        String logoHtml = "";
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            logoHtml = "<img src=\"" + frontendUrl + "/assets/images/flplogobrown.png\" alt=\"Flores Eternas LP\" style=\"max-height:80px; margin-bottom:16px;\"><br>";
        }

        return "<!DOCTYPE html>"
                + "<html lang=\"es\">"
                + "<head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>" + asunto + "</title>"
                + "</head>"
                + "<body style=\"" + estilos + "\">"
                +   "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"" + estilos + "padding:32px 16px;\">"
                +     "<tr><td align=\"center\">"
                +       "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%\" style=\"" + estilosTabla + "\">"
                +         "<tr><td style=\"" + estilosHeader + "\">"
                +           logoHtml
                +           "<h1 style=\"" + estilosTitulo + "\">" + tituloHeader + "</h1>"
                +         "</td></tr>"
                +         "<tr><td style=\"" + estilosCuerpo + "\">"
                +           contenido
                +         "</td></tr>"
                +         "<tr><td style=\"" + estilosFooter + "\">"
                +           "<p style=\"" + estilosFooterTexto + "\">"
                +             "Flores Eternas LP &mdash; Palmira, Valle del Cauca<br>"
                +             "floreseternaslpcol@gmail.com"
                +           "</p>"
                +         "</td></tr>"
                +       "</table>"
                +     "</td></tr>"
                +   "</table>"
                + "</body>"
                + "</html>";
    }

    public static String codigoRecuperacion(String codigoPlano) {
        String estilosH2 = "margin:0 0 16px 0; font-family:Georgia, serif; font-size:20px; color:#7A4E2D; font-weight:normal;";
        String estilosParrafo = "margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#5E3A1F;";
        String estilosCajaCodigo = "background-color:#FFEDE3; border:2px dashed #F0D3C1; border-radius:12px; padding:24px; text-align:center; margin:24px 0;";
        String estilosEtiquetaCodigo = "margin:0 0 8px 0; font-size:12px; text-transform:uppercase; letter-spacing:2px; color:#8C5A3C;";
        String estilosCodigo = "margin:0; font-family:'Courier New', monospace; font-size:36px; font-weight:bold; letter-spacing:8px; color:#7A4E2D;";
        String estilosAviso = "background-color:#FCEEE3; border-left:4px solid #F0D3C1; padding:12px 16px; border-radius:4px; margin:24px 0;";
        String estilosAvisoTexto = "margin:0; font-size:13px; line-height:1.5; color:#5E3A1F;";

        String contenido = "<h2 style=\"" + estilosH2 + "\">Recuperación de contraseña</h2>"
                + "<p style=\"" + estilosParrafo + "\">Hola,</p>"
                + "<p style=\"" + estilosParrafo + "\">Recibimos una solicitud para restablecer la contraseña de tu cuenta de administración. Usa el siguiente código para continuar con el proceso:</p>"
                + "<div style=\"" + estilosCajaCodigo + "\">"
                + "<p style=\"" + estilosEtiquetaCodigo + "\">Tu código</p>"
                + "<p style=\"" + estilosCodigo + "\">" + codigoPlano + "</p>"
                + "</div>"
                + "<p style=\"" + estilosParrafo + "\">Este código expirará en <strong>10 minutos</strong>.</p>"
                + "<div style=\"" + estilosAviso + "\">"
                + "<p style=\"" + estilosAvisoTexto + "\"><strong>¿No solicitaste este cambio?</strong><br>Si no fuiste tú quien pidió recuperar la contraseña, puedes ignorar este mensaje. Tu contraseña actual seguirá siendo válida.</p>"
                + "</div>";

        return getBaseTemplate(ASUNTO_RECUPERACION, "Flores Eternas LP", contenido, null);
    }

    private static String getCajaEstado(String pedidoId, String estado) {
        String estilosCajaCodigo = "background-color:#FFEDE3; border:2px dashed #F0D3C1; border-radius:12px; padding:24px; text-align:center; margin:24px 0;";
        String estilosEtiquetaCodigo = "margin:0 0 8px 0; font-size:12px; text-transform:uppercase; letter-spacing:2px; color:#8C5A3C;";
        String estilosCodigo = "margin:0; font-family:'Courier New', monospace; font-size:30px; font-weight:bold; letter-spacing:4px; color:#7A4E2D;";
        return "<div style=\"" + estilosCajaCodigo + "\">"
                + "<p style=\"" + estilosEtiquetaCodigo + "\">Pedido #" + pedidoId + "</p>"
                + "<p style=\"" + estilosCodigo + "\">" + estado + "</p>"
                + "</div>";
    }

    public static String notificacionEnProceso(Long pedidoId, String frontendUrl) {
        String estilosH2 = "margin:0 0 16px 0; font-family:Georgia, serif; font-size:20px; color:#7A4E2D; font-weight:normal;";
        String estilosParrafo = "margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#5E3A1F;";
        
        String contenido = "<h2 style=\"" + estilosH2 + "\">¡Hemos recibido tu pedido!</h2>"
                + "<p style=\"" + estilosParrafo + "\">Hola,</p>"
                + "<p style=\"" + estilosParrafo + "\">Gracias por elegir a Flores Eternas LP. Te confirmamos que tu pedido ha sido registrado en nuestro sistema.</p>"
                + getCajaEstado(String.valueOf(pedidoId), "EN PROCESO")
                + "<p style=\"" + estilosParrafo + "\">Nuestro equipo revisará los detalles a la brevedad. Te enviaremos una nueva notificación tan pronto como comencemos a preparar tu pedido.</p>"
                + "<p style=\"" + estilosParrafo + "\">Si tienes alguna duda o deseas hacer una modificación, contáctanos a <strong>floreseternaslpcol@gmail.com</strong>.</p>";

        return getBaseTemplate("Pedido #" + pedidoId + " en proceso", "Actualización de Pedido", contenido, frontendUrl);
    }

    public static String notificacionEnPreparacion(Long pedidoId, String frontendUrl) {
        String estilosH2 = "margin:0 0 16px 0; font-family:Georgia, serif; font-size:20px; color:#7A4E2D; font-weight:normal;";
        String estilosParrafo = "margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#5E3A1F;";
        String estilosDestacado = "background-color:#FCEEE3; border-left:4px solid #8C5A3C; padding:16px; border-radius:4px; margin:24px 0;";

        String contenido = "<h2 style=\"" + estilosH2 + "\">En preparación</h2>"
                + "<p style=\"" + estilosParrafo + "\">Hola,</p>"
                + "<p style=\"" + estilosParrafo + "\">Tu pedido ha avanzado a la etapa de preparación.</p>"
                + getCajaEstado(String.valueOf(pedidoId), "PREPARANDO")
                + "<div style=\"" + estilosDestacado + "\">"
                + "<p style=\"margin:0; font-size:15px; line-height:1.5; color:#5E3A1F;\">La tienda ha comenzado a trabajar en tu pedido.</p>"
                + "</div>"
                + "<p style=\"" + estilosParrafo + "\">Te notificaremos una vez que tu pedido esté completamente listo para la entrega.</p>";

        return getBaseTemplate("Tu pedido #" + pedidoId + " está en preparación", "Actualización de Pedido", contenido, frontendUrl);
    }

    public static String notificacionPendienteEntrega(Long pedidoId, BigDecimal pendiente, String frontendUrl, String pagoToken) {
        String estilosH2 = "margin:0 0 16px 0; font-family:Georgia, serif; font-size:20px; color:#7A4E2D; font-weight:normal;";
        String estilosParrafo = "margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#5E3A1F;";
        String botonEstilo = "display:inline-block; padding:14px 28px; background-color:#8C5A3C; color:#FFFFFF; text-decoration:none; border-radius:8px; font-weight:bold; font-size:16px; margin:16px 0;";
        String estilosAviso = "background-color:#FCEEE3; border-left:4px solid #F0D3C1; padding:12px 16px; border-radius:4px; margin:24px 0;";

        StringBuilder contenido = new StringBuilder();
        contenido.append("<h2 style=\"").append(estilosH2).append("\">¡Tu pedido está listo!</h2>")
                 .append("<p style=\"").append(estilosParrafo).append("\">Hola,</p>")
                 .append("<p style=\"").append(estilosParrafo).append("\">Te informamos que tu pedido ya está empacado y listo para ser entregado.</p>")
                 .append(getCajaEstado(String.valueOf(pedidoId), "LISTO"));

        if (pendiente != null && pendiente.compareTo(BigDecimal.ZERO) > 0) {
            contenido.append("<div style=\"").append(estilosAviso).append("\">")
                     .append("<p style=\"margin:0 0 8px 0; font-size:14px; text-transform:uppercase; letter-spacing:1px; color:#8C5A3C;\">Saldo pendiente</p>")
                     .append("<p style=\"margin:0; font-family:Georgia, serif; font-size:24px; font-weight:bold; color:#7A4E2D;\">$")
                     .append(pendiente.setScale(0, RoundingMode.HALF_UP))
                     .append(" COP</p>")
                     .append("<p style=\"margin:8px 0 0 0; font-size:13px; line-height:1.5; color:#5E3A1F;\">Para proceder con el envío o retiro, por favor realiza el pago de tu saldo restante.</p>")
                     .append("</div>");

            if (frontendUrl != null && !frontendUrl.isBlank() && pagoToken != null) {
                String linkPago = frontendUrl + "/pago/personalizado/" + pagoToken;
                contenido.append("<div style=\"text-align:center;\">")
                         .append("<a href=\"").append(linkPago).append("\" style=\"").append(botonEstilo).append("\">Pagar Saldo Restante</a>")
                         .append("</div>");
            } else if (pagoToken != null) {
                contenido.append("<p style=\"").append(estilosParrafo).append("\">Usa este código de pago en nuestra web: <strong>").append(pagoToken).append("</strong></p>");
            }
        }

        contenido.append("<p style=\"").append(estilosParrafo).append("\">Gracias por tu compra.</p>");

        return getBaseTemplate("Pedido #" + pedidoId + " listo para entrega", "Tu pedido está listo", contenido.toString(), frontendUrl);
    }

    public static String notificacionEntregado(Long pedidoId, String frontendUrl) {
        String estilosH2 = "margin:0 0 16px 0; font-family:Georgia, serif; font-size:20px; color:#7A4E2D; font-weight:normal;";
        String estilosParrafo = "margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#5E3A1F;";

        String contenido = "<h2 style=\"" + estilosH2 + "\">Entrega Confirmada</h2>"
                + "<p style=\"" + estilosParrafo + "\">Hola,</p>"
                + "<p style=\"" + estilosParrafo + "\">Te confirmamos que tu pedido ha sido entregado exitosamente.</p>"
                + getCajaEstado(String.valueOf(pedidoId), "ENTREGADO")
                + "<p style=\"" + estilosParrafo + "\">¡Gracias por confiar en nosotros! Esperamos volver a verte pronto en Flores Eternas LP.</p>";

        return getBaseTemplate("¡Pedido #" + pedidoId + " entregado con éxito!", "Entrega Confirmada", contenido, frontendUrl);
    }

    public static String notificacionCancelado(Long pedidoId, String frontendUrl) {
        String estilosH2 = "margin:0 0 16px 0; font-family:Georgia, serif; font-size:20px; color:#BA4A4A; font-weight:normal;";
        String estilosParrafo = "margin:0 0 16px 0; font-size:15px; line-height:1.6; color:#5E3A1F;";
        String estilosAviso = "background-color:#FDEFEF; border-left:4px solid #BA4A4A; padding:16px; border-radius:4px; margin:24px 0;";

        String contenido = "<h2 style=\"" + estilosH2 + "\">Pedido Cancelado</h2>"
                + "<p style=\"" + estilosParrafo + "\">Hola,</p>"
                + "<p style=\"" + estilosParrafo + "\">Te informamos que tu pedido ha sido cancelado.</p>"
                + getCajaEstado(String.valueOf(pedidoId), "CANCELADO")
                + "<div style=\"" + estilosAviso + "\">"
                + "<p style=\"margin:0 0 8px 0; font-size:14px; font-weight:bold; color:#BA4A4A;\">¿Hubo algún inconveniente?</p>"
                + "<p style=\"margin:0; font-size:14px; line-height:1.5; color:#5E3A1F;\">Si esta cancelación fue un error o necesitas ayuda adicional, estamos aquí para escucharte. Puedes contactarnos escribiendo a <strong>floreseternaslpcol@gmail.com</strong>.</p>"
                + "</div>"
                + "<p style=\"" + estilosParrafo + "\">Lamentamos no haber podido concretar esta entrega. Esperamos tener la oportunidad de servirte en el futuro.</p>";

        return getBaseTemplate("Tu pedido #" + pedidoId + " ha sido cancelado", "Actualización de Pedido", contenido, frontendUrl);
    }
}
