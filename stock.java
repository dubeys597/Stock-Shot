import java.sql.*;

public class StockBroker {
    private Connection conn;
    
    public StockBroker() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost/stockbroker", "root", "password");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Login with broker account
    public boolean login(String username, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM broker WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Register new customer
    public boolean registerCustomer(String username, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO customer (username, password, wallet) VALUES (?, ?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setDouble(3, 0.0);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // View all customers
    public void viewAllCustomers() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM customer");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Username: " + rs.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Add new stocks
    public boolean addStock(String symbol, int quantity) {
        try {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO stock (symbol, quantity) VALUES (?, ?)");
            stmt.setString(1, symbol);
            stmt.setInt(2, quantity);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // View all stocks
    public void viewAllStocks() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM stock");
            while (rs.next()) {
                System.out.println("Symbol: " + rs.getString("symbol") + ", Quantity: " + rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // View consolidated report of a stock
    public void viewStockReport(String symbol) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT SUM(quantity) AS sold, (SELECT quantity FROM stock WHERE symbol = ?) - SUM(quantity) AS remaining FROM transaction WHERE symbol = ?");
            stmt.setString(1, symbol);
            stmt.setString(2, symbol);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Sold: " + rs.getInt("sold") + ", Remaining: " + rs.getInt("remaining"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Delete customer
    public boolean deleteCustomer(int customerId) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT SUM(amount) AS total FROM transaction WHERE customer_id = ?");
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            double total = 0.0;
            if (rs.next()) {
                total = rs.getDouble("total");
            }

            PreparedStatement stmt2 = conn.prepareStatement("UPDATE customer SET wallet = wallet + ? WHERE id = ?");
            stmt2.setDouble(1, total);
            stmt2.setInt(2, customerId);
            int rows1 = stmt2.executeUpdate();
            PreparedStatement stmt3 = conn.prepareStatement("UPDATE customer SET active = 0 WHERE id = ?");
            stmt3.setInt(1, customerId);
            int rows2 = stmt3.executeUpdate();
            if (rows1 > 0 && rows2 > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Delete stock
    public boolean deleteStock(String symbol) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT SUM(amount) AS total FROM transaction WHERE symbol = ?");
            stmt.setString(1, symbol);
            ResultSet rs = stmt.executeQuery();
            double total = 0.0;
            if (rs.next()) {
                total = rs.getDouble("total");
            }
            PreparedStatement stmt2 = conn.prepareStatement("UPDATE customer SET wallet = wallet + ? WHERE active = 1");
            stmt2.setDouble(1, total);
            int rows1 = stmt2.executeUpdate();
            PreparedStatement stmt3 = conn.prepareStatement("DELETE FROM stock WHERE symbol = ?");
            stmt3.setString(1, symbol);
            int rows2 = stmt3.executeUpdate();
            if (rows1 > 0 && rows2 > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Login with customer account
    public boolean loginCustomer(String username, String password) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM customer WHERE username = ? AND password = ? AND active = 1");
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // View all stocks
    public void viewAllStocksCustomer() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM stock");
            while (rs.next()) {
                System.out.println("Symbol: " + rs.getString("symbol") + ", Quantity: " + rs.getInt("quantity"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Buy stock
    public boolean buyStock(int customerId, String symbol, int quantity) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT quantity, price FROM stock WHERE symbol = ?");
            stmt.setString(1, symbol);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int stockQuantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                if (stockQuantity >= quantity) {
                    double amount = quantity * price;
                    PreparedStatement stmt2 = conn.prepareStatement("UPDATE customer SET wallet = wallet - ? WHERE id = ?");
                    stmt2.setDouble(1, amount);
                    stmt2.setInt(2, customerId);
                    int rows1 = stmt2.executeUpdate();
                    PreparedStatement stmt3 = conn.prepareStatement("UPDATE stock SET quantity = quantity - ? WHERE symbol = ?");
                    stmt3.setInt(1, quantity);
                    stmt3.setString(2, symbol);
                    int rows2 = stmt3.executeUpdate();
                    PreparedStatement stmt4 = conn.prepareStatement("INSERT INTO transaction (customer_id, symbol, quantity, price, amount, type) VALUES (?, ?, ?, ?, ?, 'BUY')");
        stmt4.setInt(1, customerId);
        stmt4.setString(2, symbol);
        stmt4.setInt(3, quantity);
        stmt4.setDouble(4, price);
        stmt4.setDouble(5, amount);
        int rows3 = stmt4.executeUpdate();
        if (rows1 > 0 && rows2 > 0 && rows3 > 0) {
        return true;
        }
        }
        }
        } catch (SQLException e) {
        e.printStackTrace();
        }
        return false;
        }

        java
        Copy code
        // Sell stock
        public boolean sellStock(int customerId, String symbol, int quantity) {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT quantity, price FROM stock WHERE symbol = ?");
                stmt.setString(1, symbol);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int stockQuantity = rs.getInt("quantity");
                    double price = rs.getDouble("price");
                    PreparedStatement stmt2 = conn.prepareStatement("SELECT * FROM transaction WHERE customer_id = ? AND symbol = ?");
                    stmt2.setInt(1, customerId);
                    stmt2.setString(2, symbol);
                    ResultSet rs2 = stmt2.executeQuery();
                    int totalQuantity = 0;
                    while (rs2.next()) {
                        if (rs2.getString("type").equals("BUY")) {
                            totalQuantity += rs2.getInt("quantity");
                        } else {
                            totalQuantity -= rs2.getInt("quantity");
                        }
                    }
                    if (totalQuantity >= quantity) {
                        double amount = quantity * price;
                        PreparedStatement stmt3 = conn.prepareStatement("UPDATE customer SET wallet = wallet + ? WHERE id = ?");
                        stmt3.setDouble(1, amount);
                        stmt3.setInt(2, customerId);
                        int rows1 = stmt3.executeUpdate();
                        PreparedStatement stmt4 = conn.prepareStatement("UPDATE stock SET quantity = quantity + ? WHERE symbol = ?");
                        stmt4.setInt(1, quantity);
                        stmt4.setString(2, symbol);
                        int rows2 = stmt4.executeUpdate();
                        PreparedStatement stmt5 = conn.prepareStatement("INSERT INTO transaction (customer_id, symbol, quantity, price, amount, type) VALUES (?, ?, ?, ?, ?, 'SELL')");
                        stmt5.setInt(1, customerId);
                        stmt5.setString(2, symbol);
                        stmt5.setInt(3, quantity);
                        stmt5.setDouble(4, price);
                        stmt5.setDouble(5, amount);
                        int rows3 = stmt5.executeUpdate();
                        if (rows1 > 0 && rows2 > 0 && rows3 > 0) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        // View customer transaction history
        public void viewTransactionHistory(int customerId) {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM transaction WHERE customer_id = ?");
                stmt.setInt(1, customerId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    System.out.println("Symbol: " + rs.getString("symbol") + ", Quantity: " + rs.getInt("quantity") + ", Price: " + rs.getDouble("price") + ", Amount: " + rs.getDouble("amount") + ", Type: " + rs.getString("type"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Add funds to wallet
        public boolean addFunds(int customerId, double amount) {
            try {
                PreparedStatement stmt = conn.prepareStatement("UPDATE customer SET wallet = wallet + ? WHERE id = ?");
                stmt.setDouble(1, amount);
                stmt.setInt(2, customerId);
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }

        // Withdraw funds from wallet
        public boolean withdrawFunds(int customerId, double amount) {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT wallet FROM customer WHERE id = ?");
                stmt.setInt(1, customerId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    double walletAmount = rs.getDouble("wallet");
                    if (walletAmount >= amount) {
                        PreparedStatement stmt2 = conn.prepareStatement("UPDATE customer SET wallet = wallet - ? WHERE id = ?");
                        stmt2.setDouble(1, amount);
                        stmt2.setInt(2, customerId);
                        int rows = stmt2.executeUpdate();
                        if (rows > 0) {
                            return true;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }
