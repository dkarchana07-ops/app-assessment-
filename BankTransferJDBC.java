import java.sql.*;

public class BankTransferJDBC {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/studentdb";
        String user = "root";
        String pass = "archana";

        // Details for the transfer
        int senderAcc = 101;
        int receiverAcc = 102;
        double transferAmount = 2000.0;

        Connection con = null;

        try {
            // 1. Load Driver & Connect
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);

            // 2. Setup Bank Accounts Table
            Statement stmt = con.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS bank_account (" +
                               "acc_no INT PRIMARY KEY, " +
                               "holder_name VARCHAR(50), " +
                               "balance DOUBLE)");

            // Populate initial test accounts
            stmt.executeUpdate("REPLACE INTO bank_account VALUES (101, 'Archana', 10000.0)");
            stmt.executeUpdate("REPLACE INTO bank_account VALUES (102, 'Priya', 5000.0)");

            // Display balances before transfer
            System.out.println("--- Balances Before Transfer ---");
            displayBalances(con);

            // 3. START TRANSACTION (Disable auto-commit for atomicity)
            con.setAutoCommit(false);

            // 4. Verify Sender Account & Check Sufficient Balance
            String checkSenderSql = "SELECT balance FROM bank_account WHERE acc_no = ?";
            PreparedStatement pstmtSender = con.prepareStatement(checkSenderSql);
            pstmtSender.setInt(1, senderAcc);
            ResultSet rsSender = pstmtSender.executeQuery();

            if (!rsSender.next()) {
                System.out.println("Error: Sender account not found!");
                con.rollback();
                return;
            }

            double senderBalance = rsSender.getDouble("balance");
            if (senderBalance < transferAmount) {
                System.out.println("Error: Insufficient funds in sender account!");
                con.rollback();
                return;
            }

            // 5. Verify Receiver Account
            String checkRecSql = "SELECT balance FROM bank_account WHERE acc_no = ?";
            PreparedStatement pstmtRec = con.prepareStatement(checkRecSql);
            pstmtRec.setInt(1, receiverAcc);
            ResultSet rsRec = pstmtRec.executeQuery();

            if (!rsRec.next()) {
                System.out.println("Error: Receiver account not found!");
                con.rollback();
                return;
            }

            // 6. Debit Sender Account
            String debitSql = "UPDATE bank_account SET balance = balance - ? WHERE acc_no = ?";
            PreparedStatement pstmtDebit = con.prepareStatement(debitSql);
            pstmtDebit.setDouble(1, transferAmount);
            pstmtDebit.setInt(2, senderAcc);
            pstmtDebit.executeUpdate();

            // 7. Credit Receiver Account
            String creditSql = "UPDATE bank_account SET balance = balance + ? WHERE acc_no = ?";
            PreparedStatement pstmtCredit = con.prepareStatement(creditSql);
            pstmtCredit.setDouble(1, transferAmount);
            pstmtCredit.setInt(2, receiverAcc);
            pstmtCredit.executeUpdate();

            // 8. Commit Transaction (Permanently save changes)
            con.commit();
            System.out.println("\nTransfer of Rs. " + transferAmount + " completed successfully!");

            // Reset auto-commit back to default
            con.setAutoCommit(true);

            // Display balances after transfer
            System.out.println("\n--- Balances After Transfer ---");
            displayBalances(con);

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
            try {
                if (con != null) {
                    con.rollback(); // Rollback if any unexpected SQL error occurs
                    System.out.println("Rollback executed: All accounts restored to previous state.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper method to display current balances
    private static void displayBalances(Connection con) throws SQLException {
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT * FROM bank_account");
        while (rs.next()) {
            System.out.println("Acc: " + rs.getInt("acc_no") + 
                               " | Name: " + rs.getString("holder_name") + 
                               " | Balance: Rs. " + rs.getDouble("balance"));
        }
    }
}