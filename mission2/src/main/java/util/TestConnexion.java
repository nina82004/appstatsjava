package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestConnexion {

    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("✅ Connexion réussie à la BDD");

            String sql = "SELECT company_id, name FROM companies LIMIT 3";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String id = rs.getString("company_id");
                String nom = rs.getString("name");
                System.out.println("→ Company #" + id + " : " + nom);
            }


        } catch (SQLException e) {
            System.err.println(" Erreur de connexion : " + e.getMessage());
        }
    }
}



