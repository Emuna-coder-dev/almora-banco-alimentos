package com.almora.dto.comun;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Respuesta paginada (RNF-ES01: los listados nunca devuelven toda la coleccion de golpe). */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <S, T> Pagina<T> de(Page<S> page, Function<S, T> mapper) {
        return new Pagina<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> Pagina<T> de(List<T> todos, int pagina, int tamano) {
        int desde = Math.min(pagina * tamano, todos.size());
        int hasta = Math.min(desde + tamano, todos.size());
        int totalPaginas = tamano == 0 ? 0 : (int) Math.ceil((double) todos.size() / tamano);
        return new Pagina<>(List.copyOf(todos.subList(desde, hasta)), pagina, tamano, todos.size(), totalPaginas);
    }
}
