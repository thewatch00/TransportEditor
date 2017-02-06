package com.oskopek.transporteditor.controller;

import com.google.common.eventbus.Subscribe;
import com.oskopek.transporteditor.event.DisposeGraphViewerEvent;
import com.oskopek.transporteditor.event.GraphUpdatedEvent;
import com.oskopek.transporteditor.model.problem.RoadGraph;
import com.oskopek.transporteditor.view.AlertCreator;
import com.oskopek.transporteditor.view.ProgressCreator;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.graphstream.algorithm.Toolkit;
import org.graphstream.stream.ProxyPipe;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.j2dviewer.J2DGraphRenderer;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.util.DefaultMouseManager;
import org.graphstream.ui.view.util.MouseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
public class CenterPaneController extends AbstractController {

    @Inject
    private transient Logger logger;

    @FXML
    private transient SwingNode problemGraph;

    private transient Viewer viewer;

    private transient RoadGraphSelectionHandler graphSelectionHandler;

    @FXML
    private void initialize() {
        eventBus.register(this);
    }

    @Subscribe
    public void disposeGraphViewer(DisposeGraphViewerEvent event) {
        logger.debug("Disposing Graph viewer in CenterPane.");
        if (viewer != null) {
            viewer.close();
            viewer = null;
        }
        problemGraph.getContent().removeAll();
        problemGraph.setContent(null);
    }

    @Subscribe
    public void redrawGraph(GraphUpdatedEvent graphUpdatedEvent) {
        Platform.runLater(() -> problemGraph.setContent(new JLabel(messages.getString("problem.noproblemloaded"))));

        RoadGraph graph = null;
        try {
            graph = application.getPlanningSession().getProblem().getRoadGraph();
        } catch (NullPointerException e) {
            logger.trace("Could not get graph for redrawing, got a NPE along the way.", e);
        }

        if (graph == null) {
            return;
        }

        graph.setDefaultStyling();
        graphSelectionHandler = new RoadGraphSelectionHandler(graph);
        disposeGraphViewer(null);
        final long nodeCount = graph.getNodeCount();
        viewer = graph.display(true);
        ProxyPipe proxyPipe = viewer.newViewerPipe();
        proxyPipe.addAttributeSink(graph);
        MousePipe mousePipe = new MousePipe("mousePipe", viewer.newViewerPipe());
        MouseCatcher mouseCatcher = new MouseCatcher(graph, viewer.getGraphicGraph(), graphSelectionHandler);
        mousePipe.addViewerListener(mouseCatcher);
        ViewPanel viewPanel = viewer.addView("graph", new J2DGraphRenderer(), false);
        viewPanel.setMouseManager(new SpriteUnclickableMouseManager());
        viewPanel.addMouseListener(new MouseAdapter() {

            private double xFrom, yFrom, xTo, yTo;

            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);

            }

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                xFrom = e.getX();
                yFrom = e.getY();
                mouseCatcher.setControlDown(e.isControlDown());
                proxyPipe.pump();
                mousePipe.pump();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                xTo = e.getX();
                yTo = e.getY();
                logger.debug("From: {}x{} -> {}x{}", xFrom, yFrom, xTo, yTo);
                logger.debug("Selected elements: {}", viewPanel.allNodesOrSpritesIn(xFrom, yFrom, xTo, yTo));
                mouseCatcher.setControlDown(e.isControlDown());
                proxyPipe.pump();
                mousePipe.pump();
            }
        });
        Platform.runLater(() -> {
            problemGraph.setContent(viewPanel);
            problemGraph.setDisable(false);
            Task<Void> springLayoutEarlyTermination = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    final long start = 2000L;
                    final long step = 50L;
                    final long total = start + nodeCount * step;
                    updateProgress(0, total);
                    try {
                        Thread.sleep(start / 2);
                        updateProgress(start / 2, total);
                        Thread.sleep(start / 2);
                        updateProgress(start, total);
                        for (int i = 0; i < nodeCount; i++) {
                            Thread.sleep(step);
                            updateProgress(start + step * i, total);
                        }
                        updateProgress(total, total);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Sleep broken.", e);
                    }
                    logger.debug("Killing spring layout early ({}ms).", total);
                    return null;
                }
            };
            Stage progressDialog = ProgressCreator.showProgress(springLayoutEarlyTermination::progressProperty,
                    messages.getString("progress.pleaseWait"));
            springLayoutEarlyTermination.setOnFailed(event -> {
                progressDialog.close();
                AlertCreator.showAlert(Alert.AlertType.ERROR,
                        messages.getString("root.failedToLayoutGraph") + ":\n\n" + event.getSource().getException());
            });
            springLayoutEarlyTermination.setOnSucceeded(event -> {
                progressDialog.close();
                Platform.runLater(viewer::disableAutoLayout);
            });
            CompletableFuture.runAsync(springLayoutEarlyTermination);
        });
    }

    private static class SpriteUnclickableMouseManager extends DefaultMouseManager {
        @Override
        protected void elementMoving(GraphicElement element, MouseEvent event) {
            if (!(element instanceof GraphicSprite)) {
                super.elementMoving(element, event);
            }
        }
    }

    private static class MouseCatcher implements ViewerListener {

        private static final Function<Double, Integer> convertToInt = x -> (int) (x * 1000);
        private final Logger logger = LoggerFactory.getLogger(getClass());
        private final RoadGraph roadGraph;
        private final GraphicGraph graph;
        private boolean controlDown = false;
        private final RoadGraphSelectionHandler graphSelectionHandler;

        MouseCatcher(RoadGraph roadGraph, GraphicGraph graph, RoadGraphSelectionHandler graphSelectionHandler) {
            this.roadGraph = roadGraph;
            this.graph = graph;
            this.graphSelectionHandler = graphSelectionHandler;
        }

        public void setControlDown(boolean controlDown) {
            this.controlDown = controlDown;
        }

        @Override
        public void viewClosed(String s) {
            logger.debug("Closed view \"{}\"", s);
        }

        @Override
        public void buttonPushed(String s) {
            logger.debug("Pushed node \"{}\"", s);
        }

        @Override
        public void buttonReleased(String s) {
            logger.debug("Released node \"{}\"", s);
            if (controlDown) {
                logger.debug("Toggle select location: {}", s);
                graphSelectionHandler.toggleSelectLocation(s); // TODO: toggle select road
            }
            roadGraph.getAllLocations().forEach(n -> {
                String name = n.getName();
                Point3 t3 = Toolkit.nodePointPosition(graph, name);
                roadGraph.moveLocation(name, convertToInt.apply(t3.x), convertToInt.apply(t3.y));
            });
        }
    }

    private static class MousePipe extends ViewerPipe {
        MousePipe(String id, ProxyPipe pipeIn) {
            super(id, pipeIn);
        }

        @Override
        public void edgeAttributeAdded(String sourceId, long timeId, String edgeId, String attribute, Object value) {
            sendEdgeAttributeAdded(sourceId, timeId, edgeId, attribute, value);
            if ("ui.clicked".equals(attribute)) {
                for (ViewerListener listener : viewerListeners) {
                    listener.buttonPushed(edgeId);
                }
            }
        }

        public void edgeAttributeRemoved(String sourceId, long timeId, String edgeId, String attribute) {
            sendEdgeAttributeRemoved(sourceId, timeId, edgeId, attribute);
            if ("ui.clicked".equals(attribute)) {
                for (ViewerListener listener : viewerListeners) {
                    listener.buttonReleased(edgeId);
                }
            }
        }


    }

}
