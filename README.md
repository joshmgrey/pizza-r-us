# Pizza R Us — Java + MySQL Application

A Java application for managing a pizza restaurant's orders, customers, toppings, and reporting. Connects to a MySQL database using the MySQL Connector/J JDBC driver.

## Prerequisites

- Java 8 or later
- MySQL 8.0 or later
- [MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/) (JDBC driver)

## Database Setup

1. Start your MySQL server and log in:
   ```
   mysql -u root -p
   ```

2. Run the SQL scripts in order:
   ```sql
   source CreateTables.sql
   source CreateSPs.sql
   source CreateViews.sql
   source PopulateData.sql
   ```

   To reset the database at any point:
   ```sql
   source DropTables.sql
   ```

## Configure the Connection

Open [DBConnector.java](DBConnector.java) and update the credentials to match your MySQL setup:

```java
protected static String user = "root";
protected static String password = "your_password_here";
private static String database_name = "PizzaDB";
private static String url = "jdbc:mysql://127.0.0.1:3306";
```

The default port `3306` is the MySQL default — change `url` only if your instance runs on a different port.

## Compile and Run

### Download the MySQL Connector/J JAR

Download `mysql-connector-j-<version>.jar` from https://dev.mysql.com/downloads/connector/j/ (select "Platform Independent").

### Compile

Place the JAR in the project directory (or any path you prefer), then compile all Java files with the connector on the classpath:

**Windows (PowerShell):**
```powershell
javac -cp ".;mysql-connector-j-9.3.0.jar" *.java
```

**macOS / Linux:**
```bash
javac -cp ".:mysql-connector-j-9.3.0.jar" *.java
```

Replace `mysql-connector-j-9.3.0.jar` with the actual filename of the JAR you downloaded.

### Run

**Windows (PowerShell):**
```powershell
java -cp ".;mysql-connector-j-9.3.0.jar" cpsc4620.Menu
```

**macOS / Linux:**
```bash
java -cp ".:mysql-connector-j-9.3.0.jar" cpsc4620.Menu
```

## Project Structure

| File | Description |
|---|---|
| `DBConnector.java` | Opens the JDBC connection to MySQL |
| `DBNinja.java` | All database query and update logic |
| `Menu.java` | Main entry point / UI loop |
| `Order.java` / `DineinOrder.java` / `PickupOrder.java` / `DeliveryOrder.java` | Order model classes |
| `Pizza.java` | Pizza model |
| `Topping.java` | Topping model with inventory tracking |
| `Customer.java` | Customer model |
| `Discount.java` | Discount model |
| `CreateTables.sql` | Schema creation |
| `CreateSPs.sql` | Stored procedures |
| `CreateViews.sql` | Views used for reports (`ToppingPopularity`, `ProfitByPizza`, `ProfitByOrderType`) |
| `PopulateData.sql` | Sample seed data |
| `DropTables.sql` | Drops all tables for a clean reset |

## Troubleshooting

**`Could not load the driver`** — the MySQL Connector/J JAR is not on the classpath. Double-check the `-cp` flag includes the JAR path.

**`Access denied for user`** — the username or password in `DBConnector.java` does not match your MySQL credentials.

**`Unknown database 'PizzaDB'`** — the SQL scripts have not been run yet, or the `database_name` field in `DBConnector.java` does not match the database created by the scripts.
