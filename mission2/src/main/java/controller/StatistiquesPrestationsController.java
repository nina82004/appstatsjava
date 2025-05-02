package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;

import java.util.ArrayList;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import util.DatabaseConnection;




public class StatistiquesPrestationsController {

    @FXML
    public  BarChart<String, Number> barServiceParType;
    @FXML
    public  PieChart pieRepartitionIntervention;
    @FXML
    public  BarChart<String, Number> barServiceParPrix;

    @FXML
    public  PieChart pieTopPrestations;

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
    public void initialize() {
        afficherNombrePrestationsParType();
        afficherRepartitionIntervention();
        afficherServiceParPrix();
        afficherTop5PrestationsPie();

    }

    private void afficherNombrePrestationsParType() {
        String sql = "SELECT type, COUNT(*) AS total FROM contractors GROUP BY type";

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de prestations par type");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("type");
                int count = rs.getInt("total");
                series.getData().add(new XYChart.Data<>(type, count));
            }

            barServiceParType.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void afficherRepartitionIntervention() {
        String sql = "SELECT intervention, COUNT(*) AS total FROM contractors GROUP BY intervention";

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String intervention = rs.getString("intervention");
                int count = rs.getInt("total");
                data.add(new PieChart.Data(intervention, count));
            }

            pieRepartitionIntervention.setData(data);
            pieRepartitionIntervention.setTitle("Répartition des modes d’intervention");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherServiceParPrix() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Coût par service");

        String sql = "SELECT service, service_price FROM contractors";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String service = rs.getString("service");
                int price = rs.getInt("service_price");
                series.getData().add(new XYChart.Data<>(service, price));
            }

            barServiceParPrix.getData().add(series);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherTop5PrestationsPie() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        String sql = """
        SELECT c.service, COUNT(*) AS total
        FROM contractors c
        LEFT JOIN (
            SELECT a.contractor_id
            FROM appointments a
            JOIN booked_appointments b ON a.appointment_id = b.appointment_id
            UNION ALL
            SELECT m.contractor_id
            FROM medical_appointments m
        ) AS all_appointments ON c.contractor_id = all_appointments.contractor_id
        GROUP BY c.service
        ORDER BY total DESC
        LIMIT 5
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            double totalSum = 0;
            List<PieChart.Data> tempList = new ArrayList<>();

            while (rs.next()) {
                String service = rs.getString("service");
                int total = rs.getInt("total");
                totalSum += total;
                tempList.add(new PieChart.Data(service, total));
            }

            for (PieChart.Data d : tempList) {
                double percent = (d.getPieValue() / totalSum) * 100;
                d.setName(String.format("%s (%.1f%%)", d.getName(), percent));
            }

            data.addAll(tempList);
            pieTopPrestations.setData(data);
            pieTopPrestations.setTitle("Top 5 prestations réservées");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void telechargerGraphiquesPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fileChooser.showSaveDialog(null);
            if (file == null) return;

            List<WritableImage> chartImages = new ArrayList<>();
            chartImages.add(barServiceParType.snapshot(null, null));
            chartImages.add(pieRepartitionIntervention.snapshot(null, null));
            chartImages.add(barServiceParPrix.snapshot(null, null));
            chartImages.add(pieTopPrestations.snapshot(null, null));

            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            for (WritableImage chartImage : chartImages) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(SwingFXUtils.fromFXImage(chartImage, null), "png", os);
                ImageData imageData = ImageDataFactory.create(os.toByteArray());
                com.itextpdf.layout.element.Image pdfImage = new com.itextpdf.layout.element.Image(imageData);
                pdfImage.setAutoScale(true);
                document.add(pdfImage);
                document.add(new Paragraph("\n"));
            }

            document.close();
            System.out.println("PDF généré avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public WritableImage getChartImage(Node chart) {
        return chart.snapshot(new SnapshotParameters(), null);
    }

}
