package com.concorde.springboot.servicio;

import com.concorde.springboot.modelo.Asiento;
import com.concorde.springboot.modelo.Vehiculo;
import com.concorde.springboot.repositorio.AsientoRepository;
import com.concorde.springboot.repositorio.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

// Crea automáticamente los asientos 1..capacidad de cada vehículo.
// Es idempotente: solo agrega los números que todavía no existen, así que
// se puede llamar las veces que sea necesario sin duplicar nada.
@Service
public class GeneradorAsientos {

    @Autowired
    private AsientoRepository asientoRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    // Todos los vehículos (se ejecuta al arrancar la aplicación).
    public int generarParaTodos() {
        int creados = 0;
        for (Vehiculo v : vehiculoRepository.findAll()) {
            creados += generarParaVehiculo(v);
        }
        return creados;
    }

    // Un solo vehículo (se ejecuta al crearlo o editarlo).
    public int generarParaVehiculo(Vehiculo v) {
        if (v == null || v.getIdVehiculo() == null || v.getCapacidad() == null || v.getCapacidad() <= 0) {
            return 0;
        }
        Set<String> existentes = new HashSet<>();
        for (Asiento a : asientoRepository.findByVehiculo_IdVehiculo(v.getIdVehiculo())) {
            existentes.add(a.getNumeroAsiento());
        }
        int creados = 0;
        for (int n = 1; n <= v.getCapacidad(); n++) {
            String numero = String.valueOf(n);
            if (existentes.contains(numero)) continue;
            Asiento nuevo = new Asiento();
            nuevo.setVehiculo(v);
            nuevo.setNumeroAsiento(numero);
            asientoRepository.save(nuevo);
            creados++;
        }
        return creados;
    }
}
