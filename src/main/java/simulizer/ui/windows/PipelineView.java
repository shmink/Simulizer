package simulizer.ui.windows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;
import simulizer.assembler.representation.Address;
import simulizer.assembler.representation.Statement;
import simulizer.lowlevel.models.PipelineHistoryModel;
import simulizer.simulation.cpu.components.CPU;
import simulizer.simulation.messages.PipelineHazardMessage;
import simulizer.ui.components.NumberTextField;
import simulizer.ui.interfaces.InternalWindow;
import simulizer.ui.interfaces.WindowEnum;
import simulizer.utils.ColorUtils;

public class PipelineView extends InternalWindow implements Observer {
	// Graphical things
	private Canvas canvas = new Canvas();
	private Pane canvasPane = new Pane();
	private BorderPane borderPane = new BorderPane();

	private HBox controlBox = new HBox();
	private VBox buttonBox = new VBox();
	private Button leftButton = new Button("<");
	private Button rightButton = new Button(">");
	private Label cycleInputLabel = new Label("Go to:");
	private TextField cycleInput = new NumberTextField();
	private CheckBox followCheckBox = new CheckBox("Follow");

	private final String DEFAULT_INSTR = "\n\n\n\n";
	private Label instructionInfoLabel = new Label(DEFAULT_INSTR);

	private String selectedAddress;

	// Model (currently static - watch out)
	public static PipelineHistoryModel model = new PipelineHistoryModel();
	// Canvas has a maximum size, so don't draw more than it!
	private int numColumnsToDraw;
	private int startCycle = 0;
	private boolean snapToEnd;

	private boolean isPipelined;
	private boolean isRunning;

	// Dimensions used for calculations
	private double rectWidth;
	private double cycleWidth;
	private double rectGap;

	// The width and the height of the canvas
	// saved for reuse throughout calculations
	// after resize
	private double w;
	private double x0;
	private double h;
	private double realW;
	private double realH;

