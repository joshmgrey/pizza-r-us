-- Josh Grey
CREATE SCHEMA IF NOT EXISTS PizzaDB;
USE PizzaDB;
CREATE TABLE customer (
    customer_CustID INT AUTO_INCREMENT PRIMARY KEY,
    customer_FName VARCHAR(30) NOT NULL,
    customer_LName VARCHAR(30) NOT NULL,
    customer_PhoneNum VARCHAR(30) NOT NULL
);
CREATE TABLE baseprice (
    baseprice_Size VARCHAR(30) NOT NULL,
    baseprice_CrustType VARCHAR(30) NOT NULL,
    baseprice_CustPrice DECIMAL(5, 2) NOT NULL CHECK(baseprice_CustPrice > 0),
    baseprice_BusPrice DECIMAL(5, 2) NOT NULL CHECK(baseprice_BusPrice > 0),
    PRIMARY KEY (baseprice_Size, baseprice_CrustType)
);
CREATE TABLE topping (
    topping_TopID INT AUTO_INCREMENT PRIMARY KEY,
    topping_TopName VARCHAR(30) NOT NULL,
    topping_SmallAMT DECIMAL(5, 2) NOT NULL CHECK(topping_SmallAMT > 0),
    topping_MedAMT DECIMAL(5, 2) NOT NULL CHECK(topping_MedAMT > 0),
    topping_LgAMT DECIMAL(5, 2) NOT NULL CHECK(topping_LgAMT > 0),
    topping_XLAMT DECIMAL(5, 2) NOT NULL CHECK(topping_XLAMT > 0),
    topping_CustPrice DECIMAL(5, 2) NOT NULL CHECK(topping_CustPrice > 0),
    topping_BusPrice DECIMAL(5, 2) NOT NULL CHECK(topping_BusPrice > 0),
    topping_MinINVT INT NOT NULL CHECK(topping_MinINVT >= 0),
    topping_CurINVT INT NOT NULL
);
CREATE TABLE discount (
    discount_DiscountID INT AUTO_INCREMENT PRIMARY KEY,
    discount_DiscountName VARCHAR(30) NOT NULL,
    discount_Amount DECIMAL(5, 2) NOT NULL CHECK(discount_Amount > 0),
    discount_IsPercent BOOLEAN NOT NULL
);
CREATE TABLE ordertable (
    ordertable_OrderID INT AUTO_INCREMENT PRIMARY KEY,
    customer_CustID INT,
    ordertable_OrderType VARCHAR(30) NOT NULL,
    ordertable_OrderDateTime DATETIME NOT NULL,
    ordertable_CustPrice DECIMAL(5, 2) NOT NULL CHECK (ordertable_CustPrice > 0),
    ordertable_BusPrice DECIMAL(5, 2) NOT NULL CHECK(ordertable_BusPrice > 0),
    ordertable_isComplete BOOLEAN DEFAULT 0,
    FOREIGN KEY (customer_CustID) REFERENCES customer(customer_CustID) ON DELETE CASCADE
);
CREATE TABLE order_discount(
    ordertable_OrderID INT NOT NULL,
    discount_DiscountID INT NOT NULL,
    PRIMARY KEY (ordertable_OrderID, discount_DiscountID),
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID) ON DELETE CASCADE,
    FOREIGN KEY (discount_DiscountID) REFERENCES discount(discount_DiscountID) ON DELETE CASCADE
);
CREATE TABLE pizza (
    pizza_PizzaID INT AUTO_INCREMENT PRIMARY KEY,
    pizza_Size VARCHAR(30) NOT NULL,
    pizza_CrustType VARCHAR(30) NOT NULL,
    pizza_PizzaState VARCHAR(30) NOT NULL,
    pizza_PizzaDate DATETIME NOT NULL,
    pizza_CustPrice DECIMAL(5, 2) NOT NULL CHECK(pizza_CustPrice > 0),
    pizza_BusPrice DECIMAL(5, 2) NOT NULL CHECK(pizza_BusPrice > 0),
    ordertable_OrderID INT NOT NULL,
    FOREIGN KEY (pizza_Size, pizza_CrustType) REFERENCES baseprice(baseprice_Size, baseprice_CrustType) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable(ordertable_OrderID) ON DELETE CASCADE
);
CREATE TABLE pizza_topping (
    pizza_PizzaID INT NOT NULL,
    topping_TopID INT NOT NULL,
    pizza_topping_IsDouble INT NOT NULL,
    PRIMARY KEY (pizza_PizzaID, topping_TopID),
    FOREIGN KEY (pizza_PizzaID) REFERENCES pizza (pizza_PizzaID) ON DELETE CASCADE,
    FOREIGN KEY (topping_TopID) REFERENCES topping (topping_TopID) ON DELETE CASCADE
);
CREATE TABLE pizza_discount (
    pizza_PizzaID INT NOT NULL,
    discount_DiscountID INT NOT NULL,
    PRIMARY KEY (pizza_PizzaID, discount_DiscountID),
    FOREIGN KEY (pizza_PizzaID) REFERENCES pizza (pizza_PizzaID) ON DELETE CASCADE,
    FOREIGN KEY (discount_DiscountID) REFERENCES discount (discount_DiscountID) ON DELETE CASCADE
);
CREATE TABLE pickup (
    ordertable_OrderID INT PRIMARY KEY,
    pickup_IsPickedUp BOOLEAN NOT NULL DEFAULT 0,
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable (ordertable_OrderID) ON DELETE CASCADE
);
CREATE TABLE delivery (
    ordertable_OrderID INT PRIMARY KEY,
    delivery_HouseNum INT NOT NULL,
    delivery_Street VARCHAR(30) NOT NULL ,
    delivery_City VARCHAR(30) NOT NULL,
    delivery_State VARCHAR(2) NOT NULL,
    delivery_Zip INT NOT NULL,
    delivery_IsDelivered BOOLEAN NOT NULL DEFAULT 0,
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable (ordertable_OrderID) ON DELETE CASCADE
);
CREATE TABLE dinein (
    ordertable_OrderID INT PRIMARY KEY,
    dinein_TableNum INT NOT NULL,
    FOREIGN KEY (ordertable_OrderID) REFERENCES ordertable (ordertable_OrderID) ON DELETE CASCADE
);