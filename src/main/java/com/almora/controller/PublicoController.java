package com.almora.controller;

import com.almora.service.CertificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Rutas publicas (sin token). Solo exponen lo minimo para verificar un certificado. */
@Tag(name = "0. Publico")
@RestController
@RequestMapping("/api/publico")
public class PublicoController {

    private final CertificacionService certificacionService;

    public PublicoController(CertificacionService certificacionService) {
        this.certificacionService = certificacionService;
    }

    @Operation(summary = "Verificar la autenticidad de un certificado de donacion")
    @GetMapping("/certificados/{codigo}")
    public Map<String, Object> verificar(@PathVariable String codigo) {
        return certificacionService.verificar(codigo);
    }
}
