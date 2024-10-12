CREATE DATABASE IF NOT EXISTS product_db;
CREATE DATABASE IF NOT EXISTS order_db;

GRANT ALL PRIVILEGES ON product_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON order_db.* TO 'user'@'%';
FLUSH PRIVILEGES;

USE product_db;

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    version INT NOT NULL DEFAULT 0,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    price INT NOT NULL
);

INSERT INTO products (version, name, description, price)
VALUES(0, 'Laptop', 'High performance laptop', 1200),
      (0, 'Smartphone', 'Latest smartphone model', 800),
      (0, 'Headphones', 'Noise cancelling headphones', 150),
      (0, 'Monitor', '4K resolution monitor', 400),
      (0, 'Keyboard', 'Mechanical keyboard', 100);
