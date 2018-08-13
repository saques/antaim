package formats;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.Light;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.io.File;

public class Main extends Application {

    private final FileChooser fileChooser = new FileChooser();

    private final Button chooseImageButton = new Button("Elija la imagen ");

    private final Button changeImageButton = new Button("Cambiar imagen ");

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

        final StackPane stac = new StackPane();



        chooseImageButton.setOnAction((final ActionEvent e) -> chooseImage(stage));

        stac.getChildren().add(chooseImageButton);
        stage.setScene(new Scene(stac, 500, 500));
        stage.show();
    }
    public static void main(String args[]){
        launch(args);
    }

    private void chooseImage(final Stage stage) {
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {


            Image image = new Image(file.toURI().toString());

            ImageView iv = new ImageView(image);

            iv.setX(50);
            iv.setY(25);

            Group root = new Group(iv,changeImageButton);

            changeImageButton.setOnAction((final ActionEvent e) -> chooseImage(stage));


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
                // Do what you want with selection's properties here
                System.out.printf("X: %.2f, Y: %.2f, Width: %.2f, Height: %.2f%n",
                        selection.getX(), selection.getY(), selection.getWidth(), selection.getHeight());
                PixelReader reader = iv.getImage().getPixelReader();
              //  WritableImage newImage = new WritableImage(reader, (int)selection.getX(), (int)selection.getY(), (int)selection.getWidth(), (int)selection.getHeight());
                root.getChildren().remove(selection);
                selection.setWidth(0);
                selection.setHeight(0);


            });

            stage.setScene( new Scene(root, image.getWidth()*2+250, image.getHeight()+100));
            stage.show();


        }
    }
}
