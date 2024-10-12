db = db.getSiblingDB('inventory_db');

db.inventories.drop();

db.inventories.insertMany([
    {
        productId: 1,  // Laptop
        quantity: 10,
        version: 0
    },
    {
        productId: 2,  // Smartphone
        quantity: 20,
        version: 0
    },
    {
        productId: 3,  // Headphones
        quantity: 50,
        version: 0
    },
    {
        productId: 4,  // Monitor
        quantity: 15,
        version: 0
    },
    {
        productId: 5,  // Keyboard
        quantity: 30,
        version: 0
    }
]);

print("Initialized inventory_db with inventories data");
