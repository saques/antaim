package formats;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Light;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;

public class Main extends Application {

    private final FileChooser fileChooser = new FileChooser();

    private final Button chooseImageButton = new Button("Elija la imagen ");

    private final Button changeImageButton = new Button("Cambiar imagen ");

    private formats.Image img = null;

    private formats.Image transfImg= null;

    ImageView imageVIew = null;

    ImageView transfImageVIew = null;

    /*@Override
    public void start(Stage stage) throws FileNotFoundException {

            //Creating an image
            Image image = new Image(new FileInputStream("C:\\Users\\NMarcantonio\\Documents\\antaim\\images\\lena.jpg"));

            //Setting the image view 1
            ImageView imageView1 = new ImageView(image);

            //Setting the position of the image
            imageView1.setX(50);
            imageView1.setY(25);


            //Setting the preserve ratio of the image view
            imageView1.setPreserveRatio(true);

            Image improvedImage = new Image(new FileInputStream("C:\\Users\\NMarcantonio\\Documents\\antaim\\images\\lena.jpg"));

            //Setting the image view 2
            ImageView imageView2 = new ImageView(improvedImage);

            //Setting the position of the image
            imageView2.setX(image.getWidth()+150);
            imageView2.setY(25);


            //Setting the preserve ratio of the image view
            imageView2.setPreserveRatio(true);

            //Creating a Group object
            Group root = new Group(imageView1, imageView2);

            //Creating a scene object
            Scene scene = new Scene(root, image.getWidth()+improvedImage.getWidth()+250, Math.max(image.getHeight(),improvedImage.getHeight())+100);

            //Setting title to the Stage
            stage.setTitle("ATI interface");

            //Adding scene to the stage
            stage.setScene(scene);

            //Displaying the contents of the stage
            stage.show();
        }*/
    @Override
    public void start(final Stage stage) {

        stage.setTitle("ATI interface");

        final BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(stage.widthProperty());
        root.setTop(menuBar);

        Menu fileMenu = new Menu("Archivo");
        MenuItem openImage = new MenuItem("Cargar Imagen");
        MenuItem saveIMage = new MenuItem("Guardar Imagen Transformada");
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(actionEvent -> Platform.exit());
        openImage.setOnAction((final ActionEvent e) -> chooseImage(stage,root));

        fileMenu.getItems().addAll(openImage, saveIMage,
                new SeparatorMenuItem(), exitMenuItem);

        menuBar.getMenus().addAll(fileMenu);

        root.getChildren().addAll(chooseImageButton);
        stage.setScene(new Scene(root, 500, 500));
        stage.show();
    }
    public static void main(String args[]){
        launch(args);
    }

    private void chooseImage(final Stage stage, BorderPane root) {
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            Image fxImg = null;

            img = null;

            String[]  aux = file.toString().split("/");
            aux = aux[aux.length - 1].split("\\.");
            System.out.println(aux.length != 2);
            if (aux.length != 2){
                //mal
            } else {
                if (aux[1].toUpperCase().equals("PGM")){
                    try {
                        System.out.println(file.toString());
                        img = new Pgm(file.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fxImg = SwingFXUtils.toFXImage(img.toBufferedImage(), null);
                    showImage(stage,root,fxImg);

                } else if (aux[1].toUpperCase().equals("JPG") || aux[1].toUpperCase().equals("PNG") || aux[1].toUpperCase().equals("BMP")){
                    fxImg = new Image(file.toURI().toString());
                } else if (aux[1].toUpperCase().equals("RAW")){
                    showModalForRaw(stage,root,file.toString());
                }
            }




        }
    }

    private void showImage(Stage stage, BorderPane root, Image fxImg){
        if (transfImageVIew != null)
            root.getChildren().remove(transfImageVIew);
        ImageView iv = new ImageView(fxImg);

        iv.setX(50);
        iv.setY(40);



        iv.setPreserveRatio(true);

        root.getChildren().add(iv);

        final Rectangle selection = new Rectangle();
        final Light.Point anchor = new Light.Point();

        iv.setOnMousePressed(event -> {
            anchor.setX(event.getX());
            anchor.setY(event.getY());
            selection.setX(event.getX());
            selection.setY(event.getY());
            selection.setFill(null); // transparent
            selection.setStroke(Color.BLACK); // border
            selection.getStrokeDashArray().add(10.0);
            root.getChildren().add(selection);
        });

        iv.setOnMouseDragged(event -> {
            selection.setWidth(Math.abs(event.getX() - anchor.getX()));
            selection.setHeight(Math.abs(event.getY() - anchor.getY()));
            selection.setX(Math.min(anchor.getX(), event.getX()));
            selection.setY(Math.min(anchor.getY(), event.getY()));
        });


        formats.Image finalFormattedImg = img;
        iv.setOnMouseReleased(event -> {
            root.getChildren().remove(transfImageVIew);
            int x1 = (int) (selection.getX() - iv.getX());
            int y1 = (int) (selection.getY() - iv.getY());
            int x2 = (int) (x1+selection.getWidth());
            int y2 = (int) (y1 + selection.getHeight());
            System.out.println("x1 = "+x1 + "; y1 = "+y1+"; x2 = "+x2+"; y2 = "+y2);
            transfImg = finalFormattedImg.copy(x1,y1,x2,y2);
            Image fxTransfImg = SwingFXUtils.toFXImage(transfImg.toBufferedImage(), null);

            transfImageVIew = new ImageView(fxTransfImg);

            //Setting the position of the image
            transfImageVIew.setX(iv.getImage().getWidth()+150);
            transfImageVIew.setY(40);


            //Setting the preserve ratio of the image view
            transfImageVIew.setPreserveRatio(true);


            root.getChildren().remove(selection);
            root.getChildren().add(transfImageVIew);
            selection.setWidth(0);
            selection.setHeight(0);


        });

    }

    private void showModalForRaw(Stage stage, BorderPane root ,String path){
        Text widthLabel = new Text("Width");
        Text heightLabel = new Text("Height");

        TextField widthField = new TextField();
        TextField heightField = new TextField();

        Button submit = new Button("Ok");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(widthLabel, 0, 0);
        gridPane.add(widthField, 1, 0);
        gridPane.add(heightLabel, 0, 1);
        gridPane.add(heightField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        widthLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        heightLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        gridPane.setStyle("-fx-background-color: BEIGE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Ingresar Dimensiones");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer width = new Integer(widthField.getText());
                Integer height = new Integer(heightField.getText());
                img = new Raw(width,height,Encoding.GS,path);
            } catch (Exception e) {
                showErrorModal(stage);
                return;
            }
            Image fxImg = SwingFXUtils.toFXImage(img.toBufferedImage(), null);
            showImage(stage,root,fxImg);
            newWindow.close();
        });
    }

    private void showErrorModal(Stage stage){
        Label error = new Label("Las dimensiones ingresadas son incorrectas");

        Button close = new Button("Cerrar");

        GridPane gridPane = new GridPane();
        gridPane.setMinSize(300, 100);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(error, 0, 0);
        gridPane.add(close, 1, 1);

        error.setStyle("-fx-font: normal bold 14px 'serif' ");
        gridPane.setStyle("-fx-background-color: BEIGE;");
        close.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");


        Scene scene = new Scene(gridPane);

        // New window (Stage)
        Stage newWindow = new Stage();
        newWindow.setTitle("Datos Incorrectos");
        newWindow.setScene(scene);

        // Set position of second window, related to primary window.
        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        close.setOnAction(event -> newWindow.close());
    }
}
