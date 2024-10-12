# Inventory Management Microservices

This project involves the development of a microservices-based Inventory Management System utilizing Spring Boot. The
system comprises five microservices, including one composite service, and employs Spring Cloud, Docker, and Kubernetes
for deployment and orchestration. This work represents the final project for the Distributed Information Systems course
within the Master's program at the Faculty of Technical Sciences, University of Novi Sad.

## Microservices Overview

### 1. Product Service

- **Endpoints**:
    - `GET /products`: Retrieve all products
    - `GET /products/byIds`: Retrieve products by list of ids
    - `GET /products/{id}`: Retrieve details of a specific product
    - `POST /products`: Create a new product
    - `PUT /products/{id}`: Update an existing product
    - `DELETE /products/{id}`: Delete a product
  - **Swagger UI**: `/swagger-ui/index.html`
- **Database**: SQL (MySQL)
- **Database Schema**:
  ```mermaid
  classDiagram
      class Products {
          +UUID id
          +String name
          +String description
          +Integer price
      }
  ```

### 2. Inventory Service

- **Endpoints**:
    - `GET /inventories`: Retrieve inventory levels for all products
    - `GET /inventories/{productId}`: Retrieve inventory stock for a specific product
    - `POST /inventories`: Create a new inventory stock
    - `PUT /inventories/increaseStock`: Increase inventory stock for a product
    - `PUT /inventories/reduceStock`: Reduce inventory stocks for products
    - `DELETE /inventories/{productId}`: Delete inventory stock for a product
  - **Swagger UI**: `/swagger-ui/index.html`
- **Database**: NoSQL (MongoDB)
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
  - **Swagger UI**: `/swagger-ui/index.html`
- **Database**: SQL (MySQL)
- **Database Schema**:

  ```mermaid
  classDiagram
      class Orders {
          +UUID id
          +UUID userId
          +Integer totalPrice
          +String status
          +Date createdAt
      }

      class OrderItems {
          +UUID id
          +UUID orderId
          +UUID productId
          +Integer quantity
          +Integer price
      }

      Orders --> OrderItems : order_id
  ```

### 4. Shipping Service

- **Endpoints**:
    - `GET /shipments`: Retrieve all shipments
    - `GET /shipments/byOrdersIds`: Retrieve shipments by list of order ids
    - `GET /shipments/order/{orderId}`: Retrieve shipping details for a specific order
    - `POST /shipments`: Create a shipping order
    - `PUT /shipments/order/{orderId}`: Update shipments status for a specific order
  - **Swagger UI**: `/swagger-ui/index.html`
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
    - `GET /order-composite`: Retrieve all orders with shipping and order details
    - `GET /order-composite/user/{userId}`: Retrieve all order for specific user
    - `GET /order-composite/{id}`: Retrieve an order along with shipping and order details
    - `POST /order-composite`: Create a new order
    - `GET /actuator/health`: Check the health status of the Order Composite Service
  - **Swagger UI**: `/swagger-ui/index.html`
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
      subgraph "Inventory Management System"
        PS[ProductService]
      
        IS[InventoryService]
            
        OS[OrderService]
        
        SS[ShippingService]
    
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
        participant User
        participant OrderCompositeService
        participant ProductService
        participant InventoryService
        participant OrderService
        participant ShippingService

        User->>OrderCompositeService: POST /orders
        OrderCompositeService->>ProductService: Validate products (GET /products/{id})
        ProductService-->>OrderCompositeService: Products valid

        OrderCompositeService->>InventoryService: Check and reserve stock (POST /inventory/check)
        InventoryService-->>OrderCompositeService: Stock reserved

        OrderCompositeService->>OrderService: Create order (POST /orders)
        OrderService-->>OrderCompositeService: Order created

        OrderCompositeService->>ShippingService: Create shipping (POST /shipping)
        ShippingService-->>OrderCompositeService: Shipping created

        OrderCompositeService-->>User: Order confirmation
    ```

## Deployment

Each microservice is designed to be lightweight and easily deployable. Detailed deployment steps are not covered here as
the focus is on business logic and microservices interaction.

## Getting Started

1. **Clone the repository**:

   ```bash
   git clone https://github.com/fejes99/OrderManagementSystem.git
   ```

2. **Build and Run**:

   ```bash
   ./gradlew build && docker-compose build && docker-compose up -d
   ```

3. **Start services with Docker Compose**:

   ```bash
   docker-compose build
   docker-compose up -d
   ```

4. **Access RabbitMQ management console**:

  - **URL**: `http://localhost:15672/#/queues`
  - **Username**: `guest`
  - **Password**: `guest`

5. **Access Spring Cloud Eureka** (Service Discovery Dashboard):

  - **URL**: `http://localhost:8761`

### Shutdown

To stop and remove containers, networks, and volumes:

```bash
docker-compose down
```

### Health Monitoring and Documentation

Each microservice includes health endpoints and Swagger UI for documentation:

- Health check endpoints: `/actuator/health`
- Swagger documentation: `/swagger-ui/index.html`

## Testing

To test the system, tools such as **Postman** can be used to send HTTP requests to the exposed RESTful APIs of each microservice.

## License

This project is licensed under the **MIT License**.

## Software Components and Design Patterns

The system implements various industry-standard components and design patterns, including:

1. **Service Discovery**: Spring Cloud Eureka for service registry and discovery.
2. **Edge Server**: Spring Cloud Gateway for routing and Spring Security OAuth for securing API endpoints.
3. **Centralized Configuration**: Spring Cloud Config Server for managing external configuration in a distributed system.
4. **Circuit Breaker**: Resilience4j for fault tolerance and service resiliency.
5. **Distributed Tracing**: Micrometer Tracing and Zipkin for tracing requests across microservices.