package com.company.integrationplatform;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDbTest {
    @Test
    public void checkTables() throws Exception {
        String url = "jdbc:postgresql://ep-raspy-mode-a6hnrzpa.us-west-2.aws.neon.tech/neondb?sslmode=require";
        try (Connection conn = DriverManager.getConnection(url, "neondb_owner", "npg_HPC9Up2WFJSz");
             Statement stmt = conn.createStatement()) {
            
            System.out.println("====== CHECKING TABLES ======");
            try (ResultSet rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
                while (rs.next()) {
                    System.out.println("Table: " + rs.getString("table_name"));
                }
            }
            
            System.out.println("====== CHECKING USERS ======");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
                while (rs.next()) {
                    System.out.println("User: " + rs.getString("email") + " Role: " + rs.getString("role"));
                }
            } catch (Exception e) {
                System.out.println("Could not query users: " + e.getMessage());
            }

            System.out.println("====== CHECKING DATA SOURCES ======");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM data_sources")) {
                while (rs.next()) {
                    System.out.println("Data Source: " + rs.getString("name") + " Type: " + rs.getString("source_type"));
                }
            } catch (Exception e) {
                System.out.println("Could not query data_sources: " + e.getMessage());
            }
        }
    }
}
