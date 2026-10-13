package com.concorde.springboot.servicio;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Motor de respuestas del chatbot de atención al cliente.
 *
 * Es intencionalmente simple (basado en palabras clave, sin IA externa):
 * cada entrada de {@link #FAQ} tiene una lista de palabras/frases y una
 * respuesta. Se cuentan las coincidencias de cada entrada contra el texto
 * del cliente (normalizado: minúsculas y sin tildes) y gana la de mayor
 * puntaje. Si el cliente menciona alguna palabra de "quiero un asesor",
 * o si ninguna entrada obtiene puntaje, la conversación se marca para
 * escalar a un agente humano en lugar de forzar una respuesta del bot.
 */
@Service
public class MotorFaqChatbot {

    public static class Faq {
        final List<String> palabrasClave;
        final String respuesta;

        Faq(List<String> palabrasClave, String respuesta) {
            this.palabrasClave = palabrasClave;
            this.respuesta = respuesta;
        }
    }

    public static class Resultado {
        public final String respuesta;
        public final boolean escalar;

        Resultado(String respuesta, boolean escalar) {
            this.respuesta = respuesta;
            this.escalar = escalar;
        }
    }

    // Si el cliente menciona cualquiera de estas frases, se salta el FAQ
    // y se pasa directo a un agente humano.
    private static final List<String> PALABRAS_ESCALACION = List.of(
            "asesor", "agente", "humano", "persona real", "hablar con alguien",
            "no me sirve", "no entendiste", "no entendio", "no es lo que necesito",
            "queja", "reclamo", "denuncia", "hablar con un representante"
    );

    private static final List<Faq> FAQ = List.of(
            new Faq(List.of("hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "hey", "que tal"),
                    "¡Hola! 👋 Soy el asistente virtual de Concorde. Puedo ayudarte con horarios, equipaje, cambios de reserva, cancelaciones, medios de pago, mascotas y más. ¿En qué te ayudo hoy?"),

            new Faq(List.of("horario", "hora sale", "que hora", "cuando sale", "hora de salida", "frecuencia"),
                    "Nuestros buses operan de lunes a domingo, generalmente de 4:00am a 11:00pm (algunas rutas los sábados de 8:00am a 6:00pm). Los horarios exactos de cada ruta aparecen al buscar tu trayecto en la página principal."),

            new Faq(List.of("equipaje", "maleta", "maletas", "bodega", "cuanto peso"),
                    "En clase económica puedes llevar 1 artículo personal (10kg), 1 equipaje de mano (10kg) y 1 maleta en bodega (23kg). En clase premium los límites son mayores."),

            new Faq(List.of("cambio", "cambiar reserva", "modificar reserva", "cambiar fecha", "cambiar viaje"),
                    "Puedes cambiar tu viaje hasta 2 horas antes de la salida desde 'Mis Reservas'. El cambio tiene un cargo de $5.000."),

            new Faq(List.of("cancelar", "cancelacion", "reembolso", "devolucion", "devolver dinero"),
                    "Puedes cancelar tu reserva desde 'Mis Reservas' antes de la salida del bus. El reembolso se procesa según la política de cancelación vigente en tu tiquete."),

            new Faq(List.of("terminal", "direccion", "ubicacion", "donde queda", "donde salgo", "donde es la salida"),
                    "Salimos desde el terminal de transporte de cada ciudad. Puedes confirmar la dirección exacta de tu terminal de origen en el detalle de tu viaje o reserva."),

            new Faq(List.of("pago", "pagar", "tarjeta", "metodo de pago", "medios de pago", "pse", "efectivo"),
                    "Aceptamos tarjeta de crédito/débito, PSE y pago en efectivo en puntos autorizados. El pago se confirma automáticamente al finalizar tu compra en línea."),

            new Faq(List.of("mascota", "perro", "gato", "animal", "mi perrito"),
                    "Sí, aceptamos mascotas pequeñas en transportador (hasta 8kg) y mascotas más grandes en bodega, según el destino. Te recomendamos confirmar los requisitos específicos de tu ruta."),

            new Faq(List.of("reservar", "comprar tiquete", "como compro", "como reservo", "como saco el tiquete"),
                    "Para reservar: busca tu trayecto desde el inicio, elige el viaje que prefieras, selecciona tus asientos y completa el pago. ¡En unos minutos tienes tu tiquete listo!"),

            new Faq(List.of("documento", "cedula", "identificacion", "que debo llevar"),
                    "Debes presentar tu documento de identidad original al abordar. El nombre debe coincidir exactamente con el registrado en la reserva."),

            new Faq(List.of("precio", "tarifa", "costo", "cuanto cuesta", "cuanto vale"),
                    "El precio varía según la ruta, la fecha y la disponibilidad del viaje. Puedes ver el valor exacto al buscar tu trayecto en la página principal."),

            new Faq(List.of("niño", "nino", "menor de edad", "infante"),
                    "Los menores de edad deben viajar acompañados de un adulto responsable y presentar su documento de identidad o registro civil."),

            new Faq(List.of("llegar antes", "con cuanto tiempo", "anticipacion"),
                    "Te recomendamos llegar al terminal 30 minutos antes de la salida. Los buses parten puntualmente en el horario indicado en tu tiquete."),

            new Faq(List.of("gracias", "muchas gracias", "listo eso era todo"),
                    "¡Con gusto! Si necesitas algo más, aquí estoy. 😊")
    );

    /** Normaliza a minúsculas y sin tildes para comparar de forma más robusta. */
    private String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes;
    }

    public Resultado responder(String textoCliente) {
        String normalizado = normalizar(textoCliente);

        for (String palabra : PALABRAS_ESCALACION) {
            if (normalizado.contains(palabra)) {
                return new Resultado(
                        "Entendido, te voy a comunicar con un asesor humano para que te ayude mejor con esto. Un momento, por favor.",
                        true
                );
            }
        }

        Faq mejor = null;
        int mejorPuntaje = 0;
        for (Faq faq : FAQ) {
            int puntaje = 0;
            for (String palabra : faq.palabrasClave) {
                if (normalizado.contains(palabra)) puntaje++;
            }
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = faq;
            }
        }

        if (mejor != null) {
            return new Resultado(mejor.respuesta, false);
        }

        // No hubo coincidencias: no forzamos una respuesta genérica inútil,
        // mejor se traslada a un asesor humano.
        return new Resultado(
                "No tengo una respuesta clara para eso todavía. Te voy a comunicar con un asesor humano para que te ayude. Un momento, por favor.",
                true
        );
    }
}
