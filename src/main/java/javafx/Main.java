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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.Light;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.scene.image.Image;
import noise.NoiseApplyMode;
import noise.SaltAndPepperGenerator;
import structures.LinkedListPeekAheadStack;
import structures.PeekAheadStack;
import utils.ImageDrawingUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

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

    private ImageView xImageView = null, yImageView = null;

    private ImageView[] previews = new ImageView[PREVIEWS];

    private PeekAheadStack<formats.Image> stack = new LinkedListPeekAheadStack<>();

    private void pushAndRender(formats.Image image, Stage stage, BorderPane root){
        stack.push(image);
        renderStackTop(stage, root);
    }

    private void popAndRender(Stage stage, BorderPane root){
        if(stack.isEmpty())
            return;
        stack.pop();
        renderStackTop(stage, root);
    }

    private void initializeStackControlButtons(final Stage stage, BorderPane root){

        Button enter = new Button("ENTER");
        Button pop = new Button("POP");
        Button swap = new Button("X <-> Y");
        Button save = new Button("SAVE");

        enter.setOnAction(e-> {
            if(stack.isEmpty())
                return;
            pushAndRender(stack.peek().clone(), stage, root);
        });

        pop.setOnAction(e-> popAndRender(stage, root));

        swap.setOnAction(e-> {
            if(stack.size() < 2)
                return;
            formats.Image x = stack.pop(), y = stack.pop();
            stack.push(x); stack.push(y);
            renderStackTop(stage, root);
        });

        save.setOnAction(e-> {
            if(stack.isEmpty())
                return;
            saveImage(stage, stack.peek());
        });


        HBox bottom = new HBox();
        bottom.setPadding(new Insets(30));
        bottom.setSpacing(30);
        bottom.getChildren().addAll(enter, pop, swap, save);
        root.setBottom(bottom);

    }

    private void initializeMenu(Stage stage, BorderPane root){
        MenuBar menuBar = new MenuBar();
        menuBar.prefWidthProperty().bind(stage.widthProperty());
        root.setTop(menuBar);

        //FILE
        Menu fileMenu = new Menu("File");
            MenuItem openImage = new MenuItem("Load image");
            MenuItem exitMenuItem = new MenuItem("Exit");
            exitMenuItem.setOnAction(actionEvent -> Platform.exit());
            openImage.setOnAction((final ActionEvent e) ->  chooseImage(stage,root));
        fileMenu.getItems().addAll(openImage,
                new SeparatorMenuItem(), exitMenuItem);

        /**
         * DRAW SECTION
         */
        Menu drawMenu = new Menu("Draw");

        //SHAPES
        Menu shapesMenu = new Menu("Shapes");
        MenuItem drawCircle = new MenuItem("Draw circle");
        drawCircle.setOnAction((final ActionEvent e) -> drawCircle(stage, root));
        MenuItem drawRectangle = new MenuItem("Draw rectangle");
        drawRectangle.setOnAction((final ActionEvent e) -> drawRectangle(stage, root));
        shapesMenu.getItems().addAll(drawCircle, drawRectangle);

        //GRADIENTS
        Menu gradationMenu = new Menu("Gradients");
        MenuItem drawPlain = new MenuItem("Plain");
        drawPlain.setOnAction(x -> drawPlainImage(stage, root));
        MenuItem drawGrayScale = new MenuItem("Grey scale");
        drawGrayScale.setOnAction((final ActionEvent e) -> drawGrayScale(stage, root));
        MenuItem drawColorScale = new MenuItem("Color scale");
        drawColorScale.setOnAction((final ActionEvent e) -> drawColorScale(stage, root));
        gradationMenu.getItems().addAll(drawPlain,drawGrayScale,drawColorScale);

        drawMenu.getItems().addAll(shapesMenu, gradationMenu);

        /**
         * OPERATIONS SECTION
         */
        Menu opsMenu = new Menu("Operations");

        //Unary
        Menu unaryOps = new Menu("Unary");
        MenuItem negative = new MenuItem("Negative");
        negative.setOnAction(x-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.negative(), stage, root);
        });
        MenuItem automaticContrast = new MenuItem("Automatic contrast enhancement");
        automaticContrast.setOnAction(x-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.automaticContrastEnhancement(), stage, root);
        });
        MenuItem equalization = new MenuItem("Equalization");
        equalization.setOnAction(x-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.equalize(), stage, root);
        });
        unaryOps.getItems().addAll(negative, automaticContrast, equalization);


        //Binary
        Menu binaryOps = new Menu("Binary");
        MenuItem sum = new MenuItem("Sum");
        sum.setOnAction(e-> {
            if(stack.size() < 2){
                showErrorModal(stage, "Insufficient operands");
                return;
            }
            if(!stack.peek(0).isBinaryOperandCompatible(stack.peek(1))){
                showErrorModal(stage, "Unmatching operands");
                return;
            }
            formats.Image x = stack.pop(), y = stack.pop();
            pushAndRender(y.add(x), stage, root);
        });
        MenuItem subtract = new MenuItem("Subtract");
        subtract.setOnAction(e-> {
            if(stack.size() < 2){
                showErrorModal(stage, "Insufficient operands");
                return;
            }
            if(!stack.peek(0).isBinaryOperandCompatible(stack.peek(1))){
                showErrorModal(stage, "Unmatching operands");
                return;
            }
            formats.Image x = stack.pop(), y = stack.pop();
            pushAndRender(y.subtract(x), stage, root);
        });
        MenuItem product = new MenuItem("Product");
        product.setOnAction(e-> {
            if(stack.size() < 2){
                showErrorModal(stage, "Insufficient operands");
                return;
            }
            if(!stack.peek(0).isBinaryOperandCompatible(stack.peek(1))){
                showErrorModal(stage, "Unmatching operands");
                return;
            }
            formats.Image x = stack.pop(), y = stack.pop();
            pushAndRender(y.product(x), stage, root);
        });
        binaryOps.getItems().addAll(sum, subtract, product);


        opsMenu.getItems().addAll(unaryOps, binaryOps);


        /**
         * NOISE SECTION
         */

        Menu noiseMenu = new Menu("Noise");
        MenuItem saltAndPepper = new MenuItem("Salt and Pepper");
        saltAndPepper.setOnAction(x-> saltAndPepper(stage, root));
        noiseMenu.getItems().addAll(saltAndPepper);


        /**
         * FILTERS SECTION
         */

        Menu filtersMenu = new Menu("Filters");
        MenuItem median = new MenuItem("Median");
        median.setOnAction(x-> medianFilter(stage, root));
        filtersMenu.getItems().addAll(median);


        menuBar.getMenus().addAll(fileMenu,drawMenu, opsMenu, noiseMenu, filtersMenu);

    }

    @Override
    public void start(final Stage stage) {

        stage.setTitle("ATI interface");

        final BorderPane root = new BorderPane();

        initializeStackControlButtons(stage, root);

        initializeMenu(stage, root);

        root.setStyle("-fx-background-color: WHITE;");
        stage.setScene(new Scene(root, 1024, 1024));

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
                    case "JPG":
                    case "JPEG":
                    case "PNG":
                    case "BMP":
                        try {
                            System.out.println(file.toString());
                            pushAndRender(new formats.Image(file.toString()), stage, root);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        showErrorModal(stage,"Format " + aux[1].toUpperCase() + " not accepted.");
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
            previews[i] = renderStackPreView(stage, root, image, P_X, P_Y - i*(PREVIEW_SEPARATION+PREVIEW_HEIGHT));
        }

    }

    private void removeImages(BorderPane root){
        if (xImageView != null)
            root.getChildren().remove(xImageView);
        if (yImageView != null)
            root.getChildren().remove(yImageView);
        for(int i = 0; i < PREVIEWS; i++){
            if(previews[i] == null)
                break;
            root.getChildren().remove(previews[i]);
            previews[i] = null;
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
            grayLabel = new Text(String.format("Grey average: %.2f", avg));

            grayLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");

        }
        return grayLabel;
    }


    private void showModalForRaw(Stage stage, BorderPane root ,String path){
        Text widthLabel = new Text("Width");
        Text heightLabel = new Text("Height");

        TextField widthField = new TextField();
        TextField heightField = new TextField();

        Button submit = new Button("OK");


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

        widthLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        heightLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Enter dimensions");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer width = Integer.valueOf(widthField.getText());
                Integer height = Integer.valueOf(heightField.getText());
                pushAndRender(new Raw(width,height,Encoding.GS,path), stage, root);
            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });
    }

    private void showErrorModal(Stage stage,String errorMsg){
        Label error = new Label(errorMsg);

        Button close = new Button("OK");

        GridPane gridPane = new GridPane();
        gridPane.setMinSize(300, 100);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(error, 0, 0);
        gridPane.add(close, 1, 1);

        error.setStyle("-fx-font: normal bold 14px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");
        close.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");


        Scene scene = new Scene(gridPane);

        // New window (Stage)
        Stage newWindow = new Stage();
        newWindow.setTitle("Error");
        newWindow.setScene(scene);

        // Set position of second window, related to primary window.
        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        close.setOnAction(event -> newWindow.close());
    }


    private void drawPlainImage(Stage stage, BorderPane root){
        Text centerLabel = new Text("Color");
        Text RLabel = new Text("         R");
        Text GLabel = new Text("         G");
        Text BLabel = new Text("         B");
        Text widthLabel = new Text("Width");
        Text heightLabel = new Text("Height");

        TextField RField = new TextField();
        TextField GField = new TextField();
        TextField BField = new TextField();

        TextField widthField = new TextField();
        TextField heightField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(centerLabel, 0, 0);
        gridPane.add(RLabel, 0, 1);
        gridPane.add(RField, 1, 1);
        gridPane.add(GLabel, 0, 2);
        gridPane.add(GField, 1, 2);
        gridPane.add(BLabel, 0, 3);
        gridPane.add(BField, 1, 3);
        gridPane.add(widthLabel, 0, 4);
        gridPane.add(widthField, 1, 4);
        gridPane.add(heightLabel, 0, 5);
        gridPane.add(heightField, 1, 5);
        gridPane.add(submit, 0, 6);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        centerLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        RLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        GLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        BLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        widthLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        heightLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Enter dimensions and colors");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double R = Double.valueOf(RField.getText());
                Double G = Double.valueOf(GField.getText());
                Double B = Double.valueOf(BField.getText());
                Integer width = Integer.valueOf(widthField.getText());
                Integer height = Integer.valueOf(heightField.getText());

                if(R < 0 || G < 0 || B < 0 || R > 1 || G > 1 || B > 1 || width <= 0 || height <= 0)
                    throw new Exception();

                formats.Image image = ImageDrawingUtils.scale(Encoding.RGB, (x, y) -> new double[]{R, G, B}, width, height);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions or colors, try again");
            }
            newWindow.close();
        });
    }

    private void saltAndPepper(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text centerLabel = new Text("Threshold");
        Text PLabel = new Text("         p");

        TextField PField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(centerLabel, 0, 0);
        gridPane.add(PLabel, 0, 1);
        gridPane.add(PField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        centerLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        PLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Salt and pepper settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double P = Double.valueOf(PField.getText());

                if(P <= 0 || P >= 0.5)
                    throw new Exception();

                formats.Image image = stack.peek().contaminateO(1, new SaltAndPepperGenerator(P), NoiseApplyMode.DESTRUCTIVE);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions or colors, try again");
            }
            newWindow.close();
        });
    }

    private void medianFilter(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text centerLabel = new Text("Size");
        Text NLabel = new Text("         N");

        TextField NField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(centerLabel, 0, 0);
        gridPane.add(NLabel, 0, 1);
        gridPane.add(NField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        centerLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        NLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Salt and pepper settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Integer N = Integer.valueOf(NField.getText());

                if(N <= 0 || (N % 2) == 0)
                    throw new Exception();

                formats.Image image = stack.pop().medianFilter(N);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions or colors, try again");
            }
            newWindow.close();
        });
    }




    private void drawCircle(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text centerLabel = new Text("Center");
        Text xLabel = new Text("         X");
        Text yLabel = new Text("         Y");
        Text radiusLabel = new Text("Radius");
        Text colorLabel = new Text("Color");
        Text RLabel = new Text("         R");
        Text GLabel = new Text("         G");
        Text BLabel = new Text("         B");

        TextField xField = new TextField();
        TextField yField = new TextField();
        TextField radiusField = new TextField();
        TextField RField = new TextField();
        TextField GField = new TextField();
        TextField BField = new TextField();

        Button submit = new Button("OK");


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
        gridPane.add(colorLabel, 0, 4);
        gridPane.add(RLabel, 0, 5);
        gridPane.add(RField, 1, 5);
        gridPane.add(GLabel, 0, 6);
        gridPane.add(GField, 1, 6);
        gridPane.add(BLabel, 0, 7);
        gridPane.add(BField, 1, 7);
        gridPane.add(submit, 0, 8);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        centerLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        xLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        yLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        radiusLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        colorLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        RLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        GLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        BLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Enter dimensions");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Double R = Double.valueOf(RField.getText());
                Double G = Double.valueOf(GField.getText());
                Double B = Double.valueOf(BField.getText());
                Integer x = Integer.valueOf(xField.getText());
                Integer y = Integer.valueOf(yField.getText());
                Integer radius = Integer.valueOf(radiusField.getText());

                formats.Image image = stack.peek();

                if(R < 0 || G < 0 || B < 0 || R > 1 || G > 1 || B > 1 || image.isOutOfBounds(x, y))
                    throw new Exception();

                ImageDrawingUtils.drawCircle(image,x,y,radius, (j, k) -> new double[]{R,G,B});

                renderStackTop(stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });

    }



    private void drawRectangle(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text firstPoint = new Text("Top left corner");
        Text xLabel1 = new Text("         X");
        Text yLabel1 = new Text("         Y");
        Text secondPoint = new Text("Bottom right corner");
        Text xLabel2 = new Text("         X");
        Text yLabel2 = new Text("         Y");
        Text colorLabel = new Text("Color");
        Text RLabel = new Text("         R");
        Text GLabel = new Text("         G");
        Text BLabel = new Text("         B");

        TextField xField1 = new TextField();
        TextField yField1 = new TextField();

        TextField xField2 = new TextField();
        TextField yField2 = new TextField();

        TextField RField = new TextField();
        TextField GField = new TextField();
        TextField BField = new TextField();

        Button submit = new Button("OK");


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
        gridPane.add(colorLabel, 0, 6);
        gridPane.add(RLabel, 0, 7);
        gridPane.add(RField, 1, 7);
        gridPane.add(GLabel, 0, 8);
        gridPane.add(GField, 1, 8);
        gridPane.add(BLabel, 0, 9);
        gridPane.add(BField, 1, 9);

        gridPane.add(submit, 0, 10);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        firstPoint.setStyle("-fx-font: normal bold 20px 'Arial' ");
        xLabel1.setStyle("-fx-font: normal bold 20px 'Arial' ");
        yLabel1.setStyle("-fx-font: normal bold 20px 'Arial' ");
        secondPoint.setStyle("-fx-font: normal bold 20px 'Arial' ");
        xLabel2.setStyle("-fx-font: normal bold 20px 'Arial' ");
        yLabel2.setStyle("-fx-font: normal bold 20px 'Arial' ");
        colorLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        RLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        GLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        BLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Enter dimensions");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer x1 = Integer.valueOf(xField1.getText());
                Integer y1 = Integer.valueOf(yField1.getText());
                Integer x2 = Integer.valueOf(xField2.getText());
                Integer y2 = Integer.valueOf(yField2.getText());
                Double R = Double.valueOf(RField.getText());
                Double G = Double.valueOf(GField.getText());
                Double B = Double.valueOf(BField.getText());

                formats.Image image = stack.peek();

                if(R < 0 || G < 0 || B < 0 || R > 1 || G > 1 || B > 1 || image.isOutOfBounds(x1, y1) || image.isOutOfBounds(x2, y2) || x2 <= x1 || y2 <= y1)
                    throw new Exception();

                ImageDrawingUtils.drawRectangle(image,x1,y1,x2,y2, (j, k) -> new double[]{R,G,B});

                renderStackTop(stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });

    }

    private void drawGrayScale(Stage stage, BorderPane root){
        formats.Image grayScale = ImageDrawingUtils.scale(Encoding.GS, ImageDrawingUtils.grayScale);
        pushAndRender(grayScale,stage, root);
    }

    private void drawColorScale(Stage stage, BorderPane root){
        Text bandLabel = new Text("Fixed band");
        ChoiceBox bandChoice = new ChoiceBox(FXCollections.observableArrayList("R", "G", "B"));
        Text valueLabel = new Text("Fixed value");

        TextField valueField = new TextField();

        Button submit = new Button("OK");


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

        bandLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        valueLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Enter color");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer index = bandChoice.getSelectionModel().getSelectedIndex();
                Double value = Double.valueOf(valueField.getText());
                if (value < 0 || value > 1){
                    showErrorModal(stage,"Fixed value must be in [0, 1]");
                    return;
                }
                formats.Image img = ImageDrawingUtils.scale(Encoding.RGB, ImageDrawingUtils.fixedRGB(index,value));
                pushAndRender(img,stage, root);
            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });
    }
}
