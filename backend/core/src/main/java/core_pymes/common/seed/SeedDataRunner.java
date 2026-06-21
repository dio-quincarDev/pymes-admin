package core_pymes.common.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);
    private final JdbcTemplate jdbc;

    public SeedDataRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var count = jdbc.queryForObject("SELECT COUNT(*) FROM industries", Integer.class);
        if (count != null && count > 0) {
            log.info("Seed data already exists, skipping");
            return;
        }

        log.info("Seeding reference data...");
        seedIndustries();
        seedRestaurante();
        seedBares();
        seedSalonBelleza();
        log.info("Seed data complete");
    }

    private void seedIndustries() {
        jdbc.batchUpdate("INSERT INTO industries (code, name) VALUES (?, ?)",
                List.of(
                        new Object[]{"restaurante", "Restaurante"},
                        new Object[]{"bares", "Bares y Cantinas"},
                        new Object[]{"salon_belleza", "Salón de Belleza"}
                ));
    }

    private void seedRestaurante() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        // N1
        var bebidas = UUID.randomUUID();
        var comidas = UUID.randomUUID();
        var insumos = UUID.randomUUID();
        cats.add(cat("restaurante", bebidas, "Bebidas", null, 1));
        cats.add(cat("restaurante", comidas, "Comidas", null, 2));
        cats.add(cat("restaurante", insumos, "Insumos Cocina", null, 3));

        // N2 bajo Bebidas
        var gaseosas = UUID.randomUUID();
        var aguas = UUID.randomUUID();
        var cervezas = UUID.randomUUID();
        var licores = UUID.randomUUID();
        cats.add(cat("restaurante", gaseosas, "Gaseosas", bebidas, 1));
        cats.add(cat("restaurante", aguas, "Aguas", bebidas, 2));
        cats.add(cat("restaurante", cervezas, "Cervezas", bebidas, 3));
        cats.add(cat("restaurante", licores, "Licores", bebidas, 4));

        // N3 bajo Gaseosas
        cats.add(cat("restaurante", UUID.randomUUID(), "Colas", gaseosas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Sodas", gaseosas, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aguas Saborizadas", gaseosas, 3));

        // N3 bajo Aguas
        cats.add(cat("restaurante", UUID.randomUUID(), "Agua Natural", aguas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Agua Mineral", aguas, 2));

        // N3 bajo Cervezas
        cats.add(cat("restaurante", UUID.randomUUID(), "Nacionales", cervezas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Importadas", cervezas, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Artesanales", cervezas, 3));

        // N3 bajo Licores
        cats.add(cat("restaurante", UUID.randomUUID(), "Whisky", licores, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Ron", licores, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Vodka", licores, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Tequila", licores, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Gin", licores, 5));

        // N2 bajo Comidas
        var entradas = UUID.randomUUID();
        var platosFuertes = UUID.randomUUID();
        var guarniciones = UUID.randomUUID();
        var postres = UUID.randomUUID();
        cats.add(cat("restaurante", entradas, "Entradas", comidas, 1));
        cats.add(cat("restaurante", platosFuertes, "Platos Fuertes", comidas, 2));
        cats.add(cat("restaurante", guarniciones, "Guarniciones", comidas, 3));
        cats.add(cat("restaurante", postres, "Postres", comidas, 4));

        // N3 bajo Entradas
        cats.add(cat("restaurante", UUID.randomUUID(), "Frías", entradas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Calientes", entradas, 2));

        // N3 bajo Platos Fuertes
        cats.add(cat("restaurante", UUID.randomUUID(), "Carnes", platosFuertes, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pollo", platosFuertes, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pescados y Mariscos", platosFuertes, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pastas", platosFuertes, 4));

        // N3 bajo Guarniciones
        cats.add(cat("restaurante", UUID.randomUUID(), "Arroz", guarniciones, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Vegetales", guarniciones, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Papas", guarniciones, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Ensaladas", guarniciones, 4));

        // N3 bajo Postres
        cats.add(cat("restaurante", UUID.randomUUID(), "Pasteles", postres, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Helados", postres, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Frutas", postres, 3));

        // N2 bajo Insumos Cocina
        var condimentos = UUID.randomUUID();
        var aceites = UUID.randomUUID();
        var lacteos = UUID.randomUUID();
        var congelados = UUID.randomUUID();
        var granos = UUID.randomUUID();
        cats.add(cat("restaurante", condimentos, "Condimentos", insumos, 1));
        cats.add(cat("restaurante", aceites, "Aceites y Grasas", insumos, 2));
        cats.add(cat("restaurante", lacteos, "Lácteos y Huevos", insumos, 3));
        cats.add(cat("restaurante", congelados, "Congelados", insumos, 4));
        cats.add(cat("restaurante", granos, "Granos y Harinas", insumos, 5));

        // N3 bajo Condimentos
        cats.add(cat("restaurante", UUID.randomUUID(), "Especias", condimentos, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Salsas", condimentos, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aderezos", condimentos, 3));

        // N3 bajo Aceites
        cats.add(cat("restaurante", UUID.randomUUID(), "Aceite Vegetal", aceites, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aceite de Oliva", aceites, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Mantequilla", aceites, 3));

        // N3 bajo Lácteos
        cats.add(cat("restaurante", UUID.randomUUID(), "Leche", lacteos, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Queso", lacteos, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Crema", lacteos, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Huevos", lacteos, 4));

        // N3 bajo Congelados
        cats.add(cat("restaurante", UUID.randomUUID(), "Verduras", congelados, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Carnes", congelados, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Mariscos", congelados, 3));

        // N3 bajo Granos
        cats.add(cat("restaurante", UUID.randomUUID(), "Arroz", granos, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Harina", granos, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Frijoles", granos, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Azúcar", granos, 4));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);

        locs.add(loc("restaurante", "Cocina Principal", 1));
        locs.add(loc("restaurante", "Bodega de Alimentos", 2));
        locs.add(loc("restaurante", "Cámara Fría", 3));
        locs.add(loc("restaurante", "Barra de Bebidas", 4));
        locs.add(loc("restaurante", "Área de Servicio", 5));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
    }

    private void seedBares() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        // N1
        var licores = UUID.randomUUID();
        var cervezas = UUID.randomUUID();
        var cocteleria = UUID.randomUUID();
        var refrescos = UUID.randomUUID();
        var botanas = UUID.randomUUID();
        cats.add(cat("bares", licores, "Licores", null, 1));
        cats.add(cat("bares", cervezas, "Cervezas", null, 2));
        cats.add(cat("bares", cocteleria, "Coctelería", null, 3));
        cats.add(cat("bares", refrescos, "Refrescos y Mixers", null, 4));
        cats.add(cat("bares", botanas, "Botanas", null, 5));

        // N2 bajo Licores
        var whisky = UUID.randomUUID();
        var ron = UUID.randomUUID();
        var vodka = UUID.randomUUID();
        var tequila = UUID.randomUUID();
        var gin = UUID.randomUUID();
        var brandy = UUID.randomUUID();
        cats.add(cat("bares", whisky, "Whisky", licores, 1));
        cats.add(cat("bares", ron, "Ron", licores, 2));
        cats.add(cat("bares", vodka, "Vodka", licores, 3));
        cats.add(cat("bares", tequila, "Tequila", licores, 4));
        cats.add(cat("bares", gin, "Gin", licores, 5));
        cats.add(cat("bares", brandy, "Brandy y Cognac", licores, 6));

        // N3 bajo Whisky
        cats.add(cat("bares", UUID.randomUUID(), "Blended", whisky, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Single Malt", whisky, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Bourbon", whisky, 3));

        // N3 bajo Ron
        cats.add(cat("bares", UUID.randomUUID(), "Blanco", ron, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Dorado", ron, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Añejo", ron, 3));

        // N3 bajo Vodka
        cats.add(cat("bares", UUID.randomUUID(), "Nacional", vodka, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Importado", vodka, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Premium", vodka, 3));

        // N3 bajo Tequila
        cats.add(cat("bares", UUID.randomUUID(), "Blanco", tequila, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Reposado", tequila, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Añejo", tequila, 3));

        // N3 bajo Gin
        cats.add(cat("bares", UUID.randomUUID(), "London Dry", gin, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Premium", gin, 2));

        // N3 bajo Brandy
        cats.add(cat("bares", UUID.randomUUID(), "Brandy", brandy, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Cognac", brandy, 2));

        // N2 bajo Cervezas
        var nacionales = UUID.randomUUID();
        var importadas = UUID.randomUUID();
        var artesanales = UUID.randomUUID();
        cats.add(cat("bares", nacionales, "Nacionales", cervezas, 1));
        cats.add(cat("bares", importadas, "Importadas", cervezas, 2));
        cats.add(cat("bares", artesanales, "Artesanales", cervezas, 3));

        // N3
        cats.add(cat("bares", UUID.randomUUID(), "Lager", nacionales, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Pilsener", nacionales, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Light", nacionales, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Premium", importadas, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Especiales", importadas, 2));
        cats.add(cat("bares", UUID.randomUUID(), "IPA", artesanales, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Stout", artesanales, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Pale Ale", artesanales, 3));

        // N2 bajo Coctelería
        var clasicos = UUID.randomUUID();
        var premium = UUID.randomUUID();
        cats.add(cat("bares", clasicos, "Clásicos", cocteleria, 1));
        cats.add(cat("bares", premium, "Premium", cocteleria, 2));

        // N3
        cats.add(cat("bares", UUID.randomUUID(), "Margarita", clasicos, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Mojito", clasicos, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Old Fashioned", clasicos, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Especiales de la Casa", premium, 1));

        // N2 bajo Refrescos
        var gaseosas = UUID.randomUUID();
        var aguas = UUID.randomUUID();
        var jugos = UUID.randomUUID();
        cats.add(cat("bares", gaseosas, "Gaseosas", refrescos, 1));
        cats.add(cat("bares", aguas, "Aguas", refrescos, 2));
        cats.add(cat("bares", jugos, "Jugos", refrescos, 3));

        // N3
        cats.add(cat("bares", UUID.randomUUID(), "Colas", gaseosas, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Sodas", gaseosas, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Agua Tónica", gaseosas, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Agua Natural", aguas, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Agua Mineral", aguas, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Naturales", jugos, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Envasados", jugos, 2));

        // N2 bajo Botanas
        var snacks = UUID.randomUUID();
        var preparados = UUID.randomUUID();
        cats.add(cat("bares", snacks, "Snacks", botanas, 1));
        cats.add(cat("bares", preparados, "Preparados", botanas, 2));

        // N3
        cats.add(cat("bares", UUID.randomUUID(), "Papas", snacks, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Frutos Secos", snacks, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Aceitunas", snacks, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Alitas", preparados, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Dedos de Queso", preparados, 2));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);

        locs.add(loc("bares", "Barra Principal", 1));
        locs.add(loc("bares", "Barra Secundaria", 2));
        locs.add(loc("bares", "Bodega de Licores", 3));
        locs.add(loc("bares", "Cava de Vinos", 4));
        locs.add(loc("bares", "Cuarto Frío", 5));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
    }

    private void seedSalonBelleza() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        // N1
        var cuidadoCapilar = UUID.randomUUID();
        var herramientas = UUID.randomUUID();
        var quimicos = UUID.randomUUID();
        var cuidadoFacial = UUID.randomUUID();
        var accesorios = UUID.randomUUID();
        cats.add(cat("salon_belleza", cuidadoCapilar, "Cuidado Capilar", null, 1));
        cats.add(cat("salon_belleza", herramientas, "Herramientas", null, 2));
        cats.add(cat("salon_belleza", quimicos, "Productos Químicos", null, 3));
        cats.add(cat("salon_belleza", cuidadoFacial, "Cuidado Facial", null, 4));
        cats.add(cat("salon_belleza", accesorios, "Accesorios", null, 5));

        // N2 bajo Cuidado Capilar
        var shampoos = UUID.randomUUID();
        var acondicionadores = UUID.randomUUID();
        var tintes = UUID.randomUUID();
        var tratamientos = UUID.randomUUID();
        cats.add(cat("salon_belleza", shampoos, "Champús", cuidadoCapilar, 1));
        cats.add(cat("salon_belleza", acondicionadores, "Acondicionadores", cuidadoCapilar, 2));
        cats.add(cat("salon_belleza", tintes, "Tintes", cuidadoCapilar, 3));
        cats.add(cat("salon_belleza", tratamientos, "Tratamientos", cuidadoCapilar, 4));

        // N3
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Todo Tipo", shampoos, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Cabello Seco", shampoos, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Cabello Graso", shampoos, 3));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Clásico", acondicionadores, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Sin Enjuague", acondicionadores, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Mascarillas", acondicionadores, 3));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Permanentes", tintes, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Semi-permanentes", tintes, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Temporales", tintes, 3));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Keratina", tratamientos, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Botox Capilar", tratamientos, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Ampollas", tratamientos, 3));

        // N2 bajo Herramientas
        var secadores = UUID.randomUUID();
        var planchas = UUID.randomUUID();
        var tijeras = UUID.randomUUID();
        var cepillos = UUID.randomUUID();
        cats.add(cat("salon_belleza", secadores, "Secadores", herramientas, 1));
        cats.add(cat("salon_belleza", planchas, "Planchas", herramientas, 2));
        cats.add(cat("salon_belleza", tijeras, "Tijeras", herramientas, 3));
        cats.add(cat("salon_belleza", cepillos, "Cepillos", herramientas, 4));

        // N3
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Profesionales", secadores, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "De Viaje", secadores, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Alisadoras", planchas, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Rizadoras", planchas, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Cortar", tijeras, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Entresacar", tijeras, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Redondos", cepillos, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Planos", cepillos, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Cerdas Naturales", cepillos, 3));

        // N2 bajo Productos Químicos
        var decolorantes = UUID.randomUUID();
        var permanentes = UUID.randomUUID();
        var alisados = UUID.randomUUID();
        cats.add(cat("salon_belleza", decolorantes, "Decolorantes", quimicos, 1));
        cats.add(cat("salon_belleza", permanentes, "Permanentes", quimicos, 2));
        cats.add(cat("salon_belleza", alisados, "Alisados", quimicos, 3));

        // N3
        cats.add(cat("salon_belleza", UUID.randomUUID(), "En Polvo", decolorantes, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Crema", decolorantes, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Fuerte", permanentes, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Suave", permanentes, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Progresivos", alisados, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Japoneses", alisados, 2));

        // N2 bajo Cuidado Facial
        var cremas = UUID.randomUUID();
        var maquillaje = UUID.randomUUID();
        var limpiadores = UUID.randomUUID();
        cats.add(cat("salon_belleza", cremas, "Cremas", cuidadoFacial, 1));
        cats.add(cat("salon_belleza", maquillaje, "Maquillaje", cuidadoFacial, 2));
        cats.add(cat("salon_belleza", limpiadores, "Limpiadores", cuidadoFacial, 3));

        // N3
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Hidratantes", cremas, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Anti-edad", cremas, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Protector Solar", cremas, 3));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Base", maquillaje, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Labial", maquillaje, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Sombra", maquillaje, 3));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Delineador", maquillaje, 4));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Jabones", limpiadores, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Tónicos", limpiadores, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Exfoliantes", limpiadores, 3));

        // N2 bajo Accesorios
        var ganchos = UUID.randomUUID();
        var gorros = UUID.randomUUID();
        var guantes = UUID.randomUUID();
        var capas = UUID.randomUUID();
        cats.add(cat("salon_belleza", ganchos, "Ganchos", accesorios, 1));
        cats.add(cat("salon_belleza", gorros, "Gorros", accesorios, 2));
        cats.add(cat("salon_belleza", guantes, "Guantes", accesorios, 3));
        cats.add(cat("salon_belleza", capas, "Capas", accesorios, 4));

        // N3
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Metálicos", ganchos, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Plásticos", ganchos, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Tinte", gorros, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Ducha", gorros, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Latex", guantes, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Nitrilo", guantes, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Corte", capas, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Tinte", capas, 2));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);

        locs.add(loc("salon_belleza", "Recepción", 1));
        locs.add(loc("salon_belleza", "Área de Lavado", 2));
        locs.add(loc("salon_belleza", "Estación de Trabajo", 3));
        locs.add(loc("salon_belleza", "Bodega de Productos", 4));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
    }

    private static Object[] cat(String industry, UUID id, String name, UUID parentId, int sortOrder) {
        return new Object[]{id, industry, name, parentId, sortOrder};
    }

    private static Object[] loc(String industry, String name, int sortOrder) {
        return new Object[]{UUID.randomUUID(), industry, name, sortOrder};
    }
}
