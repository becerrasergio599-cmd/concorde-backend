package com.concorde.springboot;

import com.concorde.springboot.repositorio.VehiculoRepository;
import com.concorde.springboot.servicio.GeneradorAsientos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConcordeSpringbootApplication implements CommandLineRunner {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private GeneradorAsientos generadorAsientos;

    public static void main(String[] args) {
        SpringApplication.run(ConcordeSpringbootApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Vehiculos en la base de datos: " + vehiculoRepository.count());
        try {
            int creados = generadorAsientos.generarParaTodos();
            System.out.println("Asientos creados automaticamente: " + creados);
        } catch (Exception e) {
            // Si algo falla (por ejemplo, la tabla no existe) no se tumba el servidor.
            System.out.println("No se pudieron generar los asientos: " + e.getMessage());
        }
    }
}
