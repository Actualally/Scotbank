package uk.co.asepstrath.bank.services;

import org.slf4j.Logger;
import uk.co.asepstrath.bank.models.Transaction;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TransactionProcessor {

    private final Logger log;

    public TransactionProcessor(Logger log) {
        this.log = log;
    }

    public void applyTransaction(Transaction t,
                                 PreparedStatement balanceStmt,
                                 PreparedStatement holdingGet,
                                 PreparedStatement holdingMerge) throws SQLException {
        String investorId = t.getInvestorId().toString();
        double amount = t.getTotalCashAmount();

        switch (t.getType()) {
            case "DEPOSIT":
                adjustBalance(balanceStmt, investorId, amount);
                break;
            case "WITHDRAW":
                adjustBalance(balanceStmt, investorId, -amount);
                break;
            case "BUY":
                adjustBalance(balanceStmt, investorId, -amount);
                updateHolding(holdingGet, holdingMerge, investorId, t.getTicker(), amount, true);
                break;
            case "SELL":
                adjustBalance(balanceStmt, investorId, amount);
                updateHolding(holdingGet, holdingMerge, investorId, t.getTicker(), amount, false);
                break;
            default:
                log.warn("Unknown transaction type: {}", t.getType());
        }
    }

    private void adjustBalance(PreparedStatement stmt, String investorId, double delta)
            throws SQLException {
        stmt.setBigDecimal(1, BigDecimal.valueOf(delta));
        stmt.setString(2, investorId);
        stmt.executeUpdate();
    }

    private void updateHolding(PreparedStatement getStmt, PreparedStatement mergeStmt,
                               String investorId, String ticker, double amount, boolean isBuy)
            throws SQLException {
        getStmt.setString(1, investorId);
        getStmt.setString(2, ticker);

        int currentShares = 0;
        double currentCost = 0.0;

        try (ResultSet rs = getStmt.executeQuery()) {
            if (rs.next()) {
                currentShares = rs.getInt("Shares");
                currentCost = rs.getDouble("TotalCost");
            }
        }

        if (isBuy) {
            currentShares += 1;
            currentCost += amount;
        } else {
            currentShares = Math.max(0, currentShares - 1);
            currentCost = Math.max(0.0, currentCost - amount);
        }

        mergeStmt.setString(1, investorId);
        mergeStmt.setString(2, ticker);
        mergeStmt.setInt(3, currentShares);
        mergeStmt.setBigDecimal(4, BigDecimal.valueOf(currentCost));
        mergeStmt.executeUpdate();
    }
}
