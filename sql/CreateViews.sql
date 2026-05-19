-- Josh Grey
USE PizzaDB;
CREATE VIEW ToppingPopularity AS
    SELECT
        topping_TopName AS Topping,
        COALESCE(SUM(CASE WHEN pizza_topping_IsDouble = 1 THEN 2 WHEN pizza_topping_IsDouble = 0 THEN 1 ELSE 0 END), 0) AS ToppingCount
    FROM
        topping
    LEFT JOIN
        pizza_topping ON topping.topping_TopID = pizza_topping.topping_TopID GROUP BY Topping ORDER BY ToppingCount DESC, Topping;

CREATE VIEW ProfitByPizza AS
    SELECT
        pizza_Size AS Size,
        pizza_CrustType AS Crust,
        SUM(pizza_CustPrice - pizza_BusPrice) AS Profit,
        DATE_FORMAT(pizza_pizzaDate, '%c/%Y') AS OrderMonth
    FROM pizza GROUP BY pizza_Size, pizza_CrustType, OrderMonth ORDER BY Profit;
CREATE VIEW ProfitByOrderType AS
    SELECT
        ordertable_OrderType AS customerType,
        DATE_FORMAT(ordertable_OrderDateTime, '%c/%Y') AS OrderMonth,
        SUM(ordertable_CustPrice) AS TotalOrderPrice,
        SUM(ordertable_BusPrice) AS TotalOrderCost,
        SUM(ordertable_CustPrice - ordertable_BusPrice) AS Profit
    FROM ordertable GROUP BY OrderMonth, customerType
    UNION SELECT NULL, 'Grand Total', SUM(ordertable_CustPrice), SUM(ordertable_BusPrice), SUM(ordertable_CustPrice - ordertable_BusPrice) FROM ordertable;
