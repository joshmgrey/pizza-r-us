package cpsc4620;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;

/*
 * This file is where you will implement the methods needed to support this application.
 * You will write the code to retrieve and save information to the database and use that
 * information to build the various objects required by the applicaiton.
 * 
 * The class has several hard coded static variables used for the connection, you will need to
 * change those to your connection information
 * 
 * This class also has static string variables for pickup, delivery and dine-in. 
 * DO NOT change these constant values.
 * 
 * You can add any helper methods you need, but you must implement all the methods
 * in this class and use them to complete the project.  The autograder will rely on
 * these methods being implemented, so do not delete them or alter their method
 * signatures.
 * 
 * Make sure you properly open and close your DB connections in any method that
 * requires access to the DB.
 * Use the connect_to_db below to open your connection in DBConnector.
 * What is opened must be closed!
 */

/*
 * A utility class to help add and retrieve information from the database
 */

public final class DBNinja {
	private static Connection conn;

	// DO NOT change these variables!
	public final static String pickup = "pickup";
	public final static String delivery = "delivery";
	public final static String dine_in = "dinein";

	public final static String size_s = "Small";
	public final static String size_m = "Medium";
	public final static String size_l = "Large";
	public final static String size_xl = "XLarge";

	public final static String crust_thin = "Thin";
	public final static String crust_orig = "Original";
	public final static String crust_pan = "Pan";
	public final static String crust_gf = "Gluten-Free";

	public enum order_state {
		PREPARED,
		DELIVERED,
		PICKEDUP
	}


	private static boolean connect_to_db() throws SQLException, IOException 
	{

		try {
			conn = DBConnector.make_connection();
			return true;
		} catch (SQLException | IOException e) {
			return false;
		}

	}

