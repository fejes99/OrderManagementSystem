# Inventory Management Microservices

This project involves the development of a microservices-based Inventory Management System utilizing Spring Boot. The system comprises five microservices, including one composite service, and employs Spring Cloud, Docker, and Kubernetes for deployment and orchestration. This work represents the final project for the Distributed Information Systems course within the Master's program at the Faculty of Technical Sciences, University of Novi Sad.

## Microservices Overview

### 1. Product Service
- **Endpoints**:
  - `GET /products`: Retrieve all products
  - `GET /products/{id}`: Retrieve details of a specific product
  - `POST /products`: Create a new product
  - `PUT /products/{id}`: Update an existing product
  - `DELETE /products/{id}`: Delete a product
- **Database**: SQL (MySQL/PostgreSQL)
- **Database Schema**:
  ```mermaid
  classDiagram
      class Products {
          +UUID id
          +String name
          +String description
          +Decimal price
      }
  ```

### 2. Inventory Service
- **Endpoints**:
  - `GET /inventory`: Retrieve inventory levels for all products
  - `GET /inventory/{productId}`: Retrieve inventory stock for a specific product
  - `PUT /inventory/{productId}`: Update inventory stock for a product
- **Database**: SQL (MongoDB)
- **Database Schema**:
  ```mermaid
  classDiagram
      class Inventory {
          +UUID productId
          +Integer quantity
      }
  ```

### 3. Order Service
- **Endpoints**:
  - `GET /orders`: Retrieve all orders
  - `GET /orders/user/{userId}`: Retrieve all orders by a specific user
  - `GET /orders/{id}`: Retrieve details of a specific order
  - `POST /orders`: Create a new order
  - `PUT /orders/{id}`: Update an existing order (e.g., status)
  - `DELETE /orders/{id}`: Delete an order
- **Database**: SQL (MySQL/PostgreSQL)
- **Database Schema**:

  ```mermaid
  classDiagram
      class Orders {
          +UUID id
          +UUID userId
          +Decimal totalAmount
          +String status
          +Date createdAt
      }

      class OrderItems {
          +UUID id
          +UUID orderId
          +UUID productId
          +Integer quantity
          +Decimal price
      }

      Orders --> OrderItems : order_id
  ```

### 4. Shipping Service
- **Endpoints**:
  - `GET /shipments`: Retrieve all shipments
  - `GET /shipments/{id}`: Retrieve shipping details for a specific order
  - `POST /shipments`: Create a shipping order
  - `PUT /shipments/{id}`: Update shipments status
  - `DELETE /shipments/{id}`: Delete shipping
- **Database**: NoSQL (MongoDB)
- **Database Schema**:
  ```mermaid
  classDiagram
      class ShippingOrders {
          +UUID orderId
          +String shippingAddress
          +String status
      }
  ```

### 5. Order Composite Service
- **Endpoints**:
  - `GET /orders`: Retrieve all orders with shipping and inventory details
  - `GET /orders/{id}`: Retrieve an order along with shipping and inventory details
  - `GET /orders/user/{userId}`: Retrieve all order for specific user
  - `POST /orders`: Create a new order
- **Database**: None (Acts as an orchestrator)

## Architecture Diagrams

- **Use Case Diagram**: Shows user interactions with the system.
  ```mermaid
  sequenceDiagram
      actor User

      User ->> ProductService: Manage Products
      User ->> OrderCompositeService: Place Order
      User ->> InventoryService: Check Inventory
      OrderCompositeService ->> ProductService: Validate Product
      OrderCompositeService ->> InventoryService: Update Inventory
      OrderCompositeService ->> OrderService: Create Order
      OrderCompositeService ->> ShippingService: Create Shipping Order
  ```

- **Component Diagram**: Displays the architecture and interactions between services.
  ```mermaid
  graph TD
      subgraph "Product Service"
          PS[ProductService]
      end

      subgraph "Inventory Service"
          IS[InventoryService]
      end

      subgraph "Order Service"
          OS[OrderService]
      end

      subgraph "Shipping Service"
          SS[ShippingService]
      end

      subgraph "Order Composite Service"
          OMS[OrderCompositeService]
      end

      OMS --> PS
      OMS --> IS
      OMS --> OS
      OMS --> SS
  ```

- **Sequence Diagram**: Illustrates the process of order creation.
  ```mermaid
  sequenceDiagram
      actor User

      User ->> OrderCompositeService: POST /orders
      OrderCompositeService ->> ProductService: Validate Product
      OrderCompositeService ->> InventoryService: Update Inventory
      OrderCompositeService ->> OrderService: Create Order
      OrderCompositeService ->> ShippingService: Create Shipping Order
      OrderCompositeService ->> User: Order Confirmation
  ```

## Deployment

Each microservice is designed to be lightweight and easily deployable. Detailed deployment steps are not covered here as the focus is on business logic and microservices interaction.

## Getting Started

1. **Clone the repository**:

   ```bash
   git clone https://github.com/your-repo/inventory-management-microservices.git
   ```

2. **Run each microservice**:

   - Each service can be run independently using Docker or directly on your local machine.

3. **Testing**:
   - Use tools like Postman to interact with the services via their RESTful APIs.

## License

This project is licensed under the MIT License.