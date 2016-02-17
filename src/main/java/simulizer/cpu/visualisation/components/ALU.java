package simulizer.cpu.visualisation.components;

import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

import java.util.Observable;
import java.util.Observer;

public class ALU extends ComponentStackPane {

    public ALU(int x, int y, int width, int height, String label){
        super(x, y, width, height, label);
        drawShape(new Polyline());
        setAttributes();
        text.setTextAlignment(TextAlignment.RIGHT);
    }

    public void drawShape(Polyline polyline){
        double baseX = x;
        double baseY = y;

        double rightHeight = height * 0.4;
        double rightSmallHeight = (height - rightHeight) / 2;

        double gapWidth = height * 0.4;
        double leftHeight = (height - gapWidth) / 2;

        polyline.getPoints().clear();
        polyline.getPoints().addAll(new Double[]{
                baseX, baseY,
                baseX + width, baseY + rightSmallHeight,
                baseX + width, baseY + rightHeight + rightSmallHeight,
                baseX, baseY + height,
                baseX, baseY + gapWidth + leftHeight,
                baseX + (width * 0.6), baseY + leftHeight + (gapWidth / 2),
                baseX, baseY + leftHeight,
                baseX, baseY
        });

        this.shape = polyline;
    }

    public double getShapeHeight(){
        double rightHeight = height * 0.4;
        double rightSmallHeight = (height - rightHeight) / 2;

        double gapWidth = height * 0.4;
        double leftHeight = (height - gapWidth) / 2;

        return (height + (rightHeight + rightSmallHeight)) / 2;
    }

    public void setShapeWidth(double width){
        this.width = width;
        text.setWrappingWidth(width * 0.9);
    }

    public void setShapeHeight(double height){
        this.height = height;
    }

    public void setAttrs(double x, double y, double width, double height){
        setLayoutX(x);
        shape.setLayoutX(x);
        setLayoutY(y);
        shape.setLayoutY(y);
        setShapeWidth(width);
        setPrefWidth(width);
        setShapeHeight(height);
        setPrefHeight(height);
        drawShape(((Polyline) this.shape));
    }

}
