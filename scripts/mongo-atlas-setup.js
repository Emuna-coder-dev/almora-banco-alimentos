// =====================================================================
//  ALMORA - Preparacion de la base de datos en MongoDB Atlas
//  Ejecutar UNA vez en mongosh (Compass > pestana MONGOSH, o terminal):
//      use almora_db
//      load("scripts/mongo-atlas-setup.js")      // o pegar todo el contenido
//
//  Que hace:
//   - Crea las 9 colecciones del sistema con reglas de validacion ($jsonSchema).
//     Son una segunda barrera: aunque alguien escriba directo en la base de datos,
//     MongoDB rechaza, por ejemplo, un lote con stock negativo o un rol inventado.
//   - NO crea indices: los crea Spring Boot al arrancar (spring.data.mongodb.auto-index-creation=true),
//     con los nombres que espera. Si los creas a mano con otro nombre, la app no arranca.
//  Se puede ejecutar de nuevo sin problema: si la coleccion existe, solo actualiza la validacion.
// =====================================================================

const ENTERO = ["int", "long"];
const NUMERO = ["int", "long", "double", "decimal"];

const colecciones = {

  // Clase Rol y sus subclases (Donante, Admin, SuperAdmin) en una sola coleccion
  usuarios: {
    bsonType: "object",
    required: ["nombre", "correo", "password", "rol", "activo"],
    properties: {
      nombre: { bsonType: "string", minLength: 2, maxLength: 120 },
      correo: { bsonType: "string", pattern: "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$" },
      password: { bsonType: "string", minLength: 50, description: "hash BCrypt, nunca la clave en claro" },
      rol: { enum: ["SUPERADMIN", "ADMIN", "DONANTE"] },
      activo: { bsonType: "bool" },
      intentosFallidos: { bsonType: ENTERO, minimum: 0 },
      tipoDonante: { enum: ["PERSONA_NATURAL", "EMPRESA", "SUPERMERCADO", "RESTAURANTE", "ORGANIZACION", "OTRO"] }
    }
  },

  productos: {
    bsonType: "object",
    required: ["nombre", "nombreNormalizado", "categoria", "unidadMedida", "perecedero", "activo"],
    properties: {
      nombre: { bsonType: "string", minLength: 1, maxLength: 100 },
      categoria: { enum: ["GRANOS_Y_CEREALES", "LACTEOS", "CARNES_Y_PROTEINAS", "FRUTAS_Y_VERDURAS", "PANADERIA",
                          "ENLATADOS_Y_CONSERVAS", "BEBIDAS", "ACEITES_Y_CONDIMENTOS", "ASEO_PERSONAL", "OTRO"] },
      unidadMedida: { enum: ["UNIDAD", "KILOGRAMO", "LIBRA", "LITRO", "PAQUETE", "CAJA"] },
      perecedero: { bsonType: "bool" },
      activo: { bsonType: "bool" }
    }
  },

  // Unidad del algoritmo FEFO: cantidades nunca negativas
  lotes: {
    bsonType: "object",
    required: ["codigo", "productoId", "donacionId", "cantidadInicial", "cantidadDisponible",
               "cantidadAsignada", "cantidadEntregada", "cantidadVencida", "estado"],
    properties: {
      codigo: { bsonType: "string" },
      cantidadInicial: { bsonType: ENTERO, minimum: 1 },
      cantidadDisponible: { bsonType: ENTERO, minimum: 0 },
      cantidadAsignada: { bsonType: ENTERO, minimum: 0 },
      cantidadEntregada: { bsonType: ENTERO, minimum: 0 },
      cantidadVencida: { bsonType: ENTERO, minimum: 0 },
      cantidadAnulada: { bsonType: ENTERO, minimum: 0 },
      fechaVencimiento: { bsonType: ["date", "null"] },
      estado: { enum: ["DISPONIBLE", "AGOTADO", "VENCIDO", "ANULADO"] }
    }
  },

  donaciones: {
    bsonType: "object",
    required: ["codigo", "donanteId", "items", "totalUnidades", "estado", "fechaRegistro"],
    properties: {
      codigo: { bsonType: "string", pattern: "^DON-" },
      items: { bsonType: "array", minItems: 1, maxItems: 50 },
      totalUnidades: { bsonType: ENTERO, minimum: 1 },
      estado: { enum: ["PENDIENTE", "RECIBIDA", "ALMACENADA", "ASIGNADA", "EN_RUTA",
                       "PARCIALMENTE_ENTREGADA", "ENTREGADA", "VENCIDA", "ANULADA"] },
      fechaRegistro: { bsonType: "date" },
      seguimiento: { bsonType: "array" }
    }
  },

  fundaciones: {
    bsonType: "object",
    required: ["nombre", "direccion", "ciudad", "activa"],
    properties: {
      nombre: { bsonType: "string", minLength: 2 },
      activa: { bsonType: "bool" }
    }
  },

  entregas: {
    bsonType: "object",
    required: ["codigo", "fundacionId", "items", "estado", "fechaCreacion"],
    properties: {
      codigo: { bsonType: "string", pattern: "^ENT-" },
      items: { bsonType: "array", minItems: 1 },
      estado: { enum: ["PROGRAMADA", "EN_RUTA", "ENTREGADA", "CANCELADA"] }
    }
  },

  // Bitacora RNF-AU01
  auditoria: {
    bsonType: "object",
    required: ["fecha", "accion"],
    properties: {
      fecha: { bsonType: "date" },
      accion: { bsonType: "string" }
    }
  },

  reportes: {
    bsonType: "object",
    required: ["fechaGeneracion", "desde", "hasta"],
    properties: {
      totalDonaciones: { bsonType: NUMERO, minimum: 0 },
      totalUnidades: { bsonType: NUMERO, minimum: 0 }
    }
  },

  // Solo el hash SHA-256 del token; MongoDB los borra solos al expirar (indice TTL que crea Spring)
  refresh_tokens: {
    bsonType: "object",
    required: ["tokenHash", "usuarioId", "familia", "expiraEn", "revocado"],
    properties: {
      tokenHash: { bsonType: "string", minLength: 64, maxLength: 64 },
      expiraEn: { bsonType: "date" },
      revocado: { bsonType: "bool" }
    }
  }
};

const existentes = db.getCollectionNames();
for (const [nombre, esquema] of Object.entries(colecciones)) {
  const opciones = { validator: { $jsonSchema: esquema }, validationLevel: "strict", validationAction: "error" };
  if (existentes.includes(nombre)) {
    db.runCommand({ collMod: nombre, ...opciones });
    print("Validacion actualizada: " + nombre);
  } else {
    db.createCollection(nombre, opciones);
    print("Coleccion creada:       " + nombre);
  }
}
print("\nListo. Colecciones en " + db.getName() + ": " + db.getCollectionNames().sort().join(", "));
