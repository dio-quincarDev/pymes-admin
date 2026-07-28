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

    // ponytail: industry code constants — used ~550× across seed methods
    private static final String RESTAURANTE = "restaurante";
    private static final String BARES = "bares";
    private static final String SALON_BELLEZA = "salon_belleza";
    private static final String FERRETERIA = "ferreteria";
    private static final String MINI_SUPER = "mini_super";
    private static final String TALLER_MECANICO = "taller_mecanico";
    private static final String FARMACIA = "farmacia";
    private static final String DEFAULT = "default";

    // ponytail: SQL INSERT strings — repeated 8× each
    private static final String SQL_INSERT_CATEGORIES = "INSERT INTO template_categories (id, industry_code, name, parent_id, sort_order) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_UNITS = "INSERT INTO template_units (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)";
    private static final String SQL_INSERT_PRODUCTS = "INSERT INTO template_products (id, industry_code, category_id, name, base_unit, min_quantity, max_quantity, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_PRESENTATIONS = "INSERT INTO template_product_presentations (id, template_product_id, name, conversion, sort_order) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_PAYMENTS = "INSERT INTO template_payment_methods (id, industry_code, name, sort_order) VALUES (?, ?, ?, ?)";

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
        seedFerreteria();
        seedMiniSuper();
        seedTallerMecanico();
        seedFarmacia();
        seedDefault();
        log.info("Seed data complete");
    }

    private void seedIndustries() {
        jdbc.batchUpdate("INSERT INTO industries (code, name) VALUES (?, ?)",
                List.of(
                        new Object[]{RESTAURANTE, "Restaurante"},
                        new Object[]{BARES, "Bares y Cantinas"},
                        new Object[]{SALON_BELLEZA, "Salón de Belleza"},
                        new Object[]{FERRETERIA, "Ferretería"},
                        new Object[]{MINI_SUPER, "Mini Super"},
                        new Object[]{TALLER_MECANICO, "Taller Mecánico"},
                        new Object[]{FARMACIA, "Farmacia"},
                        new Object[]{DEFAULT, "General"}
                ));
    }

    private void seedRestaurante() {
        var cats = new ArrayList<Object[]>();
        // ponytail: same categories as before

        var bebidas = UUID.randomUUID();
        var comidas = UUID.randomUUID();
        var insumos = UUID.randomUUID();
        cats.add(cat(RESTAURANTE, bebidas, "Bebidas", null, 1));
        cats.add(cat(RESTAURANTE, comidas, "Comidas", null, 2));
        cats.add(cat(RESTAURANTE, insumos, "Insumos Cocina", null, 3));

        var gaseosas = UUID.randomUUID();
        var aguas = UUID.randomUUID();
        var cervezas = UUID.randomUUID();
        var licores = UUID.randomUUID();
        cats.add(cat(RESTAURANTE, gaseosas, "Gaseosas", bebidas, 1));
        cats.add(cat(RESTAURANTE, aguas, "Aguas", bebidas, 2));
        cats.add(cat(RESTAURANTE, cervezas, "Cervezas", bebidas, 3));
        cats.add(cat(RESTAURANTE, licores, "Licores", bebidas, 4));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Colas", gaseosas, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Sodas", gaseosas, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Aguas Saborizadas", gaseosas, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Agua Natural", aguas, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Agua Mineral", aguas, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Nacionales", cervezas, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Importadas", cervezas, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Artesanales", cervezas, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Whisky", licores, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Ron", licores, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Vodka", licores, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Tequila", licores, 4));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Gin", licores, 5));

        var entradas = UUID.randomUUID();
        var platosFuertes = UUID.randomUUID();
        var guarniciones = UUID.randomUUID();
        var postres = UUID.randomUUID();
        cats.add(cat(RESTAURANTE, entradas, "Entradas", comidas, 1));
        cats.add(cat(RESTAURANTE, platosFuertes, "Platos Fuertes", comidas, 2));
        cats.add(cat(RESTAURANTE, guarniciones, "Guarniciones", comidas, 3));
        cats.add(cat(RESTAURANTE, postres, "Postres", comidas, 4));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Frías", entradas, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Calientes", entradas, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Carnes", platosFuertes, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Pollo", platosFuertes, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Pescados y Mariscos", platosFuertes, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Pastas", platosFuertes, 4));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Arroz", guarniciones, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Vegetales", guarniciones, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Papas", guarniciones, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Ensaladas", guarniciones, 4));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Pasteles", postres, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Helados", postres, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Frutas", postres, 3));

        var condimentos = UUID.randomUUID();
        var aceites = UUID.randomUUID();
        var lacteos = UUID.randomUUID();
        var congelados = UUID.randomUUID();
        var granos = UUID.randomUUID();
        cats.add(cat(RESTAURANTE, condimentos, "Condimentos", insumos, 1));
        cats.add(cat(RESTAURANTE, aceites, "Aceites y Grasas", insumos, 2));
        cats.add(cat(RESTAURANTE, lacteos, "Lácteos y Huevos", insumos, 3));
        cats.add(cat(RESTAURANTE, congelados, "Congelados", insumos, 4));
        cats.add(cat(RESTAURANTE, granos, "Granos y Harinas", insumos, 5));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Especias", condimentos, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Salsas", condimentos, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Aderezos", condimentos, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Aceite Vegetal", aceites, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Aceite de Oliva", aceites, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Mantequilla", aceites, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Leche", lacteos, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Queso", lacteos, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Crema", lacteos, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Huevos", lacteos, 4));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Verduras", congelados, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Carnes", congelados, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Mariscos", congelados, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Arroz", granos, 1));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Harina", granos, 2));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Frijoles", granos, 3));
        cats.add(cat(RESTAURANTE, UUID.randomUUID(), "Azúcar", granos, 4));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(RESTAURANTE, "Kg", "Lb", "Gr", "Litro", "Ml", "Unidad", "Caja", "Bolsa", "Paquete"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(RESTAURANTE, "Yappy", "ACH", "Efectivo", "Crédito"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, RESTAURANTE, gaseosas, "Refresco Cola", "Botella", 1, pres("Botella 2L", 1), pres("Lata 355ml", 1));
        addProd(prods, ppts, RESTAURANTE, gaseosas, "Refresco Naranja", "Botella", 2, pres("Botella 2L", 1), pres("Lata 355ml", 1));
        addProd(prods, ppts, RESTAURANTE, gaseosas, "Refresco Lima-Limón", "Botella", 3, pres("Botella 2L", 1), pres("Lata 355ml", 1));
        addProd(prods, ppts, RESTAURANTE, aguas, "Agua Natural", "Botella", 4, pres("Botella 500ml", 1), pres("Botella 1L", 1));
        addProd(prods, ppts, RESTAURANTE, aguas, "Agua Mineral", "Botella", 5, pres("Botella 500ml", 1));
        addProd(prods, ppts, RESTAURANTE, cervezas, "Cerveza Nacional", "Unidad", 6, pres("Unidad", 1), pres("Caja x12", 12));
        addProd(prods, ppts, RESTAURANTE, cervezas, "Cerveza Importada", "Unidad", 7, pres("Unidad", 1), pres("Caja x12", 12));
        addProd(prods, ppts, RESTAURANTE, cervezas, "Cerveza Artesanal", "Unidad", 8, pres("Unidad", 1), pres("Caja x6", 6));
        addProd(prods, ppts, RESTAURANTE, licores, "Whisky", "Botella", 9, pres("Botella 750ml", 1));
        addProd(prods, ppts, RESTAURANTE, licores, "Ron", "Botella", 10, pres("Botella 750ml", 1));
        addProd(prods, ppts, RESTAURANTE, licores, "Vodka", "Botella", 11, pres("Botella 750ml", 1));
        addProd(prods, ppts, RESTAURANTE, granos, "Arroz", "Kg", 12, pres("Bolsa 1kg", 1), pres("Bolsa 5kg", 5));
        addProd(prods, ppts, RESTAURANTE, granos, "Frijoles", "Kg", 13, pres("Bolsa 1kg", 1), pres("Bolsa 2kg", 2));
        addProd(prods, ppts, RESTAURANTE, granos, "Azúcar", "Kg", 14, pres("Bolsa 1kg", 1), pres("Bolsa 5kg", 5));
        addProd(prods, ppts, RESTAURANTE, granos, "Harina de Trigo", "Kg", 15, pres("Bolsa 1kg", 1));
        addProd(prods, ppts, RESTAURANTE, aceites, "Aceite Vegetal", "Litro", 16, pres("Botella 1L", 1), pres("Galón 3.78L", 3));
        addProd(prods, ppts, RESTAURANTE, aceites, "Aceite de Oliva", "Litro", 17, pres("Botella 500ml", 1));
        addProd(prods, ppts, RESTAURANTE, lacteos, "Leche Entera", "Litro", 18, pres("Bolsa 1L", 1));
        addProd(prods, ppts, RESTAURANTE, lacteos, "Huevos", "Unidad", 19, pres("Unidad", 1), pres("Caja x30", 30));
        addProd(prods, ppts, RESTAURANTE, congelados, "Verduras Mixtas Congeladas", "Kg", 20, pres("Bolsa 1kg", 1));
        addProd(prods, ppts, RESTAURANTE, congelados, "Papas Fritas Congeladas", "Kg", 21, pres("Bolsa 1kg", 1), pres("Bolsa 2.5kg", 2));
        addProd(prods, ppts, RESTAURANTE, condimentos, "Sal", "Kg", 22, pres("Bolsa 1kg", 1));
        addProd(prods, ppts, RESTAURANTE, condimentos, "Salsa de Tomate", "Litro", 23, pres("Botella 500ml", 1));
        addProd(prods, ppts, RESTAURANTE, condimentos, "Mostaza", "Litro", 24, pres("Botella 500ml", 1));
        addProd(prods, ppts, RESTAURANTE, condimentos, "Mayonesa", "Litro", 25, pres("Botella 500ml", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedBares() {
        var cats = new ArrayList<Object[]>();

        var licores = UUID.randomUUID();
        var cervezas = UUID.randomUUID();
        var cocteleria = UUID.randomUUID();
        var refrescos = UUID.randomUUID();
        var botanas = UUID.randomUUID();
        cats.add(cat(BARES, licores, "Licores", null, 1));
        cats.add(cat(BARES, cervezas, "Cervezas", null, 2));
        cats.add(cat(BARES, cocteleria, "Coctelería", null, 3));
        cats.add(cat(BARES, refrescos, "Refrescos y Mixers", null, 4));
        cats.add(cat(BARES, botanas, "Botanas", null, 5));

        var whisky = UUID.randomUUID();
        var ron = UUID.randomUUID();
        var vodka = UUID.randomUUID();
        var tequila = UUID.randomUUID();
        var gin = UUID.randomUUID();
        var brandy = UUID.randomUUID();
        cats.add(cat(BARES, whisky, "Whisky", licores, 1));
        cats.add(cat(BARES, ron, "Ron", licores, 2));
        cats.add(cat(BARES, vodka, "Vodka", licores, 3));
        cats.add(cat(BARES, tequila, "Tequila", licores, 4));
        cats.add(cat(BARES, gin, "Gin", licores, 5));
        cats.add(cat(BARES, brandy, "Brandy y Cognac", licores, 6));
        cats.add(cat(BARES, UUID.randomUUID(), "Blended", whisky, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Single Malt", whisky, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Bourbon", whisky, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Blanco", ron, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Dorado", ron, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Añejo", ron, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Nacional", vodka, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Importado", vodka, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Premium", vodka, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Blanco", tequila, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Reposado", tequila, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Añejo", tequila, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "London Dry", gin, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Premium", gin, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Brandy", brandy, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Cognac", brandy, 2));

        var nacionales = UUID.randomUUID();
        var importadas = UUID.randomUUID();
        var artesanales = UUID.randomUUID();
        cats.add(cat(BARES, nacionales, "Nacionales", cervezas, 1));
        cats.add(cat(BARES, importadas, "Importadas", cervezas, 2));
        cats.add(cat(BARES, artesanales, "Artesanales", cervezas, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Lager", nacionales, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Pilsener", nacionales, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Light", nacionales, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Premium", importadas, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Especiales", importadas, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "IPA", artesanales, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Stout", artesanales, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Pale Ale", artesanales, 3));

        var clasicos = UUID.randomUUID();
        var premium = UUID.randomUUID();
        cats.add(cat(BARES, clasicos, "Clásicos", cocteleria, 1));
        cats.add(cat(BARES, premium, "Premium", cocteleria, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Margarita", clasicos, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Mojito", clasicos, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Old Fashioned", clasicos, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Especiales de la Casa", premium, 1));

        var gaseosas = UUID.randomUUID();
        var aguas = UUID.randomUUID();
        var jugos = UUID.randomUUID();
        cats.add(cat(BARES, gaseosas, "Gaseosas", refrescos, 1));
        cats.add(cat(BARES, aguas, "Aguas", refrescos, 2));
        cats.add(cat(BARES, jugos, "Jugos", refrescos, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Colas", gaseosas, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Sodas", gaseosas, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Agua Tónica", gaseosas, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Agua Natural", aguas, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Agua Mineral", aguas, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Naturales", jugos, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Envasados", jugos, 2));

        var snacks = UUID.randomUUID();
        var preparados = UUID.randomUUID();
        cats.add(cat(BARES, snacks, "Snacks", botanas, 1));
        cats.add(cat(BARES, preparados, "Preparados", botanas, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Papas", snacks, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Frutos Secos", snacks, 2));
        cats.add(cat(BARES, UUID.randomUUID(), "Aceitunas", snacks, 3));
        cats.add(cat(BARES, UUID.randomUUID(), "Alitas", preparados, 1));
        cats.add(cat(BARES, UUID.randomUUID(), "Dedos de Queso", preparados, 2));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(BARES, "Botella", "Lata", "Unidad", "Ml", "Litro", "Caja", "Paquete", "Kg"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(BARES, "Yappy", "ACH", "Efectivo", "Crédito", "Consignación"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, BARES, whisky, "Whisky Blended", "Botella", 1, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, whisky, "Whisky Single Malt", "Botella", 2, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, ron, "Ron Blanco", "Botella", 3, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, ron, "Ron Dorado", "Botella", 4, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, vodka, "Vodka Nacional", "Botella", 5, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, vodka, "Vodka Importado", "Botella", 6, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, tequila, "Tequila Blanco", "Botella", 7, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, tequila, "Tequila Reposado", "Botella", 8, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, gin, "Gin London Dry", "Botella", 9, pres("Botella 750ml", 1));
        addProd(prods, ppts, BARES, nacionales, "Cerveza Nacional", "Unidad", 10, pres("Unidad", 1), pres("Caja x12", 12));
        addProd(prods, ppts, BARES, importadas, "Cerveza Importada", "Unidad", 11, pres("Unidad", 1), pres("Caja x12", 12));
        addProd(prods, ppts, BARES, artesanales, "Cerveza Artesanal IPA", "Unidad", 12, pres("Unidad", 1), pres("Caja x6", 6));
        addProd(prods, ppts, BARES, gaseosas, "Refresco Cola", "Botella", 13, pres("Botella 2L", 1), pres("Lata 355ml", 1));
        addProd(prods, ppts, BARES, gaseosas, "Agua Tónica", "Botella", 14, pres("Botella 1L", 1));
        addProd(prods, ppts, BARES, aguas, "Agua Natural", "Botella", 15, pres("Botella 500ml", 1));
        addProd(prods, ppts, BARES, jugos, "Jugo de Naranja Natural", "Litro", 16, pres("Litro", 1));
        addProd(prods, ppts, BARES, snacks, "Papas Fritas", "Bolsa", 17, pres("Bolsa 150g", 1));
        addProd(prods, ppts, BARES, snacks, "Frutos Secos", "Kg", 18, pres("Bolsa 200g", 1));
        addProd(prods, ppts, BARES, snacks, "Aceitunas", "Unidad", 19, pres("Frasco 500g", 1));
        addProd(prods, ppts, BARES, preparados, "Alitas de Pollo", "Unidad", 20, pres("Porción 6pz", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedSalonBelleza() {
        var cats = new ArrayList<Object[]>();

        var cuidadoCapilar = UUID.randomUUID();
        var herramientas = UUID.randomUUID();
        var quimicos = UUID.randomUUID();
        var cuidadoFacial = UUID.randomUUID();
        var accesorios = UUID.randomUUID();
        cats.add(cat(SALON_BELLEZA, cuidadoCapilar, "Cuidado Capilar", null, 1));
        cats.add(cat(SALON_BELLEZA, herramientas, "Herramientas", null, 2));
        cats.add(cat(SALON_BELLEZA, quimicos, "Productos Químicos", null, 3));
        cats.add(cat(SALON_BELLEZA, cuidadoFacial, "Cuidado Facial", null, 4));
        cats.add(cat(SALON_BELLEZA, accesorios, "Accesorios", null, 5));

        var shampoos = UUID.randomUUID();
        var acondicionadores = UUID.randomUUID();
        var tintes = UUID.randomUUID();
        var tratamientos = UUID.randomUUID();
        cats.add(cat(SALON_BELLEZA, shampoos, "Champús", cuidadoCapilar, 1));
        cats.add(cat(SALON_BELLEZA, acondicionadores, "Acondicionadores", cuidadoCapilar, 2));
        cats.add(cat(SALON_BELLEZA, tintes, "Tintes", cuidadoCapilar, 3));
        cats.add(cat(SALON_BELLEZA, tratamientos, "Tratamientos", cuidadoCapilar, 4));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Todo Tipo", shampoos, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Cabello Seco", shampoos, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Cabello Graso", shampoos, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Clásico", acondicionadores, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Sin Enjuague", acondicionadores, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Mascarillas", acondicionadores, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Permanentes", tintes, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Semi-permanentes", tintes, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Temporales", tintes, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Keratina", tratamientos, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Botox Capilar", tratamientos, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Ampollas", tratamientos, 3));

        var secadores = UUID.randomUUID();
        var planchas = UUID.randomUUID();
        var tijeras = UUID.randomUUID();
        var cepillos = UUID.randomUUID();
        cats.add(cat(SALON_BELLEZA, secadores, "Secadores", herramientas, 1));
        cats.add(cat(SALON_BELLEZA, planchas, "Planchas", herramientas, 2));
        cats.add(cat(SALON_BELLEZA, tijeras, "Tijeras", herramientas, 3));
        cats.add(cat(SALON_BELLEZA, cepillos, "Cepillos", herramientas, 4));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Profesionales", secadores, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "De Viaje", secadores, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Alisadoras", planchas, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Rizadoras", planchas, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Cortar", tijeras, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Entresacar", tijeras, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Redondos", cepillos, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Planos", cepillos, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Cerdas Naturales", cepillos, 3));

        var decolorantes = UUID.randomUUID();
        var permanentes = UUID.randomUUID();
        var alisados = UUID.randomUUID();
        cats.add(cat(SALON_BELLEZA, decolorantes, "Decolorantes", quimicos, 1));
        cats.add(cat(SALON_BELLEZA, permanentes, "Permanentes", quimicos, 2));
        cats.add(cat(SALON_BELLEZA, alisados, "Alisados", quimicos, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "En Polvo", decolorantes, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Crema", decolorantes, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Fuerte", permanentes, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Suave", permanentes, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Progresivos", alisados, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Japoneses", alisados, 2));

        var cremas = UUID.randomUUID();
        var maquillaje = UUID.randomUUID();
        var limpiadores = UUID.randomUUID();
        cats.add(cat(SALON_BELLEZA, cremas, "Cremas", cuidadoFacial, 1));
        cats.add(cat(SALON_BELLEZA, maquillaje, "Maquillaje", cuidadoFacial, 2));
        cats.add(cat(SALON_BELLEZA, limpiadores, "Limpiadores", cuidadoFacial, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Hidratantes", cremas, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Anti-edad", cremas, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Protector Solar", cremas, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Base", maquillaje, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Labial", maquillaje, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Sombra", maquillaje, 3));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Delineador", maquillaje, 4));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Jabones", limpiadores, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Tónicos", limpiadores, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Exfoliantes", limpiadores, 3));

        var ganchos = UUID.randomUUID();
        var gorros = UUID.randomUUID();
        var guantes = UUID.randomUUID();
        var capas = UUID.randomUUID();
        cats.add(cat(SALON_BELLEZA, ganchos, "Ganchos", accesorios, 1));
        cats.add(cat(SALON_BELLEZA, gorros, "Gorros", accesorios, 2));
        cats.add(cat(SALON_BELLEZA, guantes, "Guantes", accesorios, 3));
        cats.add(cat(SALON_BELLEZA, capas, "Capas", accesorios, 4));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Metálicos", ganchos, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Plásticos", ganchos, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Tinte", gorros, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Ducha", gorros, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Latex", guantes, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Nitrilo", guantes, 2));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Corte", capas, 1));
        cats.add(cat(SALON_BELLEZA, UUID.randomUUID(), "Para Tinte", capas, 2));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(SALON_BELLEZA, "Unidad", "Ml", "Litro", "Tubo", "Caja", "Kit", "Botella"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(SALON_BELLEZA, "Yappy", "ACH", "Efectivo", "Tarjeta"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, SALON_BELLEZA, shampoos, "Champú para Todo Tipo", "Litro", 1, pres("Botella 500ml", 1), pres("Botella 1L", 1));
        addProd(prods, ppts, SALON_BELLEZA, shampoos, "Champú para Cabello Seco", "Litro", 2, pres("Botella 500ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, acondicionadores, "Acondicionador Clásico", "Litro", 3, pres("Botella 500ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, acondicionadores, "Mascarilla Capilar", "Litro", 4, pres("Tubo 250ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, tintes, "Tinte Permanente", "Unidad", 5, pres("Caja", 1));
        addProd(prods, ppts, SALON_BELLEZA, tintes, "Tinte Semi-permanente", "Unidad", 6, pres("Caja", 1));
        addProd(prods, ppts, SALON_BELLEZA, tratamientos, "Keratina", "Litro", 7, pres("Botella 500ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, tratamientos, "Botox Capilar", "Litro", 8, pres("Botella 500ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, secadores, "Secador Profesional", "Unidad", 9, pres("Unidad", 1));
        addProd(prods, ppts, SALON_BELLEZA, planchas, "Plancha Alisadora", "Unidad", 10, pres("Unidad", 1));
        addProd(prods, ppts, SALON_BELLEZA, tijeras, "Tijera para Cortar", "Unidad", 11, pres("Unidad", 1));
        addProd(prods, ppts, SALON_BELLEZA, cepillos, "Cepillo Redondo", "Unidad", 12, pres("Unidad", 1));
        addProd(prods, ppts, SALON_BELLEZA, decolorantes, "Decolorante en Polvo", "Kg", 13, pres("Bolsa 500g", 1));
        addProd(prods, ppts, SALON_BELLEZA, alisados, "Alisado Progresivo", "Litro", 14, pres("Botella 500ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, cremas, "Crema Hidratante Facial", "Litro", 15, pres("Tubo 200ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, maquillaje, "Base de Maquillaje", "Unidad", 16, pres("Tubo", 1));
        addProd(prods, ppts, SALON_BELLEZA, maquillaje, "Labial", "Unidad", 17, pres("Unidad", 1));
        addProd(prods, ppts, SALON_BELLEZA, limpiadores, "Jabón Líquido Facial", "Litro", 18, pres("Botella 250ml", 1));
        addProd(prods, ppts, SALON_BELLEZA, guantes, "Guantes de Latex", "Caja", 19, pres("Caja x100", 1));
        addProd(prods, ppts, SALON_BELLEZA, capas, "Capa para Corte", "Unidad", 20, pres("Unidad", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedFerreteria() {
        var cats = new ArrayList<Object[]>();

        var herramientas = UUID.randomUUID();
        var tornilleria = UUID.randomUUID();
        var construccion = UUID.randomUUID();
        var pintura = UUID.randomUUID();
        var fontaneria = UUID.randomUUID();
        var electricidad = UUID.randomUUID();
        var otros = UUID.randomUUID();
        cats.add(cat(FERRETERIA, herramientas, "Herramientas", null, 1));
        cats.add(cat(FERRETERIA, tornilleria, "Tornillería", null, 2));
        cats.add(cat(FERRETERIA, construccion, "Materiales de Construcción", null, 3));
        cats.add(cat(FERRETERIA, pintura, "Pintura y Acabados", null, 4));
        cats.add(cat(FERRETERIA, fontaneria, "Fontanería", null, 5));
        cats.add(cat(FERRETERIA, electricidad, "Electricidad", null, 6));
        cats.add(cat(FERRETERIA, otros, "Otros Suministros", null, 7));

        var manuales = UUID.randomUUID();
        var electricas = UUID.randomUUID();
        cats.add(cat(FERRETERIA, manuales, "Herramientas Manuales", herramientas, 1));
        cats.add(cat(FERRETERIA, electricas, "Herramientas Eléctricas", herramientas, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Martillos y Mazos", manuales, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Destornilladores", manuales, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Llaves", manuales, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Sierras", manuales, 4));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Otras", manuales, 5));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Taladros", electricas, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Sierras Eléctricas", electricas, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Otros", electricas, 3));

        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Tornillos", tornilleria, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Tuercas", tornilleria, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Arandelas", tornilleria, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Clavos", tornilleria, 4));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Pernos", tornilleria, 5));

        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Cemento y Concreto", construccion, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Arena y Grava", construccion, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Varillas de Acero", construccion, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Bloques y Ladrillos", construccion, 4));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Madera", construccion, 5));

        var pinturas = UUID.randomUUID();
        cats.add(cat(FERRETERIA, pinturas, "Pinturas", pintura, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Brochas y Rodillos", pintura, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Espátulas", pintura, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Thinner y Diluyentes", pintura, 4));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Masilla", pintura, 5));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Interior", pinturas, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Exterior", pinturas, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Especiales", pinturas, 3));

        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Tuberías", fontaneria, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Codos y Adaptadores", fontaneria, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Llaves de Paso", fontaneria, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Selladores", fontaneria, 4));

        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Cables", electricidad, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Breakers", electricidad, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Tomas de Corriente", electricidad, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Interruptores", electricidad, 4));

        var seguridad = UUID.randomUUID();
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Pegamentos y Adhesivos", otros, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Cintas", otros, 2));
        cats.add(cat(FERRETERIA, seguridad, "Seguridad", otros, 3));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Guantes", seguridad, 1));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Cascos", seguridad, 2));
        cats.add(cat(FERRETERIA, UUID.randomUUID(), "Lentes", seguridad, 3));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(FERRETERIA, "Unidad", "Metro", "Cm", "Kg", "Lb", "Galón", "Caja", "Bolsa", "Paquete"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(FERRETERIA, "Efectivo", "Tarjeta", "Crédito", "Cheque"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, FERRETERIA, manuales, "Martillo", "Unidad", 1, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, manuales, "Destornillador Plano", "Unidad", 2, pres("Unidad", 1), pres("Juego x6", 6));
        addProd(prods, ppts, FERRETERIA, manuales, "Llave Inglesa", "Unidad", 3, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, manuales, "Cinta Métrica", "Unidad", 4, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, electricas, "Taladro Eléctrico", "Unidad", 5, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, electricas, "Sierra Eléctrica", "Unidad", 6, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, tornilleria, "Tornillos Varios", "Caja", 7, pres("Caja 100pz", 1));
        addProd(prods, ppts, FERRETERIA, tornilleria, "Clavos 2\"", "Lb", 8, pres("Bolsa 1lb", 1));
        addProd(prods, ppts, FERRETERIA, tornilleria, "Tuercas y Arandelas", "Caja", 9, pres("Caja 50pz", 1));
        addProd(prods, ppts, FERRETERIA, construccion, "Cemento", "Kg", 10, pres("Bolsa 42.5kg", 1));
        addProd(prods, ppts, FERRETERIA, construccion, "Varilla de Acero", "Unidad", 11, pres("Unidad 6m", 1));
        addProd(prods, ppts, FERRETERIA, pinturas, "Pintura Blanca Interior", "Galón", 12, pres("Galón", 1), pres("Cuarto de Galón", 1));
        addProd(prods, ppts, FERRETERIA, pinturas, "Thinner", "Litro", 13, pres("Galón", 1));
        addProd(prods, ppts, FERRETERIA, pintura, "Brocha 2\"", "Unidad", 14, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, fontaneria, "Tubería PVC 1/2\"", "Metro", 15, pres("Metro", 1));
        addProd(prods, ppts, FERRETERIA, fontaneria, "Codo PVC 1/2\"", "Unidad", 16, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, fontaneria, "Sellador de Tuberías", "Unidad", 17, pres("Tubo", 1));
        addProd(prods, ppts, FERRETERIA, electricidad, "Cable Eléctrico #12", "Metro", 18, pres("Metro", 1));
        addProd(prods, ppts, FERRETERIA, electricidad, "Toma Corriente Doble", "Unidad", 19, pres("Unidad", 1));
        addProd(prods, ppts, FERRETERIA, electricidad, "Interruptor Sencillo", "Unidad", 20, pres("Unidad", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedMiniSuper() {
        var cats = new ArrayList<Object[]>();

        var frescos = UUID.randomUUID();
        var abarrotes = UUID.randomUUID();
        var bebidas = UUID.randomUUID();
        var congelados = UUID.randomUUID();
        var snacks = UUID.randomUUID();
        var higiene = UUID.randomUUID();
        var limpieza = UUID.randomUUID();
        var mascotas = UUID.randomUUID();
        var otros = UUID.randomUUID();
        cats.add(cat(MINI_SUPER, frescos, "Alimentos Frescos", null, 1));
        cats.add(cat(MINI_SUPER, abarrotes, "Abarrotes", null, 2));
        cats.add(cat(MINI_SUPER, bebidas, "Bebidas", null, 3));
        cats.add(cat(MINI_SUPER, congelados, "Congelados", null, 4));
        cats.add(cat(MINI_SUPER, snacks, "Snacks", null, 5));
        cats.add(cat(MINI_SUPER, higiene, "Higiene Personal", null, 6));
        cats.add(cat(MINI_SUPER, limpieza, "Limpieza", null, 7));
        cats.add(cat(MINI_SUPER, mascotas, "Mascotas", null, 8));
        cats.add(cat(MINI_SUPER, otros, "Otros", null, 9));

        var lacteos = UUID.randomUUID();
        var carnesFrias = UUID.randomUUID();
        var frutas = UUID.randomUUID();
        cats.add(cat(MINI_SUPER, lacteos, "Lácteos", frescos, 1));
        cats.add(cat(MINI_SUPER, carnesFrias, "Carnes Frías", frescos, 2));
        cats.add(cat(MINI_SUPER, frutas, "Frutas y Verduras", frescos, 3));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Leche", lacteos, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Quesos", lacteos, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Yogur", lacteos, 3));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Jamón", carnesFrias, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Salchicha", carnesFrias, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Mortadela", carnesFrias, 3));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Frutas", frutas, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Verduras", frutas, 2));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Arroz y Granos", abarrotes, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Pasta", abarrotes, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Aceites", abarrotes, 3));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Condimentos", abarrotes, 4));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Harinas", abarrotes, 5));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Refrescos", bebidas, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Agua", bebidas, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Jugos", bebidas, 3));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Cervezas", bebidas, 4));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Licores", bebidas, 5));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Carnes", congelados, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Verduras Congeladas", congelados, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Otros", congelados, 3));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Papitas", snacks, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Galletas", snacks, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Dulces", snacks, 3));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Jabones", higiene, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Champús", higiene, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Pasta de Dientes", higiene, 3));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Desodorantes", higiene, 4));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Detergentes", limpieza, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Desinfectantes", limpieza, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Otros", limpieza, 3));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Alimento", mascotas, 1));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Accesorios", mascotas, 2));
        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Higiene", mascotas, 3));

        cats.add(cat(MINI_SUPER, UUID.randomUUID(), "Productos Varios", otros, 1));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(MINI_SUPER, "Unidad", "Caja", "Paquete", "Botella", "Lata", "Kg", "Lb"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(MINI_SUPER, "Efectivo", "Tarjeta", "Cheque"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, MINI_SUPER, lacteos, "Leche Entera", "Litro", 1, pres("Bolsa 1L", 1));
        addProd(prods, ppts, MINI_SUPER, lacteos, "Queso Amarillo", "Kg", 2, pres("Kg", 1));
        addProd(prods, ppts, MINI_SUPER, lacteos, "Yogur Natural", "Litro", 3, pres("Botella 1L", 1));
        addProd(prods, ppts, MINI_SUPER, carnesFrias, "Jamón de Pollo", "Kg", 4, pres("Kg", 1));
        addProd(prods, ppts, MINI_SUPER, carnesFrias, "Salchicha", "Kg", 5, pres("Bolsa 500g", 1));
        addProd(prods, ppts, MINI_SUPER, frutas, "Frutas Mixtas", "Kg", 6, pres("Kg", 1));
        addProd(prods, ppts, MINI_SUPER, abarrotes, "Arroz", "Kg", 7, pres("Bolsa 1kg", 1), pres("Bolsa 5kg", 5));
        addProd(prods, ppts, MINI_SUPER, abarrotes, "Pasta Spaghetti", "Kg", 8, pres("Paquete 500g", 1));
        addProd(prods, ppts, MINI_SUPER, abarrotes, "Aceite Vegetal", "Litro", 9, pres("Botella 1L", 1));
        addProd(prods, ppts, MINI_SUPER, abarrotes, "Harina de Trigo", "Kg", 10, pres("Bolsa 1kg", 1));
        addProd(prods, ppts, MINI_SUPER, abarrotes, "Azúcar", "Kg", 11, pres("Bolsa 1kg", 1), pres("Bolsa 5kg", 5));
        addProd(prods, ppts, MINI_SUPER, bebidas, "Refresco Cola", "Botella", 12, pres("Botella 2L", 1));
        addProd(prods, ppts, MINI_SUPER, bebidas, "Agua Natural", "Botella", 13, pres("Botella 1L", 1));
        addProd(prods, ppts, MINI_SUPER, bebidas, "Cerveza Nacional", "Unidad", 14, pres("Unidad", 1), pres("Caja x12", 12));
        addProd(prods, ppts, MINI_SUPER, snacks, "Papitas", "Bolsa", 15, pres("Bolsa 150g", 1));
        addProd(prods, ppts, MINI_SUPER, snacks, "Galletas", "Paquete", 16, pres("Paquete", 1));
        addProd(prods, ppts, MINI_SUPER, snacks, "Dulces Varios", "Kg", 17, pres("Kg", 1));
        addProd(prods, ppts, MINI_SUPER, higiene, "Jabón de Baño", "Unidad", 18, pres("Unidad", 1));
        addProd(prods, ppts, MINI_SUPER, higiene, "Pasta Dental", "Unidad", 19, pres("Tubo", 1));
        addProd(prods, ppts, MINI_SUPER, limpieza, "Detergente en Polvo", "Kg", 20, pres("Bolsa 1kg", 1));
        addProd(prods, ppts, MINI_SUPER, limpieza, "Desinfectante", "Litro", 21, pres("Botella 1L", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedTallerMecanico() {
        var cats = new ArrayList<Object[]>();

        var motor = UUID.randomUUID();
        var frenos = UUID.randomUUID();
        var suspension = UUID.randomUUID();
        var electrico = UUID.randomUUID();
        var herramientas = UUID.randomUUID();
        var consumibles = UUID.randomUUID();
        var variadas = UUID.randomUUID();
        cats.add(cat(TALLER_MECANICO, motor, "Piezas de Motor", null, 1));
        cats.add(cat(TALLER_MECANICO, frenos, "Sistema de Frenos", null, 2));
        cats.add(cat(TALLER_MECANICO, suspension, "Suspensión", null, 3));
        cats.add(cat(TALLER_MECANICO, electrico, "Sistema Eléctrico", null, 4));
        cats.add(cat(TALLER_MECANICO, herramientas, "Herramientas", null, 5));
        cats.add(cat(TALLER_MECANICO, consumibles, "Consumibles", null, 6));
        cats.add(cat(TALLER_MECANICO, variadas, "Piezas Variadas", null, 7));

        var filtros = UUID.randomUUID();
        var lubricantes = UUID.randomUUID();
        var liquidos = UUID.randomUUID();
        cats.add(cat(TALLER_MECANICO, filtros, "Filtros", motor, 1));
        cats.add(cat(TALLER_MECANICO, lubricantes, "Lubricantes", motor, 2));
        cats.add(cat(TALLER_MECANICO, liquidos, "Líquidos", motor, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Filtro de Aire", filtros, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Filtro de Aceite", filtros, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Filtro de Gasolina", filtros, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Aceites", lubricantes, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Grasas", lubricantes, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Refrigerantes", lubricantes, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Líquido de Frenos", liquidos, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Líquido de Dirección", liquidos, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Combustible", liquidos, 3));

        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Pastillas de Freno", frenos, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Discos de Freno", frenos, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Cilindros", frenos, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Tuberías", frenos, 4));

        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Amortiguadores", suspension, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Resortes", suspension, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Rótulas", suspension, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Silentblocks", suspension, 4));

        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Baterías", electrico, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Alternadores", electrico, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Motores de Arranque", electrico, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Cables", electrico, 4));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Fusibles", electrico, 5));

        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Llaves", herramientas, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Destornilladores", herramientas, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Extractores", herramientas, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Especializada", herramientas, 4));

        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Paños y Trapos", consumibles, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Guantes de Trabajo", consumibles, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Selladores", consumibles, 3));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Adhesivos", consumibles, 4));

        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Correas y Mangueras", variadas, 1));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Cojinetes", variadas, 2));
        cats.add(cat(TALLER_MECANICO, UUID.randomUUID(), "Sellos", variadas, 3));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(TALLER_MECANICO, "Unidad", "Litro", "Ml", "Caja", "Juego", "Kit", "Kg"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(TALLER_MECANICO, "Efectivo", "Tarjeta", "Crédito", "ACH"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, TALLER_MECANICO, filtros, "Filtro de Aceite", "Unidad", 1, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, filtros, "Filtro de Aire", "Unidad", 2, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, lubricantes, "Aceite de Motor 20W50", "Litro", 3, pres("Botella 1L", 1), pres("Galón 5L", 5));
        addProd(prods, ppts, TALLER_MECANICO, lubricantes, "Grasa Multiusos", "Kg", 4, pres("Tarro 500g", 1));
        addProd(prods, ppts, TALLER_MECANICO, liquidos, "Líquido de Frenos DOT4", "Litro", 5, pres("Botella 500ml", 1));
        addProd(prods, ppts, TALLER_MECANICO, liquidos, "Refrigerante", "Litro", 6, pres("Galón 3.78L", 1));
        addProd(prods, ppts, TALLER_MECANICO, frenos, "Pastillas de Freno", "Juego", 7, pres("Juego x4", 1));
        addProd(prods, ppts, TALLER_MECANICO, frenos, "Disco de Freno", "Unidad", 8, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, suspension, "Amortiguador Delantero", "Unidad", 9, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, suspension, "Rótula de Suspensión", "Unidad", 10, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, electrico, "Batería de Auto", "Unidad", 11, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, electrico, "Cable de Batería", "Unidad", 12, pres("Juego", 1));
        addProd(prods, ppts, TALLER_MECANICO, electrico, "Fusibles Varios", "Caja", 13, pres("Caja x20", 1));
        addProd(prods, ppts, TALLER_MECANICO, herramientas, "Llave de Tubo", "Unidad", 14, pres("Unidad", 1), pres("Juego x12", 12));
        addProd(prods, ppts, TALLER_MECANICO, herramientas, "Destornillador Torx", "Unidad", 15, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, consumibles, "Paños de Limpieza", "Caja", 16, pres("Caja", 1));
        addProd(prods, ppts, TALLER_MECANICO, consumibles, "Guantes de Mecánico", "Caja", 17, pres("Caja x50", 1));
        addProd(prods, ppts, TALLER_MECANICO, consumibles, "Sellador de Silicona", "Unidad", 18, pres("Tubo", 1));
        addProd(prods, ppts, TALLER_MECANICO, variadas, "Correa de Distribución", "Unidad", 19, pres("Unidad", 1));
        addProd(prods, ppts, TALLER_MECANICO, variadas, "Manguera de Radiador", "Unidad", 20, pres("Unidad", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedFarmacia() {
        var cats = new ArrayList<Object[]>();

        var medicamentos = UUID.randomUUID();
        var cuidadoPersonal = UUID.randomUUID();
        var bebes = UUID.randomUUID();
        var auxilios = UUID.randomUUID();
        var suplementos = UUID.randomUUID();
        var equipos = UUID.randomUUID();
        var hogar = UUID.randomUUID();
        cats.add(cat(FARMACIA, medicamentos, "Medicamentos", null, 1));
        cats.add(cat(FARMACIA, cuidadoPersonal, "Cuidado Personal", null, 2));
        cats.add(cat(FARMACIA, bebes, "Productos para Bebés", null, 3));
        cats.add(cat(FARMACIA, auxilios, "Primeros Auxilios", null, 4));
        cats.add(cat(FARMACIA, suplementos, "Suplementos y Vitaminas", null, 5));
        cats.add(cat(FARMACIA, equipos, "Equipos y Dispositivos", null, 6));
        cats.add(cat(FARMACIA, hogar, "Higiene del Hogar", null, 7));

        cats.add(cat(FARMACIA, UUID.randomUUID(), "Analgésicos", medicamentos, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Antibióticos", medicamentos, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Antiinflamatorios", medicamentos, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Antigripales", medicamentos, 4));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Antidiarreicos", medicamentos, 5));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Laxantes", medicamentos, 6));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Otros", medicamentos, 7));

        var bucal = UUID.randomUUID();
        var corporal = UUID.randomUUID();
        var piel = UUID.randomUUID();
        cats.add(cat(FARMACIA, bucal, "Higiene Bucal", cuidadoPersonal, 1));
        cats.add(cat(FARMACIA, corporal, "Higiene Corporal", cuidadoPersonal, 2));
        cats.add(cat(FARMACIA, piel, "Cuidado de la Piel", cuidadoPersonal, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Pasta de Dientes", bucal, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Enjuague", bucal, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Cepillos", bucal, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Jabones", corporal, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Champús", corporal, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Desodorantes", corporal, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Cremas", piel, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Lociones", piel, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Protectores Solares", piel, 3));

        cats.add(cat(FARMACIA, UUID.randomUUID(), "Pañales", bebes, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Toallitas Húmedas", bebes, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Champú y Jabón", bebes, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Cremas para Bebé", bebes, 4));

        cats.add(cat(FARMACIA, UUID.randomUUID(), "Vendajes", auxilios, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Desinfectantes", auxilios, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Gasas", auxilios, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Apósitos", auxilios, 4));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Otros", auxilios, 5));

        cats.add(cat(FARMACIA, UUID.randomUUID(), "Multivitaminas", suplementos, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Probióticos", suplementos, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Minerales", suplementos, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Otros", suplementos, 4));

        cats.add(cat(FARMACIA, UUID.randomUUID(), "Termómetros", equipos, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Glucómetros", equipos, 2));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Tensiómetros", equipos, 3));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Otros", equipos, 4));

        cats.add(cat(FARMACIA, UUID.randomUUID(), "Desinfectantes", hogar, 1));
        cats.add(cat(FARMACIA, UUID.randomUUID(), "Repelentes", hogar, 2));

        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(FARMACIA, "Unidad", "Caja", "Blíster", "Frasco", "Ml", "Gr", "Botella"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(FARMACIA, "Efectivo", "Tarjeta", "Crédito", "Seguro médico"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, FARMACIA, medicamentos, "Ibuprofeno 400mg", "Caja", 1, pres("Caja x20", 1));
        addProd(prods, ppts, FARMACIA, medicamentos, "Paracetamol 500mg", "Caja", 2, pres("Caja x20", 1));
        addProd(prods, ppts, FARMACIA, medicamentos, "Aspirina 100mg", "Caja", 3, pres("Caja x20", 1));
        addProd(prods, ppts, FARMACIA, medicamentos, "Antigripal", "Caja", 4, pres("Caja x10", 1));
        addProd(prods, ppts, FARMACIA, medicamentos, "Loratadina 10mg", "Caja", 5, pres("Caja x10", 1));
        addProd(prods, ppts, FARMACIA, medicamentos, "Omeprazol 20mg", "Caja", 6, pres("Caja x14", 1));
        addProd(prods, ppts, FARMACIA, bucal, "Pasta Dental", "Tubo", 7, pres("Tubo 120ml", 1));
        addProd(prods, ppts, FARMACIA, bucal, "Enjuague Bucal", "Litro", 8, pres("Botella 500ml", 1));
        addProd(prods, ppts, FARMACIA, corporal, "Jabón de Baño", "Unidad", 9, pres("Unidad", 1));
        addProd(prods, ppts, FARMACIA, corporal, "Champú", "Litro", 10, pres("Botella 500ml", 1));
        addProd(prods, ppts, FARMACIA, corporal, "Desodorante", "Unidad", 11, pres("Unidad", 1));
        addProd(prods, ppts, FARMACIA, piel, "Protector Solar SPF50", "Litro", 12, pres("Botella 200ml", 1));
        addProd(prods, ppts, FARMACIA, bebes, "Pañales Talla M", "Caja", 13, pres("Caja x40", 1));
        addProd(prods, ppts, FARMACIA, bebes, "Toallitas Húmedas", "Caja", 14, pres("Caja x80", 1));
        addProd(prods, ppts, FARMACIA, auxilios, "Venda Elástica", "Unidad", 15, pres("Unidad", 1));
        addProd(prods, ppts, FARMACIA, auxilios, "Alcohol 70°", "Litro", 16, pres("Botella 500ml", 1));
        addProd(prods, ppts, FARMACIA, auxilios, "Gasas Estériles", "Caja", 17, pres("Caja x10", 1));
        addProd(prods, ppts, FARMACIA, suplementos, "Multivitamínico", "Caja", 18, pres("Caja x30", 1));
        addProd(prods, ppts, FARMACIA, suplementos, "Vitamina C 500mg", "Caja", 19, pres("Caja x30", 1));
        addProd(prods, ppts, FARMACIA, equipos, "Termómetro Digital", "Unidad", 20, pres("Unidad", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private void seedDefault() {
        var cats = new ArrayList<Object[]>();
        var defaultCat = UUID.randomUUID();
        cats.add(cat(DEFAULT, defaultCat, "General", null, 1));
        cats.add(cat(DEFAULT, UUID.randomUUID(), "Sin subcategoría", defaultCat, 1));
        jdbc.batchUpdate(SQL_INSERT_CATEGORIES, cats);
        jdbc.batchUpdate(SQL_INSERT_UNITS,
                units(DEFAULT, "Unidad", "Caja", "Paquete", "Kg", "Litro"));
        jdbc.batchUpdate(SQL_INSERT_PAYMENTS,
                paymentMethods(DEFAULT, "Efectivo", "Transferencia"));

        var prods = new ArrayList<Object[]>();
        var ppts = new ArrayList<Object[]>();
        addProd(prods, ppts, DEFAULT, defaultCat, "Producto Genérico", "Unidad", 1, pres("Unidad", 1));
        addProd(prods, ppts, DEFAULT, defaultCat, "Servicio General", "Unidad", 2, pres("Unidad", 1));
        jdbc.batchUpdate(SQL_INSERT_PRODUCTS, prods);
        jdbc.batchUpdate(SQL_INSERT_PRESENTATIONS, ppts);
    }

    private static Object[] cat(String industry, UUID id, String name, UUID parentId, int sortOrder) {
        return new Object[]{id, industry, name, parentId, sortOrder};
    }

    private static ArrayList<Object[]> units(String industry, String... names) {
        var list = new ArrayList<Object[]>();
        for (int i = 0; i < names.length; i++)
            list.add(new Object[]{UUID.randomUUID(), industry, names[i], i + 1});
        return list;
    }

    private static ArrayList<Object[]> paymentMethods(String industry, String... names) {
        var list = new ArrayList<Object[]>();
        for (int i = 0; i < names.length; i++)
            list.add(new Object[]{UUID.randomUUID(), industry, names[i], i + 1});
        return list;
    }

    // ponytail: single helper for both product + presentation batch data
    private void addProd(ArrayList<Object[]> prods, ArrayList<Object[]> ppts, String industry, UUID category, String name, String baseUnit, int sort, Object[]... presentations) {
        var id = UUID.randomUUID();
        prods.add(new Object[]{id, industry, category, name, baseUnit, null, null, sort});
        for (int i = 0; i < presentations.length; i++)
            ppts.add(new Object[]{UUID.randomUUID(), id, presentations[i][0], presentations[i][1], i + 1});
    }

    private static Object[] pres(String name, int conversion) {
        return new Object[]{name, conversion};
    }
}
