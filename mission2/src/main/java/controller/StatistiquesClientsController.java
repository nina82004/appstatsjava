package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import util.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javafx.scene.SnapshotParameters;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class StatistiquesClientsController {

    @FXML
    public void retourAccueil(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Page principale - Business Care");
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
   public BarChart<String, Number> barTopClients;
    @FXML
    public PieChart pieTypeClient;
    @FXML
    public  BarChart<String, Number> barTarifParType;
    @FXML
    public  PieChart pieTopClients;

    @FXML
    public void initialize() {
        afficherTop5ParRevenu();
        afficherRepartitionParType();
        afficherTarifParTypeClient();
        afficherRepartitionTop5Clients();
    }


    private void afficherTop5ParRevenu() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Top 5 clients par chiffre d’affaires");

        String sql = "SELECT name, revenue FROM companies ORDER BY revenue DESC LIMIT 5";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String nom = rs.getString("name");
                int revenue = rs.getInt("revenue");
                series.getData().add(new XYChart.Data<>(nom, revenue));
            }

            barTopClients.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void afficherRepartitionParType() {

        String sql = "SELECT size FROM companies";

        int countStarter = 0;
        int countBasic = 0;
        int countPremium = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int size = rs.getInt("size");
                if (size >= 1 && size <= 30) countStarter++;
                else if (size <= 250) countBasic++;
                else countPremium++;
            }

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                    new PieChart.Data("Starter", countStarter),
                    new PieChart.Data("Basic", countBasic),
                    new PieChart.Data("Premium", countPremium)
            );

            pieTypeClient.setData(data);
            pieTypeClient.setTitle("Répartition par type de client");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherTarifParTypeClient() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tarif annuel par type de client (€)");

        String sql = """
        SELECT 
          CASE 
            WHEN c.size BETWEEN 1 AND 30 THEN 'Starter'
            WHEN c.size BETWEEN 31 AND 250 THEN 'Basic'
            ELSE 'Premium'
          END AS client_type,
          SUM(e.amount) AS average_tarif
        FROM estimates e
        JOIN companies c ON e.company_id = c.company_id
        WHERE YEAR(e.creation_date) = 2025
        GROUP BY client_type
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("client_type");
                double tarif = rs.getDouble("average_tarif");
                series.getData().add(new XYChart.Data<>(type, tarif));
            }

            barTarifParType.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherRepartitionTop5Clients() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        String sql = """
        SELECT c.name, SUM(e.amount) AS total
        FROM estimates e
        JOIN companies c ON e.company_id = c.company_id
        WHERE YEAR(e.creation_date) = 2025
        GROUP BY c.company_id, c.name
        ORDER BY total DESC
        LIMIT 5
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String name = rs.getString("name");
                double totalAmount = rs.getDouble("total");
                data.add(new PieChart.Data(name, totalAmount));
            }

            // Calculer le total pour les pourcentages
            double globalTotal = data.stream().mapToDouble(PieChart.Data::getPieValue).sum();

            // Ajouter les pourcentages dans le label
            for (PieChart.Data d : data) {
                double pourcentage = (d.getPieValue() / globalTotal) * 100;
                d.setName(String.format("%s (%.1f%%)", d.getName(), pourcentage));
            }

            pieTopClients.setData(data);
            pieTopClients.setTitle("Répartition CA Top 5 clients");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void telechargerGraphiquesPDF(ActionEvent event) {
        WritableImage image1 = barTopClients.snapshot(null, null);
        WritableImage image2 = pieTypeClient.snapshot(null, null);
        WritableImage image3 = barTarifParType.snapshot(null, null);
        WritableImage image4 = pieTopClients.snapshot(null, null);

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));
            File file = fileChooser.showSaveDialog(((Node) event.getSource()).getScene().getWindow());

            if (file != null) {

                File temp1 = File.createTempFile("graph1", ".png");
                File temp2 = File.createTempFile("graph2", ".png");
                File temp3 = File.createTempFile("graph3", ".png");
                File temp4 = File.createTempFile("graph4", ".png");

                ImageIO.write(SwingFXUtils.fromFXImage(image1, null), "png", temp1);
                ImageIO.write(SwingFXUtils.fromFXImage(image2, null), "png", temp2);
                ImageIO.write(SwingFXUtils.fromFXImage(image3, null), "png", temp3);
                ImageIO.write(SwingFXUtils.fromFXImage(image4, null), "png", temp4);


                PdfWriter writer = new PdfWriter(file.getAbsolutePath());
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc);

                ImageData img1 = ImageDataFactory.create(temp1.getAbsolutePath());
                ImageData img2 = ImageDataFactory.create(temp2.getAbsolutePath());
                ImageData img3 = ImageDataFactory.create(temp3.getAbsolutePath());
                ImageData img4 = ImageDataFactory.create(temp4.getAbsolutePath());

                document.add(new com.itextpdf.layout.element.Image(img1).scaleToFit(500, 400));
                document.add(new com.itextpdf.layout.element.Image(img2).scaleToFit(500, 400));
                document.add(new com.itextpdf.layout.element.Image(img3).scaleToFit(500, 400));
                document.add(new com.itextpdf.layout.element.Image(img4).scaleToFit(500, 400));

                document.close();

                System.out.println("PDF généré avec succès : " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public WritableImage getChartImage(Node chart) {
        return chart.snapshot(new SnapshotParameters(), null);
    }


}
