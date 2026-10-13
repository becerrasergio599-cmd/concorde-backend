package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Persona;
import com.concorde.springboot.repositorio.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @Autowired
    private PersonaRepository repositorio;

    @GetMapping
    public List<Persona> listar() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public Persona obtenerUno(@PathVariable Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada con id: " + id));
    }

    @PostMapping
    public Persona crear(@RequestBody Persona persona) {
        return repositorio.save(persona);
    }

    @PutMapping("/{id}")
    public Persona actualizar(@PathVariable Integer id, @RequestBody Persona datos) {
        Persona persona = repositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada con id: " + id));
        persona.setNumeroDocumento(datos.getNumeroDocumento());
        persona.setNombre(datos.getNombre());
        persona.setDireccion(datos.getDireccion());
        persona.setCorreoElectronico(datos.getCorreoElectronico());
        persona.setTelefono(datos.getTelefono());
        return repositorio.save(persona);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        repositorio.deleteById(id);
    }
}
