<?php
use PHPUnit\Framework\TestCase;

// Simulamos una clase que procesa el Registro
class Registro {
    public function registrar($datos) {
        // Campos obligatorios
        $obligatorios = ['nombreCompleto', 'correoElectronico', 'numeroDocumento', 'contrasena'];
        foreach ($obligatorios as $campo) {
            if (empty(trim($datos[$campo] ?? ''))) {
                return ['ok' => false, 'error' => "Falta el campo $campo"];
            }
        }

        // Correo válido
        if (!filter_var($datos['correoElectronico'], FILTER_VALIDATE_EMAIL)) {
            return ['ok' => false, 'error' => 'Correo inválido'];
        }

        // Teléfono opcional, pero si viene debe tener entre 7 y 10 dígitos
        if (!empty($datos['telefono']) && !preg_match('/^[0-9]{7,10}$/', $datos['telefono'])) {
            return ['ok' => false, 'error' => 'Teléfono inválido'];
        }

        // Política de contraseña
        $pass = $datos['contrasena'];
        if (strlen($pass) < 8
            || !preg_match('/[A-Z]/', $pass)
            || !preg_match('/[a-z]/', $pass)
            || !preg_match('/[0-9]/', $pass)
            || !preg_match('/[^A-Za-z0-9]/', $pass)) {
            return ['ok' => false, 'error' => 'Contraseña no cumple la política'];
        }

        // Confirmación de contraseña
        if (($datos['confirmarPassword'] ?? '') !== $pass) {
            return ['ok' => false, 'error' => 'Las contraseñas no coinciden'];
        }

        // El registro público siempre asigna el rol CLIENTE
        return ['ok' => true, 'rol' => 'CLIENTE'];
    }
}

class RegistroTest extends TestCase {

    private function datosValidos() {
        return [
            'nombreCompleto'    => 'Juan García',
            'correoElectronico' => 'juan@correo.com',
            'telefono'          => '3001234567',
            'numeroDocumento'   => '1234567890',
            'contrasena'        => 'Clave#2026',
            'confirmarPassword' => 'Clave#2026',
        ];
    }

    // Registro exitoso con datos válidos
    public function testRegistroExitoso() {
        $registro = new Registro();
        $resultado = $registro->registrar($this->datosValidos());
        $this->assertTrue($resultado['ok']);
    }

    // El rol siempre debe ser CLIENTE, aunque se intente enviar otro
    public function testRolSiempreCliente() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['rol'] = 'administrador';
        $resultado = $registro->registrar($datos);
        $this->assertSame('CLIENTE', $resultado['rol']);
    }

    // Falla si falta un campo obligatorio
    public function testRegistroSinNombre() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['nombreCompleto'] = '';
        $resultado = $registro->registrar($datos);
        $this->assertFalse($resultado['ok']);
    }

    // Falla con correo mal escrito
    public function testCorreoInvalido() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['correoElectronico'] = 'correo-sin-arroba';
        $resultado = $registro->registrar($datos);
        $this->assertFalse($resultado['ok']);
    }

    // Falla con teléfono de menos de 7 dígitos
    public function testTelefonoInvalido() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['telefono'] = '123';
        $resultado = $registro->registrar($datos);
        $this->assertFalse($resultado['ok']);
    }

    // El teléfono es opcional
    public function testRegistroSinTelefono() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['telefono'] = '';
        $resultado = $registro->registrar($datos);
        $this->assertTrue($resultado['ok']);
    }

    // Falla con contraseña corta
    public function testContrasenaCorta() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['contrasena'] = $datos['confirmarPassword'] = 'Ab#1';
        $resultado = $registro->registrar($datos);
        $this->assertFalse($resultado['ok']);
    }

    // Falla si la contraseña no tiene símbolo
    public function testContrasenaSinSimbolo() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['contrasena'] = $datos['confirmarPassword'] = 'Clave2026';
        $resultado = $registro->registrar($datos);
        $this->assertFalse($resultado['ok']);
    }

    // Falla si las contraseñas no coinciden
    public function testContrasenasNoCoinciden() {
        $registro = new Registro();
        $datos = $this->datosValidos();
        $datos['confirmarPassword'] = 'OtraClave#2026';
        $resultado = $registro->registrar($datos);
        $this->assertFalse($resultado['ok']);
    }
}