package at.ac.fhcampuswien.barcode_scanner;

public class Product {
    private final String name;
    private final String brand;
    private final String category;
    private final String energy;
    private final String fat;
    private final String allergens;
    private final String imageUrl;
    private final String quantity;
    private final int nova_group;
    private final String countries;
    private final String manufacturing_places;
    private final String ingredients_text;
    private final String nutriscore_grade;

    public Product(String name, String brand, String category, String energy, String fat, String allergens,
                   String imageUrl, String quantity, int nova_group, String countries, String manufacturing_places,
                   String ingredients_text, String nutriscore_grade) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.energy = energy;
        this.fat = fat;
        this.allergens = allergens;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.nova_group = nova_group;
        this.countries = countries;
        this.manufacturing_places = manufacturing_places;
        this.ingredients_text = ingredients_text;
        this.nutriscore_grade = nutriscore_grade;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public String getCategory() {
        return category;
    }

    public String getEnergy() {
        return energy;
    }

    public String getFat() {
        return fat;
    }

    public String getAllergens() {
        return allergens;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getQuantity() {
        return quantity;
    }

    public int getNova_group() {
        return nova_group;
    }

    public String getCountries() {
        return countries;
    }

    public String getManufacturing_places() {
        return manufacturing_places;
    }

    public String getIngredients_text() {
        return ingredients_text;
    }

    public String getNutriscore_grade() {
        return nutriscore_grade;
    }
}
