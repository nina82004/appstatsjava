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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import org.json.JSONObject;
import org.json.JSONArray;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class StatistiquesPrestationsController {

    @FXML
    public BarChart<String, Number> barServiceParType;
    @FXML
    public PieChart pieRepartitionIntervention;
    @FXML
    public BarChart<String, Number> barServiceParPrix;
    @FXML
    public PieChart pieTopPrestations;

    @FXML
    public void initialize() {
        afficherNombrePrestationsParType();
        afficherRepartitionIntervention();
        afficherServiceParPrix();
        afficherTop5PrestationsPie();
    }

    private void afficherNombrePrestationsParType() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de prestations par type");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_prestations/service-count-by-type"))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            JSONObject json = new JSONObject(response.body());

            for (String key : json.keySet()) {
                series.getData().add(new XYChart.Data<>(key, json.getInt(key)));
            }

            barServiceParType.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherRepartitionIntervention() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_prestations/intervention-distribution"))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            for (String key : json.keySet()) {
                data.add(new PieChart.Data(key, json.getInt(key)));
            }

            pieRepartitionIntervention.setData(data);
            pieRepartitionIntervention.setTitle("Répartition des modes d’intervention");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherServiceParPrix() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Coût par service");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_prestations/service-price"))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(response.body());

            for (String key : json.keySet()) {
                series.getData().add(new XYChart.Data<>(key, json.getDouble(key)));
            }

            barServiceParPrix.getData().add(series);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void afficherTop5PrestationsPie() {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/stats_prestations/top5-prestations"))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray array = new JSONArray(response.body());

            double total = 0;
            for (int i = 0; i < array.length(); i++) {
                total += array.getJSONObject(i).getDouble("total");
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                double count = obj.getDouble("total");
                String name = obj.getString("service");
                double percent = (count / total) * 100;
                data.add(new PieChart.Data(name + String.format(" (%.1f%%)", percent), count));
            }

            pieTopPrestations.setData(data);
            pieTopPrestations.setTitle("Top 5 prestations réservées");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
    private void telechargerGraphiquesPDF(ActionEvent event) {
        WritableImage image1 = barServiceParType.snapshot(null, null);
        WritableImage image2 = pieRepartitionIntervention.snapshot(null, null);
        WritableImage image3 = barServiceParPrix.snapshot(null, null);
        WritableImage image4 = pieTopPrestations.snapshot(null, null);

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

                // Sauvegarde les images en PNG
                ImageIO.write(SwingFXUtils.fromFXImage(image1, null), "png", temp1);
                ImageIO.write(SwingFXUtils.fromFXImage(image2, null), "png", temp2);
                ImageIO.write(SwingFXUtils.fromFXImage(image3, null), "png", temp3);
                ImageIO.write(SwingFXUtils.fromFXImage(image4, null), "png", temp4);

                // Création du PDF
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
            System.err.println(" Erreur lors de la génération du PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }
}