	public static void addOrder(Order o) throws SQLException, IOException 
	{
		try {
			int orderID;
			connect_to_db();
			String orderQuery = "INSERT INTO ordertable (customer_CustID, ordertable_OrderType, ordertable_OrderDateTime, ordertable_CustPrice, ordertable_BusPrice, ordertable_isComplete) VALUES (?, ?, ?, ?, ?, ?);";
			PreparedStatement orderStatement = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
			if(o.getCustID() == -1){
				orderStatement.setNull(1, Types.INTEGER);
			} else {
				orderStatement.setInt(1, o.getCustID());
			}
			orderStatement.setString(2, o.getOrderType());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date orderDate= format.parse(o.getDate());
			Timestamp orderDateTime = new Timestamp(orderDate.getTime());
			orderStatement.setTimestamp(3,orderDateTime);
			orderStatement.setDouble(4, o.getCustPrice());
			orderStatement.setDouble(5, o.getBusPrice());
			orderStatement.setBoolean(6, o.getIsComplete());
			orderStatement.executeUpdate();
			ResultSet generatedKey = orderStatement.getGeneratedKeys();
			if(generatedKey.next()){
					orderID = generatedKey.getInt(1);
					o.setOrderID(orderID);
			} else{
				throw new SQLException("Failed to retrieve auto-generated Order ID.");
			}

			switch (o.getOrderType()) {
				case dine_in:
					String dineQuery = "INSERT INTO dinein (ordertable_orderID, dinein_TableNum) VALUES (?, ?);";
					PreparedStatement dineStatement = conn.prepareStatement(dineQuery);
					dineStatement.setInt(1, orderID);
					dineStatement.setInt(2, ((DineinOrder) o).getTableNum());
					dineStatement.executeUpdate();
					break;
				case pickup:
					String pickupQuery = "Insert INTO pickup (ordertable_orderID, pickup_IsPickedUp) VALUES (?, ?);";
					PreparedStatement pickupStatement = conn.prepareStatement(pickupQuery);
					pickupStatement.setInt(1, orderID);
					pickupStatement.setBoolean(2, ((PickupOrder) o).getIsPickedUp());
					pickupStatement.executeUpdate();
					break;
				case delivery:
					String deliveryQuery = "INSERT INTO delivery (ordertable_orderID, delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered) VALUES (?, ?, ?, ?, ?, ?, ?);";
					PreparedStatement deliveryStatement = conn.prepareStatement(deliveryQuery);
					DeliveryOrder order = (DeliveryOrder) o;
					String address = order.getAddress();
					String[] addressParts = address.split("\t");
					int houseNum = Integer.parseInt(addressParts[0]);
					String houseStreet = addressParts[1];
					String city = addressParts[2];
					String state = addressParts[3];
					int zip = Integer.parseInt(addressParts[4]);
					deliveryStatement.setInt(1, orderID);
					deliveryStatement.setInt(2, houseNum);
					deliveryStatement.setString(3, houseStreet);
					deliveryStatement.setString(4, city);
					deliveryStatement.setString(5, state);
					deliveryStatement.setInt(6, zip);
					deliveryStatement.setBoolean(7, order.getIsDelivered());
					deliveryStatement.executeUpdate();
					break;
			}

			for(Pizza p : o.getPizzaList()){
				Date pizzaDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(o.getDate());
				p.setOrderID(orderID);
				addPizza(pizzaDate, orderID, p);
			}
			String discountQuery = "INSERT INTO order_discount (ordertable_OrderID, discount_DiscountID) VALUES (?, ?)";
			PreparedStatement discountStatement = conn.prepareStatement(discountQuery);
			for(Discount d : o.getDiscountList()){
				discountStatement.setInt(1, orderID);
				discountStatement.setInt(2, d.getDiscountID());
				discountStatement.executeUpdate();
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		} catch(ParseException e){
			throw new RuntimeException(e);
		}
		conn.close();
	}
	
	public static int addPizza(java.util.Date d, int orderID, Pizza p) throws SQLException, IOException
	{
		int pizzaID = -1;
		boolean createdLocalConnection = false;
		try{
			if(conn.isClosed() || conn == null) {
				connect_to_db();
				createdLocalConnection = true;
			}
			String pizzaQuery = "INSERT INTO pizza (pizza_Size, pizza_CrustType, pizza_PizzaState, pizza_PizzaDate, pizza_CustPrice, pizza_BusPrice, ordertable_OrderID) VALUES (?, ?, ?, ?, ?, ?, ?);";
			PreparedStatement pizzaStatement = conn.prepareStatement(pizzaQuery, Statement.RETURN_GENERATED_KEYS);
			pizzaStatement.setString(1, p.getSize());
			pizzaStatement.setString(2, p.getCrustType());
			pizzaStatement.setString(3, p.getPizzaState());
			Timestamp pizzaDate = new Timestamp(d.getTime());
			pizzaStatement.setTimestamp(4, pizzaDate);
			pizzaStatement.setDouble(5, p.getCustPrice());
			pizzaStatement.setDouble(6, p.getBusPrice());
			pizzaStatement.setInt(7, orderID);
			pizzaStatement.executeUpdate();
			ResultSet generatedKey = pizzaStatement.getGeneratedKeys();
			if(generatedKey.next()){
				pizzaID = generatedKey.getInt(1);
			}
			else {
				throw new SQLException("Failed to retrieve the Pizza ID");
			}

			String toppingQuery = "INSERT INTO pizza_topping (pizza_PizzaID, topping_TopID, pizza_topping_IsDouble) VALUES (?, ?, ?);";
			PreparedStatement toppingStatement = conn.prepareStatement(toppingQuery);
			for(Topping t : p.getToppings()){
				toppingStatement.setInt(1, pizzaID);
				toppingStatement.setInt(2, t.getTopID());
				toppingStatement.setBoolean(3, t.getDoubled());
				toppingStatement.executeUpdate();
				String updateQuery = "UPDATE topping SET topping_CurINVT = topping_CurINVT - ? WHERE topping_TopID = ?;";
				PreparedStatement updateStatement = conn.prepareStatement(updateQuery);
				double num_toppings = 0.0;
				switch (p.getSize()) {
					case size_s:
						num_toppings = t.getSmallAMT();
						break;
					case size_m:
						num_toppings = t.getMedAMT();
						break;
					case size_l:
						num_toppings = t.getLgAMT();
						break;
					case size_xl:
						num_toppings = t.getXLAMT();
						break;
				}
				if(t.getDoubled()){
					num_toppings *= 2;
				}
				num_toppings = Math.ceil(num_toppings);
				updateStatement.setDouble(1, num_toppings);
				updateStatement.setInt(2, t.getTopID());
				updateStatement.executeUpdate();
					}
				String discountQuery = "INSERT INTO pizza_discount (pizza_PizzaID, discount_DiscountID) VALUES (?, ?);";
				PreparedStatement discountStatement = conn.prepareStatement(discountQuery);
				for(Discount discount : p.getDiscounts()){
					discountStatement.setInt(1, pizzaID);
					discountStatement.setInt(2, discount.getDiscountID());
					discountStatement.executeUpdate();
				}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		} finally {
			if(createdLocalConnection){
				conn.close();
			}
		}
		return pizzaID;
	}
	
	public static int addCustomer(Customer c) throws SQLException, IOException
	 {
		int customerID = -1;
		try {
			connect_to_db();
			String customerQuery = "INSERT INTO customer (customer_FName, customer_LName, customer_PhoneNum) VALUES (?, ?, ?);";
			PreparedStatement customerStatement = conn.prepareStatement(customerQuery, Statement.RETURN_GENERATED_KEYS);
			customerStatement.setString(1, c.getFName());
			customerStatement.setString(2, c.getLName());
			customerStatement.setString(3, c.getPhone());
			customerStatement.executeUpdate();
			ResultSet generatedKey = customerStatement.getGeneratedKeys();
			if(generatedKey.next()){
				customerID = generatedKey.getInt(1);
			} else {
				throw new SQLException("Failed to retrieve Customer ID");
			}
		} catch (IOException | SQLException e){
			e.printStackTrace();
		}
		conn.close();
		return customerID;
	}

	public static void completeOrder(int OrderID, order_state newState ) throws SQLException, IOException
	{
		try {
			connect_to_db();
			switch (newState) {
				case PREPARED:
					String preparedOrderQuery = "UPDATE ordertable SET ordertable_isComplete = 1 WHERE ordertable_OrderID = ?;";
					String preparedPizzaQuery = "Update pizza SET pizza_PizzaState = 'completed' WHERE ordertable_OrderID = ?;";
					PreparedStatement preparedOrderStatement = conn.prepareStatement(preparedOrderQuery);
					preparedOrderStatement.setInt(1, OrderID);
					preparedOrderStatement.executeUpdate();
					PreparedStatement preparedPizzaStatement = conn.prepareStatement(preparedPizzaQuery);
					preparedPizzaStatement.setInt(1, OrderID);
					preparedPizzaStatement.executeUpdate();
					break;
				case DELIVERED:
					String deliveredQuery = "UPDATE delivery SET delivery_isDelivered = 1 WHERE ordertable_OrderID = ?;";
					PreparedStatement deliveredStatement = conn.prepareStatement(deliveredQuery);
					deliveredStatement.setInt(1, OrderID);
					deliveredStatement.executeUpdate();
					break;
				case PICKEDUP:
					String pickedupQuery = "UPDATE pickup SET pickup_IsPickedUp = 1 WHERE ordertable_OrderID = ?;";
					PreparedStatement pickupStatement = conn.prepareStatement(pickedupQuery);
					pickupStatement.setInt(1, OrderID);
					pickupStatement.executeUpdate();
					break;
			}
		} catch(SQLException | IOException e) {
			e.printStackTrace();
		}
		conn.close();
	}


	public static ArrayList<Order> getOrders(int status) throws SQLException, IOException
	 {
		 ArrayList<Order> orders = new ArrayList<>();
		 try{
			 connect_to_db();
			 String orderQuery;
			 switch (status) {
				 case 1:
					 orderQuery = "SELECT * FROM ordertable WHERE ordertable_isComplete = 0;";
					 break;
				 case 2:
					 orderQuery = "SELECT * FROM ordertable WHERE ordertable_isComplete = 1;";
					 break;
				 case 3:
					 orderQuery = "SELECT * FROM ordertable;";
					 break;
				 default:
					 throw new IllegalArgumentException("Invalid status: " + status);
			 }
			 PreparedStatement orderStatement = conn.prepareStatement(orderQuery);
			 ResultSet orderResultSet = orderStatement.executeQuery();
			 while(orderResultSet.next()) {
				 int orderID = orderResultSet.getInt("ordertable_OrderID");
				 int customerID = orderResultSet.getInt("customer_CustID");
				 String orderType = orderResultSet.getString("ordertable_OrderType");
				 String orderDate = orderResultSet.getString("ordertable_OrderDateTime");
				 double orderPrice = orderResultSet.getDouble("ordertable_CustPrice");
				 double orderCost = orderResultSet.getDouble("ordertable_BusPrice");
				 boolean isComplete = orderResultSet.getBoolean("ordertable_isComplete");
				 Order o = null;
				 switch (orderType) {
					 case pickup:
						 String pickupQuery = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?;";
						 PreparedStatement pickupStatement = conn.prepareStatement(pickupQuery);
						 pickupStatement.setInt(1, orderID);
						 ResultSet pickupRs = pickupStatement.executeQuery();
						 if (pickupRs.next()) {
							 boolean pickedUp = pickupRs.getBoolean("pickup_IsPickedUp");
							 o = new PickupOrder(orderID, customerID, orderDate, orderPrice, orderCost, pickedUp, isComplete);
						 } else {
							 o = null;
						 }
						 break;
					 case dine_in:
						 String dineinQuery = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?;";
						 PreparedStatement dineinStatement = conn.prepareStatement(dineinQuery);
						 dineinStatement.setInt(1, orderID);
						 ResultSet dineinRs = dineinStatement.executeQuery();
						 if (dineinRs.next()) {
							 int tableNum = dineinRs.getInt("dinein_TableNum");
							 o = new DineinOrder(orderID, customerID, orderDate, orderPrice, orderCost, isComplete, tableNum);
						 } else {
							 o = null;
						 }
						 break;
					 case delivery:
						 String deliveryQuery = "SELECT delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered FROM delivery WHERE ordertable_OrderID = ?;";
						 PreparedStatement deliveryStatement = conn.prepareStatement(deliveryQuery);
						 deliveryStatement.setInt(1, orderID);
						 ResultSet deliveryRs = deliveryStatement.executeQuery();
						 if (deliveryRs.next()) {
							 String address = deliveryRs.getInt("delivery_HouseNum") + "\t" + deliveryRs.getString("delivery_Street") + "\t" + deliveryRs.getString("delivery_City") + "\t" + deliveryRs.getString("delivery_State") + "\t" + deliveryRs.getInt("delivery_Zip");
							 boolean isDelivered = deliveryRs.getBoolean("delivery_isDelivered");
							 o = new DeliveryOrder(orderID, customerID, orderDate, orderPrice, orderCost, isComplete, isDelivered, address);
						 }
				 }
					 if(o != null){
						 ArrayList<Pizza> pizzas = getPizzas(o);
						 o.setPizzaList(pizzas);
						 ArrayList<Discount> discounts = getDiscounts(o);
						 o.setDiscountList(discounts);
						 orders.add(o);
					 }
			 }
		 } catch (SQLException | IOException e) {
			 e.printStackTrace();
		 }
		 conn.close();
		 return orders;
	}
	
	public static Order getLastOrder() throws SQLException, IOException 
	{
		Order o = null;
		try{
			connect_to_db();
			String orderQuery = "SELECT * FROM ordertable ORDER BY ordertable_OrderID DESC LIMIT 1;";
			PreparedStatement orderStatement = conn.prepareStatement(orderQuery);
			ResultSet orderRs = orderStatement.executeQuery();
			if(orderRs.next()){
				int orderID = orderRs.getInt("ordertable_OrderID");
				int customerID = orderRs.getInt("customer_CustID");
				String orderType = orderRs.getString("ordertable_OrderType");
				String orderDate = orderRs.getString("ordertable_OrderDateTime");
				double orderPrice = orderRs.getDouble("ordertable_CustPrice");
				double orderCost = orderRs.getDouble("ordertable_BusPrice");
				boolean isComplete = orderRs.getBoolean("ordertable_isComplete");
				switch (orderType) {
					case pickup:
						String pickupQuery = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?;";
						PreparedStatement pickupStatement = conn.prepareStatement(pickupQuery);
						pickupStatement.setInt(1, orderID);
						ResultSet pickupRs = pickupStatement.executeQuery();
						if (pickupRs.next()) {
							boolean pickedUp = pickupRs.getBoolean("pickup_IsPickedUp");
							o = new PickupOrder(orderID, customerID, orderDate, orderPrice, orderCost, pickedUp, isComplete);
						}
						break;
					case dine_in:
						String dineinQuery = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?";
						PreparedStatement dineinStatement = conn.prepareStatement(dineinQuery);
						dineinStatement.setInt(1, orderID);
						ResultSet dineinRs = dineinStatement.executeQuery();
						if (dineinRs.next()) {
							int tableNum = dineinRs.getInt("dinein_TableNum");
							o = new DineinOrder(orderID, customerID, orderDate, orderPrice, orderCost, isComplete, tableNum);
						}
						break;
					case delivery:
						String deliveryQuery = "SELECT delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered FROM delivery WHERE ordertable_OrderID = ?;";
						PreparedStatement deliveryStatement = conn.prepareStatement(deliveryQuery);
						deliveryStatement.setInt(1, orderID);
						ResultSet deliveryRs = deliveryStatement.executeQuery();
						if (deliveryRs.next()) {
							String address = deliveryRs.getInt("delivery_HouseNum") + "\t" + deliveryRs.getString("delivery_Street") + "\t" + deliveryRs.getString("delivery_City") + "\t" + deliveryRs.getString("delivery_State") + "\t" + deliveryRs.getInt("delivery_Zip");
							boolean isDelivered = deliveryRs.getBoolean("delivery_isDelivered");
							o = new DeliveryOrder(orderID, customerID, orderDate, orderPrice, orderCost, isComplete, isDelivered, address);
						}
						break;
				}

				if(o != null){
					ArrayList<Pizza> pizzas = getPizzas(o);
					o.setPizzaList(pizzas);
					ArrayList<Discount> discounts = getDiscounts(o);
					o.setDiscountList(discounts);
				}
			}

		} catch (SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
		return o;
	}

	public static ArrayList<Order> getOrdersByDate(String date) throws SQLException, IOException
	 {
		ArrayList<Order> orders = new ArrayList<>();
		try{
			connect_to_db();
			String orderQuery = "SELECT * FROM ordertable WHERE DATE(ordertable_OrderDateTime) = ? ORDER BY ordertable_OrderDateTime;";
			PreparedStatement orderStatement = conn.prepareStatement(orderQuery);
			orderStatement.setString(1, date);
			ResultSet orderRs = orderStatement.executeQuery();
			while (orderRs.next()) {
				int orderID = orderRs.getInt("ordertable_OrderID");
				int customerID = orderRs.getInt("customer_CustID");
				String orderType = orderRs.getString("ordertable_OrderType");
				String orderDate = orderRs.getString("ordertable_OrderDateTime");
				double orderPrice = orderRs.getDouble("ordertable_CustPrice");
				double orderCost = orderRs.getDouble("ordertable_BusPrice");
				boolean isComplete = orderRs.getBoolean("ordertable_isComplete");
				Order o = null;
				switch (orderType) {
					case pickup:
						String pickupQuery = "SELECT pickup_IsPickedUp FROM pickup WHERE ordertable_OrderID = ?;";
						PreparedStatement pickupStatement = conn.prepareStatement(pickupQuery);
						pickupStatement.setInt(1, orderID);
						ResultSet pickupRs = pickupStatement.executeQuery();
						if (pickupRs.next()) {
							boolean pickedUp = pickupRs.getBoolean("pickup_IsPickedUp");
							o = new PickupOrder(orderID, customerID, orderDate, orderPrice, orderCost, pickedUp, isComplete);
						}
						break;
					case dine_in:
						String dineinQuery = "SELECT dinein_TableNum FROM dinein WHERE ordertable_OrderID = ?;";
						PreparedStatement dineinStatement = conn.prepareStatement(dineinQuery);
						dineinStatement.setInt(1, orderID);
						ResultSet dineinRs = dineinStatement.executeQuery();
						if (dineinRs.next()) {
							int tableNum = dineinRs.getInt("dinein_TableNum");
							o = new DineinOrder(orderID, customerID, orderDate, orderPrice, orderCost, isComplete, tableNum);
						}
						break;
					case delivery:
						String deliveryQuery = "SELECT delivery_HouseNum, delivery_Street, delivery_City, delivery_State, delivery_Zip, delivery_IsDelivered FROM delivery WHERE ordertable_OrderID = ?;";
						PreparedStatement deliveryStatement = conn.prepareStatement(deliveryQuery);
						deliveryStatement.setInt(1, orderID);
						ResultSet deliveryRs = deliveryStatement.executeQuery();
						if (deliveryRs.next()) {
							String address = deliveryRs.getInt("delivery_HouseNum") + "\t" + deliveryRs.getString("delivery_Street") + "\t" + deliveryRs.getString("delivery_City") + "\t" + deliveryRs.getString("delivery_State") + "\t" + deliveryRs.getInt("delivery_Zip");
							boolean isDelivered = deliveryRs.getBoolean("delivery_isDelivered");
							o = new DeliveryOrder(orderID, customerID, orderDate, orderPrice, orderCost, isComplete, isDelivered, address);
						}
				}
				if(o != null){
					ArrayList<Pizza> pizzas = getPizzas(o);
					o.setPizzaList(pizzas);
					ArrayList<Discount> discounts = getDiscounts(o);
					o.setDiscountList(discounts);
					orders.add(o);
				}
			}
		} catch(SQLException | IOException e){
			e.printStackTrace();
			throw e;
		}
		conn.close();
		return orders;
	}
		
	public static ArrayList<Discount> getDiscountList() throws SQLException, IOException 
	{
		ArrayList<Discount> discounts = new ArrayList<>();
		try{
			connect_to_db();
			String discountQuery = "SELECT * FROM discount ORDER BY discount_DiscountName;";
			PreparedStatement discountStatement = conn.prepareStatement(discountQuery);
			ResultSet discountRs = discountStatement.executeQuery();
			while(discountRs.next()){
				int discountID = discountRs.getInt("discount_DiscountID");
				String discountName = discountRs.getString("discount_DiscountName");
				double discountAmount = discountRs.getDouble("discount_Amount");
				boolean isPercent = discountRs.getBoolean("discount_IsPercent");
				Discount discount = new Discount(discountID, discountName, discountAmount, isPercent);
				discounts.add(discount);
			}

		} catch(SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
		return discounts;
	}

	public static Discount findDiscountByName(String name) throws SQLException, IOException 
	{
		Discount discount = null;
		try{
			connect_to_db();
			String discountQuery = "SELECT * FROM discount WHERE discount_DiscountName = ?;";
			PreparedStatement discountStatement = conn.prepareStatement(discountQuery);
			discountStatement.setString(1, name);
			ResultSet discountRs = discountStatement.executeQuery();
			if(discountRs.next()){
				int discountID = discountRs.getInt("discount_DiscountID");
				String discountName = discountRs.getString("discount_DiscountName");
				double discountAmount = discountRs.getDouble("discount_Amount");
				boolean isPercent = discountRs.getBoolean("discount_IsPercent");
				discount = new Discount(discountID, discountName, discountAmount, isPercent);
			}
		} catch(SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
		return discount;
	}


	public static ArrayList<Customer> getCustomerList() throws SQLException, IOException 
	{
		ArrayList<Customer> customers = new ArrayList<>();
		try{
			connect_to_db();
			String customerQuery = "SELECT * FROM customer ORDER BY customer_LName, customer_FName, customer_PhoneNum;";
			PreparedStatement customerStatement = conn.prepareStatement(customerQuery);
			ResultSet customerRs = customerStatement.executeQuery();
			while(customerRs.next()){
				int customerID = customerRs.getInt("customer_CustID");
				String firstName = customerRs.getString("customer_FName");
				String lastName = customerRs.getString("customer_LName");
				String phone = customerRs.getString("customer_PhoneNum");
				Customer customer = new Customer(customerID, firstName, lastName, phone);
				customers.add(customer);
			}
		} catch(SQLException | IOException e){
			e.printStackTrace();
			throw e;
		}
		conn.close();
		return customers;
	}

	public static Customer findCustomerByPhone(String phoneNumber)  throws SQLException, IOException 
	{
		Customer customer = null;
		try{
			connect_to_db();
			String customerQuery = "SELECT * FROM customer WHERE customer_PhoneNum = ?;";
			PreparedStatement customerStatement = conn.prepareStatement(customerQuery);
			customerStatement.setString(1, phoneNumber);
			ResultSet customerRs = customerStatement.executeQuery();
			if(customerRs.next()){
				int customerID = customerRs.getInt("customer_CustID");
				String firstName = customerRs.getString("customer_FName");
				String lastName = customerRs.getString("customer_LName");
				String phone = customerRs.getString("customer_PhoneNum");
				customer = new Customer(customerID, firstName, lastName, phone);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		}
		 return customer;
	}

	public static String getCustomerName(int CustID) throws SQLException, IOException 
	{
		/*
		 * COMPLETED...WORKING Example!
		 * 
		 * This is a helper method to fetch and format the name of a customer
		 * based on a customer ID. This is an example of how to interact with
		 * your database from Java.  
		 * 
		 * Notice how the connection to the DB made at the start of the 
		 *
		 */

		 connect_to_db();

		/* 
		 * an example query using a constructed string...
		 * remember, this style of query construction could be subject to sql injection attacks!
		 * 
		 */
		String cname1 = "";
		String cname2 = "";
		String query = "Select customer_FName, customer_LName From customer WHERE customer_CustID=" + CustID + ";";
		Statement stmt = conn.createStatement();
		ResultSet rset = stmt.executeQuery(query);
		
		while(rset.next())
		{
			cname1 = rset.getString(1) + " " + rset.getString(2); 
		}

		/* 
		* an BETTER example of the same query using a prepared statement...
		* with exception handling
		* 
		*/
		try {
			PreparedStatement os;
			ResultSet rset2;
			String query2;
			query2 = "Select customer_FName, customer_LName From customer WHERE customer_CustID=?;";
			os = conn.prepareStatement(query2);
			os.setInt(1, CustID);
			rset2 = os.executeQuery();
			while(rset2.next())
			{
				cname2 = rset2.getString("customer_FName") + " " + rset2.getString("customer_LName"); // note the use of field names in the getSting methods
			}
		} catch (SQLException e) {
			e.printStackTrace();
			// process the error or re-raise the exception to a higher level
		}

		conn.close();

		return cname1;
		// OR
		// return cname2;

	}


	public static ArrayList<Topping> getToppingList() throws SQLException, IOException 
	{
		ArrayList<Topping> toppings = new ArrayList<>();
		try{
			connect_to_db();
			String toppingQuery = "SELECT * FROM topping ORDER BY topping_TopName;";
			PreparedStatement toppingStatement = conn.prepareStatement(toppingQuery);
			ResultSet toppingRs = toppingStatement.executeQuery();
			while(toppingRs.next()){
				int toppingID = toppingRs.getInt("topping_TopID");
				String toppingName = toppingRs.getString("topping_TopName");
				double small = toppingRs.getDouble("topping_SmallAMT");
				double medium = toppingRs.getDouble("topping_MedAMT");
				double large = toppingRs.getDouble("topping_LgAMT");
				double xl = toppingRs.getDouble("topping_XLAMT");
				double price = toppingRs.getDouble("topping_CustPrice");
				double cost = toppingRs.getDouble("topping_BusPrice");
				int min = toppingRs.getInt("topping_MinINVT");
				int current = toppingRs.getInt("topping_CurINVT");
				Topping topping = new Topping(toppingID, toppingName, small, medium, large, xl, price, cost, min, current);
				toppings.add(topping);
			}
		} catch(IOException | SQLException e) {
			e.printStackTrace();
		}
		conn.close();
		return toppings;
	}

	public static Topping findToppingByName(String name) throws SQLException, IOException 
	{
		Topping topping = null;
		try{
			connect_to_db();
			String toppingQuery = "SELECT * FROM topping WHERE topping_TopName = ?;";
			PreparedStatement toppingStatement = conn.prepareStatement(toppingQuery);
			toppingStatement.setString(1, name);
			ResultSet toppingRs = toppingStatement.executeQuery();
			if(toppingRs.next()){
				int toppingID = toppingRs.getInt("topping_TopID");
				String toppingName = toppingRs.getString("topping_TopName");
				double small = toppingRs.getDouble("topping_SmallAMT");
				double medium = toppingRs.getDouble("topping_MedAMT");
				double large = toppingRs.getDouble("topping_LgAMT");
				double xl = toppingRs.getDouble("topping_XLAMT");
				double price = toppingRs.getDouble("topping_CustPrice");
				double cost = toppingRs.getDouble("topping_BusPrice");
				int min = toppingRs.getInt("topping_MinINVT");
				int current = toppingRs.getInt("topping_CurINVT");
				topping = new Topping(toppingID, toppingName, small, medium, large, xl, price, cost, min, current);
			}
		} catch (IOException | SQLException e){
			e.printStackTrace();
		}
		conn.close();
		return topping;
	}

	public static ArrayList<Topping> getToppingsOnPizza(Pizza p) throws SQLException, IOException 
	{
		ArrayList<Topping> toppings = new ArrayList<>();
		boolean locallyConnected = false;
		try {
			if (conn.isClosed() || conn == null) {
				connect_to_db();
				locallyConnected = true;
			}
			String toppingQuery = "SELECT topping.topping_TopID, topping_TopName, topping_SmallAMT, topping_MedAMT, topping_LgAMT, topping_XLAMT, topping_CustPrice, topping_BusPrice, topping_MinINVT, topping_CurINVT, pizza_topping_IsDouble FROM topping JOIN pizza_topping ON topping.topping_TopID = pizza_topping.topping_TopID WHERE pizza_PizzaID = ?;";
			PreparedStatement toppingStatement = conn.prepareStatement(toppingQuery);
			toppingStatement.setInt(1, p.getPizzaID());
			ResultSet toppingRs = toppingStatement.executeQuery();
			while(toppingRs.next()){
				int toppingID = toppingRs.getInt("topping_TopID");
				String toppingName = toppingRs.getString("topping_TopName");
				double small = toppingRs.getDouble("topping_SmallAMT");
				double medium = toppingRs.getDouble("topping_MedAMT");
				double large = toppingRs.getDouble("topping_LgAMT");
				double xl = toppingRs.getDouble("topping_XLAMT");
				double price = toppingRs.getDouble("topping_CustPrice");
				double cost = toppingRs.getDouble("topping_BusPrice");
				int min = toppingRs.getInt("topping_MinINVT");
				int current = toppingRs.getInt("topping_CurINVT");
				boolean isDouble = toppingRs.getBoolean("pizza_topping_IsDouble");
				Topping topping = new Topping(toppingID, toppingName, small, medium, large, xl, price, cost, min, current);
				topping.setDoubled(isDouble);
				toppings.add(topping);
			}
		} catch (IOException | SQLException e){
			e.printStackTrace();
		} finally {
			if(locallyConnected){
				conn.close();
			}
		}
		return toppings;
	}

	public static void addToInventory(int toppingID, double quantity) throws SQLException, IOException 
	{
		try{
			connect_to_db();
			String updateQuery = "UPDATE topping SET topping_CurINVT = topping_CurINVT + ? WHERE topping_TopID = ?";
			PreparedStatement updateStatement = conn.prepareStatement(updateQuery);
			updateStatement.setDouble(1, quantity);
			updateStatement.setInt(2, toppingID);
			updateStatement.executeUpdate();
		} catch(SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
	}
	
	
	public static ArrayList<Pizza> getPizzas(Order o) throws SQLException, IOException 
	{
		ArrayList<Pizza> pizzas = new ArrayList<>();
		boolean locallyConnected = false;
		try{
			if(conn.isClosed() || conn == null) {
				connect_to_db();
				locallyConnected = true;
			}
			String pizzaQuery = "SELECT * FROM pizza WHERE ordertable_OrderID = ?;";
			PreparedStatement pizzaStatement = conn.prepareStatement(pizzaQuery);
			pizzaStatement.setInt(1, o.getOrderID());
			ResultSet pizzaRs = pizzaStatement.executeQuery();
			while(pizzaRs.next()){
				int pizzaID = pizzaRs.getInt("pizza_PizzaID");
				String pizzaSize = pizzaRs.getString("pizza_Size");
				String crustType = pizzaRs.getString("pizza_CrustType");
				String state = pizzaRs.getString("pizza_PizzaState");
				String pizzaDate = pizzaRs.getString("pizza_PizzaDate");
				double price = pizzaRs.getDouble("pizza_CustPrice");
				double cost = pizzaRs.getDouble("pizza_BusPrice");
				int orderID = pizzaRs.getInt("ordertable_OrderID");
				Pizza pizza = new Pizza(pizzaID, pizzaSize, crustType, orderID, state, pizzaDate, price, cost);
				ArrayList<Topping> toppings = getToppingsOnPizza(pizza);
				pizza.setToppings(toppings);
				ArrayList<Discount> discounts = getDiscounts(pizza);
				pizza.setDiscounts(discounts);
				pizzas.add(pizza);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		}finally {
			if(locallyConnected) {
				conn.close();
			}
		}
		return pizzas;
	}

	public static ArrayList<Discount> getDiscounts(Order o) throws SQLException, IOException 
	{
		ArrayList<Discount> discounts = new ArrayList<>();
		boolean locallyConnected = false;
		try{
			if(conn.isClosed() || conn == null) {
				connect_to_db();
				locallyConnected = true;
			}
			String discountQuery = "SELECT discount.discount_DiscountID, discount_DiscountName, discount_Amount, discount_IsPercent FROM discount JOIN order_discount ON discount.discount_DiscountID = order_discount.discount_DiscountID WHERE ordertable_OrderID = ?;";
			PreparedStatement discountStatement = conn.prepareStatement(discountQuery);
			discountStatement.setInt(1, o.getOrderID());
			ResultSet discountRs = discountStatement.executeQuery();
			while(discountRs.next()){
				int discountID = discountRs.getInt("discount_DiscountID");
				String discountName = discountRs.getString("discount_DiscountName");
				double discountAmount = discountRs.getDouble("discount_Amount");
				boolean isPercent = discountRs.getBoolean("discount_IsPercent");
				Discount discount = new Discount(discountID, discountName, discountAmount, isPercent);
				discounts.add(discount);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		} finally {
			if(locallyConnected){
				conn.close();
			}
		}
		return discounts;
	}

	public static ArrayList<Discount> getDiscounts(Pizza p) throws SQLException, IOException 
	{
		ArrayList<Discount> discounts = new ArrayList<>();
		boolean locallyConnected = false;
		try {
			if(conn.isClosed() || conn == null) {
				connect_to_db();
				locallyConnected = true;
			}
			String discountQuery = "SELECT discount.discount_DiscountID, discount_DiscountName, discount_Amount, discount_IsPercent FROM discount JOIN pizza_discount ON discount.discount_DiscountID = pizza_discount.discount_DiscountID WHERE pizza_PizzaID = ?;";
			PreparedStatement discountStatement = conn.prepareStatement(discountQuery);
			discountStatement.setInt(1, p.getPizzaID());
			ResultSet discountRs = discountStatement.executeQuery();
			while(discountRs.next()){
				int discountID = discountRs.getInt("discount_DiscountID");
				String discountName = discountRs.getString("discount_DiscountName");
				double discountAmount = discountRs.getDouble("discount_Amount");
				boolean isPercent = discountRs.getBoolean("discount_IsPercent");
				Discount discount = new Discount(discountID, discountName, discountAmount, isPercent);
				discounts.add(discount);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		} finally {
			if(locallyConnected){
				conn.close();
			}
		}
		return discounts;
	}

	public static double getBaseCustPrice(String size, String crust) throws SQLException, IOException 
	{
		double basePrice = 0.0;
		try{
			connect_to_db();
			String priceQuery =  "SELECT baseprice_CustPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?;";
			PreparedStatement priceStatement = conn.prepareStatement(priceQuery);
			priceStatement.setString(1, size);
			priceStatement.setString(2, crust);
			ResultSet priceRs = priceStatement.executeQuery();
			if(priceRs.next()){
				basePrice = priceRs.getDouble("baseprice_CustPrice");
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		}
		return basePrice;
	}

	public static double getBaseBusPrice(String size, String crust) throws SQLException, IOException 
	{
		double baseCost = 0.0;
		try{
			connect_to_db();
			String priceQuery = "SELECT baseprice_BusPrice FROM baseprice WHERE baseprice_Size = ? AND baseprice_CrustType = ?;";
			PreparedStatement priceStatement = conn.prepareStatement(priceQuery);
			priceStatement.setString(1, size);
			priceStatement.setString(2, crust);
			ResultSet priceRs = priceStatement.executeQuery();
			if(priceRs.next()){
				baseCost = priceRs.getDouble("baseprice_BusPrice");
			}
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}
		return baseCost;
	}

	
	public static void printToppingReport() throws SQLException, IOException
	{
		try{
			connect_to_db();
			String toppingViewQuery = "SELECT * FROM  ToppingPopularity;";
			System.out.printf("%-20s%s\n", "Topping", "Topping Count");
			System.out.printf("%-20s%s\n", "-------", "-------------");
			PreparedStatement toppingViewStatement = conn.prepareStatement(toppingViewQuery);
			ResultSet toppingViewRs = toppingViewStatement.executeQuery();
			while(toppingViewRs.next()){
				String topping = toppingViewRs.getString("Topping");
				int count = toppingViewRs.getInt("ToppingCount");
				System.out.printf("%-20s%d\n", topping, count);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
	}
	
	public static void printProfitByPizzaReport() throws SQLException, IOException 
	{
		try {
			connect_to_db();
			String profitQuery = "SELECT * FROM ProfitByPizza;";
			System.out.printf("%-20s%-20s%-20s%s\n", "Pizza Size", "Pizza Crust", "Profit", "Last Order Date");
			System.out.printf("%-20s%-20s%-20s%s\n", "----------", "-----------", "------", "---------------");
			PreparedStatement profitStatement = conn.prepareStatement(profitQuery);
			ResultSet profitRs = profitStatement.executeQuery();
			while(profitRs.next()){
				String size = profitRs.getString("Size");
				String crust = profitRs.getString("Crust");
				double profit = profitRs.getDouble("Profit");
				String orderMonth = profitRs.getString("OrderMonth");
				System.out.printf("%-20s%-20s%-20.2f%s\n", size, crust, profit, orderMonth);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
	}
	
	public static void printProfitByOrderTypeReport() throws SQLException, IOException
	{
		try {
			connect_to_db();
			String profitQuery = "SELECT * FROM ProfitByOrderType;";
			System.out.printf("%-20s%-20s%-20s%-20s%s\n", "Customer Type", "Order Month", "Total Order Price", "Total Order Cost", "Profit");
			System.out.printf("%-20s%-20s%-20s%-20s%s\n", "-------------", "-----------", "-----------------", "----------------", "------");
			PreparedStatement profitStatement = conn.prepareStatement(profitQuery);
			ResultSet profitRs = profitStatement.executeQuery();
			while(profitRs.next()){
				String orderType = profitRs.getString("customerType");
				String orderMonth = profitRs.getString("OrderMonth");
				double orderPrice = profitRs.getDouble("TotalOrderPrice");
				double orderCost = profitRs.getDouble("TotalOrderCost");
				double profit = profitRs.getDouble("Profit");
				System.out.printf("%-20s%-20s%-20.2f%-20.2f%.2f\n", (orderType == null) ? " " : orderType, orderMonth, orderPrice, orderCost, profit);
			}
		} catch (SQLException | IOException e){
			e.printStackTrace();
		}
		conn.close();
	}
	
	
	
	/*
	 * These private methods help get the individual components of an SQL datetime object. 
	 * You're welcome to keep them or remove them....but they are usefull!
	 */
	private static int getYear(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(0,4));
	}
	private static int getMonth(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(5, 7));
	}
	private static int getDay(String date)// assumes date format 'YYYY-MM-DD HH:mm:ss'
	{
		return Integer.parseInt(date.substring(8, 10));
	}

	public static boolean checkDate(int year, int month, int day, String dateOfOrder)
	{
		if(getYear(dateOfOrder) > year)
			return true;
		else if(getYear(dateOfOrder) < year)
			return false;
		else
		{
			if(getMonth(dateOfOrder) > month)
				return true;
			else if(getMonth(dateOfOrder) < month)
				return false;
			else
			{
				if(getDay(dateOfOrder) >= day)
					return true;
				else
					return false;
			}
		}
	}
}