	public PipelineView() {
		setTitle("Pipeline");

		// Configure the components
		followCheckBox.setSelected(true);
		followCheckBox.setOnAction(e -> repaint());

		leftButton.setOnAction(e -> {
			setStartCycle(startCycle - 1);
			repaint();
		});
		rightButton.setOnAction(e -> {
			setStartCycle(startCycle + 1);
			repaint();
		});
		leftButton.setCursor(Cursor.DEFAULT);
		rightButton.setCursor(Cursor.DEFAULT);

		cycleInput.setPrefColumnCount(5);
		cycleInput.setPromptText("Cycle no.");
		cycleInput.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ENTER && !cycleInput.getText().isEmpty()) {
				setStartCycle(Integer.valueOf(cycleInput.getText()));
				repaint();
				Platform.runLater(() -> cycleInput.setText(""));
			}
		});

		canvasPane.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			if (!isRunning || !isPipelined) return;

			Optional<Pair<Integer, Address>> cycleAndAddress = getAddressAtPoint(e.getX(), e.getY());
			if (cycleAndAddress.isPresent()) {
				Pair<Integer, Address> ca = cycleAndAddress.get();
				Address addr = ca.getValue();
				String name;
				if (addr == null || (name = getShortName(addr)).equals(selectedAddress)) selectedAddress = null;
				else selectedAddress = name;

				int line = getWindowManager().getCPU().getProgram().lineNumbers.getOrDefault(ca.getValue(), -1);
				if (line != -1) {
					Editor ed = (Editor) getWindowManager().getWorkspace().findInternalWindow(WindowEnum.EDITOR);
					if (ed != null) {
						Platform.runLater(() -> ed.gotoLine(line));
					}
				}

			} else selectedAddress = null;

			repaint();

			Platform.runLater(canvasPane::requestFocus);
		});
		canvasPane.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (!isRunning || !isPipelined) return;

			KeyCode code = e.getCode();

			if (code == KeyCode.LEFT || code == KeyCode.KP_LEFT) {
				setStartCycle(startCycle - 1);
				repaint();
			} else if (code == KeyCode.RIGHT || code == KeyCode.KP_RIGHT) {
				setStartCycle(startCycle + 1);
				repaint();
			}

			e.consume(); // the right arrow key seems to want to transfer the focus
			Platform.runLater(canvasPane::requestFocus);
		});

		instructionInfoLabel.setStyle("-fx-font-family: monospace");
		instructionInfoLabel.setPadding(new Insets(20, 0, 15, 20));

		canvasPane.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
			if (!isRunning) return;

			Optional<Pair<Integer, Address>> cycleAndAddress = getAddressAtPoint(e.getX(), e.getY());

			String newText;

			if (cycleAndAddress.isPresent()) {
				Pair<Integer, Address> ca = cycleAndAddress.get();
				int cycle = ca.getKey();
				Address addr = ca.getValue();

				newText = addr == null ? getHazardInfo(cycle) : getAddressInfo(addr);
			} else newText = DEFAULT_INSTR;

			Platform.runLater(() -> instructionInfoLabel.setText(newText));
		});

		controlBox.setAlignment(Pos.CENTER_LEFT);
		buttonBox.setAlignment(Pos.CENTER_LEFT);

		// Now add everything
		canvasPane.getChildren().add(canvas);
		HBox topBox = new HBox(), bottomBox = new HBox();
		topBox.setAlignment(Pos.CENTER);
		bottomBox.setAlignment(Pos.CENTER);
		topBox.getChildren().addAll(followCheckBox, leftButton, rightButton);
		bottomBox.getChildren().addAll(cycleInputLabel, cycleInput);
		buttonBox.getChildren().addAll(topBox, bottomBox);
		controlBox.getChildren().addAll(buttonBox, instructionInfoLabel);

		borderPane.setCenter(canvasPane);
		borderPane.setBottom(controlBox);

		getContentPane().getChildren().add(borderPane);

		// These things should be the same throughout, so just set them now
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setLineWidth(2);
		gc.setTextAlign(TextAlignment.CENTER);
		gc.setTextBaseline(VPos.CENTER);
		gc.setFont(new Font("Monospace", 12));

		// The canvas controls its own width, but it needs to know
		// the height of the scroll pane
		canvas.heightProperty().bind(canvasPane.heightProperty());
		canvas.heightProperty().addListener(e -> Platform.runLater(this::repaint));
		canvas.widthProperty().bind(canvasPane.widthProperty());
		canvas.widthProperty().addListener(e -> Platform.runLater(this::repaint));

		model.addObserver(this);
	}

	private Optional<Pair<Integer, Address>> getAddressAtPoint(double x, double y) {
		List<PipelineHistoryModel.PipelineState> history = model.getHistory();

		// Loop over each column
		for (int col = 0, cycle = startCycle; col < numColumnsToDraw; ++col, ++cycle) {
			PipelineHistoryModel.PipelineState state = history.get(cycle);

			double xLeft = x0 + (col + 0.5) * cycleWidth - rectWidth / 2;
			// If the x coordinate isn't right, then skip to the next iteration
			if (x < xLeft || x > xLeft + rectWidth) continue;

			double yTop = rectGap / 2;

			List<Address> before = state.before;
			for (Address addr : before) {
				if (y >= yTop && y < yTop + rectWidth) return Optional.of(new Pair<>(cycle, addr));
				else yTop += rectGap + rectWidth;
			}

			yTop = rectGap/2 + 3 * (rectGap + rectWidth);

			List<Address> pipeline = Arrays.asList(state.fetched, state.decoded, state.executed);
			for (Address stage : pipeline) {
				if (y >= yTop && y < yTop + rectWidth) {
					return Optional.of(new Pair<>(cycle, stage));
				} else yTop += rectGap + rectWidth;
			}

			yTop = rectGap/2 + 6 * (rectGap + rectWidth);

			List<Address> after = state.after;
			for (Address addr : after) {
				if (y >= yTop && y < yTop + rectWidth) return Optional.of(new Pair<>(cycle, addr));
				else yTop += rectGap + rectWidth;
			}
		}

		return Optional.empty();
	}

	@Override
	public void close() {
		model.removeObserver(this);
		super.close();
	}

	/**
	 * Sets the leftmost cycle on the screen.
	 * If the input is < 0, then it is snapped to 0.
	 * If the input is off the right end, it is snapped to the rightmost cycle.
	 * Otherwise it is set as the input.
	 *
	 * @param startCycle
	 *            the new value for start cycle
	 */
	private void setStartCycle(final int startCycle) {
		if (startCycle <= 0) this.startCycle = 0;
		else if (startCycle >= model.size()) this.startCycle = Math.max(0, model.size() - 1);
		else this.startCycle = startCycle;
	}

	private void calculateParameters() {
		x0 = 150;

		this.w = realW - x0;
		this.h = 0.95 * realH;

		rectWidth = 2. / 21 * h;
		cycleWidth = 3. / 2 * rectWidth;
		// TODO figure out a proper calculation
		rectGap = (h - 8 * rectWidth) / 15;

		setStartCycle(startCycle); // will reset startCycle to 0 if model has been reset

		numColumnsToDraw = Math.min(model.size() - startCycle, (int) (w / cycleWidth));
		numColumnsToDraw = Math.max(0, numColumnsToDraw);
		assert (numColumnsToDraw >= 0);

		if (snapToEnd) setStartCycle(model.size() - numColumnsToDraw);
	}

	/**
	 * Draws the dividers between the different contents of the
	 * pipeline between cycles
	 */
	private void drawDividers(GraphicsContext gc) {
		// Draw the vertical dividers
		gc.setStroke(Color.GRAY);

		gc.beginPath();
		for (int i = 0; i <= numColumnsToDraw; ++i) {
			double x = x0 + i * cycleWidth;
			gc.strokeLine(x, 0, x, h);
		}
		gc.closePath();

		gc.beginPath();
		if (numColumnsToDraw > 0) {
			double xEnd = x0 + numColumnsToDraw * cycleWidth;
			// Draw the horizontal dividers
			gc.setStroke(Color.BLACK);
			double y0 = h / 3;
			double y1 = 2 * h / 3;
			gc.strokeLine(0, y0, xEnd, y0);
			gc.strokeLine(0, y1, xEnd, y1);
		}
		gc.closePath();
	}

	private void drawExplainers(GraphicsContext gc) {
		gc.setTextAlign(TextAlignment.RIGHT);

		double x = 0.95 * x0;
		double maxWidth = 0.90 * x0;
		double y = h / 6;

		gc.setFill(Paint.valueOf("black"));

		gc.beginPath();
		gc.fillText("Waiting\nInstructions", x, y, maxWidth);

		y = h / 3 + rectGap / 2 + rectWidth / 2;
		gc.fillText("Stage 1: Fetch", x, y, maxWidth);

		y += rectGap + rectWidth;
		gc.fillText("Stage 2: Decode", x, y, maxWidth);

		y += rectGap + rectWidth;
		gc.fillText("Stage 3: Execute", x, y, maxWidth);

		y = 5 * h / 6;
		gc.fillText("Completed\nInstructions", x, y, maxWidth);

		gc.closePath();

		gc.setTextAlign(TextAlignment.CENTER);
	}

	@Override
	public void update(Observable o, Object pipelineState) {
		this.isPipelined = getWindowManager().getCPU().isPipelined();
		this.isRunning = getWindowManager().getCPU().isRunning();

		this.snapToEnd = followCheckBox.isSelected();
		repaint();
		// Should only snap to end when an update has been fire
		// i.e. not when user is scrolling around
		this.snapToEnd = false;
	}

	private void drawAddresses(GraphicsContext gc) {
		// First need to draw the rectangles in the column
		// We know the length of the rectangles and the gaps
		// between them. Their position is just
		// half way between the cycle boundaries then - rectWidth/2.
		gc.setFill(Paint.valueOf("skyblue"));

		List<PipelineHistoryModel.PipelineState> history = model.getHistory();

		if (isPipelined) {
			// Draw the addresses before the pipeline
			for (int col = 0, cycle = startCycle; col < numColumnsToDraw; ++col, ++cycle) {
				List<Address> before = history.get(cycle).before;

				double yTracker = rectGap / 2;
				double yCenter = yTracker + rectWidth / 2;

				double xCenter = x0 + (col + 0.5) * cycleWidth;
				double xLeft = xCenter - rectWidth / 2;

				for (Address addr : before) {
					String name = getShortName(addr);

					Color bg = ColorUtils.getColor(addr);
					gc.setFill(bg);
					drawBorderedRectangle(gc, xLeft, yTracker, rectWidth, rectWidth, name.equals(selectedAddress));

					gc.setFill(ColorUtils.getTextColor(bg));
					drawText(gc, name, xCenter, yCenter, rectWidth);

					yTracker += rectGap + rectWidth;
					yCenter += rectGap + rectWidth;
				}
			}
		}

		// Draw the addresses in the pipeline
		for (int col = 0, cycle = startCycle; col < numColumnsToDraw; ++col, ++cycle) {
			PipelineHistoryModel.PipelineState state = history.get(cycle);

			double yTracker = h / 3 + rectGap / 2; // starts at top of first rect to draw
			double yCenter = yTracker + rectWidth / 2;

			double xCenter = x0 + (col + 0.5) * cycleWidth;
			double xLeft = xCenter - rectWidth / 2;

			Address[] parts = { state.fetched, state.decoded, state.executed };

			for (int a = 0; a < 3; ++a) {
				Color bg = ColorUtils.getColor(parts[a]);
				gc.setFill(bg);
				if (parts[a] == null) drawBorderedCircle(gc, xLeft, yTracker, rectWidth, rectWidth);
				else {
					String name = getShortName(parts[a]);
					drawBorderedRectangle(gc, xLeft, yTracker, rectWidth, rectWidth, name.equals(selectedAddress));
					gc.setFill(ColorUtils.getTextColor(bg));
					drawText(gc, name, xCenter, yCenter);
				}

				yTracker += rectGap + rectWidth;
				yCenter += rectGap + rectWidth;
			}

			gc.setFill(Paint.valueOf("black"));
			drawText(gc, "" + cycle, xCenter, 0.975 * realH);
		}

		if (isPipelined) {
			// Draw the addresses after the pipeline
			for (int col = 0, cycle = startCycle; col < numColumnsToDraw; ++col, ++cycle) {
				List<Address> after = history.get(cycle).after;

				double yTracker = 2 * h / 3 + rectGap / 2;
				double yCenter = yTracker + rectWidth / 2;

				double xCenter = x0 + (col + 0.5) * cycleWidth;
				double xLeft = xCenter - rectWidth / 2;

				for (Address addr : after) {
					String name = getShortName(addr);

					Color bg = ColorUtils.getColor(addr);
					gc.setFill(bg);
					drawBorderedRectangle(gc, xLeft, yTracker, rectWidth, rectWidth, name.equals(selectedAddress));

					gc.setFill(ColorUtils.getTextColor(bg));
					drawText(gc, name, xCenter, yCenter);

					yTracker += rectGap + rectWidth;
					yCenter += rectGap + rectWidth;
				}
			}
		}
	}

	@Override
	public void ready() {
		super.ready();
		isPipelined = getWindowManager().getCPU().isPipelined();
		isRunning = getWindowManager().getCPU().isRunning();
		this.snapToEnd = true;
		repaint();
		this.snapToEnd = false;
	}

	public void repaint() {
		GraphicsContext gc = canvas.getGraphicsContext2D();
		this.realW = canvas.getWidth();
		this.realH = canvas.getHeight();
		gc.clearRect(0, 0, realW, realH);

		calculateParameters();

		if (isRunning) {
			drawDividers(gc);
			drawExplainers(gc);
			drawAddresses(gc);
		} else {
			gc.setFill(Color.BLACK);
			drawText(gc, "Check the CPU is running to view this window", realW / 2, h / 2);
		}
	}

	// ***Utilities***

	private void drawBorderedRectangle(GraphicsContext gc, double x, double y, double w, double h, boolean highlight) {
		gc.beginPath();

		gc.fillRect(x, y, w, h);

		if (highlight) {
			gc.setStroke(Color.YELLOW);
			gc.setLineWidth(7);
		}

		gc.strokeRect(x, y, w, h);
		gc.closePath();

		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2);
	}

	private void drawBorderedCircle(GraphicsContext gc, double x, double y, double w, double h) {
		gc.beginPath();
		gc.fillOval(x, y, w, h);
		gc.strokeOval(x, y, w, h);
		gc.closePath();
	}

	private void drawText(GraphicsContext gc, String text, double x, double y, double maxWidth) {
		gc.beginPath();
		gc.fillText(text, x, y, maxWidth);
		gc.closePath();
	}

	private void drawText(GraphicsContext gc, String text, double x, double y) {
		gc.beginPath();
		gc.fillText(text, x, y);
		gc.closePath();
	}

	private static String getShortName(Address address) {
		String hex = address.toString();
		return hex.substring(Math.max(0, hex.length() - 3));
	}

	private String getHazardInfo(int cycle) {
		Optional<PipelineHazardMessage.Hazard> hOpt = model.getHistory().get(cycle).hazard;

		if (hOpt.isPresent()) {
			String fortune = " ";
      String shouldShow = System.getProperty("easter-fortune");
			if (shouldShow != null && shouldShow.equals("true") && cycle > 0 && cycle % 100 == 0 && fortunes.size() > 0) fortune = fortunes.get(cycle / 100);

			PipelineHazardMessage.Hazard h = hOpt.get();
			return String.format("Hazard: %s%n %n%s%n ", h.toString(), fortune);
		} else return String.format("Not a hazard%n %n %n ");
	}

	private static List<String> fortunes = new ArrayList<>();
	{
		try (BufferedReader reader = new BufferedReader(new FileReader(new File(getClass().getResource("/fortunes").getFile())))) {

			String s;
			while ((s = reader.readLine()) != null)
				if (!s.trim().isEmpty()) fortunes.add(s);

			Collections.shuffle(fortunes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getAddressInfo(Address address) {
		CPU cpu = getWindowManager().getCPU();
		Map<Address, Statement> textSegment = cpu.getProgram().textSegment;

		if (textSegment.containsKey(address)) {
			Statement s = textSegment.get(address);
			int lineNum = cpu.getProgram().lineNumbers.getOrDefault(address, -1);

			// @formatter:off
			// TODO: (matt) working on it...
			return String.format(
				  "       Statement: %s%n"
				+ "         Address: %s%n"
				+ "Instruction type: %s%n"
				+ "     Line number: %d",
					s.toString(), address.toString(), s.getInstruction(), lineNum);
			// @formatter:on
		} else {
			// @formatter:off
			return "         Address: " + address + "\n"
				  +"         Does not point to an instruction\n"
				  + " \n"
				  + " ";
			// @formatter:on
		}
	}

}
