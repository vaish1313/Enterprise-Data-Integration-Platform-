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
            
            System.out.println("====== DB ROW COUNTS ======");
            String[] tables = {"users", "data_sources", "ingestion_jobs", "sync_jobs", "transformation_rules", "audit_logs"};
            
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        System.out.println(table + ": " + rs.getInt(1));
                    }
                } catch (Exception e) {
                    System.out.println(table + ": Error - " + e.getMessage());
                }
            }
        }
    }
}
