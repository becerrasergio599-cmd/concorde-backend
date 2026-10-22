package com.concorde.springboot;

import com.concorde.springboot.repositorio.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ConcordeSpringbootApplication implements CommandLineRunner {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    public static void main(String[] args) {
        SpringApplication.run(ConcordeSpringbootApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Vehiculos en la base de datos: " + vehiculoRepository.count());
    }
}
