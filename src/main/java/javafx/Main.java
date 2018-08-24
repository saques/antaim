package javafx;

import formats.Encoding;
import formats.Pgm;
import formats.Ppm;
import formats.Raw;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Effect;
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
import structures.LinkedListPeekAheadStack;
import structures.PeekAheadStack;
import utils.ImageDrawingUtils;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Main extends Application {

    private static final double MAX_WIDTH = 256;
    private static final double MAX_HEIGHT = 256;

    private static final int PREVIEWS = 5;
    private static final double PREVIEW_WIDTH = 64;
    private static final double PREVIEW_HEIGHT = 64;
    private static final double PREVIEW_SEPARATION = 16;

    private static final double Y_X = 256, Y_Y = 64;
    private static final double X_X = 256, X_Y = 32+Y_Y+MAX_HEIGHT;
    private static final double P_X = 128, P_Y = X_Y+(MAX_HEIGHT-PREVIEW_HEIGHT);

    private final FileChooser fileChooser = new FileChooser();

    private final Button chooseImageButton = new Button("Elija la imagen ");

    private final Button changeImageButton = new Button("Cambiar imagen ");

    private Text transfImgAvg = null;

    private ImageView xImageView = null, yImageView = null;

    private ImageView[] previews = new ImageView[PREVIEWS];

    private PeekAheadStack<formats.Image> stack = new LinkedListPeekAheadStack<>();

    private void pushAndRender(formats.Image image, Stage stage, BorderPane root){
        stack.push(image);
        renderStackTop(stage, root);
    }

    private void popAndRender(formats.Image image, Stage stage, BorderPane root){
        if(stack.isEmpty())
            return;
        stack.pop();
        renderStackTop(stage, root);
    }

    @Override
    public void start(final Stage stage) {

        stage.setTitle("ATI interface");

        final BorderPane root = new BorderPane();

        MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(stage.widthProperty());
        root.setTop(menuBar);

        Menu fileMenu = new Menu("Archivo");
        MenuItem openImage = new MenuItem("Cargar Imagen");
        //MenuItem saveImage = new MenuItem("Guardar Imagen Transformada");
        MenuItem exitMenuItem = new MenuItem("Exit");
        exitMenuItem.setOnAction(actionEvent -> Platform.exit());
        openImage.setOnAction((final ActionEvent e) ->  chooseImage(stage,root));
        //saveImage.setOnAction((final ActionEvent e) -> saveImage(stage,transfImg));
        //saveImage.setDisable(true);

        Menu toolsMenu = new Menu("Herramientas");
        Menu drawImage = new Menu("Dibujar Figura");
        MenuItem drawCircle = new MenuItem("Dibujar círculo");
        drawCircle.setOnAction((final ActionEvent e) -> drawCircle(stage));
        MenuItem drawRectangle = new MenuItem("Dibujar rectángulo");
        drawRectangle.setOnAction((final ActionEvent e) -> drawRectangle(stage));

        drawImage.getItems().addAll(drawCircle,drawRectangle);

        Menu createGradation = new Menu("Crear Degradé");
        MenuItem drawGrayScale = new MenuItem("Degradé de grises");
        drawGrayScale.setOnAction((final ActionEvent e) -> drawGrayScale(stage));
        MenuItem drawColorScale = new MenuItem("Degradé a color");
        drawColorScale.setOnAction((final ActionEvent e) -> drawColorScale(stage));

        createGradation.getItems().addAll(drawGrayScale,drawColorScale);


        toolsMenu.getItems().addAll(drawImage,createGradation);


        fileMenu.getItems().addAll(openImage,
                new SeparatorMenuItem(), exitMenuItem);

        menuBar.getMenus().addAll(fileMenu,toolsMenu);

        root.getChildren().addAll(chooseImageButton);
        root.setStyle("-fx-background-color: BEIGE;");
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

            String[]  aux = file.toString().split("/");
            aux = aux[aux.length - 1].split("\\.");
            System.out.println(aux.length != 2);
            if (aux.length == 2){
                switch (aux[1].toUpperCase()){
                    case "PGM":
                        try {
                            System.out.println(file.toString());
                            pushAndRender(new Pgm(file.toString()), stage, root);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "PPM":
                        try {
                            System.out.println(file.toString());
                            pushAndRender(new Ppm(file.toString()), stage, root);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "RAW":
                        showModalForRaw(stage,root,file.toString());
                        break;
                    default:
                        showErrorModal(stage,"El formato de archivo es inválido. Solo se aceptan imágenes con extensión .ppm .pgm o .raw");
                        break;

                }

            }
        }
    }

    private void saveImage(Stage stage, formats.Image image) {
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("BMP files (*.bmp)", "*.bmp");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(stage);

        if(file != null){
            try {
                ImageIO.write(image.toBufferedImage(),
                        "bmp", new File(file.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void renderStackTop(Stage stage, BorderPane root){
        removeImages(root);

        renderX(stage, root);

        renderY(stage, root);

        for(int i = 0; i < PREVIEWS; i++){
            if(stack.size() < 2+i+1)
                break;
            formats.Image image = stack.peek(2+i);
            renderStackPreView(stage, root, image, P_X, P_Y - i*(PREVIEW_SEPARATION+PREVIEW_HEIGHT));
        }

    }

    private void removeImages(BorderPane root){
        if (xImageView != null)
            root.getChildren().remove(xImageView);
        if (yImageView != null)
            root.getChildren().remove(yImageView);
        for(int i = 0; i < PREVIEWS; i++){
            if (previews[i] == null)
                break;
            root.getChildren().remove(previews[i]);
        }
    }


    private ImageView renderStackPreView(Stage stage, BorderPane root, formats.Image image, double x, double y){

        Image fxImg = SwingFXUtils.toFXImage(image.toBufferedImage(), null);

        ImageView iv = new ImageView(fxImg);

        iv.setX(x);
        iv.setY(y);

        iv.setFitWidth(PREVIEW_WIDTH);
        iv.setFitHeight(PREVIEW_HEIGHT);
        iv.setPreserveRatio(true);

        root.getChildren().addAll(iv);

        return iv;
    }


    private ImageView renderImage(Stage stage, BorderPane root, formats.Image image, double x, double y){

        Image fxImg = SwingFXUtils.toFXImage(image.toBufferedImage(), null);

        ImageView iv = new ImageView(fxImg);

        iv.setX(x);
        iv.setY(y);

        iv.setFitWidth(MAX_WIDTH);
        iv.setFitHeight(MAX_HEIGHT);
        iv.setPreserveRatio(true);

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


        iv.setOnMouseReleased(event -> {
            int x1 = (int) (selection.getX() - iv.getX());
            int y1 = (int) (selection.getY() - iv.getY());
            int x2 = (int) (x1+selection.getWidth());
            int y2 = (int) (y1 + selection.getHeight());
            System.out.println("x1 = "+x1 + "; y1 = "+y1+"; x2 = "+x2+"; y2 = "+y2);
            int width = image.getWidth();
            int height = image.getHeight();
            if (!isSelectionOutOfBounds(x1,y1,x2,y2,width,height) && !areSamePoint(x1,y1,x2,y2)) {
                pushAndRender(image.copy(x1, y1, x2, y2), stage, root);
            }
            root.getChildren().remove(selection);
            selection.setWidth(0);
            selection.setHeight(0);

        });
        root.getChildren().addAll(iv);

        return iv;
    }

    private void renderX(Stage stage, BorderPane root){
        if(stack.isEmpty())
            return;

        formats.Image X = stack.peek();

        xImageView = renderImage(stage, root, X, X_X, X_Y);
    }

    private void renderY(Stage stage, BorderPane root){
        if(stack.size() < 2)
            return;


        formats.Image Y = stack.peek(1);

        yImageView = renderImage(stage, root, Y, Y_X, Y_Y);

    }

    private boolean isSelectionOutOfBounds(int x1, int y1, int x2, int y2,int width, int height){
        return (x1 < 0 || x1 > width || x2 < 0 || x2 > width || y1 < 0 || y1 > height || y2 < 0 || y2 > height);
    }

    private boolean areSamePoint(int x1, int y1, int x2, int y2){
        return x1 == x2 && y1 == y2;
    }


    private Text getAverage(Stage stage, formats.Image img){
        Text grayLabel = null;
        if (img.getEncoding().equals(Encoding.RGB)){

        } else if(img.getEncoding().equals(Encoding.GS)){
            double avg = img.avg()[0];
            grayLabel = new Text(String.format("Promedio de Gris: %.2f", avg));

            grayLabel.setStyle("-fx-font: normal bold 20px 'serif' ");

        }
        return grayLabel;
    }


    private void showModalForRaw(Stage stage, BorderPane root ,String path){
        Text widthLabel = new Text("Ancho");
        Text heightLabel = new Text("Alto");

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
                pushAndRender(new Raw(width,height,Encoding.GS,path), stage, root);
            } catch (Exception e) {
                showErrorModal(stage,"Las dimensiones ingresadas son incorrectas");
                return;
            }
            newWindow.close();
        });
    }

    private void showErrorModal(Stage stage,String errorMsg){
        Label error = new Label(errorMsg);

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



    private void drawCircle(Stage stage){
        Text centerLabel = new Text("Centro");
        Text xLabel = new Text("         X");
        Text yLabel = new Text("         Y");
        Text radiusLabel = new Text("Radio");
        Text widthLabel = new Text("Ancho");
        Text heightLabel = new Text("Alto");

        TextField xField = new TextField();
        TextField yField = new TextField();

        TextField radiusField = new TextField();

        TextField widthField = new TextField();
        TextField heightField = new TextField();

        Button submit = new Button("Ok");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(centerLabel, 0, 0);
        gridPane.add(xLabel, 0, 1);
        gridPane.add(xField, 1, 1);
        gridPane.add(yLabel, 0, 2);
        gridPane.add(yField, 1, 2);
        gridPane.add(radiusLabel, 0, 3);
        gridPane.add(radiusField, 1, 3);
        gridPane.add(widthLabel, 0, 4);
        gridPane.add(widthField, 1, 4);
        gridPane.add(heightLabel, 0, 5);
        gridPane.add(heightField, 1, 5);

        gridPane.add(submit, 0, 6);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        centerLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        xLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        yLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        radiusLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
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
                Integer x = new Integer(xField.getText());
                Integer y = new Integer(yField.getText());
                Integer radius = new Integer(radiusField.getText());
                Integer width = new Integer(widthField.getText());
                Integer height = new Integer(heightField.getText());
                formats.Image blackImageWhiteCircle = new formats.Image(width,height, Encoding.GS, true);
                ImageDrawingUtils.drawCircle(blackImageWhiteCircle,x,y,radius, (j, k) -> new double[]{1,1,1});
                saveImage(stage,blackImageWhiteCircle);

            } catch (Exception e) {
                showErrorModal(stage,"Las dimensiones ingresadas son incorrectas");
                return;
            }
            newWindow.close();
        });

    }



    private void drawRectangle(Stage stage){
        Text firstPoint = new Text("Punto inicial");
        Text xLabel1 = new Text("         X");
        Text yLabel1 = new Text("         Y");
        Text secondPoint = new Text("Punto final");
        Text xLabel2 = new Text("         X");
        Text yLabel2 = new Text("         Y");
        Text widthLabel = new Text("Ancho");
        Text heightLabel = new Text("Alto");

        TextField xField1 = new TextField();
        TextField yField1 = new TextField();

        TextField xField2 = new TextField();
        TextField yField2 = new TextField();

        TextField widthField = new TextField();
        TextField heightField = new TextField();

        Button submit = new Button("Ok");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(firstPoint, 0, 0);
        gridPane.add(xLabel1, 0, 1);
        gridPane.add(xField1, 1, 1);
        gridPane.add(yLabel1, 0, 2);
        gridPane.add(yField1, 1, 2);
        gridPane.add(secondPoint, 0, 3);
        gridPane.add(xLabel2, 0, 4);
        gridPane.add(xField2, 1, 4);
        gridPane.add(yLabel2, 0, 5);
        gridPane.add(yField2, 1, 5);
        gridPane.add(widthLabel, 0, 6);
        gridPane.add(widthField, 1, 6);
        gridPane.add(heightLabel, 0, 7);
        gridPane.add(heightField, 1, 7);

        gridPane.add(submit, 0, 8);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        firstPoint.setStyle("-fx-font: normal bold 20px 'serif' ");
        xLabel1.setStyle("-fx-font: normal bold 20px 'serif' ");
        yLabel1.setStyle("-fx-font: normal bold 20px 'serif' ");
        secondPoint.setStyle("-fx-font: normal bold 20px 'serif' ");
        xLabel2.setStyle("-fx-font: normal bold 20px 'serif' ");
        yLabel2.setStyle("-fx-font: normal bold 20px 'serif' ");
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
                Integer x1 = new Integer(xField1.getText());
                Integer y1 = new Integer(yField1.getText());
                Integer x2 = new Integer(xField2.getText());
                Integer y2 = new Integer(yField2.getText());
                Integer width = new Integer(widthField.getText());
                Integer height = new Integer(heightField.getText());
                formats.Image blackImageWhiteRectangle = new formats.Image(width,height, Encoding.GS, true);
                ImageDrawingUtils.drawRectangle(blackImageWhiteRectangle,x1,y1,x2,y2, (j, k) -> new double[]{1,1,1});
                saveImage(stage,blackImageWhiteRectangle);

            } catch (Exception e) {
                showErrorModal(stage,"Las dimensiones ingresadas son incorrectas");
                return;
            }
            newWindow.close();
        });

    }

    private void drawGrayScale(Stage stage){
        formats.Image grayScale = ImageDrawingUtils.scale(Encoding.GS, ImageDrawingUtils.grayScale);
        saveImage(stage,grayScale);
    }

    private void drawColorScale(Stage stage){
        Text bandLabel = new Text("Banda fija");
        ChoiceBox bandChoice = new ChoiceBox(FXCollections.observableArrayList(
                "R", "G", "B")
        );
        Text valueLabel = new Text("Valor fijo");

        TextField valueField = new TextField();

        Button submit = new Button("Ok");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(bandLabel, 0, 0);
        gridPane.add(bandChoice, 1, 0);
        gridPane.add(valueLabel, 0, 1);
        gridPane.add(valueField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        bandLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        valueLabel.setStyle("-fx-font: normal bold 20px 'serif' ");
        gridPane.setStyle("-fx-background-color: BEIGE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Ingresar Color");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer index = bandChoice.getSelectionModel().getSelectedIndex();
                Double value = new Double(valueField.getText());
                if (value < 0 || value > 1){
                    showErrorModal(stage,"El valor debe ir de 0 a 1");
                    return;
                }
                formats.Image img = ImageDrawingUtils.scale(Encoding.RGB, ImageDrawingUtils.fixedRGB(index,value));
                saveImage(stage,img);
                newWindow.close();
            } catch (Exception e) {
                showErrorModal(stage,"Las dimensiones ingresadas son incorrectas");
                return;
            }
        });
    }
}
