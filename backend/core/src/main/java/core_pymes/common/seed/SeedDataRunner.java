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
        createTables();
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
        seedFerreteria();
        seedMiniSuper();
        seedTallerMecanico();
        seedFarmacia();
        seedDefault();
        log.info("Seed data complete");
    }

    private void createTables() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS template_units (
                id UUID PRIMARY KEY,
                industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
                name VARCHAR(100) NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            )""");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_template_units_industry ON template_units(industry_code)");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS template_movement_reasons (
                id UUID PRIMARY KEY,
                industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
                name VARCHAR(100) NOT NULL,
                movement_type VARCHAR(20) NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            )""");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_template_movement_reasons_industry ON template_movement_reasons(industry_code)");
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS template_payment_methods (
                id UUID PRIMARY KEY,
                industry_code VARCHAR(50) NOT NULL REFERENCES industries(code),
                name VARCHAR(100) NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            )""");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_template_payment_methods_industry ON template_payment_methods(industry_code)");
    }

    private void seedIndustries() {
        jdbc.batchUpdate("INSERT INTO industries (code, name) VALUES (?, ?)",
                List.of(
                        new Object[]{"restaurante", "Restaurante"},
                        new Object[]{"bares", "Bares y Cantinas"},
                        new Object[]{"salon_belleza", "Salón de Belleza"},
                        new Object[]{"ferreteria", "Ferretería"},
                        new Object[]{"mini_super", "Mini Super"},
                        new Object[]{"taller_mecanico", "Taller Mecánico"},
                        new Object[]{"farmacia", "Farmacia"},
                        new Object[]{"default", "General"}
                ));
    }

    private void seedRestaurante() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();
        // ponytail: same categories as before

        var bebidas = UUID.randomUUID();
        var comidas = UUID.randomUUID();
        var insumos = UUID.randomUUID();
        cats.add(cat("restaurante", bebidas, "Bebidas", null, 1));
        cats.add(cat("restaurante", comidas, "Comidas", null, 2));
        cats.add(cat("restaurante", insumos, "Insumos Cocina", null, 3));

        var gaseosas = UUID.randomUUID();
        var aguas = UUID.randomUUID();
        var cervezas = UUID.randomUUID();
        var licores = UUID.randomUUID();
        cats.add(cat("restaurante", gaseosas, "Gaseosas", bebidas, 1));
        cats.add(cat("restaurante", aguas, "Aguas", bebidas, 2));
        cats.add(cat("restaurante", cervezas, "Cervezas", bebidas, 3));
        cats.add(cat("restaurante", licores, "Licores", bebidas, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Colas", gaseosas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Sodas", gaseosas, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aguas Saborizadas", gaseosas, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Agua Natural", aguas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Agua Mineral", aguas, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Nacionales", cervezas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Importadas", cervezas, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Artesanales", cervezas, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Whisky", licores, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Ron", licores, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Vodka", licores, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Tequila", licores, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Gin", licores, 5));

        var entradas = UUID.randomUUID();
        var platosFuertes = UUID.randomUUID();
        var guarniciones = UUID.randomUUID();
        var postres = UUID.randomUUID();
        cats.add(cat("restaurante", entradas, "Entradas", comidas, 1));
        cats.add(cat("restaurante", platosFuertes, "Platos Fuertes", comidas, 2));
        cats.add(cat("restaurante", guarniciones, "Guarniciones", comidas, 3));
        cats.add(cat("restaurante", postres, "Postres", comidas, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Frías", entradas, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Calientes", entradas, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Carnes", platosFuertes, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pollo", platosFuertes, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pescados y Mariscos", platosFuertes, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pastas", platosFuertes, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Arroz", guarniciones, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Vegetales", guarniciones, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Papas", guarniciones, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Ensaladas", guarniciones, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Pasteles", postres, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Helados", postres, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Frutas", postres, 3));

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
        cats.add(cat("restaurante", UUID.randomUUID(), "Especias", condimentos, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Salsas", condimentos, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aderezos", condimentos, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aceite Vegetal", aceites, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Aceite de Oliva", aceites, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Mantequilla", aceites, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Leche", lacteos, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Queso", lacteos, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Crema", lacteos, 3));
        cats.add(cat("restaurante", UUID.randomUUID(), "Huevos", lacteos, 4));
        cats.add(cat("restaurante", UUID.randomUUID(), "Verduras", congelados, 1));
        cats.add(cat("restaurante", UUID.randomUUID(), "Carnes", congelados, 2));
        cats.add(cat("restaurante", UUID.randomUUID(), "Mariscos", congelados, 3));
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
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("restaurante", "Kg", "Lb", "Gr", "Litro", "Ml", "Unidad", "Caja", "Bolsa", "Paquete"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("restaurante",
                        "Entrada (Compra a proveedor)", "ENTRADA",
                        "Salida (Venta, Consumo)", "SALIDA",
                        "Ajuste (Conteo físico, Rotura, Robo)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("restaurante", "Yappy", "ACH", "Efectivo", "Crédito"));
    }

    private void seedBares() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

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
        cats.add(cat("bares", UUID.randomUUID(), "Blended", whisky, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Single Malt", whisky, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Bourbon", whisky, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Blanco", ron, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Dorado", ron, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Añejo", ron, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Nacional", vodka, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Importado", vodka, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Premium", vodka, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Blanco", tequila, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Reposado", tequila, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Añejo", tequila, 3));
        cats.add(cat("bares", UUID.randomUUID(), "London Dry", gin, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Premium", gin, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Brandy", brandy, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Cognac", brandy, 2));

        var nacionales = UUID.randomUUID();
        var importadas = UUID.randomUUID();
        var artesanales = UUID.randomUUID();
        cats.add(cat("bares", nacionales, "Nacionales", cervezas, 1));
        cats.add(cat("bares", importadas, "Importadas", cervezas, 2));
        cats.add(cat("bares", artesanales, "Artesanales", cervezas, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Lager", nacionales, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Pilsener", nacionales, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Light", nacionales, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Premium", importadas, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Especiales", importadas, 2));
        cats.add(cat("bares", UUID.randomUUID(), "IPA", artesanales, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Stout", artesanales, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Pale Ale", artesanales, 3));

        var clasicos = UUID.randomUUID();
        var premium = UUID.randomUUID();
        cats.add(cat("bares", clasicos, "Clásicos", cocteleria, 1));
        cats.add(cat("bares", premium, "Premium", cocteleria, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Margarita", clasicos, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Mojito", clasicos, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Old Fashioned", clasicos, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Especiales de la Casa", premium, 1));

        var gaseosas = UUID.randomUUID();
        var aguas = UUID.randomUUID();
        var jugos = UUID.randomUUID();
        cats.add(cat("bares", gaseosas, "Gaseosas", refrescos, 1));
        cats.add(cat("bares", aguas, "Aguas", refrescos, 2));
        cats.add(cat("bares", jugos, "Jugos", refrescos, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Colas", gaseosas, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Sodas", gaseosas, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Agua Tónica", gaseosas, 3));
        cats.add(cat("bares", UUID.randomUUID(), "Agua Natural", aguas, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Agua Mineral", aguas, 2));
        cats.add(cat("bares", UUID.randomUUID(), "Naturales", jugos, 1));
        cats.add(cat("bares", UUID.randomUUID(), "Envasados", jugos, 2));

        var snacks = UUID.randomUUID();
        var preparados = UUID.randomUUID();
        cats.add(cat("bares", snacks, "Snacks", botanas, 1));
        cats.add(cat("bares", preparados, "Preparados", botanas, 2));
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
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("bares", "Botella", "Lata", "Unidad", "Ml", "Litro", "Caja", "Paquete", "Kg"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("bares",
                        "Entrada (Compra a distribuidor)", "ENTRADA",
                        "Salida (Servicio, Consumo personal)", "SALIDA",
                        "Ajuste (Rotura, Conteo físico)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("bares", "Yappy", "ACH", "Efectivo", "Crédito", "Consignación"));
    }

    private void seedSalonBelleza() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

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

        var shampoos = UUID.randomUUID();
        var acondicionadores = UUID.randomUUID();
        var tintes = UUID.randomUUID();
        var tratamientos = UUID.randomUUID();
        cats.add(cat("salon_belleza", shampoos, "Champús", cuidadoCapilar, 1));
        cats.add(cat("salon_belleza", acondicionadores, "Acondicionadores", cuidadoCapilar, 2));
        cats.add(cat("salon_belleza", tintes, "Tintes", cuidadoCapilar, 3));
        cats.add(cat("salon_belleza", tratamientos, "Tratamientos", cuidadoCapilar, 4));
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

        var secadores = UUID.randomUUID();
        var planchas = UUID.randomUUID();
        var tijeras = UUID.randomUUID();
        var cepillos = UUID.randomUUID();
        cats.add(cat("salon_belleza", secadores, "Secadores", herramientas, 1));
        cats.add(cat("salon_belleza", planchas, "Planchas", herramientas, 2));
        cats.add(cat("salon_belleza", tijeras, "Tijeras", herramientas, 3));
        cats.add(cat("salon_belleza", cepillos, "Cepillos", herramientas, 4));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Profesionales", secadores, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "De Viaje", secadores, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Alisadoras", planchas, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Rizadoras", planchas, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Cortar", tijeras, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Para Entresacar", tijeras, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Redondos", cepillos, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Planos", cepillos, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Cerdas Naturales", cepillos, 3));

        var decolorantes = UUID.randomUUID();
        var permanentes = UUID.randomUUID();
        var alisados = UUID.randomUUID();
        cats.add(cat("salon_belleza", decolorantes, "Decolorantes", quimicos, 1));
        cats.add(cat("salon_belleza", permanentes, "Permanentes", quimicos, 2));
        cats.add(cat("salon_belleza", alisados, "Alisados", quimicos, 3));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "En Polvo", decolorantes, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Crema", decolorantes, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Fuerte", permanentes, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Suave", permanentes, 2));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Progresivos", alisados, 1));
        cats.add(cat("salon_belleza", UUID.randomUUID(), "Japoneses", alisados, 2));

        var cremas = UUID.randomUUID();
        var maquillaje = UUID.randomUUID();
        var limpiadores = UUID.randomUUID();
        cats.add(cat("salon_belleza", cremas, "Cremas", cuidadoFacial, 1));
        cats.add(cat("salon_belleza", maquillaje, "Maquillaje", cuidadoFacial, 2));
        cats.add(cat("salon_belleza", limpiadores, "Limpiadores", cuidadoFacial, 3));
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

        var ganchos = UUID.randomUUID();
        var gorros = UUID.randomUUID();
        var guantes = UUID.randomUUID();
        var capas = UUID.randomUUID();
        cats.add(cat("salon_belleza", ganchos, "Ganchos", accesorios, 1));
        cats.add(cat("salon_belleza", gorros, "Gorros", accesorios, 2));
        cats.add(cat("salon_belleza", guantes, "Guantes", accesorios, 3));
        cats.add(cat("salon_belleza", capas, "Capas", accesorios, 4));
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
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("salon_belleza", "Unidad", "Ml", "Litro", "Tubo", "Caja", "Kit", "Botella"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("salon_belleza",
                        "Entrada (Compra a proveedor)", "ENTRADA",
                        "Salida (Uso en servicio, Venta)", "SALIDA",
                        "Ajuste (Expiración, Conteo físico)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("salon_belleza", "Yappy", "ACH", "Efectivo", "Tarjeta"));
    }

    private void seedFerreteria() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        var herramientas = UUID.randomUUID();
        var tornilleria = UUID.randomUUID();
        var construccion = UUID.randomUUID();
        var pintura = UUID.randomUUID();
        var fontaneria = UUID.randomUUID();
        var electricidad = UUID.randomUUID();
        var otros = UUID.randomUUID();
        cats.add(cat("ferreteria", herramientas, "Herramientas", null, 1));
        cats.add(cat("ferreteria", tornilleria, "Tornillería", null, 2));
        cats.add(cat("ferreteria", construccion, "Materiales de Construcción", null, 3));
        cats.add(cat("ferreteria", pintura, "Pintura y Acabados", null, 4));
        cats.add(cat("ferreteria", fontaneria, "Fontanería", null, 5));
        cats.add(cat("ferreteria", electricidad, "Electricidad", null, 6));
        cats.add(cat("ferreteria", otros, "Otros Suministros", null, 7));

        var manuales = UUID.randomUUID();
        var electricas = UUID.randomUUID();
        cats.add(cat("ferreteria", manuales, "Herramientas Manuales", herramientas, 1));
        cats.add(cat("ferreteria", electricas, "Herramientas Eléctricas", herramientas, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Martillos y Mazos", manuales, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Destornilladores", manuales, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Llaves", manuales, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Sierras", manuales, 4));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Otras", manuales, 5));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Taladros", electricas, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Sierras Eléctricas", electricas, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Otros", electricas, 3));

        cats.add(cat("ferreteria", UUID.randomUUID(), "Tornillos", tornilleria, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Tuercas", tornilleria, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Arandelas", tornilleria, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Clavos", tornilleria, 4));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Pernos", tornilleria, 5));

        cats.add(cat("ferreteria", UUID.randomUUID(), "Cemento y Concreto", construccion, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Arena y Grava", construccion, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Varillas de Acero", construccion, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Bloques y Ladrillos", construccion, 4));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Madera", construccion, 5));

        var pinturas = UUID.randomUUID();
        cats.add(cat("ferreteria", pinturas, "Pinturas", pintura, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Brochas y Rodillos", pintura, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Espátulas", pintura, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Thinner y Diluyentes", pintura, 4));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Masilla", pintura, 5));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Interior", pinturas, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Exterior", pinturas, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Especiales", pinturas, 3));

        cats.add(cat("ferreteria", UUID.randomUUID(), "Tuberías", fontaneria, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Codos y Adaptadores", fontaneria, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Llaves de Paso", fontaneria, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Selladores", fontaneria, 4));

        cats.add(cat("ferreteria", UUID.randomUUID(), "Cables", electricidad, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Breakers", electricidad, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Tomas de Corriente", electricidad, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Interruptores", electricidad, 4));

        var seguridad = UUID.randomUUID();
        cats.add(cat("ferreteria", UUID.randomUUID(), "Pegamentos y Adhesivos", otros, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Cintas", otros, 2));
        cats.add(cat("ferreteria", seguridad, "Seguridad", otros, 3));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Guantes", seguridad, 1));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Cascos", seguridad, 2));
        cats.add(cat("ferreteria", UUID.randomUUID(), "Lentes", seguridad, 3));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);
        locs.add(loc("ferreteria", "Depósito", 1));
        locs.add(loc("ferreteria", "Tienda", 2));
        locs.add(loc("ferreteria", "Mostrador", 3));
        locs.add(loc("ferreteria", "Área de Carga", 4));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("ferreteria", "Unidad", "Metro", "Cm", "Kg", "Lb", "Galón", "Caja", "Bolsa", "Paquete"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("ferreteria",
                        "Entrada (Compra a distribuidor)", "ENTRADA",
                        "Salida (Venta a cliente)", "SALIDA",
                        "Ajuste (Conteo físico, Daño)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("ferreteria", "Efectivo", "Tarjeta", "Crédito", "Cheque"));
    }

    private void seedMiniSuper() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        var frescos = UUID.randomUUID();
        var abarrotes = UUID.randomUUID();
        var bebidas = UUID.randomUUID();
        var congelados = UUID.randomUUID();
        var snacks = UUID.randomUUID();
        var higiene = UUID.randomUUID();
        var limpieza = UUID.randomUUID();
        var mascotas = UUID.randomUUID();
        var otros = UUID.randomUUID();
        cats.add(cat("mini_super", frescos, "Alimentos Frescos", null, 1));
        cats.add(cat("mini_super", abarrotes, "Abarrotes", null, 2));
        cats.add(cat("mini_super", bebidas, "Bebidas", null, 3));
        cats.add(cat("mini_super", congelados, "Congelados", null, 4));
        cats.add(cat("mini_super", snacks, "Snacks", null, 5));
        cats.add(cat("mini_super", higiene, "Higiene Personal", null, 6));
        cats.add(cat("mini_super", limpieza, "Limpieza", null, 7));
        cats.add(cat("mini_super", mascotas, "Mascotas", null, 8));
        cats.add(cat("mini_super", otros, "Otros", null, 9));

        var lacteos = UUID.randomUUID();
        var carnesFrias = UUID.randomUUID();
        var frutas = UUID.randomUUID();
        cats.add(cat("mini_super", lacteos, "Lácteos", frescos, 1));
        cats.add(cat("mini_super", carnesFrias, "Carnes Frías", frescos, 2));
        cats.add(cat("mini_super", frutas, "Frutas y Verduras", frescos, 3));
        cats.add(cat("mini_super", UUID.randomUUID(), "Leche", lacteos, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Quesos", lacteos, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Yogur", lacteos, 3));
        cats.add(cat("mini_super", UUID.randomUUID(), "Jamón", carnesFrias, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Salchicha", carnesFrias, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Mortadela", carnesFrias, 3));
        cats.add(cat("mini_super", UUID.randomUUID(), "Frutas", frutas, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Verduras", frutas, 2));

        cats.add(cat("mini_super", UUID.randomUUID(), "Arroz y Granos", abarrotes, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Pasta", abarrotes, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Aceites", abarrotes, 3));
        cats.add(cat("mini_super", UUID.randomUUID(), "Condimentos", abarrotes, 4));
        cats.add(cat("mini_super", UUID.randomUUID(), "Harinas", abarrotes, 5));

        cats.add(cat("mini_super", UUID.randomUUID(), "Refrescos", bebidas, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Agua", bebidas, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Jugos", bebidas, 3));
        cats.add(cat("mini_super", UUID.randomUUID(), "Cervezas", bebidas, 4));
        cats.add(cat("mini_super", UUID.randomUUID(), "Licores", bebidas, 5));

        cats.add(cat("mini_super", UUID.randomUUID(), "Carnes", congelados, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Verduras Congeladas", congelados, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Otros", congelados, 3));

        cats.add(cat("mini_super", UUID.randomUUID(), "Papitas", snacks, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Galletas", snacks, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Dulces", snacks, 3));

        cats.add(cat("mini_super", UUID.randomUUID(), "Jabones", higiene, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Champús", higiene, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Pasta de Dientes", higiene, 3));
        cats.add(cat("mini_super", UUID.randomUUID(), "Desodorantes", higiene, 4));

        cats.add(cat("mini_super", UUID.randomUUID(), "Detergentes", limpieza, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Desinfectantes", limpieza, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Otros", limpieza, 3));

        cats.add(cat("mini_super", UUID.randomUUID(), "Alimento", mascotas, 1));
        cats.add(cat("mini_super", UUID.randomUUID(), "Accesorios", mascotas, 2));
        cats.add(cat("mini_super", UUID.randomUUID(), "Higiene", mascotas, 3));

        cats.add(cat("mini_super", UUID.randomUUID(), "Productos Varios", otros, 1));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);
        locs.add(loc("mini_super", "Depósito", 1));
        locs.add(loc("mini_super", "Estantería", 2));
        locs.add(loc("mini_super", "Nevera", 3));
        locs.add(loc("mini_super", "Congelador", 4));
        locs.add(loc("mini_super", "Mostrador", 5));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("mini_super", "Unidad", "Caja", "Paquete", "Botella", "Lata", "Kg", "Lb"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("mini_super",
                        "Entrada (Compra a distribuidor)", "ENTRADA",
                        "Salida (Venta a cliente)", "SALIDA",
                        "Ajuste (Vencimiento, Rotura, Conteo)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("mini_super", "Efectivo", "Tarjeta", "Cheque"));
    }

    private void seedTallerMecanico() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        var motor = UUID.randomUUID();
        var frenos = UUID.randomUUID();
        var suspension = UUID.randomUUID();
        var electrico = UUID.randomUUID();
        var herramientas = UUID.randomUUID();
        var consumibles = UUID.randomUUID();
        var variadas = UUID.randomUUID();
        cats.add(cat("taller_mecanico", motor, "Piezas de Motor", null, 1));
        cats.add(cat("taller_mecanico", frenos, "Sistema de Frenos", null, 2));
        cats.add(cat("taller_mecanico", suspension, "Suspensión", null, 3));
        cats.add(cat("taller_mecanico", electrico, "Sistema Eléctrico", null, 4));
        cats.add(cat("taller_mecanico", herramientas, "Herramientas", null, 5));
        cats.add(cat("taller_mecanico", consumibles, "Consumibles", null, 6));
        cats.add(cat("taller_mecanico", variadas, "Piezas Variadas", null, 7));

        var filtros = UUID.randomUUID();
        var lubricantes = UUID.randomUUID();
        var liquidos = UUID.randomUUID();
        cats.add(cat("taller_mecanico", filtros, "Filtros", motor, 1));
        cats.add(cat("taller_mecanico", lubricantes, "Lubricantes", motor, 2));
        cats.add(cat("taller_mecanico", liquidos, "Líquidos", motor, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Filtro de Aire", filtros, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Filtro de Aceite", filtros, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Filtro de Gasolina", filtros, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Aceites", lubricantes, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Grasas", lubricantes, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Refrigerantes", lubricantes, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Líquido de Frenos", liquidos, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Líquido de Dirección", liquidos, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Combustible", liquidos, 3));

        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Pastillas de Freno", frenos, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Discos de Freno", frenos, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Cilindros", frenos, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Tuberías", frenos, 4));

        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Amortiguadores", suspension, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Resortes", suspension, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Rótulas", suspension, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Silentblocks", suspension, 4));

        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Baterías", electrico, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Alternadores", electrico, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Motores de Arranque", electrico, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Cables", electrico, 4));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Fusibles", electrico, 5));

        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Llaves", herramientas, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Destornilladores", herramientas, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Extractores", herramientas, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Especializada", herramientas, 4));

        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Paños y Trapos", consumibles, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Guantes de Trabajo", consumibles, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Selladores", consumibles, 3));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Adhesivos", consumibles, 4));

        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Correas y Mangueras", variadas, 1));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Cojinetes", variadas, 2));
        cats.add(cat("taller_mecanico", UUID.randomUUID(), "Sellos", variadas, 3));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);
        locs.add(loc("taller_mecanico", "Depósito", 1));
        locs.add(loc("taller_mecanico", "Área de Trabajo", 2));
        locs.add(loc("taller_mecanico", "Mostrador", 3));
        locs.add(loc("taller_mecanico", "Banco de Trabajo", 4));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("taller_mecanico", "Unidad", "Litro", "Ml", "Caja", "Juego", "Kit", "Kg"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("taller_mecanico",
                        "Entrada (Compra a distribuidor)", "ENTRADA",
                        "Salida (Uso en servicio, Venta)", "SALIDA",
                        "Ajuste (Conteo físico, Daño)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("taller_mecanico", "Efectivo", "Tarjeta", "Crédito", "ACH"));
    }

    private void seedFarmacia() {
        var cats = new ArrayList<Object[]>();
        var locs = new ArrayList<Object[]>();

        var medicamentos = UUID.randomUUID();
        var cuidadoPersonal = UUID.randomUUID();
        var bebes = UUID.randomUUID();
        var auxilios = UUID.randomUUID();
        var suplementos = UUID.randomUUID();
        var equipos = UUID.randomUUID();
        var hogar = UUID.randomUUID();
        cats.add(cat("farmacia", medicamentos, "Medicamentos", null, 1));
        cats.add(cat("farmacia", cuidadoPersonal, "Cuidado Personal", null, 2));
        cats.add(cat("farmacia", bebes, "Productos para Bebés", null, 3));
        cats.add(cat("farmacia", auxilios, "Primeros Auxilios", null, 4));
        cats.add(cat("farmacia", suplementos, "Suplementos y Vitaminas", null, 5));
        cats.add(cat("farmacia", equipos, "Equipos y Dispositivos", null, 6));
        cats.add(cat("farmacia", hogar, "Higiene del Hogar", null, 7));

        cats.add(cat("farmacia", UUID.randomUUID(), "Analgésicos", medicamentos, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Antibióticos", medicamentos, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Antiinflamatorios", medicamentos, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Antigripales", medicamentos, 4));
        cats.add(cat("farmacia", UUID.randomUUID(), "Antidiarreicos", medicamentos, 5));
        cats.add(cat("farmacia", UUID.randomUUID(), "Laxantes", medicamentos, 6));
        cats.add(cat("farmacia", UUID.randomUUID(), "Otros", medicamentos, 7));

        var bucal = UUID.randomUUID();
        var corporal = UUID.randomUUID();
        var piel = UUID.randomUUID();
        cats.add(cat("farmacia", bucal, "Higiene Bucal", cuidadoPersonal, 1));
        cats.add(cat("farmacia", corporal, "Higiene Corporal", cuidadoPersonal, 2));
        cats.add(cat("farmacia", piel, "Cuidado de la Piel", cuidadoPersonal, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Pasta de Dientes", bucal, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Enjuague", bucal, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Cepillos", bucal, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Jabones", corporal, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Champús", corporal, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Desodorantes", corporal, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Cremas", piel, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Lociones", piel, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Protectores Solares", piel, 3));

        cats.add(cat("farmacia", UUID.randomUUID(), "Pañales", bebes, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Toallitas Húmedas", bebes, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Champú y Jabón", bebes, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Cremas para Bebé", bebes, 4));

        cats.add(cat("farmacia", UUID.randomUUID(), "Vendajes", auxilios, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Desinfectantes", auxilios, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Gasas", auxilios, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Apósitos", auxilios, 4));
        cats.add(cat("farmacia", UUID.randomUUID(), "Otros", auxilios, 5));

        cats.add(cat("farmacia", UUID.randomUUID(), "Multivitaminas", suplementos, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Probióticos", suplementos, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Minerales", suplementos, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Otros", suplementos, 4));

        cats.add(cat("farmacia", UUID.randomUUID(), "Termómetros", equipos, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Glucómetros", equipos, 2));
        cats.add(cat("farmacia", UUID.randomUUID(), "Tensiómetros", equipos, 3));
        cats.add(cat("farmacia", UUID.randomUUID(), "Otros", equipos, 4));

        cats.add(cat("farmacia", UUID.randomUUID(), "Desinfectantes", hogar, 1));
        cats.add(cat("farmacia", UUID.randomUUID(), "Repelentes", hogar, 2));

        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);
        locs.add(loc("farmacia", "Almacén", 1));
        locs.add(loc("farmacia", "Mostrador", 2));
        locs.add(loc("farmacia", "Vitrina (productos controlados)", 3));
        locs.add(loc("farmacia", "Refrigeración", 4));
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)", locs);
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("farmacia", "Unidad", "Caja", "Blíster", "Frasco", "Ml", "Gr", "Botella"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("farmacia",
                        "Entrada (Compra a distribuidor)", "ENTRADA",
                        "Salida (Venta a cliente)", "SALIDA",
                        "Ajuste (Vencimiento, Conteo físico)", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("farmacia", "Efectivo", "Tarjeta", "Crédito", "Seguro médico"));
    }

    private void seedDefault() {
        var cats = new ArrayList<Object[]>();
        var defaultCat = UUID.randomUUID();
        cats.add(cat("default", defaultCat, "General", null, 1));
        cats.add(cat("default", UUID.randomUUID(), "Sin subcategoría", defaultCat, 1));
        jdbc.batchUpdate("INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)", cats);
        jdbc.batchUpdate("INSERT INTO template_locations (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                locs("default", "Principal"));
        jdbc.batchUpdate("INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                units("default", "Unidad", "Caja", "Paquete", "Kg", "Litro"));
        jdbc.batchUpdate("INSERT INTO template_movement_reasons (id, industry_code, name, movement_type, sort_order) VALUES (?, ?, ?, ?, ?)",
                reasons("default",
                        "Entrada", "ENTRADA",
                        "Salida", "SALIDA",
                        "Ajuste", "AJUSTE"));
        jdbc.batchUpdate("INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)",
                paymentMethods("default", "Efectivo", "Transferencia"));
    }

    private static Object[] cat(String industry, UUID id, String name, UUID parentId, int sortOrder) {
        return new Object[]{id, industry, name, parentId, sortOrder};
    }

    private static Object[] loc(String industry, String name, int sortOrder) {
        return new Object[]{UUID.randomUUID(), industry, name, sortOrder};
    }

    private static ArrayList<Object[]> locs(String industry, String... names) {
        var list = new ArrayList<Object[]>();
        for (int i = 0; i < names.length; i++)
            list.add(new Object[]{UUID.randomUUID(), industry, names[i], i + 1});
        return list;
    }

    private static ArrayList<Object[]> units(String industry, String... names) {
        var list = new ArrayList<Object[]>();
        for (int i = 0; i < names.length; i++)
            list.add(new Object[]{UUID.randomUUID(), industry, names[i], i + 1});
        return list;
    }

    private static ArrayList<Object[]> reasons(String industry, String... args) {
        var list = new ArrayList<Object[]>();
        for (int i = 0; i < args.length; i += 2)
            list.add(new Object[]{UUID.randomUUID(), industry, args[i], args[i + 1], (i / 2) + 1});
        return list;
    }

    private static ArrayList<Object[]> paymentMethods(String industry, String... names) {
        var list = new ArrayList<Object[]>();
        for (int i = 0; i < names.length; i++)
            list.add(new Object[]{UUID.randomUUID(), industry, names[i], i + 1});
        return list;
    }
}
