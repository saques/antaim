package javafx;

import formats.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.effect.Light;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.*;
import javafx.scene.image.Image;
import javafx.util.Duration;
import noise.*;
import structures.LinkedListPeekAheadStack;
import structures.PeekAheadStack;
import utils.ImageDrawingUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Main extends Application {

    private static final double MAX_WIDTH = 384;
    private static final double MAX_HEIGHT = 384;

    private static final int PREVIEWS = 5;
    private static final double PREVIEW_WIDTH = 64;
    private static final double PREVIEW_HEIGHT = 64;
    private static final double PREVIEW_SEPARATION = 16;

    private static final double Y_X = 256, Y_Y = 96;
    private static final double X_X = 256, X_Y = 32+Y_Y+MAX_HEIGHT;
    private static final double P_X = 128, P_Y = X_Y+(MAX_HEIGHT-PREVIEW_HEIGHT);

    private final FileChooser fileChooser = new FileChooser();

    private ImageView xImageView = null, yImageView = null;

    private ImageView[] previews = new ImageView[PREVIEWS];

    private PeekAheadStack<formats.Image> stack = new LinkedListPeekAheadStack<>();

    private Group histogramGroup;

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
        Button crop = new Button("CROP");
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

        crop.setOnAction(e-> {
            if(stack.isEmpty())
                return;
            cropImage(stage, root);
        });

        save.setOnAction(e-> {
            if(stack.isEmpty())
                return;
            saveImage(stage, stack.peek());
        });


        HBox bottom = new HBox();
        bottom.setPadding(new Insets(30));
        bottom.setSpacing(30);
        bottom.getChildren().addAll(enter, pop, swap, crop, save);
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
        MenuItem scalarProduct = new MenuItem("Scalar product");
        scalarProduct.setOnAction(x-> scalarProduct(stage, root));
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
        MenuItem greyscale = new MenuItem("Greyscale");
        greyscale.setOnAction(x-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.toGS(), stage, root);
        });
        MenuItem gammaCorrection = new MenuItem("Gamma correction");
        gammaCorrection.setOnAction(x-> gammaPower(stage, root));
        MenuItem dynamicRangeCompression = new MenuItem("Dynamic range compression");
        dynamicRangeCompression.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.dynamicRangeCompression(), stage, root);
        });
        MenuItem convolution3by3 = new MenuItem("3*3 Convolution");
        convolution3by3.setOnAction(x -> threeByThreeMask(stage, root));
        MenuItem laplace = new MenuItem("Laplace");
        laplace.setOnAction(e ->{
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.laplace(), stage, root);
        });
        MenuItem lOG = new MenuItem("Laplacian of Gaussian");
        lOG.setOnAction(x-> lOG(stage, root));
        unaryOps.getItems().addAll(scalarProduct, negative, automaticContrast, equalization, greyscale, gammaCorrection, dynamicRangeCompression, convolution3by3,laplace,lOG);


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
        MenuItem gaussian = new MenuItem("Gaussian");
        gaussian.setOnAction(x-> gaussNoise(stage, root));
        MenuItem exponential = new MenuItem("Exponential");
        exponential.setOnAction(x-> exponentialNoise(stage, root));
        MenuItem rayleigh = new MenuItem("Rayleigh");
        rayleigh.setOnAction(x-> rayleighNoise(stage, root));
        MenuItem saltAndPepper = new MenuItem("Salt and Pepper");
        saltAndPepper.setOnAction(x-> saltAndPepper(stage, root));
        noiseMenu.getItems().addAll(gaussian, exponential, rayleigh, saltAndPepper);



        /**
         * FILTERS SECTION
         */

        Menu filtersMenu = new Menu("Filters");
        MenuItem mean = new MenuItem("Mean");
        mean.setOnAction(x-> meanFilter(stage, root));
        MenuItem median = new MenuItem("Median");
        median.setOnAction(x-> medianFilter(stage, root));
        MenuItem weightedMedian = new MenuItem("Weighted median");
        weightedMedian.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.weightedMedianFilter(), stage, root);
        });
        MenuItem gauss = new MenuItem("Gauss");
        gauss.setOnAction(e-> gaussFilter(stage, root));
        MenuItem diffusion = new MenuItem("Iso/Anisotropic diffusion");
        diffusion.setOnAction(e-> diffusion(stage, root));
        MenuItem bilateral = new MenuItem("Bilateral Filter");
        bilateral.setOnAction(e-> bilateral(stage, root));
        filtersMenu.getItems().addAll(mean,median, weightedMedian, gauss, diffusion,bilateral);

        /**
         * THRESHOLDS
         */

        Menu thresholdMenu = new Menu("Threshold");

        MenuItem manualThreshold = new MenuItem("Manual");
        manualThreshold.setOnAction(x-> threshold(stage, root));
        MenuItem automaticThreshold = new MenuItem("Automatic");
        automaticThreshold.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.automaticThresholding(), stage, root);
        });
        MenuItem otsu = new MenuItem("Otsu");
        otsu.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.otsu(), stage, root);
        });
        thresholdMenu.getItems().addAll(manualThreshold, automaticThreshold, otsu);

        /**
         * BORDER DETECTION
         */

        Menu borderDetectionMenu = new Menu("Border detection");
        MenuItem basic = new MenuItem("Basic");
        basic.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.contourEnhancement(), stage, root);
        });
        MenuItem prewitt = new MenuItem("Prewitt");
        prewitt.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.prewitt(), stage, root);
        });
        MenuItem sobel = new MenuItem("Sobel");
        sobel.setOnAction(e-> {
            if(stack.isEmpty()){
                showErrorModal(stage, "Empty stack");
                return;
            }
            formats.Image image = stack.pop();
            pushAndRender(image.sobel(), stage, root);
        });

        Menu canny = new Menu("Canny");

        MenuItem standardCanny = new MenuItem("Standard");
        standardCanny.setOnAction(e -> canny(stage, root));

        MenuItem cannyDif = new MenuItem("Anisotropic Diffusion");
        cannyDif.setOnAction(e -> cannyIso(stage, root));

        canny.getItems().addAll(standardCanny, cannyDif);

        Menu susan = new Menu("SUSAN");

        MenuItem susanCurrent = new MenuItem("SUSAN on current image");
        susanCurrent.setOnAction(e-> susan(stage,root,true));

        MenuItem susanNewImage = new MenuItem("SUSAN on new image");
        susanNewImage.setOnAction(e-> susan(stage,root,false));

        susan.getItems().addAll(susanCurrent, susanNewImage);

        Menu activeContours = new Menu("Active contours");

        MenuItem oneImageAC = new MenuItem("Image");
        oneImageAC.setOnAction(e -> activeContours(stage, root));

        Menu videoAC = new Menu("Video");

        MenuItem videoACStandard = new MenuItem("Standard");
        videoACStandard.setOnAction(e -> activeContoursVideo(stage, root, (i, features) -> {
            i.activeContours(features);
            ImageDrawingUtils.drawLinLout(i, features);
            return i.toBufferedImage();
        }, x -> x));

        MenuItem videoACDynamicRange = new MenuItem("Dynamic Range");
        videoACDynamicRange.setOnAction(e -> activeContoursVideo(stage, root, (i, features) -> {
            i.dynamicRangeCompression().activeContours(features);
            ImageDrawingUtils.drawLinLout(i, features);
            return i.toBufferedImage();
        }, formats.Image::dynamicRangeCompression));

        videoAC.getItems().addAll(videoACStandard, videoACDynamicRange);



        activeContours.getItems().addAll(oneImageAC, videoAC);

        borderDetectionMenu.getItems().addAll(basic, prewitt, sobel, canny, susan, activeContours);

        /**
         *
         */

        menuBar.getMenus().addAll(fileMenu,drawMenu, opsMenu, noiseMenu, filtersMenu, thresholdMenu, borderDetectionMenu);

    }

    private void bilateral(Stage stage, BorderPane root) {
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }



        Text TLabel = new Text("Sigma s");
        TextField sField = new TextField();

        Text SLabel = new Text("Sigma r");
        TextField rField = new TextField();

        Button submit = new Button("Apply");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(TLabel, 0, 0);
        gridPane.add(sField, 1, 0);
        gridPane.add(SLabel, 0, 1);
        gridPane.add(rField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        TLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        SLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Bilateral Filter settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();
        formats.Image img = stack.peek();
        formats.Image prev = img;
        submit.setOnAction(new EventHandler<ActionEvent>() {

            formats.Image prev = img;
            @Override
            public void handle(ActionEvent event) {
                try {
                    Integer s = Integer.valueOf(sField.getText());
                    Integer r = Integer.valueOf(rField.getText());
                    formats.Image ans = img.bilateralFilter(7, s, r);
                    stack.replace(prev, ans);
                    prev = ans;
                    renderStackTop(stage, root);
                } catch (Exception e) {
                    showErrorModal(stage, "Invalid dimensions, try again");
                }
            }
        });

    }


    private void lOG(Stage stage, BorderPane root) {
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        formats.Image img = stack.peek();

        final Slider thresholdLevel = new Slider(0, 0.3, 0.15);

        thresholdLevel.setShowTickLabels(true);
        thresholdLevel.setShowTickMarks(true);

        final Label thresholdCaption = new Label("Threshold Level:");

        final Label thresholdValue = new Label(Double.toString(thresholdLevel.getValue()));

        Image fxImg = SwingFXUtils.toFXImage(img.toBufferedImage(), null);

        ImageView iv = new ImageView(fxImg);

        iv.setFitWidth(PREVIEW_WIDTH);
        iv.setFitHeight(PREVIEW_HEIGHT);
        iv.setPreserveRatio(true);


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(500, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(iv, 0, 0);
        gridPane.add(thresholdLevel, 1, 0);
        gridPane.add(thresholdCaption, 2, 0);
        gridPane.add(thresholdValue, 3, 0);


        thresholdCaption.setStyle("-fx-font: normal bold 20px 'Arial' ");
        thresholdValue.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        thresholdLevel.valueProperty().addListener(new ChangeListener<Number>() {

            formats.Image prev = img;

            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {

                thresholdValue.setText(String.format("%.2f", new_val));

                formats.Image ans = img.loGFilter(7,1,(Double)new_val);

                stack.replace(prev, ans);

                prev = ans;

                renderStackTop(stage, root);

            }
        });

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Threshold settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();
    }

    @Override
    public void start(final Stage stage) {

        stage.setTitle("ATI interface");

        final BorderPane root = new BorderPane();

        initializeStackControlButtons(stage, root);

        initializeMenu(stage, root);

        root.setStyle("-fx-background-color: WHITE;");
        stage.setScene(new Scene(root, 1920, 1040));

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
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(stage);

        if(file != null){
            try {
                ImageIO.write(image.toBufferedImage(),
                        "png", new File(file.toString()));
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
            previews[i] = renderImage(stage, root, image, P_X, P_Y - i*(PREVIEW_SEPARATION+PREVIEW_HEIGHT), PREVIEW_WIDTH, PREVIEW_HEIGHT);
        }

    }

    private void removeImages(BorderPane root){
        if (histogramGroup != null)
            root.getChildren().remove(histogramGroup);
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


    private ImageView renderImage(Stage stage, BorderPane root, formats.Image image, double x, double y, double maxWidth, double maxHeight){

        Image fxImg = SwingFXUtils.toFXImage(image.toBufferedImage(), null);

        ImageView iv = new ImageView(fxImg);

        iv.setX(x);
        iv.setY(y);

        iv.setFitWidth(maxWidth);
        iv.setFitHeight(maxHeight);
        iv.setPreserveRatio(true);

        root.getChildren().addAll(iv);

        return iv;
    }

    private void renderX(Stage stage, BorderPane root){
        if(stack.isEmpty())
            return;

        formats.Image X = stack.peek();

        xImageView = renderImage(stage, root, X, X_X, X_Y, MAX_WIDTH, MAX_HEIGHT);


        if (X.getEncoding().equals(Encoding.GS)) {
            showHistogram(root,0,X , null);

        } else{
            Text bandLabel = new Text("Band");
            ChoiceBox bandChoice = new ChoiceBox(FXCollections.observableArrayList("R", "G", "B"));
            bandChoice.getSelectionModel().select(0);
            showHistogram(root,0,X,bandChoice);
            bandChoice.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (!oldValue.equals(newValue)){
                    showHistogram(root , bandChoice.getSelectionModel().getSelectedIndex() , X , bandChoice);
                }
            });

        }

    }

    private void showHistogram(BorderPane root , int component , formats.Image X , ChoiceBox bandChoice){
        double[] histogram = X.histogram(component);

        //Defining the X axis
        NumberAxis xAxis = new NumberAxis(0, 255, 15);
        xAxis.setLabel("Color");

        //defining the y Axis
        NumberAxis yAxis = new NumberAxis(0, Arrays.stream(histogram).max().getAsDouble() + 4, 10);
        yAxis.setLabel("Value");

        //Creating the Area chart
        AreaChart<String, Number> areaChart = new AreaChart(xAxis, yAxis);
        areaChart.setMinHeight(MAX_HEIGHT);
        areaChart.setMinWidth(1.5*MAX_WIDTH);

        //Prepare XYChart.Series objects by setting data
        XYChart.Series series1 = new XYChart.Series();
        series1.setName("Histogram");
        for (int i = 0 ; i < histogram.length ; i++) {
            series1.getData().add(new XYChart.Data(i , histogram[i]));
        }

        //Setting the XYChart.Series objects to area chart
        areaChart.getData().addAll(series1);

        if (histogramGroup != null){
            root.getChildren().remove(histogramGroup);
        }


        histogramGroup = new Group(areaChart);
        if (bandChoice != null){
            histogramGroup.getChildren().add(bandChoice);
        }
        histogramGroup.setLayoutX(X_X + MAX_WIDTH + 100);
        histogramGroup.setLayoutY(Y_Y);
        root.getChildren().add(histogramGroup);
    }

    private void renderY(Stage stage, BorderPane root){
        if(stack.size() < 2)
            return;


        formats.Image Y = stack.peek(1);

        yImageView = renderImage(stage, root, Y, Y_X, Y_Y, MAX_WIDTH, MAX_HEIGHT);

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

                formats.Image image = stack.pop().contaminateO(1, new SaltAndPepperGenerator(P), NoiseApplyMode.DESTRUCTIVE);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid threshold, try again");
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
        newWindow.setTitle("Median filter settings");
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
                showErrorModal(stage,"Invalid grid size, try again");
            }
            newWindow.close();
        });
    }

    private void meanFilter(Stage stage, BorderPane root){

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
        newWindow.setTitle("Mean filter settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Integer N = Integer.valueOf(NField.getText());

                if(N <= 0 || (N % 2) == 0)
                    throw new Exception();

                formats.Image image = stack.pop().meanFilter(N);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid grid size, try again");
            }
            newWindow.close();
        });
    }

    private void scalarProduct(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text centerLabel = new Text("Scalar");
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
        newWindow.setTitle("Scalar product");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double N = Double.valueOf(NField.getText());

                formats.Image image = stack.pop().scalarProduct(N);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"An error occurred");
            }
            newWindow.close();
        });
    }



    private void threshold(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        formats.Image img = stack.peek();

        final Slider thresholdLevel = new Slider(0, 1, 0.5);

        thresholdLevel.setShowTickLabels(true);
        thresholdLevel.setShowTickMarks(true);

        final Label thresholdCaption = new Label("Threshold Level:");

        final Label thresholdValue = new Label(Double.toString(thresholdLevel.getValue()));

        Image fxImg = SwingFXUtils.toFXImage(img.toBufferedImage(), null);

        ImageView iv = new ImageView(fxImg);

        iv.setFitWidth(PREVIEW_WIDTH);
        iv.setFitHeight(PREVIEW_HEIGHT);
        iv.setPreserveRatio(true);


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(500, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(iv, 0, 0);
        gridPane.add(thresholdLevel, 1, 0);
        gridPane.add(thresholdCaption, 2, 0);
        gridPane.add(thresholdValue, 3, 0);


        thresholdCaption.setStyle("-fx-font: normal bold 20px 'Arial' ");
        thresholdValue.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        thresholdLevel.valueProperty().addListener(new ChangeListener<Number>() {

            formats.Image prev = img;

            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {

                thresholdValue.setText(String.format("%.2f", new_val));

                formats.Image ans = img.thresholding((Double)new_val);

                stack.replace(prev, ans);

                prev = ans;

                renderStackTop(stage, root);

            }
        });

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Threshold settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

    }


    private void gammaPower(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text centerLabel = new Text("Value");
        Text gammaLabel = new Text("        gamma");

        TextField gammaField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(centerLabel, 0, 0);
        gridPane.add(gammaLabel, 0, 1);
        gridPane.add(gammaField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        centerLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gammaLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Gamma Correction settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double g = Double.valueOf(gammaField.getText());


                formats.Image image = stack.pop().gammaCorrection(g);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid threshold value, try again");
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

    private void susan(Stage stage, BorderPane root,boolean overImage){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text tLabel = new Text("t");

        TextField tField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(tLabel, 0, 0);
        gridPane.add(tField, 1, 0);
        gridPane.add(submit, 0, 1);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        tLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Susan detector");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer t = Integer.valueOf(tField.getText());


                if(t < 0 || t > 255) {
                    throw new Exception();
                }

                formats.Image image = stack.pop();

                pushAndRender(image.susanDetector(t/255.0,overImage), stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });

    }

    private void gaussFilter(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text NLabel = new Text("N");
        Text SIGMALabel = new Text("SIGMA");

        TextField NField = new TextField();
        TextField SIGMAField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(NLabel, 0, 0);
        gridPane.add(NField, 1, 0);
        gridPane.add(SIGMALabel, 0, 1);
        gridPane.add(SIGMAField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        NLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        SIGMALabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Gaussian filter");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Double SIGMA = Double.valueOf(SIGMAField.getText());
                Integer N = Integer.valueOf(NField.getText());


                if(SIGMA < 0 || (N%2) == 0) {
                    throw new Exception();
                }

                formats.Image image = stack.pop();

                pushAndRender(image.gaussFilter(N, SIGMA), stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });

    }

    private void gaussNoise(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text DLabel = new Text("Density");
        Text SIGMALabel = new Text("Sigma");

        TextField DField = new TextField();
        TextField SIGMAField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(DLabel, 0, 0);
        gridPane.add(DField, 1, 0);
        gridPane.add(SIGMALabel, 0, 1);
        gridPane.add(SIGMAField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        DLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        SIGMALabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Gaussian noise");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Double SIGMA = Double.valueOf(SIGMAField.getText());
                Double D = Double.valueOf(DField.getText());


                if(SIGMA < 0 || D <= 0 || D > 1) {
                    throw new Exception();
                }

                formats.Image image = stack.pop();

                pushAndRender(image.contaminateO(D, new GaussianNoiseGenerator(SIGMA, 0), NoiseApplyMode.ADDITIVE), stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid values, try again");
            }
            newWindow.close();
        });

    }

    private void exponentialNoise(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text DLabel = new Text("Density");
        Text LAMBDALabel = new Text("Lambda");

        TextField DField = new TextField();
        TextField LAMBDAField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(DLabel, 0, 0);
        gridPane.add(DField, 1, 0);
        gridPane.add(LAMBDALabel, 0, 1);
        gridPane.add(LAMBDAField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        DLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        LAMBDALabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Exponential noise");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Double LAMBDA = Double.valueOf(LAMBDAField.getText());
                Double D = Double.valueOf(DField.getText());


                if(D <= 0 || D > 1) {
                    throw new Exception();
                }

                formats.Image image = stack.pop();

                pushAndRender(image.contaminateO(D, new ExponentialNoiseGenerator(LAMBDA), NoiseApplyMode.MULTIPLICATIVE), stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid values, try again");
            }
            newWindow.close();
        });

    }

    private void rayleighNoise(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text DLabel = new Text("Density");
        Text PHILabel = new Text("Phi");

        TextField DField = new TextField();
        TextField PHIField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(DLabel, 0, 0);
        gridPane.add(DField, 1, 0);
        gridPane.add(PHILabel, 0, 1);
        gridPane.add(PHIField, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        DLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        PHILabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Rayleigh noise");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Double PHI = Double.valueOf(PHIField.getText());
                Double D = Double.valueOf(DField.getText());


                if(D <= 0 || D > 1) {
                    throw new Exception();
                }

                formats.Image image = stack.pop();

                pushAndRender(image.contaminateO(D, new RayleighNoiseGenerator(PHI), NoiseApplyMode.MULTIPLICATIVE), stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid values, try again");
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



    private void diffusion(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }


        Text detectorLabel = new Text("Detector");
        ChoiceBox detectorChoice = new ChoiceBox(FXCollections.observableArrayList("Leclerc", "Lorentz", "Isotropic"));

        Text TLabel = new Text("Iterations");
        TextField TField = new TextField();

        Text SLabel = new Text("Sigma");
        TextField SField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(detectorLabel, 0, 0);
        gridPane.add(detectorChoice, 1, 0);
        gridPane.add(TLabel, 0, 1);
        gridPane.add(TField, 1, 1);
        gridPane.add(SLabel, 0, 2);
        gridPane.add(SField, 1, 2);
        gridPane.add(submit, 0, 3);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        detectorLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        TLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        SLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Diffusion settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {
                Integer index = detectorChoice.getSelectionModel().getSelectedIndex();


                formats.Image.DiffusionBorderDetector detector = null;

                switch (index){
                    case 0:
                        detector = formats.Image.DiffusionBorderDetector.LECLERC;
                        break;
                    case 1:
                        detector = formats.Image.DiffusionBorderDetector.LORENTZ;
                        break;
                    case 2:
                        detector = formats.Image.DiffusionBorderDetector.ISOTROPIC;
                        break;
                    default:
                        showErrorModal(stage,"Please select a valid detector");
                        return;

                }

                Integer t = Integer.valueOf(TField.getText());
                Double s = Double.valueOf(SField.getText());

                if (t <= 0){
                    showErrorModal(stage,"Invalid number of iterations");
                    return;
                }

                formats.Image img = stack.pop();
                pushAndRender(img.diffusion(t, s, detector),stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid dimensions, try again");
            }
            newWindow.close();
        });
    }


    private void threeByThreeMask(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }


        Text label = new Text("Mask");

        TextField _00 = new TextField();
        TextField _01 = new TextField();
        TextField _02 = new TextField();

        TextField _10 = new TextField();
        TextField _11 = new TextField();
        TextField _12 = new TextField();

        TextField _20 = new TextField();
        TextField _21 = new TextField();
        TextField _22 = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(label, 0, 0);

        gridPane.add(_00, 0, 1);
        gridPane.add(_01, 1, 1);
        gridPane.add(_02, 2, 1);

        gridPane.add(_10, 0, 2);
        gridPane.add(_11, 1, 2);
        gridPane.add(_12, 2, 2);

        gridPane.add(_20, 0, 3);
        gridPane.add(_21, 1, 3);
        gridPane.add(_22, 2, 3);


        gridPane.add(submit, 0, 4);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        label.setStyle("-fx-font: normal bold 20px 'Arial' ");

        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("3*3 Convolution");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double d00 = Double.valueOf(_00.getText());
                Double d01 = Double.valueOf(_01.getText());
                Double d02 = Double.valueOf(_02.getText());

                Double d10 = Double.valueOf(_10.getText());
                Double d11 = Double.valueOf(_11.getText());
                Double d12 = Double.valueOf(_12.getText());

                Double d20 = Double.valueOf(_20.getText());
                Double d21 = Double.valueOf(_21.getText());
                Double d22 = Double.valueOf(_22.getText());

                double[][] MASK = {{d00, d01, d02},
                                   {d10, d11, d12},
                                   {d20, d21, d22}};

                formats.Image img = stack.pop();
                pushAndRender(img.genericConvolution(MASK) ,stage, root);

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


    private void cropImage(Stage stage, BorderPane root){
        if(stack.isEmpty())
            return;

        Stage newWindow = new Stage();


        formats.Image image = stack.peek();
        ImageView iv = new ImageView(SwingFXUtils.toFXImage(image.toBufferedImage(), null));




        BorderPane bo = new BorderPane(iv);
        bo.setMaxWidth(image.getWidth()); bo.setMaxHeight(image.getHeight());
        ScrollPane scrollPane = new ScrollPane(bo);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


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
            bo.getChildren().add(selection);
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
                newWindow.close();
            }
            bo.getChildren().remove(selection);
            selection.setWidth(0);
            selection.setHeight(0);
        });

        bo.setStyle("-fx-background-color: WHITE;");




        Scene scene = new Scene(scrollPane);

        newWindow.setTitle("Crop image");
        newWindow.setScene(scene);
        newWindow.setWidth(512);
        newWindow.setHeight(512);


        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

    }


    private void activeContours(Stage stage, BorderPane root){
        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Stage newWindow = new Stage();


        formats.Image image = stack.pop();
        ImageView iv = new ImageView(SwingFXUtils.toFXImage(image.toBufferedImage(), null));




        BorderPane bo = new BorderPane(iv);
        bo.setMaxWidth(image.getWidth()); bo.setMaxHeight(image.getHeight());
        ScrollPane scrollPane = new ScrollPane(bo);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


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
            bo.getChildren().add(selection);
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
            int width = image.getWidth();
            int height = image.getHeight();
            if (!isSelectionOutOfBounds(x1,y1,x2,y2,width,height) && !areSamePoint(x1,y1,x2,y2)) {
                RegionFeatures features = RegionFeatures.buildRegionFeatures(image, x1, y1, x2, y2);
                image.activeContours(features);
                formats.Image img = image.clone();
                pushAndRender(ImageDrawingUtils.drawLinLout(img, features), stage, root);
                newWindow.close();
            }
            bo.getChildren().remove(selection);
            selection.setWidth(0);
            selection.setHeight(0);
        });

        bo.setStyle("-fx-background-color: WHITE;");




        Scene scene = new Scene(scrollPane);

        newWindow.setTitle("Select initial region");
        newWindow.setScene(scene);
        newWindow.setWidth(512);
        newWindow.setHeight(512);


        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

    }


    private void canny(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text t1Label = new Text("t1");
        Text t2Label = new Text("t2");

        TextField t1Field = new TextField();
        TextField t2Field = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(t1Label, 0, 0);
        gridPane.add(t1Field, 1, 0);
        gridPane.add(t2Label, 0, 1);
        gridPane.add(t2Field, 1, 1);
        gridPane.add(submit, 0, 2);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        t1Label.setStyle("-fx-font: normal bold 20px 'Arial' ");
        t2Label.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Canny settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double t1 = Double.valueOf(t1Field.getText());

                Double t2 = Double.valueOf(t2Field.getText());

                if(t1 >= t2)
                    throw new Exception();

                formats.Image image = stack.pop().canny(t1, t2);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid threshold value, try again");
            }
            newWindow.close();
        });
    }

    private void cannyIso(Stage stage, BorderPane root){

        if(stack.isEmpty()){
            showErrorModal(stage, "Empty stack");
            return;
        }

        Text t1Label = new Text("t1");
        Text t2Label = new Text("t2");

        TextField t1Field = new TextField();
        TextField t2Field = new TextField();

        Text detectorLabel = new Text("Detector");
        ChoiceBox detectorChoice = new ChoiceBox(FXCollections.observableArrayList("Leclerc", "Lorentz", "Isotropic"));

        Text TLabel = new Text("Iterations");
        TextField TField = new TextField();

        Text SLabel = new Text("Sigma");
        TextField SField = new TextField();

        Button submit = new Button("OK");


        GridPane gridPane = new GridPane();
        gridPane.setMinSize(400, 200);
        gridPane.setPadding(new Insets(10, 10, 10, 10));
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        gridPane.setAlignment(Pos.CENTER);

        gridPane.add(t1Label, 0, 0);
        gridPane.add(t1Field, 1, 0);
        gridPane.add(t2Label, 0, 1);
        gridPane.add(t2Field, 1, 1);
        gridPane.add(detectorLabel, 0, 2);
        gridPane.add(detectorChoice, 1, 2);
        gridPane.add(TLabel, 0, 3);
        gridPane.add(TField, 1, 3);
        gridPane.add(SLabel, 0, 4);
        gridPane.add(SField, 1, 4);
        gridPane.add(submit, 0, 5);

        submit.setStyle("-fx-background-color: darkslateblue; -fx-text-fill: white;");

        t1Label.setStyle("-fx-font: normal bold 20px 'Arial' ");
        t2Label.setStyle("-fx-font: normal bold 20px 'Arial' ");
        detectorLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        TLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        SLabel.setStyle("-fx-font: normal bold 20px 'Arial' ");
        gridPane.setStyle("-fx-background-color: WHITE;");

        Scene scene = new Scene(gridPane);


        Stage newWindow = new Stage();
        newWindow.setTitle("Canny settings");
        newWindow.setScene(scene);

        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

        submit.setOnAction(event -> {
            try {

                Double t1 = Double.valueOf(t1Field.getText());

                Double t2 = Double.valueOf(t2Field.getText());

                if(t1 >= t2)
                    throw new Exception();


                Integer index = detectorChoice.getSelectionModel().getSelectedIndex();


                formats.Image.DiffusionBorderDetector detector = null;

                switch (index){
                    case 0:
                        detector = formats.Image.DiffusionBorderDetector.LECLERC;
                        break;
                    case 1:
                        detector = formats.Image.DiffusionBorderDetector.LORENTZ;
                        break;
                    case 2:
                        detector = formats.Image.DiffusionBorderDetector.ISOTROPIC;
                        break;
                    default:
                        showErrorModal(stage,"Please select a valid detector");
                        return;

                }

                Integer t = Integer.valueOf(TField.getText());
                Double s = Double.valueOf(SField.getText());

                if (t <= 0){
                    showErrorModal(stage,"Invalid number of iterations");
                    return;
                }

                formats.Image image = stack.pop().diffusion(t, s, detector).canny(t1, t2);

                pushAndRender(image, stage, root);

            } catch (Exception e) {
                showErrorModal(stage,"Invalid threshold value, try again");
            }
            newWindow.close();
        });
    }


    private void activeContoursVideo(Stage stage, BorderPane root, BiFunction<formats.Image, RegionFeatures, BufferedImage> f, Function<formats.Image, formats.Image> regionF){
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if(files == null){
            showErrorModal(stage, "No images selected");
            return;
        }

        List<formats.Image> images = new ArrayList<>(files.size());

        files.forEach(x -> {
            try {
                images.add(new formats.Image(x.toString()));
            } catch (Exception e) {
            }
        });

        Stage newWindow = new Stage();


        formats.Image image = images.get(0);

        ImageView iv = new ImageView(SwingFXUtils.toFXImage(image.toBufferedImage(), null));


        BorderPane bo = new BorderPane(iv);
        bo.setMaxWidth(image.getWidth()); bo.setMaxHeight(image.getHeight());
        ScrollPane scrollPane = new ScrollPane(bo);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);


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
            bo.getChildren().add(selection);
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
            int width = image.getWidth();
            int height = image.getHeight();
            if (!isSelectionOutOfBounds(x1,y1,x2,y2,width,height) && !areSamePoint(x1,y1,x2,y2)) {

                newWindow.close();
                RegionFeatures features = RegionFeatures.buildRegionFeatures(regionF.apply(image), x1, y1, x2, y2);
                activeContoursVideoReproduction(stage, root, features, images, image.getWidth(), image.getHeight(), f);

            }
            bo.getChildren().remove(selection);
            selection.setWidth(0);
            selection.setHeight(0);
        });

        bo.setStyle("-fx-background-color: WHITE;");




        Scene scene = new Scene(scrollPane);

        newWindow.setTitle("Select initial region");
        newWindow.setScene(scene);
        newWindow.setWidth(512);
        newWindow.setHeight(512);


        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();

    }


    private void activeContoursVideoReproduction(Stage stage, BorderPane root, RegionFeatures features, List<formats.Image> images, int width, int height, BiFunction<formats.Image, RegionFeatures, BufferedImage> f) {


        Stage newWindow = new Stage();

        List<ImageView> processedImages = new ArrayList<>(images.size());
        long[] times = new long[images.size()];

        for (int k = 0; k < images.size(); k++) {
            long t0 = System.currentTimeMillis();
            processedImages.add(new ImageView(SwingFXUtils.toFXImage(f.apply(images.get(k), features), null)));
            times[k] = System.currentTimeMillis()-t0;
        }

        Group sequence = new Group(processedImages.get(0));
        Timeline timeline = new Timeline();

        for(int k = 0; k < images.size(); k++){
            final int idx = k;
            final long time = times[k];
            timeline.getKeyFrames().addAll(new KeyFrame(Duration.millis((1000/24)*(k+1)), new EventHandler<ActionEvent>(){
                                @Override
                                public void handle(ActionEvent t) {
                                    System.out.println(idx);
                                    sequence.getChildren().setAll(new Text(String.format("%d\n", time)), processedImages.get(idx));
                                }
                           }));
        }
        timeline.play();




        Group rootGroup = new Group(sequence);

        Scene scene = new Scene(rootGroup, width, height);

        newWindow.setTitle("Active contours video");
        newWindow.setScene(scene);
        newWindow.setWidth(width);
        newWindow.setHeight(height);


        newWindow.setX(stage.getX() + 200);
        newWindow.setY(stage.getY() + 100);

        newWindow.show();
        newWindow.setOnCloseRequest(e-> timeline.stop());









    }



}
