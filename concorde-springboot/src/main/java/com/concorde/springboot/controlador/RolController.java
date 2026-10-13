package com.concorde.springboot.controlador;

import com.concorde.springboot.modelo.Rol;
import com.concorde.springboot.repositorio.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolRepository repositorio;

    @GetMapping
    public List<Rol> listar() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public Rol obtenerUno(@PathVariable Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
    }

    @PostMapping
    public Rol crear(@RequestBody Rol rol) {
        return repositorio.save(rol);
    }

    @PutMapping("/{id}")
    public Rol actualizar(@PathVariable Integer id, @RequestBody Rol datos) {
        Rol rol = repositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con id: " + id));
        rol.setNombreRol(datos.getNombreRol());
        return repositorio.save(rol);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        repositorio.deleteById(id);
    }
}
