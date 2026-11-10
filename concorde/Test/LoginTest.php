<?php
use PHPUnit\Framework\TestCase;

// Simulamos una clase que procesa el Login
class Login {
    public function autenticar($usuario, $password) {
        if ($usuario === 'admin' && $password === '1234') {
            return true;
        }
        return false;
    }
}

class LoginTest extends TestCase {
    // Prueba para verificar un login exitoso
    public function testLoginExitoso() {
        $login = new Login();
        $resultado = $login->autenticar('admin', '1234');
        $this->assertTrue($resultado);
    }

    // Prueba para verificar que falle con credenciales incorrectas
    public function testLoginFallido() {
        $login = new Login();
        $resultado = $login->autenticar('usuario_incorrecto', 'clave_falsa');
        $this->assertFalse($resultado);
    }
}
