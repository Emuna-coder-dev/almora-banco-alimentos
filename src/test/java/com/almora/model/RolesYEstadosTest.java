package com.almora.model;

import com.almora.model.donacion.EstadoDonacion;
import com.almora.model.entrega.EstadoEntrega;
import com.almora.model.usuario.Admin;
import com.almora.model.usuario.Donante;
import com.almora.model.usuario.Rol;
import com.almora.model.usuario.SuperAdmin;
import com.almora.model.usuario.TipoRol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Herencia de la clase Rol y reglas de transicion de estados (HU08, SCAMPER A2). */
class RolesYEstadosTest {

    private final Rol superAdmin = new SuperAdmin();
    private final Rol admin = new Admin();
    private final Rol otroAdmin = new Admin();
    private final Rol donante = new Donante();

    @Test
    void cadaSubclaseFijaSuRol() {
        assertEquals(TipoRol.SUPERADMIN, superAdmin.getRol());
        assertEquals(TipoRol.ADMIN, admin.getRol());
        assertEquals(TipoRol.DONANTE, donante.getRol());
    }

    @Test
    void superAdminHeredaDeAdminYGestionaAdministradores() {
        assertTrue(superAdmin instanceof Admin);
        assertTrue(superAdmin.puedeGestionarAdministradores());
        assertTrue(superAdmin.puedeGestionar(admin));
        assertTrue(superAdmin.puedeGestionar(donante));
        assertFalse(superAdmin.puedeGestionar(new SuperAdmin()));
    }

    @Test
    void adminSoloGestionaDonantes() {
        assertFalse(admin.puedeGestionarAdministradores());
        assertTrue(admin.puedeGestionar(donante));
        assertFalse(admin.puedeGestionar(otroAdmin));
        assertFalse(admin.puedeGestionar(superAdmin));
    }

    @Test
    void donanteNoGestionaANadie() {
        assertFalse(donante.puedeGestionar(new Donante()));
        assertFalse(donante.puedeGestionar(admin));
    }

    @Test
    void transicionesManualesDeDonacion() {
        assertEquals(java.util.Set.of(EstadoDonacion.RECIBIDA), EstadoDonacion.PENDIENTE.transicionesManuales());
        assertEquals(java.util.Set.of(EstadoDonacion.ALMACENADA), EstadoDonacion.RECIBIDA.transicionesManuales());
        assertTrue(EstadoDonacion.ALMACENADA.transicionesManuales().isEmpty());
        assertFalse(EstadoDonacion.PENDIENTE.transicionesManuales().contains(EstadoDonacion.ENTREGADA));
    }

    @Test
    void soloSeAnulaAntesDeSalirDeBodega() {
        assertTrue(EstadoDonacion.PENDIENTE.puedeAnularse());
        assertTrue(EstadoDonacion.ALMACENADA.puedeAnularse());
        assertFalse(EstadoDonacion.ASIGNADA.puedeAnularse());
        assertFalse(EstadoDonacion.ENTREGADA.puedeAnularse());
    }

    @Test
    void certificadoSoloSiYaFueRecibida() {
        assertFalse(EstadoDonacion.PENDIENTE.permiteCertificado());
        assertFalse(EstadoDonacion.ANULADA.permiteCertificado());
        assertTrue(EstadoDonacion.RECIBIDA.permiteCertificado());
        assertTrue(EstadoDonacion.ENTREGADA.permiteCertificado());
    }

    @Test
    void transicionesDeEntrega() {
        assertTrue(EstadoEntrega.PROGRAMADA.puedePasarA(EstadoEntrega.EN_RUTA));
        assertTrue(EstadoEntrega.EN_RUTA.puedePasarA(EstadoEntrega.ENTREGADA));
        assertTrue(EstadoEntrega.EN_RUTA.puedePasarA(EstadoEntrega.CANCELADA));
        assertFalse(EstadoEntrega.ENTREGADA.puedePasarA(EstadoEntrega.CANCELADA));
        assertFalse(EstadoEntrega.CANCELADA.puedePasarA(EstadoEntrega.EN_RUTA));
    }
}
