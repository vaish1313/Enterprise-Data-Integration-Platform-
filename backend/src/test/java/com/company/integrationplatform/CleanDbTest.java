package com.company.integrationplatform;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CleanDbTest {
    @Test
    public void dropTables() throws Exception {
        String url = "jdbc:postgresql://ep-raspy-mode-a6hnrzpa.us-west-2.aws.neon.tech/neondb?sslmode=require";
        try (Connection conn = DriverManager.getConnection(url, "neondb_owner", "npg_HPC9Up2WFJSz");
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
            System.out.println("====== SCHEMA DROPPED AND RECREATED SUCCESSFULLY! ======");
        }
    }
}
