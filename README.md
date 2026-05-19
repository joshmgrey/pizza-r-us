# Pizza R Us

A Java console application for managing a pizza restaurant's orders, customers, toppings inventory, and financial reporting. Backed by a MySQL database via the JDBC API.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Getting Started](#getting-started)
   - [Database Setup](#1-database-setup)
   - [Configure the Connection](#2-configure-the-connection)
   - [Download the JDBC Driver](#3-download-the-jdbc-driver)
   - [Compile](#4-compile)
   - [Run](#5-run)
3. [Project Structure](#project-structure)
4. [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java (JDK) | 8 or later |
| MySQL Server | 8.0 or later |
| MySQL Connector/J | 8.x or 9.x |

---

## Getting Started

### 1. Database Setup

Start your MySQL server, then log in and run the setup scripts **in the order shown below**:

```sql
source CreateTables.sql
source CreateSPs.sql
source CreateViews.sql
source PopulateData.sql
```

To wipe and reset the database at any point:

```sql
source DropTables.sql
```

Then re-run the setup scripts above.

---

### 2. Configure the Connection

Open [`DBConnector.java`](DBConnector.java) and update the four fields to match your local MySQL installation:

```java
protected static String user          = "root";
protected static String password      = "your_password_here";
private static String   database_name = "PizzaDB";
private static String   url           = "jdbc:mysql://127.0.0.1:3306";
```

> **Note:** Port `3306` is the MySQL default. Only change `url` if your instance runs on a non-standard port.

---

### 3. Download the JDBC Driver

Download the MySQL Connector/J JAR from the [official MySQL downloads page](https://dev.mysql.com/downloads/connector/j/) — select **Platform Independent**. Place the JAR in the project root directory.

The commands below assume version `9.3.0`. Replace the filename with the version you downloaded.

---

### 4. Compile

Open a terminal in the project root directory and run:

**Windows (PowerShell)**
```powershell
javac -cp ".;mysql-connector-j-9.3.0.jar" *.java
```

**macOS / Linux**
```bash
javac -cp ".:mysql-connector-j-9.3.0.jar" *.java
```

---

### 5. Run

**Windows (PowerShell)**
```powershell
java -cp ".;mysql-connector-j-9.3.0.jar" cpsc4620.Menu
```

**macOS / Linux**
```bash
java -cp ".:mysql-connector-j-9.3.0.jar" cpsc4620.Menu
```

---

## Project Structure

### Java Source Files

| File | Role |
|---|---|
| `DBConnector.java` | Establishes the JDBC connection to MySQL |
| `DBNinja.java` | Data access layer — all queries and updates |
| `Menu.java` | Application entry point and interactive menu loop |
| `Order.java` | Base order model |
| `DineinOrder.java` | Dine-in order (extends `Order`) |
| `PickupOrder.java` | Pickup order (extends `Order`) |
| `DeliveryOrder.java` | Delivery order with address (extends `Order`) |
| `Pizza.java` | Pizza model |
| `Topping.java` | Topping model with inventory amounts |
| `Customer.java` | Customer model |
| `Discount.java` | Discount model (flat or percentage) |

### SQL Scripts

| File | Purpose |
|---|---|
| `CreateTables.sql` | Creates all database tables and constraints |
| `CreateSPs.sql` | Creates stored procedures |
| `CreateViews.sql` | Creates views for reporting (`ToppingPopularity`, `ProfitByPizza`, `ProfitByOrderType`) |
| `PopulateData.sql` | Inserts sample seed data |
| `DropTables.sql` | Drops all tables for a clean reset |

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `Could not load the driver` | Connector/J JAR missing from classpath | Verify the `-cp` flag includes the correct JAR filename and path |
| `Access denied for user` | Wrong credentials | Update `user` and `password` in `DBConnector.java` |
| `Unknown database 'PizzaDB'` | Database not yet created | Run the SQL setup scripts; confirm `database_name` matches the database name in the scripts |
| `Communications link failure` | MySQL server not running or wrong host/port | Start MySQL and verify the `url` in `DBConnector.java` |
